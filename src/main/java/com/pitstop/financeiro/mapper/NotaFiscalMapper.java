package com.pitstop.financeiro.mapper;

import com.pitstop.financeiro.domain.NotaFiscal;
import com.pitstop.financeiro.dto.NotaFiscalRequestDTO;
import com.pitstop.financeiro.dto.NotaFiscalResponseDTO;
import com.pitstop.financeiro.dto.NotaFiscalResumoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper para conversão entre NotaFiscal e DTOs.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-23
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotaFiscalMapper {

    /**
     * Converte DTO de requisição para entidade.
     *
     * <p>O campo oficina é auto-populado via @PrePersist JPA hook.</p>
     *
     * @param dto DTO de requisição
     * @return entidade NotaFiscal
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "numero", ignore = true)
    @Mapping(target = "chaveAcesso", ignore = true)
    @Mapping(target = "protocoloAutorizacao", ignore = true)
    @Mapping(target = "dataHoraAutorizacao", ignore = true)
    @Mapping(target = "xmlEnviado", ignore = true)
    @Mapping(target = "xmlAutorizado", ignore = true)
    @Mapping(target = "xmlCancelamento", ignore = true)
    @Mapping(target = "protocoloCancelamento", ignore = true)
    @Mapping(target = "dataHoraCancelamento", ignore = true)
    @Mapping(target = "justificativaCancelamento", ignore = true)
    @Mapping(target = "oficina", ignore = true) // Auto-populado via @PrePersist JPA hook
    @Mapping(target = "itens", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NotaFiscal toEntity(NotaFiscalRequestDTO dto);

    /**
     * Converte entidade para DTO de resposta.
     *
     * @param notaFiscal entidade
     * @return DTO de resposta
     */
    NotaFiscalResponseDTO toResponseDTO(NotaFiscal notaFiscal);

    /**
     * Converte entidade para DTO de resumo.
     *
     * @param notaFiscal entidade
     * @return DTO de resumo
     */
    NotaFiscalResumoDTO toResumoDTO(NotaFiscal notaFiscal);
}
