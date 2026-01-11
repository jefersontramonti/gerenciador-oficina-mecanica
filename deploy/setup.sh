#!/bin/bash
#===============================================================================
# PitStop + Evolution API - Script de Instalação Completa
# VPS: Contabo 12GB RAM
# Autor: PitStop Team
# Data: 2025
#===============================================================================

set -e  # Parar em caso de erro

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Banner
print_banner() {
    echo -e "${CYAN}"
    echo "╔═══════════════════════════════════════════════════════════════════╗"
    echo "║                                                                   ║"
    echo "║   🔧 PitStop + Evolution API - Setup Integrado                   ║"
    echo "║                                                                   ║"
    echo "║   VPS: Contabo 12GB RAM                                          ║"
    echo "║   Services: PostgreSQL, Redis, Spring Boot, React, Evolution     ║"
    echo "║                                                                   ║"
    echo "╚═══════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Logging
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${PURPLE}[STEP]${NC} $1"; }

# Verificar se é root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        log_error "Este script precisa ser executado como root"
        echo "Execute: sudo bash setup.sh"
        exit 1
    fi
}

# Verificar requisitos do sistema
check_requirements() {
    log_step "Verificando requisitos do sistema..."

    # RAM mínima (8GB)
    total_ram=$(free -g | awk '/^Mem:/{print $2}')
    if [[ $total_ram -lt 8 ]]; then
        log_warning "RAM disponível: ${total_ram}GB. Recomendado: 8GB+"
    else
        log_success "RAM disponível: ${total_ram}GB"
    fi

    # Disco mínimo (20GB livres)
    free_disk=$(df -BG / | awk 'NR==2 {print $4}' | tr -d 'G')
    if [[ $free_disk -lt 20 ]]; then
        log_error "Espaço em disco insuficiente: ${free_disk}GB. Necessário: 20GB+"
        exit 1
    fi
    log_success "Espaço em disco: ${free_disk}GB"
}

# Atualizar sistema
update_system() {
    log_step "Atualizando sistema operacional..."
    apt update && apt upgrade -y
    log_success "Sistema atualizado"
}

# Instalar dependências
install_dependencies() {
    log_step "Instalando dependências..."

    apt install -y \
        apt-transport-https \
        ca-certificates \
        curl \
        gnupg \
        lsb-release \
        git \
        htop \
        nano \
        unzip \
        ufw \
        fail2ban \
        certbot \
        python3-certbot-nginx

    log_success "Dependências instaladas"
}

# Instalar Docker
install_docker() {
    log_step "Instalando Docker..."

    if command -v docker &> /dev/null; then
        log_warning "Docker já está instalado"
        docker --version
    else
        # Remover versões antigas
        apt remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true

        # Adicionar repositório Docker
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

        echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

        apt update
        apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

        # Iniciar Docker
        systemctl enable docker
        systemctl start docker

        log_success "Docker instalado"
    fi
}

# Configurar diretórios
setup_directories() {
    log_step "Criando estrutura de diretórios..."

    mkdir -p /opt/pitstop/{backend,frontend,nginx,ssl,logs,backups}
    mkdir -p /opt/pitstop/data/{postgres-pitstop,postgres-evolution,redis}
    mkdir -p /opt/evolution

    log_success "Diretórios criados em /opt/pitstop"
}

