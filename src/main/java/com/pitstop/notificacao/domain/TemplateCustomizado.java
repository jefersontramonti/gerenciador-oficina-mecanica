package com.pitstop.notificacao.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Template de notificação customizado por oficina.
 *
 * Permite que cada oficina personalize seus próprios templates
 * de email, WhatsApp, SMS, etc.
 *
 * Se a oficina não tiver um template customizado, o sistema
 * usa o template padrão.
 *
 * @author PitStop Team
 */
@Entity
@Table(name = "templates_customizados", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"oficina_id", "tipo_template", "tipo_notificacao"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateCustomizado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * ID da oficina dona do template.
     * NULL = template padrão do sistema.
     */
    @Column(name = "oficina_id")
    private UUID oficinaId;

    /**
     * Tipo de template (WELCOME, TRIAL_EXPIRING, etc).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_template", nullable = false, length = 50)
    private TemplateNotificacao tipoTemplate;

    /**
     * Canal de notificação (EMAIL, WHATSAPP, SMS, TELEGRAM).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_notificacao", nullable = false, length = 20)
    private TipoNotificacao tipoNotificacao;

    /**
     * Assunto do template (usado para EMAIL).
     * Suporta variáveis: {nomeOficina}, {valor}, etc.
     */
    @Column(name = "assunto", length = 200)
    private String assunto;

    /**
     * Corpo do template.
     *
     * Para EMAIL: HTML com variáveis Thymeleaf ou placeholders simples
     * Para WHATSAPP/SMS/TELEGRAM: Texto com placeholders: {variavel}
     *
     * Exemplo WhatsApp:
     * "Olá {nomeCliente}, sua OS #{numeroOS} está pronta! Valor: R$ {valor}"
     *
     * Exemplo Email HTML:
     * "<h1>Olá [[${nomeCliente}]]</h1><p>Sua OS #[[${numeroOS}]] está pronta!</p>"
     */
    @Column(name = "corpo", columnDefinition = "TEXT", nullable = false)
    private String corpo;

    /**
     * Template está ativo?
     */
    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /**
     * Variáveis disponíveis para este template.
     * Armazenado como JSON array.
     * Exemplo: ["nomeOficina", "valor", "dataVencimento"]
     */
    @Column(name = "variaveis_disponiveis", columnDefinition = "JSONB")
    private String variaveisDisponiveis;

    /**
     * URL para preview do template.
     * Pode ser usado no frontend para mostrar como ficará a notificação.
     */
    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    /**
     * Categoria do template.
     * TRANSACIONAL: Notificações de ações do sistema (confirmações, alertas)
     * MARKETING: Campanhas promocionais
     * SISTEMA: Notificações internas (métricas, alertas para admins)
     * ALERTAS: Avisos importantes (pagamentos, suspensões)
     */
    @Column(name = "categoria", length = 50)
    private String categoria;

    /**
     * Tags para organização e busca.
     * Separadas por vírgula.
     * Exemplo: "pagamento,cobranca,financeiro"
     */
    @Column(name = "tags", length = 200)
    private String tags;

    /**
     * Observações sobre o template (uso interno).
     */
    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Verifica se é um template padrão do sistema.
     *
     * @return true se oficina_id é NULL
     */
    public boolean isTemplatePadrao() {
        return oficinaId == null;
    }

    /**
     * Processa o corpo do template substituindo variáveis.
     *
     * Para templates simples (WhatsApp/SMS), substitui {variavel} por valores.
     * Para templates HTML (Email), usa Thymeleaf então não precisa substituir aqui.
     *
     * @param variaveis Mapa de variáveis
     * @return Corpo processado
     */
    public String processarCorpo(java.util.Map<String, Object> variaveis) {
        if (tipoNotificacao == TipoNotificacao.EMAIL) {
            // Email usa Thymeleaf, não precisa processar aqui
            return corpo;
        }

        // Para WhatsApp/SMS/Telegram, substitui placeholders simples
        String resultado = corpo;
        if (variaveis != null) {
            for (java.util.Map.Entry<String, Object> entry : variaveis.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String valor = entry.getValue() != null ? entry.getValue().toString() : "";
                resultado = resultado.replace(placeholder, valor);
            }
        }
        return resultado;
    }

    /**
     * Processa o assunto substituindo variáveis.
     *
     * @param variaveis Mapa de variáveis
     * @return Assunto processado
     */
    public String processarAssunto(java.util.Map<String, Object> variaveis) {
        if (assunto == null) {
            return tipoTemplate.getSubject();
        }

        String resultado = assunto;
        if (variaveis != null) {
            for (java.util.Map.Entry<String, Object> entry : variaveis.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String valor = entry.getValue() != null ? entry.getValue().toString() : "";
                resultado = resultado.replace(placeholder, valor);
            }
        }
        return resultado;
    }
}
