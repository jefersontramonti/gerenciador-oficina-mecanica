-- Migration: Create fornecedores table
-- Description: Supplier management with support for both individual and business suppliers
-- Author: PitStop Team
-- Date: 2026-01-28

CREATE TABLE fornecedores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Multi-tenant
    oficina_id UUID NOT NULL REFERENCES oficinas(id),

    -- Tipo
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('FABRICANTE', 'DISTRIBUIDOR', 'ATACADISTA', 'VAREJISTA', 'IMPORTADOR', 'OUTRO')),

    -- Identification
    nome_fantasia VARCHAR(200) NOT NULL,
    razao_social VARCHAR(200),
    cpf_cnpj VARCHAR(18),
    inscricao_estadual VARCHAR(20),

    -- Contact
    email VARCHAR(100),
    telefone VARCHAR(20),
    celular VARCHAR(20),
    website VARCHAR(200),
    contato_nome VARCHAR(150),

    -- Embedded Address (same pattern as clientes)
    logradouro VARCHAR(200),
    numero VARCHAR(10),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(2),
    cep VARCHAR(9),

    -- Commercial
    prazo_entrega VARCHAR(100),
    condicoes_pagamento VARCHAR(200),
    desconto_padrao NUMERIC(5,2) DEFAULT 0,

    -- Notes
    observacoes TEXT,

    -- Soft Delete
    ativo BOOLEAN NOT NULL DEFAULT true,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fornecedores_email_format CHECK (
        email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    ),
    CONSTRAINT fornecedores_desconto_range CHECK (
        desconto_padrao IS NULL OR (desconto_padrao >= 0 AND desconto_padrao <= 100)
    )
);

-- Unique CPF/CNPJ per oficina (only when not null)
CREATE UNIQUE INDEX idx_fornecedores_cpf_cnpj_oficina ON fornecedores(oficina_id, cpf_cnpj) WHERE cpf_cnpj IS NOT NULL AND ativo = true;

-- Indexes for Performance
CREATE INDEX idx_fornecedores_oficina_id ON fornecedores(oficina_id);
CREATE INDEX idx_fornecedores_nome_fantasia ON fornecedores(nome_fantasia) WHERE ativo = true;
CREATE INDEX idx_fornecedores_tipo ON fornecedores(tipo) WHERE ativo = true;
CREATE INDEX idx_fornecedores_ativo ON fornecedores(ativo);
CREATE INDEX idx_fornecedores_created_at ON fornecedores(created_at);

-- Comments
COMMENT ON TABLE fornecedores IS 'Supplier records for inventory parts management';
COMMENT ON COLUMN fornecedores.tipo IS 'Supplier type: FABRICANTE, DISTRIBUIDOR, ATACADISTA, VAREJISTA, IMPORTADOR, OUTRO';
COMMENT ON COLUMN fornecedores.cpf_cnpj IS 'Brazilian tax ID: CPF or CNPJ, stored with formatting';
COMMENT ON COLUMN fornecedores.ativo IS 'Soft delete flag - false indicates deleted suppliers';
