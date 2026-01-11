#!/bin/bash
#===============================================================================
# PitStop + Evolution API - Deploy Script
# Para VPS já configurada com Docker, UFW, Fail2Ban
# Contabo 12GB RAM - Ubuntu 24.04
#===============================================================================

set -e

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Banner
echo -e "${CYAN}"
echo "╔═══════════════════════════════════════════════════════════════════╗"
echo "║        🔧 PitStop + Evolution API - Deploy                       ║"
echo "║        VPS Contabo 12GB RAM - Ubuntu 24.04                       ║"
echo "╚═══════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Verificar root
if [[ $EUID -ne 0 ]]; then
    echo -e "${RED}Execute como root: sudo bash deploy-pitstop.sh${NC}"
    exit 1
fi

# Verificar Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker não encontrado! Instale primeiro.${NC}"
    exit 1
fi

echo -e "${GREEN}Docker encontrado: $(docker --version)${NC}"
echo ""

#===============================================================================
# CONFIGURAÇÃO INTERATIVA
#===============================================================================

echo -e "${CYAN}═══ CONFIGURAÇÃO DO AMBIENTE ═══${NC}"
echo ""

# Domínio principal
read -p "Domínio principal do PitStop (ex: pitstop.empresa.com): " DOMAIN
while [[ -z "$DOMAIN" ]]; do
    read -p "Domínio é obrigatório: " DOMAIN
done

# Domínio Evolution
read -p "Domínio do WhatsApp/Evolution (ex: whatsapp.empresa.com): " EVOLUTION_DOMAIN
while [[ -z "$EVOLUTION_DOMAIN" ]]; do
    read -p "Domínio Evolution é obrigatório: " EVOLUTION_DOMAIN
done

# Email SSL
read -p "Email para certificado SSL (Let's Encrypt): " SSL_EMAIL
while [[ -z "$SSL_EMAIL" ]]; do
    read -p "Email é obrigatório: " SSL_EMAIL
done

echo ""
echo -e "${CYAN}═══ DADOS DA OFICINA ═══${NC}"
read -p "Nome da oficina: " OFICINA_NOME
OFICINA_NOME="${OFICINA_NOME:-PitStop Auto Center}"

read -p "CNPJ (formato: 00.000.000/0001-00): " OFICINA_CNPJ
OFICINA_CNPJ="${OFICINA_CNPJ:-00.000.000/0001-00}"

read -p "Telefone: " OFICINA_TELEFONE
OFICINA_TELEFONE="${OFICINA_TELEFONE:-(11) 99999-9999}"

read -p "Email da oficina: " OFICINA_EMAIL
OFICINA_EMAIL="${OFICINA_EMAIL:-contato@${DOMAIN}}"

#===============================================================================
# GERAR SENHAS SEGURAS
#===============================================================================

echo ""
echo -e "${BLUE}[INFO]${NC} Gerando senhas seguras..."

POSTGRES_PITSTOP_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
POSTGRES_EVOLUTION_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
REDIS_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
JWT_SECRET=$(openssl rand -base64 64 | tr -dc 'a-zA-Z0-9' | head -c 64)
EVOLUTION_API_KEY=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)

echo -e "${GREEN}[OK]${NC} Senhas geradas"

#===============================================================================
# CRIAR ESTRUTURA DE DIRETÓRIOS
#===============================================================================

echo -e "${BLUE}[INFO]${NC} Criando estrutura de diretórios..."

mkdir -p /opt/pitstop/{backend,frontend,nginx,ssl,logs,backups,scripts}
mkdir -p /opt/pitstop/data/{postgres-pitstop,postgres-evolution,redis}

# Ajustar permissões
chown -R pitstop:pitstop /opt/pitstop 2>/dev/null || true

echo -e "${GREEN}[OK]${NC} Diretórios criados"

#===============================================================================
# CRIAR ARQUIVO .env
#===============================================================================

echo -e "${BLUE}[INFO]${NC} Criando arquivo de configuração..."

