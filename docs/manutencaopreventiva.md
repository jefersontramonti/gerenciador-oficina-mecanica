# SISTEMA COMPLETO DE MANUTENÇÃO PREVENTIVA - PITSTOP

> **STATUS: ✅ IMPLEMENTADO**
> Data de conclusão: 2026-01-17
>
> Este módulo está **100% funcional** e inclui:
> - 41 arquivos backend (entities, services, controllers, scheduler)
> - 11 páginas frontend (dashboard, planos, templates, agendamentos, calendário)
> - Notificações multicanal (WhatsApp, Email, Telegram)
> - Agendamento personalizado de notificações
> - Criação automática de OS quando manutenção vence
> - Atualização em tempo real via WebSocket

---

## CONTEXTO DO PROJETO

Você está trabalhando no **PitStop**, um sistema SaaS de gestão de oficinas mecânicas.

**Stack Técnico:**
- Backend: Java 25 LTS + Spring Boot 3.5.7
- Frontend: React 19 + TypeScript 5.9 + Vite
- Database: PostgreSQL 16 + Liquibase para migrations
- Cache: Redis 7.x
- State Management: Redux Toolkit + React Query
- UI: Tailwind CSS + Shadcn/ui components
- Notifications: WhatsApp (Evolution API), Email, Telegram
- Architecture: Monolito Modular + SPA

**Estrutura Backend:**
```
src/main/java/com/pitstop/
├── config/                    # Spring configs
├── shared/                    # Infrastructure
│   ├── security/
│   ├── exception/
│   └── audit/
├── cliente/                   # Módulo clientes
├── veiculo/                   # Módulo veículos
├── ordemservico/              # Módulo OS
└── [novo] manutencaopreventiva/  # CRIAR ESTE MÓDULO
```

**Estrutura Frontend:**
```
frontend/src/
├── features/
│   ├── clientes/
│   ├── veiculos/
│   ├── ordens-servico/
│   └── [novo] manutencao-preventiva/  # CRIAR ESTE MÓDULO
├── shared/
│   ├── components/
│   ├── services/
│   └── hooks/
```

## OBJETIVO - SISTEMA DE MANUTENÇÃO PREVENTIVA

Criar um sistema completo que:

1. **Permita criar planos de manutenção preventiva por veículo**
2. **Gere alertas automáticos baseados em critérios configuráveis**
3. **Envie notificações multicanal (WhatsApp, Email, Push)**
4. **Tenha calendário visual de próximas manutenções**
5. **Automatize agendamentos e lembretes**
6. **Aumente retenção e recorrência de clientes**

---

## REQUISITOS FUNCIONAIS DETALHADOS

### 1. BACKEND - DATABASE (Liquibase Migration)

**Criar arquivo:** `src/main/resources/db/changelog/migrations/V061__create_manutencao_preventiva_tables.sql`

**Tabelas necessárias:**

#### **A) `planos_manutencao_preventiva`**
- Planos individuais vinculados a veículos
- Critérios: TEMPO (dias), KM (quilometragem), AMBOS
- Status: ATIVO, PAUSADO, CONCLUIDO, VENCIDO
- Campos de previsão (próxima data, próximo km)
- Campos de controle de alertas

#### **B) `templates_manutencao`**
- Templates pré-configurados reutilizáveis
- Exemplos: "Troca de Óleo 10.000km", "Revisão 6 meses"
- Podem ser globais (oficina_id NULL) ou por oficina
- Contém checklist, peças sugeridas, valor estimado

#### **C) `historico_manutencao_preventiva`**
- Log de execuções das manutenções
- Link para ordem_servico_id quando executado
- Tracking de km e data na execução
- Observações do mecânico

#### **D) `alertas_manutencao`**
- Fila de alertas a enviar
- Status: PENDENTE, ENVIADO, FALHOU, CANCELADO
- Canais: WHATSAPP, EMAIL, SMS, PUSH, INTERNO
- Retry automático em falhas

#### **E) `agendamentos_manutencao`**
- Agendamentos de manutenções futuras
- Status: AGENDADO, CONFIRMADO, REMARCADO, CANCELADO, REALIZADO
- Data/hora do agendamento
- Confirmação do cliente (token único)

**Relacionamentos:**
- planos → veiculos (N:1)
- planos → templates (N:1, opcional)
- historico → planos (N:1)
- historico → ordem_servico (N:1, opcional)
- alertas → planos (N:1)
- agendamentos → planos (N:1)
- agendamentos → veiculos (N:1)

