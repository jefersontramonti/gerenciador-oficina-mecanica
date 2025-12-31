package com.pitstop.estoque.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidade que representa um local de armazenamento físico no estoque.
 * Implementa hierarquia auto-referencial (árvore) para organização de locais.
 *
 * Exemplos de hierarquia:
 * - Depósito Principal > Setor A > Armário 1 > Prateleira 3 > Caixa 5
 * - Vitrine de Peças > Gaveta Superior
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Entity
@Table(name = "local_armazenamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"localizacaoPai", "locaisFilhos"})
public class LocalArmazenamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Oficina à qual este local pertence (multi-tenant).
     * Preenchida automaticamente via TenantContext no @PrePersist.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    /**
     * Código único do local (normalizado para UPPERCASE).
     * Exemplos: "DEP-A", "PRAT-B1", "ARM-01", "GAV-SUPERIOR"
     */
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    /**
     * Tipo do local de armazenamento.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLocal tipo;

    /**
     * Descrição do local.
     * Exemplos: "Depósito Principal", "Prateleira 3 - Filtros", "Gaveta de parafusos"
     */
    @Column(nullable = false, length = 200)
    private String descricao;

    /**
     * Local pai na hierarquia (self-referential).
     * NULL para locais raiz (depósitos, vitrines principais).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "localizacao_pai_id")
    private LocalArmazenamento localizacaoPai;

    /**
     * Locais filhos (inverso de localizacaoPai).
     * Permite navegar pela hierarquia de cima para baixo.
     */
    @OneToMany(mappedBy = "localizacaoPai", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private Set<LocalArmazenamento> locaisFilhos = new HashSet<>();

    /**
     * Capacidade máxima de itens (opcional).
     * Pode ser usado para alertas de lotação.
     */
    @Column(name = "capacidade_maxima")
    private Integer capacidadeMaxima;

    /**
     * Observações adicionais sobre o local.
     */
    @Column(columnDefinition = "TEXT")
    private String observacoes;

    /**
     * Flag de ativação (soft delete).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /**
     * Data de criação (auditoria).
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data de última atualização (auditoria).
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========== Business Methods ==========

    /**
     * Retorna o caminho completo hierárquico do local.
     * Exemplo: "Depósito Principal > Setor A > Armário 1 > Prateleira 3"
     *
     * Verifica se o proxy Hibernate está inicializado antes de acessar
     * recursivamente para evitar LazyInitializationException.
     *
     * @return caminho completo (ou parcial se hierarquia não estiver carregada)
     */
    public String getCaminhoCompleto() {
        if (localizacaoPai == null) {
            return descricao;
        }
        // Verifica se o proxy do pai está inicializado para evitar LazyInitializationException
        if (!Hibernate.isInitialized(localizacaoPai)) {
            return descricao;
        }
        return localizacaoPai.getCaminhoCompleto() + " > " + descricao;
    }

    /**
     * Calcula o nível na hierarquia.
     * Nível 0 = raiz (sem pai), nível 1 = filho direto da raiz, etc.
     *
     * Verifica se o proxy Hibernate está inicializado antes de acessar
     * recursivamente para evitar LazyInitializationException.
     *
     * @return nível na hierarquia (1 se hierarquia não estiver carregada)
     */
    public int getNivel() {
        if (localizacaoPai == null) {
            return 0;
        }
        // Verifica se o proxy do pai está inicializado para evitar LazyInitializationException
        if (!Hibernate.isInitialized(localizacaoPai)) {
            return 1; // Retorna 1 se tem pai mas não consegue calcular recursivamente
        }
        return 1 + localizacaoPai.getNivel();
    }

    /**
     * Verifica se é um local raiz (sem pai).
     *
     * @return true se é raiz
     */
    public boolean isRaiz() {
        return localizacaoPai == null;
    }

    /**
     * Verifica se tem filhos.
     *
     * @return true se tem locais filhos
     */
    public boolean temFilhos() {
        return locaisFilhos != null && !locaisFilhos.isEmpty();
    }

    /**
     * Adiciona um local filho.
     *
     * @param filho local filho
     */
    public void adicionarFilho(LocalArmazenamento filho) {
        if (locaisFilhos == null) {
            locaisFilhos = new HashSet<>();
        }
        locaisFilhos.add(filho);
        filho.setLocalizacaoPai(this);
    }

    /**
     * Remove um local filho.
     *
     * @param filho local filho
     */
    public void removerFilho(LocalArmazenamento filho) {
        if (locaisFilhos != null) {
            locaisFilhos.remove(filho);
            filho.setLocalizacaoPai(null);
        }
    }

    // ========== Lifecycle Callbacks ==========

    /**
     * Define oficina via TenantContext e normaliza o código para UPPERCASE antes de persistir.
     */
    @PrePersist
    @PreUpdate
    private void prePersist() {
        // Multi-tenancy: set oficina from TenantContext
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }

        // Normaliza código para UPPERCASE
        if (codigo != null) {
            codigo = codigo.trim().toUpperCase();
        }
    }
}
