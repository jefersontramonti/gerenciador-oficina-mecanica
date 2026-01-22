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
  CalendarClock,
  Webhook,
  Percent,
  TrendingUp,
  FileBarChart,
  Target,
  Scale,
  Repeat,
  ChevronDown,
  ChevronRight,
  Briefcase,
  Wallet,
  FileCheck,
  MessageSquare,
  ArrowDownCircle,
  Cog,
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { useWebSocket } from '@/shared/hooks/useWebSocket';
import { useRealtimeUpdates } from '@/shared/hooks/useRealtimeUpdates';
import { WebSocketNotificationHandler } from '@/shared/components/WebSocketNotificationHandler';
import { useContadorEstoqueBaixo } from '@/features/estoque/hooks/usePecas';
import { useComunicadosNaoLidos } from '@/features/comunicados/hooks/useComunicados';
import { useContadorAlertasDRE, useContadorAlertasFluxo } from '@/features/financeiro/hooks/useFluxoCaixa';
import { useContadorAlertasDespesas } from '@/features/financeiro/hooks/useDespesas';
import { useContadorAlertasAssinaturas } from '@/features/financeiro/hooks/useAssinaturas';
import { ThemeToggle } from '@/shared/components/common';
import { useFeatureFlag } from '@/shared/hooks/useFeatureFlag';

interface NavigationItem {
  name: string;
  href: string;
  icon: any;
  requiredRoles?: PerfilUsuario[];
  requiredFeature?: string;
}

interface NavigationGroup {
  name: string;
  icon: any;
  items: NavigationItem[];
  requiredRoles?: PerfilUsuario[];
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
    name: 'Gateway Pagamento',
    href: '/admin/configuracoes/gateway',
    icon: CreditCard,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
  {
    name: 'Auditoria',
    href: '/admin/audit',
    icon: ClipboardList,
    requiredRoles: [PerfilUsuario.SUPER_ADMIN],
  },
];

// Dashboard item (standalone, not in a group)
const dashboardItem: NavigationItem = {
  name: 'Dashboard',
  href: '/',
  icon: LayoutDashboard,
  requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
};

// Grouped navigation for regular users
const navigationGroups: NavigationGroup[] = [
  {
    name: 'Gestão',
    icon: Briefcase,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE, PerfilUsuario.MECANICO],
    items: [
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
        name: 'Manutenção Preventiva',
        href: '/manutencao-preventiva',
        icon: CalendarClock,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
      },
    ],
  },
  {
    name: 'Estoque',
    icon: Package,
    items: [
      {
        name: 'Peças',
        href: '/estoque',
        icon: Package,
      },
      {
        name: 'Locais de Armazenamento',
        href: '/estoque/locais',
        icon: MapPin,
      },
    ],
  },
  {
    name: 'Financeiro',
    icon: Wallet,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
    items: [
      {
        name: 'Pagamentos',
        href: '/financeiro',
        icon: DollarSign,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
      },
      {
        name: 'Despesas',
        href: '/financeiro/despesas',
        icon: ArrowDownCircle,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
      },
      {
        name: 'Fluxo de Caixa',
        href: '/financeiro/fluxo-caixa',
        icon: TrendingUp,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE],
        requiredFeature: 'FLUXO_CAIXA_AVANCADO',
      },
      {
        name: 'DRE Simplificado',
        href: '/financeiro/dre',
        icon: FileBarChart,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE],
        requiredFeature: 'FLUXO_CAIXA_AVANCADO',
      },
      {
        name: 'Projeção Financeira',
        href: '/financeiro/projecao',
        icon: Target,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE],
        requiredFeature: 'FLUXO_CAIXA_AVANCADO',
      },
      {
        name: 'Conciliação Bancária',
        href: '/financeiro/conciliacao',
        icon: Scale,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE],
        requiredFeature: 'CONCILIACAO_BANCARIA',
      },
      {
        name: 'Parcelamento',
        href: '/financeiro/parcelamento',
        icon: Percent,
        requiredRoles: [PerfilUsuario.ADMIN],
        requiredFeature: 'PARCELAMENTO_CARTAO',
      },
      {
        name: 'Assinaturas',
        href: '/financeiro/assinaturas',
        icon: Repeat,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
        requiredFeature: 'COBRANCA_RECORRENTE',
      },
    ],
  },
  {
    name: 'Fiscal',
    icon: FileCheck,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE],
    items: [
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
        requiredFeature: 'INTEGRACAO_MERCADO_PAGO',
      },
    ],
  },
  {
    name: 'Comunicação',
    icon: MessageSquare,
    requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE, PerfilUsuario.MECANICO],
    items: [
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
        name: 'Webhooks',
        href: '/webhooks',
        icon: Webhook,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE],
      },
    ],
  },
  {
    name: 'Sistema',
    icon: Cog,
    items: [
      {
        name: 'Usuários',
        href: '/usuarios',
        icon: UserCog,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE],
      },
      {
        name: 'Meu Plano',
        href: '/meu-plano',
        icon: Crown,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE, PerfilUsuario.MECANICO],
      },
      {
        name: 'Minha Conta',
        href: '/minha-conta',
        icon: Receipt,
        requiredRoles: [PerfilUsuario.ADMIN, PerfilUsuario.GERENTE],
      },
      {
        name: 'Configurações',
        href: '/configuracoes',
        icon: Settings,
      },
    ],
  },
];

