-- =====================================================
-- V060: Additional Performance Indexes
-- Date: 2026-01-11
-- Description: Add supplementary indexes for performance optimization
-- Note: Many base indexes already exist in V005 and V028
-- =====================================================

-- Index for customers by name (search/autocomplete)
-- Uses varchar_pattern_ops for LIKE 'prefix%' queries
CREATE INDEX IF NOT EXISTS idx_clientes_nome_oficina
ON clientes (oficina_id, UPPER(nome) varchar_pattern_ops);

-- Index for inventory movements by part and date (part history view)
CREATE INDEX IF NOT EXISTS idx_movimentacao_peca_data
ON movimentacao_estoque (peca_id, data_movimentacao DESC);

-- Index for payments by order (financial summary on OS detail page)
CREATE INDEX IF NOT EXISTS idx_pagamentos_ordem_servico
ON pagamentos (ordem_servico_id, status);

-- Index for users by workshop and active status (user listing)
CREATE INDEX IF NOT EXISTS idx_usuarios_oficina_ativo
ON usuarios (oficina_id, ativo);

-- Index for service order items by type (for financial calculations)
CREATE INDEX IF NOT EXISTS idx_item_os_tipo
ON item_os (ordem_servico_id, tipo);

-- Composite index for OS with status and veiculo (vehicle detail page)
CREATE INDEX IF NOT EXISTS idx_ordem_servico_veiculo_status
ON ordem_servico (veiculo_id, status);
