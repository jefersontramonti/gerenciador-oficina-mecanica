package com.pitstop.saas.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comunicados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comunicado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 500)
    private String resumo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TipoComunicado tipo = TipoComunicado.NOVIDADE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PrioridadeComunicado prioridade = PrioridadeComunicado.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusComunicado status = StatusComunicado.RASCUNHO;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "autor_nome", nullable = false)
    private String autorNome;

    // Segmentação
    @Column(name = "planos_alvo", columnDefinition = "TEXT[]")
    private String[] planosAlvo;

    @Column(name = "oficinas_alvo", columnDefinition = "UUID[]")
    private UUID[] oficinasAlvo;

    @Column(name = "status_oficinas_alvo", columnDefinition = "TEXT[]")
    private String[] statusOficinasAlvo;

    // Agendamento
    @Column(name = "data_agendamento")
    private OffsetDateTime dataAgendamento;

    @Column(name = "data_envio")
    private OffsetDateTime dataEnvio;

    // Estatísticas
    @Column(name = "total_destinatarios")
    @Builder.Default
    private Integer totalDestinatarios = 0;

    @Column(name = "total_visualizacoes")
    @Builder.Default
    private Integer totalVisualizacoes = 0;

    @Column(name = "total_confirmacoes")
    @Builder.Default
    private Integer totalConfirmacoes = 0;

    // Configurações
    @Column(name = "requer_confirmacao")
    @Builder.Default
    private Boolean requerConfirmacao = false;

    @Column(name = "exibir_no_login")
    @Builder.Default
    private Boolean exibirNoLogin = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Relacionamentos
    @OneToMany(mappedBy = "comunicado", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComunicadoLeitura> leituras = new ArrayList<>();

    // Métodos auxiliares
    public boolean isRascunho() {
        return status == StatusComunicado.RASCUNHO;
    }

    public boolean isAgendado() {
        return status == StatusComunicado.AGENDADO;
    }

    public boolean isEnviado() {
        return status == StatusComunicado.ENVIADO;
    }

    public boolean isCancelado() {
        return status == StatusComunicado.CANCELADO;
    }

    public boolean podeEditar() {
        return status == StatusComunicado.RASCUNHO || status == StatusComunicado.AGENDADO;
    }

    public boolean podeEnviar() {
        return status == StatusComunicado.RASCUNHO;
    }

    public boolean podeCancelar() {
        return status == StatusComunicado.AGENDADO;
    }

    public void enviar(int totalDestinatarios) {
        this.status = StatusComunicado.ENVIADO;
        this.dataEnvio = OffsetDateTime.now();
        this.totalDestinatarios = totalDestinatarios;
    }

    public void agendar(OffsetDateTime dataAgendamento) {
        this.status = StatusComunicado.AGENDADO;
        this.dataAgendamento = dataAgendamento;
    }

    public void cancelar() {
        this.status = StatusComunicado.CANCELADO;
    }

    public void incrementarVisualizacoes() {
        this.totalVisualizacoes++;
    }

    public void incrementarConfirmacoes() {
        this.totalConfirmacoes++;
    }

    public double getTaxaVisualizacao() {
        if (totalDestinatarios == 0) return 0;
        return (double) totalVisualizacoes / totalDestinatarios * 100;
    }

    public double getTaxaConfirmacao() {
        if (totalDestinatarios == 0) return 0;
        return (double) totalConfirmacoes / totalDestinatarios * 100;
    }
}
