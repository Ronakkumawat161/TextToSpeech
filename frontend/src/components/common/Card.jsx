import React from 'react';

export default function Card({
  title,
  subtitle,
  icon: Icon,
  badge,
  children,
  action,
  className = ''
}) {
  return (
    <div className={`rounded-2xl border border-slate-800 bg-slate-900 p-5 sm:p-6 shadow-sm transition-colors ${className}`}>
      {(title || Icon || action) && (
        <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-800">
          <div className="flex items-center gap-3">
            {Icon && (
              <div className="p-2 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
                <Icon className="w-4 h-4" aria-hidden="true" />
              </div>
            )}
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-sm sm:text-base font-semibold text-slate-100">{title}</h3>
                {badge && (
                  <span className="text-[10px] uppercase tracking-wider font-semibold px-2 py-0.5 rounded-md bg-slate-800 text-slate-300 border border-slate-700/80">
                    {badge}
                  </span>
                )}
              </div>
              {subtitle && <p className="text-xs text-slate-400 mt-0.5">{subtitle}</p>}
            </div>
          </div>
          {action && <div>{action}</div>}
        </div>
      )}
      <div>{children}</div>
    </div>
  );
}
