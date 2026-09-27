import React from 'react';
import { Loader2 } from 'lucide-react';

/**
 * Reusable Loading Spinner Component
 * @param {'sm' | 'md' | 'lg' | 'xl'} size - Size of the spinner
 * @param {string} label - Optional accessible or visible status text
 * @param {string} className - Additional CSS classes
 */
export default function LoadingSpinner({
  size = 'md',
  label = 'Loading...',
  showLabel = false,
  className = ''
}) {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-5 h-5',
    lg: 'w-7 h-7',
    xl: 'w-10 h-10',
  };

  return (
    <div
      role="status"
      aria-live="polite"
      className={`inline-flex items-center justify-center gap-2.5 text-indigo-400 ${className}`}
    >
      <Loader2
        className={`animate-spin ${sizeClasses[size] || sizeClasses.md} flex-shrink-0`}
        aria-hidden="true"
      />
      {showLabel && label && (
        <span className="text-sm font-medium text-slate-300">{label}</span>
      )}
      <span className="sr-only">{label}</span>
    </div>
  );
}
