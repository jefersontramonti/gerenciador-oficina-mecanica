--liquibase formatted sql
--changeset pitstop:V048-insert-all-feature-flags

-- =============================================
-- FEATURE FLAGS COMPLETAS - PITSTOP SaaS
-- Total: 70 Feature Flags em 12 categorias
-- =============================================

-- 1. COMUNICAÇÃO (9 flags)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('WHATSAPP_CAMPANHAS', 'Campanhas de WhatsApp', 'Envio de mensagens em massa para clientes (promoções, lembretes de revisão)', false, 'COMUNICACAO', '{"TURBINADO": true}'),
('EMAIL_MARKETING', 'Email Marketing', 'Campanhas de email marketing para base de clientes', false, 'COMUNICACAO', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('SMS_NOTIFICATIONS', 'Notificações via SMS', 'Envio de SMS para clientes (lembretes, confirmações)', false, 'COMUNICACAO', '{"TURBINADO": true}'),
('TELEGRAM_BOT', 'Bot do Telegram', 'Bot interno para mecânicos consultarem status de OS', false, 'COMUNICACAO', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('PUSH_NOTIFICATIONS', 'Notificações Push', 'Push notifications no app mobile', true, 'COMUNICACAO', '{}'),
('WEBHOOK_NOTIFICATIONS', 'Webhooks Personalizados', 'Envio de eventos para URLs externas (integrações customizadas)', false, 'COMUNICACAO', '{"TURBINADO": true}'),
('CHAT_INTERNO', 'Chat Interno', 'Mensagens em tempo real entre membros da equipe', false, 'COMUNICACAO', '{"PROFISSIONAL": true, "TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

-- 2. RELATÓRIOS E EXPORTAÇÃO (11 flags - já temos PDF_EXPORT e EXCEL_EXPORT)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('PDF_EXPORT_BASICO', 'Exportação PDF Básica', 'Gerar PDFs de OS, orçamentos e recibos', true, 'RELATORIOS', '{}'),
('PDF_EXPORT_AVANCADO', 'Exportação PDF Avançada', 'PDFs personalizados com logo, cores customizadas', false, 'RELATORIOS', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('CSV_EXPORT', 'Exportação para CSV', 'Exportar dados em formato CSV', true, 'RELATORIOS', '{}'),
('RELATORIOS_GERENCIAIS', 'Relatórios Gerenciais', 'Relatórios complexos com gráficos (faturamento, performance)', false, 'RELATORIOS', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('DASHBOARD_BASICO', 'Dashboard Básico', 'Dashboard com métricas essenciais (OS, faturamento)', true, 'RELATORIOS', '{}'),
('RELATORIO_FISCAL', 'Relatórios Fiscais', 'Relatórios para contabilidade e fisco', false, 'RELATORIOS', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('EXPORT_CONTABIL', 'Exportação Contábil', 'Exportar movimentações em formato contábil (SPED)', false, 'RELATORIOS', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('RELATORIO_AGENDADO', 'Relatórios Agendados', 'Agendar envio automático de relatórios por email', false, 'RELATORIOS', '{"TURBINADO": true}'),
('ANALISE_PREDITIVA', 'Análise Preditiva', 'Previsões baseadas em dados históricos', false, 'RELATORIOS', '{"TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

-- 3. FINANCEIRO E PAGAMENTOS (8 flags - já temos INTEGRACAO_PAGAMENTOS)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('INTEGRACAO_MERCADO_PAGO', 'Integração Mercado Pago', 'Pagamentos online via Mercado Pago (PIX, cartão, boleto)', false, 'FINANCEIRO', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('INTEGRACAO_STRIPE', 'Integração Stripe', 'Pagamentos internacionais via Stripe', false, 'FINANCEIRO', '{"TURBINADO": true}'),
('INTEGRACAO_PAGSEGURO', 'Integração PagSeguro', 'Pagamentos via PagSeguro', false, 'FINANCEIRO', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('PARCELAMENTO_CARTAO', 'Parcelamento no Cartão', 'Permitir pagamento parcelado em até 12x', false, 'FINANCEIRO', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('SPLIT_PAYMENT', 'Split Payment', 'Dividir pagamento entre múltiplas contas', false, 'FINANCEIRO', '{"TURBINADO": true}'),
('CONCILIACAO_BANCARIA', 'Conciliação Bancária', 'Importar extratos bancários e conciliar automaticamente', false, 'FINANCEIRO', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('FLUXO_CAIXA_AVANCADO', 'Fluxo de Caixa Avançado', 'Projeções, DRE, análise de lucratividade', false, 'FINANCEIRO', '{"TURBINADO": true}'),
('COBRANCA_RECORRENTE', 'Cobrança Recorrente', 'Cobrar clientes automaticamente (assinaturas, planos)', false, 'FINANCEIRO', '{"TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

-- 4. NOTA FISCAL ELETRÔNICA (5 flags)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('EMISSAO_NFE', 'Emissão de NF-e', 'Emitir Nota Fiscal Eletrônica (NF-e)', false, 'FISCAL', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('EMISSAO_NFSE', 'Emissão de NFS-e', 'Emitir Nota Fiscal de Serviço Eletrônica', false, 'FISCAL', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('EMISSAO_NFCE', 'Emissão de NFC-e', 'Nota Fiscal de Consumidor Eletrônica (varejo)', false, 'FISCAL', '{"TURBINADO": true}'),
('IMPORTACAO_XML_NFE', 'Importação de XML NF-e', 'Importar peças direto do XML da nota fiscal de compra', false, 'FISCAL', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('MANIFESTACAO_DESTINATARIO', 'Manifestação do Destinatário', 'Confirmar recebimento de NF-e (ciência, confirmação)', false, 'FISCAL', '{"PROFISSIONAL": true, "TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

-- 5. OPERACIONAL E GESTÃO (10 flags - já temos AGENDAMENTO_OS e MULTI_FILIAL)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('CHECKLIST_VISTORIA', 'Checklist de Vistoria', 'Checklist digital ao receber veículo (pneus, lataria, combustível)', false, 'OPERACIONAL', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('FOTOS_DOCUMENTACAO', 'Fotos e Anexos em OS', 'Anexar fotos e documentos nas ordens de serviço', false, 'OPERACIONAL', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('MANUTENCAO_PREVENTIVA', 'Manutenção Preventiva', 'Alertas de revisão baseados em KM/tempo', false, 'OPERACIONAL', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('CONTROLE_GARANTIA', 'Controle de Garantia', 'Gerenciar garantias de peças e serviços', false, 'OPERACIONAL', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('GESTAO_FORNECEDORES', 'Gestão de Fornecedores', 'Cadastro de fornecedores, compras, histórico', false, 'OPERACIONAL', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('ORDEM_COMPRA', 'Ordem de Compra', 'Gerar ordens de compra para fornecedores', false, 'OPERACIONAL', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('CONTROLE_PONTO', 'Controle de Ponto', 'Registrar entrada/saída de funcionários', false, 'OPERACIONAL', '{"TURBINADO": true}'),
('COMISSAO_MECANICOS', 'Comissão de Mecânicos', 'Calcular comissões por OS realizada', false, 'OPERACIONAL', '{"PROFISSIONAL": true, "TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

-- 6. INTEGRAÇÕES E API (7 flags - já temos API_EXTERNA)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('API_PUBLICA', 'API Pública', 'Acesso à API REST do PitStop para integrações', false, 'INTEGRACAO', '{"TURBINADO": true}'),
('WEBHOOK_EVENTOS', 'Webhooks de Eventos', 'Receber eventos do sistema via webhook', false, 'INTEGRACAO', '{"TURBINADO": true}'),
('INTEGRACAO_ZAPIER', 'Integração com Zapier', 'Conectar PitStop com 5000+ apps via Zapier', false, 'INTEGRACAO', '{"TURBINADO": true}'),
('INTEGRACAO_ERP', 'Integração com ERP', 'Integrar com sistemas ERP externos', false, 'INTEGRACAO', '{"TURBINADO": true}'),
('INTEGRACAO_GOOGLE', 'Integração Google Workspace', 'Sincronizar com Google Calendar, Drive, Sheets', false, 'INTEGRACAO', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('INTEGRACAO_MARKETPLACE', 'Integração com Marketplaces', 'Vender peças em marketplaces (Mercado Livre, OLX)', false, 'INTEGRACAO', '{"TURBINADO": true}'),
('SSO_SAML', 'Single Sign-On (SSO/SAML)', 'Login único com sistemas corporativos', false, 'INTEGRACAO', '{"TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

-- 7. BACKUP E SEGURANÇA (5 flags)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('BACKUP_AUTOMATICO', 'Backup Automático', 'Backup diário automático dos dados', false, 'SEGURANCA', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('BACKUP_MANUAL', 'Backup Manual', 'Baixar backup completo sob demanda', true, 'SEGURANCA', '{}'),
('AUTENTICACAO_2FA', 'Autenticação 2FA', 'Two-Factor Authentication (TOTP)', false, 'SEGURANCA', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('AUDITORIA_AVANCADA', 'Auditoria Avançada', 'Log completo de todas ações dos usuários', false, 'SEGURANCA', '{"TURBINADO": true}'),
('RESTRICAO_IP', 'Restrição por IP', 'Permitir acesso apenas de IPs específicos', false, 'SEGURANCA', '{"TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

-- 8. MOBILE E APPS (4 flags)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('APP_MOBILE_CLIENTE', 'App Mobile para Cliente', 'Aplicativo para clientes acompanharem suas OS', false, 'MOBILE', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('APP_MOBILE_MECANICO', 'App Mobile para Mecânico', 'App para mecânicos atualizarem OS em tempo real', false, 'MOBILE', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('OFFLINE_MODE', 'Modo Offline', 'Trabalhar sem internet e sincronizar depois', false, 'MOBILE', '{"TURBINADO": true}'),
('QR_CODE_VEICULO', 'QR Code de Veículo', 'Escanear QR Code para abrir ficha do veículo', false, 'MOBILE', '{"PROFISSIONAL": true, "TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

-- 9. CUSTOMIZAÇÃO E BRANDING (4 flags)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('LOGO_CUSTOMIZADA', 'Logo Customizada', 'Usar logo própria em documentos e sistema', false, 'BRANDING', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('CORES_CUSTOMIZADAS', 'Cores Personalizadas', 'Customizar cores do sistema (tema)', false, 'BRANDING', '{"TURBINADO": true}'),
('DOMINIO_PROPRIO', 'Domínio Próprio', 'Usar domínio próprio (minhaoficina.com.br)', false, 'BRANDING', '{"TURBINADO": true}'),
('EMAIL_CUSTOMIZADO', 'Email Customizado', 'Emails enviados do próprio domínio (@minhaoficina.com.br)', false, 'BRANDING', '{"TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

-- 10. MARKETING E CRM (5 flags)
INSERT INTO feature_flags (codigo, nome, descricao, habilitado_global, categoria, habilitado_por_plano) VALUES
('CRM_BASICO', 'CRM Básico', 'Histórico de interações com clientes', false, 'MARKETING', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('PROGRAMA_FIDELIDADE', 'Programa de Fidelidade', 'Pontos, descontos, cashback para clientes frequentes', false, 'MARKETING', '{"TURBINADO": true}'),
('PESQUISA_SATISFACAO', 'Pesquisa de Satisfação', 'Enviar NPS/CSAT após finalização de OS', false, 'MARKETING', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('CUPONS_DESCONTO', 'Cupons de Desconto', 'Criar e gerenciar cupons promocionais', false, 'MARKETING', '{"PROFISSIONAL": true, "TURBINADO": true}'),
('INDICACAO_CLIENTES', 'Programa de Indicação', 'Clientes ganham benefícios ao indicar novos clientes', false, 'MARKETING', '{"TURBINADO": true}')
ON CONFLICT (codigo) DO NOTHING;

--rollback DELETE FROM feature_flags WHERE codigo IN ('WHATSAPP_CAMPANHAS', 'EMAIL_MARKETING', 'SMS_NOTIFICATIONS', 'TELEGRAM_BOT', 'PUSH_NOTIFICATIONS', 'WEBHOOK_NOTIFICATIONS', 'CHAT_INTERNO', 'PDF_EXPORT_BASICO', 'PDF_EXPORT_AVANCADO', 'CSV_EXPORT', 'RELATORIOS_GERENCIAIS', 'DASHBOARD_BASICO', 'RELATORIO_FISCAL', 'EXPORT_CONTABIL', 'RELATORIO_AGENDADO', 'ANALISE_PREDITIVA', 'INTEGRACAO_MERCADO_PAGO', 'INTEGRACAO_STRIPE', 'INTEGRACAO_PAGSEGURO', 'PARCELAMENTO_CARTAO', 'SPLIT_PAYMENT', 'CONCILIACAO_BANCARIA', 'FLUXO_CAIXA_AVANCADO', 'COBRANCA_RECORRENTE', 'EMISSAO_NFE', 'EMISSAO_NFSE', 'EMISSAO_NFCE', 'IMPORTACAO_XML_NFE', 'MANIFESTACAO_DESTINATARIO', 'CHECKLIST_VISTORIA', 'FOTOS_DOCUMENTACAO', 'MANUTENCAO_PREVENTIVA', 'CONTROLE_GARANTIA', 'GESTAO_FORNECEDORES', 'ORDEM_COMPRA', 'CONTROLE_PONTO', 'COMISSAO_MECANICOS', 'API_PUBLICA', 'WEBHOOK_EVENTOS', 'INTEGRACAO_ZAPIER', 'INTEGRACAO_ERP', 'INTEGRACAO_GOOGLE', 'INTEGRACAO_MARKETPLACE', 'SSO_SAML', 'BACKUP_AUTOMATICO', 'BACKUP_MANUAL', 'AUTENTICACAO_2FA', 'AUDITORIA_AVANCADA', 'RESTRICAO_IP', 'APP_MOBILE_CLIENTE', 'APP_MOBILE_MECANICO', 'OFFLINE_MODE', 'QR_CODE_VEICULO', 'LOGO_CUSTOMIZADA', 'CORES_CUSTOMIZADAS', 'DOMINIO_PROPRIO', 'EMAIL_CUSTOMIZADO', 'CRM_BASICO', 'PROGRAMA_FIDELIDADE', 'PESQUISA_SATISFACAO', 'CUPONS_DESCONTO', 'INDICACAO_CLIENTES');
