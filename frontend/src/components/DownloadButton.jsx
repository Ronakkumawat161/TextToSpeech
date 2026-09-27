import React from 'react';
import { Download } from 'lucide-react';

/**
 * Reusable Download Audio Button Component
 * Supports direct download of generated MP3 audio blobs or URLs with meaningful filenames.
 *
 * @param {string|null} audioUrl - URL or blob object URL of the audio
 * @param {Blob|null} [audioBlob] - Optional raw audio blob
 * @param {string} filename - Target filename for download (ensures .mp3 extension)
 * @param {Function} [onClick] - Optional callback
 * @param {boolean} disabled - Force disable flag
 * @param {string} className - Additional CSS classes
 */
export default function DownloadButton({
  audioUrl = null,
  audioBlob = null,
  filename = 'synthesized_speech.mp3',
  onClick,
  disabled = false,
  id = 'download-audio-button',
  className = ''
}) {
  const isButtonDisabled = disabled || (!audioUrl && !audioBlob);

  const handleDownload = (e) => {
    if (isButtonDisabled) {
      e.preventDefault();
      return;
    }

    if (onClick) {
      onClick(e);
    }

    // Ensure filename ends with .mp3
    const safeFilename = filename && filename.toLowerCase().endsWith('.mp3')
      ? filename
      : `${filename || 'synthesized_speech'}.mp3`;

    // Download mechanism
    const downloadUrl = audioUrl || (audioBlob ? URL.createObjectURL(audioBlob) : null);
    if (!downloadUrl) return;

    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = safeFilename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <button
      id={id}
      type="button"
      onClick={handleDownload}
      disabled={isButtonDisabled}
      title={isButtonDisabled ? 'Synthesize speech first to enable audio download' : `Download ${filename}`}
      aria-label="Download synthesized MP3 audio file"
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 min-h-[44px] text-sm font-medium transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 ${
        isButtonDisabled
          ? 'bg-slate-850 text-slate-500 border border-slate-800 cursor-not-allowed shadow-none'
          : 'bg-slate-800 text-slate-100 border border-slate-700/80 hover:bg-slate-750 hover:border-slate-600 active:scale-[0.99] shadow-sm cursor-pointer'
      } ${className}`}
    >
      <Download
        className={`w-4 h-4 ${isButtonDisabled ? 'text-slate-500' : 'text-indigo-400'}`}
        aria-hidden="true"
      />
      <span>{audioUrl || audioBlob ? 'Download MP3 Audio' : 'Download (No Audio)'}</span>
    </button>
  );
}
