import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { ShieldAlert, Home, ArrowLeft, FileQuestion } from 'lucide-react';
import { useAuth } from './features/auth/hooks/useAuth';
import { AuthInitializer } from './features/auth/components/AuthInitializer';
import { ProtectedRoute } from './shared/components/common/ProtectedRoute';
import { PageLoader } from './shared/components/common/PageLoader';
import { MainLayout } from './shared/layouts/MainLayout';
import { PerfilUsuario } from './features/auth/types';
import { ErrorBoundary } from './shared/components/ErrorBoundary';

// Lazy-loaded pages for code splitting
// Auth pages
const LoginPage = lazy(() => import('./features/auth/pages').then(m => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import('./features/auth/pages').then(m => ({ default: m.RegisterPage })));
const ForgotPasswordPage = lazy(() => import('./features/auth/pages').then(m => ({ default: m.ForgotPasswordPage })));
const ResetPasswordPage = lazy(() => import('./features/auth/pages').then(m => ({ default: m.ResetPasswordPage })));

// Dashboard
const DashboardPage = lazy(() => import('./features/dashboard/pages/DashboardPage').then(m => ({ default: m.DashboardPage })));

// Clientes
const ClientesListPage = lazy(() => import('./features/clientes/pages/ClientesListPage').then(m => ({ default: m.ClientesListPage })));
const ClienteFormPage = lazy(() => import('./features/clientes/pages/ClienteFormPage').then(m => ({ default: m.ClienteFormPage })));
const ClienteDetailPage = lazy(() => import('./features/clientes/pages/ClienteDetailPage').then(m => ({ default: m.ClienteDetailPage })));

// Veículos
const VeiculosListPage = lazy(() => import('./features/veiculos/pages/VeiculosListPage').then(m => ({ default: m.VeiculosListPage })));
const VeiculoFormPage = lazy(() => import('./features/veiculos/pages/VeiculoFormPage').then(m => ({ default: m.VeiculoFormPage })));
const VeiculoDetailPage = lazy(() => import('./features/veiculos/pages/VeiculoDetailPage').then(m => ({ default: m.VeiculoDetailPage })));

// Ordens de Serviço
const OrdemServicoListPage = lazy(() => import('./features/ordens-servico/pages/OrdemServicoListPage').then(m => ({ default: m.OrdemServicoListPage })));
const OrdemServicoFormPage = lazy(() => import('./features/ordens-servico/pages/OrdemServicoFormPage').then(m => ({ default: m.OrdemServicoFormPage })));
const OrdemServicoDetailPage = lazy(() => import('./features/ordens-servico/pages/OrdemServicoDetailPage').then(m => ({ default: m.OrdemServicoDetailPage })));

// Usuários
const UsuariosListPage = lazy(() => import('./features/usuarios/pages').then(m => ({ default: m.UsuariosListPage })));
const UsuarioFormPage = lazy(() => import('./features/usuarios/pages').then(m => ({ default: m.UsuarioFormPage })));
const UsuarioDetailPage = lazy(() => import('./features/usuarios/pages').then(m => ({ default: m.UsuarioDetailPage })));

// Estoque - Peças
const PecasListPage = lazy(() => import('./features/estoque/pages').then(m => ({ default: m.PecasListPage })));
const PecaFormPage = lazy(() => import('./features/estoque/pages').then(m => ({ default: m.PecaFormPage })));
const PecaDetailPage = lazy(() => import('./features/estoque/pages').then(m => ({ default: m.PecaDetailPage })));
const AlertasEstoquePage = lazy(() => import('./features/estoque/pages').then(m => ({ default: m.AlertasEstoquePage })));
const PecasSemLocalizacaoPage = lazy(() => import('./features/estoque/pages').then(m => ({ default: m.PecasSemLocalizacaoPage })));

// Estoque - Locais de Armazenamento
const LocaisArmazenamentoListPage = lazy(() => import('./features/estoque/pages').then(m => ({ default: m.LocaisArmazenamentoListPage })));
const LocalArmazenamentoFormPage = lazy(() => import('./features/estoque/pages').then(m => ({ default: m.LocalArmazenamentoFormPage })));
const LocalArmazenamentoDetailPage = lazy(() => import('./features/estoque/pages').then(m => ({ default: m.LocalArmazenamentoDetailPage })));

// Financeiro
const PagamentosPage = lazy(() => import('./features/financeiro/pages/PagamentosPage').then(m => ({ default: m.PagamentosPage })));
const NotasFiscaisListPage = lazy(() => import('./features/financeiro/pages/NotasFiscaisListPage').then(m => ({ default: m.NotasFiscaisListPage })));
const NotaFiscalFormPage = lazy(() => import('./features/financeiro/pages/NotaFiscalFormPage').then(m => ({ default: m.NotaFiscalFormPage })));
const NotaFiscalDetailPage = lazy(() => import('./features/financeiro/pages/NotaFiscalDetailPage').then(m => ({ default: m.NotaFiscalDetailPage })));

// Configurações
const ConfiguracoesPage = lazy(() => import('./features/configuracoes/pages').then(m => ({ default: m.ConfiguracoesPage })));

// Notificações
const ConfiguracaoNotificacoesPage = lazy(() => import('./features/notificacoes/pages/ConfiguracaoNotificacoesPage').then(m => ({ default: m.ConfiguracaoNotificacoesPage })));
const HistoricoNotificacoesPage = lazy(() => import('./features/notificacoes/pages/HistoricoNotificacoesPage').then(m => ({ default: m.HistoricoNotificacoesPage })));

// Comunicados (para oficinas)
const ComunicadosOficinaPage = lazy(() => import('./features/comunicados').then(m => ({ default: m.ComunicadosPage })));

// Paginas Publicas (aprovacao de orcamento)
const AprovarOrcamentoPage = lazy(() => import('./features/ordens-servico/pages/AprovarOrcamentoPage').then(m => ({ default: m.AprovarOrcamentoPage })));

// Admin (SUPER_ADMIN) pages
const SaasDashboardPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.SaasDashboardPage })));
const AdminOficinasPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.OficinasPage })));
const OficinaDetailPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.OficinaDetailPage })));
const CreateOficinaPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.CreateOficinaPage })));
const EditOficinaPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.EditOficinaPage })));
const AdminPagamentosPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.PagamentosPage })));
const AuditPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.AuditPage })));
const PlanosListPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.PlanosListPage })));
const PlanoFormPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.PlanoFormPage })));
const FaturasListPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.FaturasListPage })));
const FaturaDetailPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.FaturaDetailPage })));
const InadimplenciaPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.InadimplenciaPage })));
const RelatoriosPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.RelatoriosPage })));
const TicketsListPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.TicketsListPage })));
const TicketDetailPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.TicketDetailPage })));
const ComunicadosListPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.ComunicadosListPage })));
const ComunicadoFormPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.ComunicadoFormPage })));
const ComunicadoDetailPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.ComunicadoDetailPage })));
const FeatureFlagsPage = lazy(() => import('./features/admin/pages').then(m => ({ default: m.FeatureFlagsPage })));

