package com.pitstop.estoque.mapper;

import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.dto.CreatePecaDTO;
import com.pitstop.estoque.dto.PecaResponseDTO;
import com.pitstop.estoque.dto.UpdatePecaDTO;
import org.mapstruct.*;

/**
 * MapStruct mapper para conversão entre Peca entity e DTOs.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Mapper(componentModel = "spring", uses = {LocalArmazenamentoMapper.class})
public interface PecaMapper {

    /**
     * Converte CreatePecaDTO para entidade Peca.
     *
     * @param dto DTO de criação
     * @return entidade Peca
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "localArmazenamento", ignore = true) // Gerenciado separadamente
    @Mapping(target = "quantidadeAtual", constant = "0") // Estoque inicial é 0
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Peca toEntity(CreatePecaDTO dto);

    /**
     * Converte UpdatePecaDTO para entidade Peca.
     * Usado para criar objeto com dados de atualização.
     *
     * @param dto DTO de atualização
     * @return entidade Peca
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "localArmazenamento", ignore = true) // Gerenciado separadamente
    @Mapping(target = "quantidadeAtual", ignore = true) // NÃO atualiza estoque por este DTO
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Peca toEntity(UpdatePecaDTO dto);

    /**
     * Converte entidade Peca para PecaResponseDTO.
     * Inclui campos calculados.
     *
     * @param peca entidade
     * @return DTO de resposta
     */
    @Mapping(source = "estoqueBaixo", target = "estoqueBaixo")
    @Mapping(source = "margemLucro", target = "margemLucro")
    @Mapping(source = "valorTotalEstoque", target = "valorTotalEstoque")
    PecaResponseDTO toResponseDTO(Peca peca);
}
