# Sistema de Notificações - Frontend

Sistema completo de gerenciamento de notificações do PitStop, incluindo configuração de canais (Email, WhatsApp, SMS) e histórico de envios.

## Estrutura

```
notificacoes/
├── components/
│   ├── WhatsAppStatusCard.tsx      # Card de status da integração WhatsApp
│   ├── MetricasCards.tsx           # Cards de métricas de notificações
│   ├── NotificacaoDetailModal.tsx  # Modal de detalhes da notificação
│   └── index.ts
├── hooks/
│   └── useNotificacoes.ts          # React Query hooks
├── pages/
│   ├── ConfiguracaoNotificacoesPage.tsx  # Página de configuração
│   ├── HistoricoNotificacoesPage.tsx     # Página de histórico
│   └── index.ts
├── services/
│   └── notificacaoService.ts       # Cliente API
├── types/
│   └── index.ts                    # TypeScript types
└── README.md
```

## Funcionalidades

### 1. Configuração de Notificações (`/notificacoes/configuracao`)

- **Métricas em tempo real**: Total enviadas, pendentes, erros, taxa de sucesso
- **Status WhatsApp**: Conexão/desconexão, QR Code, status da instância
- **Teste de notificações**: Envie testes para validar configuração
- **Ativação de canais**: Toggle para ativar/desativar Email, WhatsApp, SMS

### 2. Histórico de Notificações (`/notificacoes/historico`)

- **Listagem completa**: Todas as notificações enviadas
- **Filtros avançados**: Por tipo, status, evento, destinatário
- **Paginação**: 20 itens por página
- **Detalhes**: Modal com informações completas da notificação
- **Ações**: Reenviar (erro), Cancelar (pendente)

## Componentes

### WhatsAppStatusCard

Card que exibe o status da integração WhatsApp e permite conectar/desconectar.

**Features:**
- Auto-refresh a cada 30 segundos
- QR Code para conexão
- Informações da instância (ID, telefone)
- Última verificação

```tsx
import { WhatsAppStatusCard } from '@/features/notificacoes/components';

<WhatsAppStatusCard />
```

### MetricasCards

Exibe métricas gerais do sistema de notificações.

**Features:**
- Cards de resumo (enviadas, pendentes, erros, taxa de sucesso)
- Distribuição por tipo (Email, WhatsApp, SMS)
- Distribuição por evento (OS criada, aprovada, etc.)
- Barras de progresso

```tsx
import { MetricasCards } from '@/features/notificacoes/components';

<MetricasCards />
```

### NotificacaoDetailModal

Modal para exibir detalhes completos de uma notificação.

**Features:**
- Informações completas (destinatário, mensagem, status)
- Histórico de tentativas
- Mensagem de erro (se houver)
- Ações: Reenviar, Cancelar

```tsx
import { NotificacaoDetailModal } from '@/features/notificacoes/components';

const [selectedId, setSelectedId] = useState<string | null>(null);

<NotificacaoDetailModal
  isOpen={!!selectedId}
  onClose={() => setSelectedId(null)}
  notificacaoId={selectedId!}
/>
```

## React Query Hooks

### useNotificacoes

Lista notificações com filtros e paginação.

```tsx
import { useNotificacoes } from '@/features/notificacoes/hooks/useNotificacoes';

const { data, isLoading, error } = useNotificacoes({
  tipo: 'EMAIL',
  status: 'ENVIADA',
  page: 0,
  size: 20,
});
```

### useNotificacao

Busca uma notificação específica por ID.

```tsx
import { useNotificacao } from '@/features/notificacoes/hooks/useNotificacoes';

const { data: notificacao } = useNotificacao(id);
```

### useNotificacaoMetricas

Obtém métricas gerais do sistema.

```tsx
import { useNotificacaoMetricas } from '@/features/notificacoes/hooks/useNotificacoes';

const { data: metricas } = useNotificacaoMetricas();
```

### useConfiguracoes

Lista todas as configurações de notificação.

```tsx
import { useConfiguracoes } from '@/features/notificacoes/hooks/useNotificacoes';

const { data: configuracoes } = useConfiguracoes();
```

