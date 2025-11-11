-- ============================================================================
-- Fix passwords that were saved as plaintext due to mapper bug
-- Delete users with plaintext passwords (where senha does not start with $2a$)
-- ============================================================================

-- Delete users that have plaintext passwords (not BCrypt hashed)
-- This is safer than trying to fix corrupted passwords
DELETE FROM usuarios
WHERE senha NOT LIKE '$2a$%'
  AND email NOT IN ('admin@pitstop.com', 'gerente@pitstop.com', 'atendente@pitstop.com', 'mecanico@pitstop.com', 'mecanico2@pitstop.com');

-- Note: Users with valid BCrypt passwords (starting with $2a$) are kept
-- Users deleted by this migration need to be recreated via frontend or API
