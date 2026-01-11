# PitStop - Melhorias de Performance Pendentes

**Data da Auditoria:** 11 de janeiro de 2026
**Status:** Pendente de implementação

---

## Resumo

| Área | Quantidade | Impacto |
|------|------------|---------|
| Frontend | 8 | Alto |
| Backend | 7 | Alto |

---

## FRONTEND - ALTA PRIORIDADE

### 1. [CRÍTICA] Implementar Code Splitting e Lazy Loading

**Problema:** Todas as rotas são carregadas de uma vez, aumentando o bundle inicial.

**Arquivo:** `frontend/src/App.tsx`

**DE:**
```typescript
import { DashboardPage } from '@/features/dashboard/pages/DashboardPage';
import { ClientesListPage } from '@/features/clientes/pages/ClientesListPage';
import { VeiculosListPage } from '@/features/veiculos/pages/VeiculosListPage';
// ... todas as importações diretas
```

**PARA:**
```typescript
import { lazy, Suspense } from 'react';
import { LoadingSpinner } from '@/shared/components/ui/LoadingSpinner';

// Lazy loading de todas as páginas
const DashboardPage = lazy(() => import('@/features/dashboard/pages/DashboardPage').then(m => ({ default: m.DashboardPage })));
const ClientesListPage = lazy(() => import('@/features/clientes/pages/ClientesListPage').then(m => ({ default: m.ClientesListPage })));
const VeiculosListPage = lazy(() => import('@/features/veiculos/pages/VeiculosListPage').then(m => ({ default: m.VeiculosListPage })));
const OrdemServicoListPage = lazy(() => import('@/features/ordens-servico/pages/OrdemServicoListPage').then(m => ({ default: m.OrdemServicoListPage })));
const EstoqueListPage = lazy(() => import('@/features/estoque/pages/EstoqueListPage').then(m => ({ default: m.EstoqueListPage })));
const FinanceiroPage = lazy(() => import('@/features/financeiro/pages/FinanceiroPage').then(m => ({ default: m.FinanceiroPage })));

// Admin pages (SUPER_ADMIN only - carregar sob demanda)
const AdminDashboardPage = lazy(() => import('@/features/admin/pages/AdminDashboardPage').then(m => ({ default: m.AdminDashboardPage })));
const OficinasPage = lazy(() => import('@/features/admin/pages/OficinasPage').then(m => ({ default: m.OficinasPage })));

// Wrapper para Suspense
const LazyPage = ({ children }: { children: React.ReactNode }) => (
  <Suspense fallback={<div className="flex h-64 items-center justify-center"><LoadingSpinner /></div>}>
    {children}
  </Suspense>
);

// Nas rotas:
<Route path="/" element={<LazyPage><DashboardPage /></LazyPage>} />
```

**Benefício:** Reduz bundle inicial em ~60-70%.

---

### 2. [CRÍTICA] Usar React.memo em Componentes de Lista

**Problema:** Nenhum componente usa `React.memo`, causando re-renders desnecessários em listas.

**Arquivos afetados:**
- `frontend/src/features/clientes/components/ClienteCard.tsx`
- `frontend/src/features/veiculos/components/VeiculoCard.tsx`
- `frontend/src/features/ordens-servico/components/OrdemServicoCard.tsx`
- `frontend/src/features/estoque/components/PecaCard.tsx`

**Exemplo de implementação:**

```typescript
// DE:
export const ClienteCard = ({ cliente, onClick }: ClienteCardProps) => {
  return (
    <div onClick={onClick}>
      {/* conteúdo */}
    </div>
  );
};

// PARA:
import { memo } from 'react';

export const ClienteCard = memo(({ cliente, onClick }: ClienteCardProps) => {
  return (
    <div onClick={onClick}>
      {/* conteúdo */}
    </div>
  );
});

ClienteCard.displayName = 'ClienteCard';
```

**Componentes de tabela também:**
```typescript
// ItemOSTable.tsx
export const ItemOSTable = memo(({ items, onRemove, onEdit }: Props) => {
  // ...
});
```

---

### 3. [ALTA] Otimizar Handlers com useCallback

**Problema:** Funções de callback são recriadas a cada render, invalidando memoização.

**Arquivo exemplo:** `frontend/src/features/ordens-servico/pages/OrdemServicoListPage.tsx`

