package com.pitstop.saas.dto;

import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO with complete workshop details.
 *
 * Includes all workshop information, subscription status, usage statistics,
 * and payment history. Used in detail views and administrative operations.
 *
 * @author PitStop Team
 */
public record OficinaDetailResponse(
    UUID id,
    String razaoSocial,
    String nomeFantasia,
    String cnpj,
    String email,
    String telefone,
    StatusOficina status,
    PlanoAssinatura plano,
    BigDecimal mensalidade,
    LocalDate dataInicioTrial,
    LocalDate dataFimTrial,
    Integer diasRestantesTrial,
    LocalDate dataVencimento,
    Boolean ativa,

    // Endereço
    String cep,
    String logradouro,
    String numero,
    String complemento,
    String bairro,
    String cidade,
    String estado,

    // Estatísticas de uso
    Long totalUsuarios,
    Long totalClientes,
    Long totalVeiculos,
    Long totalOrdensServico,
    Long totalPecas,

    // Estatísticas financeiras
    BigDecimal totalFaturamento,
    Integer pagamentosRealizados,
    Integer pagamentosPendentes,
    LocalDate ultimoPagamento,

    // Metadata
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Creates a detailed workshop response with complete information.
     *
     * @param id Unique identifier
     * @param razaoSocial Legal company name
     * @param nomeFantasia Trade name
     * @param cnpj Brazilian company registration number
     * @param email Contact email
     * @param telefone Contact phone
     * @param status Current subscription status
     * @param plano Subscription plan tier
     * @param mensalidade Monthly fee
     * @param dataInicioTrial Trial start date (null if never in trial)
     * @param dataFimTrial Trial end date (null if never in trial)
     * @param diasRestantesTrial Days remaining in trial (null if not in trial)
     * @param dataVencimento Next payment due date
     * @param ativa Whether workshop is active in the system
     * @param totalUsuarios Number of registered users
     * @param totalClientes Number of customers
     * @param totalVeiculos Number of vehicles
     * @param totalOrdensServico Number of service orders
     * @param totalPecas Number of parts in inventory
     * @param totalFaturamento Total revenue generated
     * @param pagamentosRealizados Count of completed payments
     * @param pagamentosPendentes Count of pending/overdue payments
     * @param ultimoPagamento Date of last payment
     * @param createdAt Workshop registration timestamp
     * @param updatedAt Last update timestamp
     */
    public OficinaDetailResponse {
        // Compact constructor
    }
}
