--liquibase formatted sql
--changeset pitstop:V081-add-diagnostico-ia-feature-flag

-- =============================================
-- Feature Flag: Diagnóstico Assistido por IA
-- Disponível para: PROFISSIONAL e TURBINADO
-- =============================================

INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('DIAGNOSTICO_IA', 'Diagnóstico Assistido por IA', 'Diagnóstico automático de problemas usando inteligência artificial', false, 'OPERACIONAL', '{"PROFISSIONAL": true, "TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

--rollback DELETE FROM feature_flags WHERE codigo = 'DIAGNOSTICO_IA';
