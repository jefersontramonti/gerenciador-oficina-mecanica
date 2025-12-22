# 🛠️ Comandos Úteis do Servidor - PitStop

Guia rápido de comandos para gerenciar o PitStop no servidor Contabo.

## 🚀 Gerenciamento de Containers

### Iniciar todos os serviços
```bash
cd /opt/pitstop
docker compose -f docker-compose.prod.yml up -d
```

### Parar todos os serviços
```bash
docker compose -f docker-compose.prod.yml down
```

### Reiniciar serviços específicos
```bash
# Backend
docker compose -f docker-compose.prod.yml restart backend

# Frontend
docker compose -f docker-compose.prod.yml restart frontend

# Database
docker compose -f docker-compose.prod.yml restart postgres

# Evolution API
docker compose -f docker-compose.prod.yml restart evolution-api
```

### Ver status dos containers
```bash
docker compose -f docker-compose.prod.yml ps
```

### Ver uso de recursos
```bash
docker stats
```

## 📊 Logs e Monitoramento

### Ver logs em tempo real
```bash
# Todos os serviços
docker compose -f docker-compose.prod.yml logs -f

# Apenas backend
docker compose -f docker-compose.prod.yml logs -f backend

# Últimas 100 linhas
docker compose -f docker-compose.prod.yml logs --tail=100 backend
```

### Health checks
```bash
# Backend
curl http://localhost:8080/actuator/health

# Frontend
curl http://localhost/health

# Evolution API
curl http://localhost:8081/health
```

### Métricas do sistema
```bash
# CPU e memória
htop

# Espaço em disco
df -h

# Uso de memória
free -h

# Processos Docker
docker ps
```

## 💾 Backup e Restore

### Backup manual
```bash
cd /opt/pitstop
./scripts/backup.sh manual
```

### Backup agendado (verificar cron)
```bash
crontab -l
```

### Restaurar backup
```bash
./scripts/restore.sh /var/backups/pitstop/daily/pitstop_diario_YYYYMMDD.sql.gz
```

### Listar backups
```bash
ls -lh /var/backups/pitstop/daily/
ls -lh /var/backups/pitstop/weekly/
```

## 🔄 Atualizações

### Atualizar código (após git push)
```bash
cd /opt/pitstop
git pull
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

### Atualizar apenas backend
```bash
docker compose -f docker-compose.prod.yml build backend
docker compose -f docker-compose.prod.yml up -d backend
```

### Atualizar apenas frontend
```bash
cd frontend
npm run build  # Se fez alterações locais
cd ..
docker compose -f docker-compose.prod.yml build frontend
docker compose -f docker-compose.prod.yml up -d frontend
```

## 🗄️ Gerenciamento do Banco de Dados

### Acessar PostgreSQL via psql
```bash
docker exec -it pitstop-postgres psql -U pitstop -d pitstop
```

### Comandos SQL úteis
```sql
-- Ver tamanho do banco
SELECT pg_size_pretty(pg_database_size('pitstop'));

-- Listar tabelas
\dt

-- Ver conexões ativas
SELECT * FROM pg_stat_activity;

-- Limpar conexões idle
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
AND state_change < current_timestamp - interval '5 minutes';
```

### Executar query direto do shell
```bash
docker exec pitstop-postgres psql -U pitstop -d pitstop -c "SELECT COUNT(*) FROM clientes;"
```

## 🔴 Redis

### Acessar Redis CLI
```bash
docker exec -it pitstop-redis redis-cli
```

### Comandos Redis úteis
```bash
# Ver todas as chaves
KEYS *

# Ver informações do servidor
INFO

# Limpar cache
FLUSHDB

# Ver uso de memória
INFO memory
```

## 🔒 SSL/HTTPS

### Renovar certificado SSL
```bash
certbot renew

