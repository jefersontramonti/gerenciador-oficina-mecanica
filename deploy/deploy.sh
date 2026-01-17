#!/bin/bash
#===============================================================================
# PitStop AI - Script de Deploy
# Localização na VPS: /opt/pitstop/deploy.sh
#
# Uso:
#   ./deploy.sh              # Deploy completo (backend + frontend)
#   ./deploy.sh backend      # Apenas backend
#   ./deploy.sh frontend     # Apenas frontend
#   ./deploy.sh landing      # Apenas landing page
#===============================================================================

set -e

cd /opt/pitstop

echo "=========================================="
echo "PitStop AI - Deploy"
echo "=========================================="
echo "Iniciado em: $(date)"
echo ""

# Função para limpar containers órfãos
cleanup_containers() {
    echo "[1/5] Limpando containers órfãos..."
    docker compose down --remove-orphans 2>/dev/null || true
    docker rm -f pitstop-backend pitstop-frontend 2>/dev/null || true
}

# Função para atualizar do GitHub
update_from_github() {
    echo "[2/5] Atualizando código do GitHub..."

    # IMPORTANTE: Fazer backup do .env antes de qualquer operação
    if [ -f /opt/pitstop/.env ]; then
        cp /opt/pitstop/.env /opt/pitstop/.env.backup
        echo "Backup do .env criado em .env.backup"
    fi

    git fetch origin main
}

