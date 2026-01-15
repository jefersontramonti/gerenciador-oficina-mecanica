# CLAUDE.md - Frontend PitStop

Este arquivo fornece orientações para o Claude Code ao trabalhar no frontend do PitStop.

## Stack Tecnológica

- **React 19.0.0** com TypeScript 5.9
- **Vite 6.0+** - Build tool
- **React Router 7.0** - Roteamento
- **React Query 5.62** - State management do servidor
- **Redux Toolkit 2.9** - State management da UI
- **Axios 1.7.9** - Cliente HTTP com interceptors JWT
- **React Hook Form 7.54** + Zod 3.24 - Formulários e validação
- **Tailwind CSS 4.0** - Estilização
- **Lucide React** - Ícones

## 🔐 Segurança

**IMPORTANTE:** Leia `SECURITY.md` para detalhes completos sobre segurança.

### Armazenamento de Tokens

✅ **Access Token:** Armazenado APENAS em memória (variável JavaScript)
✅ **Refresh Token:** Armazenado em HttpOnly cookie (gerenciado pelo backend)
❌ **NUNCA** armazene tokens em localStorage ou sessionStorage

```typescript
// ❌ NUNCA FAÇA ISSO
localStorage.setItem('token', accessToken);

// ✅ Use as funções fornecidas
import { setAccessToken, getAccessToken } from '@/shared/services/api';
setAccessToken(token); // Armazena apenas em memória
```

### Auto-Autenticação

O app tenta restaurar a sessão automaticamente usando o refresh token:

```typescript
// Executado no AuthInitializer na inicialização do app
dispatch(initializeAuth());
// Tenta refresh token → Se sucesso, busca perfil do usuário
```

### Proteção XSS

- Access token em memória = inacessível a scripts XSS
- Refresh token em HttpOnly cookie = inacessível a JavaScript
- User data em localStorage = apenas dados públicos (nome, email, perfil)

## Estrutura do Projeto

```
src/
├── features/              # Módulos de features
│   ├── auth/             # Autenticação (login, registro)
│   ├── clientes/         # Gestão de clientes
│   ├── veiculos/         # Gestão de veículos
│   └── [feature]/
│       ├── components/   # Componentes específicos da feature
│       ├── hooks/        # React Query hooks
│       ├── pages/        # Páginas da feature
│       ├── services/     # Chamadas à API
│       ├── store/        # Redux slices (se necessário)
│       ├── types/        # TypeScript types
│       └── utils/        # Utilitários da feature
├── shared/
│   ├── components/       # Componentes reutilizáveis
│   │   ├── forms/       # InputMask, DatePicker, etc
│   │   └── ui/          # Button, Card, Modal, etc
│   ├── hooks/           # Hooks compartilhados
│   ├── services/        # Configuração Axios, API base
│   ├── store/           # Redux store global
│   └── types/           # Types globais
└── App.tsx
```

## Padrões de Implementação

### 1. Filtros em Páginas de Listagem

**IMPORTANTE:** Sempre siga o padrão documentado em `PADRAO_FILTROS.md`

**Pontos críticos:**
- ✅ Use `defaultValue` nos inputs (não `value`)
- ✅ Nunca faça early return com `if (isLoading)`
- ✅ Mostre loading DENTRO da tabela
- ✅ Handlers `handleSearch` e `handleFilterChange` separados
- ✅ Atualização imediata do estado (sem debounce)

**Referências:**
- `features/clientes/pages/ClientesListPage.tsx` - Implementação correta
- `features/veiculos/pages/VeiculosListPage.tsx` - Implementação correta
- `PADRAO_FILTROS.md` - Documentação completa

### 2. React Query Hooks

