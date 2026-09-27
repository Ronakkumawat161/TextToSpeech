import React from 'react';
import { Sparkles } from 'lucide-react';
import LoadingSpinner from './LoadingSpinner';

/**
 * Primary Speech Generation Button
 * Handles loading states, validation locks, and prevents duplicate requests.
 *
 * @param {Function} onClick - Action trigger
 * @param {boolean} isLoading - Loading/synthesizing state
 * @param {boolean} disabled - Whether button is disabled
 * @param {string} text - Button label
 * @param {string} loadingText - Label while synthesizing
 */
export default function GenerateButton({
  onClick,
  isLoading = false,
  disabled = false,
  text = 'Generate Speech',
  loadingText = 'Synthesizing Audio...',
  id = 'generate-speech-button',
  className = ''
}) {
  const handleClick = (e) => {
    // Strictly prevent duplicate requests when already loading or disabled
    if (isLoading || disabled) {
      e.preventDefault();
      return;
    }
    if (onClick) {
      onClick(e);
    }
  };

  const isButtonDisabled = disabled || isLoading;

  return (
    <button
      id={id}
      type="button"
      onClick={handleClick}
      disabled={isButtonDisabled}
      aria-busy={isLoading}
      className={`relative inline-flex items-center justify-center gap-2 rounded-xl px-6 py-3 min-h-[44px] text-sm sm:text-base font-semibold text-white shadow-sm transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 ${
        isButtonDisabled
          ? 'bg-slate-800 text-slate-500 cursor-not-allowed border border-slate-750 shadow-none'
          : 'bg-indigo-600 hover:bg-indigo-500 active:bg-indigo-700 shadow-indigo-600/20 active:scale-[0.99] cursor-pointer'
      } ${className}`}
    >
      {isLoading ? (
        <>
          <LoadingSpinner size="sm" className="text-white" />
          <span>{loadingText}</span>
        </>
      ) : (
        <>
          <Sparkles className="w-4 h-4 text-indigo-200" aria-hidden="true" />
          <span>{text}</span>
        </>
      )}
    </button>
  );
}
