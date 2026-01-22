-- =====================================================
-- V074: Tabela de Despesas Operacionais
-- Integração com Fluxo de Caixa e DRE
-- Data: 2026-01-20
-- =====================================================

-- =====================================================
-- Tabela de Despesas
-- Usa enum para categorias (mais simples que tabela separada)
-- =====================================================

CREATE TABLE IF NOT EXISTS despesas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Categoria (enum no Java)
    categoria VARCHAR(50) NOT NULL,

    -- Descrição
    descricao VARCHAR(500) NOT NULL,

    -- Valor
    valor DECIMAL(10, 2) NOT NULL CHECK (valor > 0),

    -- Datas
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',

    -- Informações adicionais
    numero_documento VARCHAR(100),
    fornecedor VARCHAR(200),
    observacoes TEXT,

    -- Recorrência
    recorrente BOOLEAN NOT NULL DEFAULT false,

    -- Forma de pagamento (quando paga)
    tipo_pagamento VARCHAR(30),

    -- Vinculação com movimentação de estoque (para compras de peças)
    movimentacao_estoque_id UUID REFERENCES movimentacao_estoque(id),

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_despesa_status CHECK (status IN ('PENDENTE', 'PAGA', 'VENCIDA', 'CANCELADA')),
    CONSTRAINT chk_despesa_categoria CHECK (categoria IN (
        'SALARIOS', 'ENCARGOS_SOCIAIS', 'BENEFICIOS', 'PROLABORE',
        'ALUGUEL', 'CONDOMINIO', 'IPTU', 'MANUTENCAO_PREDIAL',
        'ENERGIA_ELETRICA', 'AGUA', 'GAS', 'TELEFONE', 'INTERNET',
        'COMPRA_PECAS', 'FERRAMENTAS', 'MATERIAL_CONSUMO', 'MATERIAL_LIMPEZA', 'DESCARTE_RESIDUOS',
        'CONTABILIDADE', 'ADVOCACIA', 'SISTEMAS_SOFTWARE', 'MATERIAL_ESCRITORIO', 'TAXAS_BANCARIAS',
        'PUBLICIDADE', 'MARKETING_DIGITAL', 'BRINDES',
        'IMPOSTOS_FEDERAIS', 'IMPOSTOS_ESTADUAIS', 'IMPOSTOS_MUNICIPAIS', 'TAXAS_LICENCAS',
        'COMBUSTIVEL', 'MANUTENCAO_VEICULOS', 'SEGURO_VEICULOS',
        'SEGURO_ESTABELECIMENTO', 'SEGURO_RESPONSABILIDADE',
        'JUROS_EMPRESTIMOS', 'TARIFAS_CARTAO',
        'OUTRAS_DESPESAS'
    )),
    CONSTRAINT chk_despesa_tipo_pagamento CHECK (tipo_pagamento IS NULL OR tipo_pagamento IN (
        'DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'PIX', 'TRANSFERENCIA', 'BOLETO'
    ))
);

-- Índices para performance
CREATE INDEX idx_despesas_oficina ON despesas(oficina_id);
CREATE INDEX idx_despesas_categoria ON despesas(categoria);
CREATE INDEX idx_despesas_data_vencimento ON despesas(data_vencimento);
CREATE INDEX idx_despesas_data_pagamento ON despesas(data_pagamento);
CREATE INDEX idx_despesas_status ON despesas(status);
CREATE INDEX idx_despesas_oficina_status ON despesas(oficina_id, status);
CREATE INDEX idx_despesas_oficina_vencimento ON despesas(oficina_id, data_vencimento);
CREATE INDEX idx_despesas_oficina_pagamento ON despesas(oficina_id, data_pagamento);
CREATE INDEX idx_despesas_movimentacao ON despesas(movimentacao_estoque_id);

COMMENT ON TABLE despesas IS 'Despesas operacionais da oficina para fluxo de caixa e DRE';
COMMENT ON COLUMN despesas.categoria IS 'Categoria da despesa (enum CategoriaDespesa no Java)';
COMMENT ON COLUMN despesas.movimentacao_estoque_id IS 'Vinculação com entrada de estoque (compra de peças)';

-- =====================================================
-- Trigger para atualizar updated_at
-- =====================================================

CREATE OR REPLACE FUNCTION update_despesas_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_despesas_updated_at
    BEFORE UPDATE ON despesas
    FOR EACH ROW
    EXECUTE FUNCTION update_despesas_updated_at();

-- =====================================================
-- Trigger para marcar como VENCIDA automaticamente
-- (usado em job agendado, mas pode ser útil para consultas)
-- =====================================================

-- View para despesas vencidas (otimização de consultas)
CREATE OR REPLACE VIEW vw_despesas_vencidas AS
SELECT d.*
FROM despesas d
WHERE d.status = 'PENDENTE'
  AND d.data_vencimento < CURRENT_DATE;

COMMENT ON VIEW vw_despesas_vencidas IS 'Despesas pendentes que já venceram';

-- =====================================================
-- Função para atualizar materialized view de fluxo de caixa
-- =====================================================

-- Atualizar a materialized view para incluir despesas
DROP MATERIALIZED VIEW IF EXISTS mv_resumo_financeiro_diario;

CREATE MATERIALIZED VIEW mv_resumo_financeiro_diario AS
SELECT
    COALESCE(r.oficina_id, d.oficina_id) as oficina_id,
    COALESCE(r.data, d.data) as data,
    COALESCE(r.qtd_pagamentos, 0) as qtd_pagamentos,
    COALESCE(r.total_receitas, 0) as total_receitas,
    COALESCE(d.total_despesas, 0) as total_despesas,
    COALESCE(r.total_receitas, 0) - COALESCE(d.total_despesas, 0) as saldo
FROM (
    -- Receitas (pagamentos confirmados)
    SELECT
        p.oficina_id,
        DATE(p.created_at) as data,
        COUNT(*) as qtd_pagamentos,
        COALESCE(SUM(p.valor), 0) as total_receitas
    FROM pagamentos p
    WHERE p.status = 'PAGO'
    GROUP BY p.oficina_id, DATE(p.created_at)
) r
FULL OUTER JOIN (
    -- Despesas pagas
    SELECT
        d.oficina_id,
        d.data_pagamento as data,
        COALESCE(SUM(d.valor), 0) as total_despesas
    FROM despesas d
    WHERE d.status = 'PAGA'
      AND d.data_pagamento IS NOT NULL
    GROUP BY d.oficina_id, d.data_pagamento
) d ON r.oficina_id = d.oficina_id AND r.data = d.data;

CREATE UNIQUE INDEX ON mv_resumo_financeiro_diario (oficina_id, data);

COMMENT ON MATERIALIZED VIEW mv_resumo_financeiro_diario IS 'Resumo diário de receitas e despesas por oficina';

-- =====================================================
-- Função para atualizar status de despesas vencidas
-- (para ser chamada por job agendado)
-- =====================================================

CREATE OR REPLACE FUNCTION atualizar_despesas_vencidas()
RETURNS INTEGER AS $$
DECLARE
    qtd_atualizada INTEGER;
BEGIN
    UPDATE despesas
    SET status = 'VENCIDA',
        updated_at = NOW()
    WHERE status = 'PENDENTE'
      AND data_vencimento < CURRENT_DATE;

    GET DIAGNOSTICS qtd_atualizada = ROW_COUNT;
    RETURN qtd_atualizada;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION atualizar_despesas_vencidas IS 'Atualiza status de despesas vencidas para VENCIDA';
