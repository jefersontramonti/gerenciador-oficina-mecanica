# PitStop - Documentação Completa de Funcionalidades

**Sistema de Gerenciamento para Oficinas Mecânicas**

**Versão:** 1.0.0
**Data:** 2024-12-24
**Status:** MVP Completo - Pronto para Produção

---

## Resumo Executivo

| Categoria | Quantidade |
|-----------|------------|
| Controllers (Backend) | 22 |
| Endpoints REST | 60+ |
| Páginas (Frontend) | 40+ |
| Entidades/Tabelas | 16 |
| Migrations | 41 |
| Services | 24+ |

---

## Stack Tecnológica

### Backend
- **Java 25 LTS** + **Spring Boot 3.5**
- **PostgreSQL 16** - Banco de dados principal
- **Redis 7.x** - Cache e sessões
- **JWT (HS512)** - Autenticação
- **Liquibase** - Migrations
- **MapStruct** - Mapeamento DTO
- **springdoc-openapi** - Documentação API

### Frontend
- **React 19** + **TypeScript 5.9**
- **Vite 6.0** - Build tool
- **React Query 5** - Estado do servidor
- **Redux Toolkit 2.9** - Estado da UI
- **React Hook Form + Zod** - Formulários e validação
- **Tailwind CSS 4** - Estilização
- **Axios** - Cliente HTTP

---

## 1. AUTENTICAÇÃO E AUTORIZAÇÃO

### 1.1 Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/login` | Login com email/senha |
| POST | `/api/auth/register` | Registro de novo usuário |
| POST | `/api/auth/refresh` | Renovação de tokens |
| POST | `/api/auth/logout` | Logout (revoga refresh token) |
| GET | `/api/auth/me` | Perfil do usuário autenticado |
| PUT | `/api/auth/profile` | Atualizar nome e email |
| PUT | `/api/auth/password` | Alterar senha |
| POST | `/api/auth/forgot-password` | Solicitar reset de senha |
| POST | `/api/auth/reset-password` | Resetar senha com token |

### 1.2 Funcionalidades

- **Tokens JWT**
  - Access Token: 15 minutos, armazenado em memória
  - Refresh Token: 7 dias, HttpOnly cookie
  - Algoritmo HS512 com secret de 256 bits
  - Rotação automática de tokens

- **Segurança**
  - Senhas com BCrypt (12 rounds)
  - Proteção contra XSS (tokens não ficam em localStorage)
  - CSRF via SameSite=Strict
  - Rate limiting (preparado)

- **Recuperação de Senha**
  - Link por email com token único
  - Expiração configurável
  - Proteção contra enumeração de emails

### 1.3 Perfis de Usuário (RBAC)

| Perfil | Descrição | Permissões |
|--------|-----------|------------|
| SUPER_ADMIN | Dono do SaaS | Acesso total + painel administrativo |
| ADMIN | Administrador da oficina | CRUD completo de todos os módulos |
| GERENTE | Gerente | Todos módulos exceto gestão de usuários avançada |
| ATENDENTE | Atendente | Clientes, veículos, OS, pagamentos |
| MECANICO | Mecânico | Visualizar e atualizar OS atribuídas |

### 1.4 Páginas Frontend

- `/login` - Login
- `/register` - Registro
- `/forgot-password` - Esqueci minha senha
- `/reset-password` - Redefinir senha

**Status:** COMPLETO

---

## 2. GESTÃO DE USUÁRIOS

### 2.1 Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/usuarios` | Criar usuário |
| GET | `/api/usuarios` | Listar usuários (paginado) |
| GET | `/api/usuarios/{id}` | Buscar por ID |
| GET | `/api/usuarios/email/{email}` | Buscar por email |
| GET | `/api/usuarios/perfil/{perfil}` | Listar por perfil |
| GET | `/api/usuarios/ativos` | Listar apenas ativos |
| PUT | `/api/usuarios/{id}` | Atualizar usuário |
| DELETE | `/api/usuarios/{id}` | Desativar (soft delete) |
| PATCH | `/api/usuarios/{id}/reativar` | Reativar usuário |

### 2.2 Funcionalidades

- CRUD completo de usuários
- Atribuição de perfis
- Soft delete (desativação)
- Reativação de usuários
- Validação de email único
- Proteção contra desativar último admin
- Último acesso registrado

### 2.3 Páginas Frontend

