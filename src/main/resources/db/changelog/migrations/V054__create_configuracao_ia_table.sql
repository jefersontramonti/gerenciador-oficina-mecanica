-- =====================================================
-- V054: Criação da tabela de configuração de IA por oficina
-- =====================================================

-- Tabela para armazenar configurações de IA por oficina
CREATE TABLE IF NOT EXISTS configuracao_ia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL UNIQUE REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Configurações de API
    api_key_encrypted VARCHAR(500),
    provedor VARCHAR(50) NOT NULL DEFAULT 'ANTHROPIC',
    modelo_padrao VARCHAR(100) DEFAULT 'claude-haiku-4-5-20251001',
    modelo_avancado VARCHAR(100) DEFAULT 'claude-sonnet-4-20250514',

    -- Configurações de comportamento
    ia_habilitada BOOLEAN NOT NULL DEFAULT false,
    usar_cache BOOLEAN NOT NULL DEFAULT true,
    usar_pre_validacao BOOLEAN NOT NULL DEFAULT true,
    usar_roteamento_inteligente BOOLEAN NOT NULL DEFAULT true,

    -- Limites
    max_tokens_resposta INTEGER DEFAULT 1000,
    max_requisicoes_dia INTEGER DEFAULT 100,
    requisicoes_hoje INTEGER DEFAULT 0,
    data_reset_contador DATE DEFAULT CURRENT_DATE,

    -- Estatísticas
    total_requisicoes BIGINT DEFAULT 0,
    total_tokens_consumidos BIGINT DEFAULT 0,
    total_cache_hits BIGINT DEFAULT 0,
    total_template_hits BIGINT DEFAULT 0,
    custo_estimado_total DECIMAL(10, 4) DEFAULT 0,

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_configuracao_ia_oficina ON configuracao_ia(oficina_id);
CREATE INDEX IF NOT EXISTS idx_configuracao_ia_habilitada ON configuracao_ia(ia_habilitada);

-- Comentários
COMMENT ON TABLE configuracao_ia IS 'Configurações de IA (Anthropic) por oficina';
COMMENT ON COLUMN configuracao_ia.api_key_encrypted IS 'API Key criptografada da Anthropic';
COMMENT ON COLUMN configuracao_ia.provedor IS 'Provedor de IA (ANTHROPIC por padrão)';
COMMENT ON COLUMN configuracao_ia.modelo_padrao IS 'Modelo para diagnósticos simples (Haiku)';
COMMENT ON COLUMN configuracao_ia.modelo_avancado IS 'Modelo para diagnósticos complexos (Sonnet)';
COMMENT ON COLUMN configuracao_ia.usar_roteamento_inteligente IS 'Se true, usa Haiku para classificar e rotear para modelo adequado';
