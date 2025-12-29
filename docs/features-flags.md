📋 ESPECIFICAÇÃO TÉCNICA - MELHORIAS FEATURE FLAGS
🎯 OBJETIVO
Completar a funcionalidade de Feature Flags adicionando campos faltantes, melhorar a visualização quando houver features criadas, e implementar filtros funcionais.

1️⃣ MODAL DE CRIAÇÃO/EDIÇÃO - CAMPOS FALTANTES
1.1 Período de Validade (Data Início e Fim)
Localização: Após o slider de "Percentual de Rollout"
Campos:

Data Início (opcional) - input datetime-local
Data Fim (opcional) - input datetime-local

Comportamento:

Ambos os campos são opcionais
Se preenchido apenas início: feature ativa a partir dessa data
Se preenchido apenas fim: feature ativa até essa data
Se ambos preenchidos: feature ativa apenas entre essas datas
Data fim não pode ser anterior à data início (validação)
Mostrar hint: "💡 Deixe em branco para feature permanente"

Exemplo de uso: Feature promocional válida apenas em Janeiro/2026

1.2 Oficinas Específicas (Beta Testers)
Localização: Após a seção "Habilitar por Plano"
Título: "Oficinas Específicas (Beta)"
Funcionalidade:

Botão "+ Adicionar oficinas" que abre modal de seleção
Lista de oficinas selecionadas exibidas com:

Ícone de building
Nome fantasia
CNPJ
Botão X para remover



Modal de Seleção de Oficinas:

Campo de busca por nome/CNPJ
Lista paginada de todas as oficinas
Checkbox para selecionar múltiplas
Badge visual mostrando o plano de cada oficina
Botões "Cancelar" e "Adicionar Selecionadas"
Mostrar contador: "X oficinas selecionadas"

Comportamento:

Oficinas na lista específica têm acesso INDEPENDENTE de plano ou rollout
São "beta testers" com acesso garantido
Mostrar hint: "💡 Essas oficinas terão acesso independente de plano ou rollout"


1.3 Preview de Impacto
Localização: Final do modal, antes dos botões de ação
Aparência:

Card com fundo azul escuro semi-transparente
Borda azul
Ícone de info

Conteúdo Dinâmico:

Título: "Impacto Estimado"
Calcular e mostrar quantas oficinas serão afetadas baseado nas regras:

Se global ativo: "Todas as X oficinas"
Se por plano: "X oficinas nos planos Y, Z"
Se específicas: "X oficinas específicas (beta)"
Se rollout: "~X oficinas no rollout de Y%"



Cálculo:

Se global = true → todas as oficinas
Se planos selecionados → contar oficinas nesses planos
Se lista específica → contar oficinas na lista
Se rollout < 100% → aplicar percentual sobre total
Oficinas específicas sempre incluídas (não afetadas por rollout)


1.4 Templates Rápidos
Localização: Topo do modal, logo após o título
Funcionalidade:

Link "📋 Usar template" que expande/colapsa grid de templates
Grid 2 colunas com cards de templates pré-configurados

Templates Sugeridos:

WhatsApp para PRO

Código: WHATSAPP_NOTIFICATIONS
Categoria: COMUNICACAO
Global: ativo
Planos: PROFISSIONAL, TURBINADO


Beta Test 25%

Código: NOVA_FUNCIONALIDADE
Categoria: GERAL
Global: ativo
Rollout: 25%


Premium Exclusivo

Código: FUNCIONALIDADE_PREMIUM
Categoria: PREMIUM
Global: ativo
Planos: TURBINADO


Teste Temporário

Código: TESTE_SAZONAL
Categoria: OPERACIONAL
Global: ativo
Data Fim: +30 dias



Comportamento ao clicar template:

Preencher automaticamente todos os campos do formulário
Usuário pode editar após aplicar
Código pode ser alterado (é apenas sugestão)


2️⃣ LISTAGEM DE FEATURES (SUBSTITUIR EMPTY STATE)
2.1 Estrutura Geral
Quando houver features:

Agrupar por categoria (accordion/seções)
Cada categoria mostra suas features em cards
Header de categoria com ícone e nome

Categorias e Ícones:

GERAL → Flag
COMUNICACAO → MessageCircle
RELATORIOS → FileText
PREMIUM → Star
FINANCEIRO → DollarSign
INTEGRACAO → Plug
OPERACIONAL → Settings


2.2 Card de Feature (Modo Compacto)
Layout:
Header (sempre visível):

Indicador de status (bolinha verde=ativo, cinza=inativo)
Código da feature (font-mono, destaque)
Badges visuais:

Se rollout > 0: badge laranja "X% Rollout"
Se tem oficinas específicas: badge azul "X Beta"
Se requer autorização: ícone de cadeado



Conteúdo:

Nome da feature (título principal)
Descrição (texto secundário, se existir)

Linha de Informações Rápidas (rodapé):

Planos habilitados (ex: "PRO, TURBINADO") com ícone Layers
Período de validade (se existir) com ícone Calendar
Tempo desde criação (ex: "há 3 dias") com ícone Clock

Ações (canto superior direito):

Switch/Toggle para habilitar/desabilitar globalmente
Menu dropdown (3 pontinhos) com:

Ver Detalhes / Ocultar Detalhes
Editar
─── (separador)
Excluir (vermelho)




2.3 Card de Feature (Modo Expandido)
Trigger: Clicar em "Ver Detalhes" no menu dropdown
Comportamento:

Expande abaixo do conteúdo atual
Área com fundo mais escuro para destacar
Borda superior para separar visualmente

Conteúdo Expandido - 2 Colunas:
Coluna Esquerda: Configurações

Status Global: Ativo/Inativo (cor condicional)
Planos: lista ou "Todos"
Rollout: X%
Autorização: Requerida/Não requerida
Período: datas ou "Permanente"

Coluna Direita: Estatísticas de Uso

Oficinas com acesso: número total
Usos últimos 7 dias: contador
Última verificação: tempo relativo (ex: "há 2 horas")

Seção Adicional (se houver beta testers):

Título: "Beta Testers (X)"
Lista horizontal de badges com nomes das oficinas
Se mais de 5: mostrar 5 primeiros + badge "+X mais"


