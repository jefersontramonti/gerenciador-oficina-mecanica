import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Save } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/components/ui/select';
import { useLocalArmazenamento, useCreateLocal, useUpdateLocal, useLocaisRaiz } from '../hooks/useLocaisArmazenamento';
import { TipoLocal, TipoLocalLabel, TipoLocalIcon } from '../types';

// Tipos que DEVEM ter um pai
const TIPOS_FILHOS_OBRIGATORIOS: TipoLocal[] = [TipoLocal.GAVETA, TipoLocal.PRATELEIRA, TipoLocal.VITRINE];

const schema = z.object({
  codigo: z.string().min(1, 'Código obrigatório').trim(),
  descricao: z.string().min(3, 'Descrição deve ter ao menos 3 caracteres').trim(),
  tipo: z.nativeEnum(TipoLocal, {
    message: 'Selecione um tipo válido',
  }),
  localizacaoPaiId: z.string().optional(),
  capacidadeMaxima: z.number().positive('Capacidade deve ser positiva').optional().nullable(),
  observacoes: z.string().optional(),
}).refine(
  (data) => {
    // Se o tipo exige um pai, deve ter localizacaoPaiId
    if (TIPOS_FILHOS_OBRIGATORIOS.includes(data.tipo) && !data.localizacaoPaiId) {
      return false;
    }
    return true;
  },
  {
    message: 'Este tipo de local deve ter um local pai definido',
    path: ['localizacaoPaiId'],
  }
);

type FormData = z.infer<typeof schema>;

