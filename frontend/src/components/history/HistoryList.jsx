import React from 'react';
import HistoryCard from './HistoryCard';
import { SearchX, RotateCcw } from 'lucide-react';

/**
 * HistoryList component rendering a list of HistoryCard items.
 * Displays dedicated empty state when no items match filters.
 */
export default function HistoryList({
  items = [],
  onDelete,
  onToggleFavorite,
  deletingId = null,
  emptyMessage = 'No history records found.',
  onClearFilters
}) {
  if (items.length === 0) {
    return (
      <div className="rounded-2xl border border-slate-800 bg-slate-900 p-8 sm:p-10 text-center flex flex-col items-center justify-center shadow-sm">
        <div className="w-12 h-12 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-center text-slate-500 mb-3">
          <SearchX className="w-6 h-6" aria-hidden="true" />
        </div>
        <p className="text-slate-200 font-semibold text-sm mb-1">{emptyMessage}</p>
        <p className="text-xs text-slate-400 mb-4 max-w-sm">Try broadening your search term or adjusting language and voice filters.</p>
        {onClearFilters && (
          <button
            type="button"
            onClick={onClearFilters}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 min-h-[38px] rounded-xl bg-slate-800 hover:bg-slate-750 text-slate-200 border border-slate-700 text-xs font-semibold transition-colors cursor-pointer"
          >
            <RotateCcw className="w-3.5 h-3.5" aria-hidden="true" />
            <span>Reset All Filters</span>
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {items.map((item) => (
        <HistoryCard
          key={item.id}
          item={item}
          onDelete={onDelete}
          onToggleFavorite={onToggleFavorite}
          isDeleting={deletingId === item.id}
        />
      ))}
    </div>
  );
}
