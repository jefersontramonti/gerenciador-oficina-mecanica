package com.pitstop.ordemservico.dto;

import com.pitstop.ordemservico.domain.HistoricoStatusOS;
import com.pitstop.ordemservico.domain.StatusOS;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para resposta de histórico de status de OS.
 */
@Schema(description = "Histórico de mudança de status da Ordem de Serviço")
public record HistoricoStatusOSDTO(
    @Schema(description = "ID do registro de histórico")
    UUID id,

    @Schema(description = "Status anterior (null se for criação)")
    String statusAnterior,

    @Schema(description = "Label amigável do status anterior")
    String statusAnteriorLabel,

    @Schema(description = "Novo status")
    String statusNovo,

    @Schema(description = "Label amigável do novo status")
    String statusNovoLabel,

    @Schema(description = "ID do usuário que fez a alteração")
    UUID usuarioId,

    @Schema(description = "Nome do usuário que fez a alteração")
    String usuarioNome,

    @Schema(description = "Observação sobre a mudança")
    String observacao,

    @Schema(description = "Data/hora da alteração")
    LocalDateTime dataAlteracao
) {
    /**
     * Converte entidade para DTO.
     */
    public static HistoricoStatusOSDTO fromEntity(HistoricoStatusOS entity) {
        return new HistoricoStatusOSDTO(
            entity.getId(),
            entity.getStatusAnterior() != null ? entity.getStatusAnterior().name() : null,
            entity.getStatusAnterior() != null ? getStatusLabel(entity.getStatusAnterior()) : null,
            entity.getStatusNovo().name(),
            getStatusLabel(entity.getStatusNovo()),
            entity.getUsuarioId(),
            entity.getUsuarioNome(),
            entity.getObservacao(),
            entity.getDataAlteracao()
        );
    }

    /**
     * Retorna label amigável para o status.
     */
    private static String getStatusLabel(StatusOS status) {
        return switch (status) {
            case ORCAMENTO -> "Orçamento";
            case APROVADO -> "Aprovado";
            case EM_ANDAMENTO -> "Em Andamento";
            case AGUARDANDO_PECA -> "Aguardando Peça";
            case FINALIZADO -> "Finalizado";
            case ENTREGUE -> "Entregue";
            case CANCELADO -> "Cancelado";
        };
    }
}
