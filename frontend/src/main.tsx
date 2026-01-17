import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { ThemeProvider, FeatureFlagProvider } from './shared/contexts';
import { store } from './shared/store';
import { queryClient } from './config/queryClient';
import { initializeErrorHandlers } from './shared/services/errorLogger';
import './index.css';
import App from './App.tsx';

// Initialize global error handlers for unhandled errors and promise rejections
initializeErrorHandlers();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <Provider store={store}>
        <QueryClientProvider client={queryClient}>
          <FeatureFlagProvider>
            <App />
          </FeatureFlagProvider>
          <Toaster
            position="top-right"
            containerStyle={{
              top: 20,
              right: 20,
              zIndex: 99999,
            }}
            toastOptions={{
              duration: 4000,
              className: 'dark:bg-gray-800 dark:text-gray-100 dark:border-gray-700',
              style: {
                background: '#1f2937',
                color: '#f9fafb',
                border: '1px solid #374151',
                padding: '12px 16px',
                fontSize: '14px',
                zIndex: 99999,
              },
              success: {
                iconTheme: {
                  primary: '#10b981',
                  secondary: '#f9fafb',
                },
              },
              error: {
                iconTheme: {
                  primary: '#ef4444',
                  secondary: '#f9fafb',
                },
              },
            }}
          />
        </QueryClientProvider>
      </Provider>
    </ThemeProvider>
  </StrictMode>
);
