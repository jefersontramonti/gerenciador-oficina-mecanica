-- =============================================
-- V045: Create tickets and mensagens_ticket tables
-- Sistema de Suporte para SUPER_ADMIN
-- =============================================

-- Tickets table
CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero VARCHAR(20) NOT NULL UNIQUE,

    -- Relacionamento com oficina
    oficina_id UUID REFERENCES oficinas(id),

    -- Dados do solicitante
    usuario_id UUID,
    usuario_nome VARCHAR(255) NOT NULL,
    usuario_email VARCHAR(255) NOT NULL,

    -- Classificação
    tipo VARCHAR(50) NOT NULL CHECK (tipo IN ('TECNICO', 'FINANCEIRO', 'COMERCIAL', 'SUGESTAO', 'OUTRO')),
    prioridade VARCHAR(20) NOT NULL CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('ABERTO', 'EM_ANDAMENTO', 'AGUARDANDO_CLIENTE', 'AGUARDANDO_INTERNO', 'RESOLVIDO', 'FECHADO')),

    -- Conteúdo
    assunto VARCHAR(255) NOT NULL,
    descricao TEXT NOT NULL,

    -- Anexos (JSON array de URLs)
    anexos JSONB DEFAULT '[]',

    -- Atribuição e SLA
    atribuido_a UUID REFERENCES usuarios(id),
    sla_minutos INTEGER DEFAULT 1440, -- 24 horas padrão
    resposta_inicial_em TIMESTAMP WITH TIME ZONE,
    tempo_resposta_minutos INTEGER,

    -- Datas
    abertura_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolvido_em TIMESTAMP WITH TIME ZONE,
    fechado_em TIMESTAMP WITH TIME ZONE,

    -- Auditoria
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Mensagens do ticket
CREATE TABLE IF NOT EXISTS mensagens_ticket (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,

    -- Autor da mensagem
    autor_id UUID,
    autor_nome VARCHAR(255) NOT NULL,
    autor_tipo VARCHAR(20) NOT NULL CHECK (autor_tipo IN ('CLIENTE', 'SUPORTE', 'SISTEMA')),

    -- Tipo de mensagem
    is_interno BOOLEAN NOT NULL DEFAULT FALSE, -- Se true, nota interna (não visível para cliente)

    -- Conteúdo
    conteudo TEXT NOT NULL,

    -- Anexos (JSON array de URLs)
    anexos JSONB DEFAULT '[]',

    -- Data
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para tickets
CREATE INDEX IF NOT EXISTS idx_tickets_oficina_id ON tickets(oficina_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_prioridade ON tickets(prioridade);
CREATE INDEX IF NOT EXISTS idx_tickets_tipo ON tickets(tipo);
CREATE INDEX IF NOT EXISTS idx_tickets_atribuido_a ON tickets(atribuido_a);
CREATE INDEX IF NOT EXISTS idx_tickets_abertura_em ON tickets(abertura_em DESC);
CREATE INDEX IF NOT EXISTS idx_tickets_numero ON tickets(numero);

-- Índices para mensagens
CREATE INDEX IF NOT EXISTS idx_mensagens_ticket_ticket_id ON mensagens_ticket(ticket_id);
CREATE INDEX IF NOT EXISTS idx_mensagens_ticket_criado_em ON mensagens_ticket(criado_em);

-- Sequence para numeração de tickets
CREATE SEQUENCE IF NOT EXISTS ticket_numero_seq START WITH 1 INCREMENT BY 1;

-- Comentários
COMMENT ON TABLE tickets IS 'Tickets de suporte do sistema SaaS';
COMMENT ON TABLE mensagens_ticket IS 'Mensagens/respostas dos tickets de suporte';
COMMENT ON COLUMN tickets.numero IS 'Número único do ticket no formato TKT-YYYY-NNNNN';
COMMENT ON COLUMN tickets.sla_minutos IS 'SLA em minutos para primeira resposta';
COMMENT ON COLUMN mensagens_ticket.is_interno IS 'Se true, é uma nota interna não visível para o cliente';
