package com.pitstop.ordemservico.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para transição de OS para status AGUARDANDO_PECA.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados para colocar OS em aguardando peça")
public record AguardarPecaDTO(

    @Schema(description = "Descrição da peça que está sendo aguardada",
            example = "Aguardando filtro de óleo Mann W 712/83",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Descrição da peça aguardada é obrigatória")
    @Size(min = 5, max = 500, message = "Descrição deve ter entre 5 e 500 caracteres")
    String descricaoPeca
) {
}