**DE:**
```typescript
const handleStatusChange = (id: string, status: string) => {
  updateStatus.mutate({ id, status });
};

const handleSearch = (term: string) => {
  setFilters(prev => ({ ...prev, search: term }));
};
```

**PARA:**
```typescript
import { useCallback, useMemo } from 'react';

const handleStatusChange = useCallback((id: string, status: string) => {
  updateStatus.mutate({ id, status });
}, [updateStatus]);

const handleSearch = useCallback((term: string) => {
  setFilters(prev => ({ ...prev, search: term }));
}, []);

// Para valores derivados
const filteredItems = useMemo(() =>
  items?.filter(item => item.status === filters.status),
  [items, filters.status]
);
```

---

### 4. [ALTA] Lazy Load de ECharts

**Problema:** Biblioteca ECharts (~800KB) é carregada mesmo para usuários que não acessam o Dashboard.

**Arquivo:** `frontend/src/features/dashboard/components/charts/`

**Implementação:**
```typescript
// ChartWrapper.tsx
import { lazy, Suspense } from 'react';

const EChartsReact = lazy(() => import('echarts-for-react'));

export const ChartWrapper = ({ options, ...props }) => (
  <Suspense fallback={<div className="h-64 animate-pulse bg-gray-100 rounded-lg" />}>
    <EChartsReact option={options} {...props} />
  </Suspense>
);
```

**Ou usar dynamic import no hook:**
```typescript
// useDashboard.ts
const [ECharts, setECharts] = useState<typeof import('echarts-for-react') | null>(null);

useEffect(() => {
  import('echarts-for-react').then(module => {
    setECharts(() => module.default);
  });
}, []);
```

---

### 5. [MÉDIA] Configurar React Query com staleTime Adequado

**Problema:** Muitas queries refazem requisições desnecessárias.

**Arquivo:** `frontend/src/config/queryClient.ts`

**DE:**
```typescript
export const queryClient = new QueryClient();
```

**PARA:**
```typescript
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1 * 60 * 1000,      // 1 minuto (dados "frescos")
      gcTime: 5 * 60 * 1000,          // 5 minutos (cache garbage collection)
      refetchOnWindowFocus: false,    // Não refetch ao focar janela
      refetchOnReconnect: true,       // Refetch ao reconectar
      retry: 1,                       // Tentar apenas 1 vez
      retryDelay: 1000,               // Delay de 1s entre retries
    },
    mutations: {
      retry: 0,                       // Mutations não fazem retry
    },
  },
});
```

**Para queries específicas que precisam de dados mais frescos:**
```typescript
// Em hooks específicos
useQuery({
  queryKey: ['dashboard', 'stats'],
  queryFn: fetchDashboardStats,
  staleTime: 30 * 1000, // 30 segundos para dados de dashboard
  refetchInterval: 60 * 1000, // Atualizar a cada minuto
});
```

---

### 6. [MÉDIA] Virtualização de Listas Longas

**Problema:** Listas com muitos itens renderizam todos os elementos, impactando performance.

**Instalar:**
```bash
npm install @tanstack/react-virtual
```

**Implementação:**
```typescript
// ListaVirtualizada.tsx
import { useVirtualizer } from '@tanstack/react-virtual';

export const ListaVirtualizada = ({ items }: { items: Item[] }) => {
  const parentRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: items.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 72, // altura estimada de cada item
    overscan: 5, // itens extras para scroll suave
  });

  return (
    <div ref={parentRef} className="h-[600px] overflow-auto">
      <div
        style={{
          height: `${virtualizer.getTotalSize()}px`,
          width: '100%',
          position: 'relative',
        }}
      >
        {virtualizer.getVirtualItems().map((virtualItem) => (
          <div
            key={virtualItem.key}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: `${virtualItem.size}px`,
              transform: `translateY(${virtualItem.start}px)`,
            }}
          >
            <ItemCard item={items[virtualItem.index]} />
          </div>
        ))}
      </div>
    </div>
  );
};
```

---

### 7. [MÉDIA] Otimizar Bundle Size do Vite

**Arquivo:** `frontend/vite.config.ts`

**Adicionar configurações:**
```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          // Separar vendor em chunks menores
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          'vendor-query': ['@tanstack/react-query'],
          'vendor-forms': ['react-hook-form', '@hookform/resolvers', 'zod'],
          'vendor-redux': ['@reduxjs/toolkit', 'react-redux'],
          'vendor-charts': ['echarts', 'echarts-for-react'],
          'vendor-utils': ['axios', 'date-fns', 'clsx'],
        },
      },
    },
    // Limites de warning
    chunkSizeWarningLimit: 500,
    // Minificação
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // Remove console.log em produção
        drop_debugger: true,
      },
    },
  },
});
```

