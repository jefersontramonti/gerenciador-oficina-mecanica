package com.pitstop.ia.mapper;

import com.pitstop.ia.domain.ConfiguracaoIA;
import com.pitstop.ia.dto.ConfiguracaoIAResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para ConfiguracaoIA.
 */
@Mapper(componentModel = "spring")
public interface ConfiguracaoIAMapper {

    @Mapping(target = "oficinaId", source = "oficina.id")
    @Mapping(target = "apiKeyConfigurada", expression = "java(entity.getApiKeyEncrypted() != null && !entity.getApiKeyEncrypted().isBlank())")
    @Mapping(target = "requisicoesRestantes", expression = "java(entity.getMaxRequisicoesDia() - entity.getRequisicoesHoje())")
    @Mapping(target = "estatisticas", expression = "java(mapEstatisticas(entity))")
    ConfiguracaoIAResponse toResponse(ConfiguracaoIA entity);

    default ConfiguracaoIAResponse.EstatisticasIA mapEstatisticas(ConfiguracaoIA entity) {
        double taxaCache = entity.getTotalRequisicoes() > 0
                ? (double) entity.getTotalCacheHits() / entity.getTotalRequisicoes() * 100
                : 0.0;

        double taxaTemplate = entity.getTotalRequisicoes() > 0
                ? (double) entity.getTotalTemplateHits() / entity.getTotalRequisicoes() * 100
                : 0.0;

        return new ConfiguracaoIAResponse.EstatisticasIA(
                entity.getTotalRequisicoes(),
                entity.getTotalTokensConsumidos(),
                entity.getTotalCacheHits(),
                entity.getTotalTemplateHits(),
                entity.getCustoEstimadoTotal(),
                taxaCache,
                taxaTemplate
        );
    }
}
