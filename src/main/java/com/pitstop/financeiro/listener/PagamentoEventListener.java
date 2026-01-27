package com.pitstop.financeiro.listener;

import com.pitstop.financeiro.repository.PagamentoRepository;
import com.pitstop.ordemservico.event.OrdemServicoCanceladaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener de eventos relacionados a Ordem de Serviço.
 * Responsável por cancelar pagamentos pendentes quando uma OS é cancelada.
 *
 * <p><strong>Comportamento:</strong></p>
 * <ul>
 *   <li>Eventos são processados SINCRONAMENTE na mesma transação</li>
 *   <li>Cancela todos os pagamentos PENDENTES ou VENCIDOS da OS cancelada</li>
 *   <li>Evita que pagamentos órfãos apareçam no dashboard de alertas</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2026-01-26
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PagamentoEventListener {

    private final PagamentoRepository pagamentoRepository;

    /**
     * Escuta evento de cancelamento de OS e cancela pagamentos pendentes.
     *
     * <p><strong>Comportamento:</strong></p>
     * <ul>
     *   <li>Roda na MESMA transação que cancelou a OS (síncrono)</li>
     *   <li>Atualiza status de pagamentos PENDENTE/VENCIDO para CANCELADO</li>
     *   <li>Log informativo com quantidade de pagamentos cancelados</li>
     * </ul>
     *
     * @param event evento de OS cancelada
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void handleOrdemServicoCancelada(OrdemServicoCanceladaEvent event) {
        log.info("==> Evento recebido: {} - Verificando pagamentos pendentes para cancelamento", event);

        try {
            int pagamentosCancelados = pagamentoRepository.cancelarPagamentosPendentesPorOS(
                    event.getOrdemServicoId()
            );

            if (pagamentosCancelados > 0) {
                log.info("<== {} pagamento(s) pendente(s) cancelado(s) para OS #{}",
                        pagamentosCancelados, event.getNumeroOS());
            } else {
                log.debug("<== Nenhum pagamento pendente para cancelar na OS #{}", event.getNumeroOS());
            }

        } catch (Exception e) {
            log.error("<== Erro ao cancelar pagamentos pendentes para OS #{}: {}",
                    event.getNumeroOS(), e.getMessage(), e);
            // Re-lança para causar rollback da transação inteira
            throw new RuntimeException("Erro ao cancelar pagamentos pendentes: " + e.getMessage(), e);
        }
    }
}
