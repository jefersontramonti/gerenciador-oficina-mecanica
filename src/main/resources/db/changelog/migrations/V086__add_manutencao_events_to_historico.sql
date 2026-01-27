-- Migration: V086 - Add maintenance events to historico_notificacoes
-- Description: Adiciona eventos de manutenção preventiva ao CHECK constraint
-- Author: PitStop Team
-- Date: 2026-01-27

-- Remove constraint antiga
ALTER TABLE historico_notificacoes
DROP CONSTRAINT IF EXISTS historico_notificacoes_evento_check;

-- Adiciona nova constraint com eventos de manutenção preventiva
ALTER TABLE historico_notificacoes
ADD CONSTRAINT historico_notificacoes_evento_check
CHECK (evento IN (
    'OS_CRIADA',
    'OS_AGUARDANDO_APROVACAO',
    'OS_APROVADA',
    'OS_EM_ANDAMENTO',
    'OS_AGUARDANDO_PECA',
    'OS_FINALIZADA',
    'OS_ENTREGUE',
    'PAGAMENTO_PENDENTE',
    'PAGAMENTO_CONFIRMADO',
    'LEMBRETE_RETIRADA',
    'LEMBRETE_REVISAO',
    'TESTE',
    'MANUTENCAO_PROXIMA',
    'MANUTENCAO_VENCIDA',
    'LEMBRETE_AGENDAMENTO',
    'CONFIRMACAO_AGENDAMENTO'
));