```typescript
// features/[feature]/hooks/use[Feature].ts

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

// Query keys para cache
export const entityKeys = {
  all: ['entities'] as const,
  lists: () => [...entityKeys.all, 'list'] as const,
  list: (filters: Filters) => [...entityKeys.lists(), filters] as const,
  details: () => [...entityKeys.all, 'detail'] as const,
  detail: (id: string) => [...entityKeys.details(), id] as const,
};

// Hook para listar
export const useEntities = (filters: Filters = {}) => {
  return useQuery({
    queryKey: entityKeys.list(filters),
    queryFn: () => entityService.findAll(filters),
    staleTime: 1 * 60 * 1000, // 1 minuto
  });
};

// Hook para buscar por ID
export const useEntity = (id?: string) => {
  return useQuery({
    queryKey: entityKeys.detail(id || ''),
    queryFn: () => entityService.findById(id!),
    enabled: !!id, // Importante: enabled dentro do hook
    staleTime: 2 * 60 * 1000,
  });
};

// Hook para criar
export const useCreateEntity = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateRequest) => entityService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: entityKeys.lists() });
    },
  });
};

// Hook para atualizar
export const useUpdateEntity = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateRequest }) =>
      entityService.update(id, data),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: entityKeys.detail(updated.id) });
      queryClient.invalidateQueries({ queryKey: entityKeys.lists() });
    },
  });
};

// Hook para deletar
export const useDeleteEntity = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => entityService.delete(id),
    onSuccess: (_, deletedId) => {
      queryClient.invalidateQueries({ queryKey: entityKeys.lists() });
      queryClient.removeQueries({ queryKey: entityKeys.detail(deletedId) });
    },
  });
};
```

**Padrão importante:**
- ✅ `enabled: !!id` DENTRO do hook (não como parâmetro)
- ✅ Hook aceita `id?: string` (opcional)
- ✅ Invalidação correta do cache após mutações

### 3. Services (Chamadas à API)

```typescript
// features/[feature]/services/[feature]Service.ts

import { api } from '@/shared/services/api';
import type { Entity, Filters, CreateRequest, UpdateRequest } from '../types';

export const entityService = {
  async findAll(filters: Filters = {}) {
    const { data } = await api.get<PageResponse<Entity>>('/entities', {
      params: filters,
    });
    return data;
  },

  async findById(id: string) {
    const { data } = await api.get<Entity>(`/entities/${id}`);
    return data;
  },

  async create(request: CreateRequest) {
    const { data } = await api.post<Entity>('/entities', request);
    return data;
  },

  async update(id: string, request: UpdateRequest) {
    const { data } = await api.put<Entity>(`/entities/${id}`, request);
    return data;
  },

  async delete(id: string) {
    await api.delete(`/entities/${id}`);
  },
};
```

### 4. Validação com Zod

```typescript
// features/[feature]/utils/validation.ts

import { z } from 'zod';

export const createEntitySchema = z.object({
  campo1: z.string().min(3, 'Mínimo 3 caracteres'),
  campo2: z.string().email('Email inválido'),
  campo3: z.number().min(0, 'Deve ser positivo').optional(),
});

export const updateEntitySchema = createEntitySchema.partial({
  campo1: true, // Torna opcional na atualização
});

export type CreateEntityFormData = z.infer<typeof createEntitySchema>;
export type UpdateEntityFormData = z.infer<typeof updateEntitySchema>;
```

### 5. Formulários com React Hook Form

```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

const {
  register,
  handleSubmit,
  formState: { errors },
} = useForm<FormData>({
  resolver: zodResolver(schema),
});

const onSubmit = async (data: FormData) => {
  try {
    await mutation.mutateAsync(data);
    navigate('/path');
  } catch (error: any) {
    if (error.response?.status === 409) {
      alert('Conflito');
    } else {
      alert('Erro');
    }
  }
};

return (
  <form onSubmit={handleSubmit(onSubmit)}>
    <input {...register('campo')} />
    {errors.campo && <p className="text-red-500">{errors.campo.message}</p>}
  </form>
);
```

## Máscaras de Input

Use o componente `InputMask` de `@/shared/components/forms/InputMask`:

```typescript
import { Controller } from 'react-hook-form';
import { InputMask } from '@/shared/components/forms/InputMask';

<Controller
  name="cpfCnpj"
  control={control}
  render={({ field }) => (
    <InputMask
      {...field}
      mask="cpfCnpj"
      label="CPF/CNPJ"
      required
      error={errors.cpfCnpj?.message}
    />
  )}
/>
```

**Máscaras disponíveis:**
- `cpf` - 000.000.000-00
- `cnpj` - 00.000.000/0000-00
- `cpfCnpj` - Detecta automaticamente
- `telefone` - (00) 0000-0000
- `celular` - (00) 00000-0000
- `cep` - 00000-000
- `placa` - ABC-1234 ou ABC1D23
- `chassi` - VIN 17 caracteres