function App() {
  return (
    <BrowserRouter>
      <ErrorBoundary boundaryName="App Root">
        <AuthInitializer>
          <Suspense fallback={<PageLoader />}>
            <Routes>
            {/* Public routes */}
            <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
            <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />
            <Route path="/forgot-password" element={<PublicRoute><ForgotPasswordPage /></PublicRoute>} />
            <Route path="/reset-password" element={<PublicRoute><ResetPasswordPage /></PublicRoute>} />

            {/* Aprovacao de orcamento (rota publica - cliente acessa via email) */}
            <Route path="/orcamento/aprovar" element={<AprovarOrcamentoPage />} />

            {/* Protected routes */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <MainLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<DashboardPage />} />

          {/* Clientes - Acessível por ADMIN, GERENTE, ATENDENTE */}
          <Route path="clientes">
            <Route
              index
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <ClientesListPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="novo"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <ClienteFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path=":id"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <ClienteDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path=":id/editar"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <ClienteFormPage />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Veículos - Acessível por ADMIN, GERENTE, ATENDENTE */}
          <Route path="veiculos">
            <Route
              index
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <VeiculosListPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="novo"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <VeiculoFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path=":id"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <VeiculoDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path=":id/editar"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <VeiculoFormPage />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Ordens de Serviço - Todos podem ver */}
          <Route path="ordens-servico">
            <Route
              index
              element={
                <ProtectedRoute>
                  <OrdemServicoListPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="novo"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <OrdemServicoFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path=":id"
              element={
                <ProtectedRoute>
                  <OrdemServicoDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path=":id/editar"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <OrdemServicoFormPage />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Usuários - ADMIN e GERENTE */}
          <Route path="usuarios">
            <Route
              index
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}
                >
                  <UsuariosListPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="novo"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}
                >
                  <UsuarioFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path=":id"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}
                >
                  <UsuarioDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path=":id/editar"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}
                >
                  <UsuarioFormPage />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Estoque - Todos podem ver, ADMIN/GERENTE/ATENDENTE podem modificar */}
          <Route path="estoque">
            <Route
              index
              element={
                <ProtectedRoute>
                  <PecasListPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="novo"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <PecaFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="alertas"
              element={
                <ProtectedRoute>
                  <AlertasEstoquePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="sem-localizacao"
              element={
                <ProtectedRoute>
                  <PecasSemLocalizacaoPage />
                </ProtectedRoute>
              }
            />
            <Route
              path=":id"
              element={
                <ProtectedRoute>
                  <PecaDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path=":id/editar"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <PecaFormPage />
                </ProtectedRoute>
              }
            />

            {/* Locais de Armazenamento */}
            <Route path="locais">
              <Route
                index
                element={
                  <ProtectedRoute>
                    <LocaisArmazenamentoListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="novo"
                element={
                  <ProtectedRoute
                    requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                  >
                    <LocalArmazenamentoFormPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id"
                element={
                  <ProtectedRoute>
                    <LocalArmazenamentoDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id/editar"
                element={
                  <ProtectedRoute
                    requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                  >
                    <LocalArmazenamentoFormPage />
                  </ProtectedRoute>
                }
              />
            </Route>
          </Route>

          {/* Financeiro - ADMIN, GERENTE e ATENDENTE */}
          <Route path="financeiro">
            <Route
              index
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                >
                  <PagamentosPage />
                </ProtectedRoute>
              }
            />

            {/* Notas Fiscais */}
            <Route path="notas-fiscais">
              <Route
                index
                element={
                  <ProtectedRoute
                    requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                  >
                    <NotasFiscaisListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="novo"
                element={
                  <ProtectedRoute
                    requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                  >
                    <NotaFiscalFormPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id"
                element={
                  <ProtectedRoute>
                    <NotaFiscalDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id/editar"
                element={
                  <ProtectedRoute
                    requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
                  >
                    <NotaFiscalFormPage />
                  </ProtectedRoute>
                }
              />
            </Route>
          </Route>

          {/* Configurações */}
          <Route
            path="configuracoes"
            element={
              <ProtectedRoute>
                <ConfiguracoesPage />
              </ProtectedRoute>
            }
          />

          {/* Notificações - ADMIN e GERENTE */}
          <Route path="notificacoes">
            <Route
              path="configuracao"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}
                >
                  <ConfiguracaoNotificacoesPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="historico"
              element={
                <ProtectedRoute
                  requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}
                >
                  <HistoricoNotificacoesPage />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Comunicados - Todos da oficina podem ver */}
          <Route
            path="comunicados"
            element={
              <ProtectedRoute
                requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE, PerfilUsuario.MECANICO]}
              >
                <ComunicadosOficinaPage />
              </ProtectedRoute>
            }
          />

          {/* Admin SaaS - SUPER_ADMIN apenas */}
          <Route path="admin">
            <Route
              index
              element={
                <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                  <SaasDashboardPage />
                </ProtectedRoute>
              }
            />
            <Route path="oficinas">
              <Route
                index
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <AdminOficinasPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="nova"
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <CreateOficinaPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id"
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <OficinaDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id/editar"
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <EditOficinaPage />
                  </ProtectedRoute>
                }
              />
            </Route>
            <Route
              path="pagamentos"
              element={
                <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                  <AdminPagamentosPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="audit"
              element={
                <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                  <AuditPage />
                </ProtectedRoute>
              }
            />
            <Route path="planos">
              <Route
                index
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <PlanosListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="novo"
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <PlanoFormPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id/editar"
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <PlanoFormPage />
                  </ProtectedRoute>
                }
              />
            </Route>
            <Route path="faturas">
              <Route
                index
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <FaturasListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id"
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <FaturaDetailPage />
                  </ProtectedRoute>
                }
              />
            </Route>
            <Route
              path="inadimplencia"
              element={
                <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                  <InadimplenciaPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="relatorios"
              element={
                <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                  <RelatoriosPage />
                </ProtectedRoute>
              }
            />
            <Route path="tickets">
              <Route
                index
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <TicketsListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id"
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <TicketDetailPage />
                  </ProtectedRoute>
                }
              />
            </Route>
            <Route path="comunicados">
              <Route
                index
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <ComunicadosListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="novo"
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <ComunicadoFormPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id"
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <ComunicadoDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path=":id/editar"
                element={
                  <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                    <ComunicadoFormPage />
                  </ProtectedRoute>
                }
              />
            </Route>
            <Route
              path="features"
              element={
                <ProtectedRoute requiredRoles={[PerfilUsuario.SUPER_ADMIN]}>
                  <FeatureFlagsPage />
                </ProtectedRoute>
              }
            />
          </Route>
        </Route>

        {/* Unauthorized */}
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

            {/* 404 */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
      </AuthInitializer>
      </ErrorBoundary>
    </BrowserRouter>
  );
}

