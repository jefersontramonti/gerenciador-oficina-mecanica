package com.pitstop.saas.dto;

import com.pitstop.oficina.domain.PlanoAssinatura;
import jakarta.validation.constraints.*;

/**
 * Request DTO for updating workshop information.
 *
 * Allows SUPER_ADMIN to modify workshop details including plan,
 * contact information, and address. Status changes are handled
 * by specific endpoints (activate, suspend, cancel).
 *
 * @author PitStop Team
 */
public record UpdateOficinaRequest(

    @NotBlank(message = "Razão social é obrigatória")
    @Size(min = 3, max = 200, message = "Razão social deve ter entre 3 e 200 caracteres")
    String razaoSocial,

    @NotBlank(message = "Nome fantasia é obrigatório")
    @Size(min = 3, max = 200, message = "Nome fantasia deve ter entre 3 e 200 caracteres")
    String nomeFantasia,

    @Email(message = "Email inválido")
    @NotBlank(message = "Email é obrigatório")
    String email,

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve ter 10 ou 11 dígitos")
    String telefone,

    @NotNull(message = "Plano é obrigatório")
    PlanoAssinatura plano,

    // Endereço
    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "\\d{8}", message = "CEP deve conter 8 dígitos")
    String cep,

    @NotBlank(message = "Logradouro é obrigatório")
    @Size(max = 200)
    String logradouro,

    @NotBlank(message = "Número é obrigatório")
    @Size(max = 20)
    String numero,

    @Size(max = 100)
    String complemento,

    @NotBlank(message = "Bairro é obrigatório")
    @Size(max = 100)
    String bairro,

    @NotBlank(message = "Cidade é obrigatória")
    @Size(max = 100)
    String cidade,

    @NotBlank(message = "Estado é obrigatório")
    @Pattern(regexp = "[A-Z]{2}", message = "Estado deve ser uma UF válida (2 letras)")
    String estado
) {
    /**
     * Creates a request for updating workshop information.
     * CNPJ cannot be changed (immutable identifier).
     * Status changes must be done through specific action endpoints.
     */
    public UpdateOficinaRequest {
        // Compact constructor
    }
}
