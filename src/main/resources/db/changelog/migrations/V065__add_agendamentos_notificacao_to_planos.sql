--liquibase formatted sql
--changeset pitstop:V065 splitStatements:true

-- ============================================================================
-- AGENDAMENTOS DE NOTIFICAÇÃO PERSONALIZADOS
-- ============================================================================
-- Adiciona coluna para armazenar agendamentos de notificação personalizados
-- no plano de manutenção preventiva. Permite até 2 notificações com data/hora
-- específicas.
-- ============================================================================

-- Adiciona coluna JSONB para agendamentos de notificação
ALTER TABLE planos_manutencao_preventiva
ADD COLUMN IF NOT EXISTS agendamentos_notificacao JSONB DEFAULT '[]'::jsonb;

-- Comentário explicativo
COMMENT ON COLUMN planos_manutencao_preventiva.agendamentos_notificacao IS
'Agendamentos de notificação personalizados. Formato: [{"data": "2026-02-10", "hora": "12:00", "enviado": false}]. Máximo 2 agendamentos.';

-- Índice para buscar planos com notificações pendentes
CREATE INDEX IF NOT EXISTS idx_planos_agendamentos_notificacao
ON planos_manutencao_preventiva USING gin (agendamentos_notificacao)
WHERE agendamentos_notificacao IS NOT NULL AND agendamentos_notificacao != '[]'::jsonb;
