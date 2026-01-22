-- =====================================================
-- V071: Conciliação Bancária
-- =====================================================
-- Este script cria as tabelas para o módulo de conciliação bancária:
-- - Extratos bancários importados (OFX/CSV)
-- - Transações do extrato
-- - Conciliações entre transações e pagamentos
-- =====================================================

-- Feature flag para conciliação bancária
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria)
VALUES (
    'CONCILIACAO_BANCARIA',
    'Conciliação Bancária',
    'Permite importar extratos bancários (OFX) e conciliar automaticamente com os pagamentos do sistema.',
    true,
    'FINANCEIRO'
)
ON CONFLICT (codigo) DO UPDATE SET
    nome = EXCLUDED.nome,
    descricao = EXCLUDED.descricao;

-- Tabela de contas bancárias configuradas
CREATE TABLE IF NOT EXISTS contas_bancarias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    nome VARCHAR(100) NOT NULL,
    banco VARCHAR(100) NOT NULL,
    agencia VARCHAR(20),
    conta VARCHAR(30),
    tipo VARCHAR(20) DEFAULT 'CORRENTE',  -- CORRENTE, POUPANCA
    ativo BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para contas bancárias
CREATE INDEX IF NOT EXISTS idx_contas_bancarias_oficina ON contas_bancarias(oficina_id);
CREATE INDEX IF NOT EXISTS idx_contas_bancarias_ativo ON contas_bancarias(oficina_id, ativo);

-- Tabela de extratos bancários importados
CREATE TABLE IF NOT EXISTS extratos_bancarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    conta_bancaria_id UUID REFERENCES contas_bancarias(id) ON DELETE SET NULL,
    arquivo_nome VARCHAR(255) NOT NULL,
    arquivo_hash VARCHAR(64) NOT NULL,  -- SHA-256 para detectar duplicatas
    tipo_arquivo VARCHAR(20) NOT NULL DEFAULT 'OFX',  -- OFX, CSV
    data_importacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    saldo_inicial DECIMAL(15,2),
    saldo_final DECIMAL(15,2),
    total_transacoes INTEGER DEFAULT 0,
    total_conciliadas INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',  -- PENDENTE, EM_ANDAMENTO, CONCLUIDO
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_extrato_hash UNIQUE (oficina_id, arquivo_hash)
);

-- Índices para extratos
CREATE INDEX IF NOT EXISTS idx_extratos_bancarios_oficina ON extratos_bancarios(oficina_id);
CREATE INDEX IF NOT EXISTS idx_extratos_bancarios_conta ON extratos_bancarios(conta_bancaria_id);
CREATE INDEX IF NOT EXISTS idx_extratos_bancarios_periodo ON extratos_bancarios(oficina_id, data_inicio, data_fim);
CREATE INDEX IF NOT EXISTS idx_extratos_bancarios_status ON extratos_bancarios(oficina_id, status);

-- Tabela de transações do extrato
CREATE TABLE IF NOT EXISTS transacoes_extrato (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    extrato_id UUID NOT NULL REFERENCES extratos_bancarios(id) ON DELETE CASCADE,
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Dados da transação bancária
    data_transacao DATE NOT NULL,
    data_lancamento DATE,
    tipo VARCHAR(20) NOT NULL,  -- CREDITO, DEBITO
    valor DECIMAL(15,2) NOT NULL,
    descricao VARCHAR(500),
    identificador_banco VARCHAR(100),  -- FITID do OFX
    referencia VARCHAR(100),  -- Número do documento/cheque
    categoria_banco VARCHAR(100),  -- Categoria do banco (se disponível)

    -- Status da conciliação
    status VARCHAR(20) NOT NULL DEFAULT 'NAO_CONCILIADA',  -- NAO_CONCILIADA, CONCILIADA, IGNORADA, MANUAL

    -- Referência para pagamento conciliado
    pagamento_id UUID REFERENCES pagamentos(id) ON DELETE SET NULL,
    data_conciliacao TIMESTAMP,
    metodo_conciliacao VARCHAR(20),  -- AUTO, MANUAL, SUGESTAO
    observacao VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para transações
CREATE INDEX IF NOT EXISTS idx_transacoes_extrato_extrato ON transacoes_extrato(extrato_id);
CREATE INDEX IF NOT EXISTS idx_transacoes_extrato_oficina ON transacoes_extrato(oficina_id);
CREATE INDEX IF NOT EXISTS idx_transacoes_extrato_data ON transacoes_extrato(oficina_id, data_transacao);
CREATE INDEX IF NOT EXISTS idx_transacoes_extrato_status ON transacoes_extrato(oficina_id, status);
CREATE INDEX IF NOT EXISTS idx_transacoes_extrato_valor ON transacoes_extrato(oficina_id, valor);
CREATE INDEX IF NOT EXISTS idx_transacoes_extrato_pagamento ON transacoes_extrato(pagamento_id);
CREATE INDEX IF NOT EXISTS idx_transacoes_extrato_identificador ON transacoes_extrato(extrato_id, identificador_banco);

-- Tabela de regras de conciliação automática
CREATE TABLE IF NOT EXISTS regras_conciliacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),

    -- Critérios de matching
    tipo_transacao VARCHAR(20),  -- CREDITO, DEBITO, null para ambos
    descricao_contem VARCHAR(200),  -- Texto que a descrição deve conter
    valor_minimo DECIMAL(15,2),
    valor_maximo DECIMAL(15,2),

    -- Ação quando encontrar match
    acao VARCHAR(20) NOT NULL DEFAULT 'SUGESTAO',  -- AUTO, SUGESTAO, IGNORAR
    categoria_destino VARCHAR(50),  -- Para classificar transações não conciliáveis

    ativo BOOLEAN DEFAULT true,
    prioridade INTEGER DEFAULT 0,  -- Maior prioridade = executar primeiro

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para regras
CREATE INDEX IF NOT EXISTS idx_regras_conciliacao_oficina ON regras_conciliacao(oficina_id);
CREATE INDEX IF NOT EXISTS idx_regras_conciliacao_ativo ON regras_conciliacao(oficina_id, ativo);

