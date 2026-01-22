package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.StatusExtrato;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para extrato bancário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtratoBancarioDTO {

    private UUID id;
    private UUID contaBancariaId;
    private String contaBancariaNome;
    private String arquivoNome;
    private String tipoArquivo;
    private LocalDateTime dataImportacao;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private BigDecimal saldoInicial;
    private BigDecimal saldoFinal;
    private Integer totalTransacoes;
    private Integer totalConciliadas;
    private Integer totalPendentes;
    private Double percentualConciliado;
    private StatusExtrato status;
    private List<TransacaoExtratoDTO> transacoes;
}
