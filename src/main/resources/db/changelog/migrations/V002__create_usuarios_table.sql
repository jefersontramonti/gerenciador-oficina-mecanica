-- Liquibase Migration V002: Create usuarios table
-- Description: Creates usuarios table with RBAC support and seeds admin user
-- Author: PitStop Team
-- Date: 2025-10-16

-- Create usuarios table
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL CHECK (perfil IN ('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_acesso TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_ativo ON usuarios(ativo);
CREATE INDEX idx_usuarios_perfil ON usuarios(perfil);

-- Seed admin user
-- Email: admin@pitstop.com
-- Senha: admin123 (BCrypt 12 rounds hash)
INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    'Administrador',
    'admin@pitstop.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIq.H9DQO6',
    'ADMIN',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Add comment to table
COMMENT ON TABLE usuarios IS 'Tabela de usuários do sistema com autenticação e RBAC';
COMMENT ON COLUMN usuarios.perfil IS 'Perfil RBAC: ADMIN (completo), GERENTE (relatórios), ATENDENTE (CRUD), MECANICO (apenas suas OS)';
COMMENT ON COLUMN usuarios.ativo IS 'Flag de soft delete - false desativa o usuário sem remover dados';
COMMENT ON COLUMN usuarios.ultimo_acesso IS 'Timestamp do último login bem-sucedido';
