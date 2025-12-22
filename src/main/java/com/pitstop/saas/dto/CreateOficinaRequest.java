package com.pitstop.saas.dto;

import com.pitstop.oficina.domain.PlanoAssinatura;
import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a new workshop in the SaaS platform.
 *
 * Includes all required information to set up a new workshop tenant
 * with automatic 30-day trial period.
 *
 * @author PitStop Team
 */
public record CreateOficinaRequest(

    @NotBlank(message = "Razão social é obrigatória")
    @Size(min = 3, max = 200, message = "Razão social deve ter entre 3 e 200 caracteres")
    String razaoSocial,

    @NotBlank(message = "Nome fantasia é obrigatório")
    @Size(min = 3, max = 200, message = "Nome fantasia deve ter entre 3 e 200 caracteres")
    String nomeFantasia,

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 dígitos")
    String cnpj,

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
    String estado,

    // Dados do usuário admin inicial
    @NotBlank(message = "Nome do administrador é obrigatório")
    @Size(min = 3, max = 200)
    String nomeAdmin,

    @Email(message = "Email do administrador inválido")
    @NotBlank(message = "Email do administrador é obrigatório")
    String emailAdmin,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    String senhaAdmin
) {
    /**
     * Creates a request for new workshop creation.
     * Workshop will automatically start with TRIAL status and 30-day trial period.
     * An admin user will be created with the provided credentials.
     */
    public CreateOficinaRequest {
        // Compact constructor
    }
}
