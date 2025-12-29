--liquibase formatted sql
--changeset pitstop:V045-create-comunicados-table

-- Tabela principal de comunicados
CREATE TABLE comunicados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo VARCHAR(255) NOT NULL,
    resumo VARCHAR(500),
    conteudo TEXT NOT NULL,
    tipo VARCHAR(50) NOT NULL DEFAULT 'NOVIDADE',
    prioridade VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(50) NOT NULL DEFAULT 'RASCUNHO',
    autor_id UUID NOT NULL,
    autor_nome VARCHAR(255) NOT NULL,
    planos_alvo TEXT[],
    oficinas_alvo UUID[],
    status_oficinas_alvo TEXT[],
    data_agendamento TIMESTAMP WITH TIME ZONE,
    data_envio TIMESTAMP WITH TIME ZONE,
    total_destinatarios INTEGER DEFAULT 0,
    total_visualizacoes INTEGER DEFAULT 0,
    total_confirmacoes INTEGER DEFAULT 0,
    requer_confirmacao BOOLEAN DEFAULT FALSE,
    exibir_no_login BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_comunicados_tipo CHECK (tipo IN ('NOVIDADE', 'ATUALIZACAO', 'MANUTENCAO', 'PROMOCAO', 'ALERTA', 'OUTRO')),
    CONSTRAINT chk_comunicados_prioridade CHECK (prioridade IN ('BAIXA', 'NORMAL', 'ALTA', 'URGENTE')),
    CONSTRAINT chk_comunicados_status CHECK (status IN ('RASCUNHO', 'AGENDADO', 'ENVIADO', 'CANCELADO'))
);

-- Tabela de leituras de comunicados por oficina
CREATE TABLE comunicados_leitura (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comunicado_id UUID NOT NULL REFERENCES comunicados(id) ON DELETE CASCADE,
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    visualizado BOOLEAN NOT NULL DEFAULT FALSE,
    data_visualizacao TIMESTAMP WITH TIME ZONE,
    confirmado BOOLEAN NOT NULL DEFAULT FALSE,
    data_confirmacao TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_comunicados_leitura_oficina UNIQUE (comunicado_id, oficina_id)
);

-- Índices para performance
CREATE INDEX idx_comunicados_status ON comunicados(status);
CREATE INDEX idx_comunicados_tipo ON comunicados(tipo);
CREATE INDEX idx_comunicados_data_envio ON comunicados(data_envio);
CREATE INDEX idx_comunicados_data_agendamento ON comunicados(data_agendamento);
CREATE INDEX idx_comunicados_leitura_comunicado ON comunicados_leitura(comunicado_id);
CREATE INDEX idx_comunicados_leitura_oficina ON comunicados_leitura(oficina_id);
CREATE INDEX idx_comunicados_leitura_visualizado ON comunicados_leitura(visualizado);

-- Comentários
COMMENT ON TABLE comunicados IS 'Comunicados em massa enviados pelo SUPER_ADMIN para as oficinas';
COMMENT ON TABLE comunicados_leitura IS 'Registro de leitura/confirmação de comunicados por oficina';
