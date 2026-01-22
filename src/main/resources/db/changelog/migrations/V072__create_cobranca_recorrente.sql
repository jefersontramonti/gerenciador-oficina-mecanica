-- =====================================================
-- V072: Cobrança Recorrente (Assinaturas)
-- =====================================================
-- Este script cria as tabelas para o módulo de cobranças recorrentes:
-- - Planos de assinatura
-- - Assinaturas de clientes
-- - Faturas recorrentes
-- - Histórico de cobranças
-- =====================================================

-- Feature flag para cobrança recorrente
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria)
VALUES (
    'COBRANCA_RECORRENTE',
    'Cobrança Recorrente',
    'Permite criar assinaturas e cobrar clientes de forma recorrente (mensal, quinzenal, semanal).',
    true,
    'FINANCEIRO'
)
ON CONFLICT (codigo) DO UPDATE SET
    nome = EXCLUDED.nome,
    descricao = EXCLUDED.descricao;

-- Tabela de planos de assinatura (modelos de assinatura da oficina)
CREATE TABLE IF NOT EXISTS planos_assinatura (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),

    -- Configuração de preço e periodicidade
    valor DECIMAL(10,2) NOT NULL,
    periodicidade VARCHAR(20) NOT NULL DEFAULT 'MENSAL',  -- SEMANAL, QUINZENAL, MENSAL, TRIMESTRAL, SEMESTRAL, ANUAL

    -- Serviços incluídos (JSON array com descrições)
    servicos_incluidos JSONB DEFAULT '[]',

    -- Limites do plano (se aplicável)
    limite_os_mes INTEGER,  -- Limite de OS por mês (null = ilimitado)
    desconto_pecas DECIMAL(5,2) DEFAULT 0,  -- Percentual de desconto em peças
    desconto_mao_obra DECIMAL(5,2) DEFAULT 0,  -- Percentual de desconto em mão de obra

    -- Status
    ativo BOOLEAN DEFAULT true,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para planos
CREATE INDEX IF NOT EXISTS idx_planos_assinatura_oficina ON planos_assinatura(oficina_id);
CREATE INDEX IF NOT EXISTS idx_planos_assinatura_ativo ON planos_assinatura(oficina_id, ativo);

-- Tabela de assinaturas de clientes
CREATE TABLE IF NOT EXISTS assinaturas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    cliente_id UUID NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    plano_id UUID NOT NULL REFERENCES planos_assinatura(id) ON DELETE RESTRICT,

    -- Dados da assinatura
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVA',  -- ATIVA, PAUSADA, CANCELADA, INADIMPLENTE
    data_inicio DATE NOT NULL,
    data_fim DATE,  -- NULL se assinatura sem prazo definido
    data_proximo_vencimento DATE NOT NULL,

    -- Valor atual (pode ser diferente do plano se tiver desconto/ajuste)
    valor_atual DECIMAL(10,2) NOT NULL,

    -- Integração com gateway (Mercado Pago)
    gateway_subscription_id VARCHAR(100),  -- ID da assinatura no gateway
    gateway_payer_id VARCHAR(100),  -- ID do pagador no gateway

    -- Configurações específicas da assinatura
    dia_vencimento INTEGER DEFAULT 10,  -- Dia do mês para vencimento
    tolerancia_dias INTEGER DEFAULT 5,  -- Dias de tolerância antes de marcar como inadimplente

    -- Controle de uso
    os_utilizadas_mes INTEGER DEFAULT 0,
    mes_referencia DATE,  -- Mês de referência para contagem de OS

    -- Histórico
    motivo_cancelamento VARCHAR(500),
    data_cancelamento TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para assinaturas
CREATE INDEX IF NOT EXISTS idx_assinaturas_oficina ON assinaturas(oficina_id);
CREATE INDEX IF NOT EXISTS idx_assinaturas_cliente ON assinaturas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_assinaturas_plano ON assinaturas(plano_id);
CREATE INDEX IF NOT EXISTS idx_assinaturas_status ON assinaturas(oficina_id, status);
CREATE INDEX IF NOT EXISTS idx_assinaturas_vencimento ON assinaturas(data_proximo_vencimento);
CREATE INDEX IF NOT EXISTS idx_assinaturas_gateway ON assinaturas(gateway_subscription_id);

