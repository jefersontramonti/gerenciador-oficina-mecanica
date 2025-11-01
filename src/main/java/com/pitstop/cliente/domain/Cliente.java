package com.pitstop.cliente.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um cliente (pessoa física ou jurídica) no sistema PitStop.
 *
 * <p>Clientes são proprietários de veículos e podem ter múltiplas ordens de serviço.
 * A entidade suporta soft delete através do campo {@code ativo}.</p>
 *
 * <p><strong>Regras de Negócio:</strong></p>
 * <ul>
 *   <li>CPF/CNPJ deve ser único no sistema</li>
 *   <li>Pessoa Física requer CPF com 11 dígitos (formato: 000.000.000-00)</li>
 *   <li>Pessoa Jurídica requer CNPJ com 14 dígitos (formato: 00.000.000/0000-00)</li>
 *   <li>Ao menos um telefone (fixo ou celular) deve ser fornecido</li>
 *   <li>Clientes desativados não aparecem em consultas padrão (soft delete)</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Entity
@Table(
    name = "clientes",
    indexes = {
        @Index(name = "idx_clientes_cpf_cnpj", columnList = "cpfCnpj"),
        @Index(name = "idx_clientes_nome", columnList = "nome"),
        @Index(name = "idx_clientes_tipo", columnList = "tipo")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Where(clause = "ativo = true") // Soft delete: apenas clientes ativos aparecem por padrão
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "nome", "cpfCnpj", "tipo"})
public class Cliente implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador único do cliente (UUID v4).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Tipo de cliente: Pessoa Física (CPF) ou Pessoa Jurídica (CNPJ).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    @NotNull(message = "Tipo de cliente é obrigatório")
    private TipoCliente tipo;

    /**
     * Nome completo (pessoa física) ou razão social (pessoa jurídica).
     */
    @Column(name = "nome", nullable = false, length = 150)
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    /**
     * CPF (formato: 000.000.000-00) ou CNPJ (formato: 00.000.000/0000-00).
     * Deve ser único no sistema e compatível com o tipo de cliente.
     *
     * <p><strong>TODO:</strong> Implementar validação de CPF/CNPJ válido (algoritmo de dígitos verificadores)
     * usando biblioteca externa ou custom validator.</p>
     */
    @Column(name = "cpf_cnpj", nullable = false, unique = true, length = 18)
    @NotBlank(message = "CPF/CNPJ é obrigatório")
    @Pattern(
        regexp = "^(\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})$",
        message = "CPF/CNPJ deve estar no formato válido (CPF: 000.000.000-00 ou CNPJ: 00.000.000/0000-00)"
    )
    private String cpfCnpj;

    /**
     * Endereço de e-mail do cliente.
     */
    @Column(name = "email", length = 100)
    @Email(message = "E-mail deve ser válido")
    @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres")
    private String email;

    /**
     * Telefone fixo com DDD (formato: (00) 0000-0000).
     */
    @Column(name = "telefone", length = 20)
    @Pattern(
        regexp = "^(\\(\\d{2}\\)\\s?)?\\d{4,5}-?\\d{4}$",
        message = "Telefone deve estar no formato (00) 0000-0000 ou (00) 00000-0000"
    )
    private String telefone;

    /**
     * Telefone celular com DDD (formato: (00) 00000-0000).
     * Ao menos um telefone (fixo ou celular) é obrigatório.
     */
    @Column(name = "celular", length = 20)
    @Pattern(
        regexp = "^(\\(\\d{2}\\)\\s?)?\\d{4,5}-?\\d{4}$",
        message = "Celular deve estar no formato (00) 00000-0000"
    )
    private String celular;

    /**
     * Endereço completo do cliente (Value Object embutido).
     */
    @Embedded
    @Valid
    private Endereco endereco;

    /**
     * Indica se o cliente está ativo no sistema.
     * Soft delete: clientes desativados têm este campo como false.
     */
    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /**
     * Data e hora de criação do registro (auditoria).
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data e hora da última atualização do registro (auditoria).
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Valida regras de negócio antes de persistir no banco.
     *
     * @throws IllegalStateException se validação falhar
     */
    @PrePersist
    @PreUpdate
    private void validarRegrasDeNegocio() {
        // Valida que ao menos um telefone está preenchido
        if ((telefone == null || telefone.isBlank()) && (celular == null || celular.isBlank())) {
            throw new IllegalStateException("Ao menos um telefone (fixo ou celular) deve ser informado");
        }

        // Valida compatibilidade entre tipo e CPF/CNPJ (apenas tamanho, validação completa virá depois)
        if (cpfCnpj != null && !tipo.validarTamanhoDocumento(cpfCnpj)) {
            throw new IllegalStateException(
                String.format("Documento incompatível com tipo %s. Esperado %d dígitos.",
                    tipo.getDescricao(), tipo.getDigitosDocumento())
            );
        }
    }

    /**
     * Realiza soft delete do cliente, marcando-o como inativo.
     */
    public void desativar() {
        this.ativo = false;
    }

    /**
     * Reativa um cliente previamente desativado.
     */
    public void reativar() {
        this.ativo = true;
    }

    /**
     * Verifica se é pessoa física.
     *
     * @return true se tipo é PESSOA_FISICA
     */
    public boolean isPessoaFisica() {
        return TipoCliente.PESSOA_FISICA.equals(this.tipo);
    }

    /**
     * Verifica se é pessoa jurídica.
     *
     * @return true se tipo é PESSOA_JURIDICA
     */
    public boolean isPessoaJuridica() {
        return TipoCliente.PESSOA_JURIDICA.equals(this.tipo);
    }
}