/**
 * Public route - redirects to home if already authenticated
 * SUPER_ADMIN users are redirected to /admin instead of /
 */
function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading, user } = useAuth();

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-white dark:bg-gray-900">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (isAuthenticated) {
    // SUPER_ADMIN goes to /admin, others go to /
    if (user?.perfil === PerfilUsuario.SUPER_ADMIN) {
      return <Navigate to="/admin" replace />;
    }
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

/**
 * Unauthorized page - Acesso negado (403)
 */
function UnauthorizedPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-orange-50 to-red-50 p-6 dark:from-orange-950 dark:to-red-950">
      <div className="w-full max-w-md">
        {/* Card */}
        <div className="rounded-2xl bg-white p-8 shadow-xl dark:bg-gray-800">
          {/* Ícone */}
          <div className="mb-6 flex justify-center">
            <div className="rounded-full bg-orange-100 p-4 dark:bg-orange-900/30">
              <ShieldAlert className="h-16 w-16 text-orange-600 dark:text-orange-400" strokeWidth={1.5} />
            </div>
          </div>

          {/* Título */}
          <div className="text-center">
            <h1 className="text-6xl font-bold text-gray-900 dark:text-white">403</h1>
            <h2 className="mt-4 text-2xl font-semibold text-gray-800 dark:text-gray-100">
              Acesso Negado
            </h2>
            <p className="mt-3 text-gray-600 dark:text-gray-300">
              Você não tem permissão para acessar esta página.
            </p>
            <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
              Apenas usuários com perfil de Administrador ou Gerente podem visualizar este conteúdo.
            </p>
          </div>

          {/* Ações */}
          <div className="mt-8 space-y-3">
            {/* Botão principal - Dashboard */}
            <Link
              to="/"
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-6 py-3 font-medium text-white shadow-sm transition-colors hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-600"
            >
              <Home className="h-5 w-5" />
              Ir para o Dashboard
            </Link>

            {/* Botão secundário - Voltar */}
            <button
              onClick={() => window.history.back()}
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-6 py-3 font-medium text-gray-700 transition-colors hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-200 dark:hover:bg-gray-600"
            >
              <ArrowLeft className="h-5 w-5" />
              Voltar à página anterior
            </button>
          </div>

          {/* Informação adicional */}
          <div className="mt-6 rounded-lg border border-orange-200 bg-orange-50 p-4 dark:border-orange-800 dark:bg-orange-900/20">
            <p className="text-sm text-orange-800 dark:text-orange-300">
              <strong>Dica:</strong> Se você acredita que deveria ter acesso a esta página,
              entre em contato com o administrador do sistema.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * Not found page - Página não encontrada (404)
 */
