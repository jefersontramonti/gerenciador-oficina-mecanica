/**
 * DTO com informações de uso vs limites do plano.
 */
export interface UsoLimites {
  planoNome: string;
  planoCodigo: string;

  // Usuários
  limiteUsuarios: number;
  usuariosAtivos: number;
  percentualUsuarios: number;
  usuariosIlimitados: boolean;

  // OS por mês
  limiteOsMes: number;
  osNoMes: number;
  percentualOsMes: number;
  osIlimitadas: boolean;

  // Mês de referência
  mesReferencia: number;
  anoReferencia: number;
}