2.4 Checkbox de Seleção Múltipla
Localização: Canto esquerdo de cada card
Funcionalidade:

Checkbox visível em hover do card
Clicar marca/desmarca feature
Seleções persistem ao scroll

Quando houver seleções ativas:

Mostrar barra de ações no topo da página
Fundo azul, texto branco
Mensagem: "X feature(s) selecionada(s)"
Botões de ação:

Ativar Todas
Desativar Todas
Excluir
Cancelar



Confirmação para ações em massa:

Modal de confirmação antes de executar
Listar quais features serão afetadas
Avisar sobre impacto (quantas oficinas afetadas)


3️⃣ SISTEMA DE FILTROS FUNCIONAIS
3.1 Barra de Filtros
Layout: Horizontal, acima da listagem
Componentes:

Campo de Busca

Placeholder: "Buscar por nome ou código..."
Busca em tempo real (debounce 300ms)
Campos pesquisados: código, nome, descrição
Case-insensitive


Dropdown de Categoria

Label: "Todas Categorias"
Opções:

Todas Categorias (padrão)
Geral
Comunicação
Relatórios
Premium
Financeiro
Integração
Operacional




Dropdown de Status

Label padrão: "Todos"
Opções:

Todos (padrão)
Ativos (habilitadoGlobal = true)
Inativos (habilitadoGlobal = false)




Dropdown de Plano (opcional/avançado)

Label: "Todos os Planos"
Opções:

Todos os Planos
Econômico
Profissional
Turbinado





Comportamento:

Filtros combinados com AND (todos devem passar)
Atualização em tempo real conforme usuário altera
Se nenhum resultado: mostrar empty state específico

"Nenhuma feature encontrada com os filtros aplicados"
Botão "Limpar filtros"




3.2 Estado Vazio Específico de Filtros
Diferente do empty state inicial
Quando não há resultados por filtros:

Ícone de filtro/funil
Mensagem: "Nenhuma feature encontrada"
Submensagem: "Tente ajustar os filtros ou criar uma nova feature"
Botão: "Limpar todos os filtros"

Quando clica em limpar filtros:

Reset de todos os filtros para estado padrão
Busca limpa
Categoria = Todas
Status = Todos


4️⃣ MELHORIAS NO EMPTY STATE INICIAL
Quando ainda não há features criadas:
Estrutura:

Ícone grande de flag em círculo cinza
Título: "Nenhuma feature flag encontrada"
Descrição explicativa (2-3 linhas sobre o que são feature flags)
Botão primário: "+ Criar Primeira Feature Flag"
Card informativo abaixo com "Exemplos de uso:"

Lista de 3-4 casos de uso práticos
Ícone de livro/documentação



Texto sugerido:

Título: "Nenhuma feature flag encontrada"
Descrição: "Feature flags permitem ativar/desativar funcionalidades sem deploy, fazer rollout gradual e diferenciar planos dinamicamente."
Exemplos:

Liberar WhatsApp apenas para planos PRO
Testar novo dashboard com 25% dos usuários
Dar acesso beta a clientes específicos
Desativar feature com problema (kill switch)




5️⃣ VALIDAÇÕES E REGRAS DE NEGÓCIO
5.1 Validações de Formulário
Código:

Obrigatório
Apenas maiúsculas, números e underscores
Único (não pode duplicar)
Hint: "Maiúsculas, números e underscores"

Nome:

Obrigatório
Máximo 100 caracteres

Categoria:

Obrigatório
Dropdown com opções fixas

Datas:

Data fim deve ser posterior à data início
Não podem ser no passado (apenas futuro ou presente)
Formato: datetime-local do HTML5

Percentual de Rollout:

Valor entre 0 e 100
Slider visual
Mostrar hint explicativo: "0% = Desabilitado, 100% = Habilitado para todas oficinas não cobertas por outras regras"

Planos:

Multi-select com pills
Ao menos 1 plano ou global ativo ou oficinas específicas
Não pode ter tudo desmarcado (validação)


5.2 Regras de Habilitação (Hierarquia)
Ordem de Verificação no Backend:

Global OFF → Bloqueado para todos (fim)
Oficinas Específicas → Se ID está na lista, LIBERADO (bypass de todas outras regras)
Planos → Se plano não está na lista, BLOQUEADO
Rollout → Se hash % 100 > percentual, BLOQUEADO
Período → Se fora do período, BLOQUEADO
Resultado → LIBERADO

Importante:

Oficinas específicas (beta) têm prioridade sobre TUDO
São uma "whitelist" absoluta


5.3 Comportamento do Toggle Global
Quando desativar globalmente:

Modal de confirmação
Mensagem: "Isso irá desabilitar a feature para TODAS as oficinas imediatamente. Confirma?"
Mostrar número de oficinas afetadas
Botões: "Cancelar" e "Confirmar"

Quando ativar globalmente:

Não precisa confirmação (ação menos arriscada)
Apenas atualiza o status

Visual do toggle:

ON: verde
OFF: cinza
Transição suave (animação)


6️⃣ ESTATÍSTICAS E CARDS DO TOPO
6.1 Cálculo das Métricas
Total Features:

Contar todas as features cadastradas

Habilitadas Globalmente:

Contar features com habilitadoGlobal = true
Cor verde

Por Plano:

Contar features que têm pelo menos 1 plano selecionado
Mas que NÃO estão habilitadas globalmente
Cor roxa

Em Rollout:

Contar features com percentualRollout > 0 e < 100
Cor laranja


6.2 Visual dos Cards de Estatística
Estrutura de cada card:

Ícone colorido (correspondente à métrica)
Label descritivo
Número grande
Link "Ver todas" que filtra a lista

Comportamento ao clicar "Ver todas":

Aplicar filtro correspondente automaticamente
Ex: Clicar em "Em Rollout" → filtra só features com rollout


7️⃣ EXPERIÊNCIA DO USUÁRIO
7.1 Loading States
Ao carregar página:

Skeleton nos cards de estatística (4 retângulos pulsando)
Skeleton na área de listagem

Ao criar/editar feature:

Botão "Criar" desabilitado enquanto processa
Texto muda para "Criando..." com spinner
Modal não fecha até receber resposta

Ao togglear feature:

