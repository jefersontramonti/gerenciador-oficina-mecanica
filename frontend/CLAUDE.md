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

## Estilos com Tailwind

### Classes Padrão

**Botões:**
```tsx
// Primary
className="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"

// Secondary
className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50"

// Danger
className="rounded-lg border border-red-600 px-4 py-2 text-red-600 hover:bg-red-50"
```

**Inputs:**
```tsx
className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
```

**Cards:**
```tsx
className="rounded-lg bg-white p-6 shadow"
```

**Tabelas:**
```tsx
// Container
className="overflow-hidden rounded-lg bg-white shadow"

// Table
className="w-full divide-y divide-gray-200"

// Header
className="bg-gray-50"
className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-700"

// Body
className="divide-y divide-gray-200 bg-white"
className="px-6 py-4 text-sm text-gray-900"
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
- `src/shared/services/api.ts` - Configuração do Axios
- `src/shared/contexts/AuthContext.tsx` - Autenticação

---

**Última atualização:** 2025-11-01
**Versão:** 1.0.0