- `/usuarios` - Lista de usuários
- `/usuarios/novo` - Criar usuário
- `/usuarios/:id` - Detalhes do usuário
- `/usuarios/:id/editar` - Editar usuário

**Status:** COMPLETO

---

## 3. GESTÃO DE CLIENTES

### 3.1 Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/clientes` | Criar cliente |
| GET | `/api/clientes` | Listar com filtros e paginação |
| GET | `/api/clientes/{id}` | Buscar por ID |
| GET | `/api/clientes/cpf-cnpj/{cpfCnpj}` | Buscar por CPF/CNPJ |
| PUT | `/api/clientes/{id}` | Atualizar cliente |
| DELETE | `/api/clientes/{id}` | Desativar (soft delete) |
| PATCH | `/api/clientes/{id}/reativar` | Reativar cliente |
| GET | `/api/clientes/filtros/estados` | Listar estados disponíveis |
| GET | `/api/clientes/filtros/cidades` | Listar cidades disponíveis |
| GET | `/api/clientes/estatisticas` | Estatísticas por tipo |

### 3.2 Funcionalidades

- **Tipos de Cliente**
  - PESSOA_FISICA (CPF)
  - PESSOA_JURIDICA (CNPJ)

- **Dados Cadastrais**
  - Nome/Razão Social
  - CPF/CNPJ (único, validado)
  - Email e telefones
  - Endereço completo (logradouro, número, complemento, bairro, cidade, estado, CEP)

- **Filtros de Busca**
  - Por nome
  - Por tipo (PF/PJ)
  - Por status (ativo/inativo)
  - Por cidade/estado

### 3.3 Páginas Frontend

- `/clientes` - Lista de clientes
- `/clientes/novo` - Criar cliente
- `/clientes/:id` - Detalhes do cliente
- `/clientes/:id/editar` - Editar cliente

**Status:** COMPLETO

---

## 4. GESTÃO DE VEÍCULOS

### 4.1 Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/veiculos` | Criar veículo |
| GET | `/api/veiculos` | Listar com filtros |
| GET | `/api/veiculos/{id}` | Buscar por ID |
| GET | `/api/veiculos/placa/{placa}` | Buscar por placa |
| GET | `/api/veiculos/cliente/{clienteId}` | Listar por cliente |
| PUT | `/api/veiculos/{id}` | Atualizar veículo |
| PATCH | `/api/veiculos/{id}/quilometragem` | Atualizar KM |
| DELETE | `/api/veiculos/{id}` | Excluir veículo |
| GET | `/api/veiculos/filtros/marcas` | Listar marcas |
| GET | `/api/veiculos/filtros/modelos` | Listar modelos |
| GET | `/api/veiculos/filtros/anos` | Listar anos |
| GET | `/api/veiculos/cliente/{clienteId}/estatisticas` | Estatísticas por cliente |

### 4.2 Funcionalidades

- **Dados do Veículo**
  - Placa (única, formatos ABC1234 e ABC-1234)
  - Marca, modelo, ano, cor
  - Chassi (VIN)
  - Quilometragem (só aumenta)

- **Relacionamentos**
  - Vinculado a um cliente (imutável)
  - Histórico de ordens de serviço

- **Filtros**
  - Por cliente
  - Por placa, marca, modelo, ano

### 4.3 Páginas Frontend

- `/veiculos` - Lista de veículos
- `/veiculos/novo` - Criar veículo
- `/veiculos/:id` - Detalhes do veículo
- `/veiculos/:id/editar` - Editar veículo

**Status:** COMPLETO

---

## 5. ORDENS DE SERVIÇO

### 5.1 Endpoints

#### CRUD
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/ordens-servico` | Criar OS |
| GET | `/api/ordens-servico` | Listar com filtros |
| GET | `/api/ordens-servico/{id}` | Buscar por ID |
| GET | `/api/ordens-servico/numero/{numero}` | Buscar por número |
| PUT | `/api/ordens-servico/{id}` | Atualizar OS |
| GET | `/api/ordens-servico/veiculo/{veiculoId}/historico` | Histórico do veículo |

#### Transições de Status
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| PATCH | `/api/ordens-servico/{id}/aprovar` | ORCAMENTO → APROVADO |
| PATCH | `/api/ordens-servico/{id}/iniciar` | APROVADO → EM_ANDAMENTO |
| PATCH | `/api/ordens-servico/{id}/finalizar` | → FINALIZADO (deduz estoque) |
| PATCH | `/api/ordens-servico/{id}/entregar` | FINALIZADO → ENTREGUE |
| PATCH | `/api/ordens-servico/{id}/cancelar` | → CANCELADO |

#### Dashboard
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/ordens-servico/dashboard/contagem-por-status` | KPI por status |
| GET | `/api/ordens-servico/dashboard/faturamento` | Faturamento do período |
| GET | `/api/ordens-servico/dashboard/ticket-medio` | Ticket médio |
| POST | `/api/ordens-servico/{id}/gerar-pdf` | Gerar PDF profissional |

