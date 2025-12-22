package com.pitstop.veiculo.mapper;

import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.dto.VeiculoRequestDTO;
import com.pitstop.veiculo.dto.VeiculoResponseDTO;
import com.pitstop.veiculo.dto.VeiculoUpdateDTO;
import org.mapstruct.*;

import java.util.UUID;

/**
 * MapStruct mapper para conversão entre entidade Veiculo e seus DTOs.
 *
 * <p>MapStruct gera automaticamente a implementação em tempo de compilação,
 * proporcionando alta performance e type-safety.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface VeiculoMapper {

    /**
     * Converte VeiculoRequestDTO para entidade Veiculo.
     *
     * <p>O campo oficina é auto-populado via @PrePersist JPA hook.</p>
     *
     * @param request DTO de criação
     * @return entidade Veiculo
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "oficina", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Veiculo toEntity(VeiculoRequestDTO request);

    /**
     * Atualiza entidade Veiculo existente com dados do VeiculoUpdateDTO.
     *
     * <p>Campos não modificáveis (id, oficina, clienteId, placa) são ignorados.</p>
     *
     * @param request DTO de atualização
     * @param veiculo entidade existente a ser atualizada
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "oficina", ignore = true)
    @Mapping(target = "clienteId", ignore = true)
    @Mapping(target = "placa", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(VeiculoUpdateDTO request, @MappingTarget Veiculo veiculo);

    /**
     * Converte entidade Veiculo para VeiculoResponseDTO.
     *
     * @param veiculo entidade
     * @return DTO de resposta
     */
    @Mapping(target = "placa", expression = "java(veiculo.getPlacaFormatada())")
    @Mapping(target = "descricaoCompleta", expression = "java(veiculo.getDescricaoCompleta())")
    @Mapping(target = "cliente", ignore = true) // Será preenchido no service
    VeiculoResponseDTO toResponse(Veiculo veiculo);

    /**
     * Cria ClienteResumoDTO a partir de dados individuais.
     * Método auxiliar para uso no service.
     *
     * @param id ID do cliente
     * @param nome nome do cliente
     * @param cpfCnpj CPF/CNPJ do cliente
     * @param telefone telefone do cliente (celular preferencial)
     * @return DTO com dados resumidos do cliente
     */
    default VeiculoResponseDTO.ClienteResumoDTO toClienteResumo(UUID id, String nome, String cpfCnpj, String telefone) {
        return VeiculoResponseDTO.ClienteResumoDTO.builder()
            .id(id)
            .nome(nome)
            .cpfCnpj(cpfCnpj)
            .telefone(telefone)
            .build();
    }
}