---

### 8. [BAIXA] Preload de Rotas Críticas

**Arquivo:** `frontend/src/App.tsx`

**Implementação:**
```typescript
// Preload rotas mais acessadas após login
const preloadCriticalRoutes = () => {
  // Preload após 2 segundos de idle
  setTimeout(() => {
    import('@/features/dashboard/pages/DashboardPage');
    import('@/features/ordens-servico/pages/OrdemServicoListPage');
    import('@/features/clientes/pages/ClientesListPage');
  }, 2000);
};

// No AuthInitializer, após login bem-sucedido:
useEffect(() => {
  if (isAuthenticated) {
    preloadCriticalRoutes();
  }
}, [isAuthenticated]);
```

---

## BACKEND - ALTA PRIORIDADE

### 1. [CRÍTICA] Corrigir Evicção Agressiva de Cache

**Problema:** `@CacheEvict(allEntries = true)` invalida todo o cache em cada alteração.

**Arquivos afetados:**
- `OrdemServicoService.java`
- `ClienteService.java`
- `VeiculoService.java`
- `PecaService.java`

**DE:**
```java
@CacheEvict(value = "ordens-servico", allEntries = true)
public OrdemServicoResponseDTO update(Long id, UpdateOrdemServicoDTO dto) {
    // ...
}
```

**PARA:**
```java
@Caching(evict = {
    @CacheEvict(value = "ordens-servico", key = "#id"),
    @CacheEvict(value = "ordens-servico-list", allEntries = true)
})
public OrdemServicoResponseDTO update(Long id, UpdateOrdemServicoDTO dto) {
    // ...
}

// Para busca por ID, usar cache específico
@Cacheable(value = "ordens-servico", key = "#id")
public OrdemServicoResponseDTO findById(Long id) {
    // ...
}

// Para listagem, cache separado com TTL menor
@Cacheable(value = "ordens-servico-list", key = "#filters.hashCode()")
public Page<OrdemServicoResponseDTO> findAll(OrdemServicoFilters filters, Pageable pageable) {
    // ...
}
```

**Configurar TTL diferenciado no CacheConfig:**
```java
@Bean
public RedisCacheConfiguration detailCacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
}

@Bean
public RedisCacheConfiguration listCacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5)) // Listas expiram mais rápido
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
}
```

---

### 2. [CRÍTICA] Prevenir N+1 Queries com Entity Graphs

**Problema:** Lazy loading causa múltiplas queries ao acessar relacionamentos.

**Arquivo:** `OrdemServicoRepository.java`

**DE:**
```java
@Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId")
Page<OrdemServico> findByOficinaId(@Param("oficinaId") Long oficinaId, Pageable pageable);
```

**PARA:**
```java
@EntityGraph(attributePaths = {"cliente", "veiculo", "itens", "itens.peca"})
@Query("SELECT os FROM OrdemServico os WHERE os.oficina.id = :oficinaId")
Page<OrdemServico> findByOficinaIdWithDetails(@Param("oficinaId") Long oficinaId, Pageable pageable);

// Ou definir no Entity
@Entity
@NamedEntityGraph(
    name = "OrdemServico.withDetails",
    attributeNodes = {
        @NamedAttributeNode("cliente"),
        @NamedAttributeNode("veiculo"),
        @NamedAttributeNode(value = "itens", subgraph = "itens-subgraph")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "itens-subgraph",
            attributeNodes = @NamedAttributeNode("peca")
        )
    }
)
public class OrdemServico {
    // ...
}

// Usar no repository
@EntityGraph(value = "OrdemServico.withDetails", type = EntityGraph.EntityGraphType.LOAD)
Optional<OrdemServico> findByIdAndOficinaId(Long id, Long oficinaId);
```

---

### 3. [ALTA] Otimizar Connection Pool

**Arquivo:** `application.yml`

**Adicionar configurações HikariCP:**
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000         # 5 minutos
      max-lifetime: 1800000        # 30 minutos
      connection-timeout: 30000    # 30 segundos
      leak-detection-threshold: 60000  # Detectar leaks após 1 minuto
      pool-name: PitStopHikariPool
```

---

### 4. [ALTA] Adicionar Índices nas Queries Frequentes

**Criar migration:**
```sql
-- V060__add_performance_indexes.sql

