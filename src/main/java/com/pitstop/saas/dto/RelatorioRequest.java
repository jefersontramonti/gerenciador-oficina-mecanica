package com.pitstop.saas.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request para geração de relatórios customizados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioRequest {

    @NotNull(message = "Tipo do relatório é obrigatório")
    private TipoRelatorio tipo;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    private LocalDate dataFim;

    // Filtros opcionais
    private List<UUID> oficinaIds;
    private List<String> planoIds;
    private List<String> status;

    // Agrupamento
    private AgrupamentoRelatorio agrupamento;

    // Formato de exportação
    private FormatoExport formato;

    // Opções específicas por tipo de relatório
    private Boolean incluirGraficos;
    private Boolean incluirDetalhamento;
    private Boolean incluirComparativo;

    public enum TipoRelatorio {
        FINANCEIRO,
        OPERACIONAL,
        CRESCIMENTO,
        INADIMPLENCIA,
        CUSTOMIZADO
    }

    public enum AgrupamentoRelatorio {
        DIA,
        SEMANA,
        MES,
        TRIMESTRE,
        ANO
    }

    public enum FormatoExport {
        PDF,
        EXCEL,
        CSV,
        JSON
    }
}
