# 🚗 PitStop - Sistema de Gerenciamento de Oficina Mecânica

Sistema web completo para gestão de oficinas mecânicas (pequeno e médio porte), com controle de ordens de serviço, estoque, clientes, veículos, financeiro e notificações em tempo real.

## 📋 Sobre o Projeto

**PitStop** é uma solução moderna e completa para automatizar a gestão de oficinas mecânicas, oferecendo:

- ✅ Controle de Ordens de Serviço
- 📦 Gerenciamento de Estoque de Peças
- 👥 Cadastro de Clientes e Veículos
- 💰 Controle Financeiro e Pagamentos
- 📊 Dashboard com métricas e gráficos
- 🔔 Notificações em tempo real (WebSocket)
- 📱 Integração WhatsApp (Evolution API)
- 🔐 Autenticação JWT com controle de permissões
- 📄 Geração de PDFs e relatórios

## 🛠️ Stack Tecnológica

### Backend
- **Java 25 LTS** (Oracle No-Fee License)
- **Spring Boot 3.5.7**
- **PostgreSQL 16**
- **Redis 7** (cache)
- **Spring Security** + JWT
- **WebSocket** (STOMP)
- **Liquibase** (migrations)

### Frontend
- **React 19** + TypeScript
- **Vite 6**
- **Redux Toolkit** + React Query
- **Tailwind CSS** + shadcn/ui
- **React Hook Form** + Zod
- **Apache ECharts** (gráficos)

### Integrações
- **Evolution API** (WhatsApp)
- **Mercado Pago** (pagamentos)
- **AWS SES** (emails)
- **Telegram Bot** (notificações)

## 🚀 Quick Start

### Desenvolvimento Local

**Pré-requisitos:**
- Java 25
- Node.js 20+
- Docker Desktop (para PostgreSQL e Redis)

**1. Clonar repositório:**
```bash
git clone https://github.com/seu-usuario/pitstop.git
cd pitstop
```

**2. Configurar ambiente:**
```bash
# Copiar .env de exemplo
cp .env.example .env

# Editar com suas configurações
nano .env
```

**3. Subir banco de dados e cache:**
```bash
# Usar docker-compose de desenvolvimento
docker compose -f docker-compose.dev.yml up -d

# Ou apenas PostgreSQL e Redis
docker compose up -d postgres redis
```

**4. Iniciar backend:**
```bash
# Linux/Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

**5. Iniciar frontend:**
```bash
cd frontend
npm install
npm run dev
```

**Acessar:**
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- Adminer (DB): http://localhost:8082

### Deploy em Produção (Contabo VPS)

**Opção 1: Quick Start (5 minutos)**

Consulte [QUICKSTART.md](./QUICKSTART.md) para deploy rápido.

**Opção 2: Guia Completo**

Consulte [DEPLOY.md](./DEPLOY.md) para instruções detalhadas.

**Instalação automática do servidor:**
```bash
# Executar no servidor VPS Ubuntu 22.04+
curl -fsSL https://raw.githubusercontent.com/seu-usuario/pitstop/main/scripts/install-server.sh | sudo bash
```

## 📁 Estrutura do Projeto

```
pitstop/
├── src/main/java/com/pitstop/          # Backend Java/Spring Boot
│   ├── config/                         # Configurações
│   ├── shared/                         # Código compartilhado
│   ├── cliente/                        # Módulo de clientes
│   ├── veiculo/                        # Módulo de veículos
│   ├── ordemservico/                   # Módulo de ordens de serviço
│   ├── estoque/                        # Módulo de estoque
│   ├── financeiro/                     # Módulo financeiro
│   └── usuario/                        # Módulo de usuários
├── frontend/                           # Frontend React
│   ├── src/
│   │   ├── features/                   # Módulos por funcionalidade
│   │   ├── shared/                     # Componentes compartilhados
│   │   └── store/                      # Redux store
│   ├── Dockerfile                      # Build produção frontend
│   └── nginx.conf                      # Configuração Nginx
├── scripts/                            # Scripts de deploy/backup
│   ├── install-server.sh              # Instalação automática VPS
│   ├── backup.sh                      # Backup automático
│   ├── restore.sh                     # Restauração de backup
│   └── server-commands.md             # Comandos úteis
├── Dockerfile                          # Build produção backend
├── docker-compose.prod.yml             # Deploy produção
├── docker-compose.dev.yml              # Desenvolvimento local
├── DEPLOY.md                           # Guia de deploy completo
├── QUICKSTART.md                       # Deploy rápido
└── CLAUDE.md                           # Documentação técnica
```

## 📚 Documentação

- **[CLAUDE.md](./CLAUDE.md)** - Documentação técnica completa (arquitetura, stack, padrões)
- **[DEPLOY.md](./DEPLOY.md)** - Guia detalhado de deploy em produção
- **[QUICKSTART.md](./QUICKSTART.md)** - Deploy rápido em 5 minutos
- **[scripts/README.md](./scripts/README.md)** - Documentação dos scripts
- **[scripts/server-commands.md](./scripts/server-commands.md)** - Comandos úteis do servidor

## 🔧 Comandos Úteis

### Desenvolvimento

```bash
# Backend
./mvnw spring-boot:run              # Iniciar backend
./mvnw test                         # Rodar testes
./mvnw clean package               # Gerar JAR

