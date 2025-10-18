# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Maven Build & Run
- **Run application**: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- **Build JAR**: `./mvnw clean package`
- **Run tests**: `./mvnw test`
- **Run single test**: `./mvnw test -Dtest=ClassNameTest`
- **Skip tests**: `./mvnw clean package -DskipTests`
- **Clean build**: `./mvnw clean install`

### Docker Compose
- **Start services**: `docker-compose up -d` (PostgreSQL, Redis)
- **Stop services**: `docker-compose down`
- **View logs**: `docker-compose logs -f`
- **Rebuild containers**: `docker-compose up -d --build`

### Development Mode
- Spring Boot DevTools is included for hot reload
- Application automatically restarts on code changes
- Docker Compose support is enabled (services start automatically in dev mode)

## Project Overview

**PitStop** is a web-based management system for small to medium-sized automotive repair shops (oficinas mecânicas). The system handles service orders, inventory management, customer/vehicle records, financial tracking, and real-time notifications.

**Architecture**: Monolithic modular backend (Spring Boot) + SPA frontend (React)
**Current Status**: Initial project setup - ready for feature implementation

## Technology Stack

### Backend
- **Language**: Java 25 LTS (Oracle No-Fee License, support until 2033)
- **Framework**: Spring Boot 3.5.7-SNAPSHOT
- **Build Tool**: Maven 3.9+
- **Database**: PostgreSQL 16
- **Cache**: Redis 7.x (development via Docker Compose)
- **Security**: Spring Security with JWT authentication
- **ORM**: Spring Data JPA with Hibernate
- **Migrations**: Liquibase (to be configured)
- **WebSocket**: Spring WebSocket with STOMP protocol

### Key Libraries (Planned)
- **JWT**: `io.jsonwebtoken:jjwt` 0.12.6
- **Mapping**: MapStruct 1.6.0
- **API Docs**: springdoc-openapi 2.6.0
- **Monitoring**: Micrometer + Prometheus
- **Reports**: iText 8.0.3 (simple PDFs), JasperReports 6.21.3 (complex reports), Apache POI 5.2.5 (Excel)
- **Redis Client**: Lettuce (included in Spring Data Redis)
- **Integrations**: Mercado Pago SDK, Twilio (WhatsApp), Telegram Bots API

### Frontend (Planned)
- **Library**: React 19.0.0 with TypeScript 5.9
- **Build Tool**: Vite 6.0+
- **State**: Redux Toolkit 2.9.0 (UI state) + React Query 5.62.0 (server state)
- **HTTP**: Axios 1.7.9 with interceptors for JWT
- **WebSocket**: STOMP.js 7.0.0 + SockJS 1.6.1
- **UI**: Tailwind CSS 4.0 + shadcn/ui (Radix UI components)
- **Forms**: React Hook Form 7.54.0 + Zod 3.24.1
- **Charts**: Apache ECharts 5.5.1
- **Routing**: React Router 7.0.0

## Architecture

### Package Structure (To Be Implemented)

The project will follow a **modular monolith** architecture with vertical slices:

```
src/main/java/com/example/pitstop/
├── config/                    # Spring configuration classes
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   ├── CacheConfig.java
│   └── OpenAPIConfig.java
├── shared/                    # Shared infrastructure
│   ├── security/             # JWT, authentication
│   ├── exception/            # Global exception handling
│   ├── dto/                  # Common DTOs
│   └── utils/                # Utilities
├── cliente/                  # Customer module
│   ├── domain/              # Entities, value objects
│   ├── repository/          # JPA repositories
│   ├── service/             # Business logic
│   ├── controller/          # REST endpoints
│   └── dto/                 # Module-specific DTOs
├── veiculo/                 # Vehicle module
├── ordemservico/            # Service order module
├── estoque/                 # Inventory module
├── financeiro/              # Financial module
├── usuario/                 # User management module
└── notificacao/             # Notification module (WebSocket)
```

### Domain Model

#### Core Entities

**Cliente** (Customer)
- `UUID id` (PK)
- `TipoCliente tipo` (PESSOA_FISICA | PESSOA_JURIDICA)
- `String nome`, `cpfCnpj` (unique), `email`, `telefone`, `celular`
- `Endereco endereco` (embedded: logradouro, numero, complemento, bairro, cidade, estado, cep)
- `Boolean ativo`
- Timestamps: `createdAt`, `updatedAt`
- Relationship: One-to-Many with Veiculo

