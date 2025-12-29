# Plano de Implementação - Painel SUPER_ADMIN

Este documento serve como guia passo-a-passo para implementar o painel completo do SUPER_ADMIN conforme especificado em `docs/super.md`.

**Data de Criação:** 2024-12-24
**Última Atualização:** 2024-12-24
**Status:** Em Andamento

---

## Estado Atual

### O que já existe:

#### Backend (`/api/saas/*`)
- [x] `GET /api/saas/dashboard/stats` - Estatísticas básicas
- [x] `GET /api/saas/dashboard/mrr-breakdown` - MRR por plano
- [x] `GET /api/saas/oficinas` - Lista de oficinas
- [x] `GET /api/saas/oficinas/{id}` - Detalhes da oficina
- [x] `POST /api/saas/oficinas/{id}/ativar` - Ativar oficina
- [x] `POST /api/saas/oficinas/{id}/suspender` - Suspender oficina
- [x] `POST /api/saas/oficinas/{id}/cancelar` - Cancelar oficina
- [x] `GET /api/saas/pagamentos` - Lista de pagamentos
- [x] `GET /api/saas/audit` - Logs de auditoria
- [x] `POST /api/saas/jobs/*` - Jobs administrativos

#### Frontend (`/admin/*`)
- [x] Dashboard SaaS básico (`/admin`)
- [x] Lista de Oficinas (`/admin/oficinas`)
- [x] Detalhes da Oficina (`/admin/oficinas/:id`)
- [x] Pagamentos (`/admin/pagamentos`)
- [x] Auditoria (`/admin/audit`)

---

## FASE 1: Dashboard Completo + Oficinas Expandido + Planos

**Estimativa:** 3-4 sessões de desenvolvimento
**Prioridade:** ALTA

### 1.1 Dashboard Avançado

#### Backend - Novos Endpoints

```
GET /api/saas/dashboard/metrics
```
Retorna métricas completas:
```java
public class DashboardMetricsDTO {
    // Financeiro
    private BigDecimal mrrTotal;
    private BigDecimal mrrGrowth;      // % crescimento
    private BigDecimal arrTotal;
    private BigDecimal churnRate;
    private BigDecimal ltv;            // Lifetime Value médio
    private BigDecimal cac;            // Custo de Aquisição

    // Oficinas
    private Integer oficinasAtivas;
    private Integer oficinasTrial;
    private Integer oficinasInativas;
    private Integer oficinasInadimplentes;
    private Integer novasOficinas30d;

    // Usuários
    private Integer usuariosAtivos;
    private Integer usuariosTotais;
    private Integer loginsMes;

    // Dados Gerais
    private Long totalClientes;
    private Long totalVeiculos;
    private Long totalOS;
    private Long totalOSMes;
}
```

```
GET /api/saas/dashboard/mrr-evolution?months=12
```
Retorna evolução do MRR nos últimos X meses para gráfico de linha.

```
GET /api/saas/dashboard/churn-evolution?months=12
```
Retorna evolução do churn rate.

```
GET /api/saas/dashboard/signups-vs-cancellations?months=12
```
Retorna novos cadastros vs cancelamentos por mês.

#### Backend - Arquivos a criar/modificar

1. **DTO:** `src/main/java/com/pitstop/saas/dto/DashboardMetricsDTO.java`
2. **DTO:** `src/main/java/com/pitstop/saas/dto/MRREvolutionDTO.java`
3. **DTO:** `src/main/java/com/pitstop/saas/dto/ChurnEvolutionDTO.java`
4. **Service:** Adicionar métodos em `SaasDashboardService.java`
5. **Controller:** Adicionar endpoints em `SaasDashboardController.java`

#### Frontend - Melhorias no Dashboard

1. **Instalar ECharts:**
   ```bash
   cd frontend
   npm install echarts echarts-for-react
   ```

