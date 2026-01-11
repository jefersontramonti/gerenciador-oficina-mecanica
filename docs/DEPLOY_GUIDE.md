# PitStop - Guia Completo de Deploy

Este documento contém todas as configurações necessárias para deploy do PitStop em produção.

## Arquitetura de Produção

```
                    Internet
                        │
                        ▼
              ┌─────────────────┐
              │   Nginx (HOST)  │  ← SSL/TLS termination
              │    :80 / :443   │
              └────────┬────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
   app.domain     api.domain    whatsapp.domain
        │              │              │
        ▼              ▼              ▼
   ┌─────────┐   ┌──────────┐   ┌──────────┐
   │Frontend │   │ Backend  │   │Evolution │
   │  :3000  │   │  :8080   │   │  :8021   │
   └─────────┘   └──────────┘   └──────────┘
        │              │
        │         ┌────┴────┐
        │         ▼         ▼
        │   ┌─────────┐ ┌───────┐
        │   │PostgreSQL│ │ Redis │
        │   │  :5432  │ │ :6379 │
        │   └─────────┘ └───────┘
        │
   ┌────┴────┐
   │  Nginx  │  ← Dentro do container
   │(interno)│     Serve arquivos estáticos
   └─────────┘     Proxy para backend (/api, /ws)
```

## 1. Domínios e DNS

Configure os seguintes registros DNS (tipo A) apontando para o IP da VPS:

| Subdomínio | Uso |
|------------|-----|
| `pitstopai.com.br` | Landing page |
| `www.pitstopai.com.br` | Landing page (redirect) |
| `app.pitstopai.com.br` | Frontend React |
| `api.pitstopai.com.br` | Backend Spring Boot |
| `whatsapp.pitstopai.com.br` | Evolution API (WhatsApp) |

## 2. Variáveis de Ambiente (.env.production)

```bash
#===============================================================================
# PitStop AI - Variáveis de Ambiente (Produção VPS)
# Arquivo de referência - copiar para /opt/pitstop/.env na VPS
#===============================================================================

#--- DOMÍNIOS ---
DOMAIN_MAIN=pitstopai.com.br
DOMAIN_APP=app.pitstopai.com.br
DOMAIN_API=api.pitstopai.com.br
DOMAIN_WHATSAPP=whatsapp.pitstopai.com.br
SSL_EMAIL=noreply@pitstopai.com.br

#--- URLs DA APLICAÇÃO ---
APP_FRONTEND_URL=https://app.pitstopai.com.br
APP_BASE_URL=https://api.pitstopai.com.br
CORS_ALLOWED_ORIGINS=https://app.pitstopai.com.br,https://pitstopai.com.br

#--- POSTGRESQL ---
POSTGRES_PITSTOP_DB=pitstop_db
POSTGRES_PITSTOP_USER=pitstop
POSTGRES_PITSTOP_PASSWORD=<SENHA_SEGURA_GERADA>
DATABASE_URL=jdbc:postgresql://postgres-pitstop:5432/pitstop_db
DATABASE_USERNAME=pitstop
DATABASE_PASSWORD=<MESMA_SENHA_ACIMA>

#--- REDIS ---
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<SENHA_SEGURA_GERADA>

#--- JWT ---
# Gerar com: openssl rand -base64 64
JWT_SECRET=<CHAVE_BASE64_64_BYTES>

#--- SPRING ---
SPRING_PROFILES_ACTIVE=prod
APP_COOKIE_DOMAIN=.pitstopai.com.br

#--- OFICINA ---
PITSTOP_OFICINA_NOME="Nome da Oficina"
PITSTOP_OFICINA_CNPJ="00000000000000"
PITSTOP_OFICINA_TELEFONE="00000000000"
PITSTOP_OFICINA_EMAIL="contato@oficina.com"

#--- EMAIL (SMTP) ---
MAIL_HOST=smtp.hostinger.com
MAIL_PORT=465
MAIL_USERNAME=noreply@pitstopai.com.br
MAIL_PASSWORD=<SENHA_EMAIL>
MAIL_FROM_NAME=PitStop
MAIL_SMTP_AUTH=true
MAIL_SMTP_SSL_ENABLE=true
MAIL_SMTP_STARTTLS_ENABLE=false

#--- EVOLUTION API (WhatsApp) ---
EVOLUTION_API_URL=https://whatsapp.pitstopai.com.br
EVOLUTION_API_KEY=<CHAVE_API_EVOLUTION>

#--- VITE (Frontend Build Args) ---
VITE_API_URL=https://api.pitstopai.com.br/api
VITE_WS_URL=wss://api.pitstopai.com.br/ws
VITE_API_BASE_URL=https://api.pitstopai.com.br

#--- MERCADO PAGO (opcional) ---
# MERCADOPAGO_WEBHOOK_SECRET=<SECRET_DO_WEBHOOK>

#--- ANTHROPIC IA (opcional) ---
# ANTHROPIC_API_KEY=<CHAVE_API>
```

