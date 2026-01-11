# Plano de Implementação - Portal SaaS para Oficinas

Este documento detalha as funcionalidades que precisam ser implementadas no lado da **oficina (tenant)** para complementar o painel SUPER_ADMIN existente.

## Status Atual

### SUPER_ADMIN (100% Completo)
- Dashboard com métricas globais
- Gerenciamento de oficinas (CRUD completo)
- Gerenciamento de planos
- Faturamento e cobranças
- Gestão de inadimplência
- Sistema de tickets de suporte
- Sistema de comunicados
- Feature flags
- Auditoria
- Relatórios

### Oficina (Lacunas Identificadas)
As oficinas atualmente **não têm visibilidade** sobre:
1. Suas faturas e histórico de pagamentos
2. Criação de tickets de suporte
3. Seu plano atual, limites e consumo
4. Features habilitadas para sua conta

---

## 1. Portal de Cobrança (Billing Portal)

### 1.1 Objetivo
Permitir que oficinas visualizem suas faturas, histórico de pagamentos e status da assinatura.

### 1.2 Backend

#### 1.2.1 Controller
**Arquivo:** `src/main/java/com/pitstop/oficina/controller/OficinaBillingController.java`

```java
@RestController
@RequestMapping("/api/oficina/billing")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class OficinaBillingController {

    @GetMapping("/faturas")
    public ResponseEntity<Page<FaturaOficinaDTO>> listarMinhasFaturas(Pageable pageable);

    @GetMapping("/faturas/{id}")
    public ResponseEntity<FaturaOficinaDTO> buscarFatura(@PathVariable Long id);

    @GetMapping("/faturas/{id}/pdf")
    public ResponseEntity<byte[]> downloadFaturaPDF(@PathVariable Long id);

    @GetMapping("/plano")
    public ResponseEntity<PlanoAtualDTO> meuPlano();

    @GetMapping("/consumo")
    public ResponseEntity<ConsumoDTO> meuConsumo();

    @GetMapping("/pagamentos")
    public ResponseEntity<Page<PagamentoOficinaDTO>> meusPagamentos(Pageable pageable);
}
```

#### 1.2.2 DTOs
**Arquivo:** `src/main/java/com/pitstop/oficina/dto/billing/`

```java
// PlanoAtualDTO.java
public record PlanoAtualDTO(
    String nome,
    String descricao,
    BigDecimal valorMensal,
    LocalDate dataProximaCobranca,
    String status, // ATIVO, INADIMPLENTE, CANCELADO
    List<LimiteDTO> limites,
    List<String> featuresAtivas
) {}

// ConsumoDTO.java
public record ConsumoDTO(
    int usuariosAtivos,
    int limiteUsuarios,
    int ordensServicoMes,
    int limiteOrdensMes,
    long espacoUsadoMB,
    long limiteEspacoMB,
    double percentualUsuarios,
    double percentualOrdens,
    double percentualEspaco
) {}

// FaturaOficinaDTO.java
public record FaturaOficinaDTO(
    Long id,
    String numero,
    LocalDate dataVencimento,
    LocalDate dataPagamento,
    BigDecimal valorTotal,
    String status, // PENDENTE, PAGA, VENCIDA, CANCELADA
    String linkPagamento,
    List<ItemFaturaDTO> itens
) {}

// PagamentoOficinaDTO.java
public record PagamentoOficinaDTO(
    Long id,
    LocalDateTime dataPagamento,
    BigDecimal valor,
    String formaPagamento,
    String comprovante
) {}
```

#### 1.2.3 Service
**Arquivo:** `src/main/java/com/pitstop/oficina/service/OficinaBillingService.java`