2. **Criar componentes de gráficos:**
   - `frontend/src/features/admin/components/charts/MRREvolutionChart.tsx`
   - `frontend/src/features/admin/components/charts/OficinasStatusChart.tsx`
   - `frontend/src/features/admin/components/charts/ChurnRateChart.tsx`
   - `frontend/src/features/admin/components/charts/SignupsVsCancellationsChart.tsx`

3. **Atualizar SaasDashboardPage.tsx:**
   - Adicionar cards para todas as métricas
   - Integrar gráficos
   - Layout responsivo em grid

#### Checklist Fase 1.1
- [ ] Criar DTOs de métricas avançadas
- [ ] Implementar cálculo de ARR, Churn, LTV, CAC
- [ ] Criar endpoint `/dashboard/metrics`
- [ ] Criar endpoint `/dashboard/mrr-evolution`
- [ ] Criar endpoint `/dashboard/churn-evolution`
- [ ] Instalar ECharts no frontend
- [ ] Criar componentes de gráficos
- [ ] Atualizar dashboard com métricas completas
- [ ] Testar responsividade

---

### 1.2 Oficinas - Funcionalidades Expandidas

#### Backend - Novos Endpoints

```
POST /api/saas/oficinas
```
Criar nova oficina (wizard de onboarding):
```java
public class CreateOficinaRequest {
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private String email;
    private String telefone;
    private String responsavel;
    private EnderecoDTO endereco;
    private String planoId;
    private Integer diasTrial;  // default: 14
}
```

```
PUT /api/saas/oficinas/{id}
```
Editar oficina completa.

```
GET /api/saas/oficinas/{id}/metricas
```
Métricas individuais da oficina:
```java
public class OficinaMetricasDTO {
    private Integer usuariosAtivos;
    private Integer limiteUsuarios;
    private Long espacoUsado;        // bytes
    private Long limiteEspaco;
    private Integer osNoMes;
    private Integer clientesTotal;
    private Integer veiculosTotal;
    private BigDecimal faturamentoMes;
    private LocalDateTime ultimoAcesso;
    private List<LoginHistoricoDTO> ultimosLogins;
}
```

```
POST /api/saas/oficinas/{id}/impersonate
```
Gerar token temporário para acessar como a oficina (suporte):
```java
public class ImpersonateResponse {
    private String accessToken;
    private String redirectUrl;
    private LocalDateTime expiresAt;  // 1 hora
}
```

```
PUT /api/saas/oficinas/{id}/limites
```
Atualizar limites da oficina:
```java
public class UpdateLimitesRequest {
    private Integer limiteUsuarios;
    private Long limiteEspaco;
    private Integer limiteOSMes;
    private Map<String, Boolean> features;
}
```

#### Backend - Arquivos a criar/modificar

1. **DTO:** `CreateOficinaRequest.java`
2. **DTO:** `UpdateOficinaRequest.java`
3. **DTO:** `OficinaMetricasDTO.java`
4. **DTO:** `ImpersonateResponse.java`
5. **DTO:** `UpdateLimitesRequest.java`
6. **Service:** `OficinaProvisioningService.java` - Para criar tenant
7. **Service:** `ImpersonationService.java` - Para impersonate
8. **Controller:** Adicionar endpoints em `SaasOficinasController.java`

#### Frontend - Novas Páginas

1. **Criar Oficina (Wizard):**
   - `frontend/src/features/admin/pages/CreateOficinaPage.tsx`
   - Steps: Dados Básicos → Endereço → Plano → Confirmar

2. **Editar Oficina:**
   - `frontend/src/features/admin/pages/EditOficinaPage.tsx`

3. **Métricas da Oficina:**
   - `frontend/src/features/admin/components/OficinaMetricsPanel.tsx`
   - Adicionar na página de detalhes

4. **Impersonate:**
   - `frontend/src/features/admin/components/ImpersonateButton.tsx`
   - Modal de confirmação com aviso de auditoria