## Tratamento de Erros

### 1. Configuração do Axios

O arquivo `src/shared/services/api.ts` já possui interceptors para:
- Adicionar JWT token automaticamente
- Refresh token automático em 401
- Logout em 403
- Tratamento de erros de rede

### 2. Tratamento em Componentes

```typescript
// Em mutations
try {
  await mutation.mutateAsync(data);
} catch (error: any) {
  if (error.response?.status === 409) {
    alert('Recurso já existe');
  } else if (error.response?.status === 404) {
    alert('Não encontrado');
  } else {
    alert(error.message || 'Erro desconhecido');
  }
}

// Em queries
const { data, isLoading, error } = useEntity(id);

if (error) {
  return (
    <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
      Erro ao carregar. Tente novamente.
    </div>
  );
}
```

## Dark Mode e Responsividade (OBRIGATÓRIO)

**IMPORTANTE:** Todo componente deve suportar tema claro/escuro E ser responsivo para mobile.

### Regra de Ouro

Ao escrever qualquer classe Tailwind para cores, **SEMPRE** adicione a variante `dark:`:

```tsx
// ❌ ERRADO - Apenas tema claro
className="bg-white text-gray-900 border-gray-200"

// ✅ CORRETO - Ambos os temas
className="bg-white dark:bg-gray-800 text-gray-900 dark:text-white border-gray-200 dark:border-gray-700"
```

### Padrão de Cores - Dark Mode

| Elemento | Light | Dark |
|----------|-------|------|
| Background principal | `bg-white` | `dark:bg-gray-800` |
| Background secundário | `bg-gray-50` | `dark:bg-gray-700/50` |
| Background sutil | `bg-gray-100` | `dark:bg-gray-700` |
| Texto primário | `text-gray-900` | `dark:text-white` ou `dark:text-gray-100` |
| Texto secundário | `text-gray-600` | `dark:text-gray-400` |
| Texto muted | `text-gray-500` | `dark:text-gray-400` |
| Bordas | `border-gray-200` | `dark:border-gray-700` ou `dark:border-gray-600` |
| Hover background | `hover:bg-gray-50` | `dark:hover:bg-gray-700` |

### Padrão de Cores - Status/Badges

```tsx
// Success
className="bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-400"

// Warning
className="bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-400"

// Error
className="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-400"

// Info
className="bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400"
```

### Responsividade - Breakpoints

| Breakpoint | Tamanho | Uso |
|------------|---------|-----|
| (default) | < 640px | Mobile |
| `sm:` | >= 640px | Mobile landscape / Tablet pequeno |
| `md:` | >= 768px | Tablet |
| `lg:` | >= 1024px | Desktop |
| `xl:` | >= 1280px | Desktop grande |

### Padrões Responsivos Comuns

```tsx
// Padding
className="p-3 sm:p-4 lg:p-6"

// Texto
className="text-sm sm:text-base"
className="text-xs sm:text-sm"

// Ícones
className="h-4 w-4 sm:h-5 sm:w-5"

// Layout flex
className="flex flex-col sm:flex-row"
className="flex-col sm:flex-row sm:items-center"

// Grid
className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3"
className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5"

// Gap
className="gap-2 sm:gap-3 lg:gap-4"

// Esconder/Mostrar
className="hidden sm:block"  // Esconde em mobile
className="sm:hidden"        // Mostra só em mobile
```

### Exemplo Completo - Card Responsivo com Dark Mode

```tsx
<div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 shadow-sm p-3 sm:p-4 lg:p-6">
  <h3 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-white mb-2">
    Título
  </h3>
  <p className="text-sm text-gray-600 dark:text-gray-400">
    Descrição do card
  </p>
  <div className="mt-3 sm:mt-4 flex flex-col sm:flex-row gap-2">
    <button className="flex-1 sm:flex-none px-3 sm:px-4 py-2 bg-blue-600 dark:bg-blue-700 text-white rounded-lg hover:bg-blue-700 dark:hover:bg-blue-600 text-sm sm:text-base">
      Ação Principal
    </button>
    <button className="flex-1 sm:flex-none px-3 sm:px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 text-sm sm:text-base">
      Cancelar
    </button>
  </div>
</div>
```