### 5.2 Fluxo de Status

```
ORCAMENTO → APROVADO → EM_ANDAMENTO → FINALIZADO → ENTREGUE
                ↓              ↓
          AGUARDANDO_PECA ←────┘
                ↓
           CANCELADO (de qualquer status exceto ENTREGUE)
```

### 5.3 Funcionalidades

- **Dados da OS**
  - Número sequencial único
  - Veículo e mecânico atribuído
  - Problemas relatados, diagnóstico, observações
  - Datas: abertura, previsão, finalização, entrega

- **Itens da OS**
  - Tipo: PECA ou SERVICO
  - Quantidade, valor unitário, desconto
  - Vinculação com estoque (para peças)

- **Financeiro**
  - Valor mão de obra
  - Valor peças
  - Desconto (% ou valor fixo)
  - Valor total e final

- **Aprovação por Cliente**
  - Token único com expiração
  - Página pública de aprovação
  - Notificação por email/WhatsApp

- **PDF**
  - Geração de orçamento/OS em PDF profissional
  - Logo da oficina
  - Dados completos do serviço

### 5.4 Páginas Frontend

- `/ordens-servico` - Lista de OS
- `/ordens-servico/novo` - Criar OS
- `/ordens-servico/:id` - Detalhes da OS
- `/ordens-servico/:id/editar` - Editar OS
- `/orcamento/aprovar?token=...` - Aprovação pública

**Status:** COMPLETO

---

## 6. GESTÃO DE ESTOQUE

### 6.1 Peças/Produtos

#### Endpoints
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/estoque` | Criar peça |
| GET | `/api/estoque` | Listar com filtros |
| GET | `/api/estoque/{id}` | Buscar por ID |
| GET | `/api/estoque/codigo/{codigo}` | Buscar por SKU |
| PUT | `/api/estoque/{id}` | Atualizar peça |
| DELETE | `/api/estoque/{id}` | Desativar peça |
| PATCH | `/api/estoque/{id}/reativar` | Reativar peça |
| GET | `/api/estoque/alertas/baixo` | Estoque baixo |
| GET | `/api/estoque/alertas/zerado` | Estoque zerado |
| GET | `/api/estoque/relatorios/valor-total` | Valor total do estoque |
| GET | `/api/estoque/dashboard/estoque-baixo` | Contador de alertas |
| GET | `/api/estoque/sem-localizacao` | Peças sem local |
| POST | `/api/estoque/{pecaId}/definir-localizacao` | Definir local |

#### Dados da Peça
- Código SKU (único)
- Descrição, marca, aplicação
- Quantidade atual e mínima
- Valor de custo e venda
- Margem de lucro
- Unidade de medida
- Local de armazenamento

### 6.2 Locais de Armazenamento

#### Endpoints
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/locais-armazenamento` | Criar local |
| GET | `/api/locais-armazenamento` | Listar todos |
| GET | `/api/locais-armazenamento/{id}` | Buscar por ID |
| GET | `/api/locais-armazenamento/codigo/{codigo}` | Buscar por código |
| GET | `/api/locais-armazenamento/raiz` | Locais raiz |
| GET | `/api/locais-armazenamento/filhos/{paiId}` | Filhos de um local |
| GET | `/api/locais-armazenamento/tipo/{tipo}` | Por tipo |
| PUT | `/api/locais-armazenamento/{id}` | Atualizar local |
| DELETE | `/api/locais-armazenamento/{id}` | Excluir local |

#### Tipos de Local
- DEPOSITO
- ESTANTE
- PRATELEIRA
- VITRINE

#### Hierarquia
```
DEPOSITO
  └── ESTANTE
       └── PRATELEIRA
VITRINE
```

### 6.3 Movimentações

- **Tipos:** ENTRADA, SAIDA, AJUSTE, DEVOLUCAO
- Registro automático ao finalizar OS
- Histórico completo por peça
- Usuário responsável registrado

