/**
 * Pagina de listagem de Notas Fiscais
 * Em desenvolvimento - aguardando integracao com API de terceiros
 */

import { FileText, Wrench, ExternalLink } from 'lucide-react';

export function NotasFiscaisListPage() {
  return (
    <div className="p-4 sm:p-6">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
          Notas Fiscais
        </h1>
        <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
          Emissao de NF-e, NFS-e e NFC-e
        </p>
      </div>

      {/* Mensagem de Em Desenvolvimento */}
      <div className="mx-auto max-w-2xl">
        <div className="rounded-lg border-2 border-dashed border-amber-300 bg-amber-50 p-4 sm:p-8 text-center dark:border-amber-700 dark:bg-amber-900/20">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-800">
            <Wrench className="h-8 w-8 text-amber-600 dark:text-amber-400" />
          </div>

          <h2 className="mb-2 text-xl font-semibold text-amber-800 dark:text-amber-200">
            Em Desenvolvimento
          </h2>

          <p className="mb-6 text-amber-700 dark:text-amber-300">
            O modulo de emissao de notas fiscais eletronicas esta em desenvolvimento.
            Em breve voce podera emitir NF-e, NFS-e e NFC-e diretamente pelo sistema.
          </p>

          {/* Tipos de NF suportados */}
          <div className="mb-6 grid gap-4 sm:grid-cols-3">
            <div className="rounded-lg bg-white p-4 shadow-sm dark:bg-gray-800">
              <FileText className="mx-auto mb-2 h-8 w-8 text-blue-600" />
              <h3 className="font-medium text-gray-900 dark:text-white">NF-e</h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Nota Fiscal Eletronica
              </p>
            </div>
            <div className="rounded-lg bg-white p-4 shadow-sm dark:bg-gray-800">
              <FileText className="mx-auto mb-2 h-8 w-8 text-green-600" />
              <h3 className="font-medium text-gray-900 dark:text-white">NFS-e</h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Nota Fiscal de Servico
              </p>
            </div>
            <div className="rounded-lg bg-white p-4 shadow-sm dark:bg-gray-800">
              <FileText className="mx-auto mb-2 h-8 w-8 text-purple-600" />
              <h3 className="font-medium text-gray-900 dark:text-white">NFC-e</h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Nota Fiscal Consumidor
              </p>
            </div>
          </div>

          {/* Funcionalidades planejadas */}
          <div className="rounded-lg bg-white p-4 text-left dark:bg-gray-800">
            <h4 className="mb-3 font-medium text-gray-900 dark:text-white">
              Funcionalidades planejadas:
            </h4>
            <ul className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
              <li className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-blue-500"></span>
                Emissao automatica a partir da Ordem de Servico
              </li>
              <li className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-blue-500"></span>
                Geracao de DANFE em PDF
              </li>
              <li className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-blue-500"></span>
                Cancelamento e Carta de Correcao
              </li>
              <li className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-blue-500"></span>
                Envio automatico por email/WhatsApp
              </li>
              <li className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-blue-500"></span>
                Integracao com SEFAZ e Prefeituras
              </li>
            </ul>
          </div>

          {/* Link para mais informacoes */}
          <div className="mt-6">
            <a
              href="https://www.nfe.fazenda.gov.br"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 text-sm text-amber-700 hover:text-amber-800 dark:text-amber-400 dark:hover:text-amber-300"
            >
              <ExternalLink className="h-4 w-4" />
              Saiba mais sobre NF-e
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
