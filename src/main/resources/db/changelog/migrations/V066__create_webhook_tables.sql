-- =============================================================================
-- V066: Criação das tabelas de Webhooks
-- Data: 2026-01-19
-- Descrição: Sistema de webhooks para notificar sistemas externos sobre eventos
-- =============================================================================

-- Tabela de configuração de webhooks
CREATE TABLE webhook_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id),
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(200),
    headers_json TEXT,
    max_tentativas INTEGER NOT NULL DEFAULT 3,
    timeout_segundos INTEGER NOT NULL DEFAULT 30,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    falhas_consecutivas INTEGER NOT NULL DEFAULT 0,
    ultima_execucao_sucesso TIMESTAMP,
    ultima_falha TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de eventos por webhook (relacionamento N:N)
CREATE TABLE webhook_config_eventos (
    webhook_config_id UUID NOT NULL REFERENCES webhook_configs(id) ON DELETE CASCADE,
    evento VARCHAR(50) NOT NULL,
    PRIMARY KEY (webhook_config_id, evento)
);

-- Tabela de logs de webhooks
CREATE TABLE webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_config_id UUID NOT NULL REFERENCES webhook_configs(id) ON DELETE CASCADE,
    oficina_id UUID NOT NULL REFERENCES oficinas(id),
    evento VARCHAR(50) NOT NULL,
    entidade_id UUID,
    entidade_tipo VARCHAR(50),
    url VARCHAR(500) NOT NULL,
    payload TEXT,
    http_status INTEGER,
    response_body VARCHAR(2000),
    erro_mensagem VARCHAR(1000),
    tempo_resposta_ms BIGINT,
    tentativa INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    proxima_tentativa TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para webhook_configs
CREATE INDEX idx_webhook_config_oficina ON webhook_configs(oficina_id);
CREATE INDEX idx_webhook_config_ativo ON webhook_configs(ativo);

-- Índices para webhook_logs
CREATE INDEX idx_webhook_log_config ON webhook_logs(webhook_config_id);
CREATE INDEX idx_webhook_log_oficina ON webhook_logs(oficina_id);
CREATE INDEX idx_webhook_log_evento ON webhook_logs(evento);
CREATE INDEX idx_webhook_log_status ON webhook_logs(status);
CREATE INDEX idx_webhook_log_created ON webhook_logs(created_at);
CREATE INDEX idx_webhook_log_proxima ON webhook_logs(proxima_tentativa) WHERE status = 'AGUARDANDO_RETRY';

-- Comentários
COMMENT ON TABLE webhook_configs IS 'Configuração de webhooks por oficina';
COMMENT ON TABLE webhook_config_eventos IS 'Eventos que disparam cada webhook';
COMMENT ON TABLE webhook_logs IS 'Log de envios de webhooks';

COMMENT ON COLUMN webhook_configs.secret IS 'Secret para assinatura HMAC-SHA256 do payload';
COMMENT ON COLUMN webhook_configs.headers_json IS 'Headers customizados em formato JSON';
COMMENT ON COLUMN webhook_configs.falhas_consecutivas IS 'Contador de falhas consecutivas - webhook desativado após 10 falhas';

COMMENT ON COLUMN webhook_logs.status IS 'PENDENTE, SUCESSO, FALHA, AGUARDANDO_RETRY, ESGOTADO';
COMMENT ON COLUMN webhook_logs.proxima_tentativa IS 'Data/hora do próximo retry (backoff exponencial)';
