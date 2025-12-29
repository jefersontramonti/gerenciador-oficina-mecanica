/**
 * Impersonate Button Component
 * Allows SUPER_ADMIN to access the system as a workshop's admin
 */

import { useState } from 'react';
import { UserCheck, AlertTriangle, Loader2, ExternalLink } from 'lucide-react';
import { useImpersonateOficina } from '../hooks/useSaas';
import { Modal } from '@/shared/components/ui/Modal';
import { showSuccess, showError } from '@/shared/utils/notifications';
import { setAccessToken } from '@/shared/services/api';

interface ImpersonateButtonProps {
  oficinaId: string;
  oficinaNome: string;
  disabled?: boolean;
}

export const ImpersonateButton = ({
  oficinaId,
  oficinaNome,
  disabled = false,
}: ImpersonateButtonProps) => {
  const [showConfirm, setShowConfirm] = useState(false);
  const impersonateMutation = useImpersonateOficina();

  const handleImpersonate = async () => {
    try {
      const response = await impersonateMutation.mutateAsync(oficinaId);

      // Store the impersonation token
      setAccessToken(response.accessToken);

      // Store original session info for return
      sessionStorage.setItem('impersonating', 'true');
      sessionStorage.setItem('impersonatedOficina', oficinaNome);

      showSuccess(`Acessando como ${oficinaNome}...`);

      // Redirect to the workshop's dashboard
      // Open in new tab to preserve SUPER_ADMIN session
      window.open(response.redirectUrl, '_blank');

      setShowConfirm(false);
    } catch (error: any) {
      showError(error.response?.data?.message || 'Erro ao iniciar impersonação');
    }
  };

  return (
    <>
      <button
        onClick={() => setShowConfirm(true)}
        disabled={disabled || impersonateMutation.isPending}
        className="flex items-center gap-2 rounded-lg border border-purple-300 bg-purple-50 px-4 py-2 text-purple-700 hover:bg-purple-100 disabled:opacity-50 dark:border-purple-700 dark:bg-purple-900/20 dark:text-purple-400 dark:hover:bg-purple-900/30"
      >
        {impersonateMutation.isPending ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : (
          <UserCheck className="h-4 w-4" />
        )}
        Acessar como Oficina
      </button>

      <Modal
        isOpen={showConfirm}
        onClose={() => setShowConfirm(false)}
        title="Acessar como Oficina"
      >
        <div className="space-y-4">
          <div className="flex items-start gap-3 rounded-lg border border-yellow-300 bg-yellow-50 p-4 dark:border-yellow-700 dark:bg-yellow-900/20">
            <AlertTriangle className="h-5 w-5 flex-shrink-0 text-yellow-600 dark:text-yellow-400" />
            <div className="text-sm text-yellow-800 dark:text-yellow-300">
              <p className="font-semibold">Atenção!</p>
              <p className="mt-1">
                Você está prestes a acessar o sistema como um usuário da oficina{' '}
                <strong>{oficinaNome}</strong>.
              </p>
              <ul className="mt-2 list-inside list-disc">
                <li>Todas as ações serão registradas na auditoria</li>
                <li>A sessão de impersonação expira em 1 hora</li>
                <li>O acesso será aberto em uma nova aba</li>
              </ul>
            </div>
          </div>

          <p className="text-gray-600 dark:text-gray-400">
            Deseja continuar?
          </p>

          <div className="flex justify-end gap-3">
            <button
              onClick={() => setShowConfirm(false)}
              className="rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              Cancelar
            </button>
            <button
              onClick={handleImpersonate}
              disabled={impersonateMutation.isPending}
              className="flex items-center gap-2 rounded-lg bg-purple-600 px-4 py-2 text-white hover:bg-purple-700 disabled:opacity-50"
            >
              {impersonateMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <>
                  <ExternalLink className="h-4 w-4" />
                  Acessar Oficina
                </>
              )}
            </button>
          </div>
        </div>
      </Modal>
    </>
  );
};
