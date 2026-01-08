package com.pitstop.ia.dto;

import java.util.List;

/**
 * Histórico do veículo comprimido para envio à IA.
 * Formato otimizado para reduzir tokens.
 */
public record HistoricoComprimido(
        DadosVeiculo veiculo,
        List<OSRecente> ordensRecentes,
        ResumoEstatistico resumoEstatistico
) {
    /**
     * Dados básicos do veículo.
     */
    public record DadosVeiculo(
            String marca,
            String modelo,
            Integer ano,
            Integer quilometragem,
            String combustivel
    ) {}

    /**
     * OS recente resumida.
     */
    public record OSRecente(
            String data,
            String problema,
            String diagnostico,
            String solucao
    ) {}

    /**
     * Resumo estatístico do histórico.
     */
    public record ResumoEstatistico(
            Integer totalOS,
            Integer problemasMotor,
            Integer problemasFreio,
            Integer problemasSuspensao,
            Integer problemasEletrico,
            Integer problemasOutros,
            String ultimaTrocaOleo,
            String ultimaRevisao
    ) {}

    /**
     * Converte para formato compacto (pipe-separated) para reduzir tokens.
     */
    public String toFormatoCompacto() {
        StringBuilder sb = new StringBuilder();

        // Veículo
        sb.append("VEICULO: ").append(veiculo.marca()).append("|")
                .append(veiculo.modelo()).append("|")
                .append(veiculo.ano()).append("|")
                .append(veiculo.quilometragem()).append("km|")
                .append(veiculo.combustivel()).append("\n");

        // OS recentes
        if (ordensRecentes != null && !ordensRecentes.isEmpty()) {
            sb.append("HISTORICO:\n");
            for (OSRecente os : ordensRecentes) {
                sb.append("- ").append(os.data()).append("|")
                        .append(os.problema()).append("|")
                        .append(os.solucao()).append("\n");
            }
        }

        // Estatísticas
        if (resumoEstatistico != null) {
            sb.append("STATS: ");
            sb.append("total=").append(resumoEstatistico.totalOS());
            if (resumoEstatistico.problemasMotor() > 0) {
                sb.append("|motor=").append(resumoEstatistico.problemasMotor());
            }
            if (resumoEstatistico.problemasFreio() > 0) {
                sb.append("|freio=").append(resumoEstatistico.problemasFreio());
            }
            if (resumoEstatistico.problemasSuspensao() > 0) {
                sb.append("|suspensao=").append(resumoEstatistico.problemasSuspensao());
            }
            if (resumoEstatistico.problemasEletrico() > 0) {
                sb.append("|eletrico=").append(resumoEstatistico.problemasEletrico());
            }
        }

        return sb.toString();
    }
}
