-- Migration: Create ordem_servico table and sequence
-- Description: Service order management - core business entity
-- Author: PitStop Team
-- Date: 2025-11-01

-- Create sequence for sequential ordem_servico number
CREATE SEQUENCE ordem_servico_numero_seq START 1 INCREMENT 1;

CREATE TABLE ordem_servico (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero BIGINT NOT NULL UNIQUE DEFAULT nextval('ordem_servico_numero_seq'),
    veiculo_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ORCAMENTO',

    -- Dates
    data_abertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_previsao DATE,
    data_finalizacao TIMESTAMP,
    data_entrega TIMESTAMP,

    -- Descriptions
    problemas_relatados TEXT NOT NULL,
    diagnostico TEXT,
    observacoes TEXT,

    -- Financial values
    valor_mao_obra DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    valor_pecas DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    valor_total DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    desconto_percentual DECIMAL(5, 2) DEFAULT 0.00,
    desconto_valor DECIMAL(10, 2) DEFAULT 0.00,
    valor_final DECIMAL(10, 2) NOT NULL DEFAULT 0.00,

    -- Business rules
    aprovado_pelo_cliente BOOLEAN DEFAULT FALSE,

    -- Optimistic locking
    version INTEGER NOT NULL DEFAULT 0,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_ordem_servico_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ordem_servico_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT,

    -- Check constraints
    CONSTRAINT chk_ordem_servico_status CHECK (status IN (
        'ORCAMENTO',
        'APROVADO',
        'EM_ANDAMENTO',
        'AGUARDANDO_PECA',
        'FINALIZADO',
        'ENTREGUE',
        'CANCELADO'
    )),
    CONSTRAINT chk_ordem_servico_valores CHECK (
        valor_mao_obra >= 0 AND
        valor_pecas >= 0 AND
        valor_total >= 0 AND
        valor_final >= 0
    ),
    CONSTRAINT chk_ordem_servico_desconto_percentual CHECK (
        desconto_percentual >= 0 AND desconto_percentual <= 100
    ),
    CONSTRAINT chk_ordem_servico_desconto_valor CHECK (desconto_valor >= 0),
    CONSTRAINT chk_ordem_servico_datas CHECK (
        (data_finalizacao IS NULL OR data_finalizacao >= data_abertura) AND
        (data_entrega IS NULL OR data_entrega >= data_abertura) AND
        (data_previsao IS NULL OR data_previsao >= DATE(data_abertura))
    ),
    CONSTRAINT chk_ordem_servico_problemas_min_length CHECK (LENGTH(TRIM(problemas_relatados)) >= 10)
);

-- Performance indexes
CREATE INDEX idx_ordem_servico_numero ON ordem_servico(numero);
CREATE INDEX idx_ordem_servico_status ON ordem_servico(status);
CREATE INDEX idx_ordem_servico_veiculo_id ON ordem_servico(veiculo_id);
CREATE INDEX idx_ordem_servico_usuario_id ON ordem_servico(usuario_id);
CREATE INDEX idx_ordem_servico_data_abertura ON ordem_servico(data_abertura DESC);
CREATE INDEX idx_ordem_servico_status_data ON ordem_servico(status, data_abertura DESC);
CREATE INDEX idx_ordem_servico_created_at ON ordem_servico(created_at DESC);

-- Comments
COMMENT ON TABLE ordem_servico IS 'Ordens de serviço - núcleo do sistema de gestão da oficina';
COMMENT ON COLUMN ordem_servico.numero IS 'Número sequencial único da OS (gerado por sequence do PostgreSQL)';
COMMENT ON COLUMN ordem_servico.veiculo_id IS 'FK para veiculos - qual veículo está sendo atendido';
COMMENT ON COLUMN ordem_servico.usuario_id IS 'FK para usuarios - mecânico responsável pela OS';
COMMENT ON COLUMN ordem_servico.status IS 'Status atual da OS: ORCAMENTO → APROVADO → EM_ANDAMENTO → FINALIZADO → ENTREGUE (ou CANCELADO)';
COMMENT ON COLUMN ordem_servico.data_abertura IS 'Data/hora de criação da OS';
COMMENT ON COLUMN ordem_servico.data_previsao IS 'Data prevista para conclusão (informada ao cliente)';
COMMENT ON COLUMN ordem_servico.data_finalizacao IS 'Data/hora em que a OS foi finalizada (status = FINALIZADO)';
COMMENT ON COLUMN ordem_servico.data_entrega IS 'Data/hora em que o veículo foi entregue ao cliente (status = ENTREGUE)';
COMMENT ON COLUMN ordem_servico.problemas_relatados IS 'Descrição dos problemas relatados pelo cliente (mínimo 10 caracteres)';
COMMENT ON COLUMN ordem_servico.diagnostico IS 'Diagnóstico técnico realizado pelo mecânico';
COMMENT ON COLUMN ordem_servico.observacoes IS 'Observações gerais sobre a OS';
COMMENT ON COLUMN ordem_servico.valor_mao_obra IS 'Valor total da mão de obra';
COMMENT ON COLUMN ordem_servico.valor_pecas IS 'Valor total das peças (calculado da soma dos itens)';
COMMENT ON COLUMN ordem_servico.valor_total IS 'Valor total antes do desconto (mão de obra + peças)';
COMMENT ON COLUMN ordem_servico.desconto_percentual IS 'Desconto percentual aplicado (0-100%)';
COMMENT ON COLUMN ordem_servico.desconto_valor IS 'Desconto em valor absoluto';
COMMENT ON COLUMN ordem_servico.valor_final IS 'Valor final após desconto (valor que o cliente pagará)';
COMMENT ON COLUMN ordem_servico.aprovado_pelo_cliente IS 'Se o cliente aprovou o orçamento (obrigatório para mudar para EM_ANDAMENTO)';
COMMENT ON COLUMN ordem_servico.version IS 'Versão para controle de concorrência otimista (Optimistic Locking)';
COMMENT ON SEQUENCE ordem_servico_numero_seq IS 'Sequence para geração de números sequenciais de OS (1, 2, 3, ...)';
COMMENT ON CONSTRAINT fk_ordem_servico_veiculo ON ordem_servico IS 'FK com ON DELETE RESTRICT - não permite deletar veículo com OS ativa';
COMMENT ON CONSTRAINT fk_ordem_servico_usuario ON ordem_servico IS 'FK com ON DELETE RESTRICT - mecânico responsável pela OS';
COMMENT ON CONSTRAINT chk_ordem_servico_status ON ordem_servico IS 'Status deve ser um dos 7 valores válidos da máquina de estados';
COMMENT ON CONSTRAINT chk_ordem_servico_valores ON ordem_servico IS 'Valores financeiros devem ser não-negativos';
COMMENT ON CONSTRAINT chk_ordem_servico_desconto_percentual ON ordem_servico IS 'Desconto percentual entre 0 e 100%';