---

### 2. BACKEND - DOMAIN MODEL (Entities)

**Criar no pacote:** `com.pitstop.manutencaopreventiva.domain`

**Entities necessárias:**

#### **PlanoManutencaoPreventiva.java**
```
- Critérios configuráveis (dias, km, ambos)
- Cálculo automático de próxima manutenção
- Status com máquina de estados
- Soft delete (ativo boolean)
- Multi-tenant (oficinaId)
```

#### **TemplateManutencao.java**
```
- Templates reutilizáveis
- Checklist em JSONB
- Peças sugeridas
- Valor estimado
```

#### **HistoricoManutencaoPreventiva.java**
```
- Registro de execuções
- Link com OS
- KM e data na execução
```

#### **AlertaManutencao.java**
```
- Alertas pendentes/enviados
- Multicanal
- Retry logic
```

#### **AgendamentoManutencao.java**
```
- Agendamentos futuros
- Confirmação cliente
- Remarcação
```

**Enums:**
- `CriterioManutencao`: TEMPO, KM, AMBOS
- `StatusPlanoManutencao`: ATIVO, PAUSADO, CONCLUIDO, VENCIDO
- `StatusAlerta`: PENDENTE, ENVIADO, FALHOU, CANCELADO
- `CanalNotificacao`: WHATSAPP, EMAIL, SMS, PUSH, INTERNO
- `StatusAgendamento`: AGENDADO, CONFIRMADO, REMARCADO, CANCELADO, REALIZADO

---

### 3. BACKEND - REPOSITORIES

**Criar no pacote:** `com.pitstop.manutencaopreventiva.repository`

**Queries necessárias:**
```java
// PlanoManutencaoRepository
- findByOficinaIdAndAtivoTrue()
- findByVeiculoIdAndAtivoTrue()
- findByStatusAndProximaPrevisaoDataBefore(date) // Vencidos
- findPlanosParaAlertar(date, km) // Próximos de vencer

// AlertaManutencaoRepository
- findByStatusAndTentativasLessThan() // Retry
- findPendentesParaEnvio()

// AgendamentoManutencaoRepository
- findByOficinaIdAndDataAgendamentoBetween(start, end) // Calendário
- findByTokenConfirmacao(token) // Confirmação cliente
- findProximosAgendamentos(oficinaId, dias)
```

---

### 4. BACKEND - SERVICES

**Criar no pacote:** `com.pitstop.manutencaopreventiva.service`

#### **PlanoManutencaoService.java**
```java
Funcionalidades:
- criar(PlanoDTO) → Criar plano individual
- criarAPartirDeTemplate(veiculoId, templateId) → Template → Plano
- atualizar(id, PlanoDTO)
- ativar/pausar/concluir(id) → Transições de estado
- calcularProximaManutencao(plano) → Lógica de cálculo
- buscarVencidos() → Planos que passaram data/km
- buscarProximosAVencer(dias, km) → Para alertar
- registrarExecucao(planoId, ordemServicoId, km, data) → Atualizar após execução
```

#### **TemplateManutencaoService.java**
```java
- listarTemplatesGlobais()
- listarTemplatesDaOficina(oficinaId)
- criar/atualizar/deletar templates
- aplicarTemplateEmVeiculo(templateId, veiculoId)
```

#### **AlertaManutencaoService.java**
```java
- gerarAlertasAutomaticos() → Job agendado
- enviarAlerta(alertaId) → Dispatch por canal
- processarFilaDeAlertas() → Retry logic
- marcarComoEnviado/Falhou(alertaId)
```

#### **AgendamentoManutencaoService.java**
```java
- criar(AgendamentoDTO)
- confirmarAgendamento(token) → Cliente confirma
- remarcar(id, novaData)
- cancelar(id, motivo)
- buscarAgendamentosDoDia(oficinaId, data)
- buscarAgendamentosMes(oficinaId, mes, ano) → Para calendário
```

#### **NotificacaoManutencaoService.java**
```java
- enviarWhatsApp(destinatario, mensagem)
- enviarEmail(destinatario, assunto, corpo)
- enviarSMS(destinatario, mensagem)
- enviarNotificacaoInterna(usuarioId, mensagem)
- escolherMelhorCanal(cliente) → Preferência + disponibilidade
```

