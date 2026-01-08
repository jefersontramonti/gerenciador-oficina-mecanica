import { useState, useMemo } from 'react';
import { Link, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { PerfilUsuario } from '@/features/auth/types';
import {
  LayoutDashboard,
  Users,
  Car,
  FileText,
  Package,
  DollarSign,
  Settings,
  LogOut,
  Menu,
  X,
  Wrench,
  UserCog,
  MapPin,
  Receipt,
  Bell,
  Building2,
  CreditCard,
  ClipboardList,
  Crown,
  Layers,
  AlertTriangle,
  BarChart3,
  Headphones,
  Megaphone,
  Flag,
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { useWebSocket } from '@/shared/hooks/useWebSocket';
import { WebSocketNotificationHandler } from '@/shared/components/WebSocketNotificationHandler';
import { useContadorEstoqueBaixo } from '@/features/estoque/hooks/usePecas';
import { useComunicadosNaoLidos } from '@/features/comunicados/hooks/useComunicados';
import { ThemeToggle } from '@/shared/components/common';

interface NavigationItem {
  name: string;
  href: string;
  icon: any;
  requiredRoles?: PerfilUsuario[]; // Se não definido, todos têm acesso
}

// Navigation items for SUPER_ADMIN (SaaS management)
const superAdminNavigationItems: NavigationItem[] = [
  {
    name: 'Dashboard SaaS',
    href: '/admin',
    icon: Crown,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Oficinas',
    href: '/admin/oficinas',
    icon: Building2,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Planos',
    href: '/admin/planos',
    icon: Layers,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Faturas',
    href: '/admin/faturas',
    icon: Receipt,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Inadimplência',
    href: '/admin/inadimplencia',
    icon: AlertTriangle,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Pagamentos',
    href: '/admin/pagamentos',
    icon: CreditCard,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Relatórios',
    href: '/admin/relatorios',
    icon: BarChart3,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Tickets/Suporte',
    href: '/admin/tickets',
    icon: Headphones,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Comunicados',
    href: '/admin/comunicados',
    icon: Megaphone,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Feature Flags',
    href: '/admin/features',
    icon: Flag,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Auditoria',
    href: '/admin/audit',
    icon: ClipboardList,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
];

// Navigation items for regular users (oficina management)
const navigationItems: NavigationItem[] = [
  {
    name: 'Dashboard',
    href: '/',
    icon: LayoutDashboard,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
  },
  {
    name: 'Clientes',
    href: '/clientes',
    icon: Users,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
  },
  {
    name: 'Veículos',
    href: '/veiculos',
    icon: Car,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
  },
  {
    name: 'Ordens de Serviço',
    href: '/ordens-servico',
    icon: FileText,
    // Todos têm acesso (visualizar)
  },
  {
    name: 'Usuários',
    href: '/usuarios',
    icon: UserCog,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE],
  },
  {
    name: 'Estoque',
    href: '/estoque',
    icon: Package,
    // Todos têm acesso (em desenvolvimento)
  },
  {
    name: 'Locais de Armazenamento',
    href: '/estoque/locais',
    icon: MapPin,
    // Todos têm acesso para visualizar
  },
  {
    name: 'Pagamentos',
    href: '/financeiro',
    icon: DollarSign,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
  },
  {
    name: 'Notas Fiscais',
    href: '/financeiro/notas-fiscais',
    icon: Receipt,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
  },
  {
    name: 'Gateway de Pagamento',
    href: '/financeiro/gateways',
    icon: CreditCard,
    requiredRoles: [PerfilUsuario.ADMIN],
  },
  {
    name: 'Notificações',
    href: '/notificacoes/configuracao',
    icon: Bell,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE],
  },
  {
    name: 'Comunicados',
    href: '/comunicados',
    icon: Megaphone,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE, PerfilUsuario.MECANICO],
  },
  {
    name: 'Configurações',
    href: '/configuracoes',
    icon: Settings,
    // Todos têm acesso (em desenvolvimento)
  },
];

