package com.pitstop.financeiro.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade que representa uma Nota Fiscal Eletrônica (NF-e/NFS-e/NFC-e).
 *
 * <p><strong>⚠️ ESTRUTURA PREPARADA PARA FUTURO ⚠️</strong></p>
 * <p>Esta entidade está preparada para implementação futura de emissão de notas fiscais.
 * Consulte o arquivo {@code docs/NFE_IMPLEMENTATION_PLAN.md} para detalhes completos
 * da implementação planejada.</p>
 *
 * <p><strong>Funcionalidades Planejadas:</strong></p>
 * <ul>
 *   <li>Emissão de NF-e (modelo 55) - SEFAZ estadual</li>
 *   <li>Emissão de NFS-e - Prefeitura municipal</li>
 *   <li>Emissão de NFC-e (modelo 65) - SAT/Contingência</li>
 *   <li>Cancelamento de notas (até 24h)</li>
 *   <li>Carta de Correção Eletrônica (CC-e)</li>
 *   <li>Armazenamento de XMLs (enviado e retornado)</li>
 *   <li>Geração de DANFE em PDF</li>
 *   <li>Integração com certificado digital A1/A3</li>
 * </ul>
 *
 * <p><strong>Requisitos para Emissão:</strong></p>
 * <ul>
 *   <li>Plano PROFISSIONAL ou TURBINADO (validar via PlanoAssinatura)</li>
 *   <li>CNPJ ativo e credenciado na SEFAZ/Prefeitura</li>
 *   <li>Certificado Digital e-CNPJ (A1 ou A3)</li>
 *   <li>Inscrição Estadual/Municipal ativa</li>
 *   <li>Dados fiscais completos na entidade Oficina</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Entity
