import { useSelector, type TypedUseSelectorHook } from 'react-redux';
import type { RootState } from '../store';

/**
 * Typed useSelector hook for Redux
 */
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