function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-50 p-6 dark:from-blue-950 dark:to-indigo-950">
      <div className="w-full max-w-md">
        {/* Card */}
        <div className="rounded-2xl bg-white p-8 shadow-xl dark:bg-gray-800">
          {/* Ícone */}
          <div className="mb-6 flex justify-center">
            <div className="rounded-full bg-blue-100 p-4 dark:bg-blue-900/30">
              <FileQuestion className="h-16 w-16 text-blue-600 dark:text-blue-400" strokeWidth={1.5} />
            </div>
          </div>

          {/* Título */}
          <div className="text-center">
            <h1 className="text-6xl font-bold text-gray-900 dark:text-white">404</h1>
            <h2 className="mt-4 text-2xl font-semibold text-gray-800 dark:text-gray-100">
              Página Não Encontrada
            </h2>
            <p className="mt-3 text-gray-600 dark:text-gray-300">
              Ops! A página que você está procurando não existe.
            </p>
            <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
              O link pode estar quebrado ou a página foi removida.
            </p>
          </div>

          {/* Ações */}
          <div className="mt-8 space-y-3">
            {/* Botão principal - Dashboard */}
            <Link
              to="/"
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-6 py-3 font-medium text-white shadow-sm transition-colors hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-600"
            >
              <Home className="h-5 w-5" />
              Ir para o Dashboard
            </Link>

            {/* Botão secundário - Voltar */}
            <button
              onClick={() => window.history.back()}
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-6 py-3 font-medium text-gray-700 transition-colors hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-200 dark:hover:bg-gray-600"
            >
              <ArrowLeft className="h-5 w-5" />
              Voltar à página anterior
            </button>
          </div>

          {/* Informação adicional */}
          <div className="mt-6 rounded-lg border border-blue-200 bg-blue-50 p-4 dark:border-blue-800 dark:bg-blue-900/20">
            <p className="text-sm text-blue-800 dark:text-blue-300">
              <strong>Dica:</strong> Verifique se o endereço da URL está correto ou
              use a navegação do menu para encontrar o que procura.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
