-- Migration: V037 - Create configuracoes_notificacao table
-- Description: Configuracoes de notificacao por oficina (canais, credenciais, horarios)
-- Author: PitStop Team
-- Date: 2025-12-23

CREATE TABLE configuracoes_notificacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Oficina (cada oficina tem UMA configuracao)
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    -- ===== CANAIS HABILITADOS =====
    email_habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    whatsapp_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    sms_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    telegram_habilitado BOOLEAN NOT NULL DEFAULT FALSE,

    -- ===== CONFIGURACOES DE EMAIL (SMTP Proprio - Opcional) =====
    smtp_host VARCHAR(200),
    smtp_port INTEGER,
    smtp_username VARCHAR(200),
    smtp_password VARCHAR(500),
    -- Nota: senha deve ser criptografada antes de salvar
    smtp_usar_tls BOOLEAN NOT NULL DEFAULT TRUE,
    email_remetente VARCHAR(200),
    email_remetente_nome VARCHAR(200),

    -- ===== CONFIGURACOES DE WHATSAPP (Evolution API) =====
    evolution_api_url VARCHAR(500),
    -- URL base da Evolution API. Ex: https://api.evolution.com.br
    evolution_api_token VARCHAR(500),
    -- Token de autenticacao (deve ser criptografado)
    evolution_instance_name VARCHAR(100),
    -- Nome da instancia na Evolution API
    whatsapp_numero VARCHAR(20),
    -- Numero de WhatsApp da oficina (formato: 5511999999999)

    -- ===== CONFIGURACOES DE TELEGRAM =====
    telegram_bot_token VARCHAR(200),
    telegram_chat_id VARCHAR(100),

    -- ===== HORARIO COMERCIAL =====
    respeitar_horario_comercial BOOLEAN NOT NULL DEFAULT TRUE,
    horario_inicio TIME NOT NULL DEFAULT '08:00:00',
    horario_fim TIME NOT NULL DEFAULT '18:00:00',
    enviar_sabados BOOLEAN NOT NULL DEFAULT TRUE,
    enviar_domingos BOOLEAN NOT NULL DEFAULT FALSE,

    -- ===== EVENTOS HABILITADOS =====
    eventos_habilitados TEXT,
    -- JSON com configuracao por evento
    -- Formato: {"OS_CRIADA": {"email": true, "whatsapp": false, "delayMinutos": 0}, ...}

    -- ===== CONFIGURACOES AVANCADAS =====
    delay_entre_envios_ms INTEGER DEFAULT 1000,
    -- Delay entre envios em milissegundos (rate limiting)
    max_tentativas_reenvio INTEGER DEFAULT 3,
    -- Maximo de tentativas de reenvio em caso de falha
    modo_simulacao BOOLEAN NOT NULL DEFAULT FALSE,
    -- Modo simulacao: registra mas nao envia realmente
    canal_fallback VARCHAR(20),
    -- Canal de fallback caso o principal falhe

    -- ===== STATUS =====
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    -- ===== AUDITORIA =====
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT configuracoes_notificacao_oficina_unique UNIQUE(oficina_id),

    CONSTRAINT configuracoes_notificacao_canal_fallback_check
        CHECK (canal_fallback IS NULL OR canal_fallback IN ('EMAIL', 'WHATSAPP', 'SMS', 'TELEGRAM'))
);

-- Indices para performance
CREATE INDEX idx_config_notif_oficina ON configuracoes_notificacao(oficina_id);

-- Comentarios
COMMENT ON TABLE configuracoes_notificacao IS 'Configuracoes de notificacao por oficina';
COMMENT ON COLUMN configuracoes_notificacao.oficina_id IS 'ID da oficina. Cada oficina tem uma unica configuracao';
COMMENT ON COLUMN configuracoes_notificacao.email_habilitado IS 'Se notificacoes por email estao habilitadas';
COMMENT ON COLUMN configuracoes_notificacao.whatsapp_habilitado IS 'Se notificacoes por WhatsApp estao habilitadas (requer plano TURBINADO)';
COMMENT ON COLUMN configuracoes_notificacao.evolution_api_url IS 'URL base da Evolution API para WhatsApp';
COMMENT ON COLUMN configuracoes_notificacao.evolution_api_token IS 'Token de autenticacao da Evolution API (criptografado)';
COMMENT ON COLUMN configuracoes_notificacao.evolution_instance_name IS 'Nome da instancia na Evolution API';
COMMENT ON COLUMN configuracoes_notificacao.eventos_habilitados IS 'JSON com configuracao de eventos por canal';
COMMENT ON COLUMN configuracoes_notificacao.modo_simulacao IS 'Quando TRUE, registra mas nao envia realmente (teste)';
COMMENT ON COLUMN configuracoes_notificacao.canal_fallback IS 'Canal alternativo quando o principal falha';
