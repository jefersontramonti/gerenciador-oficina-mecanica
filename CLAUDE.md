# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## REGRAS OBRIGATÓRIAS

**IMPORTANTE: Siga estas regras SEMPRE, sem exceção:**

### 1. Commits e Git
- **NUNCA** fazer commit sem o usuário pedir explicitamente
- **NUNCA** fazer push sem o usuário pedir explicitamente
- Sempre esperar o usuário testar as mudanças antes de commitar
- Perguntar ao usuário antes de qualquer operação git

### 2. VPS e Deploy
- **NUNCA** acessar a VPS sem permissão explícita do usuário
- **NUNCA** iniciar deploy sem o usuário pedir
- **NUNCA** usar o agente pitstop-devops sem autorização

### 3. Modificação de Código
- **NUNCA** modificar código sem o usuário pedir ou aprovar
- Sempre explicar as mudanças propostas ANTES de implementar
- Aguardar confirmação do usuário antes de editar arquivos
- Se o usuário pedir uma análise, apenas analisar - não modificar

### 4. Fluxo de Trabalho
1. Usuário pede uma mudança
2. Claude propõe a solução e explica
3. Usuário aprova
4. Claude implementa
5. Usuário testa
6. Usuário pede commit (se quiser)
7. Claude faz commit
8. Usuário pede deploy (se quiser)

---

## Development Commands

### Backend (Maven)
```bash
# Run application
./mvnw spring-boot:run          # Linux/Mac
mvnw.cmd spring-boot:run        # Windows

# Build & Package
./mvnw clean package            # Build JAR
./mvnw clean package -DskipTests # Skip tests
./mvnw clean install            # Full install

# Testing
./mvnw test                     # Run all tests
./mvnw test -Dtest=ClassNameTest # Single test class
```

### Frontend (npm)
```bash
cd frontend
npm install                     # Install dependencies
npm run dev                     # Start dev server (port 5173)
npm run build                   # Production build
npm run preview                 # Preview production build
npm run lint                    # Run ESLint
```

### Docker Compose
```bash
docker-compose up -d            # Start PostgreSQL + Redis
docker-compose down             # Stop services
docker-compose logs -f          # View logs
```

### Quick Start
1. Start Docker Desktop
2. Run `docker-compose up -d`
3. Run `./mvnw spring-boot:run` (backend on port 8080)
4. Run `cd frontend && npm run dev` (frontend on port 5173)
5. Access: http://localhost:5173

## Project Overview

**PitStop** is a complete web-based management system for automotive repair shops (oficinas mecânicas) with multi-tenancy SaaS architecture. The system handles service orders, inventory management, customer/vehicle records, financial tracking, real-time notifications, and SaaS administration.

**Architecture**: Modular Monolith (Spring Boot) + SPA (React/TypeScript)
**Status**: Production-ready MVP with 10+ functional modules

## Technology Stack

### Backend (Implemented)
| Component | Technology |
|-----------|------------|
| Language | Java 25 LTS |
| Framework | Spring Boot 3.5.7-SNAPSHOT |
| Database | PostgreSQL 16 |
| Cache | Redis 7.x |
| Security | Spring Security + JWT (HS512) |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Liquibase (41 migrations) |
| API Docs | SpringDoc OpenAPI 2.7.0 |
| PDF | OpenPDF 1.3.30 |
| Mapping | MapStruct 1.6.0 |
| Metrics | Micrometer + Prometheus |
| WebSocket | Spring WebSocket + STOMP |

### Frontend (Implemented)
| Component | Technology |
|-----------|------------|
| Library | React 19.0.0 + TypeScript 5.9 |
| Build Tool | Vite 7.0+ |
| State (UI) | Redux Toolkit 2.9.0 |
| State (Server) | React Query 5.62.0 |
| HTTP | Axios 1.7.9 |
| WebSocket | STOMP.js + SockJS |
| UI | Tailwind CSS 4.0 + Custom Components |
| Forms | React Hook Form + Zod 3.24.1 |
| Charts | ECharts 5.5.1 |
| Routing | React Router 7.0.0 |
| Icons | Lucide React |

### External Integrations (Implemented)
- **WhatsApp**: Evolution API (self-hosted)
- **Telegram**: Telegram Bots API
- **Email**: Spring Mail + Thymeleaf templates
- **Phone Validation**: libphonenumber