---

### 5. BACKEND - SCHEDULED JOBS

**Criar no pacote:** `com.pitstop.manutencaopreventiva.scheduler`

#### **ManutencaoPreventivaScheduler.java**
```java
@Scheduled Jobs necessários:

1. verificarManutencoesPendentes()
   - Cron: "0 0 8 * * ?" (todo dia 8h)
   - Busca planos próximos de vencer
   - Gera alertas automaticamente

2. enviarAlertasDoDia()
   - Cron: "0 0 9,14 * * ?" (9h e 14h)
   - Processa fila de alertas
   - Envia por WhatsApp/Email

3. atualizarStatusPlanos()
   - Cron: "0 0 1 * * ?" (1h da manhã)
   - Marca planos como VENCIDO se passou prazo
   - Atualiza próximas previsões

4. lembretesAgendamentosDoDia()
   - Cron: "0 0 7 * * ?" (7h)
   - Lembra agendamentos do dia
   - Envia confirmação para clientes

5. processarAlertasFalhados()
   - Cron: "0 */30 * * * ?" (a cada 30min)
   - Retry de alertas falhados
   - Máximo 3 tentativas
```

---

### 6. BACKEND - REST CONTROLLERS

**Criar no pacote:** `com.pitstop.manutencaopreventiva.controller`

#### **PlanoManutencaoController.java**
```java
Endpoints:

GET    /api/manutencao-preventiva/planos
       - Listar planos (filtros: veiculoId, status, dataInicio, dataFim)
       - Paginado

POST   /api/manutencao-preventiva/planos
       - Criar plano novo

GET    /api/manutencao-preventiva/planos/{id}
       - Detalhes do plano

PUT    /api/manutencao-preventiva/planos/{id}
       - Atualizar plano

DELETE /api/manutencao-preventiva/planos/{id}
       - Soft delete (ativo = false)

PATCH  /api/manutencao-preventiva/planos/{id}/ativar
PATCH  /api/manutencao-preventiva/planos/{id}/pausar
PATCH  /api/manutencao-preventiva/planos/{id}/concluir

POST   /api/manutencao-preventiva/planos/{id}/executar
       - Registra execução (atualiza última manutenção)
       - Pode criar OS automaticamente

GET    /api/manutencao-preventiva/planos/veiculo/{veiculoId}
       - Todos planos de um veículo

GET    /api/manutencao-preventiva/planos/vencidos
       - Planos que passaram data/km

GET    /api/manutencao-preventiva/planos/proximos-vencer
       - Query params: dias=15, km=1000
```

#### **TemplateManutencaoController.java**
```java
GET    /api/manutencao-preventiva/templates
POST   /api/manutencao-preventiva/templates
GET    /api/manutencao-preventiva/templates/{id}
PUT    /api/manutencao-preventiva/templates/{id}
DELETE /api/manutencao-preventiva/templates/{id}

POST   /api/manutencao-preventiva/templates/{id}/aplicar
       - Body: { veiculoId }
       - Cria plano a partir do template
```

#### **AgendamentoManutencaoController.java**
```java
GET    /api/manutencao-preventiva/agendamentos
       - Filtros: dataInicio, dataFim, status
       
POST   /api/manutencao-preventiva/agendamentos
       - Criar agendamento

GET    /api/manutencao-preventiva/agendamentos/{id}

PUT    /api/manutencao-preventiva/agendamentos/{id}

DELETE /api/manutencao-preventiva/agendamentos/{id}

PATCH  /api/manutencao-preventiva/agendamentos/{id}/confirmar
       - Confirmação manual (admin/atendente)

GET    /api/manutencao-preventiva/agendamentos/confirmar/{token}
       - Confirmação pública (cliente via link)
       - Retorna página de sucesso

PATCH  /api/manutencao-preventiva/agendamentos/{id}/remarcar
       - Body: { novaData, novaHora }

GET    /api/manutencao-preventiva/agendamentos/calendario
       - Query: mes=1, ano=2026
       - Retorna eventos para calendário
```

#### **DashboardManutencaoController.java**
```java
GET    /api/manutencao-preventiva/dashboard/estatisticas
       - Totais: ativos, vencidos, próximos 30 dias
       - Taxa de adesão
       - Taxa de execução

GET    /api/manutencao-preventiva/dashboard/proximas-manutencoes
       - Próximas 10 manutenções

GET    /api/manutencao-preventiva/dashboard/alertas-pendentes
       - Alertas não enviados

GET    /api/manutencao-preventiva/dashboard/agendamentos-hoje
       - Agendamentos do dia
```

