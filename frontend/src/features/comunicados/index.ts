// Pages
export { ComunicadosPage } from './pages/ComunicadosPage';

// Components
export { ComunicadoAlert, ComunicadoBadge } from './components/ComunicadoAlert';

// Hooks
export {
  useComunicadosOficina,
  useComunicadoDetail,
  useComunicadosNaoLidos,
  useComunicadoAlerta,
  useComunicadosLogin,
  useConfirmarComunicado,
  useMarcarTodosLidos,
} from './hooks/useComunicados';

// Types
export type { ComunicadoOficina, ComunicadoOficinaDetail, ComunicadoAlerta } from './types';
