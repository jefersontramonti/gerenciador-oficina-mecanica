package com.pitstop.saas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO containing detailed metrics for a specific workshop.
 *
 * Includes usage statistics, resource consumption, and recent activity.
 * Used in the workshop detail page for SUPER_ADMIN monitoring.
 *
 * @author PitStop Team
 */
public record OficinaMetricasDTO(
    // Limites e Uso
    Integer usuariosAtivos,
    Integer limiteUsuarios,
    Long espacoUsadoBytes,
    Long limiteEspacoBytes,
    Integer osNoMes,
    Integer limiteOSMes,

    // Estatísticas Gerais
    Integer clientesTotal,
    Integer veiculosTotal,
    Integer pecasTotal,
    BigDecimal faturamentoMes,
    BigDecimal faturamentoTotal,

    // Atividade
    LocalDateTime ultimoAcesso,
    Integer loginsUltimos30Dias,
    List<LoginHistoricoDTO> ultimosLogins,

    // Performance
    Integer osFinalizadasMes,
    Integer osCanceladasMes,
    BigDecimal ticketMedio,
    Double taxaConversao, // Orçamentos aprovados / total

    // Estoque
    Integer pecasEstoqueBaixo,
    BigDecimal valorEstoqueTotal
) {
    /**
     * Nested DTO for login history entries.
     */
    public record LoginHistoricoDTO(
        String usuarioNome,
        String usuarioEmail,
        LocalDateTime dataLogin,
        String ip,
        String dispositivo
    ) {}

    /**
     * Creates a metrics DTO with all workshop metrics.
     */
    public OficinaMetricasDTO {
        // Compact constructor for validation
    }
}
