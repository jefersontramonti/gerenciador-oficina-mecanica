package com.pitstop.ordemservico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO para finalização de Ordem de Serviço.
 *
 * <p>Usado para informar as horas trabalhadas no modelo de cobrança POR_HORA.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados para finalização de Ordem de Serviço")
public record FinalizarOSDTO(

    @Schema(description = "Horas efetivamente trabalhadas (obrigatório para modelo POR_HORA)",
            example = "4.5",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Horas trabalhadas é obrigatório")
    @DecimalMin(value = "0.5", message = "Mínimo 30 minutos (0.5 horas)")
    @DecimalMax(value = "100.0", message = "Máximo 100 horas")
    BigDecimal horasTrabalhadas,

    @Schema(description = "Observações finais sobre o serviço realizado",
            example = "Serviço concluído sem intercorrências. Peças trocadas conforme orçamento.")
    @Size(max = 1000, message = "Observações finais devem ter no máximo 1000 caracteres")
    String observacoesFinais

) {
}
