import { useDispatch } from 'react-redux';
import type { AppDispatch } from '../store';

/**
 * Typed useDispatch hook for Redux
 */
export const useAppDispatch = () => useDispatch<AppDispatch>();