cat > /opt/pitstop/.env << EOF
#===============================================================================
# PitStop + Evolution API - Variáveis de Ambiente
# Gerado em: $(date)
# ⚠️  MANTENHA ESTE ARQUIVO SEGURO!
#===============================================================================

#--- DOMÍNIOS ---
DOMAIN=${DOMAIN}
EVOLUTION_DOMAIN=${EVOLUTION_DOMAIN}
SSL_EMAIL=${SSL_EMAIL}

#--- URLs ---
APP_FRONTEND_URL=https://${DOMAIN}
APP_BASE_URL=https://${DOMAIN}/api
CORS_ALLOWED_ORIGINS=https://${DOMAIN}

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
EVOLUTION_API_URL=https://${EVOLUTION_DOMAIN}
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
#MAIL_FROM_NAME=PitStop

#--- MERCADO PAGO (configurar depois) ---
#MERCADOPAGO_ACCESS_TOKEN=
#MERCADOPAGO_PUBLIC_KEY=

#--- ANTHROPIC IA (configurar depois) ---
#ANTHROPIC_API_KEY=
EOF

chmod 600 /opt/pitstop/.env
echo -e "${GREEN}[OK]${NC} Arquivo .env criado"

#===============================================================================
# CRIAR DOCKER-COMPOSE.YML
#===============================================================================

echo -e "${BLUE}[INFO]${NC} Criando docker-compose.yml..."

cat > /opt/pitstop/docker-compose.yml << 'DOCKEREOF'
#===============================================================================
# PitStop + Evolution API - Docker Compose
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
        VITE_API_URL: /api
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

  #--- EVOLUTION API V2 ---
  evolution-api:
    image: atendai/evolution-api:v2.2.3
    container_name: evolution-api
    restart: unless-stopped
    environment:
      SERVER_URL: https://${EVOLUTION_DOMAIN}
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

echo -e "${GREEN}[OK]${NC} docker-compose.yml criado"

#===============================================================================
# CRIAR CONFIGURAÇÃO NGINX
#===============================================================================

echo -e "${BLUE}[INFO]${NC} Criando configuração Nginx..."

# Instalar Nginx e Certbot
apt install -y nginx certbot python3-certbot-nginx > /dev/null 2>&1

# Criar config Nginx
cat > /etc/nginx/sites-available/pitstop << NGINXEOF
# Rate limiting
limit_req_zone \$binary_remote_addr zone=api_limit:10m rate=10r/s;
limit_req_zone \$binary_remote_addr zone=login_limit:10m rate=1r/s;

