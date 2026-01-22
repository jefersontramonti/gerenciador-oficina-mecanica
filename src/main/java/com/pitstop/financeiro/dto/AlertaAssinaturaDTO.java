package com.pitstop.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para alertas inteligentes de Assinaturas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaAssinaturaDTO {

    private TipoAlerta tipo;
    private NivelAlerta nivel;
    private String mensagem;
    private BigDecimal valor;
    private Integer quantidade;
    private String sugestao;

    public enum TipoAlerta {
        FATURAS_VENCIDAS,
        FATURAS_VENCENDO,
        ASSINATURAS_INADIMPLENTES,
        ASSINATURAS_CANCELADAS_MES,
        RECEITA_RECORRENTE_BAIXA,
        CHURN_ALTO,
        SEM_ASSINATURAS_ATIVAS
    }

    public enum NivelAlerta {
        INFO,
        WARNING,
        CRITICAL
    }

    // Factory methods

    public static AlertaAssinaturaDTO faturasVencidas(int quantidade, BigDecimal valorTotal) {
        return AlertaAssinaturaDTO.builder()
                .tipo(TipoAlerta.FATURAS_VENCIDAS)
                .nivel(NivelAlerta.CRITICAL)
                .mensagem(String.format("%d fatura(s) vencida(s) totalizando R$ %.2f", quantidade, valorTotal))
                .valor(valorTotal)
                .quantidade(quantidade)
                .sugestao("Entre em contato com os clientes para regularizar os pagamentos pendentes")
                .build();
    }

    public static AlertaAssinaturaDTO faturasVencendo(int quantidade, BigDecimal valorTotal, int dias) {
        return AlertaAssinaturaDTO.builder()
                .tipo(TipoAlerta.FATURAS_VENCENDO)
                .nivel(NivelAlerta.WARNING)
                .mensagem(String.format("%d fatura(s) vencem nos próximos %d dias (R$ %.2f)", quantidade, dias, valorTotal))
                .valor(valorTotal)
                .quantidade(quantidade)
                .sugestao("Envie lembretes de pagamento aos clientes")
                .build();
    }

    public static AlertaAssinaturaDTO assinaturasInadimplentes(int quantidade) {
        return AlertaAssinaturaDTO.builder()
                .tipo(TipoAlerta.ASSINATURAS_INADIMPLENTES)
                .nivel(NivelAlerta.CRITICAL)
                .mensagem(String.format("%d assinatura(s) inadimplente(s)", quantidade))
                .quantidade(quantidade)
                .sugestao("Tome ações de cobrança ou considere suspender os serviços")
                .build();
    }

    public static AlertaAssinaturaDTO assinaturasCanceladasMes(int quantidade, BigDecimal receitaPerdida) {
        NivelAlerta nivel = quantidade >= 3 ? NivelAlerta.CRITICAL :
                quantidade >= 1 ? NivelAlerta.WARNING : NivelAlerta.INFO;

        return AlertaAssinaturaDTO.builder()
                .tipo(TipoAlerta.ASSINATURAS_CANCELADAS_MES)
                .nivel(nivel)
                .mensagem(String.format("%d assinatura(s) cancelada(s) este mês (R$ %.2f/mês perdidos)", quantidade, receitaPerdida))
                .valor(receitaPerdida)
                .quantidade(quantidade)
                .sugestao("Analise os motivos de cancelamento e implemente ações de retenção")
                .build();
    }

    public static AlertaAssinaturaDTO receitaRecorrenteBaixa(BigDecimal mrr, BigDecimal meta) {
        BigDecimal percentualMeta = mrr.multiply(BigDecimal.valueOf(100)).divide(meta, 2, java.math.RoundingMode.HALF_UP);

        return AlertaAssinaturaDTO.builder()
                .tipo(TipoAlerta.RECEITA_RECORRENTE_BAIXA)
                .nivel(percentualMeta.compareTo(BigDecimal.valueOf(50)) < 0 ? NivelAlerta.WARNING : NivelAlerta.INFO)
                .mensagem(String.format("MRR de R$ %.2f está em %.0f%% da meta", mrr, percentualMeta))
                .valor(mrr)
                .sugestao("Intensifique a captação de novos assinantes")
                .build();
    }

    public static AlertaAssinaturaDTO churnAlto(BigDecimal taxaChurn) {
        NivelAlerta nivel = taxaChurn.compareTo(BigDecimal.valueOf(10)) > 0 ? NivelAlerta.CRITICAL :
                taxaChurn.compareTo(BigDecimal.valueOf(5)) > 0 ? NivelAlerta.WARNING : NivelAlerta.INFO;

        return AlertaAssinaturaDTO.builder()
                .tipo(TipoAlerta.CHURN_ALTO)
                .nivel(nivel)
                .mensagem(String.format("Taxa de cancelamento de %.1f%% no mês (meta: < 5%%)", taxaChurn))
                .valor(taxaChurn)
                .sugestao("Investigue os motivos de cancelamento e melhore a experiência do cliente")
                .build();
    }

    public static AlertaAssinaturaDTO semAssinaturasAtivas() {
        return AlertaAssinaturaDTO.builder()
                .tipo(TipoAlerta.SEM_ASSINATURAS_ATIVAS)
                .nivel(NivelAlerta.INFO)
                .mensagem("Nenhuma assinatura ativa no momento")
                .quantidade(0)
                .sugestao("Comece a oferecer planos de assinatura para fidelizar clientes")
                .build();
    }
}
