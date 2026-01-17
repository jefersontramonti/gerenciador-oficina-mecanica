package com.pitstop.manutencaopreventiva.dto;

import com.pitstop.manutencaopreventiva.domain.HistoricoManutencaoPreventiva;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO para executar (registrar conclusão de) um plano de manutenção.
 */
public record ExecutarPlanoRequestDTO(
    @NotNull(message = "Data de execução é obrigatória")
    LocalDate dataExecucao,

    @Min(value = 0, message = "Km não pode ser negativo")
    Integer kmExecucao,

    UUID ordemServicoId,

    List<HistoricoManutencaoPreventiva.ChecklistExecutado> checklistExecutado,

    List<HistoricoManutencaoPreventiva.PecaUtilizada> pecasUtilizadas,

    @DecimalMin(value = "0.0", message = "Valor não pode ser negativo")
    BigDecimal valorMaoObra,

    @DecimalMin(value = "0.0", message = "Valor não pode ser negativo")
    BigDecimal valorPecas,

    String observacoes,

    String observacoesMecanico,

    Boolean criarOrdemServico
) {}
