package com.pitstop.ordemservico.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Entidade que representa uma Ordem de Serviço (OS) no sistema PitStop.
 *
 * <p>A Ordem de Serviço é o núcleo do sistema, gerenciando todo o ciclo de vida
 * do atendimento de um veículo na oficina, desde a abertura do orçamento até a entrega.</p>
 *
 * <p><strong>Fluxo principal de status:</strong></p>
 * <pre>
 * ORCAMENTO → APROVADO → EM_ANDAMENTO → FINALIZADO → ENTREGUE
 * </pre>
 *
 * <p><strong>Regras de Negócio:</strong></p>
 * <ul>
 *   <li>Número sequencial único gerado por sequence do PostgreSQL</li>
 *   <li>Transições de status são validadas pela máquina de estados</li>
 *   <li>Valores financeiros calculados automaticamente a partir dos itens</li>
 *   <li>Aprovação do cliente obrigatória para iniciar execução</li>
 *   <li>Baixa de estoque automática ao finalizar (status = FINALIZADO)</li>
 *   <li>Uma vez entregue, a OS não pode mais ser modificada</li>
 *   <li>Optimistic locking para controle de concorrência</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Entity
@Table(
    name = "ordem_servico",
    indexes = {
        @Index(name = "idx_ordem_servico_numero", columnList = "numero"),
        @Index(name = "idx_ordem_servico_status", columnList = "status"),
        @Index(name = "idx_ordem_servico_veiculo_id", columnList = "veiculo_id"),
        @Index(name = "idx_ordem_servico_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_ordem_servico_data_abertura", columnList = "data_abertura"),
        @Index(name = "idx_ordem_servico_status_data", columnList = "status, data_abertura"),
        @Index(name = "idx_ordem_servico_created_at", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "numero", "status", "veiculoId"})
public class OrdemServico implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Mapa de transições permitidas entre status.
     * Usado para validar mudanças de estado na máquina de estados.
     */
    private static final Map<StatusOS, Set<StatusOS>> TRANSICOES_PERMITIDAS = Map.of(
        StatusOS.ORCAMENTO, Set.of(StatusOS.APROVADO, StatusOS.CANCELADO),
        StatusOS.APROVADO, Set.of(StatusOS.EM_ANDAMENTO, StatusOS.CANCELADO),
        StatusOS.EM_ANDAMENTO, Set.of(StatusOS.AGUARDANDO_PECA, StatusOS.FINALIZADO, StatusOS.CANCELADO),
        StatusOS.AGUARDANDO_PECA, Set.of(StatusOS.EM_ANDAMENTO, StatusOS.CANCELADO),
        StatusOS.FINALIZADO, Set.of(StatusOS.ENTREGUE),
        StatusOS.ENTREGUE, Set.of(), // Estado final
        StatusOS.CANCELADO, Set.of() // Estado final
    );

    /**
     * Identificador único da OS (UUID v4).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Número sequencial único da OS (gerado por sequence do PostgreSQL).
     * Exemplo: 1, 2, 3, ...
     */
    @Column(name = "numero", nullable = false, unique = true, updatable = false)
    private Long numero;

    /**
     * Identificador do veículo (FK para tabela veiculos).
     */
    @Column(name = "veiculo_id", nullable = false)
    @NotNull(message = "Veículo é obrigatório")
    private UUID veiculoId;

    /**
     * Identificador do mecânico responsável (FK para tabela usuarios).
     */
    @Column(name = "usuario_id", nullable = false)
    @NotNull(message = "Mecânico responsável é obrigatório")
    private UUID usuarioId;

    /**
     * Status atual da OS (enum).
     * Define em qual etapa do fluxo a OS se encontra.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @NotNull(message = "Status é obrigatório")
    @Builder.Default
    private StatusOS status = StatusOS.ORCAMENTO;

    // ===== DATAS =====

    /**
     * Data/hora de abertura da OS (criação do registro).
     */
    @Column(name = "data_abertura", nullable = false, updatable = false)
    @NotNull(message = "Data de abertura é obrigatória")
    @Builder.Default
    private LocalDateTime dataAbertura = LocalDateTime.now();

    /**
     * Data prevista para conclusão (informada ao cliente no orçamento).
     */
    @Column(name = "data_previsao")
    private LocalDate dataPrevisao;

    /**
     * Data/hora em que a OS foi finalizada (status = FINALIZADO).
     */
    @Column(name = "data_finalizacao")
    private LocalDateTime dataFinalizacao;

    /**
     * Data/hora em que o veículo foi entregue ao cliente (status = ENTREGUE).
     */
    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    // ===== DESCRIÇÕES =====

    /**
     * Problemas relatados pelo cliente (mínimo 10 caracteres).
     */
    @Column(name = "problemas_relatados", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Problemas relatados são obrigatórios")
    @Size(min = 10, max = 5000, message = "Problemas relatados devem ter entre 10 e 5000 caracteres")
    private String problemasRelatados;

    /**
     * Diagnóstico técnico realizado pelo mecânico.
     */
    @Column(name = "diagnostico", columnDefinition = "TEXT")
    @Size(max = 5000, message = "Diagnóstico deve ter no máximo 5000 caracteres")
    private String diagnostico;

    /**
     * Observações gerais sobre a OS (motivo de cancelamento, observações de entrega, etc).
     */
    @Column(name = "observacoes", columnDefinition = "TEXT")
    @Size(max = 5000, message = "Observações devem ter no máximo 5000 caracteres")
    private String observacoes;

    // ===== VALORES FINANCEIROS =====

    /**
     * Valor total da mão de obra.
     */
    @Column(name = "valor_mao_obra", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Valor da mão de obra é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor da mão de obra não pode ser negativo")
    @Builder.Default
    private BigDecimal valorMaoObra = BigDecimal.ZERO;

    /**
     * Valor total das peças (calculado automaticamente da soma dos itens).
     */
    @Column(name = "valor_pecas", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Valor das peças é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor das peças não pode ser negativo")
    @Builder.Default
    private BigDecimal valorPecas = BigDecimal.ZERO;

    /**
     * Valor total antes do desconto (valorMaoObra + valorPecas).
     */
    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor total não pode ser negativo")
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    /**
     * Desconto percentual aplicado (0-100%).
     */
    @Column(name = "desconto_percentual", precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Desconto percentual não pode ser negativo")
    @DecimalMax(value = "100.00", message = "Desconto percentual não pode ser maior que 100%")
    @Builder.Default
    private BigDecimal descontoPercentual = BigDecimal.ZERO;

    /**
     * Desconto em valor absoluto.
     */
    @Column(name = "desconto_valor", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Desconto em valor não pode ser negativo")
    @Builder.Default
    private BigDecimal descontoValor = BigDecimal.ZERO;

    /**
     * Valor final após desconto (valor que o cliente pagará).
     */
    @Column(name = "valor_final", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Valor final é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor final não pode ser negativo")
    @Builder.Default
    private BigDecimal valorFinal = BigDecimal.ZERO;

    // ===== REGRAS DE NEGÓCIO =====

    /**
     * Indica se o cliente aprovou o orçamento.
     * Obrigatório para transição APROVADO → EM_ANDAMENTO.
     */
    @Column(name = "aprovado_pelo_cliente")
    @Builder.Default
    private Boolean aprovadoPeloCliente = false;

    // ===== OPTIMISTIC LOCKING =====

    /**
     * Versão para controle de concorrência otimista.
     * Incrementado automaticamente a cada update.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;

    // ===== AUDITORIA =====

    /**
     * Data/hora de criação do registro (preenchido automaticamente).
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data/hora da última modificação (atualizado automaticamente).
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== RELACIONAMENTOS =====

    /**
     * Itens da OS (peças e serviços).
     * Relacionamento One-to-Many com cascade ALL e orphan removal.
     */
    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<ItemOS> itens = new ArrayList<>();

    // ===== MÉTODOS DE NEGÓCIO =====

    /**
     * Adiciona um item à OS.
     * Estabelece o relacionamento bidirecional.
     *
     * @param item Item a ser adicionado
     */
    public void adicionarItem(ItemOS item) {
        if (item == null) {
            throw new IllegalArgumentException("Item não pode ser nulo");
        }
        this.itens.add(item);
        item.setOrdemServico(this);
    }

    /**
     * Remove um item da OS.
     *
     * @param item Item a ser removido
     */
    public void removerItem(ItemOS item) {
        if (item == null) {
            throw new IllegalArgumentException("Item não pode ser nulo");
        }
        this.itens.remove(item);
        item.setOrdemServico(null);
    }

    /**
     * Remove todos os itens da OS.
     */
    public void limparItens() {
        this.itens.forEach(item -> item.setOrdemServico(null));
        this.itens.clear();
    }

    /**
     * Recalcula todos os valores financeiros da OS com base nos itens.
     * Deve ser chamado sempre que itens forem adicionados/removidos/modificados.
     */
    public void recalcularValores() {
        // Calcula valor das peças (soma dos itens do tipo PECA)
        this.valorPecas = this.itens.stream()
            .filter(item -> item.getTipo() == TipoItem.PECA)
            .map(ItemOS::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        // Calcula valor total (mão de obra + peças)
        this.valorTotal = this.valorMaoObra.add(this.valorPecas)
            .setScale(2, RoundingMode.HALF_UP);

        // Calcula desconto total
        BigDecimal descontoTotal = calcularDescontoTotal();

        // Calcula valor final
        this.valorFinal = this.valorTotal.subtract(descontoTotal)
            .setScale(2, RoundingMode.HALF_UP);

        // Garante que valor final não seja negativo
        if (this.valorFinal.compareTo(BigDecimal.ZERO) < 0) {
            this.valorFinal = BigDecimal.ZERO;
        }
    }

    /**
     * Calcula o desconto total (percentual + valor absoluto).
     *
     * @return Valor total do desconto
     */
    private BigDecimal calcularDescontoTotal() {
        BigDecimal descontoPercent = BigDecimal.ZERO;
        BigDecimal descontoAbsoluto = this.descontoValor != null ? this.descontoValor : BigDecimal.ZERO;

        if (this.descontoPercentual != null && this.descontoPercentual.compareTo(BigDecimal.ZERO) > 0) {
            descontoPercent = this.valorTotal
                .multiply(this.descontoPercentual)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        return descontoPercent.add(descontoAbsoluto).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Muda o status da OS validando a transição pela máquina de estados.
     *
     * @param novoStatus Novo status desejado
     * @throws IllegalStateException se a transição não for permitida
     */
    public void mudarStatus(StatusOS novoStatus) {
        if (novoStatus == null) {
            throw new IllegalArgumentException("Novo status não pode ser nulo");
        }

        if (this.status == novoStatus) {
            return; // Já está no status desejado
        }

        Set<StatusOS> transicoesPermitidas = TRANSICOES_PERMITIDAS.get(this.status);
        if (transicoesPermitidas == null || !transicoesPermitidas.contains(novoStatus)) {
            throw new IllegalStateException(
                String.format("Transição de %s para %s não é permitida", this.status, novoStatus)
            );
        }

        this.status = novoStatus;
        atualizarDatasPorStatus(novoStatus);
    }

    /**
     * Atualiza campos de data conforme o status.
     *
     * @param status Status atual
     */
    private void atualizarDatasPorStatus(StatusOS status) {
        LocalDateTime agora = LocalDateTime.now();
        switch (status) {
            case FINALIZADO -> this.dataFinalizacao = agora;
            case ENTREGUE -> this.dataEntrega = agora;
        }
    }

    /**
     * Aprova o orçamento (transição para APROVADO).
     *
     * @param aprovado Indicador de aprovação do cliente
     * @throws IllegalStateException se não estiver em status ORCAMENTO
     */
    public void aprovar(boolean aprovado) {
        if (this.status != StatusOS.ORCAMENTO) {
            throw new IllegalStateException("Apenas orçamentos podem ser aprovados");
        }
        this.aprovadoPeloCliente = aprovado;
        if (aprovado) {
            mudarStatus(StatusOS.APROVADO);
        }
    }

    /**
     * Inicia a execução dos serviços (transição para EM_ANDAMENTO).
     *
     * @throws IllegalStateException se não estiver aprovado ou aprovação do cliente não existir
     */
    public void iniciar() {
        if (this.status != StatusOS.APROVADO) {
            throw new IllegalStateException("Apenas OS aprovadas podem ser iniciadas");
        }
        if (!Boolean.TRUE.equals(this.aprovadoPeloCliente)) {
            throw new IllegalStateException("OS precisa estar aprovada pelo cliente");
        }
        mudarStatus(StatusOS.EM_ANDAMENTO);
    }

    /**
     * Finaliza a OS (transição para FINALIZADO).
     * Neste ponto, ocorre a baixa automática de peças do estoque.
     *
     * @throws IllegalStateException se não estiver em execução
     */
    public void finalizar() {
        if (this.status != StatusOS.EM_ANDAMENTO && this.status != StatusOS.AGUARDANDO_PECA) {
            throw new IllegalStateException("Apenas OS em execução podem ser finalizadas");
        }
        mudarStatus(StatusOS.FINALIZADO);
    }

    /**
     * Entrega o veículo ao cliente (transição para ENTREGUE).
     *
     * @throws IllegalStateException se não estiver finalizada
     */
    public void entregar() {
        if (this.status != StatusOS.FINALIZADO) {
            throw new IllegalStateException("Apenas OS finalizadas podem ser entregues");
        }
        mudarStatus(StatusOS.ENTREGUE);
    }

    /**
     * Cancela a OS (transição para CANCELADO).
     *
     * @param motivo Motivo do cancelamento
     * @throws IllegalStateException se já estiver entregue ou cancelada
     */
    public void cancelar(String motivo) {
        if (!this.status.isCancelavel()) {
            throw new IllegalStateException("Esta OS não pode mais ser cancelada");
        }
        if (motivo != null && !motivo.isBlank()) {
            this.observacoes = (this.observacoes != null ? this.observacoes + "\n\n" : "")
                + "CANCELAMENTO: " + motivo;
        }
        mudarStatus(StatusOS.CANCELADO);
    }

    /**
     * Lifecycle callback executado antes de salvar no banco.
     * Valida regras de negócio e normaliza dados.
     */
    @PrePersist
    @PreUpdate
    protected void prePersistOrUpdate() {
        recalcularValores();

        // Trim de strings
        if (this.problemasRelatados != null) {
            this.problemasRelatados = this.problemasRelatados.trim();
        }
        if (this.diagnostico != null) {
            this.diagnostico = this.diagnostico.trim();
        }
        if (this.observacoes != null) {
            this.observacoes = this.observacoes.trim();
        }
    }
}
