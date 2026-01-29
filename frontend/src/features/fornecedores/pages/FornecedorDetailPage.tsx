import { useNavigate, useParams, Link } from 'react-router-dom';
import { useFornecedor, useDeleteFornecedor, useReativarFornecedor } from '../hooks/useFornecedores';
import { ArrowLeft, Edit, Trash2, RotateCw, Mail, Phone, MapPin, Globe, Building2, FileText } from 'lucide-react';
import { showError } from '@/shared/utils/notifications';
import { TipoFornecedorLabel } from '../types';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

const formatWhatsAppLink = (phone: string): string => {
  const digits = phone.replace(/\D/g, '');
  const phoneWithCountry = digits.startsWith('55') ? digits : `55${digits}`;
  return `https://wa.me/${phoneWithCountry}`;
};

export const FornecedorDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  const { data: fornecedor, isLoading, error } = useFornecedor(id);
  const deleteMutation = useDeleteFornecedor();
  const reativarMutation = useReativarFornecedor();

  const handleDelete = async () => {
    if (window.confirm('Tem certeza que deseja desativar este fornecedor?')) {
      try {
        await deleteMutation.mutateAsync(id!);
        navigate('/fornecedores');
      } catch (error) {
        showError('Erro ao desativar fornecedor');
      }
    }
  };

  const handleReativar = async () => {
    try {
      await reativarMutation.mutateAsync(id!);
    } catch (error) {
      showError('Erro ao reativar fornecedor');
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

  if (error || !fornecedor) {
    return (
      <div className="p-4 sm:p-6">
        <div className="rounded-lg border border-red-800 dark:border-red-700 bg-red-900/20 dark:bg-red-900/30 p-4 text-red-400 dark:text-red-300">
          Fornecedor não encontrado
        </div>
      </div>
    );
  }

  const hasEndereco = fornecedor.endereco && (
    fornecedor.endereco.logradouro ||
    fornecedor.endereco.cidade ||
    fornecedor.endereco.estado
  );

  const hasComercial = fornecedor.prazoEntrega || fornecedor.condicoesPagamento || fornecedor.descontoPadrao != null;

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-4 sm:mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3 sm:gap-4">
          <button
            onClick={() => navigate('/fornecedores')}
            className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <ArrowLeft className="h-5 w-5 text-gray-900 dark:text-gray-100" />
          </button>
          <div className="min-w-0 flex-1">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100 truncate">
              {fornecedor.nomeFantasia}
            </h1>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              {TipoFornecedorLabel[fornecedor.tipo]}
              {fornecedor.cpfCnpj && ` • ${fornecedor.cpfCnpj}`}
            </p>
          </div>
        </div>

        <div className="flex gap-2">
          <Link
            to={`/fornecedores/${id}/editar`}
            className="flex flex-1 sm:flex-none items-center justify-center gap-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 sm:px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
          >
            <Edit className="h-4 w-4 sm:h-5 sm:w-5" />
            <span className="hidden sm:inline">Editar</span>
          </Link>
          {fornecedor.ativo ? (
            <button
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
              className="flex flex-1 sm:flex-none items-center justify-center gap-2 rounded-lg border border-red-600 dark:border-red-700 bg-white dark:bg-gray-700 px-3 sm:px-4 py-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Trash2 className="h-4 w-4 sm:h-5 sm:w-5" />
              <span className="hidden sm:inline">Desativar</span>
            </button>
          ) : (
            <button
              onClick={handleReativar}
              disabled={reativarMutation.isPending}
              className="flex flex-1 sm:flex-none items-center justify-center gap-2 rounded-lg border border-green-600 dark:border-green-700 bg-white dark:bg-gray-700 px-3 sm:px-4 py-2 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RotateCw className="h-4 w-4 sm:h-5 sm:w-5" />
              <span className="hidden sm:inline">Reativar</span>
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="grid gap-4 sm:gap-6 lg:grid-cols-3">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-4 sm:space-y-6">
          {/* Dados Básicos */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-gray-100">
              <Building2 className="h-5 w-5" />
              Dados do Fornecedor
            </h2>

            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Nome Fantasia</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100 break-words">{fornecedor.nomeFantasia}</p>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Tipo</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">
                    {TipoFornecedorLabel[fornecedor.tipo]}
                  </p>
                </div>
              </div>

              {fornecedor.razaoSocial && (
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Razão Social</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100 break-words">{fornecedor.razaoSocial}</p>
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {fornecedor.cpfCnpj && (
                  <div>
                    <label className="text-sm font-medium text-gray-500 dark:text-gray-400">CPF/CNPJ</label>
                    <p className="mt-1 text-gray-900 dark:text-gray-100">{fornecedor.cpfCnpj}</p>
                  </div>
                )}

                {fornecedor.inscricaoEstadual && (
                  <div>
                    <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Inscrição Estadual</label>
                    <p className="mt-1 text-gray-900 dark:text-gray-100">{fornecedor.inscricaoEstadual}</p>
                  </div>
                )}
              </div>

              {/* Contact info */}
              {fornecedor.contatoNome && (
                <div>
                  <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Pessoa de Contato</label>
                  <p className="mt-1 text-gray-900 dark:text-gray-100">{fornecedor.contatoNome}</p>
                </div>
              )}

              {fornecedor.email && (
                <div className="flex items-center gap-2">
                  <Mail className="h-5 w-5 text-gray-400 dark:text-gray-500" />
                  <a
                    href={`mailto:${fornecedor.email}`}
                    className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 hover:underline"
                    title="Enviar email"
                  >
                    {fornecedor.email}
                  </a>
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {fornecedor.telefone && (
                  <div className="flex items-center gap-2">
                    <Phone className="h-5 w-5 flex-shrink-0 text-gray-400 dark:text-gray-500" />
                    <a
                      href={formatWhatsAppLink(fornecedor.telefone)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 hover:underline truncate"
                      title="Abrir WhatsApp"
                    >
                      {fornecedor.telefone}
                    </a>
                  </div>
                )}

                {fornecedor.celular && (
                  <div className="flex items-center gap-2">
                    <Phone className="h-5 w-5 flex-shrink-0 text-gray-400 dark:text-gray-500" />
                    <a
                      href={formatWhatsAppLink(fornecedor.celular)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 hover:underline truncate"
                      title="Abrir WhatsApp"
                    >
                      {fornecedor.celular}
                    </a>
                  </div>
                )}
              </div>

              {fornecedor.website && (
                <div className="flex items-center gap-2">
                  <Globe className="h-5 w-5 text-gray-400 dark:text-gray-500" />
                  <a
                    href={fornecedor.website.startsWith('http') ? fornecedor.website : `https://${fornecedor.website}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 hover:underline truncate"
                    title="Abrir website"
                  >
                    {fornecedor.website}
                  </a>
                </div>
              )}
            </div>
          </div>

          {/* Endereço */}
          {hasEndereco && (
            <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
              <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-gray-100">
                <MapPin className="h-5 w-5" />
                Endereço
              </h2>

              <div className="space-y-2">
                {fornecedor.endereco!.logradouro && (
                  <p className="text-gray-900 dark:text-gray-100">
                    {fornecedor.endereco!.logradouro}
                    {fornecedor.endereco!.numero && `, ${fornecedor.endereco!.numero}`}
                  </p>
                )}
                {fornecedor.endereco!.complemento && (
                  <p className="text-gray-600 dark:text-gray-400">{fornecedor.endereco!.complemento}</p>
                )}
                <p className="text-gray-900 dark:text-gray-100">
                  {fornecedor.endereco!.bairro && `${fornecedor.endereco!.bairro} - `}
                  {fornecedor.endereco!.cidade}
                  {fornecedor.endereco!.estado && `/${fornecedor.endereco!.estado}`}
                </p>
                {fornecedor.endereco!.cep && (
                  <p className="text-gray-600 dark:text-gray-400">CEP: {fornecedor.endereco!.cep}</p>
                )}
              </div>
            </div>
          )}

          {/* Dados Comerciais */}
          {hasComercial && (
            <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
              <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-gray-100">
                <FileText className="h-5 w-5" />
                Dados Comerciais
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {fornecedor.prazoEntrega && (
                  <div>
                    <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Prazo de Entrega</label>
                    <p className="mt-1 text-gray-900 dark:text-gray-100">{fornecedor.prazoEntrega}</p>
                  </div>
                )}

                {fornecedor.condicoesPagamento && (
                  <div>
                    <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Condições de Pagamento</label>
                    <p className="mt-1 text-gray-900 dark:text-gray-100">{fornecedor.condicoesPagamento}</p>
                  </div>
                )}

                {fornecedor.descontoPadrao != null && (
                  <div>
                    <label className="text-sm font-medium text-gray-500 dark:text-gray-400">Desconto Padrão</label>
                    <p className="mt-1 text-gray-900 dark:text-gray-100">{fornecedor.descontoPadrao}%</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Observações */}
          {fornecedor.observacoes && (
            <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
              <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Observações</h2>
              <p className="text-gray-900 dark:text-gray-100 whitespace-pre-wrap">{fornecedor.observacoes}</p>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-4 sm:space-y-6">
          {/* Status */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Status</h3>
            {fornecedor.ativo ? (
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
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <h3 className="mb-4 text-sm font-semibold text-gray-700 dark:text-gray-300">Informações</h3>

            <div className="space-y-3">
              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">Cadastrado em</label>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{formatDate(fornecedor.createdAt)}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">Última atualização</label>
                <p className="mt-1 text-sm text-gray-900 dark:text-gray-100">{formatDate(fornecedor.updatedAt)}</p>
              </div>

              <div>
                <label className="text-xs font-medium text-gray-500 dark:text-gray-400">ID</label>
                <p className="mt-1 text-xs font-mono text-gray-600 dark:text-gray-400">{fornecedor.id}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
