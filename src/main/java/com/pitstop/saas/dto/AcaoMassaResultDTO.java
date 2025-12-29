package com.pitstop.saas.dto;

import java.util.List;
import java.util.UUID;

/**
 * Result of mass action execution.
 */
public record AcaoMassaResultDTO(
    int totalProcessadas,
    int totalSucesso,
    int totalFalha,
    List<ResultadoIndividualDTO> resultados
) {
    /**
     * Individual result for each workshop.
     */
    public record ResultadoIndividualDTO(
        UUID oficinaId,
        String oficinaNome,
        boolean sucesso,
        String mensagem
    ) {}
}
