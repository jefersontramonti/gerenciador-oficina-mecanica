-- =====================================================
-- Migration: V024 - Create pagamentos table
-- Description: Creates table for payment management
-- Author: PitStop Team
-- Date: 2025-11-11
-- =====================================================

-- Create pagamentos table
CREATE TABLE pagamentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ordem_servico_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('DINHEIRO', 'PIX', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'BOLETO', 'TRANSFERENCIA', 'CHEQUE')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'PAGO', 'CANCELADO', 'ESTORNADO', 'VENCIDO')),
    valor DECIMAL(10,2) NOT NULL CHECK (valor > 0),
    parcelas INTEGER NOT NULL DEFAULT 1 CHECK (parcelas BETWEEN 1 AND 12),
    parcela_atual INTEGER NOT NULL DEFAULT 1 CHECK (parcela_atual >= 1),
    data_vencimento DATE,
    data_pagamento DATE,
    observacao TEXT,
    comprovante TEXT,
    nota_fiscal_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint (commented out for now as ordem_servico_id might not exist yet)
    -- Will be added when multi-tenant structure is fully implemented
    -- CONSTRAINT fk_pagamentos_ordem_servico FOREIGN KEY (ordem_servico_id) REFERENCES ordem_servico(id) ON DELETE CASCADE,

    -- Business rule: parcela_atual cannot exceed total parcelas
    CONSTRAINT chk_parcela_valida CHECK (parcela_atual <= parcelas),

    -- Business rule: data_pagamento required when status = PAGO
    CONSTRAINT chk_data_pagamento_obrigatoria CHECK (
        (status = 'PAGO' AND data_pagamento IS NOT NULL) OR
        (status != 'PAGO')
    )
);

-- Create indexes for better query performance
CREATE INDEX idx_pagamentos_os ON pagamentos(ordem_servico_id);
CREATE INDEX idx_pagamentos_status ON pagamentos(status);
CREATE INDEX idx_pagamentos_tipo ON pagamentos(tipo);
CREATE INDEX idx_pagamentos_vencimento ON pagamentos(data_vencimento);
CREATE INDEX idx_pagamentos_data_pagamento ON pagamentos(data_pagamento);
CREATE INDEX idx_pagamentos_created_at ON pagamentos(created_at);

-- Create index for composite queries (OS + status)
CREATE INDEX idx_pagamentos_os_status ON pagamentos(ordem_servico_id, status);

-- Add comment to table
COMMENT ON TABLE pagamentos IS 'Tabela de pagamentos vinculados a ordens de serviço. Suporta parcelamento e múltiplos tipos de pagamento.';

-- Add comments to columns
COMMENT ON COLUMN pagamentos.ordem_servico_id IS 'ID da ordem de serviço relacionada';
COMMENT ON COLUMN pagamentos.tipo IS 'Tipo de pagamento: DINHEIRO, PIX, CARTAO_CREDITO, CARTAO_DEBITO, BOLETO, TRANSFERENCIA, CHEQUE';
COMMENT ON COLUMN pagamentos.status IS 'Status do pagamento: PENDENTE, PAGO, CANCELADO, ESTORNADO, VENCIDO';
COMMENT ON COLUMN pagamentos.valor IS 'Valor do pagamento ou da parcela (se parcelado)';
COMMENT ON COLUMN pagamentos.parcelas IS 'Total de parcelas (1 para pagamento à vista)';
COMMENT ON COLUMN pagamentos.parcela_atual IS 'Número da parcela atual (1, 2, 3...)';
COMMENT ON COLUMN pagamentos.data_vencimento IS 'Data de vencimento do pagamento/parcela';
COMMENT ON COLUMN pagamentos.data_pagamento IS 'Data efetiva do pagamento (quando foi quitado)';
COMMENT ON COLUMN pagamentos.comprovante IS 'Comprovante de pagamento (path do arquivo ou base64)';
COMMENT ON COLUMN pagamentos.nota_fiscal_id IS 'ID da nota fiscal relacionada (se houver)';
