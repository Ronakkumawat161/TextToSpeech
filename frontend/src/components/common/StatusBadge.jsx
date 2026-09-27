import React from 'react';

export default function StatusBadge({ status, label, latency }) {
  const isOnline = status === 'online';
  const isPending = status === 'pending';

  const defaultLabel = isOnline
    ? 'Engine Online'
    : isPending
    ? 'Connecting...'
    : 'Engine Offline';

  return (
    <div
      role="status"
      aria-label={`Backend server status: ${label || defaultLabel}`}
      className={`inline-flex items-center gap-2 px-2.5 py-1 rounded-xl text-xs font-medium border transition-colors ${
        isOnline
          ? 'bg-emerald-950/40 border-emerald-500/30 text-emerald-300'
          : isPending
          ? 'bg-amber-950/40 border-amber-500/30 text-amber-300'
          : 'bg-rose-950/40 border-rose-500/30 text-rose-300'
      }`}
    >
      <span className="relative flex h-2 w-2 flex-shrink-0" aria-hidden="true">
        {isOnline && (
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-60" />
        )}
        <span
          className={`relative inline-flex rounded-full h-2 w-2 ${
            isOnline ? 'bg-emerald-400' : isPending ? 'bg-amber-400' : 'bg-rose-500'
          }`}
        />
      </span>
      <span className="font-medium text-[11px]">{label || defaultLabel}</span>
      {latency !== undefined && latency !== null && (
        <span className="text-[10px] text-slate-400 font-mono pl-1 border-l border-slate-700/60 tabular-nums">
          {latency}ms
        </span>
      )}
    </div>
  );
}
