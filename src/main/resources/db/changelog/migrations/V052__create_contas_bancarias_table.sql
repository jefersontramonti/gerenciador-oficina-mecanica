-- =====================================================
-- V052: Criar tabela de contas bancárias
-- =====================================================
-- Permite que oficinas cadastrem múltiplas contas bancárias
-- para recebimento de pagamentos (PIX, transferência, etc.)
-- =====================================================

CREATE TABLE IF NOT EXISTS contas_bancarias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Identificação
    nome VARCHAR(100) NOT NULL,

    -- Dados bancários
    codigo_banco VARCHAR(5),
    banco VARCHAR(100) NOT NULL,
    agencia VARCHAR(10),
    digito_agencia VARCHAR(2),
    conta VARCHAR(20),
    digito_conta VARCHAR(2),
    tipo_conta VARCHAR(20) CHECK (tipo_conta IN ('CORRENTE', 'POUPANCA')),

    -- Titular
    titular VARCHAR(200),
    cpf_cnpj_titular VARCHAR(20),

    -- Dados PIX
    tipo_chave_pix VARCHAR(20) CHECK (tipo_chave_pix IN ('CPF', 'CNPJ', 'EMAIL', 'TELEFONE', 'ALEATORIA')),
    chave_pix VARCHAR(100),
    nome_beneficiario_pix VARCHAR(200),
    cidade_beneficiario_pix VARCHAR(100),

    -- Configurações
    padrao BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    finalidade VARCHAR(200),
    observacoes TEXT,

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para otimização
CREATE INDEX IF NOT EXISTS idx_contas_bancarias_oficina ON contas_bancarias(oficina_id);
CREATE INDEX IF NOT EXISTS idx_contas_bancarias_padrao ON contas_bancarias(padrao);
CREATE INDEX IF NOT EXISTS idx_contas_bancarias_ativo ON contas_bancarias(ativo);

-- Comentários
COMMENT ON TABLE contas_bancarias IS 'Contas bancárias das oficinas para recebimento de pagamentos';
COMMENT ON COLUMN contas_bancarias.nome IS 'Nome/apelido da conta para identificação';
COMMENT ON COLUMN contas_bancarias.codigo_banco IS 'Código FEBRABAN do banco (ex: 001, 341)';
COMMENT ON COLUMN contas_bancarias.tipo_chave_pix IS 'Tipo da chave PIX: CPF, CNPJ, EMAIL, TELEFONE, ALEATORIA';
COMMENT ON COLUMN contas_bancarias.padrao IS 'Se é a conta padrão para recebimentos';
COMMENT ON COLUMN contas_bancarias.finalidade IS 'Finalidade da conta (ex: Recebimentos gerais, Boletos)';
