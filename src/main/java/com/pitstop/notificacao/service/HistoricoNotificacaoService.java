package com.pitstop.notificacao.service;

import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.HistoricoNotificacao;
import com.pitstop.notificacao.domain.StatusNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.dto.HistoricoNotificacaoDTO;
import com.pitstop.notificacao.dto.NotificacaoMetricasDTO;
import com.pitstop.notificacao.repository.HistoricoNotificacaoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servico para consulta de historico de notificacoes.
 *
 * @author PitStop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricoNotificacaoService {

    private final HistoricoNotificacaoRepository repository;

    /**
     * Lista historico da oficina atual com paginacao.
     *
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    @Transactional(readOnly = true)
    public Page<HistoricoNotificacaoDTO.Resumido> listar(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return repository.findByOficinaIdOrderByCreatedAtDesc(oficinaId, pageable)
            .map(HistoricoNotificacaoDTO.Resumido::fromEntity);
    }

    /**
     * Lista historico com filtros combinados.
     *
     * @param tipo Canal (opcional)
     * @param status Status (opcional)
     * @param evento Evento (opcional)
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    @Transactional(readOnly = true)
    public Page<HistoricoNotificacaoDTO.Resumido> listarComFiltros(
            TipoNotificacao tipo,
            StatusNotificacao status,
            EventoNotificacao evento,
            Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return repository.findWithFilters(oficinaId, tipo, status, evento, pageable)
            .map(HistoricoNotificacaoDTO.Resumido::fromEntity);
    }

    /**
     * Lista historico filtrado por status.
     *
     * @param status Status desejado
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    @Transactional(readOnly = true)
    public Page<HistoricoNotificacaoDTO.Resumido> listarPorStatus(StatusNotificacao status, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return repository.findByOficinaIdAndStatusOrderByCreatedAtDesc(oficinaId, status, pageable)
            .map(HistoricoNotificacaoDTO.Resumido::fromEntity);
    }

    /**
     * Lista historico filtrado por evento.
     *
     * @param evento Evento
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    @Transactional(readOnly = true)
    public Page<HistoricoNotificacaoDTO.Resumido> listarPorEvento(EventoNotificacao evento, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return repository.findByOficinaIdAndEventoOrderByCreatedAtDesc(oficinaId, evento, pageable)
            .map(HistoricoNotificacaoDTO.Resumido::fromEntity);
    }

    /**
     * Lista historico filtrado por canal.
     *
     * @param canal Canal
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    @Transactional(readOnly = true)
    public Page<HistoricoNotificacaoDTO.Resumido> listarPorCanal(TipoNotificacao canal, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return repository.findByOficinaIdAndTipoNotificacaoOrderByCreatedAtDesc(oficinaId, canal, pageable)
            .map(HistoricoNotificacaoDTO.Resumido::fromEntity);
    }

    /**
     * Obtem detalhes de uma notificacao.
     *
     * @param id ID da notificacao
     * @return Detalhes da notificacao
     */
    @Transactional(readOnly = true)
    public HistoricoNotificacaoDTO buscarPorId(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        HistoricoNotificacao historico = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Notificacao nao encontrada"));

        // Verifica se pertence a oficina
        if (!historico.getOficinaId().equals(oficinaId)) {
            throw new SecurityException("Acesso negado a esta notificacao");
        }

        return HistoricoNotificacaoDTO.fromEntity(historico);
    }

    /**
     * Lista notificacoes de uma OS especifica.
     *
     * @param ordemServicoId ID da OS
     * @return Lista de notificacoes
     */
    @Transactional(readOnly = true)
    public List<HistoricoNotificacaoDTO.Resumido> listarPorOrdemServico(UUID ordemServicoId) {
        return repository.findByOrdemServicoIdOrderByCreatedAtDesc(ordemServicoId)
            .stream()
            .map(HistoricoNotificacaoDTO.Resumido::fromEntity)
            .toList();
    }

    /**
     * Lista notificacoes de um cliente.
     *
     * @param clienteId ID do cliente
     * @param pageable Paginacao
     * @return Pagina de notificacoes
     */
    @Transactional(readOnly = true)
    public Page<HistoricoNotificacaoDTO.Resumido> listarPorCliente(UUID clienteId, Pageable pageable) {
        return repository.findByClienteIdOrderByCreatedAtDesc(clienteId, pageable)
            .map(HistoricoNotificacaoDTO.Resumido::fromEntity);
    }

    /**
     * Obtem metricas de notificacoes da oficina atual.
     *
     * @param dataInicio Data inicio do periodo
     * @param dataFim Data fim do periodo
     * @return Metricas
     */
    @Transactional(readOnly = true)
    public NotificacaoMetricasDTO getMetricas(LocalDate dataInicio, LocalDate dataFim) {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(LocalTime.MAX);

        // Contagens por status
        long enviadas = repository.countByOficinaAndStatusAndPeriodo(oficinaId, StatusNotificacao.ENVIADO, inicio, fim);
        long entregues = repository.countByOficinaAndStatusAndPeriodo(oficinaId, StatusNotificacao.ENTREGUE, inicio, fim);
        long lidas = repository.countByOficinaAndStatusAndPeriodo(oficinaId, StatusNotificacao.LIDO, inicio, fim);
        long falhas = repository.countByOficinaAndStatusAndPeriodo(oficinaId, StatusNotificacao.FALHA, inicio, fim);
        long pendentes = repository.countByOficinaAndStatusAndPeriodo(oficinaId, StatusNotificacao.PENDENTE, inicio, fim);

        long totalEnviadas = enviadas + entregues + lidas;

        // Contagens por canal
        Map<String, Long> enviadasPorCanal = new HashMap<>();
        Map<String, Long> falhasPorCanal = new HashMap<>();
        for (TipoNotificacao canal : TipoNotificacao.values()) {
            long count = repository.countByOficinaAndCanalAndPeriodo(oficinaId, canal, inicio, fim);
            if (count > 0) {
                enviadasPorCanal.put(canal.name(), count);
            }
        }

        // Contagens por evento
        Map<String, Long> enviadasPorEvento = new HashMap<>();
        for (EventoNotificacao evento : EventoNotificacao.values()) {
            long count = repository.countByOficinaAndEventoAndPeriodo(oficinaId, evento, inicio, fim);
            if (count > 0) {
                enviadasPorEvento.put(evento.name(), count);
            }
        }

        // Taxas
        double taxaEntrega = NotificacaoMetricasDTO.calcularTaxaEntrega(entregues + lidas, totalEnviadas);
        double taxaLeitura = NotificacaoMetricasDTO.calcularTaxaLeitura(lidas, entregues + lidas);
        double taxaFalha = NotificacaoMetricasDTO.calcularTaxaFalha(falhas, totalEnviadas + falhas);

        return NotificacaoMetricasDTO.builder()
            .oficinaId(oficinaId)
            .dataInicio(dataInicio)
            .dataFim(dataFim)
            .totalEnviadas(totalEnviadas)
            .totalEntregues(entregues)
            .totalLidas(lidas)
            .totalFalhas(falhas)
            .totalPendentes(pendentes)
            .enviadasPorCanal(enviadasPorCanal)
            .falhasPorCanal(falhasPorCanal)
            .enviadasPorEvento(enviadasPorEvento)
            .taxaEntrega(taxaEntrega)
            .taxaLeitura(taxaLeitura)
            .taxaFalha(taxaFalha)
            .build();
    }

    /**
     * Conta total de notificacoes da oficina.
     *
     * @return Total
     */
    @Transactional(readOnly = true)
    public long contarTotal() {
        UUID oficinaId = TenantContext.getTenantId();
        return repository.countByOficinaId(oficinaId);
    }

    /**
     * Conta notificacoes enviadas com sucesso.
     *
     * @return Total enviadas
     */
    @Transactional(readOnly = true)
    public long contarEnviadas() {
        UUID oficinaId = TenantContext.getTenantId();
        return repository.countEnviadasByOficina(oficinaId);
    }

    /**
     * Conta notificacoes que falharam.
     *
     * @return Total falhas
     */
    @Transactional(readOnly = true)
    public long contarFalhas() {
        UUID oficinaId = TenantContext.getTenantId();
        return repository.countByOficinaIdAndStatus(oficinaId, StatusNotificacao.FALHA);
    }

    /**
     * Lista notificacoes pendentes de reenvio.
     *
     * @param maxTentativas Maximo de tentativas
     * @return Lista para reenvio
     */
    @Transactional(readOnly = true)
    public List<HistoricoNotificacao> listarParaReenvio(int maxTentativas) {
        UUID oficinaId = TenantContext.getTenantId();
        return repository.findParaReenvio(oficinaId, maxTentativas);
    }

    /**
     * Atualiza status de uma notificacao pelo ID externo (callback da API).
     *
     * @param idExterno Message ID da API
     * @param novoStatus Novo status
     */
    @Transactional
    public void atualizarStatusPorIdExterno(String idExterno, StatusNotificacao novoStatus) {
        repository.findByIdExterno(idExterno).ifPresent(historico -> {
            switch (novoStatus) {
                case ENTREGUE -> historico.marcarComoEntregue();
                case LIDO -> historico.marcarComoLido();
                default -> historico.setStatus(novoStatus);
            }
            repository.save(historico);
            log.debug("Status atualizado para {} (idExterno: {})", novoStatus, idExterno);
        });
    }

    /**
     * Limpa historico antigo.
     *
     * @param diasRetencao Dias para manter
     * @return Quantidade removida
     */
    @Transactional
    public int limparHistoricoAntigo(int diasRetencao) {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(diasRetencao);
        int removidos = repository.deleteHistoricoAntigo(oficinaId, dataLimite);
        log.info("Removidos {} registros de historico antigos da oficina {}", removidos, oficinaId);
        return removidos;
    }
}
