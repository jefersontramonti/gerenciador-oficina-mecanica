package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.TipoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO para criar ou atualizar um pagamento.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Schema(description = "Dados para criar ou atualizar um pagamento")
public record PagamentoRequestDTO(

    @Schema(description = "ID da ordem de serviço", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    @NotNull(message = "ID da ordem de serviço é obrigatório")
    UUID ordemServicoId,

    @Schema(description = "Tipo de pagamento", example = "PIX", required = true)
    @NotNull(message = "Tipo de pagamento é obrigatório")
    TipoPagamento tipo,

    @Schema(description = "Valor do pagamento", example = "1250.50", required = true)
    @NotNull(message = "Valor do pagamento é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    BigDecimal valor,

    @Schema(description = "Número de parcelas (1 para à vista)", example = "1", required = true)
    @NotNull(message = "Número de parcelas é obrigatório")
    @Min(value = 1, message = "Número mínimo de parcelas é 1")
    @Max(value = 12, message = "Número máximo de parcelas é 12")
    Integer parcelas,

    @Schema(description = "Parcela atual (para parcelado)", example = "1")
    @Min(value = 1, message = "Parcela atual mínima é 1")
    Integer parcelaAtual,

    @Schema(description = "Data de vencimento", example = "2025-12-01")
    LocalDate dataVencimento,

    @Schema(description = "Observações sobre o pagamento", example = "Pago com desconto de 5%")
    @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres")
    String observacao

) {

    /**
     * Construtor para pagamento à vista sem data de vencimento.
     *
     * @param ordemServicoId ID da OS
     * @param tipo tipo de pagamento
     * @param valor valor
     */
    public PagamentoRequestDTO(UUID ordemServicoId, TipoPagamento tipo, BigDecimal valor) {
        this(ordemServicoId, tipo, valor, 1, 1, null, null);
    }

    /**
     * Construtor para pagamento parcelado.
     *
     * @param ordemServicoId ID da OS
     * @param tipo tipo de pagamento
     * @param valor valor da parcela
     * @param parcelas total de parcelas
     * @param parcelaAtual número da parcela atual
     * @param dataVencimento data de vencimento
     */
    public PagamentoRequestDTO(
        UUID ordemServicoId,
        TipoPagamento tipo,
        BigDecimal valor,
        Integer parcelas,
        Integer parcelaAtual,
        LocalDate dataVencimento
    ) {
        this(ordemServicoId, tipo, valor, parcelas, parcelaAtual, dataVencimento, null);
    }
}
