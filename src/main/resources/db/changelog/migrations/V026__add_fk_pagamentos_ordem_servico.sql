-- =====================================================
-- Migration: V026 - Add FK from pagamentos to ordem_servico
-- Description: Establishes the foreign key relationship between
--              payments and service orders (was commented in V024)
-- Author: PitStop Team
-- Date: 2025-12-16
-- =====================================================

-- Add foreign key constraint
-- This was commented out in V024 but is now safe to add since all tables are created
ALTER TABLE pagamentos
    ADD CONSTRAINT fk_pagamentos_ordem_servico
    FOREIGN KEY (ordem_servico_id)
    REFERENCES ordem_servico(id)
    ON DELETE CASCADE;

-- Add index for better query performance (if not exists)
CREATE INDEX IF NOT EXISTS idx_pagamentos_os ON pagamentos(ordem_servico_id);

-- Add comment to constraint
COMMENT ON CONSTRAINT fk_pagamentos_ordem_servico ON pagamentos
    IS 'CASCADE: quando uma OS é deletada, seus pagamentos são deletados automaticamente';

-- This ensures referential integrity:
-- - Cannot create payment for non-existent service order
-- - Deleting a service order automatically deletes all its payments
-- - Maintains data consistency across the system
