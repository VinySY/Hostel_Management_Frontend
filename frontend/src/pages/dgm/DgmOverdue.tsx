import React, { useState, useEffect } from 'react';
import {
  AlertTriangle,
  Clock,
  Shield,
  Layers,
  ArrowUpRight,
  CheckCircle2,
  Filter,
  User,
  Building2,
  Calendar,
  AlertOctagon
} from 'lucide-react';
import { apiService } from '../../services/api';
import { Complaint, Warden, Hostel } from '../../types';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { PriorityBadge } from '../../components/common/PriorityBadge';
import { EmptyState } from '../../components/common/EmptyState';

export const DgmOverdue: React.FC = () => {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [wardens, setWardens] = useState<Warden[]>([]);
  const [hostels, setHostels] = useState<Hostel[]>([]);
  const [selectedHostelFilter, setSelectedHostelFilter] = useState('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const fetchData = async () => {
    setIsLoading(true);
    try {
      const [allComplaints, wList, hList] = await Promise.all([
        apiService.getComplaints(),
        apiService.getWardens(),
        apiService.getHostels(),
      ]);
      setComplaints(allComplaints);
      setWardens(wList);
      setHostels(hList);
    } catch (err) {
      console.error('Failed to load overdue complaints:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Filter complaints that are Overdue, SLA-breached, Escalated, or High/Urgent unresolved
  const overdueTickets = complaints.filter((c) => {
    const isUnresolved = c.status !== 'Resolved' && c.status !== 'Rejected';
    const isOverdueOrEscalated = Boolean(c.status === 'Overdue' || c.escalated || c.slaBreached);
    const matchesHostel =
      selectedHostelFilter === 'ALL' ||
      (c.hostelId !== undefined && String(c.hostelId) === selectedHostelFilter) ||
      c.hostelName === selectedHostelFilter;

    return (
      isUnresolved &&
      (isOverdueOrEscalated || c.priority === 'Urgent' || c.priority === 'High') &&
      matchesHostel
    );
  });

  const getResponsibleWarden = (roomNumber?: string, floor?: number) => {
    if (!floor && roomNumber) {
      const parsedFloor = parseInt(roomNumber.charAt(0), 10);
      if (!isNaN(parsedFloor)) floor = parsedFloor;
    }

    if (!floor) return 'Unassigned';

    const match = wardens.find((w) => w.assignedFloors && w.assignedFloors.includes(floor!));
    return match ? `${match.name} (Floor ${floor})` : `Warden on Duty (Floor ${floor})`;
  };

  const handleEscalateDirectly = async (complaintId: string) => {
    try {
      await apiService.escalateComplaint(
        complaintId,
        'Direct DGM executive intervention due to SLA breach',
        'Initiated from the DGM Overdue & Supervisory Accountability view.'
      );
      setActionSuccess(`Ticket #${complaintId} escalated to highest executive priority`);
      setTimeout(() => setActionSuccess(null), 3000);
      await fetchData();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to escalate ticket';
      alert(message);
    }
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Overdue Complaints & Supervisory Accountability</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Identify SLA breaches, enforce floor warden accountability, and initiate executive escalations
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Filter className="w-3.5 h-3.5 text-slate-400" />
          <select
            value={selectedHostelFilter}
            onChange={(e) => setSelectedHostelFilter(e.target.value)}
            className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 font-medium text-slate-700"
          >
            <option value="ALL">All Campus Hostels</option>
            {hostels.map((h) => (
              <option key={h.id} value={h.name}>
                {h.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {actionSuccess && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-xs text-emerald-800 flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
          <span>{actionSuccess}</span>
        </div>
      )}

      {/* Overview Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="!p-4 bg-red-50 border-red-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-red-800 uppercase tracking-wider">
              Critical & Overdue
            </span>
            <AlertOctagon className="w-4 h-4 text-red-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-3xl font-black text-red-900">{overdueTickets.length}</span>
            <span className="text-xs text-red-700">active breaches</span>
          </div>
        </Card>

        <Card className="!p-4 bg-amber-50 border-amber-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-amber-800 uppercase tracking-wider">
              Pending Delegations
            </span>
            <Clock className="w-4 h-4 text-amber-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-3xl font-black text-amber-900">
              {overdueTickets.filter((t) => !t.escalated).length}
            </span>
            <span className="text-xs text-amber-700">awaiting closure</span>
          </div>
        </Card>

        <Card className="!p-4 bg-indigo-50 border-indigo-200">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-indigo-800 uppercase tracking-wider">
              Executive Escalated
            </span>
            <Shield className="w-4 h-4 text-indigo-600" />
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-3xl font-black text-indigo-900">
              {overdueTickets.filter((t) => Boolean(t.escalated)).length}
            </span>
            <span className="text-xs text-indigo-700">under DGM review</span>
          </div>
        </Card>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-slate-400">Auditing SLA compliance across hostels...</div>
      ) : overdueTickets.length === 0 ? (
        <EmptyState
          title="All complaints within SLA threshold"
          description="There are currently zero overdue or critical complaints exceeding resolution deadlines."
        />
      ) : (
        <div className="space-y-4">
          {overdueTickets.map((t) => {
            const responsible = getResponsibleWarden(t.roomNumber, t.floor ?? undefined);
            return (
              <Card
                key={t.id}
                className="!p-5 border-l-4 border-l-red-500 hover:shadow-sm transition-all"
              >
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 mb-2">
                  <div>
                    <div className="flex items-center gap-2 mb-1.5">
                      <span className="text-xs font-mono font-bold text-slate-400">
                        #{t.complaintNumber || t.id}
                      </span>
                      <PriorityBadge priority={t.priority} size="sm" />
                      <StatusBadge status={t.status} size="sm" />
                      <span className="text-[10px] font-bold px-2 py-0.5 bg-red-100 text-red-800 rounded">
                        SLA BREACH
                      </span>
                    </div>
                    <h3 className="text-sm font-bold text-slate-900">{t.subject}</h3>
                  </div>

                  <div className="flex items-center gap-2 self-start">
                    {!t.escalated ? (
                      <Button
                        variant="primary"
                        size="sm"
                        icon={<ArrowUpRight className="w-3.5 h-3.5" />}
                        onClick={() => handleEscalateDirectly(t.id)}
                        className="text-xs bg-red-600 hover:bg-red-700 border-red-600"
                      >
                        Escalate to DGM
                      </Button>
                    ) : (
                      <span className="text-[11px] font-semibold px-2.5 py-1 bg-indigo-100 text-indigo-800 rounded-lg">
                        Executive Escalated
                      </span>
                    )}
                  </div>
                </div>

                <p className="text-xs text-slate-700 whitespace-pre-line mb-3 bg-slate-50 p-2.5 rounded-lg border border-slate-100">
                  {t.description}
                </p>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 p-3 bg-slate-50/70 rounded-lg text-xs border border-slate-100">
                  <div>
                    <span className="text-[11px] text-slate-400 block">Location</span>
                    <span className="font-semibold text-slate-800">
                      {t.hostelName || t.block || 'Hostel'}, Room {t.roomNumber || 'Common Area'}
                    </span>
                  </div>

                  <div>
                    <span className="text-[11px] text-slate-400 block">Responsible Warden</span>
                    <span className="font-semibold text-indigo-700">{responsible}</span>
                  </div>

                  <div>
                    <span className="text-[11px] text-slate-400 block">Reported By</span>
                    <span className="font-semibold text-slate-800">
                      {t.studentName || 'Resident Student'}
                    </span>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};