```java
@Service
@RequiredArgsConstructor
public class OficinaBillingService {

    private final FaturaRepository faturaRepository;
    private final PlanoRepository planoRepository;
    private final OficinaRepository oficinaRepository;
    private final SaasPagamentoRepository pagamentoRepository;

    public Page<FaturaOficinaDTO> listarFaturas(Pageable pageable) {
        Long oficinaId = TenantContext.getCurrentOficinaId();
        return faturaRepository.findByOficinaId(oficinaId, pageable)
            .map(this::toFaturaDTO);
    }

    public PlanoAtualDTO getPlanoAtual() {
        Long oficinaId = TenantContext.getCurrentOficinaId();
        Oficina oficina = oficinaRepository.findById(oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        Plano plano = oficina.getPlano();
        List<FeatureFlag> features = featureFlagRepository.findActiveByOficinaId(oficinaId);

        return new PlanoAtualDTO(
            plano.getNome(),
            plano.getDescricao(),
            plano.getValorMensal(),
            calcularProximaCobranca(oficina),
            oficina.getStatusAssinatura(),
            toLimiteDTOs(plano),
            features.stream().map(FeatureFlag::getCodigo).toList()
        );
    }

    public ConsumoDTO getConsumo() {
        Long oficinaId = TenantContext.getCurrentOficinaId();
        // Calcular consumo atual vs limites do plano
    }
}
```

### 1.3 Frontend

#### 1.3.1 Estrutura de Arquivos
```
frontend/src/features/billing/
├── components/
│   ├── FaturaCard.tsx
│   ├── ConsumoGauge.tsx
│   ├── PlanoCard.tsx
│   └── PagamentoRow.tsx
├── hooks/
│   └── useBilling.ts
├── pages/
│   ├── BillingDashboardPage.tsx
│   ├── FaturasPage.tsx
│   └── FaturaDetailPage.tsx
├── services/
│   └── billingService.ts
└── types/
    └── index.ts
```

#### 1.3.2 Tipos TypeScript
**Arquivo:** `frontend/src/features/billing/types/index.ts`

```typescript
export interface PlanoAtual {
  nome: string;
  descricao: string;
  valorMensal: number;
  dataProximaCobranca: string;
  status: 'ATIVO' | 'INADIMPLENTE' | 'CANCELADO';
  limites: Limite[];
  featuresAtivas: string[];
}

export interface Limite {
  recurso: string;
  limite: number;
  usado: number;
  unidade: string;
}

export interface Consumo {
  usuariosAtivos: number;
  limiteUsuarios: number;
  ordensServicoMes: number;
  limiteOrdensMes: number;
  espacoUsadoMB: number;
  limiteEspacoMB: number;
  percentualUsuarios: number;
  percentualOrdens: number;
  percentualEspaco: number;
}

export interface FaturaOficina {
  id: number;
  numero: string;
  dataVencimento: string;
  dataPagamento?: string;
  valorTotal: number;
  status: 'PENDENTE' | 'PAGA' | 'VENCIDA' | 'CANCELADA';
  linkPagamento?: string;
  itens: ItemFatura[];
}

export interface PagamentoOficina {
  id: number;
  dataPagamento: string;
  valor: number;
  formaPagamento: string;
  comprovante?: string;
}
```

#### 1.3.3 Service
**Arquivo:** `frontend/src/features/billing/services/billingService.ts`

```typescript
import { api } from '@/shared/services/api';
import type { PlanoAtual, Consumo, FaturaOficina, PagamentoOficina } from '../types';

export const billingService = {
  async getPlanoAtual(): Promise<PlanoAtual> {
    const { data } = await api.get('/oficina/billing/plano');
    return data;
  },

  async getConsumo(): Promise<Consumo> {
    const { data } = await api.get('/oficina/billing/consumo');
    return data;
  },

  async getFaturas(page = 0, size = 10): Promise<PageResponse<FaturaOficina>> {
    const { data } = await api.get('/oficina/billing/faturas', {
      params: { page, size },
    });
    return data;
  },

  async getFatura(id: number): Promise<FaturaOficina> {
    const { data } = await api.get(`/oficina/billing/faturas/${id}`);
    return data;
  },

  async downloadFaturaPDF(id: number): Promise<Blob> {
    const { data } = await api.get(`/oficina/billing/faturas/${id}/pdf`, {
      responseType: 'blob',
    });
    return data;
  },

  async getPagamentos(page = 0, size = 10): Promise<PageResponse<PagamentoOficina>> {
    const { data } = await api.get('/oficina/billing/pagamentos', {
      params: { page, size },
    });
    return data;
  },
};
```

