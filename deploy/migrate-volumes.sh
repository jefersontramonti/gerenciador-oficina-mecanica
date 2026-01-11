#!/bin/bash
#===============================================================================
# Script para migrar volumes existentes para volumes externos
# Execute este script NO VPS antes do próximo deploy
# Uso: sudo bash migrate-volumes.sh
#===============================================================================

set -e

echo "=========================================="
echo "  MIGRAÇÃO DE VOLUMES - PITSTOP"
echo "=========================================="

cd /opt/pitstop

# Lista todos os volumes existentes relacionados ao pitstop
echo ""
echo "=== Volumes Docker existentes ==="
docker volume ls | grep -E "(pitstop|postgres|redis)" || echo "Nenhum volume encontrado"

# Identifica o volume do postgres atual
echo ""
echo "=== Identificando volume do PostgreSQL ==="

# Possíveis nomes do volume
POSSIBLE_PG_VOLUMES=(
  "pitstop_postgres_data"
  "opt_pitstop_postgres_data"
  "pitstop-postgres_data"
  "postgres_data"
)

CURRENT_PG_VOLUME=""
for vol in "${POSSIBLE_PG_VOLUMES[@]}"; do
  if docker volume ls --format '{{.Name}}' | grep -q "^${vol}$"; then
    CURRENT_PG_VOLUME=$vol
    echo "Volume PostgreSQL encontrado: $vol"
    break
  fi
done

# Verifica volume em uso pelo container
if [ -z "$CURRENT_PG_VOLUME" ]; then
  if docker ps --format '{{.Names}}' | grep -q pitstop-postgres; then
    CURRENT_PG_VOLUME=$(docker inspect pitstop-postgres --format '{{range .Mounts}}{{if eq .Destination "/var/lib/postgresql/data"}}{{.Name}}{{end}}{{end}}' 2>/dev/null || true)
    if [ -n "$CURRENT_PG_VOLUME" ]; then
      echo "Volume PostgreSQL em uso: $CURRENT_PG_VOLUME"
    fi
  fi
fi

# Identifica o volume do Redis atual
echo ""
echo "=== Identificando volume do Redis ==="

POSSIBLE_REDIS_VOLUMES=(
  "pitstop_redis_data"
  "opt_pitstop_redis_data"
  "pitstop-redis_data"
  "redis_data"
)

CURRENT_REDIS_VOLUME=""
for vol in "${POSSIBLE_REDIS_VOLUMES[@]}"; do
  if docker volume ls --format '{{.Name}}' | grep -q "^${vol}$"; then
    CURRENT_REDIS_VOLUME=$vol
    echo "Volume Redis encontrado: $vol"
    break
  fi
done

# Verifica volume em uso pelo container
if [ -z "$CURRENT_REDIS_VOLUME" ]; then
  if docker ps --format '{{.Names}}' | grep -q pitstop-redis; then
    CURRENT_REDIS_VOLUME=$(docker inspect pitstop-redis --format '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Name}}{{end}}{{end}}' 2>/dev/null || true)
    if [ -n "$CURRENT_REDIS_VOLUME" ]; then
      echo "Volume Redis em uso: $CURRENT_REDIS_VOLUME"
    fi
  fi
fi

# Verifica se já existem os volumes externos corretos
echo ""
echo "=== Verificando volumes externos ==="

NEW_PG_VOLUME="pitstop_postgres_data"
NEW_REDIS_VOLUME="pitstop_redis_data"

# Se o volume atual já é o correto, não precisa migrar
if [ "$CURRENT_PG_VOLUME" = "$NEW_PG_VOLUME" ]; then
  echo "PostgreSQL já está usando o volume correto: $NEW_PG_VOLUME"
  PG_NEEDS_MIGRATION=false
else
  PG_NEEDS_MIGRATION=true
  echo "PostgreSQL precisa migrar de '$CURRENT_PG_VOLUME' para '$NEW_PG_VOLUME'"
fi

if [ "$CURRENT_REDIS_VOLUME" = "$NEW_REDIS_VOLUME" ]; then
  echo "Redis já está usando o volume correto: $NEW_REDIS_VOLUME"
  REDIS_NEEDS_MIGRATION=false
else
  REDIS_NEEDS_MIGRATION=true
  echo "Redis precisa migrar de '$CURRENT_REDIS_VOLUME' para '$NEW_REDIS_VOLUME'"
fi

