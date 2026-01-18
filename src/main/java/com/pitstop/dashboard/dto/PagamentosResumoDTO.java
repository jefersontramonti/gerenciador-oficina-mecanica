package com.pitstop.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para resumo de pagamentos do widget expansível.
 *
 * @param recebidoMes valor total recebido no mês atual
 * @param pendentesCount quantidade de pagamentos pendentes
 * @param pendentesValor valor total de pagamentos pendentes
 * @param vencidosCount quantidade de pagamentos vencidos
 * @param vencidosValor valor total de pagamentos vencidos
 * @param porTipo lista de pagamentos agrupados por tipo
 * @param vencidosLista lista dos últimos pagamentos vencidos
 *
 * @author PitStop Team
 * @since 2026-01-18
 */
public record PagamentosResumoDTO(
        BigDecimal recebidoMes,
        Long pendentesCount,
        BigDecimal pendentesValor,
        Long vencidosCount,
        BigDecimal vencidosValor,
        List<PagamentoPorTipoDTO> porTipo,
        List<PagamentoVencidoDTO> vencidosLista
) {}
