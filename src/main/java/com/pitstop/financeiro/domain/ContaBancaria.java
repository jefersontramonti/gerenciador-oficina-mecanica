package com.pitstop.financeiro.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.TipoConta;
import com.pitstop.shared.security.tenant.TenantContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa uma conta bancária da oficina.
 * Permite cadastrar múltiplas contas para diferentes finalidades.
 */
@Entity
@Table(
    name = "contas_bancarias",
    indexes = {
        @Index(name = "idx_contas_bancarias_oficina", columnList = "oficina_id"),
        @Index(name = "idx_contas_bancarias_padrao", columnList = "padrao"),
        @Index(name = "idx_contas_bancarias_ativo", columnList = "ativo")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ContaBancaria implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    /**
     * Nome/apelido da conta para identificação.
     * Ex: "Conta Principal", "Conta para Boletos"
     */
    @Column(name = "nome", nullable = false, length = 100)
    @NotBlank(message = "Nome da conta é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    /**
     * Código do banco (ex: 001 para BB, 341 para Itaú).
     */
    @Column(name = "codigo_banco", length = 5)
    @Size(max = 5, message = "Código do banco deve ter no máximo 5 caracteres")
    private String codigoBanco;

    /**
     * Nome do banco.
     */
    @Column(name = "banco", nullable = false, length = 100)
    @NotBlank(message = "Nome do banco é obrigatório")
    @Size(max = 100, message = "Nome do banco deve ter no máximo 100 caracteres")
    private String banco;

    /**
     * Número da agência (sem dígito).
     */
    @Column(name = "agencia", length = 10)
    @Size(max = 10, message = "Agência deve ter no máximo 10 caracteres")
    private String agencia;

    /**
     * Dígito verificador da agência.
     */
    @Column(name = "digito_agencia", length = 2)
    private String digitoAgencia;

    /**
     * Número da conta (sem dígito).
     */
    @Column(name = "conta", length = 20)
    @Size(max = 20, message = "Conta deve ter no máximo 20 caracteres")
    private String conta;

    /**
     * Dígito verificador da conta.
     */
    @Column(name = "digito_conta", length = 2)
    private String digitoConta;

    /**
     * Tipo da conta (corrente ou poupança).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conta", length = 20)
    private TipoConta tipoConta;

    /**
     * Titular da conta bancária.
     */
    @Column(name = "titular", length = 200)
    @Size(max = 200, message = "Titular deve ter no máximo 200 caracteres")
    private String titular;

    /**
     * CPF/CNPJ do titular.
     */
    @Column(name = "cpf_cnpj_titular", length = 20)
    @Size(max = 20, message = "CPF/CNPJ deve ter no máximo 20 caracteres")
    private String cpfCnpjTitular;

    // ========== DADOS PIX ==========

    /**
     * Tipo da chave PIX.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_chave_pix", length = 20)
    private TipoChavePix tipoChavePix;

    /**
     * Chave PIX (CPF, CNPJ, e-mail, telefone ou aleatória).
     */
    @Column(name = "chave_pix", length = 100)
    @Size(max = 100, message = "Chave PIX deve ter no máximo 100 caracteres")
    private String chavePix;

    /**
     * Nome do beneficiário para exibir no QR Code PIX.
     */
    @Column(name = "nome_beneficiario_pix", length = 200)
    private String nomeBeneficiarioPix;

    /**
     * Cidade do beneficiário para QR Code PIX.
     */
    @Column(name = "cidade_beneficiario_pix", length = 100)
    private String cidadeBeneficiarioPix;

    // ========== CONFIGURAÇÕES ==========

    /**
     * Se é a conta padrão para recebimentos.
     */
    @Column(name = "padrao", nullable = false)
    @Builder.Default
    private Boolean padrao = false;

    /**
     * Se a conta está ativa.
     */
    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /**
     * Finalidade da conta.
     * Ex: "Recebimentos gerais", "Boletos", "PIX", "Parcelas"
     */
    @Column(name = "finalidade", length = 200)
    private String finalidade;

    /**
     * Observações sobre a conta.
     */
    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void prePersist() {
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }
    }

    /**
     * Retorna dados bancários formatados para exibição.
     */
    public String getDadosFormatados() {
        StringBuilder sb = new StringBuilder();
        sb.append(banco);
        if (agencia != null) {
            sb.append(" - Ag: ").append(agencia);
            if (digitoAgencia != null) {
                sb.append("-").append(digitoAgencia);
            }
        }
        if (conta != null) {
            sb.append(" Cc: ").append(conta);
            if (digitoConta != null) {
                sb.append("-").append(digitoConta);
            }
        }
        return sb.toString();
    }

    /**
     * Verifica se tem dados PIX configurados.
     */
    public boolean temPix() {
        return chavePix != null && !chavePix.isBlank();
    }

    /**
     * Verifica se tem dados bancários completos.
     */
    public boolean temDadosBancarios() {
        return banco != null && !banco.isBlank() &&
               agencia != null && !agencia.isBlank() &&
               conta != null && !conta.isBlank();
    }
}