---

### 7. FRONTEND - ESTRUTURA DE PÁGINAS

**Criar em:** `frontend/src/features/manutencao-preventiva/`

#### **Páginas necessárias:**
```typescript
1. /manutencao-preventiva
   - Lista de planos com filtros
   - Cards: Ativos, Vencidos, Próximos (badges coloridos)
   - Botão: Novo Plano

2. /manutencao-preventiva/novo
   - Form: Selecionar veículo
   - Opção 1: Criar do zero
   - Opção 2: Usar template
   - Critérios: dias, km, ambos
   - Antecedência de alertas
   - Canais de notificação

3. /manutencao-preventiva/:id
   - Detalhes do plano
   - Timeline de histórico
   - Botões: Editar, Pausar, Executar
   - Próxima previsão destacada

4. /manutencao-preventiva/:id/editar
   - Form igual ao criar
   - Pre-populado

5. /manutencao-preventiva/templates
   - Lista de templates
   - Cards com preview
   - Botões: Usar, Editar, Criar Novo

6. /manutencao-preventiva/templates/novo
   - Form template
   - Checklist builder
   - Peças sugeridas (autocomplete)

7. /manutencao-preventiva/agendamentos
   - Calendário visual (mês)
   - Lista lateral com agendamentos
   - Filtros: status, tipo

8. /manutencao-preventiva/agendamentos/novo
   - Selecionar plano ou veículo
   - Date picker + time picker
   - Observações
   - Enviar confirmação (checkbox)

9. /manutencao-preventiva/dashboard
   - Cards estatísticas
   - Gráficos:
     * Manutenções por mês (line chart)
     * Taxa de execução (gauge)
     * Tipos de manutenção (pie chart)
   - Lista: Próximas 10 manutenções
   - Lista: Agendamentos hoje
```

---

### 8. FRONTEND - COMPONENTES

**Criar em:** `frontend/src/features/manutencao-preventiva/components/`
```typescript
Componentes necessários:

1. PlanoCard.tsx
   - Card visual do plano
   - Badge de status (verde/amarelo/vermelho)
   - Ícone do tipo de manutenção
   - Próxima data/km destacado
   - Ações quick: Pausar, Executar

2. PlanoForm.tsx
   - Form completo criar/editar
   - Select veículo (com autocomplete)
   - Radio: Critério (tempo/km/ambos)
   - Inputs numéricos validados
   - Toggle canais de notificação

3. TemplateCard.tsx
   - Preview do template
   - Checklist resumido
   - Valor estimado
   - Botão: Usar Template

4. CalendarioManutencoes.tsx
   - Calendário visual (react-big-calendar ou similar)
   - Eventos coloridos por status
   - Click evento → Modal detalhes
   - Navegação mês/semana/dia

5. AgendamentoModal.tsx
   - Modal criar/editar agendamento
   - Date/time picker
   - Cliente (readonly)
   - Observações
   - Botões: Salvar, Cancelar

6. TimelineHistorico.tsx
   - Timeline vertical
   - Cada execução com data/km/OS
   - Expandir para ver detalhes
   - Link para OS

7. DashboardStats.tsx
   - Grid de cards estatísticas
   - Números grandes
   - Indicadores (↑↓)
   - Links para filtros

8. ProximasManutencoesList.tsx
   - Lista próximas 10
   - Item: Veículo, Tipo, Previsão, Dias faltando
   - Badge urgência (< 7 dias = vermelho)
   - Click → Detalhes

9. AlertasPanel.tsx
   - Badge com contador pendentes
   - Dropdown com lista
   - Ações: Enviar agora, Cancelar
```

---

### 9. FRONTEND - SERVICES & HOOKS

**Criar em:** `frontend/src/features/manutencao-preventiva/services/`
```typescript
manutencaoService.ts:
- listarPlanos(filtros)
- criarPlano(dados)
- buscarPlano(id)
- atualizarPlano(id, dados)
- deletarPlano(id)
- ativarPlano(id)
- pausarPlano(id)
- executarPlano(id, dados)
- listarTemplates()
- criarTemplate(dados)
- aplicarTemplate(templateId, veiculoId)
- listarAgendamentos(filtros)
- criarAgendamento(dados)
- confirmarAgendamento(token)
- remarcarAgendamento(id, novaData)
- buscarEstatisticas()
- buscarAgendamentosCalendario(mes, ano)
```

