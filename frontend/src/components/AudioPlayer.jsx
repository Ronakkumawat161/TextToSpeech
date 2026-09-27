import React, { useState, useRef, useEffect } from 'react';
import { Play, Pause, Volume2, Volume1, VolumeX, RotateCcw, Disc3, Loader2, AlertCircle } from 'lucide-react';

/**
 * Reusable Audio Player Component
 * Supports play, pause, seek, volume control, duration, progress, loading states,
 * keyboard accessibility, and audio decoding error handling.
 *
 * @param {string|null} audioUrl - URL or blob object URL of the generated audio
 * @param {string} title - Title of the audio track
 * @param {string} subtitle - Subtitle details (Voice, Language, Format)
 * @param {Function} [onError] - Callback when audio decoding or playback encounters an error
 * @param {string} className - Optional CSS classes
 */
export default function AudioPlayer({
  audioUrl = null,
  title = 'Synthesized Audio',
  subtitle = 'Speech Output',
  onError,
  className = ''
}) {
  const audioRef = useRef(null);
  const containerRef = useRef(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1.0);
  const [isMuted, setIsMuted] = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1.0);
  const [isBuffering, setIsBuffering] = useState(false);
  const [audioError, setAudioError] = useState(null);

  // Reset player when audioUrl changes
  useEffect(() => {
    setIsPlaying(false);
    setCurrentTime(0);
    setDuration(0);
    setAudioError(null);
    setIsBuffering(Boolean(audioUrl));
  }, [audioUrl]);

  const togglePlay = () => {
    if (!audioRef.current || !audioUrl || audioError) return;

    if (isPlaying) {
      audioRef.current.pause();
      setIsPlaying(false);
    } else {
      setIsBuffering(true);
      audioRef.current
        .play()
        .then(() => {
          setIsPlaying(true);
          setIsBuffering(false);
        })
        .catch((err) => {
          console.error('[AudioPlayer] Playback error:', err);
          setIsPlaying(false);
          setIsBuffering(false);
          setAudioError('Playback failed. Browser blocked autoplay or audio format is unsupported.');
          if (onError) onError('Audio playback failed.');
        });
    }
  };

  const handleTimeUpdate = () => {
    if (audioRef.current) {
      setCurrentTime(audioRef.current.currentTime);
    }
  };

  const handleLoadedMetadata = () => {
    if (audioRef.current) {
      setDuration(audioRef.current.duration || 0);
      setIsBuffering(false);
      setAudioError(null);
    }
  };

  const handleSeek = (e) => {
    if (!audioRef.current || !audioUrl) return;
    const seekTime = Number(e.target.value);
    audioRef.current.currentTime = seekTime;
    setCurrentTime(seekTime);
  };

  const handleVolumeChange = (e) => {
    const val = Number(e.target.value);
    setVolume(val);
    if (audioRef.current) {
      audioRef.current.volume = val;
      if (val === 0) {
        setIsMuted(true);
      } else if (isMuted) {
        setIsMuted(false);
      }
    }
  };

  const toggleMute = () => {
    if (!audioRef.current || !audioUrl) return;
    const nextMuted = !isMuted;
    audioRef.current.muted = nextMuted;
    setIsMuted(nextMuted);
  };

  const handleRestart = () => {
    if (!audioRef.current || !audioUrl) return;
    audioRef.current.currentTime = 0;
    setCurrentTime(0);
    if (!isPlaying) {
      audioRef.current.play().then(() => setIsPlaying(true));
    }
  };

  const cycleSpeed = () => {
    if (!audioRef.current || !audioUrl) return;
    const speeds = [1.0, 1.25, 1.5, 2.0, 0.75];
    const nextIdx = (speeds.indexOf(playbackRate) + 1) % speeds.length;
    const nextSpeed = speeds[nextIdx];
    audioRef.current.playbackRate = nextSpeed;
    setPlaybackRate(nextSpeed);
  };

  const handleAudioError = () => {
    setIsBuffering(false);
    setIsPlaying(false);
    const msg = 'Corrupted or unsupported audio stream. Failed to decode MP3 data.';
    setAudioError(msg);
    if (onError) onError(msg);
  };

  // Keyboard controls when focused on player container
  const handleKeyDown = (e) => {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'BUTTON') return;
    if (e.key === ' ' || e.key === 'k') {
      e.preventDefault();
      togglePlay();
    } else if (e.key === 'ArrowRight' && audioRef.current) {
      e.preventDefault();
      audioRef.current.currentTime = Math.min(duration, currentTime + 2);
    } else if (e.key === 'ArrowLeft' && audioRef.current) {
      e.preventDefault();
      audioRef.current.currentTime = Math.max(0, currentTime - 2);
    }
  };

  const formatTime = (secs) => {
    if (isNaN(secs) || secs <= 0) return '00:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  const renderVolumeIcon = () => {
    if (isMuted || volume === 0) return <VolumeX className="w-4 h-4 text-rose-400" aria-hidden="true" />;
    if (volume < 0.5) return <Volume1 className="w-4 h-4 text-slate-300" aria-hidden="true" />;
    return <Volume2 className="w-4 h-4 text-slate-300" aria-hidden="true" />;
  };

  return (
    <div
      ref={containerRef}
      tabIndex={0}
      onKeyDown={handleKeyDown}
      aria-label="Synthesized Audio Player"
      className={`rounded-2xl border border-slate-800 bg-slate-900 p-5 shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 ${className}`}
    >
      {audioUrl && (
        <audio
          ref={audioRef}
          src={audioUrl}
          onTimeUpdate={handleTimeUpdate}
          onLoadedMetadata={handleLoadedMetadata}
          onWaiting={() => setIsBuffering(true)}
          onPlaying={() => setIsBuffering(false)}
          onEnded={() => setIsPlaying(false)}
          onError={handleAudioError}
        />
      )}

      {/* Track Header */}
      <div className="flex items-center justify-between pb-3 mb-4 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div
            className={`p-2.5 rounded-xl border ${
              audioUrl && !audioError
                ? 'bg-indigo-500/10 border-indigo-500/30 text-indigo-400'
                : 'bg-slate-850 border-slate-750 text-slate-500'
            }`}
          >
            {isBuffering ? (
              <Loader2 className="w-5 h-5 animate-spin text-indigo-400" aria-hidden="true" />
            ) : (
              <Disc3
                className={`w-5 h-5 ${isPlaying ? 'animate-spin' : ''}`}
                aria-hidden="true"
              />
            )}
          </div>
          <div>
            <h4 className="text-sm font-semibold text-slate-100">{title}</h4>
            <p className="text-xs text-slate-400">{subtitle}</p>
          </div>
        </div>

        <span
          className={`text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded-md border ${
            audioError
              ? 'bg-rose-500/10 border-rose-500/30 text-rose-300'
              : audioUrl
              ? isPlaying
                ? 'bg-indigo-500/10 border-indigo-500/30 text-indigo-300'
                : isBuffering
                ? 'bg-amber-500/10 border-amber-500/30 text-amber-300'
                : 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
              : 'bg-slate-800 border-slate-700 text-slate-400'
          }`}
        >
          {audioError ? 'Corrupted Audio' : audioUrl ? (isPlaying ? 'Playing' : isBuffering ? 'Buffering' : 'Ready') : 'Standby'}
        </span>
      </div>

      {/* Audio Error Alert if any */}
      {audioError && (
        <div
          role="alert"
          aria-live="polite"
          className="mb-4 flex items-center gap-2 rounded-xl bg-rose-950/40 border border-rose-500/40 p-3 text-xs text-rose-300"
        >
          <AlertCircle className="w-4 h-4 flex-shrink-0 text-rose-400" aria-hidden="true" />
          <span>{audioError}</span>
        </div>
      )}

      {/* Controls & Scrubber */}
      <div className="space-y-4">
        {/* Timeline Slider / Progress */}
        <div className="space-y-1.5">
          <div className="relative flex items-center">
            <input
              type="range"
              min="0"
              max={duration || 100}
              step="0.05"
              value={currentTime}
              onChange={handleSeek}
              disabled={!audioUrl || Boolean(audioError)}
              aria-label="Audio timeline position"
              aria-valuemin="0"
              aria-valuemax={Math.round(duration)}
              aria-valuenow={Math.round(currentTime)}
              aria-valuetext={`${formatTime(currentTime)} of ${formatTime(duration)}`}
              className="w-full h-2 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500 disabled:cursor-not-allowed disabled:opacity-40"
              style={{
                background: audioUrl && !audioError
                  ? `linear-gradient(to right, #6366f1 0%, #6366f1 ${progressPercent}%, #1e293b ${progressPercent}%, #1e293b 100%)`
                  : '#1e293b'
              }}
            />
          </div>

          <div className="flex justify-between text-[11px] font-mono tabular-nums text-slate-400 px-0.5">
            <span>{formatTime(currentTime)}</span>
            <span>{audioUrl && !audioError ? formatTime(duration) : '--:--'}</span>
          </div>
        </div>

        {/* Buttons and Volume Row */}
        <div className="flex items-center justify-between pt-1">
          {/* Left: Restart & Speed */}
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={handleRestart}
              disabled={!audioUrl || Boolean(audioError)}
              title="Restart from beginning"
              aria-label="Restart audio from beginning"
              className="p-2 text-slate-400 hover:text-slate-200 disabled:opacity-40 disabled:cursor-not-allowed rounded-xl hover:bg-slate-800 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 cursor-pointer min-h-[36px]"
            >
              <RotateCcw className="w-4 h-4" aria-hidden="true" />
            </button>

            <button
              type="button"
              onClick={cycleSpeed}
              disabled={!audioUrl || Boolean(audioError)}
              title="Change playback rate"
              aria-label={`Playback speed: ${playbackRate}x. Click to change.`}
              className="px-2.5 py-1 text-xs font-mono tabular-nums font-semibold text-slate-300 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed rounded-xl hover:bg-slate-800 border border-slate-750 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 cursor-pointer min-h-[36px]"
            >
              {playbackRate}x
            </button>
          </div>

          {/* Center: Play/Pause Button */}
          <div className="flex items-center">
            <button
              type="button"
              onClick={togglePlay}
              disabled={!audioUrl || Boolean(audioError)}
              aria-label={isPlaying ? 'Pause speech audio' : 'Play speech audio'}
              className={`p-3.5 rounded-full transition-transform focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900 cursor-pointer ${
                audioUrl && !audioError
                  ? 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-sm hover:scale-105 active:scale-95'
                  : 'bg-slate-800 text-slate-600 cursor-not-allowed border border-slate-700/50'
              }`}
            >
              {isBuffering ? (
                <Loader2 className="w-5 h-5 animate-spin" aria-hidden="true" />
              ) : isPlaying ? (
                <Pause className="w-5 h-5 fill-current" aria-hidden="true" />
              ) : (
                <Play className="w-5 h-5 fill-current ml-0.5" aria-hidden="true" />
              )}
            </button>
          </div>

          {/* Right: Volume & Slider */}
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={toggleMute}
              disabled={!audioUrl || Boolean(audioError)}
              aria-label={isMuted ? 'Unmute audio' : 'Mute audio'}
              title={isMuted ? 'Unmute audio' : 'Mute audio'}
              className="p-2 text-slate-400 hover:text-slate-200 disabled:opacity-40 disabled:cursor-not-allowed rounded-xl hover:bg-slate-800 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 cursor-pointer min-h-[36px]"
            >
              {renderVolumeIcon()}
            </button>

            <input
              type="range"
              min="0"
              max="1"
              step="0.05"
              value={isMuted ? 0 : volume}
              onChange={handleVolumeChange}
              disabled={!audioUrl || Boolean(audioError)}
              aria-label="Volume level"
              aria-valuemin="0"
              aria-valuemax="100"
              aria-valuenow={isMuted ? 0 : Math.round(volume * 100)}
              aria-valuetext={isMuted ? 'Muted' : `${Math.round(volume * 100)}%`}
              className="w-16 sm:w-20 h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed"
            />
          </div>
        </div>
      </div>
    </div>
  );
}
