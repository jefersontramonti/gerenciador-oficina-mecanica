-- Migration: V041 - Add TESTE event to historico_notificacoes
-- Description: Adiciona evento TESTE para testes de configuracao
-- Author: PitStop Team
-- Date: 2025-12-24

-- Remove constraint antiga
ALTER TABLE historico_notificacoes
DROP CONSTRAINT historico_notificacoes_evento_check;

-- Adiciona nova constraint com TESTE
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
    'TESTE'
));
