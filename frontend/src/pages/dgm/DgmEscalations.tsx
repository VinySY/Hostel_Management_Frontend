import React, { useEffect, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  ChevronDown,
  ChevronUp,
  Loader2,
  RefreshCw,
  MessageSquare,
  X,
} from 'lucide-react';
import { apiService } from '../../services/api';
import { Escalation } from '../../types';
import { useToast } from '../../context/ToastContext';

const STATUS_BADGE: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-800 border-amber-200',
  IN_REVIEW: 'bg-blue-100 text-blue-800 border-blue-200',
  RESOLVED: 'bg-green-100 text-green-800 border-green-200',
  REJECTED: 'bg-red-100 text-red-800 border-red-200',
};

interface HandleModalProps {
  escalation: Escalation;
  onClose: () => void;
  onSuccess: (updated: Escalation) => void;
}

const HandleModal: React.FC<HandleModalProps> = ({ escalation, onClose, onSuccess }) => {
  const { showToast } = useToast();
  const [status, setStatus] = useState<'RESOLVED' | 'REJECTED' | 'IN_REVIEW'>('IN_REVIEW');
  const [response, setResponse] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!response.trim()) {
      showToast('Please provide a response before submitting.', 'error');
      return;
    }
    setSubmitting(true);
    try {
      const updated = await apiService.handleDgmEscalation(escalation.id, { status, response });
      showToast('Escalation handled successfully.', 'success');
      onSuccess(updated);
    } catch (err) {
      showToast('Failed to handle escalation. Please try again.', 'error');
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg">
        <div className="flex items-center justify-between p-5 border-b border-slate-100">
          <div>
            <h3 className="text-base font-bold text-slate-900">Handle Escalation</h3>
            <p className="text-xs text-slate-500 mt-0.5">{escalation.complaintNumber}</p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700 cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-5">
          <div className="bg-amber-50 border border-amber-100 rounded-lg p-3 mb-4">
            <p className="text-xs text-slate-600 font-medium mb-1">Student Reason:</p>
            <p className="text-sm text-slate-800">{escalation.reason}</p>
            {escalation.details && (
              <p className="text-xs text-slate-500 mt-1">{escalation.details}</p>
            )}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Action
              </label>
              <div className="grid grid-cols-3 gap-2">
                {(['IN_REVIEW', 'RESOLVED', 'REJECTED'] as const).map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => setStatus(s)}
                    className={`py-1.5 px-2 text-xs font-medium rounded-lg border transition-colors cursor-pointer ${
                      status === s
                        ? STATUS_BADGE[s] + ' border-current font-semibold'
                        : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    {s.replace('_', ' ')}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Your Response <span className="text-red-500">*</span>
              </label>
              <textarea
                rows={4}
                value={response}
                onChange={(e) => setResponse(e.target.value)}
                placeholder="Provide your official response or decision for this escalation..."
                className="w-full text-sm border border-slate-200 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none text-slate-800"
                required
              />
            </div>

            <div className="flex space-x-3 pt-1">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2 text-sm font-medium text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="flex-1 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 cursor-pointer flex items-center justify-center"
              >
                {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Submit Decision'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export const DgmEscalations: React.FC = () => {
  const [escalations, setEscalations] = useState<Escalation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<number | null>(null);
  const [handling, setHandling] = useState<Escalation | null>(null);
  const [filter, setFilter] = useState<'ALL' | 'PENDING' | 'IN_REVIEW' | 'RESOLVED' | 'REJECTED'>('ALL');

  const fetchEscalations = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiService.getDgmPendingEscalations();
      setEscalations(data);
    } catch (err) {
      setError('Failed to load escalations.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEscalations();
  }, []);

  const handleSuccess = (updated: Escalation) => {
    setEscalations((prev) =>
      prev.map((e) => (e.id === updated.id ? updated : e))
    );
    setHandling(null);
  };

  const filtered = filter === 'ALL' ? escalations : escalations.filter((e) => e.status === filter);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-7 h-7 text-blue-600 animate-spin" />
        <span className="ml-3 text-slate-600 text-sm">Loading escalations…</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center">
        <AlertTriangle className="w-8 h-8 text-red-500 mx-auto mb-2" />
        <p className="text-sm text-red-700">{error}</p>
        <button onClick={fetchEscalations} className="mt-3 text-xs text-red-600 hover:underline flex items-center mx-auto cursor-pointer">
          <RefreshCw className="w-3.5 h-3.5 mr-1" /> Retry
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {/* Filters */}
      <div className="flex flex-wrap gap-2">
        {(['ALL', 'PENDING', 'IN_REVIEW', 'RESOLVED', 'REJECTED'] as const).map((f) => {
          const count = f === 'ALL' ? escalations.length : escalations.filter((e) => e.status === f).length;
          return (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors cursor-pointer flex items-center space-x-1.5 ${
                filter === f
                  ? 'bg-blue-600 text-white border-blue-600'
                  : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
              }`}
            >
              <span>{f.replace('_', ' ')}</span>
              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full ${
                filter === f ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-600'
              }`}>
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Empty state */}
      {filtered.length === 0 && (
        <div className="bg-white border border-slate-200 rounded-xl p-10 text-center">
          <CheckCircle2 className="w-10 h-10 text-green-400 mx-auto mb-3" />
          <p className="text-sm font-medium text-slate-600">No escalations found</p>
          <p className="text-xs text-slate-400 mt-1">
            {filter === 'ALL' ? 'There are no escalations in the system yet.' : `No escalations with status "${filter.replace('_', ' ')}".`}
          </p>
        </div>
      )}

      {/* Escalation list */}
      <div className="space-y-3">
        {filtered.map((esc) => {
          const isExpanded = expanded === esc.id;
          const isPending = esc.status === 'PENDING' || esc.status === 'IN_REVIEW';
          return (
            <div key={esc.id} className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
              {/* Header row */}
              <div
                className="flex items-center justify-between p-4 cursor-pointer hover:bg-slate-50 transition-colors"
                onClick={() => setExpanded(isExpanded ? null : esc.id)}
              >
                <div className="flex items-center space-x-3 min-w-0">
                  <div className={`w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0 ${
                    isPending ? 'bg-amber-50' : 'bg-green-50'
                  }`}>
                    {isPending
                      ? <Clock className="w-5 h-5 text-amber-600" />
                      : <CheckCircle2 className="w-5 h-5 text-green-600" />
                    }
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center space-x-2">
                      <span className="text-sm font-semibold text-slate-900">{esc.complaintNumber}</span>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${STATUS_BADGE[esc.status]}`}>
                        {esc.status.replace('_', ' ')}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 truncate">{esc.escalatedByStudentName} · {new Date(esc.escalatedAt).toLocaleDateString()}</p>
                  </div>
                </div>
                <div className="flex items-center space-x-2 flex-shrink-0 ml-2">
                  {isPending && (
                    <button
                      onClick={(e) => { e.stopPropagation(); setHandling(esc); }}
                      className="px-3 py-1.5 text-xs font-semibold bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors cursor-pointer"
                    >
                      Review
                    </button>
                  )}
                  {isExpanded ? (
                    <ChevronUp className="w-4 h-4 text-slate-400" />
                  ) : (
                    <ChevronDown className="w-4 h-4 text-slate-400" />
                  )}
                </div>
              </div>

              {/* Expanded detail */}
              {isExpanded && (
                <div className="px-4 pb-4 border-t border-slate-100 pt-4 space-y-3">
                  <div>
                    <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">Student Reason</p>
                    <p className="text-sm text-slate-800">{esc.reason}</p>
                  </div>
                  {esc.details && (
                    <div>
                      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">Supporting Details</p>
                      <p className="text-sm text-slate-700">{esc.details}</p>
                    </div>
                  )}
                  {esc.dgmResponse && (
                    <div className="bg-blue-50 border border-blue-100 rounded-lg p-3">
                      <p className="text-xs font-semibold text-blue-700 mb-1 flex items-center">
                        <MessageSquare className="w-3.5 h-3.5 mr-1" /> DGM Response
                      </p>
                      <p className="text-sm text-slate-700">{esc.dgmResponse}</p>
                      {esc.handledByDgmName && (
                        <p className="text-xs text-blue-600 mt-1">— {esc.handledByDgmName}</p>
                      )}
                    </div>
                  )}
                  <div className="flex space-x-4 text-xs text-slate-400">
                    <span>Escalated: {new Date(esc.escalatedAt).toLocaleString()}</span>
                    {esc.handledAt && (
                      <span>Handled: {new Date(esc.handledAt).toLocaleString()}</span>
                    )}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Handle Modal */}
      {handling && (
        <HandleModal
          escalation={handling}
          onClose={() => setHandling(null)}
          onSuccess={handleSuccess}
        />
      )}
    </div>
  );
};
