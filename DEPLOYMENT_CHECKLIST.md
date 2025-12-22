# ✅ Checklist de Deploy - PitStop

Use esta lista para garantir que nada foi esquecido durante o deploy em produção.

## 🔧 Pré-Deploy

### Servidor VPS
- [ ] VPS contratado (mínimo 8GB RAM, 4 vCores)
- [ ] Ubuntu 22.04+ ou Debian 12 instalado
- [ ] Acesso SSH configurado
- [ ] IP público do servidor anotado
- [ ] Firewall básico ativo

### Domínio e DNS
- [ ] Domínio registrado
- [ ] DNS configurado:
  - [ ] Registro A: `seudominio.com` → IP do servidor
  - [ ] Registro A: `www.seudominio.com` → IP do servidor
  - [ ] (Opcional) Registro A: `api.seudominio.com` → IP do servidor
- [ ] DNS propagado (verificar com `dig` ou `nslookup`)

### Contas e Credenciais
- [ ] Conta Mailtrap (desenvolvimento) ou AWS SES (produção)
- [ ] Chaves de API geradas:
  - [ ] JWT_SECRET (64 caracteres Base64)
  - [ ] EVOLUTION_API_KEY (32 caracteres hex)
  - [ ] DB_PASSWORD (32+ caracteres)
- [ ] Credenciais anotadas em local seguro (gerenciador de senhas)

## 📦 Instalação do Servidor

- [ ] Servidor atualizado (`apt update && apt upgrade`)
- [ ] Docker instalado e rodando
- [ ] Docker Compose instalado
- [ ] Firewall (UFW) configurado:
  - [ ] Porta 22 (SSH) aberta
  - [ ] Porta 80 (HTTP) aberta
  - [ ] Porta 443 (HTTPS) aberta
  - [ ] Porta 8081 (Evolution API) aberta (se necessário)
- [ ] Fail2ban instalado e ativo
- [ ] Swap configurado (4GB+)
- [ ] Diretórios criados:
  - [ ] `/opt/pitstop`
  - [ ] `/var/backups/pitstop/{daily,weekly,manual}`

## 🔐 Configuração de Segurança

- [ ] Senhas fortes configuradas:
  - [ ] Senha do servidor (root/sudo)
  - [ ] DB_PASSWORD
  - [ ] JWT_SECRET
  - [ ] EVOLUTION_API_KEY
- [ ] Autenticação SSH por chave (senha desabilitada)
- [ ] Fail2ban monitorando SSH
- [ ] Firewall bloqueando portas não utilizadas
- [ ] Usuário não-root criado para deploy

## 📝 Configuração do Projeto

- [ ] Repositório clonado em `/opt/pitstop`
- [ ] Arquivo `.env` criado (a partir de `.env.production.example`)
- [ ] Variáveis de ambiente configuradas:
  - [ ] DB_USER
  - [ ] DB_PASSWORD
  - [ ] JWT_SECRET
  - [ ] JWT_ACCESS_EXPIRATION
  - [ ] JWT_REFRESH_EXPIRATION
  - [ ] MAIL_HOST
  - [ ] MAIL_PORT
  - [ ] MAIL_USERNAME
  - [ ] MAIL_PASSWORD
  - [ ] EVOLUTION_SERVER_URL
  - [ ] EVOLUTION_API_KEY
- [ ] Arquivo `.env` com permissões corretas (`chmod 600 .env`)
- [ ] Scripts com permissão de execução (`chmod +x scripts/*.sh`)

## 🚀 Deploy da Aplicação

- [ ] Build das imagens executado
- [ ] Containers iniciados (`docker compose -f docker-compose.prod.yml up -d`)
- [ ] Todos os containers rodando:
  - [ ] pitstop-postgres (healthy)
  - [ ] pitstop-redis (healthy)
  - [ ] pitstop-backend (healthy)
  - [ ] pitstop-frontend (rodando)
  - [ ] evolution-api (healthy)

## ✅ Verificação de Saúde

- [ ] Backend respondendo:
  - [ ] `curl http://localhost:8080/actuator/health` retorna `UP`
  - [ ] API acessível em `/api`
- [ ] Frontend respondendo:
  - [ ] `curl http://localhost/health` retorna `OK`
  - [ ] Aplicação carrega no navegador
- [ ] PostgreSQL:
  - [ ] Containers conectando com sucesso
  - [ ] Migrations executadas (verificar logs do backend)
- [ ] Redis:
  - [ ] `docker exec pitstop-redis redis-cli ping` retorna `PONG`
- [ ] Evolution API:
  - [ ] `curl http://localhost:8081/health` responde
  - [ ] Acessível via navegador

## 🔒 SSL/HTTPS

- [ ] Certbot instalado
- [ ] Containers parados temporariamente (frontend)
- [ ] Certificado SSL obtido:
  - [ ] `certbot certonly --standalone -d seudominio.com`
