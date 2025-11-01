import { forwardRef, type InputHTMLAttributes } from 'react';
import { masks } from '@/shared/utils/masks';
import { cn } from '@/shared/utils/cn';

type MaskType = 'cpf' | 'cnpj' | 'cpfCnpj' | 'phone' | 'cep' | 'placa' | 'chassi';

interface InputMaskProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange'> {
  mask: MaskType;
  onChange?: (value: string) => void;
  error?: string;
  label?: string;
}

export const InputMask = forwardRef<HTMLInputElement, InputMaskProps>(
  ({ mask, onChange, error, label, className, ...props }, ref) => {
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const maskedValue = masks[mask](e.target.value);
      e.target.value = maskedValue;
      onChange?.(maskedValue);
    };

    return (
      <div className="w-full">
        {label && (
          <label className="mb-1 block text-sm font-medium text-gray-700">
            {label}
            {props.required && <span className="ml-1 text-red-500">*</span>}
          </label>
        )}
        <input
          ref={ref}
          onChange={handleChange}
          className={cn(
            'w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2',
            error
              ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
              : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500/20',
            className
          )}
          {...props}
        />
        {error && <p className="mt-1 text-sm text-red-500">{error}</p>}
      </div>
    );
  }
);

InputMask.displayName = 'InputMask';
