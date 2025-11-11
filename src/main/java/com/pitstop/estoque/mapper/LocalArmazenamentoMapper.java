package com.pitstop.estoque.mapper;

import com.pitstop.estoque.domain.LocalArmazenamento;
import com.pitstop.estoque.dto.CreateLocalArmazenamentoDTO;
import com.pitstop.estoque.dto.LocalArmazenamentoResponseDTO;
import com.pitstop.estoque.dto.LocalArmazenamentoSimplificadoDTO;
import com.pitstop.estoque.dto.UpdateLocalArmazenamentoDTO;
import org.mapstruct.*;

/**
 * MapStruct mapper para conversão entre entidade LocalArmazenamento e DTOs.
 * Configurado para evitar recursão infinita na hierarquia pai-filho.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LocalArmazenamentoMapper {

    /**
     * Converte CreateDTO para entidade.
     * O campo localizacaoPai será setado manualmente no service (através do localizacaoPaiId).
     *
     * @param dto DTO de criação
     * @return entidade LocalArmazenamento
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "localizacaoPai", ignore = true)
    @Mapping(target = "locaisFilhos", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    LocalArmazenamento toEntity(CreateLocalArmazenamentoDTO dto);

    /**
     * Converte UpdateDTO para entidade.
     * O campo localizacaoPai será setado manualmente no service (através do localizacaoPaiId).
     *
     * @param dto DTO de atualização
     * @return entidade LocalArmazenamento
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "localizacaoPai", ignore = true)
    @Mapping(target = "locaisFilhos", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    LocalArmazenamento toEntity(UpdateLocalArmazenamentoDTO dto);

    /**
     * Converte entidade para DTO de resposta completo.
     * Usa método customizado para evitar recursão no campo localizacaoPai.
     *
     * @param entity entidade LocalArmazenamento
     * @return DTO de resposta completo
     */
    @Mapping(target = "localizacaoPai", source = "localizacaoPai", qualifiedByName = "toSimplificadoDTO")
    @Mapping(target = "caminhoCompleto", expression = "java(entity.getCaminhoCompleto())")
    @Mapping(target = "nivel", expression = "java(entity.getNivel())")
    @Mapping(target = "isRaiz", expression = "java(entity.isRaiz())")
    @Mapping(target = "temFilhos", expression = "java(entity.temFilhos())")
    LocalArmazenamentoResponseDTO toResponseDTO(LocalArmazenamento entity);

    /**
     * Converte entidade para DTO simplificado.
     * Não inclui o pai para evitar recursão.
     *
     * @param entity entidade LocalArmazenamento
     * @return DTO simplificado
     */
    @Named("toSimplificadoDTO")
    @Mapping(target = "caminhoCompleto", expression = "java(entity.getCaminhoCompleto())")
    LocalArmazenamentoSimplificadoDTO toSimplificadoDTO(LocalArmazenamento entity);

    /**
     * Atualiza entidade existente com dados do UpdateDTO.
     * Não atualiza campos de auditoria nem a hierarquia (localizacaoPai setado manualmente).
     *
     * @param dto DTO de atualização
     * @param entity entidade existente
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "localizacaoPai", ignore = true)
    @Mapping(target = "locaisFilhos", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(UpdateLocalArmazenamentoDTO dto, @MappingTarget LocalArmazenamento entity);
}
