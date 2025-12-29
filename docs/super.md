Métricas em Tempo Real
typescriptinterface DashboardMetrics {
// Financeiro
mrrTotal: number;           // MRR (Monthly Recurring Revenue)
mrrGrowth: number;          // Crescimento MRR (%)
arrTotal: number;           // ARR (Annual Recurring Revenue)
churnRate: number;          // Taxa de cancelamento
ltv: number;                // Lifetime Value médio
cac: number;                // Custo de Aquisição

// Oficinas
oficinasAtivas: number;
oficinasTrial: number;
oficinasInativas: number;
oficinasInadimplentes: number;
novasOficinas30d: number;

// Usuários
usuariosAtivos: number;
usuariosTotais: number;
loginsMes: number;

// Performance
uptimePercentual: number;
tempoRespostaMedia: number;
requestsPorMinuto: number;

// Dados Gerais
totalClientes: number;      // Soma de todos os clientes das oficinas
totalVeiculos: number;
totalOS: number;
totalOSMes: number;
}
Gráficos Essenciais

MRR Evolution (linha temporal)
Oficinas por Status (donut)
MRR por Plano (barras empilhadas)
Churn Rate (linha)
New Signups vs Cancellations (área)
Revenue Breakdown (waterfall)
Geographic Distribution (mapa Brasil)


🏢 2. GERENCIAMENTO DE OFICINAS (Tenants)
Lista de Oficinas
typescriptinterface Oficina {
id: string;
razaoSocial: string;
nomeFantasia: string;
cnpj: string;
status: 'ATIVA' | 'TRIAL' | 'SUSPENSA' | 'CANCELADA';
plano: 'BASICO' | 'PROFISSIONAL' | 'EMPRESARIAL';
dataContratacao: Date;
dataExpiracao?: Date;
valorMensal: number;

// Dados de Contato
email: string;
telefone: string;
responsavel: string;

// Endereço
endereco: Endereco;

// Estatísticas
usuariosAtivos: number;
limiteUsuarios: number;
espacoUsado: number;        // GB
limiteEspaco: number;       // GB

// Billing
diaVencimento: number;
formaPagamento: string;
ultimoPagamento?: Date;
proximoPagamento: Date;

// Configurações
features: FeatureFlags;
limites: TenantLimits;

// Auditoria
criadoEm: Date;
atualizadoEm: Date;
}
Ações por Oficina

✅ Criar Nova Oficina

Wizard de onboarding
Seleção de plano
Dados cadastrais
Configuração inicial
Provisioning automático do tenant


✅ Editar Detalhes

Dados cadastrais
Plano atual
Limites e quotas
Features habilitadas


✅ Gerenciar Status

Ativar/Suspender
Cancelar (com período de retenção)
Reativar
Migrar de plano


✅ Acessar Como (Impersonate)

Login direto na oficina
Troubleshooting
Suporte técnico


✅ Visualizar Métricas Individuais

Uso de recursos
Estatísticas de uso
Performance
Logs de acesso




💳 3. PLANOS E ASSINATURAS
Gerenciamento de Planos
typescriptinterface Plano {
id: string;
nome: string;
descricao: string;
codigo: 'BASIC' | 'PRO' | 'ENTERPRISE';

// Preço
valorMensal: number;
valorAnual: number;        // Com desconto
trialDias: number;

// Limites
limites: {
usuarios: number;
ordemServico: number;     // Por mês
espacoArmazenamento: number; // GB
apiCalls: number;          // Por dia
whatsappMensagens: number; // Por mês
emailsMes: number;
};

// Features
features: {
multiUsuarios: boolean;
whatsappIntegration: boolean;
relatoriosAvancados: boolean;
apiAccess: boolean;
suportePrioritario: boolean;
backupAutomatico: boolean;
personalizacao: boolean;
};

// Status
ativo: boolean;
visivel: boolean;           // Aparece no site
recomendado: boolean;

// Marketing
corDestaque: string;
tagPromocao?: string;
}
Funcionalidades

✅ CRUD de planos
✅ Definir features por plano
✅ Configurar limites
✅ Preços e descontos
✅ Trial periods
✅ Planos promocionais
✅ Upgrade/Downgrade automático
✅ Grandfathering (manter preço antigo)


💰 4. BILLING E PAGAMENTOS
Gerenciamento Financeiro
typescriptinterface Fatura {
id: string;
oficinaId: string;
numero: string;

// Valores
valor: number;
desconto: number;
valorFinal: number;

// Datas
dataEmissao: Date;
dataVencimento: Date;
dataPagamento?: Date;

// Status
status: 'PENDENTE' | 'PAGO' | 'VENCIDO' | 'CANCELADO';

// Pagamento
metodoPagamento?: string;
transacaoId?: string;
comprovante?: string;

// Itens
itens: ItemFatura[];

// Ações
tentativasCobranca: number;
proximaTentativa?: Date;
}

