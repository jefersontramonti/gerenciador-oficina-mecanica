/**
 * Formulario para edicao dos dados fiscais da oficina
 */

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  FileText,
  Building2,
  Receipt,
  Loader2,
  Save,
  AlertTriangle,
  Info,
} from 'lucide-react';
import { useOficina, useUpdateOficinaFiscal, useCurrentOficinaId } from '../hooks/useOficina';
import { oficinaFiscalSchema, type OficinaFiscalFormData } from '../utils/validation';
import {
  regimeTributarioLabels,
  statusOficinaLabels,
  planoLabels,
  type RegimeTributario,
} from '../types';

export const OficinaFiscalForm = () => {
  const oficinaId = useCurrentOficinaId();
  const { data: oficina, isLoading } = useOficina();
  const updateMutation = useUpdateOficinaFiscal();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<OficinaFiscalFormData>({
    resolver: zodResolver(oficinaFiscalSchema),
    defaultValues: {
      inscricaoEstadual: '',
      inscricaoMunicipal: '',
      regimeTributario: undefined,
    },
  });

  // Update form when oficina data is loaded
  useEffect(() => {
    if (oficina) {
      reset({
        inscricaoEstadual: oficina.inscricaoEstadual || '',
        inscricaoMunicipal: oficina.inscricaoMunicipal || '',
        regimeTributario: oficina.regimeTributario as RegimeTributario | undefined,
      });
    }
  }, [oficina, reset]);

  const onSubmit = async (data: OficinaFiscalFormData) => {
    if (!oficinaId) return;

    await updateMutation.mutateAsync({
      id: oficinaId,
      data: {
        inscricaoEstadual: data.inscricaoEstadual || undefined,
        inscricaoMunicipal: data.inscricaoMunicipal || undefined,
        regimeTributario: data.regimeTributario,
      },
    });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (!oficinaId) {
    return (
      <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-4 text-yellow-800 dark:border-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-200">
        Voce nao possui uma oficina vinculada.
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Aviso importante */}
      <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 dark:border-amber-800 dark:bg-amber-900/20">
        <div className="flex items-start gap-3">
          <AlertTriangle className="h-5 w-5 flex-shrink-0 text-amber-600 dark:text-amber-400" />
          <div>
            <h4 className="text-sm font-medium text-amber-800 dark:text-amber-200">
              Atencao
            </h4>
            <p className="mt-1 text-sm text-amber-700 dark:text-amber-300">
              Os dados fiscais sao utilizados para emissao de notas fiscais
              (NF-e, NFS-e, NFC-e). Certifique-se de que as informacoes estao
              corretas antes de salvar.
            </p>
          </div>
        </div>
      </div>

      {/* Dados Somente Leitura */}
      <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-800">
        <h3 className="mb-4 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
          <Info className="h-4 w-4" />
          Dados Cadastrais (somente leitura)
        </h3>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">CNPJ/CPF</p>
            <p className="font-medium text-gray-900 dark:text-white">
              {oficina?.cnpjCpf || '-'}
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Razao Social</p>
            <p className="font-medium text-gray-900 dark:text-white">
              {oficina?.razaoSocial || oficina?.nome || '-'}
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Status</p>
            <p className="font-medium text-gray-900 dark:text-white">
              {oficina?.status ? statusOficinaLabels[oficina.status] : '-'}
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-500 dark:text-gray-400">Plano</p>
            <p className="font-medium text-gray-900 dark:text-white">
              {oficina?.plano ? planoLabels[oficina.plano] : '-'}
            </p>
          </div>
        </div>
        <p className="mt-3 text-xs text-gray-500 dark:text-gray-400">
          Para alterar o CNPJ ou razao social, entre em contato com o suporte.
        </p>
      </div>

      {/* Inscricoes */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <FileText className="h-5 w-5" />
          Inscricoes
        </h3>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label
              htmlFor="inscricaoEstadual"
              className="mb-1 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              <Building2 className="h-4 w-4" />
              Inscricao Estadual
            </label>
            <input
              id="inscricaoEstadual"
              type="text"
              placeholder="000.000.000.000"
              maxLength={30}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('inscricaoEstadual')}
            />
            <p className="mt-1 text-xs text-gray-500">
              Numero da inscricao na Secretaria da Fazenda Estadual
            </p>
            {errors.inscricaoEstadual && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.inscricaoEstadual.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="inscricaoMunicipal"
              className="mb-1 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              <Building2 className="h-4 w-4" />
              Inscricao Municipal
            </label>
            <input
              id="inscricaoMunicipal"
              type="text"
              placeholder="000.000.000.000"
              maxLength={30}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('inscricaoMunicipal')}
            />
            <p className="mt-1 text-xs text-gray-500">
              Numero da inscricao na Prefeitura Municipal
            </p>
            {errors.inscricaoMunicipal && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.inscricaoMunicipal.message}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Regime Tributario */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <Receipt className="h-5 w-5" />
          Regime Tributario
        </h3>

        <div>
          <label
            htmlFor="regimeTributario"
            className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
          >
            Selecione o regime tributario
          </label>
          <select
            id="regimeTributario"
            className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white sm:max-w-md"
            {...register('regimeTributario')}
          >
            <option value="">Selecione</option>
            {Object.entries(regimeTributarioLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
          {errors.regimeTributario && (
            <p className="mt-1 text-sm text-red-600 dark:text-red-400">
              {errors.regimeTributario.message}
            </p>
          )}
        </div>

        {/* Explicacao dos regimes */}
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-900">
          <h4 className="mb-3 text-sm font-medium text-gray-700 dark:text-gray-300">
            Entenda os regimes tributarios:
          </h4>
          <div className="space-y-3 text-sm text-gray-600 dark:text-gray-400">
            <div>
              <strong className="text-gray-700 dark:text-gray-300">MEI</strong>
              <p>
                Microempreendedor Individual. Faturamento anual de ate R$ 81.000.
                Isento de impostos federais.
              </p>
            </div>
            <div>
              <strong className="text-gray-700 dark:text-gray-300">
                Simples Nacional
              </strong>
              <p>
                Regime simplificado para micro e pequenas empresas. Faturamento
                anual de ate R$ 4,8 milhoes. Aliquota unica que engloba varios
                impostos.
              </p>
            </div>
            <div>
              <strong className="text-gray-700 dark:text-gray-300">
                Lucro Presumido
              </strong>
              <p>
                Forma simplificada de tributacao onde a base de calculo e
                presumida a partir da receita bruta. Faturamento anual de ate R$
                78 milhoes.
              </p>
            </div>
            <div>
              <strong className="text-gray-700 dark:text-gray-300">
                Lucro Real
              </strong>
              <p>
                Regime obrigatorio para empresas com faturamento acima de R$ 78
                milhoes/ano ou atividades especificas. Tributos calculados sobre
                o lucro liquido efetivo.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Info sobre emissao de NF */}
      <div className="rounded-lg border border-blue-200 bg-blue-50 p-4 dark:border-blue-800 dark:bg-blue-900/20">
        <h4 className="mb-2 flex items-center gap-2 text-sm font-medium text-blue-800 dark:text-blue-200">
          <FileText className="h-4 w-4" />
          Emissao de Notas Fiscais
        </h4>
        <p className="text-sm text-blue-700 dark:text-blue-300">
          A emissao de notas fiscais esta disponivel nos planos{' '}
          <strong>Profissional</strong> e <strong>Turbinado</strong>. Certifique-se
          de configurar corretamente os dados fiscais antes de emitir sua primeira
          nota.
        </p>
      </div>

      {/* Submit */}
      <div className="flex justify-end border-t border-gray-200 pt-4 dark:border-gray-700">
        <button
          type="submit"
          disabled={updateMutation.isPending || !isDirty}
          className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {updateMutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Salvando...
            </>
          ) : (
            <>
              <Save className="h-4 w-4" />
              Salvar Alteracoes
            </>
          )}
        </button>
      </div>
    </form>
  );
};