# Configurar variáveis de ambiente
setup_environment() {
    log_step "Configurando variáveis de ambiente..."

    ENV_FILE="/opt/pitstop/.env"

    if [[ -f "$ENV_FILE" ]]; then
        log_warning "Arquivo .env já existe. Deseja sobrescrever? (y/N)"
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            log_info "Mantendo arquivo .env existente"
            return
        fi
    fi

    # Gerar senhas seguras
    POSTGRES_PITSTOP_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
    POSTGRES_EVOLUTION_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
    REDIS_PASSWORD=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)
    JWT_SECRET=$(openssl rand -base64 64 | tr -dc 'a-zA-Z0-9' | head -c 64)
    EVOLUTION_API_KEY=$(openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32)

    # Solicitar informações do usuário
    echo ""
    echo -e "${CYAN}═══ Configuração do Domínio ═══${NC}"
    read -p "Digite seu domínio principal (ex: pitstop.seudominio.com): " DOMAIN
    read -p "Digite seu email (para SSL Let's Encrypt): " SSL_EMAIL

    echo ""
    echo -e "${CYAN}═══ Configuração do PitStop ═══${NC}"
    read -p "Nome da oficina: " OFICINA_NOME
    read -p "CNPJ da oficina (formato: 00.000.000/0001-00): " OFICINA_CNPJ
    read -p "Telefone da oficina: " OFICINA_TELEFONE
    read -p "Email da oficina: " OFICINA_EMAIL

    echo ""
    echo -e "${CYAN}═══ Configuração do Evolution API (WhatsApp) ═══${NC}"
    read -p "Subdomínio para Evolution API (ex: whatsapp.seudominio.com): " EVOLUTION_DOMAIN

    # Criar arquivo .env
    cat > "$ENV_FILE" << EOF
#===============================================================================
# PitStop + Evolution API - Variáveis de Ambiente
# Gerado em: $(date)
# ATENÇÃO: Mantenha este arquivo seguro! Contém senhas e chaves secretas.
#===============================================================================

#---------------------------------------
# DOMÍNIOS E URLs
#---------------------------------------
DOMAIN=${DOMAIN}
EVOLUTION_DOMAIN=${EVOLUTION_DOMAIN}
SSL_EMAIL=${SSL_EMAIL}

# URLs da aplicação
APP_FRONTEND_URL=https://${DOMAIN}
APP_BASE_URL=https://${DOMAIN}/api
CORS_ALLOWED_ORIGINS=https://${DOMAIN}

#---------------------------------------
# POSTGRESQL - PITSTOP
#---------------------------------------
POSTGRES_PITSTOP_HOST=postgres-pitstop
POSTGRES_PITSTOP_PORT=5432
POSTGRES_PITSTOP_DB=pitstop_db
POSTGRES_PITSTOP_USER=pitstop
POSTGRES_PITSTOP_PASSWORD=${POSTGRES_PITSTOP_PASSWORD}
DATABASE_URL=jdbc:postgresql://postgres-pitstop:5432/pitstop_db
DATABASE_USERNAME=pitstop
DATABASE_PASSWORD=${POSTGRES_PITSTOP_PASSWORD}

#---------------------------------------
# POSTGRESQL - EVOLUTION
#---------------------------------------
POSTGRES_EVOLUTION_HOST=postgres-evolution
POSTGRES_EVOLUTION_PORT=5433
POSTGRES_EVOLUTION_DB=evolution_db
POSTGRES_EVOLUTION_USER=evolution
POSTGRES_EVOLUTION_PASSWORD=${POSTGRES_EVOLUTION_PASSWORD}

#---------------------------------------
# REDIS
#---------------------------------------
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}

#---------------------------------------
# JWT / SEGURANÇA
#---------------------------------------
JWT_SECRET=${JWT_SECRET}

#---------------------------------------
# EVOLUTION API (WhatsApp)
#---------------------------------------
EVOLUTION_API_URL=https://${EVOLUTION_DOMAIN}
EVOLUTION_API_KEY=${EVOLUTION_API_KEY}
AUTHENTICATION_API_KEY=${EVOLUTION_API_KEY}

#---------------------------------------
# PITSTOP - DADOS DA OFICINA
#---------------------------------------
PITSTOP_OFICINA_NOME="${OFICINA_NOME}"
PITSTOP_OFICINA_CNPJ="${OFICINA_CNPJ}"
PITSTOP_OFICINA_TELEFONE="${OFICINA_TELEFONE}"
PITSTOP_OFICINA_EMAIL="${OFICINA_EMAIL}"

#---------------------------------------
# SPRING PROFILES
#---------------------------------------
SPRING_PROFILES_ACTIVE=prod

#---------------------------------------
# LOGGING
#---------------------------------------
LOG_LEVEL=INFO

#---------------------------------------
# TELEGRAM (OPCIONAL - configurar depois)
#---------------------------------------
# TELEGRAM_BOT_TOKEN=seu_bot_token_aqui
# TELEGRAM_CHAT_ID=seu_chat_id_aqui

