# Sistema de Cobrança SaaS - Plano de Implementação

**Data:** 2026-01-21
**Status:** Em Implementação
**Versão:** 1.0

## Visão Geral

Este documento detalha a implementação do sistema de cobrança completo entre SUPER_ADMIN e oficinas no PitStop.

---

## Arquitetura do Sistema

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           SUPER_ADMIN                                    │
│                                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │   Dashboard  │  │   Faturas    │  │ Inadimplência│  │  Relatórios │ │
│  │   /admin     │  │/admin/faturas│  │/admin/inadim.│  │/admin/relat.│ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
            │  Scheduled  │ │   Email     │ │  Mercado    │
            │    Jobs     │ │   Service   │ │   Pago      │
            └─────────────┘ └─────────────┘ └─────────────┘
                    │               │               │
                    └───────────────┼───────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                             OFICINA                                      │
│                                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │  Meu Plano   │  │Minhas Faturas│  │   Pagar      │  │  Histórico  │ │
│  │  /meu-plano  │  │  /faturas    │  │/faturas/{id} │  │  Pagamentos │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Fluxo de Cobrança Automatizado

```
Dia 1 do Mês (00:05)
        │
        ▼
┌───────────────────┐
│ Job: Gerar        │
│ Faturas Mensais   │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐     ┌───────────────────┐
│ Criar Fatura      │────▶│ Enviar Email      │
│ Status: PENDENTE  │     │ "Nova Fatura"     │
└─────────┬─────────┘     └───────────────────┘
          │
          ▼
┌───────────────────┐
│ Oficina Acessa    │
│ /faturas          │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐     ┌───────────────────┐
│ Clica "Pagar"     │────▶│ Mercado Pago      │
│                   │     │ Checkout/PIX      │
└─────────┬─────────┘     └─────────┬─────────┘
          │                         │
          │    ┌────────────────────┘
          │    │
          ▼    ▼
┌───────────────────┐     ┌───────────────────┐
│ Webhook MP        │────▶│ Status: PAGO      │
│ payment.approved  │     │ Email Confirmação │
└───────────────────┘     └───────────────────┘

        OU (se não pagar)

Dia 5 (antes vencimento)
        │
        ▼
┌───────────────────┐
│ Email: Lembrete   │
│ "Fatura vence em  │
│  5 dias"          │
└───────────────────┘

Dia 10 (vencimento)
        │
        ▼
┌───────────────────┐
│ Status: VENCIDO   │
│ Email: "Fatura    │
│ vencida"          │
└───────────────────┘

Dia 15 (5 dias após)
        │
        ▼
┌───────────────────┐
│ Oficina SUSPENSA  │
│ Email: "Conta     │
│ suspensa"         │
└───────────────────┘
```

---

## 1. Backend - Novos Endpoints para Oficinas

### Controller: OficinaFaturaController

```
GET  /api/minha-conta/faturas              Lista faturas da oficina logada
GET  /api/minha-conta/faturas/{id}         Detalhe de uma fatura
GET  /api/minha-conta/faturas/{id}/pdf     Download PDF da fatura
POST /api/minha-conta/faturas/{id}/pagar   Iniciar pagamento (retorna link MP)
GET  /api/minha-conta/resumo               Resumo financeiro
GET  /api/minha-conta/pagamentos           Histórico de pagamentos
```

### Arquivos a Criar:
- `src/main/java/com/pitstop/saas/controller/OficinaFaturaController.java`
- `src/main/java/com/pitstop/saas/dto/MinhaContaResumoDTO.java`
- `src/main/java/com/pitstop/saas/dto/FaturaOficinaDTO.java`

---

## 2. Scheduled Jobs

### Adicionar ao SaasScheduledJobs.java:

```java
// Gerar faturas mensais - Dia 1 às 00:05
@Scheduled(cron = "0 5 0 1 * *")
public void gerarFaturasMensais() {
    faturaService.gerarFaturasMensais();
}

// Lembrete de vencimento - 5 dias antes - Diário às 09:00
@Scheduled(cron = "0 0 9 * * *")
public void enviarLembretesVencimento() {
    faturaService.enviarLembretesVencimento(5);
}

// Marcar faturas vencidas - Diário às 00:30
@Scheduled(cron = "0 30 0 * * *")
public void processarFaturasVencidas() {
    faturaService.processarFaturasVencidas();
}

// Enviar notificação de vencido - Diário às 09:30
@Scheduled(cron = "0 30 9 * * *")
public void notificarFaturasVencidas() {
    faturaService.notificarFaturasVencidas();
}
```

---

## 3. Emails de Cobrança

### Templates a Criar:

| Template | Quando Enviar | Assunto |
|----------|---------------|---------|
| `FATURA_GERADA` | Dia 1 | "Sua fatura PitStop - {mes}/{ano}" |
| `FATURA_LEMBRETE` | 5 dias antes | "Lembrete: Fatura vence em 5 dias" |
| `FATURA_VENCIDA` | Dia do vencimento | "Fatura vencida - Regularize" |
| `FATURA_PAGA` | Após pagamento | "Pagamento confirmado" |
| `CONTA_SUSPENSA` | Após suspensão | "Conta suspensa por inadimplência" |

### Arquivos:
- `src/main/resources/templates/email/fatura-gerada.html`
- `src/main/resources/templates/email/fatura-lembrete.html`
- `src/main/resources/templates/email/fatura-vencida.html`
- `src/main/resources/templates/email/fatura-paga.html`

---

## 4. Integração Mercado Pago para Faturas SaaS

