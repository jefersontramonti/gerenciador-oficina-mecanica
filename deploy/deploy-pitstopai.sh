#!/bin/bash
#===============================================================================
# PitStop AI - Deploy Script Personalizado
#
# Domínios:
#   pitstopai.com.br         → Landing Page
#   app.pitstopai.com.br     → Frontend React
#   api.pitstopai.com.br     → Backend Spring Boot
#   whatsapp.pitstopai.com.br → Evolution API
#
# VPS: Contabo 12GB RAM - 86.48.22.177
#===============================================================================

set -e

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configurações fixas
DOMAIN_MAIN="pitstopai.com.br"
DOMAIN_APP="app.pitstopai.com.br"
DOMAIN_API="api.pitstopai.com.br"
DOMAIN_WHATSAPP="whatsapp.pitstopai.com.br"
VPS_IP="86.48.22.177"

echo -e "${CYAN}"
echo "╔═══════════════════════════════════════════════════════════════════╗"
echo "║          🔧 PitStop AI - Deploy Personalizado                    ║"
echo "╠═══════════════════════════════════════════════════════════════════╣"
echo "║  pitstopai.com.br         → Landing Page                         ║"
echo "║  app.pitstopai.com.br     → Frontend React                       ║"
echo "║  api.pitstopai.com.br     → Backend API                          ║"
echo "║  whatsapp.pitstopai.com.br → Evolution API                       ║"
echo "╚═══════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Verificar root
if [[ $EUID -ne 0 ]]; then
    echo -e "${RED}Execute como root: sudo bash deploy-pitstopai.sh${NC}"
    exit 1
fi

# Verificar Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker não encontrado!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker: $(docker --version | cut -d' ' -f3)${NC}"

#===============================================================================
# COLETAR INFORMAÇÕES
#===============================================================================

echo ""
echo -e "${CYAN}═══ CONFIGURAÇÕES ═══${NC}"
echo ""

read -p "Email para SSL Let's Encrypt: " SSL_EMAIL
while [[ -z "$SSL_EMAIL" ]]; do
    read -p "Email é obrigatório: " SSL_EMAIL
done

echo ""
echo -e "${CYAN}═══ DADOS DA OFICINA ═══${NC}"
read -p "Nome da oficina [PitStop AI]: " OFICINA_NOME
OFICINA_NOME="${OFICINA_NOME:-PitStop AI}"

read -p "CNPJ [00.000.000/0001-00]: " OFICINA_CNPJ
OFICINA_CNPJ="${OFICINA_CNPJ:-00.000.000/0001-00}"

read -p "Telefone [(11) 99999-9999]: " OFICINA_TELEFONE
OFICINA_TELEFONE="${OFICINA_TELEFONE:-(11) 99999-9999}"

read -p "Email da oficina [contato@pitstopai.com.br]: " OFICINA_EMAIL
OFICINA_EMAIL="${OFICINA_EMAIL:-contato@pitstopai.com.br}"

#===============================================================================
# GERAR SENHAS
#===============================================================================

echo ""
echo -e "${BLUE}[1/8]${NC} Gerando senhas seguras..."

POSTGRES_PITSTOP_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
POSTGRES_EVOLUTION_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
REDIS_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
JWT_SECRET=$(openssl rand -base64 64 | tr -dc 'a-zA-Z0-9' | head -c 64)
EVOLUTION_API_KEY=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)

echo -e "${GREEN}✓${NC} Senhas geradas"

#===============================================================================
# CRIAR DIRETÓRIOS
#===============================================================================

echo -e "${BLUE}[2/8]${NC} Criando diretórios..."

mkdir -p /opt/pitstop/{backend,frontend,landing,nginx,ssl,logs,backups,scripts}
mkdir -p /opt/pitstop/data/{postgres-pitstop,postgres-evolution,redis}

echo -e "${GREEN}✓${NC} Diretórios criados"

#===============================================================================
# CRIAR .env
#===============================================================================

echo -e "${BLUE}[3/8]${NC} Criando arquivo .env..."

