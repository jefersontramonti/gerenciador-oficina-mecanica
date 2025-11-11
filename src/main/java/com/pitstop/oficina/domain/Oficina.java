package com.pitstop.oficina.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pitstop.cliente.domain.Endereco;
import com.pitstop.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Entity representing an automotive workshop in the PitStop system.
 *
 * <p><b>Single-Tenant Model (Current):</b> Only ONE workshop per installation</p>
 * <p><b>Multi-Tenant Model (Future):</b> Multiple workshops with oficinaId as tenant identifier</p>
 *
 * <p><b>Important Business Rules:</b></p>
 * <ul>
 *   <li>CNPJ/CPF must be unique in the system</li>
 *   <li>Email must be unique</li>
 *   <li>Fiscal data (inscricaoEstadual, etc.) is OPTIONAL for ECONOMICO plan</li>
 *   <li>Fiscal data is REQUIRED for PROFISSIONAL and TURBINADO plans (invoice emission)</li>
 *   <li>Status ATIVA is required to use the system</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "oficinas",
       indexes = {
           @Index(name = "idx_oficinas_cnpj_cpf", columnList = "cnpj_cpf"),
           @Index(name = "idx_oficinas_email", columnList = "email"),
           @Index(name = "idx_oficinas_status", columnList = "status"),
           @Index(name = "idx_oficinas_plano", columnList = "plano")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oficina extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    // =====================================
    // IDENTIFICAÇÃO BÁSICA
    // =====================================

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome_fantasia", nullable = false, length = 200)
    @NotBlank(message = "Nome fantasia é obrigatório")
    @Size(max = 200, message = "Nome fantasia deve ter no máximo 200 caracteres")
    private String nomeFantasia;

    @Column(name = "razao_social", nullable = false, length = 200)
    @NotBlank(message = "Razão social é obrigatória")
    @Size(max = 200, message = "Razão social deve ter no máximo 200 caracteres")
    private String razaoSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 20)
    @NotNull(message = "Tipo de pessoa é obrigatório")
    private TipoPessoa tipoPessoa;

    @Column(name = "cnpj_cpf", unique = true, nullable = false, length = 18)
    @NotBlank(message = "CNPJ/CPF é obrigatório")
    @Size(max = 18, message = "CNPJ/CPF deve ter no máximo 18 caracteres")
    private String cnpjCpf;

    // =====================================
    // DOCUMENTAÇÃO (Opcional para Plano Econômico)
    // =====================================

    @Column(name = "inscricao_estadual", length = 20)
    @Size(max = 20, message = "Inscrição estadual deve ter no máximo 20 caracteres")
    private String inscricaoEstadual;

    @Column(name = "inscricao_municipal", length = 20)
    @Size(max = 20, message = "Inscrição municipal deve ter no máximo 20 caracteres")
    private String inscricaoMunicipal;

    // =====================================
    // RESPONSÁVEL LEGAL
    // =====================================

    @Column(name = "nome_responsavel", nullable = false, length = 200)
    @NotBlank(message = "Nome do responsável é obrigatório")
    @Size(max = 200, message = "Nome do responsável deve ter no máximo 200 caracteres")
    private String nomeResponsavel;

    @JsonIgnore // Dado sensível - LGPD
    @Column(name = "cpf_responsavel", length = 14)
    @Size(max = 14, message = "CPF do responsável deve ter no máximo 14 caracteres")
    private String cpfResponsavel;

    // =====================================
    // CONTATO
    // =====================================

    @Embedded
    @Valid
    @NotNull(message = "Dados de contato são obrigatórios")
    private Contato contato;

    // =====================================
    // ENDEREÇO
    // =====================================

    @Embedded
    @Valid
    @NotNull(message = "Endereço é obrigatório")
    @AttributeOverrides({
        @AttributeOverride(name = "logradouro", column = @Column(name = "endereco_logradouro")),
        @AttributeOverride(name = "numero", column = @Column(name = "endereco_numero")),
        @AttributeOverride(name = "complemento", column = @Column(name = "endereco_complemento")),
        @AttributeOverride(name = "bairro", column = @Column(name = "endereco_bairro")),
        @AttributeOverride(name = "cidade", column = @Column(name = "endereco_cidade")),
        @AttributeOverride(name = "estado", column = @Column(name = "endereco_estado")),
        @AttributeOverride(name = "cep", column = @Column(name = "endereco_cep"))
    })
    private Endereco endereco;

    // =====================================
    // INFORMAÇÕES OPERACIONAIS
    // =====================================

    @Embedded
    private InformacoesOperacionais informacoesOperacionais;

    // =====================================
    // REDES SOCIAIS
    // =====================================

    @Embedded
    private RedesSociais redesSociais;

    // =====================================
    // DADOS BANCÁRIOS (Opcional)
    // =====================================

    @Embedded
    private DadosBancarios dadosBancarios;

    // =====================================
    // CONFIGURAÇÕES
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Status é obrigatório")
    @Builder.Default
    private StatusOficina status = StatusOficina.ATIVA;

    @Column(name = "taxa_desconto_maxima", precision = 5, scale = 2)
    private BigDecimal taxaDescontoMaxima;

    // =====================================
    // PLANO E BILLING
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(name = "plano", length = 20)
    @Builder.Default
    private PlanoAssinatura plano = PlanoAssinatura.ECONOMICO;

    @Column(name = "data_assinatura")
    private LocalDate dataAssinatura;

    @Column(name = "data_vencimento_plano")
    private LocalDate dataVencimentoPlano;

    @Column(name = "valor_mensalidade", precision = 10, scale = 2)
    private BigDecimal valorMensalidade;

    // =====================================
    // FISCAL (Obrigatório apenas para planos com NF)
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(name = "regime_tributario", length = 30)
    private RegimeTributario regimeTributario;

    @Column(name = "aliquota_issqn", precision = 5, scale = 2)
    private BigDecimal aliquotaIssqn;

    // =====================================
    // MÍDIA
    // =====================================

    @Column(name = "logo_url", length = 500)
    @Size(max = 500, message = "URL do logo deve ter no máximo 500 caracteres")
    private String logoUrl;

    // =====================================
    // OBSERVAÇÕES
    // =====================================

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    // =====================================
    // CONTROLE INTERNO
    // =====================================

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;

    // =====================================
    // MÉTODOS DE NEGÓCIO
    // =====================================

    /**
     * Checks if the workshop can operate (active status and subscription valid).
     *
     * @return true if status is ATIVA and not ativo=false
     */
    public boolean podeOperar() {
        return status.isOperacional() && Boolean.TRUE.equals(ativo);
    }

    /**
     * Checks if the subscription plan is expired.
     *
     * @return true if dataVencimentoPlano is in the past
     */
    public boolean isPlanoVencido() {
        return dataVencimentoPlano != null &&
               dataVencimentoPlano.isBefore(LocalDate.now());
    }

    /**
     * Gets days remaining until plan expiration.
     *
     * @return number of days, or -1 if no expiration date set
     */
    public long getDiasAteVencimento() {
        if (dataVencimentoPlano == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), dataVencimentoPlano);
    }

    /**
     * Checks if fiscal data is required for current plan.
     *
     * @return true if plan requires invoice emission (PROFISSIONAL or TURBINADO)
     */
    public boolean requerDadosFiscais() {
        return plano != null && plano.isEmiteNotaFiscal();
    }

    /**
     * Validates if fiscal data is complete when required.
     *
     * @return true if fiscal data is complete or not required
     */
    public boolean isDadosFiscaisCompletos() {
        if (!requerDadosFiscais()) {
            return true; // Not required for ECONOMICO plan
        }

        return inscricaoEstadual != null && !inscricaoEstadual.isBlank() &&
               regimeTributario != null;
    }

    /**
     * Records last access timestamp.
     */
    public void registrarAcesso() {
        this.ultimoAcesso = LocalDateTime.now();
    }

    /**
     * Activates the workshop.
     */
    public void ativar() {
        this.status = StatusOficina.ATIVA;
        this.ativo = true;
    }

    /**
     * Deactivates the workshop temporarily.
     */
    public void desativar() {
        this.status = StatusOficina.INATIVA;
        this.ativo = false;
    }

    /**
     * Suspends the workshop (e.g., due to payment issues).
     */
    public void suspender() {
        this.status = StatusOficina.SUSPENSA;
        this.ativo = false;
    }

    /**
     * Cancels the workshop account permanently.
     */
    public void cancelar() {
        this.status = StatusOficina.CANCELADA;
        this.ativo = false;
    }
}
