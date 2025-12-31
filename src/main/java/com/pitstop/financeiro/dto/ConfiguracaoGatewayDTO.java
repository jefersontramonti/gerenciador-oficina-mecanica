package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.AmbienteGateway;
import com.pitstop.financeiro.domain.TipoGateway;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para representar configuração de gateway (resposta).
 * Não expõe credenciais sensíveis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoGatewayDTO {
    private UUID id;
    private TipoGateway tipoGateway;
    private String tipoGatewayDescricao;
    private AmbienteGateway ambiente;
    private String ambienteDescricao;
    private Boolean ativo;
    private Boolean padrao;
    private Boolean configurado; // Se tem credenciais configuradas
    private BigDecimal taxaPercentual;
    private BigDecimal taxaFixa;
    private String webhookUrl;
    private String statusValidacao;
    private LocalDateTime dataUltimaValidacao;
    private String observacoes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
