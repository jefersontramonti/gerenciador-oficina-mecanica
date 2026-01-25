package com.pitstop.saas.dto;

import com.pitstop.saas.domain.StatusLead;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a lead.
 *
 * Allows updating status and adding observations.
 *
 * @author PitStop Team
 */
public record UpdateLeadRequest(

    StatusLead status,

    @Size(max = 5000, message = "Observações não podem exceder 5000 caracteres")
    String observacoes
) {
}
