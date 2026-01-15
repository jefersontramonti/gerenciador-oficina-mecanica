package com.pitstop.saas.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for creating a new subscription plan.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePlanoRequest {

    @NotBlank(message = "Código é obrigatório")
    @Size(max = 30, message = "Código deve ter no máximo 30 caracteres")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Código deve conter apenas letras maiúsculas, números e underscore")
    private String codigo;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
    private String descricao;

    // Pricing
    @NotNull(message = "Valor mensal é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor mensal não pode ser negativo")
    private BigDecimal valorMensal;

    @DecimalMin(value = "0.00", message = "Valor anual não pode ser negativo")
    private BigDecimal valorAnual;

    @Min(value = 0, message = "Dias de trial não pode ser negativo")
    @Max(value = 365, message = "Dias de trial não pode exceder 365")
    @Builder.Default
    private Integer trialDias = 14;

    // Limits (-1 = unlimited)
    @Min(value = -1, message = "Limite de usuários inválido")
    @Builder.Default
    private Integer limiteUsuarios = 1;

    @Min(value = -1, message = "Limite de OS/mês inválido")
    @Builder.Default
    private Integer limiteOsMes = -1;

    @Min(value = -1, message = "Limite de clientes inválido")
    @Builder.Default
    private Integer limiteClientes = -1;

    @Min(value = -1, message = "Limite de espaço inválido")
    @Builder.Default
    private Long limiteEspacoMb = 5120L;

    @Min(value = -1, message = "Limite de API calls inválido")
    @Builder.Default
    private Integer limiteApiCalls = -1;

    @Min(value = 0, message = "Limite de WhatsApp não pode ser negativo")
    @Builder.Default
    private Integer limiteWhatsappMensagens = 0;

    @Min(value = 0, message = "Limite de emails não pode ser negativo")
    @Builder.Default
    private Integer limiteEmailsMes = 100;

    // Features
    private Map<String, Boolean> features;

    // Display & Marketing
    @Builder.Default
    private Boolean ativo = true;
    @Builder.Default
    private Boolean visivel = true;
    @Builder.Default
    private Boolean recomendado = false;

    @Size(max = 20, message = "Cor deve ter no máximo 20 caracteres")
    private String corDestaque;

    @Size(max = 50, message = "Tag de promoção deve ter no máximo 50 caracteres")
    private String tagPromocao;

    @Min(value = 0, message = "Ordem de exibição não pode ser negativa")
    @Builder.Default
    private Integer ordemExibicao = 0;
}
