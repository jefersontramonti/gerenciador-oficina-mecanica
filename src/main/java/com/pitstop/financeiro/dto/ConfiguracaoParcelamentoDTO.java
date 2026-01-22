package com.pitstop.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta para configuração de parcelamento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoParcelamentoDTO {

    private UUID id;
    private Integer parcelasMaximas;
    private BigDecimal valorMinimoParcela;
    private BigDecimal valorMinimoParcelamento;

    // Bandeiras aceitas
    private Boolean aceitaVisa;
    private Boolean aceitaMastercard;
    private Boolean aceitaElo;
    private Boolean aceitaAmex;
    private Boolean aceitaHipercard;

    // Exibição
    private Boolean exibirValorTotal;
    private Boolean exibirJuros;

    private Boolean ativo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Lista de faixas de juros configuradas.
     */
    private List<TabelaJurosDTO> faixasJuros;

    /**
     * Lista de bandeiras aceitas (para exibição).
     */
    private List<String> bandeirasAceitas;
}
