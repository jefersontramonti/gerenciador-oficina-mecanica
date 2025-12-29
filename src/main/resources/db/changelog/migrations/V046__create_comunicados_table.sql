-- =====================================================
-- V046: Create Comunicados table for mass communication
-- Sistema de comunicação em massa para SUPER_ADMIN
-- =====================================================

-- Enum for tipo de comunicado
-- NOVIDADE: Novidades do sistema
-- MANUTENCAO: Avisos de manutenção
-- FINANCEIRO: Comunicados financeiros
-- ATUALIZACAO: Atualizações de funcionalidades
-- ALERTA: Alertas importantes
-- PROMOCAO: Promoções e ofertas
-- OUTRO: Outros comunicados

-- Enum for status do comunicado
-- RASCUNHO: Em edição
-- AGENDADO: Agendado para envio futuro
-- ENVIADO: Já enviado
-- CANCELADO: Cancelado

-- Enum for prioridade do comunicado
-- BAIXA: Informativo
-- NORMAL: Padrão
-- ALTA: Importante
-- URGENTE: Requer atenção imediata

-- Create comunicados table
CREATE TABLE IF NOT EXISTS comunicados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Conteúdo
    titulo VARCHAR(255) NOT NULL,
    resumo VARCHAR(500),
    conteudo TEXT NOT NULL,

    -- Classificação
    tipo VARCHAR(50) NOT NULL DEFAULT 'NOVIDADE',
    prioridade VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO',

    -- Autor
    autor_id UUID NOT NULL,
    autor_nome VARCHAR(255) NOT NULL,

    -- Segmentação (NULL = todas as oficinas)
    planos_alvo TEXT[], -- Array de códigos de planos (ECONOMICO, PROFISSIONAL, TURBINADO)
    oficinas_alvo UUID[], -- Array de IDs de oficinas específicas
    status_oficinas_alvo TEXT[], -- Array de status de oficinas (ATIVA, TRIAL, SUSPENSA)

    -- Agendamento
    data_agendamento TIMESTAMP WITH TIME ZONE,
    data_envio TIMESTAMP WITH TIME ZONE,

    -- Estatísticas
    total_destinatarios INTEGER DEFAULT 0,
    total_visualizacoes INTEGER DEFAULT 0,
    total_confirmacoes INTEGER DEFAULT 0,

    -- Configurações
    requer_confirmacao BOOLEAN DEFAULT FALSE,
    exibir_no_login BOOLEAN DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_comunicado_tipo CHECK (tipo IN ('NOVIDADE', 'MANUTENCAO', 'FINANCEIRO', 'ATUALIZACAO', 'ALERTA', 'PROMOCAO', 'OUTRO')),
    CONSTRAINT chk_comunicado_prioridade CHECK (prioridade IN ('BAIXA', 'NORMAL', 'ALTA', 'URGENTE')),
    CONSTRAINT chk_comunicado_status CHECK (status IN ('RASCUNHO', 'AGENDADO', 'ENVIADO', 'CANCELADO'))
);

-- Create comunicados_leitura table to track who read what
CREATE TABLE IF NOT EXISTS comunicados_leitura (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    comunicado_id UUID NOT NULL REFERENCES comunicados(id) ON DELETE CASCADE,
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,

    -- Status da leitura
    visualizado BOOLEAN DEFAULT FALSE,
    data_visualizacao TIMESTAMP WITH TIME ZONE,
    confirmado BOOLEAN DEFAULT FALSE,
    data_confirmacao TIMESTAMP WITH TIME ZONE,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint - uma leitura por oficina por comunicado
    CONSTRAINT uq_comunicado_oficina UNIQUE (comunicado_id, oficina_id)
);

-- Indexes for comunicados
CREATE INDEX IF NOT EXISTS idx_comunicados_status ON comunicados(status);
CREATE INDEX IF NOT EXISTS idx_comunicados_tipo ON comunicados(tipo);
CREATE INDEX IF NOT EXISTS idx_comunicados_prioridade ON comunicados(prioridade);
CREATE INDEX IF NOT EXISTS idx_comunicados_data_envio ON comunicados(data_envio);
CREATE INDEX IF NOT EXISTS idx_comunicados_data_agendamento ON comunicados(data_agendamento);
CREATE INDEX IF NOT EXISTS idx_comunicados_autor_id ON comunicados(autor_id);
CREATE INDEX IF NOT EXISTS idx_comunicados_created_at ON comunicados(created_at DESC);

-- Indexes for comunicados_leitura
CREATE INDEX IF NOT EXISTS idx_comunicados_leitura_comunicado ON comunicados_leitura(comunicado_id);
CREATE INDEX IF NOT EXISTS idx_comunicados_leitura_oficina ON comunicados_leitura(oficina_id);
CREATE INDEX IF NOT EXISTS idx_comunicados_leitura_usuario ON comunicados_leitura(usuario_id);
CREATE INDEX IF NOT EXISTS idx_comunicados_leitura_visualizado ON comunicados_leitura(visualizado);
CREATE INDEX IF NOT EXISTS idx_comunicados_leitura_confirmado ON comunicados_leitura(confirmado);

-- Comments
COMMENT ON TABLE comunicados IS 'Comunicados em massa enviados pelo SUPER_ADMIN para as oficinas';
COMMENT ON TABLE comunicados_leitura IS 'Registro de leitura/confirmação de comunicados por oficina';
COMMENT ON COLUMN comunicados.planos_alvo IS 'Códigos dos planos alvo. NULL significa todos os planos';
COMMENT ON COLUMN comunicados.oficinas_alvo IS 'IDs de oficinas específicas. NULL significa todas as oficinas';
COMMENT ON COLUMN comunicados.status_oficinas_alvo IS 'Status das oficinas alvo. NULL significa todos os status';
COMMENT ON COLUMN comunicados.requer_confirmacao IS 'Se true, exige que a oficina confirme ter lido';
COMMENT ON COLUMN comunicados.exibir_no_login IS 'Se true, exibe popup ao fazer login';
