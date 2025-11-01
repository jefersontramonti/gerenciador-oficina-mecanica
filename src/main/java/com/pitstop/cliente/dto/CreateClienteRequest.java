package com.pitstop.cliente.dto;

import com.pitstop.cliente.domain.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para criação de um novo cliente.
 *
 * <p>Contém todas as validações necessárias para garantir dados consistentes
 * antes de criar a entidade Cliente.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para criação de um novo cliente")
public class CreateClienteRequest {

    @Schema(description = "Tipo de cliente", example = "PESSOA_FISICA", required = true)
    @NotNull(message = "Tipo de cliente é obrigatório")
    private TipoCliente tipo;

    @Schema(description = "Nome completo (PF) ou razão social (PJ)", example = "João da Silva", required = true)
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @Schema(
        description = "CPF (formato: 000.000.000-00) ou CNPJ (formato: 00.000.000/0000-00)",
        example = "123.456.789-00",
        required = true
    )
    @NotBlank(message = "CPF/CNPJ é obrigatório")
    @Pattern(
        regexp = "^(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})$",
        message = "CPF/CNPJ deve estar no formato válido (CPF: 000.000.000-00 ou CNPJ: 00.000.000/0000-00)"
    )
    private String cpfCnpj;

    @Schema(description = "E-mail do cliente", example = "joao@email.com")
    @Email(message = "E-mail deve ser válido")
    @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres")
    private String email;

    @Schema(description = "Telefone fixo com DDD", example = "(11) 3333-4444")
    @Pattern(
        regexp = "^(\\(\\d{2}\\)\\s?)?\\d{4,5}-?\\d{4}$",
        message = "Telefone deve estar no formato (00) 0000-0000 ou (00) 00000-0000"
    )
    private String telefone;

    @Schema(description = "Telefone celular com DDD", example = "(11) 98888-7777")
    @Pattern(
        regexp = "^(\\(\\d{2}\\)\\s?)?\\d{4,5}-?\\d{4}$",
        message = "Celular deve estar no formato (00) 00000-0000"
    )
    private String celular;

    // Campos de Endereço

    @Schema(description = "Nome da rua/avenida", example = "Rua das Flores")
    @Size(max = 200, message = "Logradouro deve ter no máximo 200 caracteres")
    private String logradouro;

    @Schema(description = "Número do imóvel", example = "123")
    @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
    private String numero;

    @Schema(description = "Complemento (apt, sala, bloco, etc.)", example = "Apto 45B")
    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    private String complemento;

    @Schema(description = "Bairro", example = "Centro")
    @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
    private String bairro;

    @Schema(description = "Cidade", example = "São Paulo")
    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    private String cidade;

    @Schema(description = "Sigla do estado (UF)", example = "SP")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Estado deve ser a sigla da UF com 2 letras maiúsculas (ex: SP, RJ)")
    private String estado;

    @Schema(description = "CEP no formato 00000-000", example = "01310-100")
    @Pattern(regexp = "^\\d{5}-\\d{3}$", message = "CEP deve estar no formato 00000-000")
    private String cep;

    /**
     * Validação customizada: ao menos um telefone deve estar preenchido.
     */
    @AssertTrue(message = "Ao menos um telefone (fixo ou celular) deve ser informado")
    private boolean isAoMenosUmTelefonePreenchido() {
        return (telefone != null && !telefone.isBlank()) || (celular != null && !celular.isBlank());
    }
}
