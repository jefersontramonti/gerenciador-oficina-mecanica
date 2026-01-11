/**
 * Formulario para edicao das informacoes operacionais da oficina
 */

import { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Clock,
  Wrench,
  Car,
  Globe,
  Loader2,
  Save,
  CalendarDays,
  DollarSign,
} from 'lucide-react';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { useOficina, useUpdateOficinaOperacional, useCurrentOficinaId } from '../hooks/useOficina';
import { oficinaOperacionalSchema, type OficinaOperacionalFormData } from '../utils/validation';
import {
  diaSemanaLabels,
  DiaSemana,
  ESPECIALIDADES,
  MARCAS_AUTOMOTIVAS,
  SERVICOS_AUTOMOTIVOS,
} from '../types';

export const OficinaOperacionalForm = () => {
  const oficinaId = useCurrentOficinaId();
  const { data: oficina, isLoading } = useOficina();
  const updateMutation = useUpdateOficinaOperacional();

  const {
    register,
    handleSubmit,
    control,
    reset,
    watch,
    setValue,
    formState: { errors, isDirty },
  } = useForm<OficinaOperacionalFormData>({
    resolver: zodResolver(oficinaOperacionalSchema),
    defaultValues: {
      horarioAbertura: '',
      horarioFechamento: '',
      diasFuncionamento: [],
      capacidadeAtendimento: undefined,
      quantidadeElevadores: undefined,
      especialidades: [],
      marcasAtendidas: [],
      servicosOferecidos: [],
      aceitaAgendamentoOnline: false,
      tempoMedioAtendimento: undefined,
      observacoes: '',
      valorHora: undefined,
      website: '',
      facebook: '',
      instagram: '',
      youtube: '',
      linkedin: '',
      twitter: '',
      tiktok: '',
    },
  });

  const diasFuncionamento = watch('diasFuncionamento') || [];
  const especialidades = watch('especialidades') || [];
  const marcasAtendidas = watch('marcasAtendidas') || [];
  const servicosOferecidos = watch('servicosOferecidos') || [];

  // Update form when oficina data is loaded
  useEffect(() => {
    if (oficina) {
      const info = oficina.informacoesOperacionais;
      const redes = oficina.redesSociais;
      reset({
        horarioAbertura: info?.horarioAbertura || '',
        horarioFechamento: info?.horarioFechamento || '',
        diasFuncionamento: (info?.diasFuncionamento as DiaSemana[]) || [],
        capacidadeAtendimento: info?.capacidadeAtendimento,
        quantidadeElevadores: info?.quantidadeElevadores,
        especialidades: info?.especialidades || [],
        marcasAtendidas: info?.marcasAtendidas || [],
        servicosOferecidos: info?.servicosOferecidos || [],
        aceitaAgendamentoOnline: info?.aceitaAgendamentoOnline || false,
        tempoMedioAtendimento: info?.tempoMedioAtendimento,
        observacoes: info?.observacoes || '',
        valorHora: oficina.valorHora,
        website: redes?.website || '',
        facebook: redes?.facebook || '',
        instagram: redes?.instagram || '',
        youtube: redes?.youtube || '',
        linkedin: redes?.linkedin || '',
        twitter: redes?.twitter || '',
        tiktok: redes?.tiktok || '',
      });
    }
  }, [oficina, reset]);

  const toggleDia = (dia: DiaSemana) => {
    const current = diasFuncionamento || [];
    const newDias = current.includes(dia)
      ? current.filter((d) => d !== dia)
      : [...current, dia];
    setValue('diasFuncionamento', newDias, { shouldDirty: true });
  };

  const toggleItem = (
    field: 'especialidades' | 'marcasAtendidas' | 'servicosOferecidos',
    item: string
  ) => {
    const currentValues = {
      especialidades,
      marcasAtendidas,
      servicosOferecidos,
    }[field];
    const current = currentValues || [];
    const newItems = current.includes(item)
      ? current.filter((i) => i !== item)
      : [...current, item];
    setValue(field, newItems, { shouldDirty: true });
  };

  const onSubmit = async (data: OficinaOperacionalFormData) => {
    if (!oficinaId) return;

    // Helper to convert NaN to undefined
    const safeNumber = (val: number | undefined | null): number | undefined => {
      if (val === undefined || val === null) return undefined;
      if (typeof val === 'number' && isNaN(val)) return undefined;
      return val;
    };

    try {
      await updateMutation.mutateAsync({
        id: oficinaId,
        data: {
          informacoesOperacionais: {
            horarioAbertura: data.horarioAbertura || undefined,
            horarioFechamento: data.horarioFechamento || undefined,
            diasFuncionamento: data.diasFuncionamento,
            capacidadeAtendimento: safeNumber(data.capacidadeAtendimento),
            quantidadeElevadores: safeNumber(data.quantidadeElevadores),
            especialidades: data.especialidades,
            marcasAtendidas: data.marcasAtendidas,
            servicosOferecidos: data.servicosOferecidos,
            aceitaAgendamentoOnline: data.aceitaAgendamentoOnline,
            tempoMedioAtendimento: safeNumber(data.tempoMedioAtendimento),
            observacoes: data.observacoes || undefined,
          },
          redesSociais: {
            website: data.website || undefined,
            facebook: data.facebook || undefined,
            instagram: data.instagram || undefined,
            youtube: data.youtube || undefined,
            linkedin: data.linkedin || undefined,
            twitter: data.twitter || undefined,
            tiktok: data.tiktok || undefined,
          },
          valorHora: safeNumber(data.valorHora),
        },
      });
      showSuccess('Informacoes operacionais salvas com sucesso!');
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Erro ao salvar as alteracoes';
      showError(errorMessage);
    }
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
      {/* Horario de Funcionamento */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <Clock className="h-5 w-5" />
          Horario de Funcionamento
        </h3>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label
              htmlFor="horarioAbertura"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Abertura
            </label>
            <input
              id="horarioAbertura"
              type="time"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('horarioAbertura')}
            />
            {errors.horarioAbertura && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.horarioAbertura.message}
              </p>
            )}
          </div>

          <div>
            <label
              htmlFor="horarioFechamento"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Fechamento
            </label>
            <input
              id="horarioFechamento"
              type="time"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('horarioFechamento')}
            />
            {errors.horarioFechamento && (
              <p className="mt-1 text-sm text-red-600 dark:text-red-400">
                {errors.horarioFechamento.message}
              </p>
            )}
          </div>
        </div>

        {/* Dias de Funcionamento */}
        <div>
          <label className="mb-2 flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-300">
            <CalendarDays className="h-4 w-4" />
            Dias de Funcionamento
          </label>
          <div className="flex flex-wrap gap-2">
            {Object.entries(diaSemanaLabels).map(([key, label]) => (
              <button
                key={key}
                type="button"
                onClick={() => toggleDia(key as DiaSemana)}
                className={`rounded-full px-3 py-1 text-sm font-medium transition-colors ${
                  diasFuncionamento.includes(key as DiaSemana)
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-200 text-gray-700 hover:bg-gray-300 dark:bg-gray-700 dark:text-gray-300'
                }`}
              >
                {label.substring(0, 3)}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Valor/Hora de Mao de Obra */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <DollarSign className="h-5 w-5" />
          Mao de Obra
        </h3>

        <div className="max-w-xs">
          <label
            htmlFor="valorHora"
            className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
          >
            Valor/Hora (R$)
          </label>
          <div className="relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">
              R$
            </span>
            <input
              id="valorHora"
              type="number"
              min="0"
              max="10000"
              step="0.01"
              placeholder="80.00"
              className="w-full rounded-lg border border-gray-300 py-2 pl-10 pr-3 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('valorHora', { valueAsNumber: true })}
            />
          </div>
          <p className="mt-1 text-xs text-gray-500">
            Valor cobrado por hora de mao de obra. Usado no modelo de cobranca por hora.
          </p>
          {errors.valorHora && (
            <p className="mt-1 text-sm text-red-600 dark:text-red-400">
              {errors.valorHora.message}
            </p>
          )}
        </div>
      </div>

      {/* Capacidade */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <Wrench className="h-5 w-5" />
          Capacidade
        </h3>

        <div className="grid gap-4 sm:grid-cols-3">
          <div>
            <label
              htmlFor="capacidadeAtendimento"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Capacidade de Atendimento
            </label>
            <input
              id="capacidadeAtendimento"
              type="number"
              min="0"
              max="100"
              placeholder="Ex: 10"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('capacidadeAtendimento', { valueAsNumber: true })}
            />
            <p className="mt-1 text-xs text-gray-500">Veiculos simultaneos</p>
          </div>

          <div>
            <label
              htmlFor="quantidadeElevadores"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Elevadores
            </label>
            <input
              id="quantidadeElevadores"
              type="number"
              min="0"
              max="50"
              placeholder="Ex: 4"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('quantidadeElevadores', { valueAsNumber: true })}
            />
          </div>

          <div>
            <label
              htmlFor="tempoMedioAtendimento"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Tempo Medio (min)
            </label>
            <input
              id="tempoMedioAtendimento"
              type="number"
              min="0"
              max="1440"
              placeholder="Ex: 120"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('tempoMedioAtendimento', { valueAsNumber: true })}
            />
            <p className="mt-1 text-xs text-gray-500">Tempo medio de atendimento</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Controller
            name="aceitaAgendamentoOnline"
            control={control}
            render={({ field }) => (
              <input
                type="checkbox"
                id="aceitaAgendamentoOnline"
                checked={field.value}
                onChange={(e) => field.onChange(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
            )}
          />
          <label
            htmlFor="aceitaAgendamentoOnline"
            className="text-sm font-medium text-gray-700 dark:text-gray-300"
          >
            Aceita agendamento online
          </label>
        </div>
      </div>

      {/* Especialidades */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <Wrench className="h-5 w-5" />
          Especialidades
        </h3>
        <div className="flex flex-wrap gap-2">
          {ESPECIALIDADES.map((esp) => (
            <button
              key={esp}
              type="button"
              onClick={() => toggleItem('especialidades', esp)}
              className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                especialidades.includes(esp)
                  ? 'bg-green-600 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300 dark:bg-gray-700 dark:text-gray-300'
              }`}
            >
              {esp}
            </button>
          ))}
        </div>
      </div>

      {/* Marcas Atendidas */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <Car className="h-5 w-5" />
          Marcas Atendidas
        </h3>
        <div className="flex flex-wrap gap-2">
          {MARCAS_AUTOMOTIVAS.map((marca) => (
            <button
              key={marca}
              type="button"
              onClick={() => toggleItem('marcasAtendidas', marca)}
              className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                marcasAtendidas.includes(marca)
                  ? 'bg-purple-600 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300 dark:bg-gray-700 dark:text-gray-300'
              }`}
            >
              {marca}
            </button>
          ))}
        </div>
      </div>

      {/* Servicos Oferecidos */}
      <div className="space-y-4">
        <h3 className="text-lg font-medium text-gray-900 dark:text-white">
          Servicos Oferecidos
        </h3>
        <div className="flex flex-wrap gap-2">
          {SERVICOS_AUTOMOTIVOS.map((servico) => (
            <button
              key={servico}
              type="button"
              onClick={() => toggleItem('servicosOferecidos', servico)}
              className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                servicosOferecidos.includes(servico)
                  ? 'bg-orange-600 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300 dark:bg-gray-700 dark:text-gray-300'
              }`}
            >
              {servico}
            </button>
          ))}
        </div>
      </div>

      {/* Observacoes */}
      <div>
        <label
          htmlFor="observacoes"
          className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
        >
          Observacoes
        </label>
        <textarea
          id="observacoes"
          rows={3}
          placeholder="Informacoes adicionais sobre a oficina..."
          className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
          {...register('observacoes')}
        />
        {errors.observacoes && (
          <p className="mt-1 text-sm text-red-600 dark:text-red-400">
            {errors.observacoes.message}
          </p>
        )}
      </div>

      {/* Redes Sociais */}
      <div className="space-y-4">
        <h3 className="flex items-center gap-2 text-lg font-medium text-gray-900 dark:text-white">
          <Globe className="h-5 w-5" />
          Redes Sociais
        </h3>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label
              htmlFor="website"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Website
            </label>
            <input
              id="website"
              type="url"
              placeholder="https://www.suaoficina.com.br"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('website')}
            />
          </div>

          <div>
            <label
              htmlFor="instagram"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Instagram
            </label>
            <input
              id="instagram"
              type="text"
              placeholder="@suaoficina"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('instagram')}
            />
          </div>

          <div>
            <label
              htmlFor="facebook"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              Facebook
            </label>
            <input
              id="facebook"
              type="text"
              placeholder="facebook.com/suaoficina"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('facebook')}
            />
          </div>

          <div>
            <label
              htmlFor="youtube"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              YouTube
            </label>
            <input
              id="youtube"
              type="text"
              placeholder="youtube.com/@suaoficina"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('youtube')}
            />
          </div>

          <div>
            <label
              htmlFor="linkedin"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              LinkedIn
            </label>
            <input
              id="linkedin"
              type="text"
              placeholder="linkedin.com/company/suaoficina"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('linkedin')}
            />
          </div>

          <div>
            <label
              htmlFor="tiktok"
              className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300"
            >
              TikTok
            </label>
            <input
              id="tiktok"
              type="text"
              placeholder="@suaoficina"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 dark:border-gray-600 dark:bg-gray-900 dark:text-white"
              {...register('tiktok')}
            />
          </div>
        </div>
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
