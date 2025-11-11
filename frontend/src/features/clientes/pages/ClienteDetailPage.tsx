import { useNavigate, useParams, Link } from 'react-router-dom';
import { useCliente, useDeleteCliente, useReativarCliente } from '../hooks/useClientes';
import { ArrowLeft, Edit, Trash2, RotateCw, Mail, Phone, MapPin } from 'lucide-react';
import { showError } from '@/shared/utils/notifications';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

export const ClienteDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  const { data: cliente, isLoading, error } = useCliente(id);
  const deleteMutation = useDeleteCliente();
  const reativarMutation = useReativarCliente();

  const handleDelete = async () => {
    if (window.confirm('Tem certeza que deseja desativar este cliente?')) {
      try {
        await deleteMutation.mutateAsync(id!);
        navigate('/clientes');
      } catch (error) {
        showError('Erro ao desativar cliente');
      }
    }
  };

  const handleReativar = async () => {
    try {
      await reativarMutation.mutateAsync(id!);
    } catch (error) {
      showError('Erro ao reativar cliente');
    }
  };

  const formatDate = (date: string | number[]) => {
    if (Array.isArray(date)) {
      // Convert Java LocalDateTime array to Date
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

  if (error || !cliente) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 bg-red-900/20 p-4 text-red-400">
          Cliente não encontrado
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
            onClick={() => navigate('/clientes')}
            className="rounded-lg p-2 hover:bg-gray-100"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{cliente.nome}</h1>
            <p className="mt-1 text-sm text-gray-600">
              {cliente.tipo === 'PESSOA_FISICA' ? 'Pessoa Física' : 'Pessoa Jurídica'} •{' '}
              {cliente.cpfCnpj}
            </p>
          </div>
        </div>

        <div className="flex gap-2">
          <Link
            to={`/clientes/${id}/editar`}
            className="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50"
          >
            <Edit className="h-5 w-5" />
            Editar
          </Link>
          {cliente.ativo ? (
            <button
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
              className="flex items-center gap-2 rounded-lg border border-red-600 px-4 py-2 text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Trash2 className="h-5 w-5" />
              Desativar
            </button>
          ) : (
            <button
              onClick={handleReativar}
              disabled={reativarMutation.isPending}
              className="flex items-center gap-2 rounded-lg border border-green-600 px-4 py-2 text-green-600 hover:bg-green-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RotateCw className="h-5 w-5" />
              Reativar
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Dados Básicos */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900">Dados Básicos</h2>

            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-gray-500">Nome</label>
                <p className="mt-1 text-gray-900">{cliente.nome}</p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-500">
                    {cliente.tipo === 'PESSOA_FISICA' ? 'CPF' : 'CNPJ'}
                  </label>
                  <p className="mt-1 text-gray-900">{cliente.cpfCnpj}</p>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-500">Tipo</label>
                  <p className="mt-1 text-gray-900">
                    {cliente.tipo === 'PESSOA_FISICA' ? 'Pessoa Física' : 'Pessoa Jurídica'}
                  </p>
                </div>
              </div>

              {cliente.email && (
                <div className="flex items-center gap-2">
                  <Mail className="h-5 w-5 text-gray-400" />
                  <a
                    href={`mailto:${cliente.email}`}
                    className="text-blue-600 hover:underline"
                  >
                    {cliente.email}
                  </a>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4">
                {cliente.telefone && (
                  <div className="flex items-center gap-2">
                    <Phone className="h-5 w-5 text-gray-400" />
                    <a href={`tel:${cliente.telefone}`} className="text-gray-900">
                      {cliente.telefone}
                    </a>
                  </div>
                )}

                {cliente.celular && (
                  <div className="flex items-center gap-2">
                    <Phone className="h-5 w-5 text-gray-400" />
                    <a href={`tel:${cliente.celular}`} className="text-gray-900">
                      {cliente.celular}
                    </a>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Endereço */}
          {cliente.endereco && (
            <div className="rounded-lg bg-white p-6 shadow">
              <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900">
                <MapPin className="h-5 w-5" />
                Endereço
              </h2>

              <div className="space-y-2">
                <p className="text-gray-900">
                  {cliente.endereco.logradouro}
                  {cliente.endereco.numero && `, ${cliente.endereco.numero}`}
                </p>
                {cliente.endereco.complemento && (
                  <p className="text-gray-600">{cliente.endereco.complemento}</p>
                )}
                <p className="text-gray-900">
                  {cliente.endereco.bairro && `${cliente.endereco.bairro} - `}
                  {cliente.endereco.cidade}/{cliente.endereco.estado}
                </p>
                {cliente.endereco.cep && (
                  <p className="text-gray-600">CEP: {cliente.endereco.cep}</p>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Status */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700">Status</h3>
            {cliente.ativo ? (
              <span className="inline-flex rounded-full bg-green-100 px-3 py-1 text-sm font-semibold text-green-800">
                Ativo
              </span>
            ) : (
              <span className="inline-flex rounded-full bg-gray-100 px-3 py-1 text-sm font-semibold text-gray-800">
                Inativo
              </span>
            )}
          </div>

          {/* Metadata */}
          <div className="rounded-lg bg-white p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700">Informações</h3>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-medium text-gray-500">Cadastrado em</label>
                <p className="mt-1 text-sm text-gray-900">{formatDate(cliente.createdAt)}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500">Última atualização</label>
                <p className="mt-1 text-sm text-gray-900">{formatDate(cliente.updatedAt)}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500">ID</label>
                <p className="mt-1 text-xs font-mono text-gray-600">{cliente.id}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
