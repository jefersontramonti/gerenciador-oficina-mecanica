/**
 * Página de formulário para criar/editar Ordem de Serviço
 * Suporta itens dinâmicos com cálculos automáticos
 */

import { useEffect, useState, useRef } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useForm, useFieldArray, Controller, type Resolver } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, Save, Plus, Trash2, Package, AlertCircle, Clock, DollarSign, Camera, X, Eye, Loader2 } from 'lucide-react';
import { showError, showSuccess } from '@/shared/utils/notifications';
import { useOrdemServico, useCreateOrdemServico, useUpdateOrdemServico } from '../hooks/useOrdensServico';
import { useOficina } from '@/features/configuracoes/hooks/useOficina';
import { ordemServicoFormSchema } from '../utils/validation';
import { canEdit } from '../utils/statusTransitions';
import { TipoItem, TipoCobrancaMaoObra, OrigemPeca } from '../types';
import type { OrdemServicoFormData } from '../utils/validation';
import { VeiculoAutocomplete } from '../components/VeiculoAutocomplete';
import { MecanicoSelect } from '../components/MecanicoSelect';
import { PecaAutocomplete } from '../components/PecaAutocomplete';
import { DiagnosticoIA } from '@/features/ia/components';
import { anexoService } from '@/features/anexos/services/anexoService';
import type { CategoriaAnexo } from '@/features/anexos/types';

/**
 * Interface para arquivos pendentes de upload
 */
interface PendingFile {
  id: string;
  file: File;
  preview: string;
  categoria: CategoriaAnexo;
}

