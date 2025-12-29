/**
 * Comunicado Form Page - Create or edit a comunicado
 */

import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  ArrowLeft,
  Save,
  Loader2,
  Send,
  Clock,
  Megaphone,
  AlertTriangle,
  Info,
} from 'lucide-react';
import {
  useComunicadoDetail,
  useCreateComunicado,
  useUpdateComunicado,
  useEnviarComunicado,
  useAgendarComunicado,
  usePlanos,
  useOficinas,
} from '../hooks/useSaas';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { tipoComunicadoLabels, prioridadeComunicadoLabels, statusLabels } from '../types';

const comunicadoSchema = z.object({
  titulo: z
    .string()
    .min(3, 'Titulo deve ter pelo menos 3 caracteres')
    .max(200, 'Titulo deve ter no maximo 200 caracteres'),
  resumo: z.string().max(500, 'Resumo deve ter no maximo 500 caracteres').optional(),
  conteudo: z
    .string()
    .min(10, 'Conteudo deve ter pelo menos 10 caracteres')
    .max(10000, 'Conteudo deve ter no maximo 10000 caracteres'),
  tipo: z.enum(['NOVIDADE', 'MANUTENCAO', 'FINANCEIRO', 'ATUALIZACAO', 'ALERTA', 'PROMOCAO', 'OUTRO']),
  prioridade: z.enum(['BAIXA', 'NORMAL', 'ALTA', 'URGENTE']),
  planosAlvo: z.array(z.string()).optional(),
  oficinasAlvo: z.array(z.string()).optional(),
  statusOficinasAlvo: z.array(z.string()).optional(),
  requerConfirmacao: z.boolean(),
  exibirNoLogin: z.boolean(),
  dataAgendamento: z.string().optional(),
});

type ComunicadoFormData = z.infer<typeof comunicadoSchema>;

const defaultValues: ComunicadoFormData = {
  titulo: '',
  resumo: '',
  conteudo: '',
  tipo: 'NOVIDADE',
  prioridade: 'NORMAL',
  planosAlvo: [],
  oficinasAlvo: [],
  statusOficinasAlvo: [],
  requerConfirmacao: false,
  exibirNoLogin: false,
  dataAgendamento: '',
};

type SubmitAction = 'draft' | 'send' | 'schedule';