#### Checklist Fase 1.2
- [ ] Criar endpoint POST oficinas (criar)
- [ ] Criar endpoint PUT oficinas (editar)
- [ ] Criar endpoint GET métricas individuais
- [ ] Implementar impersonation seguro
- [ ] Criar endpoint de atualização de limites
- [ ] Criar wizard de nova oficina no frontend
- [ ] Criar página de edição de oficina
- [ ] Adicionar painel de métricas na página de detalhes
- [ ] Implementar botão de impersonate
- [ ] Adicionar logs de auditoria para todas as ações

---

### 1.3 Planos e Assinaturas

#### Backend - Novas Entidades

```java
@Entity
@Table(name = "planos")
public class Plano {
    @Id
    private UUID id;

    private String nome;
    private String descricao;
    private String codigo;  // BASIC, PRO, ENTERPRISE

    private BigDecimal valorMensal;
    private BigDecimal valorAnual;
    private Integer trialDias;

    // Limites - JSON ou embedded
    private Integer limiteUsuarios;
    private Integer limiteOSMes;
    private Long limiteEspaco;
    private Integer limiteApiCalls;
    private Integer limiteWhatsappMensagens;
    private Integer limiteEmailsMes;

    // Features - JSON map
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Boolean> features;

    private Boolean ativo;
    private Boolean visivel;
    private Boolean recomendado;

    private String corDestaque;
    private String tagPromocao;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Backend - Endpoints

```
GET /api/saas/planos
POST /api/saas/planos
GET /api/saas/planos/{id}
PUT /api/saas/planos/{id}
DELETE /api/saas/planos/{id}
POST /api/saas/planos/{id}/toggle-visibilidade
```

```
POST /api/saas/oficinas/{id}/alterar-plano
```
Alterar plano de uma oficina:
```java
public class AlterarPlanoRequest {
    private UUID novoPlanoId;
    private Boolean aplicarImediatamente;
    private Boolean manterPrecoAntigo;  // Grandfathering
}
```

#### Backend - Arquivos a criar

1. **Entity:** `src/main/java/com/pitstop/saas/domain/Plano.java`
2. **Repository:** `src/main/java/com/pitstop/saas/repository/PlanoRepository.java`
3. **DTO:** `PlanoDTO.java`, `CreatePlanoRequest.java`, `UpdatePlanoRequest.java`
4. **Service:** `src/main/java/com/pitstop/saas/service/PlanoService.java`
5. **Controller:** `src/main/java/com/pitstop/saas/controller/SaasPlanoController.java`
6. **Migration:** `V040__create_planos_table.sql`

#### Frontend - Novas Páginas

1. **Lista de Planos:**
   - `frontend/src/features/admin/pages/PlanosListPage.tsx`
   - Visualização em cards estilo pricing

2. **Criar/Editar Plano:**
   - `frontend/src/features/admin/pages/PlanoFormPage.tsx`
   - Form com seções: Info Básica, Preços, Limites, Features

3. **Modal Alterar Plano da Oficina:**
   - `frontend/src/features/admin/components/AlterarPlanoModal.tsx`

#### Checklist Fase 1.3
- [ ] Criar migration da tabela planos
- [ ] Criar entity Plano
- [ ] Criar repository PlanoRepository
- [ ] Criar DTOs de plano
- [ ] Criar PlanoService com CRUD
- [ ] Criar SaasPlanoController
- [ ] Implementar alteração de plano de oficina
- [ ] Criar página de listagem de planos
- [ ] Criar formulário de plano
- [ ] Criar modal de alteração de plano
- [ ] Adicionar rota `/admin/planos` no App.tsx
- [ ] Adicionar no menu de navegação

---

## FASE 2: Billing Completo + Relatórios

**Estimativa:** 3-4 sessões de desenvolvimento
**Prioridade:** ALTA

### 2.1 Sistema de Faturas

#### Backend - Novas Entidades

```java
@Entity
@Table(name = "faturas")
public class Fatura {
    @Id
    private UUID id;

