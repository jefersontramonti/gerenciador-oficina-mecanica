-- =====================================================
-- V078: Rename configuracao_gateway to saas_configuracao_gateway
-- =====================================================
-- Renames the table to avoid conflict with the existing
-- configuracoes_gateway table (used by workshops).
-- =====================================================

-- Rename table
ALTER TABLE IF EXISTS configuracao_gateway RENAME TO saas_configuracao_gateway;

-- Rename indexes
ALTER INDEX IF EXISTS idx_configuracao_gateway_tipo RENAME TO idx_saas_configuracao_gateway_tipo;
ALTER INDEX IF EXISTS idx_configuracao_gateway_ativo RENAME TO idx_saas_configuracao_gateway_ativo;

-- Update foreign key constraint name
ALTER TABLE IF EXISTS saas_configuracao_gateway
    DROP CONSTRAINT IF EXISTS fk_configuracao_gateway_updated_by;

ALTER TABLE IF EXISTS saas_configuracao_gateway
    ADD CONSTRAINT fk_saas_configuracao_gateway_updated_by
    FOREIGN KEY (updated_by) REFERENCES usuarios(id) ON DELETE SET NULL;

-- Update comments
COMMENT ON TABLE saas_configuracao_gateway IS 'SaaS payment gateway configuration for receiving payments from workshops';
