package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for defaulting workshop information.
 * Shows workshop details along with their overdue invoice summary.
 */
public record OficinaInadimplenteDTO(
    UUID oficinaId,
    String nomeFantasia,
    String cnpj,
    String email,
    String telefone,
    String planoNome,

    // Default status
    Integer faturasVencidas,
    BigDecimal valorTotalDevido,
    LocalDate faturaMaisAntigaVencimento,
    Integer diasAtrasoMaior,

    // History
    Integer notificacoesEnviadas,
    LocalDateTime ultimaNotificacao,

    // Agreements
    boolean possuiAcordoAtivo,
    BigDecimal valorAcordoAtivo,

    // Last activity
    LocalDateTime ultimoAcesso,

    // Actions available
    boolean podeNotificar,
    boolean podeSuspender,
    boolean podeCancelar,

    // Overdue invoices summary
    List<FaturaVencidaResumoDTO> faturasVencidasList
) {
    /**
     * Simplified overdue invoice info.
     */
    public record FaturaVencidaResumoDTO(
        UUID faturaId,
        String numero,
        BigDecimal valor,
        LocalDate dataVencimento,
        Integer diasAtraso,
        String mesReferencia
    ) {}
}