## 3. GitHub Secrets

Configure em: `Settings > Secrets and variables > Actions > New repository secret`

### Secrets Obrigatórios

| Secret | Valor | Descrição |
|--------|-------|-----------|
| `VPS_HOST` | `IP_DA_VPS` | IP ou hostname da VPS |
| `VPS_USER` | `root` | Usuário SSH |
| `VPS_SSH_KEY` | `-----BEGIN...` | Chave privada SSH completa |
| `GH_PAT` | `ghp_...` | GitHub Personal Access Token (read:packages) |

### Secrets de Aplicação

| Secret | Valor |
|--------|-------|
| `DATABASE_URL` | `jdbc:postgresql://postgres-pitstop:5432/pitstop_db` |
| `DATABASE_USERNAME` | `pitstop` |
| `DATABASE_PASSWORD` | Senha do PostgreSQL |
| `REDIS_PASSWORD` | Senha do Redis |
| `JWT_SECRET` | Chave JWT (base64, 64 bytes) |
| `CORS_ALLOWED_ORIGINS` | `https://app.pitstopai.com.br,https://pitstopai.com.br` |
| `APP_FRONTEND_URL` | `https://app.pitstopai.com.br` |
| `APP_BASE_URL` | `https://api.pitstopai.com.br` |

### Secrets do Frontend (Build Args)

| Secret | Valor |
|--------|-------|
| `VITE_API_URL` | `https://api.pitstopai.com.br/api` |
| `VITE_WS_URL` | `wss://api.pitstopai.com.br/ws` |
| `VITE_API_BASE_URL` | `https://api.pitstopai.com.br` |

### Secrets de Banco/Infra

| Secret | Valor |
|--------|-------|
| `POSTGRES_PITSTOP_DB` | `pitstop_db` |
| `POSTGRES_PITSTOP_USER` | `pitstop` |
| `POSTGRES_PITSTOP_PASSWORD` | Senha do PostgreSQL |

### Secrets de Email

| Secret | Valor |
|--------|-------|
| `MAIL_HOST` | `smtp.hostinger.com` |
| `MAIL_PORT` | `465` |
| `MAIL_USERNAME` | `noreply@pitstopai.com.br` |
| `MAIL_PASSWORD` | Senha do email |
| `MAIL_FROM_NAME` | `PitStop` |
| `MAIL_SMTP_AUTH` | `true` |
| `MAIL_SMTP_SSL_ENABLE` | `true` |
| `MAIL_SMTP_STARTTLS_ENABLE` | `false` |

### Secrets da Oficina

| Secret | Valor |
|--------|-------|
| `PITSTOP_OFICINA_NOME` | Nome da oficina |
| `PITSTOP_OFICINA_CNPJ` | CNPJ (apenas números) |
| `PITSTOP_OFICINA_TELEFONE` | Telefone (apenas números) |
| `PITSTOP_OFICINA_EMAIL` | Email da oficina |

### Secrets de Integrações

| Secret | Valor |
|--------|-------|
| `EVOLUTION_API_URL` | `https://whatsapp.pitstopai.com.br` |
| `EVOLUTION_API_KEY` | Chave da Evolution API |

