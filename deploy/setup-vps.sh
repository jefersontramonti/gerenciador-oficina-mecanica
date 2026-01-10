#!/bin/bash
# Script de setup inicial da VPS para PitStop
# Execute: chmod +x setup-vps.sh && ./setup-vps.sh

set -e

DOMAIN="app.pitstopai.com.br"
APP_DIR="/root/pitstop"

echo "======================================"
echo "    PitStop VPS Setup Script"
echo "======================================"

# Atualizar sistema
echo "[1/7] Atualizando sistema..."
apt update && apt upgrade -y

# Instalar Docker (se necessario)
if ! command -v docker &> /dev/null; then
    echo "[2/7] Instalando Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    rm get-docker.sh
else
    echo "[2/7] Docker ja instalado"
fi

# Instalar Docker Compose plugin
echo "[3/7] Verificando Docker Compose..."
docker compose version || apt install docker-compose-plugin -y

# Instalar Certbot para SSL
echo "[4/7] Instalando Certbot..."
apt install certbot -y

# Criar diretorio da aplicacao
echo "[5/7] Criando diretorio da aplicacao..."
mkdir -p $APP_DIR/ssl
cd $APP_DIR

# Gerar certificado SSL
echo "[6/7] Gerando certificado SSL para $DOMAIN..."
certbot certonly --standalone -d $DOMAIN --non-interactive --agree-tos --email admin@pitstopai.com.br || {
    echo "AVISO: Certificado SSL nao gerado. Configure manualmente depois."
}

# Copiar certificados para pasta ssl
if [ -d "/etc/letsencrypt/live/$DOMAIN" ]; then
    ln -sf /etc/letsencrypt/live/$DOMAIN/fullchain.pem $APP_DIR/ssl/cert.pem
    ln -sf /etc/letsencrypt/live/$DOMAIN/privkey.pem $APP_DIR/ssl/key.pem
    echo "Certificados SSL configurados!"
fi

# Criar .env de exemplo
echo "[7/7] Criando arquivo .env..."
cat > $APP_DIR/.env << 'EOF'
# === OBRIGATORIO: Configuracoes do Banco ===
DB_USER=pitstop
DB_PASSWORD=GERAR_SENHA_FORTE_AQUI

# === OBRIGATORIO: JWT Secret (minimo 64 caracteres) ===
JWT_SECRET=GERAR_CHAVE_SEGURA_AQUI_MINIMO_64_CARACTERES_ALEATORIOS_12345678

# === GitHub Container Registry ===
GITHUB_USER=SEU_USUARIO_GITHUB

# === Email (Opcional) ===
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=sua-senha-de-app

# === API URLs (para frontend build) ===
VITE_API_URL=https://app.pitstopai.com.br/api
VITE_WS_URL=wss://app.pitstopai.com.br/ws
VITE_API_BASE_URL=https://app.pitstopai.com.br
EOF

echo ""
echo "======================================"
echo "    Setup concluido!"
echo "======================================"
echo ""
echo "Proximos passos:"
echo "1. Edite o arquivo .env em $APP_DIR/.env"
echo "2. Copie docker-compose.yml e nginx.conf para $APP_DIR"
echo "3. Execute: docker compose up -d"
echo ""
echo "Para renovar SSL automaticamente, adicione ao crontab:"
echo "0 0 1 * * certbot renew --quiet && docker restart pitstop-frontend"
