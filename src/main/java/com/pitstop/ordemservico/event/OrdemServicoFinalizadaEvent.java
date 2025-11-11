package com.pitstop.ordemservico.event;

import com.pitstop.ordemservico.domain.ItemOS;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.UUID;

/**
 * Evento disparado quando uma Ordem de Serviço é finalizada.
 * Este evento é consumido pelo módulo de estoque para baixa automática de peças.
 *
 * <p><strong>Características:</strong></p>
 * <ul>
 *   <li>Evento síncrono - processa na mesma transação</li>
 *   <li>Se processamento falhar, faz rollback da finalização da OS</li>
 *   <li>Lista de itens é imutável (cópia defensiva)</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Getter
public class OrdemServicoFinalizadaEvent extends ApplicationEvent {

    private final UUID ordemServicoId;
    private final Long numeroOS;
    private final UUID usuarioId;
    private final List<ItemOS> itens;

    /**
     * Construtor do evento.
     *
     * @param source objeto que disparou o evento
     * @param ordemServicoId ID da OS finalizada
     * @param numeroOS número sequencial da OS
     * @param usuarioId ID do usuário que finalizou
     * @param itens lista de itens da OS (será copiada para imutabilidade)
     */
    public OrdemServicoFinalizadaEvent(
            Object source,
            UUID ordemServicoId,
            Long numeroOS,
            UUID usuarioId,
            List<ItemOS> itens
    ) {
        super(source);
        this.ordemServicoId = ordemServicoId;
        this.numeroOS = numeroOS;
        this.usuarioId = usuarioId;
        this.itens = List.copyOf(itens); // Imutável
    }

    @Override
    public String toString() {
        return String.format("OrdemServicoFinalizadaEvent[osId=%s, numero=%d, itens=%d]",
                ordemServicoId, numeroOS, itens.size());
    }
}
