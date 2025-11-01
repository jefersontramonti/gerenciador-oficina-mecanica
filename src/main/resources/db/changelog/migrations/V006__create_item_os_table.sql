-- Migration: Create item_os table
-- Description: Items (parts and services) of a service order
-- Author: PitStop Team
-- Date: 2025-11-01

CREATE TABLE item_os (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ordem_servico_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    peca_id UUID,
    descricao VARCHAR(500) NOT NULL,
    quantidade INTEGER NOT NULL,
    valor_unitario DECIMAL(10, 2) NOT NULL,
    desconto DECIMAL(10, 2) DEFAULT 0.00,
    valor_total DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_item_os_ordem_servico FOREIGN KEY (ordem_servico_id) REFERENCES ordem_servico(id) ON DELETE CASCADE,
    -- Note: peca_id FK will be added when pecas table is created in future migration

    -- Check constraints
    CONSTRAINT chk_item_os_tipo CHECK (tipo IN ('PECA', 'SERVICO')),
    CONSTRAINT chk_item_os_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_item_os_valor_unitario CHECK (valor_unitario >= 0),
    CONSTRAINT chk_item_os_desconto CHECK (desconto >= 0),
    CONSTRAINT chk_item_os_valor_total CHECK (valor_total >= 0),
    CONSTRAINT chk_item_os_descricao_min_length CHECK (LENGTH(TRIM(descricao)) >= 3),
    CONSTRAINT chk_item_os_peca_obrigatoria CHECK (
        (tipo = 'SERVICO') OR (tipo = 'PECA' AND peca_id IS NOT NULL)
    )
);

-- Performance indexes
CREATE INDEX idx_item_os_ordem_servico_id ON item_os(ordem_servico_id);
CREATE INDEX idx_item_os_peca_id ON item_os(peca_id);
CREATE INDEX idx_item_os_tipo ON item_os(tipo);
CREATE INDEX idx_item_os_created_at ON item_os(created_at DESC);

-- Comments
COMMENT ON TABLE item_os IS 'Itens de uma ordem de serviço (peças e serviços)';
COMMENT ON COLUMN item_os.ordem_servico_id IS 'FK para ordem_servico - relacionamento Many-to-One obrigatório';
COMMENT ON COLUMN item_os.tipo IS 'Tipo do item: PECA (peça do estoque) ou SERVICO (mão de obra/serviço)';
COMMENT ON COLUMN item_os.peca_id IS 'FK para pecas (será criada no futuro) - obrigatório se tipo = PECA';
COMMENT ON COLUMN item_os.descricao IS 'Descrição do item (mínimo 3 caracteres)';
COMMENT ON COLUMN item_os.quantidade IS 'Quantidade do item (deve ser > 0)';
COMMENT ON COLUMN item_os.valor_unitario IS 'Valor unitário do item';
COMMENT ON COLUMN item_os.desconto IS 'Desconto aplicado neste item (em valor absoluto)';
COMMENT ON COLUMN item_os.valor_total IS 'Valor total do item: (quantidade * valor_unitario) - desconto';
COMMENT ON CONSTRAINT fk_item_os_ordem_servico ON item_os IS 'FK com ON DELETE CASCADE - ao deletar OS, deleta todos os itens (agregado)';
COMMENT ON CONSTRAINT chk_item_os_tipo ON item_os IS 'Tipo deve ser PECA ou SERVICO';
COMMENT ON CONSTRAINT chk_item_os_peca_obrigatoria ON item_os IS 'Se tipo = PECA, o campo peca_id é obrigatório (FK para catálogo de peças)';