## Package Structure

### Backend (`src/main/java/com/pitstop/`)
```
com.pitstop/
├── config/                    # Spring configuration (10 files)
│   ├── SecurityConfig.java   # JWT + CORS + method security
│   ├── CacheConfig.java      # Redis caching
│   ├── WebSocketConfig.java  # STOMP WebSocket
│   ├── OpenAPIConfig.java    # Swagger/OpenAPI
│   └── ...
├── shared/                    # Infrastructure (40+ files)
│   ├── security/             # JWT, TenantContext, filters
│   ├── audit/                # Audit logging
│   ├── exception/            # Global error handling
│   ├── controller/           # AuthController
│   └── service/              # RefreshToken, PasswordReset
├── cliente/                  # Customer module
├── veiculo/                  # Vehicle module
├── ordemservico/             # Service Order module (CORE)
├── estoque/                  # Inventory module
├── financeiro/               # Financial module
├── notificacao/              # Notification module
├── oficina/                  # Workshop configuration
├── usuario/                  # User management
├── dashboard/                # Dashboard statistics
└── saas/                     # SaaS administration (SUPER_ADMIN)
```

### Frontend (`frontend/src/`)
```
src/
├── features/                  # Feature modules
│   ├── admin/                # SaaS Admin (SUPER_ADMIN)
│   ├── auth/                 # Authentication
│   ├── clientes/             # Customers
│   ├── veiculos/             # Vehicles
│   ├── ordens-servico/       # Service Orders
│   ├── estoque/              # Inventory
│   ├── financeiro/           # Payments & Invoices
│   ├── notificacoes/         # Notifications config
│   ├── usuarios/             # User management
│   ├── dashboard/            # Dashboard
│   └── configuracoes/        # Settings
├── shared/
│   ├── components/           # UI components
│   ├── services/             # API services
│   ├── store/                # Redux store
│   ├── hooks/                # Custom hooks
│   └── layouts/              # MainLayout
└── config/                   # Environment, QueryClient
```

## Domain Model

### Core Entities

**Usuario** (User)
- Roles: `SUPER_ADMIN` | `ADMIN` | `GERENTE` | `ATENDENTE` | `MECANICO`
- SUPER_ADMIN has no `oficinaId` (manages all workshops)
- BCrypt password hashing (strength 12)

**Oficina** (Workshop/Tenant)
- Multi-tenant isolation via `oficina_id` on all entities
- Status: `ATIVA` | `INATIVA` | `SUSPENSA` | `CANCELADA`
- Plans: `ECONOMICO` | `PROFISSIONAL` | `TURBINADO`

**Cliente** (Customer)
- Types: `PESSOA_FISICA` | `PESSOA_JURIDICA`
- Embedded `Endereco` (address)
- Soft delete via `ativo` field

**Veiculo** (Vehicle)
- Unique `placa` (license plate) per tenant
- Linked to Cliente (Many-to-One)

**OrdemServico** (Service Order) - CORE ENTITY
- Status workflow: `ORCAMENTO` → `APROVADO` → `EM_ANDAMENTO` → `AGUARDANDO_PECA` → `FINALIZADO` → `ENTREGUE` | `CANCELADO`
- Sequential numbering via PostgreSQL sequence
- Financial: `valorMaoObra`, `valorPecas`, `valorTotal`, `descontoPercentual`, `valorFinal`
- Items: `PECA` | `SERVICO`
- Automatic stock deduction on delivery

**Peca** (Inventory Part)
- SKU code (unique per tenant)
- Quantity tracking with minimum alerts
- Hierarchical storage locations (`LocalArmazenamento`)
- Movement types: `ENTRADA` | `SAIDA` | `AJUSTE` | `DEVOLUCAO`

**Pagamento** (Payment)
- Types: `DINHEIRO` | `CARTAO_CREDITO` | `CARTAO_DEBITO` | `PIX` | `TRANSFERENCIA` | `BOLETO`
- Status: `PENDENTE` | `PAGO` | `CANCELADO` | `ESTORNADO`

**NotaFiscal** (Invoice)
- Types: `SERVICO` | `PRODUTO` | `MISTA`
- Status: `RASCUNHO` | `EMITIDA` | `CANCELADA`

