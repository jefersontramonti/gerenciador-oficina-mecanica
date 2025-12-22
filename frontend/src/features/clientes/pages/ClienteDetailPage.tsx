import { useNavigate, useParams, Link } from 'react-router-dom';
import { useCliente, useDeleteCliente, useReativarCliente } from '../hooks/useClientes';
import { ArrowLeft, Edit, Trash2, RotateCw, Mail, Phone, MapPin } from 'lucide-react';
import { showError } from '@/shared/utils/notifications';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

/**
 * Formata telefone para WhatsApp (remove caracteres especiais e adiciona código do país)
 */
const formatWhatsAppLink = (phone: string): string => {
  // Remove tudo que não é número
  const digits = phone.replace(/\D/g, '');

  // Se já tem código do país (55), usa direto
  // Se não tem, adiciona 55 (Brasil)
  const phoneWithCountry = digits.startsWith('55') ? digits : `55${digits}`;

  return `https://wa.me/${phoneWithCountry}`;
};

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
        <div className="text-gray-500 dark:text-gray-400">Carregando...</div>
      </div>
    );
  }

  if (error || !cliente) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
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
            className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <ArrowLeft className="h-5 w-5 text-gray-900 dark:text-gray-100" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{cliente.nome}</h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              {cliente.tipo === 'PESSOA_FISICA' ? 'Pessoa Física' : 'Pessoa Jurídica'} •{' '}
              {cliente.cpfCnpj}
            </p>
          </div>
        </div>

        <div className="flex gap-2">
          <Link
            to={`/clientes/${id}/editar`}
            className="flex items-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
          >
            <Edit className="h-5 w-5" />
            Editar
          </Link>
          {cliente.ativo ? (
            <button
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
              className="flex items-center gap-2 rounded-lg border border-red-600 dark:border-red-700 bg-white dark:bg-gray-700 px-4 py-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Trash2 className="h-5 w-5" />
              Desativar
            </button>
          ) : (
            <button
              onClick={handleReativar}
              disabled={reativarMutation.isPending}
              className="flex items-center gap-2 rounded-lg border border-green-600 dark:border-green-700 bg-white dark:bg-gray-700 px-4 py-2 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30 disabled:cursor-not-allowed disabled:opacity-50"
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
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Dados Básicos</h2>

            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Nome</label>
                <p className="mt-1 text-gray-900 dark:text-gray-100">{cliente.nome}</p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">
                    {cliente.tipo === 'PESSOA_FISICA' ? 'CPF' : 'CNPJ'}
                  </label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">{cliente.cpfCnpj}</p>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Tipo</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">
                    {cliente.tipo === 'PESSOA_FISICA' ? 'Pessoa Física' : 'Pessoa Jurídica'}
                  </p>
                </div>
              </div>

              {cliente.email && (
                <div className="flex items-center gap-2">
                  <Mail className="h-5 w-5 text-gray-400 dark:text-gray-500" />
                  <a
                    href={`mailto:${cliente.email}`}
                    className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 hover:underline"
                    title="Enviar email"
                  >
                    {cliente.email}
                  </a>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4">
                {cliente.telefone && (
                  <div className="flex items-center gap-2">
                    <Phone className="h-5 w-5 text-gray-400 dark:text-gray-500" />
                    <a
                      href={formatWhatsAppLink(cliente.telefone)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 hover:underline"
                      title="Abrir WhatsApp"
                    >
                      {cliente.telefone}
                    </a>
                  </div>
                )}

                {cliente.celular && (
                  <div className="flex items-center gap-2">
                    <Phone className="h-5 w-5 text-gray-400 dark:text-gray-500" />
                    <a
                      href={formatWhatsAppLink(cliente.celular)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 hover:underline"
                      title="Abrir WhatsApp"
                    >
                      {cliente.celular}
                    </a>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Endereço */}
          {cliente.endereco && (
            <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
              <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-gray-100">
                <MapPin className="h-5 w-5" />
                Endereço
              </h2>

              <div className="space-y-2">
                <p className="text-gray-900 dark:text-gray-100">
                  {cliente.endereco.logradouro}
                  {cliente.endereco.numero && `, ${cliente.endereco.numero}`}
                </p>
                {cliente.endereco.complemento && (
                  <p className="text-gray-600 dark:text-gray-400">{cliente.endereco.complemento}</p>
                )}
                <p className="text-gray-900 dark:text-gray-100">
                  {cliente.endereco.bairro && `${cliente.endereco.bairro} - `}
                  {cliente.endereco.cidade}/{cliente.endereco.estado}
                </p>
                {cliente.endereco.cep && (
                  <p className="text-gray-600 dark:text-gray-400">CEP: {cliente.endereco.cep}</p>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Status */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Status</h3>
            {cliente.ativo ? (
              <span className="inline-flex rounded-full bg-green-100 dark:bg-green-900/30 px-3 py-1 text-sm font-semibold text-green-800 dark:text-green-400">
                Ativo
              </span>
            ) : (
              <span className="inline-flex rounded-full bg-gray-100 dark:bg-gray-700 px-3 py-1 text-sm font-semibold text-gray-800 dark:text-gray-300">
                Inativo
              </span>
            )}
          </div>

          {/* Metadata */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Informações</h3>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">Cadastrado em</label>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{formatDate(cliente.createdAt)}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">Última atualização</label>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{formatDate(cliente.updatedAt)}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">ID</label>
                <p className="mt-1 text-xs font-mono text-gray-600 dark:text-gray-400">{cliente.id}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