export const OrdemServicoFormPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();

  // Previne que "novo" seja tratado como ID válido
  const actualId = id === 'novo' ? undefined : id;
  const isEditMode = !!actualId;

  const { data: ordemServico, isLoading: loadingOS } = useOrdemServico(actualId);
  const { data: oficina } = useOficina();
  const createMutation = useCreateOrdemServico();
  const updateMutation = useUpdateOrdemServico();

  // Estado para arquivos pendentes de upload (apenas para criação)
  const [pendingFiles, setPendingFiles] = useState<PendingFile[]>([]);
  const [uploadingFiles, setUploadingFiles] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<OrdemServicoFormData>({
    resolver: zodResolver(ordemServicoFormSchema) as Resolver<OrdemServicoFormData>,
    defaultValues: {
      veiculoId: undefined as unknown as string,
      usuarioId: undefined as unknown as string,
      problemasRelatados: '',
      dataAbertura: new Date().toISOString().split('T')[0],
      diagnostico: '',
      observacoes: '',
      dataPrevisao: '',
      // Modelo híbrido de mão de obra
      tipoCobrancaMaoObra: TipoCobrancaMaoObra.VALOR_FIXO,
      valorMaoObra: 0,
      tempoEstimadoHoras: undefined,
      limiteHorasAprovado: undefined,
      valorPecas: 0,
      valorTotal: 0,
      descontoPercentual: 0,
      descontoValor: 0,
      valorFinal: 0,
      itens: [],
    },
  });

  // Log de erros de validação
  useEffect(() => {
    if (Object.keys(errors).length > 0) {
      console.log('[OrdemServicoForm] Erros de validação:', errors);
      // Log detalhado do veiculoId
      const currentVeiculoId = watch('veiculoId');
      console.log('[OrdemServicoForm] Valor atual de veiculoId:', currentVeiculoId);
      console.log('[OrdemServicoForm] Tipo de veiculoId:', typeof currentVeiculoId);
      console.log('[OrdemServicoForm] É string?', typeof currentVeiculoId === 'string');
      console.log('[OrdemServicoForm] Comprimento:', currentVeiculoId?.length);
      console.log('[OrdemServicoForm] Tem espaços?', currentVeiculoId?.includes(' '));
      console.log('[OrdemServicoForm] Código char primeiro:', currentVeiculoId?.charCodeAt(0));
      console.log('[OrdemServicoForm] Código char último:', currentVeiculoId?.charCodeAt(currentVeiculoId.length - 1));
      // Teste manual de UUID regex
      const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
      console.log('[OrdemServicoForm] Match UUID regex?', uuidRegex.test(currentVeiculoId || ''));
    }
  }, [errors, watch]);

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'itens',
  });

  // Watch para cálculos automáticos - observa todo o formulário
  const formValues = watch();
  const itens = formValues.itens || [];
  const tipoCobrancaMaoObra = formValues.tipoCobrancaMaoObra || TipoCobrancaMaoObra.VALOR_FIXO;
  const valorMaoObra = Number(formValues.valorMaoObra) || 0;
  const tempoEstimadoHoras = Number(formValues.tempoEstimadoHoras) || 0;
  const limiteHorasAprovado = Number(formValues.limiteHorasAprovado) || 0;
  const descontoPercentual = Number(formValues.descontoPercentual) || 0;
  const descontoValor = Number(formValues.descontoValor) || 0;

  // Valor/hora da oficina (busca da configuração da oficina)
  const valorHoraOficina = oficina?.valorHora ?? 80; // Default 80 se não configurado

  // Cálculos automáticos de valores
  useEffect(() => {
    // Calcular valor de peças
    const valorPecas = itens
      .filter((item) => item.tipo === TipoItem.PECA)
      .reduce((sum, item) => {
        const valorItem = (Number(item.quantidade) || 0) * (Number(item.valorUnitario) || 0) - (Number(item.desconto) || 0);
        return sum + valorItem;
      }, 0);

    // Calcular valor de serviços
    const valorServicos = itens
      .filter((item) => item.tipo === TipoItem.SERVICO)
      .reduce((sum, item) => {
        const valorItem = (Number(item.quantidade) || 0) * (Number(item.valorUnitario) || 0) - (Number(item.desconto) || 0);
        return sum + valorItem;
      }, 0);

    // Calcular valor da mão de obra (baseado no modelo)
    let valorMaoObraCalculado = 0;
    if (tipoCobrancaMaoObra === TipoCobrancaMaoObra.VALOR_FIXO) {
      valorMaoObraCalculado = valorMaoObra;
    } else {
      // POR_HORA: usar tempo estimado para exibição no orçamento
      valorMaoObraCalculado = tempoEstimadoHoras * valorHoraOficina;
    }

    // Calcular valor total (mão de obra + peças + serviços)
    const valorTotal = valorMaoObraCalculado + valorPecas + valorServicos;

    // Calcular desconto (prioriza percentual)
    const desconto = descontoPercentual > 0
      ? (valorTotal * descontoPercentual) / 100
      : descontoValor;

    // Calcular valor final
    const valorFinal = Math.max(0, valorTotal - desconto);

    // Atualizar campos calculados apenas se mudaram (evita loop)
    if (formValues.valorPecas !== valorPecas) {
      setValue('valorPecas', valorPecas, { shouldValidate: false });
    }
    if (formValues.valorTotal !== valorTotal) {
      setValue('valorTotal', valorTotal, { shouldValidate: false });
    }
    if (formValues.valorFinal !== valorFinal) {
      setValue('valorFinal', valorFinal, { shouldValidate: false });
    }

    // Se descontoPercentual mudou, zerar descontoValor
    if (descontoPercentual > 0 && descontoValor > 0) {
      setValue('descontoValor', 0, { shouldValidate: false });
    }
  }, [formValues, setValue, tipoCobrancaMaoObra, tempoEstimadoHoras, valorHoraOficina]);

  // Calcular valorTotal de cada item
  useEffect(() => {
    itens.forEach((item, index) => {
      const valorItem =
        (Number(item.quantidade) || 0) * (Number(item.valorUnitario) || 0) - (Number(item.desconto) || 0);
      const valorCalculado = Math.max(0, valorItem);

      if (item.valorTotal !== valorCalculado) {
        setValue(`itens.${index}.valorTotal`, valorCalculado, { shouldValidate: false });
      }
    });
  }, [formValues, setValue, itens]);

  // Pre-select vehicle from URL parameter (only when creating new OS)
  useEffect(() => {
    if (!isEditMode) {
      const veiculoId = searchParams.get('veiculoId');
      console.log('[OrdemServicoForm] VeiculoId da URL:', veiculoId);
      if (veiculoId) {
        console.log('[OrdemServicoForm] Definindo veiculoId no form:', veiculoId);
        setValue('veiculoId', veiculoId, { shouldValidate: true });
      }
    }
  }, [isEditMode, searchParams, setValue]);

  // Load OS data when editing
  useEffect(() => {
    if (ordemServico && isEditMode) {
      // Verificar se pode editar
      if (!canEdit(ordemServico.status)) {
        showError('Esta OS não pode ser editada neste status.');
        navigate(`/ordens-servico/${id}`);
        return;
      }

      if (ordemServico.veiculoId) {
        setValue('veiculoId', ordemServico.veiculoId);
      }
      if (ordemServico.usuarioId) {
        setValue('usuarioId', ordemServico.usuarioId);
      }
      setValue('problemasRelatados', ordemServico.problemasRelatados);
      setValue('diagnostico', ordemServico.diagnostico || '');
      setValue('observacoes', ordemServico.observacoes || '');
      // Modelo híbrido de mão de obra
      setValue('tipoCobrancaMaoObra', ordemServico.tipoCobrancaMaoObra || TipoCobrancaMaoObra.VALOR_FIXO);
      setValue('valorMaoObra', ordemServico.valorMaoObra);
      if (ordemServico.tempoEstimadoHoras) {
        setValue('tempoEstimadoHoras', ordemServico.tempoEstimadoHoras);
      }
      if (ordemServico.limiteHorasAprovado) {
        setValue('limiteHorasAprovado', ordemServico.limiteHorasAprovado);
      }
      setValue('descontoPercentual', ordemServico.descontoPercentual);
      setValue('descontoValor', ordemServico.descontoValor);

      // Data de abertura (formato YYYY-MM-DD)
      if (ordemServico.dataAbertura) {
        const dataAbertura = Array.isArray(ordemServico.dataAbertura)
          ? new Date(ordemServico.dataAbertura[0], ordemServico.dataAbertura[1] - 1, ordemServico.dataAbertura[2])
          : new Date(ordemServico.dataAbertura);
        setValue('dataAbertura', dataAbertura.toISOString().split('T')[0]);
      }

      // Data de previsão
      if (ordemServico.dataPrevisao) {
        const dataPrevisao = Array.isArray(ordemServico.dataPrevisao)
          ? new Date(ordemServico.dataPrevisao[0], ordemServico.dataPrevisao[1] - 1, ordemServico.dataPrevisao[2])
          : new Date(ordemServico.dataPrevisao);
        setValue('dataPrevisao', dataPrevisao.toISOString().split('T')[0]);
      }

      // Itens
      if (ordemServico.itens && ordemServico.itens.length > 0) {
        setValue('itens', ordemServico.itens);
      }
    }
  }, [ordemServico, isEditMode, setValue, navigate, id]);

  const onSubmit = async (data: OrdemServicoFormData) => {
    console.log('[OrdemServicoForm] Iniciando submit...');
    console.log('[OrdemServicoForm] Dados do formulário:', data);
    console.log('[OrdemServicoForm] veiculoId tipo:', typeof data.veiculoId);
    console.log('[OrdemServicoForm] veiculoId valor:', data.veiculoId);
    console.log('[OrdemServicoForm] veiculoId é string?', typeof data.veiculoId === 'string');

    try {
      const payload = {
        veiculoId: data.veiculoId,
        usuarioId: data.usuarioId,
        problemasRelatados: data.problemasRelatados,
        diagnostico: data.diagnostico || undefined,
        observacoes: data.observacoes || undefined,
        dataPrevisao: data.dataPrevisao || undefined,
        // Modelo híbrido de mão de obra
        tipoCobrancaMaoObra: data.tipoCobrancaMaoObra,
        valorMaoObra: data.tipoCobrancaMaoObra === TipoCobrancaMaoObra.VALOR_FIXO ? data.valorMaoObra : undefined,
        tempoEstimadoHoras: data.tipoCobrancaMaoObra === TipoCobrancaMaoObra.POR_HORA ? data.tempoEstimadoHoras : undefined,
        limiteHorasAprovado: data.tipoCobrancaMaoObra === TipoCobrancaMaoObra.POR_HORA ? data.limiteHorasAprovado : undefined,
        descontoPercentual: data.descontoPercentual || 0,
        descontoValor: data.descontoValor || 0,
        itens: data.itens.map((item) => ({
          tipo: item.tipo,
          origemPeca: item.tipo === TipoItem.PECA ? (item.origemPeca || OrigemPeca.ESTOQUE) : undefined,
          pecaId: item.pecaId || undefined,
          descricao: item.descricao,
          quantidade: item.quantidade,
          valorUnitario: item.valorUnitario,
          desconto: item.desconto || 0,
        })),
      };

      console.log('[OrdemServicoForm] Payload para envio:', payload);

      if (isEditMode) {
        await updateMutation.mutateAsync({
          id: id!,
          data: payload,
        });
        navigate('/ordens-servico');
      } else {
        // Criar a OS primeiro
        const novaOS = await createMutation.mutateAsync(payload);

        // Se há arquivos pendentes, fazer upload
        if (pendingFiles.length > 0) {
          setUploadingFiles(true);

          try {
            // Fazer upload de todos os arquivos
            for (const pendingFile of pendingFiles) {
              try {
                // Upload do arquivo
                const anexoResponse = await anexoService.upload({
                  file: pendingFile.file,
                  entidadeTipo: 'ORDEM_SERVICO',
                  entidadeId: novaOS.id,
                  categoria: pendingFile.categoria,
                });

                // Marcar como visível para cliente
                await anexoService.alterarVisibilidade(anexoResponse.id, {
                  visivelParaCliente: true,
                });
              } catch (uploadError) {
                console.error('Erro ao fazer upload de arquivo:', uploadError);
                // Continua com os outros arquivos mesmo se um falhar
              }
            }

            showSuccess(`OS criada com ${pendingFiles.length} anexo(s)!`);
          } catch (uploadError) {
            console.error('Erro geral no upload de arquivos:', uploadError);
            showError('OS criada, mas houve erro ao enviar alguns anexos.');
          } finally {
            setUploadingFiles(false);
          }
        }

        navigate('/ordens-servico');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Erro ao salvar OS';
      showError(`Erro: ${errorMessage}`);
    }
  };

  const handleAddItem = () => {
    append({
      tipo: TipoItem.SERVICO,
      descricao: '',
      quantidade: 1,
      valorUnitario: 0,
      desconto: 0,
      valorTotal: 0,
    });
  };

  // Manipuladores de arquivos para anexos
  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files) return;

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];
    const maxSize = 5 * 1024 * 1024; // 5MB

    const newFiles: PendingFile[] = [];

    Array.from(files).forEach((file) => {
      // Validar tipo
      if (!allowedTypes.includes(file.type)) {
        showError(`Tipo de arquivo não suportado: ${file.name}`);
        return;
      }

      // Validar tamanho
      if (file.size > maxSize) {
        showError(`Arquivo muito grande (máx 5MB): ${file.name}`);
        return;
      }

      // Criar preview para imagens
      const preview = file.type.startsWith('image/')
        ? URL.createObjectURL(file)
        : '';

      newFiles.push({
        id: crypto.randomUUID(),
        file,
        preview,
        categoria: 'FOTO_VEICULO' as CategoriaAnexo,
      });
    });

    setPendingFiles((prev) => [...prev, ...newFiles]);

    // Limpar input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleRemoveFile = (fileId: string) => {
    setPendingFiles((prev) => {
      const file = prev.find((f) => f.id === fileId);
      if (file?.preview) {
        URL.revokeObjectURL(file.preview);
      }
      return prev.filter((f) => f.id !== fileId);
    });
  };

  const handleCategoriaChange = (fileId: string, categoria: CategoriaAnexo) => {
    setPendingFiles((prev) =>
      prev.map((f) => (f.id === fileId ? { ...f, categoria } : f))
    );
  };

  // Cleanup previews on unmount
  useEffect(() => {
    return () => {
      pendingFiles.forEach((f) => {
        if (f.preview) URL.revokeObjectURL(f.preview);
      });
    };
  }, []);

  if (loadingOS) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-gray-600 dark:text-gray-400">Carregando...</p>
      </div>
    );
  }

  const isSubmitting = createMutation.isPending || updateMutation.isPending || uploadingFiles;

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-6 flex items-start gap-3 sm:gap-4">
        <button
          type="button"
          onClick={() => navigate('/ordens-servico')}
          className="rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 p-2 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 shrink-0"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div className="min-w-0">
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">
            {isEditMode ? 'Editar OS' : 'Nova OS'}
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400 hidden sm:block">
            {isEditMode
              ? 'Atualize os dados da ordem de serviço'
              : 'Preencha os dados para criar uma nova OS'}
          </p>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Validation Error Summary */}
        {Object.keys(errors).length > 0 && (
          <div className="rounded-lg border-2 border-red-500 dark:border-red-700 bg-red-50 dark:bg-red-900/30 p-4">
            <div className="flex items-start gap-3">
              <AlertCircle className="h-5 w-5 flex-shrink-0 text-red-600 dark:text-red-400" />
              <div className="flex-1">
                <h3 className="font-semibold text-red-900 dark:text-red-300">
                  Corrija os seguintes erros antes de salvar:
                </h3>
                <ul className="mt-2 list-inside list-disc space-y-1 text-sm text-red-800 dark:text-red-400">
                  {errors.veiculoId && <li>{errors.veiculoId.message}</li>}
                  {errors.usuarioId && <li>{errors.usuarioId.message}</li>}
                  {errors.problemasRelatados && <li>{errors.problemasRelatados.message}</li>}
                  {errors.diagnostico && <li>{errors.diagnostico.message}</li>}
                  {errors.observacoes && <li>{errors.observacoes.message}</li>}
                  {errors.dataAbertura && <li>{errors.dataAbertura.message}</li>}
                  {errors.dataPrevisao && <li>{errors.dataPrevisao.message}</li>}
                  {errors.valorMaoObra && <li>{errors.valorMaoObra.message}</li>}
                  {errors.descontoPercentual && <li>{errors.descontoPercentual.message}</li>}
                  {errors.descontoValor && <li>{errors.descontoValor.message}</li>}
                  {errors.itens && typeof errors.itens === 'object' && 'message' in errors.itens && (
                    <li>{errors.itens.message as string}</li>
                  )}
                  {errors.itens && Array.isArray(errors.itens) && errors.itens.some(item => item) && (
                    <li>Há erros em um ou mais itens - verifique os campos destacados</li>
                  )}
                </ul>
              </div>
            </div>
          </div>
        )}

        {/* Seção: Veículo e Mecânico */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Veículo e Mecânico</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            {/* Veículo - Autocomplete */}
            <Controller
              name="veiculoId"
              control={control}
              render={({ field }) => {
                console.log('[Controller veiculoId] Field value:', field.value);
                return (
                  <VeiculoAutocomplete
                    value={field.value}
                    onChange={(veiculoId) => {
                      console.log('[Controller veiculoId] onChange chamado com:', veiculoId);
                      field.onChange(veiculoId);
                    }}
                    error={errors.veiculoId?.message}
                    required
                  />
                );
              }}
            />

            {/* Mecânico - Select */}
            <Controller
              name="usuarioId"
              control={control}
              render={({ field }) => (
                <MecanicoSelect
                  value={field.value}
                  onChange={field.onChange}
                  error={errors.usuarioId?.message}
                  required
                />
              )}
            />
          </div>
        </div>

        {/* Seção: Problemas e Diagnóstico */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
            Problemas e Diagnóstico
          </h2>
          <div className="space-y-4">
            {/* Problemas Relatados */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Problemas Relatados <span className="text-red-500 dark:text-red-400">*</span>
              </label>
              <textarea
                {...register('problemasRelatados')}
                rows={3}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                placeholder="Descreva os problemas relatados pelo cliente..."
              />
              {errors.problemasRelatados && (
                <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.problemasRelatados.message}</p>
              )}
            </div>

            {/* Diagnóstico por IA */}
            <DiagnosticoIA
              veiculoId={watch('veiculoId')}
              problemasRelatados={watch('problemasRelatados')}
              onUsarDiagnostico={(diagnostico) => {
                setValue('diagnostico', diagnostico);
              }}
            />

            {/* Diagnóstico */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Diagnóstico</label>
              <textarea
                {...register('diagnostico')}
                rows={3}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                placeholder="Diagnóstico técnico (opcional)..."
              />
              {errors.diagnostico && (
                <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.diagnostico.message}</p>
              )}
            </div>

            {/* Observações */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Observações</label>
              <textarea
                {...register('observacoes')}
                rows={2}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                placeholder="Observações adicionais (opcional)..."
              />
              {errors.observacoes && (
                <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.observacoes.message}</p>
              )}
            </div>
          </div>
        </div>

        {/* Seção: Fotos e Documentos (apenas para criação) */}
        {!isEditMode && (
          <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Camera className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                  Fotos e Documentos
                </h2>
              </div>
              <div className="flex items-center gap-2">
                <Eye className="h-4 w-4 text-green-600 dark:text-green-400" />
                <span className="text-xs text-gray-500 dark:text-gray-400">
                  Visíveis para o cliente
                </span>
              </div>
            </div>

            <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
              Adicione fotos do veículo ou documentos que serão enviados junto com o orçamento para o cliente.
            </p>

            {/* Input de arquivo oculto */}
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileSelect}
              accept="image/jpeg,image/png,image/webp,application/pdf"
              multiple
              className="hidden"
            />

            {/* Botão de adicionar */}
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="flex items-center gap-2 rounded-lg border-2 border-dashed border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-700/50 px-4 py-3 text-gray-600 dark:text-gray-400 hover:border-blue-500 dark:hover:border-blue-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors w-full justify-center"
            >
              <Plus className="h-5 w-5" />
              <span>Adicionar fotos ou documentos</span>
            </button>

            {/* Preview dos arquivos selecionados */}
            {pendingFiles.length > 0 && (
              <div className="mt-4 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                {pendingFiles.map((pf) => (
                  <div
                    key={pf.id}
                    className="relative bg-gray-100 dark:bg-gray-700 rounded-lg overflow-hidden border border-gray-200 dark:border-gray-600"
                  >
                    {/* Preview da imagem ou ícone de PDF */}
                    <div className="aspect-square flex items-center justify-center">
                      {pf.preview ? (
                        <img
                          src={pf.preview}
                          alt={pf.file.name}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="text-center p-4">
                          <div className="text-3xl mb-1">📄</div>
                          <p className="text-xs text-gray-500 dark:text-gray-400 truncate px-2">
                            {pf.file.name}
                          </p>
                        </div>
                      )}
                    </div>

                    {/* Botão remover */}
                    <button
                      type="button"
                      onClick={() => handleRemoveFile(pf.id)}
                      className="absolute top-1 right-1 p-1 bg-red-500 text-white rounded-full hover:bg-red-600"
                    >
                      <X className="h-3 w-3" />
                    </button>

                    {/* Seletor de categoria */}
                    <div className="p-2 bg-white dark:bg-gray-800">
                      <select
                        value={pf.categoria}
                        onChange={(e) => handleCategoriaChange(pf.id, e.target.value as CategoriaAnexo)}
                        className="w-full text-xs rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-2 py-1"
                      >
                        <option value="FOTO_VEICULO">Foto do Veículo</option>
                        <option value="DIAGNOSTICO">Diagnóstico</option>
                        <option value="AUTORIZACAO">Autorização</option>
                        <option value="LAUDO_TECNICO">Laudo Técnico</option>
                        <option value="OUTROS">Outros</option>
                      </select>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {pendingFiles.length > 0 && (
              <p className="mt-3 text-sm text-green-600 dark:text-green-400">
                {pendingFiles.length} arquivo(s) selecionado(s) - serão enviados ao salvar a OS
              </p>
            )}
          </div>
        )}

        {/* Seção: Datas */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Datas</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            {/* Data de Abertura */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Data de Abertura <span className="text-red-500 dark:text-red-400">*</span>
              </label>
              <input
                type="date"
                {...register('dataAbertura')}
                disabled={isEditMode}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:bg-gray-100 dark:disabled:bg-gray-600"
              />
              {errors.dataAbertura && (
                <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.dataAbertura.message}</p>
              )}
            </div>

            {/* Data de Previsão */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Previsão de Entrega
              </label>
              <input
                type="date"
                {...register('dataPrevisao')}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              {errors.dataPrevisao && (
                <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.dataPrevisao.message}</p>
              )}
            </div>
          </div>
        </div>

        {/* Seção: Itens de Serviço */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Itens de Serviço</h2>
            <button
              type="button"
              onClick={handleAddItem}
              className="flex items-center gap-2 rounded-lg bg-green-600 dark:bg-green-700 px-4 py-2 text-sm text-white hover:bg-green-700 dark:hover:bg-green-600"
            >
              <Plus className="h-4 w-4" />
              Adicionar Item
            </button>
          </div>

          {errors.itens && typeof errors.itens === 'object' && 'message' in errors.itens && (
            <p className="mb-4 text-sm text-red-500 dark:text-red-400">{errors.itens.message as string}</p>
          )}

          <div className="space-y-4">
            {fields.map((field, index) => (
              <div key={field.id} className="rounded-lg border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Item #{index + 1}</span>
                  <button
                    type="button"
                    onClick={() => remove(index)}
                    className="rounded p-1 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>

                <div className="grid gap-3 sm:gap-4 grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6">
                  {/* Tipo */}
                  <div className="col-span-1">
                    <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Tipo</label>
                    <select
                      {...register(`itens.${index}.tipo`)}
                      onChange={(e) => {
                        setValue(`itens.${index}.tipo`, e.target.value as TipoItem);
                        // Limpar campos relacionados ao mudar tipo
                        if (e.target.value === TipoItem.SERVICO) {
                          setValue(`itens.${index}.origemPeca`, undefined);
                          setValue(`itens.${index}.pecaId`, undefined);
                        } else {
                          // Default para ESTOQUE quando muda para PECA
                          setValue(`itens.${index}.origemPeca`, OrigemPeca.ESTOQUE);
                        }
                      }}
                      className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                    >
                      <option value={TipoItem.SERVICO}>Serviço</option>
                      <option value={TipoItem.PECA}>Peça</option>
                    </select>
                  </div>

                  {/* Origem da Peça (apenas se tipo = PECA) */}
                  {watch(`itens.${index}.tipo`) === TipoItem.PECA && (
                    <div className="col-span-1">
                      <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Origem</label>
                      <select
                        {...register(`itens.${index}.origemPeca`)}
                        onChange={(e) => {
                          const novaOrigem = e.target.value as OrigemPeca;
                          setValue(`itens.${index}.origemPeca`, novaOrigem, { shouldValidate: true });
                          // Limpar pecaId se não for do estoque
                          if (novaOrigem !== OrigemPeca.ESTOQUE) {
                            setValue(`itens.${index}.pecaId`, undefined);
                            setValue(`itens.${index}.descricao`, '');
                            setValue(`itens.${index}.valorUnitario`, 0);
                          }
                          // Se for do cliente, zerar valor
                          if (novaOrigem === OrigemPeca.CLIENTE) {
                            setValue(`itens.${index}.valorUnitario`, 0);
                          }
                        }}
                        className={`w-full rounded-lg border ${errors.itens?.[index]?.origemPeca ? 'border-red-500 dark:border-red-400' : 'border-gray-300 dark:border-gray-600'} bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20`}
                      >
                        <option value={OrigemPeca.ESTOQUE}>Estoque</option>
                        <option value={OrigemPeca.AVULSA}>Avulsa</option>
                        <option value={OrigemPeca.CLIENTE}>Cliente</option>
                      </select>
                      {errors.itens?.[index]?.origemPeca ? (
                        <p className="mt-1 text-sm text-red-500 dark:text-red-400">
                          {errors.itens[index]?.origemPeca?.message}
                        </p>
                      ) : (
                        <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                          {watch(`itens.${index}.origemPeca`) === OrigemPeca.ESTOQUE && 'Do inventário'}
                          {watch(`itens.${index}.origemPeca`) === OrigemPeca.AVULSA && 'Compra externa'}
                          {watch(`itens.${index}.origemPeca`) === OrigemPeca.CLIENTE && 'Cliente trouxe'}
                        </p>
                      )}
                    </div>
                  )}

                  {/* Descrição ou Seleção de Peça */}
                  <div className={watch(`itens.${index}.tipo`) === TipoItem.PECA ? 'col-span-1' : 'col-span-1 md:col-span-2 lg:col-span-2'}>
                    {watch(`itens.${index}.tipo`) === TipoItem.PECA && watch(`itens.${index}.origemPeca`) === OrigemPeca.ESTOQUE ? (
                      <Controller
                        name={`itens.${index}.pecaId`}
                        control={control}
                        render={({ field }) => (
                          <PecaAutocomplete
                            value={field.value || ''}
                            onChange={(pecaId, valorVenda, descricao) => {
                              field.onChange(pecaId);
                              // Preencher automaticamente o valor unitário com o valor de venda
                              setValue(`itens.${index}.valorUnitario`, valorVenda);
                              // Preencher automaticamente a descrição
                              setValue(`itens.${index}.descricao`, descricao);
                            }}
                            error={errors.itens?.[index]?.pecaId?.message}
                            required
                          />
                        )}
                      />
                    ) : watch(`itens.${index}.tipo`) === TipoItem.PECA ? (
                      <>
                        <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                          Descrição da Peça <span className="text-red-500">*</span>
                        </label>
                        <input
                          type="text"
                          {...register(`itens.${index}.descricao`)}
                          className={`w-full rounded-lg border ${errors.itens?.[index]?.descricao ? 'border-red-500 dark:border-red-400' : 'border-gray-300 dark:border-gray-600'} bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20`}
                          placeholder="Descreva a peça (mín. 10 caracteres)"
                        />
                        {errors.itens?.[index]?.descricao ? (
                          <p className="mt-1 text-sm text-red-500 dark:text-red-400">
                            {errors.itens[index]?.descricao?.message}
                          </p>
                        ) : (
                          <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                            Mínimo 10 caracteres para peças avulsas/cliente
                          </p>
                        )}
                      </>
                    ) : (
                      <>
                        <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                          Descrição
                        </label>
                        <input
                          type="text"
                          {...register(`itens.${index}.descricao`)}
                          className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                          placeholder="Descrição do serviço"
                        />
                        {errors.itens?.[index]?.descricao && (
                          <p className="mt-1 text-sm text-red-500 dark:text-red-400">
                            {errors.itens[index]?.descricao?.message}
                          </p>
                        )}
                      </>
                    )}
                  </div>

                  {/* Quantidade */}
                  <div className="col-span-1">
                    <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Qtd.</label>
                    <input
                      type="number"
                      {...register(`itens.${index}.quantidade`, { valueAsNumber: true })}
                      className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                      min="1"
                    />
                  </div>

                  {/* Valor Unitário */}
                  <div className="col-span-1">
                    <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                      Valor Unit.
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      {...register(`itens.${index}.valorUnitario`, { valueAsNumber: true })}
                      className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                      min="0"
                    />
                  </div>

                  {/* Desconto */}
                  <div className="col-span-1">
                    <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                      Desconto
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      {...register(`itens.${index}.desconto`, { valueAsNumber: true })}
                      className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                      min="0"
                    />
                  </div>
                </div>
              </div>
            ))}

            {fields.length === 0 && (
              <div className="rounded-lg border border-dashed border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-700/30 p-8 text-center">
                <Package className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-500" />
                <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
                  Nenhum item adicionado. Clique em "Adicionar Item" para começar.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Seção: Valores Financeiros */}
        <div className="rounded-lg bg-white dark:bg-gray-800 p-4 sm:p-6 shadow">
          <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Valores Financeiros</h2>
          <div className="space-y-4">
            {/* Tipo de Cobrança de Mão de Obra */}
            <div>
              <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Tipo de Cobrança de Mão de Obra
              </label>
              <div className="flex flex-col gap-3 sm:flex-row sm:gap-4">
                <label className={`flex cursor-pointer items-center gap-3 rounded-lg border-2 p-3 sm:p-4 transition-colors ${
                  tipoCobrancaMaoObra === TipoCobrancaMaoObra.VALOR_FIXO
                    ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                    : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 hover:border-gray-400 dark:hover:border-gray-500'
                }`}>
                  <input
                    type="radio"
                    {...register('tipoCobrancaMaoObra')}
                    value={TipoCobrancaMaoObra.VALOR_FIXO}
                    className="h-4 w-4 text-blue-600"
                  />
                  <div className="flex items-center gap-2">
                    <DollarSign className="h-5 w-5 text-gray-600 dark:text-gray-400" />
                    <div>
                      <p className="font-medium text-gray-900 dark:text-gray-100">Valor Fixo</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">Mão de obra com valor definido</p>
                    </div>
                  </div>
                </label>

                <label className={`flex cursor-pointer items-center gap-3 rounded-lg border-2 p-3 sm:p-4 transition-colors ${
                  tipoCobrancaMaoObra === TipoCobrancaMaoObra.POR_HORA
                    ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                    : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 hover:border-gray-400 dark:hover:border-gray-500'
                }`}>
                  <input
                    type="radio"
                    {...register('tipoCobrancaMaoObra')}
                    value={TipoCobrancaMaoObra.POR_HORA}
                    className="h-4 w-4 text-blue-600"
                  />
                  <div className="flex items-center gap-2">
                    <Clock className="h-5 w-5 text-gray-600 dark:text-gray-400" />
                    <div>
                      <p className="font-medium text-gray-900 dark:text-gray-100">Por Hora</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">Calculado por horas trabalhadas</p>
                    </div>
                  </div>
                </label>
              </div>
            </div>

            {/* Campos de Mão de Obra */}
            <div className="grid gap-4 sm:grid-cols-2">
              {tipoCobrancaMaoObra === TipoCobrancaMaoObra.VALOR_FIXO ? (
                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Valor Mão de Obra (R$) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    {...register('valorMaoObra', { valueAsNumber: true })}
                    className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                    min="0"
                  />
                  {errors.valorMaoObra && (
                    <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.valorMaoObra.message}</p>
                  )}
                </div>
              ) : (
                <>
                  <div>
                    <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                      Tempo Estimado (horas) <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="number"
                      step="0.5"
                      {...register('tempoEstimadoHoras', { valueAsNumber: true })}
                      className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                      min="0.5"
                      max="100"
                    />
                    {errors.tempoEstimadoHoras && (
                      <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.tempoEstimadoHoras.message}</p>
                    )}
                  </div>

                  <div>
                    <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                      Limite de Horas Aprovado <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="number"
                      step="0.5"
                      {...register('limiteHorasAprovado', { valueAsNumber: true })}
                      className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                      min="0.5"
                      max="100"
                    />
                    {errors.limiteHorasAprovado && (
                      <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.limiteHorasAprovado.message}</p>
                    )}
                  </div>
                </>
              )}

              {/* Valor Peças (calculado) */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Valor Peças (R$)
                </label>
                <input
                  type="text"
                  value={new Intl.NumberFormat('pt-BR', {
                    style: 'currency',
                    currency: 'BRL',
                  }).format(Number(watch('valorPecas')) || 0)}
                  disabled
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-gray-100 dark:bg-gray-600 text-gray-900 dark:text-gray-100 px-3 py-2"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">Calculado automaticamente</p>
              </div>
            </div>

            {/* Info box para POR_HORA */}
            {tipoCobrancaMaoObra === TipoCobrancaMaoObra.POR_HORA && (
              <div className="rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 p-4">
                <div className="flex items-start gap-3">
                  <Clock className="h-5 w-5 text-blue-600 dark:text-blue-400 mt-0.5" />
                  <div className="text-sm">
                    <p className="font-medium text-blue-900 dark:text-blue-100">Cobrança por Hora</p>
                    <p className="text-blue-700 dark:text-blue-300 mt-1">
                      Valor/hora da oficina: <strong>{new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valorHoraOficina)}</strong>
                    </p>
                    <div className="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 text-blue-800 dark:text-blue-200">
                      <div>
                        <p className="text-xs text-blue-600 dark:text-blue-400">Estimativa Mínima</p>
                        <p className="font-semibold">
                          {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(tempoEstimadoHoras * valorHoraOficina)}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-blue-600 dark:text-blue-400">Limite Aprovado (máx)</p>
                        <p className="font-semibold">
                          {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(limiteHorasAprovado * valorHoraOficina)}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Valor Total (calculado) */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Valor Total (R$)
              </label>
              <input
                type="text"
                value={new Intl.NumberFormat('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                }).format(Number(watch('valorTotal')) || 0)}
                disabled
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-gray-100 dark:bg-gray-600 text-gray-900 dark:text-gray-100 px-3 py-2 font-medium"
              />
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">Mão de Obra + Peças + Serviços</p>
            </div>

            {/* Descontos */}
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Desconto (%)
                </label>
                <input
                  type="number"
                  step="0.01"
                  {...register('descontoPercentual', { valueAsNumber: true })}
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  min="0"
                  max="100"
                />
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Desconto (R$)
                </label>
                <input
                  type="number"
                  step="0.01"
                  {...register('descontoValor', { valueAsNumber: true })}
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:bg-gray-100 dark:disabled:bg-gray-600"
                  min="0"
                  disabled={descontoPercentual > 0}
                />
                {descontoPercentual > 0 && (
                  <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                    Desabilitado (usando desconto percentual)
                  </p>
                )}
              </div>
            </div>

            {/* Valor Final (calculado) */}
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Valor Final (R$)
              </label>
              <input
                type="text"
                value={new Intl.NumberFormat('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                }).format(Number(watch('valorFinal')) || 0)}
                disabled
                className="w-full rounded-lg border-2 border-green-500 dark:border-green-700 bg-green-50 dark:bg-green-900/30 px-3 py-2 text-lg font-bold text-green-900 dark:text-green-400"
              />
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">Valor Total - Desconto</p>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={() => navigate('/ordens-servico')}
            className="w-full sm:w-auto rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-6 py-2.5 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600"
            disabled={isSubmitting}
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full sm:w-auto flex items-center justify-center gap-2 rounded-lg bg-blue-600 dark:bg-blue-700 px-6 py-2.5 text-white hover:bg-blue-700 dark:hover:bg-blue-600 disabled:opacity-50"
          >
            {isSubmitting ? (
              <Loader2 className="h-5 w-5 animate-spin" />
            ) : (
              <Save className="h-5 w-5" />
            )}
            {uploadingFiles
              ? 'Enviando fotos...'
              : createMutation.isPending || updateMutation.isPending
                ? 'Salvando...'
                : `Salvar OS${pendingFiles.length > 0 ? ` (${pendingFiles.length} foto${pendingFiles.length > 1 ? 's' : ''})` : ''}`}
          </button>
        </div>
      </form>
    </div>
  );
};
