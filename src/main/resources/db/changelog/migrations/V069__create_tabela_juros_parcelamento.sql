-- =====================================================
-- V069: Tabela de Juros para Parcelamento
-- Feature: PARCELAMENTO_CARTAO
-- Data: 2026-01-19
-- =====================================================

-- Tabela de configuração de juros por faixa de parcelas
CREATE TABLE tabelas_juros_parcelamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Faixa de parcelas
    parcelas_minimo INTEGER NOT NULL,
    parcelas_maximo INTEGER NOT NULL,

    -- Configurações de juros
    percentual_juros DECIMAL(5, 2) NOT NULL DEFAULT 0.00,  -- % ao mês
    tipo_juros VARCHAR(20) NOT NULL DEFAULT 'SEM_JUROS',  -- SEM_JUROS, JUROS_SIMPLES, JUROS_COMPOSTO

    -- Se a taxa é repassada ao cliente ou absorvida pela oficina
    repassar_cliente BOOLEAN NOT NULL DEFAULT true,

    -- Status
    ativo BOOLEAN NOT NULL DEFAULT true,

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraint para evitar sobreposição de faixas
    CONSTRAINT chk_faixa_valida CHECK (parcelas_minimo <= parcelas_maximo),
    CONSTRAINT chk_parcelas_positivas CHECK (parcelas_minimo >= 1 AND parcelas_maximo <= 24),
    CONSTRAINT chk_juros_positivo CHECK (percentual_juros >= 0 AND percentual_juros <= 100)
);

-- Índices para performance
CREATE INDEX idx_tabela_juros_oficina ON tabelas_juros_parcelamento(oficina_id);
CREATE INDEX idx_tabela_juros_ativo ON tabelas_juros_parcelamento(ativo);
CREATE INDEX idx_tabela_juros_faixa ON tabelas_juros_parcelamento(parcelas_minimo, parcelas_maximo);

-- Unique constraint para evitar faixas duplicadas
CREATE UNIQUE INDEX uk_tabela_juros_oficina_faixa
    ON tabelas_juros_parcelamento(oficina_id, parcelas_minimo, parcelas_maximo)
    WHERE ativo = true;

-- Comentários
COMMENT ON TABLE tabelas_juros_parcelamento IS 'Configuração de juros por faixa de parcelas para cada oficina';
COMMENT ON COLUMN tabelas_juros_parcelamento.percentual_juros IS 'Taxa de juros mensal em percentual (ex: 2.99 para 2.99% ao mês)';
COMMENT ON COLUMN tabelas_juros_parcelamento.tipo_juros IS 'SEM_JUROS: 0%, JUROS_SIMPLES: juros fixo, JUROS_COMPOSTO: juros sobre juros';
COMMENT ON COLUMN tabelas_juros_parcelamento.repassar_cliente IS 'Se true, o cliente paga os juros. Se false, a oficina absorve.';

-- =====================================================
-- Configuração padrão de parcelamento
-- =====================================================

-- Tabela para configurações gerais de parcelamento por oficina
CREATE TABLE configuracoes_parcelamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL UNIQUE REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Limites
    parcelas_maximas INTEGER NOT NULL DEFAULT 12,
    valor_minimo_parcela DECIMAL(10, 2) NOT NULL DEFAULT 50.00,
    valor_minimo_parcelamento DECIMAL(10, 2) NOT NULL DEFAULT 100.00,

    -- Bandeiras aceitas para parcelamento
    aceita_visa BOOLEAN NOT NULL DEFAULT true,
    aceita_mastercard BOOLEAN NOT NULL DEFAULT true,
    aceita_elo BOOLEAN NOT NULL DEFAULT true,
    aceita_amex BOOLEAN NOT NULL DEFAULT true,
    aceita_hipercard BOOLEAN NOT NULL DEFAULT true,

    -- Exibição
    exibir_valor_total BOOLEAN NOT NULL DEFAULT true,
    exibir_juros BOOLEAN NOT NULL DEFAULT true,

    -- Status
    ativo BOOLEAN NOT NULL DEFAULT true,

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Índice
CREATE INDEX idx_config_parcelamento_oficina ON configuracoes_parcelamento(oficina_id);

-- Comentários
COMMENT ON TABLE configuracoes_parcelamento IS 'Configurações gerais de parcelamento por oficina';

-- =====================================================
-- Feature Flag para PARCELAMENTO_CARTAO
-- =====================================================

-- Inserir a feature flag se não existir
INSERT INTO feature_flags (
    codigo,
    nome,
    descricao,
    habilitado_global,
    habilitado_por_plano,
    categoria
)
VALUES (
    'PARCELAMENTO_CARTAO',
    'Parcelamento Avançado',
    'Configuração de parcelamento com juros personalizados por faixa',
    false,
    '{"PROFISSIONAL": true, "TURBINADO": true}'::JSONB,
    'FINANCEIRO'
)
ON CONFLICT (codigo) DO NOTHING;
