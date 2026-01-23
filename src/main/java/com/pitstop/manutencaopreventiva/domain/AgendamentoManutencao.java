package com.pitstop.manutencaopreventiva.domain;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.domain.Veiculo;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Agendamento de manutenção preventiva.
 */
@Entity
@Table(name = "agendamentos_manutencao",
    indexes = {
        @Index(name = "idx_agendamentos_manutencao_oficina", columnList = "oficina_id"),
        @Index(name = "idx_agendamentos_manutencao_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_agendamentos_manutencao_cliente", columnList = "cliente_id"),
        @Index(name = "idx_agendamentos_manutencao_data", columnList = "data_agendamento, hora_agendamento"),
        @Index(name = "idx_agendamentos_manutencao_status", columnList = "status")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "tipoManutencao", "dataAgendamento", "horaAgendamento", "status"})
public class AgendamentoManutencao implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id")
    private PlanoManutencaoPreventiva plano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    @NotNull(message = "Veículo é obrigatório")
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @NotNull(message = "Cliente é obrigatório")
    private Cliente cliente;

    @Column(name = "data_agendamento", nullable = false)
    @NotNull(message = "Data do agendamento é obrigatória")
    private LocalDate dataAgendamento;

    @Column(name = "hora_agendamento", nullable = false)
    @NotNull(message = "Hora do agendamento é obrigatória")
    private LocalTime horaAgendamento;

    @Column(name = "duracao_estimada_minutos")
    @Builder.Default
    private Integer duracaoEstimadaMinutos = 60;

    @Column(name = "tipo_manutencao", nullable = false, length = 50)
    @NotBlank(message = "Tipo de manutenção é obrigatório")
    private String tipoManutencao;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    // Confirmação
    @Column(name = "token_confirmacao", length = 64, unique = true)
    private String tokenConfirmacao;

    @Column(name = "token_expira_em")
    private LocalDateTime tokenExpiraEm;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @Column(name = "confirmado_via", length = 20)
    private String confirmadoVia;  // LINK, WHATSAPP, TELEFONE, PRESENCIAL

    // Remarcação
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remarcado_de_id")
    private AgendamentoManutencao remarcadoDe;

    @Column(name = "motivo_remarcacao", columnDefinition = "TEXT")
    private String motivoRemarcacao;

    // Cancelamento
    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    private String motivoCancelamento;

    @Column(name = "cancelado_por")
    private UUID canceladoPor;

    // Realização
    @Column(name = "realizado_em")
    private LocalDateTime realizadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id")
    private OrdemServico ordemServico;

    // Lembretes
    @Column(name = "lembrete_enviado")
    @Builder.Default
    private Boolean lembreteEnviado = false;

    @Column(name = "lembrete_enviado_em")
    private LocalDateTime lembreteEnviadoEm;

    // Observações
    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "observacoes_internas", columnDefinition = "TEXT")
    private String observacoesInternas;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    private void prePersist() {
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }
        gerarTokenConfirmacao(72); // 72 horas padrão
    }

    /**
     * Gera token único para confirmação pelo cliente.
     * Usa apenas caracteres alfanuméricos para evitar problemas com
     * Markdown do Telegram/WhatsApp que interpreta _ como itálico.
     */
    public void gerarTokenConfirmacao(int horasValidade) {
        // Caracteres seguros para URLs e que não são interpretados como formatação
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder token = new StringBuilder(64);
        for (int i = 0; i < 64; i++) {
            token.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        this.tokenConfirmacao = token.toString();
        this.tokenExpiraEm = LocalDateTime.now().plusHours(horasValidade);
    }

    /**
     * Confirma o agendamento.
     */
    public void confirmar(String via) {
        this.status = StatusAgendamento.CONFIRMADO;
        this.confirmadoEm = LocalDateTime.now();
        this.confirmadoVia = via;
    }

    /**
     * Remarca o agendamento para nova data/hora.
     */
    public AgendamentoManutencao remarcar(LocalDate novaData, LocalTime novaHora, String motivo) {
        this.status = StatusAgendamento.REMARCADO;
        this.motivoRemarcacao = motivo;

        // Cria novo agendamento
        AgendamentoManutencao novo = AgendamentoManutencao.builder()
            .oficina(this.oficina)
            .plano(this.plano)
            .veiculo(this.veiculo)
            .cliente(this.cliente)
            .dataAgendamento(novaData)
            .horaAgendamento(novaHora)
            .duracaoEstimadaMinutos(this.duracaoEstimadaMinutos)
            .tipoManutencao(this.tipoManutencao)
            .descricao(this.descricao)
            .observacoes(this.observacoes)
            .observacoesInternas(this.observacoesInternas)
            .remarcadoDe(this)
            .build();

        return novo;
    }

    /**
     * Cancela o agendamento.
     */
    public void cancelar(String motivo, UUID usuarioId) {
        this.status = StatusAgendamento.CANCELADO;
        this.canceladoEm = LocalDateTime.now();
        this.motivoCancelamento = motivo;
        this.canceladoPor = usuarioId;
    }

    /**
     * Marca como realizado vinculando a uma OS.
     */
    public void marcarComoRealizado(OrdemServico os) {
        this.status = StatusAgendamento.REALIZADO;
        this.realizadoEm = LocalDateTime.now();
        this.ordemServico = os;
    }

    /**
     * Verifica se o token é válido.
     */
    public boolean isTokenValido(String token) {
        if (tokenConfirmacao == null || token == null) {
            return false;
        }
        if (!tokenConfirmacao.equals(token)) {
            return false;
        }
        return tokenExpiraEm != null && LocalDateTime.now().isBefore(tokenExpiraEm);
    }

    /**
     * Retorna data/hora do agendamento.
     */
    public LocalDateTime getDataHoraAgendamento() {
        return LocalDateTime.of(dataAgendamento, horaAgendamento);
    }

    /**
     * Verifica se é hoje.
     */
    public boolean isHoje() {
        return dataAgendamento.equals(LocalDate.now());
    }

    /**
     * Verifica se já passou.
     */
    public boolean isPassado() {
        return getDataHoraAgendamento().isBefore(LocalDateTime.now());
    }
}
