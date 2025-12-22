# 📁 Scripts de Deploy e Manutenção

Esta pasta contém scripts para facilitar o deploy e manutenção do PitStop em servidores Linux.

## 📜 Scripts Disponíveis

### 🚀 `install-server.sh`
**Instalação inicial do servidor VPS**

Configura automaticamente todo o ambiente necessário:
- Atualizações do sistema
- Docker e Docker Compose
- Firewall (UFW)
- Fail2ban para segurança SSH
- Certbot para SSL
- Swap de 4GB
- Diretórios do projeto

**Uso:**
```bash
# No servidor VPS (Ubuntu 22.04/24.04 ou Debian 12)
sudo bash install-server.sh
```

⚠️ **Atenção:** Execute apenas em servidores novos ou de teste. Revise o código antes de usar em produção.

---

### 💾 `backup.sh`
**Backup automático do banco de dados**

Faz backup completo do PostgreSQL (PitStop + Evolution API) com compressão.

**Uso:**
```bash
# Backup manual
./backup.sh manual

# Backup diário (usar no cron)
./backup.sh diario

# Backup semanal (usar no cron)
./backup.sh semanal
```

**Configurar no cron:**
```bash
crontab -e

# Adicionar:
0 2 * * * /opt/pitstop/scripts/backup.sh diario >> /var/log/pitstop-backup.log 2>&1
0 3 * * 0 /opt/pitstop/scripts/backup.sh semanal >> /var/log/pitstop-backup.log 2>&1
```

**Onde ficam os backups:**
- Diários: `/var/backups/pitstop/daily/` (mantidos por 30 dias)
- Semanais: `/var/backups/pitstop/weekly/` (mantidos por 12 semanas)
- Manuais: `/var/backups/pitstop/manual/` (mantidos indefinidamente)

---

### 🔄 `restore.sh`
**Restauração de backups**

Restaura um backup do banco de dados com segurança.

**Uso:**
```bash
./restore.sh /var/backups/pitstop/daily/pitstop_diario_20250116_020000.sql.gz
```

**Recursos de segurança:**
- Cria backup de segurança antes do restore
- Solicita confirmação explícita (digite "SIM")
- Para a aplicação durante o processo
- Reinicia automaticamente após conclusão
- Mantém backup de segurança em caso de falha

---

### 🗄️ `init-evolution-db.sql`
**Script de inicialização do banco Evolution API**

Executado automaticamente pelo PostgreSQL na primeira inicialização via Docker.

Cria o database `evolution` e configura permissões.

---

## 📖 Documentação

### `server-commands.md`
Guia completo de comandos úteis para gerenciar o servidor no dia a dia:
- Gerenciamento de containers
- Logs e monitoramento
- Backup e restore
- Atualizações
- Banco de dados
- Redis
- SSL/HTTPS
- Troubleshooting
- E muito mais!

**Acesse:** [server-commands.md](./server-commands.md)

---

## 🔐 Permissões

Todos os scripts `.sh` precisam de permissão de execução:

```bash
chmod +x scripts/*.sh
```

---

## 🛡️ Segurança

- **NUNCA** commite arquivos `.env` com valores reais
- Revise scripts antes de executar, especialmente em produção
- Execute backups regularmente (mínimo diário)
- Teste restores periodicamente
- Mantenha backups em local separado (cloud storage)

---

## 💡 Dicas

### Criar alias úteis

Adicione ao `~/.bashrc` no servidor:

```bash
alias pitstop-backup='cd /opt/pitstop && ./scripts/backup.sh manual'
alias pitstop-logs='docker compose -f /opt/pitstop/docker-compose.prod.yml logs -f'
alias pitstop-status='docker compose -f /opt/pitstop/docker-compose.prod.yml ps'
```

Depois: `source ~/.bashrc`

### Monitorar backups

```bash
# Ver últimos backups
ls -lht /var/backups/pitstop/daily/ | head -5

# Ver espaço usado
du -sh /var/backups/pitstop/*
```

### Automatizar sync com cloud

```bash
# Instalar rclone
curl https://rclone.org/install.sh | bash

# Configurar (S3, Google Drive, etc)
rclone config

# Sync automático após backup
echo '0 4 * * * rclone sync /var/backups/pitstop remote:pitstop-backups' | crontab -
```

---

## ❓ Ajuda

Para mais informações sobre deploy, consulte:
- [DEPLOY.md](../DEPLOY.md) - Guia completo de deploy
- [CLAUDE.md](../CLAUDE.md) - Documentação técnica do projeto
- [docker-compose.prod.yml](../docker-compose.prod.yml) - Configuração de produção

---

**Desenvolvido para PitStop** 🚗💨