#### 1.3.4 Hooks
**Arquivo:** `frontend/src/features/billing/hooks/useBilling.ts`

```typescript
import { useQuery } from '@tanstack/react-query';
import { billingService } from '../services/billingService';

export const billingKeys = {
  all: ['billing'] as const,
  plano: () => [...billingKeys.all, 'plano'] as const,
  consumo: () => [...billingKeys.all, 'consumo'] as const,
  faturas: () => [...billingKeys.all, 'faturas'] as const,
  fatura: (id: number) => [...billingKeys.all, 'fatura', id] as const,
  pagamentos: () => [...billingKeys.all, 'pagamentos'] as const,
};

export const usePlanoAtual = () => {
  return useQuery({
    queryKey: billingKeys.plano(),
    queryFn: billingService.getPlanoAtual,
    staleTime: 5 * 60 * 1000, // 5 minutos
  });
};

export const useConsumo = () => {
  return useQuery({
    queryKey: billingKeys.consumo(),
    queryFn: billingService.getConsumo,
    staleTime: 1 * 60 * 1000, // 1 minuto
  });
};

export const useFaturas = (page = 0, size = 10) => {
  return useQuery({
    queryKey: [...billingKeys.faturas(), { page, size }],
    queryFn: () => billingService.getFaturas(page, size),
    staleTime: 5 * 60 * 1000,
  });
};

export const useFatura = (id?: number) => {
  return useQuery({
    queryKey: billingKeys.fatura(id!),
    queryFn: () => billingService.getFatura(id!),
    enabled: !!id,
  });
};
```

#### 1.3.5 Rotas
**Adicionar em `App.tsx`:**

```typescript
// Imports
const BillingDashboardPage = lazy(() => import('./features/billing/pages/BillingDashboardPage'));
const FaturasPage = lazy(() => import('./features/billing/pages/FaturasPage'));
const FaturaDetailPage = lazy(() => import('./features/billing/pages/FaturaDetailPage'));

// Rotas (dentro do MainLayout, após /configuracoes)
<Route
  path="assinatura"
  element={
    <ProtectedRoute requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}>
      <BillingDashboardPage />
    </ProtectedRoute>
  }
/>
<Route
  path="assinatura/faturas"
  element={
    <ProtectedRoute requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}>
      <FaturasPage />
    </ProtectedRoute>
  }
/>
<Route
  path="assinatura/faturas/:id"
  element={
    <ProtectedRoute requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}>
      <FaturaDetailPage />
    </ProtectedRoute>
  }
/>
```

#### 1.3.6 Menu Lateral
**Adicionar em `MainLayout.tsx`:**

```typescript
// Após "Configurações", adicionar:
{(user?.perfil === 'ADMIN' || user?.perfil === 'GERENTE') && (
  <NavItem
    to="/assinatura"
    icon={CreditCard}
    label="Assinatura"
  />
)}
```

---

## 2. Sistema de Tickets de Suporte

### 2.1 Objetivo
Permitir que oficinas criem tickets de suporte e acompanhem o status.

### 2.2 Backend

#### 2.2.1 Controller
**Arquivo:** `src/main/java/com/pitstop/oficina/controller/OficinaSuporteController.java`

