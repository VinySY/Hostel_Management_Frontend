import React from 'react';
import { ComplaintPriority, ComplaintStatus, RoomOccupancyStatus } from '../../types';

interface StatusBadgeProps {
  status: ComplaintStatus | RoomOccupancyStatus | ComplaintPriority | string;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const sizeClasses = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs font-medium';

  const getColorClasses = (val: string) => {
    switch (val) {
      // Complaint Status
      case 'Submitted':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'Under Review':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'In Progress':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'Resolved':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'Rejected':
        return 'bg-rose-50 text-rose-700 border-rose-200';

      // Room Occupancy
      case 'Available':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'Partially Occupied':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'Full':
        return 'bg-slate-100 text-slate-700 border-slate-200';

      // Priority
      case 'Low':
        return 'bg-slate-100 text-slate-700 border-slate-200';
      case 'Medium':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'High':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'Urgent':
        return 'bg-rose-50 text-rose-700 border-rose-200';

      // Student Status
      case 'Active':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'Inactive':
        return 'bg-slate-100 text-slate-600 border-slate-200';
      case 'Suspended':
        return 'bg-rose-50 text-rose-700 border-rose-200';

      default:
        return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  };

  return (
    <span
      className={`inline-flex items-center rounded-full border leading-none font-medium whitespace-nowrap ${sizeClasses} ${getColorClasses(
        status
      )}`}
    >
      <span className="w-1.5 h-1.5 rounded-full mr-1.5 bg-current opacity-80" />
      {status}
    </span>
  );
};
