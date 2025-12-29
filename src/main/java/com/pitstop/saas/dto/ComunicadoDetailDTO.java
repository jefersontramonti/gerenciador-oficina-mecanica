package com.pitstop.saas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.saas.domain.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ComunicadoDetailDTO(
    UUID id,
    String titulo,
    String resumo,
    String conteudo,
    TipoComunicado tipo,
    String tipoDescricao,
    PrioridadeComunicado prioridade,
    String prioridadeDescricao,
    StatusComunicado status,
    String statusDescricao,
    UUID autorId,
    String autorNome,
    // Segmentação
    String[] planosAlvo,
    UUID[] oficinasAlvo,
    String[] statusOficinasAlvo,
    // Datas
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime dataAgendamento,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime dataEnvio,
    // Estatísticas
    Integer totalDestinatarios,
    Integer totalVisualizacoes,
    Integer totalConfirmacoes,
    Double taxaVisualizacao,
    Double taxaConfirmacao,
    // Configurações
    Boolean requerConfirmacao,
    Boolean exibirNoLogin,
    // Flags de permissão
    Boolean podeEditar,
    Boolean podeEnviar,
    Boolean podeCancelar,
    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime updatedAt,
    // Leituras recentes
    List<LeituraResumoDTO> leiturasRecentes
) {
    public static ComunicadoDetailDTO fromEntity(Comunicado entity, List<LeituraResumoDTO> leituras) {
        return new ComunicadoDetailDTO(
            entity.getId(),
            entity.getTitulo(),
            entity.getResumo(),
            entity.getConteudo(),
            entity.getTipo(),
            entity.getTipo().getDescricao(),
            entity.getPrioridade(),
            entity.getPrioridade().getDescricao(),
            entity.getStatus(),
            entity.getStatus().getDescricao(),
            entity.getAutorId(),
            entity.getAutorNome(),
            entity.getPlanosAlvo(),
            entity.getOficinasAlvo(),
            entity.getStatusOficinasAlvo(),
            entity.getDataAgendamento(),
            entity.getDataEnvio(),
            entity.getTotalDestinatarios(),
            entity.getTotalVisualizacoes(),
            entity.getTotalConfirmacoes(),
            entity.getTaxaVisualizacao(),
            entity.getTaxaConfirmacao(),
            entity.getRequerConfirmacao(),
            entity.getExibirNoLogin(),
            entity.podeEditar(),
            entity.podeEnviar(),
            entity.podeCancelar(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            leituras
        );
    }

    public record LeituraResumoDTO(
        UUID oficinaId,
        String oficinaNome,
        Boolean visualizado,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime dataVisualizacao,
        Boolean confirmado,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime dataConfirmacao
    ) {}
}
