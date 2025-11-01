-- ============================================================================
-- Fix user passwords with correct BCrypt hash for "senha123"
-- Hash generated: $2a$12$l9cZpZkOlqkJZovKTzxFRObyqsHnWPHVK/xGmI0OPprqSCDCQUCf6
-- ============================================================================

UPDATE usuarios
SET senha = '$2a$12$l9cZpZkOlqkJZovKTzxFRObyqsHnWPHVK/xGmI0OPprqSCDCQUCf6'
WHERE email IN ('gerente@pitstop.com', 'atendente@pitstop.com', 'mecanico@pitstop.com', 'mecanico2@pitstop.com');