export function ComunicadoFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = !!id;

  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [scheduleDate, setScheduleDate] = useState('');
  const [submitAction, setSubmitAction] = useState<SubmitAction>('draft');

  const { data: comunicado, isLoading: isLoadingComunicado } = useComunicadoDetail(id);
  const { data: planos } = usePlanos();
  const { data: oficinasData } = useOficinas({ page: 0, size: 1000 });

  const createMutation = useCreateComunicado();
  const updateMutation = useUpdateComunicado();
  const enviarMutation = useEnviarComunicado();
  const agendarMutation = useAgendarComunicado();

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isDirty },
  } = useForm<ComunicadoFormData>({
    resolver: zodResolver(comunicadoSchema),
    defaultValues,
  });

  // Populate form with existing data
  useEffect(() => {
    if (comunicado && isEditing) {
      reset({
        titulo: comunicado.titulo,
        resumo: comunicado.resumo || '',
        conteudo: comunicado.conteudo,
        tipo: comunicado.tipo,
        prioridade: comunicado.prioridade,
        planosAlvo: comunicado.planosAlvo || [],
        oficinasAlvo: comunicado.oficinasAlvo || [],
        statusOficinasAlvo: comunicado.statusOficinasAlvo || [],
        requerConfirmacao: comunicado.requerConfirmacao,
        exibirNoLogin: comunicado.exibirNoLogin,
        dataAgendamento: comunicado.dataAgendamento || '',
      });
    }
  }, [comunicado, isEditing, reset]);

  const onSubmit = async (data: ComunicadoFormData) => {
    try {
      if (isEditing) {
        await updateMutation.mutateAsync({
          id: id!,
          data: {
            ...data,
            resumo: data.resumo || undefined,
            dataAgendamento: data.dataAgendamento || undefined,
          },
        });
        showSuccess('Comunicado atualizado com sucesso!');
      } else {
        // For new comunicados, handle the submit action
        const enviarAgora = submitAction === 'send';
        const dataAgendamento = submitAction === 'schedule' ? scheduleDate : undefined;

        await createMutation.mutateAsync({
          ...data,
          resumo: data.resumo || undefined,
          dataAgendamento: dataAgendamento ? new Date(dataAgendamento).toISOString() : undefined,
          enviarAgora,
        });

        if (enviarAgora) {
          showSuccess('Comunicado criado e enviado com sucesso!');
        } else if (dataAgendamento) {
          showSuccess('Comunicado criado e agendado com sucesso!');
        } else {
          showSuccess('Comunicado criado como rascunho!');
        }
      }
      navigate('/admin/comunicados');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao salvar comunicado');
    }
  };

  const handleSubmitWithAction = (action: SubmitAction) => {
    if (action === 'schedule' && !isEditing) {
      setSubmitAction('schedule');
      setShowScheduleModal(true);
    } else {
      setSubmitAction(action);
      // Trigger form submit
      handleSubmit(onSubmit)();
    }
  };

  const handleScheduleConfirm = () => {
    if (!scheduleDate) return;
    setShowScheduleModal(false);
    handleSubmit(onSubmit)();
  };

  const handleEnviarAgora = async () => {
    if (!id) return;

    const confirmed = window.confirm(
      'Tem certeza que deseja enviar este comunicado agora? Esta acao nao pode ser desfeita.'
    );

    if (!confirmed) return;

    try {
      await enviarMutation.mutateAsync(id);
      showSuccess('Comunicado enviado com sucesso!');
      navigate('/admin/comunicados');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao enviar comunicado');
    }
  };

  const handleAgendar = async () => {
    if (!id || !scheduleDate) return;

    try {
      await agendarMutation.mutateAsync({
        id,
        dataAgendamento: new Date(scheduleDate).toISOString(),
      });
      showSuccess('Comunicado agendado com sucesso!');
      setShowScheduleModal(false);
      navigate('/admin/comunicados');
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao agendar comunicado');
    }
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;
  const canSend = comunicado && comunicado.podeEnviar;

  if (isEditing && isLoadingComunicado) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
      </div>
    );
  }

  if (isEditing && comunicado && !comunicado.podeEditar) {
    return (
      <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
        <div className="mx-auto max-w-4xl">
          <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-6 dark:border-yellow-800 dark:bg-yellow-900/20">
            <div className="flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-yellow-600" />
              <div>
                <h2 className="font-medium text-yellow-800 dark:text-yellow-200">
                  Este comunicado nao pode ser editado
                </h2>
                <p className="mt-1 text-sm text-yellow-700 dark:text-yellow-300">
                  Comunicados enviados ou cancelados nao podem ser editados.
                </p>
              </div>
            </div>
            <Link
              to="/admin/comunicados"
              className="mt-4 inline-flex items-center gap-2 text-sm text-yellow-800 underline hover:text-yellow-900 dark:text-yellow-300 dark:hover:text-yellow-200"
            >
              <ArrowLeft className="h-4 w-4" />
              Voltar para lista
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6 dark:bg-gray-900">
      <div className="mx-auto max-w-4xl">
        {/* Header */}
        <div className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link
              to="/admin/comunicados"
              className="rounded-lg p-2 text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800"
            >
              <ArrowLeft className="h-5 w-5" />
            </Link>
            <div>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                {isEditing ? 'Editar Comunicado' : 'Novo Comunicado'}
              </h1>
              {comunicado && (
                <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                  Status: {comunicado.statusDescricao}
                </p>
              )}
            </div>
          </div>

          {isEditing && canSend && (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setShowScheduleModal(true)}
                className="flex items-center gap-2 rounded-lg border border-blue-600 px-4 py-2 text-sm font-medium text-blue-600 hover:bg-blue-50 dark:border-blue-400 dark:text-blue-400 dark:hover:bg-blue-900/20"
              >
                <Clock className="h-4 w-4" />
                Agendar
              </button>
              <button
                type="button"
                onClick={handleEnviarAgora}
                disabled={enviarMutation.isPending}
                className="flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
              >
                {enviarMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Send className="h-4 w-4" />
                )}
                Enviar Agora
              </button>
            </div>
          )}
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Basic Info */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="mb-4 flex items-center gap-2">
              <Megaphone className="h-5 w-5 text-blue-600" />
              <h2 className="font-medium text-gray-900 dark:text-white">Informacoes Basicas</h2>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div className="md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Titulo *
                </label>
                <input
                  type="text"
                  {...register('titulo')}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  placeholder="Titulo do comunicado"
                />
                {errors.titulo && (
                  <p className="mt-1 text-sm text-red-500">{errors.titulo.message}</p>
                )}
              </div>

              <div className="md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Resumo (opcional)
                </label>
                <input
                  type="text"
                  {...register('resumo')}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  placeholder="Breve resumo do comunicado"
                />
                {errors.resumo && (
                  <p className="mt-1 text-sm text-red-500">{errors.resumo.message}</p>
                )}
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Tipo *
                </label>
                <select
                  {...register('tipo')}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                >
                  {Object.entries(tipoComunicadoLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Prioridade *
                </label>
                <select
                  {...register('prioridade')}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                >
                  {Object.entries(prioridadeComunicadoLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Conteudo *
                </label>
                <textarea
                  {...register('conteudo')}
                  rows={10}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                  placeholder="Conteudo completo do comunicado..."
                />
                {errors.conteudo && (
                  <p className="mt-1 text-sm text-red-500">{errors.conteudo.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Target Settings */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="mb-4 flex items-center gap-2">
              <Info className="h-5 w-5 text-blue-600" />
              <h2 className="font-medium text-gray-900 dark:text-white">Segmentacao</h2>
            </div>

            <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">
              Deixe vazio para enviar para todas as oficinas. Selecione opcoes para segmentar.
            </p>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Planos Alvo
                </label>
                <Controller
                  name="planosAlvo"
                  control={control}
                  render={({ field }) => (
                    <select
                      multiple
                      value={field.value || []}
                      onChange={(e) => {
                        const values = Array.from(e.target.selectedOptions, (option) => option.value);
                        field.onChange(values);
                      }}
                      className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                      size={4}
                    >
                      {planos?.map((plano) => (
                        <option key={plano.id} value={plano.codigo}>
                          {plano.nome}
                        </option>
                      ))}
                    </select>
                  )}
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Ctrl+click para selecionar multiplos
                </p>
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Status das Oficinas
                </label>
                <Controller
                  name="statusOficinasAlvo"
                  control={control}
                  render={({ field }) => (
                    <select
                      multiple
                      value={field.value || []}
                      onChange={(e) => {
                        const values = Array.from(e.target.selectedOptions, (option) => option.value);
                        field.onChange(values);
                      }}
                      className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                      size={4}
                    >
                      {Object.entries(statusLabels).map(([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ))}
                    </select>
                  )}
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Ctrl+click para selecionar multiplos
                </p>
              </div>

              <div className="md:col-span-2">
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Oficinas Especificas
                </label>
                <Controller
                  name="oficinasAlvo"
                  control={control}
                  render={({ field }) => (
                    <select
                      multiple
                      value={field.value || []}
                      onChange={(e) => {
                        const values = Array.from(e.target.selectedOptions, (option) => option.value);
                        field.onChange(values);
                      }}
                      className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
                      size={5}
                    >
                      {oficinasData?.content.map((oficina) => (
                        <option key={oficina.id} value={oficina.id}>
                          {oficina.nomeFantasia} - {oficina.cnpjCpf}
                        </option>
                      ))}
                    </select>
                  )}
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Ctrl+click para selecionar multiplas oficinas especificas
                </p>
              </div>
            </div>
          </div>

          {/* Options */}
          <div className="rounded-lg bg-white p-6 shadow dark:bg-gray-800">
            <div className="mb-4">
              <h2 className="font-medium text-gray-900 dark:text-white">Opcoes</h2>
            </div>

            <div className="space-y-4">
              <label className="flex items-center gap-3">
                <input
                  type="checkbox"
                  {...register('requerConfirmacao')}
                  className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <div>
                  <span className="text-sm font-medium text-gray-900 dark:text-white">
                    Requer confirmacao de leitura
                  </span>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    Usuarios deverao confirmar que leram o comunicado
                  </p>
                </div>
              </label>

              <label className="flex items-center gap-3">
                <input
                  type="checkbox"
                  {...register('exibirNoLogin')}
                  className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <div>
                  <span className="text-sm font-medium text-gray-900 dark:text-white">
                    Exibir no login
                  </span>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    O comunicado sera exibido quando o usuario fizer login
                  </p>
                </div>
              </label>
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-3">
            <Link
              to="/admin/comunicados"
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </Link>
            {isEditing ? (
              <button
                type="submit"
                disabled={isSaving || !isDirty}
                className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {isSaving ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Save className="h-4 w-4" />
                )}
                Salvar Alteracoes
              </button>
            ) : (
              <>
                <button
                  type="button"
                  onClick={() => handleSubmitWithAction('draft')}
                  disabled={isSaving}
                  className="flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
                >
                  {isSaving && submitAction === 'draft' ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Save className="h-4 w-4" />
                  )}
                  Salvar Rascunho
                </button>
                <button
                  type="button"
                  onClick={() => handleSubmitWithAction('schedule')}
                  disabled={isSaving}
                  className="flex items-center gap-2 rounded-lg border border-blue-600 px-4 py-2 text-sm font-medium text-blue-600 hover:bg-blue-50 disabled:opacity-50 dark:border-blue-400 dark:text-blue-400 dark:hover:bg-blue-900/20"
                >
                  {isSaving && submitAction === 'schedule' ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Clock className="h-4 w-4" />
                  )}
                  Agendar
                </button>
                <button
                  type="button"
                  onClick={() => handleSubmitWithAction('send')}
                  disabled={isSaving}
                  className="flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
                >
                  {isSaving && submitAction === 'send' ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Send className="h-4 w-4" />
                  )}
                  Enviar Agora
                </button>
              </>
            )}
          </div>
        </form>
      </div>

      {/* Schedule Modal */}
      {showScheduleModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl dark:bg-gray-800">
            <h3 className="mb-4 text-lg font-medium text-gray-900 dark:text-white">
              Agendar Envio
            </h3>
            <div className="mb-4">
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Data e Hora
              </label>
              <input
                type="datetime-local"
                value={scheduleDate}
                onChange={(e) => setScheduleDate(e.target.value)}
                min={new Date().toISOString().slice(0, 16)}
                className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              />
            </div>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => {
                  setShowScheduleModal(false);
                  setSubmitAction('draft');
                }}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={isEditing ? handleAgendar : handleScheduleConfirm}
                disabled={!scheduleDate || (isEditing ? agendarMutation.isPending : isSaving)}
                className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {(isEditing ? agendarMutation.isPending : isSaving) ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Clock className="h-4 w-4" />
                )}
                Agendar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
