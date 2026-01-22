package com.pitstop.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para alertas inteligentes do Fluxo de Caixa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaFluxoCaixaDTO {

    private TipoAlerta tipo;
    private NivelAlerta nivel;
    private String mensagem;
    private BigDecimal valor;
    private String sugestao;

    public enum TipoAlerta {
        SALDO_NEGATIVO,
        QUEIMANDO_CAIXA,
        RECEITA_EM_QUEDA,
        DESPESAS_CRESCENTES,
        SEM_RECEITAS,
        DIAS_NEGATIVOS,
        CONCENTRACAO_DESPESA
    }

    public enum NivelAlerta {
        INFO,
        WARNING,
        CRITICAL
    }

    // Factory methods

    public static AlertaFluxoCaixaDTO saldoNegativo(BigDecimal saldo) {
        return AlertaFluxoCaixaDTO.builder()
                .tipo(TipoAlerta.SALDO_NEGATIVO)
                .nivel(NivelAlerta.CRITICAL)
                .mensagem(String.format("Saldo negativo de R$ %.2f no período", saldo.abs()))
                .valor(saldo)
                .sugestao("Analise despesas que podem ser adiadas ou busque antecipar recebimentos")
                .build();
    }

    public static AlertaFluxoCaixaDTO queimandoCaixa(BigDecimal despesas, BigDecimal receitas) {
        BigDecimal diferenca = despesas.subtract(receitas);
        BigDecimal percentual = receitas.compareTo(BigDecimal.ZERO) > 0
                ? diferenca.multiply(BigDecimal.valueOf(100)).divide(receitas, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100);

        return AlertaFluxoCaixaDTO.builder()
                .tipo(TipoAlerta.QUEIMANDO_CAIXA)
                .nivel(percentual.compareTo(BigDecimal.valueOf(50)) > 0 ? NivelAlerta.CRITICAL : NivelAlerta.WARNING)
                .mensagem(String.format("Despesas superam receitas em R$ %.2f (%.1f%%)", diferenca, percentual))
                .valor(diferenca)
                .sugestao("Reduza despesas ou aumente esforços comerciais para gerar mais receita")
                .build();
    }

    public static AlertaFluxoCaixaDTO receitaEmQueda(BigDecimal variacaoPercentual) {
        NivelAlerta nivel = variacaoPercentual.compareTo(BigDecimal.valueOf(-30)) < 0
                ? NivelAlerta.CRITICAL
                : variacaoPercentual.compareTo(BigDecimal.valueOf(-15)) < 0
                ? NivelAlerta.WARNING
                : NivelAlerta.INFO;

        return AlertaFluxoCaixaDTO.builder()
                .tipo(TipoAlerta.RECEITA_EM_QUEDA)
                .nivel(nivel)
                .mensagem(String.format("Receitas caíram %.1f%% em relação ao período anterior", variacaoPercentual.abs()))
                .valor(variacaoPercentual)
                .sugestao("Investiga as causas da queda e considere ações promocionais ou de fidelização")
                .build();
    }

    public static AlertaFluxoCaixaDTO despesasCrescentes(BigDecimal variacaoPercentual) {
        return AlertaFluxoCaixaDTO.builder()
                .tipo(TipoAlerta.DESPESAS_CRESCENTES)
                .nivel(variacaoPercentual.compareTo(BigDecimal.valueOf(30)) > 0 ? NivelAlerta.WARNING : NivelAlerta.INFO)
                .mensagem(String.format("Despesas aumentaram %.1f%% em relação ao período anterior", variacaoPercentual))
                .valor(variacaoPercentual)
                .sugestao("Revise os gastos e identifique oportunidades de redução de custos")
                .build();
    }

    public static AlertaFluxoCaixaDTO semReceitas(int dias) {
        return AlertaFluxoCaixaDTO.builder()
                .tipo(TipoAlerta.SEM_RECEITAS)
                .nivel(NivelAlerta.WARNING)
                .mensagem(String.format("Nenhuma receita registrada nos últimos %d dias", dias))
                .valor(BigDecimal.valueOf(dias))
                .sugestao("Verifique se há pagamentos pendentes de registro ou se há problemas operacionais")
                .build();
    }

    public static AlertaFluxoCaixaDTO diasNegativos(int quantidadeDias, int totalDias) {
        BigDecimal percentual = BigDecimal.valueOf(quantidadeDias * 100.0 / totalDias);
        NivelAlerta nivel = percentual.compareTo(BigDecimal.valueOf(50)) > 0
                ? NivelAlerta.CRITICAL
                : percentual.compareTo(BigDecimal.valueOf(30)) > 0
                ? NivelAlerta.WARNING
                : NivelAlerta.INFO;

        return AlertaFluxoCaixaDTO.builder()
                .tipo(TipoAlerta.DIAS_NEGATIVOS)
                .nivel(nivel)
                .mensagem(String.format("%d de %d dias com saldo negativo (%.0f%%)", quantidadeDias, totalDias, percentual))
                .valor(percentual)
                .sugestao("Organize melhor o fluxo de pagamentos e recebimentos para evitar descobertos")
                .build();
    }

    public static AlertaFluxoCaixaDTO concentracaoDespesa(String categoria, BigDecimal percentual) {
        return AlertaFluxoCaixaDTO.builder()
                .tipo(TipoAlerta.CONCENTRACAO_DESPESA)
                .nivel(percentual.compareTo(BigDecimal.valueOf(50)) > 0 ? NivelAlerta.WARNING : NivelAlerta.INFO)
                .mensagem(String.format("%.1f%% das despesas concentradas em '%s'", percentual, categoria))
                .valor(percentual)
                .sugestao("Diversifique fornecedores ou negocie melhores condições")
                .build();
    }
}