-- Tabela de faturas de assinatura
CREATE TABLE IF NOT EXISTS faturas_assinatura (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    assinatura_id UUID NOT NULL REFERENCES assinaturas(id) ON DELETE CASCADE,

    -- Dados da fatura
    numero_fatura VARCHAR(50) NOT NULL,
    mes_referencia DATE NOT NULL,  -- Mês a que a fatura se refere
    valor DECIMAL(10,2) NOT NULL,

    -- Status da fatura
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',  -- PENDENTE, PAGA, VENCIDA, CANCELADA
    data_vencimento DATE NOT NULL,
    data_pagamento TIMESTAMP,

    -- Gateway de pagamento
    gateway_payment_id VARCHAR(100),  -- ID do pagamento no gateway
    gateway_payment_status VARCHAR(50),
    link_pagamento VARCHAR(500),  -- Link para pagamento (PIX, boleto)

    -- Detalhes
    descricao VARCHAR(500),
    observacoes VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_fatura_assinatura_mes UNIQUE (assinatura_id, mes_referencia)
);

-- Índices para faturas
CREATE INDEX IF NOT EXISTS idx_faturas_assinatura_oficina ON faturas_assinatura(oficina_id);
CREATE INDEX IF NOT EXISTS idx_faturas_assinatura_assinatura ON faturas_assinatura(assinatura_id);
CREATE INDEX IF NOT EXISTS idx_faturas_assinatura_status ON faturas_assinatura(oficina_id, status);
CREATE INDEX IF NOT EXISTS idx_faturas_assinatura_vencimento ON faturas_assinatura(data_vencimento);
CREATE INDEX IF NOT EXISTS idx_faturas_assinatura_gateway ON faturas_assinatura(gateway_payment_id);

-- Tabela de histórico de cobranças (tentativas)
CREATE TABLE IF NOT EXISTS historico_cobrancas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    fatura_id UUID NOT NULL REFERENCES faturas_assinatura(id) ON DELETE CASCADE,

    -- Dados da tentativa
    data_tentativa TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_cobranca VARCHAR(20) NOT NULL,  -- AUTOMATICA, MANUAL, WEBHOOK
    resultado VARCHAR(20) NOT NULL,  -- SUCESSO, FALHA, PENDENTE

    -- Detalhes da resposta
    gateway_response JSONB,
    mensagem_erro VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para histórico
CREATE INDEX IF NOT EXISTS idx_historico_cobrancas_fatura ON historico_cobrancas(fatura_id);
CREATE INDEX IF NOT EXISTS idx_historico_cobrancas_data ON historico_cobrancas(data_tentativa);

-- Sequence para número de fatura
CREATE SEQUENCE IF NOT EXISTS seq_fatura_assinatura_numero START 1000;

-- Triggers para atualizar updated_at
CREATE OR REPLACE FUNCTION update_cobranca_recorrente_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_planos_assinatura_updated_at ON planos_assinatura;
CREATE TRIGGER trg_planos_assinatura_updated_at
    BEFORE UPDATE ON planos_assinatura
    FOR EACH ROW EXECUTE FUNCTION update_cobranca_recorrente_updated_at();

DROP TRIGGER IF EXISTS trg_assinaturas_updated_at ON assinaturas;
CREATE TRIGGER trg_assinaturas_updated_at
    BEFORE UPDATE ON assinaturas
    FOR EACH ROW EXECUTE FUNCTION update_cobranca_recorrente_updated_at();

DROP TRIGGER IF EXISTS trg_faturas_assinatura_updated_at ON faturas_assinatura;
CREATE TRIGGER trg_faturas_assinatura_updated_at
    BEFORE UPDATE ON faturas_assinatura
    FOR EACH ROW EXECUTE FUNCTION update_cobranca_recorrente_updated_at();

-- Comentários
COMMENT ON TABLE planos_assinatura IS 'Modelos/templates de planos de assinatura que a oficina oferece';
COMMENT ON TABLE assinaturas IS 'Assinaturas ativas de clientes';
COMMENT ON TABLE faturas_assinatura IS 'Faturas geradas para cada período de assinatura';
COMMENT ON TABLE historico_cobrancas IS 'Histórico de tentativas de cobrança';

COMMENT ON COLUMN assinaturas.gateway_subscription_id IS 'ID da assinatura no Mercado Pago';
COMMENT ON COLUMN faturas_assinatura.link_pagamento IS 'Link de pagamento gerado pelo gateway (PIX, boleto)';
COMMENT ON COLUMN planos_assinatura.servicos_incluidos IS 'Array JSON com descrição dos serviços incluídos no plano';
