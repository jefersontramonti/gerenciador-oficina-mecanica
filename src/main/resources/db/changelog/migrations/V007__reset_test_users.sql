-- ============================================================================
-- Reset test users with known password (senha123)
-- Hash: $2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lk3v8n7W4/zO
-- ============================================================================

-- Delete existing usuarios
DELETE FROM usuarios;

-- Insert test users with known password "senha123"
INSERT INTO usuarios (id, nome, email, senha, perfil, ativo, created_at, updated_at, ultimo_acesso) VALUES
('f47ac10b-58cc-4372-a567-0e02b2c3d479', 'Carlos Henrique Silva', 'gerente@pitstop.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lk3v8n7W4/zO', 'GERENTE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),
('e58ed763-928c-4155-bee9-fdbaaadc15f3', 'Maria Santos Oliveira', 'atendente@pitstop.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lk3v8n7W4/zO', 'ATENDENTE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),
('d364f5a2-5c1a-4c3e-8f9b-1e4d2c8a6b3c', 'João Pedro Almeida', 'mecanico@pitstop.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lk3v8n7W4/zO', 'MECANICO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL),
('c273e4d1-4b0a-3b2e-7e8a-0d3c1b7a5a2b', 'Roberto Costa Lima', 'mecanico2@pitstop.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lk3v8n7W4/zO', 'MECANICO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);
