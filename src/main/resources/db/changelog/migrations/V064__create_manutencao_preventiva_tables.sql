--liquibase formatted sql
--changeset pitstop:V064 splitStatements:true

-- ============================================================================
-- MÓDULO DE MANUTENÇÃO PREVENTIVA - PitStop
-- ============================================================================
-- Tabelas para gestão de planos de manutenção preventiva, templates,
-- histórico de execuções, alertas automáticos e agendamentos.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. TEMPLATES DE MANUTENÇÃO
-- Templates pré-configurados reutilizáveis (podem ser globais ou por oficina)
-- ----------------------------------------------------------------------------
CREATE TABLE templates_manutencao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID REFERENCES oficinas(id) ON DELETE CASCADE,  -- NULL = template global
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    tipo_manutencao VARCHAR(50) NOT NULL,  -- Troca de óleo, Revisão, Alinhamento, etc.

    -- Critérios padrão
    intervalo_dias INTEGER,           -- Intervalo em dias (ex: 180 = 6 meses)
    intervalo_km INTEGER,             -- Intervalo em km (ex: 10000)
    criterio VARCHAR(20) NOT NULL DEFAULT 'AMBOS',  -- TEMPO, KM, AMBOS

    -- Configurações de alerta
    antecedencia_dias INTEGER DEFAULT 15,    -- Dias antes para alertar
    antecedencia_km INTEGER DEFAULT 1000,    -- KM antes para alertar

    -- Checklist e peças sugeridas (JSONB)
    checklist JSONB DEFAULT '[]'::jsonb,       -- [{"item": "Verificar filtro", "obrigatorio": true}]
    pecas_sugeridas JSONB DEFAULT '[]'::jsonb, -- [{"pecaId": "uuid", "quantidade": 1}]

    -- Estimativas
    valor_estimado DECIMAL(10,2),
    tempo_estimado_minutos INTEGER,

    -- Controle
    ativo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_criterio_template CHECK (criterio IN ('TEMPO', 'KM', 'AMBOS')),
    CONSTRAINT chk_intervalo_template CHECK (intervalo_dias > 0 OR intervalo_km > 0)
);

-- Índices para templates
CREATE INDEX idx_templates_manutencao_oficina ON templates_manutencao(oficina_id) WHERE oficina_id IS NOT NULL;
CREATE INDEX idx_templates_manutencao_ativo ON templates_manutencao(ativo) WHERE ativo = TRUE;
CREATE INDEX idx_templates_manutencao_tipo ON templates_manutencao(tipo_manutencao);

-- ----------------------------------------------------------------------------
-- 2. PLANOS DE MANUTENÇÃO PREVENTIVA
-- Planos individuais vinculados a veículos específicos
-- ----------------------------------------------------------------------------
CREATE TABLE planos_manutencao_preventiva (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    veiculo_id UUID NOT NULL REFERENCES veiculos(id) ON DELETE CASCADE,
    template_id UUID REFERENCES templates_manutencao(id) ON DELETE SET NULL,

    -- Identificação
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    tipo_manutencao VARCHAR(50) NOT NULL,

    -- Critérios de manutenção
    criterio VARCHAR(20) NOT NULL DEFAULT 'AMBOS',  -- TEMPO, KM, AMBOS
    intervalo_dias INTEGER,                          -- Periodicidade em dias
    intervalo_km INTEGER,                            -- Periodicidade em km

    -- Configurações de alerta
    antecedencia_dias INTEGER DEFAULT 15,
    antecedencia_km INTEGER DEFAULT 1000,
    canais_notificacao VARCHAR(100)[] DEFAULT ARRAY['WHATSAPP', 'EMAIL'],

    -- Última execução
    ultima_execucao_data DATE,
    ultima_execucao_km INTEGER,
    ultima_ordem_servico_id UUID REFERENCES ordem_servico(id) ON DELETE SET NULL,

    -- Próxima previsão (calculada automaticamente)
    proxima_previsao_data DATE,
    proxima_previsao_km INTEGER,

    -- Status do plano
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',  -- ATIVO, PAUSADO, CONCLUIDO, VENCIDO
    motivo_pausa TEXT,

    -- Controle de alertas
    ultimo_alerta_enviado_em TIMESTAMP,
    proxima_verificacao_alerta TIMESTAMP,

    -- Checklist personalizado (pode ser diferente do template)
    checklist JSONB DEFAULT '[]'::jsonb,
    pecas_sugeridas JSONB DEFAULT '[]'::jsonb,

    -- Estimativas
    valor_estimado DECIMAL(10,2),

    -- Controle
    ativo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES usuarios(id),

    CONSTRAINT chk_criterio_plano CHECK (criterio IN ('TEMPO', 'KM', 'AMBOS')),
    CONSTRAINT chk_status_plano CHECK (status IN ('ATIVO', 'PAUSADO', 'CONCLUIDO', 'VENCIDO')),
    CONSTRAINT chk_intervalo_plano CHECK (intervalo_dias > 0 OR intervalo_km > 0)
);