Switch fica desabilitado durante request
Loading visual no switch
Se erro: reverter estado anterior


7.2 Feedback de Sucesso/Erro
Após criar feature:

Toast de sucesso: "Feature criada com sucesso!"
Modal fecha automaticamente
Lista atualiza mostrando nova feature
Nova feature destacada (animação de entrada)

Após editar:

Toast: "Feature atualizada com sucesso!"
Modal fecha
Card da feature atualiza conteúdo

Após excluir:

Modal de confirmação antes
Toast: "Feature excluída com sucesso!"
Card some com animação de saída

Ao errar:

Toast vermelho com mensagem de erro
Manter modal aberto
Destacar campo com erro (se aplicável)


7.3 Animações e Transições
Cards de feature:

Hover: elevação sutil (shadow)
Expand/Collapse: transição suave (300ms)

Filtros:

Resultados aparecem/somem com fade (200ms)

Toggle switch:

Transição suave entre estados (200ms)

Toast notifications:

Slide-in da direita
Auto-dismiss após 3 segundos
Fechar manual com X


8️⃣ RESPONSIVIDADE
Desktop (> 1024px):

Cards em grid ou lista vertical
Modal com width adequado (max 600px)
Filtros em linha horizontal

Tablet (768px - 1024px):

Cards em lista vertical
Modal centralizado
Filtros podem quebrar linha

Mobile (< 768px):

Cards em lista vertical, width 100%
Modal fullscreen ou quase
Filtros em stack vertical
Estatísticas em grid 2x2


9️⃣ ACESSIBILIDADE
Formulários:

Todos os inputs com labels
Mensagens de erro associadas (aria-describedby)
Foco visível em todos elementos interativos

Modais:

Trap de foco (Tab não sai do modal)
ESC fecha o modal
aria-modal="true"
Foco automático no primeiro campo ao abrir

Botões:

Textos descritivos (não apenas ícones)
Ou aria-label quando só ícone
Estados disabled visualmente claros

Cores:

Contraste adequado (WCAG AA mínimo)
Não usar apenas cor para indicar estado


🎯 RESUMO DAS ENTREGAS
Modal (Criação/Edição):

✅ Campos de data início/fim
✅ Seleção de oficinas específicas
✅ Preview de impacto calculado
✅ Templates rápidos
✅ Validações completas

Listagem:

✅ Cards de features por categoria
✅ Modo compacto e expandido
✅ Seleção múltipla com ações em massa
✅ Estatísticas de uso por feature

Filtros:

✅ Busca em tempo real
✅ Filtro por categoria
✅ Filtro por status (ativo/inativo)
✅ Filtro por plano (opcional)
✅ Empty state específico quando sem resultados

UX/UI:

✅ Loading states
✅ Feedback de sucesso/erro (toasts)
✅ Confirmações para ações destrutivas
✅ Animações suaves
✅ Responsividade completa


📦 DADOS E API
Assumir que o backend já retorna:

Lista de features com todos os campos
Lista de oficinas para seleção
Contagem de oficinas por plano
Estatísticas de uso (mock se necessário)

Endpoints esperados:

GET /api/super-admin/feature-flags
POST /api/super-admin/feature-flags
PUT /api/super-admin/feature-flags/:id
DELETE /api/super-admin/feature-flags/:id
PATCH /api/super-admin/feature-flags/:id/toggle
GET /api/super-admin/oficinas (para picker)

=========================================================================================================================================

🎚️ LISTA COMPLETA DE FEATURE FLAGS - PITSTOP
📋 ORGANIZAÇÃO
Total estimado: 50-60 Feature Flags
Organizadas em 10 categorias

1️⃣ COMUNICAÇÃO (9 flags)
WHATSAPP_NOTIFICATIONS

Nome: Notificações via WhatsApp
Descrição: Envio automático de mensagens via WhatsApp (OS criada, orçamento pronto, veículo finalizado)
Categoria: COMUNICACAO
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Sistema de notificações automáticas integrado com Twilio/Evolution API

WHATSAPP_CAMPANHAS

Nome: Campanhas de WhatsApp
Descrição: Envio de mensagens em massa para clientes (promoções, lembretes de revisão)
Categoria: COMUNICACAO
Planos sugeridos: TURBINADO
Uso: Marketing e relacionamento com clientes

EMAIL_NOTIFICATIONS

Nome: Notificações por Email
Descrição: Envio automático de emails (confirmações, orçamentos, faturas)
Categoria: COMUNICACAO
Planos sugeridos: TODOS (com limites diferentes)
Uso: Sistema de emails transacionais

EMAIL_MARKETING

Nome: Email Marketing
Descrição: Campanhas de email marketing para base de clientes
Categoria: COMUNICACAO
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Newsletter, promoções

SMS_NOTIFICATIONS

Nome: Notificações via SMS
Descrição: Envio de SMS para clientes (lembretes, confirmações)
Categoria: COMUNICACAO
Planos sugeridos: TURBINADO
Uso: Canal adicional de comunicação

TELEGRAM_BOT

Nome: Bot do Telegram
Descrição: Bot interno para mecânicos consultarem status de OS
Categoria: COMUNICACAO
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Comunicação interna da equipe

PUSH_NOTIFICATIONS

Nome: Notificações Push (App Mobile)
Descrição: Push notifications no app mobile
Categoria: COMUNICACAO
Planos sugeridos: TODOS
Uso: Notificações em tempo real no celular

WEBHOOK_NOTIFICATIONS

Nome: Webhooks Personalizados
Descrição: Envio de eventos para URLs externas (integrações customizadas)
Categoria: COMUNICACAO
Planos sugeridos: TURBINADO
Uso: Integrações com sistemas externos

CHAT_INTERNO

Nome: Chat Interno entre Usuários
Descrição: Mensagens em tempo real entre membros da equipe
Categoria: COMUNICACAO
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Comunicação interna via WebSocket


2️⃣ RELATÓRIOS E EXPORTAÇÃO (12 flags)
PDF_EXPORT_BASICO

Nome: Exportação PDF Básica
Descrição: Gerar PDFs de OS, orçamentos e recibos
Categoria: RELATORIOS
Planos sugeridos: TODOS
Uso: iText para documentos simples

