import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MapPin, Save, X, Package, Hash, AlignLeft, Building2, Tag } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Label } from '@/shared/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/components/ui/select';
import { Textarea } from '@/shared/components/ui/textarea';
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
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Carregando local...</p>
        </div>
      </div>
    );
  }

  if (errorLocal) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="rounded-lg border border-red-200 bg-red-50 p-6 max-w-md">
            <h2 className="text-lg font-semibold text-red-900 mb-2">Erro ao carregar local</h2>
            <p className="text-red-700">Não foi possível carregar os dados do local.</p>
            <Button
              variant="outline"
              onClick={() => navigate('/estoque/locais')}
              className="mt-4"
            >
              Voltar para lista
            </Button>
          </div>
        </div>
      </div>
    );
  }

  if (isEditing && !local) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="rounded-lg border border-yellow-200 bg-yellow-50 p-6 max-w-md">
            <h2 className="text-lg font-semibold text-yellow-900 mb-2">Local não encontrado</h2>
            <p className="text-yellow-700">O local que você está tentando editar não existe.</p>
            <Button
              variant="outline"
              onClick={() => navigate('/estoque/locais')}
              className="mt-4"
            >
              Voltar para lista
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-8">
      {/* Header com gradiente */}
      <div className="relative overflow-hidden rounded-lg bg-gradient-to-r from-blue-600 to-blue-800 p-6 text-white shadow-lg">
        <div className="relative z-10">
          <div className="flex items-center justify-between">
            <div>
              <div className="flex items-center gap-3 mb-2">
                <div className="rounded-full bg-white/20 p-2 backdrop-blur-sm">
                  <MapPin className="h-6 w-6" />
                </div>
                <h1 className="text-3xl font-bold">
                  {isEditing ? 'Editar Local de Armazenamento' : 'Novo Local de Armazenamento'}
                </h1>
              </div>
              <p className="text-blue-100 ml-12">
                {isEditing
                  ? 'Atualize as informações do local de armazenamento'
                  : 'Configure um novo espaço para organizar seu estoque'}
              </p>
            </div>
            <Button
              variant="outline"
              onClick={() => navigate('/estoque/locais')}
              className="bg-white/10 text-white border-white/20 hover:bg-white/20 backdrop-blur-sm"
            >
              <X className="h-4 w-4 mr-2" />
              Cancelar
            </Button>
          </div>
        </div>
        {/* Decoração de fundo */}
        <div className="absolute top-0 right-0 opacity-10">
          <MapPin className="h-48 w-48" />
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6 max-w-4xl">
        {/* Aviso sobre hierarquia */}
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border-l-4 border-blue-500 rounded-lg p-5 shadow-sm">
          <div className="flex gap-3">
            <div className="flex-shrink-0">
              <div className="rounded-full bg-blue-500 p-2">
                <Package className="h-5 w-5 text-white" />
              </div>
            </div>
            <div className="flex-1">
              <h3 className="text-sm font-semibold text-blue-900 mb-2">
                💡 Como funciona a hierarquia de locais
              </h3>
              <div className="grid md:grid-cols-2 gap-3 text-sm text-blue-800">
                <div className="bg-white/60 rounded p-2">
                  <p className="font-medium mb-1">✅ Podem ser raiz:</p>
                  <ul className="space-y-0.5 ml-4 list-disc">
                    <li>Depósito</li>
                    <li>Área</li>
                  </ul>
                </div>
                <div className="bg-white/60 rounded p-2">
                  <p className="font-medium mb-1">🔗 Precisam de pai:</p>
                  <ul className="space-y-0.5 ml-4 list-disc">
                    <li>Prateleira</li>
                    <li>Gaveta</li>
                    <li>Vitrine</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Seção: Identificação */}
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div className="bg-gradient-to-r from-gray-50 to-gray-100 px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
              <Hash className="h-5 w-5 text-gray-600" />
              Identificação
            </h2>
          </div>
          <div className="p-6 space-y-5">
            <div className="grid gap-6 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="codigo" className="text-sm font-medium flex items-center gap-2">
                  <Tag className="h-4 w-4 text-gray-500" />
                  Código <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="codigo"
                  placeholder="Ex: DEP-01, PRAT-A1"
                  className="h-11"
                  {...register('codigo')}
                />
                {errors.codigo && (
                  <p className="text-sm text-red-600 flex items-center gap-1">
                    <span className="text-red-500">●</span> {errors.codigo.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="tipo" className="text-sm font-medium flex items-center gap-2">
                  <Building2 className="h-4 w-4 text-gray-500" />
                  Tipo <span className="text-red-500">*</span>
                </Label>
                <Select
                  value={tipoAtual}
                  onValueChange={(value) => setValue('tipo', value as TipoLocal)}
                >
                  <SelectTrigger className="h-11">
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
                  <p className="text-sm text-red-600 flex items-center gap-1">
                    <span className="text-red-500">●</span> {errors.tipo.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="descricao" className="text-sm font-medium flex items-center gap-2">
                <AlignLeft className="h-4 w-4 text-gray-500" />
                Descrição <span className="text-red-500">*</span>
              </Label>
              <Input
                id="descricao"
                placeholder="Ex: Depósito Principal de Peças"
                className="h-11"
                {...register('descricao')}
              />
              {errors.descricao && (
                <p className="text-sm text-red-600 flex items-center gap-1">
                  <span className="text-red-500">●</span> {errors.descricao.message}
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Seção: Configurações */}
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div className="bg-gradient-to-r from-gray-50 to-gray-100 px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
              <Package className="h-5 w-5 text-gray-600" />
              Configurações
            </h2>
          </div>
          <div className="p-6 space-y-5">
            <div className="grid gap-6 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="localizacaoPaiId" className="text-sm font-medium flex items-center gap-2">
                  <MapPin className="h-4 w-4 text-gray-500" />
                  Local Pai {TIPOS_FILHOS_OBRIGATORIOS.includes(tipoAtual) && <span className="text-red-500">*</span>}
                </Label>
                <Select
                  value={localPaiAtual || ''}
                  onValueChange={(value) => setValue('localizacaoPaiId', value || undefined)}
                >
                  <SelectTrigger className={`h-11 ${errors.localizacaoPaiId ? 'border-red-500' : ''}`}>
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
                  <div className="flex items-start gap-2 text-xs text-orange-700 bg-orange-50 border border-orange-200 rounded p-2 mt-2">
                    <span className="text-orange-500 flex-shrink-0">⚠️</span>
                    <span>{TipoLocalLabel[tipoAtual]} deve ter um local pai (não pode ser raiz)</span>
                  </div>
                )}
                {errors.localizacaoPaiId && (
                  <p className="text-sm text-red-600 flex items-center gap-1">
                    <span className="text-red-500">●</span> {errors.localizacaoPaiId.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="capacidadeMaxima" className="text-sm font-medium flex items-center gap-2">
                  <Package className="h-4 w-4 text-gray-500" />
                  Capacidade Máxima (opcional)
                </Label>
                <Input
                  id="capacidadeMaxima"
                  type="number"
                  min="1"
                  placeholder="Ex: 100"
                  className="h-11"
                  {...register('capacidadeMaxima', {
                    setValueAs: (value) => value === '' || value === null ? undefined : Number(value)
                  })}
                />
                <p className="text-xs text-gray-500">Capacidade máxima de itens neste local</p>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="observacoes" className="text-sm font-medium flex items-center gap-2">
                <AlignLeft className="h-4 w-4 text-gray-500" />
                Observações (opcional)
              </Label>
              <Textarea
                id="observacoes"
                placeholder="Adicione notas ou instruções sobre este local..."
                className="min-h-[100px] resize-none"
                {...register('observacoes')}
              />
              <p className="text-xs text-gray-500">Informações adicionais sobre o local</p>
            </div>
          </div>
        </div>

        {/* Botões de Ação */}
        <div className="flex items-center justify-between pt-4 border-t border-gray-200">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate('/estoque/locais')}
            className="h-11 px-6"
          >
            <X className="h-4 w-4 mr-2" />
            Cancelar
          </Button>
          <Button
            type="submit"
            disabled={createLocal.isPending || updateLocal.isPending}
            className="h-11 px-8 bg-blue-600 hover:bg-blue-700"
          >
            <Save className="h-4 w-4 mr-2" />
            {createLocal.isPending || updateLocal.isPending
              ? 'Salvando...'
              : isEditing ? 'Atualizar Local' : 'Criar Local'}
          </Button>
        </div>
      </form>
    </div>
  );
};
