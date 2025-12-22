import { useNavigate, useParams, Link } from 'react-router-dom';
import { useVeiculo, useDeleteVeiculo } from '../hooks/useVeiculos';
import { ArrowLeft, Edit, Trash2, Car, User, Calendar, Gauge } from 'lucide-react';
import { showError } from '@/shared/utils/notifications';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

export const VeiculoDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  const { data: veiculo, isLoading, error } = useVeiculo(id);
  const deleteMutation = useDeleteVeiculo();

  const handleDelete = async () => {
    if (window.confirm('Tem certeza que deseja remover este veículo?')) {
      try {
        await deleteMutation.mutateAsync(id!);
        navigate('/veiculos');
      } catch (error: any) {
        if (error.response?.status === 409) {
          showError('Não é possível remover este veículo pois há ordens de serviço vinculadas.');
        } else {
          showError('Erro ao remover veículo');
        }
      }
    }
  };

  const formatDate = (date: string | number[]) => {
    if (Array.isArray(date)) {
      const [year, month, day, hour = 0, minute = 0, second = 0] = date;
      return format(new Date(year, month - 1, day, hour, minute, second), "dd/MM/yyyy 'às' HH:mm", {
        locale: ptBR,
      });
    }
    return format(new Date(date), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR });
  };

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Carregando...</div>
      </div>
    );
  }

  if (error || !veiculo) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          Veículo não encontrado
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/veiculos')}
            className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <ArrowLeft className="h-5 w-5 text-gray-900 dark:text-gray-100" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{veiculo.placa}</h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              {veiculo.marca} {veiculo.modelo} • {veiculo.ano}
            </p>
          </div>
        </div>

        <div className="flex gap-2">
          <Link
            to={`/veiculos/${id}/editar`}
            className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
          >
            <Edit className="h-5 w-5" />
            Editar
          </Link>
          <button
            onClick={handleDelete}
            disabled={deleteMutation.isPending}
            className="flex items-center gap-2 rounded-lg border border-red-600 dark:border-red-700 bg-white dark:bg-gray-700 px-4 py-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Trash2 className="h-5 w-5" />
            Remover
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Dados do Veículo */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-gray-100">
              <Car className="h-5 w-5" />
              Dados do Veículo
            </h2>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Placa</label>
                <p className="mt-1 text-lg font-semibold text-gray-900 dark:text-gray-100">{veiculo.placa}</p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Chassi</label>
                <p className="mt-1 font-mono text-sm text-gray-900 dark:text-gray-100">
                  {veiculo.chassi || '-'}
                </p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Marca</label>
                <p className="mt-1 text-gray-900 dark:text-gray-100">{veiculo.marca}</p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Modelo</label>
                <p className="mt-1 text-gray-900 dark:text-gray-100">{veiculo.modelo}</p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Ano</label>
                <p className="mt-1 flex items-center gap-2 text-gray-900 dark:text-gray-100">
                  <Calendar className="h-4 w-4 text-gray-400 dark:text-gray-500" />
                  {veiculo.ano}
                </p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Cor</label>
                <p className="mt-1 text-gray-900 dark:text-gray-100">{veiculo.cor || '-'}</p>
              </div>

              <div className="md:col-span-2">
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Quilometragem</label>
                <p className="mt-1 flex items-center gap-2 text-gray-900 dark:text-gray-100">
                  <Gauge className="h-4 w-4 text-gray-400 dark:text-gray-500" />
                  {veiculo.quilometragem
                    ? `${veiculo.quilometragem.toLocaleString('pt-BR')} km`
                    : 'Não informada'}
                </p>
              </div>
            </div>
          </div>

          {/* Proprietário */}
          {veiculo.cliente && (
            <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
              <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-gray-100">
                <User className="h-5 w-5" />
                Proprietário
              </h2>

              <div className="space-y-3">
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Nome</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">{veiculo.cliente.nome}</p>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">CPF/CNPJ</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">{veiculo.cliente.cpfCnpj}</p>
                </div>

                {veiculo.cliente.telefone && (
                  <div>
                    <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Telefone</label>
                    <p className="mt-1 text-gray-900 dark:text-gray-100">
                      <a
                        href={`tel:${veiculo.cliente.telefone}`}
                        className="text-blue-600 dark:text-blue-400 hover:underline"
                      >
                        {veiculo.cliente.telefone}
                      </a>
                    </p>
                  </div>
                )}

                <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                  <Link
                    to={`/clientes/${veiculo.clienteId}`}
                    className="text-sm text-blue-600 dark:text-blue-400 hover:underline"
                  >
                    Ver perfil completo do cliente →
                  </Link>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Metadata */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Informações</h3>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">Cadastrado em</label>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{formatDate(veiculo.createdAt)}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">Última atualização</label>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{formatDate(veiculo.updatedAt)}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">ID</label>
                <p className="mt-1 text-xs font-mono text-gray-600 dark:text-gray-400">{veiculo.id}</p>
              </div>
            </div>
          </div>

          {/* Ações Rápidas */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Ações Rápidas</h3>

            <div className="space-y-2">
              <button
                onClick={() => navigate(`/ordens-servico/novo?veiculo=${id}`)}
                className="w-full rounded-lg border border-green-600 dark:border-green-700 bg-white dark:bg-gray-700 px-4 py-2 text-sm font-medium text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30"
              >
                Nova Ordem de Serviço
              </button>

              <Link
                to={`/ordens-servico?veiculo=${id}`}
                className="block w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-center text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
              >
                Ver Histórico de OS
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
