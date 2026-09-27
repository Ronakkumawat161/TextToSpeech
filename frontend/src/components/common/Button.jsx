import React from 'react';

export default function Button({
  children,
  onClick,
  variant = 'primary',
  size = 'md',
  disabled = false,
  icon: Icon,
  className = '',
  type = 'button',
  ariaLabel,
  ...props
}) {
  const baseStyles =
    'inline-flex items-center justify-center font-medium rounded-xl transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer';

  const sizeStyles = {
    sm: 'text-xs px-3 py-2 gap-1.5 min-h-[36px]',
    md: 'text-sm px-4 py-2.5 gap-2 min-h-[42px]',
    lg: 'text-base px-5 py-3 gap-2.5 min-h-[48px]'
  };

  const variantStyles = {
    primary:
      'bg-indigo-600 hover:bg-indigo-500 text-white shadow-sm focus-visible:ring-indigo-500 active:bg-indigo-700',
    secondary:
      'bg-slate-850 hover:bg-slate-800 text-slate-100 border border-slate-700/80 focus-visible:ring-slate-400 active:bg-slate-900',
    outline:
      'border border-slate-700 hover:border-slate-600 hover:bg-slate-800/60 text-slate-200 focus-visible:ring-indigo-500',
    ghost:
      'hover:bg-slate-800/80 text-slate-300 hover:text-white focus-visible:ring-slate-400',
    danger:
      'bg-rose-600 hover:bg-rose-500 text-white shadow-sm focus-visible:ring-rose-500'
  };

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      aria-label={ariaLabel}
      className={`${baseStyles} ${sizeStyles[size] || sizeStyles.md} ${variantStyles[variant] || variantStyles.primary} ${className}`}
      {...props}
    >
      {Icon && <Icon className="w-4 h-4 flex-shrink-0" aria-hidden="true" />}
      {children}
    </button>
  );
}