# HTTP -> HTTPS redirect
server {
    listen 80;
    listen [::]:80;
    server_name ${DOMAIN} ${EVOLUTION_DOMAIN};

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

# PitStop Main
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name ${DOMAIN};

    ssl_certificate /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:50m;
    ssl_protocols TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=63072000" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml;

    access_log /var/log/nginx/pitstop_access.log;
    error_log /var/log/nginx/pitstop_error.log;

    client_max_body_size 50M;

    # API Backend
    location /api/ {
        limit_req zone=api_limit burst=20 nodelay;
        proxy_pass http://127.0.0.1:8080/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Connection "";
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Login rate limiting
    location /api/auth/login {
        limit_req zone=login_limit burst=3 nodelay;
        proxy_pass http://127.0.0.1:8080/auth/login;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # WebSocket
    location /ws {
        proxy_pass http://127.0.0.1:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 86400;
    }

    # Actuator (internal only)
    location /actuator {
        allow 127.0.0.1;
        deny all;
        proxy_pass http://127.0.0.1:8080/actuator;
    }

    # Swagger
    location /swagger-ui {
        proxy_pass http://127.0.0.1:8080/swagger-ui;
        proxy_set_header Host \$host;
    }

    location /v3/api-docs {
        proxy_pass http://127.0.0.1:8080/v3/api-docs;
    }

    # Frontend
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}

# Evolution API
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name ${EVOLUTION_DOMAIN};

    ssl_certificate /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    add_header Strict-Transport-Security "max-age=63072000" always;

    access_log /var/log/nginx/evolution_access.log;
    error_log /var/log/nginx/evolution_error.log;

    client_max_body_size 100M;

    location / {
        proxy_pass http://127.0.0.1:8021;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }
}
NGINXEOF

# Habilitar site
ln -sf /etc/nginx/sites-available/pitstop /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

echo -e "${GREEN}[OK]${NC} Nginx configurado"

#===============================================================================
# CRIAR SCRIPTS DE MANUTENÇÃO
#===============================================================================

echo -e "${BLUE}[INFO]${NC} Criando scripts de manutenção..."

# Status
cat > /opt/pitstop/scripts/status.sh << 'STATUSEOF'
#!/bin/bash
echo "═══════════════════════════════════════════"
echo "🔧 PitStop - Status dos Serviços"
echo "═══════════════════════════════════════════"
echo ""
echo "🐳 Containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "💾 Disco:"
df -h / | tail -1
echo ""
echo "🧠 Memória:"
free -h | head -2
echo ""
echo "📊 Health:"
curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP" && echo "  Backend: ✅" || echo "  Backend: ❌"
curl -s http://localhost:8021/ 2>/dev/null && echo "  Evolution: ✅" || echo "  Evolution: ❌"
curl -s http://localhost:3000/health 2>/dev/null && echo "  Frontend: ✅" || echo "  Frontend: ❌"
STATUSEOF

# Logs
cat > /opt/pitstop/scripts/logs.sh << 'LOGSEOF'
#!/bin/bash
case "$1" in
    backend) docker logs -f pitstop-backend --tail 100 ;;
    frontend) docker logs -f pitstop-frontend --tail 100 ;;
    evolution) docker logs -f evolution-api --tail 100 ;;
    postgres) docker logs -f pitstop-postgres --tail 100 ;;
    redis) docker logs -f pitstop-redis --tail 100 ;;
    nginx) tail -f /var/log/nginx/pitstop_*.log ;;
    all) docker compose -f /opt/pitstop/docker-compose.yml logs -f --tail 50 ;;
    *) echo "Uso: $0 {backend|frontend|evolution|postgres|redis|nginx|all}" ;;
esac
LOGSEOF

# Backup
cat > /opt/pitstop/scripts/backup.sh << 'BACKUPEOF'
#!/bin/bash
BACKUP_DIR="/opt/pitstop/backups"
DATE=$(date +%Y%m%d_%H%M%S)
source /opt/pitstop/.env

echo "📦 Iniciando backup..."
docker exec pitstop-postgres pg_dump -U $POSTGRES_PITSTOP_USER $POSTGRES_PITSTOP_DB | gzip > "$BACKUP_DIR/pitstop_db_$DATE.sql.gz"
docker exec evolution-postgres pg_dump -U $POSTGRES_EVOLUTION_USER $POSTGRES_EVOLUTION_DB | gzip > "$BACKUP_DIR/evolution_db_$DATE.sql.gz"
cp /opt/pitstop/.env "$BACKUP_DIR/env_$DATE.backup"
find "$BACKUP_DIR" -type f -mtime +7 -delete
echo "✅ Backup concluído: $DATE"
BACKUPEOF

# Restart
cat > /opt/pitstop/scripts/restart.sh << 'RESTARTEOF'
#!/bin/bash
case "$1" in
    all) cd /opt/pitstop && docker compose restart ;;
    backend) docker restart pitstop-backend ;;
    frontend) docker restart pitstop-frontend ;;
    evolution) docker restart evolution-api ;;
    *) echo "Uso: $0 {all|backend|frontend|evolution}" ;;
esac
RESTARTEOF

# Deploy
cat > /opt/pitstop/scripts/deploy.sh << 'DEPLOYEOF'
#!/bin/bash
set -e
cd /opt/pitstop
echo "📦 Parando serviços..."
docker compose down
echo "🔨 Reconstruindo..."
docker compose build --no-cache
echo "🚀 Iniciando..."
docker compose up -d
echo "⏳ Aguardando..."
sleep 30
echo "✅ Deploy concluído!"
docker compose ps
DEPLOYEOF

