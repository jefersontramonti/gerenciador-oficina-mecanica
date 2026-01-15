package com.pitstop.saas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for changing a workshop's subscription plan.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlterarPlanoOficinaRequest {

    /**
     * The code of the new plan (e.g., "PROFISSIONAL", "TURBINADO").
     */
    @NotBlank(message = "Código do novo plano é obrigatório")
    private String novoPlano;

    /**
     * Whether to apply the change immediately or at the end of the billing cycle.
     */
    @Builder.Default
    private Boolean aplicarImediatamente = true;

    /**
     * Whether to keep the old price (grandfathering) for existing customers.
     */
    @Builder.Default
    private Boolean manterPrecoAntigo = false;

    /**
     * Optional reason for the plan change.
     */
    private String motivo;
}