**Criar em:** `frontend/src/features/manutencao-preventiva/hooks/`
```typescript
useManutencaoPreventiva.ts:
- useListarPlanos(filtros)
- useCriarPlano()
- useBuscarPlano(id)
- useAtualizarPlano()
- useExecutarPlano()
- useTemplates()
- useAgendamentos(filtros)
- useCriarAgendamento()
- useEstatisticas()
- useCalendarioAgendamentos(mes, ano)

Todos usando React Query (TanStack Query)
Cache, invalidação automática, optimistic updates
```

---

### 10. NOTIFICAÇÕES - TEMPLATES

**Criar templates de mensagem em:**
`src/main/resources/templates/notificacoes/manutencao/`

#### **WhatsApp Templates:**
```
1. alerta-manutencao-proximidade.txt
"Olá {{nomeCliente}}! 🔧

Seu {{veiculoModelo}} ({{placa}}) está próximo da 
{{tipoManutencao}}!

📅 Previsão: {{dataPrevisao}}
🛞 KM atual: {{kmAtual}} / {{kmPrevisao}}

Agende agora: {{linkAgendamento}}

Oficina {{nomeOficina}}
{{telefoneOficina}}"

2. alerta-manutencao-vencida.txt
"⚠️ Atenção {{nomeCliente}}!

A {{tipoManutencao}} do seu {{veiculoModelo}} 
está VENCIDA desde {{diasVencidos}} dias!

É importante realizar o quanto antes.

Agende: {{linkAgendamento}}

Oficina {{nomeOficina}}"

3. confirmacao-agendamento.txt
"✅ Agendamento confirmado!

{{nomeCliente}}, sua {{tipoManutencao}} está 
agendada para:

📅 {{dataAgendamento}}
🕐 {{horaAgendamento}}

Endereço: {{enderecoOficina}}

Para remarcar: {{linkRemarcar}}

Até lá! 👋"

4. lembrete-agendamento-dia.txt
"🔔 Lembrete!

{{nomeCliente}}, sua manutenção é hoje às {{horaAgendamento}}!

{{tipoManutencao}} - {{veiculoModelo}}

Nos vemos em breve! 😊

Oficina {{nomeOficina}}"
```

#### **Email Templates (Thymeleaf):**
```html
1. alerta-manutencao-proximidade.html
- Header com logo
- Card veículo
- Informações da manutenção
- Botão CTA: Agendar Agora
- Footer oficina

2. confirmacao-agendamento.html
- Header
- Card confirmação
- Adicionar ao Google Calendar (button)
- Mapa localização
- Footer

3. relatorio-mensal.html
- Header
- Manutenções realizadas no mês
- Próximas previstas
- Gráfico simples
- Footer
```

---

### 11. INTEGRAÇÃO COM MÓDULOS EXISTENTES

**Modificações necessárias:**

#### **A) Módulo Veículo**
```java
VeiculoService.java:
- Adicionar método: atualizarQuilometragem(veiculoId, novoKm)
  * Chamado ao criar/atualizar OS
  * Trigger cálculo de próximas manutenções

VeiculoDTO.java:
- Adicionar campo: planosManuteção (lista resumida)
- Frontend mostra badge: "3 planos ativos"
```

#### **B) Módulo Ordem de Serviço**
```java
OrdemServicoService.java:
- Ao criar OS, verificar se é execução de plano preventivo
- Adicionar campo opcional: planoManutencaoId
- Ao finalizar OS, se vinculado a plano:
  * Atualizar última execução
  * Recalcular próxima manutenção
  * Criar entrada no histórico

OrdemServicoDTO.java:
- Adicionar: planoManutencaoId (opcional)
- Adicionar: origemManutencaoPreventiva (boolean)
```

#### **C) Módulo Dashboard**
```java
DashboardService.java:
- Adicionar card: Manutenções Preventivas
  * Ativos: X
  * Vencidos: Y (destaque vermelho)
  * Agendamentos hoje: Z

Frontend dashboard:
- Novo card visual
- Link para /manutencao-preventiva
```

