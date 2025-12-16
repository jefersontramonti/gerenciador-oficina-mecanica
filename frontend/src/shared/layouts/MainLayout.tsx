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
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { useWebSocket } from '@/shared/hooks/useWebSocket';
import { WebSocketNotificationHandler } from '@/shared/components/WebSocketNotificationHandler';
import { useContadorEstoqueBaixo } from '@/features/estoque/hooks/usePecas';

interface NavigationItem {
  name: string;
  href: string;
  icon: any;
  requiredRoles?: PerfilUsuario[]; // Se não definido, todos têm acesso
}

const navigationItems: NavigationItem[] = [
  {
    name: 'Dashboard',
    href: '/',
    icon: LayoutDashboard,
    // Todos têm acesso
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
    name: 'Configurações',
    href: '/configuracoes',
    icon: Settings,
    // Todos têm acesso (em desenvolvimento)
  },
];

/**
 * Filtra itens do menu baseado no perfil do usuário
 */
const filterNavigationByRole = (
  items: NavigationItem[],
  userPerfil?: PerfilUsuario
): NavigationItem[] => {
  if (!userPerfil) return [];

  return items.filter((item) => {
    // Se não há restrição de roles, todos podem acessar
    if (!item.requiredRoles || item.requiredRoles.length === 0) {
      return true;
    }
    // Verifica se o perfil do usuário está na lista de perfis permitidos
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

  // Filter navigation items based on user's profile
  const navigation = useMemo(
    () => filterNavigationByRole(navigationItems, user?.perfil),
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
    <div className="flex h-screen bg-gray-100">
      {/* WebSocket notification handler - invisible component */}
      <WebSocketNotificationHandler />

      {/* Sidebar - Desktop */}
      <aside className="hidden w-64 flex-col border-r border-gray-200 bg-white lg:flex">
        <div className="flex h-16 items-center gap-2 border-b border-gray-200 px-6">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-600">
            <Wrench className="h-6 w-6 text-white" />
          </div>
          <span className="text-xl font-bold text-gray-900">PitStop</span>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto p-4">
          {navigation.map((item) => {
            const isActive = location.pathname === item.href;
            const showBadge = item.href === '/estoque' && contadorEstoqueBaixo && contadorEstoqueBaixo > 0;

            return (
              <Link
                key={item.name}
                to={item.href}
                className={cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900'
                )}
              >
                <item.icon className="h-5 w-5" />
                <span className="flex-1">{item.name}</span>
                {showBadge && (
                  <span className="inline-flex items-center justify-center rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-800">
                    {contadorEstoqueBaixo}
                  </span>
                )}
              </Link>
            );
          })}
        </nav>

        <div className="border-t border-gray-200 p-4">
          <div className="mb-3 flex items-center gap-3 px-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-100 text-sm font-medium text-primary-700">
              {user?.nome.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="truncate text-sm font-medium text-gray-900">{user?.nome}</p>
              <p className="truncate text-xs text-gray-500">{user?.perfil}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 hover:text-gray-900"
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
            className="fixed inset-0 bg-gray-900/50"
            onClick={() => setSidebarOpen(false)}
          />
          <aside className="fixed inset-y-0 left-0 w-64 bg-white">
            <div className="flex h-16 items-center justify-between border-b border-gray-200 px-6">
              <div className="flex items-center gap-2">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-600">
                  <Wrench className="h-6 w-6 text-white" />
                </div>
                <span className="text-xl font-bold text-gray-900">PitStop</span>
              </div>
              <button
                onClick={() => setSidebarOpen(false)}
                className="rounded-lg p-1 hover:bg-gray-100"
              >
                <X className="h-6 w-6" />
              </button>
            </div>

            <nav className="space-y-1 p-4">
              {navigation.map((item) => {
                const isActive = location.pathname === item.href;
                const showBadge = item.href === '/estoque' && contadorEstoqueBaixo && contadorEstoqueBaixo > 0;

                return (
                  <Link
                    key={item.name}
                    to={item.href}
                    onClick={() => setSidebarOpen(false)}
                    className={cn(
                      'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-primary-50 text-primary-700'
                        : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900'
                    )}
                  >
                    <item.icon className="h-5 w-5" />
                    <span className="flex-1">{item.name}</span>
                    {showBadge && (
                      <span className="inline-flex items-center justify-center rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-800">
                        {contadorEstoqueBaixo}
                      </span>
                    )}
                  </Link>
                );
              })}
            </nav>

            <div className="absolute bottom-0 left-0 right-0 border-t border-gray-200 p-4">
              <button
                onClick={handleLogout}
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 hover:text-gray-900"
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
        <header className="flex h-16 items-center justify-between border-b border-gray-200 bg-white px-6">
          <button
            onClick={() => setSidebarOpen(true)}
            className="rounded-lg p-2 hover:bg-gray-100 lg:hidden"
          >
            <Menu className="h-6 w-6" />
          </button>

          <div className="hidden lg:block">
            <h2 className="text-lg font-semibold text-gray-900">
              {navigation.find((item) => item.href === location.pathname)?.name || 'Dashboard'}
            </h2>
          </div>

          <div className="flex items-center gap-3 lg:hidden">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-100 text-sm font-medium text-primary-700">
              {user?.nome.charAt(0).toUpperCase()}
            </div>
          </div>

          <div className="hidden items-center gap-4 lg:flex">
            {/* WebSocket connection status indicator */}
            {isConnected ? (
              <div className="flex items-center gap-2 rounded-lg bg-green-50 px-3 py-1.5 text-xs font-medium text-green-700">
                <div className="h-2 w-2 animate-pulse rounded-full bg-green-500" />
                <span>Tempo real ativo</span>
              </div>
            ) : (
              <div className="flex items-center gap-2 rounded-lg bg-gray-50 px-3 py-1.5 text-xs font-medium text-gray-500">
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
