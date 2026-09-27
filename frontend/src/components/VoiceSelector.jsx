import React, { useEffect, useState } from 'react';
import { Mic, ChevronDown, RefreshCw } from 'lucide-react';
import { ttsService } from '../services/ttsService';
import { MOCK_VOICES_BY_LANGUAGE } from '../utils/voiceData';

/**
 * Voice Selector Dropdown
 * Retrieves available voices from backend /api/voices and filters dynamically by language.
 * Falls back to local voice configuration if backend is temporarily unreachable.
 *
 * @param {string} selectedLanguage - Current active language code/id (e.g. 'en', 'en-US')
 * @param {string} selectedVoice - Current active voice id
 * @param {Function} onChange - Callback when voice selection changes
 * @param {Array} [voices] - Optional external voice list from parent
 * @param {boolean} [isLoading] - External loading state
 * @param {boolean} disabled - Disable state
 */
export default function VoiceSelector({
  selectedLanguage = 'en',
  selectedVoice,
  onChange,
  voices: externalVoices = null,
  isLoading: externalLoading = null,
  disabled = false,
  id = 'voice-selector',
  className = ''
}) {
  const [internalVoices, setInternalVoices] = useState([]);
  const [isLoadingVoices, setIsLoadingVoices] = useState(false);
  const [isBackendSource, setIsBackendSource] = useState(false);

  // If voices are not provided by parent, fetch from backend
  useEffect(() => {
    if (externalVoices !== null) return;

    let isMounted = true;
    async function fetchVoicesFromBackend() {
      setIsLoadingVoices(true);
      try {
        const data = await ttsService.getVoices();
        if (isMounted && Array.isArray(data) && data.length > 0) {
          setInternalVoices(data);
          setIsBackendSource(true);
        }
      } catch (err) {
        console.warn('[VoiceSelector] Backend voices fetch failed, using fallback:', err.userFriendlyMessage);
        if (isMounted) {
          setIsBackendSource(false);
        }
      } finally {
        if (isMounted) {
          setIsLoadingVoices(false);
        }
      }
    }

    fetchVoicesFromBackend();
    return () => {
      isMounted = false;
    };
  }, [externalVoices]);

  // Determine active voices list (external prop, internal fetched, or mock fallback)
  const allVoices = externalVoices !== null ? externalVoices : internalVoices;
  const isLoading = externalLoading !== null ? externalLoading : isLoadingVoices;

  // Filter voices matching selected language (e.g. 'en' matches 'en-US', or exact 'en-US')
  const langKey = selectedLanguage ? selectedLanguage.toLowerCase() : 'en';
  
  let currentVoices = [];
  if (allVoices && allVoices.length > 0) {
    currentVoices = allVoices.filter((v) => {
      const vLang = (v.language || '').toLowerCase();
      return vLang === langKey || vLang.startsWith(langKey) || langKey.startsWith(vLang.substring(0, 2));
    });
  }

  // Fallback to local mock voices if backend list has no match for this language
  if (currentVoices.length === 0 && !isLoading) {
    const fallbackList = MOCK_VOICES_BY_LANGUAGE[langKey.substring(0, 2)] || MOCK_VOICES_BY_LANGUAGE.en || [];
    currentVoices = fallbackList;
  }

  // Auto-select first matching voice when language changes or if selectedVoice is not in list
  useEffect(() => {
    if (currentVoices.length > 0) {
      const exists = currentVoices.some((v) => v.id === selectedVoice);
      if (!exists && onChange) {
        onChange(currentVoices[0].id);
      }
    }
  }, [selectedLanguage, currentVoices, selectedVoice, onChange]);

  const handleChange = (e) => {
    if (onChange) {
      onChange(e.target.value);
    }
  };

  return (
    <div className={`space-y-1.5 ${className}`}>
      <div className="flex items-center justify-between">
        <label
          htmlFor={id}
          className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-slate-300"
        >
          <Mic className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />
          <span>Voice Model</span>
          {isLoading && <RefreshCw className="w-3 h-3 text-indigo-400 animate-spin" aria-hidden="true" />}
        </label>
        <span className="text-[11px] text-slate-400 font-mono">
          {isLoading
            ? 'Loading...'
            : `${currentVoices.length} ${currentVoices.length === 1 ? 'voice' : 'voices'}`}
        </span>
      </div>

      <div className="relative">
        <select
          id={id}
          value={selectedVoice || (currentVoices[0]?.id || '')}
          onChange={handleChange}
          disabled={disabled || isLoading || currentVoices.length === 0}
          className="w-full min-h-[44px] appearance-none rounded-xl border border-slate-700/80 bg-slate-900 px-3.5 py-2.5 pr-10 text-sm font-medium text-slate-100 shadow-sm transition-colors hover:border-slate-600 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/30 disabled:cursor-not-allowed disabled:opacity-50 cursor-pointer"
        >
          {currentVoices.length > 0 ? (
            currentVoices.map((voice) => (
              <option
                key={voice.id}
                value={voice.id}
                className="bg-slate-900 text-slate-100 py-1"
              >
                {voice.name || voice.label} ({voice.gender || 'Standard'}) {voice.language ? `• ${voice.language}` : ''}
              </option>
            ))
          ) : (
            <option value="" disabled>
              {isLoading ? 'Fetching voices from server...' : 'No voices available for this language'}
            </option>
          )}
        </select>

        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3.5 text-slate-400">
          <ChevronDown className="w-4 h-4" aria-hidden="true" />
        </div>
      </div>
    </div>
  );
}