# Se nenhuma migração é necessária, sai
if [ "$PG_NEEDS_MIGRATION" = false ] && [ "$REDIS_NEEDS_MIGRATION" = false ]; then
  echo ""
  echo "=========================================="
  echo "  NENHUMA MIGRAÇÃO NECESSÁRIA!"
  echo "  Os volumes já estão configurados corretamente."
  echo "=========================================="
  exit 0
fi

# Confirmação antes de prosseguir
echo ""
echo "=========================================="
echo "  ATENÇÃO: Este script irá:"
if [ "$PG_NEEDS_MIGRATION" = true ]; then
  echo "  - Copiar dados do PostgreSQL para novo volume"
fi
if [ "$REDIS_NEEDS_MIGRATION" = true ]; then
  echo "  - Copiar dados do Redis para novo volume"
fi
echo "  - Parar os containers temporariamente"
echo "=========================================="
echo ""
read -p "Deseja continuar? (s/N): " confirm
if [ "$confirm" != "s" ] && [ "$confirm" != "S" ]; then
  echo "Operação cancelada."
  exit 1
fi

# Faz backup antes de migrar
echo ""
echo "=== Criando backup de segurança ==="
BACKUP_FILE="/opt/pitstop/backups/pitstop_pre_migration_$(date +%Y%m%d_%H%M%S).sql.gz"
mkdir -p /opt/pitstop/backups

if docker ps --format '{{.Names}}' | grep -q pitstop-postgres; then
  docker exec pitstop-postgres pg_dump -U pitstop pitstop_db 2>/dev/null | gzip > "$BACKUP_FILE"
  echo "Backup criado: $BACKUP_FILE"
  ls -lh "$BACKUP_FILE"
fi

# Para os containers
echo ""
echo "=== Parando containers ==="
docker compose -f docker-compose.prod.yml stop || docker-compose -f docker-compose.prod.yml stop || true

# Migra PostgreSQL
if [ "$PG_NEEDS_MIGRATION" = true ] && [ -n "$CURRENT_PG_VOLUME" ]; then
  echo ""
  echo "=== Migrando volume do PostgreSQL ==="

  # Cria o novo volume se não existir
  if ! docker volume ls --format '{{.Name}}' | grep -q "^${NEW_PG_VOLUME}$"; then
    docker volume create "$NEW_PG_VOLUME"
    echo "Volume criado: $NEW_PG_VOLUME"
  fi

  # Copia os dados usando um container temporário
  echo "Copiando dados de $CURRENT_PG_VOLUME para $NEW_PG_VOLUME..."
  docker run --rm \
    -v "${CURRENT_PG_VOLUME}:/source:ro" \
    -v "${NEW_PG_VOLUME}:/dest" \
    alpine sh -c "cp -av /source/. /dest/"

  echo "Migração do PostgreSQL concluída!"
fi

# Migra Redis
if [ "$REDIS_NEEDS_MIGRATION" = true ] && [ -n "$CURRENT_REDIS_VOLUME" ]; then
  echo ""
  echo "=== Migrando volume do Redis ==="

  # Cria o novo volume se não existir
  if ! docker volume ls --format '{{.Name}}' | grep -q "^${NEW_REDIS_VOLUME}$"; then
    docker volume create "$NEW_REDIS_VOLUME"
    echo "Volume criado: $NEW_REDIS_VOLUME"
  fi

  # Copia os dados usando um container temporário
  echo "Copiando dados de $CURRENT_REDIS_VOLUME para $NEW_REDIS_VOLUME..."
  docker run --rm \
    -v "${CURRENT_REDIS_VOLUME}:/source:ro" \
    -v "${NEW_REDIS_VOLUME}:/dest" \
    alpine sh -c "cp -av /source/. /dest/"

  echo "Migração do Redis concluída!"
fi

# Verifica os novos volumes
echo ""
echo "=== Verificando novos volumes ==="
docker volume ls | grep pitstop

echo ""
echo "=========================================="
echo "  MIGRAÇÃO CONCLUÍDA COM SUCESSO!"
echo "=========================================="
echo ""
echo "Os containers podem ser reiniciados com:"
echo "  docker compose -f docker-compose.prod.yml up -d"
echo ""
echo "NOTA: Os volumes antigos ainda existem."
echo "Após confirmar que tudo funciona, você pode removê-los com:"
if [ "$PG_NEEDS_MIGRATION" = true ] && [ -n "$CURRENT_PG_VOLUME" ]; then
  echo "  docker volume rm $CURRENT_PG_VOLUME"
fi
if [ "$REDIS_NEEDS_MIGRATION" = true ] && [ -n "$CURRENT_REDIS_VOLUME" ]; then
  echo "  docker volume rm $CURRENT_REDIS_VOLUME"
fi