PDF_EXPORT_AVANCADO

Nome: Exportação PDF Avançada
Descrição: PDFs personalizados com logo, cores customizadas
Categoria: RELATORIOS
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Templates customizáveis

EXCEL_EXPORT

Nome: Exportação para Excel
Descrição: Exportar relatórios e listas para Excel (.xlsx)
Categoria: RELATORIOS
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Apache POI

CSV_EXPORT

Nome: Exportação para CSV
Descrição: Exportar dados em formato CSV
Categoria: RELATORIOS
Planos sugeridos: TODOS
Uso: Integração com outros sistemas

RELATORIOS_GERENCIAIS

Nome: Relatórios Gerenciais
Descrição: Relatórios complexos com gráficos (faturamento, performance)
Categoria: RELATORIOS
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: JasperReports

RELATORIOS_PERSONALIZADOS

Nome: Relatórios Personalizados
Descrição: Criar relatórios customizados com filtros avançados
Categoria: RELATORIOS
Planos sugeridos: TURBINADO
Uso: Query builder visual

DASHBOARD_BASICO

Nome: Dashboard Básico
Descrição: Dashboard com métricas essenciais (OS, faturamento)
Categoria: RELATORIOS
Planos sugeridos: TODOS
Uso: Cards de métricas simples

DASHBOARD_AVANCADO

Nome: Dashboard Avançado
Descrição: Dashboard com gráficos interativos ECharts
Categoria: RELATORIOS
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Apache ECharts, 50+ tipos de gráficos

RELATORIO_FISCAL

Nome: Relatórios Fiscais
Descrição: Relatórios para contabilidade e fisco
Categoria: RELATORIOS
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Conformidade fiscal


EXPORT_CONTABIL

Nome: Exportação Contábil
Descrição: Exportar movimentações em formato contábil (SPED)
Categoria: RELATORIOS
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Integração com sistemas contábeis

RELATORIO_AGENDADO

Nome: Relatórios Agendados
Descrição: Agendar envio automático de relatórios por email
Categoria: RELATORIOS
Planos sugeridos: TURBINADO
Uso: Cron jobs


3️⃣ FINANCEIRO E PAGAMENTOS (8 flags)
INTEGRACAO_MERCADO_PAGO

Nome: Integração Mercado Pago
Descrição: Pagamentos online via Mercado Pago (PIX, cartão, boleto)
Categoria: FINANCEIRO
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: SDK Mercado Pago

INTEGRACAO_STRIPE

Nome: Integração Stripe
Descrição: Pagamentos internacionais via Stripe
Categoria: FINANCEIRO
Planos sugeridos: TURBINADO
Uso: SDK Stripe

INTEGRACAO_PAGSEGURO

Nome: Integração PagSeguro
Descrição: Pagamentos via PagSeguro
Categoria: FINANCEIRO
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: SDK PagSeguro

PARCELAMENTO_CARTAO

Nome: Parcelamento no Cartão
Descrição: Permitir pagamento parcelado em até 12x
Categoria: FINANCEIRO
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Gateway de pagamento

SPLIT_PAYMENT

Nome: Split Payment (Divisão de Pagamento)
Descrição: Dividir pagamento entre múltiplas contas
Categoria: FINANCEIRO
Planos sugeridos: TURBINADO
Uso: Multi-filial

CONCILIACAO_BANCARIA

Nome: Conciliação Bancária
Descrição: Importar extratos bancários e conciliar automaticamente
Categoria: FINANCEIRO
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Integração OFX

FLUXO_CAIXA_AVANCADO

Nome: Fluxo de Caixa Avançado
Descrição: Projeções, DRE, análise de lucratividade
Categoria: FINANCEIRO
Planos sugeridos: TURBINADO
Uso: Gestão financeira completa

COBRANCA_RECORRENTE

Nome: Cobrança Recorrente
Descrição: Cobrar clientes automaticamente (assinaturas, planos)
Categoria: FINANCEIRO
Planos sugeridos: TURBINADO
Uso: Subscription billing


4️⃣ NOTA FISCAL ELETRÔNICA (5 flags)
EMISSAO_NFE

Nome: Emissão de NF-e
Descrição: Emitir Nota Fiscal Eletrônica (NF-e)
Categoria: FISCAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: SEFAZ webservices

EMISSAO_NFSE

Nome: Emissão de NFS-e
Descrição: Emitir Nota Fiscal de Serviço Eletrônica
Categoria: FISCAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Prefeitura webservices

EMISSAO_NFCE

Nome: Emissão de NFC-e
Descrição: Nota Fiscal de Consumidor Eletrônica (varejo)
Categoria: FISCAL
Planos sugeridos: TURBINADO
Uso: SEFAZ webservices

IMPORTACAO_XML_NFE

Nome: Importação de XML NF-e
Descrição: Importar peças direto do XML da nota fiscal de compra
Categoria: FISCAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Parser XML automatizado

MANIFESTACAO_DESTINATARIO

Nome: Manifestação do Destinatário
Descrição: Confirmar recebimento de NF-e (ciência, confirmação)
Categoria: FISCAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: SEFAZ webservices


5️⃣ OPERACIONAL E GESTÃO (11 flags)
AGENDAMENTO_OS

Nome: Agendamento de Ordens de Serviço
Descrição: Calendário para agendar OS com data/hora
Categoria: OPERACIONAL
Planos sugeridos: TODOS
Uso: Gestão de agenda

CHECKLIST_VISTORIA

Nome: Checklist de Vistoria
Descrição: Checklist digital ao receber veículo (pneus, lataria, combustível)
Categoria: OPERACIONAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Tablet/mobile

FOTOS_DOCUMENTACAO

Nome: Fotos e Anexos em OS
Descrição: Anexar fotos e documentos nas ordens de serviço
Categoria: OPERACIONAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Upload S3/storage

MANUTENCAO_PREVENTIVA

Nome: Manutenção Preventiva
Descrição: Alertas de revisão baseados em KM/tempo
Categoria: OPERACIONAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Lembrete automático

CONTROLE_GARANTIA

Nome: Controle de Garantia
Descrição: Gerenciar garantias de peças e serviços
Categoria: OPERACIONAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Data de vencimento

