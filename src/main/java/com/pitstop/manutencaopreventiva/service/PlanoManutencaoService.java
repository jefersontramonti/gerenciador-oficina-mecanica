package com.pitstop.manutencaopreventiva.service;

import com.pitstop.manutencaopreventiva.domain.*;
import com.pitstop.manutencaopreventiva.dto.*;
import com.pitstop.manutencaopreventiva.mapper.ManutencaoMapper;
import com.pitstop.manutencaopreventiva.repository.*;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlanoManutencaoService {

    private final PlanoManutencaoRepository planoRepository;
    private final TemplateManutencaoRepository templateRepository;
    private final HistoricoManutencaoRepository historicoRepository;
    private final AlertaManutencaoRepository alertaManutencaoRepository;
    private final VeiculoRepository veiculoRepository;
    private final ManutencaoMapper mapper;
    private final OrdemServicoRepository ordemServicoRepository;

    /**
     * Lista planos com filtros.
     */
    @Transactional(readOnly = true)
    public Page<PlanoManutencaoResponseDTO> listar(
            UUID veiculoId,
            StatusPlanoManutencao status,
            String tipoManutencao,
            String busca,
            Pageable pageable) {

        UUID oficinaId = TenantContext.getTenantId();
        Page<PlanoManutencaoPreventiva> planos = planoRepository.findByFilters(
            oficinaId, veiculoId, status, tipoManutencao, busca, pageable
        );
        return planos.map(mapper::toPlanoResponse);
    }

    /**
     * Busca plano por ID.
     */
    @Transactional(readOnly = true)
    public PlanoManutencaoResponseDTO buscarPorId(UUID id) {
        PlanoManutencaoPreventiva plano = planoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));

        validarTenant(plano);
        return mapper.toPlanoResponse(plano);
    }

    /**
     * Lista planos de um veículo.
     */
    @Transactional(readOnly = true)
    public List<PlanoManutencaoResponseDTO> listarPorVeiculo(UUID veiculoId) {
        return planoRepository.findByVeiculoIdAndAtivoTrue(veiculoId)
            .stream()
            .map(mapper::toPlanoResponse)
            .toList();
    }

    /**
     * Lista planos vencidos.
     */
    @Transactional(readOnly = true)
    public List<PlanoManutencaoResponseDTO> listarVencidos() {
        UUID oficinaId = TenantContext.getTenantId();
        return planoRepository.findPlanosVencidosPorData(oficinaId, LocalDate.now())
            .stream()
            .map(mapper::toPlanoResponse)
            .toList();
    }

    /**
     * Lista planos próximos a vencer.
     */
    @Transactional(readOnly = true)
    public List<PlanoManutencaoResponseDTO> listarProximosAVencer(int dias) {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDate hoje = LocalDate.now();
        LocalDate dataLimite = hoje.plusDays(dias);

        return planoRepository.findPlanosProximosAVencerPorData(oficinaId, hoje, dataLimite)
            .stream()
            .map(mapper::toPlanoResponse)
            .toList();
    }

    /**
     * Cria novo plano.
     */
    public PlanoManutencaoResponseDTO criar(PlanoManutencaoRequestDTO request) {
        UUID oficinaId = TenantContext.getTenantId();

        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
            .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado"));

        TemplateManutencao template = null;
        if (request.templateId() != null) {
            template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template não encontrado"));
        }

        Oficina oficina = new Oficina();
        oficina.setId(oficinaId);

        PlanoManutencaoPreventiva plano = PlanoManutencaoPreventiva.builder()
            .oficina(oficina)
            .veiculo(veiculo)
            .template(template)
            .nome(request.nome())
            .descricao(request.descricao())
            .tipoManutencao(request.tipoManutencao())
            .criterio(request.criterio())
            .intervaloDias(request.intervaloDias())
            .intervaloKm(request.intervaloKm())
            .antecedenciaDias(request.antecedenciaDias() != null ? request.antecedenciaDias() : 15)
            .antecedenciaKm(request.antecedenciaKm() != null ? request.antecedenciaKm() : 1000)
            .canaisNotificacao(request.canaisNotificacao())
            .ultimaExecucaoData(request.ultimaExecucaoData())
            .ultimaExecucaoKm(request.ultimaExecucaoKm())
            .checklist(request.checklist())
            .pecasSugeridas(request.pecasSugeridas())
            .valorEstimado(request.valorEstimado())
            .agendamentosNotificacao(request.agendamentosNotificacao())
            .status(StatusPlanoManutencao.ATIVO)
            .build();

        plano.calcularProximaManutencao();
        plano = planoRepository.save(plano);

        log.info("Plano de manutenção criado: {} para veículo {}", plano.getId(), veiculo.getPlaca());
        return mapper.toPlanoResponse(plano);
    }

    /**
     * Cria plano a partir de template.
     */
    public PlanoManutencaoResponseDTO criarAPartirDeTemplate(UUID templateId, AplicarTemplateRequestDTO request) {
        TemplateManutencao template = templateRepository.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template não encontrado"));

        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
            .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado"));

        UUID oficinaId = TenantContext.getTenantId();
        Oficina oficina = new Oficina();
        oficina.setId(oficinaId);

        PlanoManutencaoPreventiva plano = PlanoManutencaoPreventiva.builder()
            .oficina(oficina)
            .veiculo(veiculo)
            .template(template)
            .nome(template.getNome())
            .descricao(template.getDescricao())
            .tipoManutencao(template.getTipoManutencao())
            .criterio(template.getCriterio())
            .intervaloDias(template.getIntervaloDias())
            .intervaloKm(template.getIntervaloKm())
            .antecedenciaDias(template.getAntecedenciaDias())
            .antecedenciaKm(template.getAntecedenciaKm())
            .ultimaExecucaoData(request.ultimaExecucaoData())
            .ultimaExecucaoKm(request.ultimaExecucaoKm())
            .checklist(template.getChecklist())
            .pecasSugeridas(template.getPecasSugeridas())
            .valorEstimado(template.getValorEstimado())
            .status(StatusPlanoManutencao.ATIVO)
            .build();

        plano.calcularProximaManutencao();
        plano = planoRepository.save(plano);

        log.info("Plano criado a partir do template {}: {} para veículo {}",
            template.getNome(), plano.getId(), veiculo.getPlaca());
        return mapper.toPlanoResponse(plano);
    }

    /**
     * Atualiza plano.
     */
    public PlanoManutencaoResponseDTO atualizar(UUID id, PlanoManutencaoRequestDTO request) {
        PlanoManutencaoPreventiva plano = planoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));

        validarTenant(plano);

        plano.setNome(request.nome());
        plano.setDescricao(request.descricao());
        plano.setTipoManutencao(request.tipoManutencao());
        plano.setCriterio(request.criterio());
        plano.setIntervaloDias(request.intervaloDias());
        plano.setIntervaloKm(request.intervaloKm());
        plano.setAntecedenciaDias(request.antecedenciaDias());
        plano.setAntecedenciaKm(request.antecedenciaKm());
        plano.setCanaisNotificacao(request.canaisNotificacao());
        plano.setChecklist(request.checklist());
        plano.setPecasSugeridas(request.pecasSugeridas());
        plano.setValorEstimado(request.valorEstimado());
        plano.setAgendamentosNotificacao(request.agendamentosNotificacao());

        plano.calcularProximaManutencao();
        plano = planoRepository.save(plano);

        log.info("Plano de manutenção atualizado: {}", plano.getId());
        return mapper.toPlanoResponse(plano);
    }

    /**
     * Ativa plano.
     */
    public PlanoManutencaoResponseDTO ativar(UUID id) {
        PlanoManutencaoPreventiva plano = planoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));

        validarTenant(plano);
        plano.ativar();
        plano = planoRepository.save(plano);

        log.info("Plano de manutenção ativado: {}", plano.getId());
        return mapper.toPlanoResponse(plano);
    }

    /**
     * Pausa plano.
     */
    public PlanoManutencaoResponseDTO pausar(UUID id, String motivo) {
        PlanoManutencaoPreventiva plano = planoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));

        validarTenant(plano);
        plano.pausar(motivo);
        plano = planoRepository.save(plano);

        // Cancela alertas pendentes associados ao plano pausado
        alertaManutencaoRepository.cancelarAlertasPendentes(id);

        log.info("Plano de manutenção pausado: {} - alertas pendentes cancelados", plano.getId());
        return mapper.toPlanoResponse(plano);
    }

    /**
     * Conclui plano.
     */
    public PlanoManutencaoResponseDTO concluir(UUID id) {
        PlanoManutencaoPreventiva plano = planoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));

        validarTenant(plano);
        plano.concluir();
        plano = planoRepository.save(plano);

        // Cancela alertas pendentes associados ao plano concluído
        alertaManutencaoRepository.cancelarAlertasPendentes(id);

        log.info("Plano de manutenção concluído: {} - alertas pendentes cancelados", plano.getId());
        return mapper.toPlanoResponse(plano);
    }

    /**
     * Registra execução de manutenção.
     * A OS já foi criada automaticamente pelo scheduler quando a manutenção ficou próxima/vencida.
     * Este método apenas registra que a manutenção foi realizada.
     */
    public PlanoManutencaoResponseDTO executar(UUID id, ExecutarPlanoRequestDTO request) {
        PlanoManutencaoPreventiva plano = planoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));

        validarTenant(plano);

        // Busca OS existente se informada
        OrdemServico ordemServico = null;
        if (request.ordemServicoId() != null) {
            ordemServico = ordemServicoRepository.findById(request.ordemServicoId()).orElse(null);
        }

        // Registra execução no plano
        plano.registrarExecucao(request.dataExecucao(), request.kmExecucao(), ordemServico);

        // Cria histórico
        HistoricoManutencaoPreventiva historico = HistoricoManutencaoPreventiva.builder()
            .plano(plano)
            .veiculo(plano.getVeiculo())
            .dataExecucao(request.dataExecucao())
            .kmExecucao(request.kmExecucao())
            .tipoManutencao(plano.getTipoManutencao())
            .checklistExecutado(request.checklistExecutado())
            .pecasUtilizadas(request.pecasUtilizadas())
            .valorMaoObra(request.valorMaoObra())
            .valorPecas(request.valorPecas())
            .observacoes(request.observacoes())
            .observacoesMecanico(request.observacoesMecanico())
            .proximaPrevisaoData(plano.getProximaPrevisaoData())
            .proximaPrevisaoKm(plano.getProximaPrevisaoKm())
            .ordemServico(ordemServico)
            .build();

        historicoRepository.save(historico);
        plano = planoRepository.save(plano);

        // Cancela alertas pendentes pois a manutenção foi executada
        alertaManutencaoRepository.cancelarAlertasPendentes(id);

        log.info("Execução registrada para plano: {} - alertas pendentes cancelados", plano.getId());
        return mapper.toPlanoResponse(plano);
    }

    /**
     * Soft delete do plano.
     */
    public void deletar(UUID id) {
        PlanoManutencaoPreventiva plano = planoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));

        validarTenant(plano);
        plano.setAtivo(false);
        planoRepository.save(plano);

        // Cancela alertas pendentes associados ao plano deletado
        alertaManutencaoRepository.cancelarAlertasPendentes(id);

        log.info("Plano de manutenção deletado (soft): {} - alertas pendentes cancelados", id);
    }

    private void validarTenant(PlanoManutencaoPreventiva plano) {
        UUID oficinaId = TenantContext.getTenantId();
        if (!plano.getOficina().getId().equals(oficinaId)) {
            throw new ResourceNotFoundException("Plano não encontrado");
        }
    }
}
