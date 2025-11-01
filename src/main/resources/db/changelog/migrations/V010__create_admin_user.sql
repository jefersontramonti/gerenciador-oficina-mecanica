-- ============================================================================
-- Create admin user with password "admin123"
-- Hash generated: $2a$12$8ZqY7Y0p9L7vL3P0MxLcWOaH8R7J9KqF5N6M4X1W2T3S4V5Y6Z7A8
-- Generated with: BCryptPasswordEncoder (strength 12)
-- ============================================================================

-- Delete any existing admin user first (in case it was created by seed endpoint)
DELETE FROM usuarios WHERE email = 'admin@pitstop.com';

-- Insert admin user
INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
('a0000000-0000-0000-0000-000000000001', 'Administrador do Sistema', 'admin@pitstop.com', '$2a$12$l9cZpZkOlqkJZovKTzxFRObyqsHnWPHVK/xGmI0OPprqSCDCQUCf6', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);
