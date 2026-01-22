package com.pitstop.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para DRE (Demonstração do Resultado do Exercício) simplificado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DRESimplificadoDTO {

    private Integer mes;
    private Integer ano;
    private String periodo; // "Janeiro/2026"

    // RECEITAS
    private BigDecimal receitaBrutaServicos;
    private BigDecimal receitaBrutaPecas;
    private BigDecimal outrasReceitas;
    private BigDecimal receitaBrutaTotal;

    // DEDUÇÕES
    private BigDecimal descontosConcedidos;
    private BigDecimal cancelamentos;
    private BigDecimal deducoesTotal;

    // RECEITA LÍQUIDA
    private BigDecimal receitaLiquida;

    // CUSTOS
    private BigDecimal custoPecasVendidas;
    private BigDecimal custoMaoObra;
    private BigDecimal custosTotal;

    // LUCRO BRUTO
    private BigDecimal lucroBruto;
    private BigDecimal margemBruta; // percentual

    // DESPESAS OPERACIONAIS
    private BigDecimal despesasAdministrativas;
    private BigDecimal despesasPessoal;
    private BigDecimal despesasMarketing;
    private BigDecimal outrasDespesas;
    private BigDecimal despesasOperacionaisTotal;

    // RESULTADO OPERACIONAL (EBIT)
    private BigDecimal resultadoOperacional;
    private BigDecimal margemOperacional; // percentual

    // RESULTADO FINANCEIRO
    private BigDecimal receitasFinanceiras;
    private BigDecimal despesasFinanceiras;
    private BigDecimal resultadoFinanceiro;

    // RESULTADO ANTES DOS IMPOSTOS
    private BigDecimal resultadoAntesImpostos;

    // IMPOSTOS
    private BigDecimal impostos;

    // LUCRO LÍQUIDO
    private BigDecimal lucroLiquido;
    private BigDecimal margemLiquida; // percentual

    // Comparativo
    private ComparativoDTO comparativoMesAnterior;
    private ComparativoDTO comparativoAnoAnterior;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparativoDTO {
        private BigDecimal receitaAnterior;
        private BigDecimal lucroAnterior;
        private BigDecimal variacaoReceita; // percentual
        private BigDecimal variacaoLucro; // percentual
    }

    // Alertas inteligentes
    private List<AlertaDREDTO> alertas;

    // Detalhamento por categoria
    private List<LinhaDREDTO> linhasDetalhadas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinhaDREDTO {
        private String descricao;
        private String grupo;
        private BigDecimal valor;
        private BigDecimal percentualReceita;
        private Integer ordem;
        private Boolean destaque;
    }
}
