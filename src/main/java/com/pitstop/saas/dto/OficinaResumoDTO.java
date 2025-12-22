package com.pitstop.saas.dto;

import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for workshop summary information in list views.
 *
 * Provides essential workshop data for dashboard cards and table rows
 * without exposing sensitive operational details.
 *
 * @author PitStop Team
 */
public record OficinaResumoDTO(
    UUID id,
    String nomeFantasia,
    String cnpj,
    StatusOficina status,
    PlanoAssinatura plano,
    BigDecimal mensalidade,
    LocalDate dataVencimento,
    Integer diasRestantesTrial,
    Long totalUsuarios,
    Long totalOrdensServico,
    Long totalClientes,
    LocalDateTime createdAt
) {
    /**
     * Creates a workshop summary DTO.
     *
     * @param id Unique identifier
     * @param nomeFantasia Trade name of the workshop
     * @param cnpj Brazilian company registration number
     * @param status Current subscription status
     * @param plano Subscription plan tier
     * @param mensalidade Monthly fee for the plan
     * @param dataVencimento Next payment due date
     * @param diasRestantesTrial Days remaining in trial (null if not in trial)
     * @param totalUsuarios Number of users in this workshop
     * @param totalOrdensServico Number of service orders created
     * @param totalClientes Number of customers registered
     * @param createdAt Workshop registration date
     */
    public OficinaResumoDTO {
        // Compact constructor
    }
}
