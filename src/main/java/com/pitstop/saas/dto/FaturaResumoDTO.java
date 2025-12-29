package com.pitstop.saas.dto;

import com.pitstop.saas.domain.Fatura;
import com.pitstop.saas.domain.StatusFatura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Simplified DTO for invoice list views.
 */
public record FaturaResumoDTO(
    UUID id,
    String numero,

    // Workshop info
    UUID oficinaId,
    String oficinaNome,

    // Status
    StatusFatura status,
    String statusLabel,
    String statusCor,

    // Values
    BigDecimal valorTotal,

    // Dates
    LocalDate mesReferencia,
    String mesReferenciaFormatado,
    LocalDate dataEmissao,
    LocalDate dataVencimento,
    LocalDateTime dataPagamento,
    Long diasAteVencimento,

    // Flags
    boolean vencida,
    boolean pagavel
) {
    /**
     * Convert entity to DTO.
     */
    public static FaturaResumoDTO fromEntity(Fatura fatura) {
        return new FaturaResumoDTO(
            fatura.getId(),
            fatura.getNumero(),
            fatura.getOficina().getId(),
            fatura.getOficina().getNomeFantasia(),
            fatura.getStatus(),
            fatura.getStatus().getLabel(),
            fatura.getStatus().getCor(),
            fatura.getValorTotal(),
            fatura.getMesReferencia(),
            fatura.getMesReferenciaFormatado(),
            fatura.getDataEmissao(),
            fatura.getDataVencimento(),
            fatura.getDataPagamento(),
            fatura.getDiasAteVencimento(),
            fatura.isVencida(),
            fatura.isPagavel()
        );
    }
}
