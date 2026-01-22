package com.pitstop.manutencaopreventiva.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.ordemservico.domain.OrdemServico;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Plano de manutenção preventiva vinculado a um veículo específico.
 */
@Entity
@Table(name = "planos_manutencao_preventiva",
    indexes = {
        @Index(name = "idx_planos_manutencao_oficina", columnList = "oficina_id"),
        @Index(name = "idx_planos_manutencao_veiculo", columnList = "veiculo_id"),
        @Index(name = "idx_planos_manutencao_status", columnList = "status")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "nome", "tipoManutencao", "status"})
public class PlanoManutencaoPreventiva implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    @NotNull(message = "Veículo é obrigatório")
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private TemplateManutencao template;

    @Column(name = "nome", nullable = false, length = 100)
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "tipo_manutencao", nullable = false, length = 50)
    @NotBlank(message = "Tipo de manutenção é obrigatório")
    @Size(max = 50, message = "Tipo de manutenção deve ter no máximo 50 caracteres")
    private String tipoManutencao;

    @Column(name = "criterio", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CriterioManutencao criterio = CriterioManutencao.AMBOS;

    /** Intervalo em dias entre manutenções */
    @Column(name = "intervalo_dias")
    @Min(value = 1, message = "Intervalo de dias deve ser maior que zero")
    private Integer intervaloDias;

    /** Intervalo em km entre manutenções */
    @Column(name = "intervalo_km")
    @Min(value = 1, message = "Intervalo de km deve ser maior que zero")
    private Integer intervaloKm;

    /** Dias de antecedência para alertar */
    @Column(name = "antecedencia_dias")
    @Builder.Default
    private Integer antecedenciaDias = 15;

    /** KM de antecedência para alertar */
    @Column(name = "antecedencia_km")
    @Builder.Default
    private Integer antecedenciaKm = 1000;

    /** Canais de notificação habilitados */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canais_notificacao", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> canaisNotificacao = List.of("WHATSAPP", "EMAIL");

    // Última execução
    @Column(name = "ultima_execucao_data")
    private LocalDate ultimaExecucaoData;

    @Column(name = "ultima_execucao_km")
    private Integer ultimaExecucaoKm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ultima_ordem_servico_id")
    private OrdemServico ultimaOrdemServico;

    // Próxima previsão (calculada)
    @Column(name = "proxima_previsao_data")
    private LocalDate proximaPrevisaoData;

    @Column(name = "proxima_previsao_km")
    private Integer proximaPrevisaoKm;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusPlanoManutencao status = StatusPlanoManutencao.ATIVO;

    @Column(name = "motivo_pausa", columnDefinition = "TEXT")
    private String motivoPausa;

    // Controle de alertas
    @Column(name = "ultimo_alerta_enviado_em")
    private LocalDateTime ultimoAlertaEnviadoEm;

    @Column(name = "proxima_verificacao_alerta")
    private LocalDateTime proximaVerificacaoAlerta;

    /** Lista de itens do checklist em JSON */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist", columnDefinition = "jsonb")
    private List<TemplateManutencao.ChecklistItem> checklist;

    /** Lista de peças sugeridas em JSON */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pecas_sugeridas", columnDefinition = "jsonb")
    private List<TemplateManutencao.PecaSugerida> pecasSugeridas;

    /** Agendamentos de notificação personalizados (máximo 2) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "agendamentos_notificacao", columnDefinition = "jsonb")
    private List<AgendamentoNotificacao> agendamentosNotificacao;

    @Column(name = "valor_estimado", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Valor estimado não pode ser negativo")
    private BigDecimal valorEstimado;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

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
        calcularProximaManutencao();
    }

    /**
     * Calcula a próxima previsão de manutenção baseada no critério.
     */
    public void calcularProximaManutencao() {
        LocalDate hoje = LocalDate.now();
        LocalDate baseData = ultimaExecucaoData != null ? ultimaExecucaoData : hoje;
        Integer baseKm = ultimaExecucaoKm != null ? ultimaExecucaoKm :
                         (veiculo != null ? veiculo.getQuilometragem() : 0);

        switch (criterio) {
            case TEMPO:
                if (intervaloDias != null) {
                    this.proximaPrevisaoData = baseData.plusDays(intervaloDias);
                }
                this.proximaPrevisaoKm = null;
                break;

            case KM:
                this.proximaPrevisaoData = null;
                if (intervaloKm != null && baseKm != null) {
                    this.proximaPrevisaoKm = baseKm + intervaloKm;
                }
                break;

            case AMBOS:
            default:
                if (intervaloDias != null) {
                    this.proximaPrevisaoData = baseData.plusDays(intervaloDias);
                }
                if (intervaloKm != null && baseKm != null) {
                    this.proximaPrevisaoKm = baseKm + intervaloKm;
                }
                break;
        }
    }

    /**
     * Registra uma execução de manutenção.
     */
    public void registrarExecucao(LocalDate data, Integer km, OrdemServico os) {
        this.ultimaExecucaoData = data;
        this.ultimaExecucaoKm = km;
        this.ultimaOrdemServico = os;
        this.status = StatusPlanoManutencao.ATIVO;
        calcularProximaManutencao();
    }

    /**
     * Verifica se o plano está próximo de vencer.
     */
    public boolean isProximoAVencer(int diasAntecedencia, int kmAntecedencia) {
        if (status != StatusPlanoManutencao.ATIVO) {
            return false;
        }

        LocalDate hoje = LocalDate.now();
        Integer kmAtual = veiculo != null ? veiculo.getQuilometragem() : null;

        boolean proximoPorData = proximaPrevisaoData != null &&
            !hoje.plusDays(diasAntecedencia).isBefore(proximaPrevisaoData);

        boolean proximoPorKm = proximaPrevisaoKm != null && kmAtual != null &&
            (proximaPrevisaoKm - kmAtual) <= kmAntecedencia;

        return proximoPorData || proximoPorKm;
    }

    /**
     * Verifica se o plano está vencido.
     */
    public boolean isVencido() {
        if (status != StatusPlanoManutencao.ATIVO) {
            return false;
        }

        LocalDate hoje = LocalDate.now();
        Integer kmAtual = veiculo != null ? veiculo.getQuilometragem() : null;

        boolean vencidoPorData = proximaPrevisaoData != null &&
            proximaPrevisaoData.isBefore(hoje);

        boolean vencidoPorKm = proximaPrevisaoKm != null && kmAtual != null &&
            kmAtual >= proximaPrevisaoKm;

        return vencidoPorData || vencidoPorKm;
    }

    /**
     * Pausa o plano com um motivo.
     */
    public void pausar(String motivo) {
        this.status = StatusPlanoManutencao.PAUSADO;
        this.motivoPausa = motivo;
    }

    /**
     * Reativa o plano.
     */
    public void ativar() {
        this.status = StatusPlanoManutencao.ATIVO;
        this.motivoPausa = null;
        calcularProximaManutencao();
    }

    /**
     * Marca como concluído.
     */
    public void concluir() {
        this.status = StatusPlanoManutencao.CONCLUIDO;
    }

    /**
     * Marca como vencido.
     */
    public void marcarComoVencido() {
        this.status = StatusPlanoManutencao.VENCIDO;
    }
}