```java
@RestController
@RequestMapping("/api/oficina/suporte")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class OficinaSuporteController {

    @GetMapping("/tickets")
    public ResponseEntity<Page<TicketOficinaDTO>> listarMeusTickets(
        @RequestParam(required = false) String status,
        Pageable pageable
    );

    @GetMapping("/tickets/{id}")
    public ResponseEntity<TicketOficinaDTO> buscarTicket(@PathVariable Long id);

    @PostMapping("/tickets")
    public ResponseEntity<TicketOficinaDTO> criarTicket(@RequestBody @Valid CreateTicketRequest request);

    @PostMapping("/tickets/{id}/mensagens")
    public ResponseEntity<MensagemTicketDTO> enviarMensagem(
        @PathVariable Long id,
        @RequestBody @Valid CreateMensagemRequest request
    );

    @PatchMapping("/tickets/{id}/fechar")
    public ResponseEntity<TicketOficinaDTO> fecharTicket(@PathVariable Long id);
}
```

#### 2.2.2 DTOs
**Arquivo:** `src/main/java/com/pitstop/oficina/dto/suporte/`

```java
// CreateTicketRequest.java
public record CreateTicketRequest(
    @NotBlank String assunto,
    @NotBlank String descricao,
    @NotNull CategoriaTicket categoria, // TECNICO, FINANCEIRO, COMERCIAL, OUTROS
    @NotNull PrioridadeTicket prioridade // BAIXA, MEDIA, ALTA, URGENTE
) {}

// TicketOficinaDTO.java
public record TicketOficinaDTO(
    Long id,
    String numero,
    String assunto,
    String descricao,
    CategoriaTicket categoria,
    PrioridadeTicket prioridade,
    StatusTicket status,
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm,
    LocalDateTime resolvidoEm,
    List<MensagemTicketDTO> mensagens
) {}

// CreateMensagemRequest.java
public record CreateMensagemRequest(
    @NotBlank String conteudo
) {}
```

### 2.3 Frontend

#### 2.3.1 Estrutura de Arquivos
```
frontend/src/features/suporte/
├── components/
│   ├── TicketCard.tsx
│   ├── TicketForm.tsx
│   ├── MensagemItem.tsx
│   └── ChatSuporte.tsx
├── hooks/
│   └── useSuporte.ts
├── pages/
│   ├── TicketsPage.tsx
│   ├── TicketDetailPage.tsx
│   └── NovoTicketPage.tsx
├── services/
│   └── suporteService.ts
└── types/
    └── index.ts
```

#### 2.3.2 Tipos TypeScript
**Arquivo:** `frontend/src/features/suporte/types/index.ts`

```typescript
export type CategoriaTicket = 'TECNICO' | 'FINANCEIRO' | 'COMERCIAL' | 'OUTROS';
export type PrioridadeTicket = 'BAIXA' | 'MEDIA' | 'ALTA' | 'URGENTE';
export type StatusTicket = 'ABERTO' | 'EM_ANDAMENTO' | 'AGUARDANDO_CLIENTE' | 'RESOLVIDO' | 'FECHADO';

export interface TicketOficina {
  id: number;
  numero: string;
  assunto: string;
  descricao: string;
  categoria: CategoriaTicket;
  prioridade: PrioridadeTicket;
  status: StatusTicket;
  criadoEm: string;
  atualizadoEm: string;
  resolvidoEm?: string;
  mensagens: MensagemTicket[];
}

export interface MensagemTicket {
  id: number;
  conteudo: string;
  remetente: 'OFICINA' | 'SUPORTE';
  nomeRemetente: string;
  criadoEm: string;
}

export interface CreateTicketRequest {
  assunto: string;
  descricao: string;
  categoria: CategoriaTicket;
  prioridade: PrioridadeTicket;
}
```

#### 2.3.3 Service
**Arquivo:** `frontend/src/features/suporte/services/suporteService.ts`