cat > /opt/pitstop/.env << EOF
#===============================================================================
# PitStop AI - Variáveis de Ambiente
# Gerado em: $(date)
# ⚠️  MANTENHA ESTE ARQUIVO SEGURO!
#===============================================================================

#--- DOMÍNIOS ---
DOMAIN_MAIN=${DOMAIN_MAIN}
DOMAIN_APP=${DOMAIN_APP}
DOMAIN_API=${DOMAIN_API}
DOMAIN_WHATSAPP=${DOMAIN_WHATSAPP}
SSL_EMAIL=${SSL_EMAIL}

#--- URLs DA APLICAÇÃO ---
APP_FRONTEND_URL=https://${DOMAIN_APP}
APP_BASE_URL=https://${DOMAIN_API}
CORS_ALLOWED_ORIGINS=https://${DOMAIN_APP},https://${DOMAIN_MAIN}

#--- POSTGRESQL PITSTOP ---
POSTGRES_PITSTOP_HOST=postgres-pitstop
POSTGRES_PITSTOP_PORT=5432
POSTGRES_PITSTOP_DB=pitstop_db
POSTGRES_PITSTOP_USER=pitstop
POSTGRES_PITSTOP_PASSWORD=${POSTGRES_PITSTOP_PASSWORD}
DATABASE_URL=jdbc:postgresql://postgres-pitstop:5432/pitstop_db
DATABASE_USERNAME=pitstop
DATABASE_PASSWORD=${POSTGRES_PITSTOP_PASSWORD}

#--- POSTGRESQL EVOLUTION ---
POSTGRES_EVOLUTION_HOST=postgres-evolution
POSTGRES_EVOLUTION_PORT=5433
POSTGRES_EVOLUTION_DB=evolution_db
POSTGRES_EVOLUTION_USER=evolution
POSTGRES_EVOLUTION_PASSWORD=${POSTGRES_EVOLUTION_PASSWORD}

#--- REDIS ---
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}

#--- JWT ---
JWT_SECRET=${JWT_SECRET}

#--- EVOLUTION API ---
EVOLUTION_API_URL=https://${DOMAIN_WHATSAPP}
EVOLUTION_API_KEY=${EVOLUTION_API_KEY}
AUTHENTICATION_API_KEY=${EVOLUTION_API_KEY}

#--- OFICINA ---
PITSTOP_OFICINA_NOME="${OFICINA_NOME}"
PITSTOP_OFICINA_CNPJ="${OFICINA_CNPJ}"
PITSTOP_OFICINA_TELEFONE="${OFICINA_TELEFONE}"
PITSTOP_OFICINA_EMAIL="${OFICINA_EMAIL}"

#--- SPRING ---
SPRING_PROFILES_ACTIVE=prod
LOG_LEVEL=INFO

#--- TELEGRAM (configurar depois) ---
#TELEGRAM_BOT_TOKEN=
#TELEGRAM_CHAT_ID=

#--- EMAIL SMTP (configurar depois) ---
#MAIL_HOST=smtp.gmail.com
#MAIL_PORT=587
#MAIL_USERNAME=
#MAIL_PASSWORD=
#MAIL_FROM_NAME=PitStop AI

#--- MERCADO PAGO (configurar depois) ---
#MERCADOPAGO_ACCESS_TOKEN=
#MERCADOPAGO_PUBLIC_KEY=

#--- ANTHROPIC IA (configurar depois) ---
#ANTHROPIC_API_KEY=
EOF

chmod 600 /opt/pitstop/.env
echo -e "${GREEN}✓${NC} .env criado"

#===============================================================================
# CRIAR DOCKER-COMPOSE
#===============================================================================

echo -e "${BLUE}[4/8]${NC} Criando docker-compose.yml..."

cat > /opt/pitstop/docker-compose.yml << 'DOCKEREOF'
#===============================================================================
# PitStop AI - Docker Compose
#===============================================================================

