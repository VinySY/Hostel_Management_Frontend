import React, { useState, useEffect } from 'react';
import { Clock, MapPin, User, FileText, ExternalLink, Save } from 'lucide-react';
import { Modal } from '../../components/common/Modal';
import { Button } from '../../components/common/Button';
import { Select } from '../../components/common/Select';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Complaint, ComplaintPriority, ComplaintStatus } from '../../types';

interface WardenComplaintDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  complaint: Complaint | null;
  onUpdate: (
    id: string,
    status: ComplaintStatus,
    adminNotes: string,
    priority: ComplaintPriority
  ) => Promise<void>;
}

export const WardenComplaintDetailModal: React.FC<WardenComplaintDetailModalProps> = ({
  isOpen,
  onClose,
  complaint,
  onUpdate,
}) => {
  const [status, setStatus] = useState<ComplaintStatus>('Submitted');
  const [priority, setPriority] = useState<ComplaintPriority>('Medium');
  const [adminNotes, setAdminNotes] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);

  useEffect(() => {
    if (complaint) {
      setStatus(complaint.status);
      setPriority(complaint.priority);
      setAdminNotes(complaint.adminNotes || '');
    }
  }, [complaint]);

  if (!complaint) return null;

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsUpdating(true);
    try {
      await onUpdate(complaint.id, status, adminNotes, priority);
      onClose();
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Complaint #${complaint.complaintNumber} - Triage & Resolution`}
      size="xl"
    >
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 cols: Complaint details & attachments */}
        <div className="lg:col-span-2 space-y-4">
          <div className="p-4 bg-slate-50 rounded-xl border border-slate-200">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-blue-100 text-blue-800">
                {complaint.type}
              </span>
              <span className="text-xs text-slate-400">
                Logged:{' '}
                {new Date(complaint.createdAt).toLocaleDateString('en-GB', {
                  day: '2-digit',
                  month: 'short',
                  year: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </span>
            </div>
            <h4 className="text-base font-bold text-slate-900 mb-2">{complaint.subject}</h4>
            <p className="text-xs text-slate-600 leading-relaxed whitespace-pre-line">
              {complaint.description}
            </p>

            <div className="mt-4 pt-3 border-t border-slate-200 flex flex-wrap gap-4 text-xs text-slate-500">
              <span className="flex items-center">
                <MapPin className="w-3.5 h-3.5 mr-1 text-slate-400" />
                {complaint.location}
              </span>
              <span className="flex items-center">
                <User className="w-3.5 h-3.5 mr-1 text-slate-400" />
                {complaint.studentName} ({complaint.block}, Rm {complaint.roomNumber})
              </span>
            </div>
          </div>

          {/* Attachments preview */}
          <div>
            <h5 className="text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
              Submitted Attachments & Evidence ({complaint.attachments.length})
            </h5>
            {complaint.attachments.length === 0 ? (
              <p className="text-xs text-slate-400 italic">No attachments submitted with this ticket.</p>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                {complaint.attachments.map((att) => (
                  <div
                    key={att.id}
                    className="p-2.5 rounded-lg border border-slate-200 flex items-center justify-between bg-white text-xs"
                  >
                    <div className="flex items-center space-x-2 truncate">
                      <FileText className="w-4 h-4 text-blue-500 flex-shrink-0" />
                      <span className="font-medium text-slate-800 truncate">{att.name}</span>
                    </div>
                    {att.url && (
                      <a
                        href={att.url}
                        target="_blank"
                        rel="noreferrer"
                        className="text-blue-600 hover:underline flex items-center ml-2 flex-shrink-0"
                      >
                        <ExternalLink className="w-3 h-3 mr-1" /> View
                      </a>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Audit Timeline */}
          <div>
            <h5 className="text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
              Action Timeline
            </h5>
            <div className="space-y-2 max-h-40 overflow-y-auto pr-1">
              {complaint.timeline.map((item, idx) => (
                <div
                  key={idx}
                  className="p-2.5 rounded bg-slate-50 text-xs border border-slate-100 flex items-start justify-between"
                >
                  <div>
                    <div className="flex items-center space-x-2">
                      <StatusBadge status={item.status} size="sm" />
                      {item.note && <span className="text-slate-600">{item.note}</span>}
                    </div>
                  </div>
                  <span className="text-[10px] text-slate-400 flex items-center flex-shrink-0 ml-2">
                    <Clock className="w-3 h-3 mr-0.5" />
                    {new Date(item.timestamp).toLocaleDateString('en-GB', {
                      day: '2-digit',
                      month: 'short',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right col: Warden Action form */}
        <form
          onSubmit={handleSave}
          className="p-4 bg-slate-50/70 border border-slate-200 rounded-xl space-y-4 flex flex-col justify-between"
        >
          <div className="space-y-4">
            <h5 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
              Warden Administration
            </h5>

            <Select
              label="Update Status"
              value={status}
              onChange={(e) => setStatus(e.target.value as ComplaintStatus)}
              options={[
                { value: 'Submitted', label: 'Submitted (New)' },
                { value: 'Under Review', label: 'Under Review' },
                { value: 'In Progress', label: 'In Progress / Assigned' },
                { value: 'Resolved', label: 'Resolved (Closed)' },
                { value: 'Rejected', label: 'Rejected (Invalid)' },
              ]}
              required
            />

            <Select
              label="Triage Priority"
              value={priority}
              onChange={(e) => setPriority(e.target.value as ComplaintPriority)}
              options={[
                { value: 'Low', label: 'Low - Standard turnaround' },
                { value: 'Medium', label: 'Medium - Normal maintenance' },
                { value: 'High', label: 'High - Expedited fix' },
                { value: 'Urgent', label: 'Urgent - Safety / Emergency' },
              ]}
              required
            />

            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Admin Comments / Instructions
              </label>
              <textarea
                rows={4}
                value={adminNotes}
                onChange={(e) => setAdminNotes(e.target.value)}
                placeholder="e.g. Electrician assigned with ticket #EL-889. Work order issued..."
                className="w-full text-xs rounded-lg border border-slate-300 p-2.5 bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 text-slate-800"
              />
              <p className="text-[10px] text-slate-400 mt-1">
                This note will be visible to the student resident in their complaint portal.
              </p>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-200 flex justify-end space-x-2">
            <Button type="button" variant="outline" size="sm" onClick={onClose} disabled={isUpdating}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              icon={<Save className="w-3.5 h-3.5" />}
              isLoading={isUpdating}
            >
              Update Ticket
            </Button>
          </div>
        </form>
      </div>
    </Modal>
  );
};
