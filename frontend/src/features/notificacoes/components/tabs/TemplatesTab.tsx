import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mail, MessageSquare, Eye, Save, Clock, X, Send, Smile, Bell, Smartphone, Loader2 } from 'lucide-react';
import { useNotificacoes } from '../../hooks/useNotificacoes';
import type { TipoNotificacao, StatusNotificacao } from '../../types';

interface Template {
  evento: string;
  canal: string;
  assunto: string;
  mensagem: string;
  ativo: boolean;
}

interface Variable {
  key: string;
  label: string;
  description: string;
}

const eventos = [
  { value: 'OS_CRIADA', label: 'Ordem criada' },
  { value: 'OS_AGUARDANDO_APROVACAO', label: 'Aguardando aprovação' },
  { value: 'OS_APROVADA', label: 'Ordem aprovada' },
  { value: 'OS_EM_ANDAMENTO', label: 'Em andamento' },
  { value: 'OS_AGUARDANDO_PECA', label: 'Aguardando peça' },
  { value: 'OS_FINALIZADA', label: 'Ordem finalizada' },
  { value: 'OS_ENTREGUE', label: 'Ordem entregue' },
  { value: 'PAGAMENTO_PENDENTE', label: 'Pagamento pendente' },
  { value: 'PAGAMENTO_CONFIRMADO', label: 'Pagamento confirmado' },
  { value: 'LEMBRETE_RETIRADA', label: 'Lembrete de retirada' },
  { value: 'LEMBRETE_REVISAO', label: 'Lembrete de revisão' },
];

const canais = [
  { value: 'WHATSAPP', label: 'WhatsApp', icon: MessageSquare, supportsEmoji: true },
  { value: 'TELEGRAM', label: 'Telegram', icon: Send, supportsEmoji: true },
  { value: 'EMAIL', label: 'E-mail', icon: Mail, supportsEmoji: false },
];

// Emojis populares para mensagens de oficina
const emojiCategories = [
  {
    name: 'Veículos',
    emojis: ['🚗', '🚙', '🏎️', '🚕', '🛻', '🏍️', '🛵', '🚐', '🚚', '⚙️'],
  },
  {
    name: 'Status',
    emojis: ['✅', '⏳', '🔧', '🔩', '🛠️', '⚠️', '❌', '✔️', '🔴', '🟢'],
  },
  {
    name: 'Pagamento',
    emojis: ['💰', '💵', '💳', '🧾', '📋', '📝', '💲', '🏦', '💸', '🪙'],
  },
  {
    name: 'Comunicação',
    emojis: ['📱', '📞', '💬', '📧', '📨', '🔔', '📢', '👋', '👍', '🤝'],
  },
  {
    name: 'Tempo',
    emojis: ['⏰', '📅', '🕐', '⌛', '📆', '🗓️', '⏱️', '🌅', '🌙', '☀️'],
  },
  {
    name: 'Celebração',
    emojis: ['🎉', '🎊', '⭐', '🌟', '🏆', '👏', '🙌', '😊', '🥳', '💪'],
  },
];

const variables: Variable[] = [
  { key: '{{cliente_nome}}', label: 'Nome do Cliente', description: 'Nome completo do cliente' },
  { key: '{{placa}}', label: 'Placa', description: 'Placa do veículo' },
  { key: '{{modelo}}', label: 'Modelo', description: 'Marca e modelo do veículo' },
  { key: '{{ordem_id}}', label: 'Número da OS', description: 'Número da ordem de serviço' },
  { key: '{{valor_total}}', label: 'Valor Total', description: 'Valor total formatado' },
  { key: '{{data_previsao}}', label: 'Data Previsão', description: 'Data prevista de entrega' },
  { key: '{{status}}', label: 'Status', description: 'Status atual da OS' },
  { key: '{{nome_oficina}}', label: 'Nome Oficina', description: 'Nome da oficina' },
  { key: '{{telefone_oficina}}', label: 'Telefone', description: 'Telefone da oficina' },
  { key: '{{link_aprovacao}}', label: 'Link Aprovação', description: 'Link para aprovar orçamento' },
];

const sampleData: Record<string, string> = {
  cliente_nome: 'Carlos Almeida',
  placa: 'ABC1D23',
  modelo: 'Toyota Corolla 2019',
  ordem_id: 'OS-10458',
  valor_total: 'R$ 1.280,00',
  data_previsao: '27/12/2025',
  status: 'Em andamento',
  nome_oficina: 'Auto Center PitStop',
  telefone_oficina: '(11) 99999-9999',
  link_aprovacao: 'https://pitstop.com/aprovar/abc123',
};

