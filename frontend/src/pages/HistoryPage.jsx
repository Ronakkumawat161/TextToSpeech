import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../services/api';
import HistoryList from '../components/history/HistoryList';
import { 
  History, 
  Search, 
  Calendar, 
  Globe, 
  Volume2, 
  Heart, 
  ArrowLeft, 
  RefreshCw, 
  AlertCircle, 
  ChevronLeft, 
  ChevronRight, 
  X,
  Sparkles
} from 'lucide-react';

const LOCAL_FAVORITES_KEY = 'voxflow_local_favorites';
const ITEMS_PER_PAGE = 6;

export default function HistoryPage() {
  const [history, setHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [deletingId, setDeletingId] = useState(null);

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedLanguage, setSelectedLanguage] = useState('ALL');
  const [selectedVoice, setSelectedVoice] = useState('ALL');
  const [selectedDateFilter, setSelectedDateFilter] = useState('ALL'); // ALL, TODAY, WEEK, MONTH
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false);

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);

  // Fetch history records from backend
  const fetchHistory = async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const response = await apiClient.get('/api/history');
      const data = Array.isArray(response.data) ? response.data : [];

      // Merge with local favorite overrides if any
      const localFavorites = getLocalFavorites();
      const enriched = data.map((item) => ({
        ...item,
        favorite: localFavorites.has(String(item.id)) || Boolean(item.favorite)
      }));

      setHistory(enriched);
    } catch (err) {
      const msg = err.userFriendlyMessage || err.response?.data?.error?.message || 'Unable to load speech history. Service is temporarily unavailable.';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  // Local storage favorites helper
  const getLocalFavorites = () => {
    try {
      const saved = localStorage.getItem(LOCAL_FAVORITES_KEY);
      return new Set(saved ? JSON.parse(saved) : []);
    } catch (e) {
      return new Set();
    }
  };

  const saveLocalFavorites = (favSet) => {
    try {
      localStorage.setItem(LOCAL_FAVORITES_KEY, JSON.stringify(Array.from(favSet)));
    } catch (e) {}
  };

  // Toggle Favorite Handler
  const handleToggleFavorite = async (id, newStatus) => {
    setHistory((prev) =>
      prev.map((item) => (item.id === id ? { ...item, favorite: newStatus } : item))
    );

    const favSet = getLocalFavorites();
    if (newStatus) {
      favSet.add(String(id));
    } else {
      favSet.delete(String(id));
    }
    saveLocalFavorites(favSet);

    try {
      await apiClient.patch(`/api/history/${id}/favorite`);
    } catch (err) {
      console.warn('Backend favorite sync failed, local state preserved:', err.message);
    }
  };

  // Delete History Handler
  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this speech synthesis record?')) {
      return;
    }

    setDeletingId(id);
    try {
      await apiClient.delete(`/api/history/${id}`);
      setHistory((prev) => prev.filter((item) => item.id !== id));

      const favSet = getLocalFavorites();
      favSet.delete(String(id));
      saveLocalFavorites(favSet);
    } catch (err) {
      alert(err.userFriendlyMessage || 'Failed to delete history record.');
    } finally {
      setDeletingId(null);
    }
  };

  // Extract distinct languages and voices from history
  const availableLanguages = useMemo(() => {
    const langs = new Set(history.map((h) => h.language).filter(Boolean));
    return Array.from(langs).sort();
  }, [history]);

  const availableVoices = useMemo(() => {
    const vList = history
      .filter((h) => selectedLanguage === 'ALL' || h.language === selectedLanguage)
      .map((h) => h.voice)
      .filter(Boolean);
    return Array.from(new Set(vList)).sort();
  }, [history, selectedLanguage]);

  // Filter pipeline
  const filteredHistory = useMemo(() => {
    return history.filter((item) => {
      if (searchQuery.trim()) {
        const query = searchQuery.trim().toLowerCase();
        const matchesText = (item.text || '').toLowerCase().includes(query);
        const matchesVoice = (item.voice || '').toLowerCase().includes(query);
        const matchesId = String(item.id).includes(query);
        if (!matchesText && !matchesVoice && !matchesId) {
          return false;
        }
      }

      if (selectedLanguage !== 'ALL' && item.language !== selectedLanguage) {
        return false;
      }

      if (selectedVoice !== 'ALL' && item.voice !== selectedVoice) {
        return false;
      }

      if (showFavoritesOnly && !item.favorite) {
        return false;
      }

      if (selectedDateFilter !== 'ALL' && item.createdAt) {
        const itemDate = new Date(item.createdAt);
        const now = new Date();
        const diffMs = now - itemDate;
        const diffDays = diffMs / (1000 * 60 * 60 * 24);

        if (selectedDateFilter === 'TODAY' && diffDays > 1) {
          return false;
        }
        if (selectedDateFilter === 'WEEK' && diffDays > 7) {
          return false;
        }
        if (selectedDateFilter === 'MONTH' && diffDays > 30) {
          return false;
        }
      }

      return true;
    });
  }, [history, searchQuery, selectedLanguage, selectedVoice, showFavoritesOnly, selectedDateFilter]);

  // Reset page when filter changes
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, selectedLanguage, selectedVoice, showFavoritesOnly, selectedDateFilter]);

  // Pagination calculation
  const totalPages = Math.max(1, Math.ceil(filteredHistory.length / ITEMS_PER_PAGE));
  const paginatedItems = useMemo(() => {
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    return filteredHistory.slice(startIndex, startIndex + ITEMS_PER_PAGE);
  }, [filteredHistory, currentPage]);

  const hasActiveFilters = 
    searchQuery.trim() !== '' || 
    selectedLanguage !== 'ALL' || 
    selectedVoice !== 'ALL' || 
    selectedDateFilter !== 'ALL' || 
    showFavoritesOnly;

  const handleClearFilters = () => {
    setSearchQuery('');
    setSelectedLanguage('ALL');
    setSelectedVoice('ALL');
    setSelectedDateFilter('ALL');
    setShowFavoritesOnly(false);
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <Link
            to="/"
            className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-200 transition-colors mb-1.5 group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 rounded"
          >
            <ArrowLeft className="w-3.5 h-3.5 group-hover:-translate-x-0.5 transition-transform" aria-hidden="true" />
            <span>Back to Studio</span>
          </Link>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white flex items-center gap-2.5">
            <History className="w-6 h-6 text-indigo-400" aria-hidden="true" />
            <span>Speech History &amp; Favorites</span>
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Search, replay, filter, and download your synthesized audio library
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          <button
            type="button"
            onClick={fetchHistory}
            disabled={isLoading}
            aria-label="Refresh history list"
            className="inline-flex items-center gap-2 px-3 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 text-xs font-medium transition-colors disabled:opacity-50 cursor-pointer min-h-[38px]"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin text-indigo-400' : ''}`} aria-hidden="true" />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Error state */}
      {errorMessage && (
        <div
          role="alert"
          aria-live="polite"
          className="p-4 rounded-xl bg-amber-950/40 border border-amber-500/40 flex items-start gap-3 text-amber-300 text-xs sm:text-sm"
        >
          <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5 text-amber-400" aria-hidden="true" />
          <div className="flex-1">
            <p className="font-semibold text-amber-200">History Service Notice</p>
            <p className="text-xs text-amber-300/90 mt-0.5">{errorMessage}</p>
          </div>
          <button
            type="button"
            onClick={fetchHistory}
            className="px-2.5 py-1 rounded-lg bg-amber-500/20 hover:bg-amber-500/30 text-amber-200 text-xs font-semibold transition-colors cursor-pointer"
          >
            Retry
          </button>
        </div>
      )}

      {/* Search and Filters Bar */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900 p-4 sm:p-5 space-y-3.5 shadow-sm">
        {/* Search Input & Favorite Toggle Row */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          {/* Search Box */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" aria-hidden="true" />
            <input
              type="text"
              aria-label="Search speech history"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by text content, voice name, or ID..."
              className="w-full min-h-[42px] bg-slate-950 border border-slate-700/80 rounded-xl pl-10 pr-9 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/30 focus:border-indigo-500 transition-colors"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                aria-label="Clear search query"
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 p-1 cursor-pointer"
              >
                <X className="w-4 h-4" aria-hidden="true" />
              </button>
            )}
          </div>

          {/* Favorites Only Filter Toggle */}
          <button
            type="button"
            onClick={() => setShowFavoritesOnly(!showFavoritesOnly)}
            aria-pressed={showFavoritesOnly}
            className={`inline-flex items-center justify-center gap-2 px-3.5 py-2 min-h-[42px] rounded-xl text-xs font-medium border transition-colors cursor-pointer ${
              showFavoritesOnly
                ? 'bg-rose-500/15 border-rose-500/40 text-rose-300'
                : 'bg-slate-950 border-slate-700/80 text-slate-400 hover:text-slate-200 hover:border-slate-600'
            }`}
          >
            <Heart className={`w-4 h-4 ${showFavoritesOnly ? 'fill-rose-500 text-rose-500' : ''}`} aria-hidden="true" />
            <span>Favorites ({history.filter((h) => h.favorite).length})</span>
          </button>
        </div>

        {/* Dropdown Filters Row */}
        <div className="flex flex-wrap items-center gap-2.5 pt-3 border-t border-slate-800 text-xs">
          {/* Language Selector */}
          <div className="flex items-center gap-1.5 bg-slate-950 border border-slate-750 rounded-xl px-3 py-1.5 text-slate-300 min-h-[36px]">
            <Globe className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />
            <label htmlFor="history-lang-filter" className="text-slate-400">Language:</label>
            <select
              id="history-lang-filter"
              value={selectedLanguage}
              onChange={(e) => setSelectedLanguage(e.target.value)}
              className="bg-transparent text-slate-200 focus:outline-none font-medium cursor-pointer"
            >
              <option value="ALL" className="bg-slate-900 text-slate-200">All Languages</option>
              {availableLanguages.map((lang) => (
                <option key={lang} value={lang} className="bg-slate-900 text-slate-200">
                  {lang}
                </option>
              ))}
            </select>
          </div>

          {/* Voice Selector */}
          <div className="flex items-center gap-1.5 bg-slate-950 border border-slate-750 rounded-xl px-3 py-1.5 text-slate-300 min-h-[36px]">
            <Volume2 className="w-3.5 h-3.5 text-purple-400" aria-hidden="true" />
            <label htmlFor="history-voice-filter" className="text-slate-400">Voice:</label>
            <select
              id="history-voice-filter"
              value={selectedVoice}
              onChange={(e) => setSelectedVoice(e.target.value)}
              className="bg-transparent text-slate-200 focus:outline-none font-medium cursor-pointer"
            >
              <option value="ALL" className="bg-slate-900 text-slate-200">All Voices</option>
              {availableVoices.map((voice) => (
                <option key={voice} value={voice} className="bg-slate-900 text-slate-200">
                  {voice}
                </option>
              ))}
            </select>
          </div>

          {/* Date Filter */}
          <div className="flex items-center gap-1.5 bg-slate-950 border border-slate-750 rounded-xl px-3 py-1.5 text-slate-300 min-h-[36px]">
            <Calendar className="w-3.5 h-3.5 text-slate-400" aria-hidden="true" />
            <label htmlFor="history-date-filter" className="text-slate-400">Date:</label>
            <select
              id="history-date-filter"
              value={selectedDateFilter}
              onChange={(e) => setSelectedDateFilter(e.target.value)}
              className="bg-transparent text-slate-200 focus:outline-none font-medium cursor-pointer"
            >
              <option value="ALL" className="bg-slate-900 text-slate-200">All Time</option>
              <option value="TODAY" className="bg-slate-900 text-slate-200">Today</option>
              <option value="WEEK" className="bg-slate-900 text-slate-200">Last 7 Days</option>
              <option value="MONTH" className="bg-slate-900 text-slate-200">Last 30 Days</option>
            </select>
          </div>

          {/* Reset Filters Button */}
          {hasActiveFilters && (
            <button
              type="button"
              onClick={handleClearFilters}
              className="inline-flex items-center gap-1 text-slate-400 hover:text-slate-200 transition-colors ml-auto text-xs py-1 cursor-pointer"
            >
              <X className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Clear Filters</span>
            </button>
          )}
        </div>
      </div>

      {/* Results Header: Count & Pagination Info */}
      <div className="flex items-center justify-between text-xs text-slate-400 px-1 font-mono tabular-nums">
        <span>
          Showing <strong className="text-slate-200">{paginatedItems.length}</strong> of{' '}
          <strong className="text-slate-200">{filteredHistory.length}</strong> records
          {history.length > 0 && ` (${history.length} total)`}
        </span>

        {totalPages > 1 && (
          <span>
            Page <strong className="text-slate-200">{currentPage}</strong> of{' '}
            <strong className="text-slate-200">{totalPages}</strong>
          </span>
        )}
      </div>

      {/* Main Content Area */}
      {isLoading ? (
        /* Loading Skeleton State */
        <div className="space-y-4">
          {[1, 2, 3].map((n) => (
            <div
              key={n}
              className="rounded-2xl border border-slate-800 bg-slate-900 p-5 animate-pulse space-y-3"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-16 h-5 rounded-lg bg-slate-800" />
                  <div className="w-20 h-5 rounded-lg bg-slate-800" />
                  <div className="w-24 h-5 rounded-lg bg-slate-800" />
                </div>
                <div className="w-8 h-8 rounded-xl bg-slate-800" />
              </div>
              <div className="w-full h-12 rounded-xl bg-slate-800/60" />
              <div className="flex items-center justify-between pt-2">
                <div className="w-32 h-4 rounded bg-slate-800" />
                <div className="w-24 h-7 rounded-xl bg-slate-800" />
              </div>
            </div>
          ))}
        </div>
      ) : history.length === 0 ? (
        /* Global Empty State (No records yet) */
        <div className="rounded-2xl border border-slate-800 bg-slate-900 p-12 text-center flex flex-col items-center justify-center">
          <div className="w-14 h-14 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mb-4">
            <Sparkles className="w-7 h-7" aria-hidden="true" />
          </div>
          <h3 className="text-base sm:text-lg font-bold text-slate-100 mb-1">No speech records yet</h3>
          <p className="text-xs sm:text-sm text-slate-400 max-w-sm mb-6">
            Synthesize speech audio from text in the studio. All your generated audio history will be saved here.
          </p>
          <Link
            to="/"
            className="px-5 py-2.5 min-h-[42px] rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs tracking-wide shadow-sm transition-colors cursor-pointer inline-flex items-center justify-center"
          >
            Create Speech in Studio
          </Link>
        </div>
      ) : (
        /* History List */
        <HistoryList
          items={paginatedItems}
          onDelete={handleDelete}
          onToggleFavorite={handleToggleFavorite}
          deletingId={deletingId}
          onClearFilters={handleClearFilters}
          emptyMessage="No speech records match your active search or filter criteria."
        />
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-4">
          <button
            type="button"
            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            disabled={currentPage === 1}
            aria-label="Previous page"
            className="inline-flex items-center gap-1 px-3 py-2 min-h-[38px] rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white text-xs disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
          >
            <ChevronLeft className="w-4 h-4" aria-hidden="true" />
            <span>Previous</span>
          </button>

          <div className="flex items-center gap-1 font-mono tabular-nums">
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
              <button
                key={page}
                type="button"
                onClick={() => setCurrentPage(page)}
                aria-label={`Go to page ${page}`}
                aria-current={currentPage === page ? 'page' : undefined}
                className={`w-8 h-8 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
                  currentPage === page
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'bg-slate-900 text-slate-400 hover:text-slate-200'
                }`}
              >
                {page}
              </button>
            ))}
          </div>

          <button
            type="button"
            onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
            disabled={currentPage === totalPages}
            aria-label="Next page"
            className="inline-flex items-center gap-1 px-3 py-2 min-h-[38px] rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white text-xs disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
          >
            <span>Next</span>
            <ChevronRight className="w-4 h-4" aria-hidden="true" />
          </button>
        </div>
      )}
    </div>
  );
}
