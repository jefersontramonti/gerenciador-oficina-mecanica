-- ============================================================================
-- Migration: Add FK constraint from item_os to pecas
-- Description: Completes the relationship between service order items and parts catalog
-- Author: PitStop Team
-- Date: 2025-11-02
-- ============================================================================

-- Add foreign key constraint to existing column
-- item_os.peca_id already exists but had no FK constraint
ALTER TABLE item_os
    ADD CONSTRAINT fk_item_os_peca
    FOREIGN KEY (peca_id)
    REFERENCES pecas(id)
    ON DELETE RESTRICT;

-- Comment
COMMENT ON CONSTRAINT fk_item_os_peca ON item_os
    IS 'RESTRICT: não pode deletar peça que está referenciada em itens de ordem de serviço';

-- Note: peca_id is nullable in item_os because some items can be services (tipo=SERVICO)
-- Only items with tipo=PECA should have a peca_id value