const defaultTemplates: Record<string, Record<string, Template>> = {
  OS_CRIADA: {
    WHATSAPP: {
      evento: 'OS_CRIADA',
      canal: 'WHATSAPP',
      assunto: '',
      mensagem: `🔧 *Ordem de Serviço Criada*

Olá {{cliente_nome}}! 👋

Sua ordem de serviço nº {{ordem_id}} foi criada.

🚗 Veículo: {{modelo}} - {{placa}}

Assim que tivermos o diagnóstico, entraremos em contato.

{{nome_oficina}}
📞 {{telefone_oficina}}`,
      ativo: true,
    },
    TELEGRAM: {
      evento: 'OS_CRIADA',
      canal: 'TELEGRAM',
      assunto: '',
      mensagem: `🔧 *Ordem de Serviço Criada*

Olá {{cliente_nome}}! 👋

Sua ordem de serviço nº {{ordem_id}} foi criada.

🚗 Veículo: {{modelo}} - {{placa}}

Assim que tivermos o diagnóstico, entraremos em contato.

{{nome_oficina}}
📞 {{telefone_oficina}}`,
      ativo: true,
    },
    EMAIL: {
      evento: 'OS_CRIADA',
      canal: 'EMAIL',
      assunto: 'Ordem de Serviço {{ordem_id}} Criada',
      mensagem: `Olá {{cliente_nome}},

Sua ordem de serviço nº {{ordem_id}} foi criada em nosso sistema.

Dados do veículo:
- Modelo: {{modelo}}
- Placa: {{placa}}

Assim que tivermos o diagnóstico completo, entraremos em contato para aprovação do orçamento.

Atenciosamente,
{{nome_oficina}}`,
      ativo: true,
    },
  },
  OS_AGUARDANDO_APROVACAO: {
    WHATSAPP: {
      evento: 'OS_AGUARDANDO_APROVACAO',
      canal: 'WHATSAPP',
      assunto: '',
      mensagem: `📋 *Orçamento Pronto*

Olá {{cliente_nome}}! 👋

O orçamento da OS {{ordem_id}} está pronto para aprovação.

🚗 Veículo: {{modelo}} - {{placa}}
💰 Valor: {{valor_total}}

✅ Clique para aprovar: {{link_aprovacao}}

{{nome_oficina}}`,
      ativo: true,
    },
    TELEGRAM: {
      evento: 'OS_AGUARDANDO_APROVACAO',
      canal: 'TELEGRAM',
      assunto: '',
      mensagem: `📋 *Orçamento Pronto*

Olá {{cliente_nome}}! 👋

O orçamento da OS {{ordem_id}} está pronto para aprovação.

🚗 Veículo: {{modelo}} - {{placa}}
💰 Valor: {{valor_total}}

✅ Clique para aprovar: {{link_aprovacao}}

{{nome_oficina}}`,
      ativo: true,
    },
    EMAIL: {
      evento: 'OS_AGUARDANDO_APROVACAO',
      canal: 'EMAIL',
      assunto: 'Orçamento Pronto - OS {{ordem_id}}',
      mensagem: `Olá {{cliente_nome}},

O orçamento da sua ordem de serviço está pronto para aprovação.

Detalhes:
- OS: {{ordem_id}}
- Veículo: {{modelo}} - {{placa}}
- Valor Total: {{valor_total}}

Clique no link abaixo para aprovar ou recusar:
{{link_aprovacao}}

Atenciosamente,
{{nome_oficina}}`,
      ativo: true,
    },
  },
  OS_APROVADA: {
    WHATSAPP: {
      evento: 'OS_APROVADA',
      canal: 'WHATSAPP',
      assunto: '',
      mensagem: `✅ *Orçamento Aprovado*

Olá {{cliente_nome}}! 👋

Seu orçamento foi aprovado com sucesso!

🚗 Veículo: {{modelo}} - {{placa}}
📋 OS: {{ordem_id}}

⏳ Em breve iniciaremos os serviços.

{{nome_oficina}}`,
      ativo: true,
    },
    TELEGRAM: {
      evento: 'OS_APROVADA',
      canal: 'TELEGRAM',
      assunto: '',
      mensagem: `✅ *Orçamento Aprovado*

Olá {{cliente_nome}}! 👋

Seu orçamento foi aprovado com sucesso!

🚗 Veículo: {{modelo}} - {{placa}}
📋 OS: {{ordem_id}}

⏳ Em breve iniciaremos os serviços.

{{nome_oficina}}`,
      ativo: true,
    },
    EMAIL: {
      evento: 'OS_APROVADA',
      canal: 'EMAIL',
      assunto: 'Orçamento Aprovado - OS {{ordem_id}}',
      mensagem: `Olá {{cliente_nome}},

Seu orçamento foi aprovado com sucesso!

- Veículo: {{modelo}} - {{placa}}
- OS: {{ordem_id}}

Em breve iniciaremos os serviços.

Atenciosamente,
{{nome_oficina}}`,
      ativo: true,
    },
  },
  OS_EM_ANDAMENTO: {
    WHATSAPP: {
      evento: 'OS_EM_ANDAMENTO',
      canal: 'WHATSAPP',
      assunto: '',
      mensagem: `🛠️ *Serviço Iniciado*

Olá {{cliente_nome}}! 👋

O serviço do seu veículo foi iniciado!

🚗 {{modelo}} - {{placa}}
📋 OS: {{ordem_id}}
📅 Previsão: {{data_previsao}}

{{nome_oficina}}`,
      ativo: true,
    },
    TELEGRAM: {
      evento: 'OS_EM_ANDAMENTO',
      canal: 'TELEGRAM',
      assunto: '',
      mensagem: `🛠️ *Serviço Iniciado*

Olá {{cliente_nome}}! 👋

O serviço do seu veículo foi iniciado!

🚗 {{modelo}} - {{placa}}
📋 OS: {{ordem_id}}
📅 Previsão: {{data_previsao}}

{{nome_oficina}}`,
      ativo: true,
    },
    EMAIL: {
      evento: 'OS_EM_ANDAMENTO',
      canal: 'EMAIL',
      assunto: 'Serviço Iniciado - OS {{ordem_id}}',
      mensagem: `Olá {{cliente_nome}},

O serviço do seu veículo foi iniciado!

- Veículo: {{modelo}} - {{placa}}
- OS: {{ordem_id}}
- Previsão de entrega: {{data_previsao}}

Atenciosamente,
{{nome_oficina}}`,
      ativo: true,
    },
  },
  OS_FINALIZADA: {
    WHATSAPP: {
      evento: 'OS_FINALIZADA',
      canal: 'WHATSAPP',
      assunto: '',
      mensagem: `🎉 *Veículo Pronto!*

Olá {{cliente_nome}}! 👋

Ótima notícia! Seu veículo está pronto para retirada! 🚗

📋 OS: {{ordem_id}}
🚗 Veículo: {{modelo}} - {{placa}}
💰 Valor: {{valor_total}}

Aguardamos você!

{{nome_oficina}}
📞 {{telefone_oficina}}`,
      ativo: true,
    },
    TELEGRAM: {
      evento: 'OS_FINALIZADA',
      canal: 'TELEGRAM',
      assunto: '',
      mensagem: `🎉 *Veículo Pronto!*

Olá {{cliente_nome}}! 👋

Ótima notícia! Seu veículo está pronto para retirada! 🚗

📋 OS: {{ordem_id}}
🚗 Veículo: {{modelo}} - {{placa}}
💰 Valor: {{valor_total}}

Aguardamos você!

{{nome_oficina}}
📞 {{telefone_oficina}}`,
      ativo: true,
    },
    EMAIL: {
      evento: 'OS_FINALIZADA',
      canal: 'EMAIL',
      assunto: 'Veículo Pronto - OS {{ordem_id}}',
      mensagem: `Olá {{cliente_nome}},

Temos o prazer de informar que seu veículo está pronto para retirada!

Detalhes:
- OS: {{ordem_id}}
- Veículo: {{modelo}} - {{placa}}
- Valor Total: {{valor_total}}

Aguardamos sua visita para retirada do veículo.

Atenciosamente,
{{nome_oficina}}
{{telefone_oficina}}`,
      ativo: true,
    },
  },
  OS_ENTREGUE: {
    WHATSAPP: {
      evento: 'OS_ENTREGUE',
      canal: 'WHATSAPP',
      assunto: '',
      mensagem: `✅ *Veículo Entregue*

Olá {{cliente_nome}}! 👋

Confirmamos a entrega do seu veículo! 🚗

📋 OS: {{ordem_id}}
🚗 Veículo: {{placa}}

Obrigado pela preferência! 🙏

{{nome_oficina}}`,
      ativo: true,
    },
    TELEGRAM: {
      evento: 'OS_ENTREGUE',
      canal: 'TELEGRAM',
      assunto: '',
      mensagem: `✅ *Veículo Entregue*

Olá {{cliente_nome}}! 👋

Confirmamos a entrega do seu veículo! 🚗

📋 OS: {{ordem_id}}
🚗 Veículo: {{placa}}

Obrigado pela preferência! 🙏

{{nome_oficina}}`,
      ativo: true,
    },
    EMAIL: {
      evento: 'OS_ENTREGUE',
      canal: 'EMAIL',
      assunto: 'Veículo Entregue - OS {{ordem_id}}',
      mensagem: `Olá {{cliente_nome}},

Confirmamos a entrega do seu veículo ({{placa}}).

Obrigado pela preferência!

Atenciosamente,
{{nome_oficina}}`,
      ativo: true,
    },
  },
  PAGAMENTO_PENDENTE: {
    WHATSAPP: {
      evento: 'PAGAMENTO_PENDENTE',
      canal: 'WHATSAPP',
      assunto: '',
      mensagem: `💳 *Pagamento Pendente*

Olá {{cliente_nome}}! 👋

Há um pagamento pendente referente à OS {{ordem_id}}.

💰 Valor: {{valor_total}}
📅 Vencimento: {{data_previsao}}

{{nome_oficina}}`,
      ativo: true,
    },
    TELEGRAM: {
      evento: 'PAGAMENTO_PENDENTE',
      canal: 'TELEGRAM',
      assunto: '',
      mensagem: `💳 *Pagamento Pendente*

Olá {{cliente_nome}}! 👋

Há um pagamento pendente referente à OS {{ordem_id}}.

💰 Valor: {{valor_total}}
📅 Vencimento: {{data_previsao}}

{{nome_oficina}}`,
      ativo: true,
    },
    EMAIL: {
      evento: 'PAGAMENTO_PENDENTE',
      canal: 'EMAIL',
      assunto: 'Pagamento Pendente - OS {{ordem_id}}',
      mensagem: `Olá {{cliente_nome}},

Há um pagamento pendente referente à OS {{ordem_id}}.

- Valor: {{valor_total}}
- Vencimento: {{data_previsao}}

Atenciosamente,
{{nome_oficina}}`,
      ativo: true,
    },
  },
  PAGAMENTO_CONFIRMADO: {
    WHATSAPP: {
      evento: 'PAGAMENTO_CONFIRMADO',
      canal: 'WHATSAPP',
      assunto: '',
      mensagem: `✅ *Pagamento Confirmado*

Olá {{cliente_nome}}! 👋

Seu pagamento foi confirmado com sucesso! 🎉

📋 OS: {{ordem_id}}
💰 Valor: {{valor_total}}

Obrigado! 🙏

{{nome_oficina}}`,
      ativo: true,
    },
    TELEGRAM: {
      evento: 'PAGAMENTO_CONFIRMADO',
      canal: 'TELEGRAM',
      assunto: '',
      mensagem: `✅ *Pagamento Confirmado*

Olá {{cliente_nome}}! 👋

Seu pagamento foi confirmado com sucesso! 🎉

📋 OS: {{ordem_id}}
💰 Valor: {{valor_total}}

Obrigado! 🙏

{{nome_oficina}}`,
      ativo: true,
    },
    EMAIL: {
      evento: 'PAGAMENTO_CONFIRMADO',
      canal: 'EMAIL',
      assunto: 'Pagamento Confirmado - OS {{ordem_id}}',
      mensagem: `Olá {{cliente_nome}},

Seu pagamento foi confirmado com sucesso!

- OS: {{ordem_id}}
- Valor: {{valor_total}}

Obrigado!

Atenciosamente,
{{nome_oficina}}`,
      ativo: true,
    },
  },
  LEMBRETE_RETIRADA: {
    WHATSAPP: {
      evento: 'LEMBRETE_RETIRADA',
      canal: 'WHATSAPP',
      assunto: '',
      mensagem: `⏰ *Lembrete: Veículo Pronto*

Olá {{cliente_nome}}! 👋

Seu veículo está pronto para retirada! 🚗

📋 OS: {{ordem_id}}
🚗 Veículo: {{modelo}} - {{placa}}

Aguardamos você!

{{nome_oficina}}
📞 {{telefone_oficina}}`,
      ativo: true,
    },
    TELEGRAM: {
      evento: 'LEMBRETE_RETIRADA',
      canal: 'TELEGRAM',
      assunto: '',
      mensagem: `⏰ *Lembrete: Veículo Pronto*

Olá {{cliente_nome}}! 👋

Seu veículo está pronto para retirada! 🚗

📋 OS: {{ordem_id}}
🚗 Veículo: {{modelo}} - {{placa}}

Aguardamos você!

{{nome_oficina}}
📞 {{telefone_oficina}}`,
      ativo: true,
    },
    EMAIL: {
      evento: 'LEMBRETE_RETIRADA',
      canal: 'EMAIL',
      assunto: 'Lembrete: Seu Veículo Está Pronto',
      mensagem: `Olá {{cliente_nome}},

Este é um lembrete de que seu veículo está pronto para retirada.

- OS: {{ordem_id}}
- Veículo: {{modelo}} - {{placa}}

Por favor, agende a retirada.

Atenciosamente,
{{nome_oficina}}
{{telefone_oficina}}`,
      ativo: true,
    },
  },
  LEMBRETE_REVISAO: {
    WHATSAPP: {
      evento: 'LEMBRETE_REVISAO',
      canal: 'WHATSAPP',
      assunto: '',
      mensagem: `🔧 *Hora da Revisão!*

Olá {{cliente_nome}}! 👋

Está na hora de fazer a revisão do seu veículo! 🚗

🚗 {{modelo}} - {{placa}}

Agende sua visita conosco!

{{nome_oficina}}
📞 {{telefone_oficina}}`,
      ativo: true,
    },
    TELEGRAM: {
      evento: 'LEMBRETE_REVISAO',
      canal: 'TELEGRAM',
      assunto: '',
      mensagem: `🔧 *Hora da Revisão!*

Olá {{cliente_nome}}! 👋

Está na hora de fazer a revisão do seu veículo! 🚗

🚗 {{modelo}} - {{placa}}

Agende sua visita conosco!

{{nome_oficina}}
📞 {{telefone_oficina}}`,
      ativo: true,
    },
    EMAIL: {
      evento: 'LEMBRETE_REVISAO',
      canal: 'EMAIL',
      assunto: 'Hora da Revisão do Seu Veículo',
      mensagem: `Olá {{cliente_nome}},

Está na hora de fazer a revisão do seu veículo.

- Veículo: {{modelo}} - {{placa}}

Agende sua visita!

Atenciosamente,
{{nome_oficina}}
{{telefone_oficina}}`,
      ativo: true,
    },
  },
};