-- Índices para planos
CREATE INDEX idx_planos_manutencao_oficina ON planos_manutencao_preventiva(oficina_id);
CREATE INDEX idx_planos_manutencao_veiculo ON planos_manutencao_preventiva(veiculo_id);
CREATE INDEX idx_planos_manutencao_status ON planos_manutencao_preventiva(status);
CREATE INDEX idx_planos_manutencao_proxima_data ON planos_manutencao_preventiva(proxima_previsao_data) WHERE status = 'ATIVO';
CREATE INDEX idx_planos_manutencao_proxima_km ON planos_manutencao_preventiva(proxima_previsao_km) WHERE status = 'ATIVO';
CREATE INDEX idx_planos_manutencao_ativo ON planos_manutencao_preventiva(ativo, status) WHERE ativo = TRUE;

-- ----------------------------------------------------------------------------
-- 3. HISTÓRICO DE MANUTENÇÃO PREVENTIVA
-- Log de todas as execuções de manutenções
-- ----------------------------------------------------------------------------
CREATE TABLE historico_manutencao_preventiva (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    plano_id UUID NOT NULL REFERENCES planos_manutencao_preventiva(id) ON DELETE CASCADE,
    veiculo_id UUID NOT NULL REFERENCES veiculos(id) ON DELETE CASCADE,
    ordem_servico_id UUID REFERENCES ordem_servico(id) ON DELETE SET NULL,

    -- Dados da execução
    data_execucao DATE NOT NULL,
    km_execucao INTEGER,
    tipo_manutencao VARCHAR(50) NOT NULL,

    -- Checklist executado
    checklist_executado JSONB DEFAULT '[]'::jsonb,  -- Com status de cada item

    -- Peças utilizadas
    pecas_utilizadas JSONB DEFAULT '[]'::jsonb,  -- [{"pecaId": "uuid", "quantidade": 2, "valor": 50.00}]

    -- Valores
    valor_mao_obra DECIMAL(10,2),
    valor_pecas DECIMAL(10,2),
    valor_total DECIMAL(10,2),

    -- Observações
    observacoes TEXT,
    observacoes_mecanico TEXT,

    -- Próxima previsão após execução
    proxima_previsao_data DATE,
    proxima_previsao_km INTEGER,

    -- Controle
    executado_por UUID REFERENCES usuarios(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para histórico
CREATE INDEX idx_historico_manutencao_oficina ON historico_manutencao_preventiva(oficina_id);
CREATE INDEX idx_historico_manutencao_plano ON historico_manutencao_preventiva(plano_id);
CREATE INDEX idx_historico_manutencao_veiculo ON historico_manutencao_preventiva(veiculo_id);
CREATE INDEX idx_historico_manutencao_data ON historico_manutencao_preventiva(data_execucao DESC);
CREATE INDEX idx_historico_manutencao_os ON historico_manutencao_preventiva(ordem_servico_id) WHERE ordem_servico_id IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 4. ALERTAS DE MANUTENÇÃO
-- Fila de alertas a serem enviados (com retry)
-- ----------------------------------------------------------------------------
CREATE TABLE alertas_manutencao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    plano_id UUID NOT NULL REFERENCES planos_manutencao_preventiva(id) ON DELETE CASCADE,
    veiculo_id UUID NOT NULL REFERENCES veiculos(id) ON DELETE CASCADE,
    cliente_id UUID NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,

    -- Tipo de alerta
    tipo_alerta VARCHAR(30) NOT NULL,  -- PROXIMIDADE, VENCIDO, LEMBRETE_AGENDAMENTO

    -- Canal de envio
    canal VARCHAR(20) NOT NULL,  -- WHATSAPP, EMAIL, SMS, PUSH, INTERNO
    destinatario VARCHAR(255) NOT NULL,  -- Telefone ou email

    -- Conteúdo
    titulo VARCHAR(200),
    mensagem TEXT NOT NULL,
    dados_extras JSONB DEFAULT '{}'::jsonb,  -- Dados para template

    -- Status e retry
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',  -- PENDENTE, ENVIADO, FALHOU, CANCELADO
    tentativas INTEGER DEFAULT 0,
    max_tentativas INTEGER DEFAULT 3,
    proxima_tentativa TIMESTAMP,

    -- Resultado
    enviado_em TIMESTAMP,
    erro_mensagem TEXT,
    resposta_gateway JSONB,

    -- Agendamento de envio
    agendar_para TIMESTAMP,  -- Se NULL, enviar imediatamente

    -- Controle
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tipo_alerta CHECK (tipo_alerta IN ('PROXIMIDADE', 'VENCIDO', 'LEMBRETE_AGENDAMENTO', 'CONFIRMACAO')),
    CONSTRAINT chk_canal_alerta CHECK (canal IN ('WHATSAPP', 'EMAIL', 'SMS', 'PUSH', 'INTERNO')),
    CONSTRAINT chk_status_alerta CHECK (status IN ('PENDENTE', 'ENVIADO', 'FALHOU', 'CANCELADO'))
);

-- Índices para alertas
CREATE INDEX idx_alertas_manutencao_oficina ON alertas_manutencao(oficina_id);
CREATE INDEX idx_alertas_manutencao_plano ON alertas_manutencao(plano_id);
CREATE INDEX idx_alertas_manutencao_status ON alertas_manutencao(status);
CREATE INDEX idx_alertas_manutencao_pendentes ON alertas_manutencao(status, proxima_tentativa)
    WHERE status IN ('PENDENTE', 'FALHOU');
CREATE INDEX idx_alertas_manutencao_agendados ON alertas_manutencao(agendar_para)
    WHERE status = 'PENDENTE' AND agendar_para IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 5. AGENDAMENTOS DE MANUTENÇÃO
-- Agendamentos de manutenções futuras com confirmação do cliente
-- ----------------------------------------------------------------------------
CREATE TABLE agendamentos_manutencao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id) ON DELETE CASCADE,
    plano_id UUID REFERENCES planos_manutencao_preventiva(id) ON DELETE SET NULL,
    veiculo_id UUID NOT NULL REFERENCES veiculos(id) ON DELETE CASCADE,
    cliente_id UUID NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,

    -- Dados do agendamento
    data_agendamento DATE NOT NULL,
    hora_agendamento TIME NOT NULL,
    duracao_estimada_minutos INTEGER DEFAULT 60,

    -- Tipo de manutenção
    tipo_manutencao VARCHAR(50) NOT NULL,
    descricao TEXT,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'AGENDADO',  -- AGENDADO, CONFIRMADO, REMARCADO, CANCELADO, REALIZADO

    -- Confirmação do cliente
    token_confirmacao VARCHAR(64) UNIQUE,  -- Token único para link de confirmação
    token_expira_em TIMESTAMP,
    confirmado_em TIMESTAMP,
    confirmado_via VARCHAR(20),  -- LINK, WHATSAPP, TELEFONE, PRESENCIAL

    -- Remarcação
    remarcado_de_id UUID REFERENCES agendamentos_manutencao(id),
    motivo_remarcacao TEXT,

    -- Cancelamento
    cancelado_em TIMESTAMP,
    motivo_cancelamento TEXT,
    cancelado_por UUID REFERENCES usuarios(id),

    -- Realização
    realizado_em TIMESTAMP,
    ordem_servico_id UUID REFERENCES ordem_servico(id) ON DELETE SET NULL,

    -- Lembretes
    lembrete_enviado BOOLEAN DEFAULT FALSE,
    lembrete_enviado_em TIMESTAMP,

    -- Observações
    observacoes TEXT,
    observacoes_internas TEXT,

    -- Controle
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES usuarios(id),

    CONSTRAINT chk_status_agendamento CHECK (status IN ('AGENDADO', 'CONFIRMADO', 'REMARCADO', 'CANCELADO', 'REALIZADO'))
);

