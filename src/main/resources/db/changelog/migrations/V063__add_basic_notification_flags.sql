--liquibase formatted sql
--changeset pitstop:V063-add-basic-notification-flags

-- =============================================
-- FLAGS BASICAS DE NOTIFICACAO
-- Controla acesso aos canais de notificacao
-- =============================================

-- Notificacoes basicas por canal (disponiveis para planos superiores)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('WHATSAPP_NOTIFICATIONS', 'Notificacoes WhatsApp', 'Enviar notificacoes de OS e lembretes via WhatsApp', false, 'COMUNICACAO', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('TELEGRAM_NOTIFICATIONS', 'Notificacoes Telegram', 'Enviar notificacoes de OS e lembretes via Telegram', false, 'COMUNICACAO', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('EMAIL_NOTIFICATIONS', 'Notificacoes por Email', 'Enviar notificacoes de OS e lembretes por email', true, 'COMUNICACAO', '{}'),
('SMTP_CUSTOMIZADO', 'SMTP Customizado', 'Configurar servidor SMTP proprio da oficina', false, 'COMUNICACAO', '{"PROFISSIONAL": true, "TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

--rollback DELETE FROM feature_flags WHERE codigo IN ('WHATSAPP_NOTIFICATIONS', 'TELEGRAM_NOTIFICATIONS', 'EMAIL_NOTIFICATIONS', 'SMTP_CUSTOMIZADO');
