package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.AmbienteGateway;
import com.pitstop.financeiro.domain.TipoGateway;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO para criar/atualizar configuração de gateway.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoGatewayRequestDTO {

    @NotNull(message = "Tipo do gateway é obrigatório")
    private TipoGateway tipoGateway;

    @NotNull(message = "Ambiente é obrigatório")
    private AmbienteGateway ambiente;

    /**
     * Access Token (chave privada) do gateway.
     */
    private String accessToken;

    /**
     * Public Key (chave pública) do gateway.
     */
    private String publicKey;

    /**
     * Client ID (se aplicável).
     */
    private String clientId;

    /**
     * Client Secret (se aplicável).
     */
    private String clientSecret;

    /**
     * Se o gateway deve ser ativado.
     */
    private Boolean ativo;

    /**
     * Se deve ser o gateway padrão.
     */
    private Boolean padrao;

    /**
     * Taxa percentual cobrada.
     */
    private BigDecimal taxaPercentual;

    /**
     * Taxa fixa por transação.
     */
    private BigDecimal taxaFixa;

    /**
     * Observações.
     */
    private String observacoes;
}