chmod +x /opt/pitstop/scripts/*.sh

# Aliases
cat >> /root/.bashrc << 'ALIASEOF'

# PitStop Aliases
alias ps-status='/opt/pitstop/scripts/status.sh'
alias ps-logs='/opt/pitstop/scripts/logs.sh'
alias ps-restart='/opt/pitstop/scripts/restart.sh'
alias ps-backup='/opt/pitstop/scripts/backup.sh'
alias ps-deploy='/opt/pitstop/scripts/deploy.sh'
alias ps-cd='cd /opt/pitstop'
ALIASEOF

echo -e "${GREEN}[OK]${NC} Scripts criados"

#===============================================================================
# CONFIGURAR BACKUP AUTOMÁTICO
#===============================================================================

echo "0 3 * * * root /opt/pitstop/scripts/backup.sh >> /var/log/pitstop-backup.log 2>&1" > /etc/cron.d/pitstop-backup
echo -e "${GREEN}[OK]${NC} Backup automático configurado (diário às 3h)"

#===============================================================================
# RESUMO
#===============================================================================

echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║              CONFIGURAÇÃO CONCLUÍDA!                              ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}═══ SENHAS GERADAS (SALVE!) ═══${NC}"
echo -e "PostgreSQL PitStop: ${YELLOW}${POSTGRES_PITSTOP_PASSWORD}${NC}"
echo -e "PostgreSQL Evolution: ${YELLOW}${POSTGRES_EVOLUTION_PASSWORD}${NC}"
echo -e "Redis: ${YELLOW}${REDIS_PASSWORD}${NC}"
echo -e "JWT Secret: ${YELLOW}${JWT_SECRET:0:20}...${NC}"
echo -e "Evolution API Key: ${YELLOW}${EVOLUTION_API_KEY}${NC}"
echo ""
echo -e "${CYAN}═══ PRÓXIMOS PASSOS ═══${NC}"
echo ""
echo "1️⃣  Configure o DNS dos domínios:"
echo "    ${DOMAIN} → ${YELLOW}86.48.22.177${NC}"
echo "    ${EVOLUTION_DOMAIN} → ${YELLOW}86.48.22.177${NC}"
echo ""
echo "2️⃣  Após DNS propagado, obtenha SSL:"
echo "    ${YELLOW}mkdir -p /var/www/certbot${NC}"
echo "    ${YELLOW}certbot certonly --webroot -w /var/www/certbot -d ${DOMAIN} -d ${EVOLUTION_DOMAIN} --email ${SSL_EMAIL} --agree-tos --non-interactive${NC}"
echo ""
echo "3️⃣  Copie o código fonte:"
echo "    ${YELLOW}# Do seu PC, execute:${NC}"
echo "    ${YELLOW}scp -r src pom.xml Dockerfile root@86.48.22.177:/opt/pitstop/backend/${NC}"
echo "    ${YELLOW}scp -r frontend/* root@86.48.22.177:/opt/pitstop/frontend/${NC}"
echo ""
echo "4️⃣  Inicie os serviços:"
echo "    ${YELLOW}cd /opt/pitstop && docker compose up -d --build${NC}"
echo ""
echo "5️⃣  Ative Nginx:"
echo "    ${YELLOW}nginx -t && systemctl reload nginx${NC}"
echo ""
echo -e "${CYAN}═══ URLs FINAIS ═══${NC}"
echo "🔧 PitStop: https://${DOMAIN}"
echo "🔧 API: https://${DOMAIN}/api"
echo "📱 Evolution: https://${EVOLUTION_DOMAIN}"
echo ""
echo -e "${CYAN}═══ COMANDOS ÚTEIS ═══${NC}"
echo "ps-status   - Status dos serviços"
echo "ps-logs     - Ver logs"
echo "ps-restart  - Reiniciar"
echo "ps-backup   - Backup manual"
echo "ps-deploy   - Deploy/atualização"
echo ""
echo -e "${GREEN}✅ Configuração salva em /opt/pitstop/.env${NC}"
