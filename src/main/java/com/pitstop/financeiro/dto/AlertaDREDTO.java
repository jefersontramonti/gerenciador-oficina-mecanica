package com.pitstop.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para alertas inteligentes do DRE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaDREDTO {

    /**
     * Tipo do alerta para categorização.
     */
    private TipoAlerta tipo;

    /**
     * Nível de severidade do alerta.
     */
    private NivelAlerta nivel;

    /**
     * Mensagem descritiva do alerta.
     */
    private String mensagem;

    /**
     * Valor numérico relacionado ao alerta (ex: percentual de margem).
     */
    private BigDecimal valor;

    /**
     * Limite/threshold que foi ultrapassado.
     */
    private BigDecimal limite;

    /**
     * Sugestão de ação para resolver o alerta.
     */
    private String sugestao;

    /**
     * Tipos de alerta do DRE.
     */
    public enum TipoAlerta {
        MARGEM_BRUTA_BAIXA,
        MARGEM_OPERACIONAL_BAIXA,
        MARGEM_LIQUIDA_NEGATIVA,
        CMV_ALTO,
        DESPESAS_OPERACIONAIS_ALTAS,
        RESULTADO_OPERACIONAL_NEGATIVO,
        LUCRO_LIQUIDO_NEGATIVO,
        DESPESAS_PESSOAL_ALTAS,
        CUSTO_MAO_OBRA_ALTO,
        SEM_RECEITA,
        DEDUCOES_ALTAS
    }

    /**
     * Níveis de severidade do alerta.
     */
    public enum NivelAlerta {
        INFO,      // Informativo
        WARNING,   // Atenção
        CRITICAL   // Crítico
    }

    // Factory methods para criar alertas comuns

    public static AlertaDREDTO margemBrutaBaixa(BigDecimal margem) {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.MARGEM_BRUTA_BAIXA)
                .nivel(margem.compareTo(BigDecimal.ZERO) < 0 ? NivelAlerta.CRITICAL : NivelAlerta.WARNING)
                .mensagem(String.format("Margem bruta de %.1f%% está abaixo do recomendado (mínimo 20%%)", margem))
                .valor(margem)
                .limite(new BigDecimal("20"))
                .sugestao("Revise a precificação dos serviços e negocie melhores preços com fornecedores de peças")
                .build();
    }

    public static AlertaDREDTO margemOperacionalBaixa(BigDecimal margem) {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.MARGEM_OPERACIONAL_BAIXA)
                .nivel(margem.compareTo(BigDecimal.ZERO) < 0 ? NivelAlerta.CRITICAL : NivelAlerta.WARNING)
                .mensagem(String.format("Margem operacional de %.1f%% está abaixo do recomendado (mínimo 10%%)", margem))
                .valor(margem)
                .limite(new BigDecimal("10"))
                .sugestao("Analise as despesas operacionais e busque reduzir custos fixos")
                .build();
    }

    public static AlertaDREDTO margemLiquidaNegativa(BigDecimal margem) {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.MARGEM_LIQUIDA_NEGATIVA)
                .nivel(NivelAlerta.CRITICAL)
                .mensagem(String.format("Margem líquida negativa de %.1f%% - a oficina está operando com prejuízo", margem))
                .valor(margem)
                .limite(BigDecimal.ZERO)
                .sugestao("É urgente revisar a estrutura de custos e aumentar as receitas")
                .build();
    }

    public static AlertaDREDTO cmvAlto(BigDecimal percentualCMV) {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.CMV_ALTO)
                .nivel(percentualCMV.compareTo(new BigDecimal("95")) >= 0 ? NivelAlerta.CRITICAL : NivelAlerta.WARNING)
                .mensagem(String.format("Custo das peças representa %.1f%% da receita de peças (máximo recomendado: 70%%)", percentualCMV))
                .valor(percentualCMV)
                .limite(new BigDecimal("70"))
                .sugestao("Aumente o markup das peças ou negocie melhores preços com fornecedores")
                .build();
    }

    public static AlertaDREDTO despesasOperacionaisAltas(BigDecimal percentual) {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.DESPESAS_OPERACIONAIS_ALTAS)
                .nivel(percentual.compareTo(new BigDecimal("60")) >= 0 ? NivelAlerta.CRITICAL : NivelAlerta.WARNING)
                .mensagem(String.format("Despesas operacionais representam %.1f%% da receita líquida (máximo recomendado: 40%%)", percentual))
                .valor(percentual)
                .limite(new BigDecimal("40"))
                .sugestao("Identifique despesas que podem ser reduzidas ou eliminadas")
                .build();
    }

    public static AlertaDREDTO resultadoOperacionalNegativo(BigDecimal valor) {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.RESULTADO_OPERACIONAL_NEGATIVO)
                .nivel(NivelAlerta.CRITICAL)
                .mensagem("Resultado operacional negativo - a operação principal está gerando prejuízo")
                .valor(valor)
                .limite(BigDecimal.ZERO)
                .sugestao("A operação core do negócio precisa ser reestruturada urgentemente")
                .build();
    }

    public static AlertaDREDTO lucroLiquidoNegativo(BigDecimal valor) {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.LUCRO_LIQUIDO_NEGATIVO)
                .nivel(NivelAlerta.CRITICAL)
                .mensagem("Lucro líquido negativo - a oficina está operando com prejuízo no período")
                .valor(valor)
                .limite(BigDecimal.ZERO)
                .sugestao("Revise toda a estrutura de custos e despesas, e busque aumentar o faturamento")
                .build();
    }

    public static AlertaDREDTO despesasPessoalAltas(BigDecimal percentual) {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.DESPESAS_PESSOAL_ALTAS)
                .nivel(NivelAlerta.WARNING)
                .mensagem(String.format("Despesas com pessoal representam %.1f%% da receita líquida (máximo recomendado: 30%%)", percentual))
                .valor(percentual)
                .limite(new BigDecimal("30"))
                .sugestao("Avalie a produtividade da equipe e considere otimizar a folha de pagamento")
                .build();
    }

    public static AlertaDREDTO custoMaoObraAlto(BigDecimal percentual) {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.CUSTO_MAO_OBRA_ALTO)
                .nivel(NivelAlerta.WARNING)
                .mensagem(String.format("Custo de mão de obra representa %.1f%% da receita de serviços (máximo recomendado: 50%%)", percentual))
                .valor(percentual)
                .limite(new BigDecimal("50"))
                .sugestao("Revise a precificação da mão de obra ou otimize a produtividade dos mecânicos")
                .build();
    }

    public static AlertaDREDTO semReceita() {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.SEM_RECEITA)
                .nivel(NivelAlerta.INFO)
                .mensagem("Nenhuma receita registrada no período")
                .valor(BigDecimal.ZERO)
                .limite(BigDecimal.ZERO)
                .sugestao("Verifique se existem ordens de serviço entregues neste período")
                .build();
    }

    public static AlertaDREDTO deducoesAltas(BigDecimal percentual) {
        return AlertaDREDTO.builder()
                .tipo(TipoAlerta.DEDUCOES_ALTAS)
                .nivel(NivelAlerta.WARNING)
                .mensagem(String.format("Deduções (descontos e cancelamentos) representam %.1f%% da receita bruta (máximo recomendado: 5%%)", percentual))
                .valor(percentual)
                .limite(new BigDecimal("5"))
                .sugestao("Revise a política de descontos e analise as causas dos cancelamentos")
                .build();
    }
}
