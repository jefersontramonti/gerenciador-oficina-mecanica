-- =====================================================
-- V056: Adicionar campos do modelo híbrido de cobrança de mão de obra
-- =====================================================
-- Permite escolher entre VALOR_FIXO (atual) ou POR_HORA (novo)
-- No modelo POR_HORA, o cliente aprova um limite máximo de horas

-- Tipo de cobrança de mão de obra (VALOR_FIXO = comportamento atual)
ALTER TABLE ordem_servico ADD COLUMN IF NOT EXISTS tipo_cobranca_mao_obra VARCHAR(20) DEFAULT 'VALOR_FIXO';

-- Campos para o modelo POR_HORA
ALTER TABLE ordem_servico ADD COLUMN IF NOT EXISTS tempo_estimado_horas DECIMAL(5, 2);
ALTER TABLE ordem_servico ADD COLUMN IF NOT EXISTS limite_horas_aprovado DECIMAL(5, 2);
ALTER TABLE ordem_servico ADD COLUMN IF NOT EXISTS horas_trabalhadas DECIMAL(5, 2);
ALTER TABLE ordem_servico ADD COLUMN IF NOT EXISTS valor_hora_snapshot DECIMAL(10, 2);

-- Constraint para tipo de cobrança
ALTER TABLE ordem_servico ADD CONSTRAINT chk_os_tipo_cobranca_mao_obra
    CHECK (tipo_cobranca_mao_obra IN ('VALOR_FIXO', 'POR_HORA'));

-- Constraint para range de horas (0.5 = 30min mínimo, 100h máximo)
ALTER TABLE ordem_servico ADD CONSTRAINT chk_os_horas_trabalhadas_range
    CHECK (horas_trabalhadas IS NULL OR (horas_trabalhadas >= 0.5 AND horas_trabalhadas <= 100));

-- Constraint para limite de horas aprovado
ALTER TABLE ordem_servico ADD CONSTRAINT chk_os_limite_horas_range
    CHECK (limite_horas_aprovado IS NULL OR (limite_horas_aprovado >= 0.5 AND limite_horas_aprovado <= 100));

-- Constraint para tempo estimado
ALTER TABLE ordem_servico ADD CONSTRAINT chk_os_tempo_estimado_range
    CHECK (tempo_estimado_horas IS NULL OR (tempo_estimado_horas >= 0.5 AND tempo_estimado_horas <= 100));

-- Índice para consultas por tipo de cobrança
CREATE INDEX IF NOT EXISTS idx_os_tipo_cobranca ON ordem_servico(tipo_cobranca_mao_obra);

-- Comentários
COMMENT ON COLUMN ordem_servico.tipo_cobranca_mao_obra IS 'Tipo de cobrança: VALOR_FIXO (valor definido no orçamento) ou POR_HORA (calculado na finalização)';
COMMENT ON COLUMN ordem_servico.tempo_estimado_horas IS 'Estimativa de horas para o serviço (modelo POR_HORA)';
COMMENT ON COLUMN ordem_servico.limite_horas_aprovado IS 'Máximo de horas que o cliente aprovou (modelo POR_HORA)';
COMMENT ON COLUMN ordem_servico.horas_trabalhadas IS 'Horas efetivamente trabalhadas, informadas na finalização (modelo POR_HORA)';
COMMENT ON COLUMN ordem_servico.valor_hora_snapshot IS 'Valor/hora da oficina no momento da criação da OS (snapshot para histórico)';