export const LocalArmazenamentoFormPage = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditing = !!id;

  const { data: local, isLoading: loadingLocal, error: errorLocal } = useLocalArmazenamento(id);
  const { data: locaisRaiz = [] } = useLocaisRaiz();
  const createLocal = useCreateLocal();
  const updateLocal = useUpdateLocal();

  const { register, handleSubmit, formState: { errors }, reset, setValue, watch } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      tipo: TipoLocal.DEPOSITO, // Valor padrão
    },
  });

  const tipoAtual = watch('tipo');
  const localPaiAtual = watch('localizacaoPaiId');

  useEffect(() => {
    if (local) {
      reset({
        codigo: local.codigo,
        descricao: local.descricao,
        tipo: local.tipo,
        localizacaoPaiId: local.localizacaoPai?.id || undefined,
        capacidadeMaxima: local.capacidadeMaxima ?? undefined,
        observacoes: local.observacoes || undefined,
      });
    }
  }, [local, reset]);

  const onSubmit = async (data: FormData) => {
    // Transformar dados para enviar ao backend
    const payload = {
      codigo: data.codigo,
      descricao: data.descricao,
      tipo: data.tipo,
      localizacaoPaiId: data.localizacaoPaiId || undefined,
      capacidadeMaxima: data.capacidadeMaxima || undefined,
      observacoes: data.observacoes || undefined,
    };

    try {
      if (isEditing) {
        await updateLocal.mutateAsync({ id: id!, data: payload });
      } else {
        await createLocal.mutateAsync(payload);
      }
      navigate('/estoque/locais');
    } catch (error: any) {
      // O erro já é tratado pelo toast no hook
      console.error('Erro ao salvar:', error);
    }
  };

  if (loadingLocal) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Carregando...</div>
      </div>
    );
  }

  if (errorLocal) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-950/30 p-6 max-w-md mx-auto">
          <h2 className="text-lg font-semibold text-red-900 dark:text-red-200 mb-2">Erro ao carregar local</h2>
          <p className="text-red-700 dark:text-red-300">Não foi possível carregar os dados do local.</p>
          <Button
            variant="outline"
            onClick={() => navigate('/estoque/locais')}
            className="mt-4"
          >
            Voltar para lista
          </Button>
        </div>
      </div>
    );
  }

  if (isEditing && !local) {
    return (
      <div className="p-6">
        <div className="rounded-lg border border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-950/30 p-6 max-w-md mx-auto">
          <h2 className="text-lg font-semibold text-yellow-900 dark:text-yellow-200 mb-2">Local não encontrado</h2>
          <p className="text-yellow-700 dark:text-yellow-300">O local que você está tentando editar não existe.</p>
          <Button
            variant="outline"
            onClick={() => navigate('/estoque/locais')}
            className="mt-4"
          >
            Voltar para lista
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-6 flex items-start gap-3 sm:gap-4">
        <button
          onClick={() => navigate('/estoque/locais')}
          className="rounded-lg p-2 hover:bg-gray-100 dark:hover:bg-gray-700 shrink-0"
        >
          <ArrowLeft className="h-5 w-5 text-gray-900 dark:text-gray-100" />
        </button>
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-gray-100">
            {isEditing ? 'Editar Local de Armazenamento' : 'Novo Local de Armazenamento'}
          </h1>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
            {isEditing
              ? 'Atualize as informações do local de armazenamento'
              : 'Configure um novo espaço para organizar seu estoque'}
          </p>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="mx-auto max-w-4xl">
        <div className="space-y-6">
          {/* Identificação */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Identificação</h2>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Código <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <input
                  {...register('codigo')}
                  type="text"
                  placeholder="Ex: DEP-01, PRAT-A1"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                {errors.codigo && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.codigo.message}</p>
                )}
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Tipo <span className="text-red-500 dark:text-red-400">*</span>
                </label>
                <Select
                  value={tipoAtual}
                  onValueChange={(value) => setValue('tipo', value as TipoLocal)}
                >
                  <SelectTrigger className="h-10">
                    <SelectValue placeholder="Selecione o tipo" />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.values(TipoLocal).map((tipo) => (
                      <SelectItem key={tipo} value={tipo}>
                        <span className="flex items-center gap-2">
                          <span>{TipoLocalIcon[tipo]}</span>
                          <span>{TipoLocalLabel[tipo]}</span>
                        </span>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.tipo && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.tipo.message}</p>
                )}
              </div>
            </div>

            <div className="mt-4">
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Descrição <span className="text-red-500 dark:text-red-400">*</span>
              </label>
              <input
                {...register('descricao')}
                type="text"
                placeholder="Ex: Depósito Principal de Peças"
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
              />
              {errors.descricao && (
                <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.descricao.message}</p>
              )}
            </div>
          </div>

          {/* Configurações */}
          <div className="rounded-lg bg-white dark:bg-gray-800 p-6 shadow">
            <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-gray-100">Configurações</h2>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Local Pai {TIPOS_FILHOS_OBRIGATORIOS.includes(tipoAtual) && <span className="text-red-500 dark:text-red-400">*</span>}
                </label>
                <Select
                  value={localPaiAtual || ''}
                  onValueChange={(value) => setValue('localizacaoPaiId', value || undefined)}
                >
                  <SelectTrigger className="h-10">
                    <SelectValue placeholder={TIPOS_FILHOS_OBRIGATORIOS.includes(tipoAtual) ? "Selecione um local pai" : "Nenhum (raiz)"} />
                  </SelectTrigger>
                  <SelectContent>
                    {!TIPOS_FILHOS_OBRIGATORIOS.includes(tipoAtual) && (
                      <SelectItem value="">Nenhum (raiz)</SelectItem>
                    )}
                    {locaisRaiz.map((localRaiz) => (
                      <SelectItem key={localRaiz.id} value={localRaiz.id}>
                        <span className="flex items-center gap-2">
                          <span>{TipoLocalIcon[localRaiz.tipo]}</span>
                          <span>{localRaiz.codigo} - {localRaiz.descricao}</span>
                        </span>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {TIPOS_FILHOS_OBRIGATORIOS.includes(tipoAtual) && (
                  <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                    {TipoLocalLabel[tipoAtual]} deve ter um local pai (não pode ser raiz)
                  </p>
                )}
                {errors.localizacaoPaiId && (
                  <p className="mt-1 text-sm text-red-500 dark:text-red-400">{errors.localizacaoPaiId.message}</p>
                )}
              </div>

              <div>
                <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Capacidade Máxima (opcional)
                </label>
                <input
                  {...register('capacidadeMaxima', {
                    setValueAs: (value) => value === '' || value === null ? undefined : Number(value)
                  })}
                  type="number"
                  min="1"
                  placeholder="Ex: 100"
                  className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  Capacidade máxima de itens neste local
                </p>
              </div>
            </div>

            <div className="mt-4">
              <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
                Observações (opcional)
              </label>
              <textarea
                {...register('observacoes')}
                rows={3}
                placeholder="Adicione notas ou instruções sobre este local..."
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 px-3 py-2 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 resize-none"
              />
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Informações adicionais sobre o local
              </p>
            </div>
          </div>

          {/* Botões de Ação */}
          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate('/estoque/locais')}
              className="w-full sm:w-auto"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={createLocal.isPending || updateLocal.isPending}
              className="w-full sm:w-auto"
            >
              <Save className="h-4 w-4 mr-2" />
              {createLocal.isPending || updateLocal.isPending
                ? 'Salvando...'
                : isEditing ? 'Atualizar Local' : 'Criar Local'}
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
};
