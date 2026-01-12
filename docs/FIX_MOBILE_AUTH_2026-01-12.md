# Fix: Autenticação Mobile - Problema de Cookie Cross-Domain

**Data:** 2026-01-12
**Problema:** Sistema funciona no computador mas perde autenticação ao recarregar no celular

## Diagnóstico

### Causa Raiz
O frontend estava em `app.pitstopai.com.br` e a API em `api.pitstopai.com.br` (domínios diferentes). Browsers mobile bloqueiam cookies cross-site por padrões de privacidade mais restritivos, mesmo com subdomínios do mesmo domínio raiz.

### Fluxo do Problema
1. Usuário faz login em `app.pitstopai.com.br`
2. Backend define cookie `refreshToken` com `domain=.pitstopai.com.br`
3. No computador: Cookie é enviado para `api.pitstopai.com.br` (funciona)
4. No celular: Browser bloqueia cookie cross-site → requisição `/api/auth/refresh` falha → redireciona para login

## Solução Implementada

### 1. Proxy da API no mesmo domínio do frontend

Adicionamos proxy no nginx de `app.pitstopai.com.br` para que a API seja servida no mesmo domínio:

**Arquivo:** `/etc/nginx/sites-available/pitstopai`

No bloco `server` de `app.pitstopai.com.br`, adicionado antes do `location /`:

```nginx
    # API proxy - para cookies funcionarem no mobile
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # WebSocket proxy
    location /ws {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 86400;
    }
```

### 2. Atualização do docker-compose.yml

**Alterações no `/opt/pitstop/docker-compose.yml`:**

1. Adicionado `DOMAIN_API` no `.env`:
```env
DOMAIN_API=app.pitstopai.com.br
```

2. Corrigido `VITE_WS_URL` de `wss://` para `https://` (SockJS requer HTTP para handshake):
```yaml
args:
    VITE_API_URL: https://${DOMAIN_API}/api
    VITE_WS_URL: https://${DOMAIN_API}/ws  # NÃO usar wss://, SockJS faz upgrade
    VITE_API_BASE_URL: https://${DOMAIN_API}
```

3. Corrigida definição de rede:
```yaml
networks:
  pitstop-app-network:
    driver: bridge
```

### 3. Variáveis de ambiente (.env)

```env
DOMAIN_API=app.pitstopai.com.br
CORS_ALLOWED_ORIGINS=https://app.pitstopai.com.br
APP_COOKIE_DOMAIN=.pitstopai.com.br
```

## Comandos Executados

```bash
# 1. Editar nginx
nano /etc/nginx/sites-available/pitstopai
# Adicionar blocos location /api/ e location /ws no server app.pitstopai.com.br

# 2. Recarregar nginx
nginx -t && systemctl reload nginx

# 3. Adicionar DOMAIN_API no .env
echo "DOMAIN_API=app.pitstopai.com.br" >> /opt/pitstop/.env

# 4. Corrigir VITE_WS_URL no docker-compose
sed -i 's|VITE_WS_URL: wss://|VITE_WS_URL: https://|' /opt/pitstop/docker-compose.yml

# 5. Corrigir nome da rede (se necessário)
sed -i 's/pitstop-network/pitstop-app-network/g' /opt/pitstop/docker-compose.yml

# 6. Adicionar definição de rede no final do docker-compose.yml
cat >> /opt/pitstop/docker-compose.yml << 'EOF'

networks:
  pitstop-app-network:
    driver: bridge
EOF

# 7. Rebuild e restart
docker compose build pitstop-frontend --no-cache
docker compose up -d

# 8. Verificar URLs no build
docker exec pitstop-frontend sh -c "grep -o 'https://app.pitstopai' /usr/share/nginx/html/assets/*.js | head -1"
```

## Resultado

- Frontend: `https://app.pitstopai.com.br`
- API: `https://app.pitstopai.com.br/api` (proxy para backend:8080)
- WebSocket: `https://app.pitstopai.com.br/ws` (proxy para backend:8080)
- Cookie: Same-origin (não mais cross-site)
- Mobile: Autenticação persiste após recarregar

## Lições Aprendidas

1. **Cookies cross-site são bloqueados em mobile**: Safari/iOS e Chrome mobile têm políticas restritivas
2. **SockJS usa HTTP para handshake**: Não passar URL `wss://`, usar `https://`
3. **Proxy reverso resolve cross-origin**: Servir API no mesmo domínio do frontend
4. **Testar em dispositivos reais**: Comportamento de cookies varia entre browsers

## Arquitetura Final

```
Cliente (Browser/Mobile)
         │
         ▼
┌─────────────────────────────────┐
│   app.pitstopai.com.br (nginx)  │
│                                 │
│  /          → frontend:3000     │
│  /api/*     → backend:8080      │
│  /ws        → backend:8080      │
└─────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│      Docker Containers          │
│  - pitstop-frontend (3000)      │
│  - pitstop-backend (8080)       │
│  - pitstop-postgres (5432)      │
│  - pitstop-redis (6379)         │
└─────────────────────────────────┘
```
