import React, { useState, useEffect, useCallback } from 'react';
import { 
  Sparkles, 
  RotateCcw, 
  Info, 
  Volume2, 
  CheckCircle2, 
  Headphones, 
  Sliders,
  Radio
} from 'lucide-react';
import TextInput from '../components/TextInput';
import LanguageSelector from '../components/LanguageSelector';
import VoiceSelector from '../components/VoiceSelector';
import GenerateButton from '../components/GenerateButton';
import AudioPlayer from '../components/AudioPlayer';
import DownloadButton from '../components/DownloadButton';
import SpeechSettings from '../components/SpeechSettings';
import DocumentUploader from '../components/DocumentUploader';
import AIEnhancement from '../components/AIEnhancement';
import ErrorMessage from '../components/ErrorMessage';
import LoadingSpinner from '../components/LoadingSpinner';
import { ttsService } from '../services/ttsService';
import { DEFAULT_LANGUAGES, SAMPLE_TEXTS } from '../utils/voiceData';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { API_BASE_URL } from '../utils/constants';

export default function HomePage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  // Input & Selection State
  const [text, setText] = useState('');
  const [selectedLanguage, setSelectedLanguage] = useState('en');
  const [selectedVoice, setSelectedVoice] = useState('en-female-1');

  // Backend Voices State
  const [voices, setVoices] = useState([]);
  const [isLoadingVoices, setIsLoadingVoices] = useState(true);
  const [isBackendConnected, setIsBackendConnected] = useState(null);

  // Generation & Audio Handling State
  const [isGenerating, setIsGenerating] = useState(false);
  const [generatedAudio, setGeneratedAudio] = useState(null);
  const [generatedBlob, setGeneratedBlob] = useState(null);
  const [generationMeta, setGenerationMeta] = useState(null);

  // Audio Customization Settings (Module 12)
  const [settings, setSettings] = useState({
    speed: 1.0,
    pitch: 0,
    volume: 1.0,
    style: 'default'
  });

  // Validation & Error Handling State
  const [validationError, setValidationError] = useState('');
  const [apiError, setApiError] = useState(null);

  // Revoke object URL on unmount to prevent browser memory leaks
  useEffect(() => {
    return () => {
      if (generatedAudio) {
        URL.revokeObjectURL(generatedAudio);
      }
    };
  }, [generatedAudio]);

  // 1. Fetch available voices from Spring Boot backend on mount
  const loadVoices = useCallback(async () => {
    setIsLoadingVoices(true);
    setApiError(null);
    try {
      const data = await ttsService.getVoices();
      if (Array.isArray(data) && data.length > 0) {
        setVoices(data);
        setIsBackendConnected(true);

        // Pre-select first voice matching initial language
        const matchingVoice = data.find((v) =>
          (v.language || '').toLowerCase().startsWith(selectedLanguage.toLowerCase())
        );
        if (matchingVoice) {
          setSelectedVoice(matchingVoice.id);
        }
      }
    } catch (err) {
      console.warn('[HomePage] Failed to fetch voices from backend:', err);
      setIsBackendConnected(false);
      setApiError(
        err.userFriendlyMessage ||
        `Unable to connect to the synthesis service (${API_BASE_URL}). Please verify that the backend server is operational.`
      );
    } finally {
      setIsLoadingVoices(false);
    }
  }, [selectedLanguage]);

  useEffect(() => {
    loadVoices();
  }, [loadVoices]);

  // Handle language change: updates selection and syncs voice
  const handleLanguageChange = (newLangId) => {
    setSelectedLanguage(newLangId);
    setValidationError('');
    setApiError(null);

    // Filter available voices for selected language
    const langKey = newLangId.toLowerCase();
    const matching = voices.filter((v) => {
      const vLang = (v.language || '').toLowerCase();
      return vLang === langKey || vLang.startsWith(langKey) || langKey.startsWith(vLang.substring(0, 2));
    });

    if (matching.length > 0) {
      setSelectedVoice(matching[0].id);
    }
  };

  // Derive supported styles for currently selected voice
  const selectedVoiceObj = voices.find(
    (v) => (v.id || '').toLowerCase() === (selectedVoice || '').toLowerCase()
  );
  const supportedStyles = selectedVoiceObj?.supportedStyles || ['default'];

  // Gracefully reset style to 'default' if newly selected voice does not support the chosen style
  useEffect(() => {
    if (selectedVoice && voices.length > 0) {
      const vObj = voices.find((v) => (v.id || '').toLowerCase() === selectedVoice.toLowerCase());
      const styles = vObj?.supportedStyles || ['default'];
      if (settings.style !== 'default' && !styles.includes(settings.style)) {
        setSettings((prev) => ({ ...prev, style: 'default' }));
      }
    }
  }, [selectedVoice, voices]);

  // Quick insert of sample text
  const handleLoadSample = () => {
    const sample = SAMPLE_TEXTS[selectedLanguage] || SAMPLE_TEXTS.en;
    setText(sample);
    setValidationError('');
    if (apiError) setApiError(null);
  };

  // Handler for text extracted from uploaded documents (Module 13)
  const handleDocumentTextExtracted = (extractedText) => {
    setText(extractedText);
    setValidationError('');
    if (apiError) setApiError(null);
  };

  // Comprehensive client-side validation before request dispatch
  const validateForm = () => {
    if (!text || text.length === 0) {
      setValidationError('Text is required. Please enter text to convert to speech.');
      return false;
    }

    if (text.trim().length === 0) {
      setValidationError('Text cannot consist of whitespace only.');
      return false;
    }

    if (text.trim().length < 3) {
      setValidationError('Input text must be at least 3 characters.');
      return false;
    }

    if (text.length > 5000) {
      setValidationError(`Text exceeds the maximum limit of 5,000 characters (currently ${text.length.toLocaleString()} characters).`);
      return false;
    }

    if (!selectedLanguage || selectedLanguage.trim() === '') {
      setValidationError('Language selection is required. Please select a language.');
      return false;
    }

    const isValidLanguage = DEFAULT_LANGUAGES.some(
      (l) => l.id.toLowerCase() === selectedLanguage.toLowerCase() || l.code.toLowerCase() === selectedLanguage.toLowerCase()
    );
    if (!isValidLanguage) {
      setValidationError(`The selected language '${selectedLanguage}' is invalid or unsupported.`);
      return false;
    }

    if (!selectedVoice || selectedVoice.trim() === '') {
      setValidationError('Voice selection is required. Please select a voice.');
      return false;
    }

    if (voices.length > 0) {
      const selectedVoiceObj = voices.find(
        (v) => v.id.toLowerCase() === selectedVoice.trim().toLowerCase()
      );
      if (!selectedVoiceObj) {
        setValidationError(`The selected voice '${selectedVoice}' is invalid or not available in the voice catalog.`);
        return false;
      }

      const voiceLang = (selectedVoiceObj.language || '').toLowerCase();
      const targetLang = selectedLanguage.toLowerCase();
      const isCompatible =
        voiceLang === targetLang ||
        voiceLang.startsWith(targetLang) ||
        targetLang.startsWith(voiceLang.substring(0, 2));

      if (!isCompatible) {
        setValidationError(
          `Selected voice '${selectedVoiceObj.name || selectedVoice}' is not compatible with language '${selectedLanguage}'. Please choose a matching voice.`
        );
        return false;
      }
    }

    setValidationError('');
    return true;
  };

  // Generate speech via backend POST /api/tts (Direct Audio Bytes Pipeline)
  const handleGenerateSpeech = async () => {
    if (isGenerating) return;

    if (!isAuthenticated) {
      setValidationError('Please sign in or register to synthesize speech and save to your personal history.');
      return;
    }

    if (!validateForm()) return;

    setIsGenerating(true);
    setApiError(null);

    const langObj = DEFAULT_LANGUAGES.find((l) => l.id === selectedLanguage);
    const targetLanguageCode = langObj ? langObj.code : selectedLanguage;

    try {
      const result = await ttsService.generateSpeech({
        text: text.trim(),
        language: targetLanguageCode,
        voice: selectedVoice,
        speed: settings.speed,
        pitch: settings.pitch,
        volume: settings.volume,
        style: settings.style
      });

      if (generatedAudio) {
        URL.revokeObjectURL(generatedAudio);
      }

      setIsBackendConnected(true);
      setGeneratedAudio(result.audioUrl);
      setGeneratedBlob(result.audioBlob);
      setGenerationMeta({
        title: `${result.voice} (${result.language})`,
        subtitle: `${result.characterCount} characters • ${settings.speed.toFixed(2)}x rate • ${settings.style !== 'default' ? settings.style + ' • ' : ''}MP3 Audio • ${result.provider}`,
        filename: result.filename,
        provider: result.provider,
        format: result.format
      });
    } catch (err) {
      console.error('[HomePage] Speech generation failed:', err);

      if (err.validationErrors) {
        const firstField = Object.keys(err.validationErrors)[0];
        const fieldMsg = err.validationErrors[firstField];
        setValidationError(fieldMsg || err.userFriendlyMessage);
      } else {
        setApiError(
          err.userFriendlyMessage ||
          'Failed to synthesize speech. Please check backend server status and try again.'
        );
      }

      if (!err.response && !err.status) {
        setIsBackendConnected(false);
      }
    } finally {
      setIsGenerating(false);
    }
  };

  const handleAudioPlayerError = (errorMsg) => {
    setApiError(
      errorMsg || 'The received audio stream could not be decoded. The audio response may be corrupted or unsupported.'
    );
  };

  const handleReset = () => {
    if (generatedAudio) {
      URL.revokeObjectURL(generatedAudio);
    }
    setText('');
    setGeneratedAudio(null);
    setGeneratedBlob(null);
    setGenerationMeta(null);
    setSettings({
      speed: 1.0,
      pitch: 0,
      volume: 1.0,
      style: 'default'
    });
    setValidationError('');
    setApiError(null);
  };

  const isTextValid = text.trim().length >= 3 && text.length <= 5000;

  return (
    <div className="space-y-6 sm:space-y-8 max-w-6xl mx-auto">
      {/* Studio Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div className="space-y-1.5">
          <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-md text-[11px] font-semibold bg-indigo-500/10 border border-indigo-500/20 text-indigo-300">
            <Sparkles className="w-3 h-3 text-indigo-400" aria-hidden="true" />
            <span>Neural Speech Engine &bull; Studio Pipeline</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
            Text to Speech Studio
          </h1>
          <p className="text-slate-400 text-xs sm:text-sm max-w-xl">
            Synthesize natural spoken audio with expressive voice models, document extraction, and granular speech controls.
          </p>
        </div>

        {/* Engine Status Pill */}
        <div className="flex items-center gap-2 self-start sm:self-center">
          <div
            className={`flex items-center gap-2 px-3 py-1.5 rounded-xl border text-xs font-medium ${
              isBackendConnected === true
                ? 'bg-emerald-950/40 border-emerald-500/30 text-emerald-300'
                : isBackendConnected === false
                ? 'bg-rose-950/40 border-rose-500/30 text-rose-300'
                : 'bg-slate-900 border-slate-800 text-slate-400'
            }`}
          >
            <Radio
              className={`w-3.5 h-3.5 ${
                isBackendConnected === true
                  ? 'text-emerald-400'
                  : isBackendConnected === false
                  ? 'text-rose-400'
                  : 'text-slate-500'
              }`}
              aria-hidden="true"
            />
            <span>
              {isBackendConnected === true
                ? 'Engine Connected'
                : isBackendConnected === false
                ? 'Engine Unreachable'
                : 'Connecting Engine...'}
            </span>
          </div>
        </div>
      </div>

      {/* Global Error Banner */}
      {apiError && (
        <ErrorMessage
          title="Speech Processing Notice"
          message={apiError}
          onDismiss={() => setApiError(null)}
          onRetry={handleGenerateSpeech}
        />
      )}

      {/* Main Studio Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8 items-start">
        {/* Left Column: Form Controls (7 cols) */}
        <section
          aria-label="Speech Synthesis Controls"
          className="lg:col-span-7 space-y-5 rounded-2xl border border-slate-800 bg-slate-900 p-5 sm:p-6 shadow-sm"
        >
          {/* Section Eyebrow */}
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div className="flex items-center gap-2">
              <Sliders className="w-4 h-4 text-indigo-400" aria-hidden="true" />
              <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-200">
                Voice Configuration &amp; Input
              </h2>
            </div>
            <button
              type="button"
              onClick={handleLoadSample}
              className="text-xs font-medium text-indigo-400 hover:text-indigo-300 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-400 rounded px-1.5 py-0.5 cursor-pointer"
            >
              Insert sample text
            </button>
          </div>

          {/* Language & Voice Selectors */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <LanguageSelector
              selectedLanguage={selectedLanguage}
              onChange={handleLanguageChange}
              disabled={isGenerating}
            />
            <VoiceSelector
              selectedLanguage={selectedLanguage}
              selectedVoice={selectedVoice}
              onChange={setSelectedVoice}
              voices={voices}
              isLoading={isLoadingVoices}
              disabled={isGenerating}
            />
          </div>

          {/* Audio Customization Settings (Speed, Pitch, Volume, Style) */}
          <SpeechSettings
            settings={settings}
            onChange={setSettings}
            supportedStyles={supportedStyles}
            disabled={isGenerating}
          />

          {/* Document Upload & Text Extraction (Module 13) */}
          <DocumentUploader
            onTextExtracted={handleDocumentTextExtracted}
            disabled={isGenerating}
          />

          {/* AI Text Enhancement (Module 14) */}
          <AIEnhancement
            text={text}
            onApplyText={(enhancedText) => {
              setText(enhancedText);
              setValidationError('');
              if (apiError) setApiError(null);
            }}
            disabled={isGenerating}
          />

          {/* Large Textarea Input */}
          <TextInput
            value={text}
            onChange={(val) => {
              setText(val);
              if (validationError) setValidationError('');
              if (apiError) setApiError(null);
            }}
            onClear={() => {
              setText('');
              setValidationError('');
            }}
            validationError={validationError}
            disabled={isGenerating}
            maxLength={5000}
            placeholder="Type or paste the text you want to synthesize into natural speech, or upload a document above..."
          />

          {/* Action Bar */}
          <div className="pt-2 flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-t border-slate-800">
            <div className="flex items-center gap-3">
              <GenerateButton
                onClick={handleGenerateSpeech}
                isLoading={isGenerating}
                disabled={!isTextValid || isGenerating}
                text="Generate Speech"
                loadingText="Synthesizing Audio..."
              />

              {(text.length > 0 || generatedAudio) && (
                <button
                  type="button"
                  onClick={handleReset}
                  disabled={isGenerating}
                  title="Reset form and audio"
                  aria-label="Reset form and generated audio"
                  className="inline-flex items-center gap-1.5 px-3.5 py-3 min-h-[44px] text-xs font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-xl border border-slate-750 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-400 cursor-pointer"
                >
                  <RotateCcw className="w-3.5 h-3.5" aria-hidden="true" />
                  <span>Reset</span>
                </button>
              )}
            </div>

            <div className="flex items-center gap-1.5 text-xs text-slate-500">
              <Info className="w-3.5 h-3.5 flex-shrink-0" aria-hidden="true" />
              <span>Direct 24kHz MP3 binary stream</span>
            </div>
          </div>
        </section>

        {/* Right Column: Audio Output & Sticky Player on Desktop (5 cols) */}
        <section
          aria-label="Synthesized Speech Output"
          className="lg:col-span-5 lg:sticky lg:top-24 space-y-4"
        >
          {/* Section Eyebrow */}
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div className="flex items-center gap-2">
              <Headphones className="w-4 h-4 text-indigo-400" aria-hidden="true" />
              <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-200">
                Audio Output &amp; Player
              </h2>
            </div>
            {generatedAudio && (
              <span className="text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded-md border bg-emerald-500/10 border-emerald-500/30 text-emerald-300">
                MP3 Ready
              </span>
            )}
          </div>

          {/* Active Generation Loading Indicator */}
          {isGenerating && (
            <div className="rounded-2xl border border-indigo-500/30 bg-indigo-950/20 p-8 text-center space-y-3">
              <LoadingSpinner size="lg" className="mx-auto text-indigo-400" />
              <div className="space-y-1">
                <h3 className="text-sm font-semibold text-white">
                  Synthesizing Speech Stream...
                </h3>
                <p className="text-xs text-indigo-200/80 max-w-xs mx-auto">
                  Communicating with speech provider. Direct binary audio stream in progress.
                </p>
              </div>
            </div>
          )}

          {/* After successful generation: AudioPlayer & DownloadButton */}
          {!isGenerating && generatedAudio && (
            <div className="space-y-4">
              <AudioPlayer
                audioUrl={generatedAudio}
                title={generationMeta?.title || 'Synthesized Speech Output'}
                subtitle={generationMeta?.subtitle || 'MP3 Audio Stream'}
                onError={handleAudioPlayerError}
              />

              {/* Enabled Download Button */}
              <DownloadButton
                audioUrl={generatedAudio}
                audioBlob={generatedBlob}
                filename={generationMeta?.filename || 'synthesized_speech.mp3'}
                className="w-full"
              />

              {/* Success Metadata Banner */}
              <div className="rounded-2xl border border-emerald-500/30 bg-emerald-950/20 p-4 space-y-1">
                <div className="flex items-center gap-2 text-emerald-400 text-xs font-semibold">
                  <CheckCircle2 className="w-4 h-4" aria-hidden="true" />
                  <span>Speech Synthesized Successfully</span>
                </div>
                <p className="text-xs text-slate-300 leading-relaxed">
                  Audio delivered directly as binary MP3 (<code className="text-emerald-300 bg-emerald-950/60 px-1 py-0.5 rounded text-[11px] font-mono">audio/mpeg</code>). Streamed directly to your browser without intermediate disk storage.
                </p>
              </div>
            </div>
          )}

          {/* Empty State Card (Visible when no audio has been generated yet) */}
          {!isGenerating && !generatedAudio && (
            <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5 sm:p-6 space-y-4 text-slate-300 shadow-sm">
              <div className="flex items-center gap-3">
                <div className="p-3 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                  <Volume2 className="w-5 h-5" aria-hidden="true" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-slate-100">
                    Audio Player Standby
                  </h3>
                  <p className="text-xs text-slate-400">
                    Your generated speech audio will appear here
                  </p>
                </div>
              </div>

              <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-2.5">
                <h4 className="text-[11px] font-semibold text-slate-300 uppercase tracking-wider">
                  Synthesis Workflow:
                </h4>
                <ol className="space-y-2 text-xs text-slate-400">
                  <li className="flex items-start gap-2">
                    <span className="flex-shrink-0 w-4 h-4 rounded-full bg-slate-800 text-indigo-300 flex items-center justify-center font-bold text-[10px]">1</span>
                    <span>Choose language and voice model.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="flex-shrink-0 w-4 h-4 rounded-full bg-slate-800 text-indigo-300 flex items-center justify-center font-bold text-[10px]">2</span>
                    <span>Enter text, upload a document, or use AI tools.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="flex-shrink-0 w-4 h-4 rounded-full bg-slate-800 text-indigo-300 flex items-center justify-center font-bold text-[10px]">3</span>
                    <span>Click <strong>Generate Speech</strong> to stream high-fidelity MP3.</span>
                  </li>
                </ol>
              </div>

              {/* Disabled Download Button in empty state */}
              <DownloadButton
                disabled={true}
                filename="synthesized_speech.mp3"
                className="w-full"
              />
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
