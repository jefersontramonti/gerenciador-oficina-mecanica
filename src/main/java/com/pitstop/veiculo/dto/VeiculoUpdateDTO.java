package com.pitstop.veiculo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para atualização de dados de um veículo existente.
 *
 * <p>Nota: Placa e clienteId não podem ser alterados após criação.
 * Para alterar esses dados, é necessário criar um novo registro.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para atualização de um veículo existente")
public class VeiculoUpdateDTO {

    @Schema(description = "Marca do veículo", example = "Volkswagen", required = true)
    @NotBlank(message = "Marca é obrigatória")
    @Size(min = 2, max = 50, message = "Marca deve ter entre 2 e 50 caracteres")
    private String marca;

    @Schema(description = "Modelo do veículo", example = "Gol G8", required = true)
    @NotBlank(message = "Modelo é obrigatório")
    @Size(min = 2, max = 100, message = "Modelo deve ter entre 2 e 100 caracteres")
    private String modelo;

    @Schema(description = "Ano de fabricação", example = "2021", required = true)
    @NotNull(message = "Ano é obrigatório")
    @Min(value = 1900, message = "Ano deve ser igual ou superior a 1900")
    private Integer ano;

    @Schema(description = "Cor do veículo", example = "Branco")
    @Size(max = 30, message = "Cor deve ter no máximo 30 caracteres")
    private String cor;

    @Schema(description = "Número do chassi (VIN - 17 caracteres)", example = "9BWZZZ377VT004251")
    @Size(min = 17, max = 17, message = "Chassi deve ter exatamente 17 caracteres")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "Chassi inválido (deve conter apenas letras e números, exceto I, O, Q)")
    private String chassi;

    @Schema(description = "Quilometragem atual do veículo", example = "60000")
    @Min(value = 0, message = "Quilometragem não pode ser negativa")
    private Integer quilometragem;
}
