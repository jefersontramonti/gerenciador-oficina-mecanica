#!/bin/bash

# ========================================
# Script de Backup Automático - PitStop
# ========================================
# Uso: ./backup.sh [diario|semanal|manual]

set -e

# Configurações
BACKUP_DIR="/var/backups/pitstop"
RETENTION_DAYS=30
RETENTION_WEEKS=12
CONTAINER_NAME="pitstop-postgres"
DB_NAME="pitstop"
DB_USER="pitstop"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_TYPE=${1:-"manual"}

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Funções auxiliares
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Criar diretórios
mkdir -p "${BACKUP_DIR}"/{daily,weekly,manual}

# Verificar se container está rodando
if ! docker ps | grep -q $CONTAINER_NAME; then
    log_error "Container $CONTAINER_NAME não está rodando!"
    exit 1
fi

# Determinar diretório de destino
case $BACKUP_TYPE in
    "diario")
        DEST_DIR="${BACKUP_DIR}/daily"
        ;;
    "semanal")
        DEST_DIR="${BACKUP_DIR}/weekly"
        ;;
    *)
        DEST_DIR="${BACKUP_DIR}/manual"
        ;;
esac

BACKUP_FILE="${DEST_DIR}/pitstop_${BACKUP_TYPE}_${TIMESTAMP}.sql"

log_info "Iniciando backup ${BACKUP_TYPE}..."

# Executar backup
if docker exec $CONTAINER_NAME pg_dump -U $DB_USER -d $DB_NAME -F c -b -v -f /tmp/backup.dump; then
    docker cp $CONTAINER_NAME:/tmp/backup.dump $BACKUP_FILE
    docker exec $CONTAINER_NAME rm /tmp/backup.dump

    # Comprimir backup
    gzip $BACKUP_FILE
    BACKUP_FILE="${BACKUP_FILE}.gz"

    log_info "Backup criado: ${BACKUP_FILE}"

    # Calcular tamanho
    SIZE=$(du -h $BACKUP_FILE | cut -f1)
    log_info "Tamanho do backup: ${SIZE}"
else
    log_error "Falha ao criar backup!"
    exit 1
fi

# Limpeza de backups antigos
log_info "Limpando backups antigos..."

# Diários: manter últimos 30 dias
find ${BACKUP_DIR}/daily -name "*.gz" -type f -mtime +$RETENTION_DAYS -delete

# Semanais: manter últimas 12 semanas
find ${BACKUP_DIR}/weekly -name "*.gz" -type f -mtime +$((RETENTION_WEEKS * 7)) -delete

log_info "Backup concluído com sucesso!"

# Estatísticas
DAILY_COUNT=$(find ${BACKUP_DIR}/daily -name "*.gz" -type f | wc -l)
WEEKLY_COUNT=$(find ${BACKUP_DIR}/weekly -name "*.gz" -type f | wc -l)
MANUAL_COUNT=$(find ${BACKUP_DIR}/manual -name "*.gz" -type f | wc -l)

log_info "Backups existentes:"
log_info "  Diários: ${DAILY_COUNT}"
log_info "  Semanais: ${WEEKLY_COUNT}"
log_info "  Manuais: ${MANUAL_COUNT}"

# Backup do Evolution API também
log_info "Fazendo backup do Evolution API..."
EVOLUTION_BACKUP="${DEST_DIR}/evolution_${BACKUP_TYPE}_${TIMESTAMP}.sql"

if docker exec $CONTAINER_NAME pg_dump -U $DB_USER -d evolution -F c -b -v -f /tmp/evolution_backup.dump; then
    docker cp $CONTAINER_NAME:/tmp/evolution_backup.dump $EVOLUTION_BACKUP
    docker exec $CONTAINER_NAME rm /tmp/evolution_backup.dump
    gzip $EVOLUTION_BACKUP
    log_info "Backup Evolution criado: ${EVOLUTION_BACKUP}.gz"
else
    log_warn "Falha ao criar backup do Evolution API (pode não existir ainda)"
fi

exit 0
