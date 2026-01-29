package com.pitstop.fornecedor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.fornecedor.domain.TipoFornecedor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta contendo dados completos de um fornecedor.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados completos de um fornecedor")
public class FornecedorResponse {

    @Schema(description = "Identificador único", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Tipo de fornecedor", example = "DISTRIBUIDOR")
    private TipoFornecedor tipo;

    @Schema(description = "Nome fantasia", example = "Auto Peças Brasil")
    private String nomeFantasia;

    @Schema(description = "Razão social", example = "Auto Peças Brasil Ltda")
    private String razaoSocial;

    @Schema(description = "CPF/CNPJ", example = "12.345.678/0001-90")
    private String cpfCnpj;

    @Schema(description = "Inscrição estadual", example = "123.456.789.000")
    private String inscricaoEstadual;

    // Contact
    @Schema(description = "E-mail", example = "contato@autopecas.com.br")
    private String email;

    @Schema(description = "Telefone fixo", example = "(11) 3333-4444")
    private String telefone;

    @Schema(description = "Celular", example = "(11) 98888-7777")
    private String celular;

    @Schema(description = "Website", example = "https://www.autopecas.com.br")
    private String website;

    @Schema(description = "Nome do contato", example = "Carlos Silva")
    private String contatoNome;

    // Address
    @Schema(description = "Endereço completo")
    private EnderecoResponse endereco;

    // Commercial
    @Schema(description = "Prazo de entrega", example = "3 a 5 dias úteis")
    private String prazoEntrega;

    @Schema(description = "Condições de pagamento", example = "30/60/90 dias")
    private String condicoesPagamento;

    @Schema(description = "Desconto padrão (%)", example = "5.00")
    private BigDecimal descontoPadrao;

    // Notes
    @Schema(description = "Observações")
    private String observacoes;

    @Schema(description = "Status ativo", example = "true")
    private Boolean ativo;

    @Schema(description = "Data de criação", example = "2026-01-28T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Data de atualização", example = "2026-01-28T15:30:00")
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
        private String logradouro;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private String estado;
        private String cep;
        private String enderecoFormatado;
    }
}
