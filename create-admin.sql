-- ============================================================================
-- Script para criar usuário ADMIN no PitStop
-- ============================================================================
-- Execute este script no PostgreSQL para criar o primeiro usuário ADMIN
--
-- Credenciais:
-- Email: admin@pitstop.com
-- Senha: admin123
-- ============================================================================

INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Administrador do Sistema',
    'admin@pitstop.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',  -- senha: admin123
    'ADMIN',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
)
ON CONFLICT (email) DO NOTHING;

-- Verificar se foi criado
SELECT id, nome, email, perfil, ativo
FROM usuarios
WHERE email = 'admin@pitstop.com';

-- ============================================================================
-- INSTRUÇÕES DE USO:
-- ============================================================================
-- 1. Conecte-se ao PostgreSQL via psql ou DBeaver/pgAdmin
-- 2. Execute este script no banco de dados 'pitstop'
-- 3. Faça login no sistema com:
--    Email: admin@pitstop.com
--    Senha: admin123
--
-- ⚠️ IMPORTANTE: Após o primeiro login, altere a senha em Configurações!
-- ============================================================================
