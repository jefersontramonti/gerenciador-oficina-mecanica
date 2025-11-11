/**
 * Página de formulário para Criar/Editar Peça
 */

import { useEffect, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, Save } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Label } from '@/shared/components/ui/label';
import { Textarea } from '@/shared/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select';
import { formatCurrency } from '@/shared/utils/formatters';
import { usePeca, useCreatePeca, useUpdatePeca } from '../hooks/usePecas';
import {
  createPecaSchema,
  type CreatePecaFormData,
} from '../utils/validation';
import { UnidadeMedida, UnidadeMedidaLabel, getMargemLucroStatus } from '../types';
import { LocalArmazenamentoSelect } from '../components';

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
      if (isEditMode && id) {
        await updatePeca.mutateAsync({ id, data });
        navigate(`/estoque/${id}`);
      } else {
        const result = await createPeca.mutateAsync(data);
        navigate(`/estoque/${result.id}`);
      }
    } catch (error) {
      // Error handled by mutation
    }
  };

  if (isLoading && isEditMode) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <Button variant="ghost" size="sm" onClick={() => navigate('/estoque')}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          Voltar
        </Button>
        <h1 className="text-3xl font-bold tracking-tight mt-2">
          {isEditMode ? 'Editar Peça' : 'Nova Peça'}
        </h1>
        <p className="text-muted-foreground">
          {isEditMode
            ? 'Atualize as informações da peça'
            : 'Preencha os dados para cadastrar uma nova peça'}
        </p>
      </div>

      {/* Nota sobre quantidade inicial */}
      {!isEditMode && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <p className="text-sm text-blue-800">
            <strong>Atenção:</strong> A quantidade inicial em estoque será{' '}
            <strong>0 (zero)</strong>. Após criar a peça, use a função{' '}
            <strong>"Registrar Entrada"</strong> para adicionar o estoque inicial.
          </p>
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
        {/* Informações Básicas */}
        <div className="bg-card p-6 rounded-lg border space-y-4">
          <h2 className="text-lg font-semibold">Informações Básicas</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="codigo">Código (SKU) *</Label>
              <Input
                id="codigo"
                placeholder="Ex: FLT-001"
                {...register('codigo')}
              />
              {errors.codigo && (
                <p className="text-sm text-destructive">{errors.codigo.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="unidadeMedida">Unidade de Medida *</Label>
              <Select
                defaultValue={UnidadeMedida.UNIDADE}
                onValueChange={(value) =>
                  setValue('unidadeMedida', value as UnidadeMedida)
                }
              >
                <SelectTrigger>
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
                <p className="text-sm text-destructive">{errors.unidadeMedida.message}</p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="descricao">Descrição *</Label>
            <Textarea
              id="descricao"
              rows={3}
              placeholder="Descreva a peça..."
              {...register('descricao')}
            />
            {errors.descricao && (
              <p className="text-sm text-destructive">{errors.descricao.message}</p>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="marca">Marca</Label>
              <Input
                id="marca"
                placeholder="Ex: Bosch, NGK..."
                {...register('marca')}
              />
              {errors.marca && (
                <p className="text-sm text-destructive">{errors.marca.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Controller
                name="localArmazenamentoId"
                control={control}
                render={({ field }) => (
                  <LocalArmazenamentoSelect
                    {...field}
                    label="Local de Armazenamento"
                    placeholder="Selecione o local de armazenamento"
                    error={errors.localArmazenamentoId?.message}
                    allowEmpty
                  />
                )}
              />
            </div>
          </div>

          {/* Campo legado mantido temporariamente para compatibilidade */}
          <div className="space-y-2">
            <Label htmlFor="localizacao">Localização (Texto Livre - Opcional)</Label>
            <Input
              id="localizacao"
              placeholder="Ex: Observações adicionais sobre localização"
              {...register('localizacao')}
            />
            <p className="text-sm text-muted-foreground">
              Use o campo acima para selecionar o local. Este campo é opcional e serve apenas para observações.
            </p>
            {errors.localizacao && (
              <p className="text-sm text-destructive">{errors.localizacao.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="aplicacao">Aplicação</Label>
            <Textarea
              id="aplicacao"
              rows={2}
              placeholder="Ex: Veículos compatíveis, observações..."
              {...register('aplicacao')}
            />
            {errors.aplicacao && (
              <p className="text-sm text-destructive">{errors.aplicacao.message}</p>
            )}
          </div>
        </div>

        {/* Estoque */}
        <div className="bg-card p-6 rounded-lg border space-y-4">
          <h2 className="text-lg font-semibold">Controle de Estoque</h2>

          <div className="space-y-2">
            <Label htmlFor="quantidadeMinima">Quantidade Mínima *</Label>
            <Input
              id="quantidadeMinima"
              type="number"
              min="0"
              step="1"
              {...register('quantidadeMinima', { valueAsNumber: true })}
            />
            <p className="text-sm text-muted-foreground">
              Quantidade mínima para alerta de estoque baixo
            </p>
            {errors.quantidadeMinima && (
              <p className="text-sm text-destructive">{errors.quantidadeMinima.message}</p>
            )}
          </div>
        </div>

        {/* Precificação */}
        <div className="bg-card p-6 rounded-lg border space-y-4">
          <h2 className="text-lg font-semibold">Precificação</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="valorCusto">Valor de Custo (R$) *</Label>
              <Input
                id="valorCusto"
                type="number"
                min="0"
                step="0.01"
                placeholder="0.00"
                {...register('valorCusto', { valueAsNumber: true })}
              />
              {errors.valorCusto && (
                <p className="text-sm text-destructive">{errors.valorCusto.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="valorVenda">Valor de Venda (R$) *</Label>
              <Input
                id="valorVenda"
                type="number"
                min="0"
                step="0.01"
                placeholder="0.00"
                {...register('valorVenda', { valueAsNumber: true })}
              />
              {errors.valorVenda && (
                <p className="text-sm text-destructive">{errors.valorVenda.message}</p>
              )}
            </div>
          </div>

          {/* Margem de Lucro Calculada */}
          {valorCusto > 0 && valorVenda > 0 && (
            <div className="bg-muted p-4 rounded-md">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium">Margem de Lucro</p>
                  <p className="text-2xl font-bold">{margemLucro.toFixed(2)}%</p>
                  <p className="text-sm text-muted-foreground">
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

        {/* Botões de Ação */}
        <div className="flex gap-3 justify-end">
          <Button type="button" variant="outline" onClick={() => navigate('/estoque')}>
            Cancelar
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            <Save className="h-4 w-4 mr-2" />
            {isSubmitting ? 'Salvando...' : 'Salvar'}
          </Button>
        </div>
      </form>
    </div>
  );
};
