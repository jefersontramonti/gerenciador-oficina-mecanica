import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Plus, Calendar, Clock, Car } from 'lucide-react';
import { useCalendario } from '../hooks/useManutencaoPreventiva';
import type { CalendarioEvento, StatusAgendamento } from '../types';

const DIAS_SEMANA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
const MESES = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
];

export default function CalendarioPage() {
  const hoje = new Date();
  const [mesAtual, setMesAtual] = useState(hoje.getMonth() + 1);
  const [anoAtual, setAnoAtual] = useState(hoje.getFullYear());
  const [eventoSelecionado, setEventoSelecionado] = useState<CalendarioEvento | null>(null);

  const { data: eventos, isLoading } = useCalendario(mesAtual, anoAtual);

  const diasDoMes = useMemo(() => {
    const primeiroDia = new Date(anoAtual, mesAtual - 1, 1);
    const ultimoDia = new Date(anoAtual, mesAtual, 0);
    const diasAntes = primeiroDia.getDay();
    const totalDias = ultimoDia.getDate();

    const dias: { dia: number; mesAtual: boolean; data: Date }[] = [];

    // Dias do mês anterior
    const mesAnterior = new Date(anoAtual, mesAtual - 1, 0);
    for (let i = diasAntes - 1; i >= 0; i--) {
      dias.push({
        dia: mesAnterior.getDate() - i,
        mesAtual: false,
        data: new Date(anoAtual, mesAtual - 2, mesAnterior.getDate() - i),
      });
    }

    // Dias do mês atual
    for (let i = 1; i <= totalDias; i++) {
      dias.push({
        dia: i,
        mesAtual: true,
        data: new Date(anoAtual, mesAtual - 1, i),
      });
    }

    // Dias do próximo mês
    const diasDepois = 42 - dias.length;
    for (let i = 1; i <= diasDepois; i++) {
      dias.push({
        dia: i,
        mesAtual: false,
        data: new Date(anoAtual, mesAtual, i),
      });
    }

    return dias;
  }, [mesAtual, anoAtual]);

  const eventosMap = useMemo(() => {
    const map: Record<string, CalendarioEvento[]> = {};
    if (!eventos) return map;

    eventos.forEach((evento) => {
      const dataStr = evento.inicio.split('T')[0];
      if (!map[dataStr]) map[dataStr] = [];
      map[dataStr].push(evento);
    });

    return map;
  }, [eventos]);

  const mesAnterior = () => {
    if (mesAtual === 1) {
      setMesAtual(12);
      setAnoAtual(anoAtual - 1);
    } else {
      setMesAtual(mesAtual - 1);
    }
  };

  const proximoMes = () => {
    if (mesAtual === 12) {
      setMesAtual(1);
      setAnoAtual(anoAtual + 1);
    } else {
      setMesAtual(mesAtual + 1);
    }
  };

  const irParaHoje = () => {
    setMesAtual(hoje.getMonth() + 1);
    setAnoAtual(hoje.getFullYear());
  };

  const formatarData = (data: Date) => {
    return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`;
  };

  const isHoje = (data: Date) => {
    return formatarData(data) === formatarData(hoje);
  };

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 dark:text-white">
            Calendário de Manutenções
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 hidden sm:block">
            Visualize todos os agendamentos em formato de calendário
          </p>
        </div>
        <div className="flex flex-col sm:flex-row gap-2">
          <Link
            to="/manutencao-preventiva/agendamentos/novo"
            className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Plus className="h-4 w-4" />
            <span>Novo Agendamento</span>
          </Link>
          <Link
            to="/manutencao-preventiva/agendamentos"
            className="flex items-center justify-center gap-2 w-full sm:w-auto px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Ver Lista
          </Link>
        </div>
      </div>

      {/* Calendar Navigation */}
      <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-3 sm:p-4">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 sm:gap-4 mb-4">
          <div className="flex items-center gap-2">
            <button
              onClick={mesAnterior}
              className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
            >
              <ChevronLeft className="h-5 w-5 text-gray-600 dark:text-gray-400" />
            </button>
            <h2 className="text-lg sm:text-xl font-semibold text-gray-900 dark:text-white min-w-[160px] sm:min-w-[200px] text-center">
              {MESES[mesAtual - 1]} {anoAtual}
            </h2>
            <button
              onClick={proximoMes}
              className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
            >
              <ChevronRight className="h-5 w-5 text-gray-600 dark:text-gray-400" />
            </button>
          </div>
          <button
            onClick={irParaHoje}
            className="w-full sm:w-auto px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300"
          >
            Hoje
          </button>
        </div>

        {/* Calendar Grid */}
        {isLoading ? (
          <div className="flex items-center justify-center h-96">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : (
          <div className="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden">
            {/* Header */}
            <div className="grid grid-cols-7 bg-gray-50 dark:bg-gray-700">
              {DIAS_SEMANA.map((dia) => (
                <div
                  key={dia}
                  className="px-1 sm:px-2 py-2 sm:py-3 text-center text-xs font-medium text-gray-700 dark:text-gray-300 uppercase"
                >
                  <span className="hidden sm:inline">{dia}</span>
                  <span className="sm:hidden">{dia.charAt(0)}</span>
                </div>
              ))}
            </div>

            {/* Days */}
            <div className="grid grid-cols-7">
              {diasDoMes.map(({ dia, mesAtual: isMesAtual, data }, index) => {
                const dataStr = formatarData(data);
                const eventosNoDia = eventosMap[dataStr] || [];
                const isToday = isHoje(data);

                return (
                  <div
                    key={index}
                    className={`
                      min-h-[80px] sm:min-h-[100px] p-0.5 sm:p-1 border-t border-r border-gray-200 dark:border-gray-700
                      ${index % 7 === 0 ? 'border-l' : ''}
                      ${!isMesAtual ? 'bg-gray-50 dark:bg-gray-800/50' : 'bg-white dark:bg-gray-800'}
                    `}
                  >
                    <div className={`
                      flex items-center justify-center w-5 h-5 sm:w-7 sm:h-7 mb-0.5 sm:mb-1 text-xs sm:text-sm font-medium rounded-full
                      ${isToday ? 'bg-blue-600 text-white' : ''}
                      ${!isMesAtual ? 'text-gray-400 dark:text-gray-500' : 'text-gray-900 dark:text-white'}
                    `}>
                      {dia}
                    </div>
                    <div className="space-y-0.5 sm:space-y-1">
                      {eventosNoDia.slice(0, 2).map((evento) => (
                        <button
                          key={evento.id}
                          onClick={() => setEventoSelecionado(evento)}
                          className={`
                            w-full px-0.5 sm:px-1 py-0.5 text-[10px] sm:text-xs rounded truncate text-left
                            ${getEventoColor(evento.status)}
                          `}
                          title={evento.titulo}
                        >
                          <span className="hidden sm:inline">{evento.titulo}</span>
                          <span className="sm:hidden">•</span>
                        </button>
                      ))}
                      {eventosNoDia.length > 2 && (
                        <div className="text-[10px] sm:text-xs text-gray-500 dark:text-gray-400 text-center">
                          +{eventosNoDia.length - 2}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* Legend */}
      <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-3 sm:p-4">
        <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 sm:mb-3">Legenda</h3>
        <div className="flex flex-wrap gap-2 sm:gap-4">
          {[
            { status: 'AGENDADO', label: 'Agendado', color: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400' },
            { status: 'CONFIRMADO', label: 'Confirmado', color: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' },
            { status: 'REMARCADO', label: 'Remarcado', color: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400' },
            { status: 'CANCELADO', label: 'Cancelado', color: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' },
            { status: 'REALIZADO', label: 'Realizado', color: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-400' },
          ].map(({ status, label, color }) => (
            <div key={status} className="flex items-center gap-2">
              <span className={`px-2 py-0.5 text-xs rounded ${color}`}>{label}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Event Detail Modal */}
      {eventoSelecionado && (
        <EventoDetailModal
          evento={eventoSelecionado}
          onClose={() => setEventoSelecionado(null)}
        />
      )}
    </div>
  );
}

function getEventoColor(status: StatusAgendamento): string {
  const colors: Record<StatusAgendamento, string> = {
    AGENDADO: 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300 hover:bg-blue-200 dark:hover:bg-blue-900/70',
    CONFIRMADO: 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-300 hover:bg-green-200 dark:hover:bg-green-900/70',
    REMARCADO: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/50 dark:text-yellow-300 hover:bg-yellow-200 dark:hover:bg-yellow-900/70',
    CANCELADO: 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300 hover:bg-red-200 dark:hover:bg-red-900/70',
    REALIZADO: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600',
  };
  return colors[status] || colors.AGENDADO;
}

interface EventoDetailModalProps {
  evento: CalendarioEvento;
  onClose: () => void;
}

function EventoDetailModal({ evento, onClose }: EventoDetailModalProps) {
  const dataInicio = new Date(evento.inicio);
  const dataFim = new Date(evento.fim);

  return (
    <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-center justify-center p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg max-w-md w-full p-6">
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
              {evento.titulo}
            </h2>
            <span className={`inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full mt-1 ${getEventoColor(evento.status)}`}>
              {evento.status}
            </span>
          </div>
          <button
            onClick={onClose}
            className="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <Calendar className="h-5 w-5 text-gray-400" />
            <div>
              <p className="font-medium text-gray-900 dark:text-white">
                {dataInicio.toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Clock className="h-5 w-5 text-gray-400" />
            <div>
              <p className="text-gray-900 dark:text-white">
                {dataInicio.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
                {' - '}
                {dataFim.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
              </p>
            </div>
          </div>

          {evento.veiculoPlaca && (
            <div className="flex items-center gap-3">
              <Car className="h-5 w-5 text-gray-400" />
              <div>
                <p className="font-medium text-gray-900 dark:text-white">{evento.veiculoPlaca}</p>
                {evento.veiculoDescricao && (
                  <p className="text-sm text-gray-500 dark:text-gray-400">{evento.veiculoDescricao}</p>
                )}
              </div>
            </div>
          )}

          {evento.clienteNome && (
            <div className="flex items-start gap-3">
              <svg className="h-5 w-5 text-gray-400 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              <div>
                <p className="font-medium text-gray-900 dark:text-white">{evento.clienteNome}</p>
              </div>
            </div>
          )}

          <div className="flex items-start gap-3">
            <svg className="h-5 w-5 text-gray-400 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <div>
              <p className="font-medium text-gray-900 dark:text-white">{evento.tipoManutencao}</p>
            </div>
          </div>

          {evento.descricao && (
            <div className="pt-3 border-t border-gray-200 dark:border-gray-700">
              <p className="text-gray-600 dark:text-gray-400">{evento.descricao}</p>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 mt-6">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            Fechar
          </button>
          <Link
            to={`/manutencao-preventiva/agendamentos/${evento.id}`}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Ver Detalhes
          </Link>
        </div>
      </div>
    </div>
  );
}
