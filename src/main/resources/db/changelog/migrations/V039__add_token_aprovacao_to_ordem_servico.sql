-- =====================================================
-- V039: Add token_aprovacao columns to ordem_servico
-- =====================================================
-- Purpose: Enable public approval of quotations via unique token
-- Author: PitStop Team
-- Date: 2025-01-17
-- =====================================================

-- Add token_aprovacao column (unique token for public approval)
ALTER TABLE ordem_servico
ADD COLUMN IF NOT EXISTS token_aprovacao VARCHAR(64) UNIQUE;

-- Add token_aprovacao_expiracao column (token expiration datetime)
ALTER TABLE ordem_servico
ADD COLUMN IF NOT EXISTS token_aprovacao_expiracao TIMESTAMP;

-- Create index for faster token lookup
CREATE INDEX IF NOT EXISTS idx_ordem_servico_token_aprovacao
ON ordem_servico(token_aprovacao) WHERE token_aprovacao IS NOT NULL;

-- Add comment to columns
COMMENT ON COLUMN ordem_servico.token_aprovacao IS 'Unique token for public approval of quotation by customer';
COMMENT ON COLUMN ordem_servico.token_aprovacao_expiracao IS 'Expiration datetime of approval token (7 days from creation)';
