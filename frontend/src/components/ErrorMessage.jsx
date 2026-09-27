import React from 'react';
import { AlertCircle, X, RotateCcw } from 'lucide-react';

/**
 * Reusable Error Message Component
 * @param {string} title - Optional short error title
 * @param {string} message - Error explanation message
 * @param {Function} onDismiss - Optional callback when user dismisses the error
 * @param {Function} onRetry - Optional callback to retry the failed operation
 * @param {string} className - Optional container styling
 */
export default function ErrorMessage({
  title = 'Something went wrong',
  message,
  onDismiss,
  onRetry,
  className = ''
}) {
  if (!message) return null;

  return (
    <div
      role="alert"
      aria-live="assertive"
      className={`rounded-2xl border border-rose-500/40 bg-rose-950/40 p-4 text-rose-200 shadow-sm ${className}`}
    >
      <div className="flex items-start gap-3">
        <div className="rounded-xl bg-rose-500/15 p-2 text-rose-400 flex-shrink-0 mt-0.5">
          <AlertCircle className="w-5 h-5" aria-hidden="true" />
        </div>

        <div className="flex-1 min-w-0">
          {title && (
            <h4 className="text-sm font-semibold text-rose-100 mb-0.5">
              {title}
            </h4>
          )}
          <p className="text-xs sm:text-sm text-rose-200/90 leading-relaxed">
            {message}
          </p>

          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="mt-3 inline-flex items-center gap-1.5 text-xs font-semibold text-rose-300 hover:text-white underline underline-offset-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-400 rounded px-1 py-0.5"
            >
              <RotateCcw className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Try again</span>
            </button>
          )}
        </div>

        {onDismiss && (
          <button
            type="button"
            onClick={onDismiss}
            aria-label="Dismiss error notification"
            className="rounded-lg p-1.5 text-rose-400 hover:bg-rose-900/40 hover:text-rose-100 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-400 cursor-pointer"
          >
            <X className="w-4 h-4" aria-hidden="true" />
          </button>
        )}
      </div>
    </div>
  );
}
