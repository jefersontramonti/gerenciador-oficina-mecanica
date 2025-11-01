import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './features/auth/hooks/useAuth';
import { ProtectedRoute } from './shared/components/common/ProtectedRoute';
import { MainLayout } from './shared/layouts/MainLayout';
import { LoginPage } from './features/auth/pages/LoginPage';
import { DashboardPage } from './features/dashboard/pages/DashboardPage';
import { ClientesListPage } from './features/clientes/pages/ClientesListPage';
import { ClienteFormPage } from './features/clientes/pages/ClienteFormPage';
import { ClienteDetailPage } from './features/clientes/pages/ClienteDetailPage';
import { PerfilUsuario } from './features/auth/types';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />

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
          <Route
            path="veiculos"
            element={
              <ProtectedRoute
                requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE, PerfilUsuario.ATENDENTE]}
              >
                <ComingSoonPage title="Veículos" />
              </ProtectedRoute>
            }
          />

          {/* Ordens de Serviço - Todos podem ver */}
          <Route
            path="ordens-servico"
            element={<ComingSoonPage title="Ordens de Serviço" />}
          />

          {/* Estoque - Todos podem ver, mas só ADMIN e GERENTE podem modificar */}
          <Route
            path="estoque"
            element={<ComingSoonPage title="Estoque" />}
          />

          {/* Financeiro - Apenas ADMIN e GERENTE */}
          <Route
            path="financeiro"
            element={
              <ProtectedRoute
                requiredRoles={[PerfilUsuario.ADMIN, PerfilUsuario.GERENTE]}
              >
                <ComingSoonPage title="Financeiro" />
              </ProtectedRoute>
            }
          />

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
 * Unauthorized page
 */
function UnauthorizedPage() {
  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-gray-900">403</h1>
        <p className="mt-4 text-xl text-gray-600">Acesso não autorizado</p>
        <p className="mt-2 text-gray-500">Você não tem permissão para acessar esta página.</p>
        <a
          href="/"
          className="mt-6 inline-block rounded-md bg-primary-600 px-6 py-2.5 text-white hover:bg-primary-700"
        >
          Voltar ao Dashboard
        </a>
      </div>
    </div>
  );
}

/**
 * Not found page
 */
function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-gray-900">404</h1>
        <p className="mt-4 text-xl text-gray-600">Página não encontrada</p>
        <p className="mt-2 text-gray-500">A página que você está procurando não existe.</p>
        <a
          href="/"
          className="mt-6 inline-block rounded-md bg-primary-600 px-6 py-2.5 text-white hover:bg-primary-700"
        >
          Voltar ao Dashboard
        </a>
      </div>
    </div>
  );
}

export default App;