### useWhatsAppStatus

Obtém status da integração WhatsApp (auto-refresh 30s).

```tsx
import { useWhatsAppStatus } from '@/features/notificacoes/hooks/useNotificacoes';

const { data: status } = useWhatsAppStatus();
```

### Mutations

```tsx
import {
  useCreateNotificacao,
  useRetryNotificacao,
  useCancelNotificacao,
  useUpdateConfiguracao,
  useConnectWhatsApp,
  useDisconnectWhatsApp,
  useTestarNotificacao,
} from '@/features/notificacoes/hooks/useNotificacoes';

// Criar notificação
const createMutation = useCreateNotificacao();
await createMutation.mutateAsync({
  tipo: 'EMAIL',
  destinatario: 'cliente@email.com',
  mensagem: 'Sua OS foi aprovada!',
});

// Testar configuração
const testMutation = useTestarNotificacao();
const result = await testMutation.mutateAsync({
  tipo: 'WHATSAPP',
  destinatario: '5511999999999',
  mensagem: 'Teste de integração',
});
```

## Types

### TipoNotificacao

```typescript
type TipoNotificacao = 'EMAIL' | 'SMS' | 'WHATSAPP' | 'PUSH';
```

### StatusNotificacao

```typescript
type StatusNotificacao = 'PENDENTE' | 'ENVIADA' | 'ERRO' | 'CANCELADA';
```

### EventoNotificacao

```typescript
type EventoNotificacao =
  | 'OS_CRIADA'
  | 'OS_APROVADA'
  | 'OS_EM_ANDAMENTO'
  | 'OS_FINALIZADA'
  | 'OS_ENTREGUE'
  | 'OS_CANCELADA'
  | 'PAGAMENTO_RECEBIDO'
  | 'PAGAMENTO_VENCIDO'
  | 'ESTOQUE_BAIXO'
  | 'ORCAMENTO_ENVIADO';
```

## API Endpoints

### GET `/api/notificacoes/historico`

Lista notificações com filtros.

**Query Params:**
- `tipo`: TipoNotificacao
- `status`: StatusNotificacao
- `eventoTipo`: EventoNotificacao
- `destinatario`: string
- `dataInicio`: ISO date
- `dataFim`: ISO date
- `page`: number
- `size`: number
- `sort`: string

### GET `/api/notificacoes/metricas`

Obtém métricas do sistema.

### GET `/api/notificacoes/configuracao`

Lista todas as configurações.

### PUT `/api/notificacoes/configuracao/{tipo}`

Atualiza configuração de um canal.

### GET `/api/notificacoes/whatsapp/status`

Status da integração WhatsApp.

### POST `/api/notificacoes/whatsapp/conectar`

Conecta instância WhatsApp.

### POST `/api/notificacoes/whatsapp/desconectar`

Desconecta instância WhatsApp.

### POST `/api/notificacoes/testar`

Testa configuração de notificação.

## Permissões

- **ADMIN**: Acesso total
- **GERENTE**: Acesso total
- **ATENDENTE**: Sem acesso
- **MECANICO**: Sem acesso

## Navegação

As rotas foram adicionadas ao menu lateral:

```
Notificações → /notificacoes/configuracao
  ├── Configuração
  └── Histórico → /notificacoes/historico
```

## Padrões Seguidos

1. **Feature-based organization**: Tudo dentro de `features/notificacoes`
2. **React Query**: Gerenciamento de estado do servidor
3. **TypeScript strict mode**: Sem `any` types
4. **Naming conventions**: PascalCase components, camelCase functions
5. **Error handling**: Try-catch com user feedback
6. **Loading states**: Skeleton loaders e spinners
7. **Responsive design**: Mobile-first com Tailwind
8. **Accessibility**: ARIA labels e keyboard navigation

## Próximos Passos

1. Implementar WebSocket para notificações em tempo real
2. Adicionar filtros de data range picker
3. Export de histórico (Excel/PDF)
4. Templates de mensagens personalizáveis
5. Agendamento de notificações

---

**Implementado em**: 2025-12-23
**Versão**: 1.0.0
