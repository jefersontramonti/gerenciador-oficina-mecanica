package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.StatusNotaFiscal;
import com.pitstop.financeiro.domain.TipoNotaFiscal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resumo para listagens de Nota Fiscal.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-23
 */
public record NotaFiscalResumoDTO(
    UUID id,
    Long numero,
    Integer serie,
    TipoNotaFiscal tipo,
    StatusNotaFiscal status,
    BigDecimal valorTotal,
    LocalDateTime dataEmissao,
    String chaveAcesso
) {
}
