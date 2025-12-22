-- Script de inicialização para criar database do Evolution API
-- Este script roda automaticamente quando o container PostgreSQL sobe pela primeira vez

-- Criar database para Evolution API (se não existir)
SELECT 'CREATE DATABASE evolution'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'evolution')\gexec

-- Conectar ao database evolution e configurar
\c evolution

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE evolution TO pitstop;
