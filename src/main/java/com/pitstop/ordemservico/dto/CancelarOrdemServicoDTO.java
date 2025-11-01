package com.pitstop.ordemservico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para cancelamento de Ordem de Serviço.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados para cancelamento de OS")
public record CancelarOrdemServicoDTO(

    @Schema(description = "Motivo do cancelamento", example = "Cliente desistiu do serviço", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Motivo do cancelamento é obrigatório")
    @Size(min = 10, max = 500, message = "Motivo deve ter entre 10 e 500 caracteres")
    String motivo
) {
}
