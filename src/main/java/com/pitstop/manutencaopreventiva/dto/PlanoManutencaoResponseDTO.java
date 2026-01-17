package com.pitstop.manutencaopreventiva.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.manutencaopreventiva.domain.AgendamentoNotificacao;
import com.pitstop.manutencaopreventiva.domain.CriterioManutencao;
import com.pitstop.manutencaopreventiva.domain.StatusPlanoManutencao;
import com.pitstop.manutencaopreventiva.domain.TemplateManutencao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta do plano de manutenção preventiva.
 */
public record PlanoManutencaoResponseDTO(
    UUID id,
    VeiculoResumoDTO veiculo,
    TemplateResumoDTO template,
    String nome,
    String descricao,
    String tipoManutencao,
    CriterioManutencao criterio,
    Integer intervaloDias,
    Integer intervaloKm,
    Integer antecedenciaDias,
    Integer antecedenciaKm,
    List<String> canaisNotificacao,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate ultimaExecucaoData,
    Integer ultimaExecucaoKm,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate proximaPrevisaoData,
    Integer proximaPrevisaoKm,

    StatusPlanoManutencao status,
    String motivoPausa,

    Boolean proximoAVencer,
    Boolean vencido,
    Integer diasParaVencer,

    List<TemplateManutencao.ChecklistItem> checklist,
    List<TemplateManutencao.PecaSugerida> pecasSugeridas,
    BigDecimal valorEstimado,

    /** Agendamentos de notificação personalizados */
    List<AgendamentoNotificacao> agendamentosNotificacao,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {
    /**
     * Resumo do veículo vinculado ao plano.
     */
    public record VeiculoResumoDTO(
        UUID id,
        String placa,
        String placaFormatada,
        String marca,
        String modelo,
        Integer ano,
        Integer quilometragem,
        String clienteNome
    ) {}

    /**
     * Resumo do template usado.
     */
    public record TemplateResumoDTO(
        UUID id,
        String nome,
        String tipoManutencao
    ) {}
}