/**
 * Componente que renderiza um item de navegação com verificação de feature flag
 */
interface NavigationItemLinkProps {
  item: NavigationItem;
  isActive: boolean;
  showBadge?: boolean;
  badgeCount?: number;
  badgeVariant?: 'orange' | 'red';
  onClick?: () => void;
  isSubItem?: boolean;
}

const NavigationItemLink = ({
  item,
  isActive,
  showBadge,
  badgeCount,
  badgeVariant = 'orange',
  onClick,
  isSubItem = false,
}: NavigationItemLinkProps) => {
  const isFeatureEnabled = useFeatureFlag(item.requiredFeature || '');

  // Se tem requiredFeature e não está habilitada, não renderiza
  if (item.requiredFeature && !isFeatureEnabled) {
    return null;
  }

  return (
    <Link
      to={item.href}
      onClick={onClick}
      className={cn(
        'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
        isSubItem && 'pl-10',
        isActive
          ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-400'
          : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-300 dark:hover:bg-gray-700 dark:hover:text-white'
      )}
    >
      <item.icon className="h-5 w-5 flex-shrink-0" />
      <span className="flex-1 truncate">{item.name}</span>
      {showBadge && badgeCount && badgeCount > 0 && (
        <span
          className={cn(
            'inline-flex items-center justify-center rounded-full px-2 py-0.5 text-xs font-medium',
            badgeVariant === 'orange'
              ? 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400'
              : 'bg-red-500 text-white font-bold'
          )}
        >
          {badgeCount > 99 ? '99+' : badgeCount}
        </span>
      )}
    </Link>
  );
};

/**
 * Componente de seção colapsável do menu
 */
interface NavigationSectionProps {
  group: NavigationGroup;
  userPerfil?: PerfilUsuario;
  currentPath: string;
  contadorEstoqueBaixo?: number;
  comunicadosNaoLidos?: number;
  alertasDRE?: number;
  alertasDespesas?: number;
  alertasFluxo?: number;
  alertasAssinaturas?: number;
  onItemClick?: () => void;
}

