-- =============================================
-- V079: Add agendamento reference to alertas_manutencao
-- =============================================
-- Permite que alertas sejam vinculados a agendamentos (além de planos)
-- Torna plano_id nullable para suportar alertas de agendamentos sem plano

-- Tornar plano_id nullable
ALTER TABLE alertas_manutencao
ALTER COLUMN plano_id DROP NOT NULL;

-- Adicionar coluna agendamento_id
ALTER TABLE alertas_manutencao
ADD COLUMN IF NOT EXISTS agendamento_id UUID;

-- Criar foreign key
ALTER TABLE alertas_manutencao
ADD CONSTRAINT fk_alertas_manutencao_agendamento
FOREIGN KEY (agendamento_id) REFERENCES agendamentos_manutencao(id)
ON DELETE SET NULL;

-- Criar índice para performance
CREATE INDEX IF NOT EXISTS idx_alertas_manutencao_agendamento
ON alertas_manutencao(agendamento_id);
