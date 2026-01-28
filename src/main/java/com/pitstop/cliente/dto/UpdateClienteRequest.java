package com.pitstop.cliente.dto;

import com.pitstop.cliente.domain.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para atualização de um cliente existente.
 *
 * <p>Nota: CPF/CNPJ e tipo não podem ser alterados após criação por questões de
 * consistência tributária. Para alterar esses dados, é necessário criar um novo cliente.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para atualização de um cliente existente")
public class UpdateClienteRequest {

    @Schema(description = "Nome completo (PF) ou razão social (PJ)", example = "João da Silva Santos", required = true)
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @Schema(description = "E-mail do cliente", example = "joao.santos@email.com")
    @Email(message = "E-mail deve ser válido")
    @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres")
    private String email;

    @Schema(description = "Telefone fixo com DDD", example = "(11) 3333-5555")
    @Pattern(
        regexp = "^(\\(\\d{2}\\)\\s?)?\\d{4,5}-?\\d{4}$",
        message = "Telefone deve estar no formato (00) 0000-0000 ou (00) 00000-0000"
    )
    private String telefone;

    @Schema(description = "Telefone celular com DDD (obrigatório)", example = "(11) 99999-8888", required = true)
    @NotBlank(message = "Celular é obrigatório")
    @Pattern(
        regexp = "^\\(\\d{2}\\)\\s?9\\d{4}-\\d{4}$",
        message = "Celular deve estar no formato (00) 90000-0000 (com DDD e começando com 9)"
    )
    private String celular;

    // Campos de Endereço

    @Schema(description = "Nome da rua/avenida", example = "Av. Paulista")
    @Size(max = 200, message = "Logradouro deve ter no máximo 200 caracteres")
    private String logradouro;

    @Schema(description = "Número do imóvel", example = "1000")
    @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
    private String numero;

    @Schema(description = "Complemento (apt, sala, bloco, etc.)", example = "Sala 1503")
    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    private String complemento;

    @Schema(description = "Bairro", example = "Bela Vista")
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
     * Validação customizada: celular é obrigatório (já validado por @NotBlank).
     * Mantido para compatibilidade - celular é o campo principal de contato.
     */
    @AssertTrue(message = "Celular é obrigatório")
    private boolean isCelularPreenchido() {
        return celular != null && !celular.isBlank();
    }
}
