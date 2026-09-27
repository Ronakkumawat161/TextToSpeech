import React from 'react';
import { Server, Code2, ShieldCheck, Zap } from 'lucide-react';

export default function Footer() {
  return (
    <footer className="border-t border-slate-900 bg-slate-950 mt-auto py-8 text-slate-400 text-xs">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="font-semibold text-slate-200">VoxFlow Audio Studio</span>
              <span className="text-slate-600">&bull;</span>
              <span className="text-slate-400">High-Performance Neural Speech Synthesis</span>
            </div>
            <p className="text-[11px] text-slate-500">
              Low-latency binary MP3 streaming pipeline with multi-provider voice customization and document parsing.
            </p>
          </div>

          {/* Infrastructure Chips */}
          <div className="flex flex-wrap items-center gap-2">
            <span className="px-2.5 py-1 rounded-md bg-slate-900 border border-slate-800 text-slate-300 flex items-center gap-1.5 text-[11px]">
              <Server className="w-3 h-3 text-emerald-400" aria-hidden="true" />
              Spring Boot 3.3
            </span>
            <span className="px-2.5 py-1 rounded-md bg-slate-900 border border-slate-800 text-slate-300 flex items-center gap-1.5 text-[11px]">
              <Code2 className="w-3 h-3 text-indigo-400" aria-hidden="true" />
              React 18 + Vite
            </span>
            <span className="px-2.5 py-1 rounded-md bg-slate-900 border border-slate-800 text-slate-300 flex items-center gap-1.5 text-[11px]">
              <Zap className="w-3 h-3 text-amber-400" aria-hidden="true" />
              Direct Audio Stream
            </span>
            <span className="px-2.5 py-1 rounded-md bg-slate-900 border border-slate-800 text-slate-300 flex items-center gap-1.5 text-[11px]">
              <ShieldCheck className="w-3 h-3 text-cyan-400" aria-hidden="true" />
              Stateless Security
            </span>
          </div>
        </div>

        <div className="mt-6 pt-4 border-t border-slate-900 flex flex-col sm:flex-row items-center justify-between text-slate-500 text-[11px] gap-2">
          <p>&copy; {new Date().getFullYear()} VoxFlow Studio. All rights reserved.</p>
          <div className="flex items-center gap-4">
            <span className="hover:text-slate-400 transition-colors">Privacy &amp; Stateless Processing</span>
            <span className="text-slate-700">&bull;</span>
            <span className="hover:text-slate-400 transition-colors">24kHz Audio Delivery</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
