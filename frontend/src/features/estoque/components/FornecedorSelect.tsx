/**
 * Componente de seleção de Fornecedor
 * Compatível com React Hook Form
 */

import { forwardRef } from 'react';
import { useFornecedoresResumo } from '@/features/fornecedores/hooks/useFornecedores';
import { TipoFornecedorLabel } from '@/features/fornecedores/types';

interface FornecedorSelectProps {
  value?: string;
  onChange?: (value: string) => void;
  onBlur?: () => void;
  name?: string;
  label?: string;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  error?: string;
}

export const FornecedorSelect = forwardRef<
  HTMLSelectElement,
  FornecedorSelectProps
>(
  (
    {
      value,
      onChange,
      onBlur,
      name,
      label = 'Fornecedor',
      placeholder = 'Selecione um fornecedor',
      required = false,
      disabled = false,
      error,
    },
    ref
  ) => {
    const { data: fornecedores = [], isLoading } = useFornecedoresResumo();

    const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
      const newValue = e.target.value;
      onChange?.(newValue);
    };

    return (
      <div className="space-y-1">
        {label && (
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
            {label}
            {required && <span className="text-red-500 dark:text-red-400"> *</span>}
          </label>
        )}

        <select
          ref={ref}
          name={name}
          value={value || ''}
          onChange={handleChange}
          onBlur={onBlur}
          disabled={disabled || isLoading}
          required={required}
          className={`
            w-full rounded-lg border px-3 py-2 transition-colors
            focus:outline-none focus:ring-2
            ${
              error
                ? 'border-red-300 dark:border-red-700 bg-red-50 dark:bg-red-950/30 text-red-900 dark:text-red-200 focus:border-red-500 focus:ring-red-500/20'
                : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:border-blue-500 focus:ring-blue-500/20'
            }
            ${disabled || isLoading ? 'cursor-not-allowed opacity-50' : ''}
          `}
        >
          <option value="">
            {isLoading ? 'Carregando...' : placeholder}
          </option>

          {fornecedores.length > 0 ? (
            fornecedores.map((f) => (
              <option key={f.id} value={f.id}>
                {f.nomeFantasia}
                {f.cpfCnpj ? ` (${f.cpfCnpj})` : ''}
                {' - '}
                {TipoFornecedorLabel[f.tipo]}
              </option>
            ))
          ) : (
            !isLoading && (
              <option value="" disabled>
                Nenhum fornecedor cadastrado
              </option>
            )
          )}
        </select>

        {error && (
          <p className="text-sm text-red-500 dark:text-red-400">{error}</p>
        )}
      </div>
    );
  }
);

FornecedorSelect.displayName = 'FornecedorSelect';
