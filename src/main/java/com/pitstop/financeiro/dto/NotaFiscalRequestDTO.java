package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.TipoNotaFiscal;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para requisição de criação/atualização de Nota Fiscal.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-23
 */
public record NotaFiscalRequestDTO(

    @NotNull(message = "Ordem de serviço é obrigatória")
    UUID ordemServicoId,

    @NotNull(message = "Tipo de nota fiscal é obrigatório")
    TipoNotaFiscal tipo,

    @NotNull(message = "Série da nota é obrigatória")
    @Min(value = 1, message = "Série deve ser maior que zero")
    Integer serie,

    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor total deve ser maior que zero")
    BigDecimal valorTotal,

    @Size(max = 60, message = "Natureza da operação deve ter no máximo 60 caracteres")
    String naturezaOperacao,

    @Size(max = 4, message = "CFOP deve ter 4 caracteres")
    @Pattern(regexp = "\\d{4}", message = "CFOP deve conter 4 dígitos")
    String cfop,

    @Size(max = 1000, message = "Informações complementares devem ter no máximo 1000 caracteres")
    String informacoesComplementares,

    @NotNull(message = "Data de emissão é obrigatória")
    LocalDateTime dataEmissao
) {
}