**Veiculo** (Vehicle)
- `UUID id` (PK), `UUID clienteId` (FK)
- `String placa` (unique), `marca`, `modelo`, `ano`, `cor`, `chassi`
- `Integer quilometragem`
- Timestamps: `createdAt`, `updatedAt`
- Relationship: One-to-Many with OrdemServico

**OrdemServico** (Service Order)
- `UUID id` (PK), `Long numero` (sequential unique), `UUID veiculoId` (FK), `UUID usuarioId` (FK - mechanic)
- `StatusOS status` enum (ORCAMENTO, APROVADO, EM_ANDAMENTO, AGUARDANDO_PECA, FINALIZADO, ENTREGUE, CANCELADO)
- Dates: `dataAbertura`, `dataPrevisao`, `dataFinalizacao`, `dataEntrega`
- `String problemasRelatados`, `diagnostico`, `observacoes`
- Financial: `valorMaoObra`, `valorPecas`, `valorTotal`, `descontoPercentual`, `descontoValor`, `valorFinal`
- `Boolean aprovadoPeloCliente`
- Timestamps: `createdAt`, `updatedAt`
- Relationships: One-to-Many with ItemOS, Pagamento, MovimentacaoEstoque

**ItemOS** (Service Order Item)
- `UUID id` (PK), `UUID ordemServicoId` (FK)
- `TipoItem tipo` (PECA | SERVICO)
- `UUID pecaId` (FK, nullable)
- `String descricao`, `Integer quantidade`, `BigDecimal valorUnitario`, `valorTotal`, `desconto`

**Peca** (Part/Inventory Item)
- `UUID id` (PK), `String codigo` (unique)
- `String descricao`, `marca`, `aplicacao`, `localizacao`
- Stock: `quantidadeAtual`, `quantidadeMinima`
- Financial: `valorCusto`, `valorVenda`, `BigDecimal margemLucro`
- `Boolean ativo`
- Timestamps: `createdAt`, `updatedAt`

**MovimentacaoEstoque** (Inventory Movement)
- `UUID id` (PK), `UUID pecaId` (FK), `UUID ordemServicoId` (FK, nullable), `UUID usuarioId` (FK)
- `TipoMovimentacao tipo` (ENTRADA | SAIDA | AJUSTE | DEVOLUCAO)
- `Integer quantidade`, `BigDecimal valorUnitario`, `valorTotal`
- `String motivo`, `observacao`, `LocalDateTime dataMovimentacao`
- Timestamp: `createdAt`

**Pagamento** (Payment)
- `UUID id` (PK), `UUID ordemServicoId` (FK)
- `TipoPagamento tipo` (DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, PIX, TRANSFERENCIA, BOLETO)
- `BigDecimal valor`, `LocalDate dataPagamento`, `dataVencimento`
- `StatusPagamento status` (PENDENTE, PAGO, CANCELADO, ESTORNADO)
- `String observacao`
- Timestamp: `createdAt`

**Usuario** (User)
- `UUID id` (PK)
- `String nome`, `email` (unique), `senha` (BCrypt hashed)
- `PerfilUsuario perfil` (ADMIN, GERENTE, ATENDENTE, MECANICO)
- `Boolean ativo`, `LocalDateTime ultimoAcesso`
- Timestamps: `createdAt`, `updatedAt`

### Database Indexes (To Be Created via Liquibase)

Performance-critical indexes:
- `cliente.cpfCnpj`, `veiculo.placa`, `peca.codigo`, `usuario.email` (unique indexes)
- `ordemServico.numero` (unique)
- `ordemServico.status`, `ordemServico.dataAbertura`
- Composite: `ordemServico(status, dataAbertura)`, `movimentacaoEstoque(pecaId, dataMovimentacao)`

## Security & Authentication

### JWT Implementation

**Token Strategy**:
- **Access Token**: 15 minutes validity, stored in memory (frontend), contains `userId`, `email`, `perfil`, `exp`
- **Refresh Token**: 7 days validity, stored in HttpOnly cookie + Redis, rotated on each refresh
- **Algorithm**: HS512 (HMAC SHA-512) with 256-bit secret (environment variable)