## 4. Configuração Nginx (VPS Host)

Arquivo: `/etc/nginx/sites-available/pitstopai`

```nginx
#===============================================================================
# PitStop AI - Nginx Configuration
#===============================================================================

# Rate limiting
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=1r/s;

#---------------------------------------
# HTTP -> HTTPS Redirect (todos domínios)
#---------------------------------------
server {
    listen 80;
    listen [::]:80;
    server_name pitstopai.com.br www.pitstopai.com.br app.pitstopai.com.br api.pitstopai.com.br whatsapp.pitstopai.com.br;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

#---------------------------------------
# Landing Page - pitstopai.com.br
#---------------------------------------
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name pitstopai.com.br www.pitstopai.com.br;

    ssl_certificate /etc/letsencrypt/live/pitstopai.com.br/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pitstopai.com.br/privkey.pem;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:50m;
    ssl_protocols TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=63072000" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;

    root /opt/pitstop/landing;
    index index.html;

    access_log /var/log/nginx/landing_access.log;
    error_log /var/log/nginx/landing_error.log;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}

#---------------------------------------
# App Frontend - app.pitstopai.com.br
#---------------------------------------
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name app.pitstopai.com.br;

    ssl_certificate /etc/letsencrypt/live/pitstopai.com.br/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pitstopai.com.br/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=63072000" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml;

    access_log /var/log/nginx/app_access.log;
    error_log /var/log/nginx/app_error.log;

    client_max_body_size 50M;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

#---------------------------------------
# API Backend - api.pitstopai.com.br
#---------------------------------------
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name api.pitstopai.com.br;

    ssl_certificate /etc/letsencrypt/live/pitstopai.com.br/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pitstopai.com.br/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=63072000" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript;

    access_log /var/log/nginx/api_access.log;
    error_log /var/log/nginx/api_error.log;

    client_max_body_size 50M;

    # API geral
    location / {
        limit_req zone=api_limit burst=20 nodelay;

        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Rate limit mais restrito para login
    location /api/auth/login {
        limit_req zone=login_limit burst=3 nodelay;

        proxy_pass http://127.0.0.1:8080/api/auth/login;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket - IMPORTANTE: proxy_pass sem path para preservar URI
    location = /ws {
        return 301 /ws/;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 86400;
    }

    # Actuator (apenas local)
    location /actuator {
        allow 127.0.0.1;
        deny all;
        proxy_pass http://127.0.0.1:8080/actuator;
    }

    # Swagger UI
    location /swagger-ui {
        proxy_pass http://127.0.0.1:8080/swagger-ui;
        proxy_set_header Host $host;
    }

    location /v3/api-docs {
        proxy_pass http://127.0.0.1:8080/v3/api-docs;
    }
}

#---------------------------------------
# WhatsApp Evolution - whatsapp.pitstopai.com.br
#---------------------------------------
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name whatsapp.pitstopai.com.br;

    ssl_certificate /etc/letsencrypt/live/pitstopai.com.br/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pitstopai.com.br/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=63072000" always;

    access_log /var/log/nginx/whatsapp_access.log;
    error_log /var/log/nginx/whatsapp_error.log;

    client_max_body_size 100M;

    location / {
        proxy_pass http://127.0.0.1:8021;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }
}
```

**Após editar, validar e recarregar:**
```bash
sudo nginx -t && sudo systemctl reload nginx
```

## 5. Estrutura de Diretórios na VPS

```
/opt/pitstop/
├── .env                      # Variáveis de ambiente (criado pelo deploy)
├── docker-compose.prod.yml   # Docker Compose (copiado pelo deploy)
├── backups/                  # Backups automáticos do banco
│   └── pitstop_pre_deploy_*.sql.gz
├── logs/                     # Logs da aplicação
└── landing/                  # Landing page estática (opcional)
    └── index.html
```

## 6. Comandos Úteis na VPS

