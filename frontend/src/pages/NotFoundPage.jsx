import React from 'react';
import { Link } from 'react-router-dom';
import { Home } from 'lucide-react';

export default function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
      <span className="text-6xl sm:text-7xl font-black text-indigo-400 font-mono tabular-nums mb-3">
        404
      </span>
      <h1 className="text-xl sm:text-2xl font-bold text-white mb-2">Page Not Found</h1>
      <p className="text-slate-400 text-xs sm:text-sm max-w-sm mb-6">
        The requested page or resource could not be found. Please check the URL or return to the Studio.
      </p>
      <Link
        to="/"
        className="inline-flex items-center gap-2 px-5 py-2.5 min-h-[44px] rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs tracking-wide shadow-sm transition-colors cursor-pointer"
      >
        <Home className="w-4 h-4" aria-hidden="true" />
        <span>Return to Studio</span>
      </Link>
    </div>
  );
}
