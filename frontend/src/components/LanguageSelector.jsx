import React from 'react';
import { Globe, ChevronDown } from 'lucide-react';
import { DEFAULT_LANGUAGES } from '../utils/voiceData';

/**
 * Configurable Language Selector Dropdown
 * @param {Array} languages - Configurable language list
 * @param {string} selectedLanguage - Currently selected language id
 * @param {Function} onChange - Callback with selected language id
 * @param {boolean} disabled - Disable state
 */
export default function LanguageSelector({
  languages = DEFAULT_LANGUAGES,
  selectedLanguage = 'en',
  onChange,
  disabled = false,
  id = 'language-selector',
  className = ''
}) {
  const handleChange = (e) => {
    if (onChange) {
      onChange(e.target.value);
    }
  };

  return (
    <div className={`space-y-1.5 ${className}`}>
      <label
        htmlFor={id}
        className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-slate-300"
      >
        <Globe className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />
        <span>Language</span>
      </label>

      <div className="relative">
        <select
          id={id}
          value={selectedLanguage}
          onChange={handleChange}
          disabled={disabled}
          className="w-full min-h-[44px] appearance-none rounded-xl border border-slate-700/80 bg-slate-900 px-3.5 py-2.5 pr-10 text-sm font-medium text-slate-100 shadow-sm transition-colors hover:border-slate-600 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/30 disabled:cursor-not-allowed disabled:opacity-50 cursor-pointer"
        >
          {languages.map((lang) => (
            <option
              key={lang.id || lang.code}
              value={lang.id}
              className="bg-slate-900 text-slate-100 py-1"
            >
              {lang.name} {lang.nativeName && lang.nativeName !== lang.name ? `(${lang.nativeName})` : ''}
            </option>
          ))}
        </select>

        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3.5 text-slate-400">
          <ChevronDown className="w-4 h-4" aria-hidden="true" />
        </div>
      </div>
    </div>
  );
}
