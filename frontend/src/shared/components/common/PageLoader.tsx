/**
 * PageLoader - Loading indicator for lazy-loaded pages
 * Used as Suspense fallback during code splitting
 */
export function PageLoader() {
  return (
    <div className="flex h-screen items-center justify-center bg-gray-50 dark:bg-gray-900">
      <div className="text-center">
        {/* Spinner */}
        <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-blue-600 border-t-transparent dark:border-blue-500" />

        {/* Loading text */}
        <p className="mt-4 text-sm text-gray-600 dark:text-gray-400">
          Carregando página...
        </p>
      </div>
    </div>
  );
}