    @ManyToOne
    private Oficina oficina;

    private String numero;  // FAT-2024-00001

    private BigDecimal valor;
    private BigDecimal desconto;
    private BigDecimal valorFinal;

    private LocalDate dataEmissao;
    private LocalDate dataVencimento;
    private LocalDateTime dataPagamento;

    @Enumerated(EnumType.STRING)
    private StatusFatura status;  // PENDENTE, PAGO, VENCIDO, CANCELADO

    private String metodoPagamento;
    private String transacaoId;
    private String comprovante;

    @OneToMany(mappedBy = "fatura")
    private List<ItemFatura> itens;

    private Integer tentativasCobranca;
    private LocalDateTime proximaTentativa;

    private LocalDateTime createdAt;
}

@Entity
@Table(name = "itens_fatura")
public class ItemFatura {
    @Id
    private UUID id;

    @ManyToOne
    private Fatura fatura;

    private String descricao;
    private Integer quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal valorTotal;
}
```

#### Backend - Endpoints

```
GET /api/saas/faturas
GET /api/saas/faturas/{id}
POST /api/saas/faturas
POST /api/saas/faturas/{id}/cancelar
POST /api/saas/faturas/{id}/reenviar
GET /api/saas/faturas/{id}/pdf
POST /api/saas/faturas/{id}/registrar-pagamento
GET /api/saas/faturas/inadimplentes
POST /api/saas/faturas/gerar-mensal  // Job de geração
```

#### Backend - Arquivos a criar

1. **Entities:** `Fatura.java`, `ItemFatura.java`
2. **Enum:** `StatusFatura.java`
3. **Repository:** `FaturaRepository.java`
4. **DTOs:** `FaturaDTO.java`, `CreateFaturaRequest.java`, etc.
5. **Service:** `FaturaService.java`
6. **Service:** `FaturaPdfService.java` - Geração de PDF
7. **Controller:** `SaasFaturaController.java`
8. **Job:** `GeracaoFaturaJob.java` - Scheduled monthly
9. **Migration:** `V041__create_faturas_table.sql`

#### Frontend - Novas Páginas

1. **Lista de Faturas:**
   - `frontend/src/features/admin/pages/FaturasListPage.tsx`
   - Filtros: status, período, oficina
   - Ações em massa

2. **Detalhes da Fatura:**
   - `frontend/src/features/admin/pages/FaturaDetailPage.tsx`
   - Timeline de eventos
   - Botões de ação

3. **Criar Fatura Manual:**
   - `frontend/src/features/admin/pages/CreateFaturaPage.tsx`

#### Checklist Fase 2.1
- [ ] Criar migration de faturas
- [ ] Criar entities Fatura e ItemFatura
- [ ] Criar repository e service
- [ ] Implementar geração de número sequencial
- [ ] Implementar geração de PDF
- [ ] Criar job de geração mensal
- [ ] Implementar retry de cobrança
- [ ] Criar páginas no frontend
- [ ] Integrar com Mercado Pago (webhook de pagamento)

---

### 2.2 Gestão de Inadimplência

#### Backend - Endpoints

```
GET /api/saas/inadimplencia/dashboard
```
Retorna:
```java
public class InadimplenciaDashboardDTO {
    private BigDecimal valorTotalInadimplente;
    private Integer oficinasInadimplentes;
    private Map<Integer, BigDecimal> porDiasAtraso;  // 1-30, 31-60, 60+
    private List<OficinaInadimplenteDTO> top10;
}
```

```
POST /api/saas/inadimplencia/acao-massa
```
Ações em massa:
```java
public class AcaoMassaRequest {
    private List<UUID> oficinaIds;
    private String acao;  // NOTIFICAR, SUSPENDER, CANCELAR
    private String mensagemCustomizada;
}
```

```
POST /api/saas/inadimplencia/negociar/{oficinaId}
```
Criar acordo de pagamento:
```java
public class NegociacaoRequest {
    private BigDecimal valorAcordado;
    private Integer parcelas;
    private LocalDate primeiroVencimento;
    private String observacoes;
}
```

#### Checklist Fase 2.2
- [ ] Criar dashboard de inadimplência
- [ ] Implementar ações em massa
- [ ] Criar sistema de negociação
- [ ] Implementar notificações automáticas
- [ ] Criar régua de cobrança configurável

---

### 2.3 Relatórios e Analytics

#### Backend - Endpoints

```
GET /api/saas/relatorios/financeiro
GET /api/saas/relatorios/operacional
GET /api/saas/relatorios/crescimento
POST /api/saas/relatorios/customizado
GET /api/saas/relatorios/export/{tipo}?formato=pdf|excel|csv
POST /api/saas/relatorios/agendar
```

#### Frontend - Novas Páginas

1. **Central de Relatórios:**
   - `frontend/src/features/admin/pages/RelatoriosPage.tsx`
   - Cards para cada tipo de relatório
   - Filtros de período

2. **Relatório Individual:**
   - `frontend/src/features/admin/pages/RelatorioViewPage.tsx`
   - Gráficos interativos
   - Export em múltiplos formatos

3. **Agendamento de Relatórios:**
   - `frontend/src/features/admin/pages/RelatoriosAgendadosPage.tsx`

#### Checklist Fase 2.3
- [ ] Criar service de geração de relatórios
- [ ] Implementar export PDF (iText)
- [ ] Implementar export Excel (Apache POI)
- [ ] Criar job de relatórios agendados
- [ ] Criar páginas no frontend
- [ ] Integrar gráficos ECharts

---

## FASE 3: Comunicação + Tickets + Feature Flags

**Estimativa:** 3-4 sessões de desenvolvimento
**Prioridade:** MÉDIA

### 3.1 Sistema de Tickets

#### Backend - Novas Entidades

```java
@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    private UUID id;

    private String numero;  // TKT-2024-00001

    @ManyToOne
    private Oficina oficina;

    private UUID usuarioId;
    private String usuarioNome;
    private String usuarioEmail;

    @Enumerated(EnumType.STRING)
    private TipoTicket tipo;  // TECNICO, FINANCEIRO, COMERCIAL, OUTRO

    @Enumerated(EnumType.STRING)
    private PrioridadeTicket prioridade;  // BAIXA, MEDIA, ALTA, URGENTE

    @Enumerated(EnumType.STRING)
    private StatusTicket status;  // ABERTO, EM_ANDAMENTO, AGUARDANDO, RESOLVIDO, FECHADO

    private String assunto;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Type(JsonBinaryType.class)
    private List<String> anexos;

    private UUID atribuidoA;
    private LocalDateTime respostaInicial;
    private Integer slaMinutos;
    private Integer tempoResposta;

    @OneToMany(mappedBy = "ticket")
    private List<MensagemTicket> mensagens;

    private LocalDateTime aberturaEm;
    private LocalDateTime atualizadoEm;
    private LocalDateTime resolvidoEm;
    private LocalDateTime fechadoEm;
}

