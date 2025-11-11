import toast from 'react-hot-toast';

/**
 * Utility functions for displaying toast notifications
 * Wraps react-hot-toast for consistent notification styling
 */

/**
 * Display a success notification
 * @param message Success message to display
 */
export const showSuccess = (message: string) => {
  toast.success(message);
};

/**
 * Display an error notification
 * @param message Error message to display
 */
export const showError = (message: string) => {
  toast.error(message);
};

/**
 * Display an informational notification
 * @param message Info message to display
 */
export const showInfo = (message: string) => {
  toast(message, { icon: 'ℹ️' });
};

/**
 * Display a warning notification
 * @param message Warning message to display
 */
export const showWarning = (message: string) => {
  toast(message, {
    icon: '⚠️',
    duration: 5000,
    style: {
      border: '1px solid #f59e0b',
    }
  });
};

/**
 * Display a toast for async promise operations
 * Shows loading, success, and error states automatically
 * @param promise Promise to track
 * @param messages Messages for each state
 * @returns The promise result
 */
export const showPromise = <T,>(
  promise: Promise<T>,
  messages: {
    loading: string;
    success: string;
    error: string;
  }
) => {
  return toast.promise(promise, messages);
};

/**
 * Display a loading notification that can be manually dismissed
 * @param message Loading message to display
 * @returns Toast ID for dismissal
 */
export const showLoading = (message: string) => {
  return toast.loading(message);
};

/**
 * Dismiss a specific toast notification
 * @param toastId Toast ID to dismiss
 */
export const dismissToast = (toastId: string) => {
  toast.dismiss(toastId);
};

/**
 * Dismiss all active toast notifications
 */
export const dismissAllToasts = () => {
  toast.dismiss();
};
