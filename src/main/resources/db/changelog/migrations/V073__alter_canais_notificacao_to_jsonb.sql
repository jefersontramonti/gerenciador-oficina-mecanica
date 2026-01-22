-- =====================================================
-- V073: Alterar coluna canais_notificacao de array para jsonb
-- =====================================================
-- Corrige problema de deserialização com Jackson/Redis
-- A coluna varchar[] não é compatível com serialização JSON
-- =====================================================

-- Converter a coluna de varchar[] para jsonb
-- Primeiro, cria uma coluna temporária
ALTER TABLE planos_manutencao_preventiva
ADD COLUMN IF NOT EXISTS canais_notificacao_new jsonb;

-- Converte os dados existentes de array para JSON array
UPDATE planos_manutencao_preventiva
SET canais_notificacao_new = CASE
    WHEN canais_notificacao IS NULL THEN '["WHATSAPP", "EMAIL"]'::jsonb
    ELSE array_to_json(canais_notificacao)::jsonb
END
WHERE canais_notificacao_new IS NULL;

-- Remove a coluna antiga
ALTER TABLE planos_manutencao_preventiva DROP COLUMN IF EXISTS canais_notificacao;

-- Renomeia a nova coluna
ALTER TABLE planos_manutencao_preventiva RENAME COLUMN canais_notificacao_new TO canais_notificacao;

-- Define o valor default
ALTER TABLE planos_manutencao_preventiva
ALTER COLUMN canais_notificacao SET DEFAULT '["WHATSAPP", "EMAIL"]'::jsonb;
