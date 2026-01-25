package com.pitstop.saas.dto;

import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a new lead.
 *
 * Typically used by public landing page forms to capture
 * potential customer information.
 *
 * @author PitStop Team
 */
public record CreateLeadRequest(

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    String nome,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 255)
    String email,

    @NotBlank(message = "WhatsApp é obrigatório")
    @Pattern(regexp = "\\d{10,15}", message = "WhatsApp deve conter apenas números (10 a 15 dígitos)")
    String whatsapp,

    @Size(max = 100)
    String origem
) {
    /**
     * Creates a lead capture request.
     * Origem defaults to "landing-page" if not provided.
     */
    public CreateLeadRequest {
        if (origem == null || origem.isBlank()) {
            origem = "landing-page";
        }
    }
}
