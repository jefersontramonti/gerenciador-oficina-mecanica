package com.pitstop.financeiro.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de request para configuração de parcelamento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoParcelamentoRequestDTO {

    @NotNull(message = "Parcelas máximas é obrigatório")
    @Min(value = 1, message = "Mínimo de 1 parcela")
    @Max(value = 24, message = "Máximo de 24 parcelas")
    @Builder.Default
    private Integer parcelasMaximas = 12;

    @NotNull(message = "Valor mínimo da parcela é obrigatório")
    @Builder.Default
    private BigDecimal valorMinimoParcela = new BigDecimal("50.00");

    @NotNull(message = "Valor mínimo para parcelamento é obrigatório")
    @Builder.Default
    private BigDecimal valorMinimoParcelamento = new BigDecimal("100.00");

    // Bandeiras aceitas
    @Builder.Default
    private Boolean aceitaVisa = true;

    @Builder.Default
    private Boolean aceitaMastercard = true;

    @Builder.Default
    private Boolean aceitaElo = true;

    @Builder.Default
    private Boolean aceitaAmex = true;

    @Builder.Default
    private Boolean aceitaHipercard = true;

    // Exibição
    @Builder.Default
    private Boolean exibirValorTotal = true;

    @Builder.Default
    private Boolean exibirJuros = true;

    @Builder.Default
    private Boolean ativo = true;
}
