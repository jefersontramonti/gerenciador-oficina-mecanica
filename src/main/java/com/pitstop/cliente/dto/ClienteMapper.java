package com.pitstop.cliente.dto;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.domain.Endereco;
import org.mapstruct.*;

/**
 * MapStruct mapper para conversão entre entidade Cliente e seus DTOs.
 *
 * <p>MapStruct gera automaticamente a implementação em tempo de compilação,
 * proporcionando alta performance e type-safety.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface ClienteMapper {

    /**
     * Converte CreateClienteRequest para entidade Cliente.
     *
     * <p>O endereço é construído a partir dos campos individuais do request.</p>
     * <p>O campo oficina é auto-populado via @PrePersist JPA hook.</p>
     *
     * @param request DTO de criação
     * @return entidade Cliente
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "oficina", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "endereco", expression = "java(mapEndereco(request))")
    Cliente toEntity(CreateClienteRequest request);

    /**
     * Atualiza entidade Cliente existente com dados do UpdateClienteRequest.
     *
     * <p>Campos não modificáveis (id, oficina, tipo, cpfCnpj, ativo) são ignorados.</p>
     *
     * @param request DTO de atualização
     * @param cliente entidade existente a ser atualizada
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "oficina", ignore = true)
    @Mapping(target = "tipo", ignore = true)
    @Mapping(target = "cpfCnpj", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "endereco", expression = "java(mapEndereco(request))")
    void updateEntityFromDto(UpdateClienteRequest request, @MappingTarget Cliente cliente);

    /**
     * Converte entidade Cliente para ClienteResponse.
     *
     * @param cliente entidade
     * @return DTO de resposta
     */
    @Mapping(target = "endereco", expression = "java(mapEnderecoResponse(cliente.getEndereco()))")
    ClienteResponse toResponse(Cliente cliente);

    /**
     * Mapeamento customizado de campos de endereço do CreateClienteRequest para Value Object Endereco.
     *
     * @param request DTO de criação
     * @return Value Object Endereco
     */
    default Endereco mapEndereco(CreateClienteRequest request) {
        if (request == null) {
            return null;
        }

        return Endereco.builder()
            .logradouro(request.getLogradouro())
            .numero(request.getNumero())
            .complemento(request.getComplemento())
            .bairro(request.getBairro())
            .cidade(request.getCidade())
            .estado(request.getEstado())
            .cep(request.getCep())
            .build();
    }

    /**
     * Mapeamento customizado de campos de endereço do UpdateClienteRequest para Value Object Endereco.
     *
     * @param request DTO de atualização
     * @return Value Object Endereco
     */
    default Endereco mapEndereco(UpdateClienteRequest request) {
        if (request == null) {
            return null;
        }

        return Endereco.builder()
            .logradouro(request.getLogradouro())
            .numero(request.getNumero())
            .complemento(request.getComplemento())
            .bairro(request.getBairro())
            .cidade(request.getCidade())
            .estado(request.getEstado())
            .cep(request.getCep())
            .build();
    }

    /**
     * Mapeamento customizado de Value Object Endereco para EnderecoResponse.
     *
     * @param endereco Value Object
     * @return DTO de endereço
     */
    default ClienteResponse.EnderecoResponse mapEnderecoResponse(Endereco endereco) {
        if (endereco == null) {
            return null;
        }

        return ClienteResponse.EnderecoResponse.builder()
            .logradouro(endereco.getLogradouro())
            .numero(endereco.getNumero())
            .complemento(endereco.getComplemento())
            .bairro(endereco.getBairro())
            .cidade(endereco.getCidade())
            .estado(endereco.getEstado())
            .cep(endereco.getCep())
            .enderecoFormatado(endereco.getEnderecoFormatado())
            .build();
    }
}
