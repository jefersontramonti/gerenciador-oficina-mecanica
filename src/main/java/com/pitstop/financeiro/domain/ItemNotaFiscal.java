package com.pitstop.financeiro.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um item de uma Nota Fiscal Eletrônica.
 *
 * <p><strong>⚠️ ESTRUTURA PREPARADA PARA FUTURO ⚠️</strong></p>
 * <p>Esta entidade faz parte da estrutura planejada para emissão de NF-e/NFS-e/NFC-e.
 * Consulte {@code docs/NFE_IMPLEMENTATION_PLAN.md} para detalhes.</p>
 *
 * <p>Cada item representa um produto ou serviço na nota fiscal, com
 * informações tributárias completas (ICMS, PIS, COFINS, IPI, ISS).</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Entity
@Table(
    name = "itens_nota_fiscal",
    indexes = {
        @Index(name = "idx_itens_nf_nota_fiscal_id", columnList = "nota_fiscal_id"),
        @Index(name = "idx_itens_nf_numero_item", columnList = "nota_fiscal_id, numero_item")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_item_nfe", columnNames = {"nota_fiscal_id", "numero_item"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "numeroItem", "descricao", "quantidade", "valorTotal"})
public class ItemNotaFiscal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Nota fiscal à qual este item pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_fiscal_id", nullable = false)
    @NotNull(message = "Nota fiscal é obrigatória")
    private NotaFiscal notaFiscal;

    /**
     * Número sequencial do item dentro da nota (1, 2, 3...).
     */
    @Column(name = "numero_item", nullable = false)
    @NotNull(message = "Número do item é obrigatório")
    @Min(value = 1, message = "Número do item deve ser maior que zero")
    private Integer numeroItem;

    /**
     * Código do produto/serviço (interno da oficina).
     */
    @Column(name = "codigo", length = 60)
    @Size(max = 60, message = "Código deve ter no máximo 60 caracteres")
    private String codigo;

    /**
     * Código EAN/GTIN (para produtos).
     */
    @Column(name = "codigo_ean", length = 14)
    @Size(max = 14, message = "Código EAN deve ter no máximo 14 caracteres")
    private String codigoEAN;

    /**
     * Descrição do produto/serviço.
     */
    @Column(name = "descricao", nullable = false, length = 120)
    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 120, message = "Descrição deve ter no máximo 120 caracteres")
    private String descricao;

    /**
     * NCM - Nomenclatura Comum do Mercosul (8 dígitos).
     * Obrigatório para produtos. Serviços usam "00".
     */
    @Column(name = "ncm", length = 8)
    @Size(max = 8, message = "NCM deve ter 8 caracteres")
    private String ncm;

    /**
     * CEST - Código Especificador da Substituição Tributária.
     */
    @Column(name = "cest", length = 7)
    @Size(max = 7, message = "CEST deve ter 7 caracteres")
    private String cest;

    /**
     * CFOP - Código Fiscal de Operações (4 dígitos).
     * Ex: 5933 - Prestação de serviço sujeito ao ICMS.
     */
    @Column(name = "cfop", nullable = false, length = 4)
    @NotBlank(message = "CFOP é obrigatório")
    @Size(max = 4, message = "CFOP deve ter 4 caracteres")
    private String cfop;

    /**
     * Unidade comercial (UN, PC, KG, HR, etc.).
     */
    @Column(name = "unidade_comercial", nullable = false, length = 6)
    @NotBlank(message = "Unidade comercial é obrigatória")
    @Size(max = 6, message = "Unidade comercial deve ter no máximo 6 caracteres")
    private String unidadeComercial;

    /**
     * Quantidade do item.
     */
    @Column(name = "quantidade", nullable = false, precision = 15, scale = 4)
    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
    private BigDecimal quantidade;

    /**
     * Valor unitário do item.
     */
    @Column(name = "valor_unitario", nullable = false, precision = 15, scale = 4)
    @NotNull(message = "Valor unitário é obrigatório")
    @DecimalMin(value = "0.0001", message = "Valor unitário deve ser maior que zero")
    private BigDecimal valorUnitario;

    /**
     * Valor total do item (quantidade * valorUnitario - desconto + despesas).
     */
    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor total deve ser maior que zero")
    private BigDecimal valorTotal;

    /**
     * Valor de desconto aplicado no item.
     */
    @Column(name = "valor_desconto", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valorDesconto = BigDecimal.ZERO;

    /**
     * Outras despesas acessórias (frete, seguro, etc.).
     */
    @Column(name = "valor_outras_despesas", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valorOutrasDespesas = BigDecimal.ZERO;

    // =====================================
    // TRIBUTAÇÃO (Placeholder para futuro)
    // =====================================

    /**
     * CST/CSOSN de ICMS (2 ou 3 dígitos).
     * Ex: "102" (Simples Nacional - sem tributação).
     */
    @Column(name = "cst_icms", length = 3)
    @Size(max = 3, message = "CST ICMS deve ter no máximo 3 caracteres")
    private String cstICMS;

    /**
     * Alíquota de ICMS (%).
     */
    @Column(name = "aliquota_icms", precision = 5, scale = 2)
    private BigDecimal aliquotaICMS;

    /**
     * Valor do ICMS.
     */
    @Column(name = "valor_icms", precision = 15, scale = 2)
    private BigDecimal valorICMS;

    /**
     * CST de PIS (2 dígitos).
     */
    @Column(name = "cst_pis", length = 2)
    @Size(max = 2, message = "CST PIS deve ter 2 caracteres")
    private String cstPIS;

    /**
     * Alíquota de PIS (%).
     */
    @Column(name = "aliquota_pis", precision = 5, scale = 2)
    private BigDecimal aliquotaPIS;

    /**
     * Valor do PIS.
     */
    @Column(name = "valor_pis", precision = 15, scale = 2)
    private BigDecimal valorPIS;

    /**
     * CST de COFINS (2 dígitos).
     */
    @Column(name = "cst_cofins", length = 2)
    @Size(max = 2, message = "CST COFINS deve ter 2 caracteres")
    private String cstCOFINS;

    /**
     * Alíquota de COFINS (%).
     */
    @Column(name = "aliquota_cofins", precision = 5, scale = 2)
    private BigDecimal aliquotaCOFINS;

    /**
     * Valor do COFINS.
     */
    @Column(name = "valor_cofins", precision = 15, scale = 2)
    private BigDecimal valorCOFINS;

    /**
     * Alíquota de IPI (%).
     */
    @Column(name = "aliquota_ipi", precision = 5, scale = 2)
    private BigDecimal aliquotaIPI;

    /**
     * Valor do IPI.
     */
    @Column(name = "valor_ipi", precision = 15, scale = 2)
    private BigDecimal valorIPI;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