@Entity
@Table(name = "mensagens_ticket")
public class MensagemTicket {
    @Id
    private UUID id;

    @ManyToOne
    private Ticket ticket;

    private UUID autorId;
    private String autorNome;
    private Boolean isInterno;  // Nota interna vs resposta ao cliente

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    @Type(JsonBinaryType.class)
    private List<String> anexos;

    private LocalDateTime criadoEm;
}
```

#### Backend - Endpoints

```
GET /api/saas/tickets
GET /api/saas/tickets/{id}
POST /api/saas/tickets/{id}/responder
POST /api/saas/tickets/{id}/atribuir
POST /api/saas/tickets/{id}/alterar-status
POST /api/saas/tickets/{id}/alterar-prioridade
GET /api/saas/tickets/metricas
```

#### Frontend - Novas Páginas

1. **Lista de Tickets:**
   - `frontend/src/features/admin/pages/TicketsListPage.tsx`
   - Kanban view opcional
   - Filtros avançados

2. **Detalhes do Ticket:**
   - `frontend/src/features/admin/pages/TicketDetailPage.tsx`
   - Thread de mensagens
   - Sidebar com info do cliente

3. **Dashboard de Suporte:**
   - `frontend/src/features/admin/pages/SuporteDashboardPage.tsx`
   - Métricas de SLA, tempo de resposta

#### Checklist Fase 3.1
- [ ] Criar migration de tickets
- [ ] Criar entities Ticket e MensagemTicket
- [ ] Implementar numeração sequencial
- [ ] Criar notificações por email
- [ ] Implementar SLA tracking
- [ ] Criar páginas no frontend
- [ ] Adicionar WebSocket para atualizações em tempo real

---

### 3.2 Sistema de Comunicação

#### Backend - Novas Entidades

```java
@Entity
@Table(name = "mensagens_broadcast")
public class MensagemBroadcast {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private TipoMensagem tipo;  // INDIVIDUAL, BROADCAST, SEGMENTADO

