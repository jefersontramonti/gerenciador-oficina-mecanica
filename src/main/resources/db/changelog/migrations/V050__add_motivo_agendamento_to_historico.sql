-- V050: Add motivo_agendamento column to historico_notificacoes
-- This column stores a clear explanation of why a notification was scheduled

ALTER TABLE historico_notificacoes
ADD COLUMN IF NOT EXISTS motivo_agendamento TEXT;

COMMENT ON COLUMN historico_notificacoes.motivo_agendamento IS 'Mensagem explicativa do motivo do agendamento';