### 6.4 Páginas Frontend

- `/estoque` - Lista de peças
- `/estoque/novo` - Criar peça
- `/estoque/:id` - Detalhes da peça
- `/estoque/:id/editar` - Editar peça
- `/estoque/alertas` - Alertas de estoque baixo
- `/estoque/sem-localizacao` - Peças sem local
- `/estoque/locais` - Locais de armazenamento
- `/estoque/locais/novo` - Criar local
- `/estoque/locais/:id` - Detalhes do local

**Status:** COMPLETO

---

## 7. GESTÃO FINANCEIRA

### 7.1 Pagamentos

#### Endpoints
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/pagamentos` | Registrar pagamento |
| GET | `/api/pagamentos` | Listar com filtros |
| GET | `/api/pagamentos/{id}` | Buscar por ID |
| GET | `/api/pagamentos/ordem-servico/{osId}` | Pagamentos da OS |
| PUT | `/api/pagamentos/{id}/confirmar` | Confirmar pagamento |
| DELETE | `/api/pagamentos/{id}/cancelar` | Cancelar pagamento |
| PUT | `/api/pagamentos/{id}/estornar` | Estornar (ADMIN) |
| GET | `/api/pagamentos/ordem-servico/{osId}/resumo` | Resumo financeiro |

#### Tipos de Pagamento
- DINHEIRO
- CARTAO_CREDITO
- CARTAO_DEBITO
- PIX
- TRANSFERENCIA
- BOLETO

#### Status
- PENDENTE
- PAGO
- CANCELADO
- ESTORNADO

### 7.2 Notas Fiscais

#### Endpoints
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/notas-fiscais` | Criar NF |
| GET | `/api/notas-fiscais` | Listar NFs |
| GET | `/api/notas-fiscais/{id}` | Buscar por ID |
| GET | `/api/notas-fiscais/ordem-servico/{osId}` | NFs da OS |
| PUT | `/api/notas-fiscais/{id}` | Atualizar NF |
| DELETE | `/api/notas-fiscais/{id}` | Excluir NF |
| GET | `/api/notas-fiscais/proximo-numero/serie/{serie}` | Próximo número |

#### Status
- DIGITACAO (em edição)
- AUTORIZADO
- CANCELADO

> **Nota:** Integração com SEFAZ planejada para Fase 3

### 7.3 Páginas Frontend

- `/financeiro` - Lista de pagamentos
- `/financeiro/notas-fiscais` - Lista de NFs
- `/financeiro/notas-fiscais/novo` - Criar NF
- `/financeiro/notas-fiscais/:id` - Detalhes da NF

**Status:** COMPLETO (MVP - sem integração SEFAZ)

---

## 8. DASHBOARD

### 8.1 Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/dashboard/stats` | Estatísticas gerais |
| GET | `/api/dashboard/os-recentes` | OS recentes |
| GET | `/api/dashboard/os-por-status` | OS por status |
| GET | `/api/dashboard/faturamento-mensal` | Faturamento mensal |

### 8.2 Métricas Disponíveis

- Total de clientes
- Total de veículos
- Ordens de serviço ativas
- Faturamento do mês atual
- Distribuição de OS por status
- Tendência de faturamento (últimos 6 meses)
- Últimas 10 ordens de serviço

### 8.3 Página Frontend

- `/` - Dashboard principal

**Status:** COMPLETO

---

## 9. SISTEMA DE NOTIFICAÇÕES