    @Type(JsonBinaryType.class)
    private List<UUID> destinatarios;

    @Type(JsonBinaryType.class)
    private FiltrosDestinatarios filtros;

    private String assunto;

    @Column(columnDefinition = "TEXT")
    private String corpo;

    private String templateId;

    @Type(JsonBinaryType.class)
    private List<String> canais;  // EMAIL, WHATSAPP, IN_APP

    private Boolean agendada;
    private LocalDateTime dataEnvio;

    @Enumerated(EnumType.STRING)
    private StatusMensagem status;  // RASCUNHO, AGENDADA, ENVIANDO, ENVIADA, FALHA

    private Integer totalEnviados;
    private Integer totalEntregues;
    private Integer totalAbertos;
    private Integer totalCliques;

    private LocalDateTime criadoEm;
    private UUID criadoPor;
}
```

#### Backend - Endpoints

```
GET /api/saas/comunicacao/mensagens
POST /api/saas/comunicacao/mensagens
GET /api/saas/comunicacao/mensagens/{id}
POST /api/saas/comunicacao/mensagens/{id}/enviar
DELETE /api/saas/comunicacao/mensagens/{id}
GET /api/saas/comunicacao/templates
POST /api/saas/comunicacao/templates
GET /api/saas/comunicacao/segmentos
```

#### Frontend - Novas Páginas

1. **Central de Comunicação:**
   - `frontend/src/features/admin/pages/ComunicacaoPage.tsx`

2. **Criar Mensagem:**
   - `frontend/src/features/admin/pages/CreateMensagemPage.tsx`
   - Editor WYSIWYG
   - Preview por canal
   - Segmentação visual

3. **Templates:**
   - `frontend/src/features/admin/pages/TemplatesPage.tsx`

#### Checklist Fase 3.2
- [ ] Criar migration de mensagens
- [ ] Criar entity MensagemBroadcast
- [ ] Integrar com serviço de email
- [ ] Integrar com WhatsApp (já existe)
- [ ] Implementar sistema de templates
- [ ] Criar job de envio agendado
- [ ] Implementar tracking de métricas
- [ ] Criar páginas no frontend

---

### 3.3 Feature Flags

#### Backend - Novas Entidades

```java
@Entity
@Table(name = "feature_flags")
public class FeatureFlag {
    @Id
    private UUID id;

    private String codigo;
    private String nome;
    private String descricao;

    private Boolean habilitadoGlobal;

    @Type(JsonBinaryType.class)
    private Map<String, Boolean> habilitadoPorPlano;

    @Type(JsonBinaryType.class)
    private List<UUID> habilitadoPorOficina;

    private Integer percentualRollout;  // 0-100

    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

