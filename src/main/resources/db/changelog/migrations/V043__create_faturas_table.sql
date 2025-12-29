-- liquibase formatted sql

-- changeset pitstop:create-faturas-table
-- comment: Creates tables for invoice management in SaaS billing system

-- Create faturas table
CREATE TABLE faturas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Identification
    numero VARCHAR(20) NOT NULL UNIQUE,  -- FAT-2025-00001

    -- Relationships
    oficina_id UUID NOT NULL REFERENCES oficinas(id),
    plano_codigo VARCHAR(30),  -- Snapshot do plano na época

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE', 'PAGO', 'VENCIDO', 'CANCELADO')),

    -- Financial values
    valor_base DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    valor_desconto DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    valor_acrescimos DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    valor_total DECIMAL(10, 2) NOT NULL DEFAULT 0.00,

    -- Important dates
    mes_referencia DATE NOT NULL,  -- First day of the reference month
    data_emissao DATE NOT NULL DEFAULT CURRENT_DATE,
    data_vencimento DATE NOT NULL,
    data_pagamento TIMESTAMP WITH TIME ZONE,

    -- Payment info (when paid)
    metodo_pagamento VARCHAR(50),
    transacao_id VARCHAR(100),  -- External payment ID (Mercado Pago, etc)

    -- Mercado Pago integration
    qr_code_pix TEXT,
    link_pagamento VARCHAR(500),

    -- Notes and tracking
    observacao TEXT,
    tentativas_cobranca INTEGER DEFAULT 0,
    proxima_tentativa TIMESTAMP WITH TIME ZONE,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create itens_fatura table
CREATE TABLE itens_fatura (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    fatura_id UUID NOT NULL REFERENCES faturas(id) ON DELETE CASCADE,

    descricao VARCHAR(200) NOT NULL,
    quantidade INTEGER NOT NULL DEFAULT 1,
    valor_unitario DECIMAL(10, 2) NOT NULL,
    valor_total DECIMAL(10, 2) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create sequence for invoice numbering
CREATE SEQUENCE IF NOT EXISTS seq_fatura_numero START 1;

-- Create indexes for faturas
CREATE INDEX idx_faturas_oficina_id ON faturas(oficina_id);
CREATE INDEX idx_faturas_status ON faturas(status);
CREATE INDEX idx_faturas_data_vencimento ON faturas(data_vencimento);
CREATE INDEX idx_faturas_data_emissao ON faturas(data_emissao);
CREATE INDEX idx_faturas_mes_referencia ON faturas(mes_referencia);
CREATE INDEX idx_faturas_numero ON faturas(numero);

-- Composite indexes for common queries
CREATE INDEX idx_faturas_oficina_status ON faturas(oficina_id, status);
CREATE INDEX idx_faturas_status_vencimento ON faturas(status, data_vencimento);

-- Unique constraint to prevent duplicate invoices for same month
CREATE UNIQUE INDEX idx_faturas_oficina_mes_unico
    ON faturas(oficina_id, mes_referencia)
    WHERE status != 'CANCELADO';

-- Create index for itens_fatura
CREATE INDEX idx_itens_fatura_fatura_id ON itens_fatura(fatura_id);

-- Add comments
COMMENT ON TABLE faturas IS 'Monthly invoices for workshop subscriptions';
COMMENT ON COLUMN faturas.numero IS 'Unique invoice number in format FAT-YYYY-NNNNN';
COMMENT ON COLUMN faturas.plano_codigo IS 'Snapshot of the plan code at invoice creation time';
COMMENT ON COLUMN faturas.mes_referencia IS 'First day of the billing month (e.g., 2025-01-01 for January 2025)';
COMMENT ON COLUMN faturas.transacao_id IS 'External payment gateway transaction ID';
COMMENT ON COLUMN faturas.tentativas_cobranca IS 'Number of payment reminder attempts sent';

COMMENT ON TABLE itens_fatura IS 'Line items for each invoice';

-- changeset pitstop:create-faturas-trigger splitStatements:false
-- comment: Creates updated_at trigger function for faturas

CREATE OR REPLACE FUNCTION update_faturas_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_faturas_updated_at
    BEFORE UPDATE ON faturas
    FOR EACH ROW
    EXECUTE FUNCTION update_faturas_updated_at();