## Security & Authentication

### JWT Configuration
- **Access Token**: 15 min validity, stored in memory (frontend)
- **Refresh Token**: 7 days validity, HttpOnly cookie + Redis
- **Algorithm**: HS512 with 256-bit secret
- Claims: `sub` (userId), `email`, `perfil`, `oficinaId`

### Role Permissions (RBAC)
| Role | Permissions |
|------|-------------|
| `SUPER_ADMIN` | Full SaaS access, manages all workshops via `/api/saas/*` |
| `ADMIN` | Full workshop access including user management |
| `GERENTE` | All modules except user management, financial reports |
| `ATENDENTE` | CRUD customers/vehicles/orders, inventory view, payments |
| `MECANICO` | View assigned orders, update status, read-only inventory |

### Multi-Tenancy
- `TenantContext` (ThreadLocal) extracts `oficinaId` from JWT
- All repositories automatically filter by tenant
- SUPER_ADMIN bypasses tenant filtering

## API Endpoints

### Authentication (`/api/auth/*`)
```
POST /api/auth/login           # Login, returns tokens
POST /api/auth/register        # Register new user
POST /api/auth/refresh         # Rotate access token
POST /api/auth/logout          # Invalidate refresh token
GET  /api/auth/me              # Current user profile
PUT  /api/auth/profile         # Update profile
PUT  /api/auth/password        # Change password
POST /api/auth/forgot-password # Initiate password reset
POST /api/auth/reset-password  # Complete password reset
```

### Customers (`/api/clientes/*`)
```
GET  /api/clientes             # List (paginated, filterable)
POST /api/clientes             # Create
GET  /api/clientes/{id}        # Get by ID
PUT  /api/clientes/{id}        # Update
DELETE /api/clientes/{id}      # Soft delete
PATCH /api/clientes/{id}/reativar # Reactivate
GET  /api/clientes/cpf-cnpj/{cpfCnpj} # Get by CPF/CNPJ
GET  /api/clientes/estatisticas # Statistics
```

### Vehicles (`/api/veiculos/*`)
```
GET  /api/veiculos             # List
POST /api/veiculos             # Create
GET  /api/veiculos/{id}        # Get by ID
PUT  /api/veiculos/{id}        # Update
DELETE /api/veiculos/{id}      # Delete
GET  /api/veiculos/placa/{placa} # Get by license plate
GET  /api/veiculos/cliente/{clienteId} # Get by customer
```

### Service Orders (`/api/ordens-servico/*`)
```
GET  /api/ordens-servico       # List (status, date, vehicle filters)
POST /api/ordens-servico       # Create
GET  /api/ordens-servico/{id}  # Get by ID
PUT  /api/ordens-servico/{id}  # Update (ORCAMENTO/APROVADO only)
PATCH /api/ordens-servico/{id}/status # Change status
DELETE /api/ordens-servico/{id} # Cancel
GET  /api/ordens-servico/{id}/pdf # Generate PDF
```

### Inventory (`/api/estoque/*`)
```
# Parts
GET  /api/estoque/pecas        # List parts
POST /api/estoque/pecas        # Create part
GET  /api/estoque/pecas/{id}   # Get by ID
PUT  /api/estoque/pecas/{id}   # Update
DELETE /api/estoque/pecas/{id} # Soft delete

# Storage Locations
GET  /api/estoque/locais       # List locations (hierarchical)
POST /api/estoque/locais       # Create location
PUT  /api/estoque/locais/{id}  # Update
DELETE /api/estoque/locais/{id} # Delete

# Movements
POST /api/estoque/movimentacoes/entrada # Stock in
POST /api/estoque/movimentacoes/saida   # Stock out
POST /api/estoque/movimentacoes/ajuste  # Adjust
GET  /api/estoque/movimentacoes         # History
```

### Financial (`/api/financeiro/*`)
```
# Payments
GET  /api/financeiro/pagamentos            # List
POST /api/financeiro/pagamentos            # Create
PATCH /api/financeiro/pagamentos/{id}/confirmar # Confirm
PATCH /api/financeiro/pagamentos/{id}/cancelar  # Cancel

# Invoices
GET  /api/financeiro/notas-fiscais         # List
POST /api/financeiro/notas-fiscais         # Create
PATCH /api/financeiro/notas-fiscais/{id}/emitir   # Emit
PATCH /api/financeiro/notas-fiscais/{id}/cancelar # Cancel
GET  /api/financeiro/notas-fiscais/{id}/pdf       # Download PDF
```