-- Índice para busca de OS por status e data
CREATE INDEX IF NOT EXISTS idx_ordens_servico_status_data
ON ordens_servico (oficina_id, status, data_abertura DESC);

-- Índice para busca de veículos por placa
CREATE INDEX IF NOT EXISTS idx_veiculos_placa_oficina
ON veiculos (oficina_id, UPPER(placa));

-- Índice para busca de clientes por CPF/CNPJ
CREATE INDEX IF NOT EXISTS idx_clientes_cpf_cnpj_oficina
ON clientes (oficina_id, cpf_cnpj);

-- Índice para movimentações de estoque
CREATE INDEX IF NOT EXISTS idx_movimentacoes_peca_data
ON movimentacoes_estoque (peca_id, data_movimentacao DESC);

-- Índice para peças com estoque baixo
CREATE INDEX IF NOT EXISTS idx_pecas_estoque_minimo
ON pecas (oficina_id, quantidade_atual, estoque_minimo)
WHERE ativo = true;
```

---

### 5. [MÉDIA] Usar Projeções DTO para Listagens

**Problema:** Carregar entidades completas para listagens desperdiça memória.

**Criar interface de projeção:**
```java
// OrdemServicoSummaryProjection.java
public interface OrdemServicoSummaryProjection {
    Long getId();
    String getNumero();
    String getStatus();
    LocalDateTime getDataAbertura();
    BigDecimal getValorTotal();
    String getClienteNome();
    String getVeiculoPlaca();
    String getVeiculoModelo();
}
```

**No repository:**
```java
@Query("""
    SELECT
        os.id as id,
        os.numero as numero,
        os.status as status,
        os.dataAbertura as dataAbertura,
        os.valorTotal as valorTotal,
        c.nome as clienteNome,
        v.placa as veiculoPlaca,
        v.modelo as veiculoModelo
    FROM OrdemServico os
    JOIN os.cliente c
    JOIN os.veiculo v
    WHERE os.oficina.id = :oficinaId
    """)
Page<OrdemServicoSummaryProjection> findSummaryByOficinaId(
    @Param("oficinaId") Long oficinaId,
    Pageable pageable
);
```

---

### 6. [MÉDIA] Configurar Batch Size para Collections

**Arquivo:** `application.yml`

**Adicionar:**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 25
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true
```

---

### 7. [BAIXA] Compressão de Responses

**Arquivo:** `application.yml`

**Adicionar:**
```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/plain,text/css,application/javascript
    min-response-size: 1024  # Comprimir apenas respostas > 1KB
```

---

## Checklist de Implementação

### Frontend - Alta Prioridade
- [ ] Implementar lazy loading de rotas (React.lazy)
- [ ] Adicionar React.memo em componentes de lista
- [ ] Usar useCallback/useMemo em handlers e valores derivados
- [ ] Lazy load de ECharts
- [ ] Configurar React Query defaultOptions

### Frontend - Média Prioridade
- [ ] Virtualização de listas longas
- [ ] Otimizar chunks do Vite
- [ ] Preload de rotas críticas

### Backend - Alta Prioridade
- [ ] Corrigir cache eviction (remover allEntries=true)
- [ ] Adicionar Entity Graphs para prevenir N+1
- [ ] Configurar HikariCP pool
- [ ] Criar índices de performance

### Backend - Média Prioridade
- [ ] Usar projeções DTO para listagens
- [ ] Configurar Hibernate batch size
- [ ] Habilitar compressão de responses

---

## Métricas de Sucesso

Após implementar as melhorias, verificar:

| Métrica | Antes | Meta |
|---------|-------|------|
| Bundle inicial (gzipped) | ~500KB | < 200KB |
| First Contentful Paint | > 3s | < 1.5s |
| Time to Interactive | > 5s | < 3s |
| Queries por página (OS list) | 15-20 | 2-3 |
| Cache hit rate | ~30% | > 80% |
| Tempo médio de API | > 200ms | < 100ms |

---

## Ferramentas de Monitoramento

### Frontend
```bash
# Analisar bundle
npm run build -- --analyze

# Lighthouse
npx lighthouse http://localhost:5173 --view
```

### Backend
```yaml
# Habilitar logs de SQL para debug (apenas dev)
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

logging:
  level:
    org.hibernate.stat: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
```

---

**Última atualização:** 11 de janeiro de 2026
