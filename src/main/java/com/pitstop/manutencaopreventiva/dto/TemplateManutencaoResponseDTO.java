package com.pitstop.manutencaopreventiva.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.manutencaopreventiva.domain.CriterioManutencao;
import com.pitstop.manutencaopreventiva.domain.TemplateManutencao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta do template de manutenção.
 */
public record TemplateManutencaoResponseDTO(
    UUID id,
    UUID oficinaId,
    Boolean global,
    String nome,
    String descricao,
    String tipoManutencao,
    Integer intervaloDias,
    Integer intervaloKm,
    CriterioManutencao criterio,
    Integer antecedenciaDias,
    Integer antecedenciaKm,
    List<TemplateManutencao.ChecklistItem> checklist,
    List<TemplateManutencao.PecaSugerida> pecasSugeridas,
    BigDecimal valorEstimado,
    Integer tempoEstimadoMinutos,
    Boolean ativo,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {}