### 9.1 Endpoints de Configuração

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/notificacoes/configuracao` | Obter configuração |
| PUT | `/api/notificacoes/configuracao` | Atualizar configuração |
| PATCH | `/api/notificacoes/configuracao/canais/{canal}` | Habilitar/desabilitar canal |
| PUT | `/api/notificacoes/configuracao/eventos/{evento}` | Configurar evento |
| PUT | `/api/notificacoes/configuracao/smtp` | Configurar SMTP |
| PUT | `/api/notificacoes/configuracao/whatsapp` | Configurar WhatsApp |
| GET | `/api/notificacoes/configuracao/whatsapp/qrcode` | QR Code WhatsApp |
| PUT | `/api/notificacoes/configuracao/telegram` | Configurar Telegram |
| POST | `/api/notificacoes/configuracao/testar` | Enviar teste |

### 9.2 Canais Suportados

| Canal | Integração | Status |
|-------|------------|--------|
| EMAIL | SMTP customizado | Completo |
| WHATSAPP | Evolution API | Completo |
| TELEGRAM | Telegram Bots API | Completo |
| SMS | - | Planejado |

### 9.3 Eventos Configuráveis

| Evento | Descrição |
|--------|-----------|
| OS_CRIADA | Ordem de serviço criada |
| OS_APROVADA | Orçamento aprovado |
| OS_INICIADA | Trabalho iniciado |
| OS_FINALIZADA | Trabalho concluído |
| OS_ENTREGUE | Veículo entregue |
| OS_CANCELADA | OS cancelada |
| PAGAMENTO_RECEBIDO | Pagamento confirmado |

### 9.4 Histórico

- Registro de todas as notificações enviadas
- Status: ENVIADO, FALHA, PENDENTE
- Mensagens de erro para debug
- Filtros por canal, evento, período

### 9.5 Páginas Frontend

- `/notificacoes/configuracao` - Configuração de notificações
- `/notificacoes/historico` - Histórico de envios

**Status:** COMPLETO

---

## 10. PAINEL SUPER_ADMIN (SaaS)

### 10.1 Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/saas/dashboard/stats` | Estatísticas da plataforma |
| GET | `/api/saas/dashboard/mrr` | MRR por plano |
| GET | `/api/saas/dashboard/trials-expiring` | Trials expirando |
| GET | `/api/saas/oficinas` | Listar oficinas |
| GET | `/api/saas/oficinas/{id}` | Detalhes da oficina |
| POST | `/api/saas/oficinas/{id}/ativar` | Ativar oficina |
| POST | `/api/saas/oficinas/{id}/suspender` | Suspender oficina |
| POST | `/api/saas/oficinas/{id}/cancelar` | Cancelar oficina |
| GET | `/api/saas/pagamentos` | Pagamentos da plataforma |
| GET | `/api/saas/audit` | Logs de auditoria |
| POST | `/api/saas/jobs/*` | Executar jobs |

### 10.2 Funcionalidades

- **Dashboard SaaS**
  - Total de oficinas (ativas, trial, suspensas)
  - MRR (Monthly Recurring Revenue)
  - MRR por plano
  - Trials expirando

- **Gestão de Oficinas**
  - Listar todas as oficinas
  - Filtros por status e plano
  - Ativar/Suspender/Cancelar
  - Ver detalhes e estatísticas

- **Auditoria**
  - Logs de todas as ações
  - Filtros por ação, entidade, período
  - Export CSV

### 10.3 Páginas Frontend

- `/admin` - Dashboard SaaS
- `/admin/oficinas` - Lista de oficinas
- `/admin/oficinas/:id` - Detalhes da oficina
- `/admin/pagamentos` - Pagamentos
- `/admin/audit` - Logs de auditoria

**Status:** COMPLETO (básico - expansão planejada)

---

## 11. MULTI-TENANCY

### 11.1 Arquitetura

- Cada oficina é um tenant isolado
- Dados segregados por `oficina_id`
- SUPER_ADMIN acessa todos os tenants
- Usuários normais só acessam sua oficina

### 11.2 Planos de Assinatura

| Plano | Descrição |
|-------|-----------|
| TRIAL | 14 dias gratuitos |
| STARTER | Plano básico |
| PROFESSIONAL | Plano intermediário |
| ENTERPRISE | Plano completo |

### 11.3 Status da Oficina

- TRIAL
- ATIVA
- SUSPENSA
- CANCELADA

**Status:** COMPLETO

---

## 12. APROVAÇÃO PÚBLICA DE ORÇAMENTO

### 12.1 Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/public/orcamento/{token}` | Ver orçamento |
| POST | `/api/public/orcamento/{token}/aprovar` | Aprovar |
| POST | `/api/public/orcamento/{token}/rejeitar` | Rejeitar |

### 12.2 Funcionalidades

- Acesso sem autenticação (via token)
- Expiração configurável do token
- Visualização completa do orçamento
- Aprovação com um clique
- Rejeição com motivo opcional
- Notificação em tempo real à oficina

### 12.3 Página Frontend

- `/orcamento/aprovar?token=...` - Página pública

**Status:** COMPLETO

---

## 13. INFRAESTRUTURA TÉCNICA

### 13.1 Banco de Dados

