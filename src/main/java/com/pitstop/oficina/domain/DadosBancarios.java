package com.pitstop.oficina.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

/**
 * Value Object representing banking information for receiving payments.
 *
 * <p>Contains bank account details and PIX key for payment processing.</p>
 *
 * @since 1.0.0
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class DadosBancarios implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Bank name (e.g., "Banco do Brasil", "Caixa Econômica", "Itaú").
     */
    @Column(name = "banco", length = 100)
    @Size(max = 100, message = "Nome do banco deve ter no máximo 100 caracteres")
    private String banco;

    /**
     * Branch number (agência).
     */
    @Column(name = "agencia", length = 10)
    @Size(max = 10, message = "Agência deve ter no máximo 10 caracteres")
    private String agencia;

    /**
     * Account number (without check digit).
     */
    @Column(name = "conta", length = 20)
    @Size(max = 20, message = "Conta deve ter no máximo 20 caracteres")
    private String conta;

    /**
     * Account check digit.
     */
    @Column(name = "digito_conta", length = 1)
    @Size(max = 1, message = "Dígito da conta deve ter 1 caractere")
    private String digitoConta;

    /**
     * Type of bank account (checking or savings).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conta", length = 20)
    private TipoConta tipoConta;

    /**
     * PIX key (can be CPF, CNPJ, email, phone, or random key).
     */
    @Column(name = "chave_pix", length = 50)
    @Size(max = 50, message = "Chave PIX deve ter no máximo 50 caracteres")
    private String chavePix;

    /**
     * Checks if banking data is complete (all required fields filled).
     *
     * @return true if banco, agencia, conta, and tipoConta are filled
     */
    public boolean isCompleto() {
        return banco != null && !banco.isBlank() &&
               agencia != null && !agencia.isBlank() &&
               conta != null && !conta.isBlank() &&
               tipoConta != null;
    }

    /**
     * Gets formatted account number with check digit.
     *
     * @return formatted as "conta-digit" or empty if incomplete
     */
    public String getContaFormatada() {
        if (conta == null || conta.isBlank()) {
            return "";
        }
        if (digitoConta != null && !digitoConta.isBlank()) {
            return conta + "-" + digitoConta;
        }
        return conta;
    }

    /**
     * Gets full formatted banking info for display.
     *
     * @return formatted as "Banco - Ag: agencia Cc: conta-digit"
     */
    public String getDadosFormatados() {
        if (!isCompleto()) {
            return "";
        }
        return banco + " - Ag: " + agencia + " Cc: " + getContaFormatada();
    }
}