```typescript
import { api } from '@/shared/services/api';
import type { TicketOficina, CreateTicketRequest, MensagemTicket } from '../types';

export const suporteService = {
  async getTickets(status?: string, page = 0, size = 10) {
    const { data } = await api.get('/oficina/suporte/tickets', {
      params: { status, page, size },
    });
    return data;
  },

  async getTicket(id: number): Promise<TicketOficina> {
    const { data } = await api.get(`/oficina/suporte/tickets/${id}`);
    return data;
  },

  async createTicket(request: CreateTicketRequest): Promise<TicketOficina> {
    const { data } = await api.post('/oficina/suporte/tickets', request);
    return data;
  },

  async enviarMensagem(ticketId: number, conteudo: string): Promise<MensagemTicket> {
    const { data } = await api.post(`/oficina/suporte/tickets/${ticketId}/mensagens`, {
      conteudo,
    });
    return data;
  },

  async fecharTicket(id: number): Promise<TicketOficina> {
    const { data } = await api.patch(`/oficina/suporte/tickets/${id}/fechar`);
    return data;
  },
};
```

#### 2.3.4 Rotas
**Adicionar em `App.tsx`:**

```typescript
// Imports
const TicketsPage = lazy(() => import('./features/suporte/pages/TicketsPage'));
const TicketDetailPage = lazy(() => import('./features/suporte/pages/TicketDetailPage'));
const NovoTicketPage = lazy(() => import('./features/suporte/pages/NovoTicketPage'));

// Rotas
<Route
  path="suporte"
  element={
    <ProtectedRoute requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}>
      <TicketsPage />
    </ProtectedRoute>
  }
/>
<Route
  path="suporte/novo"
  element={
    <ProtectedRoute requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}>
      <NovoTicketPage />
    </ProtectedRoute>
  }
/>
<Route
  path="suporte/:id"
  element={
    <ProtectedRoute requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}>
      <TicketDetailPage />
    </ProtectedRoute>
  }
/>
```

---

## 3. Visibilidade de Features

### 3.1 Objetivo
Mostrar à oficina quais features estão habilitadas para sua conta.

### 3.2 Backend

#### 3.2.1 Endpoint (adicionar ao OficinaBillingController)

```java
@GetMapping("/features")
public ResponseEntity<List<FeatureDTO>> minhasFeatures() {
    Long oficinaId = TenantContext.getCurrentOficinaId();
    List<FeatureFlag> features = featureFlagService.getActiveFeatures(oficinaId);
    return ResponseEntity.ok(features.stream().map(this::toFeatureDTO).toList());
}
```

#### 3.2.2 DTO

```java
public record FeatureDTO(
    String codigo,
    String nome,
    String descricao,
    boolean ativa
) {}
```

### 3.3 Frontend

Incluir na página de Assinatura/Billing um card com as features:

```typescript
// Componente FeaturesCard.tsx
export function FeaturesCard({ features }: { features: Feature[] }) {
  return (
    <div className="rounded-lg bg-white p-6 shadow">
      <h3 className="text-lg font-semibold mb-4">Recursos do Plano</h3>
      <div className="space-y-3">
        {features.map((feature) => (
          <div key={feature.codigo} className="flex items-center gap-3">
            {feature.ativa ? (
              <CheckCircle className="h-5 w-5 text-green-500" />
            ) : (
              <XCircle className="h-5 w-5 text-gray-400" />
            )}
            <div>
              <p className="font-medium">{feature.nome}</p>
              <p className="text-sm text-gray-500">{feature.descricao}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
```

---

## 4. Migrations Necessárias

Não são necessárias novas migrations, pois as entidades já existem:
- `Fatura` - já existente
- `Ticket` - já existente
- `FeatureFlag` - já existente
- `Plano` - já existente

As queries apenas filtrarão por `oficina_id` do contexto do tenant.

---

## 5. Ordem de Implementação Recomendada

### Fase 1 - Portal de Cobrança (Prioridade Alta)
1. Backend: OficinaBillingService + Controller
2. Frontend: billingService + hooks
3. Frontend: BillingDashboardPage
4. Frontend: FaturasPage + FaturaDetailPage
5. Integrar no menu lateral

