-- Migration: V035 - Make entidade_id nullable in audit_logs table
-- Description: Allow system-level audit logs (e.g., DAILY_METRICS) to have null entidade_id
-- Author: PitStop Team
-- Date: 2025-12-21

ALTER TABLE audit_logs ALTER COLUMN entidade_id DROP NOT NULL;

COMMENT ON COLUMN audit_logs.entidade_id IS 'ID of the audited entity. NULL for system-level audit logs.';
