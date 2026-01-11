-- =====================================================
-- V057: Adicionar campo origem_peca na tabela item_os
-- =====================================================
-- Permite distinguir explicitamente entre:
-- - ESTOQUE: Peça do inventário da oficina (baixa automática)
-- - AVULSA: Peça comprada externamente (sem controle de estoque)
-- - CLIENTE: Peça fornecida pelo cliente (sem controle de estoque)

-- Adicionar coluna origem_peca
ALTER TABLE item_os ADD COLUMN IF NOT EXISTS origem_peca VARCHAR(20);

-- Migrar dados existentes baseado no pecaId
-- Se tem pecaId e tipo é PECA, é do estoque
UPDATE item_os SET origem_peca = 'ESTOQUE'
WHERE tipo = 'PECA' AND peca_id IS NOT NULL AND origem_peca IS NULL;

-- Se não tem pecaId e tipo é PECA, é avulsa
UPDATE item_os SET origem_peca = 'AVULSA'
WHERE tipo = 'PECA' AND peca_id IS NULL AND origem_peca IS NULL;

-- Constraint para valores válidos
ALTER TABLE item_os ADD CONSTRAINT chk_item_os_origem_peca
    CHECK (origem_peca IS NULL OR origem_peca IN ('ESTOQUE', 'AVULSA', 'CLIENTE'));

-- Índice para consultas (ex: relatório de peças avulsas)
CREATE INDEX IF NOT EXISTS idx_item_os_origem_peca ON item_os(origem_peca) WHERE origem_peca IS NOT NULL;

-- Índice composto para filtrar itens do estoque que geram baixa
CREATE INDEX IF NOT EXISTS idx_item_os_peca_estoque ON item_os(peca_id, origem_peca)
WHERE tipo = 'PECA' AND origem_peca = 'ESTOQUE';

-- Comentários
COMMENT ON COLUMN item_os.origem_peca IS 'Origem da peça: ESTOQUE (inventário, gera baixa), AVULSA (compra externa), CLIENTE (fornecida pelo cliente)';