### Notifications (`/api/notificacoes/*`)
```
GET  /api/notificacoes/configuracao  # Get settings
PUT  /api/notificacoes/configuracao  # Update settings
GET  /api/notificacoes/templates     # List templates
GET  /api/notificacoes/historico     # Notification history
GET  /api/notificacoes/metricas      # Metrics
```

### SaaS Admin (`/api/saas/*`) - SUPER_ADMIN only
```
# Dashboard
GET  /api/saas/dashboard/stats       # Global statistics
GET  /api/saas/dashboard/mrr         # MRR breakdown

# Workshops
GET  /api/saas/oficinas              # List all workshops
POST /api/saas/oficinas              # Create workshop
GET  /api/saas/oficinas/{id}         # Get details
PUT  /api/saas/oficinas/{id}         # Update
PATCH /api/saas/oficinas/{id}/status # Change status

# Payments & Audit
GET  /api/saas/pagamentos            # Workshop payments
GET  /api/saas/audit                 # Audit logs
POST /api/saas/jobs/trigger          # Trigger scheduled jobs
```

### Dashboard (`/api/dashboard/*`)
```
GET  /api/dashboard/stats            # Dashboard statistics
```

## Frontend Routes

```
/                              # Dashboard (protected)
/login                         # Public
/register                      # Public
/forgot-password               # Public
/reset-password                # Public
/orcamento/aprovar             # Public (customer approval)

/clientes                      # Customer list
/clientes/novo                 # Create customer
/clientes/:id                  # Customer details
/clientes/:id/editar           # Edit customer

/veiculos                      # Vehicle list
/veiculos/novo                 # Create vehicle
/veiculos/:id                  # Vehicle details
/veiculos/:id/editar           # Edit vehicle

/ordens-servico                # Service order list
/ordens-servico/novo           # Create order
/ordens-servico/:id            # Order details
/ordens-servico/:id/editar     # Edit order

/estoque                       # Inventory list
/estoque/novo                  # Create part
/estoque/alertas               # Low stock alerts
/estoque/sem-localizacao       # Parts without location
/estoque/:id                   # Part details
/estoque/locais                # Storage locations

/financeiro                    # Payments
/financeiro/notas-fiscais      # Invoices

/usuarios                      # User management (ADMIN/GERENTE)
/configuracoes                 # User settings
/notificacoes/configuracao     # Notification settings (ADMIN/GERENTE)
/notificacoes/historico        # Notification history

/admin                         # SaaS Dashboard (SUPER_ADMIN)
/admin/oficinas                # Workshop management
/admin/pagamentos              # SaaS payments
/admin/audit                   # Audit logs
```

## WebSocket (Real-time)

**Connection**: `ws://localhost:8080/ws`
**Protocol**: STOMP over SockJS

**Topics**:
- `/user/queue/notifications` - Personal notifications
- `/topic/os-updates` - Service order updates
- `/topic/estoque-alerts` - Low stock alerts
- `/topic/dashboard-updates` - Dashboard refresh

**Notification Types**:
- `OS_STATUS_CHANGED`, `OS_CREATED`, `OS_UPDATED`, `OS_APROVADA`
- `PAYMENT_RECEIVED`, `STOCK_ALERT`, `DASHBOARD_UPDATE`

## Database Migrations

Located in `src/main/resources/db/changelog/migrations/`

Key migrations:
- `V001-V010`: Core entities (usuarios, oficinas, clientes, veiculos)
- `V011-V020`: Service orders, items, status workflow
- `V021-V030`: Inventory (pecas, movimentacoes, locais_armazenamento)
- `V031-V040`: Financial (pagamentos, notas_fiscais)
- `V041`: Notifications (configuracoes, templates, historico)

## Caching Strategy

**Redis Configuration**:
- Default TTL: 1 hour
- Connection: Lettuce client
- Null values: not cached