GESTAO_FORNECEDORES

Nome: Gestão de Fornecedores
Descrição: Cadastro de fornecedores, compras, histórico
Categoria: OPERACIONAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: CRUD fornecedores

ORDEM_COMPRA

Nome: Ordem de Compra
Descrição: Gerar ordens de compra para fornecedores
Categoria: OPERACIONAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Gestão de compras

CONTROLE_PONTO

Nome: Controle de Ponto
Descrição: Registrar entrada/saída de funcionários
Categoria: OPERACIONAL
Planos sugeridos: TURBINADO
Uso: Gestão de RH

COMISSAO_MECANICOS

Nome: Comissão de Mecânicos
Descrição: Calcular comissões por OS realizada
Categoria: OPERACIONAL
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Gestão financeira

MULTI_FILIAL

Nome: Multi-Filial
Descrição: Gerenciar múltiplas filiais da mesma oficina
Categoria: OPERACIONAL
Planos sugeridos: TURBINADO
Uso: Multi-tenant interno


6️⃣ INTEGRAÇÕES E API (7 flags)
API_PUBLICA

Nome: API Pública
Descrição: Acesso à API REST do PitStop para integrações
Categoria: INTEGRACAO
Planos sugeridos: TURBINADO
Uso: Documentação OpenAPI

WEBHOOK_EVENTOS

Nome: Webhooks de Eventos
Descrição: Receber eventos do sistema via webhook
Categoria: INTEGRACAO
Planos sugeridos: TURBINADO
Uso: Integrações externas

INTEGRACAO_ZAPIER

Nome: Integração com Zapier
Descrição: Conectar PitStop com 5000+ apps via Zapier
Categoria: INTEGRACAO
Planos sugeridos: TURBINADO
Uso: Zapier integration

INTEGRACAO_ERP

Nome: Integração com ERP
Descrição: Integrar com sistemas ERP externos
Categoria: INTEGRACAO
Planos sugeridos: TURBINADO
Uso: APIs de terceiros

INTEGRACAO_GOOGLE

Nome: Integração Google Workspace
Descrição: Sincronizar com Google Calendar, Drive, Sheets
Categoria: INTEGRACAO
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Google APIs

INTEGRACAO_MARKETPLACE

Nome: Integração com Marketplaces
Descrição: Vender peças em marketplaces (Mercado Livre, OLX)
Categoria: INTEGRACAO
Planos sugeridos: TURBINADO
Uso: APIs de marketplaces

SSO_SAML

Nome: Single Sign-On (SSO/SAML)
Descrição: Login único com sistemas corporativos
Categoria: INTEGRACAO
Planos sugeridos: TURBINADO
Uso: SAML 2.0


7️⃣ BACKUP E SEGURANÇA (5 flags)
BACKUP_AUTOMATICO

Nome: Backup Automático
Descrição: Backup diário automático dos dados
Categoria: SEGURANCA
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Cronjob S3

BACKUP_MANUAL

Nome: Backup Manual
Descrição: Baixar backup completo sob demanda
Categoria: SEGURANCA
Planos sugeridos: TODOS
Uso: Export completo

AUTENTICACAO_2FA

Nome: Autenticação 2FA
Descrição: Two-Factor Authentication (TOTP)
Categoria: SEGURANCA
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Google Authenticator

AUDITORIA_AVANCADA

Nome: Auditoria Avançada
Descrição: Log completo de todas ações dos usuários
Categoria: SEGURANCA
Planos sugeridos: TURBINADO
Uso: Compliance

RESTRICAO_IP

Nome: Restrição por IP
Descrição: Permitir acesso apenas de IPs específicos
Categoria: SEGURANCA
Planos sugeridos: TURBINADO
Uso: Whitelist IP


8️⃣ MOBILE E APPS (4 flags)
APP_MOBILE_CLIENTE

Nome: App Mobile para Cliente
Descrição: Aplicativo para clientes acompanharem suas OS
Categoria: MOBILE
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: React Native

APP_MOBILE_MECANICO

Nome: App Mobile para Mecânico
Descrição: App para mecânicos atualizarem OS em tempo real
Categoria: MOBILE
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: React Native

OFFLINE_MODE

Nome: Modo Offline
Descrição: Trabalhar sem internet e sincronizar depois
Categoria: MOBILE
Planos sugeridos: TURBINADO
Uso: IndexedDB/SQLite

QR_CODE_VEICULO

Nome: QR Code de Veículo
Descrição: Escanear QR Code para abrir ficha do veículo
Categoria: MOBILE
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Scanner QR Code


9️⃣ CUSTOMIZAÇÃO E BRANDING (4 flags)
LOGO_CUSTOMIZADA

Nome: Logo Customizada
Descrição: Usar logo própria em documentos e sistema
Categoria: BRANDING
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: White label parcial

CORES_CUSTOMIZADAS

Nome: Cores Personalizadas
Descrição: Customizar cores do sistema (tema)
Categoria: BRANDING
Planos sugeridos: TURBINADO
Uso: CSS variables

DOMINIO_PROPRIO

Nome: Domínio Próprio
Descrição: Usar domínio próprio (minhaoficina.com.br)
Categoria: BRANDING
Planos sugeridos: TURBINADO
Uso: White label completo

EMAIL_CUSTOMIZADO

Nome: Email Customizado
Descrição: Emails enviados do próprio domínio (@minhaoficina.com.br)
Categoria: BRANDING
Planos sugeridos: TURBINADO
Uso: SMTP próprio


🔟 MARKETING E CRM (5 flags)
CRM_BASICO

Nome: CRM Básico
Descrição: Histórico de interações com clientes
Categoria: MARKETING
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Timeline de contatos

PROGRAMA_FIDELIDADE

Nome: Programa de Fidelidade
Descrição: Pontos, descontos, cashback para clientes frequentes
Categoria: MARKETING
Planos sugeridos: TURBINADO
Uso: Sistema de pontos

PESQUISA_SATISFACAO

Nome: Pesquisa de Satisfação
Descrição: Enviar NPS/CSAT após finalização de OS
Categoria: MARKETING
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Forms automáticos

CUPONS_DESCONTO

