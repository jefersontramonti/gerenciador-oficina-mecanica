package com.pitstop.ordemservico.mapper;

import com.pitstop.ordemservico.domain.ItemOS;
import com.pitstop.ordemservico.dto.CreateItemOSDTO;
import com.pitstop.ordemservico.dto.ItemOSResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper para conversão entre entidade ItemOS e seus DTOs.
 *
 * <p>MapStruct gera automaticamente a implementação em tempo de compilação,
 * proporcionando alta performance e type-safety.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ItemOSMapper {

    /**
     * Converte CreateItemOSDTO para entidade ItemOS.
     *
     * @param dto DTO de criação
     * @return entidade ItemOS
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ordemServico", ignore = true) // Será associado no service
    @Mapping(target = "valorTotal", ignore = true) // Calculado automaticamente na entity
    @Mapping(target = "createdAt", ignore = true)
    ItemOS toEntity(CreateItemOSDTO dto);

    /**
     * Converte entidade ItemOS para ItemOSResponseDTO.
     *
     * @param item entidade
     * @return DTO de resposta
     */
    ItemOSResponseDTO toResponse(ItemOS item);
}
