/**
 * Página de Configurações do Usuário
 */

import { User, Lock } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/components/ui/tabs';
import { PerfilForm } from '../components/PerfilForm';
import { AlterarSenhaForm } from '../components/AlterarSenhaForm';

export const ConfiguracoesPage = () => {
  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Configurações</h1>
        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
          Gerencie seu perfil e preferências de conta
        </p>
      </div>

      {/* Tabs */}
      <div className="rounded-lg bg-white dark:bg-gray-800 shadow">
        <Tabs defaultValue="perfil" className="w-full">
          <TabsList className="w-full justify-start rounded-none rounded-t-lg border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-700 p-0">
            <TabsTrigger
              value="perfil"
              className="flex items-center gap-2 rounded-none rounded-tl-lg border-b-2 border-transparent px-6 py-3 text-gray-600 dark:text-gray-300 data-[state=active]:border-blue-600 data-[state=active]:text-blue-600 dark:data-[state=active]:text-blue-400 data-[state=active]:bg-white dark:data-[state=active]:bg-gray-800"
            >
              <User className="h-4 w-4" />
              Perfil
            </TabsTrigger>
            <TabsTrigger
              value="seguranca"
              className="flex items-center gap-2 rounded-none border-b-2 border-transparent px-6 py-3 text-gray-600 dark:text-gray-300 data-[state=active]:border-blue-600 data-[state=active]:text-blue-600 dark:data-[state=active]:text-blue-400 data-[state=active]:bg-white dark:data-[state=active]:bg-gray-800"
            >
              <Lock className="h-4 w-4" />
              Segurança
            </TabsTrigger>
          </TabsList>

          <div className="p-6">
            <TabsContent value="perfil" className="mt-0">
              <div className="max-w-2xl">
                <h2 className="mb-1 text-lg font-semibold text-gray-900 dark:text-white">
                  Informações do Perfil
                </h2>
                <p className="mb-6 text-sm text-gray-600 dark:text-gray-400">
                  Atualize suas informações pessoais
                </p>
                <PerfilForm />
              </div>
            </TabsContent>

            <TabsContent value="seguranca" className="mt-0">
              <div className="max-w-2xl">
                <h2 className="mb-1 text-lg font-semibold text-gray-900 dark:text-white">
                  Alterar Senha
                </h2>
                <p className="mb-6 text-sm text-gray-600 dark:text-gray-400">
                  Mantenha sua conta segura com uma senha forte
                </p>
                <AlterarSenhaForm />
              </div>
            </TabsContent>
          </div>
        </Tabs>
      </div>
    </div>
  );
};
