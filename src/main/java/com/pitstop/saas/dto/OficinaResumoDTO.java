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
    String razaoSocial,
    String cnpjCpf,
    String email,
    String telefone,
    StatusOficina status,
    PlanoAssinatura plano,
    BigDecimal valorMensalidade,
    LocalDate dataVencimentoPlano,
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
     * @param razaoSocial Legal company name
     * @param cnpjCpf Brazilian company registration number (CPF or CNPJ)
     * @param email Contact email
     * @param telefone Contact phone
     * @param status Current subscription status
     * @param plano Subscription plan tier
     * @param valorMensalidade Monthly fee for the plan
     * @param dataVencimentoPlano Next payment due date
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
