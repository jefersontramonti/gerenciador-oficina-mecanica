-- =====================================================
-- V028: Add oficina_id to all tables (Multi-Tenancy)
-- =====================================================
-- Purpose: Transform single-tenant to multi-tenant SaaS
-- Strategy: Row-level isolation with oficina_id FK
-- Date: 2025-12-21
-- =====================================================

-- Step 1: Add oficina_id column to all domain tables
-- Note: Making nullable initially to allow data migration
-- Will be made NOT NULL in future migration after seeding

ALTER TABLE usuarios ADD COLUMN oficina_id UUID;
ALTER TABLE clientes ADD COLUMN oficina_id UUID;
ALTER TABLE veiculos ADD COLUMN oficina_id UUID;
ALTER TABLE ordem_servico ADD COLUMN oficina_id UUID;
ALTER TABLE item_os ADD COLUMN oficina_id UUID;
ALTER TABLE pecas ADD COLUMN oficina_id UUID;
ALTER TABLE movimentacao_estoque ADD COLUMN oficina_id UUID;
ALTER TABLE local_armazenamento ADD COLUMN oficina_id UUID;
ALTER TABLE pagamentos ADD COLUMN oficina_id UUID;
ALTER TABLE notas_fiscais ADD COLUMN oficina_id UUID;
ALTER TABLE itens_nota_fiscal ADD COLUMN oficina_id UUID;

-- Step 2: Add Foreign Key constraints
-- ON DELETE RESTRICT prevents deletion of oficina if it has data

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

ALTER TABLE clientes
    ADD CONSTRAINT fk_clientes_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

ALTER TABLE veiculos
    ADD CONSTRAINT fk_veiculos_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

ALTER TABLE ordem_servico
    ADD CONSTRAINT fk_ordem_servico_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

ALTER TABLE item_os
    ADD CONSTRAINT fk_item_os_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

ALTER TABLE pecas
    ADD CONSTRAINT fk_pecas_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

ALTER TABLE movimentacao_estoque
    ADD CONSTRAINT fk_movimentacao_estoque_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

ALTER TABLE local_armazenamento
    ADD CONSTRAINT fk_local_armazenamento_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

ALTER TABLE pagamentos
    ADD CONSTRAINT fk_pagamentos_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

ALTER TABLE notas_fiscais
    ADD CONSTRAINT fk_notas_fiscais_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

ALTER TABLE itens_nota_fiscal
    ADD CONSTRAINT fk_itens_nota_fiscal_oficina
    FOREIGN KEY (oficina_id) REFERENCES oficinas(id) ON DELETE RESTRICT;

-- Step 3: Add indexes for query performance
-- Single-column indexes for filtering by tenant

CREATE INDEX idx_usuarios_oficina_id ON usuarios(oficina_id);
CREATE INDEX idx_clientes_oficina_id ON clientes(oficina_id);
CREATE INDEX idx_veiculos_oficina_id ON veiculos(oficina_id);
CREATE INDEX idx_ordem_servico_oficina_id ON ordem_servico(oficina_id);
CREATE INDEX idx_item_os_oficina_id ON item_os(oficina_id);
CREATE INDEX idx_pecas_oficina_id ON pecas(oficina_id);
CREATE INDEX idx_movimentacao_estoque_oficina_id ON movimentacao_estoque(oficina_id);
CREATE INDEX idx_local_armazenamento_oficina_id ON local_armazenamento(oficina_id);
CREATE INDEX idx_pagamentos_oficina_id ON pagamentos(oficina_id);
CREATE INDEX idx_notas_fiscais_oficina_id ON notas_fiscais(oficina_id);
CREATE INDEX idx_itens_nota_fiscal_oficina_id ON itens_nota_fiscal(oficina_id);

-- Step 4: Add composite indexes for frequent queries
-- These indexes improve performance for typical WHERE clauses

-- Clientes: buscar por oficina + CPF/CNPJ (validação de duplicatas)
CREATE INDEX idx_clientes_oficina_cpf_cnpj ON clientes(oficina_id, cpf_cnpj);

-- Veículos: buscar por oficina + placa (validação de duplicatas)
CREATE INDEX idx_veiculos_oficina_placa ON veiculos(oficina_id, placa);

-- Ordens de Serviço: buscar por oficina + status (listagens filtradas)
CREATE INDEX idx_ordem_servico_oficina_status ON ordem_servico(oficina_id, status);

-- Ordens de Serviço: buscar por oficina + data (ordenação por data)
CREATE INDEX idx_ordem_servico_oficina_data ON ordem_servico(oficina_id, data_abertura DESC);

-- Ordens de Serviço: buscar por oficina + número (busca por número único)
CREATE INDEX idx_ordem_servico_oficina_numero ON ordem_servico(oficina_id, numero);

-- Peças: buscar por oficina + código (validação de duplicatas)
CREATE INDEX idx_pecas_oficina_codigo ON pecas(oficina_id, codigo);

-- Peças: buscar por oficina + estoque baixo (alertas)
CREATE INDEX idx_pecas_oficina_estoque ON pecas(oficina_id, quantidade_atual)
    WHERE quantidade_atual <= quantidade_minima;

-- Movimentação Estoque: buscar por oficina + data (relatórios)
CREATE INDEX idx_movimentacao_oficina_data ON movimentacao_estoque(oficina_id, data_movimentacao DESC);

-- Pagamentos: buscar por oficina + status (financeiro)
CREATE INDEX idx_pagamentos_oficina_status ON pagamentos(oficina_id, status);

-- Pagamentos: buscar por oficina + data vencimento (contas a receber)
CREATE INDEX idx_pagamentos_oficina_vencimento ON pagamentos(oficina_id, data_vencimento);

-- Usuários: buscar por oficina + email (login)
CREATE INDEX idx_usuarios_oficina_email ON usuarios(oficina_id, email);

-- Usuários: buscar por oficina + perfil (RBAC)
CREATE INDEX idx_usuarios_oficina_perfil ON usuarios(oficina_id, perfil);

-- Local Armazenamento: buscar por oficina + hierarquia (navegação)
CREATE INDEX idx_local_armazenamento_oficina_pai ON local_armazenamento(oficina_id, localizacao_pai_id);

-- =====================================================
-- IMPORTANT: Data migration needed!
-- =====================================================
-- TODO: Create V029__migrate_existing_data_to_first_oficina.sql
-- This migration should:
-- 1. Insert a default oficina if none exists
-- 2. Update all records to point to this oficina
-- 3. Make oficina_id NOT NULL
-- =====================================================

COMMENT ON COLUMN usuarios.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
COMMENT ON COLUMN clientes.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
COMMENT ON COLUMN veiculos.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
COMMENT ON COLUMN ordem_servico.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
COMMENT ON COLUMN item_os.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
COMMENT ON COLUMN pecas.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
COMMENT ON COLUMN movimentacao_estoque.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
COMMENT ON COLUMN local_armazenamento.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
COMMENT ON COLUMN pagamentos.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
COMMENT ON COLUMN notas_fiscais.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
COMMENT ON COLUMN itens_nota_fiscal.oficina_id IS 'FK para oficinas - isolamento multi-tenant';
