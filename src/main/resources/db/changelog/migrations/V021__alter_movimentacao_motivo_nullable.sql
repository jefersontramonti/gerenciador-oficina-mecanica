--liquibase formatted sql

--changeset pitstop:21-alter-movimentacao-motivo-nullable
--comment: Torna coluna motivo nullable e aumenta tamanho para 500 caracteres

-- Altera a coluna motivo para permitir NULL e aumentar tamanho
ALTER TABLE movimentacao_estoque
ALTER COLUMN motivo TYPE VARCHAR(500),
ALTER COLUMN motivo DROP NOT NULL;

--rollback ALTER TABLE movimentacao_estoque ALTER COLUMN motivo TYPE VARCHAR(100), ALTER COLUMN motivo SET NOT NULL;
