-- ===========================================================================
-- V058: Create historico_status_os table for tracking OS status changes
-- ===========================================================================

-- Table to track all status changes of service orders
CREATE TABLE historico_status_os (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id),
    ordem_servico_id UUID NOT NULL REFERENCES ordem_servico(id) ON DELETE CASCADE,
    status_anterior VARCHAR(30),
    status_novo VARCHAR(30) NOT NULL,
    usuario_id UUID REFERENCES usuarios(id),
    usuario_nome VARCHAR(100),
    observacao TEXT,
    data_alteracao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_historico_status_anterior CHECK (
        status_anterior IS NULL OR status_anterior IN (
            'ORCAMENTO', 'APROVADO', 'EM_ANDAMENTO', 'AGUARDANDO_PECA',
            'FINALIZADO', 'ENTREGUE', 'CANCELADO'
        )
    ),
    CONSTRAINT chk_historico_status_novo CHECK (
        status_novo IN (
            'ORCAMENTO', 'APROVADO', 'EM_ANDAMENTO', 'AGUARDANDO_PECA',
            'FINALIZADO', 'ENTREGUE', 'CANCELADO'
        )
    )
);

-- Indexes for efficient querying
CREATE INDEX idx_historico_status_os_ordem_id ON historico_status_os(ordem_servico_id);
CREATE INDEX idx_historico_status_os_oficina_id ON historico_status_os(oficina_id);
CREATE INDEX idx_historico_status_os_data ON historico_status_os(data_alteracao DESC);
CREATE INDEX idx_historico_status_os_ordem_data ON historico_status_os(ordem_servico_id, data_alteracao);

-- Comment
COMMENT ON TABLE historico_status_os IS 'Histórico de mudanças de status das Ordens de Serviço';
COMMENT ON COLUMN historico_status_os.status_anterior IS 'Status antes da mudança (NULL se for criação)';
COMMENT ON COLUMN historico_status_os.status_novo IS 'Novo status após a mudança';
COMMENT ON COLUMN historico_status_os.usuario_nome IS 'Nome do usuário no momento da mudança (para histórico mesmo se usuário for deletado)';
COMMENT ON COLUMN historico_status_os.observacao IS 'Observação opcional sobre a mudança (ex: motivo de cancelamento)';
