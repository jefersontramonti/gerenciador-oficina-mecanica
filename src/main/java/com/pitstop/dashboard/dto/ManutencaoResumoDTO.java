package com.pitstop.dashboard.dto;

import java.util.List;

/**
 * DTO para resumo de manutenção preventiva do widget expansível.
 *
 * @param planosAtivos quantidade de planos de manutenção ativos
 * @param alertasPendentes quantidade de alertas pendentes de envio
 * @param planosVencidos quantidade de planos com manutenção vencida
 * @param proximasManutencoes lista das próximas manutenções (7 dias)
 *
 * @author PitStop Team
 * @since 2026-01-18
 */
public record ManutencaoResumoDTO(
        Long planosAtivos,
        Long alertasPendentes,
        Long planosVencidos,
        List<ProximaManutencaoDTO> proximasManutencoes
) {}