### Modais Responsivos

```tsx
// Overlay
className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-2 sm:p-4"

// Modal container
className="bg-white dark:bg-gray-800 rounded-lg p-4 sm:p-6 max-w-sm sm:max-w-md w-full shadow-xl"

// Título
className="text-lg font-semibold text-gray-900 dark:text-white mb-2"

// Botões do modal
className="flex flex-col-reverse sm:flex-row justify-end gap-2 mt-4"
```

### Inputs com Dark Mode

```tsx
className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 px-3 py-2 text-sm sm:text-base focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 focus:border-transparent"
```

### Referências de Implementação

- `features/clientes/pages/ClientesListPage.tsx` - Lista com dark mode
- `features/ordens-servico/pages/OrdemServicoDetailPage.tsx` - Detail page completa
- `features/anexos/components/` - Componentes com dark mode e responsividade

---

## Estilos com Tailwind (Padrões Legados)

> **Nota:** Os padrões abaixo são legados. Sempre adicione as variantes `dark:` e responsivas conforme documentado acima.

### Classes Padrão

**Botões:**
```tsx
// Primary (com dark mode)
className="rounded-lg bg-blue-600 dark:bg-blue-700 px-3 sm:px-4 py-2 text-white hover:bg-blue-700 dark:hover:bg-blue-600 text-sm sm:text-base"

// Secondary (com dark mode)
className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 sm:px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 text-sm sm:text-base"

// Danger (com dark mode)
className="rounded-lg bg-red-600 dark:bg-red-700 px-3 sm:px-4 py-2 text-white hover:bg-red-700 dark:hover:bg-red-600 text-sm sm:text-base"
```

**Inputs:**
```tsx
className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-3 py-2 text-sm sm:text-base focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
```

**Cards:**
```tsx
className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow border border-gray-200 dark:border-gray-700"
```

**Tabelas:**
```tsx
// Container
className="overflow-hidden rounded-lg bg-white dark:bg-gray-800 shadow border border-gray-200 dark:border-gray-700"

// Table
className="w-full divide-y divide-gray-200 dark:divide-gray-700"

// Header
className="bg-gray-50 dark:bg-gray-700"
className="px-4 sm:px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700 dark:text-gray-300"

// Body
className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800"
className="px-4 sm:px-6 py-4 text-sm text-gray-900 dark:text-gray-100"
```

## Navegação

### Programática
```typescript
import { useNavigate } from 'react-router-dom';

const navigate = useNavigate();
navigate('/path');
navigate(-1); // Voltar
```

### Links
```typescript
import { Link } from 'react-router-dom';

<Link to="/path">Texto</Link>
```

## Autenticação

O contexto de autenticação está em `src/shared/contexts/AuthContext.tsx`:

```typescript
import { useAuth } from '@/shared/contexts/AuthContext';

const { user, isAuthenticated, login, logout } = useAuth();
```

**Rotas protegidas:**
O `PrivateRoute` component já verifica autenticação automaticamente.

## Build e Deploy

```bash
# Desenvolvimento
npm run dev

# Build de produção
npm run build

# Preview da build
npm run preview

# Lint
npm run lint
```

## Troubleshooting

### Problema: Input perde foco ao digitar em filtros
**Solução:** Verifique `PADRAO_FILTROS.md` - use `defaultValue` e remova early return de loading

### Problema: Query não atualiza após mutação
**Solução:** Adicione `invalidateQueries` no `onSuccess` da mutation

### Problema: 401 em requisições
**Solução:** Verifique se o token está sendo renovado corretamente no `api.ts`

### Problema: TypeScript errors em tipos
**Solução:** Verifique se os types do backend estão sincronizados com o frontend

## Referências Importantes

- `PADRAO_FILTROS.md` - Padrão de implementação de filtros (OBRIGATÓRIO)
- `src/features/clientes/` - Feature completa de referência
- `src/features/veiculos/` - Feature completa de referência
- `src/features/anexos/components/` - Referência de dark mode e responsividade
- `src/shared/services/api.ts` - Configuração do Axios
- `src/shared/contexts/AuthContext.tsx` - Autenticação

---

**Última atualização:** 2026-01-13
**Versão:** 1.1.0 - Adicionado padrões de Dark Mode e Responsividade
