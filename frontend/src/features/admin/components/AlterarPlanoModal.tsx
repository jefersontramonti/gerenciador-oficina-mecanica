/**
 * Alterar Plano Modal - Change a workshop's subscription plan
 */

import { useState, useEffect } from 'react';
import { Loader2, CreditCard, AlertTriangle } from 'lucide-react';
import { usePlanosActive, useAlterarPlanoOficina } from '../hooks/useSaas';
import { Modal } from '@/shared/components/ui/Modal';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { formatCurrency } from '@/shared/utils/formatters';
import type { PlanoAssinatura } from '../types';

interface AlterarPlanoModalProps {
  isOpen: boolean;
  onClose: () => void;
  oficinaId: string;
  oficinaNome: string;
  planoAtual: PlanoAssinatura;
}

export const AlterarPlanoModal = ({
  isOpen,
  onClose,
  oficinaId,
  oficinaNome,
  planoAtual,
}: AlterarPlanoModalProps) => {
  const [novoPlano, setNovoPlano] = useState<string>(planoAtual);
  const [aplicarImediatamente, setAplicarImediatamente] = useState(true);
  const [manterPrecoAntigo, setManterPrecoAntigo] = useState(false);
  const [motivo, setMotivo] = useState('');

  const { data: planos, isLoading: isLoadingPlanos } = usePlanosActive();
  const alterarPlanoMutation = useAlterarPlanoOficina();

  useEffect(() => {
    if (isOpen) {
      setNovoPlano(planoAtual);
      setAplicarImediatamente(true);
      setManterPrecoAntigo(false);
      setMotivo('');
    }
  }, [isOpen, planoAtual]);

  const handleSubmit = async () => {
    if (novoPlano === planoAtual) {
      showError('Selecione um plano diferente do atual');
      return;
    }

    try {
      await alterarPlanoMutation.mutateAsync({
        oficinaId,
        data: {
          novoPlano,
          aplicarImediatamente,
          manterPrecoAntigo,
          motivo: motivo || undefined,
        },
      });

      showSuccess('Plano alterado com sucesso!');
      onClose();
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao alterar plano');
    }
  };

  const selectedPlano = planos?.find((p) => p.codigo === novoPlano);
  const currentPlano = planos?.find((p) => p.codigo === planoAtual);

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Alterar Plano">
      <div className="space-y-4">
        {/* Workshop Info */}
        <div className="rounded-lg bg-gray-50 p-4 dark:bg-gray-700">
          <p className="font-medium text-gray-900 dark:text-white">{oficinaNome}</p>
          <p className="text-sm text-gray-600 dark:text-gray-400">
            Plano atual: <span className="font-semibold">{currentPlano?.nome || planoAtual}</span>
            {currentPlano && !currentPlano.precoSobConsulta && (
              <span className="ml-2">({formatCurrency(currentPlano.valorMensal)}/mês)</span>
            )}
          </p>
        </div>

        {/* Plan Selection */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
            Novo Plano *
          </label>
          {isLoadingPlanos ? (
            <div className="mt-2 flex items-center gap-2 text-gray-500">
              <Loader2 className="h-4 w-4 animate-spin" />
              Carregando planos...
            </div>
          ) : (
            <div className="mt-2 grid gap-2">
              {planos?.map((plano) => (
                <label
                  key={plano.id}
                  className={`flex cursor-pointer items-center justify-between rounded-lg border-2 p-3 transition-colors ${
                    novoPlano === plano.codigo
                      ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                      : plano.codigo === planoAtual
                      ? 'border-gray-300 bg-gray-100 opacity-60 dark:border-gray-600 dark:bg-gray-700'
                      : 'border-gray-200 hover:border-gray-300 dark:border-gray-600 dark:hover:border-gray-500'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <input
                      type="radio"
                      name="plano"
                      value={plano.codigo}
                      checked={novoPlano === plano.codigo}
                      onChange={() => setNovoPlano(plano.codigo)}
                      disabled={plano.codigo === planoAtual}
                      className="h-4 w-4 text-blue-600"
                    />
                    <div>
                      <p className="font-medium text-gray-900 dark:text-white">
                        {plano.nome}
                        {plano.recomendado && (
                          <span className="ml-2 text-xs text-purple-600 dark:text-purple-400">
                            Recomendado
                          </span>
                        )}
                        {plano.codigo === planoAtual && (
                          <span className="ml-2 text-xs text-gray-500">(Atual)</span>
                        )}
                      </p>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        {plano.limiteUsuarios === -1 ? 'Usuários ilimitados' : `${plano.limiteUsuarios} usuário(s)`}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    {plano.precoSobConsulta ? (
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                        Sob consulta
                      </span>
                    ) : (
                      <span className="font-bold text-gray-900 dark:text-white">
                        {formatCurrency(plano.valorMensal)}/mês
                      </span>
                    )}
                  </div>
                </label>
              ))}
            </div>
          )}
        </div>

        {/* Options */}
        {novoPlano !== planoAtual && (
          <div className="space-y-3 rounded-lg border border-gray-200 p-4 dark:border-gray-600">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={aplicarImediatamente}
                onChange={(e) => setAplicarImediatamente(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-blue-600"
              />
              <span className="text-gray-700 dark:text-gray-300">
                Aplicar imediatamente
              </span>
            </label>

            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={manterPrecoAntigo}
                onChange={(e) => setManterPrecoAntigo(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-blue-600"
              />
              <span className="text-gray-700 dark:text-gray-300">
                Manter preço anterior (grandfathering)
              </span>
            </label>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Motivo da alteração
              </label>
              <textarea
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                rows={2}
                placeholder="Opcional - informe o motivo da alteração..."
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>
        )}

        {/* Price Change Warning */}
        {novoPlano !== planoAtual && selectedPlano && currentPlano && !manterPrecoAntigo && (
          <div className="flex items-start gap-3 rounded-lg border border-amber-300 bg-amber-50 p-4 dark:border-amber-700 dark:bg-amber-900/20">
            <AlertTriangle className="h-5 w-5 flex-shrink-0 text-amber-600 dark:text-amber-400" />
            <div className="text-sm text-amber-800 dark:text-amber-300">
              {selectedPlano.precoSobConsulta ? (
                <p>O novo plano tem preço sob consulta. Defina o valor manualmente após a alteração.</p>
              ) : selectedPlano.valorMensal > (currentPlano.valorMensal || 0) ? (
                <p>
                  O valor mensal aumentará de{' '}
                  <strong>{formatCurrency(currentPlano.valorMensal || 0)}</strong> para{' '}
                  <strong>{formatCurrency(selectedPlano.valorMensal)}</strong>.
                </p>
              ) : (
                <p>
                  O valor mensal diminuirá de{' '}
                  <strong>{formatCurrency(currentPlano.valorMensal || 0)}</strong> para{' '}
                  <strong>{formatCurrency(selectedPlano.valorMensal)}</strong>.
                </p>
              )}
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-end gap-3 pt-2">
          <button
            onClick={onClose}
            className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
          >
            Cancelar
          </button>
          <button
            onClick={handleSubmit}
            disabled={novoPlano === planoAtual || alterarPlanoMutation.isPending}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {alterarPlanoMutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Alterando...
              </>
            ) : (
              <>
                <CreditCard className="h-4 w-4" />
                Alterar Plano
              </>
            )}
          </button>
        </div>
      </div>
    </Modal>
  );
};
