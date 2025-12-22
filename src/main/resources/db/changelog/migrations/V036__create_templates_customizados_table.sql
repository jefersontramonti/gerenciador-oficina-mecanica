-- Migration: V036 - Create templates_customizados table
-- Description: Sistema de templates customizáveis por oficina para notificações
-- Author: PitStop Team
-- Date: 2025-12-21

CREATE TABLE templates_customizados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Ownership
    oficina_id UUID REFERENCES oficinas(id) ON DELETE CASCADE,
    -- NULL = template padrão do sistema (usado quando oficina não tem customização)

    -- Identificação do template
    tipo_template VARCHAR(50) NOT NULL,
    -- Valores: OFICINA_WELCOME, TRIAL_EXPIRING, PAYMENT_OVERDUE, etc

    tipo_notificacao VARCHAR(20) NOT NULL,
    -- Valores: EMAIL, WHATSAPP, SMS, TELEGRAM

    -- Conteúdo do template
    assunto VARCHAR(200),
    -- Usado para EMAIL. Suporta variáveis: {nomeOficina}, {valor}, etc

    corpo TEXT NOT NULL,
    -- Para EMAIL: HTML com Thymeleaf ou placeholders
    -- Para WHATSAPP/SMS: Texto com placeholders {variavel}

    -- Metadados extras
    variaveis_disponiveis JSONB,
    -- Lista de variáveis disponíveis para este template
    -- Exemplo: ["nomeOficina", "valor", "dataVencimento", "diasRestantes"]

    preview_url VARCHAR(500),
    -- URL para visualizar preview do template renderizado

    categoria VARCHAR(50),
    -- Categorias: TRANSACIONAL, MARKETING, SISTEMA, ALERTAS
    -- Ajuda a organizar templates no painel admin

    tags VARCHAR(200),
    -- Tags separadas por vírgula para busca/filtro
    -- Exemplo: "pagamento,cobranca,financeiro"

    -- Status e observações
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    observacoes TEXT,
    -- Notas internas sobre o template

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT templates_customizados_unique_key
        UNIQUE(oficina_id, tipo_template, tipo_notificacao),

    CONSTRAINT templates_customizados_tipo_template_check
        CHECK (tipo_template IN (
            'OFICINA_WELCOME',
            'TRIAL_EXPIRING',
            'TRIAL_EXPIRED',
            'PAYMENT_OVERDUE',
            'PAYMENT_CONFIRMED',
            'OFICINA_SUSPENDED',
            'OFICINA_ACTIVATED',
            'DAILY_METRICS',
            'SYSTEM_ALERT'
        )),

    CONSTRAINT templates_customizados_tipo_notificacao_check
        CHECK (tipo_notificacao IN ('EMAIL', 'WHATSAPP', 'SMS', 'TELEGRAM')),

    CONSTRAINT templates_customizados_categoria_check
        CHECK (categoria IN ('TRANSACIONAL', 'MARKETING', 'SISTEMA', 'ALERTAS') OR categoria IS NULL)
);

-- Índices para performance
CREATE INDEX idx_templates_customizados_oficina ON templates_customizados(oficina_id)
    WHERE ativo = TRUE;

CREATE INDEX idx_templates_customizados_tipo ON templates_customizados(tipo_template, tipo_notificacao)
    WHERE ativo = TRUE;

CREATE INDEX idx_templates_customizados_categoria ON templates_customizados(categoria)
    WHERE ativo = TRUE AND categoria IS NOT NULL;

-- Índice especial para templates padrão do sistema (oficina_id IS NULL)
CREATE INDEX idx_templates_customizados_padrao ON templates_customizados(tipo_template, tipo_notificacao)
    WHERE oficina_id IS NULL AND ativo = TRUE;

-- Comentários
COMMENT ON TABLE templates_customizados IS 'Templates de notificação customizáveis por oficina';
COMMENT ON COLUMN templates_customizados.oficina_id IS 'ID da oficina dona do template. NULL = template padrão do sistema';
COMMENT ON COLUMN templates_customizados.tipo_template IS 'Tipo de template (WELCOME, TRIAL_EXPIRING, PAYMENT_OVERDUE, etc)';
COMMENT ON COLUMN templates_customizados.tipo_notificacao IS 'Canal de notificação (EMAIL, WHATSAPP, SMS, TELEGRAM)';
COMMENT ON COLUMN templates_customizados.assunto IS 'Assunto da notificação (usado para EMAIL). Suporta variáveis: {nome}';
COMMENT ON COLUMN templates_customizados.corpo IS 'Corpo da mensagem. HTML para email, texto para WhatsApp/SMS. Suporta variáveis';
COMMENT ON COLUMN templates_customizados.variaveis_disponiveis IS 'Array JSON com variáveis disponíveis para este template';
COMMENT ON COLUMN templates_customizados.preview_url IS 'URL para preview do template renderizado';
COMMENT ON COLUMN templates_customizados.categoria IS 'Categoria do template (TRANSACIONAL, MARKETING, SISTEMA, ALERTAS)';
COMMENT ON COLUMN templates_customizados.tags IS 'Tags separadas por vírgula para busca e filtro';
