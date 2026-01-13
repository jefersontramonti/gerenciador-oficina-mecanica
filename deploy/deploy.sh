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

# Função para limpar containers órfãos
cleanup_containers() {
    echo "[1/4] Limpando containers órfãos..."
    docker rm -f pitstop-backend pitstop-frontend pitstop-postgres pitstop-redis 2>/dev/null || true
}

# Função para atualizar do GitHub
update_from_github() {
    echo "[2/4] Atualizando código do GitHub..."
    git fetch origin main
}

# Deploy do backend
deploy_backend() {
    echo "[3/4] Deploy do Backend..."
    git checkout origin/main -- src/main/java/com/pitstop/
    git checkout origin/main -- pom.xml
    git checkout origin/main -- backend/Dockerfile 2>/dev/null || true

    # Limpar cache Redis para evitar erros de serialização
    echo "Limpando cache Redis..."
    docker exec pitstop-redis redis-cli -a $(grep REDIS_PASSWORD /opt/pitstop/.env | cut -d'=' -f2) FLUSHALL 2>/dev/null || true

    docker compose build pitstop-backend --no-cache
    docker compose up -d --force-recreate --remove-orphans pitstop-backend
}

# Deploy do frontend
deploy_frontend() {
    echo "[3/4] Deploy do Frontend..."
    git checkout origin/main -- frontend/src/
    git checkout origin/main -- frontend/package.json
    git checkout origin/main -- frontend/vite.config.ts
    git checkout origin/main -- frontend/Dockerfile 2>/dev/null || true

    docker compose build pitstop-frontend --no-cache
    docker compose up -d --force-recreate --remove-orphans pitstop-frontend
}

# Deploy da landing page
deploy_landing() {
    echo "[3/4] Deploy da Landing Page..."
    git checkout origin/main -- landing-page/
    cp -r landing-page/* /opt/pitstop/landing/
    echo "Landing page atualizada em /opt/pitstop/landing/"
}

# Deploy completo
deploy_all() {
    cleanup_containers
    update_from_github

    echo "[3/4] Rebuild de todos os serviços..."
    git checkout origin/main -- src/main/java/com/pitstop/
    git checkout origin/main -- frontend/src/
    git checkout origin/main -- landing-page/ 2>/dev/null || true
    git checkout origin/main -- docker-compose.prod.yml

    # Atualizar docker-compose se mudou
    cp docker-compose.prod.yml docker-compose.yml 2>/dev/null || true

    # Copiar landing page
    cp -r landing-page/* /opt/pitstop/landing/ 2>/dev/null || true

    # Limpar cache Redis
    echo "Limpando cache Redis..."
    docker exec pitstop-redis redis-cli -a $(grep REDIS_PASSWORD /opt/pitstop/.env | cut -d'=' -f2) FLUSHALL 2>/dev/null || true

    docker compose build --no-cache
    docker compose up -d --force-recreate --remove-orphans
}

# Verificar status final
check_status() {
    echo "[4/4] Verificando status..."
    sleep 10
    docker compose ps
    echo ""
    echo "=========================================="
    echo "Deploy concluído!"
    echo "=========================================="
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
    all|*)
        deploy_all
        check_status
        ;;
esac
