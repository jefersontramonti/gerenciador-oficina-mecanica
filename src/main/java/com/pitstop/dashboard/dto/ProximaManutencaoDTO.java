package com.pitstop.dashboard.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO para próximas manutenções preventivas.
 *
 * @param planoId ID do plano de manutenção
 * @param planoNome nome do plano
 * @param tipoManutencao tipo de manutenção (ex: Troca de óleo)
 * @param veiculoId ID do veículo
 * @param veiculoPlaca placa do veículo
 * @param veiculoModelo modelo do veículo (marca + modelo)
 * @param clienteNome nome do cliente proprietário
 * @param proximaData data prevista para a manutenção
 * @param proximaKm quilometragem prevista (pode ser null)
 * @param diasRestantes dias restantes até a manutenção (negativo = vencido)
 *
 * @author PitStop Team
 * @since 2026-01-18
 */
public record ProximaManutencaoDTO(
        UUID planoId,
        String planoNome,
        String tipoManutencao,
        UUID veiculoId,
        String veiculoPlaca,
        String veiculoModelo,
        String clienteNome,
        LocalDate proximaData,
        Integer proximaKm,
        Long diasRestantes
) {}
