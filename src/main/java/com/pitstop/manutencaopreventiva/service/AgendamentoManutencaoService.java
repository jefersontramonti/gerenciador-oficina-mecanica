package com.pitstop.manutencaopreventiva.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.manutencaopreventiva.domain.*;
import com.pitstop.manutencaopreventiva.dto.*;
import com.pitstop.manutencaopreventiva.mapper.ManutencaoMapper;
import com.pitstop.manutencaopreventiva.repository.*;
import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import com.pitstop.notificacao.repository.ConfiguracaoNotificacaoRepository;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AgendamentoManutencaoService {

    private final AgendamentoManutencaoRepository agendamentoRepository;
    private final PlanoManutencaoRepository planoRepository;
    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final ManutencaoMapper mapper;
    private final ConfiguracaoNotificacaoRepository configuracaoNotificacaoRepository;
    private final AlertaManutencaoRepository alertaManutencaoRepository;
    private final OficinaRepository oficinaRepository;
    private final AlertaManutencaoService alertaManutencaoService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Lista agendamentos com filtros.
     */
    @Transactional(readOnly = true)
    public Page<AgendamentoManutencaoResponseDTO> listar(
            UUID veiculoId,
            UUID clienteId,
            StatusAgendamento status,
            LocalDate dataInicio,
            LocalDate dataFim,
            Pageable pageable) {

        UUID oficinaId = TenantContext.getTenantId();
        Page<AgendamentoManutencao> agendamentos = agendamentoRepository.findByFilters(
            oficinaId, veiculoId, clienteId, status, dataInicio, dataFim, pageable
        );
        return agendamentos.map(mapper::toAgendamentoResponse);
    }

    /**
     * Busca agendamento por ID.
     */
    @Transactional(readOnly = true)
    public AgendamentoManutencaoResponseDTO buscarPorId(UUID id) {
        AgendamentoManutencao agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamento);
        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Lista agendamentos do dia.
     */
    @Transactional(readOnly = true)
    public List<AgendamentoManutencaoResponseDTO> listarAgendamentosDoDia() {
        UUID oficinaId = TenantContext.getTenantId();
        return agendamentoRepository.findAgendamentosDoDia(oficinaId, LocalDate.now())
            .stream()
            .map(mapper::toAgendamentoResponse)
            .toList();
    }

    /**
     * Lista próximos agendamentos.
     */
    @Transactional(readOnly = true)
    public List<AgendamentoManutencaoResponseDTO> listarProximos(int limite) {
        UUID oficinaId = TenantContext.getTenantId();
        return agendamentoRepository.findProximosAgendamentos(
                oficinaId, LocalDate.now(), PageRequest.of(0, limite))
            .stream()
            .map(mapper::toAgendamentoResponse)
            .toList();
    }

    /**
     * Lista eventos para calendário.
     */
    @Transactional(readOnly = true)
    public List<CalendarioEventoDTO> listarCalendario(int mes, int ano) {
        UUID oficinaId = TenantContext.getTenantId();

        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.plusMonths(1).minusDays(1);

        return agendamentoRepository.findByOficinaIdAndDataAgendamentoBetween(oficinaId, inicio, fim)
            .stream()
            .map(mapper::toCalendarioEvento)
            .toList();
    }

    /**
     * Cria agendamento.
     */
    public AgendamentoManutencaoResponseDTO criar(AgendamentoManutencaoRequestDTO request) {
        UUID oficinaId = TenantContext.getTenantId();

        // Validar conflito de horário
        if (agendamentoRepository.existsConflitoHorario(
                oficinaId, request.dataAgendamento(), request.horaAgendamento(), null)) {
            throw new IllegalStateException("Já existe agendamento para este horário");
        }

        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
            .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado"));

        Cliente cliente = clienteRepository.findById(request.clienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        PlanoManutencaoPreventiva plano = null;
        if (request.planoId() != null) {
            plano = planoRepository.findById(request.planoId())
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));
        }

        Oficina oficina = oficinaRepository.findById(oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        AgendamentoManutencao agendamento = AgendamentoManutencao.builder()
            .oficina(oficina)
            .plano(plano)
            .veiculo(veiculo)
            .cliente(cliente)
            .dataAgendamento(request.dataAgendamento())
            .horaAgendamento(request.horaAgendamento())
            .duracaoEstimadaMinutos(request.duracaoEstimadaMinutos() != null ? request.duracaoEstimadaMinutos() : 60)
            .tipoManutencao(request.tipoManutencao())
            .descricao(request.descricao())
            .observacoes(request.observacoes())
            .observacoesInternas(request.observacoesInternas())
            .build();

        agendamento = agendamentoRepository.save(agendamento);

        log.info("Agendamento criado: {} para {} em {}",
            agendamento.getId(), veiculo.getPlaca(), request.dataAgendamento());

        // Envia notificações solicitando confirmação pelos canais selecionados
        NotificacaoFeedbackDTO feedbackNotificacao = enviarNotificacoesAgendamento(
            agendamento, cliente, veiculo, oficina, request.canaisNotificacao());

        return mapper.toAgendamentoResponse(agendamento, feedbackNotificacao);
    }

    /**
     * Atualiza agendamento.
     */
    public AgendamentoManutencaoResponseDTO atualizar(UUID id, AgendamentoManutencaoRequestDTO request) {
        AgendamentoManutencao agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamento);

        if (agendamento.getStatus() == StatusAgendamento.REALIZADO ||
            agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException("Não é possível editar agendamento " + agendamento.getStatus());
        }

        UUID oficinaId = TenantContext.getTenantId();

        // Validar conflito de horário (excluindo o próprio)
        if (agendamentoRepository.existsConflitoHorario(
                oficinaId, request.dataAgendamento(), request.horaAgendamento(), id)) {
            throw new IllegalStateException("Já existe agendamento para este horário");
        }

        agendamento.setDataAgendamento(request.dataAgendamento());
        agendamento.setHoraAgendamento(request.horaAgendamento());
        agendamento.setDuracaoEstimadaMinutos(request.duracaoEstimadaMinutos());
        agendamento.setTipoManutencao(request.tipoManutencao());
        agendamento.setDescricao(request.descricao());
        agendamento.setObservacoes(request.observacoes());
        agendamento.setObservacoesInternas(request.observacoesInternas());

        agendamento = agendamentoRepository.save(agendamento);

        log.info("Agendamento atualizado: {}", agendamento.getId());
        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Confirma agendamento (por usuário interno).
     */
    public AgendamentoManutencaoResponseDTO confirmar(UUID id) {
        AgendamentoManutencao agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamento);
        agendamento.confirmar("PRESENCIAL");
        agendamento = agendamentoRepository.save(agendamento);

        log.info("Agendamento confirmado: {}", agendamento.getId());
        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Confirma agendamento por token (cliente via link).
     */
    @Transactional
    public AgendamentoManutencaoResponseDTO confirmarPorToken(String token) {
        AgendamentoManutencao agendamento = agendamentoRepository.findByTokenConfirmacao(token)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        if (!agendamento.isTokenValido(token)) {
            throw new IllegalStateException("Token expirado ou inválido");
        }

        agendamento.confirmar("LINK");
        agendamento = agendamentoRepository.save(agendamento);

        log.info("Agendamento confirmado via link: {}", agendamento.getId());
        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Remarca agendamento.
     */
    public AgendamentoManutencaoResponseDTO remarcar(UUID id, RemarcarAgendamentoRequestDTO request) {
        AgendamentoManutencao agendamentoAntigo = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamentoAntigo);

        UUID oficinaId = TenantContext.getTenantId();

        // Validar conflito de horário
        if (agendamentoRepository.existsConflitoHorario(
                oficinaId, request.novaData(), request.novaHora(), null)) {
            throw new IllegalStateException("Já existe agendamento para este horário");
        }

        AgendamentoManutencao novoAgendamento = agendamentoAntigo.remarcar(
            request.novaData(), request.novaHora(), request.motivo()
        );

        agendamentoRepository.save(agendamentoAntigo);
        novoAgendamento = agendamentoRepository.save(novoAgendamento);

        log.info("Agendamento remarcado: {} -> {}", id, novoAgendamento.getId());
        return mapper.toAgendamentoResponse(novoAgendamento);
    }

    /**
     * Cancela agendamento.
     */
    public AgendamentoManutencaoResponseDTO cancelar(UUID id, String motivo) {
        AgendamentoManutencao agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamento);
        agendamento.cancelar(motivo, null);
        agendamento = agendamentoRepository.save(agendamento);

        log.info("Agendamento cancelado: {}", agendamento.getId());
        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Deleta agendamento.
     */
    public void deletar(UUID id) {
        AgendamentoManutencao agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamento);
        agendamentoRepository.delete(agendamento);

        log.info("Agendamento deletado: {}", id);
    }

    /**
     * Conta agendamentos do dia.
     */
    @Transactional(readOnly = true)
    public long contarAgendamentosDoDia() {
        UUID oficinaId = TenantContext.getTenantId();
        return agendamentoRepository.countAgendamentosDoDia(oficinaId, LocalDate.now());
    }

    /**
     * Envia notificações solicitando confirmação do agendamento.
     * O cliente recebe um link para confirmar ou rejeitar.
     *
     * @param agendamento O agendamento criado
     * @param cliente O cliente
     * @param veiculo O veículo
     * @param oficina A oficina
     * @param canaisSelecionados Canais selecionados pelo usuário (null = usa config da oficina)
     * @return Feedback detalhado sobre o status das notificações
     */
    private NotificacaoFeedbackDTO enviarNotificacoesAgendamento(
            AgendamentoManutencao agendamento,
            Cliente cliente,
            Veiculo veiculo,
            Oficina oficina,
            List<String> canaisSelecionados) {

        NotificacaoFeedbackDTO.Builder feedbackBuilder = NotificacaoFeedbackDTO.builder();
        UUID oficinaId = oficina.getId();

        // Busca configuração de notificações da oficina
        ConfiguracaoNotificacao config = configuracaoNotificacaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        if (config == null) {
            log.debug("Configuração de notificação não encontrada para oficina {}", oficinaId);
            return feedbackBuilder
                .notificacoesCriadas(false)
                .mensagemUsuario("Configuração de notificações não encontrada. Configure as notificações em Configurações > Notificações.")
                .build();
        }

        // Verifica se estamos no horário comercial para determinar se o envio será imediato
        boolean envioImediato = config.podeEnviarAgora();
        feedbackBuilder.envioImediato(envioImediato);

        if (!envioImediato) {
            // Determina o motivo do atraso
            String motivoAtraso = determinarMotivoAtraso(config);
            feedbackBuilder.motivoAtraso(motivoAtraso);
            feedbackBuilder.horarioPrevistaEnvio(config.getHorarioInicio());
        }

        // Determina quais canais usar
        Set<String> canaisParaUsar = canaisSelecionados != null && !canaisSelecionados.isEmpty()
            ? Set.copyOf(canaisSelecionados)
            : Set.of(); // Se nenhum canal selecionado, não envia

        // Se a lista de canais selecionados estiver vazia, usa os habilitados na config
        boolean usarWhatsApp = canaisParaUsar.isEmpty()
            ? Boolean.TRUE.equals(config.getWhatsappHabilitado())
            : canaisParaUsar.contains("WHATSAPP");
        boolean usarEmail = canaisParaUsar.isEmpty()
            ? Boolean.TRUE.equals(config.getEmailHabilitado())
            : canaisParaUsar.contains("EMAIL");
        boolean usarTelegram = canaisParaUsar.isEmpty()
            ? Boolean.TRUE.equals(config.getTelegramHabilitado())
            : canaisParaUsar.contains("TELEGRAM");

        String nomeOficina = oficina.getNomeFantasia() != null
            ? oficina.getNomeFantasia()
            : oficina.getRazaoSocial();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String dataFormatada = agendamento.getDataAgendamento().format(dateFormatter);
        String horaFormatada = agendamento.getHoraAgendamento().format(timeFormatter);

        // Link de confirmação
        String linkConfirmacao = frontendUrl + "/agendamento/confirmar?token=" + agendamento.getTokenConfirmacao();

        // Mensagem solicitando confirmação (com link)
        String mensagem = String.format(
            "📅 Novo Agendamento - Confirme sua presença!\n\n" +
            "Olá %s,\n\n" +
            "Um agendamento foi criado para você:\n\n" +
            "📅 Data: %s\n" +
            "🕐 Horário: %s\n" +
            "🔧 Serviço: %s\n" +
            "🚗 Veículo: %s %s (%s)\n\n" +
            "👉 *Confirme sua presença* clicando no link abaixo:\n" +
            "%s\n\n" +
            "Se não puder comparecer, use o mesmo link para cancelar.\n\n" +
            "%s",
            cliente.getNome(),
            dataFormatada,
            horaFormatada,
            agendamento.getTipoManutencao(),
            veiculo.getMarca(),
            veiculo.getModelo(),
            veiculo.getPlacaFormatada(),
            linkConfirmacao,
            nomeOficina
        );

        Map<String, Object> dadosExtras = new HashMap<>();
        dadosExtras.put("nomeCliente", cliente.getNome());
        dadosExtras.put("dataAgendamento", dataFormatada);
        dadosExtras.put("horaAgendamento", horaFormatada);
        dadosExtras.put("tipoManutencao", agendamento.getTipoManutencao());
        dadosExtras.put("veiculoPlaca", veiculo.getPlacaFormatada());
        dadosExtras.put("veiculoModelo", veiculo.getModelo());
        dadosExtras.put("veiculoMarca", veiculo.getMarca());
        dadosExtras.put("nomeOficina", nomeOficina);
        dadosExtras.put("linkConfirmacao", linkConfirmacao);

        int alertasCriados = 0;

        // WhatsApp - usa celular (preferencial) ou telefone como fallback
        if (usarWhatsApp && config.temEvolutionApiConfigurada()) {
            String celular = cliente.getCelular() != null && !cliente.getCelular().isBlank()
                ? cliente.getCelular()
                : cliente.getTelefone();
            if (celular != null && !celular.isBlank()) {
                AlertaManutencao alerta = AlertaManutencao.builder()
                    .oficina(oficina)
                    .agendamento(agendamento)
                    .plano(agendamento.getPlano())
                    .veiculo(veiculo)
                    .cliente(cliente)
                    .tipoAlerta(TipoAlerta.CONFIRMACAO)
                    .canal(CanalNotificacao.WHATSAPP)
                    .destinatario(celular)
                    .titulo("Confirme seu Agendamento")
                    .mensagem(mensagem)
                    .dadosExtras(dadosExtras)
                    .build();

                alerta = alertaManutencaoRepository.save(alerta);
                // Envio síncrono para evitar race condition com transação
                try {
                    alertaManutencaoService.enviarAlerta(alerta);
                } catch (Exception e) {
                    log.warn("Envio imediato WhatsApp falhou para alerta {}, será tentado pelo scheduler: {}",
                        alerta.getId(), e.getMessage());
                }
                alertasCriados++;
                feedbackBuilder.addCanal(new NotificacaoFeedbackDTO.CanalFeedbackDTO(
                    "WhatsApp", true, celular,
                    envioImediato ? "Enviando" : "Agendado",
                    null
                ));
                log.debug("Alerta WhatsApp criado para agendamento {}", agendamento.getId());
            } else {
                feedbackBuilder.addCanal(new NotificacaoFeedbackDTO.CanalFeedbackDTO(
                    "WhatsApp", false, null,
                    "Não criado",
                    "Cliente não possui celular cadastrado"
                ));
                log.warn("Cliente {} não tem celular para WhatsApp", cliente.getId());
            }
        } else if (usarWhatsApp) {
            feedbackBuilder.addCanal(new NotificacaoFeedbackDTO.CanalFeedbackDTO(
                "WhatsApp", false, null,
                "Não configurado",
                "Evolution API não configurada na oficina"
            ));
        }

        // Email
        if (usarEmail) {
            String email = cliente.getEmail();
            if (email != null && !email.isBlank()) {
                AlertaManutencao alerta = AlertaManutencao.builder()
                    .oficina(oficina)
                    .agendamento(agendamento)
                    .plano(agendamento.getPlano())
                    .veiculo(veiculo)
                    .cliente(cliente)
                    .tipoAlerta(TipoAlerta.CONFIRMACAO)
                    .canal(CanalNotificacao.EMAIL)
                    .destinatario(email)
                    .titulo("Confirme seu Agendamento - " + nomeOficina)
                    .mensagem(mensagem)
                    .dadosExtras(dadosExtras)
                    .build();

                alerta = alertaManutencaoRepository.save(alerta);
                // Envio síncrono para evitar race condition com transação
                try {
                    alertaManutencaoService.enviarAlerta(alerta);
                } catch (Exception e) {
                    log.warn("Envio imediato Email falhou para alerta {}, será tentado pelo scheduler: {}",
                        alerta.getId(), e.getMessage());
                }
                alertasCriados++;
                feedbackBuilder.addCanal(new NotificacaoFeedbackDTO.CanalFeedbackDTO(
                    "Email", true, email,
                    envioImediato ? "Enviando" : "Agendado",
                    null
                ));
                log.debug("Alerta Email criado para agendamento {}", agendamento.getId());
            } else {
                feedbackBuilder.addCanal(new NotificacaoFeedbackDTO.CanalFeedbackDTO(
                    "Email", false, null,
                    "Não criado",
                    "Cliente não possui email cadastrado"
                ));
                log.warn("Cliente {} não tem email", cliente.getId());
            }
        }

        // Telegram - envia para o chatId configurado na oficina
        if (usarTelegram && config.temTelegramConfigurado()) {
            String telegramChatId = config.getTelegramChatId();
            if (telegramChatId != null && !telegramChatId.isBlank()) {
                AlertaManutencao alerta = AlertaManutencao.builder()
                    .oficina(oficina)
                    .agendamento(agendamento)
                    .plano(agendamento.getPlano())
                    .veiculo(veiculo)
                    .cliente(cliente)
                    .tipoAlerta(TipoAlerta.CONFIRMACAO)
                    .canal(CanalNotificacao.TELEGRAM)
                    .destinatario(telegramChatId)
                    .titulo("Confirme seu Agendamento")
                    .mensagem(mensagem)
                    .dadosExtras(dadosExtras)
                    .build();

                alerta = alertaManutencaoRepository.save(alerta);
                // Envio síncrono para evitar race condition com transação
                try {
                    alertaManutencaoService.enviarAlerta(alerta);
                } catch (Exception e) {
                    log.warn("Envio imediato Telegram falhou para alerta {}, será tentado pelo scheduler: {}",
                        alerta.getId(), e.getMessage());
                }
                alertasCriados++;
                feedbackBuilder.addCanal(new NotificacaoFeedbackDTO.CanalFeedbackDTO(
                    "Telegram", true, "Chat da oficina",
                    envioImediato ? "Enviando" : "Agendado",
                    null
                ));
                log.debug("Alerta Telegram criado para agendamento {}", agendamento.getId());
            }
        } else if (usarTelegram) {
            feedbackBuilder.addCanal(new NotificacaoFeedbackDTO.CanalFeedbackDTO(
                "Telegram", false, null,
                "Não configurado",
                "Telegram não configurado na oficina"
            ));
        }

        feedbackBuilder
            .notificacoesCriadas(alertasCriados > 0)
            .totalNotificacoes(alertasCriados);

        if (alertasCriados > 0) {
            log.info("Criados {} alertas de confirmação para agendamento {}",
                alertasCriados, agendamento.getId());
        } else {
            log.warn("Nenhum alerta criado para agendamento {} - verifique os canais e dados do cliente",
                agendamento.getId());
        }

        return feedbackBuilder.build();
    }

    /**
     * Determina o motivo pelo qual as notificações não serão enviadas imediatamente.
     */
    private String determinarMotivoAtraso(ConfiguracaoNotificacao config) {
        if (!config.getRespeitarHorarioComercial()) {
            return null;
        }

        java.time.LocalTime agora = java.time.LocalTime.now();
        java.time.DayOfWeek dia = java.time.LocalDate.now().getDayOfWeek();

        if (dia == java.time.DayOfWeek.SUNDAY && !config.getEnviarDomingos()) {
            return "Domingo - envio não habilitado";
        }
        if (dia == java.time.DayOfWeek.SATURDAY && !config.getEnviarSabados()) {
            return "Sábado - envio não habilitado";
        }

        if (agora.isBefore(config.getHorarioInicio())) {
            return "Antes do horário comercial (início às " + config.getHorarioInicio() + ")";
        }
        if (agora.isAfter(config.getHorarioFim())) {
            return "Após o horário comercial (encerrou às " + config.getHorarioFim() + ")";
        }

        return "Fora do horário comercial";
    }

    private void validarTenant(AgendamentoManutencao agendamento) {
        UUID oficinaId = TenantContext.getTenantId();
        if (!agendamento.getOficina().getId().equals(oficinaId)) {
            throw new ResourceNotFoundException("Agendamento não encontrado");
        }
    }
}
