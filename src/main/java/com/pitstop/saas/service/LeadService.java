package com.pitstop.saas.service;

import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.dto.NotificacaoRequest;
import com.pitstop.notificacao.service.EmailService;
import com.pitstop.notificacao.service.WhatsAppService;
import com.pitstop.saas.domain.Lead;
import com.pitstop.saas.domain.StatusLead;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.repository.LeadRepository;
import com.pitstop.shared.exception.BusinessException;
import com.pitstop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing leads in the SaaS platform.
 *
 * Handles lead capture, qualification, and conversion tracking.
 * Sends automatic notifications when new leads arrive.
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;

    @Value("${app.leads.notification-email:contato@pitstopai.com.br}")
    private String notificationEmail;

    @Value("${app.leads.notification-whatsapp:5551994651075}")
    private String notificationWhatsApp;

    /**
     * Cria um novo lead a partir do formulário público.
     *
     * @param request dados do lead
     * @return lead criado
     * @throws BusinessException se email já existe
     */
    @Transactional
    public LeadDTO criarLead(CreateLeadRequest request) {
        log.info("Capturando novo lead: {} - {}", request.nome(), request.email());

        // Validar se email já existe
        if (leadRepository.existsByEmail(request.email())) {
            log.warn("Tentativa de criar lead duplicado: {}", request.email());
            throw new BusinessException("Já existe um lead com este email");
        }

        // Criar lead
        Lead lead = Lead.builder()
            .nome(request.nome())
            .email(request.email())
            .whatsapp(request.whatsapp())
            .origem(request.origem())
            .status(StatusLead.NOVO)
            .build();

        Lead saved = leadRepository.save(lead);
        log.info("Lead criado com sucesso: id={}, origem={}", saved.getId(), saved.getOrigem());

        // Enviar notificações assíncronas
        enviarNotificacoes(saved);

        return toDTO(saved);
    }

    /**
     * Lista leads com filtros e paginação.
     *
     * @param status filtro de status (opcional)
     * @param origem filtro de origem (opcional)
     * @param nome filtro de nome (opcional)
     * @param email filtro de email (opcional)
     * @param pageable configuração de paginação
     * @return página de leads
     */
    @Transactional(readOnly = true)
    public Page<LeadResumoDTO> listarLeads(
        StatusLead status,
        String origem,
        String nome,
        String email,
        Pageable pageable
    ) {
        log.debug("Listando leads com filtros - status: {}, origem: {}, nome: {}, email: {}",
            status, origem, nome, email);

        Page<Lead> leads = leadRepository.findWithFilters(status, origem, nome, email, pageable);

        return leads.map(this::toResumoDTO);
    }

    /**
     * Busca lead por ID.
     *
     * @param id identificador do lead
     * @return lead completo
     * @throws ResourceNotFoundException se não encontrado
     */
    @Transactional(readOnly = true)
    public LeadDTO buscarPorId(UUID id) {
        log.debug("Buscando lead: {}", id);

        Lead lead = leadRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado com id: " + id));

        return toDTO(lead);
    }

    /**
     * Atualiza status e observações de um lead.
     *
     * @param id identificador do lead
     * @param request dados para atualização
     * @return lead atualizado
     * @throws ResourceNotFoundException se não encontrado
     */
    @Transactional
    public LeadDTO atualizarLead(UUID id, UpdateLeadRequest request) {
        log.info("Atualizando lead: {} - novo status: {}", id, request.status());

        Lead lead = leadRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado com id: " + id));

        // Atualizar status
        if (request.status() != null && request.status() != lead.getStatus()) {
            StatusLead oldStatus = lead.getStatus();
            lead.setStatus(request.status());
            log.info("Status do lead {} alterado de {} para {}", id, oldStatus, request.status());
        }

        // Adicionar observações
        if (request.observacoes() != null && !request.observacoes().isBlank()) {
            lead.adicionarObservacao(request.observacoes());
        }

        Lead updated = leadRepository.save(lead);
        log.debug("Lead atualizado com sucesso: {}", id);

        return toDTO(updated);
    }

    /**
     * Obtém estatísticas dos leads agrupadas por status.
     *
     * @return estatísticas dos leads
     */
    @Transactional(readOnly = true)
    public LeadStatsDTO getEstatisticas() {
        log.debug("Calculando estatísticas de leads");

        long novos = leadRepository.countByStatus(StatusLead.NOVO);
        long contatados = leadRepository.countByStatus(StatusLead.CONTATADO);
        long qualificados = leadRepository.countByStatus(StatusLead.QUALIFICADO);
        long convertidos = leadRepository.countByStatus(StatusLead.CONVERTIDO);
        long perdidos = leadRepository.countByStatus(StatusLead.PERDIDO);

        long total = novos + contatados + qualificados + convertidos + perdidos;

        return new LeadStatsDTO(novos, contatados, qualificados, convertidos, perdidos, total);
    }

    // =====================================
    // MÉTODOS PRIVADOS
    // =====================================

    /**
     * Envia notificações por email e WhatsApp quando um lead chega.
     * Executa de forma assíncrona para não bloquear a resposta.
     */
    @Async
    private void enviarNotificacoes(Lead lead) {
        try {
            // Preparar mensagem
            String mensagem = String.format(
                """
                🎯 Novo Lead Capturado!

                Nome: %s
                Email: %s
                WhatsApp: %s
                Origem: %s
                Data: %s

                Acesse o painel SaaS para mais detalhes.
                """,
                lead.getNome(),
                lead.getEmail(),
                lead.getWhatsapp(),
                lead.getOrigem(),
                lead.getCreatedAt()
            );

            // Enviar email
            enviarEmail(lead, mensagem);

            // Enviar WhatsApp
            enviarWhatsApp(mensagem);

        } catch (Exception e) {
            log.error("Erro ao enviar notificações para lead {}: {}", lead.getId(), e.getMessage(), e);
            // Não propaga o erro para não afetar a criação do lead
        }
    }

    /**
     * Envia email de notificação de novo lead.
     */
    private void enviarEmail(Lead lead, String mensagem) {
        try {
            NotificacaoRequest request = NotificacaoRequest.email(
                notificationEmail,
                "Novo Lead Capturado - " + lead.getNome(),
                mensagem
            );
            emailService.enviar(request);
            log.info("Email de notificação enviado para: {}", notificationEmail);
        } catch (Exception e) {
            log.error("Erro ao enviar email de notificação: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia WhatsApp de notificação de novo lead.
     */
    private void enviarWhatsApp(String mensagem) {
        try {
            whatsAppService.enviarAsync(
                notificationWhatsApp,
                "Equipe PitStop",
                mensagem,
                EventoNotificacao.OUTROS,
                null,
                null,
                null,
                null
            );
            log.info("WhatsApp de notificação enviado para: {}", notificationWhatsApp);
        } catch (Exception e) {
            log.error("Erro ao enviar WhatsApp de notificação: {}", e.getMessage(), e);
        }
    }

    // =====================================
    // CONVERSÃO DTO
    // =====================================

    private LeadDTO toDTO(Lead lead) {
        return new LeadDTO(
            lead.getId(),
            lead.getNome(),
            lead.getEmail(),
            lead.getWhatsapp(),
            lead.getOrigem(),
            lead.getStatus(),
            lead.getObservacoes(),
            lead.getCreatedAt(),
            lead.getUpdatedAt()
        );
    }

    private LeadResumoDTO toResumoDTO(Lead lead) {
        return new LeadResumoDTO(
            lead.getId(),
            lead.getNome(),
            lead.getEmail(),
            lead.getWhatsapp(),
            lead.getOrigem(),
            lead.getStatus(),
            lead.getCreatedAt()
        );
    }
}