**Cache Keys**:
- Workshop data: 24h TTL
- Frequent queries: 1h TTL
- User sessions: 30min TTL
- Reports: 15min TTL

**Invalidation**: `@CacheEvict` on CREATE/UPDATE/DELETE

## Environment Variables

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pitstop
SPRING_DATASOURCE_USERNAME=myuser
SPRING_DATASOURCE_PASSWORD=secret

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# JWT
JWT_SECRET=<256-bit-random-key>
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Email
SPRING_MAIL_HOST=smtp.mailtrap.io
SPRING_MAIL_PORT=2525
SPRING_MAIL_USERNAME=<user>
SPRING_MAIL_PASSWORD=<pass>

# WhatsApp (Evolution API)
EVOLUTION_API_URL=<url>
EVOLUTION_API_KEY=<key>

# Telegram
TELEGRAM_BOT_TOKEN=<token>
```

## Common Patterns

### Backend
- **Multi-tenancy**: All queries filtered by `TenantContext.getCurrentOficinaId()`
- **Soft deletes**: `ativo` boolean field instead of hard delete
- **Optimistic locking**: `@Version` on concurrent entities
- **Event-driven**: Service order changes trigger notification events
- **State machine**: OrdemServico has defined valid transitions
- **DTO mapping**: MapStruct for compile-time safe mapping

### Frontend
- **Feature-first structure**: Each module self-contained
- **React Query for server state**: Consistent query keys, cache invalidation
- **Redux for UI state**: Only auth state currently
- **Zod validation**: All forms have schemas
- **Token in memory**: XSS protection (not localStorage)
- **Mobile-first responsive**: List pages show cards on mobile (`lg:hidden`), tables on desktop (`hidden lg:block`)

## Observability

**Actuator Endpoints**:
- `/actuator/health` - Health check
- `/actuator/metrics` - Micrometer metrics
- `/actuator/prometheus` - Prometheus scraping
- `/actuator/info` - App info

**Swagger UI**: http://localhost:8080/swagger-ui.html

## Pending Tasks (Short-term)

**Landing Page (2026-01-15):**
1. **Lead Capture Endpoint** - Create `POST /api/public/leads` to receive form submissions from landing page contact form
2. **Mobile Menu** - Add hamburger menu to other landing pages (currently only `index.html` has it)
3. **Google Ads Conversion Tracking** - Configure conversion tracking with Google Ads ID: `147-235-9974`

**Completed Today (2026-01-15):**
- Google Analytics 4 (G-C1YK3X5KTX) added to all 16 landing page HTML files
- Fixed broken Unsplash images with `referrerpolicy="no-referrer"`
- Replaced corrupted/404 images with working alternatives
- Fixed Redis cache corruption issue (ClassCastException on `/api/oficinas/{id}`)

## What's Next (Not Yet Implemented)

1. **Mercado Pago Integration** - Payment gateway
2. **JasperReports** - Complex reports
3. **File Upload Service** - Logos, documents
4. **API Rate Limiting** - Request throttling
5. **Comprehensive Tests** - Unit/Integration coverage
6. **SaaS Advanced Features** - Billing, invoices, feature flags (see `docs/SUPER_ADMIN_IMPLEMENTATION_PLAN.md`)

## Important Files

| File | Purpose |
|------|---------|
| `CLAUDE.md` | This file - project guidance |
| `docs/SUPER_ADMIN_IMPLEMENTATION_PLAN.md` | SaaS panel roadmap |
| `docs/PITSTOP_FUNCIONALIDADES.md` | Feature documentation |
| `docs/FIX_MOBILE_AUTH_2026-01-12.md` | Mobile auth fix documentation |
| `compose.yaml` | Docker Compose (local development) |
| `docker-compose.prod.yml` | Docker Compose (production VPS) |
| `.env.production.template` | Environment variables template |
| `deploy/nginx-pitstopai.conf` | Nginx configuration for VPS |
| `pom.xml` | Maven dependencies |
| `frontend/package.json` | Frontend dependencies |
| `src/main/resources/application.properties` | Spring config |

## VPS Production Deployment

### Architecture Overview (Production)

```
Clients (Browser/Mobile)
         │
         ▼
