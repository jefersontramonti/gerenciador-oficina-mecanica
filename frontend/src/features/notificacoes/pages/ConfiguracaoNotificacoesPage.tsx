import { useState } from 'react';
import { Settings, Radio, MessageSquare, Send, Mail, FileText } from 'lucide-react';
import { MetricasCards } from '../components/MetricasCards';
import { CanaisTab, WhatsAppEvolutionTab, TelegramTab, EmailTab, TemplatesTab } from '../components/tabs';
import { useConfiguracoes } from '../hooks/useNotificacoes';

type TabId = 'canais' | 'whatsapp' | 'telegram' | 'email' | 'templates';

interface Tab {
  id: TabId;
  label: string;
  icon: typeof Radio;
}

const tabs: Tab[] = [
  { id: 'canais', label: 'Canais', icon: Radio },
  { id: 'whatsapp', label: 'WhatsApp (Evolution)', icon: MessageSquare },
  { id: 'telegram', label: 'Telegram Bot', icon: Send },
  { id: 'email', label: 'E-mail', icon: Mail },
  { id: 'templates', label: 'Templates', icon: FileText },
];

export function ConfiguracaoNotificacoesPage() {
  const [activeTab, setActiveTab] = useState<TabId>('canais');
  const { data: config, isLoading, error } = useConfiguracoes();

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="mb-6 flex items-center gap-3">
          <Settings className="h-6 w-6 sm:h-8 sm:w-8 text-blue-600 dark:text-blue-400" />
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Configurações</p>
            <h1 className="text-xl sm:text-2xl font-semibold tracking-tight text-gray-900 dark:text-white">
              Comunicação
            </h1>
          </div>
        </div>
        <div className="flex items-center justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 dark:border-gray-700 border-t-blue-600 dark:border-t-blue-400" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="mb-6 flex items-center gap-3">
          <Settings className="h-6 w-6 sm:h-8 sm:w-8 text-blue-600 dark:text-blue-400" />
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Configurações</p>
            <h1 className="text-xl sm:text-2xl font-semibold tracking-tight text-gray-900 dark:text-white">
              Comunicação
            </h1>
          </div>
        </div>
        <div className="rounded-lg border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20 p-6 text-center text-red-700 dark:text-red-400">
          Erro ao carregar configurações. Tente novamente.
        </div>
      </div>
    );
  }

  const renderTabContent = () => {
    switch (activeTab) {
      case 'canais':
        return <CanaisTab />;
      case 'whatsapp':
        return <WhatsAppEvolutionTab />;
      case 'telegram':
        return <TelegramTab />;
      case 'email':
        return <EmailTab />;
      case 'templates':
        return <TemplatesTab />;
      default:
        return null;
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Settings className="h-6 w-6 sm:h-8 sm:w-8 text-blue-600 dark:text-blue-400" />
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Configurações</p>
            <h1 className="text-xl sm:text-2xl font-semibold tracking-tight text-gray-900 dark:text-white">
              Comunicação
            </h1>
          </div>
        </div>

        {config?.modoSimulacao && (
          <span className="hidden sm:inline-flex items-center gap-2 rounded-full border border-yellow-300 dark:border-yellow-700 bg-yellow-50 dark:bg-yellow-900/20 px-3 py-1 text-xs font-medium text-yellow-800 dark:text-yellow-400">
            <span className="h-2 w-2 rounded-full bg-yellow-500" />
            Modo Simulação
          </span>
        )}
      </div>

      {/* Metrics */}
      <div className="mb-6">
        <MetricasCards />
      </div>

      {/* Tabs Card */}
      <div className="rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 shadow-sm">
        {/* Tab Navigation */}
        <div className="border-b border-gray-200 dark:border-gray-700 px-4 py-3 sm:px-6">
          <nav className="flex flex-wrap gap-2" aria-label="Tabs">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;

              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  <span className="hidden sm:inline">{tab.label}</span>
                  <span className="sm:hidden">
                    {tab.label.split(' ')[0]}
                  </span>
                </button>
              );
            })}
          </nav>
        </div>

        {/* Tab Content */}
        <div className="p-4 sm:p-6">{renderTabContent()}</div>
      </div>

      {/* Simulation Mode Banner (mobile) */}
      {config?.modoSimulacao && (
        <div className="mt-6 rounded-lg border border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-900/20 p-4 sm:hidden">
          <p className="flex items-center gap-2 text-sm text-yellow-800 dark:text-yellow-400">
            <span className="font-medium">Modo Simulação Ativo</span>
          </p>
          <p className="mt-1 text-xs text-yellow-700 dark:text-yellow-500">
            As notificações não serão realmente enviadas
          </p>
        </div>
      )}
    </div>
  );
}