**Authentication Flow**:
1. POST `/api/auth/login` with `{email, senha}`
2. Backend validates with BCrypt (12 rounds)
3. Generate Access + Refresh tokens
4. Return tokens + user data
5. Frontend stores in memory, Axios interceptor adds to requests
6. On 401: Call `/api/auth/refresh`, retry original request

### Authorization (RBAC)

**Role Permissions**:
- **ADMIN**: Full system access including user management
- **GERENTE**: All modules except user management, financial reports, approve discounts
- **ATENDENTE**: CRUD for customers/vehicles/service orders, view inventory, register payments
- **MECANICO**: View assigned service orders, update status, add notes, read-only inventory

**Implementation**: Spring Security `@PreAuthorize` annotations at method level

## API Design

### REST Conventions

**Base URL**: `http://localhost:8080/api`

**Standards**:
- RESTful endpoints with proper HTTP methods (GET, POST, PUT, DELETE, PATCH)
- JSON content-type
- Pagination: `?page=0&size=20`
- Sorting: `?sort=nome,asc`
- Filtering: `?status=ATIVO&nome=João`

**Response Format**:
```json
// Success
{
  "data": {...},
  "timestamp": "2025-10-16T10:30:00Z"
}

// Error
{
  "error": "RESOURCE_NOT_FOUND",
  "message": "Cliente não encontrado",
  "status": 404,
  "timestamp": "2025-10-16T10:30:00Z",
  "path": "/api/clientes/123"
}

// Paginated
{
  "content": [...],
  "pageable": {...},
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

### WebSocket (Real-time)

**Protocol**: STOMP over WebSocket/SockJS
**Connection**: `ws://localhost:8080/ws`

**Destinations**:
- User-specific: `/user/queue/notifications`, `/user/queue/messages`
- Broadcast: `/topic/os-updates`, `/topic/estoque-alerts`, `/topic/dashboard-updates`

**Message Format**:
```json
{
  "tipo": "OS_STATUS_CHANGED",
  "titulo": "OS Finalizada",
  "mensagem": "OS #123 foi finalizada",
  "timestamp": "2025-10-16T10:30:00Z",
  "dados": {
    "osId": "uuid",
    "novoStatus": "FINALIZADA"
  }
}
```

## Caching Strategy

**Redis-based caching** (Spring Cache abstraction):
- **Categorias/Peças**: 24h TTL (static data)
- **Consultas frequentes**: 1h TTL
- **Sessões de usuário**: 30min TTL
- **Relatórios**: 15min TTL
- Cache invalidation on CREATE/UPDATE/DELETE operations via `@CacheEvict`

## Testing Strategy

### Backend Tests

**Unit Tests** (JUnit 5 + Mockito + AssertJ):
- Services, repositories, utilities, validators
- Target: 80% coverage
- Naming: `ClassNameTest.java`

**Integration Tests** (Spring Boot Test + TestContainers):
- Controllers with MockMvc
- Database operations with PostgreSQL container
- Redis caching
- Security filters
- Target: 70% coverage
- Naming: `ClassNameIntegrationTest.java`

**E2E Tests** (Postman/Newman or Karate):
- Complete workflows
- API contracts
- Naming: `FeatureE2ETest.java`

**TestContainers**: Isolated PostgreSQL and Redis containers for reproducible integration tests

## Observability

### Actuator Endpoints
- `/actuator/health` - Health checks
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus scraping endpoint
- `/actuator/info` - Application info

### Custom Metrics (Micrometer)
Business metrics:
- `pitstop.os.created` (counter)
- `pitstop.os.processing.time` (timer)
- `pitstop.os.in.progress` (gauge)
- `pitstop.faturamento.diario` (gauge)
- `pitstop.estoque.baixo` (gauge)

### Logging
- **Framework**: SLF4J + Logback (Spring Boot default)
- **Development**: Colorized console output
- **Production**: JSON structured logs
- **Levels**: ERROR (critical), WARN (important), INFO (flow), DEBUG/TRACE (dev only)
- **Correlation**: Include `userId` and `requestId` in logs

## Integrations (Planned)

### Payment Gateway
- **Provider**: Mercado Pago SDK 2.1.26
- **Methods**: PIX (QR Code), credit/debit cards, boleto
- **Webhooks**: Automatic OS status update on payment confirmation

