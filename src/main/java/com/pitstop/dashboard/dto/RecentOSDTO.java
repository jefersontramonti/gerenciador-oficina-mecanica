package com.pitstop.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.ordemservico.domain.StatusOS;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para ordens de serviço recentes exibidas no dashboard.
 *
 * @param id ID da ordem de serviço
 * @param numero Número sequencial da OS
 * @param status Status atual da OS
 * @param clienteNome Nome do cliente
 * @param veiculoPlaca Placa do veículo
 * @param dataAbertura Data/hora de abertura da OS
 * @param valorFinal Valor final da OS
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Schema(description = "Ordem de serviço recente para dashboard")
public record RecentOSDTO(

    @Schema(description = "ID da ordem de serviço", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    UUID id,

    @Schema(description = "Número sequencial da OS", example = "1245")
    Long numero,

    @Schema(description = "Status da OS")
    StatusOS status,

    @Schema(description = "Nome do cliente", example = "João da Silva")
    String clienteNome,

    @Schema(description = "Placa do veículo", example = "ABC-1234")
    String veiculoPlaca,

    @Schema(description = "Data/hora de abertura", example = "2025-11-11T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataAbertura,

    @Schema(description = "Valor final da OS", example = "1250.00")
    BigDecimal valorFinal
) {}
