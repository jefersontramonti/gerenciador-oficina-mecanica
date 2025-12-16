-- =====================================================
-- Migration: V025 - Create notas_fiscais tables
-- Description: Creates tables for invoice management (NF-e, NFS-e, NFC-e)
--              STRUCTURE PREPARED FOR FUTURE IMPLEMENTATION
--              See docs/NFE_IMPLEMENTATION_PLAN.md for details
-- Author: PitStop Team
-- Date: 2025-11-11
-- =====================================================

-- Create notas_fiscais table
CREATE TABLE notas_fiscais (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ordem_servico_id UUID NOT NULL,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('NFE', 'NFSE', 'NFCE')),
    status VARCHAR(20) NOT NULL DEFAULT 'DIGITACAO' CHECK (status IN ('DIGITACAO', 'VALIDADA', 'ASSINADA', 'ENVIADA', 'AUTORIZADA', 'DENEGADA', 'REJEITADA', 'CANCELADA', 'INUTILIZADA')),
    numero BIGINT NOT NULL CHECK (numero > 0),
    serie INTEGER NOT NULL DEFAULT 1,
    chave_acesso VARCHAR(44) UNIQUE,
    protocolo_autorizacao VARCHAR(50),
    data_hora_autorizacao TIMESTAMP,
    valor_total DECIMAL(15,2) NOT NULL CHECK (valor_total > 0),
    xml_enviado TEXT,
    xml_autorizado TEXT,
    xml_cancelamento TEXT,
    protocolo_cancelamento VARCHAR(50),
    data_hora_cancelamento TIMESTAMP,
    justificativa_cancelamento TEXT,
    natureza_operacao VARCHAR(60),
    cfop VARCHAR(4),
    informacoes_complementares TEXT,
    data_emissao TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint for numero + serie combination
    CONSTRAINT uk_nfe_numero_serie UNIQUE (numero, serie)
);

-- Create indexes for notas_fiscais
CREATE INDEX idx_notas_fiscais_os ON notas_fiscais(ordem_servico_id);
CREATE INDEX idx_notas_fiscais_numero_serie ON notas_fiscais(numero, serie);
CREATE INDEX idx_notas_fiscais_status ON notas_fiscais(status);
CREATE INDEX idx_notas_fiscais_chave_acesso ON notas_fiscais(chave_acesso);
CREATE INDEX idx_notas_fiscais_data_emissao ON notas_fiscais(data_emissao);

-- Create itens_nota_fiscal table
CREATE TABLE itens_nota_fiscal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nota_fiscal_id UUID NOT NULL,
    numero_item INTEGER NOT NULL CHECK (numero_item > 0),
    codigo VARCHAR(60),
    codigo_ean VARCHAR(14),
    descricao VARCHAR(120) NOT NULL,
    ncm VARCHAR(8),
    cest VARCHAR(7),
    cfop VARCHAR(4) NOT NULL,
    unidade_comercial VARCHAR(6) NOT NULL,
    quantidade DECIMAL(15,4) NOT NULL CHECK (quantidade > 0),
    valor_unitario DECIMAL(15,4) NOT NULL CHECK (valor_unitario > 0),
    valor_total DECIMAL(15,2) NOT NULL CHECK (valor_total > 0),
    valor_desconto DECIMAL(15,2) DEFAULT 0,
    valor_outras_despesas DECIMAL(15,2) DEFAULT 0,

    -- Tributação (placeholders for future implementation)
    cst_icms VARCHAR(3),
    aliquota_icms DECIMAL(5,2),
    valor_icms DECIMAL(15,2),
    cst_pis VARCHAR(2),
    aliquota_pis DECIMAL(5,2),
    valor_pis DECIMAL(15,2),
    cst_cofins VARCHAR(2),
    aliquota_cofins DECIMAL(5,2),
    valor_cofins DECIMAL(15,2),
    aliquota_ipi DECIMAL(5,2),
    valor_ipi DECIMAL(15,2),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_itens_nf_nota_fiscal FOREIGN KEY (nota_fiscal_id) REFERENCES notas_fiscais(id) ON DELETE CASCADE,

    -- Unique constraint for numeroItem per nota
    CONSTRAINT uk_item_nfe UNIQUE (nota_fiscal_id, numero_item)
);

-- Create indexes for itens_nota_fiscal
CREATE INDEX idx_itens_nf_nota_fiscal_id ON itens_nota_fiscal(nota_fiscal_id);
CREATE INDEX idx_itens_nf_numero_item ON itens_nota_fiscal(nota_fiscal_id, numero_item);

-- Add foreign key from pagamentos to notas_fiscais (if not exists)
-- This allows linking payments to invoices when NF-e is implemented
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_pagamentos_nota_fiscal'
        AND table_name = 'pagamentos'
    ) THEN
        ALTER TABLE pagamentos
        ADD CONSTRAINT fk_pagamentos_nota_fiscal
        FOREIGN KEY (nota_fiscal_id) REFERENCES notas_fiscais(id) ON DELETE SET NULL;
    END IF;
END$$;

-- Add comments to tables
COMMENT ON TABLE notas_fiscais IS 'Tabela de notas fiscais eletrônicas (NF-e/NFS-e/NFC-e). ESTRUTURA PREPARADA PARA FUTURO. Ver docs/NFE_IMPLEMENTATION_PLAN.md';
COMMENT ON TABLE itens_nota_fiscal IS 'Itens das notas fiscais com informações tributárias completas';

-- Add comments to key columns
COMMENT ON COLUMN notas_fiscais.tipo IS 'Tipo de nota fiscal: NFE (modelo 55), NFSE (municipal), NFCE (modelo 65)';
COMMENT ON COLUMN notas_fiscais.status IS 'Status da nota: DIGITACAO, VALIDADA, ASSINADA, ENVIADA, AUTORIZADA, DENEGADA, REJEITADA, CANCELADA, INUTILIZADA';
COMMENT ON COLUMN notas_fiscais.chave_acesso IS 'Chave de acesso da NF-e (44 dígitos) - preenchida após autorização SEFAZ';
COMMENT ON COLUMN notas_fiscais.protocolo_autorizacao IS 'Protocolo de autorização retornado pela SEFAZ/Prefeitura';
COMMENT ON COLUMN notas_fiscais.xml_enviado IS 'XML assinado digitalmente enviado para SEFAZ/Prefeitura';
COMMENT ON COLUMN notas_fiscais.xml_autorizado IS 'XML autorizado retornado pela SEFAZ/Prefeitura';
COMMENT ON COLUMN notas_fiscais.xml_cancelamento IS 'XML de cancelamento (se houver)';

-- Create sequence for nota fiscal numbers (will be used in future implementation)
CREATE SEQUENCE IF NOT EXISTS nfe_numero_seq START WITH 1 INCREMENT BY 1;

COMMENT ON SEQUENCE nfe_numero_seq IS 'Sequence para numeração sequencial de notas fiscais';
