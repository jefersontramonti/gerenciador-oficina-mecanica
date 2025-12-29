-- liquibase formatted sql

-- changeset pitstop:create-planos-table
-- comment: Creates table for dynamic subscription plan management. Complements the existing PlanoAssinatura enum.

-- Create planos table
CREATE TABLE planos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Basic Info
    codigo VARCHAR(30) NOT NULL UNIQUE,  -- ECONOMICO, PROFISSIONAL, TURBINADO, CUSTOM_001
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,

    -- Pricing
    valor_mensal DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    valor_anual DECIMAL(10, 2),
    trial_dias INTEGER DEFAULT 14,

    -- Limits (-1 = unlimited)
    limite_usuarios INTEGER DEFAULT 1,
    limite_os_mes INTEGER DEFAULT -1,
    limite_clientes INTEGER DEFAULT -1,
    limite_espaco_mb BIGINT DEFAULT 5120,  -- 5GB default
    limite_api_calls INTEGER DEFAULT -1,
    limite_whatsapp_mensagens INTEGER DEFAULT 0,
    limite_emails_mes INTEGER DEFAULT 100,

    -- Features as JSONB for flexibility
    features JSONB DEFAULT '{
        "emiteNotaFiscal": false,
        "whatsappAutomatizado": false,
        "manutencaoPreventiva": false,
        "anexoImagensDocumentos": false,
        "relatoriosAvancados": false,
        "integracaoMercadoPago": false,
        "suportePrioritario": false,
        "backupAutomatico": true
    }'::jsonb,

    -- Display & Marketing
    ativo BOOLEAN DEFAULT TRUE,
    visivel BOOLEAN DEFAULT TRUE,  -- Show on pricing page
    recomendado BOOLEAN DEFAULT FALSE,  -- Highlight as recommended
    cor_destaque VARCHAR(20),  -- Hex color for UI
    tag_promocao VARCHAR(50),  -- "Mais Popular", "Melhor Custo-Benefício", etc.
    ordem_exibicao INTEGER DEFAULT 0,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_planos_codigo ON planos(codigo);
CREATE INDEX idx_planos_ativo ON planos(ativo);
CREATE INDEX idx_planos_visivel ON planos(visivel);
CREATE INDEX idx_planos_ordem ON planos(ordem_exibicao);

-- Insert default plans matching the current enum
INSERT INTO planos (
    codigo, nome, descricao, valor_mensal, valor_anual, trial_dias,
    limite_usuarios, limite_os_mes, limite_clientes, limite_espaco_mb,
    limite_whatsapp_mensagens, limite_emails_mes,
    features, ativo, visivel, recomendado, cor_destaque, tag_promocao, ordem_exibicao
) VALUES
(
    'ECONOMICO',
    'Econômico',
    'Plano ideal para oficinas iniciantes. Inclui controle de caixa, backup automático, controle de estoque, ordens de serviço, financeiro, app Motorok, relatórios, suporte online, remanufatura, e-mails automáticos, treinamento online e vendas de balcão.',
    160.00,
    1536.00,  -- 160 * 12 * 0.8 (20% discount for annual)
    14,
    1,    -- 1 user
    -1,   -- unlimited OS
    -1,   -- unlimited clients
    2048, -- 2GB
    0,    -- no WhatsApp
    50,   -- 50 emails/month
    '{
        "emiteNotaFiscal": false,
        "whatsappAutomatizado": false,
        "manutencaoPreventiva": false,
        "anexoImagensDocumentos": false,
        "relatoriosAvancados": false,
        "integracaoMercadoPago": true,
        "suportePrioritario": false,
        "backupAutomatico": true
    }'::jsonb,
    TRUE,
    TRUE,
    FALSE,
    '#3B82F6',  -- Blue
    NULL,
    1
),
(
    'PROFISSIONAL',
    'Profissional',
    'Plano mais popular! Inclui todos os recursos do plano Econômico, mais emissão de NF-e, NFS-e e NFC-e. Perfeito para oficinas em crescimento que precisam emitir notas fiscais.',
    250.00,
    2400.00,  -- 250 * 12 * 0.8
    14,
    3,    -- 3 users
    -1,   -- unlimited OS
    -1,   -- unlimited clients
    5120, -- 5GB
    0,    -- no WhatsApp
    100,  -- 100 emails/month
    '{
        "emiteNotaFiscal": true,
        "whatsappAutomatizado": false,
        "manutencaoPreventiva": false,
        "anexoImagensDocumentos": false,
        "relatoriosAvancados": true,
        "integracaoMercadoPago": true,
        "suportePrioritario": false,
        "backupAutomatico": true
    }'::jsonb,
    TRUE,
    TRUE,
    TRUE,  -- Recommended
    '#8B5CF6',  -- Purple
    'Mais Popular',
    2
),
(
    'TURBINADO',
    'Turbinado',
    'Plano completo para oficinas de alto volume. Inclui todos os recursos anteriores, mais automação de WhatsApp, manutenção preventiva, anexos de imagens/documentos e suporte prioritário. Ideal para redes de oficinas.',
    0.00,  -- Custom pricing
    0.00,
    30,   -- 30 days trial
    -1,   -- unlimited users
    -1,   -- unlimited OS
    -1,   -- unlimited clients
    -1,   -- unlimited storage
    500,  -- 500 WhatsApp messages/month
    500,  -- 500 emails/month
    '{
        "emiteNotaFiscal": true,
        "whatsappAutomatizado": true,
        "manutencaoPreventiva": true,
        "anexoImagensDocumentos": true,
        "relatoriosAvancados": true,
        "integracaoMercadoPago": true,
        "suportePrioritario": true,
        "backupAutomatico": true
    }'::jsonb,
    TRUE,
    TRUE,
    FALSE,
    '#F59E0B',  -- Amber/Gold
    'Enterprise',
    3
);

-- Add comment
COMMENT ON TABLE planos IS 'Subscription plans for the PitStop SaaS platform. Managed by SUPER_ADMIN.';
COMMENT ON COLUMN planos.codigo IS 'Unique code for the plan, matches PlanoAssinatura enum values';
COMMENT ON COLUMN planos.features IS 'JSON object containing feature flags for the plan';
COMMENT ON COLUMN planos.limite_espaco_mb IS 'Storage limit in megabytes, -1 for unlimited';