# Forçar renovação
certbot renew --force-renewal
```

### Verificar validade do certificado
```bash
certbot certificates
```

### Reiniciar após renovação
```bash
docker compose -f docker-compose.prod.yml restart frontend
```

## 🔥 Firewall (UFW)

### Ver status
```bash
ufw status
```

### Adicionar regra
```bash
ufw allow 3000/tcp
```

### Remover regra
```bash
ufw delete allow 3000/tcp
```

## 🧹 Limpeza e Manutenção

### Limpar containers parados
```bash
docker container prune -f
```

### Limpar imagens não utilizadas
```bash
docker image prune -a -f
```

### Limpar volumes órfãos (CUIDADO!)
```bash
docker volume prune -f
```

### Limpeza completa do Docker
```bash
docker system prune -a --volumes -f
```

### Limpar logs antigos
```bash
# Logs do Docker
truncate -s 0 /var/lib/docker/containers/*/*-json.log

# Logs do sistema
journalctl --vacuum-time=7d
```

## 📦 Evolution API

### Ver QR Code da instância
```bash
# Acessar logs para ver QR Code
docker compose -f docker-compose.prod.yml logs evolution-api | grep QR
```

### Criar nova instância
```bash
curl -X POST http://localhost:8081/instance/create \
  -H "Content-Type: application/json" \
  -H "apikey: SUA_EVOLUTION_API_KEY" \
  -d '{
    "instanceName": "pitstop-main",
    "qrcode": true
  }'
```

### Listar instâncias
```bash
curl http://localhost:8081/instance/fetchInstances \
  -H "apikey: SUA_EVOLUTION_API_KEY"
```

## 🚨 Troubleshooting

### Container não inicia
```bash
# Ver logs detalhados
docker compose -f docker-compose.prod.yml logs backend

# Verificar configuração
docker compose -f docker-compose.prod.yml config

# Forçar recreação
docker compose -f docker-compose.prod.yml up -d --force-recreate backend
```

### Erro de memória
```bash
# Ver uso atual
free -h

# Limpar cache do sistema
sync; echo 3 > /proc/sys/vm/drop_caches
```

### Porta em uso
```bash
# Ver o que está usando a porta
netstat -tulpn | grep :8080

# Matar processo
kill -9 <PID>
```

### Database connection refused
```bash
# Verificar se PostgreSQL está rodando
docker ps | grep postgres

# Ver logs do PostgreSQL
docker logs pitstop-postgres

# Reiniciar PostgreSQL
docker compose -f docker-compose.prod.yml restart postgres
```

## 📊 Relatórios Úteis

### Espaço usado por volumes Docker
```bash
docker system df -v
```

### Top 10 containers por uso de memória
```bash
docker stats --no-stream --format "table {{.Container}}\t{{.MemUsage}}" | sort -k 2 -h | tail -10
```

### Ver uptime dos containers
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

## 🔐 Segurança

### Ver tentativas de login SSH falhas
```bash
grep "Failed password" /var/log/auth.log | tail -20
```

### Ver IPs bloqueados pelo Fail2ban
```bash
fail2ban-client status sshd
```

### Atualizar sistema
```bash
apt update && apt upgrade -y
```

## 💡 Dicas

### Criar alias úteis
Adicione ao `~/.bashrc`:

```bash
# Aliases PitStop
alias pitstop-logs='docker compose -f /opt/pitstop/docker-compose.prod.yml logs -f'
alias pitstop-status='docker compose -f /opt/pitstop/docker-compose.prod.yml ps'
alias pitstop-restart='docker compose -f /opt/pitstop/docker-compose.prod.yml restart'
alias pitstop-backup='cd /opt/pitstop && ./scripts/backup.sh manual'
alias pitstop-health='curl -s http://localhost:8080/actuator/health | jq'
```

Depois execute: `source ~/.bashrc`

### Monitoramento contínuo
```bash
# Terminal 1: logs em tempo real
watch -n 2 'docker stats --no-stream'

# Terminal 2: health checks
watch -n 10 'curl -s http://localhost:8080/actuator/health | jq'
```

---

**Nota:** Sempre teste comandos destrutivos (prune, down, etc.) em ambiente de desenvolvimento primeiro!
