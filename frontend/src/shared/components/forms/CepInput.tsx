import { forwardRef, useState, useCallback, type InputHTMLAttributes } from 'react';
import { Search, Loader2, AlertCircle, CheckCircle } from 'lucide-react';
import { masks } from '@/shared/utils/masks';
import { useBuscaCep, type BuscaCepResult } from '@/shared/hooks/useBuscaCep';
import { cn } from '@/shared/utils/cn';

interface CepInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange'> {
  onChange?: (value: string) => void;
  onAddressFound?: (endereco: BuscaCepResult) => void;
  error?: string;
  label?: string;
}

/**
 * Input de CEP com autocomplete via ViaCEP.
 *
 * Quando o usuário digita um CEP válido (8 dígitos), busca automaticamente
 * o endereço na API ViaCEP. Se encontrado, dispara o callback `onAddressFound`
 * para preencher os demais campos do formulário.
 *
 * Se o CEP não for encontrado, o usuário pode digitar o endereço manualmente.
 *
 * @example
 * ```tsx
 * <Controller
 *   name="cep"
 *   control={control}
 *   render={({ field }) => (
 *     <CepInput
 *       {...field}
 *       label="CEP"
 *       error={errors.cep?.message}
 *       onAddressFound={(endereco) => {
 *         setValue('logradouro', endereco.logradouro);
 *         setValue('bairro', endereco.bairro);
 *         setValue('cidade', endereco.cidade);
 *         setValue('estado', endereco.estado);
 *       }}
 *     />
 *   )}
 * />
 * ```
 */
export const CepInput = forwardRef<HTMLInputElement, CepInputProps>(
  ({ onChange, onAddressFound, error, label, className, ...props }, ref) => {
    const { buscarCep, isLoading, error: cepError } = useBuscaCep();
    const [status, setStatus] = useState<'idle' | 'success' | 'not-found'>('idle');

    const handleChange = useCallback(
      async (e: React.ChangeEvent<HTMLInputElement>) => {
        const maskedValue = masks.cep(e.target.value);
        e.target.value = maskedValue;
        onChange?.(maskedValue);

        // Reset status
        setStatus('idle');

        // Se tiver 9 caracteres (00000-000), busca o CEP
        if (maskedValue.length === 9) {
          const endereco = await buscarCep(maskedValue);
          if (endereco) {
            setStatus('success');
            onAddressFound?.(endereco);
          } else {
            setStatus('not-found');
          }
        }
      },
      [onChange, buscarCep, onAddressFound]
    );

    const handleBlur = useCallback(
      async (e: React.FocusEvent<HTMLInputElement>) => {
        const value = e.target.value;
        // Se o CEP está completo e ainda não buscou, busca no blur
        if (value.length === 9 && status === 'idle') {
          const endereco = await buscarCep(value);
          if (endereco) {
            setStatus('success');
            onAddressFound?.(endereco);
          } else {
            setStatus('not-found');
          }
        }
      },
      [buscarCep, onAddressFound, status]
    );

    return (
      <div className="w-full">
        {label && (
          <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
            {label}
            {props.required && <span className="ml-1 text-red-500 dark:text-red-400">*</span>}
          </label>
        )}
        <div className="relative">
          <input
            ref={ref}
            onChange={handleChange}
            onBlur={handleBlur}
            maxLength={9}
            className={cn(
              'w-full rounded-lg border px-3 py-2 pr-10 focus:outline-none focus:ring-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100',
              error
                ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                : 'border-gray-300 dark:border-gray-600 focus:border-blue-500 focus:ring-blue-500/20',
              className
            )}
            placeholder="00000-000"
            {...props}
          />

          {/* Ícone de status */}
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            {isLoading ? (
              <Loader2 className="h-4 w-4 animate-spin text-blue-500" />
            ) : status === 'success' ? (
              <CheckCircle className="h-4 w-4 text-green-500" />
            ) : status === 'not-found' ? (
              <span title="CEP não encontrado. Preencha manualmente.">
                <AlertCircle className="h-4 w-4 text-yellow-500" />
              </span>
            ) : (
              <Search className="h-4 w-4 text-gray-400" />
            )}
          </div>
        </div>

        {/* Mensagens de status */}
        {error && <p className="mt-1 text-sm text-red-500 dark:text-red-400">{error}</p>}
        {!error && cepError && (
          <p className="mt-1 text-sm text-yellow-600 dark:text-yellow-400">{cepError}</p>
        )}
        {!error && !cepError && status === 'success' && (
          <p className="mt-1 text-sm text-green-600 dark:text-green-400">
            Endereço encontrado e preenchido automaticamente
          </p>
        )}
        {!error && !cepError && status === 'not-found' && (
          <p className="mt-1 text-sm text-yellow-600 dark:text-yellow-400">
            CEP não encontrado. Preencha o endereço manualmente.
          </p>
        )}
      </div>
    );
  }
);

CepInput.displayName = 'CepInput';
