-- =====================================================
-- Migration: V027 - Add FK from notas_fiscais to ordem_servico
-- Description: Establishes the foreign key relationship between
--              invoices and service orders
-- Author: PitStop Team
-- Date: 2025-12-16
-- =====================================================

-- Add foreign key constraint
-- Every invoice (nota fiscal) must be linked to a service order
ALTER TABLE notas_fiscais
    ADD CONSTRAINT fk_notas_fiscais_ordem_servico
    FOREIGN KEY (ordem_servico_id)
    REFERENCES ordem_servico(id)
    ON DELETE RESTRICT;

-- Add index for better query performance (if not exists)
CREATE INDEX IF NOT EXISTS idx_notas_fiscais_os ON notas_fiscais(ordem_servico_id);

-- Add comment to constraint
COMMENT ON CONSTRAINT fk_notas_fiscais_ordem_servico ON notas_fiscais
    IS 'RESTRICT: não permite deletar OS que possui nota fiscal emitida (integridade fiscal)';

-- Reasoning for RESTRICT (not CASCADE):
-- - Invoices are legal/fiscal documents that cannot be deleted automatically
-- - If a service order has an invoice, it must be cancelled/handled manually first
-- - This prevents accidental deletion of fiscal records
-- - Maintains compliance with tax regulations (NF-e/NFS-e cannot be deleted, only cancelled)
