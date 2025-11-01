/**
 * Linha do tempo de datas importantes da Ordem de Serviço
 * Exibe visualmente as datas de abertura, previsão, finalização e entrega
 */

import { Calendar, CheckCircle, Clock, TruckIcon } from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import type { OrdemServico } from '../types';

interface StatusTimelineProps {
  ordemServico: OrdemServico;
}

interface TimelineEvent {
  label: string;
  date?: string | number[];
  icon: React.ReactNode;
  completed: boolean;
}

/**
 * Converte array de números ou string ISO para objeto Date
 */
const parseDate = (date?: string | number[]): Date | null => {
  if (!date) return null;

  if (Array.isArray(date)) {
    // Java LocalDateTime vem como [year, month, day, hour, minute, second, nano]
    const [year, month, day] = date;
    return new Date(year, month - 1, day);
  }

  return new Date(date);
};

/**
 * Formata data para exibição
 */
const formatDate = (date?: string | number[]): string => {
  const parsed = parseDate(date);
  if (!parsed) return 'Não definida';

  return format(parsed, "dd 'de' MMMM 'de' yyyy", { locale: ptBR });
};

export const StatusTimeline: React.FC<StatusTimelineProps> = ({ ordemServico }) => {
  const events: TimelineEvent[] = [
    {
      label: 'Abertura',
      date: ordemServico.dataAbertura,
      icon: <Calendar className="h-5 w-5" />,
      completed: true,
    },
    {
      label: 'Previsão de Entrega',
      date: ordemServico.dataPrevisao,
      icon: <Clock className="h-5 w-5" />,
      completed: !!ordemServico.dataPrevisao,
    },
    {
      label: 'Finalização',
      date: ordemServico.dataFinalizacao,
      icon: <CheckCircle className="h-5 w-5" />,
      completed: !!ordemServico.dataFinalizacao,
    },
    {
      label: 'Entrega',
      date: ordemServico.dataEntrega,
      icon: <TruckIcon className="h-5 w-5" />,
      completed: !!ordemServico.dataEntrega,
    },
  ];

  return (
    <div className="space-y-4">
      <h3 className="text-sm font-medium text-gray-700">Linha do Tempo</h3>

      <div className="relative">
        {/* Linha vertical conectando os eventos */}
        <div className="absolute left-4 top-0 h-full w-0.5 bg-gray-200" />

        <div className="space-y-6">
          {events.map((event, index) => (
            <div key={index} className="relative flex items-start gap-4">
              {/* Ícone do evento */}
              <div
                className={`relative z-10 flex h-8 w-8 items-center justify-center rounded-full border-2 ${
                  event.completed
                    ? 'border-blue-500 bg-blue-50 text-blue-600'
                    : 'border-gray-300 bg-white text-gray-400'
                }`}
              >
                {event.icon}
              </div>

              {/* Conteúdo do evento */}
              <div className="flex-1 pt-1">
                <p
                  className={`text-sm font-medium ${
                    event.completed ? 'text-gray-900' : 'text-gray-500'
                  }`}
                >
                  {event.label}
                </p>
                <p
                  className={`mt-1 text-xs ${
                    event.completed ? 'text-gray-600' : 'text-gray-400'
                  }`}
                >
                  {formatDate(event.date)}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
