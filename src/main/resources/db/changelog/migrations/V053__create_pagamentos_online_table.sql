-- =====================================================
-- V053: Criar tabela de pagamentos online
-- =====================================================
-- Rastreia pagamentos processados via gateways online
-- (Mercado Pago, PagSeguro, Stripe, etc.)
-- =====================================================

CREATE TABLE IF NOT EXISTS pagamentos_online (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Relacionamentos
    ordem_servico_id UUID NOT NULL REFERENCES ordem_servico(id) ON DELETE CASCADE,
    pagamento_id UUID REFERENCES pagamentos(id) ON DELETE SET NULL,

    -- Gateway
    gateway VARCHAR(30) NOT NULL CHECK (gateway IN ('MERCADO_PAGO', 'PAGSEGURO', 'STRIPE', 'ASAAS', 'PAGARME')),

    -- IDs externos
    preference_id VARCHAR(255),
    id_externo VARCHAR(255),
    id_cobranca VARCHAR(255),

    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN (
        'PENDENTE', 'PROCESSANDO', 'APROVADO', 'AUTORIZADO', 'EM_ANALISE',
        'REJEITADO', 'CANCELADO', 'ESTORNADO', 'DEVOLVIDO', 'EXPIRADO'
    )),
    status_detalhe VARCHAR(100),

    -- Valores
    valor DECIMAL(10, 2) NOT NULL,
    valor_liquido DECIMAL(10, 2),
    valor_taxa DECIMAL(10, 2),

    -- Método de pagamento
    metodo_pagamento VARCHAR(50),
    bandeira_cartao VARCHAR(50),
    ultimos_digitos VARCHAR(4),
    parcelas INTEGER,

    -- URLs
    url_checkout TEXT,
    url_qr_code TEXT,
    codigo_pix TEXT,

    -- Datas
    data_expiracao TIMESTAMP,
    data_aprovacao TIMESTAMP,

    -- Dados do gateway (JSON)
    resposta_gateway TEXT,
    dados_webhook TEXT,

    -- Erro
    erro_mensagem TEXT,
    erro_codigo VARCHAR(50),
    tentativas INTEGER DEFAULT 0,

    -- Dados do pagador
    email_pagador VARCHAR(255),
    nome_pagador VARCHAR(255),
    documento_pagador VARCHAR(20),

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para otimização
CREATE INDEX IF NOT EXISTS idx_pag_online_oficina ON pagamentos_online(oficina_id);
CREATE INDEX IF NOT EXISTS idx_pag_online_os ON pagamentos_online(ordem_servico_id);
CREATE INDEX IF NOT EXISTS idx_pag_online_pagamento ON pagamentos_online(pagamento_id);
CREATE INDEX IF NOT EXISTS idx_pag_online_id_externo ON pagamentos_online(id_externo);
CREATE INDEX IF NOT EXISTS idx_pag_online_status ON pagamentos_online(status);
CREATE INDEX IF NOT EXISTS idx_pag_online_preference ON pagamentos_online(preference_id);
CREATE INDEX IF NOT EXISTS idx_pag_online_gateway ON pagamentos_online(gateway);

-- Comentários
COMMENT ON TABLE pagamentos_online IS 'Pagamentos processados via gateways online';
COMMENT ON COLUMN pagamentos_online.preference_id IS 'ID da preferência de pagamento no gateway (checkout)';
COMMENT ON COLUMN pagamentos_online.id_externo IS 'ID do pagamento no gateway (payment_id)';
COMMENT ON COLUMN pagamentos_online.id_cobranca IS 'ID da cobrança/order no gateway';
COMMENT ON COLUMN pagamentos_online.status IS 'Status do pagamento: PENDENTE, PROCESSANDO, APROVADO, etc.';
COMMENT ON COLUMN pagamentos_online.url_checkout IS 'URL para redirecionar cliente ao checkout';
COMMENT ON COLUMN pagamentos_online.codigo_pix IS 'Código copia-cola do PIX';
COMMENT ON COLUMN pagamentos_online.resposta_gateway IS 'JSON com resposta completa do gateway';
