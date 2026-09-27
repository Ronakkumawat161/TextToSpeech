import React, { useState, useRef, useEffect } from 'react';
import { 
  Play, 
  Pause, 
  Download, 
  Trash2, 
  Calendar, 
  Volume2, 
  Globe, 
  ChevronDown, 
  ChevronUp, 
  Loader2
} from 'lucide-react';
import FavoriteButton from './FavoriteButton';
import { ttsService } from '../../services/ttsService';

/**
 * HistoryCard component displaying an individual speech synthesis record.
 * Supports inline audio playback, audio download, favorite toggle, and deletion.
 */
export default function HistoryCard({
  item,
  onDelete,
  onToggleFavorite,
  isDeleting = false
}) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isLoadingAudio, setIsLoadingAudio] = useState(false);
  const [audioUrl, setAudioUrl] = useState(null);
  const [audioBlob, setAudioBlob] = useState(null);

  const audioRef = useRef(null);

  // Clean up object URL on unmount
  useEffect(() => {
    return () => {
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
      }
    };
  }, [audioUrl]);

  // Handle Play/Pause
  const handleTogglePlay = async () => {
    if (isPlaying) {
      if (audioRef.current) {
        audioRef.current.pause();
        setIsPlaying(false);
      }
      return;
    }

    // If audio is already loaded, resume playing
    if (audioRef.current && audioUrl) {
      try {
        await audioRef.current.play();
        setIsPlaying(true);
      } catch (err) {
        console.error('Failed to resume audio playback:', err);
      }
      return;
    }

    // Otherwise, generate audio for this record
    setIsLoadingAudio(true);
    try {
      const result = await ttsService.generateSpeech({
        text: item.text,
        language: item.language,
        voice: item.voice,
        speed: item.speed || 1.0,
        pitch: item.pitch || 0,
        volume: item.volume || 1.0,
        style: item.style || 'default'
      });

      setAudioBlob(result.audioBlob);
      setAudioUrl(result.audioUrl);

      const audio = new Audio(result.audioUrl);
      audioRef.current = audio;

      audio.onended = () => setIsPlaying(false);
      audio.onpause = () => setIsPlaying(false);
      audio.onerror = () => {
        setIsPlaying(false);
        setIsLoadingAudio(false);
      };

      await audio.play();
      setIsPlaying(true);
    } catch (err) {
      console.error('Failed to load audio for history card:', err);
      alert('Unable to play audio. Please ensure the backend is running.');
    } finally {
      setIsLoadingAudio(false);
    }
  };

  // Handle Audio Download
  const handleDownload = async () => {
    try {
      let blobToDownload = audioBlob;

      if (!blobToDownload) {
        const result = await ttsService.generateSpeech({
          text: item.text,
          language: item.language,
          voice: item.voice,
          speed: item.speed || 1.0,
          pitch: item.pitch || 0,
          volume: item.volume || 1.0,
          style: item.style || 'default'
        });
        blobToDownload = result.audioBlob;
      }

      const filename = `voxflow-${item.voice || 'speech'}-${item.id || Date.now()}.mp3`;
      const tempUrl = URL.createObjectURL(blobToDownload);
      const link = document.createElement('a');
      link.href = tempUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setTimeout(() => URL.revokeObjectURL(tempUrl), 5000);
    } catch (err) {
      console.error('Download error:', err);
      alert('Failed to download audio file.');
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'Recent';
    try {
      return new Date(dateStr).toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateStr;
    }
  };

  const textPreviewLength = 160;
  const isLongText = (item.text || '').length > textPreviewLength;
  const displayedText = isLongText && !isExpanded
    ? item.text.slice(0, textPreviewLength) + '...'
    : item.text;

  const wordCount = (item.text || '').trim().split(/\s+/).filter(Boolean).length;

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5 shadow-sm hover:border-slate-750 transition-colors">
      <div className="flex flex-col gap-3.5">
        {/* Top Metadata Row */}
        <div className="flex items-center justify-between gap-2 flex-wrap text-xs">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="px-2 py-0.5 rounded-md bg-slate-800 border border-slate-700/80 text-slate-300 font-mono text-[11px] tabular-nums">
              #{item.id}
            </span>

            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md bg-slate-950 border border-slate-800 text-slate-300">
              <Globe className="w-3 h-3 text-indigo-400" aria-hidden="true" />
              <span>{item.language}</span>
            </span>

            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md bg-slate-950 border border-slate-800 text-slate-300">
              <Volume2 className="w-3 h-3 text-purple-400" aria-hidden="true" />
              <span>{item.voice}</span>
            </span>

            {/* Audio Settings Badge if customized */}
            {((item.speed && item.speed !== 1.0) || (item.pitch && item.pitch !== 0) || (item.style && item.style !== 'default')) && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-[11px] font-mono tabular-nums">
                {item.speed && item.speed !== 1.0 && `${item.speed}x `}
                {item.pitch && item.pitch !== 0 && `${item.pitch > 0 ? '+' : ''}${item.pitch}% `}
                {item.style && item.style !== 'default' && `${item.style}`}
              </span>
            )}

            <span className="inline-flex items-center gap-1 text-slate-500 text-[11px] ml-1">
              <Calendar className="w-3 h-3 text-slate-500" aria-hidden="true" />
              <span>{formatDate(item.createdAt)}</span>
            </span>
          </div>

          {/* Quick Actions (Favorite) */}
          <div className="flex items-center gap-1.5 ml-auto">
            <FavoriteButton
              isFavorite={Boolean(item.favorite)}
              onToggle={() => onToggleFavorite && onToggleFavorite(item.id, !item.favorite)}
              size="sm"
            />
          </div>
        </div>

        {/* Text Content */}
        <div className="relative">
          <p className="text-sm text-slate-200 leading-relaxed font-sans bg-slate-950/60 p-3.5 rounded-xl border border-slate-800/80">
            "{displayedText}"
          </p>

          {isLongText && (
            <button
              type="button"
              onClick={() => setIsExpanded(!isExpanded)}
              aria-expanded={isExpanded}
              className="mt-1.5 inline-flex items-center gap-1 text-xs text-indigo-400 hover:text-indigo-300 transition-colors font-semibold cursor-pointer"
            >
              <span>{isExpanded ? 'Show less' : 'Read full text'}</span>
              {isExpanded ? <ChevronUp className="w-3.5 h-3.5" aria-hidden="true" /> : <ChevronDown className="w-3.5 h-3.5" aria-hidden="true" />}
            </button>
          )}
        </div>

        {/* Bottom Bar: Stats & Audio Controls */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-3 border-t border-slate-800 text-xs text-slate-400">
          <div className="flex items-center gap-2.5 font-mono tabular-nums text-slate-400">
            <span>{item.characterCount || item.text?.length || 0} chars</span>
            <span className="text-slate-600">&bull;</span>
            <span>{wordCount} words</span>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-2 self-end sm:self-center">
            {/* Play/Pause Button */}
            <button
              type="button"
              onClick={handleTogglePlay}
              disabled={isLoadingAudio || isDeleting}
              aria-label={isPlaying ? 'Pause speech audio' : 'Play speech audio'}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 min-h-[36px] rounded-xl text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white shadow-sm transition-colors disabled:opacity-50 cursor-pointer"
            >
              {isLoadingAudio ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" aria-hidden="true" />
                  <span>Loading...</span>
                </>
              ) : isPlaying ? (
                <>
                  <Pause className="w-3.5 h-3.5" aria-hidden="true" />
                  <span>Pause</span>
                </>
              ) : (
                <>
                  <Play className="w-3.5 h-3.5 fill-current" aria-hidden="true" />
                  <span>Play</span>
                </>
              )}
            </button>

            {/* Download Button */}
            <button
              type="button"
              onClick={handleDownload}
              title="Download audio file"
              aria-label="Download audio MP3 file"
              disabled={isDeleting}
              className="p-2 min-h-[36px] min-w-[36px] rounded-xl bg-slate-950 hover:bg-slate-800 text-slate-300 hover:text-white border border-slate-800 hover:border-slate-700 transition-colors disabled:opacity-50 cursor-pointer flex items-center justify-center"
            >
              <Download className="w-3.5 h-3.5" aria-hidden="true" />
            </button>

            {/* Delete Button */}
            <button
              type="button"
              onClick={() => onDelete && onDelete(item.id)}
              disabled={isDeleting}
              title="Delete record"
              aria-label="Delete history record"
              className="p-2 min-h-[36px] min-w-[36px] rounded-xl bg-slate-950 hover:bg-rose-500/10 text-slate-400 hover:text-rose-400 border border-slate-800 hover:border-rose-500/30 transition-colors disabled:opacity-50 cursor-pointer flex items-center justify-center"
            >
              {isDeleting ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin text-rose-400" aria-hidden="true" />
              ) : (
                <Trash2 className="w-3.5 h-3.5" aria-hidden="true" />
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
