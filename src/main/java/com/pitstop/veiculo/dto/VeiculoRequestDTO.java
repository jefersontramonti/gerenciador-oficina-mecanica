package com.pitstop.veiculo.dto;

import com.pitstop.veiculo.validation.PlacaVeiculo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO para criação de um novo veículo.
 *
 * <p>Contém validações necessárias para garantir dados consistentes
 * antes de criar a entidade Veiculo.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para criação de um novo veículo")
public class VeiculoRequestDTO {

    @Schema(description = "ID do cliente proprietário", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    @NotNull(message = "Cliente é obrigatório")
    private UUID clienteId;

    @Schema(
        description = "Placa do veículo (padrão BR: ABC1234 ou Mercosul: ABC1D23, com ou sem hífen)",
        example = "ABC-1234",
        required = true
    )
    @NotBlank(message = "Placa é obrigatória")
    @PlacaVeiculo
    private String placa;

    @Schema(description = "Marca do veículo", example = "Volkswagen", required = true)
    @NotBlank(message = "Marca é obrigatória")
    @Size(min = 2, max = 50, message = "Marca deve ter entre 2 e 50 caracteres")
    private String marca;

    @Schema(description = "Modelo do veículo", example = "Gol", required = true)
    @NotBlank(message = "Modelo é obrigatório")
    @Size(min = 2, max = 100, message = "Modelo deve ter entre 2 e 100 caracteres")
    private String modelo;

    @Schema(description = "Ano de fabricação", example = "2020", required = true)
    @NotNull(message = "Ano é obrigatório")
    @Min(value = 1900, message = "Ano deve ser igual ou superior a 1900")
    private Integer ano;

    @Schema(description = "Cor do veículo", example = "Prata")
    @Size(max = 30, message = "Cor deve ter no máximo 30 caracteres")
    private String cor;

    @Schema(description = "Número do chassi (VIN - 17 caracteres)", example = "9BWZZZ377VT004251")
    @Size(min = 17, max = 17, message = "Chassi deve ter exatamente 17 caracteres")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "Chassi inválido (deve conter apenas letras e números, exceto I, O, Q)")
    private String chassi;

    @Schema(description = "Quilometragem atual do veículo", example = "50000")
    @Min(value = 0, message = "Quilometragem não pode ser negativa")
    private Integer quilometragem;
}
