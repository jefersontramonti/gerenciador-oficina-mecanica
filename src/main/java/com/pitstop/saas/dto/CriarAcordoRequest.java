package com.pitstop.saas.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request for creating a payment agreement (negotiation).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriarAcordoRequest {

    @NotEmpty(message = "Lista de faturas é obrigatória")
    private List<UUID> faturaIds;

    @NotNull(message = "Valor total acordado é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valorTotalAcordado;

    @NotNull(message = "Número de parcelas é obrigatório")
    @Min(value = 1, message = "Mínimo 1 parcela")
    @Max(value = 12, message = "Máximo 12 parcelas")
    private Integer numeroParcelas;

    @NotNull(message = "Data do primeiro vencimento é obrigatória")
    @FutureOrPresent(message = "Data deve ser hoje ou no futuro")
    private LocalDate primeiroVencimento;

    @DecimalMin(value = "0", message = "Desconto não pode ser negativo")
    @DecimalMax(value = "100", message = "Desconto não pode exceder 100%")
    private BigDecimal percentualDesconto;

    @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres")
    private String observacoes;

    private Boolean enviarNotificacao;
}
