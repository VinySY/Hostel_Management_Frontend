import React from 'react';
import { ComplaintPriority } from '../../types';

interface PriorityBadgeProps {
  priority: ComplaintPriority | string;
  size?: 'sm' | 'md';
}

export const PriorityBadge: React.FC<PriorityBadgeProps> = ({ priority, size = 'md' }) => {
  const sizeClasses = size === 'sm' ? 'px-2 py-0.5 text-[10px]' : 'px-2.5 py-1 text-xs';

  const getColorClasses = (p: string) => {
    const norm = p?.toUpperCase();
    switch (norm) {
      case 'CRITICAL':
      case 'URGENT':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'HIGH':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'MEDIUM':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'LOW':
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  return (
    <span
      className={`inline-flex items-center rounded-full border font-bold uppercase tracking-wider ${sizeClasses} ${getColorClasses(
        priority
      )}`}
    >
      {priority}
    </span>
  );
};
