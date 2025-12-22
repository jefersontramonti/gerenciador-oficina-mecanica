import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, Save, Search } from 'lucide-react';
import { useVeiculo, useCreateVeiculo, useUpdateVeiculo } from '../hooks/useVeiculos';
import { useClientes } from '@/features/clientes/hooks/useClientes';
import { createVeiculoSchema, updateVeiculoSchema } from '../utils/validation';
import { InputMask } from '@/shared/components/forms/InputMask';
import { showError } from '@/shared/utils/notifications';
import type { CreateVeiculoFormData, UpdateVeiculoFormData } from '../utils/validation';

const currentYear = new Date().getFullYear();
const anos = Array.from({ length: currentYear - 1949 }, (_, i) => currentYear - i + 1);

export const VeiculoFormPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEditMode = !!id;

  const [clienteSearch, setClienteSearch] = useState('');
  const [showClienteDropdown, setShowClienteDropdown] = useState(false);

  const { data: veiculo, isLoading: loadingVeiculo } = useVeiculo(id);
  const { data: clientesData } = useClientes({ nome: clienteSearch, size: 10 });
  const createMutation = useCreateVeiculo();
  const updateMutation = useUpdateVeiculo();

  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<CreateVeiculoFormData | UpdateVeiculoFormData>({
    resolver: zodResolver(isEditMode ? updateVeiculoSchema : createVeiculoSchema),
  });

  // Load veiculo data when editing
  useEffect(() => {
    if (veiculo && isEditMode) {
      setValue('marca', veiculo.marca);
      setValue('modelo', veiculo.modelo);
      setValue('ano', veiculo.ano);
      setValue('cor', veiculo.cor || '');
      setValue('chassi', veiculo.chassi || '');
      setValue('quilometragem', veiculo.quilometragem || undefined);
    }
  }, [veiculo, isEditMode, setValue]);

  const onSubmit = async (data: CreateVeiculoFormData | UpdateVeiculoFormData) => {
    try {
      if (isEditMode) {
        await updateMutation.mutateAsync({
          id: id!,
          data: data as UpdateVeiculoFormData,
        });
      } else {
        await createMutation.mutateAsync(data as CreateVeiculoFormData);
      }
      navigate('/veiculos');
    } catch (error: any) {
      if (error.response?.status === 409) {
        showError('Esta placa já está cadastrada.');
      } else if (error.response?.status === 404) {
        showError('Cliente não encontrado.');
      } else {
        showError(error.message || 'Erro ao salvar veículo');
      }
    }
  };

  const handleClienteSelect = (clienteId: string, clienteNome: string) => {
    setValue('clienteId' as any, clienteId);
    setClienteSearch(clienteNome);
    setShowClienteDropdown(false);
  };

  if (loadingVeiculo) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Carregando...</div>
      </div>
    );
  }

  const clientes = clientesData?.content || [];

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center gap-4">
        <button
          onClick={() => navigate('/veiculos')}
          className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          <ArrowLeft className="h-5 w-5 text-gray-900 dark:text-gray-100" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            {isEditMode ? 'Editar Veículo' : 'Novo Veículo'}
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {isEditMode
              ? 'Atualize os dados do veículo'
              : 'Preencha os dados para cadastrar um novo veículo'}
          </p>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="mx-auto max-w-4xl">
        <div className="space-y-6">
          {/* Cliente e Placa */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Identificação</h2>

            <div className="grid gap-4 md:grid-cols-2">
              {/* Cliente - Only on create */}
              {!isEditMode && (
                <div className="relative md:col-span-2">
                  <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Cliente <span className="text-red-500 dark:text-red-400">*</span>
                  </label>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400 dark:text-gray-500" />
                    <input
                      type="text"
                      value={clienteSearch}
                      onChange={(e) => {
                        setClienteSearch(e.target.value);
                        setShowClienteDropdown(true);
                      }}
                      onFocus={() => setShowClienteDropdown(true)}
                      placeholder="Buscar cliente por nome..."
                      className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 py-2 pl-10 pr-3 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                    />
                  </div>
                  {showClienteDropdown && clientes.length > 0 && (
                    <div className="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800 shadow-lg">
                      {clientes.map((cliente) => (
                        <button
                          key={cliente.id}
                          type="button"
                          onClick={() => handleClienteSelect(cliente.id, cliente.nome)}
                          className="w-full px-4 py-2 text-left hover:bg-gray-50 dark:hover:bg-gray-700"
                        >
                          <div className="font-medium text-gray-900 dark:text-gray-100">{cliente.nome}</div>
                          <div className="text-sm text-gray-500 dark:text-gray-400">{cliente.cpfCnpj}</div>
                        </button>
                      ))}
                    </div>
                  )}
                  {'clienteId' in errors && errors.clienteId && (
                    <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.clienteId.message}</p>
                  )}
                </div>
              )}

              {isEditMode && veiculo?.cliente && (
                <div className="md:col-span-2">
                  <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Proprietário
                  </label>
                  <div className="rounded-lg border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 p-3">
                    <div className="font-medium text-gray-900 dark:text-gray-100">{veiculo.cliente.nome}</div>
                    <div className="text-sm text-gray-500 dark:text-gray-400">{veiculo.cliente.cpfCnpj}</div>
                  </div>
                </div>
              )}

              {/* Placa - Only on create */}
              {!isEditMode && (
                <Controller
                  name="placa"
                  control={control}
                  render={({ field }) => (
                    <InputMask
                      {...field}
                      mask="placa"
                      label="Placa"
                      placeholder="ABC-1234 ou ABC1D23"
                      required
                      error={'placa' in errors ? errors.placa?.message : undefined}
                    />
                  )}
                />
              )}

              {isEditMode && veiculo && (
                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Placa</label>
                  <div className="rounded-lg border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 px-3 py-2 font-medium text-gray-900 dark:text-gray-100">
                    {veiculo.placa}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Dados do Veículo */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Dados do Veículo</h2>

            <div className="grid gap-4 md:grid-cols-2">
              {/* Marca */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Marca <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <input
                  {...register('marca')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Volkswagen, Fiat, Ford..."
                />
                {errors.marca && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.marca.message}</p>
                )}
              </div>

              {/* Modelo */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Modelo <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <input
                  {...register('modelo')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Gol, Uno, Ka..."
                />
                {errors.modelo && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.modelo.message}</p>
                )}
              </div>

              {/* Ano */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Ano <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <select
                  {...register('ano', { valueAsNumber: true })}
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                >
                  <option value="">Selecione o ano</option>
                  {anos.map((ano) => (
                    <option key={ano} value={ano}>
                      {ano}
                    </option>
                  ))}
                </select>
                {errors.ano && <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.ano.message}</p>}
              </div>

              {/* Cor */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Cor</label>
                <input
                  {...register('cor')}
                  type="text"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="Branco, Preto, Prata..."
                />
                {errors.cor && <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.cor.message}</p>}
              </div>

              {/* Chassi */}
              <div className="md:col-span-2">
                <Controller
                  name="chassi"
                  control={control}
                  render={({ field }) => (
                    <InputMask
                      {...field}
                      mask="chassi"
                      label="Chassi (VIN)"
                      placeholder="17 caracteres (sem I, O, Q)"
                      error={errors.chassi?.message}
                    />
                  )}
                />
              </div>

              {/* Quilometragem */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Quilometragem (km)
                </label>
                <input
                  {...register('quilometragem', { valueAsNumber: true })}
                  type="number"
                  min="0"
                  step="1"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  placeholder="0"
                />
                {errors.quilometragem && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.quilometragem.message}</p>
                )}
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-4">
            <button
              type="button"
              onClick={() => navigate('/veiculos')}
              className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-6 py-2 font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending || updateMutation.isPending}
              className="flex items-center gap-2 rounded-lg bg-blue-600 dark:bg-blue-700 px-6 py-2 font-medium text-white hover:bg-blue-700 dark:hover:bg-blue-600 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Save className="h-5 w-5" />
              {isEditMode ? 'Atualizar' : 'Cadastrar'}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};
