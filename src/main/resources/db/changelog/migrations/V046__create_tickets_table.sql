--liquibase formatted sql
--changeset pitstop:V046-create-tickets-table

-- Tabela de tickets de suporte
CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero VARCHAR(20) NOT NULL UNIQUE,
    oficina_id UUID REFERENCES oficinas(id) ON DELETE SET NULL,
    usuario_id UUID,
    usuario_nome VARCHAR(255) NOT NULL,
    usuario_email VARCHAR(255) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    prioridade VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ABERTO',
    assunto VARCHAR(255) NOT NULL,
    descricao TEXT NOT NULL,
    anexos JSONB DEFAULT '[]',
    atribuido_a UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    sla_minutos INTEGER DEFAULT 1440,
    resposta_inicial_em TIMESTAMP,
    tempo_resposta_minutos INTEGER,
    abertura_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolvido_em TIMESTAMP,
    fechado_em TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tickets_tipo CHECK (tipo IN ('DUVIDA', 'PROBLEMA_TECNICO', 'BUG', 'SOLICITACAO_RECURSO', 'FINANCEIRO', 'COBRANCA', 'SUGESTAO', 'OUTRO')),
    CONSTRAINT chk_tickets_prioridade CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE')),
    CONSTRAINT chk_tickets_status CHECK (status IN ('ABERTO', 'EM_ANDAMENTO', 'AGUARDANDO_CLIENTE', 'AGUARDANDO_INTERNO', 'RESOLVIDO', 'FECHADO', 'CANCELADO'))
);

-- Tabela de mensagens de tickets
CREATE TABLE mensagens_ticket (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    autor_id UUID,
    autor_nome VARCHAR(255) NOT NULL,
    autor_tipo VARCHAR(20) NOT NULL,
    is_interno BOOLEAN NOT NULL DEFAULT FALSE,
    conteudo TEXT NOT NULL,
    anexos JSONB DEFAULT '[]',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mensagens_autor_tipo CHECK (autor_tipo IN ('CLIENTE', 'SUPORTE', 'SISTEMA'))
);

-- Índices para performance
CREATE INDEX idx_tickets_oficina_id ON tickets(oficina_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_prioridade ON tickets(prioridade);
CREATE INDEX idx_tickets_tipo ON tickets(tipo);
CREATE INDEX idx_tickets_atribuido_a ON tickets(atribuido_a);
CREATE INDEX idx_tickets_abertura_em ON tickets(abertura_em);
CREATE INDEX idx_tickets_numero ON tickets(numero);

CREATE INDEX idx_mensagens_ticket_ticket_id ON mensagens_ticket(ticket_id);
CREATE INDEX idx_mensagens_ticket_criado_em ON mensagens_ticket(criado_em);

-- Sequência para geração do número do ticket
CREATE SEQUENCE IF NOT EXISTS seq_ticket_numero START WITH 1;

-- Comentários
COMMENT ON TABLE tickets IS 'Tickets de suporte abertos pelas oficinas';
COMMENT ON TABLE mensagens_ticket IS 'Mensagens/respostas nos tickets de suporte';
