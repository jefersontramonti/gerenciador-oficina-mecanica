--liquibase formatted sql
--changeset pitstop:V082-align-feature-flags-with-marketing

-- =============================================
-- Alinhamento de Feature Flags com documento mercado.md
-- Data: 2026-01-25
-- =============================================

-- 1. Adicionar ECONOMICO às features que devem estar disponíveis no plano básico
-- (conforme documento mercado.md - Diferenciais do Econômico)

UPDATE feature_flags
SET habilitado_por_plano = '{"ECONOMICO": true, "PROFISSIONAL": true, "TURBINADO": true}'
WHERE codigo = 'LOGO_CUSTOMIZADA';

UPDATE feature_flags
SET habilitado_por_plano = '{"ECONOMICO": true, "PROFISSIONAL": true, "TURBINADO": true}'
WHERE codigo = 'FOTOS_DOCUMENTACAO';

UPDATE feature_flags
SET habilitado_por_plano = '{"ECONOMICO": true, "PROFISSIONAL": true, "TURBINADO": true}'
WHERE codigo = 'QR_CODE_VEICULO';

UPDATE feature_flags
SET habilitado_por_plano = '{"ECONOMICO": true, "PROFISSIONAL": true, "TURBINADO": true}'
WHERE codigo = 'CONTROLE_GARANTIA';

UPDATE feature_flags
SET habilitado_por_plano = '{"ECONOMICO": true, "PROFISSIONAL": true, "TURBINADO": true}'
WHERE codigo = 'CHECKLIST_VISTORIA';

-- 2. Configurar features que estão sem plano definido (conforme documento)

-- SMS_NOTIFICATIONS - Documento não menciona, manter TURBINADO only
UPDATE feature_flags
SET habilitado_por_plano = '{"TURBINADO": true}'
WHERE codigo = 'SMS_NOTIFICATIONS'
AND (habilitado_por_plano IS NULL OR habilitado_por_plano = '{}');

-- DASHBOARD_AVANCADO - Documento menciona no Turbinado (Premium Features)
UPDATE feature_flags
SET habilitado_por_plano = '{"TURBINADO": true}'
WHERE codigo = 'DASHBOARD_AVANCADO'
AND (habilitado_por_plano IS NULL OR habilitado_por_plano = '{}');

-- MULTI_FILIAL - Documento menciona no Turbinado (Premium Features)
UPDATE feature_flags
SET habilitado_por_plano = '{"TURBINADO": true}'
WHERE codigo = 'MULTI_FILIAL'
AND (habilitado_por_plano IS NULL OR habilitado_por_plano = '{}');

-- RELATORIOS_PERSONALIZADOS - Documento menciona no Turbinado (Premium Features)
UPDATE feature_flags
SET habilitado_por_plano = '{"TURBINADO": true}'
WHERE codigo = 'RELATORIOS_PERSONALIZADOS'
AND (habilitado_por_plano IS NULL OR habilitado_por_plano = '{}');

-- API_EXTERNA - Mesmo que API_PUBLICA, TURBINADO only
UPDATE feature_flags
SET habilitado_por_plano = '{"TURBINADO": true}'
WHERE codigo = 'API_EXTERNA'
AND (habilitado_por_plano IS NULL OR habilitado_por_plano = '{}');

-- INTEGRACAO_PAGAMENTOS - Feature genérica, disponível para PROFISSIONAL+
UPDATE feature_flags
SET habilitado_por_plano = '{"PROFISSIONAL": true, "TURBINADO": true}'
WHERE codigo = 'INTEGRACAO_PAGAMENTOS'
AND (habilitado_por_plano IS NULL OR habilitado_por_plano = '{}');

--rollback UPDATE feature_flags SET habilitado_por_plano = '{"PROFISSIONAL": true, "TURBINADO": true}' WHERE codigo IN ('LOGO_CUSTOMIZADA', 'FOTOS_DOCUMENTACAO', 'QR_CODE_VEICULO', 'CONTROLE_GARANTIA', 'CHECKLIST_VISTORIA');