interface Pagamento {
id: string;
faturaId: string;
oficinaId: string;

valor: number;
metodo: 'PIX' | 'BOLETO' | 'CARTAO_CREDITO' | 'CARTAO_DEBITO';
status: 'PROCESSANDO' | 'APROVADO' | 'RECUSADO' | 'ESTORNADO';

// Gateway
gatewayId: string;          // Mercado Pago, Stripe, etc
transacaoId: string;

dataPagamento: Date;
comprovante?: string;
}
Funcionalidades

✅ Dashboard Financeiro

Receita mensal/anual
Previsão de receita
Inadimplência
Taxas de conversão


✅ Faturas

Listar todas faturas
Filtros avançados
Gerar fatura manual
Reenviar fatura
Cancelar fatura
Download PDF


✅ Cobrança Automática

Cobrança recorrente
Retry logic (tentativas)
Notificações de vencimento
Suspensão automática


✅ Gestão de Inadimplência

Lista de inadimplentes
Ações em massa
Comunicação automática
Negociação de dívidas


✅ Relatórios

Faturamento detalhado
Fluxo de caixa
Previsão de receita
Churn analysis
Export Excel/PDF




👥 5. USUÁRIOS E PERMISSÕES
Gestão de Super Admins
typescriptinterface SuperAdmin {
id: string;
nome: string;
email: string;

// Perfil
perfil: 'SUPER_ADMIN' | 'ADMIN' | 'SUPORTE' | 'FINANCEIRO';

// Permissões Granulares
permissoes: {
gerenciarOficinas: boolean;
gerenciarPlanos: boolean;
gerenciarPagamentos: boolean;
gerenciarUsuarios: boolean;
acessarComoOficina: boolean;
configuracoesSistema: boolean;
visualizarLogs: boolean;
enviarMensagens: boolean;
};

// Status
ativo: boolean;
emailVerificado: boolean;
mfaHabilitado: boolean;

// Auditoria
ultimoAcesso: Date;
criadoEm: Date;
}
Funcionalidades

✅ CRUD de super admins
✅ Perfis e permissões
✅ 2FA obrigatório
✅ Logs de acesso
✅ Sessões ativas
✅ Histórico de ações


⚙️ 6. CONFIGURAÇÕES DO SISTEMA
Configurações Globais
typescriptinterface ConfiguracaoSistema {
// Geral
nomeSistema: string;
urlBase: string;
emailContato: string;
telefoneSuportel: string;

// Trial
diasTrialPadrao: number;
permitirExtensaoTrial: boolean;
maxExtensoesTrial: number;

// Billing
diaVencimentoPadrao: number;
diasAvisoVencimento: number;
diasSuspensaoAposVencimento: number;
diasCancelamentoAposVencimento: number;

// Limites Globais
maxUsuariosPorOficina: number;
maxEspacoArmazenamento: number;
maxApiCallsPorDia: number;

// Integrações
mercadoPagoConfig: MercadoPagoConfig;
whatsappConfig: WhatsAppConfig;
emailConfig: EmailConfig;

// Features
manutencaoMode: boolean;
permitirNovasCadastros: boolean;
versaoMinimaSuportada: string;

// Notificações
emailNotificacoes: string[];
webhookUrl?: string;
}
Funcionalidades

✅ Configurações gerais
✅ Integrações (API keys)
✅ SMTP settings
✅ Payment gateways
✅ Feature flags globais
✅ Modo manutenção
✅ Backups automáticos
✅ Políticas de retenção


📋 7. LOGS E AUDITORIA
Sistema de Auditoria
typescriptinterface LogAuditoria {
id: string;

// Origem
usuarioId?: string;
usuarioNome: string;
oficinaId?: string;

// Ação
acao: string;              // CREATE_TENANT, UPDATE_PLAN, etc
entidade: string;          // Oficina, Plano, Fatura, etc
entidadeId: string;

// Detalhes
descricao: string;
dadosAntigos?: any;
dadosNovos?: any;

// Contexto
ip: string;
userAgent: string;

timestamp: Date;
}
Funcionalidades

✅ Logs de Sistema

Erros e exceções
Performance issues
API errors
Database errors


✅ Logs de Auditoria

Todas ações administrativas
Alterações em oficinas
Alterações em planos
Transações financeiras


✅ Logs de Acesso

Logins/Logouts
IPs suspeitos
Tentativas de login
Sessões ativas


✅ Pesquisa e Filtros

Por usuário
Por oficina
Por período
Por tipo de ação
Export completo




