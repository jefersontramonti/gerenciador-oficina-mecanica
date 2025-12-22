/**
 * Error logging service
 *
 * Centralizes error logging for monitoring and debugging.
 * In production, this should integrate with services like Sentry, LogRocket, etc.
 */

interface ErrorContext {
  componentStack?: string;
  [key: string]: any;
}

interface ErrorLog {
  timestamp: string;
  error: Error;
  errorInfo?: ErrorContext;
  userAgent: string;
  url: string;
}

class ErrorLogger {
  private logs: ErrorLog[] = [];
  private maxLogs = 50; // Keep last 50 errors in memory

  /**
   * Log an error with context
   */
  logError(error: Error, errorInfo?: ErrorContext): void {
    const errorLog: ErrorLog = {
      timestamp: new Date().toISOString(),
      error: {
        name: error.name,
        message: error.message,
        stack: error.stack || undefined,
      } as Error,
      errorInfo,
      userAgent: navigator.userAgent,
      url: window.location.href,
    };

    // Add to in-memory logs
    this.logs.unshift(errorLog);
    if (this.logs.length > this.maxLogs) {
      this.logs.pop();
    }

    // Console log in development
    if (import.meta.env.DEV) {
      console.group('🔴 Error Logged');
      console.error('Error:', error);
      console.error('Error Info:', errorInfo);
      console.error('Stack:', error.stack);
      console.groupEnd();
    }

    // TODO: Send to error tracking service in production
    // if (import.meta.env.PROD) {
    //   this.sendToSentry(errorLog);
    // }
  }

  /**
   * Log a React error boundary error
   */
  logBoundaryError(error: Error, errorInfo: React.ErrorInfo): void {
    this.logError(error, {
      componentStack: errorInfo.componentStack || undefined,
      boundary: 'ErrorBoundary',
    });
  }

  /**
   * Log a global unhandled error
   */
  logGlobalError(event: ErrorEvent): void {
    const error = new Error(event.message);
    error.stack = event.error?.stack || undefined;

    this.logError(error, {
      type: 'unhandledError',
      filename: event.filename,
      lineno: event.lineno,
      colno: event.colno,
    });
  }

  /**
   * Log an unhandled promise rejection
   */
  logUnhandledRejection(event: PromiseRejectionEvent): void {
    const error = event.reason instanceof Error
      ? event.reason
      : new Error(String(event.reason));

    this.logError(error, {
      type: 'unhandledRejection',
      promise: event.promise,
    });
  }

  /**
   * Get recent error logs
   */
  getRecentLogs(count = 10): ErrorLog[] {
    return this.logs.slice(0, count);
  }

  /**
   * Clear all logs
   */
  clearLogs(): void {
    this.logs = [];
  }

  /**
   * Send error to Sentry (placeholder for future implementation)
   *
   * Uncomment and implement when Sentry is configured
   */
  // private sendToSentry(errorLog: ErrorLog): void {
  //   // TODO: Implement Sentry integration
  //   // Sentry.captureException(errorLog.error, {
  //   //   contexts: {
  //   //     errorInfo: errorLog.errorInfo,
  //   //   },
  //   //   tags: {
  //   //     url: errorLog.url,
  //   //   },
  //   // });
  // }

  /**
   * Send error to custom backend logging endpoint (placeholder)
   *
   * Uncomment and implement when backend logging endpoint is ready
   */
  // private async sendToBackend(errorLog: ErrorLog): Promise<void> {
  //   try {
  //     // TODO: Implement backend logging
  //     // await fetch('/api/logs/errors', {
  //     //   method: 'POST',
  //     //   headers: { 'Content-Type': 'application/json' },
  //     //   body: JSON.stringify(errorLog),
  //     // });
  //   } catch (err) {
  //     // Silently fail - don't want logging errors to crash the app
  //     console.error('Failed to send error to backend:', err);
  //   }
  // }
}

// Singleton instance
export const errorLogger = new ErrorLogger();

/**
 * Initialize global error handlers
 */
export function initializeErrorHandlers(): void {
  // Catch unhandled errors
  window.addEventListener('error', (event) => {
    errorLogger.logGlobalError(event);
  });

  // Catch unhandled promise rejections
  window.addEventListener('unhandledrejection', (event) => {
    errorLogger.logUnhandledRejection(event);
  });
}
