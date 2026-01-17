package com.pitstop.manutencaopreventiva.domain;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.domain.Veiculo;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Alerta de manutenção a ser enviado ao cliente.
 */
@Entity
@Table(name = "alertas_manutencao",
    indexes = {
        @Index(name = "idx_alertas_manutencao_oficina", columnList = "oficina_id"),
        @Index(name = "idx_alertas_manutencao_plano", columnList = "plano_id"),
        @Index(name = "idx_alertas_manutencao_status", columnList = "status")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "tipoAlerta", "canal", "status"})
public class AlertaManutencao implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    @NotNull(message = "Plano é obrigatório")
    private PlanoManutencaoPreventiva plano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    @NotNull(message = "Veículo é obrigatório")
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    @NotNull(message = "Cliente é obrigatório")
    private Cliente cliente;

    @Column(name = "tipo_alerta", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Tipo de alerta é obrigatório")
    private TipoAlerta tipoAlerta;

    @Column(name = "canal", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Canal é obrigatório")
    private CanalNotificacao canal;

    @Column(name = "destinatario", nullable = false, length = 255)
    @NotBlank(message = "Destinatário é obrigatório")
    private String destinatario;

    @Column(name = "titulo", length = 200)
    private String titulo;

    @Column(name = "mensagem", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Mensagem é obrigatória")
    private String mensagem;

    /** Dados extras para template */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_extras", columnDefinition = "jsonb")
    private Map<String, Object> dadosExtras;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusAlerta status = StatusAlerta.PENDENTE;

    @Column(name = "tentativas", nullable = false)
    @Builder.Default
    private Integer tentativas = 0;

    @Column(name = "max_tentativas", nullable = false)
    @Builder.Default
    private Integer maxTentativas = 3;

    @Column(name = "proxima_tentativa")
    private LocalDateTime proximaTentativa;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "erro_mensagem", columnDefinition = "TEXT")
    private String erroMensagem;

    /** Resposta do gateway de envio */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resposta_gateway", columnDefinition = "jsonb")
    private Map<String, Object> respostaGateway;

    /** Data/hora para envio agendado */
    @Column(name = "agendar_para")
    private LocalDateTime agendarPara;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }
    }

    /**
     * Marca como enviado com sucesso.
     */
    public void marcarComoEnviado(Map<String, Object> resposta) {
        this.status = StatusAlerta.ENVIADO;
        this.enviadoEm = LocalDateTime.now();
        this.respostaGateway = resposta;
        this.erroMensagem = null;
    }

    /**
     * Marca como falhou e agenda retry se não atingiu max tentativas.
     */
    public void marcarComoFalhou(String erro, int intervaloRetryMinutos) {
        this.tentativas++;
        this.erroMensagem = erro;

        if (this.tentativas >= this.maxTentativas) {
            this.status = StatusAlerta.FALHOU;
            this.proximaTentativa = null;
        } else {
            this.status = StatusAlerta.PENDENTE;
            this.proximaTentativa = LocalDateTime.now().plusMinutes(intervaloRetryMinutos);
        }
    }

    /**
     * Cancela o alerta.
     */
    public void cancelar() {
        this.status = StatusAlerta.CANCELADO;
    }

    /**
     * Verifica se pode fazer retry.
     */
    public boolean podeRetry() {
        return tentativas < maxTentativas &&
               (status == StatusAlerta.PENDENTE || status == StatusAlerta.FALHOU);
    }

    /**
     * Verifica se está pronto para envio.
     */
    public boolean prontoParaEnvio() {
        if (status != StatusAlerta.PENDENTE) {
            return false;
        }

        LocalDateTime agora = LocalDateTime.now();

        // Se tem agendamento, verifica se chegou a hora
        if (agendarPara != null && agendarPara.isAfter(agora)) {
            return false;
        }

        // Se tem próxima tentativa agendada, verifica se chegou a hora
        if (proximaTentativa != null && proximaTentativa.isAfter(agora)) {
            return false;
        }

        return true;
    }
}
