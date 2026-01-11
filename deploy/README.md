# 🚀 PitStop + Evolution API - Guia de Deploy

## Arquitetura

```
┌─────────────────────────────────────────────────────────────────────┐
│                     VPS CONTABO (12GB RAM)                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─── NGINX (Reverse Proxy) ───┐                                   │
│  │  :80 → :443 (HTTPS)         │                                   │
│  │  SSL/TLS Let's Encrypt      │                                   │
│  └──────────────┬──────────────┘                                   │
│                 │                                                   │
│    ┌────────────┼────────────┐                                     │
│    ▼            ▼            ▼                                     │
│  ┌────┐    ┌────────┐    ┌──────────────┐                         │
│  │ /  │    │ /api/* │    │ evolution.   │                         │
│  └──┬─┘    └───┬────┘    │ domain.com   │                         │
│     │          │         └──────┬───────┘                         │
│     ▼          ▼                ▼                                  │
│  ┌────────┐ ┌────────────┐ ┌──────────────┐                       │
│  │Frontend│ │  Backend   │ │ Evolution    │                       │
│  │ :3000  │ │   :8080    │ │  API :8021   │                       │
│  │ React  │ │Spring Boot │ │  WhatsApp    │                       │
│  └────────┘ └─────┬──────┘ └──────┬───────┘                       │
│                   │               │                                │
│         ┌─────────┴───────────────┘                                │
│         ▼                                                          │
│  ┌──────────────────────────────────────────────┐                 │
│  │              Docker Network                   │                 │
│  │  ┌──────────┐ ┌──────────┐ ┌───────────┐    │                 │
│  │  │PostgreSQL│ │PostgreSQL│ │   Redis   │    │                 │
│  │  │ PitStop  │ │Evolution │ │   :6379   │    │                 │
│  │  │  :5432   │ │  :5433   │ │           │    │                 │
│  │  └──────────┘ └──────────┘ └───────────┘    │                 │
│  └──────────────────────────────────────────────┘                 │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Pré-requisitos

- VPS com Ubuntu 22.04 LTS ou superior
- Mínimo 8GB RAM (recomendado 12GB)
- Mínimo 40GB SSD
- Domínio apontando para o IP da VPS
- Acesso root via SSH

## Instalação Rápida

### 1. Conectar na VPS

```bash
ssh root@seu-ip-da-vps
```

### 2. Baixar os arquivos de deploy

```bash
# Opção A: Clonar repositório
git clone https://github.com/seu-usuario/pitstop.git /tmp/pitstop
cp -r /tmp/pitstop/deploy/* /root/

# Opção B: Upload manual via SCP (do seu PC)
scp -r deploy/* root@seu-ip-da-vps:/root/
```

### 3. Executar o script de instalação

```bash
chmod +x setup.sh
sudo bash setup.sh
```

O script irá:
- Instalar Docker e dependências
- Configurar PostgreSQL (2 instâncias)
- Configurar Redis
- Solicitar informações (domínio, email, dados da oficina)
- Gerar senhas seguras automaticamente
- Configurar Nginx e SSL
- Configurar Firewall e Fail2Ban
- Criar scripts de manutenção

### 4. Copiar código fonte

Após o setup, copie o código fonte:

```bash
# Backend
scp -r src pom.xml Dockerfile root@seu-ip:/opt/pitstop/backend/

# Frontend
scp -r frontend/* root@seu-ip:/opt/pitstop/frontend/
```

### 5. Build e deploy

```bash
cd /opt/pitstop
docker compose up -d --build
```

## Estrutura de Arquivos

```
/opt/pitstop/
├── .env                    # Variáveis de ambiente (PROTEGER!)
├── docker-compose.yml      # Configuração dos containers
├── backend/                # Código fonte do backend
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/               # Código fonte do frontend
│   ├── src/
│   ├── package.json
│   ├── Dockerfile
│   └── nginx.conf
├── nginx/                  # Configurações Nginx
│   └── pitstop.conf
├── data/                   # Dados persistentes
│   ├── postgres-pitstop/
│   ├── postgres-evolution/
│   └── redis/
├── logs/                   # Logs da aplicação
├── backups/                # Backups automáticos
└── scripts/                # Scripts de manutenção
    ├── backup.sh
    ├── status.sh
    ├── logs.sh
    ├── restart.sh
    └── deploy.sh
```

## Comandos de Manutenção

Após a instalação, use estes comandos:

```bash
# Ver status dos serviços
ps-status

# Ver logs (backend, frontend, evolution, postgres, redis, nginx, all)
ps-logs backend
ps-logs all

# Reiniciar serviços
ps-restart all
ps-restart backend

# Executar backup manual
ps-backup

# Fazer deploy de atualização
ps-deploy

# Ir para o diretório do projeto
ps-cd
```

## Variáveis de Ambiente

O arquivo `/opt/pitstop/.env` contém todas as configurações:

### Obrigatórias
| Variável | Descrição |
|----------|-----------|
| `DOMAIN` | Domínio principal (ex: pitstop.empresa.com) |
| `EVOLUTION_DOMAIN` | Domínio do WhatsApp (ex: whatsapp.empresa.com) |
| `JWT_SECRET` | Chave secreta para tokens (64 caracteres) |
| `POSTGRES_*_PASSWORD` | Senhas dos bancos de dados |
| `REDIS_PASSWORD` | Senha do Redis |

### Opcionais (configurar depois)
| Variável | Descrição |
|----------|-----------|
| `TELEGRAM_BOT_TOKEN` | Token do bot do Telegram |
| `MAIL_*` | Configurações de email SMTP |
| `MERCADOPAGO_*` | Credenciais do Mercado Pago |
| `ANTHROPIC_API_KEY` | API Key da Anthropic (IA) |

## Configuração do WhatsApp (Evolution API)

1. Acesse: `https://whatsapp.seudominio.com`

2. Use a API Key gerada (veja em `.env`: `EVOLUTION_API_KEY`)

3. Crie uma instância:
```bash
curl -X POST "https://whatsapp.seudominio.com/instance/create" \
  -H "apikey: SUA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"instanceName": "pitstop", "qrcode": true}'
```

4. Escaneie o QR Code com o WhatsApp

## SSL/Certificados

Os certificados são renovados automaticamente. Para renovação manual:

```bash
certbot renew
systemctl reload nginx
```

## Backups

### Automáticos
- Executados diariamente às 3h
- Mantidos por 7 dias
- Salvos em `/opt/pitstop/backups/`

### Manual
```bash
/opt/pitstop/scripts/backup.sh
```

### Restaurar Backup
```bash
# PostgreSQL PitStop
gunzip -c /opt/pitstop/backups/pitstop_db_XXXXXXXX.sql.gz | \
  docker exec -i pitstop-postgres psql -U pitstop pitstop_db

# PostgreSQL Evolution
gunzip -c /opt/pitstop/backups/evolution_db_XXXXXXXX.sql.gz | \
  docker exec -i evolution-postgres psql -U evolution evolution_db
```

## Monitoramento

### Health Checks
```bash
# Backend
curl http://localhost:8080/actuator/health

# Evolution API
curl http://localhost:8021/

# Frontend
curl http://localhost:3000/health
```

### Métricas Prometheus
```bash
curl http://localhost:8080/actuator/prometheus
```

## Troubleshooting

### Container não inicia
```bash
# Ver logs do container
docker logs pitstop-backend

# Verificar status
docker ps -a

# Reiniciar container específico
docker restart pitstop-backend
```

### Erro de conexão com banco
```bash
# Testar conexão PostgreSQL
docker exec -it pitstop-postgres psql -U pitstop -d pitstop_db -c "SELECT 1"

# Testar conexão Redis
docker exec -it pitstop-redis redis-cli -a SENHA ping
```

### Erro de SSL
```bash
# Verificar certificados
certbot certificates

# Renovar forçado
certbot renew --force-renewal
systemctl reload nginx
```

### Verificar portas
```bash
ss -tlnp | grep -E '(8080|8021|5432|5433|6379|80|443)'
```

## Atualização do Sistema

### Atualizar código

```bash
# 1. Fazer backup
ps-backup

# 2. Parar serviços
cd /opt/pitstop
docker compose down

# 3. Atualizar código fonte
# (copie os novos arquivos para backend/ e frontend/)

# 4. Rebuild e restart
docker compose up -d --build
```

### Atualizar containers base
```bash
docker compose pull
docker compose up -d --build
```

## Segurança

- ✅ Firewall UFW habilitado (apenas 22, 80, 443)
- ✅ Fail2Ban protegendo SSH e Nginx
- ✅ SSL/TLS com Let's Encrypt
- ✅ Senhas geradas automaticamente
- ✅ Rate limiting no Nginx
- ✅ Headers de segurança configurados
- ✅ Banco de dados não exposto externamente

### Recomendações adicionais
1. Configure autenticação SSH por chave (desabilite senha)
2. Mude a porta SSH padrão (22)
3. Configure alertas de monitoramento
4. Faça backups externos (S3, Google Cloud, etc.)

## CI/CD com GitHub Actions

O deploy é automatizado via GitHub Actions. A cada push na branch `main`:

1. **Build**: Compila backend (Maven) e frontend (Vite)
2. **Push**: Envia imagens para GitHub Container Registry
3. **Backup**: Faz backup automático do banco antes de atualizar
4. **Deploy**: Atualiza containers na VPS
5. **Health Check**: Verifica se a aplicação está saudável
6. **Rollback**: Reverte automaticamente em caso de falha

### Secrets Necessários no GitHub

| Secret | Descrição |
|--------|-----------|
| `VPS_HOST` | IP ou hostname da VPS |
| `VPS_USER` | Usuário SSH (geralmente `root`) |
| `VPS_SSH_KEY` | Chave SSH privada para acesso |
| `GH_PAT` | Personal Access Token do GitHub |
| `POSTGRES_PITSTOP_DB` | Nome do banco (ex: `pitstop_db`) |
| `POSTGRES_PITSTOP_USER` | Usuário do banco (ex: `pitstop`) |
| `POSTGRES_PITSTOP_PASSWORD` | Senha do PostgreSQL |
| `REDIS_PASSWORD` | Senha do Redis |
| `DATABASE_URL` | URL JDBC completa |
| `DATABASE_USERNAME` | Usuário do banco |
| `DATABASE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave secreta JWT (256 bits) |
| `APP_FRONTEND_URL` | URL do frontend (ex: `https://app.pitstopai.com.br`) |
| `APP_BASE_URL` | URL base da API (ex: `https://pitstopai.com.br`) |
| `CORS_ALLOWED_ORIGINS` | URLs permitidas para CORS |
| `VITE_API_URL` | URL da API para o frontend |
| `VITE_WS_URL` | URL do WebSocket |
| `VITE_API_BASE_URL` | URL base para requisições |

### Migração de Volumes (IMPORTANTE)

Se você já tem dados na VPS e está atualizando o docker-compose, execute o script de migração ANTES do próximo deploy:

```bash
# Na VPS
cd /opt/pitstop
curl -O https://raw.githubusercontent.com/seu-usuario/pitstop/main/deploy/migrate-volumes.sh
chmod +x migrate-volumes.sh
sudo bash migrate-volumes.sh
```

Este script:
- Identifica volumes existentes
- Cria os novos volumes externos com nomes fixos
- Copia os dados preservando a integridade
- Evita perda de dados em deploys futuros

### Proteções Implementadas

1. **Volumes Externos**: Volumes com nomes fixos (`pitstop_postgres_data`, `pitstop_redis_data`) que não são recriados
2. **Backup Automático**: Backup do banco ANTES de cada deploy
3. **Health Check**: Verifica se backend está respondendo
4. **Rollback Automático**: Reverte para versão anterior se health check falhar
5. **Logs de Falha**: Mostra logs dos containers se deploy falhar

## Suporte

- Logs: `/opt/pitstop/logs/`
- Backups: `/opt/pitstop/backups/`
- Configuração: `/opt/pitstop/.env`

---

**PitStop** - Sistema de Gestão para Oficinas Mecânicas
