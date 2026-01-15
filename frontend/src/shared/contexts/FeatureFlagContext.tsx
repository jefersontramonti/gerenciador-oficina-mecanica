import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import { featureService, type OficinaFeatures } from '@/shared/services/featureService';
import { useAuth } from '@/features/auth/hooks/useAuth';

/**
 * Feature Flag Context - Gerencia o estado global das feature flags
 *
 * IMPORTANT:
 * - Features são carregadas automaticamente após autenticação bem-sucedida
 * - Dados são cacheados em localStorage para melhor UX (não são sensíveis)
 * - Cache é invalidado ao fazer logout ou ao mudar de plano
 */

const STORAGE_KEY = 'pitstop_features';

interface FeatureFlagContextType {
  /** Map de código da feature -> habilitada (boolean) */
  features: Record<string, boolean>;
  /** Se as features estão sendo carregadas */
  isLoading: boolean;
  /** Se houve erro ao carregar features */
  error: string | null;
  /** Verifica se uma feature específica está habilitada */
  isFeatureEnabled: (featureCode: string) => boolean;
  /** Verifica se todas as features fornecidas estão habilitadas */
  areAllFeaturesEnabled: (featureCodes: string[]) => boolean;
  /** Verifica se pelo menos uma das features fornecidas está habilitada */
  isAnyFeatureEnabled: (featureCodes: string[]) => boolean;
  /** Força recarregamento das features */
  refreshFeatures: () => Promise<void>;
  /** Limpa o cache de features (usado no logout) */
  clearFeatures: () => void;
}

const FeatureFlagContext = createContext<FeatureFlagContextType | undefined>(undefined);

// Load features from localStorage
const loadFeaturesFromStorage = (): Record<string, boolean> | null => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      // Verify it has the expected structure
      if (parsed && typeof parsed === 'object') {
        return parsed;
      }
    }
  } catch (error) {
    console.error('Error loading features from localStorage:', error);
  }
  return null;
};

// Save features to localStorage
const saveFeaturesToStorage = (features: Record<string, boolean>) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(features));
  } catch (error) {
    console.error('Error saving features to localStorage:', error);
  }
};

// Clear features from localStorage
const clearFeaturesFromStorage = () => {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch (error) {
    console.error('Error clearing features from localStorage:', error);
  }
};

interface FeatureFlagProviderProps {
  children: ReactNode;
}

export function FeatureFlagProvider({ children }: FeatureFlagProviderProps) {
  const { isAuthenticated, user } = useAuth();

  // Initialize from localStorage cache
  const [features, setFeatures] = useState<Record<string, boolean>>(() => {
    return loadFeaturesFromStorage() || {};
  });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Fetch features from API
  const fetchFeatures = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const response: OficinaFeatures = await featureService.getMyFeatures();
      setFeatures(response.features || {});
      saveFeaturesToStorage(response.features || {});
    } catch (err: any) {
      console.error('Error fetching features:', err);
      setError(err.message || 'Erro ao carregar funcionalidades');
      // Keep cached features on error
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Clear features cache
  const clearFeatures = useCallback(() => {
    setFeatures({});
    clearFeaturesFromStorage();
    setError(null);
  }, []);

  // Load features when authenticated
  useEffect(() => {
    if (isAuthenticated && user) {
      // SUPER_ADMIN doesn't need feature checks (has access to everything)
      // They have full access handled in isFeatureEnabled()
      if (user.perfil === 'SUPER_ADMIN') {
        setFeatures({});
        return;
      }

      // Normal users: fetch features from API
      fetchFeatures();
    }
  }, [isAuthenticated, user, fetchFeatures]);

  // Clear features on logout
  useEffect(() => {
    if (!isAuthenticated) {
      clearFeatures();
    }
  }, [isAuthenticated, clearFeatures]);

  /**
   * Check if a feature is enabled.
   * SUPER_ADMIN always returns true (full access).
   */
  const isFeatureEnabled = useCallback(
    (featureCode: string): boolean => {
      // SUPER_ADMIN has access to everything
      if (user?.perfil === 'SUPER_ADMIN') {
        return true;
      }

      // Check the features map
      return features[featureCode] === true;
    },
    [features, user?.perfil]
  );

  /**
   * Check if ALL features are enabled.
   */
  const areAllFeaturesEnabled = useCallback(
    (featureCodes: string[]): boolean => {
      return featureCodes.every((code) => isFeatureEnabled(code));
    },
    [isFeatureEnabled]
  );

  /**
   * Check if ANY feature is enabled.
   */
  const isAnyFeatureEnabled = useCallback(
    (featureCodes: string[]): boolean => {
      return featureCodes.some((code) => isFeatureEnabled(code));
    },
    [isFeatureEnabled]
  );

  const value: FeatureFlagContextType = {
    features,
    isLoading,
    error,
    isFeatureEnabled,
    areAllFeaturesEnabled,
    isAnyFeatureEnabled,
    refreshFeatures: fetchFeatures,
    clearFeatures,
  };

  return (
    <FeatureFlagContext.Provider value={value}>
      {children}
    </FeatureFlagContext.Provider>
  );
}

/**
 * Hook para acessar o contexto de feature flags
 *
 * @throws {Error} Se usado fora do FeatureFlagProvider
 *
 * @example
 * ```tsx
 * const { isFeatureEnabled } = useFeatureFlags();
 *
 * if (isFeatureEnabled('EMAIL_MARKETING')) {
 *   // Show email marketing UI
 * }
 * ```
 */
export function useFeatureFlags(): FeatureFlagContextType {
  const context = useContext(FeatureFlagContext);

  if (context === undefined) {
    throw new Error('useFeatureFlags deve ser usado dentro de um FeatureFlagProvider');
  }

  return context;
}
