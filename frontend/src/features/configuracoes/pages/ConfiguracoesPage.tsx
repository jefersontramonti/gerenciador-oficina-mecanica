/**
 * Pagina de Configuracoes do Usuario e Oficina
 *
 * Abas disponiveis:
 * - Perfil: Dados do usuario (todos os perfis)
 * - Seguranca: Alteracao de senha (todos os perfis)
 * - Oficina: Dados basicos da oficina (ADMIN/GERENTE)
 * - Operacional: Horarios, especialidades, redes sociais (ADMIN/GERENTE)
 * - Financeiro: Dados bancarios (ADMIN/GERENTE)
 * - Fiscal: Inscricoes, regime tributario (ADMIN/GERENTE)
 * - IA: Configuracao de IA para diagnosticos (ADMIN/GERENTE)
 */

import { User, Lock, Building2, Wrench, CreditCard, FileText, Bot } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/components/ui/tabs';
import { PerfilForm } from '../components/PerfilForm';
import { AlterarSenhaForm } from '../components/AlterarSenhaForm';
import { OficinaBasicoForm } from '../components/OficinaBasicoForm';
import { OficinaOperacionalForm } from '../components/OficinaOperacionalForm';
import { OficinaFinanceiroForm } from '../components/OficinaFinanceiroForm';
import { OficinaFiscalForm } from '../components/OficinaFiscalForm';
import { ConfiguracaoIAForm } from '@/features/ia/components';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { PerfilUsuario } from '@/features/auth/types';

