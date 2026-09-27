import React, { useState } from 'react';
import { 
  Sliders, 
  Gauge, 
  Music, 
  Volume2, 
  Volume1, 
  VolumeX, 
  Palette, 
  RotateCcw, 
  ChevronDown, 
  ChevronUp, 
  Sparkles,
  Info
} from 'lucide-react';

const STYLE_LABELS = {
  default: { label: 'Natural / Standard', emoji: '🎙️', desc: 'Balanced, standard conversational speech' },
  cheerful: { label: 'Cheerful & Upbeat', emoji: '😊', desc: 'Positive, happy, and optimistic tone' },
  empathetic: { label: 'Empathetic & Warm', emoji: '🤝', desc: 'Caring, supportive, and understanding tone' },
  excited: { label: 'Excited & Energetic', emoji: '⚡', desc: 'High energy and enthusiasm' },
  calm: { label: 'Calm & Soothing', emoji: '🧘', desc: 'Peaceful, relaxed, and gentle delivery' },
  sad: { label: 'Somber & Serious', emoji: '🌧️', desc: 'Solemn and reflective tone' },
  friendly: { label: 'Friendly & Casual', emoji: '👋', desc: 'Approachable, warm peer delivery' },
  whispering: { label: 'Soft Whispering', emoji: '🤫', desc: 'Intimate, quiet whispered speech' },
  serious: { label: 'Professional & Formal', emoji: '👔', desc: 'Authoritative, clear broadcast tone' }
};

/**
 * SpeechSettings component for audio customization:
 * - Speed (rate) slider
 * - Pitch slider
 * - Volume slider
 * - Voice style dropdown where supported
 */
