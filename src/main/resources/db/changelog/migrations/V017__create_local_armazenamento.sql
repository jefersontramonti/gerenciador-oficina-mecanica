-- ========================================
-- Migration V017: Create local_armazenamento table
-- Purpose: Physical location system for inventory management
-- Author: PitStop Team
-- Date: 2025-11-02
-- ========================================

-- Create local_armazenamento table with self-referential hierarchy
CREATE TABLE local_armazenamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Unique code (normalized to UPPERCASE)
    codigo VARCHAR(50) NOT NULL UNIQUE,

    -- Location type with constraint
    tipo VARCHAR(20) NOT NULL
        CHECK (tipo IN ('PRATELEIRA', 'GAVETA', 'ARMARIO', 'DEPOSITO', 'CAIXA', 'VITRINE', 'OUTRO')),

    -- Description
    descricao VARCHAR(200) NOT NULL,

    -- Self-referential parent location (ON DELETE RESTRICT prevents accidental deletion)
    localizacao_pai_id UUID REFERENCES local_armazenamento(id) ON DELETE RESTRICT,

    -- Optional capacity
    capacidade_maxima INTEGER,

    -- Additional notes
    observacoes TEXT,

    -- Soft delete flag
    ativo BOOLEAN NOT NULL DEFAULT true,

    -- Audit timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_capacidade_positiva CHECK (capacidade_maxima IS NULL OR capacidade_maxima > 0),
    CONSTRAINT chk_codigo_uppercase CHECK (codigo = UPPER(codigo))
);

-- Add comment to table
COMMENT ON TABLE local_armazenamento IS 'Hierarchical physical storage locations for inventory items';

-- Add comments to columns
COMMENT ON COLUMN local_armazenamento.codigo IS 'Unique location code (uppercase, e.g., DEP-A, PRAT-B1)';
COMMENT ON COLUMN local_armazenamento.tipo IS 'Location type: PRATELEIRA, GAVETA, ARMARIO, DEPOSITO, CAIXA, VITRINE, OUTRO';
COMMENT ON COLUMN local_armazenamento.localizacao_pai_id IS 'Parent location ID (NULL for root locations like warehouses)';
COMMENT ON COLUMN local_armazenamento.capacidade_maxima IS 'Maximum number of items (optional)';
COMMENT ON COLUMN local_armazenamento.ativo IS 'Active flag for soft delete';
