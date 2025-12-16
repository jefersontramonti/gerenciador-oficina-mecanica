package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.StatusPagamento;
import com.pitstop.financeiro.domain.TipoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para pagamento.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Schema(description = "Dados de um pagamento")
public record PagamentoResponseDTO(

    @Schema(description = "ID do pagamento", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,

    @Schema(description = "ID da ordem de serviço", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID ordemServicoId,

    @Schema(description = "Tipo de pagamento", example = "PIX")
    TipoPagamento tipo,

    @Schema(description = "Status do pagamento", example = "PAGO")
    StatusPagamento status,

    @Schema(description = "Valor do pagamento", example = "1250.50")
    BigDecimal valor,

    @Schema(description = "Total de parcelas", example = "1")
    Integer parcelas,

    @Schema(description = "Parcela atual", example = "1")
    Integer parcelaAtual,

    @Schema(description = "Data de vencimento", example = "2025-12-01")
    LocalDate dataVencimento,

    @Schema(description = "Data efetiva do pagamento", example = "2025-11-15")
    LocalDate dataPagamento,

    @Schema(description = "Observações", example = "Pago com desconto de 5%")
    String observacao,

    @Schema(description = "ID da nota fiscal (se houver)", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID notaFiscalId,

    @Schema(description = "Data de criação", example = "2025-11-11T10:30:00")
    LocalDateTime createdAt,

    @Schema(description = "Data de atualização", example = "2025-11-11T10:30:00")
    LocalDateTime updatedAt

) {
}
