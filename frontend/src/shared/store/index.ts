import { configureStore } from '@reduxjs/toolkit';
import authReducer from '@/features/auth/store/authSlice';

/**
 * Redux store configuration
 * UI state management with Redux Toolkit
 */
export const store = configureStore({
  reducer: {
    auth: authReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        // Ignore these action types for serialization checks
        ignoredActions: ['persist/PERSIST'],
      },
    }),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
