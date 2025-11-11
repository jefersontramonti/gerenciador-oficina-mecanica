-- ============================================================================
-- Migration: Create movimentacao_estoque table
-- Description: Stock movements audit trail with complete history
-- Author: PitStop Team
-- Date: 2025-11-02
-- ============================================================================

CREATE TABLE movimentacao_estoque (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    peca_id UUID NOT NULL,
    ordem_servico_id UUID,
    usuario_id UUID NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    quantidade INTEGER NOT NULL,

    -- Audit trail - quantidade antes e depois da movimentação
    quantidade_anterior INTEGER NOT NULL,
    quantidade_atual INTEGER NOT NULL,

    -- Financial - valores no momento da movimentação
    valor_unitario DECIMAL(10, 2) NOT NULL,
    valor_total DECIMAL(10, 2) NOT NULL,

    -- Details
    motivo VARCHAR(100) NOT NULL,
    observacao TEXT,
    data_movimentacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Audit timestamp
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_movimentacao_estoque_peca FOREIGN KEY (peca_id)
        REFERENCES pecas(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movimentacao_estoque_os FOREIGN KEY (ordem_servico_id)
        REFERENCES ordem_servico(id) ON DELETE SET NULL,
    CONSTRAINT fk_movimentacao_estoque_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id) ON DELETE RESTRICT,

    -- Business constraints
    CONSTRAINT chk_movimentacao_tipo CHECK (tipo IN ('ENTRADA', 'SAIDA', 'AJUSTE', 'DEVOLUCAO', 'BAIXA_OS')),
    CONSTRAINT chk_movimentacao_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_movimentacao_quantidade_anterior CHECK (quantidade_anterior >= 0),
    CONSTRAINT chk_movimentacao_quantidade_atual CHECK (quantidade_atual >= 0),
    CONSTRAINT chk_movimentacao_valor_unitario CHECK (valor_unitario >= 0),
    CONSTRAINT chk_movimentacao_valor_total CHECK (valor_total >= 0),
    CONSTRAINT chk_movimentacao_motivo_min_length CHECK (LENGTH(TRIM(motivo)) >= 3)
);

-- Performance indexes for common queries
CREATE INDEX idx_movimentacao_peca_id ON movimentacao_estoque(peca_id);
CREATE INDEX idx_movimentacao_os_id ON movimentacao_estoque(ordem_servico_id);
CREATE INDEX idx_movimentacao_usuario_id ON movimentacao_estoque(usuario_id);
CREATE INDEX idx_movimentacao_tipo ON movimentacao_estoque(tipo);
CREATE INDEX idx_movimentacao_data ON movimentacao_estoque(data_movimentacao DESC);
CREATE INDEX idx_movimentacao_peca_data ON movimentacao_estoque(peca_id, data_movimentacao DESC);

-- Composite index for common filter combinations
CREATE INDEX idx_movimentacao_tipo_data ON movimentacao_estoque(tipo, data_movimentacao DESC);

-- Comments for documentation
COMMENT ON TABLE movimentacao_estoque IS 'Registro imutável de todas as movimentações de estoque (audit trail completo)';
COMMENT ON COLUMN movimentacao_estoque.peca_id IS 'FK para pecas - peça movimentada';
COMMENT ON COLUMN movimentacao_estoque.ordem_servico_id IS 'FK opcional para ordem_servico - vincula movimentação a uma OS (null se movimentação manual)';
COMMENT ON COLUMN movimentacao_estoque.usuario_id IS 'FK para usuarios - quem realizou a movimentação';
COMMENT ON COLUMN movimentacao_estoque.tipo IS 'Tipo da movimentação: ENTRADA, SAIDA, AJUSTE, DEVOLUCAO, BAIXA_OS';
COMMENT ON COLUMN movimentacao_estoque.quantidade IS 'Quantidade movimentada (sempre positiva, o tipo define se é entrada/saída)';
COMMENT ON COLUMN movimentacao_estoque.quantidade_anterior IS 'Estoque da peça ANTES da movimentação (auditoria)';
COMMENT ON COLUMN movimentacao_estoque.quantidade_atual IS 'Estoque da peça DEPOIS da movimentação (auditoria)';
COMMENT ON COLUMN movimentacao_estoque.valor_unitario IS 'Valor unitário da peça no momento da movimentação';
COMMENT ON COLUMN movimentacao_estoque.valor_total IS 'Valor total da movimentação (quantidade * valor_unitario)';
COMMENT ON COLUMN movimentacao_estoque.motivo IS 'Descrição do motivo da movimentação (ex: "Compra fornecedor", "OS #123")';
COMMENT ON COLUMN movimentacao_estoque.observacao IS 'Detalhes adicionais opcionais';
COMMENT ON COLUMN movimentacao_estoque.data_movimentacao IS 'Data/hora em que a movimentação ocorreu';

COMMENT ON CONSTRAINT fk_movimentacao_estoque_peca ON movimentacao_estoque
    IS 'RESTRICT: não pode deletar peça que possui movimentações (integridade histórica)';
COMMENT ON CONSTRAINT fk_movimentacao_estoque_os ON movimentacao_estoque
    IS 'SET NULL: se OS for deletada, mantém movimentação mas perde referência';
COMMENT ON CONSTRAINT fk_movimentacao_estoque_usuario ON movimentacao_estoque
    IS 'RESTRICT: não pode deletar usuário que fez movimentações (auditoria)';
