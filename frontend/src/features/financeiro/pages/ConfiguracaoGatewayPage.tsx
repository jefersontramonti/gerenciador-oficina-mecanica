/**
 * Página de Configuração de Gateways de Pagamento
 */

import { useState } from 'react';
import { CreditCard, Settings, CheckCircle, XCircle, AlertTriangle, Shield, Trash2 } from 'lucide-react';
import { useGateways, useCriarGateway, useAtualizarGateway, useValidarGateway, useAtivarGateway, useDesativarGateway, useRemoverGateway } from '../hooks/usePagamentoOnline';
import { TipoGateway, TipoGatewayLabels, AmbienteGateway, AmbienteGatewayLabels, type ConfiguracaoGateway, type ConfiguracaoGatewayRequest } from '../types/pagamentoOnline';
import { showSuccess, showError } from '@/shared/utils/notifications';

export function ConfiguracaoGatewayPage() {
  const { data: gateways, isLoading } = useGateways();
  const [editingGateway, setEditingGateway] = useState<ConfiguracaoGateway | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState<ConfiguracaoGatewayRequest>({
    tipoGateway: TipoGateway.MERCADO_PAGO,
    ambiente: AmbienteGateway.SANDBOX,
  });

  const criarMutation = useCriarGateway();
  const atualizarMutation = useAtualizarGateway();
  const validarMutation = useValidarGateway();
  const ativarMutation = useAtivarGateway();
  const desativarMutation = useDesativarGateway();
  const removerMutation = useRemoverGateway();

  const handleOpenForm = (gateway?: ConfiguracaoGateway) => {
    if (gateway) {
      setEditingGateway(gateway);
      setFormData({
        tipoGateway: gateway.tipoGateway,
        ambiente: gateway.ambiente,
        ativo: gateway.ativo,
        padrao: gateway.padrao,
        taxaPercentual: gateway.taxaPercentual,
        taxaFixa: gateway.taxaFixa,
        observacoes: gateway.observacoes,
      });
    } else {
      setEditingGateway(null);
      setFormData({
        tipoGateway: TipoGateway.MERCADO_PAGO,
        ambiente: AmbienteGateway.SANDBOX,
      });
    }
    setShowForm(true);
  };

  const handleSave = async () => {
    try {
      if (editingGateway) {
        await atualizarMutation.mutateAsync({ id: editingGateway.id, data: formData });
        showSuccess('Gateway atualizado com sucesso!');
      } else {
        await criarMutation.mutateAsync(formData);
        showSuccess('Gateway configurado com sucesso!');
      }
      setShowForm(false);
      setEditingGateway(null);
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao salvar configuração');
    }
  };

  const handleValidar = async (id: string) => {
    try {
      const result = await validarMutation.mutateAsync(id);
      if (result.statusValidacao === 'VALIDO') {
        showSuccess('Credenciais válidas!');
      } else {
        showError('Credenciais inválidas. Verifique o Access Token.');
      }
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao validar credenciais');
    }
  };

  const handleToggleAtivo = async (gateway: ConfiguracaoGateway) => {
    try {
      if (gateway.ativo) {
        await desativarMutation.mutateAsync(gateway.id);
        showSuccess('Gateway desativado');
      } else {
        await ativarMutation.mutateAsync(gateway.id);
        showSuccess('Gateway ativado');
      }
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao alterar status');
    }
  };

  const handleRemover = async (id: string) => {
    if (!confirm('Deseja realmente remover esta configuração?')) return;

    try {
      await removerMutation.mutateAsync(id);
      showSuccess('Configuração removida');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao remover');
    }
  };

  const getStatusIcon = (gateway: ConfiguracaoGateway) => {
    if (!gateway.configurado) {
      return <AlertTriangle className="h-5 w-5 text-yellow-500" />;
    }
    if (gateway.statusValidacao === 'VALIDO' && gateway.ativo) {
      return <CheckCircle className="h-5 w-5 text-green-500" />;
    }
    if (gateway.statusValidacao === 'INVALIDO') {
      return <XCircle className="h-5 w-5 text-red-500" />;
    }
    return <Settings className="h-5 w-5 text-gray-400" />;
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 border-t-blue-600" />
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">Gateways de Pagamento</h1>
          <p className="text-sm text-gray-600 dark:text-gray-400">
            Configure os gateways para receber pagamentos online
          </p>
        </div>
        <button
          onClick={() => handleOpenForm()}
          className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 w-full sm:w-auto"
        >
          <CreditCard className="h-4 w-4" />
          Adicionar Gateway
        </button>
      </div>

      {/* Lista de Gateways */}
      {gateways && gateways.length > 0 ? (
        <div className="grid gap-4 lg:grid-cols-2">
          {gateways.map((gateway) => (
            <div
              key={gateway.id}
              className="rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 sm:p-6"
            >
              {/* Header do Card */}
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex items-center gap-3">
                  {getStatusIcon(gateway)}
                  <div>
                    <h3 className="font-semibold text-gray-900 dark:text-white">
                      {gateway.tipoGatewayDescricao}
                    </h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {gateway.ambienteDescricao}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 self-start">
                  {gateway.padrao && (
                    <span className="rounded bg-blue-100 dark:bg-blue-900/30 px-2 py-1 text-xs font-medium text-blue-800 dark:text-blue-400">
                      Padrão
                    </span>
                  )}
                  <span
                    className={`rounded px-2 py-1 text-xs font-medium ${
                      gateway.ativo
                        ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-400'
                        : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-400'
                    }`}
                  >
                    {gateway.ativo ? 'Ativo' : 'Inativo'}
                  </span>
                </div>
              </div>

              {/* Info */}
              <div className="mt-4 space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Status:</span>
                  <span className={`font-medium ${
                    gateway.statusValidacao === 'VALIDO'
                      ? 'text-green-600 dark:text-green-400'
                      : gateway.statusValidacao === 'INVALIDO'
                      ? 'text-red-600 dark:text-red-400'
                      : 'text-gray-600 dark:text-gray-400'
                  }`}>
                    {gateway.statusValidacao || (gateway.configurado ? 'Não validado' : 'Não configurado')}
                  </span>
                </div>
                {gateway.taxaPercentual && (
                  <div className="flex justify-between">
                    <span className="text-gray-500 dark:text-gray-400">Taxa:</span>
                    <span className="text-gray-900 dark:text-white">{gateway.taxaPercentual}%</span>
                  </div>
                )}
              </div>

              {/* Ações - Layout responsivo */}
              <div className="mt-4 flex flex-wrap gap-2">
                <button
                  onClick={() => handleOpenForm(gateway)}
                  className="flex-1 min-w-[100px] rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Configurar
                </button>
                {gateway.configurado && (
                  <button
                    onClick={() => handleValidar(gateway.id)}
                    disabled={validarMutation.isPending}
                    className="flex items-center justify-center gap-1 rounded-lg border border-blue-600 dark:border-blue-500 px-3 py-2 text-sm font-medium text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 disabled:opacity-50"
                  >
                    <Shield className="h-4 w-4" />
                    <span className="hidden sm:inline">Validar</span>
                  </button>
                )}
                <button
                  onClick={() => handleToggleAtivo(gateway)}
                  disabled={!gateway.configurado}
                  className={`rounded-lg px-3 py-2 text-sm font-medium ${
                    gateway.ativo
                      ? 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 hover:bg-red-200'
                      : 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 hover:bg-green-200'
                  } disabled:opacity-50`}
                >
                  {gateway.ativo ? 'Desativar' : 'Ativar'}
                </button>
                <button
                  onClick={() => handleRemover(gateway.id)}
                  className="rounded-lg p-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
                  title="Remover"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="rounded-lg border border-dashed border-gray-300 dark:border-gray-600 p-12 text-center">
          <CreditCard className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-4 text-lg font-medium text-gray-900 dark:text-white">
            Nenhum gateway configurado
          </h3>
          <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
            Configure um gateway de pagamento para começar a receber pagamentos online.
          </p>
          <button
            onClick={() => handleOpenForm()}
            className="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
          >
            Configurar Mercado Pago
          </button>
        </div>
      )}

      {/* Modal de Configuração */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 p-0 sm:p-4">
          <div className="w-full sm:max-w-lg max-h-[90vh] overflow-y-auto rounded-t-2xl sm:rounded-lg bg-white dark:bg-gray-800 shadow-xl">
            {/* Header fixo do modal */}
            <div className="sticky top-0 z-10 bg-white dark:bg-gray-800 px-4 sm:px-6 pt-4 sm:pt-6 pb-2 border-b border-gray-200 dark:border-gray-700">
              <div className="mx-auto mb-2 h-1 w-12 rounded-full bg-gray-300 dark:bg-gray-600 sm:hidden" />
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                {editingGateway ? 'Configurar Gateway' : 'Novo Gateway'}
              </h2>
            </div>

            <div className="px-4 sm:px-6 py-4 space-y-4">
              {/* Alerta informativo */}
              {formData.ambiente === AmbienteGateway.PRODUCAO && (
                <div className="rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 p-4">
                  <div className="flex gap-3">
                    <AlertTriangle className="h-5 w-5 text-amber-500 flex-shrink-0 mt-0.5" />
                    <div className="text-sm">
                      <p className="font-medium text-amber-800 dark:text-amber-300">
                        Credenciais de Produção
                      </p>
                      <p className="mt-1 text-amber-700 dark:text-amber-400">
                        Para receber pagamentos reais, você precisa preencher todos os campos:
                        Access Token, Public Key, Client ID e Client Secret.
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {formData.ambiente === AmbienteGateway.SANDBOX && (
                <div className="rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 p-4">
                  <div className="flex gap-3">
                    <Shield className="h-5 w-5 text-blue-500 flex-shrink-0 mt-0.5" />
                    <div className="text-sm">
                      <p className="font-medium text-blue-800 dark:text-blue-300">
                        Ambiente de Testes (Sandbox)
                      </p>
                      <p className="mt-1 text-blue-700 dark:text-blue-400">
                        Use as credenciais de teste para simular pagamentos. Nenhum dinheiro real será movimentado.
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* Tipo de Gateway */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Gateway
                </label>
                <select
                  value={formData.tipoGateway}
                  onChange={(e) => setFormData({ ...formData, tipoGateway: e.target.value as TipoGateway })}
                  disabled={!!editingGateway}
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-gray-900 dark:text-white focus:border-blue-500 focus:outline-none disabled:opacity-50"
                >
                  {Object.values(TipoGateway).map((tipo) => (
                    <option key={tipo} value={tipo}>
                      {TipoGatewayLabels[tipo]}
                    </option>
                  ))}
                </select>
              </div>

              {/* Ambiente */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Ambiente
                </label>
                <select
                  value={formData.ambiente}
                  onChange={(e) => setFormData({ ...formData, ambiente: e.target.value as AmbienteGateway })}
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-gray-900 dark:text-white focus:border-blue-500 focus:outline-none"
                >
                  {Object.values(AmbienteGateway).map((amb) => (
                    <option key={amb} value={amb}>
                      {AmbienteGatewayLabels[amb]}
                    </option>
                  ))}
                </select>
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Use Sandbox para testes. Mude para Produção quando estiver pronto.
                </p>
              </div>

              {/* Access Token */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Access Token <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  value={formData.accessToken || ''}
                  onChange={(e) => setFormData({ ...formData, accessToken: e.target.value })}
                  placeholder={editingGateway ? '••••••••• (deixe vazio para manter)' : 'Cole seu Access Token aqui'}
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-gray-900 dark:text-white focus:border-blue-500 focus:outline-none"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Encontre em: Mercado Pago → Seu negócio → Credenciais
                </p>
              </div>

              {/* Public Key */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Public Key <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.publicKey || ''}
                  onChange={(e) => setFormData({ ...formData, publicKey: e.target.value })}
                  placeholder="APP_USR-xxxxxxxx..."
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-gray-900 dark:text-white focus:border-blue-500 focus:outline-none"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Necessária para pagamento inline (Checkout Bricks)
                </p>
              </div>

              {/* Client ID */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Client ID <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.clientId || ''}
                  onChange={(e) => setFormData({ ...formData, clientId: e.target.value })}
                  placeholder={editingGateway ? '(deixe vazio para manter)' : 'Seu Client ID'}
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-gray-900 dark:text-white focus:border-blue-500 focus:outline-none"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Encontre em: Mercado Pago → Suas integrações → Credenciais de produção
                </p>
              </div>

              {/* Client Secret */}
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Client Secret <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  value={formData.clientSecret || ''}
                  onChange={(e) => setFormData({ ...formData, clientSecret: e.target.value })}
                  placeholder={editingGateway ? '••••••••• (deixe vazio para manter)' : 'Seu Client Secret'}
                  className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-gray-900 dark:text-white focus:border-blue-500 focus:outline-none"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Necessário para receber pagamentos reais em produção
                </p>
              </div>

              {/* Taxa */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Taxa (%)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.taxaPercentual || ''}
                    onChange={(e) => setFormData({ ...formData, taxaPercentual: parseFloat(e.target.value) || undefined })}
                    placeholder="4.99"
                    className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-gray-900 dark:text-white focus:border-blue-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Taxa Fixa (R$)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.taxaFixa || ''}
                    onChange={(e) => setFormData({ ...formData, taxaFixa: parseFloat(e.target.value) || undefined })}
                    placeholder="0.00"
                    className="mt-1 w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-3 py-2 text-gray-900 dark:text-white focus:border-blue-500 focus:outline-none"
                  />
                </div>
              </div>

              {/* Gateway Padrão */}
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="padrao"
                  checked={formData.padrao || false}
                  onChange={(e) => setFormData({ ...formData, padrao: e.target.checked })}
                  className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <label htmlFor="padrao" className="text-sm text-gray-700 dark:text-gray-300">
                  Definir como gateway padrão
                </label>
              </div>
            </div>

            {/* Footer fixo do modal */}
            <div className="sticky bottom-0 bg-white dark:bg-gray-800 px-4 sm:px-6 py-4 border-t border-gray-200 dark:border-gray-700">
              <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                <button
                  onClick={() => {
                    setShowForm(false);
                    setEditingGateway(null);
                  }}
                  className="w-full sm:w-auto rounded-lg border border-gray-300 dark:border-gray-600 px-4 py-2.5 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Cancelar
                </button>
                <button
                  onClick={handleSave}
                  disabled={criarMutation.isPending || atualizarMutation.isPending}
                  className="w-full sm:w-auto rounded-lg bg-blue-600 px-4 py-2.5 text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  {criarMutation.isPending || atualizarMutation.isPending ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