# Frontend
npm run dev                         # Servidor desenvolvimento
npm run build                       # Build produção
npm run preview                     # Preview build local

# Docker
docker compose up -d                # Subir PostgreSQL e Redis
docker compose logs -f              # Ver logs
docker compose down                 # Parar tudo
```

### Produção

```bash
# Deploy
docker compose -f docker-compose.prod.yml up -d

# Logs
docker compose -f docker-compose.prod.yml logs -f backend

# Status
docker compose -f docker-compose.prod.yml ps

# Backup
./scripts/backup.sh manual

# Restore
./scripts/restore.sh /path/to/backup.sql.gz
```

## 🔐 Segurança

- ✅ Autenticação JWT (Access + Refresh tokens)
- ✅ Senhas com BCrypt (12 rounds)
- ✅ RBAC (4 níveis: ADMIN, GERENTE, ATENDENTE, MECANICO)
- ✅ HTTPS/SSL via Let's Encrypt
- ✅ Firewall (UFW) configurado
- ✅ Fail2ban para proteção SSH
- ✅ CORS configurado
- ✅ Headers de segurança (X-Frame-Options, CSP, etc)
- ✅ Rate limiting no Nginx

## 💾 Backup e Recuperação

**Backups automáticos:**
- Diários: 2h da manhã (mantidos 30 dias)
- Semanais: Domingos 3h (mantidos 12 semanas)

**Configurar:**
```bash
chmod +x scripts/backup.sh
crontab -e

# Adicionar:
0 2 * * * /opt/pitstop/scripts/backup.sh diario
0 3 * * 0 /opt/pitstop/scripts/backup.sh semanal
```

## 📊 Monitoramento

**Health checks disponíveis:**
- Backend: `/actuator/health`
- Frontend: `/health`
- Evolution API: `/health`

**Métricas (Prometheus):**
- `/actuator/prometheus`

**Logs:**
```bash
# Ver logs em tempo real
docker compose -f docker-compose.prod.yml logs -f

# Backend apenas
docker logs pitstop-backend -f
```

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -m 'Add: nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

## 📝 Roadmap

### MVP (Atual)
- [x] Estrutura base do projeto
- [x] Autenticação e autorização
- [x] CRUD de clientes e veículos
- [x] Ordens de serviço completas
- [x] Controle de estoque
- [x] Módulo financeiro
- [x] Dashboard com métricas
- [x] WebSocket para notificações
- [ ] Geração de PDFs
- [ ] Testes automatizados (>80% coverage)

### Fase 2
- [ ] Relatórios avançados (PDF/Excel)
- [ ] Integração Mercado Pago
- [ ] WhatsApp automatizado (Evolution API)
- [ ] Email notifications
- [ ] PWA (Progressive Web App)

### Fase 3
- [ ] Multi-tenancy (SaaS)
- [ ] Mobile app (React Native)
- [ ] Integração Telegram
- [ ] BI/Analytics avançado
- [ ] API pública

## 💰 Custos de Hospedagem

**Contabo VPS:**
- VPS M (8GB RAM): ~€9.99/mês (~R$ 60/mês)
- VPS L (16GB RAM): ~€14.99/mês (~R$ 90/mês)

**Adicionais:**
- Domínio: ~R$ 40/ano
- SSL: Grátis (Let's Encrypt)
- Backups cloud: ~$5/mês (opcional)

**Total inicial:** R$ 60-90/mês

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 👨‍💻 Autor

Desenvolvido com ❤️ para oficinas mecânicas

## 📞 Suporte

- Issues: https://github.com/seu-usuario/pitstop/issues
- Documentação: [CLAUDE.md](./CLAUDE.md)
- Deploy: [DEPLOY.md](./DEPLOY.md)

---

**🚗💨 PitStop - Acelere sua oficina!**
