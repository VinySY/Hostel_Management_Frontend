import React, { useEffect, useState } from 'react';
import { ArrowLeft, Clock, MapPin, User, FileText, ExternalLink } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { apiService } from '../../services/api';
import { Complaint } from '../../types';

interface ComplaintDetailProps {
  complaintId: string;
  onBack: () => void;
}

export const ComplaintDetail: React.FC<ComplaintDetailProps> = ({
  complaintId,
  onBack,
}) => {
  const [complaint, setComplaint] = useState<Complaint | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    apiService.getComplaintById(complaintId).then((res) => {
      setComplaint(res);
      setIsLoading(false);
    });
  }, [complaintId]);

  if (isLoading || !complaint) {
    return (
      <div className="p-8 text-center text-slate-400">
        <div className="animate-spin w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full mx-auto mb-2" />
        Loading complaint record...
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <Button variant="ghost" size="sm" icon={<ArrowLeft className="w-4 h-4" />} onClick={onBack}>
            Back
          </Button>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-xl font-bold text-slate-900">{complaint.complaintNumber}</h2>
              <StatusBadge status={complaint.status} />
              <StatusBadge status={complaint.priority} size="sm" />
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Category: {complaint.type} • Submitted on{' '}
              {new Date(complaint.createdAt).toLocaleDateString('en-GB', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <Card title="Subject & Overview">
            <h3 className="text-base font-semibold text-slate-800 mb-2">{complaint.subject}</h3>
            <p className="text-sm text-slate-600 whitespace-pre-line leading-relaxed">
              {complaint.description}
            </p>

            <div className="mt-4 pt-4 border-t border-slate-100 flex flex-wrap gap-4 text-xs text-slate-500">
              <div className="flex items-center space-x-1.5">
                <MapPin className="w-4 h-4 text-slate-400" />
                <span>{complaint.location}</span>
              </div>
              <div className="flex items-center space-x-1.5">
                <User className="w-4 h-4 text-slate-400" />
                <span>
                  {complaint.studentName} ({complaint.block}, Rm {complaint.roomNumber})
                </span>
              </div>
            </div>
          </Card>

          {/* Evidence Attachments */}
          <Card title="Uploaded Evidence" subtitle="Photos or videos provided with this ticket">
            {complaint.attachments.length === 0 ? (
              <p className="text-xs text-slate-400 italic">No attachments provided with this ticket.</p>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {complaint.attachments.map((att) => (
                  <div
                    key={att.id}
                    className="p-3 border border-slate-200 rounded-lg flex items-center justify-between bg-slate-50"
                  >
                    <div className="flex items-center space-x-2 truncate">
                      <FileText className="w-4 h-4 text-blue-500 flex-shrink-0" />
                      <span className="text-xs font-medium text-slate-700 truncate">{att.name}</span>
                    </div>
                    {att.url && (
                      <a
                        href={att.url}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs text-blue-600 hover:underline flex items-center ml-2 flex-shrink-0"
                      >
                        <ExternalLink className="w-3.5 h-3.5 mr-1" /> View
                      </a>
                    )}
                  </div>
                ))}
              </div>
            )}
          </Card>

          {/* Warden Administrative Comments */}
          {complaint.adminNotes && (
            <Card title="Administrative Response" className="border-l-4 border-l-blue-600 bg-blue-50/20">
              <p className="text-xs font-medium text-blue-900 mb-1">Official Warden / Team Note:</p>
              <p className="text-sm text-slate-700 leading-relaxed">{complaint.adminNotes}</p>
            </Card>
          )}
        </div>

        {/* Timeline Tracking Sidebar */}
        <div className="space-y-6">
          <Card title="Progress Timeline" subtitle="Audit trail and status updates">
            <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
              {complaint.timeline.map((item, idx) => (
                <div key={idx} className="relative">
                  <div className="absolute -left-6 top-1 w-3.5 h-3.5 rounded-full border-2 border-white bg-blue-600 ring-2 ring-blue-100" />
                  <div>
                    <span className="text-xs font-semibold text-slate-800">{item.status}</span>
                    <div className="flex items-center text-[10px] text-slate-400 mt-0.5">
                      <Clock className="w-3 h-3 mr-1" />
                      {new Date(item.timestamp).toLocaleString('en-GB', {
                        day: '2-digit',
                        month: 'short',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </div>
                    {item.note && (
                      <p className="text-xs text-slate-600 mt-1 bg-slate-50 p-2 rounded border border-slate-100">
                        {item.note}
                      </p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};
