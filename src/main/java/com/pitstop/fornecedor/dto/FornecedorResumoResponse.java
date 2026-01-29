package com.pitstop.fornecedor.dto;

import com.pitstop.fornecedor.domain.TipoFornecedor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO resumido de fornecedor para uso em selects, autocomplete e referências.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados resumidos de um fornecedor")
public class FornecedorResumoResponse {

    @Schema(description = "Identificador único", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Tipo de fornecedor", example = "DISTRIBUIDOR")
    private TipoFornecedor tipo;

    @Schema(description = "Nome fantasia", example = "Auto Peças Brasil")
    private String nomeFantasia;

    @Schema(description = "CPF/CNPJ", example = "12.345.678/0001-90")
    private String cpfCnpj;

    @Schema(description = "Telefone principal", example = "(11) 3333-4444")
    private String telefone;

    @Schema(description = "Celular", example = "(11) 98888-7777")
    private String celular;
}
