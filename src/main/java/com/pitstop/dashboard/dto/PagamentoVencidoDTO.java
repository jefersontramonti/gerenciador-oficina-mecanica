package com.pitstop.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO para pagamentos vencidos exibidos no widget.
 *
 * @param id ID do pagamento
 * @param clienteNome nome do cliente
 * @param valor valor do pagamento
 * @param dataVencimento data de vencimento
 * @param diasVencido quantidade de dias vencido
 *
 * @author PitStop Team
 * @since 2026-01-18
 */
public record PagamentoVencidoDTO(
        UUID id,
        String clienteNome,
        BigDecimal valor,
        LocalDate dataVencimento,
        Long diasVencido
) {}
