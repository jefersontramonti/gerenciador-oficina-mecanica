import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Plus, Trash2, Save, Info, Search } from 'lucide-react';
import {
  usePlano,
  useCriarPlano,
  useAtualizarPlano,
  useTiposManutencao,
  useTemplatesDisponiveis,
} from '../hooks/useManutencaoPreventiva';
import { api } from '@/shared/services/api';
import { showSuccess, showError } from '@/shared/utils/notifications';
import type { PlanoManutencaoRequest, CriterioManutencao, ChecklistItem, AgendamentoNotificacao } from '../types';

const planoSchema = z.object({
  veiculoId: z.string().min(1, 'Selecione um veículo'),
  templateId: z.string().optional(),
  nome: z.string().min(3, 'Nome deve ter pelo menos 3 caracteres'),
  descricao: z.string().optional(),
  tipoManutencao: z.string().min(1, 'Selecione o tipo de manutenção'),
  criterio: z.enum(['TEMPO', 'KM', 'AMBOS']),
  intervaloDias: z.number().min(1).optional().nullable(),
  intervaloKm: z.number().min(1).optional().nullable(),
  antecedenciaDias: z.number().min(0),
  antecedenciaKm: z.number().min(0),
  canaisNotificacao: z.array(z.string()),
  ultimaExecucaoData: z.string().optional(),
  ultimaExecucaoKm: z.number().optional().nullable(),
  valorEstimado: z.number().optional().nullable(),
});

type PlanoFormData = z.infer<typeof planoSchema>;

