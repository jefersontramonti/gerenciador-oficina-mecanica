/**
 * Componente de seleção de Local de Armazenamento
 * Compatível com React Hook Form
 */

import { forwardRef } from 'react';
import { useLocaisArmazenamento } from '../hooks/useLocaisArmazenamento';
import { TipoLocalLabel, TipoLocalIcon } from '../types';

interface LocalArmazenamentoSelectProps {
  value?: string;
  onChange?: (value: string) => void;
  onBlur?: () => void;
  name?: string;
  label?: string;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  error?: string;
  allowEmpty?: boolean; // Permitir opção "Sem localização"
}

export const LocalArmazenamentoSelect = forwardRef<
  HTMLSelectElement,
  LocalArmazenamentoSelectProps
>(
  (
    {
      value,
      onChange,
      onBlur,
      name,
      label = 'Local de Armazenamento',
      placeholder = 'Selecione um local',
      required = false,
      disabled = false,
      error,
      allowEmpty = true,
    },
    ref
  ) => {
    const { data: locais = [], isLoading } = useLocaisArmazenamento();

    // Filtrar apenas locais ativos
    const locaisAtivos = locais.filter((local) => local.ativo);

    // Agrupar por tipo para melhor visualização
    const locaisPorTipo = locaisAtivos.reduce((acc, local) => {
      if (!acc[local.tipo]) {
        acc[local.tipo] = [];
      }
      acc[local.tipo].push(local);
      return acc;
    }, {} as Record<string, typeof locaisAtivos>);

    const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
      const newValue = e.target.value;
      onChange?.(newValue);
    };

    return (
      <div className="space-y-1">
        {label && (
          <label className="block text-sm font-medium text-gray-700">
            {label}
            {required && <span className="text-red-500">*</span>}
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
          <option value="" disabled={required && !allowEmpty}>
            {isLoading ? 'Carregando...' : placeholder}
          </option>

          {allowEmpty && !required && (
            <option value="">Sem localização</option>
          )}

          {Object.keys(locaisPorTipo).length > 0 ? (
            Object.entries(locaisPorTipo).map(([tipo, locaisDoTipo]) => (
              <optgroup
                key={tipo}
                label={`${TipoLocalIcon[tipo as keyof typeof TipoLocalIcon]} ${TipoLocalLabel[tipo as keyof typeof TipoLocalLabel]}`}
              >
                {locaisDoTipo.map((local) => (
                  <option key={local.id} value={local.id}>
                    {local.codigo} - {local.descricao}
                    {local.localizacaoPai ? ` (${local.localizacaoPai.descricao})` : ''}
                  </option>
                ))}
              </optgroup>
            ))
          ) : (
            !isLoading && (
              <option value="" disabled>
                Nenhum local cadastrado
              </option>
            )
          )}
        </select>

        {error && (
          <p className="text-sm text-red-500">{error}</p>
        )}
      </div>
    );
  }
);

LocalArmazenamentoSelect.displayName = 'LocalArmazenamentoSelect';
