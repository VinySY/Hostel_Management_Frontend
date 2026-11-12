import React, { useState, useEffect } from 'react';
import { Bell, Megaphone, AlertCircle, Info, Calendar, Filter, User } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { EmptyState } from '../../components/common/EmptyState';
import { apiService } from '../../services/api';
import { Notice } from '../../types';
import {
  NOTICE_CATEGORY_OPTIONS,
  getNoticeCategoryBadgeClass,
  getNoticeCategoryLabel,
} from '../../utils/noticeCategories';

export const StudentNotices: React.FC = () => {
  const [notices, setNotices] = useState<Notice[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setIsLoading(true);
    apiService.getNotices(false)
      .then((data) => {
        setNotices(data);
        setIsLoading(false);
      })
      .catch((err) => {
        console.error('Failed to load notices:', err);
        setIsLoading(false);
      });
  }, []);

  const filteredNotices = notices.filter((n) => {
    if (selectedCategory === 'ALL') return true;
    return n.category === selectedCategory;
  });

  const getPriorityBadge = (priority: string) => {
    switch (priority) {
      case 'URGENT':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-red-100 text-red-800 border border-red-200">URGENT</span>;
      case 'HIGH':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-200">HIGH</span>;
      case 'MEDIUM':
        return <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-blue-50 text-blue-700 border border-blue-200">MEDIUM</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-700">GENERAL</span>;
    }
  };

  const getCategoryBadge = (category: string) => {
    return (
      <span
        className={`px-2 py-0.5 rounded text-[10px] font-semibold uppercase tracking-wider ${getNoticeCategoryBadgeClass(
          category
        )}`}
      >
        {getNoticeCategoryLabel(category)}
      </span>
    );
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Hostel Notices & Announcements</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Official announcements issued by Wardens and the Director General of Hostels
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Filter className="w-3.5 h-3.5 text-slate-400" />
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 font-medium text-slate-700"
          >
            <option value="ALL">All Categories</option>
            {NOTICE_CATEGORY_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-slate-400">Loading published notices...</div>
      ) : filteredNotices.length === 0 ? (
        <EmptyState
          title="No active notices"
          description="There are currently no active announcements matching the selected filter."
        />
      ) : (
        <div className="space-y-4">
          {filteredNotices.map((notice) => (
            <Card
              key={notice.id}
              className={`!p-5 border-l-4 transition-all hover:shadow-sm ${
                notice.priority === 'URGENT'
                  ? 'border-l-red-500'
                  : notice.priority === 'HIGH'
                  ? 'border-l-amber-500'
                  : 'border-l-blue-500'
              }`}
            >
              <div className="flex items-start justify-between gap-3 mb-2">
                <div className="flex items-center gap-2">
                  <Megaphone className={`w-4 h-4 ${
                    notice.priority === 'URGENT' ? 'text-red-500' : 'text-blue-500'
                  }`} />
                  <h3 className="text-sm font-bold text-slate-900">{notice.title}</h3>
                </div>
                <div className="flex items-center gap-1.5 flex-shrink-0">
                  {getCategoryBadge(notice.category)}
                  {getPriorityBadge(notice.priority)}
                </div>
              </div>

              <p className="text-xs text-slate-700 whitespace-pre-line leading-relaxed mb-4">
                {notice.message}
              </p>

              <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-100 text-[11px] text-slate-400">
                <div className="flex items-center gap-3">
                  <span className="flex items-center gap-1 font-medium text-slate-600">
                    <User className="w-3.5 h-3.5 text-slate-400" />
                    {notice.authorName} ({notice.authorRole})
                  </span>
                  {notice.hostelName && (
                    <span className="font-semibold text-slate-500">
                      • {notice.hostelName}
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-1 font-mono">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  {notice.createdAt ? new Date(notice.createdAt).toLocaleDateString(undefined, {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  }) : 'Recent'}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};