Nome: Cupons de Desconto
Descrição: Criar e gerenciar cupons promocionais
Categoria: MARKETING
Planos sugeridos: PROFISSIONAL, TURBINADO
Uso: Promoções

INDICACAO_CLIENTES

Nome: Programa de Indicação
Descrição: Clientes ganham benefícios ao indicar novos clientes
Categoria: MARKETING
Planos sugeridos: TURBINADO
Uso: Referral program


📊 RESUMO POR CATEGORIA
COMUNICACAO          →  9 flags
RELATORIOS           → 12 flags
FINANCEIRO           →  8 flags
FISCAL               →  5 flags
OPERACIONAL          → 11 flags
INTEGRACAO           →  7 flags
SEGURANCA            →  5 flags
MOBILE               →  4 flags
BRANDING             →  4 flags
MARKETING            →  5 flags
─────────────────────────────────
TOTAL                → 70 flags

🎯 SUGESTÃO DE DISTRIBUIÇÃO POR PLANO
ECONÔMICO (15 flags básicas)

EMAIL_NOTIFICATIONS (50/mês)
PDF_EXPORT_BASICO
CSV_EXPORT
DASHBOARD_BASICO
AGENDAMENTO_OS
BACKUP_MANUAL
PUSH_NOTIFICATIONS
APP_MOBILE_CLIENTE (view only)
CRM_BASICO
INTEGRACAO_MERCADO_PAGO
LOGO_CUSTOMIZADA
AUTENTICACAO_2FA
QR_CODE_VEICULO
RELATORIO_FISCAL
CHECKLIST_VISTORIA

PROFISSIONAL (35 flags)

Todas do ECONÔMICO +
WHATSAPP_NOTIFICATIONS
EMAIL_MARKETING
TELEGRAM_BOT
PDF_EXPORT_AVANCADO
EXCEL_EXPORT
DASHBOARD_AVANCADO
RELATORIOS_GERENCIAIS
EMISSAO_NFE
EMISSAO_NFSE
IMPORTACAO_XML_NFE
MANIFESTACAO_DESTINATARIO
FOTOS_DOCUMENTACAO
RASTREAMENTO_VEICULO
MANUTENCAO_PREVENTIVA
CONTROLE_GARANTIA
GESTAO_FORNECEDORES
ORDEM_COMPRA
COMISSAO_MECANICOS
BACKUP_AUTOMATICO
APP_MOBILE_MECANICO
PESQUISA_SATISFACAO
CUPONS_DESCONTO
INTEGRACAO_GOOGLE
PARCELAMENTO_CARTAO
CONCILIACAO_BANCARIA
CHAT_INTERNO
AUDITORIA_AVANCADA
RELATORIO_AGENDADO
EXPORT_CONTABIL

TURBINADO (TODAS - 70 flags)

Todas do PROFISSIONAL +
WHATSAPP_CAMPANHAS
SMS_NOTIFICATIONS
WEBHOOK_NOTIFICATIONS
RELATORIOS_PERSONALIZADOS
ANALISE_PREDITIVA
INTEGRACAO_STRIPE
INTEGRACAO_PAGSEGURO
SPLIT_PAYMENT
FLUXO_CAIXA_AVANCADO
COBRANCA_RECORRENTE
EMISSAO_NFCE
CONTROLE_PONTO
MULTI_FILIAL
API_PUBLICA
WEBHOOK_EVENTOS
INTEGRACAO_ZAPIER
INTEGRACAO_ERP
INTEGRACAO_MARKETPLACE
SSO_SAML
RESTRICAO_IP
OFFLINE_MODE
CORES_CUSTOMIZADAS
DOMINIO_PROPRIO
EMAIL_CUSTOMIZADO
PROGRAMA_FIDELIDADE
INDICACAO_CLIENTES


todas outras restantes




💡 PRIORIZAÇÃO PARA IMPLEMENTAÇÃO
FASE 1 - MVP (10 flags essenciais)

EMAIL_NOTIFICATIONS
PDF_EXPORT_BASICO
DASHBOARD_BASICO
AGENDAMENTO_OS
BACKUP_MANUAL
INTEGRACAO_MERCADO_PAGO
EMISSAO_NFE
WHATSAPP_NOTIFICATIONS
EXCEL_EXPORT
RELATORIOS_GERENCIAIS

FASE 2 - Crescimento (20 flags)

Adicionar features de diferenciação premium
Mobile apps
Integrações avançadas
Customização

FASE 3 - Enterprise (40 flags restantes)

Features de grande porte
Multi-tenant avançado
API pública
White label completo


📋 ANÁLISE DO DOCUMENTO features-flags.md

1️⃣ MODAL DE CRIAÇÃO/EDIÇÃO - CAMPOS FALTANTES

1.1 Período de Validade (Data Início e Fim)

| Item                                           | Status   |
  |------------------------------------------------|----------|
| Campos dataInicio e dataFim no backend         | ✅ FEITO |
| Campos dataInicio e dataFim no DTO/Types       | ✅ FEITO |
| Inputs datetime-local no modal                 | ❌ FALTA |
| Validação data fim > data início               | ❌ FALTA |
| Hint "Deixe em branco para feature permanente" | ❌ FALTA |

1.2 Oficinas Específicas (Beta Testers)

| Item                                             | Status   |
  |--------------------------------------------------|----------|
| Campo habilitado_por_oficina no backend (UUID[]) | ✅ FEITO |
| Campo habilitadoPorOficina no Types              | ✅ FEITO |
| Exibição de contador no card expandido           | ✅ FEITO |
| Botão "+ Adicionar oficinas"                     | ❌ FALTA |
| Modal de seleção de oficinas                     | ❌ FALTA |
| Busca por nome/CNPJ                              | ❌ FALTA |
| Lista paginada com checkbox                      | ❌ FALTA |
| Badge visual mostrando plano                     | ❌ FALTA |
| Hint explicativo                                 | ❌ FALTA |

1.3 Preview de Impacto

| Item                                        | Status   |
  |---------------------------------------------|----------|
| Card com cálculo de impacto                 | ❌ FALTA |
| Cálculo: se global → todas oficinas         | ❌ FALTA |
| Cálculo: se planos → oficinas nesses planos | ❌ FALTA |
| Cálculo: se rollout → percentual aplicado   | ❌ FALTA |

