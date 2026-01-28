--liquibase formatted sql

--changeset pitstop:V087-add-new-fields-to-pecas

-- =====================================================
-- V087: Adicionar novos campos à tabela pecas
-- Campos: nome, codigo_original, codigo_fabricante,
--          codigo_barras, ncm, categoria,
--          quantidade_maxima, ponto_pedido,
--          fornecedor_principal, observacoes
-- =====================================================

-- 1. Adicionar colunas
ALTER TABLE pecas ADD COLUMN nome VARCHAR(150);
ALTER TABLE pecas ADD COLUMN codigo_original VARCHAR(100);
ALTER TABLE pecas ADD COLUMN codigo_fabricante VARCHAR(100);
ALTER TABLE pecas ADD COLUMN codigo_barras VARCHAR(50);
ALTER TABLE pecas ADD COLUMN ncm VARCHAR(20);
ALTER TABLE pecas ADD COLUMN categoria VARCHAR(30);
ALTER TABLE pecas ADD COLUMN quantidade_maxima INTEGER;
ALTER TABLE pecas ADD COLUMN ponto_pedido INTEGER;
ALTER TABLE pecas ADD COLUMN fornecedor_principal VARCHAR(200);
ALTER TABLE pecas ADD COLUMN observacoes TEXT;

-- 2. Backfill: preencher nome com LEFT(descricao, 150) para rows existentes
UPDATE pecas SET nome = LEFT(descricao, 150) WHERE nome IS NULL;

-- 3. Tornar nome NOT NULL
ALTER TABLE pecas ALTER COLUMN nome SET NOT NULL;

-- 4. CHECK constraints
ALTER TABLE pecas ADD CONSTRAINT chk_pecas_categoria
    CHECK (categoria IS NULL OR categoria IN (
        'FILTRO', 'CORREIA', 'PASTILHA_FREIO', 'DISCO_FREIO',
        'AMORTECEDOR', 'OLEO_LUBRIFICANTE', 'FLUIDO', 'VELA_IGNICAO',
        'BATERIA', 'PNEU', 'LAMPADA', 'ROLAMENTO', 'JUNTA', 'RETENTOR',
        'SENSOR', 'BOMBA', 'EMBREAGEM', 'SUSPENSAO', 'POLIA', 'MANGUEIRA',
        'ELETRICO', 'FUNILARIA', 'ACESSORIO', 'OUTROS'
    ));

ALTER TABLE pecas ADD CONSTRAINT chk_pecas_quantidade_maxima
    CHECK (quantidade_maxima IS NULL OR quantidade_maxima >= 0);

ALTER TABLE pecas ADD CONSTRAINT chk_pecas_ponto_pedido
    CHECK (ponto_pedido IS NULL OR ponto_pedido >= 0);

ALTER TABLE pecas ADD CONSTRAINT chk_pecas_nome_min_length
    CHECK (LENGTH(TRIM(nome)) >= 2);

-- 5. Indexes
CREATE INDEX idx_pecas_nome ON pecas(nome);
CREATE INDEX idx_pecas_categoria ON pecas(categoria);
CREATE INDEX idx_pecas_codigo_original ON pecas(codigo_original);
CREATE INDEX idx_pecas_codigo_barras ON pecas(codigo_barras);
