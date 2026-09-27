import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Volume2, RefreshCw, LogIn, LogOut, UserPlus, History, Mic2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import StatusBadge from './StatusBadge';

export default function Header({ healthState }) {
  const { isOnline, isLoading, latency, refresh } = healthState || {};
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const isStudioActive = location.pathname === '/';
  const isHistoryActive = location.pathname === '/history';

  return (
    <header className="sticky top-0 z-50 bg-slate-950/90 border-b border-slate-800/80 backdrop-blur-md">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-2 sm:gap-4">
        {/* Brand & Studio Navigation */}
        <div className="flex items-center gap-6">
          <Link
            to="/"
            className="flex items-center gap-2.5 group focus-visible:rounded-lg focus-visible:outline-none"
            aria-label="VoxFlow Studio Home"
          >
            <div className="h-9 w-9 rounded-xl bg-indigo-600 flex items-center justify-center text-white shadow-sm transition-transform group-hover:scale-105">
              <Volume2 className="w-5 h-5" aria-hidden="true" />
            </div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-base sm:text-lg tracking-tight text-white">
                VoxFlow
              </span>
              <span className="hidden sm:inline-block text-[11px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded-md bg-slate-900 border border-slate-800 text-slate-400">
                Studio
              </span>
            </div>
          </Link>

          {/* Primary Nav Links */}
          <nav aria-label="Main Navigation" className="hidden md:flex items-center gap-1">
            <Link
              to="/"
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                isStudioActive
                  ? 'bg-slate-800 text-white border border-slate-700/80'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
              }`}
            >
              <Mic2 className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />
              <span>Studio</span>
            </Link>

            {isAuthenticated && (
              <Link
                to="/history"
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  isHistoryActive
                    ? 'bg-slate-800 text-white border border-slate-700/80'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
                }`}
              >
                <History className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />
                <span>History</span>
              </Link>
            )}
          </nav>
        </div>

        {/* User Controls & Diagnostic Status */}
        <div className="flex items-center gap-2 sm:gap-3">
          {isAuthenticated ? (
            <div className="flex items-center gap-2">
              {/* Mobile History Link */}
              <Link
                to="/history"
                className="md:hidden p-2 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 transition-colors"
                aria-label="View Speech History"
                title="History"
              >
                <History className="w-4 h-4 text-indigo-400" aria-hidden="true" />
              </Link>

              {/* User Avatar & Name */}
              <div
                className="flex items-center gap-2 px-2.5 py-1.5 rounded-xl bg-slate-900 border border-slate-800 text-xs"
                title={user?.email || 'Logged in user'}
              >
                <div className="w-5 h-5 rounded-full bg-indigo-600 flex items-center justify-center text-white text-[10px] font-bold">
                  {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
                </div>
                <span className="font-medium text-slate-300 hidden sm:inline truncate max-w-[120px]">
                  {user?.name || user?.email}
                </span>
              </div>

              {/* Sign out */}
              <button
                type="button"
                onClick={handleLogout}
                aria-label="Sign out of account"
                title="Sign out"
                className="p-2 rounded-xl bg-slate-900 hover:bg-rose-500/10 text-slate-400 hover:text-rose-400 border border-slate-800 hover:border-rose-500/30 transition-colors"
              >
                <LogOut className="w-4 h-4" aria-hidden="true" />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                to="/login"
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-medium transition-colors"
              >
                <LogIn className="w-3.5 h-3.5 text-indigo-400" aria-hidden="true" />
                <span>Sign In</span>
              </Link>
              <Link
                to="/register"
                className="hidden sm:inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-sm transition-colors"
              >
                <UserPlus className="w-3.5 h-3.5" aria-hidden="true" />
                <span>Get Started</span>
              </Link>
            </div>
          )}

          {/* Engine Health Status Badge */}
          <div className="hidden sm:block">
            <StatusBadge
              status={isLoading ? 'pending' : isOnline ? 'online' : 'offline'}
              latency={latency}
            />
          </div>

          {/* Refresh Diagnostic Trigger */}
          <button
            type="button"
            onClick={refresh}
            aria-label="Refresh engine diagnostic status"
            title="Refresh engine status"
            disabled={isLoading}
            className="p-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-400 hover:text-slate-200 border border-slate-800 transition-colors disabled:opacity-50"
          >
            <RefreshCw
              className={`w-4 h-4 ${isLoading ? 'animate-spin text-indigo-400' : ''}`}
              aria-hidden="true"
            />
          </button>
        </div>
      </div>
    </header>
  );
}