🎫 8. SUPORTE E TICKETS
Sistema de Tickets
typescriptinterface Ticket {
id: string;
numero: string;

// Origem
oficinaId: string;
usuarioId: string;
usuarioNome: string;
usuarioEmail: string;

// Classificação
tipo: 'TECNICO' | 'FINANCEIRO' | 'COMERCIAL' | 'OUTRO';
prioridade: 'BAIXA' | 'MEDIA' | 'ALTA' | 'URGENTE';
status: 'ABERTO' | 'EM_ANDAMENTO' | 'AGUARDANDO' | 'RESOLVIDO' | 'FECHADO';

// Conteúdo
assunto: string;
descricao: string;
anexos: string[];

// Atendimento
atribuidoA?: string;       // Super Admin ID
respostaInicial?: Date;
sla: number;               // minutos
tempoResposta?: number;

// Comunicação
mensagens: MensagemTicket[];

// Datas
aberturaEm: Date;
atualizadoEm: Date;
resolvidoEm?: Date;
fechadoEm?: Date;
}
Funcionalidades

✅ Sistema de tickets completo
✅ Chat interno
✅ Anexos
✅ SLA tracking
✅ Categorização automática
✅ Templates de resposta
✅ Knowledge base
✅ Métricas de atendimento


💬 9. COMUNICAÇÃO
Sistema de Mensagens
typescriptinterface MensagemInterna {
id: string;

// Destinatários
tipo: 'INDIVIDUAL' | 'BROADCAST' | 'SEGMENTADO';
destinatarios: string[];   // Oficina IDs
filtros?: {
planos?: string[];
status?: string[];
regiao?: string[];
};

// Conteúdo
assunto: string;
corpo: string;
template?: string;

// Canal
canais: ('EMAIL' | 'WHATSAPP' | 'IN_APP')[];

// Agendamento
agendada: boolean;
dataEnvio?: Date;

// Status
status: 'RASCUNHO' | 'AGENDADA' | 'ENVIANDO' | 'ENVIADA' | 'FALHA';

// Métricas
totalEnviados: number;
totalEntregues: number;
totalAbertos: number;
totalCliques: number;

criadoEm: Date;
}
Funcionalidades

✅ Broadcast de Mensagens

E-mail em massa
WhatsApp Business
Notificações in-app
SMS (opcional)


✅ Segmentação

Por plano
Por status
Por região
Por comportamento
Custom filters


✅ Templates

Bem-vindo
Trial expirando
Pagamento confirmado
Upgrade de plano
Avisos gerais
Promoções


✅ Campanhas

Criar campanhas
Agendar envios
A/B testing
Métricas de engajamento




📈 10. RELATÓRIOS E ANALYTICS
Relatórios Essenciais
Financeiros

MRR/ARR Evolution
Revenue by Plan
Churn Analysis
Payment Success Rate
Refund Analysis
Lifetime Value (LTV)
Customer Acquisition Cost (CAC)

Operacionais

Active Tenants
Trial Conversion Rate
Feature Usage
API Usage
Storage Usage
Support Tickets

Crescimento

New Signups Trend
Cancellation Reasons
Expansion Revenue
Geographic Distribution
Referral Sources

typescriptinterface RelatorioConfig {
tipo: string;
periodo: {
inicio: Date;
fim: Date;
};
filtros: {
planos?: string[];
status?: string[];
regioes?: string[];
};
formato: 'PDF' | 'EXCEL' | 'CSV';
agendado: boolean;
frequencia?: 'DIARIA' | 'SEMANAL' | 'MENSAL';
destinatarios: string[];
}
Funcionalidades

✅ Dashboard executivo
✅ Relatórios customizados
✅ Export em múltiplos formatos
✅ Agendamento automático
✅ Gráficos interativos (ECharts)
✅ Drill-down capabilities
✅ Comparative analysis


🎚️ 11. FEATURE FLAGS
Controle de Features
typescriptinterface FeatureFlag {
id: string;
codigo: string;
nome: string;
descricao: string;

// Status
habilitadoGlobal: boolean;
habilitadoPorPlano: {
[plano: string]: boolean;
};
habilitadoPorOficina: string[]; // Oficina IDs

// Rollout
percentualRollout: number;      // 0-100

// Datas
dataInicio?: Date;
dataFim?: Date;

// Controle
requerAutorizacao: boolean;
criadoEm: Date;
}
Features a Controlar

WhatsApp Integration
API Access
Advanced Reports
Custom Branding
Multi-location
Backup Automático
Export de Dados
Integração Contábil


🔌 12. INTEGRAÇÕES
Painel de Integrações
typescriptinterface Integracao {
id: string;
nome: string;
tipo: 'PAGAMENTO' | 'MENSAGERIA' | 'ANALYTICS' | 'CONTABIL';

// Status
ativa: boolean;
configurada: boolean;

// Credenciais
config: {
apiKey?: string;
apiSecret?: string;
webhookUrl?: string;
[key: string]: any;
};

// Uso
requestsHoje: number;
limiteRequests: number;
ultimoUso?: Date;

// Health
status: 'ONLINE' | 'OFFLINE' | 'ERROR';
ultimoCheck: Date;
}
Integrações Disponíveis

