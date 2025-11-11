-- ============================================================================
-- Fix user passwords with CORRECT BCrypt hashes
-- Generated via /api/debug/hash endpoint with strength=12
-- ============================================================================

-- Update senha123 for all test users
-- Hash: $2a$12$yqFHUt2g9jVJWgmt47vN0.BVO4wIdXf5BXGzY4PT/q4NsNrHvY4NW
UPDATE usuarios
SET senha = '$2a$12$yqFHUt2g9jVJWgmt47vN0.BVO4wIdXf5BXGzY4PT/q4NsNrHvY4NW',
    updated_at = CURRENT_TIMESTAMP
WHERE email IN ('gerente@pitstop.com', 'atendente@pitstop.com', 'mecanico@pitstop.com', 'mecanico2@pitstop.com');

-- Update admin123 for admin user
-- Hash: $2a$12$Yhg1CYRCrTWtIeXwOJwVz.I.NhaZ7aInq9teW.ipY6IusG9Na.lTW
UPDATE usuarios
SET senha = '$2a$12$Yhg1CYRCrTWtIeXwOJwVz.I.NhaZ7aInq9teW.ipY6IusG9Na.lTW',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@pitstop.com';
