package com.pitstop.manutencaopreventiva.dto;

import com.pitstop.manutencaopreventiva.domain.AgendamentoNotificacao;
import com.pitstop.manutencaopreventiva.domain.CriterioManutencao;
import com.pitstop.manutencaopreventiva.domain.TemplateManutencao;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO para criar/atualizar plano de manutenção preventiva.
 */
public record PlanoManutencaoRequestDTO(
    @NotNull(message = "Veículo é obrigatório")
    UUID veiculoId,

    UUID templateId,

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    String nome,

    String descricao,

    @NotBlank(message = "Tipo de manutenção é obrigatório")
    @Size(max = 50, message = "Tipo de manutenção deve ter no máximo 50 caracteres")
    String tipoManutencao,

    @NotNull(message = "Critério é obrigatório")
    CriterioManutencao criterio,

    @Min(value = 1, message = "Intervalo de dias deve ser maior que zero")
    Integer intervaloDias,

    @Min(value = 1, message = "Intervalo de km deve ser maior que zero")
    Integer intervaloKm,

    Integer antecedenciaDias,

    Integer antecedenciaKm,

    List<String> canaisNotificacao,

    LocalDate ultimaExecucaoData,

    Integer ultimaExecucaoKm,

    List<TemplateManutencao.ChecklistItem> checklist,

    List<TemplateManutencao.PecaSugerida> pecasSugeridas,

    @DecimalMin(value = "0.0", message = "Valor estimado não pode ser negativo")
    BigDecimal valorEstimado,

    /** Agendamentos de notificação personalizados (máximo 2) */
    @Size(max = 2, message = "Máximo de 2 agendamentos de notificação")
    List<AgendamentoNotificacao> agendamentosNotificacao
) {}
