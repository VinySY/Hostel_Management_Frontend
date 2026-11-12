import { NoticeCategory } from '../types';

export interface NoticeCategoryOption {
  value: NoticeCategory;
  label: string;
}

/**
 * Exact backend NoticeCategory enum values paired with readable UI labels.
 */
export const NOTICE_CATEGORY_OPTIONS: readonly NoticeCategoryOption[] = [
  { value: 'GENERAL', label: 'General Announcement' },
  { value: 'MAINTENANCE', label: 'Maintenance / Utility Outage' },
  { value: 'MESS', label: 'Mess & Dining' },
  { value: 'SECURITY', label: 'Security & Curfew' },
  { value: 'EVENT', label: 'Hostel Event' },
  { value: 'EMERGENCY', label: 'Emergency Alert' },
] as const;

const CATEGORY_LABEL_MAP: Record<NoticeCategory, string> = {
  GENERAL: 'General',
  MAINTENANCE: 'Maintenance',
  MESS: 'Mess',
  SECURITY: 'Security',
  EVENT: 'Event',
  EMERGENCY: 'Emergency',
};

const CATEGORY_BADGE_MAP: Record<NoticeCategory, string> = {
  GENERAL: 'bg-slate-100 text-slate-700',
  MAINTENANCE: 'bg-amber-50 text-amber-800 border border-amber-200',
  MESS: 'bg-emerald-50 text-emerald-800 border border-emerald-200',
  SECURITY: 'bg-indigo-50 text-indigo-800 border border-indigo-200',
  EVENT: 'bg-sky-50 text-sky-800 border border-sky-200',
  EMERGENCY: 'bg-red-100 text-red-800 border border-red-200',
};

export function isNoticeCategory(value: string): value is NoticeCategory {
  return value in CATEGORY_LABEL_MAP;
}

export function getNoticeCategoryLabel(category: NoticeCategory | string): string {
  return isNoticeCategory(category) ? CATEGORY_LABEL_MAP[category] : category;
}

export function getNoticeCategoryBadgeClass(category: NoticeCategory | string): string {
  return isNoticeCategory(category) ? CATEGORY_BADGE_MAP[category] : 'bg-slate-100 text-slate-700';
}

