import { useState, useCallback } from 'react';

export interface EnderecoViaCep {
  cep: string;
  logradouro: string;
  complemento: string;
  bairro: string;
  localidade: string; // cidade
  uf: string; // estado
  erro?: boolean;
}

export interface BuscaCepResult {
  logradouro: string;
  bairro: string;
  cidade: string;
  estado: string;
}

interface UseBuscaCepReturn {
  buscarCep: (cep: string) => Promise<BuscaCepResult | null>;
  isLoading: boolean;
  error: string | null;
}

/**
 * Hook para buscar endereço pelo CEP usando a API ViaCEP.
 *
 * @example
 * ```tsx
 * const { buscarCep, isLoading, error } = useBuscaCep();
 *
 * const handleCepChange = async (cep: string) => {
 *   const endereco = await buscarCep(cep);
 *   if (endereco) {
 *     setValue('logradouro', endereco.logradouro);
 *     setValue('bairro', endereco.bairro);
 *     setValue('cidade', endereco.cidade);
 *     setValue('estado', endereco.estado);
 *   }
 * };
 * ```
 */
export const useBuscaCep = (): UseBuscaCepReturn => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const buscarCep = useCallback(async (cep: string): Promise<BuscaCepResult | null> => {
    // Remove caracteres não numéricos
    const cepLimpo = cep.replace(/\D/g, '');

    // Valida se tem 8 dígitos
    if (cepLimpo.length !== 8) {
      return null;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await fetch(`https://viacep.com.br/ws/${cepLimpo}/json/`);

      if (!response.ok) {
        throw new Error('Erro ao buscar CEP');
      }

      const data: EnderecoViaCep = await response.json();

      if (data.erro) {
        setError('CEP não encontrado');
        return null;
      }

      return {
        logradouro: data.logradouro || '',
        bairro: data.bairro || '',
        cidade: data.localidade || '',
        estado: data.uf || '',
      };
    } catch (err) {
      setError('Erro ao buscar CEP. Tente novamente ou preencha manualmente.');
      return null;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { buscarCep, isLoading, error };
};
