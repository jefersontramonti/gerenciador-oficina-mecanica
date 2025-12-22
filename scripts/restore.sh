#!/bin/bash

# ========================================
# Script de Restore - PitStop
# ========================================
# Uso: ./restore.sh <caminho_do_backup.sql.gz>

set -e

# Configurações
CONTAINER_NAME="pitstop-postgres"
DB_NAME="pitstop"
DB_USER="pitstop"

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validar argumentos
if [ $# -eq 0 ]; then
    log_error "Uso: $0 <caminho_do_backup.sql.gz>"
    exit 1
fi

BACKUP_FILE=$1

# Verificar se arquivo existe
if [ ! -f "$BACKUP_FILE" ]; then
    log_error "Arquivo não encontrado: $BACKUP_FILE"
    exit 1
fi

# Verificar se container está rodando
if ! docker ps | grep -q $CONTAINER_NAME; then
    log_error "Container $CONTAINER_NAME não está rodando!"
    exit 1
fi

# Confirmação
log_warn "ATENÇÃO: Este processo irá SOBRESCREVER o banco de dados atual!"
log_warn "Database: $DB_NAME"
log_warn "Backup: $BACKUP_FILE"
echo ""
read -p "Deseja continuar? (digite 'SIM' em maiúsculas): " CONFIRM

if [ "$CONFIRM" != "SIM" ]; then
    log_info "Operação cancelada pelo usuário"
    exit 0
fi

# Criar backup de segurança antes do restore
log_info "Criando backup de segurança antes do restore..."
SAFETY_BACKUP="/tmp/safety_backup_$(date +%Y%m%d_%H%M%S).sql"
docker exec $CONTAINER_NAME pg_dump -U $DB_USER -d $DB_NAME -F c -b -v -f /tmp/safety.dump
docker cp $CONTAINER_NAME:/tmp/safety.dump $SAFETY_BACKUP
gzip $SAFETY_BACKUP
log_info "Backup de segurança criado: ${SAFETY_BACKUP}.gz"

# Descompactar backup se necessário
TEMP_FILE="/tmp/restore_$(date +%s).sql"
if [[ $BACKUP_FILE == *.gz ]]; then
    log_info "Descompactando backup..."
    gunzip -c $BACKUP_FILE > $TEMP_FILE
else
    cp $BACKUP_FILE $TEMP_FILE
fi

# Copiar backup para container
log_info "Copiando backup para container..."
docker cp $TEMP_FILE $CONTAINER_NAME:/tmp/restore.dump

# Parar aplicação backend
log_info "Parando aplicação..."
docker stop pitstop-backend || true

# Desconectar todas as conexões ativas
log_info "Desconectando usuários..."
docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();"

# Drop e recriar database
log_info "Recriando database..."
docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -c "CREATE DATABASE $DB_NAME;"

# Restaurar backup
log_info "Restaurando backup..."
if docker exec $CONTAINER_NAME pg_restore -U $DB_USER -d $DB_NAME -v /tmp/restore.dump; then
    log_info "Restore concluído com sucesso!"
else
    log_error "Falha no restore!"
    log_warn "O backup de segurança está em: ${SAFETY_BACKUP}.gz"
    exit 1
fi

# Limpar arquivos temporários
docker exec $CONTAINER_NAME rm /tmp/restore.dump
rm -f $TEMP_FILE

# Reiniciar aplicação
log_info "Reiniciando aplicação..."
docker start pitstop-backend

# Aguardar aplicação subir
log_info "Aguardando aplicação inicializar..."
sleep 10

# Verificar saúde
if docker exec pitstop-backend wget --spider -q http://localhost:8080/actuator/health; then
    log_info "Aplicação reiniciada com sucesso!"
else
    log_warn "Aplicação pode estar com problemas. Verifique os logs:"
    log_warn "docker logs pitstop-backend"
fi

log_info "Restore finalizado!"
log_info "Backup de segurança mantido em: ${SAFETY_BACKUP}.gz"

exit 0
