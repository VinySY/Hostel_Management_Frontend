import React, { useState, useEffect } from 'react';
import { Megaphone, Plus, Calendar, AlertCircle, CheckCircle2, Power, Eye } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { EmptyState } from '../../components/common/EmptyState';
import { apiService } from '../../services/api';
import { Notice, NoticeCategory } from '../../types';
import {
  NOTICE_CATEGORY_OPTIONS,
  getNoticeCategoryBadgeClass,
  getNoticeCategoryLabel,
  isNoticeCategory,
} from '../../utils/noticeCategories';

export const WardenNotices: React.FC = () => {
  const [notices, setNotices] = useState<Notice[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isPublishModalOpen, setIsPublishModalOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [category, setCategory] = useState<NoticeCategory>('GENERAL');
  const [priority, setPriority] = useState<Notice['priority']>('MEDIUM');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchNotices = async () => {
    setIsLoading(true);
    try {
      const data = await apiService.getNotices();
      setNotices(data);
    } catch (err) {
      console.error('Failed to load notices:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchNotices();
  }, []);

  const handlePublish = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);

    try {
      await apiService.createNotice({
        title: title.trim(),
        message: message.trim(),
        category,
        priority,
      });
      setIsPublishModalOpen(false);
      setTitle('');
      setMessage('');
      setCategory('GENERAL');
      setPriority('MEDIUM');
      await fetchNotices();
    } catch (err: any) {
      setError(err.message || 'Failed to publish notice');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggleStatus = async (id: number, currentActive: boolean) => {
    try {
      await apiService.toggleNoticeStatus(id, !currentActive);
      await fetchNotices();
    } catch (err) {
      console.error('Failed to toggle notice status:', err);
    }
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Hostel Announcements & Notices</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Publish official announcements broadcast directly to resident student dashboards
          </p>
        </div>

        <Button
          variant="primary"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => setIsPublishModalOpen(true)}
          className="text-xs"
        >
          Publish Notice
        </Button>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-slate-400">Loading notices...</div>
      ) : notices.length === 0 ? (
        <EmptyState
          title="No notices published"
          description="Click 'Publish Notice' above to post an announcement for resident students."
        />
      ) : (
        <div className="space-y-4">
          {notices.map((n) => (
            <Card
              key={n.id}
              className={`!p-5 border-l-4 transition-all ${
                !n.active
                  ? 'opacity-60 bg-slate-50/70 border-l-slate-300'
                  : n.priority === 'URGENT'
                  ? 'border-l-red-500'
                  : n.priority === 'HIGH'
                  ? 'border-l-amber-500'
                  : 'border-l-blue-500'
              }`}
            >
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 mb-2">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span
                      className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded ${getNoticeCategoryBadgeClass(
                        n.category
                      )}`}
                    >
                      {getNoticeCategoryLabel(n.category)}
                    </span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        n.priority === 'URGENT'
                          ? 'bg-red-100 text-red-800'
                          : n.priority === 'HIGH'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-blue-100 text-blue-800'
                      }`}
                    >
                      {n.priority}
                    </span>
                    {!n.active && (
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-slate-200 text-slate-600">
                        INACTIVE / ARCHIVED
                      </span>
                    )}
                  </div>
                  <h3 className="text-sm font-bold text-slate-900">{n.title}</h3>
                </div>

                <div className="flex items-center gap-2 self-start">
                  <Button
                    variant={n.active ? 'outline' : 'primary'}
                    size="sm"
                    icon={<Power className="w-3.5 h-3.5" />}
                    onClick={() => handleToggleStatus(n.id, n.active)}
                    className="text-xs"
                  >
                    {n.active ? 'Deactivate' : 'Reactivate'}
                  </Button>
                </div>
              </div>

              <p className="text-xs text-slate-700 whitespace-pre-line leading-relaxed mb-4">
                {n.message}
              </p>

              <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-100 text-[11px] text-slate-400">
                <span>By: <strong className="text-slate-600">{n.authorName}</strong> ({n.authorRole})</span>
                {n.hostelName && <span>Hostel: <strong className="text-slate-600">{n.hostelName}</strong></span>}
                <span>Posted: {new Date(n.createdAt).toLocaleDateString()}</span>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Publish Notice Modal */}
      <Modal
        isOpen={isPublishModalOpen}
        onClose={() => setIsPublishModalOpen(false)}
        title="Publish Hostel Announcement"
        size="md"
      >
        <form onSubmit={handlePublish} className="space-y-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700">
              {error}
            </div>
          )}

          <Input
            label="Notice Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Scheduled RO Water Tank Cleaning on Sunday"
            required
          />

          <div className="grid grid-cols-2 gap-3">
            <Select
              label="Category"
              value={category}
              onChange={(e) => {
                if (isNoticeCategory(e.target.value)) {
                  setCategory(e.target.value);
                }
              }}
              options={[...NOTICE_CATEGORY_OPTIONS]}
            />

            <Select
              label="Priority Level"
              value={priority}
              onChange={(e) => setPriority(e.target.value as Notice['priority'])}
              options={[
                { value: 'LOW', label: 'Low - Informational' },
                { value: 'MEDIUM', label: 'Medium - Routine' },
                { value: 'HIGH', label: 'High - Action Required' },
                { value: 'URGENT', label: 'Urgent - Immediate Attention' },
              ]}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Announcement Message
            </label>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              rows={5}
              placeholder="Provide complete instructions, outage timings, contact points, or guidelines..."
              className="w-full p-2.5 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500 font-sans"
              required
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsPublishModalOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting}>
              Publish Announcement
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
