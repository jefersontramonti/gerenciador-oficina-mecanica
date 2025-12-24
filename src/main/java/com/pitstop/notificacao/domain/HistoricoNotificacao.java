package com.pitstop.notificacao.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Histórico de notificações enviadas.
 *
 * Registra todas as notificações enviadas para auditoria,
 * suporte, métricas e possível reenvio.
 *
 * @author PitStop Team
 */
@Entity
@Table(name = "historico_notificacoes", indexes = {
    @Index(name = "idx_historico_notif_oficina", columnList = "oficina_id"),
    @Index(name = "idx_historico_notif_destinatario", columnList = "destinatario"),
    @Index(name = "idx_historico_notif_evento", columnList = "evento"),
    @Index(name = "idx_historico_notif_status", columnList = "status"),
    @Index(name = "idx_historico_notif_data", columnList = "data_envio"),
    @Index(name = "idx_historico_notif_os", columnList = "ordem_servico_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoNotificacao {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Oficina que enviou a notificação.
     */
    @Column(name = "oficina_id", nullable = false)
    private UUID oficinaId;

    // ===== IDENTIFICAÇÃO =====

    /**
     * Evento que disparou a notificação.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "evento", nullable = false, length = 50)
    private EventoNotificacao evento;

    /**
     * Canal usado para envio.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_notificacao", nullable = false, length = 20)
    private TipoNotificacao tipoNotificacao;

    // ===== DESTINATÁRIO =====

    /**
     * Destinatário (email ou telefone).
     */
    @Column(name = "destinatario", nullable = false, length = 200)
    private String destinatario;

    /**
     * Nome do destinatário (para referência).
     */
    @Column(name = "nome_destinatario", length = 200)
    private String nomeDestinatario;

    // ===== CONTEÚDO =====

    /**
     * Assunto da notificação (usado em email).
     */
    @Column(name = "assunto", length = 500)
    private String assunto;

    /**
     * Corpo/mensagem da notificação.
     */
    @Column(name = "mensagem", columnDefinition = "TEXT", nullable = false)
    private String mensagem;

    /**
     * Variáveis usadas no template (JSON).
     */
    @Column(name = "variaveis", columnDefinition = "TEXT")
    private String variaveisJson;

    // ===== RASTREAMENTO =====

    /**
     * ID do template usado (se aplicável).
     */
    @Column(name = "template_id")
    private UUID templateId;

    /**
     * ID da ordem de serviço relacionada (se aplicável).
     */
    @Column(name = "ordem_servico_id")
    private UUID ordemServicoId;

    /**
     * ID do cliente relacionado.
     */
    @Column(name = "cliente_id")
    private UUID clienteId;

    /**
     * Usuário que disparou a notificação (se manual).
     * NULL = disparado automaticamente pelo sistema.
     */
    @Column(name = "usuario_id")
    private UUID usuarioId;

    // ===== STATUS E TENTATIVAS =====

    /**
     * Status atual da notificação.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusNotificacao status;

    /**
     * Número de tentativas de envio.
     */
    @Column(name = "tentativas", nullable = false)
    @Builder.Default
    private Integer tentativas = 0;

    /**
     * Data/hora do envio (ou tentativa).
     */
    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    /**
     * Data/hora de confirmação de entrega.
     */
    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    /**
     * Data/hora de leitura (WhatsApp read receipt).
     */
    @Column(name = "data_leitura")
    private LocalDateTime dataLeitura;

    /**
     * Data/hora agendada para envio (se delay configurado).
     */
    @Column(name = "data_agendada")
    private LocalDateTime dataAgendada;

    // ===== RESPOSTA DA API =====

    /**
     * ID externo retornado pela API (Message ID).
     * Usado para rastreamento.
     */
    @Column(name = "id_externo", length = 200)
    private String idExterno;

    /**
     * Mensagem de erro (se falhou).
     */
    @Column(name = "erro_mensagem", columnDefinition = "TEXT")
    private String erroMensagem;

    /**
     * Código de erro da API.
     */
    @Column(name = "erro_codigo", length = 50)
    private String erroCodigo;

    /**
     * Resposta completa da API (JSON).
     */
    @Column(name = "resposta_api", columnDefinition = "TEXT")
    private String respostaApiJson;

    // ===== CUSTOS =====

    /**
     * Custo do envio (para SMS, WhatsApp pago, etc).
     */
    @Column(name = "custo", precision = 10, scale = 4)
    private BigDecimal custo;

    /**
     * Moeda do custo.
     */
    @Column(name = "moeda_custo", length = 3)
    @Builder.Default
    private String moedaCusto = "BRL";

    // ===== METADADOS =====

    /**
     * IP de origem da requisição (se manual).
     */
    @Column(name = "ip_origem", length = 50)
    private String ipOrigem;

    /**
     * User Agent (se via API).
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== MÉTODOS DE NEGÓCIO =====

    /**
     * Obtém as variáveis como Map.
     */
    public Map<String, Object> getVariaveis() {
        if (variaveisJson == null || variaveisJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(
                variaveisJson,
                new TypeReference<Map<String, Object>>() {}
            );
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    /**
     * Define as variáveis a partir de um Map.
     */
    public void setVariaveis(Map<String, Object> variaveis) {
        if (variaveis == null || variaveis.isEmpty()) {
            this.variaveisJson = null;
            return;
        }
        try {
            this.variaveisJson = objectMapper.writeValueAsString(variaveis);
        } catch (JsonProcessingException e) {
            this.variaveisJson = null;
        }
    }

    /**
     * Obtém a resposta da API como Map.
     */
    public Map<String, Object> getRespostaApi() {
        if (respostaApiJson == null || respostaApiJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(
                respostaApiJson,
                new TypeReference<Map<String, Object>>() {}
            );
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    /**
     * Define a resposta da API a partir de um Map.
     */
    public void setRespostaApi(Map<String, Object> resposta) {
        if (resposta == null || resposta.isEmpty()) {
            this.respostaApiJson = null;
            return;
        }
        try {
            this.respostaApiJson = objectMapper.writeValueAsString(resposta);
        } catch (JsonProcessingException e) {
            this.respostaApiJson = null;
        }
    }

    /**
     * Marca a notificação como enviada com sucesso.
     */
    public void marcarComoEnviado(String idExterno) {
        this.status = StatusNotificacao.ENVIADO;
        this.dataEnvio = LocalDateTime.now();
        this.idExterno = idExterno;
        this.erroMensagem = null;
        this.erroCodigo = null;
    }

    /**
     * Marca a notificação como entregue.
     */
    public void marcarComoEntregue() {
        this.status = StatusNotificacao.ENTREGUE;
        this.dataEntrega = LocalDateTime.now();
    }

    /**
     * Marca a notificação como lida.
     */
    public void marcarComoLido() {
        this.status = StatusNotificacao.LIDO;
        this.dataLeitura = LocalDateTime.now();
    }

    /**
     * Marca a notificação como falha.
     */
    public void marcarComoFalha(String erro, String codigo) {
        this.status = StatusNotificacao.FALHA;
        this.erroMensagem = erro;
        this.erroCodigo = codigo;
        this.tentativas++;
    }

    /**
     * Marca como agendado para envio posterior.
     */
    public void agendar(LocalDateTime dataAgendada) {
        this.status = StatusNotificacao.AGENDADO;
        this.dataAgendada = dataAgendada;
    }

    /**
     * Verifica se pode retentar o envio.
     */
    public boolean podeRetentar(int maxTentativas) {
        return tentativas < maxTentativas && status.permiteReenvio();
    }

    /**
     * Verifica se foi disparado manualmente.
     */
    public boolean isManual() {
        return usuarioId != null;
    }

    /**
     * Verifica se foi disparado automaticamente.
     */
    public boolean isAutomatico() {
        return usuarioId == null;
    }

    // ===== FACTORY METHODS =====

    /**
     * Cria um novo histórico pendente.
     */
    public static HistoricoNotificacao criar(
        UUID oficinaId,
        EventoNotificacao evento,
        TipoNotificacao tipoNotificacao,
        String destinatario,
        String nomeDestinatario,
        String assunto,
        String mensagem,
        Map<String, Object> variaveis,
        UUID templateId,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        HistoricoNotificacao historico = HistoricoNotificacao.builder()
            .oficinaId(oficinaId)
            .evento(evento)
            .tipoNotificacao(tipoNotificacao)
            .destinatario(destinatario)
            .nomeDestinatario(nomeDestinatario)
            .assunto(assunto)
            .mensagem(mensagem)
            .templateId(templateId)
            .ordemServicoId(ordemServicoId)
            .clienteId(clienteId)
            .usuarioId(usuarioId)
            .status(StatusNotificacao.PENDENTE)
            .tentativas(0)
            .build();

        historico.setVariaveis(variaveis);

        return historico;
    }
}
