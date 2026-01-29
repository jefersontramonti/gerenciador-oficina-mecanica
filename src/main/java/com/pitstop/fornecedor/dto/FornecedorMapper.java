package com.pitstop.fornecedor.dto;

import com.pitstop.cliente.domain.Endereco;
import com.pitstop.fornecedor.domain.Fornecedor;
import org.mapstruct.*;

/**
 * MapStruct mapper para conversão entre entidade Fornecedor e seus DTOs.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface FornecedorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "oficina", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "endereco", expression = "java(mapEndereco(request))")
    Fornecedor toEntity(CreateFornecedorRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "oficina", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "endereco", expression = "java(mapEnderecoFromUpdate(request))")
    void updateEntityFromDto(UpdateFornecedorRequest request, @MappingTarget Fornecedor fornecedor);

    @Mapping(target = "endereco", expression = "java(mapEnderecoResponse(fornecedor.getEndereco()))")
    FornecedorResponse toResponse(Fornecedor fornecedor);

    FornecedorResumoResponse toResumoResponse(Fornecedor fornecedor);

    default Endereco mapEndereco(CreateFornecedorRequest request) {
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

    default Endereco mapEnderecoFromUpdate(UpdateFornecedorRequest request) {
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

    default FornecedorResponse.EnderecoResponse mapEnderecoResponse(Endereco endereco) {
        if (endereco == null) {
            return null;
        }
        return FornecedorResponse.EnderecoResponse.builder()
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
