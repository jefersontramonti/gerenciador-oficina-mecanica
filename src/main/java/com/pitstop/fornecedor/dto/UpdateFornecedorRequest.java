package com.pitstop.fornecedor.dto;

import com.pitstop.fornecedor.domain.TipoFornecedor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para atualização de um fornecedor existente.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para atualização de um fornecedor")
public class UpdateFornecedorRequest {

    @Schema(description = "Tipo de fornecedor", example = "DISTRIBUIDOR")
    private TipoFornecedor tipo;

    @Schema(description = "Nome fantasia do fornecedor", example = "Auto Peças Brasil")
    @Size(min = 2, max = 200, message = "Nome fantasia deve ter entre 2 e 200 caracteres")
    private String nomeFantasia;

    @Schema(description = "Razão social do fornecedor", example = "Auto Peças Brasil Ltda")
    @Size(max = 200, message = "Razão social deve ter no máximo 200 caracteres")
    private String razaoSocial;

    @Schema(description = "CPF ou CNPJ formatado", example = "12.345.678/0001-90")
    private String cpfCnpj;

    @Schema(description = "Inscrição estadual", example = "123.456.789.000")
    @Size(max = 20, message = "Inscrição estadual deve ter no máximo 20 caracteres")
    private String inscricaoEstadual;

    // Contact
    @Schema(description = "E-mail do fornecedor", example = "contato@autopecas.com.br")
    @Email(message = "E-mail deve ser válido")
    @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres")
    private String email;

    @Schema(description = "Telefone fixo", example = "(11) 3333-4444")
    private String telefone;

    @Schema(description = "Celular", example = "(11) 98888-7777")
    private String celular;

    @Schema(description = "Website do fornecedor", example = "https://www.autopecas.com.br")
    @Size(max = 200, message = "Website deve ter no máximo 200 caracteres")
    private String website;

    @Schema(description = "Nome da pessoa de contato", example = "Carlos Silva")
    @Size(max = 150, message = "Nome do contato deve ter no máximo 150 caracteres")
    private String contatoNome;

    // Address
    @Schema(description = "Logradouro", example = "Rua das Autopeças")
    @Size(max = 200, message = "Logradouro deve ter no máximo 200 caracteres")
    private String logradouro;

    @Schema(description = "Número", example = "500")
    @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
    private String numero;

    @Schema(description = "Complemento", example = "Galpão 3")
    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    private String complemento;

    @Schema(description = "Bairro", example = "Distrito Industrial")
    @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
    private String bairro;

    @Schema(description = "Cidade", example = "São Paulo")
    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    private String cidade;

    @Schema(description = "Estado (UF)", example = "SP")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Estado deve ser a sigla da UF com 2 letras maiúsculas")
    private String estado;

    @Schema(description = "CEP", example = "01310-100")
    @Pattern(regexp = "^\\d{5}-\\d{3}$", message = "CEP deve estar no formato 00000-000")
    private String cep;

    // Commercial
    @Schema(description = "Prazo de entrega", example = "3 a 5 dias úteis")
    @Size(max = 100, message = "Prazo de entrega deve ter no máximo 100 caracteres")
    private String prazoEntrega;

    @Schema(description = "Condições de pagamento", example = "30/60/90 dias")
    @Size(max = 200, message = "Condições de pagamento deve ter no máximo 200 caracteres")
    private String condicoesPagamento;

    @Schema(description = "Desconto padrão (%)", example = "5.00")
    @DecimalMin(value = "0.00", message = "Desconto deve ser >= 0")
    @DecimalMax(value = "100.00", message = "Desconto deve ser <= 100")
    private BigDecimal descontoPadrao;

    // Notes
    @Schema(description = "Observações gerais")
    private String observacoes;
}
