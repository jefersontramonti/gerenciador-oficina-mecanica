package com.pitstop.veiculo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta contendo dados completos de um veículo.
 *
 * <p>Usado em operações de consulta (GET) para retornar informações do veículo
 * incluindo metadados de auditoria e dados resumidos do cliente.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados completos de um veículo")
public class VeiculoResponseDTO {

    @Schema(description = "Identificador único do veículo", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "ID do cliente proprietário", example = "660e8400-e29b-41d4-a716-446655440000")
    private UUID clienteId;

    @Schema(description = "Dados resumidos do cliente proprietário")
    private ClienteResumoDTO cliente;

    @Schema(description = "Placa do veículo (formato: ABC-1234)", example = "ABC-1234")
    private String placa;

    @Schema(description = "Marca do veículo", example = "Volkswagen")
    private String marca;

    @Schema(description = "Modelo do veículo", example = "Gol")
    private String modelo;

    @Schema(description = "Ano de fabricação", example = "2020")
    private Integer ano;

    @Schema(description = "Cor do veículo", example = "Prata")
    private String cor;

    @Schema(description = "Número do chassi", example = "9BWZZZ377VT004251")
    private String chassi;

    @Schema(description = "Quilometragem atual", example = "50000")
    private Integer quilometragem;

    @Schema(description = "Descrição completa do veículo", example = "Volkswagen Gol 2020")
    private String descricaoCompleta;

    @Schema(description = "Data e hora de criação do registro", example = "2025-10-31T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Data e hora da última atualização", example = "2025-10-31T15:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * DTO aninhado com dados resumidos do cliente.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Dados resumidos do cliente proprietário")
    public static class ClienteResumoDTO {

        @Schema(description = "ID do cliente", example = "660e8400-e29b-41d4-a716-446655440000")
        private UUID id;

        @Schema(description = "Nome do cliente", example = "João da Silva")
        private String nome;

        @Schema(description = "CPF/CNPJ do cliente", example = "123.456.789-00")
        private String cpfCnpj;

        @Schema(description = "Telefone de contato", example = "(11) 98888-7777")
        private String telefone;
    }
}