### Messaging
- **WhatsApp**: Twilio WhatsApp Business API (SDK 10.4.1) or Evolution API (self-hosted)
- **Telegram**: Telegram Bots API 6.9.7.1 for internal notifications
- **Email**: Spring Boot Mail Sender + AWS SES (production) or SMTP (dev)

### Templates
- Email templates: Thymeleaf HTML with PDF attachments (iText)
- WhatsApp templates: Pre-approved with variables `{nome}`, `{veiculo}`, `{valor}`, `{data}`

## Development Roadmap

### Phase 1 - MVP (10 weeks)
**Weeks 1-2**: Infrastructure setup (project structure, Docker Compose, migrations, security)
**Weeks 3-4**: Customers & Vehicles modules (CRUD + API)
**Weeks 5-7**: Service Orders (complex business logic, PDF generation, WebSocket)
**Weeks 8-9**: Inventory & Financial modules (stock control, payments)
**Week 10**: Testing, bug fixes, deployment

### Phase 2 - Enhancements (4 weeks)
Basic reports (PDF service orders, Excel exports), automated emails, dashboard with charts, advanced filters

### Phase 3 - Complete (6 weeks)
Observability (Prometheus + Grafana), advanced reports (JasperReports), payment integration, WhatsApp/Telegram bots

## Environment Configuration

### Required Environment Variables
```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pitstop
SPRING_DATASOURCE_USERNAME=myuser
SPRING_DATASOURCE_PASSWORD=secret

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# JWT
JWT_SECRET=<256-bit-random-key>
JWT_ACCESS_EXPIRATION=900000  # 15 min in ms
JWT_REFRESH_EXPIRATION=604800000  # 7 days in ms

# Email (development)
SPRING_MAIL_HOST=smtp.mailtrap.io
SPRING_MAIL_PORT=2525
SPRING_MAIL_USERNAME=<mailtrap-user>
SPRING_MAIL_PASSWORD=<mailtrap-pass>

# Production integrations (when ready)
MERCADOPAGO_ACCESS_TOKEN=<token>
TWILIO_ACCOUNT_SID=<sid>
TWILIO_AUTH_TOKEN=<token>
AWS_SES_ACCESS_KEY=<key>
AWS_SES_SECRET_KEY=<secret>
```

### Development vs Production
- **Development**: Uses Docker Compose for PostgreSQL and Redis (automatically started by Spring Boot Docker Compose support)
- **Production**: Managed database services (AWS RDS, ElastiCache or DigitalOcean managed databases)

## Common Patterns

### Entity Validation
Use Bean Validation annotations (`@NotNull`, `@Size`, `@Email`, etc.) + custom validators for complex business rules

### DTO Mapping
Use MapStruct for compile-time mapping between entities and DTOs (high performance, type-safe)

### Exception Handling
Implement global `@ControllerAdvice` with `@ExceptionHandler` methods for consistent error responses

### Transactional Operations
Use `@Transactional` at service layer for operations requiring ACID guarantees (especially service order creation with inventory movements)

## Important Notes

- **Java 25 LTS**: Leverage modern features (Virtual Threads, Pattern Matching, Records for DTOs, Sealed Classes for enums)
- **Package Naming**: Current package is `com.example.pitstop.pitstop` - consider refactoring to `com.pitstop` for cleaner structure
- **Spring Boot 3.5.7-SNAPSHOT**: This is a snapshot version; consider switching to stable 3.5.x release when available
- **PostgreSQL Docker**: Current `compose.yaml` uses `postgres:latest` - should pin to `postgres:16-alpine` for consistency
- **Liquibase**: Not yet configured - needs to be added to `pom.xml` and configured in `application.properties`
- **Redis**: Needs to be added to `compose.yaml` and dependencies for caching
- **API Documentation**: Add springdoc-openapi dependency for auto-generated Swagger UI at `/swagger-ui.html`

## Quick Start Checklist

1. Ensure Java 25 is installed (`java -version`)
2. Start Docker Desktop
3. Run `docker-compose up -d` to start PostgreSQL
4. Run `./mvnw spring-boot:run` to start the application
5. Application should start on `http://localhost:8080`
6. Access Actuator health: `http://localhost:8080/actuator/health`

## Additional Resources

See `Sistema de Gerenciamento de Oficina Mecânica.md` for complete technical specification including detailed stack decisions, architecture diagrams, and implementation justifications.
