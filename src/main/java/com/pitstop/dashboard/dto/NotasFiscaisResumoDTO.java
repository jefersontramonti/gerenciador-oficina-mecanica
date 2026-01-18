package com.pitstop.dashboard.dto;

/**
 * DTO para resumo de notas fiscais do widget expansível.
 *
 * @param emitidasMes quantidade de notas emitidas no mês
 * @param rascunhosCount quantidade de notas em rascunho (pendentes)
 * @param canceladasMes quantidade de notas canceladas no mês
 *
 * @author PitStop Team
 * @since 2026-01-18
 */
public record NotasFiscaisResumoDTO(
        Long emitidasMes,
        Long rascunhosCount,
        Long canceladasMes
) {}