# Deploy do backend
deploy_backend() {
    echo "[3/5] Deploy do Backend..."

    # Código fonte Java (TODOS os pacotes, incluindo saas)
    git checkout origin/main -- src/main/java/com/pitstop/

    # Recursos (migrations, application.properties, templates)
    git checkout origin/main -- src/main/resources/

    # Arquivos de configuração
    git checkout origin/main -- pom.xml
    git checkout origin/main -- backend/Dockerfile 2>/dev/null || true

    # Garantir que a pasta backend/src existe e copiar código
    mkdir -p backend/src/main/java/com/pitstop
    mkdir -p backend/src/main/resources
    cp -r src/main/java/com/pitstop/* backend/src/main/java/com/pitstop/
    cp -r src/main/resources/* backend/src/main/resources/
    cp pom.xml backend/pom.xml

    echo "Arquivos do backend copiados."

    # Limpar cache Redis para evitar erros de serialização
    echo "Limpando cache Redis..."
    docker exec pitstop-redis redis-cli -a $(grep REDIS_PASSWORD /opt/pitstop/.env | cut -d'=' -f2) FLUSHALL 2>/dev/null || true

    docker compose build pitstop-backend --no-cache
    docker compose up -d --force-recreate --remove-orphans pitstop-backend
}

# Deploy do frontend
deploy_frontend() {
    echo "[3/5] Deploy do Frontend..."

    # Código fonte TypeScript/React
    git checkout origin/main -- frontend/src/
    git checkout origin/main -- frontend/public/ 2>/dev/null || true

    # Arquivos de configuração
    git checkout origin/main -- frontend/package.json
    git checkout origin/main -- frontend/package-lock.json 2>/dev/null || true
    git checkout origin/main -- frontend/vite.config.ts
    git checkout origin/main -- frontend/tsconfig.json
    git checkout origin/main -- frontend/tsconfig.node.json 2>/dev/null || true
    git checkout origin/main -- frontend/tailwind.config.js 2>/dev/null || true
    git checkout origin/main -- frontend/postcss.config.js 2>/dev/null || true
    git checkout origin/main -- frontend/index.html
    git checkout origin/main -- frontend/Dockerfile 2>/dev/null || true
    git checkout origin/main -- frontend/nginx.conf 2>/dev/null || true
    git checkout origin/main -- frontend/.env.production 2>/dev/null || true

    echo "Arquivos do frontend copiados."

    docker compose build pitstop-frontend --no-cache
    docker compose up -d --force-recreate --remove-orphans pitstop-frontend
}

# Deploy da landing page
deploy_landing() {
    echo "[3/5] Deploy da Landing Page..."
    git checkout origin/main -- landing-page/
    mkdir -p /opt/pitstop/landing
    cp -r landing-page/* /opt/pitstop/landing/
    echo "Landing page atualizada em /opt/pitstop/landing/"
}

# Deploy completo
deploy_all() {
    cleanup_containers
    update_from_github

    echo "[3/5] Checkout de todos os arquivos..."

    # Backend - código fonte e recursos
    git checkout origin/main -- src/main/java/com/pitstop/
    git checkout origin/main -- src/main/resources/
    git checkout origin/main -- pom.xml
    git checkout origin/main -- backend/ 2>/dev/null || true

    # Frontend
    git checkout origin/main -- frontend/

    # Landing page
    git checkout origin/main -- landing-page/ 2>/dev/null || true

    # Docker configs
    git checkout origin/main -- docker-compose.prod.yml
    git checkout origin/main -- deploy/ 2>/dev/null || true

    echo "[4/5] Preparando estrutura de diretórios..."

    # Restaurar .env do backup se foi sobrescrito
    if [ -f /opt/pitstop/.env.backup ]; then
        # Verificar se .env foi alterado (ficou vazio ou diferente)
        if [ ! -s /opt/pitstop/.env ] || ! grep -q "MAIL_HOST=" /opt/pitstop/.env || [ -z "$(grep 'MAIL_HOST=' /opt/pitstop/.env | cut -d'=' -f2)" ]; then
            echo "Restaurando .env do backup..."
            cp /opt/pitstop/.env.backup /opt/pitstop/.env
        fi
    fi

    # Garantir estrutura backend
    mkdir -p backend/src/main/java/com/pitstop
    mkdir -p backend/src/main/resources
    cp -r src/main/java/com/pitstop/* backend/src/main/java/com/pitstop/
    cp -r src/main/resources/* backend/src/main/resources/
    cp pom.xml backend/pom.xml

    # Atualizar docker-compose se mudou
    cp docker-compose.prod.yml docker-compose.yml 2>/dev/null || true

    # Copiar landing page
    mkdir -p /opt/pitstop/landing
    cp -r landing-page/* /opt/pitstop/landing/ 2>/dev/null || true

    # Limpar cache Redis
    echo "Limpando cache Redis..."
    docker exec pitstop-redis redis-cli -a $(grep REDIS_PASSWORD /opt/pitstop/.env | cut -d'=' -f2) FLUSHALL 2>/dev/null || true

    echo "[5/5] Build e deploy dos containers..."
    docker compose build --no-cache
    docker compose up -d --force-recreate --remove-orphans
}

# Verificar status final
check_status() {
    echo ""
    echo "Aguardando containers iniciarem..."
    sleep 10

    # Limpar cache Redis APÓS containers subirem para evitar problemas de serialização
    echo ""
    echo "=========================================="
    echo "Limpando cache Redis (pós-deploy)..."
    echo "=========================================="
    REDIS_PASS=$(grep REDIS_PASSWORD /opt/pitstop/.env | cut -d'=' -f2)
    docker exec pitstop-redis redis-cli -a "$REDIS_PASS" FLUSHALL 2>/dev/null && echo "Cache Redis limpo!" || echo "Aviso: Não foi possível limpar cache Redis"

    echo ""
    echo "=========================================="
    echo "Status dos Containers:"
    echo "=========================================="
    docker compose ps

    echo ""
    echo "=========================================="
    echo "Health Check do Backend:"
    echo "=========================================="
    # Aguardar backend ficar healthy
    sleep 5
    curl -s http://127.0.0.1:8080/actuator/health 2>/dev/null | jq '.' || echo "Backend ainda iniciando..."

    echo ""
    echo "=========================================="
    echo "Deploy concluído em: $(date)"
    echo "=========================================="
}

# Função de rollback
rollback() {
    echo "Executando rollback..."
    git checkout HEAD~1 -- src/main/java/com/pitstop/
    git checkout HEAD~1 -- src/main/resources/
    git checkout HEAD~1 -- frontend/src/

    mkdir -p backend/src/main/java/com/pitstop
    mkdir -p backend/src/main/resources
    cp -r src/main/java/com/pitstop/* backend/src/main/java/com/pitstop/
    cp -r src/main/resources/* backend/src/main/resources/

    docker compose build --no-cache
    docker compose up -d --force-recreate --remove-orphans
    echo "Rollback concluído!"
}

# Main
case "${1:-all}" in
    backend)
        update_from_github
        deploy_backend
        check_status
        ;;
    frontend)
        update_from_github
        deploy_frontend
        check_status
        ;;
    landing)
        update_from_github
        deploy_landing
        ;;
    rollback)
        rollback
        check_status
        ;;
    all|*)
        deploy_all
        check_status
        ;;
esac
