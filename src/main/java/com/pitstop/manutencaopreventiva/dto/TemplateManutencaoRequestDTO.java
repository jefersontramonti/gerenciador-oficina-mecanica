package com.pitstop.manutencaopreventiva.dto;

import com.pitstop.manutencaopreventiva.domain.CriterioManutencao;
import com.pitstop.manutencaopreventiva.domain.TemplateManutencao;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO para criar/atualizar template de manutenção.
 */
public record TemplateManutencaoRequestDTO(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    String nome,

    String descricao,

    @NotBlank(message = "Tipo de manutenção é obrigatório")
    @Size(max = 50, message = "Tipo de manutenção deve ter no máximo 50 caracteres")
    String tipoManutencao,

    @Min(value = 1, message = "Intervalo de dias deve ser maior que zero")
    Integer intervaloDias,

    @Min(value = 1, message = "Intervalo de km deve ser maior que zero")
    Integer intervaloKm,

    @NotNull(message = "Critério é obrigatório")
    CriterioManutencao criterio,

    Integer antecedenciaDias,

    Integer antecedenciaKm,

    List<TemplateManutencao.ChecklistItem> checklist,

    List<PecaSugeridaDTO> pecasSugeridas,

    @DecimalMin(value = "0.0", message = "Valor estimado não pode ser negativo")
    BigDecimal valorEstimado,

    @Min(value = 1, message = "Tempo estimado deve ser maior que zero")
    Integer tempoEstimadoMinutos
) {
    public record PecaSugeridaDTO(
        UUID pecaId,
        Integer quantidade
    ) {}
}