┌─────────────────────────────────────────────┐
│          Nginx (Reverse Proxy)               │
│                                              │
│  pitstopai.com.br      → /opt/pitstop/landing│
│  app.pitstopai.com.br  → frontend:3000       │
│  app.pitstopai.com.br/api → backend:8080     │
│  app.pitstopai.com.br/ws  → backend:8080     │
│  api.pitstopai.com.br  → backend:8080        │
│  whatsapp.pitstopai.com.br → evolution:8021  │
└─────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│          Docker Containers                   │
│  - pitstop-frontend (3000)                   │
│  - pitstop-backend (8080)                    │
│  - pitstop-postgres (5432)                   │
│  - pitstop-redis (6379)                      │
│  - evolution-api (8021) [separate compose]   │
└─────────────────────────────────────────────┘
```

### Critical: Same-Origin Cookie Configuration

**IMPORTANT**: For mobile authentication to work, the API must be served from the **same domain** as the frontend. Mobile browsers block cross-site cookies.

```
Frontend: https://app.pitstopai.com.br
API:      https://app.pitstopai.com.br/api  (proxied to backend:8080)
WebSocket: https://app.pitstopai.com.br/ws  (proxied to backend:8080)
```

The `DOMAIN_API` environment variable **MUST** match the frontend domain:
```env
DOMAIN_API=app.pitstopai.com.br  # Same as frontend, NOT api.pitstopai.com.br
```

### VPS Directory Structure

```
/opt/pitstop/
├── docker-compose.yml      # Copy from docker-compose.prod.yml
├── .env                    # Copy from .env.production.template (with real values)
├── backend/
│   └── Dockerfile
├── frontend/
│   └── Dockerfile
├── data/
│   ├── postgres-pitstop/   # PostgreSQL data
│   └── redis/              # Redis data
└── landing/                # Static landing page files

/etc/nginx/sites-available/
└── pitstopai               # Copy from deploy/nginx-pitstopai.conf

/etc/letsencrypt/live/pitstopai.com.br/
├── fullchain.pem
└── privkey.pem
```

### Initial VPS Setup

```bash
# 1. Create directories
mkdir -p /opt/pitstop/data/postgres-pitstop
mkdir -p /opt/pitstop/data/redis
mkdir -p /opt/pitstop/landing

# 2. Copy project files to VPS
scp -r backend/ frontend/ docker-compose.prod.yml .env.production.template root@VPS:/opt/pitstop/

# 3. On VPS: Rename and configure
cd /opt/pitstop
mv docker-compose.prod.yml docker-compose.yml
cp .env.production.template .env
nano .env  # Fill in real values

# 4. Copy nginx config
cp deploy/nginx-pitstopai.conf /etc/nginx/sites-available/pitstopai
ln -sf /etc/nginx/sites-available/pitstopai /etc/nginx/sites-enabled/

# 5. Generate SSL certificates (first time only)
certbot certonly --webroot -w /var/www/certbot \
  -d pitstopai.com.br -d www.pitstopai.com.br \
  -d app.pitstopai.com.br -d api.pitstopai.com.br \
  -d whatsapp.pitstopai.com.br

# 6. Test and reload nginx
nginx -t && systemctl reload nginx

# 7. Start containers
cd /opt/pitstop
docker compose up -d
```

### Deployment Commands (Updates)

**Use o script de deploy** (`/opt/pitstop/deploy.sh`) para evitar problemas com containers órfãos:

```bash
# SSH to VPS
ssh root@YOUR_VPS_IP
cd /opt/pitstop

# Deploy completo (backend + frontend + landing)
./deploy.sh

# Ou deploy específico
./deploy.sh backend      # Apenas backend
./deploy.sh frontend     # Apenas frontend
./deploy.sh landing      # Apenas landing page
```

**Instalação do script (primeira vez):**
```bash
cd /opt/pitstop
git fetch origin main
git checkout origin/main -- deploy/deploy.sh
cp deploy/deploy.sh ./deploy.sh
chmod +x deploy.sh
```

**Deploy manual (se preferir):**
```bash
cd /opt/pitstop
git fetch origin main

# Atualizar código
git checkout origin/main -- src/main/java/com/pitstop/
git checkout origin/main -- frontend/src/

