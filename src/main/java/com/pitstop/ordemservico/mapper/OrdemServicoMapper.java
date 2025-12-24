package com.pitstop.ordemservico.mapper;

import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.domain.StatusOS;
import com.pitstop.ordemservico.dto.*;
import org.mapstruct.*;

import java.util.UUID;

/**
 * MapStruct mapper para conversão entre entidade OrdemServico e seus DTOs.
 *
 * <p>MapStruct gera automaticamente a implementação em tempo de compilação,
 * proporcionando alta performance e type-safety.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = {ItemOSMapper.class})
public interface OrdemServicoMapper {

    /**
     * Converte CreateOrdemServicoDTO para entidade OrdemServico.
     *
     * <p>O campo oficina é auto-populado via @PrePersist JPA hook.</p>
     *
     * @param dto DTO de criação
     * @return entidade OrdemServico
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numero", ignore = true) // Gerado por sequence
    @Mapping(target = "status", constant = "ORCAMENTO") // Status inicial sempre ORCAMENTO
    @Mapping(target = "dataAbertura", ignore = true) // Setado automaticamente
    @Mapping(target = "dataFinalizacao", ignore = true)
    @Mapping(target = "dataEntrega", ignore = true)
    @Mapping(target = "valorPecas", ignore = true) // Calculado automaticamente
    @Mapping(target = "valorTotal", ignore = true) // Calculado automaticamente
    @Mapping(target = "descontoPercentual", constant = "0")
    @Mapping(target = "descontoValor", constant = "0")
    @Mapping(target = "valorFinal", ignore = true) // Calculado automaticamente
    @Mapping(target = "aprovadoPeloCliente", constant = "false")
    @Mapping(target = "tokenAprovacao", ignore = true) // Gerado automaticamente no service
    @Mapping(target = "tokenAprovacaoExpiracao", ignore = true) // Gerado automaticamente no service
    @Mapping(target = "version", ignore = true) // Optimistic locking gerenciado pelo JPA
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "oficina", ignore = true) // Auto-populado via @PrePersist JPA hook
    @Mapping(target = "itens", ignore = true) // Adicionados manualmente no service usando adicionarItem()
    OrdemServico toEntity(CreateOrdemServicoDTO dto);

    /**
     * Atualiza entidade OrdemServico existente com dados do UpdateOrdemServicoDTO.
     *
     * <p>Apenas campos fornecidos no DTO (não nulos) serão atualizados.</p>
     * <p>O campo oficina é auto-populado via @PrePersist JPA hook.</p>
     *
     * @param dto DTO de atualização
     * @param ordemServico entidade existente a ser atualizada
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numero", ignore = true) // Não pode ser alterado
    @Mapping(target = "veiculoId", ignore = true) // Não pode ser alterado
    @Mapping(target = "usuarioId", ignore = true) // Não pode ser alterado
    @Mapping(target = "status", ignore = true) // Alterado via métodos de negócio
    @Mapping(target = "dataAbertura", ignore = true) // Não pode ser alterado
    @Mapping(target = "dataFinalizacao", ignore = true) // Gerenciado por métodos de negócio
    @Mapping(target = "dataEntrega", ignore = true) // Gerenciado por métodos de negócio
    @Mapping(target = "valorPecas", ignore = true) // Calculado automaticamente
    @Mapping(target = "valorTotal", ignore = true) // Calculado automaticamente
    @Mapping(target = "valorFinal", ignore = true) // Calculado automaticamente
    @Mapping(target = "aprovadoPeloCliente", ignore = true) // Alterado via método aprovar()
    @Mapping(target = "tokenAprovacao", ignore = true) // Não pode ser alterado
    @Mapping(target = "tokenAprovacaoExpiracao", ignore = true) // Não pode ser alterado
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "oficina", ignore = true) // Auto-populado via @PrePersist JPA hook
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdateOrdemServicoDTO dto, @MappingTarget OrdemServico ordemServico);

    /**
     * Converte entidade OrdemServico para OrdemServicoResponseDTO.
     *
     * <p>Dados relacionados (veiculo, cliente, mecanico) devem ser preenchidos no service.</p>
     *
     * @param ordemServico entidade
     * @return DTO de resposta
     */
    @Mapping(target = "veiculo", ignore = true) // Preenchido no service
    @Mapping(target = "cliente", ignore = true) // Preenchido no service
    @Mapping(target = "mecanico", ignore = true) // Preenchido no service
    OrdemServicoResponseDTO toResponse(OrdemServico ordemServico);

    /**
     * Cria VeiculoResumoDTO a partir de dados individuais.
     * Método auxiliar para uso no service.
     *
     * @param id ID do veículo
     * @param placa placa do veículo
     * @param marca marca do veículo
     * @param modelo modelo do veículo
     * @param ano ano do veículo
     * @param cor cor do veículo
     * @return DTO com dados resumidos do veículo
     */
    default VeiculoResumoDTO toVeiculoResumo(UUID id, String placa, String marca, String modelo, Integer ano, String cor) {
        return new VeiculoResumoDTO(id, placa, marca, modelo, ano, cor);
    }

    /**
     * Cria ClienteResumoDTO a partir de dados individuais.
     * Método auxiliar para uso no service.
     *
     * @param id ID do cliente
     * @param nome nome do cliente
     * @param cpfCnpj CPF/CNPJ do cliente
     * @param telefone telefone do cliente (celular preferencial)
     * @param email email do cliente
     * @return DTO com dados resumidos do cliente
     */
    default ClienteResumoDTO toClienteResumo(UUID id, String nome, String cpfCnpj, String telefone, String email) {
        return new ClienteResumoDTO(id, nome, cpfCnpj, telefone, email);
    }

    /**
     * Cria UsuarioResumoDTO a partir de dados individuais.
     * Método auxiliar para uso no service.
     *
     * @param id ID do usuário
     * @param nome nome do usuário
     * @param email email do usuário
     * @param perfil perfil do usuário (ADMIN, GERENTE, ATENDENTE, MECANICO)
     * @return DTO com dados resumidos do usuário
     */
    default UsuarioResumoDTO toUsuarioResumo(UUID id, String nome, String email, String perfil) {
        return new UsuarioResumoDTO(id, nome, email, perfil);
    }
}
