# Variáveis de Ambiente - PitStop + Evolution API

## Visão Geral

```
/opt/pitstop/.env   ← Arquivo principal de configuração
```

---

## 1. DOMÍNIOS E URLs

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `DOMAIN` | Domínio principal do PitStop | `pitstop.empresa.com` |
| `EVOLUTION_DOMAIN` | Domínio do Evolution API (WhatsApp) | `whatsapp.empresa.com` |
| `SSL_EMAIL` | Email para Let's Encrypt | `admin@empresa.com` |
| `APP_FRONTEND_URL` | URL completa do frontend | `https://pitstop.empresa.com` |
| `APP_BASE_URL` | URL da API (usado em links de email) | `https://pitstop.empresa.com/api` |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas para CORS | `https://pitstop.empresa.com` |

---

## 2. POSTGRESQL - PITSTOP

| Variável | Descrição | Valor Padrão |
|----------|-----------|--------------|
| `POSTGRES_PITSTOP_HOST` | Hostname do container | `postgres-pitstop` |
| `POSTGRES_PITSTOP_PORT` | Porta interna | `5432` |
| `POSTGRES_PITSTOP_DB` | Nome do banco | `pitstop_db` |
| `POSTGRES_PITSTOP_USER` | Usuário do banco | `pitstop` |
| `POSTGRES_PITSTOP_PASSWORD` | Senha do banco | *gerada automaticamente* |
| `DATABASE_URL` | JDBC URL completa | `jdbc:postgresql://postgres-pitstop:5432/pitstop_db` |
| `DATABASE_USERNAME` | Mesmo que POSTGRES_PITSTOP_USER | `pitstop` |
| `DATABASE_PASSWORD` | Mesmo que POSTGRES_PITSTOP_PASSWORD | *gerada* |

---

## 3. POSTGRESQL - EVOLUTION

| Variável | Descrição | Valor Padrão |
|----------|-----------|--------------|
| `POSTGRES_EVOLUTION_HOST` | Hostname do container | `postgres-evolution` |
| `POSTGRES_EVOLUTION_PORT` | Porta interna (mapeada 5433:5432) | `5433` |
| `POSTGRES_EVOLUTION_DB` | Nome do banco | `evolution_db` |
| `POSTGRES_EVOLUTION_USER` | Usuário do banco | `evolution` |
| `POSTGRES_EVOLUTION_PASSWORD` | Senha do banco | *gerada automaticamente* |

---

## 4. REDIS

| Variável | Descrição | Valor Padrão |
|----------|-----------|--------------|
| `REDIS_HOST` | Hostname do container | `redis` |
| `REDIS_PORT` | Porta | `6379` |
| `REDIS_PASSWORD` | Senha do Redis | *gerada automaticamente* |

---

## 5. SEGURANÇA / JWT

| Variável | Descrição | Observação |
|----------|-----------|------------|
| `JWT_SECRET` | Chave secreta para assinar tokens | 64 caracteres alfanuméricos |

**Como gerar manualmente:**
```bash
openssl rand -base64 64 | tr -dc 'a-zA-Z0-9' | head -c 64
```

---

## 6. EVOLUTION API (WhatsApp)

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `EVOLUTION_API_URL` | URL do Evolution | `https://whatsapp.empresa.com` |
| `EVOLUTION_API_KEY` | API Key para autenticação | *gerada automaticamente* |
| `AUTHENTICATION_API_KEY` | Mesma que EVOLUTION_API_KEY | *gerada* |

---

## 7. DADOS DA OFICINA

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `PITSTOP_OFICINA_NOME` | Nome fantasia | `Auto Center Silva` |
| `PITSTOP_OFICINA_CNPJ` | CNPJ formatado | `00.000.000/0001-00` |
| `PITSTOP_OFICINA_TELEFONE` | Telefone de contato | `(11) 98765-4321` |
| `PITSTOP_OFICINA_EMAIL` | Email da oficina | `contato@oficina.com` |

---

## 8. SPRING BOOT

| Variável | Descrição | Valores |
|----------|-----------|---------|
| `SPRING_PROFILES_ACTIVE` | Perfil ativo | `prod` ou `dev` |
| `LOG_LEVEL` | Nível de log | `INFO`, `DEBUG`, `WARN`, `ERROR` |

---

## 9. TELEGRAM (Opcional)