**Tabelas Principais:**
- `usuarios` - Usuários do sistema
- `clientes` - Clientes da oficina
- `veiculos` - Veículos dos clientes
- `ordens_servico` - Ordens de serviço
- `itens_os` - Itens das OS
- `pecas` - Estoque de peças
- `movimentacao_estoque` - Movimentações
- `locais_armazenamento` - Locais físicos
- `pagamentos` - Pagamentos
- `notas_fiscais` - Notas fiscais
- `oficinas` - Oficinas (tenants)
- `configuracao_notificacao` - Config de notificações
- `historico_notificacoes` - Histórico de envios
- `audit_logs` - Logs de auditoria

**Migrations:** 41 scripts SQL aplicados

### 13.2 Segurança

- JWT com rotação de tokens
- BCrypt para senhas
- RBAC com @PreAuthorize
- CORS configurado
- HttpOnly cookies
- Proteção XSS/CSRF
- Validação de entrada (Bean Validation + Zod)

### 13.3 API Documentation

- Swagger UI em `/swagger-ui.html`
- OpenAPI 3.0
- 60+ endpoints documentados

### 13.4 Logging e Auditoria

- SLF4J + Logback
- Logs estruturados
- Auditoria de todas as ações
- Rastreamento de usuário

### 13.5 Tratamento de Erros

Formato padrão de resposta:
```json
{
  "error": "CODIGO_ERRO",
  "message": "Mensagem amigável",
  "status": 400,
  "timestamp": "2024-12-24T10:30:00Z",
  "path": "/api/endpoint"
}
```

---

## 14. FUNCIONALIDADES PLANEJADAS

### Fase 2 (Próxima)
- [ ] Dashboard SUPER_ADMIN completo (ARR, Churn, LTV, CAC)
- [ ] Gráficos com ECharts
- [ ] Wizard de criação de oficina
- [ ] CRUD de planos
- [ ] Sistema de faturas

### Fase 3
- [ ] Integração Mercado Pago
- [ ] Integração SEFAZ (NF-e)
- [ ] Relatórios avançados (JasperReports)
- [ ] Export Excel (Apache POI)

### Fase 4
- [ ] Sistema de tickets/suporte
- [ ] Feature flags
- [ ] Comunicação em massa
- [ ] Monitoramento (Prometheus/Grafana)

### Futuro
- [ ] App mobile
- [ ] Agendamento online
- [ ] Portal do cliente
- [ ] Integração contábil

---

## 15. COMANDOS DE DESENVOLVIMENTO

### Backend
```bash
# Iniciar aplicação
./mvnw spring-boot:run

# Build
./mvnw clean package

# Testes
./mvnw test

# Docker (PostgreSQL + Redis)
docker-compose up -d
```

### Frontend
```bash
# Desenvolvimento
npm run dev

# Build produção
npm run build

# Preview
npm run preview

# Lint
npm run lint
```

---

## 16. ESTRUTURA DE PASTAS

### Backend
```
src/main/java/com/pitstop/
├── cliente/          # Módulo de clientes
├── veiculo/          # Módulo de veículos
├── ordemservico/     # Módulo de OS
├── estoque/          # Módulo de estoque
├── financeiro/       # Módulo financeiro
├── usuario/          # Módulo de usuários
├── notificacao/      # Módulo de notificações
├── oficina/          # Módulo de oficinas
├── dashboard/        # Módulo de dashboard
├── saas/             # Módulo SaaS (admin)
├── shared/           # Código compartilhado
│   ├── security/     # JWT, autenticação
│   ├── exception/    # Tratamento de erros
│   └── audit/        # Auditoria
└── config/           # Configurações Spring
```

### Frontend
```
src/
├── features/
│   ├── auth/         # Autenticação
│   ├── clientes/     # Clientes
│   ├── veiculos/     # Veículos
│   ├── ordens/       # Ordens de serviço
│   ├── estoque/      # Estoque
│   ├── financeiro/   # Financeiro
│   ├── usuarios/     # Usuários
│   ├── notificacoes/ # Notificações
│   ├── dashboard/    # Dashboard
│   ├── admin/        # Painel SaaS
│   └── configuracoes/# Configurações
├── shared/
│   ├── components/   # Componentes reutilizáveis
│   ├── hooks/        # Hooks compartilhados
│   ├── services/     # Serviços (API)
│   ├── store/        # Redux store
│   ├── layouts/      # Layouts
│   └── utils/        # Utilitários
└── App.tsx           # Rotas principais
```

---

**Documento gerado automaticamente em 2024-12-24**
**PitStop v1.0.0 - MVP Completo**
