-- Migration: Create clientes table
-- Description: Customer management with support for both individual (CPF) and business (CNPJ) customers
-- Author: PitStop Team
-- Date: 2025-10-31

CREATE TABLE clientes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Basic Information
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('PESSOA_FISICA', 'PESSOA_JURIDICA')),
    nome VARCHAR(150) NOT NULL,
    cpf_cnpj VARCHAR(18) NOT NULL UNIQUE,
    email VARCHAR(100),
    telefone VARCHAR(20),
    celular VARCHAR(20),

    -- Embedded Address (Value Object)
    logradouro VARCHAR(200),
    numero VARCHAR(10),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    estado VARCHAR(2),
    cep VARCHAR(9),

    -- Soft Delete
    ativo BOOLEAN NOT NULL DEFAULT true,

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT clientes_cpf_cnpj_format CHECK (
        cpf_cnpj ~ '^[0-9]{3}\.[0-9]{3}\.[0-9]{3}-[0-9]{2}$' OR  -- CPF: 000.000.000-00
        cpf_cnpj ~ '^[0-9]{2}\.[0-9]{3}\.[0-9]{3}/[0-9]{4}-[0-9]{2}$'  -- CNPJ: 00.000.000/0000-00
    ),

    CONSTRAINT clientes_email_format CHECK (
        email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    )
);

-- Indexes for Performance
CREATE INDEX idx_clientes_cpf_cnpj ON clientes(cpf_cnpj) WHERE ativo = true;
CREATE INDEX idx_clientes_nome ON clientes(nome) WHERE ativo = true;
CREATE INDEX idx_clientes_tipo ON clientes(tipo) WHERE ativo = true;
CREATE INDEX idx_clientes_ativo ON clientes(ativo);
CREATE INDEX idx_clientes_created_at ON clientes(created_at);

-- Comments
COMMENT ON TABLE clientes IS 'Customer records supporting both individual (CPF) and business (CNPJ) entities';
COMMENT ON COLUMN clientes.tipo IS 'Customer type: PESSOA_FISICA (individual) or PESSOA_JURIDICA (business)';
COMMENT ON COLUMN clientes.cpf_cnpj IS 'Brazilian tax ID: CPF (11 digits) or CNPJ (14 digits), stored with formatting';
COMMENT ON COLUMN clientes.ativo IS 'Soft delete flag - false indicates deleted customers';
