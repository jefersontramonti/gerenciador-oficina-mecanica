# Padrão de Implementação de Filtros

Este documento descreve o padrão correto para implementação de filtros em páginas de listagem no frontend PitStop.

## ✅ Padrão Correto (usado em Clientes e Veículos)

### 1. Estrutura do Estado

```typescript
const ITEMS_PER_PAGE = 20;

export const ListPage = () => {
  const [filters, setFilters] = useState<Filters>({
    page: 0,
    size: ITEMS_PER_PAGE,
    sort: 'campo,asc',
  });

  const { data, isLoading, error } = useEntities(filters);
  const deleteMutation = useDeleteEntity();
```

### 2. Handlers de Filtro

```typescript
// Handler para campo de busca principal
const handleSearch = (searchTerm: string) => {
  setFilters((prev) => ({ ...prev, searchField: searchTerm || undefined, page: 0 }));
};

// Handler para outros filtros
const handleFilterChange = (key: keyof Filters, value: any) => {
  setFilters((prev) => ({ ...prev, [key]: value || undefined, page: 0 }));
};

// Handler de paginação
const handlePageChange = (newPage: number) => {
  setFilters((prev) => ({ ...prev, page: newPage }));
};
```

### 3. Renderização de Filtros

```typescript
{/* Filters */}
<div className="mb-6 rounded-lg bg-white p-4 shadow">
  <div className="grid gap-4 md:grid-cols-4">
    {/* Campo de busca principal - 2 colunas */}
    <div className="md:col-span-2">
      <label className="mb-1 block text-sm font-medium text-gray-700">
        Buscar por [campo]
      </label>
      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
        <input
          type="text"
          placeholder="Digite..."
          className="w-full rounded-lg border border-gray-300 py-2 pl-10 pr-4 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          onChange={(e) => handleSearch(e.target.value)}
          defaultValue={filters.searchField}
        />
      </div>
    </div>

    {/* Filtro adicional */}
    <div>
      <label className="mb-1 block text-sm font-medium text-gray-700">Filtro 1</label>
      <input
        type="text"
        placeholder="Filtrar..."
        defaultValue={filters.field1}
        onChange={(e) => handleFilterChange('field1', e.target.value)}
        className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
      />
    </div>

    {/* Botão limpar */}
    <div className="flex items-end">
      <button
        onClick={() =>
          setFilters({ page: 0, size: ITEMS_PER_PAGE, sort: 'campo,asc' })
        }
        className="w-full rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50"
      >
        Limpar Filtros
      </button>
    </div>
  </div>
</div>
```

### 4. Tratamento de Loading e Erro

**❌ ERRADO - Não fazer:**
```typescript
if (isLoading) {
  return <div>Carregando...</div>; // ❌ Desmonta os filtros!
}
```

**✅ CORRETO - Fazer:**
```typescript
// Apenas erro com early return
if (error) {
  return (
    <div className="p-6">
      <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
        Erro ao carregar. Tente novamente.
      </div>
    </div>
  );
}

// Loading dentro da tabela
return (
  <div className="p-6">
    {/* Header e Filtros sempre renderizados */}

    {/* Table */}
    <div className="overflow-hidden rounded-lg bg-white shadow">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gray-50">
            {/* ... cabeçalho ... */}
          </thead>
          <tbody className="divide-y divide-gray-200 bg-white">
            {isLoading ? (
              <tr>
                <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                  Carregando...
                </td>
              </tr>
            ) : data?.content.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                  Nenhum registro encontrado
                </td>
              </tr>
            ) : (
              data?.content.map((item) => (
                <tr key={item.id}>
                  {/* ... células ... */}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-gray-200 bg-white px-6 py-3">
          <div className="text-sm text-gray-700">
            Página {data.number + 1} de {data.totalPages} ({data.totalElements} total)
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => handlePageChange(filters.page! - 1)}
              disabled={data.first}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Anterior
            </button>
            <button
              onClick={() => handlePageChange(filters.page! + 1)}
              disabled={data.last}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Próxima
            </button>
          </div>
        </div>
      )}
    </div>
  </div>
);
```

## 🔑 Pontos Críticos

### 1. **Uso de `defaultValue` ao invés de `value`**
```typescript
// ✅ CORRETO - não força re-render
<input
  defaultValue={filters.campo}
  onChange={(e) => handleSearch(e.target.value)}
/>

// ❌ ERRADO - força re-render e perde foco
<input
  value={localValue}
  onChange={(e) => setLocalValue(e.target.value)}
/>
```

### 2. **Nunca desmontar os filtros**
- ✅ Filtros sempre renderizados
- ✅ Loading mostrado DENTRO da tabela
- ❌ Nunca usar `if (isLoading) return ...` antes dos filtros

### 3. **Atualização imediata do estado**
```typescript
// ✅ CORRETO - atualiza imediatamente
onChange={(e) => handleFilterChange('campo', e.target.value)}

// ❌ ERRADO - usar debounce ou local state
const [localValue, setLocalValue] = useState('');
useEffect(() => {
  const timer = setTimeout(() => {
    setFilters(...);
  }, 500);
  return () => clearTimeout(timer);
}, [localValue]);
```

### 4. **React Query faz a otimização**
O React Query já tem controle de requisições com:
- `staleTime`: Cache de dados
- Deduplicação automática de requisições
- Cancelamento de requisições em andamento

Não é necessário adicionar debounce manual!

## 📋 Checklist de Implementação

- [ ] Estado `filters` com `page`, `size`, `sort`
- [ ] Hook `useEntities(filters)` sem early return para loading
- [ ] Handlers `handleSearch` e `handleFilterChange` separados
- [ ] Inputs com `defaultValue` (não `value`)
- [ ] onChange chama handlers diretamente (sem debounce)
- [ ] Loading renderizado DENTRO da tabela (não early return)
- [ ] Paginação usa `data.first` e `data.last`
- [ ] Botão "Limpar Filtros" reseta para estado inicial
- [ ] Grid com `md:grid-cols-4` (campo principal `md:col-span-2`)

## 🎯 Benefícios deste Padrão

1. **Digitação suave** - Não perde foco do input
2. **Pesquisa em tempo real** - Atualiza a cada letra
3. **Performance** - React Query gerencia cache e requisições
4. **UX consistente** - Mesmo comportamento em todas as páginas
5. **Manutenibilidade** - Padrão claro e documentado

## 📝 Exemplos de Referência

- ✅ `ClientesListPage.tsx` - Implementação correta
- ✅ `VeiculosListPage.tsx` - Implementação correta (após correção)

## ⚠️ Problemas Comuns e Soluções

### Problema: Input perde foco ao digitar
**Causa:** `if (isLoading) return` desmonta o componente
**Solução:** Remover early return, mostrar loading dentro da tabela

### Problema: Muitas requisições simultâneas
**Causa:** Não usar `defaultValue`, forçar re-render
**Solução:** Usar `defaultValue` e deixar React Query gerenciar

### Problema: Filtros não aplicam
**Causa:** Esquecer de resetar `page: 0` ao filtrar
**Solução:** Sempre incluir `page: 0` nos handlers de filtro

---

**Última atualização:** 2025-11-01
**Autor:** Claude Code - PitStop Frontend
