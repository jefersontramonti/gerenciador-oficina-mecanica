-- Migration: V038 - Create historico_notificacoes table
-- Description: Historico de notificacoes enviadas para auditoria e metricas
-- Author: PitStop Team
-- Date: 2025-12-23

CREATE TABLE historico_notificacoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Oficina que enviou
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,

    -- ===== IDENTIFICACAO =====
    evento VARCHAR(50) NOT NULL,
    -- Evento que disparou: OS_CRIADA, OS_FINALIZADA, etc
    tipo_notificacao VARCHAR(20) NOT NULL,
    -- Canal usado: EMAIL, WHATSAPP, SMS, TELEGRAM

    -- ===== DESTINATARIO =====
    destinatario VARCHAR(200) NOT NULL,
    -- Email ou telefone do destinatario
    nome_destinatario VARCHAR(200),
    -- Nome do destinatario (para referencia)

    -- ===== CONTEUDO =====
    assunto VARCHAR(500),
    -- Assunto (usado em email)
    mensagem TEXT NOT NULL,
    -- Corpo/mensagem da notificacao
    variaveis TEXT,
    -- JSON com variaveis usadas no template

    -- ===== RASTREAMENTO =====
    template_id UUID,
    -- ID do template usado (se aplicavel)
    ordem_servico_id UUID REFERENCES ordem_servico(id) ON DELETE SET NULL,
    -- ID da OS relacionada (se aplicavel)
    cliente_id UUID REFERENCES clientes(id) ON DELETE SET NULL,
    -- ID do cliente relacionado
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    -- Usuario que disparou (NULL = automatico)

    -- ===== STATUS E TENTATIVAS =====
    status VARCHAR(20) NOT NULL,
    -- PENDENTE, ENVIADO, ENTREGUE, LIDO, FALHA, CANCELADO, AGENDADO
    tentativas INTEGER NOT NULL DEFAULT 0,
    -- Numero de tentativas de envio
    data_envio TIMESTAMP,
    -- Data/hora do envio (ou tentativa)
    data_entrega TIMESTAMP,
    -- Data/hora de confirmacao de entrega
    data_leitura TIMESTAMP,
    -- Data/hora de leitura (WhatsApp read receipt)
    data_agendada TIMESTAMP,
    -- Data/hora agendada para envio (se delay configurado)

    -- ===== RESPOSTA DA API =====
    id_externo VARCHAR(200),
    -- Message ID retornado pela API externa
    erro_mensagem TEXT,
    -- Mensagem de erro (se falhou)
    erro_codigo VARCHAR(50),
    -- Codigo de erro da API
    resposta_api TEXT,
    -- JSON com resposta completa da API

    -- ===== CUSTOS =====
    custo DECIMAL(10, 4),
    -- Custo do envio (SMS, WhatsApp pago, etc)
    moeda_custo VARCHAR(3) DEFAULT 'BRL',
    -- Moeda do custo

    -- ===== METADADOS =====
    ip_origem VARCHAR(50),
    -- IP de origem da requisicao (se manual)
    user_agent VARCHAR(500),
    -- User Agent (se via API)

    -- ===== AUDITORIA =====
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT historico_notificacoes_evento_check
        CHECK (evento IN (
            'OS_CRIADA',
            'OS_AGUARDANDO_APROVACAO',
            'OS_APROVADA',
            'OS_EM_ANDAMENTO',
            'OS_AGUARDANDO_PECA',
            'OS_FINALIZADA',
            'OS_ENTREGUE',
            'PAGAMENTO_PENDENTE',
            'PAGAMENTO_CONFIRMADO',
            'LEMBRETE_RETIRADA',
            'LEMBRETE_REVISAO'
        )),

    CONSTRAINT historico_notificacoes_tipo_notificacao_check
        CHECK (tipo_notificacao IN ('EMAIL', 'WHATSAPP', 'SMS', 'TELEGRAM')),

    CONSTRAINT historico_notificacoes_status_check
        CHECK (status IN ('PENDENTE', 'ENVIADO', 'ENTREGUE', 'LIDO', 'FALHA', 'CANCELADO', 'AGENDADO'))
);

-- Indices para performance
CREATE INDEX idx_historico_notif_oficina ON historico_notificacoes(oficina_id);
CREATE INDEX idx_historico_notif_destinatario ON historico_notificacoes(destinatario);
CREATE INDEX idx_historico_notif_evento ON historico_notificacoes(evento);
CREATE INDEX idx_historico_notif_status ON historico_notificacoes(status);
CREATE INDEX idx_historico_notif_data ON historico_notificacoes(data_envio);
CREATE INDEX idx_historico_notif_os ON historico_notificacoes(ordem_servico_id)
    WHERE ordem_servico_id IS NOT NULL;

-- Indice composto para consultas frequentes
CREATE INDEX idx_historico_notif_oficina_data ON historico_notificacoes(oficina_id, data_envio DESC);
CREATE INDEX idx_historico_notif_oficina_status ON historico_notificacoes(oficina_id, status);

-- Indice para busca de pendentes para reenvio
CREATE INDEX idx_historico_notif_pendentes ON historico_notificacoes(oficina_id, status, tentativas)
    WHERE status IN ('PENDENTE', 'FALHA', 'AGENDADO');

-- Comentarios
COMMENT ON TABLE historico_notificacoes IS 'Historico de notificacoes enviadas para auditoria e metricas';
COMMENT ON COLUMN historico_notificacoes.oficina_id IS 'ID da oficina que enviou a notificacao';
COMMENT ON COLUMN historico_notificacoes.evento IS 'Evento de negocio que disparou a notificacao';
COMMENT ON COLUMN historico_notificacoes.tipo_notificacao IS 'Canal usado (EMAIL, WHATSAPP, SMS, TELEGRAM)';
COMMENT ON COLUMN historico_notificacoes.destinatario IS 'Email ou telefone do destinatario';
COMMENT ON COLUMN historico_notificacoes.mensagem IS 'Corpo da mensagem enviada';
COMMENT ON COLUMN historico_notificacoes.variaveis IS 'JSON com variaveis usadas no template';
COMMENT ON COLUMN historico_notificacoes.status IS 'Status atual: PENDENTE, ENVIADO, ENTREGUE, LIDO, FALHA, CANCELADO, AGENDADO';
COMMENT ON COLUMN historico_notificacoes.tentativas IS 'Numero de tentativas de envio realizadas';
COMMENT ON COLUMN historico_notificacoes.id_externo IS 'ID retornado pela API externa para rastreamento';
COMMENT ON COLUMN historico_notificacoes.usuario_id IS 'Usuario que disparou manualmente. NULL = disparado automaticamente';
