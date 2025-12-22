#!/bin/bash

# ========================================
# PitStop - Script de Instalação Rápida
# ========================================
# Para servidor Contabo VPS - Ubuntu 22.04
# Executa toda a configuração inicial automaticamente

set -e

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Funções
log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# Banner
echo -e "${BLUE}"
cat << "EOF"
╔═══════════════════════════════════════════╗
║                                           ║
║         PitStop - Instalação VPS          ║
║                                           ║
╚═══════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Verificar se é root
if [ "$EUID" -ne 0 ]; then
    log_error "Este script precisa ser executado como root (sudo)"
    exit 1
fi

# Verificar sistema operacional
if ! grep -q "Ubuntu 22.04\|Ubuntu 24.04\|Debian 12" /etc/os-release; then
    log_warn "Este script foi testado em Ubuntu 22.04/24.04 e Debian 12"
    read -p "Deseja continuar mesmo assim? (s/N): " continue
    [[ ! $continue =~ ^[Ss]$ ]] && exit 0
fi

log_info "Sistema detectado: $(lsb_release -ds)"
echo ""

# Confirmação
log_warn "Este script irá instalar e configurar:"
echo "  - Atualizações do sistema"
echo "  - Docker e Docker Compose"
echo "  - Firewall (UFW)"
echo "  - Fail2ban (segurança SSH)"
echo "  - Certbot (SSL)"
echo "  - Ferramentas essenciais"
echo ""
read -p "Continuar com a instalação? (s/N): " confirm
[[ ! $confirm =~ ^[Ss]$ ]] && exit 0

# ========================================
# INÍCIO DA INSTALAÇÃO
# ========================================

log_step "1/10 - Atualizando sistema..."
apt update -qq
DEBIAN_FRONTEND=noninteractive apt upgrade -y -qq
log_info "Sistema atualizado"

log_step "2/10 - Instalando ferramentas essenciais..."
apt install -y -qq \
    curl wget git vim nano htop \
    ufw fail2ban unattended-upgrades \
    ca-certificates gnupg lsb-release \
    jq net-tools
log_info "Ferramentas instaladas"

log_step "3/10 - Instalando Docker..."
if command -v docker &> /dev/null; then
    log_warn "Docker já está instalado ($(docker --version))"
else
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
    log_info "Docker instalado: $(docker --version)"
fi

log_step "4/10 - Configurando Docker Compose..."
if docker compose version &> /dev/null; then
    log_info "Docker Compose já está disponível"
else
    log_error "Docker Compose não encontrado"
    exit 1
fi

log_step "5/10 - Configurando Firewall (UFW)..."
ufw --force reset
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH'
ufw allow 80/tcp comment 'HTTP'
ufw allow 443/tcp comment 'HTTPS'
ufw allow 8081/tcp comment 'Evolution API (opcional)'
ufw --force enable
log_info "Firewall configurado e ativado"

log_step "6/10 - Configurando Fail2ban..."
cat > /etc/fail2ban/jail.local << 'FAIL2BAN_EOF'
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = 22
logpath = /var/log/auth.log
FAIL2BAN_EOF

systemctl enable fail2ban
systemctl restart fail2ban
log_info "Fail2ban configurado"

log_step "7/10 - Instalando Certbot..."
apt install -y -qq certbot
log_info "Certbot instalado"

log_step "8/10 - Configurando Swap (4GB)..."
if [ -f /swapfile ]; then
    log_warn "Swap já existe"
else
    fallocate -l 4G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    log_info "Swap de 4GB criado"
fi

log_step "9/10 - Configurando atualizações automáticas de segurança..."
cat > /etc/apt/apt.conf.d/50unattended-upgrades << 'UNATTENDED_EOF'
Unattended-Upgrade::Allowed-Origins {
    "${distro_id}:${distro_codename}-security";
};
Unattended-Upgrade::AutoFixInterruptedDpkg "true";
Unattended-Upgrade::MinimalSteps "true";
Unattended-Upgrade::Remove-Unused-Kernel-Packages "true";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
Unattended-Upgrade::Automatic-Reboot "false";
UNATTENDED_EOF

