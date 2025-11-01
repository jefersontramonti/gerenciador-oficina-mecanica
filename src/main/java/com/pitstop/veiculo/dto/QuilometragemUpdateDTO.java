package com.pitstop.veiculo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para atualização exclusiva da quilometragem do veículo.
 *
 * <p>Usado em operações de service orders para registrar quilometragem
 * sem precisar enviar todos os dados do veículo.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para atualização da quilometragem do veículo")
public class QuilometragemUpdateDTO {

    @Schema(description = "Nova quilometragem (deve ser maior ou igual à atual)", example = "65000", required = true)
    @NotNull(message = "Quilometragem é obrigatória")
    @Min(value = 0, message = "Quilometragem não pode ser negativa")
    private Integer quilometragem;
}
