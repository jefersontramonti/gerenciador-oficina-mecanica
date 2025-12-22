-- =====================================================
-- Migration: Add SUPER_ADMIN to perfil CHECK constraint
-- Purpose: Allow SUPER_ADMIN perfil in usuarios table
-- =====================================================

-- Drop the existing constraint
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_perfil_check;

-- Recreate with SUPER_ADMIN included
ALTER TABLE usuarios ADD CONSTRAINT usuarios_perfil_check
CHECK (perfil IN ('SUPER_ADMIN', 'ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO'));
