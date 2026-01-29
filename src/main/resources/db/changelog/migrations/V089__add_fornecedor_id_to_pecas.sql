-- Migration: Add fornecedor_id FK to pecas table
-- Description: Links parts to their supplier (optional relationship)
-- Author: PitStop Team
-- Date: 2026-01-28

-- Add fornecedor FK column to pecas
ALTER TABLE pecas ADD COLUMN fornecedor_id UUID REFERENCES fornecedores(id);

-- Index for FK lookups
CREATE INDEX idx_pecas_fornecedor_id ON pecas(fornecedor_id) WHERE fornecedor_id IS NOT NULL;