-- Trigger para atualizar updated_at
CREATE OR REPLACE FUNCTION update_conciliacao_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_contas_bancarias_updated_at ON contas_bancarias;
CREATE TRIGGER trg_contas_bancarias_updated_at
    BEFORE UPDATE ON contas_bancarias
    FOR EACH ROW EXECUTE FUNCTION update_conciliacao_updated_at();

DROP TRIGGER IF EXISTS trg_extratos_bancarios_updated_at ON extratos_bancarios;
CREATE TRIGGER trg_extratos_bancarios_updated_at
    BEFORE UPDATE ON extratos_bancarios
    FOR EACH ROW EXECUTE FUNCTION update_conciliacao_updated_at();

DROP TRIGGER IF EXISTS trg_transacoes_extrato_updated_at ON transacoes_extrato;
CREATE TRIGGER trg_transacoes_extrato_updated_at
    BEFORE UPDATE ON transacoes_extrato
    FOR EACH ROW EXECUTE FUNCTION update_conciliacao_updated_at();

DROP TRIGGER IF EXISTS trg_regras_conciliacao_updated_at ON regras_conciliacao;
CREATE TRIGGER trg_regras_conciliacao_updated_at
    BEFORE UPDATE ON regras_conciliacao
    FOR EACH ROW EXECUTE FUNCTION update_conciliacao_updated_at();

-- Comentários
COMMENT ON TABLE contas_bancarias IS 'Contas bancárias configuradas para conciliação';
COMMENT ON TABLE extratos_bancarios IS 'Extratos bancários importados (OFX/CSV)';
COMMENT ON TABLE transacoes_extrato IS 'Transações individuais do extrato bancário';
COMMENT ON TABLE regras_conciliacao IS 'Regras automáticas para conciliação de transações';

COMMENT ON COLUMN extratos_bancarios.arquivo_hash IS 'Hash SHA-256 do arquivo para evitar reimportação';
COMMENT ON COLUMN transacoes_extrato.identificador_banco IS 'FITID do OFX - identificador único da transação no banco';
COMMENT ON COLUMN transacoes_extrato.metodo_conciliacao IS 'AUTO=automático, MANUAL=usuário, SUGESTAO=aceita sugestão';

-- =====================================================
-- Adicionar colunas de conciliação na tabela pagamentos
-- =====================================================

-- Coluna para indicar se pagamento foi conciliado
ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS conciliado BOOLEAN NOT NULL DEFAULT false;

-- Coluna para referência à transação do extrato (quando conciliado)
ALTER TABLE pagamentos ADD COLUMN IF NOT EXISTS transacao_extrato_id UUID REFERENCES transacoes_extrato(id) ON DELETE SET NULL;

-- Índice para busca de pagamentos não conciliados
CREATE INDEX IF NOT EXISTS idx_pagamentos_conciliado ON pagamentos(oficina_id, conciliado);
CREATE INDEX IF NOT EXISTS idx_pagamentos_transacao_extrato ON pagamentos(transacao_extrato_id);

COMMENT ON COLUMN pagamentos.conciliado IS 'Indica se o pagamento foi conciliado com extrato bancário';
COMMENT ON COLUMN pagamentos.transacao_extrato_id IS 'Referência à transação do extrato vinculada na conciliação';