### Status dos containers
```bash
cd /opt/pitstop
docker compose -f docker-compose.prod.yml ps
```

### Logs em tempo real
```bash
# Backend
docker logs -f pitstop-backend

# Frontend
docker logs -f pitstop-frontend

# Todos
docker compose -f docker-compose.prod.yml logs -f
```

### Reiniciar serviços
```bash
cd /opt/pitstop
docker compose -f docker-compose.prod.yml restart pitstop-backend
docker compose -f docker-compose.prod.yml restart pitstop-frontend
```

### Backup manual do banco
```bash
docker exec pitstop-postgres pg_dump -U pitstop pitstop_db | gzip > /opt/pitstop/backups/manual_$(date +%Y%m%d_%H%M%S).sql.gz
```

### Restaurar backup
```bash
gunzip -c /opt/pitstop/backups/ARQUIVO.sql.gz | docker exec -i pitstop-postgres psql -U pitstop pitstop_db
```

### Verificar uso de disco
```bash
docker system df
df -h
```

### Limpar imagens antigas
```bash
docker image prune -af
```

## 7. Workflow de Deploy Automático

O deploy é acionado automaticamente quando há push na branch `main`.

### Fluxo:
1. **Build**: Compila backend (Maven) e frontend (Vite)
2. **Push**: Envia imagens para GitHub Container Registry
3. **Backup**: Cria backup do banco antes de atualizar
4. **Deploy**: Para containers, remove imagens antigas, baixa novas, inicia
5. **Health Check**: Verifica se aplicação está saudável
6. **Rollback**: Se falhar, reverte para versão anterior

### Deploy manual via GitHub:
1. Acesse: `Actions > Deploy PitStop > Run workflow`
2. Selecione branch `main`
3. Clique em "Run workflow"

## 8. Troubleshooting

### Frontend não atualiza após deploy

**Causa**: Imagem em cache no servidor.

**Solução**: O workflow já foi corrigido para:
1. Parar containers
2. Remover imagens antigas (`docker rmi`)
3. Forçar pull de novas imagens
4. Recriar containers

### WebSocket retorna 403 Forbidden

**Causa**: CORS não configurado ou Nginx incorreto.

**Verificar**:
1. `CORS_ALLOWED_ORIGINS` inclui o domínio do frontend
2. Nginx do HOST tem `proxy_pass http://127.0.0.1:8080;` (sem path)
3. WebSocketConfig.java usa mesma variável CORS

### Login demora/timeout

**Causa**: Containers não iniciaram corretamente.

**Verificar**:
```bash
docker ps  # Todos devem estar "Up"
docker logs pitstop-backend --tail 50  # Verificar erros
```

### Erro de conexão com banco

**Verificar**:
```bash
docker exec pitstop-postgres pg_isready -U pitstop
docker logs pitstop-postgres --tail 20
```

### Certificado SSL expirado

**Renovar**:
```bash
sudo certbot renew
sudo systemctl reload nginx
```

## 9. Checklist de Primeiro Deploy

- [ ] DNS configurado para todos os subdomínios
- [ ] VPS com Docker e Docker Compose instalados
- [ ] Nginx instalado e configurado
- [ ] Certificado SSL gerado (Let's Encrypt)
- [ ] Todos os GitHub Secrets configurados
- [ ] Volumes Docker criados:
  ```bash
  docker volume create pitstop_postgres_data
  docker volume create pitstop_redis_data
  ```
- [ ] Diretórios criados:
  ```bash
  mkdir -p /opt/pitstop/backups /opt/pitstop/logs
  ```
- [ ] Deploy executado com sucesso
- [ ] Health check passou
- [ ] Login funcionando
- [ ] WebSocket conectando

## 10. Contatos e Recursos

- **Repositório**: https://github.com/jefersontramonti/gerenciador-oficina-mecanica
- **Actions**: https://github.com/jefersontramonti/gerenciador-oficina-mecanica/actions
- **Documentação API**: https://api.pitstopai.com.br/swagger-ui.html

---

**Última atualização**: 2025-01-11
**Versão**: 1.0.0
