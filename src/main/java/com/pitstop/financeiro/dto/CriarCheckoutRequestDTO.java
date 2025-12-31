package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.TipoGateway;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para criar um checkout/link de pagamento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriarCheckoutRequestDTO {

    @NotNull(message = "ID da ordem de serviço é obrigatório")
    private UUID ordemServicoId;

    /**
     * Valor a ser cobrado. Se não informado, usa o valor pendente da OS.
     */
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valor;

    /**
     * Gateway a ser utilizado. Se não informado, usa o gateway padrão.
     */
    private TipoGateway gateway;

    /**
     * E-mail do pagador (para enviar link).
     */
    private String emailPagador;

    /**
     * Nome do pagador.
     */
    private String nomePagador;

    /**
     * CPF/CNPJ do pagador.
     */
    private String documentoPagador;

    /**
     * Descrição adicional para o pagamento.
     */
    private String descricao;

    /**
     * Tempo de expiração em minutos (padrão: 30).
     */
    private Integer expiracaoMinutos;

    /**
     * Métodos de pagamento permitidos.
     * Ex: ["pix", "credit_card", "boleto"]
     */
    private String[] metodosPermitidos;
}
