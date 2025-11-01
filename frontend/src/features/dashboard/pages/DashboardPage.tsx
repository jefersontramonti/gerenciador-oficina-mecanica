import { useAuth } from '@/features/auth/hooks/useAuth';

export const DashboardPage = () => {
  const { user } = useAuth();

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-2 text-gray-600">
          Bem-vindo(a), <span className="font-medium">{user?.nome}</span>!
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-lg bg-white p-6 shadow">
          <h3 className="text-sm font-medium text-gray-600">Ordens em Andamento</h3>
          <p className="mt-2 text-3xl font-bold text-primary-600">0</p>
        </div>

        <div className="rounded-lg bg-white p-6 shadow">
          <h3 className="text-sm font-medium text-gray-600">Ordens Pendentes</h3>
          <p className="mt-2 text-3xl font-bold text-yellow-600">0</p>
        </div>

        <div className="rounded-lg bg-white p-6 shadow">
          <h3 className="text-sm font-medium text-gray-600">Faturamento do Mês</h3>
          <p className="mt-2 text-3xl font-bold text-green-600">R$ 0,00</p>
        </div>

        <div className="rounded-lg bg-white p-6 shadow">
          <h3 className="text-sm font-medium text-gray-600">Peças em Estoque</h3>
          <p className="mt-2 text-3xl font-bold text-blue-600">0</p>
        </div>
      </div>

      <div className="mt-6 rounded-lg bg-white p-6 shadow">
        <h2 className="mb-4 text-xl font-semibold text-gray-900">Ordens de Serviço Recentes</h2>
        <p className="text-gray-500">Nenhuma ordem de serviço cadastrada ainda.</p>
      </div>
    </div>
  );
};
