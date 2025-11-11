--liquibase formatted sql

--changeset pitstop:022-create-password-reset-tokens-table
--comment: Creates password_reset_tokens table for password recovery functionality

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(36) NOT NULL UNIQUE,
    usuario_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_reset_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE
);

--changeset pitstop:022-create-password-reset-indexes
--comment: Creates indexes for password_reset_tokens table

CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_usuario ON password_reset_tokens(usuario_id);
CREATE INDEX idx_password_reset_expires ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_used ON password_reset_tokens(used);

--rollback DROP INDEX IF EXISTS idx_password_reset_used;
--rollback DROP INDEX IF EXISTS idx_password_reset_expires;
--rollback DROP INDEX IF EXISTS idx_password_reset_usuario;
--rollback DROP INDEX IF EXISTS idx_password_reset_token;
--rollback DROP TABLE IF EXISTS password_reset_tokens;
