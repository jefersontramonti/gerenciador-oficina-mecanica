package com.pitstop.cliente.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.cliente.domain.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta contendo dados completos de um cliente.
 *
 * <p>Usado em operações de consulta (GET) para retornar informações do cliente
 * incluindo metadados de auditoria.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados completos de um cliente")
public class ClienteResponse {

    @Schema(description = "Identificador único do cliente", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Tipo de cliente", example = "PESSOA_FISICA")
    private TipoCliente tipo;

    @Schema(description = "Nome completo (PF) ou razão social (PJ)", example = "João da Silva")
    private String nome;

    @Schema(description = "CPF ou CNPJ formatado", example = "123.456.789-00")
    private String cpfCnpj;

    @Schema(description = "E-mail do cliente", example = "joao@email.com")
    private String email;

    @Schema(description = "Telefone fixo", example = "(11) 3333-4444")
    private String telefone;

    @Schema(description = "Telefone celular", example = "(11) 98888-7777")
    private String celular;

    @Schema(description = "Endereço completo do cliente")
    private EnderecoResponse endereco;

    @Schema(description = "Indica se o cliente está ativo", example = "true")
    private Boolean ativo;

    @Schema(description = "Data e hora de criação do registro", example = "2025-10-31T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Data e hora da última atualização", example = "2025-10-31T15:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * DTO aninhado para representar o endereço.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Dados do endereço")
    public static class EnderecoResponse {

        @Schema(description = "Nome da rua/avenida", example = "Rua das Flores")
        private String logradouro;

        @Schema(description = "Número do imóvel", example = "123")
        private String numero;

        @Schema(description = "Complemento", example = "Apto 45B")
        private String complemento;

        @Schema(description = "Bairro", example = "Centro")
        private String bairro;

        @Schema(description = "Cidade", example = "São Paulo")
        private String cidade;

        @Schema(description = "Sigla do estado", example = "SP")
        private String estado;

        @Schema(description = "CEP", example = "01310-100")
        private String cep;

        @Schema(description = "Endereço formatado em linha única", example = "Rua das Flores, 123 - Centro, São Paulo/SP, CEP: 01310-100")
        private String enderecoFormatado;
    }
}