const NavigationSection = ({
  group,
  userPerfil,
  currentPath,
  contadorEstoqueBaixo,
  comunicadosNaoLidos,
  alertasDRE,
  alertasDespesas,
  alertasFluxo,
  alertasAssinaturas,
  onItemClick,
}: NavigationSectionProps) => {
  // Filtrar itens baseado em permissões
  const visibleItems = group.items.filter((item) => {
    if (!item.requiredRoles || item.requiredRoles.length === 0) {
      return true;
    }
    return userPerfil && item.requiredRoles.includes(userPerfil);
  });

  // Se não há itens visíveis, não renderizar o grupo
  if (visibleItems.length === 0) {
    return null;
  }

  // Verificar se algum item do grupo está ativo
  const isGroupActive = visibleItems.some((item) => currentPath === item.href);

  // Estado de expansão - iniciar expandido se algum item está ativo
  const [isExpanded, setIsExpanded] = useState(isGroupActive);

  // Contar badges do grupo
  const groupBadgeCount = visibleItems.reduce((count, item) => {
    if (item.href === '/estoque' && contadorEstoqueBaixo) {
      return count + contadorEstoqueBaixo;
    }
    if (item.href === '/comunicados' && comunicadosNaoLidos) {
      return count + comunicadosNaoLidos;
    }
    if (item.href === '/financeiro/dre' && alertasDRE) {
      return count + alertasDRE;
    }
    if (item.href === '/financeiro/despesas' && alertasDespesas) {
      return count + alertasDespesas;
    }
    if (item.href === '/financeiro/fluxo-caixa' && alertasFluxo) {
      return count + alertasFluxo;
    }
    if (item.href === '/financeiro/assinaturas' && alertasAssinaturas) {
      return count + alertasAssinaturas;
    }
    return count;
  }, 0);

  return (
    <div className="space-y-1">
      {/* Header do grupo */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className={cn(
          'flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
          isGroupActive
            ? 'text-primary-700 dark:text-primary-400'
            : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-300 dark:hover:bg-gray-700 dark:hover:text-white'
        )}
      >
        <group.icon className="h-5 w-5 flex-shrink-0" />
        <span className="flex-1 text-left truncate">{group.name}</span>
        {groupBadgeCount > 0 && (
          <span className="inline-flex items-center justify-center rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-800 dark:bg-orange-900/30 dark:text-orange-400">
            {groupBadgeCount > 99 ? '99+' : groupBadgeCount}
          </span>
        )}
        {isExpanded ? (
          <ChevronDown className="h-4 w-4 flex-shrink-0" />
        ) : (
          <ChevronRight className="h-4 w-4 flex-shrink-0" />
        )}
      </button>

      {/* Itens do grupo */}
      {isExpanded && (
        <div className="space-y-1">
          {visibleItems.map((item) => {
            const isActive = currentPath === item.href;
            const showEstoqueBadge = item.href === '/estoque';
            const showComunicadosBadge = item.href === '/comunicados';
            const showDREBadge = item.href === '/financeiro/dre';
            const showDespesasBadge = item.href === '/financeiro/despesas';
            const showFluxoBadge = item.href === '/financeiro/fluxo-caixa';
            const showAssinaturasBadge = item.href === '/financeiro/assinaturas';

            const getBadgeCount = () => {
              if (showEstoqueBadge) return contadorEstoqueBaixo;
              if (showComunicadosBadge) return comunicadosNaoLidos;
              if (showDREBadge) return alertasDRE;
              if (showDespesasBadge) return alertasDespesas;
              if (showFluxoBadge) return alertasFluxo;
              if (showAssinaturasBadge) return alertasAssinaturas;
              return undefined;
            };

            const getBadgeVariant = (): 'orange' | 'red' => {
              if (showComunicadosBadge || showDREBadge || showDespesasBadge || showFluxoBadge || showAssinaturasBadge) return 'red';
              return 'orange';
            };

            return (
              <NavigationItemLink
                key={item.href}
                item={item}
                isActive={isActive}
                isSubItem
                showBadge={showEstoqueBadge || showComunicadosBadge || showDREBadge || showDespesasBadge || showFluxoBadge || showAssinaturasBadge}
                badgeCount={getBadgeCount()}
                badgeVariant={getBadgeVariant()}
                onClick={onItemClick}
              />
            );
          })}
        </div>
      )}
    </div>
  );
};

export const MainLayout = () => {
  const location = useLocation();
  const { user, logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Check if user is SUPER_ADMIN - needed before calling hooks
  const isSuperAdmin = user?.perfil === PerfilUsuario.SUPER_ADMIN;

  // WebSocket connection - automatically connects when authenticated
  const { isConnected } = useWebSocket();

  // Real-time cache invalidation - listens to WebSocket events and updates React Query cache
  useRealtimeUpdates();

  // Badge de alerta de estoque baixo (apenas para oficinas, não SUPER_ADMIN)
  const { data: contadorEstoqueBaixo } = useContadorEstoqueBaixo();

  // Badge de comunicados não lidos (apenas para usuários de oficina, não SUPER_ADMIN)
  const { data: comunicadosNaoLidos } = useComunicadosNaoLidos();

  // Badge de alertas do DRE (críticos + warnings) - apenas para oficinas
  const { total: alertasDRE } = useContadorAlertasDRE(!isSuperAdmin);

  // Badge de alertas de Despesas (críticos + warnings) - apenas para oficinas
  const { total: alertasDespesas } = useContadorAlertasDespesas(!isSuperAdmin);

  // Badge de alertas de Fluxo de Caixa (críticos + warnings) - apenas para oficinas
  const { total: alertasFluxo } = useContadorAlertasFluxo(!isSuperAdmin);

  // Badge de alertas de Assinaturas (críticos + warnings) - apenas para oficinas
  const { total: alertasAssinaturas } = useContadorAlertasAssinaturas(!isSuperAdmin);

  // Get flat navigation for SUPER_ADMIN
  const superAdminNavigation = useMemo(() => {
    if (!isSuperAdmin) return [];
    return superAdminNavigationItems;
  }, [isSuperAdmin]);

  // Filter navigation groups based on user role
  const filteredGroups = useMemo(() => {
    if (!user?.perfil || isSuperAdmin) return [];

    return navigationGroups.filter((group) => {
      if (!group.requiredRoles || group.requiredRoles.length === 0) {
        return true;
      }
      return group.requiredRoles.includes(user.perfil);
    });
  }, [user?.perfil, isSuperAdmin]);

  // Check if dashboard should be visible
  const showDashboard = useMemo(() => {
    if (isSuperAdmin) return false;
    if (!dashboardItem.requiredRoles || dashboardItem.requiredRoles.length === 0) {
      return true;
    }
    return user?.perfil && dashboardItem.requiredRoles.includes(user.perfil);
  }, [user?.perfil, isSuperAdmin]);

  // Get current page name for header
  const currentPageName = useMemo(() => {
    if (location.pathname === '/') return 'Dashboard';

    // Check in dashboard item
    if (dashboardItem.href === location.pathname) {
      return dashboardItem.name;
    }

    // Check in super admin navigation
    const superAdminItem = superAdminNavigation.find((item) => item.href === location.pathname);
    if (superAdminItem) return superAdminItem.name;

    // Check in grouped navigation
    for (const group of navigationGroups) {
      const item = group.items.find((i) => i.href === location.pathname);
      if (item) return item.name;
    }

    return 'PitStop';
  }, [location.pathname, superAdminNavigation]);

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error('Erro ao fazer logout:', error);
    }
  };

  const renderSidebarContent = (onItemClick?: () => void) => (
    <>
      {/* SUPER_ADMIN navigation (flat list) */}
      {isSuperAdmin && (
        <nav className="flex-1 space-y-1 overflow-y-auto p-4">
          {superAdminNavigation.map((item) => {
            const isActive = location.pathname === item.href;
            return (
              <NavigationItemLink
                key={item.href}
                item={item}
                isActive={isActive}
                onClick={onItemClick}
              />
            );
          })}
        </nav>
      )}

      {/* Regular user navigation (grouped) */}
      {!isSuperAdmin && (
        <nav className="flex-1 space-y-1 overflow-y-auto p-4">
          {/* Dashboard (standalone) */}
          {showDashboard && (
            <NavigationItemLink
              item={dashboardItem}
              isActive={location.pathname === dashboardItem.href}
              onClick={onItemClick}
            />
          )}

          {/* Divider after Dashboard */}
          {showDashboard && filteredGroups.length > 0 && (
            <div className="my-3 border-t border-gray-200 dark:border-gray-700" />
          )}

          {/* Grouped navigation */}
          {filteredGroups.map((group) => (
            <NavigationSection
              key={group.name}
              group={group}
              userPerfil={user?.perfil}
              currentPath={location.pathname}
              contadorEstoqueBaixo={contadorEstoqueBaixo}
              comunicadosNaoLidos={comunicadosNaoLidos}
              alertasDRE={alertasDRE}
              alertasDespesas={alertasDespesas}
              alertasFluxo={alertasFluxo}
              alertasAssinaturas={alertasAssinaturas}
              onItemClick={onItemClick}
            />
          ))}
        </nav>
      )}
    </>
  );

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

        {renderSidebarContent()}

        <div className="border-t border-gray-200 p-4 dark:border-gray-700">
          <div className="mb-3 flex items-center gap-3 px-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-100 text-sm font-medium text-primary-700 dark:bg-primary-900/30 dark:text-primary-400">
              {user?.nome.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
                {user?.nome}
              </p>
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
          <aside className="fixed inset-y-0 left-0 w-64 flex flex-col bg-white dark:bg-gray-800">
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

            {renderSidebarContent(() => setSidebarOpen(false))}

            <div className="border-t border-gray-200 p-4 dark:border-gray-700">
              <div className="mb-3 flex items-center gap-3 px-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-100 text-sm font-medium text-primary-700 dark:bg-primary-900/30 dark:text-primary-400">
                  {user?.nome.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="truncate text-sm font-medium text-gray-900 dark:text-white">
                    {user?.nome}
                  </p>
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
              {currentPageName}
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
