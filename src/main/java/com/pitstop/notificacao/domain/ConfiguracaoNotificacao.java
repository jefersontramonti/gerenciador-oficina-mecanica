package com.pitstop.notificacao.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Configuração de notificações por oficina.
 *
 * Cada oficina pode configurar:
 * - Quais canais estão habilitados (Email, WhatsApp, SMS, Telegram)
 * - Credenciais de integração (Evolution API, SMTP próprio)
 * - Horário comercial para envio
 * - Quais eventos disparam notificações
 *
 * @author PitStop Team
 */
@Entity
@Table(name = "configuracoes_notificacao", indexes = {
    @Index(name = "idx_config_notif_oficina", columnList = "oficina_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoNotificacao {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * ID da oficina. Cada oficina tem UMA configuração.
     */
    @Column(name = "oficina_id", nullable = false, unique = true)
    private UUID oficinaId;

    // ===== CANAIS HABILITADOS =====

    @Column(name = "email_habilitado", nullable = false)
    @Builder.Default
    private Boolean emailHabilitado = true;

    @Column(name = "whatsapp_habilitado", nullable = false)
    @Builder.Default
    private Boolean whatsappHabilitado = false;

    @Column(name = "sms_habilitado", nullable = false)
    @Builder.Default
    private Boolean smsHabilitado = false;

    @Column(name = "telegram_habilitado", nullable = false)
    @Builder.Default
    private Boolean telegramHabilitado = false;

    // ===== CONFIGURAÇÕES DE EMAIL (SMTP Próprio - Opcional) =====

    /**
     * Host do servidor SMTP. NULL = usa configuração global.
     */
    @Column(name = "smtp_host", length = 200)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username", length = 200)
    private String smtpUsername;

    /**
     * Senha SMTP (deve ser criptografada antes de salvar).
     */
    @Column(name = "smtp_password", length = 500)
    private String smtpPassword;

    @Column(name = "smtp_usar_tls", nullable = false)
    @Builder.Default
    private Boolean smtpUsarTls = true;

    @Column(name = "email_remetente", length = 200)
    private String emailRemetente;

    @Column(name = "email_remetente_nome", length = 200)
    private String emailRemetenteNome;

    // ===== CONFIGURAÇÕES DE WHATSAPP (Evolution API) =====

    /**
     * URL base da Evolution API.
     * Exemplo: https://api.evolution.com.br
     */
    @Column(name = "evolution_api_url", length = 500)
    private String evolutionApiUrl;

    /**
     * Token de autenticação da Evolution API.
     * (deve ser criptografado antes de salvar)
     */
    @Column(name = "evolution_api_token", length = 500)
    private String evolutionApiToken;

    /**
     * Nome da instância na Evolution API.
     * Cada oficina tem sua própria instância.
     */
    @Column(name = "evolution_instance_name", length = 100)
    private String evolutionInstanceName;

    /**
     * Número de WhatsApp da oficina (formato internacional).
     * Exemplo: 5511999999999
     */
    @Column(name = "whatsapp_numero", length = 20)
    private String whatsappNumero;

    // ===== CONFIGURAÇÕES DE TELEGRAM =====

    @Column(name = "telegram_bot_token", length = 200)
    private String telegramBotToken;

    @Column(name = "telegram_chat_id", length = 100)
    private String telegramChatId;

    // ===== HORÁRIO COMERCIAL =====

    @Column(name = "respeitar_horario_comercial", nullable = false)
    @Builder.Default
    private Boolean respeitarHorarioComercial = true;

    @Column(name = "horario_inicio", nullable = false)
    @Builder.Default
    private LocalTime horarioInicio = LocalTime.of(8, 0);

    @Column(name = "horario_fim", nullable = false)
    @Builder.Default
    private LocalTime horarioFim = LocalTime.of(18, 0);

    @Column(name = "enviar_sabados", nullable = false)
    @Builder.Default
    private Boolean enviarSabados = true;

    @Column(name = "enviar_domingos", nullable = false)
    @Builder.Default
    private Boolean enviarDomingos = false;

    // ===== EVENTOS HABILITADOS =====

    /**
     * Configuração de quais eventos disparam notificações e por quais canais.
     * Armazenado como JSON.
     *
     * Formato:
     * {
     *   "OS_CRIADA": {"email": true, "whatsapp": false, "delayMinutos": 0},
     *   "OS_FINALIZADA": {"email": true, "whatsapp": true, "delayMinutos": 0}
     * }
     */
    @Column(name = "eventos_habilitados", columnDefinition = "TEXT")
    private String eventosHabilitadosJson;

    // ===== CONFIGURAÇÕES AVANÇADAS =====

    /**
     * Delay entre envios em milissegundos (rate limiting).
     */
    @Column(name = "delay_entre_envios_ms")
    @Builder.Default
    private Integer delayEntreEnviosMs = 1000;

    /**
     * Máximo de tentativas de reenvio em caso de falha.
     */
    @Column(name = "max_tentativas_reenvio")
    @Builder.Default
    private Integer maxTentativasReenvio = 3;

    /**
     * Modo simulação: registra mas não envia realmente.
     */
    @Column(name = "modo_simulacao", nullable = false)
    @Builder.Default
    private Boolean modoSimulacao = false;

    /**
     * Canal de fallback caso o principal falhe.
     * Exemplo: Se WhatsApp falhar, envia por Email.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "canal_fallback", length = 20)
    private TipoNotificacao canalFallback;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== MÉTODOS DE NEGÓCIO =====

    /**
     * Verifica se pode enviar notificação no momento atual.
     * Considera horário comercial e dia da semana.
     */
    public boolean podeEnviarAgora() {
        if (!respeitarHorarioComercial) {
            return true;
        }

        LocalTime agora = LocalTime.now();
        DayOfWeek dia = LocalDate.now().getDayOfWeek();

        // Verifica dia da semana
        if (dia == DayOfWeek.SUNDAY && !enviarDomingos) {
            return false;
        }
        if (dia == DayOfWeek.SATURDAY && !enviarSabados) {
            return false;
        }

        // Verifica horário
        return !agora.isBefore(horarioInicio) && !agora.isAfter(horarioFim);
    }

    /**
     * Obtém a configuração de eventos como Map.
     */
    public Map<EventoNotificacao, EventoConfig> getEventosHabilitados() {
        if (eventosHabilitadosJson == null || eventosHabilitadosJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(
                eventosHabilitadosJson,
                new TypeReference<Map<EventoNotificacao, EventoConfig>>() {}
            );
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    /**
     * Define a configuração de eventos a partir de um Map.
     */
    public void setEventosHabilitados(Map<EventoNotificacao, EventoConfig> eventos) {
        if (eventos == null || eventos.isEmpty()) {
            this.eventosHabilitadosJson = null;
            return;
        }
        try {
            this.eventosHabilitadosJson = objectMapper.writeValueAsString(eventos);
        } catch (JsonProcessingException e) {
            this.eventosHabilitadosJson = null;
        }
    }

    /**
     * Verifica se um evento está habilitado para um canal específico.
     */
    public boolean isEventoHabilitado(EventoNotificacao evento, TipoNotificacao canal) {
        Map<EventoNotificacao, EventoConfig> eventos = getEventosHabilitados();
        EventoConfig config = eventos.get(evento);

        if (config == null) {
            // Se não configurado, usa padrão baseado no canal global
            return switch (canal) {
                case EMAIL -> emailHabilitado;
                case WHATSAPP -> whatsappHabilitado;
                case SMS -> smsHabilitado;
                case TELEGRAM -> telegramHabilitado;
            };
        }

        return switch (canal) {
            case EMAIL -> config.isEmail();
            case WHATSAPP -> config.isWhatsapp();
            case SMS -> config.isSms();
            case TELEGRAM -> config.isTelegram();
        };
    }

    /**
     * Obtém o delay configurado para um evento específico.
     */
    public int getDelayParaEvento(EventoNotificacao evento) {
        Map<EventoNotificacao, EventoConfig> eventos = getEventosHabilitados();
        EventoConfig config = eventos.get(evento);
        return config != null ? config.getDelayMinutos() : 0;
    }

    /**
     * Verifica se a oficina tem SMTP próprio configurado.
     */
    public boolean temSmtpProprio() {
        return smtpHost != null && !smtpHost.isBlank() &&
               smtpUsername != null && !smtpUsername.isBlank();
    }

    /**
     * Verifica se a Evolution API está configurada.
     */
    public boolean temEvolutionApiConfigurada() {
        return evolutionApiUrl != null && !evolutionApiUrl.isBlank() &&
               evolutionApiToken != null && !evolutionApiToken.isBlank() &&
               evolutionInstanceName != null && !evolutionInstanceName.isBlank();
    }

    /**
     * Verifica se Telegram está configurado.
     */
    public boolean temTelegramConfigurado() {
        return telegramBotToken != null && !telegramBotToken.isBlank() &&
               telegramChatId != null && !telegramChatId.isBlank();
    }

    // ===== CLASSE INTERNA PARA CONFIGURAÇÃO DE EVENTO =====

    /**
     * Configuração individual de um evento.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventoConfig {

        @Builder.Default
        private boolean email = true;

        @Builder.Default
        private boolean whatsapp = false;

        @Builder.Default
        private boolean sms = false;

        @Builder.Default
        private boolean telegram = false;

        /**
         * Delay em minutos após o evento antes de enviar.
         * 0 = enviar imediatamente.
         */
        @Builder.Default
        private int delayMinutos = 0;

        /**
         * Factory method para criar configuração padrão (só email).
         */
        public static EventoConfig emailOnly() {
            return EventoConfig.builder().email(true).build();
        }

        /**
         * Factory method para criar configuração completa.
         */
        public static EventoConfig emailEWhatsapp() {
            return EventoConfig.builder().email(true).whatsapp(true).build();
        }
    }
}
