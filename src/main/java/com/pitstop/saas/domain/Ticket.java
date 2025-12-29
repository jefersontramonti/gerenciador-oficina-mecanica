package com.pitstop.saas.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.usuario.domain.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tickets",
       indexes = {
           @Index(name = "idx_tickets_oficina_id", columnList = "oficina_id"),
           @Index(name = "idx_tickets_status", columnList = "status"),
           @Index(name = "idx_tickets_prioridade", columnList = "prioridade"),
           @Index(name = "idx_tickets_tipo", columnList = "tipo"),
           @Index(name = "idx_tickets_atribuido_a", columnList = "atribuido_a"),
           @Index(name = "idx_tickets_abertura_em", columnList = "abertura_em"),
           @Index(name = "idx_tickets_numero", columnList = "numero")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero", nullable = false, unique = true, length = 20)
    private String numero;

    // Relacionamento com Oficina (opcional - pode ser ticket interno)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    // Dados do solicitante
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "usuario_nome", nullable = false)
    private String usuarioNome;

    @NotBlank
    @Size(max = 255)
    @Column(name = "usuario_email", nullable = false)
    private String usuarioEmail;

    // Classificação
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoTicket tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 20)
    private PrioridadeTicket prioridade;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private StatusTicket status = StatusTicket.ABERTO;

    // Conteúdo
    @NotBlank
    @Size(max = 255)
    @Column(name = "assunto", nullable = false)
    private String assunto;

    @NotBlank
    @Column(name = "descricao", columnDefinition = "TEXT", nullable = false)
    private String descricao;

    // Anexos (JSON array de URLs)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anexos", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> anexos = new ArrayList<>();

    // Atribuição
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atribuido_a")
    private Usuario atribuidoA;

    // SLA
    @Column(name = "sla_minutos")
    @Builder.Default
    private Integer slaMinutos = 1440; // 24 horas padrão

    @Column(name = "resposta_inicial_em")
    private LocalDateTime respostaInicialEm;

    @Column(name = "tempo_resposta_minutos")
    private Integer tempoRespostaMinutos;

    // Datas
    @Column(name = "abertura_em", nullable = false)
    @Builder.Default
    private LocalDateTime aberturaEm = LocalDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;

    @Column(name = "fechado_em")
    private LocalDateTime fechadoEm;

    // Auditoria
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Relacionamento com mensagens
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criadoEm ASC")
    @Builder.Default
    private List<MensagemTicket> mensagens = new ArrayList<>();

    // ==================== Métodos de negócio ====================

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Define o SLA baseado na prioridade
     */
    public void definirSlaPorPrioridade() {
        if (this.prioridade != null) {
            this.slaMinutos = this.prioridade.getSlaMinutos();
        }
    }

    /**
     * Registra a primeira resposta e calcula o tempo de resposta
     */
    public void registrarPrimeiraResposta() {
        if (this.respostaInicialEm == null) {
            this.respostaInicialEm = LocalDateTime.now();
            this.tempoRespostaMinutos = (int) ChronoUnit.MINUTES.between(this.aberturaEm, this.respostaInicialEm);
        }
    }

    /**
     * Verifica se o SLA foi cumprido
     */
    public boolean slaCumprido() {
        if (this.respostaInicialEm == null || this.slaMinutos == null) {
            return false;
        }
        return this.tempoRespostaMinutos != null && this.tempoRespostaMinutos <= this.slaMinutos;
    }

    /**
     * Verifica se o SLA está vencido (sem resposta ainda)
     */
    public boolean slaVencido() {
        if (this.respostaInicialEm != null) {
            return false; // Já teve resposta
        }
        if (this.slaMinutos == null) {
            return false;
        }
        long minutosDesdeAbertura = ChronoUnit.MINUTES.between(this.aberturaEm, LocalDateTime.now());
        return minutosDesdeAbertura > this.slaMinutos;
    }

    /**
     * Minutos restantes para o SLA
     */
    public Long minutosRestantesSla() {
        if (this.respostaInicialEm != null || this.slaMinutos == null) {
            return null;
        }
        long minutosDesdeAbertura = ChronoUnit.MINUTES.between(this.aberturaEm, LocalDateTime.now());
        return Math.max(0, this.slaMinutos - minutosDesdeAbertura);
    }

    /**
     * Marca o ticket como resolvido
     */
    public void resolver() {
        this.status = StatusTicket.RESOLVIDO;
        this.resolvidoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Fecha o ticket
     */
    public void fechar() {
        this.status = StatusTicket.FECHADO;
        this.fechadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Adiciona uma mensagem ao ticket
     */
    public void adicionarMensagem(MensagemTicket mensagem) {
        mensagem.setTicket(this);
        this.mensagens.add(mensagem);
        this.atualizadoEm = LocalDateTime.now();
    }
}
