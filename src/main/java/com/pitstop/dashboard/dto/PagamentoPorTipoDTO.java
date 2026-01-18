package com.pitstop.dashboard.dto;

import com.pitstop.financeiro.domain.TipoPagamento;

import java.math.BigDecimal;

/**
 * DTO para estatísticas de pagamentos agrupados por tipo.
 *
 * @param tipo tipo de pagamento (PIX, DINHEIRO, etc.)
 * @param label label traduzido para exibição
 * @param quantidade quantidade de pagamentos
 * @param valorTotal valor total dos pagamentos
 * @param color cor hex para gráficos
 *
 * @author PitStop Team
 * @since 2026-01-18
 */
public record PagamentoPorTipoDTO(
        TipoPagamento tipo,
        String label,
        Long quantidade,
        BigDecimal valorTotal,
        String color
) {}
