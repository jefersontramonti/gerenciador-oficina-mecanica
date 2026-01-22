package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.TipoJuros;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de request para criar/atualizar tabela de juros.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TabelaJurosRequestDTO {

    @NotNull(message = "Parcelas mínimas é obrigatório")
    @Min(value = 1, message = "Mínimo de 1 parcela")
    @Max(value = 24, message = "Máximo de 24 parcelas")
    private Integer parcelasMinimo;

    @NotNull(message = "Parcelas máximas é obrigatório")
    @Min(value = 1, message = "Mínimo de 1 parcela")
    @Max(value = 24, message = "Máximo de 24 parcelas")
    private Integer parcelasMaximo;

    @NotNull(message = "Percentual de juros é obrigatório")
    @Min(value = 0, message = "Juros não pode ser negativo")
    @Builder.Default
    private BigDecimal percentualJuros = BigDecimal.ZERO;

    @NotNull(message = "Tipo de juros é obrigatório")
    @Builder.Default
    private TipoJuros tipoJuros = TipoJuros.SEM_JUROS;

    @Builder.Default
    private Boolean repassarCliente = true;

    @Builder.Default
    private Boolean ativo = true;
}
