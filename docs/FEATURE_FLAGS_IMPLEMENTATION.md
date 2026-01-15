# Feature Flags - Plano de Implementação Completo

> **Documento de Acompanhamento**
> Última atualização: 2026-01-14
> Status: EM ANDAMENTO

---

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Etapa 1: Infraestrutura de Gating](#etapa-1-infraestrutura-de-gating)
3. [Etapa 2: Comunicação e Notificações](#etapa-2-comunicação-e-notificações)
4. [Etapa 3: Financeiro e Pagamentos](#etapa-3-financeiro-e-pagamentos)
5. [Etapa 4: Nota Fiscal Eletrônica](#etapa-4-nota-fiscal-eletrônica)
6. [Etapa 5: Operacional e Gestão](#etapa-5-operacional-e-gestão)
7. [Etapa 6: Relatórios Avançados](#etapa-6-relatórios-avançados)
8. [Etapa 7: Integrações e API](#etapa-7-integrações-e-api)
9. [Etapa 8: Segurança Avançada](#etapa-8-segurança-avançada)
10. [Etapa 9: Customização e Branding](#etapa-9-customização-e-branding)
11. [Etapa 10: Marketing e CRM](#etapa-10-marketing-e-crm)
12. [Etapa 11: Aplicativos Mobile](#etapa-11-aplicativos-mobile)
13. [Changelog](#changelog)

---

## 1. Visão Geral

### 1.1 Resumo do Sistema de Feature Flags

O PitStop possui 60+ feature flags definidas no banco de dados, controladas por:
- **Habilitação Global**: Flag ativa para todos
- **Habilitação por Plano**: ECONOMICO, PROFISSIONAL, TURBINADO
- **Habilitação por Oficina**: Override específico
- **Rollout Percentual**: Liberação gradual

### 1.2 Arquivos Principais

| Arquivo | Localização | Propósito |
|---------|-------------|-----------|
| `FeatureFlag.java` | `src/main/java/com/pitstop/saas/domain/` | Entity |
| `FeatureFlagService.java` | `src/main/java/com/pitstop/saas/service/` | Lógica de verificação |
| `SaasFeatureFlagController.java` | `src/main/java/com/pitstop/saas/controller/` | API SUPER_ADMIN |
| `OficinaFeatureFlagController.java` | `src/main/java/com/pitstop/saas/controller/` | API usuários |
| `featureService.ts` | `frontend/src/shared/services/` | Cliente frontend |
| `V048__insert_all_feature_flags.sql` | `src/main/resources/db/changelog/migrations/` | Dados iniciais |

### 1.3 Planos e Preços

| Plano | Preço | Usuários | Features Principais |
|-------|-------|----------|---------------------|
| ECONOMICO | R$ 160/mês | 1 | Básico |
| PROFISSIONAL | R$ 250/mês | 3 | + NF, + Relatórios |
| TURBINADO | Consulte | Ilimitados | Tudo liberado |

### 1.4 Status Geral de Implementação

| Status | Símbolo | Quantidade |
|--------|---------|------------|
| Implementado | ✅ | 10 |
| Parcial | ⚠️ | 11 |
| Não Implementado | ❌ | 39 |
| **Total** | | **60** |

---

## Etapa 1: Infraestrutura de Gating

> **Objetivo**: Criar sistema de verificação de flags no backend e frontend
> **Prioridade**: CRÍTICA
> **Estimativa**: 2-3 dias

### 1.1 Backend - Criar Annotation para Verificação

- [x] Criar `@RequiresFeature` annotation
- [x] Criar `FeatureGateAspect` para interceptar chamadas
- [x] Criar `FeatureNotEnabledException`
- [x] Adicionar handler no `GlobalExceptionHandler`
- [ ] Criar testes unitários

**Arquivos criados:**
```
src/main/java/com/pitstop/shared/security/feature/
├── RequiresFeature.java (annotation)
├── FeatureGateAspect.java (aspect)
└── FeatureNotEnabledException.java (exception)
```

### 1.2 Frontend - Criar Componentes de Gating

- [x] Criar hook `useFeatureFlag(code)`
- [x] Criar hook `useFeatureFlagMultiple(codes[])`
- [x] Criar componente `<FeatureGate feature="CODE">`
- [x] Criar componente `<PlanUpgradePrompt plan="PROFISSIONAL">`
- [x] Criar contexto `FeatureFlagContext`
- [x] Carregar flags do usuário no login

**Arquivos criados:**
```
frontend/src/shared/
├── contexts/FeatureFlagContext.tsx
├── hooks/useFeatureFlag.ts
└── components/
    ├── FeatureGate.tsx
    └── PlanUpgradePrompt.tsx
```

### 1.3 Integrar com Fluxo de Autenticação

- [x] Carregar features no `FeatureFlagContext` após login
- [x] Cachear features no `localStorage` (não sensível)
- [x] Invalidar cache ao fazer logout
- [x] Usar endpoint existente `/api/features/me`

### Status da Etapa 1

| Item | Status | Data | Observações |
|------|--------|------|-------------|
| @RequiresFeature annotation | ✅ | 2026-01-14 | Suporta método e classe |
| FeatureGateAspect | ✅ | 2026-01-14 | AOP com Spring |
| FeatureNotEnabledException | ✅ | 2026-01-14 | Com código, nome e plano |
| GlobalExceptionHandler update | ✅ | 2026-01-14 | HTTP 403 + upgrade info |
| useFeatureFlag hook | ✅ | 2026-01-14 | Single e multiple |
| FeatureGate component | ✅ | 2026-01-14 | Com fallback e HOC |
| PlanUpgradePrompt component | ✅ | 2026-01-14 | 3 variants: card/inline/banner |
| FeatureFlagContext | ✅ | 2026-01-14 | Com cache localStorage |
| Integração main.tsx | ✅ | 2026-01-14 | Provider global |
| Testes unitários | ❌ | | Pendente |

---

## Etapa 2: Comunicação e Notificações

> **Objetivo**: Implementar flags de comunicação
> **Prioridade**: ALTA
> **Estimativa**: 5-7 dias

### 2.1 EMAIL_MARKETING

- [ ] Criar `CampanhaEmail` entity
- [ ] Criar `CampanhaEmailService`
- [ ] Criar templates de email marketing
- [ ] Integrar com Spring Mail
- [ ] Criar UI de criação de campanhas
- [ ] Adicionar gating `@RequiresFeature("EMAIL_MARKETING")`

### 2.2 SMS_NOTIFICATIONS

- [ ] Pesquisar provider (Twilio, AWS SNS)
- [ ] Criar `SmsService` interface
- [ ] Implementar provider escolhido
- [ ] Criar configuração por oficina
- [ ] Adicionar gating

### 2.3 WHATSAPP_CAMPANHAS (Completar)

- [ ] Criar `CampanhaWhatsApp` entity
- [ ] Criar endpoint de envio em massa
- [ ] Integrar com Evolution API bulk send
- [ ] Criar UI de campanhas
- [ ] Controlar limite por plano

### 2.4 TELEGRAM_BOT (Completar)

- [ ] Verificar `TelegramService.java` existente
- [ ] Completar integração com Telegram API
- [ ] Criar bot commands (/status, /os, etc)
- [ ] Vincular chat_id por usuário/mecânico

### 2.5 WEBHOOK_NOTIFICATIONS

- [ ] Criar `WebhookConfig` entity
- [ ] Criar `WebhookService` para disparo
- [ ] Implementar retry com backoff
- [ ] Criar UI de configuração de webhooks
- [ ] Eventos: OS_CREATED, OS_STATUS_CHANGED, PAYMENT_RECEIVED

### 2.6 CHAT_INTERNO

- [ ] Criar `Mensagem` entity
- [ ] Criar `Conversa` entity
- [ ] Implementar via WebSocket existente
- [ ] Criar UI de chat
- [ ] Notificações em tempo real

### Status da Etapa 2

| Flag | Status | Data | Observações |
|------|--------|------|-------------|
| EMAIL_MARKETING | ❌ | | |
| SMS_NOTIFICATIONS | ❌ | | |
| WHATSAPP_CAMPANHAS | ⚠️ | | Evolution API ok |
| TELEGRAM_BOT | ⚠️ | | Service existe |
| WEBHOOK_NOTIFICATIONS | ❌ | | |
| CHAT_INTERNO | ❌ | | WebSocket pronto |

---

## Etapa 3: Financeiro e Pagamentos

> **Objetivo**: Completar integrações de pagamento
> **Prioridade**: ALTA
> **Estimativa**: 7-10 dias

### 3.1 INTEGRACAO_MERCADO_PAGO (Completar)

- [ ] Verificar `MercadoPagoService.java`
- [ ] Implementar checkout transparente
- [ ] Implementar PIX
- [ ] Implementar cartão de crédito
- [ ] Implementar boleto
- [ ] Webhook de status de pagamento
- [ ] Testes em sandbox

### 3.2 INTEGRACAO_STRIPE

- [ ] Adicionar dependência Stripe SDK
- [ ] Criar `StripeService`
- [ ] Implementar checkout
- [ ] Implementar webhooks
- [ ] Configuração por oficina

### 3.3 INTEGRACAO_PAGSEGURO

- [ ] Adicionar SDK PagSeguro
- [ ] Criar `PagSeguroService`
- [ ] Implementar métodos de pagamento
- [ ] Webhooks

### 3.4 PARCELAMENTO_CARTAO

- [ ] Configurar parcelamento no gateway
- [ ] UI para seleção de parcelas
- [ ] Cálculo de juros (se aplicável)

### 3.5 SPLIT_PAYMENT

- [ ] Implementar split no Mercado Pago
- [ ] Configurar recebedores
- [ ] Relatório de splits

### 3.6 CONCILIACAO_BANCARIA

- [ ] Importação de extrato OFX
- [ ] Matching automático
- [ ] UI de conciliação manual
- [ ] Relatórios

### 3.7 FLUXO_CAIXA_AVANCADO

- [ ] Projeção de receitas/despesas
- [ ] DRE simplificado
- [ ] Gráficos de tendência
- [ ] Alertas de fluxo negativo

### 3.8 COBRANCA_RECORRENTE

- [ ] Criar `Assinatura` entity
- [ ] Integrar com gateway (subscription)
- [ ] Cobrança automática
- [ ] Notificações de vencimento

### Status da Etapa 3

| Flag | Status | Data | Observações |
|------|--------|------|-------------|
| INTEGRACAO_MERCADO_PAGO | ⚠️ | | Estrutura existe |
| INTEGRACAO_STRIPE | ❌ | | |
| INTEGRACAO_PAGSEGURO | ❌ | | |
| PARCELAMENTO_CARTAO | ❌ | | |
| SPLIT_PAYMENT | ❌ | | |
| CONCILIACAO_BANCARIA | ❌ | | |
| FLUXO_CAIXA_AVANCADO | ❌ | | |
| COBRANCA_RECORRENTE | ❌ | | |

---

## Etapa 4: Nota Fiscal Eletrônica

> **Objetivo**: Integrar emissão de NF-e/NFS-e
> **Prioridade**: ALTA
> **Estimativa**: 10-15 dias

### 4.1 Escolher Provider de NF-e

Opções:
- [ ] Focus NFe (https://focusnfe.com.br)
- [ ] NFe.io (https://nfe.io)
- [ ] Enotas (https://enotas.com.br)
- [ ] WebmaniaBR (https://webmaniabr.com)

### 4.2 EMISSAO_NFE (Produto)

- [ ] Integrar com provider escolhido
- [ ] Criar `NfeService`
- [ ] Dados da empresa (CNPJ, IE, etc)
- [ ] Geração de XML
- [ ] Envio para SEFAZ
- [ ] Download do DANFE
- [ ] Cancelamento

### 4.3 EMISSAO_NFSE (Serviço)

- [ ] Integrar com prefeitura (varia por cidade)
- [ ] Criar `NfseService`
- [ ] Cadastro de serviços (CNAE)
- [ ] Geração e envio
- [ ] Download PDF

### 4.4 EMISSAO_NFCE (Varejo)

- [ ] Similar a NF-e
- [ ] Impressão em impressora térmica
- [ ] Contingência offline

### 4.5 IMPORTACAO_XML_NFE

- [ ] Parser de XML NF-e
- [ ] Cadastro automático de peças
- [ ] Atualização de estoque
- [ ] Vinculação com fornecedor

### 4.6 MANIFESTACAO_DESTINATARIO

- [ ] Ciência da operação
- [ ] Confirmação da operação
- [ ] Desconhecimento
- [ ] Operação não realizada

### Status da Etapa 4

| Flag | Status | Data | Observações |
|------|--------|------|-------------|
| Provider NF-e | ❌ | | Escolher |
| EMISSAO_NFE | ❌ | | |
| EMISSAO_NFSE | ❌ | | |
| EMISSAO_NFCE | ❌ | | |
| IMPORTACAO_XML_NFE | ❌ | | |
| MANIFESTACAO_DESTINATARIO | ❌ | | |

---

## Etapa 5: Operacional e Gestão

> **Objetivo**: Funcionalidades operacionais
> **Prioridade**: MÉDIA
> **Estimativa**: 10-12 dias

### 5.1 CHECKLIST_VISTORIA

- [ ] Criar `ChecklistVistoria` entity
- [ ] Criar `ItemChecklist` entity
- [ ] Templates de checklist
- [ ] UI de preenchimento (touch-friendly)
- [ ] Fotos por item
- [ ] PDF do checklist

### 5.2 MANUTENCAO_PREVENTIVA (Completar)

- [ ] Verificar `docs/manutencaopreventiva.md`
- [ ] Criar `PlanoManutencao` entity
- [ ] Alertas por KM
- [ ] Alertas por tempo
- [ ] Notificação ao cliente
- [ ] Dashboard de veículos próximos

### 5.3 CONTROLE_GARANTIA

- [ ] Criar `Garantia` entity
- [ ] Vincular com OS/Item
- [ ] Período de garantia
- [ ] Alertas de vencimento
- [ ] Processo de acionamento

### 5.4 GESTAO_FORNECEDORES

- [ ] Criar `Fornecedor` entity
- [ ] CRUD completo
- [ ] Histórico de compras
- [ ] Avaliação de fornecedor
- [ ] Catálogo de produtos

### 5.5 ORDEM_COMPRA

- [ ] Criar `OrdemCompra` entity
- [ ] Criar `ItemOrdemCompra` entity
- [ ] Workflow (rascunho → enviada → recebida)
- [ ] Integrar com estoque
- [ ] PDF para envio

### 5.6 CONTROLE_PONTO

- [ ] Criar `RegistroPonto` entity
- [ ] Entrada/Saída
- [ ] Relatório de horas
- [ ] Banco de horas
- [ ] Integração com folha (futuro)

### 5.7 COMISSAO_MECANICOS

- [ ] Configurar % por tipo de serviço
- [ ] Cálculo automático por OS
- [ ] Relatório de comissões
- [ ] Período de apuração

### Status da Etapa 5

| Flag | Status | Data | Observações |
|------|--------|------|-------------|
| CHECKLIST_VISTORIA | ❌ | | |
| MANUTENCAO_PREVENTIVA | ⚠️ | | Doc existe |
| CONTROLE_GARANTIA | ❌ | | |
| GESTAO_FORNECEDORES | ❌ | | |
| ORDEM_COMPRA | ❌ | | |
| CONTROLE_PONTO | ❌ | | |
| COMISSAO_MECANICOS | ❌ | | |

---

## Etapa 6: Relatórios Avançados

> **Objetivo**: Relatórios gerenciais e exportações
> **Prioridade**: MÉDIA
> **Estimativa**: 7-10 dias

### 6.1 RELATORIOS_GERENCIAIS (Completar)

- [ ] Faturamento por período
- [ ] Ticket médio
- [ ] Serviços mais realizados
- [ ] Clientes mais frequentes
- [ ] Performance por mecânico
- [ ] Gráficos com ECharts

### 6.2 PDF_EXPORT_AVANCADO (Completar)

- [ ] Logo da oficina no PDF
- [ ] Cores personalizadas
- [ ] Rodapé customizado
- [ ] Templates por tipo de documento

### 6.3 RELATORIO_FISCAL

- [ ] Livro de entradas
- [ ] Livro de saídas
- [ ] Apuração de impostos
- [ ] Relatório por CFOP

### 6.4 EXPORT_CONTABIL

- [ ] Formato SPED
- [ ] Exportação para contador
- [ ] Plano de contas

### 6.5 RELATORIO_AGENDADO

- [ ] Configurar periodicidade
- [ ] Selecionar relatórios
- [ ] Envio por email
- [ ] Histórico de envios

### 6.6 ANALISE_PREDITIVA

- [ ] Previsão de demanda
- [ ] Sazonalidade
- [ ] Tendências
- [ ] Machine Learning (futuro)

### Status da Etapa 6

| Flag | Status | Data | Observações |
|------|--------|------|-------------|
| RELATORIOS_GERENCIAIS | ⚠️ | | Dashboard básico ok |
| PDF_EXPORT_AVANCADO | ⚠️ | | PDF básico ok |
| RELATORIO_FISCAL | ❌ | | |
| EXPORT_CONTABIL | ❌ | | |
| RELATORIO_AGENDADO | ❌ | | |
| ANALISE_PREDITIVA | ❌ | | |

---

## Etapa 7: Integrações e API

> **Objetivo**: Integrações externas e API pública
> **Prioridade**: BAIXA
> **Estimativa**: 10-15 dias

### 7.1 API_PUBLICA (Verificar)

- [ ] Documentação OpenAPI completa
- [ ] Rate limiting por plano
- [ ] API Keys por oficina
- [ ] Exemplos de uso

### 7.2 WEBHOOK_EVENTOS

- [ ] Configuração de endpoints
- [ ] Eventos disponíveis
- [ ] Assinatura de segurança
- [ ] Retry automático
- [ ] Logs de envio

### 7.3 INTEGRACAO_ZAPIER

- [ ] Criar triggers
- [ ] Criar actions
- [ ] Publicar no Zapier
- [ ] Documentação

### 7.4 INTEGRACAO_ERP

- [ ] Definir ERPs alvo
- [ ] APIs de integração
- [ ] Sync de dados
- [ ] Mapeamento de campos

### 7.5 INTEGRACAO_GOOGLE

- [ ] Google Calendar (agendamentos)
- [ ] Google Drive (backup)
- [ ] Google Sheets (exportação)

### 7.6 INTEGRACAO_MARKETPLACE

- [ ] Mercado Livre (peças)
- [ ] OLX (veículos)
- [ ] APIs de cada plataforma

### 7.7 SSO_SAML

- [ ] Configuração SAML
- [ ] Integração com Azure AD
- [ ] Integração com Google Workspace
- [ ] Mapeamento de roles

### Status da Etapa 7

| Flag | Status | Data | Observações |
|------|--------|------|-------------|
| API_PUBLICA | ✅ | | Swagger existe |
| WEBHOOK_EVENTOS | ❌ | | |
| INTEGRACAO_ZAPIER | ❌ | | |
| INTEGRACAO_ERP | ❌ | | |
| INTEGRACAO_GOOGLE | ❌ | | |
| INTEGRACAO_MARKETPLACE | ❌ | | |
| SSO_SAML | ❌ | | |

---

## Etapa 8: Segurança Avançada

> **Objetivo**: Recursos de segurança
> **Prioridade**: MÉDIA
> **Estimativa**: 5-7 dias

### 8.1 AUTENTICACAO_2FA

- [ ] Escolher método (TOTP, SMS, Email)
- [ ] Integrar biblioteca (Google Authenticator)
- [ ] QR Code para setup
- [ ] Backup codes
- [ ] UI de configuração
- [ ] Forçar 2FA para ADMIN

### 8.2 RESTRICAO_IP

- [ ] Criar `IpWhitelist` entity
- [ ] Configuração por oficina
- [ ] Validação no filter
- [ ] Logs de bloqueio
- [ ] Bypass para SUPER_ADMIN

### 8.3 BACKUP_AUTOMATICO (Verificar)

- [ ] Verificar cron job existente
- [ ] Notificação de backup
- [ ] Retenção configurável
- [ ] Restore via painel

### 8.4 AUDITORIA_AVANCADA (Verificar)

- [ ] Verificar `audit_logs` existente
- [ ] UI de consulta de logs
- [ ] Filtros avançados
- [ ] Exportação de logs

### Status da Etapa 8

| Flag | Status | Data | Observações |
|------|--------|------|-------------|
| AUTENTICACAO_2FA | ❌ | | |
| RESTRICAO_IP | ❌ | | |
| BACKUP_AUTOMATICO | ✅ | | Docker volumes |
| AUDITORIA_AVANCADA | ✅ | | Logs existem |

---

## Etapa 9: Customização e Branding

> **Objetivo**: Personalização visual
> **Prioridade**: BAIXA
> **Estimativa**: 5-7 dias

### 9.1 LOGO_CUSTOMIZADA (Completar)

- [ ] Upload de logo
- [ ] Redimensionamento automático
- [ ] Uso em PDFs
- [ ] Uso no login
- [ ] Favicon personalizado

### 9.2 CORES_CUSTOMIZADAS

- [ ] Seletor de cor primária
- [ ] Seletor de cor secundária
- [ ] Preview em tempo real
- [ ] Persistir no banco
- [ ] CSS variables dinâmicas

### 9.3 DOMINIO_PROPRIO

- [ ] Configuração de domínio
- [ ] Certificado SSL automático
- [ ] Instruções de DNS
- [ ] Validação

### 9.4 EMAIL_CUSTOMIZADO

- [ ] Configuração SMTP próprio
- [ ] Templates com branding
- [ ] Assinatura personalizada

### Status da Etapa 9

| Flag | Status | Data | Observações |
|------|--------|------|-------------|
| LOGO_CUSTOMIZADA | ⚠️ | | Upload ok |
| CORES_CUSTOMIZADAS | ⚠️ | | Dark mode ok |
| DOMINIO_PROPRIO | ⚠️ | | Nginx manual |
| EMAIL_CUSTOMIZADO | ❌ | | |

---

## Etapa 10: Marketing e CRM

> **Objetivo**: Fidelização de clientes
> **Prioridade**: BAIXA
> **Estimativa**: 10-12 dias

### 10.1 CRM_BASICO (Completar)

- [ ] Timeline de interações
- [ ] Anotações por cliente
- [ ] Tags/Segmentação
- [ ] Lembretes

### 10.2 PROGRAMA_FIDELIDADE

- [ ] Criar `PontosFidelidade` entity
- [ ] Regras de acúmulo
- [ ] Regras de resgate
- [ ] Catálogo de prêmios
- [ ] Extrato de pontos

### 10.3 PESQUISA_SATISFACAO

- [ ] Criar `Pesquisa` entity
- [ ] Envio automático após OS
- [ ] NPS (0-10)
- [ ] CSAT
- [ ] Dashboard de satisfação

### 10.4 CUPONS_DESCONTO

- [ ] Criar `Cupom` entity
- [ ] Código único
- [ ] Validade
- [ ] Valor/Percentual
- [ ] Limite de uso
- [ ] Aplicação no checkout

### 10.5 INDICACAO_CLIENTES

- [ ] Código de indicação por cliente
- [ ] Benefício para quem indica
- [ ] Benefício para indicado
- [ ] Rastreamento

### Status da Etapa 10

| Flag | Status | Data | Observações |
|------|--------|------|-------------|
| CRM_BASICO | ⚠️ | | Histórico existe |
| PROGRAMA_FIDELIDADE | ❌ | | |
| PESQUISA_SATISFACAO | ❌ | | |
| CUPONS_DESCONTO | ❌ | | |
| INDICACAO_CLIENTES | ❌ | | |

---

## Etapa 11: Aplicativos Mobile

> **Objetivo**: Apps nativos para clientes e mecânicos
> **Prioridade**: BAIXA (última etapa)
> **Estimativa**: 30-45 dias

### 11.1 Escolher Tecnologia

- [ ] React Native
- [ ] Flutter
- [ ] PWA (já existe parcialmente)

### 11.2 APP_MOBILE_CLIENTE

- [ ] Acompanhar OS em tempo real
- [ ] Histórico de serviços
- [ ] Aprovar orçamento
- [ ] Notificações push
- [ ] Chat com oficina
- [ ] Pagamento online
- [ ] Agendamento

### 11.3 APP_MOBILE_MECANICO

- [ ] Lista de OS atribuídas
- [ ] Atualizar status
- [ ] Registrar tempo
- [ ] Fotos/Anexos
- [ ] Consultar peças
- [ ] Notificações

### 11.4 OFFLINE_MODE

- [ ] Sync local (SQLite/Realm)
- [ ] Queue de operações
- [ ] Sync automático online
- [ ] Resolução de conflitos

### 11.5 QR_CODE_VEICULO

- [ ] Gerar QR por veículo
- [ ] Etiqueta para impressão
- [ ] Scanner no app
- [ ] Acesso rápido ao histórico

### Status da Etapa 11

| Flag | Status | Data | Observações |
|------|--------|------|-------------|
| Tecnologia Mobile | ❌ | | Escolher |
| APP_MOBILE_CLIENTE | ⚠️ | | PWA parcial |
| APP_MOBILE_MECANICO | ⚠️ | | PWA parcial |
| OFFLINE_MODE | ❌ | | |
| QR_CODE_VEICULO | ❌ | | |

---

## Changelog

### 2026-01-14 (Tarde)

**Etapa 1 - Infraestrutura de Gating: CONCLUÍDA ✅**

Backend:
- [x] Criado `@RequiresFeature` annotation para marcar métodos/classes
- [x] Criado `FeatureGateAspect` com AOP (Spring Boot Starter AOP adicionado)
- [x] Criado `FeatureNotEnabledException` com suporte a código, nome e plano
- [x] Adicionado handler no `GlobalExceptionHandler` (HTTP 403 + info de upgrade)

Frontend:
- [x] Criado `FeatureFlagContext` com cache em localStorage
- [x] Criado hooks `useFeatureFlag` e `useFeatureFlagMultiple`
- [x] Criado componente `<FeatureGate>` com fallback e HOC
- [x] Criado componente `<PlanUpgradePrompt>` com 3 variants (card/inline/banner)
- [x] Integrado `FeatureFlagProvider` no `main.tsx`

Arquivos criados/modificados:
- `src/main/java/com/pitstop/shared/security/feature/RequiresFeature.java`
- `src/main/java/com/pitstop/shared/security/feature/FeatureGateAspect.java`
- `src/main/java/com/pitstop/shared/security/feature/FeatureNotEnabledException.java`
- `src/main/java/com/pitstop/shared/exception/GlobalExceptionHandler.java` (modificado)
- `pom.xml` (adicionado spring-boot-starter-aop)
- `frontend/src/shared/contexts/FeatureFlagContext.tsx`
- `frontend/src/shared/hooks/useFeatureFlag.ts`
- `frontend/src/shared/components/FeatureGate.tsx`
- `frontend/src/shared/components/PlanUpgradePrompt.tsx`
- `frontend/src/shared/contexts/index.ts` (modificado)
- `frontend/src/main.tsx` (modificado)

### 2026-01-14 (Manhã)

- [x] Documento criado
- [x] Estrutura de etapas definida
- [x] Status inicial mapeado
- [x] Prioridades definidas

### Template de Atualização

```markdown
### YYYY-MM-DD

- [x] Descrição do que foi implementado
- [x] Arquivos criados/modificados
- [ ] Pendências identificadas
```

---

## Métricas de Progresso

| Etapa | Total | Feito | % |
|-------|-------|-------|---|
| 1. Infraestrutura | 10 | 9 | 90% |
| 2. Comunicação | 6 | 0 | 0% |
| 3. Financeiro | 8 | 0 | 0% |
| 4. Nota Fiscal | 6 | 0 | 0% |
| 5. Operacional | 7 | 0 | 0% |
| 6. Relatórios | 6 | 0 | 0% |
| 7. Integrações | 7 | 2 | 29% |
| 8. Segurança | 4 | 2 | 50% |
| 9. Customização | 4 | 0 | 0% |
| 10. Marketing | 5 | 0 | 0% |
| 11. Mobile | 5 | 0 | 0% |
| **TOTAL** | **68** | **13** | **19%** |

---

## Próximos Passos

1. ~~**Etapa 1** - Infraestrutura de Gating~~ ✅ CONCLUÍDA
2. **Priorizar Etapa 2** - Comunicação (WhatsApp, Telegram já parcial)
3. **Priorizar Etapa 3** - Mercado Pago (receita)
4. **Priorizar Etapa 4** - NF-e (compliance)

---

> **Nota**: Este documento deve ser atualizado ao final de cada sessão de trabalho.
