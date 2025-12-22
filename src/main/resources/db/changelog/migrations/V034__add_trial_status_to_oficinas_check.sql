-- Migration: Add TRIAL status to oficinas status check constraint
-- Description: Allow TRIAL status for new workshops with trial period
-- Author: PitStop Team
-- Date: 2025-12-21

-- Drop old constraint
ALTER TABLE oficinas DROP CONSTRAINT IF EXISTS oficinas_status_check;

-- Add new constraint with TRIAL status
ALTER TABLE oficinas ADD CONSTRAINT oficinas_status_check
    CHECK (status IN ('ATIVA', 'INATIVA', 'SUSPENSA', 'CANCELADA', 'TRIAL'));
