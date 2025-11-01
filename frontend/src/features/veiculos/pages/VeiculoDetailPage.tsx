import { useNavigate, useParams, Link } from 'react-router-dom';
import { useVeiculo, useDeleteVeiculo } from '../hooks/useVeiculos';
import { ArrowLeft, Edit, Trash2, Car, User, Calendar, Gauge } from 'lucide-react';
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
          alert('Não é possível remover este veículo pois há ordens de serviço vinculadas.');
        } else {
          alert('Erro ao remover veículo');
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
        <div className="text-gray-500">Carregando...</div>
      </div>
    );
  }

  if (error || !veiculo) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
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
            className="rounded-lg p-2 hover:bg-gray-100"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{veiculo.placa}</h1>
            <p className="mt-1 text-sm text-gray-600">
              {veiculo.marca} {veiculo.modelo} • {veiculo.ano}
            </p>
          </div>
        </div>

        <div className="flex gap-2">
          <Link
            to={`/veiculos/${id}/editar`}
            className="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50"
          >
            <Edit className="h-5 w-5" />
            Editar
          </Link>
          <button
            onClick={handleDelete}
            disabled={deleteMutation.isPending}
            className="flex items-center gap-2 rounded-lg border border-red-600 px-4 py-2 text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
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
          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900">
              <Car className="h-5 w-5" />
              Dados do Veículo
            </h2>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="text-sm font-medium text-gray-500">Placa</label>
                <p className="mt-1 text-lg font-semibold text-gray-900">{veiculo.placa}</p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500">Chassi</label>
                <p className="mt-1 font-mono text-sm text-gray-900">
                  {veiculo.chassi || '-'}
                </p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500">Marca</label>
                <p className="mt-1 text-gray-900">{veiculo.marca}</p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500">Modelo</label>
                <p className="mt-1 text-gray-900">{veiculo.modelo}</p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500">Ano</label>
                <p className="mt-1 flex items-center gap-2 text-gray-900">
                  <Calendar className="h-4 w-4 text-gray-400" />
                  {veiculo.ano}
                </p>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-500">Cor</label>
                <p className="mt-1 text-gray-900">{veiculo.cor || '-'}</p>
              </div>

              <div className="md:col-span-2">
                <label className="text-sm font-medium text-gray-500">Quilometragem</label>
                <p className="mt-1 flex items-center gap-2 text-gray-900">
                  <Gauge className="h-4 w-4 text-gray-400" />
                  {veiculo.quilometragem
                    ? `${veiculo.quilometragem.toLocaleString('pt-BR')} km`
                    : 'Não informada'}
                </p>
              </div>
            </div>
          </div>

          {/* Proprietário */}
          {veiculo.cliente && (
            <div className="rounded-lg bg-white p-6 shadow">
              <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900">
                <User className="h-5 w-5" />
                Proprietário
              </h2>

              <div className="space-y-3">
                <div>
                  <label className="text-sm font-medium text-gray-500">Nome</label>
                  <p className="mt-1 text-gray-900">{veiculo.cliente.nome}</p>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-500">CPF/CNPJ</label>
                  <p className="mt-1 text-gray-900">{veiculo.cliente.cpfCnpj}</p>
                </div>

                {veiculo.cliente.telefone && (
                  <div>
                    <label className="text-sm font-medium text-gray-500">Telefone</label>
                    <p className="mt-1 text-gray-900">
                      <a
                        href={`tel:${veiculo.cliente.telefone}`}
                        className="text-blue-600 hover:underline"
                      >
                        {veiculo.cliente.telefone}
                      </a>
                    </p>
                  </div>
                )}

                <div className="mt-4 pt-4 border-t border-gray-200">
                  <Link
                    to={`/clientes/${veiculo.clienteId}`}
                    className="text-sm text-blue-600 hover:underline"
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
          <div className="rounded-lg bg-white p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700">Informações</h3>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-medium text-gray-500">Cadastrado em</label>
                <p className="mt-1 text-sm text-gray-900">{formatDate(veiculo.createdAt)}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500">Última atualização</label>
                <p className="mt-1 text-sm text-gray-900">{formatDate(veiculo.updatedAt)}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500">ID</label>
                <p className="mt-1 text-xs font-mono text-gray-600">{veiculo.id}</p>
              </div>
            </div>
          </div>

          {/* Ações Rápidas */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700">Ações Rápidas</h3>

            <div className="space-y-2">
              <button
                onClick={() => navigate(`/ordens-servico/novo?veiculo=${id}`)}
                className="w-full rounded-lg border border-green-600 px-4 py-2 text-sm font-medium text-green-600 hover:bg-green-50"
              >
                Nova Ordem de Serviço
              </button>

              <Link
                to={`/ordens-servico?veiculo=${id}`}
                className="block w-full rounded-lg border border-gray-300 px-4 py-2 text-center text-sm font-medium text-gray-700 hover:bg-gray-50"
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
