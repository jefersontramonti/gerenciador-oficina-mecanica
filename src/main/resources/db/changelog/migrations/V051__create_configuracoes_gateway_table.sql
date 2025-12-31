-- =====================================================
-- V051: Criar tabela de configurações de gateway de pagamento
-- =====================================================
-- Armazena credenciais e configurações dos gateways de pagamento
-- por oficina (Mercado Pago, PagSeguro, Stripe, etc.)
-- =====================================================

CREATE TABLE IF NOT EXISTS configuracoes_gateway (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Tipo e ambiente do gateway
    tipo_gateway VARCHAR(30) NOT NULL CHECK (tipo_gateway IN ('MERCADO_PAGO', 'PAGSEGURO', 'STRIPE', 'ASAAS', 'PAGARME')),
    ambiente VARCHAR(20) NOT NULL DEFAULT 'SANDBOX' CHECK (ambiente IN ('SANDBOX', 'PRODUCAO')),

    -- Credenciais (criptografadas em produção)
    access_token TEXT,
    public_key VARCHAR(500),
    client_id VARCHAR(255),
    client_secret TEXT,

    -- Webhook
    webhook_url VARCHAR(500),
    webhook_secret VARCHAR(255),

    -- Status e configurações
    ativo BOOLEAN NOT NULL DEFAULT FALSE,
    padrao BOOLEAN NOT NULL DEFAULT FALSE,

    -- Taxas
    taxa_percentual DECIMAL(5, 2),
    taxa_fixa DECIMAL(10, 2),

    -- Observações
    observacoes TEXT,

    -- Validação
    data_ultima_validacao TIMESTAMP,
    status_validacao VARCHAR(20),

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraint: apenas um gateway de cada tipo por oficina
    CONSTRAINT uk_config_gateway_oficina_tipo UNIQUE (oficina_id, tipo_gateway)
);

-- Índices para otimização
CREATE INDEX IF NOT EXISTS idx_config_gateway_oficina ON configuracoes_gateway(oficina_id);
CREATE INDEX IF NOT EXISTS idx_config_gateway_tipo ON configuracoes_gateway(tipo_gateway);
CREATE INDEX IF NOT EXISTS idx_config_gateway_ativo ON configuracoes_gateway(ativo);

-- Comentários
COMMENT ON TABLE configuracoes_gateway IS 'Configurações de gateways de pagamento por oficina';
COMMENT ON COLUMN configuracoes_gateway.tipo_gateway IS 'Tipo do gateway: MERCADO_PAGO, PAGSEGURO, STRIPE, ASAAS, PAGARME';
COMMENT ON COLUMN configuracoes_gateway.ambiente IS 'Ambiente: SANDBOX para testes, PRODUCAO para produção';
COMMENT ON COLUMN configuracoes_gateway.access_token IS 'Token de acesso (chave privada) do gateway';
COMMENT ON COLUMN configuracoes_gateway.public_key IS 'Chave pública para uso no frontend';
COMMENT ON COLUMN configuracoes_gateway.padrao IS 'Se é o gateway padrão para novos pagamentos';
