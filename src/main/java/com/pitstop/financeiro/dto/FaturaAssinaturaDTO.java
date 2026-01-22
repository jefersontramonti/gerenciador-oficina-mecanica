package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.StatusFaturaAssinatura;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para fatura de assinatura.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaturaAssinaturaDTO {

    private UUID id;

    private UUID assinaturaId;
    private String clienteNome;
    private String planoNome;

    private String numeroFatura;
    private LocalDate mesReferencia;
    private BigDecimal valor;

    private StatusFaturaAssinatura status;
    private LocalDate dataVencimento;
    private LocalDateTime dataPagamento;

    // Gateway
    private String gatewayPaymentId;
    private String gatewayPaymentStatus;
    private String linkPagamento;

    private String descricao;
    private String observacoes;

    // Calculados
    private Integer diasAteVencimento;
    private Boolean vencida;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * DTO para registrar pagamento manual.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrarPagamentoDTO {
        private String observacao;
    }
}
