/**
 * Página de formulário para Criar/Editar Peça
 */

import { useEffect, useMemo, useState, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, Save, Camera, Plus, X, Loader2, Upload, Image } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select';
import { formatCurrency } from '@/shared/utils/formatters';
import { showError, showSuccess } from '@/shared/utils/notifications';
import { usePeca, useCreatePeca, useUpdatePeca } from '../hooks/usePecas';
import {
  createPecaSchema,
  type CreatePecaFormData,
} from '../utils/validation';
import { UnidadeMedida, UnidadeMedidaLabel, getMargemLucroStatus } from '../types';
import { LocalArmazenamentoSelect } from '../components';
import { anexoService } from '@/features/anexos/services/anexoService';
import type { CategoriaAnexo } from '@/features/anexos/types';

/**
 * Interface para arquivos pendentes de upload
 */
interface PendingFile {
  id: string;
  file: File;
  preview: string;
}

export const PecaFormPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEditMode = !!id;

  const { data: peca, isLoading } = usePeca(id);
  const createPeca = useCreatePeca();
  const updatePeca = useUpdatePeca();

  const {
    register,
    handleSubmit,
    watch,
    reset,
    setValue,
    control,
    formState: { errors, isSubmitting },
  } = useForm<CreatePecaFormData>({
    resolver: zodResolver(createPecaSchema),
    defaultValues: {
      codigo: '',
      descricao: '',
      marca: '',
      aplicacao: '',
      localizacao: '',
      localArmazenamentoId: '',
      unidadeMedida: UnidadeMedida.UNIDADE,
      quantidadeMinima: 0,
      valorCusto: 0,
      valorVenda: 0,
    },
  });

  // Estado para upload de imagens
  const [pendingFiles, setPendingFiles] = useState<PendingFile[]>([]);
  const [uploadingFiles, setUploadingFiles] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Popular form no modo edição
  useEffect(() => {
    if (peca) {
      reset({
        codigo: peca.codigo,
        descricao: peca.descricao,
        marca: peca.marca || '',
        aplicacao: peca.aplicacao || '',
        localizacao: peca.localizacao || '',
        localArmazenamentoId: peca.localArmazenamentoId || '',
        unidadeMedida: peca.unidadeMedida,
        quantidadeMinima: peca.quantidadeMinima,
        valorCusto: peca.valorCusto,
        valorVenda: peca.valorVenda,
      });
    }
  }, [peca, reset]);

  // Limpar URLs de preview quando componente desmontar
  useEffect(() => {
    return () => {
      pendingFiles.forEach((file) => URL.revokeObjectURL(file.preview));
    };
  }, [pendingFiles]);

  // Handler para seleção de arquivos
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;

    const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
    const maxSize = 5 * 1024 * 1024; // 5MB

    const newFiles: PendingFile[] = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];

      if (!validTypes.includes(file.type)) {
        showError(`Arquivo ${file.name} não é uma imagem válida. Use JPEG, PNG ou WebP.`);
        continue;
      }

      if (file.size > maxSize) {
        showError(`Arquivo ${file.name} excede 5MB.`);
        continue;
      }

      newFiles.push({
        id: crypto.randomUUID(),
        file,
        preview: URL.createObjectURL(file),
      });
    }

    setPendingFiles((prev) => [...prev, ...newFiles]);

    // Limpar input para permitir selecionar mesmo arquivo novamente
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  // Handler para remover arquivo pendente
  const handleRemoveFile = (fileId: string) => {
    setPendingFiles((prev) => {
      const file = prev.find((f) => f.id === fileId);
      if (file) {
        URL.revokeObjectURL(file.preview);
      }
      return prev.filter((f) => f.id !== fileId);
    });
  };

  const valorCusto = watch('valorCusto');
  const valorVenda = watch('valorVenda');

  // Calcular margem de lucro
  const margemLucro = useMemo(() => {
    if (valorCusto > 0 && valorVenda > 0) {
      return ((valorVenda - valorCusto) / valorCusto) * 100;
    }
    return 0;
  }, [valorCusto, valorVenda]);

  const margemStatus = getMargemLucroStatus(margemLucro);

  const onSubmit = async (data: CreatePecaFormData) => {
    try {
      let pecaId: string;

      if (isEditMode && id) {
        await updatePeca.mutateAsync({ id, data });
        pecaId = id;
      } else {
        const result = await createPeca.mutateAsync(data);
        pecaId = result.id;
      }

      // Upload de imagens após criar/atualizar peça
      if (pendingFiles.length > 0) {
        setUploadingFiles(true);
        try {
          for (const pendingFile of pendingFiles) {
            await anexoService.upload({
              file: pendingFile.file,
              entidadeTipo: 'PECA',
              entidadeId: pecaId,
              categoria: 'FOTO_PECA' as CategoriaAnexo,
              descricao: null,
            });
          }
          showSuccess(`${pendingFiles.length} imagem(ns) enviada(s) com sucesso!`);
        } catch (uploadError) {
          showError('Erro ao enviar algumas imagens. A peça foi salva.');
        } finally {
          setUploadingFiles(false);
        }
      }

      navigate(`/estoque/${pecaId}`);
    } catch (error) {
      // Error handled by mutation
    }
  };

  if (isLoading && isEditMode) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Carregando...</div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-6 flex items-start gap-3 sm:gap-4">
        <button
          onClick={() => navigate('/estoque')}
          className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700 shrink-0"
        >
          <ArrowLeft className="h-5 w-5 text-gray-900 dark:text-gray-100" />
        </button>
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">
            {isEditMode ? 'Editar Peça' : 'Nova Peça'}
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {isEditMode
              ? 'Atualize as informações da peça'
              : 'Preencha os dados para cadastrar uma nova peça'}
          </p>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="mx-auto max-w-4xl">
        <div className="space-y-6">
          {/* Informações Básicas */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Informações Básicas</h2>

            <div className="grid gap-4 md:grid-cols-2">
              {/* Código */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Código (SKU) <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <input
                  {...register('codigo')}
                  type="text"
                  placeholder="Ex: FLT-001"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                {errors.codigo && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.codigo.message}</p>
                )}
              </div>

              {/* Unidade de Medida */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Unidade de Medida <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <Select
                  defaultValue={UnidadeMedida.UNIDADE}
                  onValueChange={(value) => setValue('unidadeMedida', value as UnidadeMedida)}
                >
                  <SelectTrigger className="h-10">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.values(UnidadeMedida).map((unidade) => (
                      <SelectItem key={unidade} value={unidade}>
                        {UnidadeMedidaLabel[unidade]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.unidadeMedida && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.unidadeMedida.message}</p>
                )}
              </div>
            </div>

            {/* Descrição */}
            <div className="mt-4">
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Descrição <span className="text-red-500 dark:text-red-400">*</span>
              </label>
              <textarea
                {...register('descricao')}
                rows={3}
                placeholder="Descreva a peça..."
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 resize-none"
              />
              {errors.descricao && (
                <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.descricao.message}</p>
              )}
            </div>

            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {/* Marca */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Marca</label>
                <input
                  {...register('marca')}
                  type="text"
                  placeholder="Ex: Bosch, NGK..."
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                {errors.marca && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.marca.message}</p>
                )}
              </div>

              {/* Local de Armazenamento */}
              <div>
                <Controller
                  name="localArmazenamentoId"
                  control={control}
                  render={({ field }) => (
                    <LocalArmazenamentoSelect
                      {...field}
                      label="Local de Armazenamento"
                      placeholder="Selecione o local"
                      error={errors.localArmazenamentoId?.message}
                      allowEmpty
                    />
                  )}
                />
              </div>
            </div>

            {/* Localização (legado) */}
            <div className="mt-4">
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Localização (Texto Livre)
              </label>
              <input
                {...register('localizacao')}
                type="text"
                placeholder="Ex: Observações adicionais"
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Campo opcional para observações adicionais sobre localização
              </p>
            </div>

            {/* Aplicação */}
            <div className="mt-4">
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">Aplicação</label>
              <textarea
                {...register('aplicacao')}
                rows={2}
                placeholder="Ex: Veículos compatíveis, observações..."
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 resize-none"
              />
              {errors.aplicacao && (
                <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.aplicacao.message}</p>
              )}
            </div>
          </div>

          {/* Controle de Estoque */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Controle de Estoque</h2>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Quantidade Mínima <span className="text-red-500 dark:text-red-400">*</span>
              </label>
              <input
                {...register('quantidadeMinima', { valueAsNumber: true })}
                type="number"
                min="0"
                step="1"
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Quantidade mínima para alerta de estoque baixo
              </p>
              {errors.quantidadeMinima && (
                <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.quantidadeMinima.message}</p>
              )}
            </div>
          </div>

          {/* Precificação */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Precificação</h2>

            <div className="grid gap-4 md:grid-cols-2">
              {/* Valor de Custo */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Valor de Custo (R$) <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <input
                  {...register('valorCusto', { valueAsNumber: true })}
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                {errors.valorCusto && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.valorCusto.message}</p>
                )}
              </div>

              {/* Valor de Venda */}
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Valor de Venda (R$) <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <input
                  {...register('valorVenda', { valueAsNumber: true })}
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                {errors.valorVenda && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.valorVenda.message}</p>
                )}
              </div>
            </div>

            {/* Margem de Lucro */}
            {valorCusto > 0 && valorVenda > 0 && (
              <div className="mt-4 rounded-lg border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Margem de Lucro</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{margemLucro.toFixed(2)}%</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      Lucro: {formatCurrency(valorVenda - valorCusto)}
                    </p>
                  </div>
                  <span
                    className={`px-3 py-1 rounded-full text-sm font-medium ${margemStatus.bgColor} ${margemStatus.textColor}`}
                  >
                    {margemStatus.label}
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Fotos da Peça */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <Image className="h-5 w-5 text-blue-600" />
              Fotos da Peça
            </h2>

            <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
              Adicione fotos da peça para facilitar a identificação no estoque.
            </p>

            {/* Área de Upload */}
            <div
              onClick={() => fileInputRef.current?.click()}
              className="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg p-6 text-center cursor-pointer hover:border-blue-500 dark:hover:border-blue-400 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
            >
              <input
                ref={fileInputRef}
                type="file"
                multiple
                accept="image/jpeg,image/png,image/webp"
                onChange={handleFileSelect}
                className="hidden"
              />
              <Upload className="h-8 w-8 text-gray-400 dark:text-gray-500 mx-auto mb-2" />
              <p className="text-sm text-gray-600 dark:text-gray-400">
                Clique para selecionar imagens
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-500 mt-1">
                JPEG, PNG ou WebP (máx. 5MB cada)
              </p>
            </div>

            {/* Preview das Imagens Selecionadas */}
            {pendingFiles.length > 0 && (
              <div className="mt-4">
                <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  {pendingFiles.length} imagem(ns) selecionada(s)
                </p>
                <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
                  {pendingFiles.map((pf) => (
                    <div key={pf.id} className="relative group">
                      <img
                        src={pf.preview}
                        alt="Preview"
                        className="w-full aspect-square object-cover rounded-lg border border-gray-200 dark:border-gray-600"
                      />
                      <button
                        type="button"
                        onClick={() => handleRemoveFile(pf.id)}
                        className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full p-1 shadow-lg opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Botões */}
          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" onClick={() => navigate('/estoque')} className="w-full sm:w-auto">
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting || uploadingFiles} className="w-full sm:w-auto">
              {uploadingFiles ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Enviando imagens...
                </>
              ) : (
                <>
                  <Save className="h-4 w-4 mr-2" />
                  {isSubmitting ? 'Salvando...' : 'Salvar'}
                </>
              )}
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
};