export const ConfiguracoesPage = () => {
  const { user } = useAuth();

  // Verifica se o usuario pode gerenciar a oficina (ADMIN ou GERENTE)
  const canManageOficina =
    user?.perfil === PerfilUsuario.ADMIN || user?.perfil === PerfilUsuario.GERENTE;

  // Define a aba padrao
  const defaultTab = canManageOficina ? 'oficina' : 'perfil';

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          Configuracoes
        </h1>
        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
          {canManageOficina
            ? 'Gerencie os dados da oficina e seu perfil pessoal'
            : 'Gerencie seu perfil e preferencias de conta'}
        </p>
      </div>

      {/* Tabs */}
      <div className="rounded-lg bg-white shadow dark:bg-gray-800">
        <Tabs defaultValue={defaultTab} className="w-full">
          <TabsList className="w-full justify-start overflow-x-auto rounded-none rounded-t-lg border-b border-gray-200 bg-gray-50 p-0 dark:border-gray-700 dark:bg-gray-700">
            {/* Abas da Oficina (apenas para ADMIN/GERENTE) */}
            {canManageOficina && (
              <>
                <TabsTrigger
                  value="oficina"
                  className="flex items-center gap-2 whitespace-nowrap rounded-none rounded-tl-lg border-b-2 border-transparent px-4 py-3 text-gray-600 data-[state=active]:border-blue-600 data-[state=active]:bg-white data-[state=active]:text-blue-600 dark:text-gray-300 dark:data-[state=active]:bg-gray-800 dark:data-[state=active]:text-blue-400"
                >
                  <Building2 className="h-4 w-4" />
                  Oficina
                </TabsTrigger>
                <TabsTrigger
                  value="operacional"
                  className="flex items-center gap-2 whitespace-nowrap rounded-none border-b-2 border-transparent px-4 py-3 text-gray-600 data-[state=active]:border-blue-600 data-[state=active]:bg-white data-[state=active]:text-blue-600 dark:text-gray-300 dark:data-[state=active]:bg-gray-800 dark:data-[state=active]:text-blue-400"
                >
                  <Wrench className="h-4 w-4" />
                  Operacional
                </TabsTrigger>
                <TabsTrigger
                  value="financeiro"
                  className="flex items-center gap-2 whitespace-nowrap rounded-none border-b-2 border-transparent px-4 py-3 text-gray-600 data-[state=active]:border-blue-600 data-[state=active]:bg-white data-[state=active]:text-blue-600 dark:text-gray-300 dark:data-[state=active]:bg-gray-800 dark:data-[state=active]:text-blue-400"
                >
                  <CreditCard className="h-4 w-4" />
                  Financeiro
                </TabsTrigger>
                <TabsTrigger
                  value="fiscal"
                  className="flex items-center gap-2 whitespace-nowrap rounded-none border-b-2 border-transparent px-4 py-3 text-gray-600 data-[state=active]:border-blue-600 data-[state=active]:bg-white data-[state=active]:text-blue-600 dark:text-gray-300 dark:data-[state=active]:bg-gray-800 dark:data-[state=active]:text-blue-400"
                >
                  <FileText className="h-4 w-4" />
                  Fiscal
                </TabsTrigger>
                <TabsTrigger
                  value="ia"
                  className="flex items-center gap-2 whitespace-nowrap rounded-none border-b-2 border-transparent px-4 py-3 text-gray-600 data-[state=active]:border-blue-600 data-[state=active]:bg-white data-[state=active]:text-blue-600 dark:text-gray-300 dark:data-[state=active]:bg-gray-800 dark:data-[state=active]:text-blue-400"
                >
                  <Bot className="h-4 w-4" />
                  Inteligencia Artificial
                </TabsTrigger>
              </>
            )}

            {/* Abas do Usuario (todos os perfis) */}
            <TabsTrigger
              value="perfil"
              className={`flex items-center gap-2 whitespace-nowrap rounded-none border-b-2 border-transparent px-4 py-3 text-gray-600 data-[state=active]:border-blue-600 data-[state=active]:bg-white data-[state=active]:text-blue-600 dark:text-gray-300 dark:data-[state=active]:bg-gray-800 dark:data-[state=active]:text-blue-400 ${
                !canManageOficina ? 'rounded-tl-lg' : ''
              }`}
            >
              <User className="h-4 w-4" />
              Perfil
            </TabsTrigger>
            <TabsTrigger
              value="seguranca"
              className="flex items-center gap-2 whitespace-nowrap rounded-none border-b-2 border-transparent px-4 py-3 text-gray-600 data-[state=active]:border-blue-600 data-[state=active]:bg-white data-[state=active]:text-blue-600 dark:text-gray-300 dark:data-[state=active]:bg-gray-800 dark:data-[state=active]:text-blue-400"
            >
              <Lock className="h-4 w-4" />
              Seguranca
            </TabsTrigger>
          </TabsList>

          <div className="p-6">
            {/* Conteudo das abas da Oficina */}
            {canManageOficina && (
              <>
                <TabsContent value="oficina" className="mt-0">
                  <div className="max-w-4xl">
                    <h2 className="mb-1 text-lg font-semibold text-gray-900 dark:text-white">
                      Dados da Oficina
                    </h2>
                    <p className="mb-6 text-sm text-gray-600 dark:text-gray-400">
                      Informacoes basicas, contato e endereco da oficina
                    </p>
                    <OficinaBasicoForm />
                  </div>
                </TabsContent>

                <TabsContent value="operacional" className="mt-0">
                  <div className="max-w-4xl">
                    <h2 className="mb-1 text-lg font-semibold text-gray-900 dark:text-white">
                      Informacoes Operacionais
                    </h2>
                    <p className="mb-6 text-sm text-gray-600 dark:text-gray-400">
                      Horarios de funcionamento, especialidades, servicos e redes sociais
                    </p>
                    <OficinaOperacionalForm />
                  </div>
                </TabsContent>

                <TabsContent value="financeiro" className="mt-0">
                  <div className="max-w-3xl">
                    <h2 className="mb-1 text-lg font-semibold text-gray-900 dark:text-white">
                      Dados Financeiros
                    </h2>
                    <p className="mb-6 text-sm text-gray-600 dark:text-gray-400">
                      Dados bancarios e chave PIX para recebimento de pagamentos
                    </p>
                    <OficinaFinanceiroForm />
                  </div>
                </TabsContent>

                <TabsContent value="fiscal" className="mt-0">
                  <div className="max-w-3xl">
                    <h2 className="mb-1 text-lg font-semibold text-gray-900 dark:text-white">
                      Dados Fiscais
                    </h2>
                    <p className="mb-6 text-sm text-gray-600 dark:text-gray-400">
                      Inscricoes e regime tributario para emissao de notas fiscais
                    </p>
                    <OficinaFiscalForm />
                  </div>
                </TabsContent>

                <TabsContent value="ia" className="mt-0">
                  <div className="max-w-4xl">
                    <h2 className="mb-1 text-lg font-semibold text-gray-900 dark:text-white">
                      Inteligencia Artificial
                    </h2>
                    <p className="mb-6 text-sm text-gray-600 dark:text-gray-400">
                      Configure o diagnostico assistido por IA para suas ordens de servico
                    </p>
                    <ConfiguracaoIAForm />
                  </div>
                </TabsContent>
              </>
            )}

            {/* Conteudo das abas do Usuario */}
            <TabsContent value="perfil" className="mt-0">
              <div className="max-w-2xl">
                <h2 className="mb-1 text-lg font-semibold text-gray-900 dark:text-white">
                  Informacoes do Perfil
                </h2>
                <p className="mb-6 text-sm text-gray-600 dark:text-gray-400">
                  Atualize suas informacoes pessoais
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