#---------------------------------------
# EMAIL SMTP (OPCIONAL - configurar depois)
#---------------------------------------
# MAIL_HOST=smtp.gmail.com
# MAIL_PORT=587
# MAIL_USERNAME=seu_email@gmail.com
# MAIL_PASSWORD=sua_senha_de_app
# MAIL_FROM_NAME=PitStop

#---------------------------------------
# MERCADO PAGO (OPCIONAL - configurar depois)
#---------------------------------------
# MERCADOPAGO_ACCESS_TOKEN=seu_access_token
# MERCADOPAGO_PUBLIC_KEY=sua_public_key

#---------------------------------------
# IA / ANTHROPIC (OPCIONAL)
#---------------------------------------
# ANTHROPIC_API_KEY=sua_api_key
EOF

    chmod 600 "$ENV_FILE"
    log_success "Arquivo .env criado em $ENV_FILE"

    # Mostrar resumo
    echo ""
    echo -e "${GREEN}═══ Senhas Geradas (SALVE EM LOCAL SEGURO!) ═══${NC}"
    echo -e "PostgreSQL PitStop: ${YELLOW}${POSTGRES_PITSTOP_PASSWORD}${NC}"
    echo -e "PostgreSQL Evolution: ${YELLOW}${POSTGRES_EVOLUTION_PASSWORD}${NC}"
    echo -e "Redis: ${YELLOW}${REDIS_PASSWORD}${NC}"
    echo -e "JWT Secret: ${YELLOW}${JWT_SECRET:0:20}...${NC}"
    echo -e "Evolution API Key: ${YELLOW}${EVOLUTION_API_KEY}${NC}"
    echo ""
}