services:
  #--- POSTGRESQL PITSTOP ---
  postgres-pitstop:
    image: postgres:16-alpine
    container_name: pitstop-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_PITSTOP_DB}
      POSTGRES_USER: ${POSTGRES_PITSTOP_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PITSTOP_PASSWORD}
      PGDATA: /var/lib/postgresql/data/pgdata
    volumes:
      - /opt/pitstop/data/postgres-pitstop:/var/lib/postgresql/data
    ports:
      - "127.0.0.1:5432:5432"
    networks:
      - pitstop-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_PITSTOP_USER} -d ${POSTGRES_PITSTOP_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  #--- POSTGRESQL EVOLUTION ---
  postgres-evolution:
    image: postgres:16-alpine
    container_name: evolution-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_EVOLUTION_DB}
      POSTGRES_USER: ${POSTGRES_EVOLUTION_USER}
      POSTGRES_PASSWORD: ${POSTGRES_EVOLUTION_PASSWORD}
      PGDATA: /var/lib/postgresql/data/pgdata
    volumes:
      - /opt/pitstop/data/postgres-evolution:/var/lib/postgresql/data
    ports:
      - "127.0.0.1:5433:5432"
    networks:
      - pitstop-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_EVOLUTION_USER} -d ${POSTGRES_EVOLUTION_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  #--- REDIS ---
  redis:
    image: redis:7-alpine
    container_name: pitstop-redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
    volumes:
      - /opt/pitstop/data/redis:/data
    ports:
      - "127.0.0.1:6379:6379"
    networks:
      - pitstop-network
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  #--- PITSTOP BACKEND ---
  pitstop-backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    image: pitstop-backend:latest
    container_name: pitstop-backend
    restart: unless-stopped
    environment:
      DATABASE_URL: ${DATABASE_URL}
      DATABASE_USERNAME: ${DATABASE_USERNAME}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      APP_FRONTEND_URL: ${APP_FRONTEND_URL}
      APP_BASE_URL: ${APP_BASE_URL}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      PITSTOP_OFICINA_NOME: ${PITSTOP_OFICINA_NOME}
      PITSTOP_OFICINA_CNPJ: ${PITSTOP_OFICINA_CNPJ}
      PITSTOP_OFICINA_TELEFONE: ${PITSTOP_OFICINA_TELEFONE}
      PITSTOP_OFICINA_EMAIL: ${PITSTOP_OFICINA_EMAIL}
      EVOLUTION_API_URL: ${EVOLUTION_API_URL}
      EVOLUTION_API_KEY: ${EVOLUTION_API_KEY}
      JAVA_OPTS: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Duser.timezone=America/Sao_Paulo"
    ports:
      - "127.0.0.1:8080:8080"
    networks:
      - pitstop-network
    depends_on:
      postgres-pitstop:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 90s
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G

  #--- PITSTOP FRONTEND ---
  pitstop-frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      args:
        VITE_API_URL: https://${DOMAIN_API}/api
    image: pitstop-frontend:latest
    container_name: pitstop-frontend
    restart: unless-stopped
    ports:
      - "127.0.0.1:3000:80"
    networks:
      - pitstop-network
    depends_on:
      - pitstop-backend
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost/health"]
      interval: 30s
      timeout: 5s
      retries: 3
    deploy:
      resources:
        limits:
          memory: 256M

  #--- EVOLUTION API ---
  evolution-api:
    image: atendai/evolution-api:v2.2.3
    container_name: evolution-api
    restart: unless-stopped
    environment:
      SERVER_URL: https://${DOMAIN_WHATSAPP}
      DATABASE_ENABLED: "true"
      DATABASE_PROVIDER: postgresql
      DATABASE_CONNECTION_URI: postgresql://${POSTGRES_EVOLUTION_USER}:${POSTGRES_EVOLUTION_PASSWORD}@postgres-evolution:5432/${POSTGRES_EVOLUTION_DB}?schema=public
      DATABASE_CONNECTION_CLIENT_NAME: evolution_v2
      DATABASE_SAVE_DATA_INSTANCE: "true"
      DATABASE_SAVE_DATA_NEW_MESSAGE: "true"
      DATABASE_SAVE_MESSAGE_UPDATE: "true"
      DATABASE_SAVE_DATA_CONTACTS: "true"
      DATABASE_SAVE_DATA_CHATS: "true"
      DATABASE_SAVE_DATA_LABELS: "true"
      DATABASE_SAVE_DATA_HISTORIC: "true"
      CACHE_REDIS_ENABLED: "true"
      CACHE_REDIS_URI: redis://:${REDIS_PASSWORD}@redis:6379/1
      CACHE_REDIS_PREFIX_KEY: evolution
      CACHE_REDIS_SAVE_INSTANCES: "true"
      CACHE_LOCAL_ENABLED: "false"
      AUTHENTICATION_TYPE: apikey
      AUTHENTICATION_API_KEY: ${AUTHENTICATION_API_KEY}
      AUTHENTICATION_EXPOSE_IN_FETCH_INSTANCES: "true"
      DEL_INSTANCE: "false"
      DEL_TEMP_INSTANCES: "true"
      WEBHOOK_GLOBAL_ENABLED: "false"
      RABBITMQ_ENABLED: "false"
      SQS_ENABLED: "false"
      WEBSOCKET_ENABLED: "true"
      WEBSOCKET_GLOBAL_EVENTS: "false"
      CHATWOOT_ENABLED: "false"
      OPENAI_ENABLED: "false"
      DIFY_ENABLED: "false"
      TYPEBOT_ENABLED: "false"
      S3_ENABLED: "false"
      LOG_LEVEL: ERROR
      LOG_COLOR: "true"
      LOG_BAILEYS: error
      CORS_ORIGIN: "*"
      CORS_METHODS: "POST,GET,PUT,DELETE"
      CORS_CREDENTIALS: "true"
      QRCODE_LIMIT: 30
      QRCODE_COLOR: "#198754"
      TELEMETRY_ENABLED: "false"
    ports:
      - "127.0.0.1:8021:8080"
    networks:
      - pitstop-network
    depends_on:
      postgres-evolution:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M

networks:
  pitstop-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
DOCKEREOF

echo -e "${GREEN}✓${NC} docker-compose.yml criado"

#===============================================================================
# INSTALAR NGINX E CERTBOT
#===============================================================================

echo -e "${BLUE}[5/8]${NC} Instalando Nginx e Certbot..."

apt install -y nginx certbot python3-certbot-nginx > /dev/null 2>&1 || {
    apt update && apt install -y nginx certbot python3-certbot-nginx
}

echo -e "${GREEN}✓${NC} Nginx e Certbot instalados"

#===============================================================================
# CRIAR CONFIGURAÇÃO NGINX
#===============================================================================

echo -e "${BLUE}[6/8]${NC} Configurando Nginx..."

cat > /etc/nginx/sites-available/pitstopai << 'NGINXEOF'
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

    # Cache para assets
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

    # Rate limit para API geral
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
    location /auth/login {
        limit_req zone=login_limit burst=3 nodelay;

        proxy_pass http://127.0.0.1:8080/auth/login;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket
    location /ws {
        proxy_pass http://127.0.0.1:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 86400;
    }

    # Actuator (interno)
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
NGINXEOF

# Habilitar site
ln -sf /etc/nginx/sites-available/pitstopai /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

echo -e "${GREEN}✓${NC} Nginx configurado"

#===============================================================================
# CRIAR SCRIPTS DE MANUTENÇÃO
#===============================================================================

echo -e "${BLUE}[7/8]${NC} Criando scripts de manutenção..."

# Status
cat > /opt/pitstop/scripts/status.sh << 'EOF'
#!/bin/bash
echo "═══════════════════════════════════════════"
echo "🔧 PitStop AI - Status"
echo "═══════════════════════════════════════════"
echo ""
echo "🐳 Containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "Docker não disponível"
echo ""
echo "💾 Disco: $(df -h / | tail -1 | awk '{print $4}') livres"
echo "🧠 RAM: $(free -h | grep Mem | awk '{print $3}') / $(free -h | grep Mem | awk '{print $2}')"
echo ""
echo "📊 Health Checks:"
curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP" && echo "  ✅ Backend (api.pitstopai.com.br)" || echo "  ❌ Backend"
curl -s http://localhost:3000/health 2>/dev/null && echo "  ✅ Frontend (app.pitstopai.com.br)" || echo "  ❌ Frontend"
curl -s http://localhost:8021/ 2>/dev/null && echo "  ✅ Evolution (whatsapp.pitstopai.com.br)" || echo "  ❌ Evolution"
echo ""
echo "🌐 URLs:"
echo "  https://pitstopai.com.br"
echo "  https://app.pitstopai.com.br"
echo "  https://api.pitstopai.com.br"
echo "  https://whatsapp.pitstopai.com.br"
EOF

# Logs
cat > /opt/pitstop/scripts/logs.sh << 'EOF'
#!/bin/bash
case "$1" in
    backend) docker logs -f pitstop-backend --tail 100 ;;
    frontend) docker logs -f pitstop-frontend --tail 100 ;;
    evolution) docker logs -f evolution-api --tail 100 ;;
    postgres) docker logs -f pitstop-postgres --tail 100 ;;
    redis) docker logs -f pitstop-redis --tail 100 ;;
    nginx) tail -f /var/log/nginx/*pitstop*.log /var/log/nginx/*app*.log /var/log/nginx/*api*.log ;;
    all) docker compose -f /opt/pitstop/docker-compose.yml logs -f --tail 50 ;;
    *) echo "Uso: $0 {backend|frontend|evolution|postgres|redis|nginx|all}" ;;
esac
EOF

# Backup
cat > /opt/pitstop/scripts/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/pitstop/backups"
DATE=$(date +%Y%m%d_%H%M%S)
source /opt/pitstop/.env

echo "📦 Backup PitStop AI - $DATE"
docker exec pitstop-postgres pg_dump -U $POSTGRES_PITSTOP_USER $POSTGRES_PITSTOP_DB | gzip > "$BACKUP_DIR/pitstop_db_$DATE.sql.gz"
docker exec evolution-postgres pg_dump -U $POSTGRES_EVOLUTION_USER $POSTGRES_EVOLUTION_DB | gzip > "$BACKUP_DIR/evolution_db_$DATE.sql.gz"
cp /opt/pitstop/.env "$BACKUP_DIR/env_$DATE.backup"
find "$BACKUP_DIR" -type f -mtime +7 -delete
echo "✅ Backup: $BACKUP_DIR"
ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null | tail -5
EOF

# Restart
cat > /opt/pitstop/scripts/restart.sh << 'EOF'
#!/bin/bash
cd /opt/pitstop
case "$1" in
    all) docker compose restart ;;
    backend) docker restart pitstop-backend ;;
    frontend) docker restart pitstop-frontend ;;
    evolution) docker restart evolution-api ;;
    nginx) systemctl restart nginx ;;
    *) echo "Uso: $0 {all|backend|frontend|evolution|nginx}" ;;
esac
EOF

# Deploy
cat > /opt/pitstop/scripts/deploy.sh << 'EOF'
#!/bin/bash
set -e
cd /opt/pitstop
echo "📦 Deploy PitStop AI"
echo "1/4 Backup..."
/opt/pitstop/scripts/backup.sh
echo "2/4 Parando..."
docker compose down
echo "3/4 Rebuild..."
docker compose build --no-cache
echo "4/4 Iniciando..."
docker compose up -d
sleep 30
/opt/pitstop/scripts/status.sh
EOF

# SSL Renew
cat > /opt/pitstop/scripts/ssl-setup.sh << 'EOF'
#!/bin/bash
echo "🔒 Configurando SSL para PitStop AI..."

# Criar diretório certbot
mkdir -p /var/www/certbot

# Config temporária para obter certificados
cat > /etc/nginx/sites-available/pitstopai-temp << 'TEMPEOF'
server {
    listen 80;
    server_name pitstopai.com.br www.pitstopai.com.br app.pitstopai.com.br api.pitstopai.com.br whatsapp.pitstopai.com.br;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 200 'OK';
    }
}
TEMPEOF

ln -sf /etc/nginx/sites-available/pitstopai-temp /etc/nginx/sites-enabled/pitstopai
systemctl reload nginx

# Obter certificados
source /opt/pitstop/.env
certbot certonly --webroot \
    -w /var/www/certbot \
    -d pitstopai.com.br \
    -d www.pitstopai.com.br \
    -d app.pitstopai.com.br \
    -d api.pitstopai.com.br \
    -d whatsapp.pitstopai.com.br \
    --email $SSL_EMAIL \
    --agree-tos \
    --non-interactive

# Restaurar config completa
ln -sf /etc/nginx/sites-available/pitstopai /etc/nginx/sites-enabled/pitstopai
rm -f /etc/nginx/sites-available/pitstopai-temp

nginx -t && systemctl reload nginx

echo "✅ SSL configurado!"
EOF

chmod +x /opt/pitstop/scripts/*.sh

# Cron backup
echo "0 3 * * * root /opt/pitstop/scripts/backup.sh >> /var/log/pitstop-backup.log 2>&1" > /etc/cron.d/pitstop-backup

# Cron SSL renew
echo "0 12 * * * root certbot renew --quiet --post-hook 'systemctl reload nginx'" > /etc/cron.d/pitstop-ssl

# Aliases
cat >> /root/.bashrc << 'ALIASEOF'

# PitStop AI Aliases
alias ps-status='/opt/pitstop/scripts/status.sh'
alias ps-logs='/opt/pitstop/scripts/logs.sh'
alias ps-restart='/opt/pitstop/scripts/restart.sh'
alias ps-backup='/opt/pitstop/scripts/backup.sh'
alias ps-deploy='/opt/pitstop/scripts/deploy.sh'
alias ps-ssl='/opt/pitstop/scripts/ssl-setup.sh'
alias ps-cd='cd /opt/pitstop'
ALIASEOF

echo -e "${GREEN}✓${NC} Scripts criados"

#===============================================================================
# CRIAR LANDING PAGE PLACEHOLDER
#===============================================================================

echo -e "${BLUE}[8/8]${NC} Criando landing page placeholder..."

cat > /opt/pitstop/landing/index.html << 'LANDINGEOF'
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PitStop AI - Sistema de Gestão para Oficinas</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
        }
        .container {
            text-align: center;
            padding: 40px;
        }
        .logo {
            font-size: 4rem;
            margin-bottom: 20px;
        }
        h1 {
            font-size: 3rem;
            margin-bottom: 10px;
            background: linear-gradient(90deg, #00d4ff, #7b2cbf);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .tagline {
            font-size: 1.5rem;
            color: #a0a0a0;
            margin-bottom: 40px;
        }
        .btn {
            display: inline-block;
            padding: 15px 40px;
            font-size: 1.2rem;
            color: #fff;
            background: linear-gradient(90deg, #00d4ff, #7b2cbf);
            border: none;
            border-radius: 50px;
            text-decoration: none;
            transition: transform 0.3s, box-shadow 0.3s;
        }
        .btn:hover {
            transform: translateY(-3px);
            box-shadow: 0 10px 30px rgba(0, 212, 255, 0.3);
        }
        .features {
            display: flex;
            gap: 30px;
            margin-top: 60px;
            flex-wrap: wrap;
            justify-content: center;
        }
        .feature {
            background: rgba(255,255,255,0.05);
            padding: 30px;
            border-radius: 15px;
            width: 250px;
        }
        .feature-icon { font-size: 2.5rem; margin-bottom: 15px; }
        .feature h3 { margin-bottom: 10px; }
        .feature p { color: #a0a0a0; font-size: 0.9rem; }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">🔧</div>
        <h1>PitStop AI</h1>
        <p class="tagline">Sistema Inteligente de Gestão para Oficinas Mecânicas</p>
        <a href="https://app.pitstopai.com.br" class="btn">Acessar Sistema</a>

        <div class="features">
            <div class="feature">
                <div class="feature-icon">📋</div>
                <h3>Ordens de Serviço</h3>
                <p>Gerencie OS do orçamento à entrega</p>
            </div>
            <div class="feature">
                <div class="feature-icon">📦</div>
                <h3>Estoque</h3>
                <p>Controle peças e movimentações</p>
            </div>
            <div class="feature">
                <div class="feature-icon">💬</div>
                <h3>WhatsApp</h3>
                <p>Notificações automáticas</p>
            </div>
            <div class="feature">
                <div class="feature-icon">🤖</div>
                <h3>IA Diagnóstica</h3>
                <p>Sugestões inteligentes</p>
            </div>
        </div>
    </div>
</body>
</html>
LANDINGEOF

echo -e "${GREEN}✓${NC} Landing page criada"

#===============================================================================
# RESUMO FINAL
#===============================================================================

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           🎉 CONFIGURAÇÃO CONCLUÍDA COM SUCESSO!                  ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}═══ SENHAS GERADAS (SALVE EM LOCAL SEGURO!) ═══${NC}"
echo ""
echo -e "PostgreSQL PitStop:   ${CYAN}${POSTGRES_PITSTOP_PASSWORD}${NC}"
echo -e "PostgreSQL Evolution: ${CYAN}${POSTGRES_EVOLUTION_PASSWORD}${NC}"
echo -e "Redis:                ${CYAN}${REDIS_PASSWORD}${NC}"
echo -e "JWT Secret:           ${CYAN}${JWT_SECRET:0:30}...${NC}"
echo -e "Evolution API Key:    ${CYAN}${EVOLUTION_API_KEY}${NC}"
echo ""
echo -e "${YELLOW}═══ PRÓXIMOS PASSOS ═══${NC}"
echo ""
echo -e "${CYAN}PASSO 1: Verificar DNS (aguarde propagação ~5min)${NC}"
echo "  dig +short ${DOMAIN_MAIN}"
echo "  dig +short ${DOMAIN_APP}"
echo "  dig +short ${DOMAIN_API}"
echo "  dig +short ${DOMAIN_WHATSAPP}"
echo ""
echo -e "${CYAN}PASSO 2: Configurar SSL${NC}"
echo "  ${YELLOW}/opt/pitstop/scripts/ssl-setup.sh${NC}"
echo ""
echo -e "${CYAN}PASSO 3: Copiar código fonte (do seu PC)${NC}"
echo "  ${YELLOW}# Backend:${NC}"
echo "  scp -r src pom.xml Dockerfile root@${VPS_IP}:/opt/pitstop/backend/"
echo ""
echo "  ${YELLOW}# Frontend:${NC}"
echo "  scp -r frontend/* root@${VPS_IP}:/opt/pitstop/frontend/"
echo ""
echo -e "${CYAN}PASSO 4: Iniciar serviços${NC}"
echo "  ${YELLOW}cd /opt/pitstop && docker compose up -d --build${NC}"
echo ""
echo -e "${CYAN}PASSO 5: Verificar status${NC}"
echo "  ${YELLOW}ps-status${NC}"
echo ""
echo -e "${GREEN}═══ URLs FINAIS ═══${NC}"
echo ""
echo "  🏠 Landing:  https://${DOMAIN_MAIN}"
echo "  📱 App:      https://${DOMAIN_APP}"
echo "  🔌 API:      https://${DOMAIN_API}"
echo "  💬 WhatsApp: https://${DOMAIN_WHATSAPP}"
echo "  📚 Swagger:  https://${DOMAIN_API}/swagger-ui.html"
echo ""
echo -e "${GREEN}═══ COMANDOS ÚTEIS ═══${NC}"
echo ""
echo "  ps-status   → Ver status dos serviços"
echo "  ps-logs     → Ver logs (backend|frontend|evolution|all)"
echo "  ps-restart  → Reiniciar serviços"
echo "  ps-backup   → Fazer backup manual"
echo "  ps-deploy   → Deploy completo"
echo "  ps-ssl      → Configurar/renovar SSL"
echo "  ps-cd       → Ir para /opt/pitstop"
echo ""
echo -e "${GREEN}✅ Configuração salva em: /opt/pitstop/.env${NC}"
echo ""
