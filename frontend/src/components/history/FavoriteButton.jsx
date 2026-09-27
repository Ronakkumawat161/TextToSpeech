import React, { useState } from 'react';
import { Heart } from 'lucide-react';

/**
 * Interactive FavoriteButton component.
 * Features subtle feedback, accessible labeling, and touch-friendly padding.
 */
export default function FavoriteButton({
  isFavorite = false,
  onToggle,
  disabled = false,
  size = 'md',
  showLabel = false,
  className = ''
}) {
  const [isAnimating, setIsAnimating] = useState(false);

  const handleClick = (e) => {
    e.stopPropagation();
    if (disabled) return;
    setIsAnimating(true);
    setTimeout(() => setIsAnimating(false), 250);
    if (onToggle) {
      onToggle(!isFavorite);
    }
  };

  const sizeClasses = {
    sm: 'p-1.5 text-xs min-h-[32px] min-w-[32px]',
    md: 'p-2 text-sm min-h-[38px] min-w-[38px]',
    lg: 'p-2.5 text-base min-h-[44px] min-w-[44px]'
  }[size] || 'p-2 text-sm min-h-[38px] min-w-[38px]';

  const iconSizes = {
    sm: 'w-3.5 h-3.5',
    md: 'w-4 h-4',
    lg: 'w-5 h-5'
  }[size] || 'w-4 h-4';

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={disabled}
      aria-label={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
      title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
      className={`inline-flex items-center justify-center gap-1.5 rounded-xl transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500 cursor-pointer ${sizeClasses} ${
        isFavorite
          ? 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
          : 'bg-slate-950 text-slate-400 border border-slate-800 hover:text-slate-200 hover:border-slate-700'
      } ${disabled ? 'opacity-40 cursor-not-allowed' : 'active:scale-95'} ${className}`}
    >
      <Heart
        className={`${iconSizes} transition-transform duration-150 ${
          isAnimating ? 'scale-125' : 'scale-100'
        } ${isFavorite ? 'fill-rose-500 text-rose-500' : 'text-current'}`}
        aria-hidden="true"
      />
      {showLabel && (
        <span className="text-xs font-semibold">
          {isFavorite ? 'Favorited' : 'Favorite'}
        </span>
      )}
    </button>
  );
}