| Variável | Descrição | Como obter |
|----------|-----------|------------|
| `TELEGRAM_BOT_TOKEN` | Token do bot | [@BotFather](https://t.me/BotFather) |
| `TELEGRAM_CHAT_ID` | ID do chat/grupo | [@userinfobot](https://t.me/userinfobot) |

**Passo a passo:**
1. Abra o Telegram e busque `@BotFather`
2. Envie `/newbot` e siga as instruções
3. Copie o token gerado
4. Para Chat ID, envie uma mensagem para `@userinfobot`

---

## 10. EMAIL SMTP (Opcional)

| Variável | Descrição | Exemplo Gmail |
|----------|-----------|---------------|
| `MAIL_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Porta | `587` |
| `MAIL_USERNAME` | Email | `seu@gmail.com` |
| `MAIL_PASSWORD` | Senha de App | *não é a senha normal* |
| `MAIL_FROM_NAME` | Nome remetente | `PitStop` |

**Gmail - Senha de App:**
1. Acesse https://myaccount.google.com/apppasswords
2. Ative verificação em 2 etapas se não tiver
3. Gere uma "Senha de App" para "Outro (Nome personalizado)"
4. Use essa senha de 16 caracteres

**Outros provedores:**
| Provedor | Host | Porta |
|----------|------|-------|
| Gmail | smtp.gmail.com | 587 |
| Outlook | smtp-mail.outlook.com | 587 |
| Yahoo | smtp.mail.yahoo.com | 587 |
| SendGrid | smtp.sendgrid.net | 587 |

---

## 11. MERCADO PAGO (Opcional)

| Variável | Descrição | Como obter |
|----------|-----------|------------|
| `MERCADOPAGO_ACCESS_TOKEN` | Token de acesso | [Developers](https://www.mercadopago.com.br/developers/panel) |
| `MERCADOPAGO_PUBLIC_KEY` | Chave pública | [Developers](https://www.mercadopago.com.br/developers/panel) |
| `MERCADOPAGO_WEBHOOK_SECRET` | Secret para validar webhooks | [Developers > Webhooks](https://www.mercadopago.com.br/developers/panel/app) |

**Passo a passo:**
1. Acesse https://www.mercadopago.com.br/developers/panel
2. Vá em "Credenciais"
3. Use credenciais de "Produção" para ambiente real
4. Para Webhook Secret:
   - Vá em "Integrações" > "Webhooks"
   - Configure a URL: `https://SEU_DOMINIO/api/webhooks/mercadopago`
   - Copie o "Secret de assinatura" gerado

---

## 12. RATE LIMITING E SEGURANÇA

| Variável | Descrição | Valor Padrão |
|----------|-----------|--------------|
| `RATELIMIT_ORCAMENTO_MAX` | Máx. requisições de orçamento por IP/hora | `20` |
| `RATELIMIT_ORCAMENTO_WINDOW` | Janela de tempo em segundos | `3600` |
| `RATELIMIT_WEBHOOK_MAX` | Máx. webhooks por IP/hora | `1000` |
| `RATELIMIT_WEBHOOK_WINDOW` | Janela de tempo em segundos | `3600` |
| `RATELIMIT_PASSWORD_RESET_MAX` | Máx. resets de senha por IP/hora | `5` |
| `RATELIMIT_PASSWORD_RESET_WINDOW` | Janela de tempo em segundos | `3600` |

**Sobre Rate Limiting:**
- Protege endpoints públicos contra ataques de força bruta
- Valores padrão são adequados para maioria dos casos
- Ajuste apenas se tiver problemas com falsos positivos

**Endpoints protegidos:**
- `/api/public/orcamento/*` - Aprovação de orçamentos
- `/api/webhooks/*` - Webhooks de integrações externas
- `/api/auth/forgot-password` - Reset de senha

---

## 13. ANTHROPIC / IA (Opcional)

| Variável | Descrição | Como obter |
|----------|-----------|------------|
| `ANTHROPIC_API_KEY` | API Key da Anthropic | [Console](https://console.anthropic.com/) |

---

## Arquivo .env Completo

```bash
#===============================================================================
# PitStop + Evolution API - Variáveis de Ambiente
#===============================================================================

#--- DOMÍNIOS ---
DOMAIN=pitstop.empresa.com
EVOLUTION_DOMAIN=whatsapp.empresa.com
SSL_EMAIL=admin@empresa.com

#--- URLs ---
APP_FRONTEND_URL=https://pitstop.empresa.com
APP_BASE_URL=https://pitstop.empresa.com/api
CORS_ALLOWED_ORIGINS=https://pitstop.empresa.com

#--- POSTGRESQL PITSTOP ---
POSTGRES_PITSTOP_HOST=postgres-pitstop
POSTGRES_PITSTOP_PORT=5432
POSTGRES_PITSTOP_DB=pitstop_db
POSTGRES_PITSTOP_USER=pitstop
POSTGRES_PITSTOP_PASSWORD=SuaSenhaForteAqui123
DATABASE_URL=jdbc:postgresql://postgres-pitstop:5432/pitstop_db
DATABASE_USERNAME=pitstop
DATABASE_PASSWORD=SuaSenhaForteAqui123

#--- POSTGRESQL EVOLUTION ---
POSTGRES_EVOLUTION_HOST=postgres-evolution
POSTGRES_EVOLUTION_PORT=5433
POSTGRES_EVOLUTION_DB=evolution_db
POSTGRES_EVOLUTION_USER=evolution
POSTGRES_EVOLUTION_PASSWORD=OutraSenhaForte456

#--- REDIS ---
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=SenhaRedis789

#--- JWT ---
JWT_SECRET=ChaveSecretaDe64CaracteresAlfanumericosParaAssinarTokensJWT123

#--- EVOLUTION API ---
EVOLUTION_API_URL=https://whatsapp.empresa.com
EVOLUTION_API_KEY=SuaApiKeyEvolution32Chars
AUTHENTICATION_API_KEY=SuaApiKeyEvolution32Chars

#--- OFICINA ---
PITSTOP_OFICINA_NOME="Auto Center Silva"
PITSTOP_OFICINA_CNPJ="12.345.678/0001-90"
PITSTOP_OFICINA_TELEFONE="(11) 98765-4321"
PITSTOP_OFICINA_EMAIL="contato@autocentro.com"

#--- SPRING ---
SPRING_PROFILES_ACTIVE=prod
LOG_LEVEL=INFO

#--- TELEGRAM (opcional) ---
#TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
#TELEGRAM_CHAT_ID=-1001234567890

#--- EMAIL SMTP (opcional) ---
#MAIL_HOST=smtp.gmail.com
#MAIL_PORT=587
#MAIL_USERNAME=seu@gmail.com
#MAIL_PASSWORD=xxxx xxxx xxxx xxxx
#MAIL_FROM_NAME=PitStop

#--- MERCADO PAGO (opcional) ---
#MERCADOPAGO_ACCESS_TOKEN=APP_USR-123456-...
#MERCADOPAGO_PUBLIC_KEY=APP_USR-...
#MERCADOPAGO_WEBHOOK_SECRET=SuaChaveSecretaDeWebhook

#--- ANTHROPIC (opcional) ---
#ANTHROPIC_API_KEY=sk-ant-...

#--- RATE LIMITING (opcional - valores padrão já estão bons) ---
#RATELIMIT_ORCAMENTO_MAX=20
#RATELIMIT_ORCAMENTO_WINDOW=3600
#RATELIMIT_WEBHOOK_MAX=1000
#RATELIMIT_WEBHOOK_WINDOW=3600
#RATELIMIT_PASSWORD_RESET_MAX=5
#RATELIMIT_PASSWORD_RESET_WINDOW=3600
```

---

## Comandos Úteis

### Ver variáveis atuais
```bash
cat /opt/pitstop/.env
```

### Editar variáveis
```bash
nano /opt/pitstop/.env
```

### Recarregar após alteração
```bash
cd /opt/pitstop
docker compose down
docker compose up -d
```

### Verificar se variável está sendo lida
```bash
docker exec pitstop-backend printenv | grep JWT
```

---

## Segurança

⚠️ **IMPORTANTE:**

1. **NUNCA** commite o arquivo `.env` no Git
2. Adicione `.env` ao `.gitignore`
3. Faça backup seguro das senhas
4. Use senhas diferentes para cada serviço
5. Renove o JWT_SECRET periodicamente

```bash
# Proteger arquivo
chmod 600 /opt/pitstop/.env
chown root:root /opt/pitstop/.env
```
