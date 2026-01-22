-- =============================================
-- V080: Fix canal constraint to include TELEGRAM
-- =============================================
-- A constraint chk_canal_alerta não incluía TELEGRAM como valor válido

-- Remover a constraint antiga
ALTER TABLE alertas_manutencao
DROP CONSTRAINT IF EXISTS chk_canal_alerta;

-- Criar nova constraint incluindo TELEGRAM
ALTER TABLE alertas_manutencao
ADD CONSTRAINT chk_canal_alerta CHECK (canal IN ('WHATSAPP', 'EMAIL', 'SMS', 'PUSH', 'INTERNO', 'TELEGRAM'));