export function TemplatesTab() {
  const navigate = useNavigate();
  const [selectedEvento, setSelectedEvento] = useState('OS_CRIADA');
  const [selectedCanal, setSelectedCanal] = useState<'WHATSAPP' | 'TELEGRAM' | 'EMAIL'>('WHATSAPP');
  const [showPreview, setShowPreview] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [selectedEmojiCategory, setSelectedEmojiCategory] = useState(0);

  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Fetch recent notifications for the table
  const { data: recentNotificacoes, isLoading: loadingNotificacoes } = useNotificacoes({ page: 0, size: 5 });

  const getCanalIcon = (tipo: TipoNotificacao) => {
    switch (tipo) {
      case 'WHATSAPP':
        return <MessageSquare className="h-3.5 w-3.5 text-green-600 dark:text-green-400" />;
      case 'TELEGRAM':
        return <Bell className="h-3.5 w-3.5 text-blue-500 dark:text-blue-400" />;
      case 'EMAIL':
        return <Mail className="h-3.5 w-3.5 text-blue-600 dark:text-blue-400" />;
      case 'SMS':
        return <Smartphone className="h-3.5 w-3.5 text-purple-600 dark:text-purple-400" />;
      default:
        return <Send className="h-3.5 w-3.5 text-gray-600 dark:text-gray-400" />;
    }
  };

  const getCanalLabel = (tipo: TipoNotificacao) => {
    switch (tipo) {
      case 'WHATSAPP': return 'WhatsApp';
      case 'TELEGRAM': return 'Telegram';
      case 'EMAIL': return 'E-mail';
      case 'SMS': return 'SMS';
      default: return tipo;
    }
  };

  const getStatusBadge = (status: StatusNotificacao) => {
    switch (status) {
      case 'ENVIADO':
      case 'ENTREGUE':
      case 'LIDO':
        return (
          <span className="rounded-full bg-green-100 dark:bg-green-900/30 px-2 py-1 text-xs font-medium text-green-700 dark:text-green-400">
            Enviado
          </span>
        );
      case 'PENDENTE':
      case 'AGENDADO':
        return (
          <span className="rounded-full bg-yellow-100 dark:bg-yellow-900/30 px-2 py-1 text-xs font-medium text-yellow-700 dark:text-yellow-400">
            Pendente
          </span>
        );
      case 'FALHA':
        return (
          <span className="rounded-full bg-red-100 dark:bg-red-900/30 px-2 py-1 text-xs font-medium text-red-700 dark:text-red-400">
            Falhou
          </span>
        );
      default:
        return (
          <span className="rounded-full bg-gray-100 dark:bg-gray-900/30 px-2 py-1 text-xs font-medium text-gray-700 dark:text-gray-400">
            {status}
          </span>
        );
    }
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Verifica se o canal suporta emoji
  const currentCanalConfig = canais.find(c => c.value === selectedCanal);
  const supportsEmoji = currentCanalConfig?.supportsEmoji ?? false;

  const currentTemplate = defaultTemplates[selectedEvento]?.[selectedCanal] || {
    evento: selectedEvento,
    canal: selectedCanal,
    assunto: '',
    mensagem: '',
    ativo: false,
  };

  const [template, setTemplate] = useState<Template>(currentTemplate);
  const [isActive, setIsActive] = useState(currentTemplate.ativo);

  const handleEventoChange = (evento: string) => {
    setSelectedEvento(evento);
    const newTemplate = defaultTemplates[evento]?.[selectedCanal] || {
      evento,
      canal: selectedCanal,
      assunto: '',
      mensagem: '',
      ativo: false,
    };
    setTemplate(newTemplate);
    setIsActive(newTemplate.ativo);
  };

  const handleCanalChange = (canal: 'WHATSAPP' | 'TELEGRAM' | 'EMAIL') => {
    setSelectedCanal(canal);
    setShowEmojiPicker(false); // Fecha o picker ao trocar de canal
    const newTemplate = defaultTemplates[selectedEvento]?.[canal] || {
      evento: selectedEvento,
      canal,
      assunto: '',
      mensagem: '',
      ativo: false,
    };
    setTemplate(newTemplate);
    setIsActive(newTemplate.ativo);
  };

  const insertEmoji = (emoji: string) => {
    const textarea = textareaRef.current;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const before = template.mensagem.substring(0, start);
    const after = template.mensagem.substring(end);

    setTemplate({
      ...template,
      mensagem: before + emoji + after,
    });

    // Focus and set cursor position after the inserted emoji
    setTimeout(() => {
      textarea.focus();
      const newPos = start + emoji.length;
      textarea.setSelectionRange(newPos, newPos);
    }, 0);
  };

  const insertVariable = (variable: string) => {
    const textarea = textareaRef.current;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const before = template.mensagem.substring(0, start);
    const after = template.mensagem.substring(end);

    setTemplate({
      ...template,
      mensagem: before + variable + after,
    });

    // Focus and set cursor position after the inserted variable
    setTimeout(() => {
      textarea.focus();
      const newPos = start + variable.length;
      textarea.setSelectionRange(newPos, newPos);
    }, 0);
  };

  const renderPreview = (text: string) => {
    return text.replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g, (match, key) => {
      return sampleData[key] || match;
    });
  };

  const handleSave = () => {
    // TODO: Implement save to backend
    alert('Template salvo com sucesso!');
  };

  return (
    <div className="flex flex-col gap-6 lg:flex-row">
      {/* Main Editor */}
      <div className="flex-1">
        <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
          {/* Selectors */}
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Evento</label>
              <select
                value={selectedEvento}
                onChange={(e) => handleEventoChange(e.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              >
                {eventos.map((e) => (
                  <option key={e.value} value={e.value}>
                    {e.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Canal</label>
              <select
                value={selectedCanal}
                onChange={(e) => handleCanalChange(e.target.value as 'WHATSAPP' | 'TELEGRAM' | 'EMAIL')}
                className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              >
                {canais.map((c) => (
                  <option key={c.value} value={c.value}>
                    {c.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex gap-2">
              <button
                onClick={() => setIsActive(!isActive)}
                className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-600 text-white hover:bg-blue-700'
                    : 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-300 dark:hover:bg-gray-600'
                }`}
              >
                {isActive ? 'Ativo' : 'Inativo'}
              </button>
            </div>
          </div>

          {/* Subject (email only) */}
          {selectedCanal === 'EMAIL' && (
            <div className="mt-4">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Assunto (E-mail)
              </label>
              <input
                type="text"
                value={template.assunto}
                onChange={(e) => setTemplate({ ...template, assunto: e.target.value })}
                placeholder="Assunto do e-mail"
                className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Use variáveis como {`{{ordem_id}}`} no assunto
              </p>
            </div>
          )}

          {/* Message Editor */}
          <div className="mt-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Mensagem</label>
                {supportsEmoji && (
                  <button
                    type="button"
                    onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                    className={`flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-medium transition-colors ${
                      showEmojiPicker
                        ? 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 hover:bg-yellow-50 dark:hover:bg-yellow-900/20 hover:text-yellow-600 dark:hover:text-yellow-400'
                    }`}
                  >
                    <Smile className="h-3.5 w-3.5" />
                    Emojis
                  </button>
                )}
              </div>
              <span className="text-xs text-gray-500 dark:text-gray-400">
                {template.mensagem.length} caracteres
              </span>
            </div>

            {/* Emoji Picker */}
            {supportsEmoji && showEmojiPicker && (
              <div className="mt-2 rounded-xl border border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-900/20 p-3">
                <div className="flex flex-wrap gap-1 mb-2">
                  {emojiCategories.map((cat, idx) => (
                    <button
                      key={cat.name}
                      onClick={() => setSelectedEmojiCategory(idx)}
                      className={`rounded-lg px-2 py-1 text-xs font-medium transition-colors ${
                        selectedEmojiCategory === idx
                          ? 'bg-yellow-200 dark:bg-yellow-800 text-yellow-800 dark:text-yellow-200'
                          : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-yellow-100 dark:hover:bg-yellow-900/40'
                      }`}
                    >
                      {cat.name}
                    </button>
                  ))}
                </div>
                <div className="flex flex-wrap gap-1">
                  {emojiCategories[selectedEmojiCategory].emojis.map((emoji) => (
                    <button
                      key={emoji}
                      onClick={() => insertEmoji(emoji)}
                      className="rounded-lg p-1.5 text-xl hover:bg-yellow-100 dark:hover:bg-yellow-900/40 transition-colors"
                      title={`Inserir ${emoji}`}
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
                <p className="mt-2 text-xs text-yellow-700 dark:text-yellow-400">
                  Clique em um emoji para inserir no cursor atual
                </p>
              </div>
            )}

            <textarea
              ref={textareaRef}
              value={template.mensagem}
              onChange={(e) => setTemplate({ ...template, mensagem: e.target.value })}
              rows={12}
              placeholder="Digite o template aqui..."
              className="mt-2 w-full resize-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 font-mono text-sm text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 leading-6 focus:border-blue-500 dark:focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
            <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
              Use variáveis entre chaves duplas, por exemplo:{' '}
              <code className="rounded bg-gray-100 dark:bg-gray-700 px-1 py-0.5 font-mono text-gray-900 dark:text-gray-300">
                {`{{cliente_nome}}`}
              </code>
              {supportsEmoji && (
                <span className="ml-2">
                  • Emojis são suportados para {selectedCanal === 'WHATSAPP' ? 'WhatsApp' : 'Telegram'}
                </span>
              )}
            </p>
          </div>

          {/* Actions */}
          <div className="mt-6 flex flex-wrap gap-3">
            <button
              onClick={() => setShowPreview(true)}
              className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
            >
              <Eye className="h-4 w-4" />
              Preview
            </button>
            <button
              onClick={handleSave}
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              <Save className="h-4 w-4" />
              Salvar
            </button>
          </div>
        </div>

        {/* Recent Sends */}
        <div className="mt-6 rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-5">
          <div className="flex items-center justify-between">
            <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900 dark:text-white">
              <Clock className="h-4 w-4" />
              Últimos envios
            </h3>
            <button
              onClick={() => navigate('/notificacoes/historico')}
              className="text-sm text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 hover:underline"
            >
              Ver logs completos
            </button>
          </div>

          <div className="mt-4 overflow-hidden rounded-xl border border-gray-200 dark:border-gray-700">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 dark:bg-gray-900 text-gray-600 dark:text-gray-400">
                <tr>
                  <th className="px-3 py-2 font-medium">Data</th>
                  <th className="px-3 py-2 font-medium">Canal</th>
                  <th className="px-3 py-2 font-medium">Evento</th>
                  <th className="px-3 py-2 font-medium">Destinatário</th>
                  <th className="px-3 py-2 font-medium">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {loadingNotificacoes ? (
                  <tr>
                    <td colSpan={5} className="px-3 py-8 text-center">
                      <div className="flex items-center justify-center gap-2 text-gray-500 dark:text-gray-400">
                        <Loader2 className="h-4 w-4 animate-spin" />
                        Carregando...
                      </div>
                    </td>
                  </tr>
                ) : recentNotificacoes?.content && recentNotificacoes.content.length > 0 ? (
                  recentNotificacoes.content.map((notif) => (
                    <tr key={notif.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-3 py-2 text-gray-600 dark:text-gray-400">
                        {notif.dataEnvio ? formatDate(notif.dataEnvio) : '-'}
                      </td>
                      <td className="px-3 py-2 text-gray-900 dark:text-white">
                        <span className="flex items-center gap-1.5">
                          {getCanalIcon(notif.tipo)}
                          {getCanalLabel(notif.tipo)}
                        </span>
                      </td>
                      <td className="px-3 py-2 text-gray-600 dark:text-gray-400">
                        {notif.evento?.replace(/_/g, ' ')}
                      </td>
                      <td className="px-3 py-2 font-mono text-gray-600 dark:text-gray-400 text-xs">
                        {notif.destinatario}
                      </td>
                      <td className="px-3 py-2">
                        {getStatusBadge(notif.status)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5} className="px-3 py-8 text-center text-gray-500 dark:text-gray-400">
                      Nenhuma notificação enviada ainda.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Sidebar */}
      <aside className="w-full lg:w-96">
        {/* Variables */}
        <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 p-4">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Variáveis disponíveis</h3>
          <p className="mt-1 text-xs text-gray-600 dark:text-gray-400">
            Clique para inserir no template
          </p>

          <div className="mt-4 grid grid-cols-1 gap-2">
            {variables.map((v) => (
              <button
                key={v.key}
                onClick={() => insertVariable(v.key)}
                className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-2 text-left text-sm hover:bg-blue-50 dark:hover:bg-blue-900/30 hover:border-blue-200 dark:hover:border-blue-800 transition-colors"
              >
                <span className="font-mono text-blue-600 dark:text-blue-400">{v.key}</span>
                <span className="block text-xs text-gray-500 dark:text-gray-400">{v.description}</span>
              </button>
            ))}
          </div>

          <div className="mt-4 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3 text-xs text-gray-600 dark:text-gray-400">
            <strong>Dica:</strong> WhatsApp e Telegram para mensagens curtas com emojis;
            E-mail para detalhamento e anexos.
          </div>
        </div>

        {/* Sending Policies */}
        <div className="mt-6 rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Políticas de envio</h3>
          <div className="mt-4 space-y-3 text-sm">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                defaultChecked
                className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="text-gray-700 dark:text-gray-300">Aplicar horário comercial</span>
            </label>
            <div className="ml-6 grid grid-cols-2 gap-2">
              <div>
                <label className="text-xs text-gray-600 dark:text-gray-400">Início</label>
                <input
                  type="time"
                  defaultValue="08:00"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-2 py-1 text-sm text-gray-900 dark:text-white"
                />
              </div>
              <div>
                <label className="text-xs text-gray-600 dark:text-gray-400">Fim</label>
                <input
                  type="time"
                  defaultValue="18:00"
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-2 py-1 text-sm text-gray-900 dark:text-white"
                />
              </div>
            </div>

            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                defaultChecked
                className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="text-gray-700 dark:text-gray-300">Fallback WhatsApp → E-mail se falhar</span>
            </label>

            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="text-gray-700 dark:text-gray-300">Exigir opt-in do cliente</span>
            </label>
          </div>
        </div>
      </aside>

      {/* Preview Modal */}
      {showPreview && (
        <div className="fixed inset-0 z-50 flex items-start justify-center overflow-auto bg-gray-900/50 dark:bg-gray-900/80 p-4 pt-24">
          <div className="w-full max-w-2xl rounded-2xl bg-white dark:bg-gray-800 shadow-xl">
            <div className="flex items-center justify-between border-b border-gray-200 dark:border-gray-700 px-6 py-4">
              <div>
                <p className="text-xs text-gray-500 dark:text-gray-400">Preview renderizado</p>
                <p className="text-sm font-semibold text-gray-900 dark:text-white">
                  {eventos.find((e) => e.value === selectedEvento)?.label} •{' '}
                  {canais.find((c) => c.value === selectedCanal)?.label}
                </p>
              </div>
              <button
                onClick={() => setShowPreview(false)}
                className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 p-2 text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-600 hover:text-gray-700 dark:hover:text-gray-300"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="p-6">
              {selectedCanal === 'EMAIL' && template.assunto && (
                <div className="mb-4">
                  <p className="text-xs font-medium text-gray-500 dark:text-gray-400">Assunto</p>
                  <p className="mt-1 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 px-3 py-2 text-sm text-gray-900 dark:text-white">
                    {renderPreview(template.assunto)}
                  </p>
                </div>
              )}

              <div>
                <p className="text-xs font-medium text-gray-500 dark:text-gray-400">Mensagem</p>
                <pre className="mt-1 whitespace-pre-wrap rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900 px-3 py-3 font-sans text-sm text-gray-900 dark:text-white leading-6">
                  {renderPreview(template.mensagem)}
                </pre>
              </div>

              <div className="mt-4 rounded-xl border border-blue-200 dark:border-blue-800 bg-blue-50 dark:bg-blue-900/30 p-3 text-xs text-blue-700 dark:text-blue-400">
                Este preview usa dados fictícios apenas para validar o template.
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
