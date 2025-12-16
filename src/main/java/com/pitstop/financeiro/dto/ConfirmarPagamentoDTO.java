package com.pitstop.financeiro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO para confirmar um pagamento.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Schema(description = "Dados para confirmar um pagamento")
public record ConfirmarPagamentoDTO(

    @Schema(description = "Data efetiva do pagamento", example = "2025-11-15", required = true)
    @NotNull(message = "Data de pagamento é obrigatória")
    LocalDate dataPagamento,

    @Schema(description = "Comprovante de pagamento (base64 ou path)", example = "/uploads/comprovantes/pix-123.jpg")
    String comprovante

) {
}
