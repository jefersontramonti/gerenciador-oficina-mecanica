package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.StatusNotaFiscal;
import com.pitstop.financeiro.domain.TipoNotaFiscal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para Nota Fiscal.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-23
 */
public record NotaFiscalResponseDTO(
    UUID id,
    UUID ordemServicoId,
    TipoNotaFiscal tipo,
    StatusNotaFiscal status,
    Long numero,
    Integer serie,
    String chaveAcesso,
    String protocoloAutorizacao,
    LocalDateTime dataHoraAutorizacao,
    BigDecimal valorTotal,
    String naturezaOperacao,
    String cfop,
    String informacoesComplementares,
    LocalDateTime dataEmissao,
    String protocoloCancelamento,
    LocalDateTime dataHoraCancelamento,
    String justificativaCancelamento,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
