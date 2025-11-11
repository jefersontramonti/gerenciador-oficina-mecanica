-- liquibase formatted sql

-- changeset pitstop:create-oficinas-table
-- comment: Creates the oficinas table for storing workshop information

CREATE TABLE IF NOT EXISTS oficinas (
    -- Primary Key
    id UUID PRIMARY KEY,

    -- Basic Identification
    nome_fantasia VARCHAR(200) NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    tipo_pessoa VARCHAR(20) NOT NULL CHECK (tipo_pessoa IN ('PESSOA_FISICA', 'PESSOA_JURIDICA')),
    cnpj_cpf VARCHAR(18) NOT NULL UNIQUE,

    -- Documentation (Optional for ECONOMICO plan, required for PROFISSIONAL/TURBINADO)
    inscricao_estadual VARCHAR(20),
    inscricao_municipal VARCHAR(20),

    -- Legal Representative
    nome_responsavel VARCHAR(200) NOT NULL,
    cpf_responsavel VARCHAR(14),

    -- Contact Information (Embedded: Contato)
    telefone_fixo VARCHAR(15),
    telefone_celular VARCHAR(16) NOT NULL,
    telefone_adicional VARCHAR(16),
    email VARCHAR(200) NOT NULL,
    email_secundario VARCHAR(200),
    website VARCHAR(200),

    -- Address (Embedded: Endereco)
    endereco_logradouro VARCHAR(200),
    endereco_numero VARCHAR(10),
    endereco_complemento VARCHAR(100),
    endereco_bairro VARCHAR(100),
    endereco_cidade VARCHAR(100),
    endereco_estado VARCHAR(2),
    endereco_cep VARCHAR(9),

    -- Operational Information (Embedded: InformacoesOperacionais)
    horario_funcionamento VARCHAR(100),
    capacidade_simultanea INTEGER CHECK (capacidade_simultanea >= 0),
    numero_funcionarios INTEGER CHECK (numero_funcionarios >= 0),
    numero_mecanicos INTEGER CHECK (numero_mecanicos >= 0),

    -- Social Media (Embedded: RedesSociais)
    instagram VARCHAR(100),
    facebook VARCHAR(100),

    -- Banking Data (Embedded: DadosBancarios - Optional)
    banco VARCHAR(100),
    agencia VARCHAR(10),
    conta VARCHAR(20),
    digito_conta VARCHAR(1),
    tipo_conta VARCHAR(20) CHECK (tipo_conta IN ('CORRENTE', 'POUPANCA')),
    chave_pix VARCHAR(50),

    -- Configuration
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVA' CHECK (status IN ('ATIVA', 'INATIVA', 'SUSPENSA', 'CANCELADA')),
    taxa_desconto_maxima DECIMAL(5,2),

    -- Subscription Plan and Billing
    plano VARCHAR(20) DEFAULT 'ECONOMICO' CHECK (plano IN ('ECONOMICO', 'PROFISSIONAL', 'TURBINADO')),
    data_assinatura DATE,
    data_vencimento_plano DATE,
    valor_mensalidade DECIMAL(10,2),

    -- Tax/Fiscal Information (Required only for plans with invoice emission)
    regime_tributario VARCHAR(30) CHECK (regime_tributario IN ('MEI', 'SIMPLES_NACIONAL', 'LUCRO_PRESUMIDO', 'LUCRO_REAL')),
    aliquota_issqn DECIMAL(5,2),

    -- Media
    logo_url VARCHAR(500),

    -- Observations
    observacoes TEXT,

    -- Internal Control
    ativo BOOLEAN NOT NULL DEFAULT true,
    ultimo_acesso TIMESTAMP,

    -- Audit Fields (from AuditableEntity)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_oficinas_cnpj_cpf ON oficinas(cnpj_cpf);
CREATE INDEX IF NOT EXISTS idx_oficinas_email ON oficinas(email);
CREATE INDEX IF NOT EXISTS idx_oficinas_status ON oficinas(status);
CREATE INDEX IF NOT EXISTS idx_oficinas_plano ON oficinas(plano);

COMMENT ON TABLE oficinas IS 'Stores workshop/oficina information for PitStop system. Currently single-tenant (1 record), prepared for future multi-tenant.';
COMMENT ON COLUMN oficinas.nome_fantasia IS 'Trade name (fantasy name) of the workshop';
COMMENT ON COLUMN oficinas.razao_social IS 'Legal name (corporate name) of the company';
COMMENT ON COLUMN oficinas.tipo_pessoa IS 'Type of legal entity: PESSOA_FISICA (individual) or PESSOA_JURIDICA (company)';
COMMENT ON COLUMN oficinas.cnpj_cpf IS 'CNPJ (company tax ID) or CPF (individual tax ID) - 14 or 11 digits';
COMMENT ON COLUMN oficinas.inscricao_estadual IS 'State tax registration number (IE) - Optional for ECONOMICO, required for PROFISSIONAL/TURBINADO';
COMMENT ON COLUMN oficinas.inscricao_municipal IS 'Municipal tax registration number (IM)';
COMMENT ON COLUMN oficinas.plano IS 'Subscription plan: ECONOMICO (R$127, no invoice), PROFISSIONAL (R$237, with invoice), TURBINADO (custom, all features)';
COMMENT ON COLUMN oficinas.regime_tributario IS 'Tax regime: MEI, SIMPLES_NACIONAL, LUCRO_PRESUMIDO, or LUCRO_REAL';
COMMENT ON COLUMN oficinas.status IS 'Operational status: ATIVA (can use system), INATIVA (temporarily disabled), SUSPENSA (blocked), CANCELADA (permanently closed)';
COMMENT ON COLUMN oficinas.ativo IS 'Soft delete flag - false means logically deleted';

-- rollback DROP TABLE IF EXISTS oficinas;