systemctl enable unattended-upgrades
systemctl start unattended-upgrades
log_info "Atualizações automáticas configuradas"

log_step "10/10 - Criando diretórios do projeto..."
mkdir -p /opt/pitstop
mkdir -p /var/backups/pitstop/{daily,weekly,manual}
mkdir -p /var/log/pitstop
log_info "Diretórios criados"

# ========================================
# CONFIGURAÇÃO FINAL
# ========================================

echo ""
log_info "Instalação base concluída!"
echo ""
echo -e "${BLUE}╔═══════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         PRÓXIMOS PASSOS                   ║${NC}"
echo -e "${BLUE}╔═══════════════════════════════════════════╗${NC}"
echo ""
echo "1. Clone o repositório PitStop:"
echo -e "   ${YELLOW}cd /opt/pitstop${NC}"
echo -e "   ${YELLOW}git clone <URL_DO_SEU_REPO> .${NC}"
echo ""
echo "2. Configure as variáveis de ambiente:"
echo -e "   ${YELLOW}cp .env.production.example .env${NC}"
echo -e "   ${YELLOW}nano .env${NC}"
echo ""
echo "   Gere chaves seguras:"
echo -e "   ${YELLOW}openssl rand -base64 64  # JWT_SECRET${NC}"
echo -e "   ${YELLOW}openssl rand -hex 32     # EVOLUTION_API_KEY${NC}"
echo -e "   ${YELLOW}openssl rand -base64 32  # DB_PASSWORD${NC}"
echo ""
echo "3. Configure DNS do seu domínio:"
echo "   - Adicione registro A apontando para: $(curl -s ifconfig.me)"
echo ""
echo "4. Obtenha certificado SSL:"
echo -e "   ${YELLOW}docker compose -f docker-compose.prod.yml stop frontend${NC}"
echo -e "   ${YELLOW}certbot certonly --standalone -d seudominio.com${NC}"
echo -e "   ${YELLOW}ln -s /etc/letsencrypt/live/seudominio.com/fullchain.pem /opt/pitstop/ssl/cert.pem${NC}"
echo -e "   ${YELLOW}ln -s /etc/letsencrypt/live/seudominio.com/privkey.pem /opt/pitstop/ssl/key.pem${NC}"
echo ""
echo "5. Inicie a aplicação:"
echo -e "   ${YELLOW}docker compose -f docker-compose.prod.yml up -d${NC}"
echo ""
echo "6. Configure backups automáticos:"
echo -e "   ${YELLOW}crontab -e${NC}"
echo "   Adicione: 0 2 * * * /opt/pitstop/scripts/backup.sh diario"
echo ""
echo -e "${BLUE}╔═══════════════════════════════════════════╗${NC}"
echo ""

# Informações do sistema
log_info "Informações do servidor:"
echo "  IP Público: $(curl -s ifconfig.me)"
echo "  Memória: $(free -h | awk '/^Mem:/ {print $2}')"
echo "  Disco: $(df -h / | awk 'NR==2 {print $2}')"
echo "  CPUs: $(nproc)"
echo "  Docker: $(docker --version)"
echo ""

log_info "Status dos serviços:"
systemctl is-active --quiet docker && echo "  ✓ Docker: rodando" || echo "  ✗ Docker: parado"
systemctl is-active --quiet ufw && echo "  ✓ UFW: ativo" || echo "  ✗ UFW: inativo"
systemctl is-active --quiet fail2ban && echo "  ✓ Fail2ban: ativo" || echo "  ✗ Fail2ban: inativo"
echo ""

log_info "Instalação concluída! 🎉"
echo ""
log_warn "IMPORTANTE: Configure as variáveis .env antes de subir a aplicação!"

exit 0
