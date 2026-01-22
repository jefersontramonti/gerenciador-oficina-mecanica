-- =====================================================
-- V070: Infraestrutura para Fluxo de Caixa Avançado
-- Feature: FLUXO_CAIXA_AVANCADO
-- Data: 2026-01-19
-- =====================================================

-- Feature Flag para FLUXO_CAIXA_AVANCADO
INSERT INTO feature_flags (
    codigo,
    nome,
    descricao,
    habilitado_global,
    habilitado_por_plano,
    categoria
)
VALUES (
    'FLUXO_CAIXA_AVANCADO',
    'Fluxo de Caixa Avançado',
    'Dashboard financeiro com projeções, DRE simplificado e alertas de fluxo',
    false,
    '{"PROFISSIONAL": true, "TURBINADO": true}'::JSONB,
    'FINANCEIRO'
)
ON CONFLICT (codigo) DO NOTHING;

-- =====================================================
-- Tabela para categorias de despesas (contas a pagar)
-- =====================================================

CREATE TABLE IF NOT EXISTS categorias_financeiras (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    nome VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL, -- RECEITA, DESPESA
    cor VARCHAR(7), -- Hex color para gráficos

    -- Padrão para agrupar
    grupo VARCHAR(50), -- Ex: OPERACIONAL, ADMINISTRATIVA, IMPOSTOS

    ativo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_categoria_oficina_nome UNIQUE (oficina_id, nome)
);

CREATE INDEX idx_categoria_oficina ON categorias_financeiras(oficina_id);
CREATE INDEX idx_categoria_tipo ON categorias_financeiras(tipo);

COMMENT ON TABLE categorias_financeiras IS 'Categorias para classificar receitas e despesas';

-- =====================================================
-- Tabela para contas a pagar/receber (lançamentos futuros)
-- =====================================================

CREATE TABLE IF NOT EXISTS lancamentos_financeiros (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Tipo do lançamento
    tipo VARCHAR(20) NOT NULL, -- RECEITA, DESPESA

    -- Descrição
    descricao VARCHAR(500) NOT NULL,
    categoria_id UUID REFERENCES categorias_financeiras(id),

    -- Valores
    valor DECIMAL(12, 2) NOT NULL,
    valor_pago DECIMAL(12, 2) DEFAULT 0,

    -- Datas
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    data_competencia DATE, -- Mês de referência

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE, PAGO, PARCIAL, CANCELADO, VENCIDO

    -- Recorrência (para lançamentos fixos)
    recorrente BOOLEAN NOT NULL DEFAULT false,
    frequencia_recorrencia VARCHAR(20), -- MENSAL, SEMANAL, ANUAL
    lancamento_pai_id UUID REFERENCES lancamentos_financeiros(id),

    -- Vinculações opcionais
    pagamento_id UUID REFERENCES pagamentos(id),
    ordem_servico_id UUID REFERENCES ordem_servico(id),
    fornecedor_nome VARCHAR(200),

    -- Forma de pagamento
    forma_pagamento VARCHAR(30),
    conta_bancaria_id UUID REFERENCES contas_bancarias(id),

    observacoes TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lancamento_oficina ON lancamentos_financeiros(oficina_id);
CREATE INDEX idx_lancamento_tipo ON lancamentos_financeiros(tipo);
CREATE INDEX idx_lancamento_vencimento ON lancamentos_financeiros(data_vencimento);
CREATE INDEX idx_lancamento_status ON lancamentos_financeiros(status);
CREATE INDEX idx_lancamento_competencia ON lancamentos_financeiros(data_competencia);
CREATE INDEX idx_lancamento_categoria ON lancamentos_financeiros(categoria_id);

COMMENT ON TABLE lancamentos_financeiros IS 'Lançamentos financeiros (contas a pagar/receber)';

-- =====================================================
-- View materializada para resumo diário (performance)
-- =====================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_resumo_financeiro_diario AS
SELECT
    p.oficina_id,
    DATE(p.created_at) as data,
    COUNT(CASE WHEN p.status = 'PAGO' THEN 1 END) as qtd_pagamentos,
    COALESCE(SUM(CASE WHEN p.status = 'PAGO' THEN p.valor ELSE 0 END), 0) as total_receitas,
    0::DECIMAL as total_despesas, -- Será atualizado quando tivermos despesas
    COALESCE(SUM(CASE WHEN p.status = 'PAGO' THEN p.valor ELSE 0 END), 0) as saldo
FROM pagamentos p
WHERE p.status = 'PAGO'
GROUP BY p.oficina_id, DATE(p.created_at);

CREATE UNIQUE INDEX ON mv_resumo_financeiro_diario (oficina_id, data);

COMMENT ON MATERIALIZED VIEW mv_resumo_financeiro_diario IS 'Resumo diário de receitas e despesas por oficina';

-- =====================================================
-- Categorias padrão para novas oficinas
-- =====================================================

-- Inserir categorias padrão para oficinas existentes
DO $$
DECLARE
    oficina_record RECORD;
BEGIN
    FOR oficina_record IN SELECT id FROM oficinas WHERE status = 'ATIVA' LOOP
        -- Receitas
        INSERT INTO categorias_financeiras (oficina_id, nome, tipo, grupo, cor)
        VALUES
            (oficina_record.id, 'Serviços', 'RECEITA', 'OPERACIONAL', '#10B981'),
            (oficina_record.id, 'Peças', 'RECEITA', 'OPERACIONAL', '#3B82F6'),
            (oficina_record.id, 'Outros', 'RECEITA', 'OUTROS', '#8B5CF6')
        ON CONFLICT (oficina_id, nome) DO NOTHING;

        -- Despesas
        INSERT INTO categorias_financeiras (oficina_id, nome, tipo, grupo, cor)
        VALUES
            (oficina_record.id, 'Aluguel', 'DESPESA', 'ADMINISTRATIVA', '#EF4444'),
            (oficina_record.id, 'Salários', 'DESPESA', 'PESSOAL', '#F97316'),
            (oficina_record.id, 'Fornecedores', 'DESPESA', 'OPERACIONAL', '#EC4899'),
            (oficina_record.id, 'Impostos', 'DESPESA', 'IMPOSTOS', '#6366F1'),
            (oficina_record.id, 'Manutenção', 'DESPESA', 'OPERACIONAL', '#14B8A6'),
            (oficina_record.id, 'Utilidades', 'DESPESA', 'ADMINISTRATIVA', '#F59E0B'),
            (oficina_record.id, 'Marketing', 'DESPESA', 'ADMINISTRATIVA', '#8B5CF6'),
            (oficina_record.id, 'Outros', 'DESPESA', 'OUTROS', '#6B7280')
        ON CONFLICT (oficina_id, nome) DO NOTHING;
    END LOOP;
END $$;
