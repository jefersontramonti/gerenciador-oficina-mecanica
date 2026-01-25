package com.pitstop.saas.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um lead capturado.
 *
 * Um lead é um potencial cliente que demonstrou interesse
 * no sistema através de formulários de contato, landing pages, etc.
 *
 * @author PitStop Team
 */
@Entity
@Table(name = "leads", indexes = {
    @Index(name = "idx_leads_status", columnList = "status"),
    @Index(name = "idx_leads_created_at", columnList = "created_at"),
    @Index(name = "idx_leads_email", columnList = "email"),
    @Index(name = "idx_leads_origem", columnList = "origem")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String whatsapp;

    /**
     * Origem do lead (ex: "landing-page", "indicacao", "google-ads", "facebook").
     */
    @Column(length = 100)
    @Builder.Default
    private String origem = "landing-page";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private StatusLead status = StatusLead.NOVO;

    /**
     * Observações e anotações da equipe de vendas sobre o lead.
     */
    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =====================================
    // BUSINESS METHODS
    // =====================================

    /**
     * Verifica se o lead ainda está ativo (não convertido nem perdido).
     */
    public boolean isAtivo() {
        return status != StatusLead.CONVERTIDO && status != StatusLead.PERDIDO;
    }

    /**
     * Marca o lead como contatado.
     */
    public void marcarComoContatado() {
        if (status == StatusLead.NOVO) {
            this.status = StatusLead.CONTATADO;
        }
    }

    /**
     * Qualifica o lead.
     */
    public void qualificar() {
        if (status == StatusLead.NOVO || status == StatusLead.CONTATADO) {
            this.status = StatusLead.QUALIFICADO;
        }
    }

    /**
     * Converte o lead (virou cliente).
     */
    public void converter() {
        this.status = StatusLead.CONVERTIDO;
    }

    /**
     * Marca o lead como perdido.
     */
    public void perder(String motivo) {
        this.status = StatusLead.PERDIDO;
        if (motivo != null && !motivo.isEmpty()) {
            this.observacoes = (this.observacoes != null ? this.observacoes + "\n" : "")
                + "Motivo da perda: " + motivo;
        }
    }

    /**
     * Adiciona uma observação ao lead.
     */
    public void adicionarObservacao(String novaObservacao) {
        if (novaObservacao != null && !novaObservacao.isEmpty()) {
            this.observacoes = (this.observacoes != null ? this.observacoes + "\n" : "")
                + LocalDateTime.now() + ": " + novaObservacao;
        }
    }
}
