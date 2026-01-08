package com.pitstop.ordemservico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO para criação de Ordem de Serviço.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados para criação de Ordem de Serviço")
public record CreateOrdemServicoDTO(

    @Schema(description = "ID do veículo", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Veículo é obrigatório")
    UUID veiculoId,

    @Schema(description = "ID do mecânico responsável", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Mecânico responsável é obrigatório")
    UUID usuarioId,

    @Schema(description = "Problemas relatados pelo cliente", example = "Veículo fazendo barulho ao frear", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Problemas relatados são obrigatórios")
    @Size(min = 10, max = 5000, message = "Problemas relatados devem ter entre 10 e 5000 caracteres")
    String problemasRelatados,

    @Schema(description = "Data prevista para conclusão", example = "2025-11-05")
    LocalDate dataPrevisao,

    @Schema(description = "Valor da mão de obra", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Valor da mão de obra é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor da mão de obra não pode ser negativo")
    BigDecimal valorMaoObra,

    @Schema(description = "Desconto percentual (0-100%)", example = "10.00")
    @DecimalMin(value = "0.00", message = "Desconto percentual não pode ser negativo")
    @DecimalMax(value = "100.00", message = "Desconto percentual não pode ser maior que 100%")
    BigDecimal descontoPercentual,

    @Schema(description = "Desconto em valor absoluto", example = "50.00")
    @DecimalMin(value = "0.00", message = "Desconto em valor não pode ser negativo")
    BigDecimal descontoValor,

    @Schema(description = "Diagnóstico técnico", example = "Pastilhas de freio desgastadas")
    @Size(max = 5000, message = "Diagnóstico deve ter no máximo 5000 caracteres")
    String diagnostico,

    @Schema(description = "Observações gerais", example = "Cliente solicitou urgência")
    @Size(max = 5000, message = "Observações devem ter no máximo 5000 caracteres")
    String observacoes,

    @Schema(description = "Itens da OS (peças e serviços)")
    @Valid
    List<CreateItemOSDTO> itens
) {
}