**Estimativa:** Escopo médio

### Fase 2 - Sistema de Suporte (Prioridade Alta)
1. Backend: OficinaSuporteService + Controller
2. Frontend: suporteService + hooks
3. Frontend: TicketsPage + NovoTicketPage
4. Frontend: TicketDetailPage com chat
5. Integrar no menu lateral

**Estimativa:** Escopo médio

### Fase 3 - Visibilidade de Features (Prioridade Média)
1. Backend: Endpoint adicional
2. Frontend: FeaturesCard component
3. Integrar na página de Assinatura

**Estimativa:** Escopo pequeno

---

## 6. Testes Necessários

### Backend
- `OficinaBillingServiceTest.java` - Testes de consulta de faturas e plano
- `OficinaSuporteServiceTest.java` - Testes de CRUD de tickets
- Testes de autorização (apenas ADMIN/GERENTE acessam)
- Testes de multi-tenancy (oficina só vê seus próprios dados)

### Frontend
- Testes de componentes com React Testing Library
- Testes E2E com Playwright para fluxos críticos

---

## 7. Considerações de Segurança

1. **Multi-tenancy**: Todas as queries DEVEM filtrar por `TenantContext.getCurrentOficinaId()`
2. **Autorização**: Apenas ADMIN e GERENTE da oficina podem acessar billing e suporte
3. **Validação**: Verificar se a fatura/ticket pertence à oficina antes de retornar
4. **Audit**: Registrar ações importantes (criação de ticket, download de fatura)

---

## 8. Impacto na UI Existente

### Menu Lateral (MainLayout.tsx)
Adicionar 2 novos itens:
- "Assinatura" (ícone: CreditCard)
- "Suporte" (ícone: HelpCircle ou MessageSquare)

### Dashboard
Considerar adicionar cards resumidos:
- Status da assinatura (próxima cobrança)
- Tickets abertos

---

## Arquivos a Criar

### Backend (8 arquivos)
```
src/main/java/com/pitstop/oficina/
├── controller/
│   ├── OficinaBillingController.java
│   └── OficinaSuporteController.java
├── service/
│   ├── OficinaBillingService.java
│   └── OficinaSuporteService.java
└── dto/
    ├── billing/
    │   ├── PlanoAtualDTO.java
    │   ├── ConsumoDTO.java
    │   ├── FaturaOficinaDTO.java
    │   └── PagamentoOficinaDTO.java
    └── suporte/
        ├── CreateTicketRequest.java
        ├── TicketOficinaDTO.java
        └── CreateMensagemRequest.java
```

### Frontend (20+ arquivos)
```
frontend/src/features/
├── billing/
│   ├── components/
│   │   ├── FaturaCard.tsx
│   │   ├── ConsumoGauge.tsx
│   │   ├── PlanoCard.tsx
│   │   ├── FeaturesCard.tsx
│   │   └── PagamentoRow.tsx
│   ├── hooks/useBilling.ts
│   ├── pages/
│   │   ├── BillingDashboardPage.tsx
│   │   ├── FaturasPage.tsx
│   │   └── FaturaDetailPage.tsx
│   ├── services/billingService.ts
│   └── types/index.ts
└── suporte/
    ├── components/
    │   ├── TicketCard.tsx
    │   ├── TicketForm.tsx
    │   ├── MensagemItem.tsx
    │   └── ChatSuporte.tsx
    ├── hooks/useSuporte.ts
    ├── pages/
    │   ├── TicketsPage.tsx
    │   ├── TicketDetailPage.tsx
    │   └── NovoTicketPage.tsx
    ├── services/suporteService.ts
    └── types/index.ts
```

---

**Documento criado em:** 2026-01-10
**Última atualização:** 2026-01-10
**Versão:** 1.0.0
