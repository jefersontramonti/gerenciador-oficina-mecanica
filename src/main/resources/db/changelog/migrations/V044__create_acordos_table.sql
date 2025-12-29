-- liquibase formatted sql

-- changeset pitstop:create-acordos-table
-- comment: Create payment agreements table for default management

CREATE TABLE acordos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero VARCHAR(20) NOT NULL UNIQUE,
    oficina_id UUID NOT NULL REFERENCES oficinas(id),

    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',

    -- Original values
    valor_original DECIMAL(10, 2) NOT NULL,
    percentual_desconto DECIMAL(5, 2) DEFAULT 0,
    valor_desconto DECIMAL(10, 2) DEFAULT 0,
    valor_acordado DECIMAL(10, 2) NOT NULL,

    -- Installments
    numero_parcelas INTEGER NOT NULL,
    valor_parcela DECIMAL(10, 2) NOT NULL,
    parcelas_pagas INTEGER DEFAULT 0,
    valor_pago DECIMAL(10, 2) DEFAULT 0,

    -- Dates
    data_acordo DATE NOT NULL DEFAULT CURRENT_DATE,
    primeiro_vencimento DATE NOT NULL,

    -- Notes and audit
    observacoes TEXT,
    criado_por UUID,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_status_acordo CHECK (status IN ('ATIVO', 'QUITADO', 'QUEBRADO', 'CANCELADO')),
    CONSTRAINT chk_parcelas_positivas CHECK (numero_parcelas > 0),
    CONSTRAINT chk_valor_acordado_positivo CHECK (valor_acordado > 0)
);

-- Create indexes for performance
CREATE INDEX idx_acordos_oficina_id ON acordos(oficina_id);
CREATE INDEX idx_acordos_status ON acordos(status);
CREATE INDEX idx_acordos_numero ON acordos(numero);

-- Create junction table for agreement-invoices relationship
CREATE TABLE acordo_faturas (
    acordo_id UUID NOT NULL REFERENCES acordos(id) ON DELETE CASCADE,
    fatura_id UUID NOT NULL REFERENCES faturas(id),
    PRIMARY KEY (acordo_id, fatura_id)
);

CREATE INDEX idx_acordo_faturas_acordo ON acordo_faturas(acordo_id);
CREATE INDEX idx_acordo_faturas_fatura ON acordo_faturas(fatura_id);

-- Create installments table
CREATE TABLE parcelas_acordo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    acordo_id UUID NOT NULL REFERENCES acordos(id) ON DELETE CASCADE,

    numero_parcela INTEGER NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,

    data_vencimento DATE NOT NULL,
    data_pagamento DATE,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    metodo_pagamento VARCHAR(50),
    transacao_id VARCHAR(100),
    observacao TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_status_parcela CHECK (status IN ('PENDENTE', 'PAGO', 'VENCIDO', 'CANCELADO')),
    CONSTRAINT uk_parcela_numero UNIQUE (acordo_id, numero_parcela)
);

CREATE INDEX idx_parcelas_acordo_id ON parcelas_acordo(acordo_id);
CREATE INDEX idx_parcelas_status ON parcelas_acordo(status);
CREATE INDEX idx_parcelas_vencimento ON parcelas_acordo(data_vencimento);

-- Create sequence for agreement numbers
CREATE SEQUENCE IF NOT EXISTS acordo_numero_seq START WITH 1;

COMMENT ON TABLE acordos IS 'Payment agreements for negotiating overdue invoices';
COMMENT ON TABLE acordo_faturas IS 'Junction table linking agreements to their invoices';
COMMENT ON TABLE parcelas_acordo IS 'Installments of payment agreements';

-- changeset pitstop:create-acordos-trigger splitStatements:false
-- comment: Create trigger to update updated_at on acordos

CREATE OR REPLACE FUNCTION update_acordos_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_acordos_updated_at
    BEFORE UPDATE ON acordos
    FOR EACH ROW
    EXECUTE FUNCTION update_acordos_updated_at();