    private Boolean requerAutorizacao;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
```

#### Backend - Endpoints

```
GET /api/saas/features
POST /api/saas/features
PUT /api/saas/features/{id}
DELETE /api/saas/features/{id}
POST /api/saas/features/{id}/toggle
GET /api/saas/features/oficina/{oficinaId}
```

#### Backend - Service de Verificação

```java
@Service
public class FeatureFlagService {

    public boolean isEnabled(String featureCode, UUID oficinaId) {
        // 1. Check if globally disabled
        // 2. Check oficina-specific override
        // 3. Check plano-based setting
        // 4. Check rollout percentage
        // 5. Check date range
    }
}
```

#### Frontend - Novas Páginas

1. **Feature Flags:**
   - `frontend/src/features/admin/pages/FeatureFlagsPage.tsx`
   - Toggle switches
   - Rollout percentage slider
   - Segmentação por plano

#### Checklist Fase 3.3
- [ ] Criar migration de feature flags
- [ ] Criar entity FeatureFlag
- [ ] Implementar service de verificação
- [ ] Criar cache Redis para flags
- [ ] Criar endpoint para frontend das oficinas verificar flags
- [ ] Criar página de administração

---

## FASE 4: Monitoramento + Integrações + Configurações

**Estimativa:** 2-3 sessões de desenvolvimento
**Prioridade:** MÉDIA-BAIXA

### 4.1 Monitoramento e Performance

#### Backend - Endpoints

```
GET /api/saas/monitoring/health
GET /api/saas/monitoring/metrics
GET /api/saas/monitoring/errors
GET /api/saas/monitoring/slow-queries
GET /api/saas/monitoring/integrations-status
```

#### Integração com Actuator

```java
@Configuration
public class ActuatorConfig {
    // Expor métricas customizadas
    // Configurar health indicators para:
    // - Database
    // - Redis
    // - Mercado Pago
    // - Twilio/WhatsApp
    // - Email service
}
```

#### Frontend - Novas Páginas

1. **Dashboard de Monitoramento:**
   - `frontend/src/features/admin/pages/MonitoringPage.tsx`
   - Status cards para cada serviço
   - Gráficos de performance
   - Lista de erros recentes

#### Checklist Fase 4.1
- [ ] Configurar Actuator endpoints
- [ ] Criar health indicators customizados
- [ ] Implementar coleta de métricas
- [ ] Criar alertas por email/Telegram
- [ ] Criar página de monitoramento

---

### 4.2 Painel de Integrações

#### Backend - Novas Entidades

```java
@Entity
@Table(name = "integracoes")
public class Integracao {
    @Id
    private UUID id;

    private String nome;
    private String tipo;  // PAGAMENTO, MENSAGERIA, ANALYTICS

    private Boolean ativa;
    private Boolean configurada;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> config;  // Encrypted

    private Long requestsHoje;
    private Long limiteRequests;
    private LocalDateTime ultimoUso;

