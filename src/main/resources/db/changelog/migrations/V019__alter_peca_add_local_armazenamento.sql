-- ========================================
-- Migration V019: Alter Peca table - add FK to local_armazenamento
-- Purpose: Replace String localizacao with structured FK relationship
-- Author: PitStop Team
-- Date: 2025-11-02
-- ========================================

-- Step 1: Add new column for FK relationship
ALTER TABLE pecas
ADD COLUMN local_armazenamento_id UUID REFERENCES local_armazenamento(id) ON DELETE SET NULL;

-- Step 2: Link existing pecas to migrated locations
UPDATE pecas p
SET local_armazenamento_id = (
    SELECT id
    FROM local_armazenamento la
    WHERE la.descricao = p.localizacao
      AND la.codigo LIKE 'MIGRADO_%'
    LIMIT 1
)
WHERE p.localizacao IS NOT NULL
  AND TRIM(p.localizacao) != '';

-- Step 3: Drop old localizacao column (breaking change)
ALTER TABLE pecas
DROP COLUMN localizacao;

-- Add comment
COMMENT ON COLUMN pecas.local_armazenamento_id IS 'FK to local_armazenamento (physical storage location)';

-- Log migration statistics
DO $$
DECLARE
    total_linked INTEGER;
    total_null INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_linked
    FROM pecas
    WHERE local_armazenamento_id IS NOT NULL;

    SELECT COUNT(*) INTO total_null
    FROM pecas
    WHERE local_armazenamento_id IS NULL;

    RAISE NOTICE 'Migration V019 completed: % pecas linked to locations, % without location', total_linked, total_null;
END $$;
