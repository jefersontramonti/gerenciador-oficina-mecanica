package com.pitstop.financeiro.mapper;

import com.pitstop.financeiro.domain.Pagamento;
import com.pitstop.financeiro.dto.PagamentoRequestDTO;
import com.pitstop.financeiro.dto.PagamentoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper para conversão entre Pagamento e seus DTOs.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Mapper(componentModel = "spring")
public interface PagamentoMapper {

    /**
     * Converte PagamentoRequestDTO para Pagamento.
     *
     * <p>O campo oficina é auto-populado via @PrePersist JPA hook.</p>
     *
     * @param dto DTO de entrada
     * @return entidade Pagamento
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dataPagamento", ignore = true)
    @Mapping(target = "comprovante", ignore = true)
    @Mapping(target = "notaFiscalId", ignore = true)
    @Mapping(target = "oficina", ignore = true) // Auto-populado via @PrePersist JPA hook
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Pagamento toEntity(PagamentoRequestDTO dto);

    /**
     * Converte Pagamento para PagamentoResponseDTO.
     *
     * @param pagamento entidade Pagamento
     * @return DTO de resposta
     */
    PagamentoResponseDTO toResponseDTO(Pagamento pagamento);

    /**
     * Atualiza entidade Pagamento com dados do DTO.
     *
     * <p>O campo oficina é auto-populado via @PrePersist JPA hook.</p>
     *
     * @param dto DTO de entrada
     * @param pagamento entidade a ser atualizada
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ordemServicoId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dataPagamento", ignore = true)
    @Mapping(target = "comprovante", ignore = true)
    @Mapping(target = "notaFiscalId", ignore = true)
    @Mapping(target = "oficina", ignore = true) // Auto-populado via @PrePersist JPA hook
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(PagamentoRequestDTO dto, @MappingTarget Pagamento pagamento);
}