# Criar docker-compose.yml
create_docker_compose() {
    log_step "Criando docker-compose.yml..."

    cat > /opt/pitstop/docker-compose.yml << 'EOF'
#===============================================================================
# PitStop + Evolution API - Docker Compose Production
#===============================================================================

services:
  #-----------------------------------------------------------------------------
  # POSTGRESQL - PITSTOP DATABASE
  #-----------------------------------------------------------------------------
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

  #-----------------------------------------------------------------------------
  # POSTGRESQL - EVOLUTION DATABASE
  #-----------------------------------------------------------------------------
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

  #-----------------------------------------------------------------------------
  # REDIS
  #-----------------------------------------------------------------------------
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

  #-----------------------------------------------------------------------------
  # PITSTOP BACKEND (Spring Boot)
  #-----------------------------------------------------------------------------
  pitstop-backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    image: pitstop-backend:latest
    container_name: pitstop-backend
    restart: unless-stopped
    environment:
      # Database
      DATABASE_URL: ${DATABASE_URL}
      DATABASE_USERNAME: ${DATABASE_USERNAME}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      # Redis
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      # JWT
      JWT_SECRET: ${JWT_SECRET}
      # URLs
      APP_FRONTEND_URL: ${APP_FRONTEND_URL}
      APP_BASE_URL: ${APP_BASE_URL}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}
      # Spring
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      # Oficina
      PITSTOP_OFICINA_NOME: ${PITSTOP_OFICINA_NOME}
      PITSTOP_OFICINA_CNPJ: ${PITSTOP_OFICINA_CNPJ}
      PITSTOP_OFICINA_TELEFONE: ${PITSTOP_OFICINA_TELEFONE}
      PITSTOP_OFICINA_EMAIL: ${PITSTOP_OFICINA_EMAIL}
      # Evolution API
      EVOLUTION_API_URL: ${EVOLUTION_API_URL}
      EVOLUTION_API_KEY: ${EVOLUTION_API_KEY}
      # JVM
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
      start_period: 60s
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G

  #-----------------------------------------------------------------------------
  # PITSTOP FRONTEND (React + Nginx)
  #-----------------------------------------------------------------------------
  pitstop-frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      args:
        VITE_API_URL: ${APP_BASE_URL}/api
    image: pitstop-frontend:latest
    container_name: pitstop-frontend
    restart: unless-stopped
    ports:
      - "127.0.0.1:3000:80"
    networks:
      - pitstop-network
    depends_on:
      pitstop-backend:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost/health"]
      interval: 30s
      timeout: 5s
      retries: 3
    deploy:
      resources:
        limits:
          memory: 256M

  #-----------------------------------------------------------------------------
  # EVOLUTION API V2 (WhatsApp)
  #-----------------------------------------------------------------------------
  evolution-api:
    image: atendai/evolution-api:v2.2.3
    container_name: evolution-api
    restart: unless-stopped
    environment:
      # Server
      SERVER_URL: https://${EVOLUTION_DOMAIN}
      # Database
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
      # Redis
      CACHE_REDIS_ENABLED: "true"
      CACHE_REDIS_URI: redis://:${REDIS_PASSWORD}@redis:6379/1
      CACHE_REDIS_PREFIX_KEY: evolution
      CACHE_REDIS_SAVE_INSTANCES: "true"
      CACHE_LOCAL_ENABLED: "false"
      # Auth
      AUTHENTICATION_TYPE: apikey
      AUTHENTICATION_API_KEY: ${AUTHENTICATION_API_KEY}
      AUTHENTICATION_EXPOSE_IN_FETCH_INSTANCES: "true"
      # Instance
      DEL_INSTANCE: "false"
      DEL_TEMP_INSTANCES: "true"
      # Webhook
      WEBHOOK_GLOBAL_ENABLED: "false"
      WEBHOOK_GLOBAL_URL: ""
      WEBHOOK_GLOBAL_WEBHOOK_BY_EVENTS: "false"
      # RabbitMQ (disabled)
      RABBITMQ_ENABLED: "false"
      # SQS (disabled)
      SQS_ENABLED: "false"
      # WebSocket
      WEBSOCKET_ENABLED: "true"
      WEBSOCKET_GLOBAL_EVENTS: "false"
      # Chatwoot (disabled)
      CHATWOOT_ENABLED: "false"
      # OpenAI (disabled)
      OPENAI_ENABLED: "false"
      # Dify (disabled)
      DIFY_ENABLED: "false"
      # TypeBot (disabled)
      TYPEBOT_ENABLED: "false"
      # S3 (disabled)
      S3_ENABLED: "false"
      # Log
      LOG_LEVEL: ERROR
      LOG_COLOR: "true"
      LOG_BAILEYS: error
      # Cors
      CORS_ORIGIN: "*"
      CORS_METHODS: "POST,GET,PUT,DELETE"
      CORS_CREDENTIALS: "true"
      # QRCode
      QRCODE_LIMIT: 30
      QRCODE_COLOR: "#198754"
      # Telemetry
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

#-----------------------------------------------------------------------------
# NETWORKS
#-----------------------------------------------------------------------------
networks:
  pitstop-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
EOF

    log_success "docker-compose.yml criado"
}

# Criar configuração do Nginx
create_nginx_config() {
    log_step "Criando configuração do Nginx..."

    # Carregar variáveis
    source /opt/pitstop/.env

    # Configuração principal do Nginx
    cat > /opt/pitstop/nginx/pitstop.conf << EOF
#===============================================================================
# Nginx Configuration - PitStop + Evolution API
#===============================================================================

# Rate limiting
limit_req_zone \$binary_remote_addr zone=api_limit:10m rate=10r/s;
limit_req_zone \$binary_remote_addr zone=login_limit:10m rate=1r/s;

# Upstream servers
upstream pitstop_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}

upstream pitstop_frontend {
    server 127.0.0.1:3000;
    keepalive 16;
}

upstream evolution_api {
    server 127.0.0.1:8021;
    keepalive 16;
}

#---------------------------------------
# HTTP -> HTTPS Redirect
#---------------------------------------
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

