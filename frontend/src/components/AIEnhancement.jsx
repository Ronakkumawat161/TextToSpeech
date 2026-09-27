import React, { useState, useRef } from 'react';
import { 
  Sparkles, 
  FileText, 
  CheckCheck, 
  Wand2, 
  MessageSquareQuote, 
  Loader2, 
  Check, 
  Copy, 
  X, 
  AlertCircle,
  HelpCircle
} from 'lucide-react';
import { aiService } from '../services/aiService';

const OPERATIONS = [
  {
    id: 'SUMMARIZE',
    label: 'Summarize',
    icon: FileText,
    description: 'Condense into a concise summary ideal for speech',
    btnId: 'ai-summarize-btn'
  },
  {
    id: 'CORRECT_GRAMMAR',
    label: 'Correct Grammar',
    icon: CheckCheck,
    description: 'Fix spelling, punctuation, and syntax without altering tone',
    btnId: 'ai-grammar-btn'
  },
  {
    id: 'REWRITE',
    label: 'Rewrite',
    icon: Wand2,
    description: 'Polish vocabulary and improve sentence flow for listening',
    btnId: 'ai-rewrite-btn'
  },
  {
    id: 'CONVERSATIONAL',
    label: 'Make Conversational',
    icon: MessageSquareQuote,
    description: 'Transform into natural, friendly spoken dialogue',
    btnId: 'ai-conversational-btn'
  }
];

