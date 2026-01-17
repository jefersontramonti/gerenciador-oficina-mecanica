import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Save, Search } from 'lucide-react';
import { useCriarAgendamento } from '../hooks/useManutencaoPreventiva';
import { api } from '@/shared/services/api';
import { showSuccess, showError } from '@/shared/utils/notifications';
import type { AgendamentoManutencaoRequest } from '../types';

const agendamentoSchema = z.object({
  veiculoId: z.string().min(1, 'Selecione um veículo'),
  clienteId: z.string().min(1, 'Selecione um cliente'),
  planoId: z.string().optional(),
  dataAgendamento: z.string().min(1, 'Informe a data'),
  horaAgendamento: z.string().min(1, 'Informe a hora'),
  duracaoEstimadaMinutos: z.number().min(15, 'Mínimo 15 minutos'),
  tipoManutencao: z.string().min(1, 'Informe o tipo de manutenção'),
  descricao: z.string().optional(),
  observacoes: z.string().optional(),
  observacoesInternas: z.string().optional(),
});

type AgendamentoFormData = z.infer<typeof agendamentoSchema>;

export default function AgendamentoFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = !!id;

  const [veiculoSearch, setVeiculoSearch] = useState('');
  const [veiculoResults, setVeiculoResults] = useState<any[]>([]);
  const [selectedVeiculo, setSelectedVeiculo] = useState<any>(null);
  const [searchLoading, setSearchLoading] = useState(false);

  const criarMutation = useCriarAgendamento();

  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<AgendamentoFormData>({
    resolver: zodResolver(agendamentoSchema),
    defaultValues: {
      duracaoEstimadaMinutos: 60,
    },
  });

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

  const onSubmit = async (data: AgendamentoFormData) => {
    try {
      const request: AgendamentoManutencaoRequest = {
        veiculoId: data.veiculoId,
        clienteId: data.clienteId,
        planoId: data.planoId || undefined,
        dataAgendamento: data.dataAgendamento,
        horaAgendamento: data.horaAgendamento,
        duracaoEstimadaMinutos: data.duracaoEstimadaMinutos,
        tipoManutencao: data.tipoManutencao,
        descricao: data.descricao || undefined,
        observacoes: data.observacoes || undefined,
        observacoesInternas: data.observacoesInternas || undefined,
      };

      await criarMutation.mutateAsync(request);
      showSuccess('Agendamento criado com sucesso');
      navigate('/manutencao-preventiva/agendamentos');
    } catch (error) {
      console.error('Erro ao salvar agendamento:', error);
      showError('Erro ao criar agendamento');
    }
  };

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3 sm:gap-4">
        <Link
          to="/manutencao-preventiva/agendamentos"
          className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          <ArrowLeft className="h-5 w-5 text-gray-500 dark:text-gray-400" />
        </Link>
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
            {isEditing ? 'Editar Agendamento' : 'Novo Agendamento'}
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 hidden sm:block">
            {isEditing ? 'Atualize as informações do agendamento' : 'Agende uma manutenção preventiva'}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
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
                      {selectedVeiculo.placaFormatada || selectedVeiculo.placa}
                    </p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {selectedVeiculo.marca} {selectedVeiculo.modelo}
                    </p>
                    {selectedVeiculo.cliente && (
                      <p className="text-xs text-gray-400 dark:text-gray-500">
                        Cliente: {selectedVeiculo.cliente.nome}
                      </p>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedVeiculo(null);
                      setValue('veiculoId', '');
                      setValue('clienteId', '');
                    }}
                    className="text-sm text-red-600 dark:text-red-400 hover:underline"
                  >
                    Alterar
                  </button>
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
                  <input type="hidden" {...register('clienteId')} />
                  {veiculoResults.length > 0 && (
                    <div className="mt-2 border border-gray-200 dark:border-gray-700 rounded-lg divide-y divide-gray-200 dark:divide-gray-700 max-h-60 overflow-y-auto">
                      {veiculoResults.map((veiculo: any) => (
                        <button
                          key={veiculo.id}
                          type="button"
                          onClick={() => {
                            setSelectedVeiculo(veiculo);
                            setValue('veiculoId', veiculo.id);
                            setValue('clienteId', veiculo.cliente?.id || veiculo.clienteId);
                            setVeiculoResults([]);
                            setVeiculoSearch('');
                          }}
                          className="w-full p-3 text-left hover:bg-gray-50 dark:hover:bg-gray-700"
                        >
                          <p className="font-medium text-gray-900 dark:text-white">
                            {veiculo.placaFormatada || veiculo.placa}
                          </p>
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            {veiculo.marca} {veiculo.modelo}
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

            {/* Data e Hora */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Data e Hora</h2>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Data *
                  </label>
                  <input
                    type="date"
                    {...register('dataAgendamento')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  />
                  {errors.dataAgendamento && (
                    <p className="mt-1 text-sm text-red-500">{errors.dataAgendamento.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Hora *
                  </label>
                  <input
                    type="time"
                    {...register('horaAgendamento')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  />
                  {errors.horaAgendamento && (
                    <p className="mt-1 text-sm text-red-500">{errors.horaAgendamento.message}</p>
                  )}
                </div>
              </div>

              <div className="mt-4">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Duração Estimada (minutos)
                </label>
                <Controller
                  name="duracaoEstimadaMinutos"
                  control={control}
                  render={({ field }) => (
                    <select
                      value={field.value}
                      onChange={(e) => field.onChange(parseInt(e.target.value))}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                    >
                      <option value={30}>30 minutos</option>
                      <option value={60}>1 hora</option>
                      <option value={90}>1h 30min</option>
                      <option value={120}>2 horas</option>
                      <option value={180}>3 horas</option>
                      <option value={240}>4 horas</option>
                      <option value={480}>8 horas (dia inteiro)</option>
                    </select>
                  )}
                />
              </div>
            </div>
          </div>

          {/* Right Column */}
          <div className="space-y-6">
            {/* Informações da Manutenção */}
            <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Manutenção</h2>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Tipo de Manutenção *
                  </label>
                  <select
                    {...register('tipoManutencao')}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  >
                    <option value="">Selecione...</option>
                    <option value="TROCA_OLEO">Troca de Óleo</option>
                    <option value="REVISAO">Revisão</option>
                    <option value="ALINHAMENTO">Alinhamento</option>
                    <option value="BALANCEAMENTO">Balanceamento</option>
                    <option value="FREIOS">Freios</option>
                    <option value="SUSPENSAO">Suspensão</option>
                    <option value="AR_CONDICIONADO">Ar Condicionado</option>
                    <option value="CORREIA_DENTADA">Correia Dentada</option>
                    <option value="FILTROS">Filtros</option>
                    <option value="OUTROS">Outros</option>
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
                    rows={2}
                    placeholder="Descrição do serviço a ser realizado..."
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Observações (visíveis ao cliente)
                  </label>
                  <textarea
                    {...register('observacoes')}
                    rows={2}
                    placeholder="Informações para o cliente..."
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Observações Internas
                  </label>
                  <textarea
                    {...register('observacoesInternas')}
                    rows={2}
                    placeholder="Notas internas da oficina..."
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Submit Buttons */}
        <div className="flex flex-col-reverse sm:flex-row justify-end gap-2 sm:gap-3">
          <Link
            to="/manutencao-preventiva/agendamentos"
            className="w-full sm:w-auto text-center px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Cancelar
          </Link>
          <button
            type="submit"
            disabled={criarMutation.isPending}
            className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            <Save className="h-4 w-4" />
            {criarMutation.isPending ? 'Salvando...' : 'Salvar Agendamento'}
          </button>
        </div>
      </form>
    </div>
  );
}
