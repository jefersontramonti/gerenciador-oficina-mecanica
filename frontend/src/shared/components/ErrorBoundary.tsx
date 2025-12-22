import React, { Component, type ReactNode } from 'react';
import { errorLogger } from '../services/errorLogger';
import { ErrorFallback } from './ErrorFallback';

interface ErrorBoundaryProps {
  /**
   * Child components to protect
   */
  children: ReactNode;

  /**
   * Optional custom fallback component
   */
  fallback?: (error: Error, resetError: () => void) => ReactNode;

  /**
   * Optional callback when error occurs
   */
  onError?: (error: Error, errorInfo: React.ErrorInfo) => void;

  /**
   * Optional identifier for this boundary (for logging)
   */
  boundaryName?: string;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/**
 * Error Boundary Component
 *
 * Catches JavaScript errors anywhere in the child component tree,
 * logs those errors, and displays a fallback UI instead of crashing.
 *
 * @example
 * ```tsx
 * <ErrorBoundary>
 *   <YourComponent />
 * </ErrorBoundary>
 * ```
 *
 * @example With custom fallback
 * ```tsx
 * <ErrorBoundary
 *   fallback={(error, reset) => (
 *     <CustomErrorUI error={error} onReset={reset} />
 *   )}
 * >
 *   <YourComponent />
 * </ErrorBoundary>
 * ```
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
    };
  }

  /**
   * Update state when error is caught
   */
  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return {
      hasError: true,
      error,
    };
  }

  /**
   * Log error when caught
   */
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    const { onError, boundaryName } = this.props;

    // Log to error logger service
    errorLogger.logBoundaryError(error, errorInfo);

    // Add boundary name to context if provided
    if (boundaryName) {
      console.group(`🛡️ Error Boundary: ${boundaryName}`);
      console.error('Error:', error);
      console.error('Component Stack:', errorInfo.componentStack);
      console.groupEnd();
    }

    // Call custom error handler if provided
    onError?.(error, errorInfo);
  }

  /**
   * Reset error state and try to recover
   */
  resetError = (): void => {
    this.setState({
      hasError: false,
      error: null,
    });
  };

  render(): ReactNode {
    const { hasError, error } = this.state;
    const { children, fallback } = this.props;

    if (hasError && error) {
      // Use custom fallback if provided
      if (fallback) {
        return fallback(error, this.resetError);
      }

      // Use default fallback
      return <ErrorFallback error={error} resetError={this.resetError} />;
    }

    return children;
  }
}
