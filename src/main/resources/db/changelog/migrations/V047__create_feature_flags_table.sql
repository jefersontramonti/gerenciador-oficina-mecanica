--liquibase formatted sql
--changeset pitstop:V047-create-feature-flags-table

-- Tabela de Feature Flags para controle gradual de funcionalidades
CREATE TABLE IF NOT EXISTS feature_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(100) NOT NULL UNIQUE,
    nome VARCHAR(200) NOT NULL,
    descricao TEXT,

    -- Configurações de habilitação
    habilitado_global BOOLEAN NOT NULL DEFAULT false,
    habilitado_por_plano JSONB DEFAULT '{}',
    habilitado_por_oficina UUID[] DEFAULT '{}',

    -- Rollout gradual
    percentual_rollout INTEGER DEFAULT 0 CHECK (percentual_rollout >= 0 AND percentual_rollout <= 100),

    -- Período de validade
    data_inicio TIMESTAMP WITH TIME ZONE,
    data_fim TIMESTAMP WITH TIME ZONE,

    -- Metadados
    categoria VARCHAR(50) DEFAULT 'GERAL',
    requer_autorizacao BOOLEAN DEFAULT false,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID,
    updated_by UUID
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_feature_flags_codigo ON feature_flags(codigo);
CREATE INDEX IF NOT EXISTS idx_feature_flags_categoria ON feature_flags(categoria);
CREATE INDEX IF NOT EXISTS idx_feature_flags_habilitado_global ON feature_flags(habilitado_global);

--rollback DROP TABLE IF EXISTS feature_flags;

--changeset pitstop:V047-insert-default-feature-flags
-- Inserir algumas feature flags padrão
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria) VALUES
('WHATSAPP_NOTIFICATIONS', 'Notificações WhatsApp', 'Permite envio de notificações via WhatsApp', false, 'COMUNICACAO'),
('EMAIL_NOTIFICATIONS', 'Notificações Email', 'Permite envio de notificações via Email', true, 'COMUNICACAO'),
('PDF_EXPORT', 'Exportar PDF', 'Permite exportação de documentos em PDF', true, 'RELATORIOS'),
('EXCEL_EXPORT', 'Exportar Excel', 'Permite exportação de dados em Excel', true, 'RELATORIOS'),
('DASHBOARD_AVANCADO', 'Dashboard Avançado', 'Acesso ao dashboard com gráficos avançados', false, 'PREMIUM'),
('INTEGRACAO_PAGAMENTOS', 'Integração Pagamentos', 'Integração com gateways de pagamento', false, 'FINANCEIRO'),
('MULTI_FILIAL', 'Multi-filial', 'Permite gestão de múltiplas filiais', false, 'PREMIUM'),
('API_EXTERNA', 'API Externa', 'Acesso à API externa do sistema', false, 'INTEGRACAO'),
('RELATORIOS_PERSONALIZADOS', 'Relatórios Personalizados', 'Criação de relatórios customizados', false, 'PREMIUM'),
('AGENDAMENTO_OS', 'Agendamento de OS', 'Permite agendamento de ordens de serviço', true, 'OPERACIONAL')
ON CONFLICT (codigo) DO NOTHING;

--rollback DELETE FROM feature_flags WHERE codigo IN ('WHATSAPP_NOTIFICATIONS', 'EMAIL_NOTIFICATIONS', 'PDF_EXPORT', 'EXCEL_EXPORT', 'DASHBOARD_AVANCADO', 'INTEGRACAO_PAGAMENTOS', 'MULTI_FILIAL', 'API_EXTERNA', 'RELATORIOS_PERSONALIZADOS', 'AGENDAMENTO_OS');