1.4 Templates Rápidos

| Item                               | Status   |
  |------------------------------------|----------|
| Link "Usar template"               | ❌ FALTA |
| Grid de templates pré-configurados | ❌ FALTA |
| Template "WhatsApp para PRO"       | ❌ FALTA |
| Template "Beta Test 25%"           | ❌ FALTA |
| Template "Premium Exclusivo"       | ❌ FALTA |
| Template "Teste Temporário"        | ❌ FALTA |

  ---
2️⃣ LISTAGEM DE FEATURES

2.1 Estrutura Geral

| Item                                             | Status                             |
  |--------------------------------------------------|------------------------------------|
| Lista de features                                | ✅ FEITO                           |
| Agrupar por categoria (accordion/seções)         | ❌ FALTA                           |
| Header de categoria com ícone                    | ❌ FALTA                           |
| Ícones por categoria (Flag, MessageCircle, etc.) | ❌ FALTA (só tem cores diferentes) |

2.2 Card de Feature (Modo Compacto)

| Item                                      | Status                                      |
  |-------------------------------------------|---------------------------------------------|
| Indicador de status (bolinha verde/cinza) | ⚠️ PARCIAL (usa toggle ao invés de bolinha) |
| Código da feature (font-mono)             | ✅ FEITO                                    |
| Badge de rollout (laranja)                | ✅ FEITO                                    |
| Badge de oficinas específicas (azul)      | ✅ FEITO                                    |
| Ícone de cadeado (requer autorização)     | ❌ FALTA                                    |
| Nome da feature                           | ✅ FEITO                                    |
| Descrição                                 | ✅ FEITO                                    |
| Planos habilitados com ícone              | ✅ FEITO                                    |
| Período de validade com ícone             | ⚠️ PARCIAL (só no modo expandido)           |
| Tempo desde criação                       | ❌ FALTA (só tem data absoluta)             |
| Switch/Toggle global                      | ✅ FEITO                                    |
| Menu dropdown (3 pontinhos)               | ❌ FALTA (ações estão expostas)             |

2.3 Card de Feature (Modo Expandido)

| Item                                                     | Status                 |
  |----------------------------------------------------------|------------------------|
| Botão expandir/colapsar                                  | ✅ FEITO               |
| Área expandida com fundo mais escuro                     | ✅ FEITO               |
| Status Global: Ativo/Inativo                             | ⚠️ PARCIAL             |
| Planos: lista                                            | ✅ FEITO               |
| Rollout: X%                                              | ✅ FEITO               |
| Autorização: Requerida                                   | ✅ FEITO               |
| Período: datas                                           | ✅ FEITO               |
| Estatísticas de uso (oficinas, usos, última verificação) | ❌ FALTA               |
| Lista de Beta Testers                                    | ❌ FALTA (só contador) |

2.4 Checkbox de Seleção Múltipla

| Item                                           | Status   |
  |------------------------------------------------|----------|
| Checkbox em cada card                          | ❌ FALTA |
| Barra de ações em massa                        | ❌ FALTA |
| Botões: Ativar Todas, Desativar Todas, Excluir | ❌ FALTA |
| Modal de confirmação em massa                  | ❌ FALTA |

  ---
3️⃣ SISTEMA DE FILTROS FUNCIONAIS

3.1 Barra de Filtros

| Item                               | Status                            |
  |------------------------------------|-----------------------------------|
| Campo de Busca (nome/código)       | ✅ FEITO                          |
| Busca em tempo real                | ✅ FEITO                          |
| Dropdown de Categoria              | ✅ FEITO                          |
| Dropdown de Status (Ativo/Inativo) | ❌ FALTA                          |
| Dropdown de Plano                  | ❌ FALTA                          |
| Filtros combinados com AND         | ⚠️ PARCIAL (só busca + categoria) |

3.2 Estado Vazio Específico de Filtros

| Item                                    | Status                |
  |-----------------------------------------|-----------------------|
| Empty state quando filtros não retornam | ⚠️ PARCIAL (genérico) |
| Ícone de filtro/funil                   | ❌ FALTA              |
| Botão "Limpar todos os filtros"         | ❌ FALTA              |

  ---
4️⃣ MELHORIAS NO EMPTY STATE INICIAL

| Item                                     | Status                           |
  |------------------------------------------|----------------------------------|
| Ícone grande de flag                     | ✅ FEITO                         |
| Título "Nenhuma feature flag encontrada" | ✅ FEITO                         |
| Descrição explicativa                    | ❌ FALTA                         |
| Botão "+ Criar Primeira Feature Flag"    | ❌ FALTA (só aparece o genérico) |
| Card informativo com exemplos de uso     | ❌ FALTA                         |

  ---
5️⃣ VALIDAÇÕES E REGRAS DE NEGÓCIO

5.1 Validações de Formulário

| Item                                           | Status             |
  |------------------------------------------------|--------------------|
| Código obrigatório                             | ✅ FEITO           |
| Código apenas maiúsculas, números, underscores | ✅ FEITO           |
| Código único                                   | ✅ FEITO (backend) |
| Hint do código                                 | ✅ FEITO           |
| Nome obrigatório                               | ✅ FEITO           |
| Nome máximo 100 caracteres                     | ❌ FALTA           |
| Categoria obrigatório                          | ✅ FEITO           |
| Datas: fim > início                            | ❌ FALTA           |
| Datas: não no passado                          | ❌ FALTA           |
| Rollout 0-100                                  | ✅ FEITO           |
| Hint do rollout                                | ✅ FEITO           |
| Validação: pelo menos 1 regra ativa            | ❌ FALTA           |

5.2 Regras de Habilitação (Hierarquia no Backend)

| Item                          | Status   |
  |-------------------------------|----------|
| Global OFF → Bloqueado        | ✅ FEITO |
| Oficinas Específicas → bypass | ✅ FEITO |
| Planos → verificação          | ✅ FEITO |
| Rollout → hash percentual     | ✅ FEITO |
| Período → verificação         | ✅ FEITO |

5.3 Comportamento do Toggle Global

| Item                           | Status     |
  |--------------------------------|------------|