export default function AIEnhancement({
  text = '',
  onApplyText,
  disabled = false
}) {
  const [loadingOp, setLoadingOp] = useState(null);
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState(null);
  const [isCopied, setIsCopied] = useState(false);

  // In-memory cache to prevent redundant/unnecessary AI calls for identical text and operation
  const cacheRef = useRef(new Map());

  const trimmedText = (text || '').trim();
  const isTextAvailable = trimmedText.length >= 3;

  const handleEnhance = async (op) => {
    // 1. Guard against unnecessary or empty calls
    if (!isTextAvailable || disabled || loadingOp) return;

    setError(null);

    // 2. Check local memory cache
    const cacheKey = `${op}:${trimmedText}`;
    if (cacheRef.current.has(cacheKey)) {
      setPreview(cacheRef.current.get(cacheKey));
      return;
    }

    // 3. Dispatch request to backend abstraction
    setLoadingOp(op);
    try {
      const response = await aiService.enhanceText({
        text: trimmedText,
        operation: op
      });

      if (response && response.enhancedText) {
        const previewData = {
          operation: op,
          originalText: response.originalText || trimmedText,
          enhancedText: response.enhancedText,
          provider: response.provider || 'AI'
        };

        // Cache result for quick re-use
        cacheRef.current.set(cacheKey, previewData);
        setPreview(previewData);
      } else {
        throw new Error('No enhanced text was returned by the AI service.');
      }
    } catch (err) {
      console.error('[AIEnhancement] Enhancement error:', err);
      setError(err.userFriendlyMessage || err.message || 'AI text enhancement failed. Please try again.');
    } finally {
      setLoadingOp(null);
    }
  };

  const handleApply = () => {
    if (preview && preview.enhancedText && typeof onApplyText === 'function') {
      // User explicitly confirms replacing original text
      onApplyText(preview.enhancedText);
      setPreview(null);
      setError(null);
    }
  };

  const handleDiscard = () => {
    setPreview(null);
    setError(null);
  };

  const handleCopy = async () => {
    if (!preview || !preview.enhancedText) return;
    try {
      await navigator.clipboard.writeText(preview.enhancedText);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    } catch (e) {
      console.warn('Failed to copy to clipboard', e);
    }
  };

  const getOperationLabel = (opId) => {
    const op = OPERATIONS.find((o) => o.id === opId);
    return op ? op.label : opId;
  };

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 overflow-hidden shadow-sm transition-colors p-4 sm:p-5 space-y-3.5">
      {/* Header bar */}
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="p-1.5 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
            <Sparkles className="w-4 h-4" aria-hidden="true" />
          </div>
          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-200">
              AI Text Enhancement
            </h3>
            <p className="text-[11px] text-slate-400">
              Refine grammar, summarize, or adapt your text for natural speech
            </p>
          </div>
        </div>

        {!isTextAvailable && (
          <span className="text-[10px] text-slate-500 hidden sm:inline-flex items-center gap-1">
            <HelpCircle className="w-3 h-3" aria-hidden="true" />
            Enter text below to enable
          </span>
        )}
      </div>

      {/* Action Buttons Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
        {OPERATIONS.map((op) => {
          const Icon = op.icon;
          const isLoading = loadingOp === op.id;
          const isSelected = preview?.operation === op.id;

          return (
            <button
              key={op.id}
              id={op.btnId}
              type="button"
              onClick={() => handleEnhance(op.id)}
              disabled={!isTextAvailable || disabled || loadingOp !== null}
              title={op.description}
              aria-label={`${op.label}: ${op.description}`}
              className={`flex items-center justify-center gap-2 px-3 py-2.5 min-h-[42px] rounded-xl border text-xs font-medium transition-colors cursor-pointer ${
                isSelected
                  ? 'bg-indigo-600/20 border-indigo-500 text-indigo-200'
                  : 'bg-slate-950/60 hover:bg-slate-800/80 border-slate-800 hover:border-slate-700 text-slate-300'
              } disabled:opacity-40 disabled:cursor-not-allowed`}
            >
              {isLoading ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin text-indigo-400 flex-shrink-0" aria-hidden="true" />
              ) : (
                <Icon className="w-3.5 h-3.5 text-indigo-400 flex-shrink-0" aria-hidden="true" />
              )}
              <span className="truncate">
                {isLoading ? 'Enhancing...' : op.label}
              </span>
            </button>
          );
        })}
      </div>

      {/* Error Notice */}
      {error && (
        <div
          role="alert"
          aria-live="polite"
          className="p-3 rounded-xl bg-rose-950/30 border border-rose-500/30 flex items-center justify-between gap-3 text-xs"
        >
          <div className="flex items-center gap-2 text-rose-300 min-w-0">
            <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" aria-hidden="true" />
            <span className="truncate">{error}</span>
          </div>
          <button
            type="button"
            onClick={() => setError(null)}
            aria-label="Dismiss enhancement error"
            className="p-1 rounded-lg text-rose-400 hover:text-rose-200 hover:bg-rose-900/40 transition-colors flex-shrink-0"
          >
            <X className="w-3.5 h-3.5" aria-hidden="true" />
          </button>
        </div>
      )}

      {/* Preview Card (User can review before replacing original text) */}
      {preview && (
        <div className="mt-3 p-4 rounded-xl border border-indigo-500/30 bg-slate-950/90 space-y-3.5">
          {/* Preview Header */}
          <div className="flex items-center justify-between gap-3 border-b border-slate-800 pb-2.5">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-xs font-semibold text-indigo-300 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />
                Enhanced Preview: {getOperationLabel(preview.operation)}
              </span>
              <span className="px-2 py-0.5 rounded-md bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 font-mono text-[10px]">
                {preview.provider}
              </span>
            </div>

            <div className="flex items-center gap-1.5">
              <button
                type="button"
                onClick={handleCopy}
                aria-label="Copy enhanced text to clipboard"
                title="Copy enhanced text to clipboard"
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-[11px] font-medium transition-colors"
              >
                {isCopied ? (
                  <>
                    <Check className="w-3 h-3 text-emerald-400" aria-hidden="true" />
                    <span className="text-emerald-300">Copied</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3 h-3" aria-hidden="true" />
                    <span>Copy</span>
                  </>
                )}
              </button>

              <button
                type="button"
                id="ai-discard-btn"
                onClick={handleDiscard}
                aria-label="Discard preview without applying"
                title="Discard preview"
                className="p-1 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors"
              >
                <X className="w-4 h-4" aria-hidden="true" />
              </button>
            </div>
          </div>

          {/* Enhanced Text Content Box */}
          <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-100 text-xs leading-relaxed max-h-56 overflow-y-auto font-sans whitespace-pre-wrap">
            {preview.enhancedText}
          </div>

          {/* Character Comparison & Action Controls */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-1">
            <div className="flex items-center gap-2 text-[11px] text-slate-400 font-mono tabular-nums">
              <span>Original: <strong className="text-slate-300">{preview.originalText.length}</strong> chars</span>
              <span>&bull;</span>
              <span>Enhanced: <strong className="text-indigo-300">{preview.enhancedText.length}</strong> chars</span>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleDiscard}
                className="flex-1 sm:flex-none px-3.5 py-1.5 min-h-[36px] rounded-xl border border-slate-700 text-slate-300 hover:text-slate-100 hover:bg-slate-800 text-xs font-medium transition-colors cursor-pointer"
              >
                Keep Original
              </button>

              <button
                type="button"
                id="ai-apply-btn"
                onClick={handleApply}
                className="flex-1 sm:flex-none inline-flex items-center justify-center gap-1.5 px-4 py-1.5 min-h-[36px] rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition-colors shadow-sm cursor-pointer"
              >
                <Check className="w-3.5 h-3.5" aria-hidden="true" />
                <span>Replace Original Text</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export { AIEnhancement };
