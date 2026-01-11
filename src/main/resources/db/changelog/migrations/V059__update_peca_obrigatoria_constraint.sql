-- ============================================================
-- V059: Atualizar constraint chk_item_os_peca_obrigatoria
-- ============================================================
-- Permite peca_id NULL quando origem_peca = 'AVULSA' ou 'CLIENTE'
-- Mantém peca_id obrigatório apenas para origem_peca = 'ESTOQUE'
-- ============================================================

-- Remover constraint antiga
ALTER TABLE item_os DROP CONSTRAINT IF EXISTS chk_item_os_peca_obrigatoria;

-- Criar nova constraint que considera origem_peca
-- Regras:
-- 1. SERVICO: peca_id pode ser NULL (não se aplica)
-- 2. PECA + ESTOQUE: peca_id é obrigatório (NOT NULL)
-- 3. PECA + AVULSA: peca_id pode ser NULL
-- 4. PECA + CLIENTE: peca_id pode ser NULL
ALTER TABLE item_os ADD CONSTRAINT chk_item_os_peca_obrigatoria CHECK (
    (tipo = 'SERVICO')
    OR (tipo = 'PECA' AND origem_peca = 'ESTOQUE' AND peca_id IS NOT NULL)
    OR (tipo = 'PECA' AND origem_peca IN ('AVULSA', 'CLIENTE'))
);

-- Também precisamos garantir que peças do tipo PECA tenham origem_peca definida
-- Esta constraint complementa a validação
ALTER TABLE item_os DROP CONSTRAINT IF EXISTS chk_item_os_peca_origem_obrigatoria;
ALTER TABLE item_os ADD CONSTRAINT chk_item_os_peca_origem_obrigatoria CHECK (
    (tipo = 'SERVICO')
    OR (tipo = 'PECA' AND origem_peca IS NOT NULL)
);

-- Atualizar comentários
COMMENT ON CONSTRAINT chk_item_os_peca_obrigatoria ON item_os IS
    'Se tipo = PECA e origem = ESTOQUE, peca_id é obrigatório. Para AVULSA/CLIENTE, peca_id pode ser NULL';
COMMENT ON CONSTRAINT chk_item_os_peca_origem_obrigatoria ON item_os IS
    'Se tipo = PECA, origem_peca é obrigatório (ESTOQUE, AVULSA ou CLIENTE)';
