package com.pitstop.fornecedor.domain;

import com.pitstop.cliente.domain.Endereco;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um fornecedor de peças no sistema PitStop.
 *
 * <p>Fornecedores são empresas ou pessoas que fornecem peças para o estoque da oficina.
 * A entidade suporta soft delete através do campo {@code ativo}.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Entity
@Table(
    name = "fornecedores",
    indexes = {
        @Index(name = "idx_fornecedores_nome_fantasia", columnList = "nome_fantasia"),
        @Index(name = "idx_fornecedores_tipo", columnList = "tipo")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "ativo = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "nomeFantasia", "tipo"})
public class Fornecedor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @NotNull(message = "Tipo de fornecedor é obrigatório")
    private TipoFornecedor tipo;

    @Column(name = "nome_fantasia", nullable = false, length = 200)
    @NotBlank(message = "Nome fantasia é obrigatório")
    @Size(min = 2, max = 200, message = "Nome fantasia deve ter entre 2 e 200 caracteres")
    private String nomeFantasia;

    @Column(name = "razao_social", length = 200)
    @Size(max = 200, message = "Razão social deve ter no máximo 200 caracteres")
    private String razaoSocial;

    @Column(name = "cpf_cnpj", length = 18)
    private String cpfCnpj;

    @Column(name = "inscricao_estadual", length = 20)
    @Size(max = 20, message = "Inscrição estadual deve ter no máximo 20 caracteres")
    private String inscricaoEstadual;

    // ========== CONTACT ==========

    @Column(name = "email", length = 100)
    @Email(message = "E-mail deve ser válido")
    @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres")
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "celular", length = 20)
    private String celular;

    @Column(name = "website", length = 200)
    @Size(max = 200, message = "Website deve ter no máximo 200 caracteres")
    private String website;

    @Column(name = "contato_nome", length = 150)
    @Size(max = 150, message = "Nome do contato deve ter no máximo 150 caracteres")
    private String contatoNome;

    // ========== ADDRESS ==========

    @Embedded
    @Valid
    private Endereco endereco;

    // ========== COMMERCIAL ==========

    @Column(name = "prazo_entrega", length = 100)
    @Size(max = 100, message = "Prazo de entrega deve ter no máximo 100 caracteres")
    private String prazoEntrega;

    @Column(name = "condicoes_pagamento", length = 200)
    @Size(max = 200, message = "Condições de pagamento deve ter no máximo 200 caracteres")
    private String condicoesPagamento;

    @Column(name = "desconto_padrao", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descontoPadrao = BigDecimal.ZERO;

    // ========== NOTES ==========

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    // ========== SOFT DELETE & AUDIT ==========

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========== LIFECYCLE CALLBACKS ==========

    @PrePersist
    @PreUpdate
    private void validarRegrasDeNegocio() {
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }

        if (nomeFantasia == null || nomeFantasia.trim().length() < 2) {
            throw new IllegalStateException("Nome fantasia deve ter no mínimo 2 caracteres");
        }

        if (descontoPadrao != null && (descontoPadrao.compareTo(BigDecimal.ZERO) < 0 || descontoPadrao.compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalStateException("Desconto padrão deve estar entre 0 e 100");
        }
    }

    // ========== BUSINESS METHODS ==========

    public void desativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }
}