-- Índices para agendamentos
CREATE INDEX idx_agendamentos_manutencao_oficina ON agendamentos_manutencao(oficina_id);
CREATE INDEX idx_agendamentos_manutencao_veiculo ON agendamentos_manutencao(veiculo_id);
CREATE INDEX idx_agendamentos_manutencao_cliente ON agendamentos_manutencao(cliente_id);
CREATE INDEX idx_agendamentos_manutencao_data ON agendamentos_manutencao(data_agendamento, hora_agendamento);
CREATE INDEX idx_agendamentos_manutencao_status ON agendamentos_manutencao(status);
CREATE INDEX idx_agendamentos_manutencao_token ON agendamentos_manutencao(token_confirmacao) WHERE token_confirmacao IS NOT NULL;
CREATE INDEX idx_agendamentos_manutencao_calendario ON agendamentos_manutencao(oficina_id, data_agendamento, status);

-- ----------------------------------------------------------------------------
-- 6. CONFIGURAÇÕES DE MANUTENÇÃO PREVENTIVA (por oficina)
-- ----------------------------------------------------------------------------
CREATE TABLE configuracoes_manutencao_preventiva (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL UNIQUE REFERENCES oficinas(id) ON DELETE CASCADE,

    -- Configurações de alertas
    alertas_habilitados BOOLEAN DEFAULT TRUE,
    antecedencia_dias_padrao INTEGER DEFAULT 15,
    antecedencia_km_padrao INTEGER DEFAULT 1000,
    canais_padrao VARCHAR(100)[] DEFAULT ARRAY['WHATSAPP', 'EMAIL'],

    -- Horários de envio
    horario_envio_alertas TIME DEFAULT '09:00',
    horario_envio_lembretes TIME DEFAULT '07:00',
    dias_semana_envio INTEGER[] DEFAULT ARRAY[1, 2, 3, 4, 5],  -- 1=Seg, 7=Dom

    -- Retry de alertas
    max_tentativas_alerta INTEGER DEFAULT 3,
    intervalo_retry_minutos INTEGER DEFAULT 30,

    -- Token de confirmação
    token_expira_horas INTEGER DEFAULT 72,

    -- Mensagens personalizadas (podem sobrescrever templates globais)
    templates_personalizados JSONB DEFAULT '{}'::jsonb,

    -- Controle
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 7. INSERIR TEMPLATES GLOBAIS PADRÃO
-- ----------------------------------------------------------------------------
INSERT INTO templates_manutencao (oficina_id, nome, descricao, tipo_manutencao, intervalo_dias, intervalo_km, criterio, antecedencia_dias, antecedencia_km, checklist, valor_estimado, tempo_estimado_minutos) VALUES
(NULL, 'Troca de Óleo e Filtro', 'Troca de óleo do motor e filtro de óleo', 'TROCA_OLEO', 180, 10000, 'AMBOS', 15, 1000,
 '[{"item": "Drenar óleo usado", "obrigatorio": true}, {"item": "Substituir filtro de óleo", "obrigatorio": true}, {"item": "Adicionar óleo novo", "obrigatorio": true}, {"item": "Verificar nível", "obrigatorio": true}, {"item": "Verificar vazamentos", "obrigatorio": true}]'::jsonb,
 150.00, 30),

(NULL, 'Revisão Completa', 'Revisão completa do veículo', 'REVISAO_COMPLETA', 365, 20000, 'AMBOS', 30, 2000,
 '[{"item": "Verificar freios", "obrigatorio": true}, {"item": "Verificar suspensão", "obrigatorio": true}, {"item": "Verificar direção", "obrigatorio": true}, {"item": "Verificar sistema elétrico", "obrigatorio": true}, {"item": "Verificar níveis de fluidos", "obrigatorio": true}, {"item": "Verificar correias", "obrigatorio": true}, {"item": "Verificar pneus", "obrigatorio": true}]'::jsonb,
 450.00, 120),

(NULL, 'Alinhamento e Balanceamento', 'Alinhamento de direção e balanceamento de rodas', 'ALINHAMENTO_BALANCEAMENTO', 180, 10000, 'AMBOS', 15, 1000,
 '[{"item": "Verificar desgaste dos pneus", "obrigatorio": true}, {"item": "Alinhar direção", "obrigatorio": true}, {"item": "Balancear rodas", "obrigatorio": true}, {"item": "Calibrar pneus", "obrigatorio": true}]'::jsonb,
 120.00, 45),

(NULL, 'Troca de Filtro de Ar', 'Substituição do filtro de ar do motor', 'FILTRO_AR', 365, 15000, 'AMBOS', 30, 2000,
 '[{"item": "Remover filtro antigo", "obrigatorio": true}, {"item": "Limpar caixa do filtro", "obrigatorio": false}, {"item": "Instalar filtro novo", "obrigatorio": true}]'::jsonb,
 80.00, 15),

(NULL, 'Troca de Pastilhas de Freio', 'Substituição das pastilhas de freio', 'PASTILHAS_FREIO', 365, 30000, 'AMBOS', 30, 3000,
 '[{"item": "Verificar espessura das pastilhas", "obrigatorio": true}, {"item": "Verificar discos de freio", "obrigatorio": true}, {"item": "Substituir pastilhas", "obrigatorio": true}, {"item": "Verificar fluido de freio", "obrigatorio": true}, {"item": "Testar frenagem", "obrigatorio": true}]'::jsonb,
 250.00, 60),

(NULL, 'Troca de Correia Dentada', 'Substituição da correia dentada do motor', 'CORREIA_DENTADA', 1095, 60000, 'AMBOS', 60, 5000,
 '[{"item": "Verificar tensionador", "obrigatorio": true}, {"item": "Verificar polias", "obrigatorio": true}, {"item": "Substituir correia dentada", "obrigatorio": true}, {"item": "Verificar bomba dágua", "obrigatorio": true}]'::jsonb,
 800.00, 180),

(NULL, 'Troca de Fluido de Freio', 'Substituição do fluido de freio (DOT)', 'FLUIDO_FREIO', 730, 40000, 'AMBOS', 30, 3000,
 '[{"item": "Drenar fluido antigo", "obrigatorio": true}, {"item": "Sangrar sistema", "obrigatorio": true}, {"item": "Adicionar fluido novo", "obrigatorio": true}, {"item": "Verificar vazamentos", "obrigatorio": true}]'::jsonb,
 100.00, 45),

(NULL, 'Troca de Velas de Ignição', 'Substituição das velas de ignição', 'VELAS_IGNICAO', 730, 40000, 'AMBOS', 30, 3000,
 '[{"item": "Remover velas antigas", "obrigatorio": true}, {"item": "Verificar eletrodos", "obrigatorio": true}, {"item": "Instalar velas novas", "obrigatorio": true}, {"item": "Verificar cabos de ignição", "obrigatorio": false}]'::jsonb,
 180.00, 30);

-- ----------------------------------------------------------------------------
-- 8. INSERIR FEATURE FLAG
-- ----------------------------------------------------------------------------
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('MANUTENCAO_PREVENTIVA', 'Manutenção Preventiva', 'Sistema de Manutenção Preventiva com alertas automáticos, calendário e agendamentos', true, 'OPERACIONAL', '{"PROFISSIONAL": true, "TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 9. COMENTÁRIOS NAS TABELAS
-- ----------------------------------------------------------------------------
COMMENT ON TABLE templates_manutencao IS 'Templates de manutenção reutilizáveis (globais ou por oficina)';
COMMENT ON TABLE planos_manutencao_preventiva IS 'Planos de manutenção preventiva vinculados a veículos';
COMMENT ON TABLE historico_manutencao_preventiva IS 'Histórico de execuções de manutenções preventivas';
COMMENT ON TABLE alertas_manutencao IS 'Fila de alertas de manutenção a serem enviados';
COMMENT ON TABLE agendamentos_manutencao IS 'Agendamentos de manutenções futuras';
COMMENT ON TABLE configuracoes_manutencao_preventiva IS 'Configurações de manutenção preventiva por oficina';
