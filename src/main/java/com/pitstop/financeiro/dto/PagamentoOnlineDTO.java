package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.StatusPagamentoOnline;
import com.pitstop.financeiro.domain.TipoGateway;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para representar um pagamento online.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoOnlineDTO {
    private UUID id;
    private UUID ordemServicoId;
    private UUID pagamentoId;

    private TipoGateway gateway;
    private String gatewayDescricao;

    private String preferenceId;
    private String idExterno;
    private String idCobranca;

    private StatusPagamentoOnline status;
    private String statusDescricao;
    private String statusDetalhe;

    private BigDecimal valor;
    private BigDecimal valorLiquido;
    private BigDecimal valorTaxa;

    private String metodoPagamento;
    private String bandeiraCartao;
    private String ultimosDigitos;
    private Integer parcelas;

    private String urlCheckout;
    private String urlQrCode;
    private String codigoPix;

    private LocalDateTime dataExpiracao;
    private LocalDateTime dataAprovacao;

    private String erroMensagem;
    private String erroCodigo;
    private Integer tentativas;

    private String emailPagador;
    private String nomePagador;
    private String documentoPagador;

    private Boolean expirado;
    private Boolean aprovado;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
