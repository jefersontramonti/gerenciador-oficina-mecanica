# 🚀 Guia de Deploy - PitStop no Contabo VPS

Este guia detalha o processo completo de deploy do PitStop em um servidor Contabo VPS, incluindo PostgreSQL, Redis e Evolution API.

## 📋 Pré-requisitos

### Servidor Contabo

**Especificações mínimas:**
- **VPS M** ou superior
- **4+ vCores CPU**
- **8+ GB RAM** (12-16 GB recomendado para produção)
- **80+ GB SSD**
- **Ubuntu 22.04 LTS** ou Debian 12

### Domínio e DNS

- Domínio registrado apontando para o IP do servidor
- Registros DNS configurados:
  - `A` record: `seudominio.com` → IP do servidor
  - `A` record: `api.seudominio.com` → IP do servidor (opcional)

## 🔧 Instalação Inicial do Servidor

### 1. Acessar o servidor via SSH

```bash
ssh root@SEU_IP_SERVIDOR
```

### 2. Atualizar sistema e instalar dependências

```bash
# Atualizar pacotes
apt update && apt upgrade -y

# Instalar ferramentas essenciais
apt install -y curl wget git vim ufw fail2ban unattended-upgrades
```

### 3. Configurar firewall (UFW)

```bash
# Configurar regras
ufw allow 22/tcp      # SSH
ufw allow 80/tcp      # HTTP
ufw allow 443/tcp     # HTTPS
ufw allow 8081/tcp    # Evolution API (opcional - apenas se quiser acesso externo)

# Ativar firewall
ufw --force enable

# Verificar status
ufw status
```

### 4. Instalar Docker e Docker Compose

```bash
# Instalar Docker
curl -fsSL https://get.docker.com | sh

# Adicionar usuário ao grupo docker (opcional)
usermod -aG docker $USER

# Iniciar Docker
systemctl enable docker
systemctl start docker

# Verificar instalação
docker --version
docker compose version
```

### 5. Configurar swap (recomendado para 8GB RAM)

```bash
# Criar arquivo de swap de 4GB
fallocate -l 4G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile

# Tornar permanente
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# Verificar
free -h
```

## 📦 Deploy da Aplicação

### 1. Clonar repositório no servidor

```bash
# Criar diretório
mkdir -p /opt/pitstop
cd /opt/pitstop

# Clonar (substitua pela URL do seu repositório)
git clone https://github.com/seu-usuario/pitstop.git .
```

### 2. Configurar variáveis de ambiente

```bash
# Copiar exemplo
cp .env.example .env

# Editar com suas configurações
nano .env
```

**Gere chaves seguras:**

```bash
# JWT Secret (256 bits)
openssl rand -base64 64

# Evolution API Key
openssl rand -hex 32

# Database Password (32+ caracteres)
openssl rand -base64 32
```

**Exemplo de .env configurado:**

```env
# Database
DB_USER=pitstop
DB_PASSWORD=SuaSenhaSeguraAqui123!@#

# JWT
JWT_SECRET=sua_chave_jwt_256_bits_aqui
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Email (Mailtrap para testes)
MAIL_HOST=smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=seu_usuario
MAIL_PASSWORD=sua_senha

# Evolution API
EVOLUTION_SERVER_URL=https://seudominio.com:8081
EVOLUTION_API_KEY=sua_chave_evolution_api
```

### 3. Criar diretório para SSL (preparar para Certbot)

```bash
mkdir -p /opt/pitstop/ssl
chmod 755 /opt/pitstop/ssl
```

### 4. Build e iniciar serviços

```bash
cd /opt/pitstop

# Build das imagens
docker compose -f docker-compose.prod.yml build

# Iniciar serviços em background
docker compose -f docker-compose.prod.yml up -d

# Verificar status
docker compose -f docker-compose.prod.yml ps

# Ver logs
docker compose -f docker-compose.prod.yml logs -f
```

### 5. Verificar saúde dos serviços

```bash
# Backend
curl http://localhost:8080/actuator/health

# Frontend
curl http://localhost/health

# Evolution API
curl http://localhost:8081/health
```

## 🔒 Configurar SSL com Certbot (Let's Encrypt)

### 1. Instalar Certbot

```bash
apt install -y certbot
```

### 2. Parar Nginx temporariamente

```bash
docker stop pitstop-frontend
```

### 3. Obter certificados

```bash
# Para domínio principal
certbot certonly --standalone -d seudominio.com -d www.seudominio.com

# Para subdomínio Evolution API (opcional)
certbot certonly --standalone -d api.seudominio.com
```

### 4. Copiar certificados para o projeto

```bash
# Criar links simbólicos
ln -s /etc/letsencrypt/live/seudominio.com/fullchain.pem /opt/pitstop/ssl/cert.pem
ln -s /etc/letsencrypt/live/seudominio.com/privkey.pem /opt/pitstop/ssl/key.pem
```

### 5. Atualizar nginx.conf para HTTPS

Edite `frontend/nginx.conf` e adicione:

```nginx
server {
    listen 443 ssl http2;
    server_name seudominio.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    # SSL configs
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # ... resto da configuração
}

# Redirecionar HTTP para HTTPS
server {
    listen 80;
    server_name seudominio.com;
    return 301 https://$server_name$request_uri;
}
```

### 6. Reiniciar frontend

```bash
docker compose -f docker-compose.prod.yml restart frontend
```

### 7. Configurar renovação automática

```bash
# Testar renovação
certbot renew --dry-run

# Renovação automática já está configurada via systemd timer
systemctl status certbot.timer
```

## 🔄 Backups Automáticos

### 1. Configurar permissões dos scripts

```bash
chmod +x scripts/backup.sh
chmod +x scripts/restore.sh
```

