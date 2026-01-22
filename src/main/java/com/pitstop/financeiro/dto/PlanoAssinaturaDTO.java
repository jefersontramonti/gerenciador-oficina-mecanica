package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.PeriodicidadeAssinatura;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para plano de assinatura.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoAssinaturaDTO {

    private UUID id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String descricao;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valor;

    @NotNull(message = "Periodicidade é obrigatória")
    private PeriodicidadeAssinatura periodicidade;

    private List<String> servicosIncluidos;

    @Min(value = 1, message = "Limite de OS deve ser pelo menos 1")
    private Integer limiteOsMes;

    @DecimalMin(value = "0", message = "Desconto não pode ser negativo")
    @DecimalMax(value = "100", message = "Desconto não pode ser maior que 100%")
    private BigDecimal descontoPecas;

    @DecimalMin(value = "0", message = "Desconto não pode ser negativo")
    @DecimalMax(value = "100", message = "Desconto não pode ser maior que 100%")
    private BigDecimal descontoMaoObra;

    private Boolean ativo;

    // Campos calculados
    private Integer totalAssinantes;
    private BigDecimal receitaMensalEstimada;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
