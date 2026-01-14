package com.pitstop.anexo.domain;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.usuario.domain.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um anexo (imagem ou documento) no sistema PitStop.
 *
 * <p>Anexos podem ser associados a diferentes entidades do sistema:
 * OrdemServico, Cliente, Peca.</p>
 *
 * <p><strong>Características:</strong></p>
 * <ul>
 *   <li>Multi-tenant: Cada anexo pertence a uma oficina</li>
 *   <li>Soft delete: Anexos são marcados como inativos, não deletados</li>
 *   <li>Metadados: Armazena informações como tamanho, tipo MIME, etc.</li>
 *   <li>Categorização: Anexos são categorizados por tipo</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Entity
@Table(
    name = "anexos",
    indexes = {
        @Index(name = "idx_anexos_oficina_entidade", columnList = "oficina_id, entidade_tipo, entidade_id, ativo"),
        @Index(name = "idx_anexos_entidade", columnList = "entidade_tipo, entidade_id"),
        @Index(name = "idx_anexos_uploaded_at", columnList = "uploaded_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "nomeOriginal", "entidadeTipo", "entidadeId"})
public class Anexo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Oficina (tenant) à qual este anexo pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficina_id", nullable = false)
    @NotNull(message = "Oficina é obrigatória")
    private Oficina oficina;

    /**
     * Tipo da entidade à qual este anexo está associado.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entidade_tipo", nullable = false, length = 50)
    @NotNull(message = "Tipo de entidade é obrigatório")
    private EntidadeTipo entidadeTipo;

    /**
     * ID da entidade à qual este anexo está associado.
     */
    @Column(name = "entidade_id", nullable = false)
    @NotNull(message = "ID da entidade é obrigatório")
    private UUID entidadeId;

    /**
     * Categoria do anexo (FOTO_VEICULO, DIAGNOSTICO, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 50)
    @Builder.Default
    private CategoriaAnexo categoria = CategoriaAnexo.OUTROS;

    /**
     * Nome original do arquivo enviado pelo usuário.
     */
    @Column(name = "nome_original", nullable = false)
    @NotBlank(message = "Nome original é obrigatório")
    @Size(max = 255, message = "Nome original deve ter no máximo 255 caracteres")
    private String nomeOriginal;

    /**
     * Nome do arquivo armazenado (UUID gerado para evitar colisões).
     */
    @Column(name = "nome_arquivo", nullable = false)
    @NotBlank(message = "Nome do arquivo é obrigatório")
    @Size(max = 255, message = "Nome do arquivo deve ter no máximo 255 caracteres")
    private String nomeArquivo;

    /**
     * Tamanho do arquivo em bytes.
     */
    @Column(name = "tamanho_bytes", nullable = false)
    @NotNull(message = "Tamanho é obrigatório")
    private Long tamanhoBytes;

    /**
     * Tipo MIME do arquivo (image/jpeg, application/pdf, etc.).
     */
    @Column(name = "mime_type", nullable = false, length = 100)
    @NotBlank(message = "Tipo MIME é obrigatório")
    @Size(max = 100, message = "Tipo MIME deve ter no máximo 100 caracteres")
    private String mimeType;

    /**
     * Caminho relativo do arquivo no storage.
     * Ex: /oficina-123/ordens-servico/2026/01/uuid.jpg
     */
    @Column(name = "caminho_arquivo", nullable = false, length = 500)
    @NotBlank(message = "Caminho do arquivo é obrigatório")
    @Size(max = 500, message = "Caminho do arquivo deve ter no máximo 500 caracteres")
    private String caminhoArquivo;

    /**
     * Descrição opcional do anexo.
     */
    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    /**
     * Usuário que fez o upload do anexo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private Usuario uploadedBy;

    /**
     * Data/hora do upload.
     */
    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    /**
     * Flag de ativo para soft delete.
     */
    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /**
     * Flag que indica se o anexo será visível para o cliente
     * na página pública de aprovação de orçamento.
     */
    @Column(name = "visivel_cliente", nullable = false)
    @Builder.Default
    private Boolean visivelParaCliente = false;

    /**
     * Verifica se o anexo é uma imagem.
     */
    public boolean isImagem() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Verifica se o anexo é um PDF.
     */
    public boolean isPdf() {
        return "application/pdf".equals(mimeType);
    }

    /**
     * Retorna o tamanho formatado (ex: "1.5 MB").
     */
    public String getTamanhoFormatado() {
        if (tamanhoBytes == null) return "0 B";

        if (tamanhoBytes < 1024) {
            return tamanhoBytes + " B";
        } else if (tamanhoBytes < 1024 * 1024) {
            return String.format("%.1f KB", tamanhoBytes / 1024.0);
        } else {
            return String.format("%.1f MB", tamanhoBytes / (1024.0 * 1024));
        }
    }

    /**
     * Desativa o anexo (soft delete).
     */
    public void desativar() {
        this.ativo = false;
    }
}
