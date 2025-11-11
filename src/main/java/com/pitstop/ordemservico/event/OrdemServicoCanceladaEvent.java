package com.pitstop.ordemservico.event;

import com.pitstop.ordemservico.domain.StatusOS;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Evento disparado quando uma Ordem de Serviço é cancelada.
 * Este evento é consumido pelo módulo de estoque para estornar baixas (se OS estava finalizada).
 *
 * <p><strong>Características:</strong></p>
 * <ul>
 *   <li>Evento síncrono - processa na mesma transação</li>
 *   <li>Estorno só ocorre se statusAnterior = FINALIZADO</li>
 *   <li>Se processamento falhar, faz rollback do cancelamento</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Getter
public class OrdemServicoCanceladaEvent extends ApplicationEvent {

    private final UUID ordemServicoId;
    private final Long numeroOS;
    private final UUID usuarioId;
    private final StatusOS statusAnterior;
    private final String motivoCancelamento;

    /**
     * Construtor do evento.
     *
     * @param source objeto que disparou o evento
     * @param ordemServicoId ID da OS cancelada
     * @param numeroOS número sequencial da OS
     * @param usuarioId ID do usuário que cancelou
     * @param statusAnterior status da OS antes do cancelamento
     * @param motivoCancelamento motivo do cancelamento (opcional)
     */
    public OrdemServicoCanceladaEvent(
            Object source,
            UUID ordemServicoId,
            Long numeroOS,
            UUID usuarioId,
            StatusOS statusAnterior,
            String motivoCancelamento
    ) {
        super(source);
        this.ordemServicoId = ordemServicoId;
        this.numeroOS = numeroOS;
        this.usuarioId = usuarioId;
        this.statusAnterior = statusAnterior;
        this.motivoCancelamento = motivoCancelamento;
    }

    /**
     * Verifica se a OS estava finalizada antes de ser cancelada.
     * Usado para determinar se deve estornar estoque.
     *
     * @return true se precisa estornar estoque
     */
    public boolean precisaEstornarEstoque() {
        return statusAnterior == StatusOS.FINALIZADO;
    }

    @Override
    public String toString() {
        return String.format("OrdemServicoCanceladaEvent[osId=%s, numero=%d, statusAnterior=%s]",
                ordemServicoId, numeroOS, statusAnterior);
    }
}