### 2. Configurar cron para backups

```bash
# Editar crontab
crontab -e

# Adicionar tarefas
# Backup diário às 2h da manhã
0 2 * * * /opt/pitstop/scripts/backup.sh diario >> /var/log/pitstop-backup.log 2>&1

# Backup semanal aos domingos às 3h
0 3 * * 0 /opt/pitstop/scripts/backup.sh semanal >> /var/log/pitstop-backup.log 2>&1
```

### 3. Testar backup manual

```bash
cd /opt/pitstop
./scripts/backup.sh manual
```

### 4. Backup externo (recomendado)

Configure sincronização com cloud storage:

```bash
# Instalar rclone
curl https://rclone.org/install.sh | bash

# Configurar (exemplo: AWS S3, Google Drive, Backblaze)
rclone config

# Script para sync automático
cat > /opt/pitstop/scripts/sync-backups.sh << 'EOF'
#!/bin/bash
rclone sync /var/backups/pitstop remote:pitstop-backups --progress
EOF

chmod +x /opt/pitstop/scripts/sync-backups.sh

# Adicionar ao cron (após backup diário)
0 4 * * * /opt/pitstop/scripts/sync-backups.sh
```

## 📊 Monitoramento

### 1. Logs de aplicação

```bash
# Ver logs em tempo real
docker compose -f docker-compose.prod.yml logs -f backend

# Ver últimas 100 linhas
docker compose -f docker-compose.prod.yml logs --tail=100 backend

# Logs de todos os serviços
docker compose -f docker-compose.prod.yml logs -f
```

### 2. Monitorar recursos

```bash
# CPU, memória, disco
htop

# Docker stats
docker stats

# Espaço em disco
df -h

# Logs de sistema
journalctl -u docker -f
```

### 3. Health checks

```bash
# Script de monitoramento simples
cat > /opt/pitstop/scripts/healthcheck.sh << 'EOF'
#!/bin/bash

echo "=== PitStop Health Check ==="
echo "Backend: $(curl -s http://localhost:8080/actuator/health | jq -r .status)"
echo "Frontend: $(curl -s http://localhost/health)"
echo "Evolution: $(curl -s http://localhost:8081/health | jq -r .status)"
echo ""
docker compose -f /opt/pitstop/docker-compose.prod.yml ps
EOF

chmod +x /opt/pitstop/scripts/healthcheck.sh
```

## 🔄 Atualizações e Manutenção

### Atualizar aplicação

```bash
cd /opt/pitstop

# Backup antes de atualizar
./scripts/backup.sh manual

# Puxar últimas alterações
git pull

# Rebuild e restart
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d

# Verificar logs
docker compose -f docker-compose.prod.yml logs -f
```

### Reiniciar serviços específicos

```bash
# Apenas backend
docker compose -f docker-compose.prod.yml restart backend

# Apenas frontend
docker compose -f docker-compose.prod.yml restart frontend

# Todos os serviços
docker compose -f docker-compose.prod.yml restart
```

### Limpar recursos do Docker

```bash
# Remover containers parados
docker container prune -f

# Remover imagens não utilizadas
docker image prune -a -f

# Remover volumes órfãos (CUIDADO!)
docker volume prune -f

# Limpeza completa
docker system prune -a --volumes -f
```

## 🆘 Troubleshooting

### Serviço não inicia

```bash
# Ver logs detalhados
docker compose -f docker-compose.prod.yml logs backend

# Verificar configuração
docker compose -f docker-compose.prod.yml config

# Reiniciar do zero
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
```

### Banco de dados corrompido

```bash
# Restaurar último backup
./scripts/restore.sh /var/backups/pitstop/daily/pitstop_diario_YYYYMMDD.sql.gz
```

### Problema de memória

```bash
# Verificar uso
free -h
docker stats

# Aumentar swap
swapoff /swapfile
dd if=/dev/zero of=/swapfile bs=1G count=8
mkswap /swapfile
swapon /swapfile
```

### Certificado SSL expirado

```bash
# Renovar manualmente
certbot renew --force-renewal

# Reiniciar nginx
docker compose -f docker-compose.prod.yml restart frontend
```

## 💰 Estimativa de Custos Contabo

**VPS M (8 GB RAM, 4 vCores, 200 GB SSD):**
- €9,99/mês (~R$ 60/mês)

**VPS L (16 GB RAM, 6 vCores, 400 GB SSD) - Recomendado:**
- €14,99/mês (~R$ 90/mês)

**Custos adicionais:**
- Domínio: ~R$ 40/ano
- SSL: Grátis (Let's Encrypt)
- Backups externos (opcional): ~$5/mês (Backblaze B2)

**Total inicial:** ~R$ 60-90/mês + R$ 40/ano domínio

## ✅ Checklist de Produção

Antes de ir para produção, verifique:

- [ ] Senhas fortes configuradas no `.env`
- [ ] Firewall (UFW) ativado e configurado
- [ ] SSL/HTTPS configurado e funcionando
- [ ] Backups automáticos testados
- [ ] Monitoramento básico configurado
- [ ] Logs sendo rotacionados
- [ ] Swap configurado
- [ ] Fail2ban configurado para SSH
- [ ] Updates automáticos de segurança
- [ ] DNS configurado corretamente
- [ ] Email/WhatsApp de notificações testados

## 📚 Recursos Adicionais

- [Documentação Contabo](https://contabo.com/en/support/)
- [Docker Documentation](https://docs.docker.com/)
- [Evolution API Docs](https://doc.evolution-api.com/)
- [Let's Encrypt](https://letsencrypt.org/)

---

**Suporte:** Em caso de dúvidas, consulte a documentação ou abra uma issue no repositório.
