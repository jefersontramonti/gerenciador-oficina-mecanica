package com.pitstop.ordemservico.dto;

import com.pitstop.ordemservico.domain.StatusOS;
import com.pitstop.ordemservico.domain.TipoCobrancaMaoObra;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta completa de Ordem de Serviço.
 * Inclui dados relacionados (veículo, cliente, mecânico, itens).
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Schema(description = "Dados completos de resposta de Ordem de Serviço")
public record OrdemServicoResponseDTO(

    @Schema(description = "ID da OS", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,

    @Schema(description = "Número sequencial da OS", example = "123")
    Long numero,

    @Schema(description = "Status atual", example = "EM_ANDAMENTO")
    StatusOS status,

    @Schema(description = "Dados do veículo")
    VeiculoResumoDTO veiculo,

    @Schema(description = "Dados do cliente (proprietário do veículo)")
    ClienteResumoDTO cliente,

    @Schema(description = "Dados do mecânico responsável")
    UsuarioResumoDTO mecanico,

    @Schema(description = "Data/hora de abertura", example = "2025-11-01T10:30:00")
    LocalDateTime dataAbertura,

    @Schema(description = "Data prevista para conclusão", example = "2025-11-05")
    LocalDate dataPrevisao,

    @Schema(description = "Data/hora de finalização", example = "2025-11-04T16:00:00")
    LocalDateTime dataFinalizacao,

    @Schema(description = "Data/hora de entrega", example = "2025-11-04T17:00:00")
    LocalDateTime dataEntrega,

    @Schema(description = "Problemas relatados pelo cliente", example = "Veículo fazendo barulho ao frear")
    String problemasRelatados,

    @Schema(description = "Diagnóstico técnico", example = "Pastilhas de freio desgastadas")
    String diagnostico,

    @Schema(description = "Observações gerais", example = "Cliente solicitou urgência")
    String observacoes,

    // ===== MODELO DE COBRANÇA DE MÃO DE OBRA =====

    @Schema(description = "Tipo de cobrança de mão de obra", example = "VALOR_FIXO")
    TipoCobrancaMaoObra tipoCobrancaMaoObra,

    @Schema(description = "Valor da mão de obra (fixo ou calculado)", example = "150.00")
    BigDecimal valorMaoObra,

    @Schema(description = "Tempo estimado em horas (se POR_HORA)", example = "3.5")
    BigDecimal tempoEstimadoHoras,

    @Schema(description = "Limite de horas aprovado pelo cliente (se POR_HORA)", example = "5.0")
    BigDecimal limiteHorasAprovado,

    @Schema(description = "Horas efetivamente trabalhadas (se POR_HORA)", example = "4.0")
    BigDecimal horasTrabalhadas,

    @Schema(description = "Valor/hora capturado no momento da criação (se POR_HORA)", example = "80.00")
    BigDecimal valorHoraSnapshot,

    // ===== VALORES =====

    @Schema(description = "Valor total das peças", example = "250.00")
    BigDecimal valorPecas,

    @Schema(description = "Valor total antes do desconto", example = "400.00")
    BigDecimal valorTotal,

    @Schema(description = "Desconto percentual aplicado", example = "10.00")
    BigDecimal descontoPercentual,

    @Schema(description = "Desconto em valor absoluto", example = "40.00")
    BigDecimal descontoValor,

    @Schema(description = "Valor final a pagar", example = "360.00")
    BigDecimal valorFinal,

    @Schema(description = "Se o cliente aprovou o orçamento", example = "true")
    Boolean aprovadoPeloCliente,

    @Schema(description = "Lista de itens (peças e serviços)")
    List<ItemOSResponseDTO> itens,

    @Schema(description = "Data de criação do registro", example = "2025-11-01T10:30:00")
    LocalDateTime createdAt,

    @Schema(description = "Data da última atualização", example = "2025-11-01T14:20:00")
    LocalDateTime updatedAt
) {
}