export default function PlanoFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = !!id;

  const [checklist, setChecklist] = useState<ChecklistItem[]>([]);
  const [novoItem, setNovoItem] = useState('');
  const [itemObrigatorio, setItemObrigatorio] = useState(false);
  const [veiculoSearch, setVeiculoSearch] = useState('');
  const [veiculoResults, setVeiculoResults] = useState<any[]>([]);
  const [selectedVeiculo, setSelectedVeiculo] = useState<any>(null);
  const [agendamentosNotificacao, setAgendamentosNotificacao] = useState<AgendamentoNotificacao[]>([]);

  const { data: plano, isLoading: loadingPlano } = usePlano(id);
  const { data: tiposManutencao } = useTiposManutencao();
  const { data: templates } = useTemplatesDisponiveis();

  const criarMutation = useCriarPlano();
  const atualizarMutation = useAtualizarPlano();

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<PlanoFormData>({
    resolver: zodResolver(planoSchema),
    defaultValues: {
      criterio: 'TEMPO',
      antecedenciaDias: 7,
      antecedenciaKm: 500,
      canaisNotificacao: ['WHATSAPP', 'EMAIL', 'TELEGRAM'],
    },
  });

  const criterio = watch('criterio');
  const selectedTemplateId = watch('templateId');

  // Load existing plano data when editing
  useEffect(() => {
    if (plano && isEditing) {
      reset({
        veiculoId: plano.veiculo.id,
        nome: plano.nome,
        descricao: plano.descricao || '',
        tipoManutencao: plano.tipoManutencao,
        criterio: plano.criterio,
        intervaloDias: plano.intervaloDias || null,
        intervaloKm: plano.intervaloKm || null,
        antecedenciaDias: plano.antecedenciaDias,
        antecedenciaKm: plano.antecedenciaKm,
        canaisNotificacao: plano.canaisNotificacao,
        ultimaExecucaoData: plano.ultimaExecucaoData || undefined,
        ultimaExecucaoKm: plano.ultimaExecucaoKm || null,
        valorEstimado: plano.valorEstimado || null,
      });
      setSelectedVeiculo(plano.veiculo);
      setChecklist(plano.checklist || []);
      setAgendamentosNotificacao(plano.agendamentosNotificacao || []);
    }
  }, [plano, isEditing, reset]);

  // Apply template when selected
  useEffect(() => {
    if (selectedTemplateId && templates && !isEditing) {
      const template = templates.find((t) => t.id === selectedTemplateId);
      if (template) {
        setValue('nome', template.nome);
        setValue('descricao', template.descricao || '');
        setValue('tipoManutencao', template.tipoManutencao);
        setValue('criterio', template.criterio);
        setValue('intervaloDias', template.intervaloDias || null);
        setValue('intervaloKm', template.intervaloKm || null);
        setValue('antecedenciaDias', template.antecedenciaDias);
        setValue('antecedenciaKm', template.antecedenciaKm);
        setValue('valorEstimado', template.valorEstimado || null);
        setChecklist(template.checklist || []);
      }
    }
  }, [selectedTemplateId, templates, isEditing, setValue]);

  const [searchLoading, setSearchLoading] = useState(false);

  // Search vehicles using API
  const handleVeiculoSearch = async (search: string) => {
    setVeiculoSearch(search);
    if (search.length >= 2) {
      setSearchLoading(true);
      try {
        const response = await api.get(`/veiculos?placa=${search}&size=10`);
        setVeiculoResults(response.data.content || []);
      } catch (error) {
        console.error('Erro ao buscar veículos:', error);
        setVeiculoResults([]);
      } finally {
        setSearchLoading(false);
      }
    } else {
      setVeiculoResults([]);
    }
  };

  const addChecklistItem = () => {
    if (novoItem.trim()) {
      setChecklist([...checklist, { item: novoItem.trim(), obrigatorio: itemObrigatorio }]);
      setNovoItem('');
      setItemObrigatorio(false);
    }
  };

  const removeChecklistItem = (index: number) => {
    setChecklist(checklist.filter((_, i) => i !== index));
  };

  const onSubmit = async (data: PlanoFormData) => {
    try {
      const request: PlanoManutencaoRequest = {
        veiculoId: data.veiculoId,
        templateId: data.templateId || undefined,
        nome: data.nome,
        descricao: data.descricao || undefined,
        tipoManutencao: data.tipoManutencao,
        criterio: data.criterio as CriterioManutencao,
        intervaloDias: data.intervaloDias || undefined,
        intervaloKm: data.intervaloKm || undefined,
        antecedenciaDias: data.antecedenciaDias,
        antecedenciaKm: data.antecedenciaKm,
        canaisNotificacao: data.canaisNotificacao,
        ultimaExecucaoData: data.ultimaExecucaoData || undefined,
        ultimaExecucaoKm: data.ultimaExecucaoKm || undefined,
        checklist: checklist.length > 0 ? checklist : undefined,
        valorEstimado: data.valorEstimado || undefined,
        agendamentosNotificacao: agendamentosNotificacao.length > 0 ? agendamentosNotificacao : undefined,
      };

      if (isEditing) {
        await atualizarMutation.mutateAsync({ id, data: request });
        showSuccess('Plano atualizado com sucesso');
      } else {
        await criarMutation.mutateAsync(request);
        showSuccess('Plano criado com sucesso');
      }

      navigate('/manutencao-preventiva');
    } catch (error) {
      console.error('Erro ao salvar plano:', error);
      showError('Erro ao salvar plano');
    }
  };

  if (isEditing && loadingPlano) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3 sm:gap-4">
        <Link
          to="/manutencao-preventiva"
          className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          <ArrowLeft className="h-5 w-5 text-gray-500 dark:text-gray-400" />
        </Link>
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
            {isEditing ? 'Editar Plano' : 'Novo Plano de Manutenção'}
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 hidden sm:block">
            {isEditing ? 'Atualize as informações do plano' : 'Configure um novo plano de manutenção preventiva'}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Template Selection (only for new plans) */}
        {!isEditing && templates && templates.length > 0 && (
          <div className="bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800 p-4">
            <div className="flex items-start gap-3">
              <Info className="h-5 w-5 text-blue-600 dark:text-blue-400 mt-0.5" />
              <div className="flex-1">
                <h3 className="font-medium text-blue-900 dark:text-blue-100">Usar Template</h3>
                <p className="text-sm text-blue-700 dark:text-blue-300 mb-3">
                  Selecione um template para preencher automaticamente os campos
                </p>
                <select
                  {...register('templateId')}
                  className="w-full sm:w-auto px-3 py-2 border border-blue-300 dark:border-blue-700 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
                >
                  <option value="">Sem template (configuração manual)</option>
                  {templates.map((template) => (
                    <option key={template.id} value={template.id}>
                      {template.nome} ({template.tipoManutencao})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>
        )}

        {/* Main Form */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Left Column */}
          <div className="space-y-6">
            {/* Veículo Selection */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Veículo</h2>

              {selectedVeiculo ? (
                <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700 rounded-lg">
                  <div>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {selectedVeiculo.placaFormatada}
                    </p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {selectedVeiculo.marca} {selectedVeiculo.modelo} {selectedVeiculo.ano}
                    </p>
                  </div>
                  {!isEditing && (
                    <button
                      type="button"
                      onClick={() => {
                        setSelectedVeiculo(null);
                        setValue('veiculoId', '');
                      }}
                      className="text-sm text-red-600 dark:text-red-400 hover:underline"
                    >
                      Alterar
                    </button>
                  )}
                </div>
              ) : (
                <div>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <input
                      type="text"
                      value={veiculoSearch}
                      onChange={(e) => handleVeiculoSearch(e.target.value)}
                      placeholder="Buscar por placa..."
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                    />
                    {searchLoading && (
                      <div className="absolute right-3 top-1/2 -translate-y-1/2">
                        <div className="animate-spin h-4 w-4 border-2 border-blue-600 border-t-transparent rounded-full"></div>
                      </div>
                    )}
                  </div>
                  <input type="hidden" {...register('veiculoId')} />
                  {veiculoResults.length > 0 && (
                    <div className="mt-2 border border-gray-200 dark:border-gray-700 rounded-lg divide-y divide-gray-200 dark:divide-gray-700 max-h-60 overflow-y-auto">
                      {veiculoResults.map((veiculo: any) => (
                        <button
                          key={veiculo.id}
                          type="button"
                          onClick={() => {
                            setSelectedVeiculo(veiculo);
                            setValue('veiculoId', veiculo.id);
                            setVeiculoResults([]);
                            setVeiculoSearch('');
                          }}
                          className="w-full p-3 text-left hover:bg-gray-50 dark:hover:bg-gray-700"
                        >
                          <p className="font-medium text-gray-900 dark:text-white">
                            {veiculo.placaFormatada || veiculo.placa}
                          </p>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            {veiculo.marca} {veiculo.modelo} {veiculo.anoFabricacao && `(${veiculo.anoFabricacao})`}
                          </p>
                          {veiculo.cliente && (
                            <p className="text-xs text-gray-400 dark:text-gray-500">
                              Cliente: {veiculo.cliente.nome}
                            </p>
                          )}
                        </button>
                      ))}
                    </div>
                  )}
                  {veiculoSearch.length >= 2 && veiculoResults.length === 0 && !searchLoading && (
                    <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
                      Nenhum veículo encontrado
                    </p>
                  )}
                  {veiculoSearch.length < 2 && (
                    <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
                      Digite pelo menos 2 caracteres para buscar
                    </p>
                  )}
                </div>
              )}
              {errors.veiculoId && (
                <p className="mt-2 text-sm text-red-500">{errors.veiculoId.message}</p>
              )}
            </div>

            {/* Basic Info */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Informações Básicas</h2>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Nome do Plano *
                  </label>
                  <input
                    type="text"
                    {...register('nome')}
                    placeholder="Ex: Troca de Óleo"
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  />
                  {errors.nome && (
                    <p className="mt-1 text-sm text-red-500">{errors.nome.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Tipo de Manutenção *
                  </label>
                  <select
                    {...register('tipoManutencao')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  >
                    <option value="">Selecione...</option>
                    {tiposManutencao?.map((tipo) => (
                      <option key={tipo} value={tipo}>{tipo}</option>
                    ))}
                  </select>
                  {errors.tipoManutencao && (
                    <p className="mt-1 text-sm text-red-500">{errors.tipoManutencao.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Descrição
                  </label>
                  <textarea
                    {...register('descricao')}
                    rows={3}
                    placeholder="Descrição adicional do plano..."
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Valor Estimado (R$)
                  </label>
                  <Controller
                    name="valorEstimado"
                    control={control}
                    render={({ field }) => (
                      <input
                        type="number"
                        step="0.01"
                        value={field.value || ''}
                        onChange={(e) => field.onChange(e.target.value ? parseFloat(e.target.value) : null)}
                        placeholder="0,00"
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                      />
                    )}
                  />
                </div>
              </div>
            </div>

            {/* Critérios */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Critérios de Manutenção</h2>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Critério *
                  </label>
                  <div className="flex flex-wrap gap-4">
                    {(['TEMPO', 'KM', 'AMBOS'] as const).map((c) => (
                      <label key={c} className="flex items-center gap-2 cursor-pointer">
                        <input
                          type="radio"
                          {...register('criterio')}
                          value={c}
                          className="text-blue-600"
                        />
                        <span className="text-gray-900 dark:text-white">
                          {c === 'TEMPO' ? 'Por Tempo' : c === 'KM' ? 'Por Quilometragem' : 'Ambos'}
                        </span>
                      </label>
                    ))}
                  </div>
                </div>

                {(criterio === 'TEMPO' || criterio === 'AMBOS') && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Intervalo (Dias) *
                    </label>
                    <Controller
                      name="intervaloDias"
                      control={control}
                      render={({ field }) => (
                        <input
                          type="number"
                          value={field.value || ''}
                          onChange={(e) => field.onChange(e.target.value ? parseInt(e.target.value) : null)}
                          placeholder="Ex: 180"
                          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                        />
                      )}
                    />
                  </div>
                )}

                {(criterio === 'KM' || criterio === 'AMBOS') && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Intervalo (KM) *
                    </label>
                    <Controller
                      name="intervaloKm"
                      control={control}
                      render={({ field }) => (
                        <input
                          type="number"
                          value={field.value || ''}
                          onChange={(e) => field.onChange(e.target.value ? parseInt(e.target.value) : null)}
                          placeholder="Ex: 10000"
                          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                        />
                      )}
                    />
                  </div>
                )}

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Antecedência Alerta (Dias)
                    </label>
                    <Controller
                      name="antecedenciaDias"
                      control={control}
                      render={({ field }) => (
                        <input
                          type="number"
                          value={field.value}
                          onChange={(e) => field.onChange(parseInt(e.target.value) || 0)}
                          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                        />
                      )}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Antecedência Alerta (KM)
                    </label>
                    <Controller
                      name="antecedenciaKm"
                      control={control}
                      render={({ field }) => (
                        <input
                          type="number"
                          value={field.value}
                          onChange={(e) => field.onChange(parseInt(e.target.value) || 0)}
                          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                        />
                      )}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column */}
          <div className="space-y-6">
            {/* Última Execução */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                Última Manutenção (Referência Inicial)
              </h2>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Informe quando foi a última manutenção deste tipo para calcular a próxima previsão
              </p>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Data da Última Execução
                  </label>
                  <input
                    type="date"
                    {...register('ultimaExecucaoData')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    KM na Última Execução
                  </label>
                  <Controller
                    name="ultimaExecucaoKm"
                    control={control}
                    render={({ field }) => (
                      <input
                        type="number"
                        value={field.value || ''}
                        onChange={(e) => field.onChange(e.target.value ? parseInt(e.target.value) : null)}
                        placeholder="Ex: 45000"
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                      />
                    )}
                  />
                </div>
              </div>
            </div>

            {/* Notificações */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Canais de Notificação</h2>

              <Controller
                name="canaisNotificacao"
                control={control}
                render={({ field }) => (
                  <div className="space-y-2">
                    {['WHATSAPP', 'EMAIL', 'TELEGRAM', 'SMS', 'INTERNO'].map((canal) => (
                      <label key={canal} className="flex items-center gap-2 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={field.value.includes(canal)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              field.onChange([...field.value, canal]);
                            } else {
                              field.onChange(field.value.filter((c) => c !== canal));
                            }
                          }}
                          className="rounded border-gray-300 dark:border-gray-600 text-blue-600"
                        />
                        <span className="text-gray-900 dark:text-white">{canal}</span>
                      </label>
                    ))}
                  </div>
                )}
              />
            </div>

            {/* Agendamento de Notificações */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                Agendar Notificações
              </h2>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Configure até 2 notificações com data e hora específicas
              </p>

              <div className="space-y-4">
                {agendamentosNotificacao.map((agendamento, index) => (
                  <div
                    key={index}
                    className="flex flex-col sm:flex-row items-start sm:items-center gap-2 p-3 bg-gray-50 dark:bg-gray-700 rounded-lg"
                  >
                    <div className="flex-1 grid grid-cols-2 gap-2 w-full">
                      <div>
                        <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">
                          Data
                        </label>
                        <input
                          type="date"
                          value={agendamento.data}
                          onChange={(e) => {
                            const updated = [...agendamentosNotificacao];
                            updated[index] = { ...updated[index], data: e.target.value };
                            setAgendamentosNotificacao(updated);
                          }}
                          className="w-full px-2 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-500 dark:text-gray-400 mb-1">
                          Hora
                        </label>
                        <input
                          type="time"
                          value={agendamento.hora}
                          onChange={(e) => {
                            const updated = [...agendamentosNotificacao];
                            updated[index] = { ...updated[index], hora: e.target.value };
                            setAgendamentosNotificacao(updated);
                          }}
                          className="w-full px-2 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
                        />
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setAgendamentosNotificacao(agendamentosNotificacao.filter((_, i) => i !== index));
                      }}
                      className="p-2 text-red-500 hover:bg-red-100 dark:hover:bg-red-900/30 rounded-lg"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                    {agendamento.enviado && (
                      <span className="text-xs text-green-600 dark:text-green-400">
                        Enviado
                      </span>
                    )}
                  </div>
                ))}

                {agendamentosNotificacao.length < 2 && (
                  <button
                    type="button"
                    onClick={() => {
                      // Default: 7 days from now at 09:00
                      const defaultDate = new Date();
                      defaultDate.setDate(defaultDate.getDate() + 7);
                      const dataStr = defaultDate.toISOString().split('T')[0];
                      setAgendamentosNotificacao([
                        ...agendamentosNotificacao,
                        { data: dataStr, hora: '09:00', enviado: false }
                      ]);
                    }}
                    className="flex items-center gap-2 w-full justify-center px-3 py-2 border-2 border-dashed border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-400 rounded-lg hover:border-blue-400 hover:text-blue-600 dark:hover:border-blue-500 dark:hover:text-blue-400"
                  >
                    <Plus className="h-4 w-4" />
                    Adicionar Notificação Agendada
                  </button>
                )}

                {agendamentosNotificacao.length === 0 && (
                  <p className="text-sm text-gray-500 dark:text-gray-400 text-center py-2">
                    Nenhuma notificação agendada. As notificações serão enviadas automaticamente quando a manutenção estiver próxima ou vencida.
                  </p>
                )}
              </div>
            </div>

            {/* Checklist */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Checklist</h2>

              <div className="space-y-4">
                {/* Add new item */}
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={novoItem}
                    onChange={(e) => setNovoItem(e.target.value)}
                    placeholder="Novo item do checklist..."
                    className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                    onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addChecklistItem())}
                  />
                  <button
                    type="button"
                    onClick={addChecklistItem}
                    className="px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                  >
                    <Plus className="h-5 w-5" />
                  </button>
                </div>

                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={itemObrigatorio}
                    onChange={(e) => setItemObrigatorio(e.target.checked)}
                    className="rounded border-gray-300 dark:border-gray-600 text-blue-600"
                  />
                  <span className="text-sm text-gray-600 dark:text-gray-400">Item obrigatório</span>
                </label>

                {/* Checklist items */}
                {checklist.length > 0 ? (
                  <div className="space-y-2">
                    {checklist.map((item, index) => (
                      <div
                        key={index}
                        className="flex items-center justify-between p-2 bg-gray-50 dark:bg-gray-700 rounded-lg"
                      >
                        <div className="flex items-center gap-2">
                          <div className={`w-2 h-2 rounded-full ${item.obrigatorio ? 'bg-red-500' : 'bg-gray-400'}`} />
                          <span className="text-gray-900 dark:text-white">{item.item}</span>
                          {item.obrigatorio && (
                            <span className="text-xs text-red-500">(Obrigatório)</span>
                          )}
                        </div>
                        <button
                          type="button"
                          onClick={() => removeChecklistItem(index)}
                          className="p-1 text-red-500 hover:bg-red-100 dark:hover:bg-red-900/30 rounded"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-gray-500 dark:text-gray-400 text-center py-4">
                    Nenhum item no checklist
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Submit Buttons */}
        <div className="flex flex-col-reverse sm:flex-row justify-end gap-2 sm:gap-3">
          <Link
            to="/manutencao-preventiva"
            className="w-full sm:w-auto text-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Cancelar
          </Link>
          <button
            type="submit"
            disabled={criarMutation.isPending || atualizarMutation.isPending}
            className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            <Save className="h-4 w-4" />
            {criarMutation.isPending || atualizarMutation.isPending ? 'Salvando...' : 'Salvar Plano'}
          </button>
        </div>
      </form>
    </div>
  );
}
