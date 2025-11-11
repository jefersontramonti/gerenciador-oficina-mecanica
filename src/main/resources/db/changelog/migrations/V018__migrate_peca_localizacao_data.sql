-- ========================================
-- Migration V018: Migrate existing Peca.localizacao data
-- Purpose: Preserve existing location strings by creating generic locations
-- Author: PitStop Team
-- Date: 2025-11-02
-- ========================================

-- Step 1: Create generic locations from existing unique localizacao strings
-- This preserves all existing location data
INSERT INTO local_armazenamento (id, codigo, tipo, descricao, ativo, created_at, updated_at)
SELECT
    gen_random_uuid() AS id,
    'MIGRADO_' || LPAD(ROW_NUMBER() OVER (ORDER BY localizacao)::TEXT, 4, '0') AS codigo,
    'OUTRO' AS tipo,
    localizacao AS descricao,
    true AS ativo,
    NOW() AS created_at,
    NOW() AS updated_at
FROM (
    SELECT DISTINCT localizacao
    FROM pecas
    WHERE localizacao IS NOT NULL
      AND TRIM(localizacao) != ''
) AS unique_locations
ORDER BY localizacao;

-- Add comment explaining migrated locations
COMMENT ON TABLE local_armazenamento IS 'Hierarchical physical storage locations. Records with codigo starting with MIGRADO_ were auto-created from legacy localizacao field';

-- Log migration statistics
DO $$
DECLARE
    total_migrated INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_migrated
    FROM local_armazenamento
    WHERE codigo LIKE 'MIGRADO_%';

    RAISE NOTICE 'Migration V018 completed: % unique locations migrated from pecas.localizacao', total_migrated;
END $$;
