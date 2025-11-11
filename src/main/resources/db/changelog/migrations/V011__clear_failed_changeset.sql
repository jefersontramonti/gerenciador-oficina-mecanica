-- ============================================================================
-- Clear failed changeset V009 from Liquibase tracking
-- This allows V009 to be re-executed with corrected UUIDs
-- ============================================================================

-- Remove the failed changeset from DATABASECHANGELOG
DELETE FROM databasechangelog
WHERE filename = 'db/changelog/migrations/V009__populate_test_data.sql';

-- Remove any lock that might exist
DELETE FROM databasechangeloglock WHERE id = 1;
INSERT INTO databasechangeloglock (id, locked) VALUES (1, false);
