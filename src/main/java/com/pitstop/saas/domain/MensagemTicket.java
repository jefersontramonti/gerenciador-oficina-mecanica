package com.pitstop.saas.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mensagens_ticket",
       indexes = {
           @Index(name = "idx_mensagens_ticket_ticket_id", columnList = "ticket_id"),
           @Index(name = "idx_mensagens_ticket_criado_em", columnList = "criado_em")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensagemTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    // Autor da mensagem
    @Column(name = "autor_id")
    private UUID autorId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "autor_nome", nullable = false)
    private String autorNome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "autor_tipo", nullable = false, length = 20)
    private TipoAutorMensagem autorTipo;

    // Tipo de mensagem (interno = nota interna, não visível para cliente)
    @Column(name = "is_interno", nullable = false)
    @Builder.Default
    private Boolean isInterno = false;

    // Conteúdo
    @NotBlank
    @Column(name = "conteudo", columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    // Anexos (JSON array de URLs)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anexos", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> anexos = new ArrayList<>();

    // Data
    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    // ==================== Métodos de conveniência ====================

    /**
     * Verifica se é uma resposta do suporte
     */
    public boolean isRespostaSuporte() {
        return this.autorTipo == TipoAutorMensagem.SUPORTE && !Boolean.TRUE.equals(this.isInterno);
    }

    /**
     * Verifica se é uma mensagem do cliente
     */
    public boolean isMensagemCliente() {
        return this.autorTipo == TipoAutorMensagem.CLIENTE;
    }

    /**
     * Verifica se é uma nota interna (não visível para o cliente)
     */
    public boolean isNotaInterna() {
        return Boolean.TRUE.equals(this.isInterno);
    }

    /**
     * Verifica se é uma mensagem de sistema (automática)
     */
    public boolean isMensagemSistema() {
        return this.autorTipo == TipoAutorMensagem.SISTEMA;
    }
}
