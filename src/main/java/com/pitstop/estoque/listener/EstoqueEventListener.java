package com.pitstop.estoque.listener;

import com.pitstop.estoque.exception.EstoqueInsuficienteException;
import com.pitstop.estoque.service.MovimentacaoEstoqueService;
import com.pitstop.ordemservico.event.OrdemServicoCanceladaEvent;
import com.pitstop.ordemservico.event.OrdemServicoFinalizadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener de eventos relacionados a Ordem de Serviço.
 * Responsável por processar baixa e estorno automático de estoque.
 *
 * <p><strong>Características críticas:</strong></p>
 * <ul>
 *   <li>Eventos são processados SINCRONAMENTE na mesma transação</li>
 *   <li>Se baixa falhar, ROLLBACK completo da OS</li>
 *   <li>Usa @Transactional(propagation = MANDATORY) para garantir transação ativa</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EstoqueEventListener {

    private final MovimentacaoEstoqueService movimentacaoService;

    /**
     * Escuta evento de finalização de OS e baixa estoque automaticamente.
     *
     * <p><strong>Comportamento:</strong></p>
     * <ul>
     *   <li>Roda na MESMA transação que finalizou a OS (síncrono)</li>
     *   <li>Se estoque insuficiente, lança exception → rollback da OS</li>
     *   <li>Processa apenas itens do tipo PECA com pecaId</li>
     * </ul>
     *
     * @param event evento de OS finalizada
     * @throws EstoqueInsuficienteException se alguma peça não tem estoque
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY) // Requer transação ativa do caller
    public void handleOrdemServicoFinalizada(OrdemServicoFinalizadaEvent event) {
        log.info("==> Evento recebido: {} - Processando baixa de estoque", event);

        try {
            movimentacaoService.baixarEstoquePorOS(
                    event.getOrdemServicoId(),
                    event.getItens(),
                    event.getUsuarioId()
            );

            log.info("<== Baixa de estoque concluída com sucesso para OS #{}", event.getNumeroOS());

        } catch (EstoqueInsuficienteException e) {
            log.error("<== Falha na baixa de estoque para OS #{}: {}",
                    event.getNumeroOS(), e.getMessage());
            // Re-lança exception para causar rollback da transação inteira
            throw e;

        } catch (Exception e) {
            log.error("<== Erro inesperado ao processar baixa de estoque para OS #{}: {}",
                    event.getNumeroOS(), e.getMessage(), e);
            throw new RuntimeException("Erro ao processar baixa de estoque: " + e.getMessage(), e);
        }
    }

    /**
     * Escuta evento de cancelamento de OS e estorna estoque se necessário.
     *
     * <p><strong>Comportamento:</strong></p>
     * <ul>
     *   <li>Roda na MESMA transação que cancelou a OS (síncrono)</li>
     *   <li>Só estorna se OS estava FINALIZADA antes do cancelamento</li>
     *   <li>Cria movimentações do tipo DEVOLUCAO</li>
     * </ul>
     *
     * @param event evento de OS cancelada
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void handleOrdemServicoCancelada(OrdemServicoCanceladaEvent event) {
        log.info("==> Evento recebido: {}", event);

        // Só estorna se OS estava finalizada antes de cancelar
        if (!event.precisaEstornarEstoque()) {
            log.info("<== OS #{} não estava finalizada (status: {}), estorno de estoque não necessário",
                    event.getNumeroOS(), event.getStatusAnterior());
            return;
        }

        try {
            log.info("OS #{} estava FINALIZADA - Estornando estoque", event.getNumeroOS());

            movimentacaoService.estornarEstoquePorOS(
                    event.getOrdemServicoId(),
                    event.getUsuarioId()
            );

            log.info("<== Estorno de estoque concluído com sucesso para OS #{}", event.getNumeroOS());

        } catch (Exception e) {
            log.error("<== Erro ao estornar estoque para OS #{}: {}",
                    event.getNumeroOS(), e.getMessage(), e);
            // Re-lança para causar rollback
            throw new RuntimeException("Erro ao estornar estoque: " + e.getMessage(), e);
        }
    }
}
