-- ============================================================================
-- Migration: Create pecas table
-- Description: Part catalog and inventory control for workshop
-- Author: PitStop Team
-- Date: 2025-11-02
-- ============================================================================

CREATE TABLE pecas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(500) NOT NULL,
    marca VARCHAR(100),
    aplicacao VARCHAR(500),
    localizacao VARCHAR(100),
    unidade_medida VARCHAR(20) NOT NULL DEFAULT 'UNIDADE',

    -- Stock control
    quantidade_atual INTEGER NOT NULL DEFAULT 0,
    quantidade_minima INTEGER NOT NULL DEFAULT 1,

    -- Financial
    valor_custo DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    valor_venda DECIMAL(10, 2) NOT NULL DEFAULT 0.00,

    -- Soft delete
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Optimistic locking
    version INTEGER NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_pecas_codigo_min_length CHECK (LENGTH(TRIM(codigo)) >= 3),
    CONSTRAINT chk_pecas_descricao_min_length CHECK (LENGTH(TRIM(descricao)) >= 3),
    CONSTRAINT chk_pecas_quantidade_atual CHECK (quantidade_atual >= 0),
    CONSTRAINT chk_pecas_quantidade_minima CHECK (quantidade_minima >= 0),
    CONSTRAINT chk_pecas_valor_custo CHECK (valor_custo >= 0),
    CONSTRAINT chk_pecas_valor_venda CHECK (valor_venda >= 0),
    CONSTRAINT chk_pecas_unidade_medida CHECK (unidade_medida IN ('UNIDADE', 'LITRO', 'METRO', 'QUILO'))
);

-- Performance indexes
CREATE INDEX idx_pecas_codigo ON pecas(codigo) WHERE ativo = TRUE;
CREATE INDEX idx_pecas_descricao ON pecas(descricao) WHERE ativo = TRUE;
CREATE INDEX idx_pecas_marca ON pecas(marca) WHERE ativo = TRUE;
CREATE INDEX idx_pecas_quantidade_atual ON pecas(quantidade_atual) WHERE ativo = TRUE;
CREATE INDEX idx_pecas_ativo ON pecas(ativo);
CREATE INDEX idx_pecas_estoque_baixo ON pecas(quantidade_atual, quantidade_minima)
    WHERE ativo = TRUE AND quantidade_atual <= quantidade_minima;

-- Comments for documentation
COMMENT ON TABLE pecas IS 'Catálogo de peças e controle de estoque da oficina';
COMMENT ON COLUMN pecas.codigo IS 'SKU único da peça (código de identificação, mínimo 3 caracteres)';
COMMENT ON COLUMN pecas.descricao IS 'Descrição completa da peça (mínimo 3 caracteres)';
COMMENT ON COLUMN pecas.marca IS 'Fabricante ou marca da peça';
COMMENT ON COLUMN pecas.aplicacao IS 'Veículos ou sistemas compatíveis';
COMMENT ON COLUMN pecas.localizacao IS 'Local físico no estoque (prateleira, corredor, etc)';
COMMENT ON COLUMN pecas.unidade_medida IS 'Unidade de medida: UNIDADE, LITRO, METRO, QUILO';
COMMENT ON COLUMN pecas.quantidade_atual IS 'Quantidade em estoque atualizada automaticamente pelas movimentações (não editar diretamente)';
COMMENT ON COLUMN pecas.quantidade_minima IS 'Alerta de estoque baixo quando quantidade_atual <= quantidade_minima';
COMMENT ON COLUMN pecas.valor_custo IS 'Preço de compra/custo da peça';
COMMENT ON COLUMN pecas.valor_venda IS 'Preço de venda ao cliente';
COMMENT ON COLUMN pecas.ativo IS 'Soft delete: FALSE indica peça desativada do catálogo';
COMMENT ON COLUMN pecas.version IS 'Optimistic locking para controle de concorrência em atualizações';
