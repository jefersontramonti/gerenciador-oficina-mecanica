# PitStop Frontend

Interface web moderna para o sistema de gerenciamento de oficinas mecânicas PitStop, construída com React 19, TypeScript e Vite.

## 🚀 Stack Tecnológica

### Core
- React 19 + TypeScript 5.9
- Vite 6.0+ (Build tool)

### Estado e Dados
- Redux Toolkit 2.9.0 (UI state)
- React Query 5.62.0 (Server state)
- Axios 1.7.9 (HTTP client)

### UI e Estilos
- Tailwind CSS 3.x
- Lucide React (Icons)
- React Hook Form + Zod

### Real-time
- STOMP.js + SockJS (WebSocket)

## 📁 Estrutura

```
src/
├── config/         # Configurações
├── features/       # Módulos (DDD)
│   ├── auth/
│   ├── clientes/
│   ├── veiculos/
│   └── ...
└── shared/         # Código compartilhado
    ├── components/
    ├── hooks/
    ├── layouts/
    └── services/
```

## 🛠️ Comandos

```bash
npm install       # Instalar dependências
npm run dev       # Dev server (porta 5173)
npm run build     # Build produção
npm run preview   # Preview build
```

## 🔐 Autenticação

- JWT com refresh tokens automáticos
- RBAC: ADMIN, GERENTE, ATENDENTE, MECANICO
- Token armazenado em memória (segurança)

## 📝 Próximos Passos

1. Implementar módulos (Clientes, Veículos, OS)
2. Criar componentes UI base
3. Integrar com backend conforme implementado

## 📖 Documentação

Ver README.md completo para detalhes de arquitetura, convenções e recursos.
