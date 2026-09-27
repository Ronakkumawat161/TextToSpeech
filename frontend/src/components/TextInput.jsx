import React from 'react';
import { FileText, X, AlertCircle } from 'lucide-react';

/**
 * Large Accessible Text Input Component for Speech Synthesis
 *
 * @param {string} value - Current text value
 * @param {Function} onChange - Callback on text change
 * @param {number} maxLength - Max allowed character limit
 * @param {string} placeholder - Placeholder text
 * @param {string} validationError - Validation message if any
 * @param {boolean} disabled - Disable state
 * @param {Function} onClear - Callback to clear the input
 */
export default function TextInput({
  value = '',
  onChange,
  maxLength = 5000,
  placeholder = 'Type or paste the text you would like to synthesize into natural speech...',
  validationError = '',
  disabled = false,
  onClear,
  id = 'tts-text-input',
  className = ''
}) {
  const charCount = value ? value.length : 0;
  const wordCount = value && value.trim() ? value.trim().split(/\s+/).filter(Boolean).length : 0;
  const isNearLimit = charCount >= maxLength * 0.9;
  const isAtLimit = charCount >= maxLength;

  const handleChange = (e) => {
    const text = e.target.value;
    if (text.length <= maxLength) {
      if (onChange) onChange(text);
    }
  };

  const handleClear = () => {
    if (onClear) {
      onClear();
    } else if (onChange) {
      onChange('');
    }
  };

  return (
    <div className={`space-y-2 ${className}`}>
      {/* Header with Title and Clear Action */}
      <div className="flex items-center justify-between">
        <label
          htmlFor={id}
          className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-slate-300"
        >
          <FileText className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />
          <span>Speech Input Text</span>
        </label>

        {value.length > 0 && (
          <button
            type="button"
            onClick={handleClear}
            disabled={disabled}
            aria-label="Clear all input text"
            className="inline-flex items-center gap-1 text-xs font-medium text-slate-400 hover:text-rose-400 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-400/40 rounded px-1.5 py-0.5"
          >
            <X className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Clear text</span>
          </button>
        )}
      </div>

      {/* Main Textarea Container */}
      <div className="relative">
        <textarea
          id={id}
          value={value}
          onChange={handleChange}
          disabled={disabled}
          maxLength={maxLength}
          rows={7}
          placeholder={placeholder}
          aria-invalid={Boolean(validationError)}
          aria-describedby={
            validationError ? `${id}-error ${id}-counter` : `${id}-counter`
          }
          className={`w-full rounded-2xl border bg-slate-900 p-4 text-sm sm:text-base text-slate-100 placeholder-slate-500 shadow-inner transition-colors duration-150 focus:outline-none focus:ring-2 resize-y min-h-[160px] leading-relaxed ${
            validationError
              ? 'border-rose-500/80 focus:border-rose-500 focus:ring-rose-500/25'
              : 'border-slate-700/80 focus:border-indigo-500 focus:ring-indigo-500/25'
          } disabled:cursor-not-allowed disabled:opacity-50`}
        />
      </div>

      {/* Footer bar with Validation Message and Counters */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
        {/* Validation error */}
        <div className="min-h-[20px] flex items-center">
          {validationError ? (
            <div
              id={`${id}-error`}
              role="alert"
              aria-live="polite"
              className="flex items-center gap-1.5 text-rose-400 font-medium"
            >
              <AlertCircle className="w-4 h-4 flex-shrink-0" aria-hidden="true" />
              <span>{validationError}</span>
            </div>
          ) : (
            <span className="text-slate-500 text-[11px]">
              Natural punctuation (commas, periods, question marks) shapes realistic pauses and intonation.
            </span>
          )}
        </div>

        {/* Word and Character counters */}
        <div
          id={`${id}-counter`}
          className="flex items-center justify-end gap-2.5 text-slate-400 font-mono text-[11px] sm:text-xs tabular-nums"
        >
          <span>
            <strong className="text-slate-200">{wordCount}</strong> {wordCount === 1 ? 'word' : 'words'}
          </span>
          <span className="text-slate-600">&bull;</span>
          <span
            className={
              isAtLimit
                ? 'text-rose-400 font-semibold'
                : isNearLimit
                ? 'text-amber-400 font-semibold'
                : 'text-slate-400'
            }
          >
            <strong className="text-slate-200">{charCount.toLocaleString()}</strong> / {maxLength.toLocaleString()} chars
          </span>
        </div>
      </div>
    </div>
  );
}
