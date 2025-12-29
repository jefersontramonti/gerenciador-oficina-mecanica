-- =====================================================
-- Migration: Fix oficinas contact and address formats
-- Purpose: Update telefone_celular and endereco_cep to match validation patterns
-- =====================================================

-- Fix telefone_celular format: (XX) XXXXX-XXXX
-- Update existing data that doesn't match the pattern

-- First, fix the test oficina with raw number '1133334444' -> '(11) 33334-4444'
UPDATE oficinas
SET telefone_celular = '(11) 33334-4444'
WHERE telefone_celular = '1133334444';

-- Fix telefone_fixo format: (XX) XXXX-XXXX for 10-digit numbers
UPDATE oficinas
SET telefone_fixo = CONCAT('(', SUBSTRING(telefone_fixo, 1, 2), ') ', SUBSTRING(telefone_fixo, 3, 4), '-', SUBSTRING(telefone_fixo, 7, 4))
WHERE telefone_fixo IS NOT NULL
  AND LENGTH(REGEXP_REPLACE(telefone_fixo, '[^0-9]', '', 'g')) = 10
  AND telefone_fixo NOT LIKE '(%';

-- Fix endereco_cep format: 00000-000 (oficinas table)
UPDATE oficinas
SET endereco_cep = CONCAT(SUBSTRING(endereco_cep, 1, 5), '-', SUBSTRING(endereco_cep, 6, 3))
WHERE endereco_cep IS NOT NULL
  AND LENGTH(endereco_cep) = 8
  AND endereco_cep NOT LIKE '%-%';

-- Generic fix for telefone_celular that's just digits (11 digits = mobile)
UPDATE oficinas
SET telefone_celular = CONCAT('(', SUBSTRING(telefone_celular, 1, 2), ') ', SUBSTRING(telefone_celular, 3, 5), '-', SUBSTRING(telefone_celular, 8, 4))
WHERE telefone_celular IS NOT NULL
  AND LENGTH(REGEXP_REPLACE(telefone_celular, '[^0-9]', '', 'g')) = 11
  AND telefone_celular NOT LIKE '(%';

-- Fix clientes table (different column names: cep, celular, telefone)
UPDATE clientes
SET cep = CONCAT(SUBSTRING(cep, 1, 5), '-', SUBSTRING(cep, 6, 3))
WHERE cep IS NOT NULL
  AND LENGTH(cep) = 8
  AND cep NOT LIKE '%-%';

UPDATE clientes
SET celular = CONCAT('(', SUBSTRING(celular, 1, 2), ') ', SUBSTRING(celular, 3, 5), '-', SUBSTRING(celular, 8, 4))
WHERE celular IS NOT NULL
  AND LENGTH(REGEXP_REPLACE(celular, '[^0-9]', '', 'g')) = 11
  AND celular NOT LIKE '(%';

UPDATE clientes
SET telefone = CONCAT('(', SUBSTRING(telefone, 1, 2), ') ', SUBSTRING(telefone, 3, 4), '-', SUBSTRING(telefone, 7, 4))
WHERE telefone IS NOT NULL
  AND LENGTH(REGEXP_REPLACE(telefone, '[^0-9]', '', 'g')) = 10
  AND telefone NOT LIKE '(%';