#### **D) Módulo Cliente**
```java
ClienteDetailDTO.java:
- Adicionar: veículosComPlanos (lista)
- Frontend mostra quais veículos têm manutenção ativa

ClienteService.java:
- Método: buscarProximasManutencoes(clienteId)
```

---

### 12. TESTES

**Criar testes em:**

#### **Backend:**
```
src/test/java/com/pitstop/manutencaopreventiva/

- PlanoManutencaoServiceTest.java
  * Testa cálculo próxima manutenção
  * Testa transições de estado
  * Testa registro de execução

- AlertaManutencaoServiceTest.java
  * Testa geração automática de alertas
  * Testa retry logic
  * Mock notificações

- AgendamentoManutencaoServiceTest.java
  * Testa criação/confirmação
  * Testa remarcação
  * Testa conflitos de horário

- ManutencaoPreventivaSchedulerTest.java
  * Testa jobs agendados
  * Mock tempo
```

#### **Frontend:**
```
src/features/manutencao-preventiva/__tests__/

- PlanoForm.test.tsx
- CalendarioManutencoes.test.tsx
- useManutencaoPreventiva.test.ts
```

---

### 13. DOCUMENTAÇÃO

**Criar:**
```markdown
docs/MANUTENCAO_PREVENTIVA.md
- Visão geral do sistema
- Fluxos de uso
- Regras de negócio
- Cálculo de próximas manutenções
- Configuração de jobs
- Troubleshooting

docs/API_MANUTENCAO_PREVENTIVA.md
- Documentação completa dos endpoints
- Exemplos de request/response
- Códigos de erro

docs/TEMPLATES_NOTIFICACAO.md
- Lista de templates
- Variáveis disponíveis
- Como personalizar
```

---

### 14. CONFIGURAÇÕES

**Adicionar em:** `application.properties`
```properties
# Manutenção Preventiva
manutencao.alertas.antecedencia-dias-padrao=15
manutencao.alertas.antecedencia-km-padrao=1000
manutencao.alertas.max-tentativas=3
manutencao.alertas.intervalo-retry-minutos=30

# Jobs
manutencao.jobs.verificar-pendentes.cron=0 0 8 * * ?
manutencao.jobs.enviar-alertas.cron=0 0 9,14 * * ?
manutencao.jobs.atualizar-status.cron=0 0 1 * * ?
manutencao.jobs.lembretes-agendamentos.cron=0 0 7 * * ?
manutencao.jobs.processar-falhados.cron=0 */30 * * * ?

# Templates padrão (criar no startup)
manutencao.templates.carregar-padrao=true
```

---

### 15. FEATURE FLAGS

**Adicionar feature flag:**
```sql
INSERT INTO feature_flags (nome, descricao, habilitado, planos_permitidos) 
VALUES (
  'MANUTENCAO_PREVENTIVA',
  'Sistema de Manutenção Preventiva com alertas e agendamentos',
  true,
  ARRAY['PROFISSIONAL', 'TURBINADO']
);
```

**No backend:**
```java
@PreAuthorize("hasFeature('MANUTENCAO_PREVENTIVA')")
```

---

## INSTRUÇÕES FINAIS

**Ordem de implementação sugerida:**

1. ✅ **Database** - Criar migrations completas
2. ✅ **Domain** - Entities, enums, repositórios
3. ✅ **Services** - Lógica de negócio
4. ✅ **Scheduled Jobs** - Jobs agendados
5. ✅ **Controllers** - REST APIs
6. ✅ **Frontend Services** - API client
7. ✅ **Frontend Components** - Componentes reutilizáveis
8. ✅ **Frontend Pages** - Páginas completas
9. ✅ **Integrations** - Modificar módulos existentes
10. ✅ **Templates** - Notificações
11. ✅ **Tests** - Testes unitários e integração
12. ✅ **Docs** - Documentação

**Critérios de qualidade:**

- ✅ Código limpo e bem comentado
- ✅ Seguir padrões do projeto PitStop
- ✅ Multi-tenancy (filter por oficina_id)
- ✅ Soft delete em todas entidades
- ✅ Auditoria (created_at, updated_at)
- ✅ Validações robustas (Bean Validation)
- ✅ Error handling global
- ✅ Logs estruturados
- ✅ TypeScript strict
- ✅ Responsivo mobile-first
- ✅ Acessibilidade (ARIA labels)

**Começar por:** Criar a migration com todas as tabelas necessárias.
