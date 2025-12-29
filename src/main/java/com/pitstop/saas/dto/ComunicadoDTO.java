package com.pitstop.saas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.saas.domain.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ComunicadoDTO(
    UUID id,
    String titulo,
    String resumo,
    TipoComunicado tipo,
    String tipoDescricao,
    PrioridadeComunicado prioridade,
    String prioridadeDescricao,
    StatusComunicado status,
    String statusDescricao,
    UUID autorId,
    String autorNome,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime dataAgendamento,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime dataEnvio,
    Integer totalDestinatarios,
    Integer totalVisualizacoes,
    Integer totalConfirmacoes,
    Double taxaVisualizacao,
    Double taxaConfirmacao,
    Boolean requerConfirmacao,
    Boolean exibirNoLogin,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime createdAt
) {
    public static ComunicadoDTO fromEntity(Comunicado entity) {
        return new ComunicadoDTO(
            entity.getId(),
            entity.getTitulo(),
            entity.getResumo(),
            entity.getTipo(),
            entity.getTipo().getDescricao(),
            entity.getPrioridade(),
            entity.getPrioridade().getDescricao(),
            entity.getStatus(),
            entity.getStatus().getDescricao(),
            entity.getAutorId(),
            entity.getAutorNome(),
            entity.getDataAgendamento(),
            entity.getDataEnvio(),
            entity.getTotalDestinatarios(),
            entity.getTotalVisualizacoes(),
            entity.getTotalConfirmacoes(),
            entity.getTaxaVisualizacao(),
            entity.getTaxaConfirmacao(),
            entity.getRequerConfirmacao(),
            entity.getExibirNoLogin(),
            entity.getCreatedAt()
        );
    }
}