export default function SpeechSettings({
  settings = { speed: 1.0, pitch: 0, volume: 1.0, style: 'default' },
  onChange,
  supportedStyles = ['default'],
  disabled = false
}) {
  const [isOpen, setIsOpen] = useState(true);

  const speed = settings.speed ?? 1.0;
  const pitch = settings.pitch ?? 0;
  const volume = settings.volume ?? 1.0;
  const style = settings.style ?? 'default';

  const isCustomized = 
    speed !== 1.0 || 
    pitch !== 0 || 
    volume !== 1.0 || 
    (style && style !== 'default');

  const hasMultipleStyles = Array.isArray(supportedStyles) && supportedStyles.length > 1;

  const handleSpeedChange = (e) => {
    const val = parseFloat(e.target.value);
    if (typeof onChange === 'function') {
      onChange({ ...settings, speed: val });
    }
  };

  const handlePitchChange = (e) => {
    const val = parseInt(e.target.value, 10);
    if (typeof onChange === 'function') {
      onChange({ ...settings, pitch: val });
    }
  };

  const handleVolumeChange = (e) => {
    const val = parseFloat(e.target.value);
    if (typeof onChange === 'function') {
      onChange({ ...settings, volume: val });
    }
  };

  const handleStyleChange = (e) => {
    if (typeof onChange === 'function') {
      onChange({ ...settings, style: e.target.value });
    }
  };

  const handleReset = (e) => {
    e.stopPropagation();
    if (typeof onChange === 'function') {
      onChange({
        speed: 1.0,
        pitch: 0,
        volume: 1.0,
        style: 'default'
      });
    }
  };

  // Speed description helper
  const getSpeedLabel = (s) => {
    if (s <= 0.75) return 'Slow';
    if (s >= 1.5) return 'Fast';
    if (s > 1.0) return 'Brisk';
    if (s < 1.0) return 'Gentle';
    return 'Normal';
  };

  // Pitch description helper
  const getPitchLabel = (p) => {
    if (p > 0) return `+${p}% (Higher)`;
    if (p < 0) return `${p}% (Deeper)`;
    return '0% (Natural)';
  };

  // Volume icon helper
  const renderVolumeIcon = () => {
    if (volume === 0) return <VolumeX className="w-3.5 h-3.5 text-rose-400" aria-hidden="true" />;
    if (volume < 0.5) return <Volume1 className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />;
    return <Volume2 className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />;
  };

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 overflow-hidden shadow-sm transition-colors">
      {/* Header bar (Collapsible trigger) */}
      <button
        type="button"
        id="speech-settings-toggle"
        onClick={() => setIsOpen(!isOpen)}
        aria-expanded={isOpen}
        aria-controls="speech-settings-panel"
        className="w-full px-4 sm:px-5 py-3.5 flex items-center justify-between text-left hover:bg-slate-850 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 cursor-pointer"
      >
        <div className="flex items-center gap-3 flex-wrap">
          <div className="p-1.5 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
            <Sliders className="w-4 h-4" aria-hidden="true" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-200">
                Audio Customization
              </span>
              {isCustomized && (
                <span className="w-2 h-2 rounded-full bg-indigo-400" title="Custom settings active" />
              )}
            </div>
            <p className="text-[11px] text-slate-400">
              Fine-tune speaking rate, pitch, volume, and expressive voice style
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Active summary badges */}
          <div className="hidden sm:flex items-center gap-1.5 text-[11px] font-mono tabular-nums">
            <span className="px-2 py-0.5 rounded-md bg-slate-800 border border-slate-700/80 text-slate-300">
              {speed.toFixed(2)}x
            </span>
            <span className="px-2 py-0.5 rounded-md bg-slate-800 border border-slate-700/80 text-slate-300">
              {pitch >= 0 ? `+${pitch}%` : `${pitch}%`}
            </span>
            <span className="px-2 py-0.5 rounded-md bg-slate-800 border border-slate-700/80 text-slate-300">
              {Math.round(volume * 100)}%
            </span>
            {hasMultipleStyles && style !== 'default' && (
              <span className="px-2 py-0.5 rounded-md bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 font-sans capitalize">
                {style}
              </span>
            )}
          </div>

          {/* Reset button */}
          {isCustomized && (
            <button
              type="button"
              id="speech-settings-reset"
              onClick={handleReset}
              disabled={disabled}
              aria-label="Reset audio customization settings to defaults"
              title="Reset all settings to default"
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-slate-200 border border-slate-700/60 transition-colors disabled:opacity-40"
            >
              <RotateCcw className="w-3.5 h-3.5" aria-hidden="true" />
            </button>
          )}

          <div className="text-slate-400">
            {isOpen ? <ChevronUp className="w-4 h-4" aria-hidden="true" /> : <ChevronDown className="w-4 h-4" aria-hidden="true" />}
          </div>
        </div>
      </button>

      {/* Expanded Controls Body */}
      {isOpen && (
        <div id="speech-settings-panel" className="p-4 sm:p-5 border-t border-slate-800 space-y-4 text-xs">
          {/* 1. Speed & Pitch Sliders in responsive grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Speed (Speaking Rate) Slider */}
            <div className="space-y-2 bg-slate-950/60 p-3.5 rounded-xl border border-slate-800">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="speech-speed-slider"
                  className="flex items-center gap-1.5 font-medium text-slate-200 cursor-pointer"
                >
                  <Gauge className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />
                  <span>Speaking Speed</span>
                </label>
                <div className="flex items-center gap-1.5">
                  <span className="text-[11px] text-slate-400 font-mono">
                    {getSpeedLabel(speed)}
                  </span>
                  <span className="px-2 py-0.5 rounded-lg bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 font-mono font-bold text-[11px] tabular-nums">
                    {speed.toFixed(2)}x
                  </span>
                </div>
              </div>

              {/* Slider Input */}
              <input
                id="speech-speed-slider"
                type="range"
                min="0.5"
                max="2.0"
                step="0.05"
                value={speed}
                onChange={handleSpeedChange}
                disabled={disabled}
                aria-label={`Speaking speed: ${speed.toFixed(2)}x`}
                className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500 disabled:opacity-50"
              />

              {/* Preset buttons */}
              <div className="flex items-center justify-between gap-1.5 pt-1">
                {[0.75, 1.0, 1.25, 1.5].map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => typeof onChange === 'function' && onChange({ ...settings, speed: preset })}
                    disabled={disabled}
                    className={`flex-1 py-1.5 rounded-lg text-[11px] font-medium transition-colors border min-h-[32px] ${
                      Math.abs(speed - preset) < 0.01
                        ? 'bg-indigo-600 text-white border-indigo-500'
                        : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                    } disabled:opacity-40 cursor-pointer`}
                  >
                    {preset === 1.0 ? '1.0x (Normal)' : `${preset}x`}
                  </button>
                ))}
              </div>
            </div>

            {/* Pitch Slider */}
            <div className="space-y-2 bg-slate-950/60 p-3.5 rounded-xl border border-slate-800">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="speech-pitch-slider"
                  className="flex items-center gap-1.5 font-medium text-slate-200 cursor-pointer"
                >
                  <Music className="w-3.5 h-3.5 text-purple-400" aria-hidden="true" />
                  <span>Voice Pitch</span>
                </label>
                <span className="px-2 py-0.5 rounded-lg bg-purple-500/10 border border-purple-500/30 text-purple-300 font-mono font-bold text-[11px] tabular-nums">
                  {getPitchLabel(pitch)}
                </span>
              </div>

              {/* Pitch Slider Input */}
              <input
                id="speech-pitch-slider"
                type="range"
                min="-20"
                max="20"
                step="1"
                value={pitch}
                onChange={handlePitchChange}
                disabled={disabled}
                aria-label={`Voice pitch: ${getPitchLabel(pitch)}`}
                className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-purple-500 disabled:opacity-50"
              />

              {/* Preset buttons */}
              <div className="flex items-center justify-between gap-1.5 pt-1">
                {[
                  { label: '-10% (Deep)', val: -10 },
                  { label: '0% (Natural)', val: 0 },
                  { label: '+10% (High)', val: 10 }
                ].map((preset) => (
                  <button
                    key={preset.val}
                    type="button"
                    onClick={() => typeof onChange === 'function' && onChange({ ...settings, pitch: preset.val })}
                    disabled={disabled}
                    className={`flex-1 py-1.5 rounded-lg text-[11px] font-medium transition-colors border min-h-[32px] ${
                      pitch === preset.val
                        ? 'bg-purple-600 text-white border-purple-500'
                        : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                    } disabled:opacity-40 cursor-pointer`}
                  >
                    {preset.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* 2. Volume Slider & Voice Style Dropdown */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Volume Slider */}
            <div className="space-y-2 bg-slate-950/60 p-3.5 rounded-xl border border-slate-800">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="speech-volume-slider"
                  className="flex items-center gap-1.5 font-medium text-slate-200 cursor-pointer"
                >
                  {renderVolumeIcon()}
                  <span>Volume Level</span>
                </label>
                <span className="px-2 py-0.5 rounded-lg bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 font-mono font-bold text-[11px] tabular-nums">
                  {volume === 0 ? 'Muted' : `${Math.round(volume * 100)}%`}
                </span>
              </div>

              {/* Slider Input */}
              <input
                id="speech-volume-slider"
                type="range"
                min="0.0"
                max="1.0"
                step="0.05"
                value={volume}
                onChange={handleVolumeChange}
                disabled={disabled}
                aria-label={`Volume: ${Math.round(volume * 100)}%`}
                className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500 disabled:opacity-50"
              />

              {/* Quick Volume Levels */}
              <div className="flex items-center justify-between gap-1.5 pt-1">
                {[0.25, 0.5, 0.75, 1.0].map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => typeof onChange === 'function' && onChange({ ...settings, volume: preset })}
                    disabled={disabled}
                    className={`flex-1 py-1.5 rounded-lg text-[11px] font-medium transition-colors border min-h-[32px] ${
                      Math.abs(volume - preset) < 0.01
                        ? 'bg-indigo-600 text-white border-indigo-500'
                        : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                    } disabled:opacity-40 cursor-pointer`}
                  >
                    {Math.round(preset * 100)}%
                  </button>
                ))}
              </div>
            </div>

            {/* Voice Style Dropdown (where supported) */}
            <div className="space-y-2 bg-slate-950/60 p-3.5 rounded-xl border border-slate-800 flex flex-col justify-between">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="speech-style-select"
                  className="flex items-center gap-1.5 font-medium text-slate-200 cursor-pointer"
                >
                  <Palette className="w-3.5 h-3.5 text-pink-400" aria-hidden="true" />
                  <span>Voice Delivery Mood</span>
                </label>
                {hasMultipleStyles ? (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 font-semibold text-[10px]">
                    <Sparkles className="w-2.5 h-2.5" aria-hidden="true" />
                    <span>Styles Supported</span>
                  </span>
                ) : (
                  <span className="px-2 py-0.5 rounded-lg bg-slate-800 border border-slate-700/80 text-slate-400 text-[10px]">
                    Default Only
                  </span>
                )}
              </div>

              {/* Dropdown Select */}
              <div className="relative">
                <select
                  id="speech-style-select"
                  value={style}
                  onChange={handleStyleChange}
                  disabled={disabled || !hasMultipleStyles}
                  className="w-full min-h-[42px] appearance-none bg-slate-900 border border-slate-700/80 rounded-xl px-3.5 py-2 text-xs text-slate-200 font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/40 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                >
                  {hasMultipleStyles ? (
                    supportedStyles.map((s) => {
                      const meta = STYLE_LABELS[s] || { label: s, emoji: '✨', desc: s };
                      return (
                        <option key={s} value={s} className="bg-slate-900 text-slate-200">
                          {meta.emoji} {meta.label}
                        </option>
                      );
                    })
                  ) : (
                    <option value="default" className="bg-slate-900 text-slate-200">
                      🎙️ Natural / Standard (Default)
                    </option>
                  )}
                </select>
                <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
                  <ChevronDown className="w-3.5 h-3.5" aria-hidden="true" />
                </div>
              </div>

              {/* Informative helper note for style */}
              <div className="flex items-center gap-1.5 text-[11px] text-slate-400 pt-0.5">
                <Info className="w-3 h-3 text-slate-500 flex-shrink-0" aria-hidden="true" />
                <span>
                  {hasMultipleStyles 
                    ? (STYLE_LABELS[style]?.desc || 'Custom delivery mode')
                    : 'This voice uses natural delivery. Expressive styles available on Neural English voices.'}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export { SpeechSettings };
