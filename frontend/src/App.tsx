import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { ShieldAlert, Home, ArrowLeft, FileQuestion } from 'lucide-react';
import { useAuth } from './features/auth/hooks/useAuth';
import { AuthInitializer } from './features/auth/components/AuthInitializer';
import { ProtectedRoute } from './shared/components/common/ProtectedRoute';
import { MainLayout } from './shared/layouts/MainLayout';
import { LoginPage, RegisterPage, ForgotPasswordPage, ResetPasswordPage } from './features/auth/pages';
import { DashboardPage } from './features/dashboard/pages/DashboardPage';
import { ClientesListPage } from './features/clientes/pages/ClientesListPage';
import { ClienteFormPage } from './features/clientes/pages/ClienteFormPage';
import { ClienteDetailPage } from './features/clientes/pages/ClienteDetailPage';
import { VeiculosListPage } from './features/veiculos/pages/VeiculosListPage';
import { VeiculoFormPage } from './features/veiculos/pages/VeiculoFormPage';
import { VeiculoDetailPage } from './features/veiculos/pages/VeiculoDetailPage';
import { OrdemServicoListPage } from './features/ordens-servico/pages/OrdemServicoListPage';
import { OrdemServicoFormPage } from './features/ordens-servico/pages/OrdemServicoFormPage';
import { OrdemServicoDetailPage } from './features/ordens-servico/pages/OrdemServicoDetailPage';
import { UsuariosListPage, UsuarioFormPage, UsuarioDetailPage } from './features/usuarios/pages';
import {
  PecasListPage,
  PecaFormPage,
  PecaDetailPage,
  AlertasEstoquePage,
  PecasSemLocalizacaoPage,
  LocaisArmazenamentoListPage,
  LocalArmazenamentoFormPage,
  LocalArmazenamentoDetailPage,
} from './features/estoque/pages';
import { PagamentosPage } from './features/financeiro/pages/PagamentosPage';
import { NotasFiscaisListPage } from './features/financeiro/pages/NotasFiscaisListPage';
import { NotaFiscalFormPage } from './features/financeiro/pages/NotaFiscalFormPage';
import { NotaFiscalDetailPage } from './features/financeiro/pages/NotaFiscalDetailPage';
import { PerfilUsuario } from './features/auth/types';

function App() {
  return (
    <BrowserRouter>
      <AuthInitializer>
        <Routes>
        {/* Public routes */}
        <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
        <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />
        <Route path="/forgot-password" element={<PublicRoute><ForgotPasswordPage /></PublicRoute>} />
        <Route path="/reset-password" element={<PublicRoute><ResetPasswordPage /></PublicRoute>} />

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
            element={<ComingSoonPage title="Configurações" />}
          />
        </Route>

        {/* Unauthorized */}
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        {/* 404 */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      </AuthInitializer>
    </BrowserRouter>
  );
}

/**
 * Public route - redirects to home if already authenticated
 */
function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

/**
 * Coming soon placeholder page
 */
function ComingSoonPage({ title }: { title: string }) {
  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center p-6">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-gray-900">{title}</h1>
        <p className="mt-4 text-lg text-gray-600">Esta funcionalidade está em desenvolvimento.</p>
        <p className="mt-2 text-sm text-gray-500">Em breve estará disponível!</p>
      </div>
    </div>
  );
}

/**
 * Unauthorized page - Acesso negado (403)
 */
function UnauthorizedPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-orange-50 to-red-50 p-6">
      <div className="w-full max-w-md">
        {/* Card */}
        <div className="rounded-2xl bg-white p-8 shadow-xl">
          {/* Ícone */}
          <div className="mb-6 flex justify-center">
            <div className="rounded-full bg-orange-100 p-4">
              <ShieldAlert className="h-16 w-16 text-orange-600" strokeWidth={1.5} />
            </div>
          </div>

          {/* Título */}
          <div className="text-center">
            <h1 className="text-6xl font-bold text-gray-900">403</h1>
            <h2 className="mt-4 text-2xl font-semibold text-gray-800">
              Acesso Negado
            </h2>
            <p className="mt-3 text-gray-600">
              Você não tem permissão para acessar esta página.
            </p>
            <p className="mt-2 text-sm text-gray-500">
              Apenas usuários com perfil de Administrador ou Gerente podem visualizar este conteúdo.
            </p>
          </div>

          {/* Ações */}
          <div className="mt-8 space-y-3">
            {/* Botão principal - Dashboard */}
            <Link
              to="/"
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-6 py-3 font-medium text-white shadow-sm transition-colors hover:bg-blue-700"
            >
              <Home className="h-5 w-5" />
              Ir para o Dashboard
            </Link>

            {/* Botão secundário - Voltar */}
            <button
              onClick={() => window.history.back()}
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-6 py-3 font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              <ArrowLeft className="h-5 w-5" />
              Voltar à página anterior
            </button>
          </div>

          {/* Informação adicional */}
          <div className="mt-6 rounded-lg border border-orange-200 bg-orange-50 p-4">
            <p className="text-sm text-orange-800">
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
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-50 p-6">
      <div className="w-full max-w-md">
        {/* Card */}
        <div className="rounded-2xl bg-white p-8 shadow-xl">
          {/* Ícone */}
          <div className="mb-6 flex justify-center">
            <div className="rounded-full bg-blue-100 p-4">
              <FileQuestion className="h-16 w-16 text-blue-600" strokeWidth={1.5} />
            </div>
          </div>

          {/* Título */}
          <div className="text-center">
            <h1 className="text-6xl font-bold text-gray-900">404</h1>
            <h2 className="mt-4 text-2xl font-semibold text-gray-800">
              Página Não Encontrada
            </h2>
            <p className="mt-3 text-gray-600">
              Ops! A página que você está procurando não existe.
            </p>
            <p className="mt-2 text-sm text-gray-500">
              O link pode estar quebrado ou a página foi removida.
            </p>
          </div>

          {/* Ações */}
          <div className="mt-8 space-y-3">
            {/* Botão principal - Dashboard */}
            <Link
              to="/"
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-6 py-3 font-medium text-white shadow-sm transition-colors hover:bg-blue-700"
            >
              <Home className="h-5 w-5" />
              Ir para o Dashboard
            </Link>

            {/* Botão secundário - Voltar */}
            <button
              onClick={() => window.history.back()}
              className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-6 py-3 font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              <ArrowLeft className="h-5 w-5" />
              Voltar à página anterior
            </button>
          </div>

          {/* Informação adicional */}
          <div className="mt-6 rounded-lg border border-blue-200 bg-blue-50 p-4">
            <p className="text-sm text-blue-800">
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