✅ Pagamentos

Mercado Pago
Stripe
Pagar.me


✅ Mensageria

Twilio (WhatsApp)
Evolution API
SendGrid (Email)
AWS SES


✅ Analytics

Google Analytics
Mixpanel
Hotjar


✅ Outras

Webhook customizados
Zapier integration
API pública




📊 13. MONITORAMENTO E PERFORMANCE
Métricas de Sistema
typescriptinterface MetricasSistema {
// Performance
tempoRespostaApi: {
media: number;
p95: number;
p99: number;
};

// Disponibilidade
uptime: number;             // Percentual
downtime: number;           // Minutos no mês

// Recursos
cpuUsage: number;
memoryUsage: number;
diskUsage: number;

// Database
conexoesAtivas: number;
queryTime: number;
slowQueries: number;

// Cache
hitRate: number;
missRate: number;

// Erros
errorRate: number;
errorsPorMinuto: number;
}
Funcionalidades

✅ Dashboard de Performance

Real-time metrics
Historical data
Alertas automáticos


✅ Health Checks

API endpoints
Database
Redis
Integrações externas


✅ Alertas

Alta latência
Erros críticos
Recursos limitados
Integrations down




🚀 14. ONBOARDING E ATIVAÇÃO
Wizard de Onboarding
typescriptinterface OnboardingFlow {
oficinaId: string;

etapas: {
// Etapa 1: Dados Básicos
dadosCadastrais: boolean;
verificacaoEmail: boolean;

    // Etapa 2: Configuração Inicial
    configuracaoInicial: boolean;
    primeiroUsuario: boolean;
    
    // Etapa 3: Dados da Oficina
    informacoesOficina: boolean;
    logomarca: boolean;
    
    // Etapa 4: Primeiro Uso
    primeiroCliente: boolean;
    primeiraOS: boolean;
    primeiraPeca: boolean;
    
    // Etapa 5: Integrações
    configuracaoPagamento: boolean;
    configuracaoWhatsApp: boolean;
};

// Progresso
percentualCompleto: number;
etapaAtual: string;

// Datas
iniciadoEm: Date;
completadoEm?: Date;
}
Funcionalidades

✅ Wizard passo-a-passo
✅ Vídeos tutoriais
✅ Checklist de ativação
✅ In-app guidance
✅ Suporte proativo
✅ Métricas de ativação


🎨 15. PERSONALIZAÇÃO E BRANDING
White Label (Futuro)
typescriptinterface Branding {
oficinaId: string;

// Logo e Cores
logo: string;
logoDark: string;
corPrimaria: string;
corSecundaria: string;

// Domínio
dominioCustomizado?: string;

// E-mails
emailRemetente: string;
nomeRemetente: string;

// Customizações
favIcon: string;
metaTags: any;
}
```

---

## 📱 16. MOBILE APP MANAGEMENT (Futuro)

- Versões disponíveis
- Force update
- Feature toggles mobile
- Push notifications
- Deep links

---

## 🔐 17. SEGURANÇA E COMPLIANCE

### Funcionalidades
- ✅ **Gestão de Acessos**
  - IP whitelisting
  - Geolocation blocking
  - Rate limiting
  - Session management

- ✅ **LGPD/GDPR**
  - Data retention policies
  - Right to be forgotten
  - Data export
  - Consent management

- ✅ **Backup e Recovery**
  - Automated backups
  - Point-in-time recovery
  - Disaster recovery plan

---

## 🎯 RESUMO DAS SEÇÕES PRINCIPAIS
```
1. 📊 Dashboard - Visão geral do negócio
2. 🏢 Oficinas - Gerenciamento completo de tenants
3. 💳 Planos - CRUD de planos e features
4. 💰 Billing - Faturas, pagamentos, inadimplência
5. 👥 Usuários - Super admins e permissões
6. ⚙️ Configurações - Sistema global
7. 📋 Logs - Auditoria completa
8. 🎫 Suporte - Sistema de tickets
9. 💬 Comunicação - Mensagens e campanhas
10. 📈 Relatórios - Analytics e BI
11. 🎚️ Feature Flags - Controle de features
12. 🔌 Integrações - Gateways e APIs
13. 📊 Monitoramento - Performance e saúde
14. 🚀 Onboarding - Ativação de clientes
15. 🎨 Branding - Personalização
16. 📱 Mobile - Gerenciamento de apps
17. 🔐 Segurança - Compliance e proteção