### Fluxo:
1. Oficina clica "Pagar" na fatura
2. Backend cria preferência MP com dados da fatura
3. Retorna `init_point` (URL checkout) ou `qr_code` (PIX)
4. Oficina paga
5. Webhook recebe notificação
6. Atualiza status da fatura para PAGO

### Endpoint Webhook:
- `POST /api/webhooks/mercadopago/fatura` (público, com validação de assinatura)

---

## 5. Frontend - Páginas para Oficinas

### Rotas:
```tsx
/minha-conta                    // Dashboard financeiro
/minha-conta/faturas            // Lista de faturas
/minha-conta/faturas/:id        // Detalhe da fatura + botão pagar
/minha-conta/faturas/:id/pagar  // Página de pagamento (checkout MP)
/minha-conta/pagamentos         // Histórico de pagamentos
```

### Componentes:
```
frontend/src/features/minha-conta/
├── pages/
│   ├── MinhaContaPage.tsx           # Dashboard
│   ├── MinhasFaturasPage.tsx        # Lista de faturas
│   ├── FaturaDetalhePage.tsx        # Detalhe + pagar
│   └── HistoricoPagamentosPage.tsx  # Histórico
├── components/
│   ├── FaturaCard.tsx               # Card de fatura
│   ├── ResumoFinanceiro.tsx         # Resumo
│   ├── BotaoPagar.tsx               # Botão com loading
│   └── StatusFaturaBadge.tsx        # Badge de status
├── hooks/
│   └── useMinhasFaturas.ts          # React Query hooks
├── services/
│   └── minhaContaService.ts         # API calls
└── types/
    └── index.ts                     # TypeScript types
```

---

## 6. Banco de Dados

### Alterações Necessárias:

Já existe a tabela `faturas`. Verificar campos:
- `link_pagamento` - URL do checkout MP
- `qr_code_pix` - QR Code para PIX
- `preference_id` - ID da preferência MP
- `payment_id` - ID do pagamento MP

### Migration (se necessário):
```sql
-- V075__add_mercadopago_fields_to_faturas.sql
ALTER TABLE faturas ADD COLUMN IF NOT EXISTS preference_id VARCHAR(100);
ALTER TABLE faturas ADD COLUMN IF NOT EXISTS external_reference VARCHAR(100);
ALTER TABLE faturas ADD COLUMN IF NOT EXISTS qr_code_base64 TEXT;
ALTER TABLE faturas ADD COLUMN IF NOT EXISTS qr_code_text VARCHAR(500);
```

---

## 7. Variáveis de Ambiente

### Adicionar ao .env:
```env
# Mercado Pago - Faturas SaaS
MERCADOPAGO_ACCESS_TOKEN=APP_USR-xxx
MERCADOPAGO_PUBLIC_KEY=APP_USR-xxx
MERCADOPAGO_WEBHOOK_SECRET=xxx

# URLs de retorno
MERCADOPAGO_SUCCESS_URL=https://app.pitstopai.com.br/minha-conta/faturas?status=success
MERCADOPAGO_FAILURE_URL=https://app.pitstopai.com.br/minha-conta/faturas?status=failure
MERCADOPAGO_PENDING_URL=https://app.pitstopai.com.br/minha-conta/faturas?status=pending
```

---

## 8. Segurança

### Endpoints Oficina:
- Autenticação obrigatória
- Oficina só vê SUAS faturas (filtro por `TenantContext`)
- Não pode alterar status diretamente

### Webhook MP:
- Validação de assinatura obrigatória
- Rate limiting
- Idempotência (não processar mesmo pagamento 2x)

---

## 9. Cronograma de Implementação

### Fase 1: Backend Core (Atual)
- [x] Documento de planejamento
- [ ] Controller OficinaFaturaController
- [ ] DTOs para oficina
- [ ] Service methods para oficina

### Fase 2: Scheduled Jobs
- [ ] Job geração mensal
- [ ] Job lembretes
- [ ] Job vencidos

### Fase 3: Emails
- [ ] Templates HTML
- [ ] Service de envio
- [ ] Integração com jobs

### Fase 4: Mercado Pago
- [ ] Criar preferência para fatura
- [ ] Webhook handler
- [ ] Atualização automática status

### Fase 5: Frontend
- [ ] Pages e components
- [ ] Hooks e services
- [ ] Rotas e menu

### Fase 6: Testes e Deploy
- [ ] Testes manuais
- [ ] Deploy staging
- [ ] Deploy produção

---

## 10. Checklist de Validação

### Backend:
- [ ] Oficina consegue listar suas faturas
- [ ] Oficina consegue ver detalhe da fatura
- [ ] Oficina consegue iniciar pagamento
- [ ] Webhook processa pagamento corretamente
- [ ] Status atualiza automaticamente
- [ ] Emails são enviados nos momentos certos

### Frontend:
- [ ] Página de faturas carrega corretamente
- [ ] Filtros funcionam (status, período)
- [ ] Botão pagar abre checkout MP
- [ ] Feedback visual de loading/sucesso/erro
- [ ] Responsivo (mobile-first)
- [ ] Dark mode suportado

### Automação:
- [ ] Faturas geradas automaticamente dia 1
- [ ] Lembretes enviados 5 dias antes
- [ ] Vencidas marcadas corretamente
- [ ] Suspensão funciona após grace period

---

## Referências

- [Mercado Pago SDK Java](https://github.com/mercadopago/sdk-java)
- [Mercado Pago Webhooks](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks)
- Código existente: `MercadoPagoService.java`, `PagamentoOnlineController.java`
