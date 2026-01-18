package com.pitstop.dashboard.dto;

/**
 * DTO para alertas dinâmicos do dashboard.
 * Contém informações sobre itens que requerem atenção imediata.
 *
 * @param pagamentosVencidos quantidade de pagamentos vencidos
 * @param manutencoesAtrasadas quantidade de alertas de manutenção atrasadas
 * @param pecasCriticas quantidade de peças com estoque crítico (zerado)
 * @param planosManutencaoAtivos quantidade de planos de manutenção ativos
 *
 * @author PitStop Team
 * @since 2026-01-18
 */
public record DashboardAlertasDTO(
        Long pagamentosVencidos,
        Long manutencoesAtrasadas,
        Long pecasCriticas,
        Long planosManutencaoAtivos
) {}
