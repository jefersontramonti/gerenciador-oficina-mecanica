-- Habilitar WEBHOOK_NOTIFICATIONS para plano PROFISSIONAL
-- Anteriormente só estava disponível para TURBINADO

UPDATE feature_flags
SET habilitado_por_plano = '{"PROFISSIONAL": true, "TURBINADO": true}'
WHERE codigo = 'WEBHOOK_NOTIFICATIONS';
