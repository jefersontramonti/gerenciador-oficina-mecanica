-- =====================================================
-- Migration: Super Admin Infrastructure
-- Purpose: Enable SUPER_ADMIN profile without tenant
-- =====================================================

-- 1. Tornar usuarios.oficina_id nullable (permite SUPER_ADMIN sem oficina)
ALTER TABLE usuarios ALTER COLUMN oficina_id DROP NOT NULL;

-- 2. Corrigir usuários órfãos existentes ANTES de adicionar constraint
-- Link todos os usuários com oficina_id NULL (que não sejam SUPER_ADMIN) à oficina padrão
UPDATE usuarios
SET oficina_id = '0f111111-1111-1111-1111-111111111111'
WHERE oficina_id IS NULL AND perfil != 'SUPER_ADMIN';

-- 3. Adicionar constraint: SUPER_ADMIN não tem oficina, outros perfis devem ter
ALTER TABLE usuarios ADD CONSTRAINT chk_super_admin_oficina
CHECK (
  (perfil = 'SUPER_ADMIN' AND oficina_id IS NULL) OR
  (perfil != 'SUPER_ADMIN' AND oficina_id IS NOT NULL)
);

-- 4. Atualizar valores dos planos conforme nova tabela de preços
UPDATE oficinas SET valor_mensalidade = 160.00 WHERE plano = 'ECONOMICO';
UPDATE oficinas SET valor_mensalidade = 250.00 WHERE plano = 'PROFISSIONAL';

-- 5. Adicionar coluna para armazenar status anterior (útil para auditoria)
ALTER TABLE oficinas ADD COLUMN IF NOT EXISTS status_anterior VARCHAR(20);

-- 6. Criar tabela de auditoria (logs imutáveis para defesa legal)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario VARCHAR(100) NOT NULL, -- email do usuário que executou a ação
    usuario_id UUID, -- ID do usuário (pode ser null se ação foi automática)
    acao VARCHAR(100) NOT NULL, -- CRIAR_OFICINA, SUSPENDER_OFICINA, MARCAR_PAGAMENTO, etc.
    entidade VARCHAR(50) NOT NULL, -- Oficina, Usuario, Pagamento, etc.
    entidade_id UUID NOT NULL, -- ID da entidade afetada
    dados_antes TEXT, -- JSON com estado anterior (null em CREATE)
    dados_depois TEXT, -- JSON com estado posterior (null em DELETE)
    observacao TEXT, -- Detalhes adicionais
    ip_address VARCHAR(45), -- IP do cliente (suporta IPv6)
    user_agent VARCHAR(500), -- Browser/client info
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Criar índices para performance de queries de auditoria
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_logs_usuario ON audit_logs(usuario);
CREATE INDEX idx_audit_logs_entidade ON audit_logs(entidade, entidade_id);
CREATE INDEX idx_audit_logs_acao ON audit_logs(acao);

-- 8. Criar tabela de pagamentos SaaS (controle de mensalidades)
CREATE TABLE IF NOT EXISTS saas_pagamentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oficina_id UUID NOT NULL REFERENCES oficinas(id),
    referencia_mes DATE NOT NULL, -- Mês de referência do pagamento (ex: 2025-01-01)
    valor_pago DECIMAL(10, 2) NOT NULL,
    data_pagamento DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    forma_pagamento VARCHAR(50), -- PIX, Boleto, Cartão, etc.
    comprovante_url VARCHAR(500), -- URL do comprovante de pagamento
    observacao TEXT,
    registrado_por_usuario_id UUID REFERENCES usuarios(id), -- SUPER_ADMIN que registrou
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(oficina_id, referencia_mes) -- Evita pagamentos duplicados para mesmo mês
);

-- 9. Criar índices para tabela de pagamentos
CREATE INDEX idx_saas_pagamentos_oficina ON saas_pagamentos(oficina_id);
CREATE INDEX idx_saas_pagamentos_referencia ON saas_pagamentos(referencia_mes);
CREATE INDEX idx_saas_pagamentos_vencimento ON saas_pagamentos(data_vencimento);

-- 10. Adicionar coluna para controlar trial de 30 dias
ALTER TABLE oficinas ADD COLUMN IF NOT EXISTS trial_end_date DATE;

-- 11. Adicionar coluna para controlar se oficina foi criada via auto-registro
ALTER TABLE oficinas ADD COLUMN IF NOT EXISTS auto_registro BOOLEAN DEFAULT false;

-- 12. Criar view para dashboard SaaS (performance)
CREATE OR REPLACE VIEW vw_saas_dashboard_stats AS
SELECT
    COUNT(*) FILTER (WHERE status = 'ATIVA' AND (trial_end_date IS NULL OR trial_end_date < CURRENT_DATE)) as oficinas_ativas,
    COUNT(*) FILTER (WHERE status = 'ATIVA' AND trial_end_date >= CURRENT_DATE) as oficinas_trial,
    COUNT(*) FILTER (WHERE status = 'SUSPENSA') as oficinas_suspensas,
    COUNT(*) FILTER (WHERE status = 'CANCELADA') as oficinas_canceladas,
    COUNT(*) FILTER (WHERE status = 'INATIVA') as oficinas_inativas,
    COUNT(*) as total_oficinas,
    COALESCE(SUM(valor_mensalidade) FILTER (WHERE status = 'ATIVA' AND (trial_end_date IS NULL OR trial_end_date < CURRENT_DATE)), 0) as mrr_total,
    COUNT(*) FILTER (WHERE trial_end_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days') as trial_vencendo_7_dias
FROM oficinas;

-- 13. Criar função para calcular próximo vencimento
CREATE OR REPLACE FUNCTION calcular_proximo_vencimento(data_assinatura DATE, plano VARCHAR)
RETURNS DATE AS $$
BEGIN
    -- Trial: 30 dias a partir da assinatura
    -- Produção: 30 dias após fim do trial
    IF data_assinatura IS NULL THEN
        RETURN NULL;
    END IF;

    RETURN data_assinatura + INTERVAL '30 days';
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 14. Comentários para documentação
COMMENT ON TABLE audit_logs IS 'Logs imutáveis de auditoria para rastreabilidade e defesa legal';
COMMENT ON TABLE saas_pagamentos IS 'Controle de pagamentos mensais das oficinas (SaaS billing)';
COMMENT ON COLUMN oficinas.trial_end_date IS 'Data de término do período trial (30 dias gratuitos)';
COMMENT ON COLUMN oficinas.auto_registro IS 'Indica se oficina foi criada via cadastro público (true) ou manualmente pelo SUPER_ADMIN (false)';
