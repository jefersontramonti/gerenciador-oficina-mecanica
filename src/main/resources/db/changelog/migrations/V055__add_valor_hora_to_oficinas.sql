-- =====================================================
-- V055: Adicionar campo valor_hora na tabela oficinas
-- =====================================================
-- Este campo armazena o valor por hora de mão de obra
-- usado no modelo de cobrança POR_HORA das ordens de serviço

-- Adicionar coluna valor_hora
ALTER TABLE oficinas ADD COLUMN IF NOT EXISTS valor_hora DECIMAL(10, 2) DEFAULT 80.00;

-- Índice para consultas (ex: relatórios de oficinas por faixa de preço)
CREATE INDEX IF NOT EXISTS idx_oficinas_valor_hora ON oficinas(valor_hora) WHERE valor_hora IS NOT NULL;

-- Comentários
COMMENT ON COLUMN oficinas.valor_hora IS 'Valor por hora de mão de obra da oficina (usado no modelo de cobrança POR_HORA)';