| Modal confirmação ao DESATIVAR | ❌ FALTA   |
| Mostrar oficinas afetadas      | ❌ FALTA   |
| Toggle visual verde/cinza      | ✅ FEITO   |
| Transição suave                | ⚠️ PARCIAL |

  ---
6️⃣ ESTATÍSTICAS E CARDS DO TOPO

6.1 Cálculo das Métricas

| Item                         | Status   |
  |------------------------------|----------|
| Card Total Features          | ✅ FEITO |
| Card Habilitadas Globalmente | ✅ FEITO |
| Card Por Plano               | ✅ FEITO |
| Card Em Rollout              | ✅ FEITO |

6.2 Visual dos Cards

| Item                        | Status   |
  |-----------------------------|----------|
| Ícone colorido              | ✅ FEITO |
| Label descritivo            | ✅ FEITO |
| Número grande               | ✅ FEITO |
| Link "Ver todas" que filtra | ❌ FALTA |

  ---
7️⃣ EXPERIÊNCIA DO USUÁRIO

7.1 Loading States

| Item                                | Status                |
  |-------------------------------------|-----------------------|
| Skeleton nos cards de estatística   | ❌ FALTA (só spinner) |
| Skeleton na listagem                | ❌ FALTA (só spinner) |
| Botão "Criando..." com spinner      | ✅ FEITO              |
| Modal não fecha até resposta        | ✅ FEITO              |
| Toggle desabilitado durante request | ✅ FEITO              |

7.2 Feedback de Sucesso/Erro

| Item                              | Status                    |
  |-----------------------------------|---------------------------|
| Toast sucesso criar               | ✅ FEITO                  |
| Toast sucesso editar              | ✅ FEITO                  |
| Toast sucesso excluir             | ✅ FEITO                  |
| Toast erro                        | ✅ FEITO                  |
| Modal confirmação excluir         | ✅ FEITO (confirm nativo) |
| Nova feature destacada (animação) | ❌ FALTA                  |

7.3 Animações e Transições

| Item                   | Status     |
  |------------------------|------------|
| Hover cards (shadow)   | ❌ FALTA   |
| Expand/Collapse suave  | ❌ FALTA   |
| Filtros fade           | ❌ FALTA   |
| Toggle transição suave | ⚠️ PARCIAL |
| Toast slide-in         | ⚠️ PARCIAL |

  ---
8️⃣ RESPONSIVIDADE

| Item                         | Status                      |
  |------------------------------|-----------------------------|
| Desktop layout               | ✅ FEITO                    |
| Tablet layout                | ⚠️ NÃO TESTADO              |
| Mobile layout                | ⚠️ NÃO TESTADO              |
| Estatísticas grid 2x2 mobile | ❌ FALTA (sempre 4 colunas) |

  ---
9️⃣ ACESSIBILIDADE

| Item                      | Status     |
  |---------------------------|------------|
| Labels em inputs          | ✅ FEITO   |
| aria-describedby em erros | ❌ FALTA   |
| Foco visível              | ⚠️ PARCIAL |
| Trap de foco no modal     | ❌ FALTA   |
| ESC fecha modal           | ❌ FALTA   |
| aria-modal="true"         | ❌ FALTA   |

  ---
🎚️ LISTA DE FEATURE FLAGS (70 FLAGS)

Categorias

| Categoria   | No Documento | Implementado | Status   |
  |-------------|--------------|--------------|----------|
| GERAL       | ✅           | ✅           | ✅ FEITO |
| COMUNICACAO | ✅           | ✅           | ✅ FEITO |
| RELATORIOS  | ✅           | ✅           | ✅ FEITO |
| PREMIUM     | ✅           | ✅           | ✅ FEITO |
| FINANCEIRO  | ✅           | ✅           | ✅ FEITO |
| INTEGRACAO  | ✅           | ✅           | ✅ FEITO |
| OPERACIONAL | ✅           | ✅           | ✅ FEITO |
| FISCAL      | ✅           | ❌           | ❌ FALTA |
| MOBILE      | ✅           | ❌           | ❌ FALTA |
| BRANDING    | ✅           | ❌           | ❌ FALTA |
| MARKETING   | ✅           | ❌           | ❌ FALTA |
| SEGURANCA   | ✅           | ❌           | ❌ FALTA |

Feature Flags Seedadas

| Documento propõe | Migração atual   | Status       |
  |------------------|------------------|--------------|
| 70 Feature Flags | 10 Feature Flags | ⚠️ 60 FALTAM |

  ---
📊 RESUMO EXECUTIVO

✅ O QUE ESTÁ FEITO (35%)

Backend

- Tabela feature_flags completa com todos os campos
- CRUD endpoints funcionando
- Lógica de habilitação com hierarquia (global, oficina, plano, rollout, período)
- DTOs e Types corretos

Frontend - Listagem

- Cards com informações básicas
- Toggle global funcionando
- Modo expandido com detalhes
- Filtro por categoria
- Busca por nome/código
- Cards de estatísticas no topo

Frontend - Modal

- Campos: código, nome, descrição, categoria
- Habilitar por plano (pills)
- Percentual rollout (slider)
- Requer autorização (checkbox)
- Habilitado global (checkbox)

  ---
❌ O QUE FALTA (65%)

Alta Prioridade (Funcionalidade)

1. Campos de data início/fim no modal - Backend suporta mas frontend não tem
2. Modal de seleção de oficinas específicas - Backend suporta mas não tem UI
3. Filtro por status (ativo/inativo) - Fácil
4. Adicionar 60 feature flags faltantes - Nova migração
5. Adicionar 5 categorias faltantes - FISCAL, MOBILE, BRANDING, MARKETING, SEGURANCA

Média Prioridade (UX)

6. Preview de impacto no modal - Precisa endpoint de contagem
7. Confirmação ao desativar globalmente - Modal com count
8. Agrupar por categoria - Accordion/seções
9. Link "Ver todas" nos cards de estatísticas - Aplica filtro
10. Empty state melhorado - Com exemplos

Baixa Prioridade (Polish)

11. Templates rápidos
12. Seleção múltipla + ações em massa
13. Skeletons de loading
14. Animações de hover/expand
15. Acessibilidade completa (aria, trap focus)
16. Responsividade mobile