# IMPORTANTE: Sempre usar --force-recreate para evitar conflitos
docker compose build --no-cache
docker compose up -d --force-recreate --remove-orphans
```

**Comandos úteis:**
```bash
# View logs
docker compose logs -f pitstop-backend
docker compose logs -f pitstop-frontend
docker compose logs -f --tail=100  # All services

# Restart services
docker compose restart pitstop-backend
docker compose restart pitstop-frontend

# Check container status
docker compose ps

# Check health
curl http://localhost:8080/actuator/health
curl http://localhost:3000/health

# Limpar containers órfãos manualmente (se necessário)
docker rm -f pitstop-backend pitstop-frontend pitstop-postgres pitstop-redis 2>/dev/null
docker compose up -d --force-recreate --remove-orphans
```

### Nginx Commands

```bash
# Test configuration
nginx -t

# Reload (apply changes without downtime)
systemctl reload nginx

# Restart (full restart)
systemctl restart nginx

# View logs
tail -f /var/log/nginx/app_access.log
tail -f /var/log/nginx/app_error.log
tail -f /var/log/nginx/api_access.log
```

### Database Commands

```bash
# Access PostgreSQL
docker exec -it pitstop-postgres psql -U pitstop -d pitstop_db

# Backup database
docker exec pitstop-postgres pg_dump -U pitstop pitstop_db > backup_$(date +%Y%m%d).sql

# Restore database
docker exec -i pitstop-postgres psql -U pitstop pitstop_db < backup_20260112.sql

# Access Redis CLI
docker exec -it pitstop-redis redis-cli -a YOUR_REDIS_PASSWORD
```

### Troubleshooting

**Redis cache corruption (500 errors on pages):**
1. Error message: `Unexpected token (START_OBJECT), expected START_ARRAY`
2. Flush Redis cache: `docker exec pitstop-redis redis-cli -a $(grep REDIS_PASSWORD /opt/pitstop/.env | cut -d'=' -f2) FLUSHALL`
3. Restart backend: `docker compose restart pitstop-backend`

**Environment variables not applied after .env change:**
1. Simple restart does NOT apply new env vars
2. Must recreate container: `docker compose up -d --force-recreate pitstop-backend`
3. Verify vars applied: `docker exec pitstop-backend printenv | grep VARIABLE_NAME`

**Mobile login not persisting after reload:**
1. Verify `DOMAIN_API=app.pitstopai.com.br` in `.env` (same as frontend)
2. Check nginx has `/api/` and `/ws/` proxy in `app.pitstopai.com.br` server block
3. Rebuild frontend: `docker compose build pitstop-frontend --no-cache`
4. Verify URLs in build: `docker exec pitstop-frontend sh -c "grep -o 'https://app.pitstopai' /usr/share/nginx/html/assets/*.js | head -1"`

**WebSocket 403 Forbidden:**
1. Check `CORS_ALLOWED_ORIGINS` includes frontend URL
2. Verify `WebSocketConfig.java` uses `setAllowedOriginPatterns()` not `setAllowedOrigins()`
3. Rebuild backend: `docker compose build pitstop-backend --no-cache`

**Container network errors:**
1. Check network name matches in docker-compose.yml: `pitstop-app-network`
2. Run `docker network ls` to see existing networks
3. If conflict, use different network name

**SockJS scheme error (wss:// not allowed):**
1. Verify `VITE_WS_URL` uses `https://` not `wss://`
2. SockJS handles the WebSocket upgrade automatically

### Environment Variables Reference

See `.env.production.template` for complete list. Critical variables:

| Variable | Value | Notes |
|----------|-------|-------|
| `DOMAIN_API` | `app.pitstopai.com.br` | **MUST** match frontend domain for mobile cookies |
| `CORS_ALLOWED_ORIGINS` | `https://app.pitstopai.com.br,https://pitstopai.com.br` | Comma-separated |
| `APP_COOKIE_DOMAIN` | (empty) | Leave empty when using same-origin proxy |
| `SPRING_PROFILES_ACTIVE` | `prod` | Production profile |

### Security Notes

- Never commit `.env` files with real values
- Use strong passwords (generate with `openssl rand -base64 32`)
- JWT secret must be 64+ bytes (`openssl rand -base64 64`)
- PostgreSQL and Redis only listen on 127.0.0.1 (not exposed to internet)
- Actuator endpoints restricted to localhost in nginx config
