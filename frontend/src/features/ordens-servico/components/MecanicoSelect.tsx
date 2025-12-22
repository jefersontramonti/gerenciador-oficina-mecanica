/**
 * Componente select para seleção de mecânico responsável
 * Lista todos os usuários com perfil MECANICO
 */

import { useEffect, useState } from 'react';
import { Wrench } from 'lucide-react';
import { api } from '@/shared/services/api';

interface Mecanico {
  id: string;
  nome: string;
  email: string;
  perfil: string;
  ativo: boolean;
}

interface MecanicoSelectProps {
  value: string;
  onChange: (mecanicoId: string) => void;
  error?: string;
  required?: boolean;
}

export const MecanicoSelect: React.FC<MecanicoSelectProps> = ({
  value,
  onChange,
  error,
  required = false,
}) => {
  const [mecanicos, setMecanicos] = useState<Mecanico[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchMecanicos();
  }, []);

  const fetchMecanicos = async () => {
    setIsLoading(true);
    try {
      console.log('[MecanicoSelect] Buscando mecânicos...');
      const { data } = await api.get<Mecanico[]>('/usuarios/perfil/MECANICO');
      console.log('[MecanicoSelect] Mecânicos recebidos:', data);
      // Filtra apenas ativos
      const ativos = data.filter((m) => m.ativo);
      console.log('[MecanicoSelect] Mecânicos ativos:', ativos);
      setMecanicos(ativos);
    } catch (error: any) {
      console.error('[MecanicoSelect] Erro ao buscar mecânicos:', error);
      console.error('[MecanicoSelect] Status:', error.response?.status);
      console.error('[MecanicoSelect] Data:', error.response?.data);
      setMecanicos([]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      <label className="mb-1 block text-sm font-medium text-gray-700 dark:text-gray-300">
        <span className="flex items-center gap-2">
          <Wrench className="h-4 w-4" />
          Mecânico Responsável {required && <span className="text-red-600 dark:text-red-400">*</span>}
        </span>
      </label>

      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={isLoading}
        className={`w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 disabled:bg-gray-100 disabled:text-gray-500 dark:bg-gray-800 dark:text-white dark:disabled:bg-gray-900 dark:disabled:text-gray-500 ${
          error
            ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20 dark:border-red-400 dark:focus:border-red-400'
            : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500/20 dark:border-gray-600 dark:focus:border-blue-400'
        }`}
      >
        <option value="">
          {isLoading ? 'Carregando...' : 'Selecione o mecânico responsável'}
        </option>
        {mecanicos.map((mecanico) => (
          <option key={mecanico.id} value={mecanico.id}>
            {mecanico.nome} - {mecanico.email}
          </option>
        ))}
      </select>

      {error && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{error}</p>}

      {!isLoading && mecanicos.length === 0 && (
        <p className="mt-1 text-sm text-yellow-600 dark:text-yellow-400">
          Nenhum mecânico cadastrado. Cadastre usuários com perfil MECANICO primeiro.
        </p>
      )}
    </div>
  );
};