#---------------------------------------
# PitStop Main Application
#---------------------------------------
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name ${DOMAIN};

    # SSL Configuration (will be configured by certbot)
    ssl_certificate /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:50m;
    ssl_session_tickets off;

    # Modern SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # HSTS
    add_header Strict-Transport-Security "max-age=63072000" always;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Gzip
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml application/json application/javascript application/rss+xml application/atom+xml image/svg+xml;

    # Logs
    access_log /var/log/nginx/pitstop_access.log;
    error_log /var/log/nginx/pitstop_error.log;

    # Max upload size
    client_max_body_size 50M;

    #-----------------------------------
    # API Backend
    #-----------------------------------
    location /api/ {
        limit_req zone=api_limit burst=20 nodelay;

        proxy_pass http://pitstop_backend/;
        proxy_http_version 1.1;

        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Connection "";

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }

    #-----------------------------------
    # Login Rate Limiting (extra protection)
    #-----------------------------------
    location /api/auth/login {
        limit_req zone=login_limit burst=3 nodelay;

        proxy_pass http://pitstop_backend/auth/login;
        proxy_http_version 1.1;

        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    #-----------------------------------
    # WebSocket (for real-time notifications)
    #-----------------------------------
    location /ws {
        proxy_pass http://pitstop_backend/ws;
        proxy_http_version 1.1;

        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        proxy_read_timeout 86400;
        proxy_send_timeout 86400;
    }

    #-----------------------------------
    # Actuator (health checks - internal only)
    #-----------------------------------
    location /actuator {
        allow 127.0.0.1;
        deny all;

        proxy_pass http://pitstop_backend/actuator;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
    }

    #-----------------------------------
    # Swagger UI (development - consider disabling in production)
    #-----------------------------------
    location /swagger-ui {
        proxy_pass http://pitstop_backend/swagger-ui;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /v3/api-docs {
        proxy_pass http://pitstop_backend/v3/api-docs;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
    }

    #-----------------------------------
    # Frontend (React SPA)
    #-----------------------------------
    location / {
        proxy_pass http://pitstop_frontend;
        proxy_http_version 1.1;

        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # Cache static assets
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            proxy_pass http://pitstop_frontend;
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}

#---------------------------------------
# Evolution API (WhatsApp)
#---------------------------------------
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name ${EVOLUTION_DOMAIN};

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/${EVOLUTION_DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${EVOLUTION_DOMAIN}/privkey.pem;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:50m;
    ssl_session_tickets off;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    add_header Strict-Transport-Security "max-age=63072000" always;

    # Logs
    access_log /var/log/nginx/evolution_access.log;
    error_log /var/log/nginx/evolution_error.log;

    client_max_body_size 100M;

    location / {
        proxy_pass http://evolution_api;
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
EOF

    log_success "Configuração Nginx criada"
}

# Instalar e configurar Nginx
install_nginx() {
    log_step "Instalando e configurando Nginx..."

    apt install -y nginx

    # Remover config padrão
    rm -f /etc/nginx/sites-enabled/default

    # Copiar nossa config
    cp /opt/pitstop/nginx/pitstop.conf /etc/nginx/sites-available/pitstop
    ln -sf /etc/nginx/sites-available/pitstop /etc/nginx/sites-enabled/

    # Criar diretório para certbot
    mkdir -p /var/www/certbot

    # Testar config (vai falhar até ter SSL)
    log_warning "Nginx configurado, mas SSL ainda não está ativo"

    log_success "Nginx instalado"
}

# Configurar SSL com Let's Encrypt
setup_ssl() {
    log_step "Configurando SSL com Let's Encrypt..."

    source /opt/pitstop/.env

    # Criar config temporária sem SSL para obter certificados
    cat > /etc/nginx/sites-available/pitstop-temp << EOF
server {
    listen 80;
    server_name ${DOMAIN} ${EVOLUTION_DOMAIN};

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 200 'OK';
        add_header Content-Type text/plain;
    }
}
EOF

    ln -sf /etc/nginx/sites-available/pitstop-temp /etc/nginx/sites-enabled/pitstop
    systemctl reload nginx

    # Obter certificados
    certbot certonly --webroot \
        -w /var/www/certbot \
        -d ${DOMAIN} \
        -d ${EVOLUTION_DOMAIN} \
        --email ${SSL_EMAIL} \
        --agree-tos \
        --non-interactive

    # Restaurar config completa
    cp /opt/pitstop/nginx/pitstop.conf /etc/nginx/sites-available/pitstop
    ln -sf /etc/nginx/sites-available/pitstop /etc/nginx/sites-enabled/
    rm -f /etc/nginx/sites-available/pitstop-temp

    # Testar e recarregar
    nginx -t && systemctl reload nginx

    # Configurar renovação automática
    echo "0 12 * * * root certbot renew --quiet --post-hook 'systemctl reload nginx'" > /etc/cron.d/certbot-renew

    log_success "SSL configurado com sucesso"
}

# Configurar Firewall
setup_firewall() {
    log_step "Configurando Firewall (UFW)..."

    ufw default deny incoming
    ufw default allow outgoing

    # SSH
    ufw allow 22/tcp

    # HTTP/HTTPS
    ufw allow 80/tcp
    ufw allow 443/tcp

    # Habilitar
    ufw --force enable

    log_success "Firewall configurado"
}

# Configurar Fail2Ban
setup_fail2ban() {
    log_step "Configurando Fail2Ban..."

    cat > /etc/fail2ban/jail.local << 'EOF'
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = ssh
filter = sshd
logpath = /var/log/auth.log
maxretry = 3

[nginx-http-auth]
enabled = true
port = http,https
filter = nginx-http-auth
logpath = /var/log/nginx/error.log
maxretry = 3

[nginx-limit-req]
enabled = true
port = http,https
filter = nginx-limit-req
logpath = /var/log/nginx/error.log
maxretry = 10
EOF

    systemctl enable fail2ban
    systemctl restart fail2ban

    log_success "Fail2Ban configurado"
}

# Copiar código fonte
copy_source_code() {
    log_step "Copiando código fonte..."

    echo -e "${YELLOW}O código fonte deve ser copiado para /opt/pitstop/backend e /opt/pitstop/frontend${NC}"
    echo ""
    echo "Opções:"
    echo "1. Clone do repositório Git"
    echo "2. Upload manual (SCP/SFTP)"
    echo ""
    read -p "Digite o URL do repositório Git (ou Enter para pular): " GIT_REPO

    if [[ -n "$GIT_REPO" ]]; then
        git clone "$GIT_REPO" /tmp/pitstop-source

        # Backend
        cp -r /tmp/pitstop-source/src /opt/pitstop/backend/
        cp -r /tmp/pitstop-source/pom.xml /opt/pitstop/backend/
        cp -r /tmp/pitstop-source/Dockerfile /opt/pitstop/backend/

        # Frontend
        cp -r /tmp/pitstop-source/frontend/* /opt/pitstop/frontend/

        rm -rf /tmp/pitstop-source

        log_success "Código fonte copiado do Git"
    else
        log_warning "Pule esta etapa. Copie manualmente:"
        echo "  - Backend: /opt/pitstop/backend/"
        echo "  - Frontend: /opt/pitstop/frontend/"
    fi
}

# Criar scripts de manutenção
create_maintenance_scripts() {
    log_step "Criando scripts de manutenção..."

    # Script de backup
    cat > /opt/pitstop/scripts/backup.sh << 'EOF'
#!/bin/bash
#===============================================================================
# PitStop - Script de Backup
#===============================================================================

BACKUP_DIR="/opt/pitstop/backups"
DATE=$(date +%Y%m%d_%H%M%S)

source /opt/pitstop/.env

# Backup PostgreSQL PitStop
docker exec pitstop-postgres pg_dump -U $POSTGRES_PITSTOP_USER $POSTGRES_PITSTOP_DB | gzip > "$BACKUP_DIR/pitstop_db_$DATE.sql.gz"

# Backup PostgreSQL Evolution
docker exec evolution-postgres pg_dump -U $POSTGRES_EVOLUTION_USER $POSTGRES_EVOLUTION_DB | gzip > "$BACKUP_DIR/evolution_db_$DATE.sql.gz"

# Backup Redis
docker exec pitstop-redis redis-cli -a $REDIS_PASSWORD --rdb /data/dump.rdb
cp /opt/pitstop/data/redis/dump.rdb "$BACKUP_DIR/redis_$DATE.rdb"

# Backup .env
cp /opt/pitstop/.env "$BACKUP_DIR/env_$DATE.backup"

# Limpar backups antigos (manter últimos 7 dias)
find "$BACKUP_DIR" -type f -mtime +7 -delete

echo "Backup concluído: $DATE"
EOF

    # Script de status
    cat > /opt/pitstop/scripts/status.sh << 'EOF'
#!/bin/bash
#===============================================================================
# PitStop - Status dos Serviços
#===============================================================================

echo "╔═══════════════════════════════════════════════════════════════════╗"
echo "║              PitStop - Status dos Serviços                       ║"
echo "╚═══════════════════════════════════════════════════════════════════╝"
echo ""

echo "🐳 Docker Containers:"
echo "─────────────────────"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "💾 Uso de Disco:"
echo "─────────────────"
df -h / | tail -1

echo ""
echo "🧠 Uso de Memória:"
echo "─────────────────"
free -h | head -2

echo ""
echo "🔌 Portas em Uso:"
echo "─────────────────"
ss -tlnp | grep -E '(8080|8021|5432|5433|6379|80|443)'

echo ""
echo "📊 Health Checks:"
echo "─────────────────"
curl -s http://localhost:8080/actuator/health | head -1 && echo " - Backend OK" || echo " - Backend FAIL"
curl -s http://localhost:8021/ > /dev/null && echo " - Evolution API OK" || echo " - Evolution API FAIL"
EOF

    # Script de logs
    cat > /opt/pitstop/scripts/logs.sh << 'EOF'
#!/bin/bash
#===============================================================================
# PitStop - Visualizar Logs
#===============================================================================

case "$1" in
    backend)
        docker logs -f pitstop-backend --tail 100
        ;;
    frontend)
        docker logs -f pitstop-frontend --tail 100
        ;;
    evolution)
        docker logs -f evolution-api --tail 100
        ;;
    postgres)
        docker logs -f pitstop-postgres --tail 100
        ;;
    redis)
        docker logs -f pitstop-redis --tail 100
        ;;
    nginx)
        tail -f /var/log/nginx/pitstop_error.log /var/log/nginx/pitstop_access.log
        ;;
    all)
        docker compose -f /opt/pitstop/docker-compose.yml logs -f --tail 50
        ;;
    *)
        echo "Uso: $0 {backend|frontend|evolution|postgres|redis|nginx|all}"
        exit 1
        ;;
esac
EOF

    # Script de restart
    cat > /opt/pitstop/scripts/restart.sh << 'EOF'
#!/bin/bash
#===============================================================================
# PitStop - Reiniciar Serviços
#===============================================================================

case "$1" in
    all)
        docker compose -f /opt/pitstop/docker-compose.yml restart
        ;;
    backend)
        docker restart pitstop-backend
        ;;
    frontend)
        docker restart pitstop-frontend
        ;;
    evolution)
        docker restart evolution-api
        ;;
    *)
        echo "Uso: $0 {all|backend|frontend|evolution}"
        exit 1
        ;;
esac
EOF

    # Script de deploy/update
    cat > /opt/pitstop/scripts/deploy.sh << 'EOF'
#!/bin/bash
#===============================================================================
# PitStop - Deploy/Update
#===============================================================================

set -e

cd /opt/pitstop

echo "📦 Parando serviços..."
docker compose down

echo "🔨 Reconstruindo imagens..."
docker compose build --no-cache

echo "🚀 Iniciando serviços..."
docker compose up -d

echo "⏳ Aguardando health checks..."
sleep 30

echo "✅ Deploy concluído!"
docker compose ps
EOF

    # Tornar executáveis
    chmod +x /opt/pitstop/scripts/*.sh
    mkdir -p /opt/pitstop/scripts

    # Criar aliases
    cat >> /root/.bashrc << 'EOF'

# PitStop Aliases
alias ps-status='/opt/pitstop/scripts/status.sh'
alias ps-logs='/opt/pitstop/scripts/logs.sh'
alias ps-restart='/opt/pitstop/scripts/restart.sh'
alias ps-backup='/opt/pitstop/scripts/backup.sh'
alias ps-deploy='/opt/pitstop/scripts/deploy.sh'
alias ps-cd='cd /opt/pitstop'
EOF

    log_success "Scripts de manutenção criados"
}

# Configurar backup automático
setup_auto_backup() {
    log_step "Configurando backup automático..."

    # Backup diário às 3h da manhã
    echo "0 3 * * * root /opt/pitstop/scripts/backup.sh >> /var/log/pitstop-backup.log 2>&1" > /etc/cron.d/pitstop-backup

    log_success "Backup automático configurado (diário às 3h)"
}

# Iniciar serviços
start_services() {
    log_step "Iniciando serviços..."

    cd /opt/pitstop

    # Build e start
    docker compose up -d --build

    # Aguardar
    echo "Aguardando serviços iniciarem..."
    sleep 60

    # Status
    docker compose ps

    log_success "Serviços iniciados"
}

# Resumo final
print_summary() {
    source /opt/pitstop/.env

    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                    INSTALAÇÃO CONCLUÍDA!                          ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${CYAN}═══ URLs de Acesso ═══${NC}"
    echo -e "  🔧 PitStop Frontend:    ${YELLOW}https://${DOMAIN}${NC}"
    echo -e "  🔧 PitStop API:         ${YELLOW}https://${DOMAIN}/api${NC}"
    echo -e "  🔧 Swagger UI:          ${YELLOW}https://${DOMAIN}/swagger-ui.html${NC}"
    echo -e "  📱 Evolution API:       ${YELLOW}https://${EVOLUTION_DOMAIN}${NC}"
    echo ""
    echo -e "${CYAN}═══ Comandos Úteis ═══${NC}"
    echo -e "  ps-status    - Ver status dos serviços"
    echo -e "  ps-logs      - Ver logs (backend|frontend|evolution|all)"
    echo -e "  ps-restart   - Reiniciar serviços"
    echo -e "  ps-backup    - Executar backup manual"
    echo -e "  ps-deploy    - Fazer deploy de atualização"
    echo ""
    echo -e "${CYAN}═══ Arquivos Importantes ═══${NC}"
    echo -e "  📁 Projeto:       /opt/pitstop/"
    echo -e "  📁 Env vars:      /opt/pitstop/.env"
    echo -e "  📁 Docker:        /opt/pitstop/docker-compose.yml"
    echo -e "  📁 Nginx:         /etc/nginx/sites-available/pitstop"
    echo -e "  📁 Backups:       /opt/pitstop/backups/"
    echo -e "  📁 Logs:          /opt/pitstop/logs/"
    echo ""
    echo -e "${YELLOW}⚠️  IMPORTANTE: Salve as senhas do arquivo .env em local seguro!${NC}"
    echo ""
    echo -e "${CYAN}═══ Próximos Passos ═══${NC}"
    echo "  1. Copie o código fonte para /opt/pitstop/backend e /opt/pitstop/frontend"
    echo "  2. Execute: cd /opt/pitstop && docker compose up -d --build"
    echo "  3. Configure o Evolution API: https://${EVOLUTION_DOMAIN}"
    echo "  4. Configure notificações (Telegram, Email) no arquivo .env"
    echo ""
}

#===============================================================================
# MAIN
#===============================================================================

main() {
    print_banner

    check_root
    check_requirements

    echo ""
    echo -e "${YELLOW}Este script irá instalar e configurar:${NC}"
    echo "  - Docker e Docker Compose"
    echo "  - PostgreSQL (2 instâncias: PitStop e Evolution)"
    echo "  - Redis"
    echo "  - PitStop Backend (Spring Boot)"
    echo "  - PitStop Frontend (React)"
    echo "  - Evolution API v2 (WhatsApp)"
    echo "  - Nginx (Reverse Proxy)"
    echo "  - SSL/TLS (Let's Encrypt)"
    echo "  - Firewall (UFW)"
    echo "  - Fail2Ban"
    echo ""
    read -p "Deseja continuar? (y/N) " -n 1 -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Instalação cancelada."
        exit 0
    fi

    update_system
    install_dependencies
    install_docker
    setup_directories
    setup_environment
    create_docker_compose
    create_nginx_config
    install_nginx
    setup_firewall
    setup_fail2ban
    create_maintenance_scripts
    setup_auto_backup
    copy_source_code

    echo ""
    echo -e "${YELLOW}Deseja configurar SSL e iniciar os serviços agora? (y/N)${NC}"
    read -p "" -n 1 -r
    echo ""

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        setup_ssl
        start_services
    fi

    print_summary
}

# Executar
main "$@"
