package com.pitstop.ordemservico.dto;

import com.pitstop.ordemservico.domain.TipoCobrancaMaoObra;
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
 * <p>Suporta dois modelos de cobrança de mão de obra:</p>
 * <ul>
 *   <li><strong>VALOR_FIXO:</strong> Valor definido no orçamento (requer valorMaoObra)</li>
 *   <li><strong>POR_HORA:</strong> Valor calculado na finalização (requer tempoEstimadoHoras e limiteHorasAprovado)</li>
 * </ul>
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

    // ===== MODELO DE COBRANÇA DE MÃO DE OBRA =====

    @Schema(description = "Tipo de cobrança de mão de obra", example = "VALOR_FIXO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Tipo de cobrança de mão de obra é obrigatório")
    TipoCobrancaMaoObra tipoCobrancaMaoObra,

    @Schema(description = "Valor da mão de obra (obrigatório se tipoCobrancaMaoObra=VALOR_FIXO)", example = "150.00")
    @DecimalMin(value = "0.00", message = "Valor da mão de obra não pode ser negativo")
    BigDecimal valorMaoObra,

    @Schema(description = "Tempo estimado em horas (obrigatório se tipoCobrancaMaoObra=POR_HORA)", example = "3.5")
    @DecimalMin(value = "0.5", message = "Tempo estimado mínimo é 0.5 horas (30 minutos)")
    @DecimalMax(value = "100.0", message = "Tempo estimado máximo é 100 horas")
    BigDecimal tempoEstimadoHoras,

    @Schema(description = "Limite máximo de horas aprovado pelo cliente (obrigatório se tipoCobrancaMaoObra=POR_HORA)", example = "5.0")
    @DecimalMin(value = "0.5", message = "Limite mínimo é 0.5 horas (30 minutos)")
    @DecimalMax(value = "100.0", message = "Limite máximo é 100 horas")
    BigDecimal limiteHorasAprovado,

    // ===== DESCONTOS =====

    @Schema(description = "Desconto percentual (0-100%)", example = "10.00")
    @DecimalMin(value = "0.00", message = "Desconto percentual não pode ser negativo")
    @DecimalMax(value = "100.00", message = "Desconto percentual não pode ser maior que 100%")
    BigDecimal descontoPercentual,

    @Schema(description = "Desconto em valor absoluto", example = "50.00")
    @DecimalMin(value = "0.00", message = "Desconto em valor não pode ser negativo")
    BigDecimal descontoValor,

    // ===== DESCRIÇÕES =====

    @Schema(description = "Diagnóstico técnico", example = "Pastilhas de freio desgastadas")
    @Size(max = 5000, message = "Diagnóstico deve ter no máximo 5000 caracteres")
    String diagnostico,

    @Schema(description = "Observações gerais", example = "Cliente solicitou urgência")
    @Size(max = 5000, message = "Observações devem ter no máximo 5000 caracteres")
    String observacoes,

    // ===== ITENS =====

    @Schema(description = "Itens da OS (peças e serviços)")
    @Valid
    List<CreateItemOSDTO> itens

) {
    /**
     * Valida campos conforme tipo de cobrança de mão de obra.
     *
     * @return true se validação OK
     */
    @AssertTrue(message = "VALOR_FIXO requer valorMaoObra, POR_HORA requer tempoEstimadoHoras e limiteHorasAprovado")
    public boolean isCobrancaMaoObraValida() {
        if (tipoCobrancaMaoObra == null) {
            return false;
        }

        if (tipoCobrancaMaoObra == TipoCobrancaMaoObra.VALOR_FIXO) {
            // VALOR_FIXO requer valorMaoObra >= 0
            return valorMaoObra != null && valorMaoObra.compareTo(BigDecimal.ZERO) >= 0;
        } else {
            // POR_HORA requer tempo estimado e limite
            return tempoEstimadoHoras != null && limiteHorasAprovado != null;
        }
    }

    /**
     * Valida que limite de horas deve ser >= tempo estimado.
     *
     * @return true se validação OK
     */
    @AssertTrue(message = "Limite de horas aprovado deve ser maior ou igual ao tempo estimado")
    public boolean isLimiteHorasCoerente() {
        if (tipoCobrancaMaoObra != TipoCobrancaMaoObra.POR_HORA) {
            return true; // Não se aplica a VALOR_FIXO
        }
        if (tempoEstimadoHoras == null || limiteHorasAprovado == null) {
            return true; // Outras validações cuidam deste caso
        }
        return limiteHorasAprovado.compareTo(tempoEstimadoHoras) >= 0;
    }
}
