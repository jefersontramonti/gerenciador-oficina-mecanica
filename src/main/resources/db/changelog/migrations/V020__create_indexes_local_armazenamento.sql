-- ========================================
-- Migration V020: Create performance indexes for local_armazenamento
-- Purpose: Optimize queries for location hierarchy and part lookups
-- Author: PitStop Team
-- Date: 2025-11-02
-- ========================================

-- Index on tipo for filtering by location type
CREATE INDEX idx_local_armazenamento_tipo
ON local_armazenamento(tipo)
WHERE ativo = true;

-- Index on ativo for soft delete filtering
CREATE INDEX idx_local_armazenamento_ativo
ON local_armazenamento(ativo);

-- Index on parent FK for hierarchy traversal
CREATE INDEX idx_local_armazenamento_pai
ON local_armazenamento(localizacao_pai_id)
WHERE ativo = true;

-- Composite index for finding active root locations
CREATE INDEX idx_local_armazenamento_raiz
ON local_armazenamento(ativo, localizacao_pai_id)
WHERE localizacao_pai_id IS NULL;

-- Index on pecas FK for reverse lookup (which parts are in this location?)
CREATE INDEX idx_pecas_local_armazenamento
ON pecas(local_armazenamento_id)
WHERE ativo = true;

-- Index on codigo for unique constraint (already has unique index, but explicit for clarity)
-- PostgreSQL automatically creates unique index for UNIQUE constraint, so this is just a comment
COMMENT ON INDEX local_armazenamento_codigo_key IS 'Unique index for location code (auto-created by UNIQUE constraint)';

-- Add statistics for query planner
ANALYZE local_armazenamento;
ANALYZE pecas;

-- Log index creation
DO $$
BEGIN
    RAISE NOTICE 'Migration V020 completed: 5 performance indexes created for local_armazenamento and pecas';
END $$;
