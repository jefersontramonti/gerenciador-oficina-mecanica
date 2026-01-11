package com.pitstop.ordemservico.domain;

import com.pitstop.oficina.domain.Oficina;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa o histórico de mudanças de status de uma Ordem de Serviço.
 *
 * <p>Registra cada transição de status, incluindo:</p>
 * <ul>
 *   <li>Status anterior e novo</li>
 *   <li>Data/hora da mudança</li>
 *   <li>Usuário responsável pela mudança</li>
 *   <li>Observação opcional (ex: motivo de cancelamento)</li>
 * </ul>
 */
@Entity
@Table(
    name = "historico_status_os",
    indexes = {
        @Index(name = "idx_historico_status_os_ordem_id", columnList = "ordem_servico_id"),
        @Index(name = "idx_historico_status_os_oficina_id", columnList = "oficina_id"),
        @Index(name = "idx_historico_status_os_data", columnList = "data_alteracao"),
        @Index(name = "idx_historico_status_os_ordem_data", columnList = "ordem_servico_id, data_alteracao")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "statusAnterior", "statusNovo", "dataAlteracao"})
public class HistoricoStatusOS implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Oficina à qual este histórico pertence (multi-tenant).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    private Oficina oficina;

    /**
     * Ordem de Serviço relacionada a este histórico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServico ordemServico;

    /**
     * Status antes da mudança (null se for a criação da OS).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusOS statusAnterior;

    /**
     * Novo status após a mudança.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30)
    private StatusOS statusNovo;

    /**
     * ID do usuário que realizou a mudança (pode ser null para ações do sistema).
     */
    @Column(name = "usuario_id")
    private UUID usuarioId;

    /**
     * Nome do usuário no momento da mudança.
     * Armazenado para manter histórico mesmo se o usuário for deletado.
     */
    @Column(name = "usuario_nome", length = 100)
    private String usuarioNome;

    /**
     * Observação sobre a mudança (ex: motivo de cancelamento, descrição de peça aguardada).
     */
    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    /**
     * Data/hora da mudança de status.
     */
    @Column(name = "data_alteracao", nullable = false)
    @Builder.Default
    private LocalDateTime dataAlteracao = LocalDateTime.now();

    /**
     * Data/hora de criação do registro.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Construtor de conveniência para criar histórico a partir de uma OS.
     */
    public static HistoricoStatusOS criar(
            OrdemServico os,
            StatusOS statusAnterior,
            StatusOS statusNovo,
            UUID usuarioId,
            String usuarioNome,
            String observacao
    ) {
        return HistoricoStatusOS.builder()
                .oficina(os.getOficina())
                .ordemServico(os)
                .statusAnterior(statusAnterior)
                .statusNovo(statusNovo)
                .usuarioId(usuarioId)
                .usuarioNome(usuarioNome)
                .observacao(observacao)
                .dataAlteracao(LocalDateTime.now())
                .build();
    }
}
