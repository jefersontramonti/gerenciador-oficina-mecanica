package com.pitstop.saas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.saas.domain.FeatureFlag;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record FeatureFlagDTO(
    UUID id,
    String codigo,
    String nome,
    String descricao,
    Boolean habilitadoGlobal,
    Map<String, Boolean> habilitadoPorPlano,
    List<UUID> habilitadoPorOficina,
    Integer percentualRollout,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime dataInicio,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime dataFim,
    String categoria,
    Boolean requerAutorizacao,
    Boolean ativo,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime updatedAt
) {
    public static FeatureFlagDTO fromEntity(FeatureFlag entity) {
        return new FeatureFlagDTO(
            entity.getId(),
            entity.getCodigo(),
            entity.getNome(),
            entity.getDescricao(),
            entity.getHabilitadoGlobal(),
            entity.getHabilitadoPorPlano(),
            entity.getHabilitadoPorOficina() != null
                ? Arrays.asList(entity.getHabilitadoPorOficina())
                : List.of(),
            entity.getPercentualRollout(),
            entity.getDataInicio(),
            entity.getDataFim(),
            entity.getCategoria(),
            entity.getRequerAutorizacao(),
            entity.isAtivo(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