@Table(
    name = "notas_fiscais",
    indexes = {
        @Index(name = "idx_notas_fiscais_os", columnList = "ordem_servico_id"),
        @Index(name = "idx_notas_fiscais_numero_serie", columnList = "numero, serie"),
        @Index(name = "idx_notas_fiscais_status", columnList = "status"),
        @Index(name = "idx_notas_fiscais_chave_acesso", columnList = "chave_acesso"),
        @Index(name = "idx_notas_fiscais_data_emissao", columnList = "data_emissao")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_nfe_numero_serie", columnNames = {"numero", "serie"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "tipo", "numero", "serie", "status"})
public class NotaFiscal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Oficina à qual esta nota fiscal pertence (multi-tenant).
     * Preenchida automaticamente via TenantContext no @PrePersist.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id")
    private Oficina oficina;

    /**
     * ID da Ordem de Serviço relacionada.
     */
    @Column(name = "ordem_servico_id", nullable = false)
    @NotNull(message = "Ordem de serviço é obrigatória")
    private UUID ordemServicoId;

    /**
     * Tipo de nota fiscal (NF-e, NFS-e, NFC-e).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    @NotNull(message = "Tipo de nota fiscal é obrigatório")
    private TipoNotaFiscal tipo;

    /**
     * Status da nota fiscal.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Status da nota é obrigatório")
    @Builder.Default
    private StatusNotaFiscal status = StatusNotaFiscal.DIGITACAO;

    /**
     * Número sequencial da nota fiscal.
     */
    @Column(name = "numero", nullable = false)
    @NotNull(message = "Número da nota é obrigatório")
    @Min(value = 1, message = "Número da nota deve ser maior que zero")
    private Long numero;

    /**
     * Série da nota fiscal.
     */
    @Column(name = "serie", nullable = false)
    @NotNull(message = "Série da nota é obrigatória")
    @Builder.Default
    private Integer serie = 1;

    /**
     * Chave de acesso da NF-e (44 dígitos).
     * Preenchida após autorização pela SEFAZ.
     */
    @Column(name = "chave_acesso", unique = true, length = 44)
    @Size(max = 44, message = "Chave de acesso deve ter 44 caracteres")
    private String chaveAcesso;

    /**
     * Protocolo de autorização da SEFAZ/Prefeitura.
     */
    @Column(name = "protocolo_autorizacao", length = 50)
    @Size(max = 50, message = "Protocolo deve ter no máximo 50 caracteres")
    private String protocoloAutorizacao;

    /**
     * Data e hora da autorização.
     */
    @Column(name = "data_hora_autorizacao")
    private LocalDateTime dataHoraAutorizacao;

    /**
     * Valor total da nota fiscal.
     */
    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor total deve ser maior que zero")
    private BigDecimal valorTotal;

    /**
     * XML enviado para SEFAZ/Prefeitura (assinado).
     */
    @Column(name = "xml_enviado", columnDefinition = "TEXT")
    private String xmlEnviado;

    /**
     * XML autorizado retornado pela SEFAZ/Prefeitura.
     */
    @Column(name = "xml_autorizado", columnDefinition = "TEXT")
    private String xmlAutorizado;

    /**
     * XML de cancelamento (se houver).
     */
    @Column(name = "xml_cancelamento", columnDefinition = "TEXT")
    private String xmlCancelamento;

    /**
     * Protocolo de cancelamento.
     */
    @Column(name = "protocolo_cancelamento", length = 50)
    @Size(max = 50, message = "Protocolo de cancelamento deve ter no máximo 50 caracteres")
    private String protocoloCancelamento;

    /**
     * Data e hora do cancelamento.
     */
    @Column(name = "data_hora_cancelamento")
    private LocalDateTime dataHoraCancelamento;

    /**
     * Justificativa do cancelamento (mínimo 15 caracteres).
     */
    @Column(name = "justificativa_cancelamento", columnDefinition = "TEXT")
    @Size(min = 15, max = 255, message = "Justificativa deve ter entre 15 e 255 caracteres")
    private String justificativaCancelamento;

    /**
     * Natureza da operação (ex: "PRESTACAO DE SERVICOS").
     */
    @Column(name = "natureza_operacao", length = 60)
    @Size(max = 60, message = "Natureza da operação deve ter no máximo 60 caracteres")
    private String naturezaOperacao;

    /**
     * CFOP (Código Fiscal de Operações) padrão da nota.
     */
    @Column(name = "cfop", length = 4)
    @Size(max = 4, message = "CFOP deve ter 4 caracteres")
    private String cfop;

    /**
     * Informações complementares da nota.
     */
    @Column(name = "informacoes_complementares", columnDefinition = "TEXT")
    private String informacoesComplementares;

    /**
     * Data de emissão da nota.
     */
    @Column(name = "data_emissao", nullable = false)
    @NotNull(message = "Data de emissão é obrigatória")
    private LocalDateTime dataEmissao;

    /**
     * Itens da nota fiscal.
     */
    @OneToMany(mappedBy = "notaFiscal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemNotaFiscal> itens = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Adiciona um item à nota fiscal.
     *
     * @param item item a ser adicionado
     */
    public void adicionarItem(ItemNotaFiscal item) {
        itens.add(item);
        item.setNotaFiscal(this);
    }

    /**
     * Remove um item da nota fiscal.
     *
     * @param item item a ser removido
     */
    public void removerItem(ItemNotaFiscal item) {
        itens.remove(item);
        item.setNotaFiscal(null);
    }

    /**
     * Verifica se a nota pode ser cancelada.
     * Regra: apenas notas autorizadas podem ser canceladas.
     *
     * @return true se pode cancelar
     */
    public boolean podeCancelar() {
        return status == StatusNotaFiscal.AUTORIZADA;
    }

    /**
     * Define oficina automaticamente via TenantContext antes de persistir.
     * Executado automaticamente pelo JPA lifecycle.
     */
    @PrePersist
    protected void setOficinaFromContext() {
        if (this.oficina == null && TenantContext.isSet()) {
            UUID tenantId = TenantContext.getTenantId();
            Oficina oficina = new Oficina();
            oficina.setId(tenantId);
            this.oficina = oficina;
        }
    }
}
