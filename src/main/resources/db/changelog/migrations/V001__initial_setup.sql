-- Liquibase Migration V001: Initial Setup
-- Description: Creates initial database structure for PitStop application
-- Author: PitStop Team
-- Date: 2025-10-16

-- This is the initial migration file
-- Future entity tables will be added in subsequent migrations
-- For now, this file serves to validate that Liquibase is properly configured

-- Create extension for UUID generation (PostgreSQL specific)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Migration placeholder - actual tables will be created in future migrations
-- when we implement the domain entities (Cliente, Veiculo, OrdemServico, etc.)