/**
 * Retorna os itens de navegação apropriados baseado no perfil do usuário
 */
const getNavigationForRole = (userPerfil?: PerfilUsuario): NavigationItem[] => {
  if (!userPerfil) return [];

  // SUPER_ADMIN tem sua própria navegação específica
  if (userPerfil === PerfilUsuario.SUPER_ADMIN) {
    return superAdminNavigationItems;
  }

  // Outros perfis usam a navegação padrão
  return navigationItems.filter((item) => {
    if (!item.requiredRoles || item.requiredRoles.length === 0) {
      return true;
    }
    return item.requiredRoles.includes(userPerfil);
  });
};

export const MainLayout = () => {
  const location = useLocation();
  const { user, logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // WebSocket connection - automatically connects when authenticated
  const { isConnected } = useWebSocket();

  // Badge de alerta de estoque baixo
  const { data: contadorEstoqueBaixo } = useContadorEstoqueBaixo();

  // Badge de comunicados não lidos (apenas para usuários de oficina, não SUPER_ADMIN)
  const { data: comunicadosNaoLidos } = useComunicadosNaoLidos();

  // Get navigation items based on user's profile
  const navigation = useMemo(
    () => getNavigationForRole(user?.perfil),
    [user?.perfil]
  );

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error('Erro ao fazer logout:', error);
    }
  };

  return (
    <div className="flex h-screen bg-gray-100 dark:bg-gray-900">
      {/* WebSocket notification handler - invisible component */}
      <WebSocketNotificationHandler />

      {/* Sidebar - Desktop */}
      <aside className="hidden w-64 flex-col border-r border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-800 lg:flex">
        <div className="flex h-16 items-center gap-2 border-b border-gray-200 px-6 dark:border-gray-700">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-600 dark:bg-primary-500">
            <Wrench className="h-6 w-6 text-white" />
          </div>
          <span className="text-xl font-bold text-gray-900 dark:text-white">PitStop</span>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto p-4">
          {navigation.map((item) => {
            const isActive = location.pathname === item.href;
            const showEstoqueBadge = item.href === '/estoque' && contadorEstoqueBaixo && contadorEstoqueBaixo > 0;
            const showComunicadosBadge = item.href === '/comunicados' && comunicadosNaoLidos && comunicadosNaoLidos > 0;

            return (
              <Link
                key={item.name}
                to={item.href}
                className={cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-400'
                    : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-300 dark:hover:bg-gray-700 dark:hover:text-white'
                )}
              >
                <item.icon className="h-5 w-5" />
                <span className="flex-1">{item.name}</span>
                {showEstoqueBadge && (
                  <span className="inline-flex items-center justify-center rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-800 dark:bg-orange-900/30 dark:text-orange-400">
                    {contadorEstoqueBaixo}
                  </span>
                )}
                {showComunicadosBadge && (
                  <span className="inline-flex items-center justify-center rounded-full bg-red-500 px-2 py-0.5 text-xs font-bold text-white">
                    {comunicadosNaoLidos > 99 ? '99+' : comunicadosNaoLidos}
                  </span>
                )}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-gray-200 p-4 dark:border-gray-700">
          <div className="mb-3 flex items-center gap-3 px-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-100 text-sm font-medium text-primary-700 dark:bg-primary-900/30 dark:text-primary-400">
              {user?.nome.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="truncate text-sm font-medium text-gray-900 dark:text-white">{user?.nome}</p>
              <p className="truncate text-xs text-gray-500 dark:text-gray-400">{user?.perfil}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 hover:text-gray-900 dark:text-gray-300 dark:hover:bg-gray-700 dark:hover:text-white"
          >
            <LogOut className="h-5 w-5" />
            Sair
          </button>
        </div>
      </aside>

      {/* Sidebar - Mobile */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div
            className="fixed inset-0 bg-gray-900/50 dark:bg-gray-950/70"
            onClick={() => setSidebarOpen(false)}
          />
          <aside className="fixed inset-y-0 left-0 w-64 bg-white dark:bg-gray-800">
            <div className="flex h-16 items-center justify-between border-b border-gray-200 px-6 dark:border-gray-700">
              <div className="flex items-center gap-2">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-600 dark:bg-primary-500">
                  <Wrench className="h-6 w-6 text-white" />
                </div>
                <span className="text-xl font-bold text-gray-900 dark:text-white">PitStop</span>
              </div>
              <button
                onClick={() => setSidebarOpen(false)}
                className="rounded-lg p-1 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                <X className="h-6 w-6" />
              </button>
            </div>

            <nav className="space-y-1 p-4">
              {navigation.map((item) => {
                const isActive = location.pathname === item.href;
                const showEstoqueBadge = item.href === '/estoque' && contadorEstoqueBaixo && contadorEstoqueBaixo > 0;
                const showComunicadosBadge = item.href === '/comunicados' && comunicadosNaoLidos && comunicadosNaoLidos > 0;

                return (
                  <Link
                    key={item.name}
                    to={item.href}
                    onClick={() => setSidebarOpen(false)}
                    className={cn(
                      'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-400'
                        : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-300 dark:hover:bg-gray-700 dark:hover:text-white'
                    )}
                  >
                    <item.icon className="h-5 w-5" />
                    <span className="flex-1">{item.name}</span>
                    {showEstoqueBadge && (
                      <span className="inline-flex items-center justify-center rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-800 dark:bg-orange-900/30 dark:text-orange-400">
                        {contadorEstoqueBaixo}
                      </span>
                    )}
                    {showComunicadosBadge && (
                      <span className="inline-flex items-center justify-center rounded-full bg-red-500 px-2 py-0.5 text-xs font-bold text-white">
                        {comunicadosNaoLidos > 99 ? '99+' : comunicadosNaoLidos}
                      </span>
                    )}
                  </Link>
                );
              })}
            </nav>

            <div className="absolute bottom-0 left-0 right-0 border-t border-gray-200 p-4 dark:border-gray-700">
              <button
                onClick={handleLogout}
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 hover:text-gray-900 dark:text-gray-300 dark:hover:bg-gray-700 dark:hover:text-white"
              >
                <LogOut className="h-5 w-5" />
                Sair
              </button>
            </div>
          </aside>
        </div>
      )}

      {/* Main Content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Header */}
        <header className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-6 dark:border-gray-700 dark:bg-gray-800">
          <button
            onClick={() => setSidebarOpen(true)}
            className="rounded-lg p-2 text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-700 lg:hidden"
          >
            <Menu className="h-6 w-6" />
          </button>

          <div className="hidden lg:block">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              {navigation.find((item) => item.href === location.pathname)?.name || 'Dashboard'}
            </h2>
          </div>

          <div className="flex items-center gap-2 lg:hidden">
            <ThemeToggle size="small" />
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-100 text-sm font-medium text-primary-700 dark:bg-primary-900/30 dark:text-primary-400">
              {user?.nome.charAt(0).toUpperCase()}
            </div>
          </div>

          <div className="hidden items-center gap-3 lg:flex">
            {/* Theme toggle */}
            <ThemeToggle />

            {/* WebSocket connection status indicator */}
            {isConnected ? (
              <div className="flex items-center gap-2 rounded-lg bg-green-50 px-3 py-1.5 text-xs font-medium text-green-700 dark:bg-green-900/20 dark:text-green-400">
                <div className="h-2 w-2 animate-pulse rounded-full bg-green-500 dark:bg-green-400" />
                <span>Tempo real ativo</span>
              </div>
            ) : (
              <div className="flex items-center gap-2 rounded-lg bg-gray-50 px-3 py-1.5 text-xs font-medium text-gray-500 dark:bg-gray-800 dark:text-gray-400">
                <div className="h-2 w-2 rounded-full bg-gray-400" />
                <span>Offline</span>
              </div>
            )}
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
