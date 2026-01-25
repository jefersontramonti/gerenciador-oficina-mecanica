-- V084: Create leads table for SaaS lead capture
-- Author: PitStop Team
-- Date: 2026-01-25

-- ============================================================
-- TABLE: leads
-- ============================================================

CREATE TABLE IF NOT EXISTS leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    whatsapp VARCHAR(20) NOT NULL,
    origem VARCHAR(100) DEFAULT 'landing-page',
    status VARCHAR(50) NOT NULL DEFAULT 'NOVO',
    observacoes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_leads_status CHECK (status IN ('NOVO', 'CONTATADO', 'QUALIFICADO', 'CONVERTIDO', 'PERDIDO'))
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_created_at ON leads(created_at DESC);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_origem ON leads(origem);

-- ============================================================
-- COMMENTS
-- ============================================================

COMMENT ON TABLE leads IS 'Leads capturados de landing pages e formulários de contato';
COMMENT ON COLUMN leads.id IS 'Identificador único do lead (UUID)';
COMMENT ON COLUMN leads.nome IS 'Nome completo do lead';
COMMENT ON COLUMN leads.email IS 'Email do lead';
COMMENT ON COLUMN leads.whatsapp IS 'Número do WhatsApp (apenas dígitos)';
COMMENT ON COLUMN leads.origem IS 'Origem do lead (landing-page, indicacao, google-ads, etc)';
COMMENT ON COLUMN leads.status IS 'Status do lead no funil de vendas';
COMMENT ON COLUMN leads.observacoes IS 'Anotações da equipe de vendas sobre o lead';
COMMENT ON COLUMN leads.created_at IS 'Data e hora de criação do lead';
COMMENT ON COLUMN leads.updated_at IS 'Data e hora da última atualização';
