/**
 * Oficina Detail Page - View and manage a single workshop
 */

import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Building2,
  Mail,
  Phone,
  MapPin,
  Users,
  Car,
  FileText,
  Package,
  DollarSign,
  Calendar,
  Clock,
  Play,
  Pause,
  Ban,
  Edit,
  TrendingUp,
  RefreshCw,
  CreditCard,
} from 'lucide-react';
import { useOficinaDetail, useActivateOficina, useSuspendOficina, useCancelOficina } from '../hooks/useSaas';
import { ImpersonateButton } from '../components/ImpersonateButton';
import { AlterarPlanoModal } from '../components/AlterarPlanoModal';
import { formatCurrency, formatDate, formatNumber } from '@/shared/utils/formatters';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { StatusOficina, PlanoAssinatura, statusLabels, planoLabels } from '../types';
import { Modal } from '@/shared/components/ui/Modal';

export const OficinaDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: oficina, isLoading, error } = useOficinaDetail(id);

  const [confirmModal, setConfirmModal] = useState<{
    open: boolean;
    type: 'activate' | 'suspend' | 'cancel';
  }>({ open: false, type: 'activate' });

  const [planoModalOpen, setPlanoModalOpen] = useState(false);

  const activateMutation = useActivateOficina();
  const suspendMutation = useSuspendOficina();
  const cancelMutation = useCancelOficina();

  const handleAction = async (type: 'activate' | 'suspend' | 'cancel') => {
    if (!id) return;

    try {
      switch (type) {
        case 'activate':
          await activateMutation.mutateAsync(id);
          showSuccess('Oficina ativada com sucesso!');
          break;
        case 'suspend':
          await suspendMutation.mutateAsync(id);
          showSuccess('Oficina suspensa com sucesso!');
          break;
        case 'cancel':
          await cancelMutation.mutateAsync(id);
          showSuccess('Oficina cancelada com sucesso!');
          navigate('/admin/oficinas');
          break;
      }
      setConfirmModal({ open: false, type: 'activate' });
    } catch {
      showError('Erro ao executar ação');
    }
  };

  const getStatusBadge = (status: StatusOficina) => {
    const colors: Record<StatusOficina, string> = {
      ATIVA: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
      TRIAL: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
      SUSPENSA: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
      CANCELADA: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
      INATIVA: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
    };

    return (
      <span className={`inline-flex rounded-full px-3 py-1 text-sm font-medium ${colors[status]}`}>
        {statusLabels[status]}
      </span>
    );
  };

  if (isLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
      </div>
    );
  }

  if (error || !oficina) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-300 bg-red-50 p-4 text-red-800 dark:border-red-800 dark:bg-red-900/20 dark:text-red-400">
          Erro ao carregar oficina. Tente novamente.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6">
        <Link
          to="/admin/oficinas"
          className="mb-4 inline-flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white"
        >
          <ArrowLeft className="h-4 w-4" />
          Voltar para lista
        </Link>

        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                {oficina.nomeFantasia}
              </h1>
              {getStatusBadge(oficina.status)}
            </div>
            <p className="mt-1 text-gray-600 dark:text-gray-400">{oficina.razaoSocial}</p>
            <p className="text-sm text-gray-500 dark:text-gray-500">CNPJ: {oficina.cnpjCpf}</p>
          </div>

          <div className="flex gap-2">
            <ImpersonateButton
              oficinaId={id!}
              oficinaNome={oficina.nomeFantasia}
              disabled={oficina.status === 'CANCELADA'}
            />
            <Link
              to={`/admin/oficinas/${id}/editar`}
              className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              <Edit className="h-4 w-4" />
              Editar
            </Link>
            {oficina.status === 'SUSPENSA' && (
              <button
                onClick={() => setConfirmModal({ open: true, type: 'activate' })}
                className="flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-white hover:bg-green-700"
              >
                <Play className="h-4 w-4" />
                Ativar
              </button>
            )}
            {(oficina.status === 'ATIVA' || oficina.status === 'TRIAL') && (
              <button
                onClick={() => setConfirmModal({ open: true, type: 'suspend' })}
                className="flex items-center gap-2 rounded-lg bg-yellow-600 px-4 py-2 text-white hover:bg-yellow-700"
              >
                <Pause className="h-4 w-4" />
                Suspender
              </button>
            )}
            {oficina.status !== 'CANCELADA' && (
              <button
                onClick={() => setConfirmModal({ open: true, type: 'cancel' })}
                className="flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-white hover:bg-red-700"
              >
                <Ban className="h-4 w-4" />
                Cancelar
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-indigo-100 p-2 dark:bg-indigo-900/30">
              <Users className="h-5 w-5 text-indigo-600 dark:text-indigo-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Usuários</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {formatNumber(oficina.totalUsuarios)}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-blue-100 p-2 dark:bg-blue-900/30">
              <Users className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Clientes</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {formatNumber(oficina.totalClientes)}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-cyan-100 p-2 dark:bg-cyan-900/30">
              <Car className="h-5 w-5 text-cyan-600 dark:text-cyan-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Veículos</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {formatNumber(oficina.totalVeiculos)}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-orange-100 p-2 dark:bg-orange-900/30">
              <FileText className="h-5 w-5 text-orange-600 dark:text-orange-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">OS</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {formatNumber(oficina.totalOrdensServico)}
              </p>
            </div>
          </div>
        </div>

        <div className="rounded-lg bg-white p-4 shadow dark:bg-gray-800">
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-purple-100 p-2 dark:bg-purple-900/30">
              <Package className="h-5 w-5 text-purple-600 dark:text-purple-400" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Peças</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {formatNumber(oficina.totalPecas)}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content Grid */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Left Column - Info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Contact Info */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
              Informações de Contato
            </h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="flex items-center gap-3">
                <Mail className="h-5 w-5 text-gray-400" />
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Email</p>
                  <p className="text-gray-900 dark:text-white">{oficina.email}</p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Phone className="h-5 w-5 text-gray-400" />
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Telefone</p>
                  <p className="text-gray-900 dark:text-white">{oficina.telefone || '-'}</p>
                </div>
              </div>
              {oficina.nomeResponsavel && (
                <div className="flex items-center gap-3">
                  <Users className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Responsável</p>
                    <p className="text-gray-900 dark:text-white">{oficina.nomeResponsavel}</p>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Address */}
          {oficina.logradouro && (
            <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
              <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-white">
                <MapPin className="h-5 w-5" />
                Endereço
              </h2>
              <div className="text-gray-700 dark:text-gray-300">
                <p>
                  {oficina.logradouro}, {oficina.numero}
                  {oficina.complemento && ` - ${oficina.complemento}`}
                </p>
                <p>
                  {oficina.bairro} - {oficina.cidade}/{oficina.estado}
                </p>
                <p>CEP: {oficina.cep}</p>
              </div>
            </div>
          )}

          {/* Financial Stats */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-white">
              <TrendingUp className="h-5 w-5" />
              Estatísticas Financeiras
            </h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="rounded-lg bg-green-50 p-4 dark:bg-green-900/20">
                <p className="text-sm text-green-700 dark:text-green-400">Faturamento Total</p>
                <p className="text-2xl font-bold text-green-800 dark:text-green-300">
                  {formatCurrency(oficina.totalFaturamento)}
                </p>
              </div>
              <div className="rounded-lg bg-blue-50 p-4 dark:bg-blue-900/20">
                <p className="text-sm text-blue-700 dark:text-blue-400">Pagamentos Realizados</p>
                <p className="text-2xl font-bold text-blue-800 dark:text-blue-300">
                  {formatNumber(oficina.pagamentosRealizados)}
                </p>
              </div>
              <div className="rounded-lg bg-yellow-50 p-4 dark:bg-yellow-900/20">
                <p className="text-sm text-yellow-700 dark:text-yellow-400">Pagamentos Pendentes</p>
                <p className="text-2xl font-bold text-yellow-800 dark:text-yellow-300">
                  {formatNumber(oficina.pagamentosPendentes)}
                </p>
              </div>
              {oficina.ultimoPagamento && (
                <div className="rounded-lg bg-gray-50 p-4 dark:bg-gray-700">
                  <p className="text-sm text-gray-600 dark:text-gray-400">Último Pagamento</p>
                  <p className="text-lg font-semibold text-gray-900 dark:text-white">
                    {formatDate(oficina.ultimoPagamento)}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column - Subscription */}
        <div className="space-y-6">
          {/* Plan Info */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-lg font-semibold text-gray-900 dark:text-white">
                <Building2 className="h-5 w-5" />
                Plano de Assinatura
              </h2>
              {oficina.status !== 'CANCELADA' && (
                <button
                  onClick={() => setPlanoModalOpen(true)}
                  className="flex items-center gap-1 rounded-lg border border-blue-500 px-3 py-1.5 text-sm text-blue-600 hover:bg-blue-50 dark:border-blue-400 dark:text-blue-400 dark:hover:bg-blue-900/20"
                >
                  <CreditCard className="h-4 w-4" />
                  Alterar
                </button>
              )}
            </div>
            <div className="space-y-4">
              <div className="rounded-lg bg-gradient-to-br from-blue-500 to-blue-600 p-4 text-white">
                <p className="text-sm text-blue-100">Plano Atual</p>
                <p className="text-2xl font-bold">{planoLabels[oficina.plano as PlanoAssinatura] || oficina.plano}</p>
                <p className="mt-2 text-3xl font-bold">
                  {formatCurrency(oficina.valorMensalidade)}
                  <span className="text-sm font-normal">/mês</span>
                </p>
              </div>

              <div className="flex items-center gap-3">
                <Calendar className="h-5 w-5 text-gray-400" />
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Vencimento</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {formatDate(oficina.dataVencimentoPlano)}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <Clock className="h-5 w-5 text-gray-400" />
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Cliente desde</p>
                  <p className="font-medium text-gray-900 dark:text-white">
                    {formatDate(oficina.createdAt)}
                  </p>
                </div>
              </div>

              {oficina.status === 'TRIAL' && oficina.dataFimTrial && (
                <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 dark:border-blue-800 dark:bg-blue-900/20">
                  <p className="text-sm font-medium text-blue-800 dark:text-blue-300">
                    Período Trial
                  </p>
                  <p className="text-blue-700 dark:text-blue-400">
                    Termina em {formatDate(oficina.dataFimTrial)}
                  </p>
                  {oficina.diasRestantesTrial !== undefined && (
                    <p className="mt-1 text-lg font-bold text-blue-800 dark:text-blue-300">
                      {oficina.diasRestantesTrial} dias restantes
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Quick Actions */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
              Ações Rápidas
            </h2>
            <div className="space-y-2">
              <Link
                to={`/admin/pagamentos?oficinaId=${id}`}
                className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600"
              >
                <DollarSign className="h-4 w-4" />
                Ver Pagamentos
              </Link>
              <Link
                to={`/admin/audit?entity=Oficina&entityId=${id}`}
                className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-300 dark:hover:bg-gray-600"
              >
                <FileText className="h-4 w-4" />
                Ver Logs de Auditoria
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Confirmation Modal */}
      <Modal
        isOpen={confirmModal.open}
        onClose={() => setConfirmModal({ open: false, type: 'activate' })}
        title={
          confirmModal.type === 'activate'
            ? 'Ativar Oficina'
            : confirmModal.type === 'suspend'
            ? 'Suspender Oficina'
            : 'Cancelar Oficina'
        }
      >
        <div className="space-y-4">
          <p className="text-gray-700 dark:text-gray-300">
            {confirmModal.type === 'activate' && (
              <>
                Deseja ativar a oficina <strong>{oficina.nomeFantasia}</strong>?
                Isso permitirá que os usuários acessem o sistema novamente.
              </>
            )}
            {confirmModal.type === 'suspend' && (
              <>
                Deseja suspender a oficina <strong>{oficina.nomeFantasia}</strong>?
                Os usuários não poderão acessar o sistema.
              </>
            )}
            {confirmModal.type === 'cancel' && (
              <>
                Deseja cancelar a oficina <strong>{oficina.nomeFantasia}</strong>?
                <span className="block mt-2 text-red-600 dark:text-red-400 font-medium">
                  Esta ação é irreversível!
                </span>
              </>
            )}
          </p>
          <div className="flex justify-end gap-3">
            <button
              onClick={() => setConfirmModal({ open: false, type: 'activate' })}
              className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              onClick={() => handleAction(confirmModal.type)}
              disabled={activateMutation.isPending || suspendMutation.isPending || cancelMutation.isPending}
              className={`rounded-lg px-4 py-2 text-white ${
                confirmModal.type === 'activate'
                  ? 'bg-green-600 hover:bg-green-700'
                  : confirmModal.type === 'suspend'
                  ? 'bg-yellow-600 hover:bg-yellow-700'
                  : 'bg-red-600 hover:bg-red-700'
              }`}
            >
              Confirmar
            </button>
          </div>
        </div>
      </Modal>

      {/* Alterar Plano Modal */}
      <AlterarPlanoModal
        isOpen={planoModalOpen}
        onClose={() => setPlanoModalOpen(false)}
        oficinaId={id!}
        oficinaNome={oficina.nomeFantasia}
        planoAtual={oficina.plano as PlanoAssinatura}
      />
    </div>
  );
};
