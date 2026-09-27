import React, { useState, useRef } from 'react';
import { 
  UploadCloud, 
  FileText, 
  CheckCircle2, 
  AlertCircle, 
  X, 
  Loader2, 
  Info
} from 'lucide-react';
import { documentService } from '../services/documentService';

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
const ALLOWED_EXTENSIONS = ['txt', 'pdf', 'docx'];

export default function DocumentUploader({
  onTextExtracted,
  disabled = false
}) {
  const [isDragging, setIsDragging] = useState(false);
  const [isExtracting, setIsExtracting] = useState(false);
  const [extractedFileMeta, setExtractedFileMeta] = useState(null);
  const [uploaderError, setUploaderError] = useState(null);
  const fileInputRef = useRef(null);

  const getExtension = (name) => {
    if (!name) return '';
    const parts = name.split('.');
    return parts.length > 1 ? parts[parts.length - 1].toLowerCase() : '';
  };

  const formatFileSize = (bytes) => {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
  };

  const processFile = async (file) => {
    if (!file) return;

    // Reset previous states
    setUploaderError(null);

    // 1. Client-side extension validation
    const ext = getExtension(file.name);
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      setUploaderError(`Unsupported format '.${ext || 'unknown'}'. Please upload a TXT, PDF, or DOCX document.`);
      return;
    }

    // 2. Client-side file size validation (10MB)
    if (file.size > MAX_FILE_SIZE_BYTES) {
      setUploaderError(`File size (${formatFileSize(file.size)}) exceeds the maximum allowed limit of 10MB.`);
      return;
    }

    if (file.size === 0) {
      setUploaderError('The selected file is empty (0 bytes). Please select a valid document.');
      return;
    }

    // 3. Initiate backend extraction via POST /api/documents/extract-text
    setIsExtracting(true);
    try {
      const response = await documentService.extractText(file);

      if (response && response.text) {
        setExtractedFileMeta({
          filename: response.filename || file.name,
          fileType: response.fileType || ext,
          fileSize: response.fileSize || file.size,
          characterCount: response.characterCount || response.text.length,
          wordCount: response.wordCount || 0
        });

        // Pass extracted text to parent for insertion into TextInput
        if (typeof onTextExtracted === 'function') {
          onTextExtracted(response.text, {
            filename: response.filename || file.name,
            fileType: response.fileType || ext,
            characterCount: response.characterCount || response.text.length
          });
        }
      } else {
        throw new Error('No readable text content returned by document extraction service.');
      }
    } catch (err) {
      console.error('[DocumentUploader] Extraction failed:', err);
      const msg = err.userFriendlyMessage || err.message || 'Failed to extract text from document. Please verify the file is not corrupted or encrypted.';
      setUploaderError(msg);
      setExtractedFileMeta(null);
    } finally {
      setIsExtracting(false);
      // Reset input value so user can upload the same file again if desired
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!disabled && !isExtracting) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    if (disabled || isExtracting) return;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const droppedFile = e.dataTransfer.files[0];
      processFile(droppedFile);
    }
  };

  const handleFileInputChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      const selectedFile = e.target.files[0];
      processFile(selectedFile);
    }
  };

  const handleClear = (e) => {
    e.stopPropagation();
    setExtractedFileMeta(null);
    setUploaderError(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const renderBadge = (ext) => {
    switch (ext) {
      case 'pdf':
        return <span className="px-2 py-0.5 rounded-md bg-rose-500/15 border border-rose-500/30 text-rose-300 font-bold uppercase text-[10px]">PDF</span>;
      case 'docx':
        return <span className="px-2 py-0.5 rounded-md bg-blue-500/15 border border-blue-500/30 text-blue-300 font-bold uppercase text-[10px]">DOCX</span>;
      default:
        return <span className="px-2 py-0.5 rounded-md bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 font-bold uppercase text-[10px]">TXT</span>;
    }
  };

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 overflow-hidden shadow-sm transition-colors p-4 sm:p-5">
      {/* Header section */}
      <div className="flex items-center justify-between gap-3 mb-3">
        <div className="flex items-center gap-2.5">
          <div className="p-1.5 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
            <FileText className="w-4 h-4" aria-hidden="true" />
          </div>
          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-200">
              Document Text Extraction
            </h3>
            <p className="text-[11px] text-slate-400">
              Upload a document to extract text directly into the speech editor
            </p>
          </div>
        </div>

        {/* Supported Format Tags */}
        <div className="hidden sm:flex items-center gap-1.5 font-mono text-[10px]">
          <span className="px-2 py-0.5 rounded-md bg-slate-800 border border-slate-700/80 text-slate-300">
            TXT
          </span>
          <span className="px-2 py-0.5 rounded-md bg-slate-800 border border-slate-700/80 text-slate-300">
            PDF
          </span>
          <span className="px-2 py-0.5 rounded-md bg-slate-800 border border-slate-700/80 text-slate-300">
            DOCX
          </span>
          <span className="text-slate-500 ml-1">
            (&le; 10MB)
          </span>
        </div>
      </div>

      {/* Hidden native file input */}
      <input
        ref={fileInputRef}
        id="document-file-input"
        type="file"
        aria-label="Upload document file for text extraction"
        accept=".txt,.pdf,.docx,text/plain,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        onChange={handleFileInputChange}
        disabled={disabled || isExtracting}
        className="hidden"
      />

      {/* Drop Zone Box */}
      <div
        id="document-uploader-dropzone"
        role="button"
        tabIndex={disabled || isExtracting ? -1 : 0}
        aria-label="Upload document area. Click or press Enter to choose file"
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => {
          if (!disabled && !isExtracting && fileInputRef.current) {
            fileInputRef.current.click();
          }
        }}
        onKeyDown={(e) => {
          if ((e.key === 'Enter' || e.key === ' ') && !disabled && !isExtracting && fileInputRef.current) {
            e.preventDefault();
            fileInputRef.current.click();
          }
        }}
        className={`relative border-2 border-dashed rounded-xl p-5 text-center cursor-pointer transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 ${
          isDragging
            ? 'border-indigo-500 bg-indigo-500/10'
            : 'border-slate-800 hover:border-slate-700 bg-slate-950/50 hover:bg-slate-950/70'
        } ${disabled || isExtracting ? 'opacity-60 cursor-not-allowed' : ''}`}
      >
        {isExtracting ? (
          <div className="flex flex-col items-center justify-center py-2 space-y-2">
            <Loader2 className="w-6 h-6 text-indigo-400 animate-spin" aria-hidden="true" />
            <div className="space-y-0.5">
              <p className="text-xs font-semibold text-slate-200">
                Extracting readable text...
              </p>
              <p className="text-[11px] text-slate-400">
                Parsing document structure and filtering unprintable characters
              </p>
            </div>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-2 space-y-2">
            <div className="p-2.5 rounded-xl bg-slate-850 border border-slate-700/80 text-indigo-400">
              <UploadCloud className="w-5 h-5" aria-hidden="true" />
            </div>
            <div>
              <p className="text-xs font-medium text-slate-200">
                <span className="text-indigo-400 font-semibold underline decoration-indigo-400/40 underline-offset-2 hover:text-indigo-300">
                  Click to select file
                </span>{' '}
                or drag and drop document here
              </p>
              <p className="text-[11px] text-slate-500 mt-0.5">
                Safe stateless processing • Documents are never stored on disk
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Success Notification Banner */}
      {extractedFileMeta && !isExtracting && (
        <div
          role="status"
          aria-live="polite"
          className="mt-3.5 p-3 rounded-xl bg-emerald-950/30 border border-emerald-500/30 flex items-center justify-between gap-3 text-xs"
        >
          <div className="flex items-center gap-2.5 min-w-0">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0" aria-hidden="true" />
            <div className="min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-semibold text-slate-200 truncate">
                  {extractedFileMeta.filename}
                </span>
                {renderBadge(extractedFileMeta.fileType)}
                <span className="text-slate-400 text-[11px] font-mono tabular-nums">
                  ({formatFileSize(extractedFileMeta.fileSize)})
                </span>
              </div>
              <p className="text-[11px] text-emerald-300 mt-0.5">
                Extracted {extractedFileMeta.characterCount.toLocaleString()} characters ({extractedFileMeta.wordCount.toLocaleString()} words). Ready for editing below.
              </p>
            </div>
          </div>

          <button
            type="button"
            id="document-clear-btn"
            onClick={handleClear}
            aria-label="Dismiss extracted document details"
            title="Dismiss document info"
            className="p-1 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors flex-shrink-0"
          >
            <X className="w-3.5 h-3.5" aria-hidden="true" />
          </button>
        </div>
      )}

      {/* Error Notification Banner */}
      {uploaderError && !isExtracting && (
        <div
          role="alert"
          aria-live="polite"
          className="mt-3.5 p-3 rounded-xl bg-rose-950/30 border border-rose-500/30 flex items-center justify-between gap-3 text-xs"
        >
          <div className="flex items-center gap-2.5 min-w-0">
            <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" aria-hidden="true" />
            <div className="min-w-0">
              <span className="font-medium text-rose-200">
                {uploaderError}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setUploaderError(null)}
            aria-label="Dismiss document upload error"
            title="Dismiss error"
            className="p-1 rounded-lg text-rose-300 hover:text-rose-100 hover:bg-rose-900/40 transition-colors flex-shrink-0"
          >
            <X className="w-3.5 h-3.5" aria-hidden="true" />
          </button>
        </div>
      )}
    </div>
  );
}

export { DocumentUploader };