    private String status;  // ONLINE, OFFLINE, ERROR
    private LocalDateTime ultimoCheck;
}
```

#### Frontend - Novas Páginas

1. **Painel de Integrações:**
   - `frontend/src/features/admin/pages/IntegracoesPage.tsx`
   - Cards para cada integração
   - Status em tempo real
   - Configuração inline

#### Checklist Fase 4.2
- [ ] Criar entity Integracao
- [ ] Criar service de health check
- [ ] Implementar rotação de API keys
- [ ] Criar página de integrações
- [ ] Adicionar testes de conexão

---

### 4.3 Configurações do Sistema

#### Backend - Entidades

```java
@Entity
@Table(name = "configuracoes_sistema")
public class ConfiguracaoSistema {
    @Id
    private String chave;
    private String valor;
    private String tipo;  // STRING, NUMBER, BOOLEAN, JSON
    private String categoria;
    private String descricao;
    private Boolean sensivel;  // Se true, não expor no GET
    private LocalDateTime atualizadoEm;
}
```

#### Backend - Endpoints

```
GET /api/saas/configuracoes
GET /api/saas/configuracoes/{categoria}
PUT /api/saas/configuracoes/{chave}
POST /api/saas/configuracoes/manutencao/toggle
```

#### Frontend - Novas Páginas

1. **Configurações:**
   - `frontend/src/features/admin/pages/ConfiguracoesPage.tsx`
   - Tabs por categoria
   - Forms dinâmicos
   - Validação em tempo real

#### Checklist Fase 4.3
- [ ] Criar entity ConfiguracaoSistema
- [ ] Seed de configurações padrão
- [ ] Implementar cache de configurações
- [ ] Criar página de configurações
- [ ] Implementar modo manutenção

---

## FASE 5: Segurança e Gestão de Super Admins

**Estimativa:** 1-2 sessões de desenvolvimento
**Prioridade:** ALTA (mas pode esperar as outras fases)

### 5.1 Gestão de Super Admins

#### Backend - Melhorias na Entity Usuario

```java
// Adicionar campos para Super Admin
private Boolean mfaHabilitado;
private String mfaSecret;

@Type(JsonBinaryType.class)
private Map<String, Boolean> permissoes;
```

#### Backend - Endpoints

```
GET /api/saas/admins
POST /api/saas/admins
PUT /api/saas/admins/{id}
DELETE /api/saas/admins/{id}
POST /api/saas/admins/{id}/reset-mfa
GET /api/saas/admins/{id}/sessoes
DELETE /api/saas/admins/{id}/sessoes/{sessaoId}
```

#### Frontend - Novas Páginas

1. **Gestão de Admins:**
   - `frontend/src/features/admin/pages/AdminsPage.tsx`
   - Lista com status online
   - Configuração de permissões

#### Checklist Fase 5.1
- [ ] Adicionar campos MFA na entity Usuario
- [ ] Implementar verificação TOTP
- [ ] Criar endpoints de gestão
- [ ] Implementar permissões granulares
- [ ] Criar página de administração

---

## Ordem de Execução Recomendada

```
Sessão 1-2:  Fase 1.1 - Dashboard Avançado
Sessão 3-4:  Fase 1.2 - Oficinas Expandido
Sessão 5-6:  Fase 1.3 - Planos e Assinaturas
Sessão 7-8:  Fase 2.1 - Sistema de Faturas
Sessão 9-10: Fase 2.2 - Inadimplência + Fase 2.3 - Relatórios
Sessão 11-12: Fase 3.1 - Tickets
Sessão 13-14: Fase 3.2 - Comunicação + Fase 3.3 - Feature Flags
Sessão 15-16: Fase 4 - Monitoramento + Integrações + Configurações
Sessão 17-18: Fase 5 - Segurança e Super Admins
```

---

## Convenções de Código

### Backend

```
Package: com.pitstop.saas.*
Controller prefix: /api/saas/*
DTOs: *DTO.java, *Request.java, *Response.java
Services: *Service.java
Migrations: V0XX__description.sql
```

### Frontend

```
Pages: features/admin/pages/*Page.tsx
Components: features/admin/components/*.tsx
Hooks: features/admin/hooks/use*.ts
Services: features/admin/services/*Service.ts
Types: features/admin/types/index.ts
Routes: /admin/*
```

### Padrões

- Todos os endpoints devem ter log de auditoria
- Todas as páginas devem suportar dark mode
- Usar React Query para cache de dados
- Validação com Zod no frontend
- Bean Validation no backend

---

## Como Usar Este Documento

1. **Antes de cada sessão**: Leia a fase atual e os checklists
2. **Durante a sessão**: Marque os itens como concluídos
3. **Ao final**: Atualize o status e a data de última atualização
4. **Próxima sessão**: Continue de onde parou

Para continuar a implementação, diga:
> "Vamos implementar a Fase X.Y - [Nome]"

---

## Notas e Decisões

### 2024-12-24
- Estrutura inicial do documento criada
- Fases definidas e priorizadas
- Backend básico já existe para dashboard, oficinas, pagamentos e audit