- [ ] Links simbólicos criados:
  - [ ] `ssl/cert.pem` → certificado
  - [ ] `ssl/key.pem` → chave privada
- [ ] `nginx.conf` configurado para HTTPS
- [ ] Frontend reiniciado
- [ ] HTTPS funcionando:
  - [ ] `https://seudominio.com` carrega
  - [ ] HTTP redireciona para HTTPS
  - [ ] Cadeado verde no navegador
- [ ] Renovação automática testada:
  - [ ] `certbot renew --dry-run` executa sem erros

## 💾 Backups

- [ ] Script de backup testado:
  - [ ] `./scripts/backup.sh manual` executa
  - [ ] Arquivo gerado em `/var/backups/pitstop/manual/`
  - [ ] Arquivo compactado (.gz)
- [ ] Cron configurado:
  - [ ] Backup diário (2h): `0 2 * * * /opt/pitstop/scripts/backup.sh diario`
  - [ ] Backup semanal (domingo 3h): `0 3 * * 0 /opt/pitstop/scripts/backup.sh semanal`
- [ ] Script de restore testado:
  - [ ] `./scripts/restore.sh <backup>` funciona
- [ ] (Opcional) Sync com cloud configurado:
  - [ ] rclone instalado e configurado
  - [ ] Sync automático testado

## 📊 Monitoramento

- [ ] Logs acessíveis:
  - [ ] `docker compose logs -f` funciona
  - [ ] Logs persistentes configurados
- [ ] Health checks respondendo:
  - [ ] Backend: `/actuator/health`
  - [ ] Frontend: `/health`
  - [ ] Evolution: `/health`
- [ ] Script de health check criado e testado
- [ ] (Opcional) Grafana + Prometheus configurados
- [ ] (Opcional) Alertas configurados (email/Telegram)

## 🔄 Atualizações Automáticas

- [ ] Atualizações de segurança automáticas:
  - [ ] `unattended-upgrades` instalado e configurado
- [ ] Renovação SSL automática:
  - [ ] Certbot timer ativo (`systemctl status certbot.timer`)

## 🧪 Testes Finais

- [ ] Login na aplicação funciona
- [ ] Criar cliente funciona
- [ ] Criar veículo funciona
- [ ] Criar ordem de serviço funciona
- [ ] Dashboard carrega com dados
- [ ] (Opcional) WhatsApp Evolution API conecta
- [ ] Upload de arquivos funciona
- [ ] Geração de PDF funciona
- [ ] Notificações em tempo real funcionam (WebSocket)
- [ ] Logout funciona

## 📱 Integração Evolution API

- [ ] Container rodando
- [ ] API acessível via navegador (porta 8081)
- [ ] Instância criada:
  - [ ] POST `/instance/create` executado
- [ ] QR Code gerado
- [ ] WhatsApp conectado
- [ ] Mensagens de teste enviadas
- [ ] Webhooks configurados (se necessário)

## 📋 Documentação

- [ ] `.env.example` atualizado (sem valores reais)
- [ ] Documentação de deploy atualizada
- [ ] Credenciais salvas em gerenciador de senhas
- [ ] Informações de acesso documentadas:
  - [ ] URL da aplicação
  - [ ] URL da Evolution API
  - [ ] Credenciais de admin padrão
  - [ ] IP do servidor
- [ ] Equipe treinada no uso básico

## 🚨 Plano de Contingência

- [ ] Backup manual recente criado antes de ir ao ar
- [ ] Procedimento de rollback documentado
- [ ] Contato de suporte técnico disponível
- [ ] Horário de manutenção comunicado aos usuários

## ✅ Go Live

- [ ] Usuário admin criado
- [ ] Dados iniciais inseridos (seed)
- [ ] Testes de aceitação realizados
- [ ] Monitoramento ativo
- [ ] Equipe de suporte em standby
- [ ] Comunicado de lançamento enviado

---

## 📊 Checklist Rápido Pós-Deploy

Execute estes comandos para validação final:

```bash
# Status dos containers
docker compose -f /opt/pitstop/docker-compose.prod.yml ps

# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost/health
curl http://localhost:8081/health

# SSL
curl -I https://seudominio.com

# Logs (verificar erros)
docker compose -f /opt/pitstop/docker-compose.prod.yml logs --tail=50

# Recursos do sistema
free -h
df -h
docker stats --no-stream

# Firewall
sudo ufw status

# Backups
ls -lh /var/backups/pitstop/manual/
```

---

## 🆘 Contatos de Emergência

**Servidor:**
- IP: `___________________`
- SSH: `ssh usuario@___________________`

**DNS:**
- Provedor: `___________________`
- Login: `___________________`

**Suporte:**
- Contato 1: `___________________`
- Contato 2: `___________________`

---

**Data do Deploy:** ___/___/_____
**Responsável:** _________________
**Status:** [ ] Em Progresso [ ] Concluído [ ] Requer Atenção
