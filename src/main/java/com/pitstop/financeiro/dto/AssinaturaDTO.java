package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.PeriodicidadeAssinatura;
import com.pitstop.financeiro.domain.StatusAssinatura;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para assinatura de cliente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssinaturaDTO {

    private UUID id;

    @NotNull(message = "Cliente é obrigatório")
    private UUID clienteId;
    private String clienteNome;
    private String clienteTelefone;

    @NotNull(message = "Plano é obrigatório")
    private UUID planoId;
    private String planoNome;
    private PeriodicidadeAssinatura planoPeriodicidade;

    private StatusAssinatura status;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    private LocalDate dataFim;
    private LocalDate dataProximoVencimento;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valorAtual;

    // Configurações
    @Min(value = 1, message = "Dia de vencimento deve ser entre 1 e 28")
    @Max(value = 28, message = "Dia de vencimento deve ser entre 1 e 28")
    private Integer diaVencimento;

    @Min(value = 1, message = "Tolerância deve ser pelo menos 1 dia")
    @Max(value = 30, message = "Tolerância máxima é 30 dias")
    private Integer toleranciaDias;

    // Controle de uso
    private Integer osUtilizadasMes;
    private Integer limiteOsMes;
    private LocalDate mesReferencia;

    // Histórico
    private String motivoCancelamento;
    private LocalDateTime dataCancelamento;

    // Gateway
    private String gatewaySubscriptionId;

    // Calculados
    private BigDecimal proximaCobranca;
    private Boolean dentroDoLimite;
    private Integer diasAteVencimento;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * DTO para criação de assinatura.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAssinaturaDTO {
        @NotNull(message = "Cliente é obrigatório")
        private UUID clienteId;

        @NotNull(message = "Plano é obrigatório")
        private UUID planoId;

        @NotNull(message = "Data de início é obrigatória")
        private LocalDate dataInicio;

        private LocalDate dataFim;

        private BigDecimal valorCustomizado;

        @Min(value = 1, message = "Dia de vencimento deve ser entre 1 e 28")
        @Max(value = 28, message = "Dia de vencimento deve ser entre 1 e 28")
        private Integer diaVencimento;
    }

    /**
     * DTO para cancelamento.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelarAssinaturaDTO {
        @NotBlank(message = "Motivo é obrigatório")
        @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
        private String motivo;
    }
}
