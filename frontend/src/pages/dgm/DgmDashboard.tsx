import React, { useEffect, useState } from 'react';
import {
  LayoutDashboard,
  AlertTriangle,
  CheckCircle2,
  Building2,
  TrendingUp,
  Clock,
  FileWarning,
  RefreshCw,
  ChevronRight,
  Loader2,
} from 'lucide-react';
import { apiService } from '../../services/api';
import { DgmDashboardResponse, Escalation } from '../../types';
import { useAuth } from '../../context/AuthContext';

interface DgmDashboardProps {
  onNavigate: (tab: string) => void;
}

const StatCard: React.FC<{
  label: string;
  value: number | string;
  icon: React.ReactNode;
  color: string;
  bg: string;
  sub?: string;
}> = ({ label, value, icon, color, bg, sub }) => (
  <div className="bg-white rounded-xl border border-slate-200 p-5 flex items-center space-x-4 shadow-sm hover:shadow-md transition-shadow">
    <div className={`w-12 h-12 ${bg} rounded-xl flex items-center justify-center flex-shrink-0`}>
      <span className={color}>{icon}</span>
    </div>
    <div>
      <p className="text-2xl font-bold text-slate-900">{value}</p>
      <p className="text-xs font-medium text-slate-500 mt-0.5">{label}</p>
      {sub && <p className="text-[11px] text-slate-400 mt-0.5">{sub}</p>}
    </div>
  </div>
);

export const DgmDashboard: React.FC<DgmDashboardProps> = ({ onNavigate }) => {
  const { user } = useAuth();
  const [stats, setStats] = useState<DgmDashboardResponse | null>(null);
  const [pendingEscalations, setPendingEscalations] = useState<Escalation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [dashboardData, escalations] = await Promise.all([
        apiService.getDgmDashboard(),
        apiService.getDgmPendingEscalations(),
      ]);
      setStats(dashboardData);
      setPendingEscalations(escalations);
    } catch (err) {
      setError('Failed to load dashboard data. Please try refreshing.');
      console.error('DGM dashboard load error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
        <span className="ml-3 text-slate-600 text-sm">Loading DGM Dashboard…</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center">
        <AlertTriangle className="w-8 h-8 text-red-500 mx-auto mb-2" />
        <p className="text-sm text-red-700 font-medium">{error}</p>
        <button
          onClick={fetchData}
          className="mt-3 text-xs text-red-600 hover:underline flex items-center mx-auto cursor-pointer"
        >
          <RefreshCw className="w-3.5 h-3.5 mr-1" /> Retry
        </button>
      </div>
    );
  }

  const s = stats!;
  const totalHostels = s.hostelStats.length;

  return (
    <div className="space-y-6">
      {/* Welcome banner */}
      <div className="bg-gradient-to-r from-indigo-700 to-blue-600 rounded-2xl p-6 text-white shadow-lg">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-blue-100 text-sm font-medium">Welcome back,</p>
            <h2 className="text-2xl font-bold mt-0.5">{user?.name ?? 'Director General'}</h2>
            <p className="text-blue-200 text-xs mt-1">Director General of Maintenance (DGM) · Overseeing {totalHostels} hostel{totalHostels !== 1 ? 's' : ''}</p>
          </div>
          <LayoutDashboard className="w-12 h-12 text-white/20" />
        </div>
      </div>

      {/* Key Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          label="Total Complaints"
          value={s.totalComplaints}
          icon={<FileWarning className="w-6 h-6" />}
          color="text-blue-600"
          bg="bg-blue-50"
          sub="across assigned hostels"
        />
        <StatCard
          label="Pending Escalations"
          value={s.pendingEscalations}
          icon={<AlertTriangle className="w-6 h-6" />}
          color="text-amber-600"
          bg="bg-amber-50"
          sub="awaiting your review"
        />
        <StatCard
          label="Resolved Escalations"
          value={s.resolvedEscalations}
          icon={<CheckCircle2 className="w-6 h-6" />}
          color="text-green-600"
          bg="bg-green-50"
          sub="handled successfully"
        />
        <StatCard
          label="Pending Assignments"
          value={s.pendingAssignments}
          icon={<Clock className="w-6 h-6" />}
          color="text-purple-600"
          bg="bg-purple-50"
          sub="unassigned complaints"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Escalation Status Breakdown */}
        <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-slate-800 flex items-center">
              <TrendingUp className="w-4 h-4 mr-2 text-blue-600" />
              Escalation Status
            </h3>
            <button
              onClick={() => onNavigate('dgm-escalations')}
              className="text-xs text-blue-600 hover:underline flex items-center cursor-pointer"
            >
              View All <ChevronRight className="w-3.5 h-3.5 ml-0.5" />
            </button>
          </div>
          <div className="space-y-3">
            {[
              { key: 'PENDING', label: 'Pending', color: 'bg-amber-500', text: 'text-amber-700', bg: 'bg-amber-50' },
              { key: 'IN_REVIEW', label: 'In Review', color: 'bg-blue-500', text: 'text-blue-700', bg: 'bg-blue-50' },
              { key: 'RESOLVED', label: 'Resolved', color: 'bg-green-500', text: 'text-green-700', bg: 'bg-green-50' },
              { key: 'REJECTED', label: 'Rejected', color: 'bg-red-400', text: 'text-red-700', bg: 'bg-red-50' },
            ].map(({ key, label, color, text, bg }) => {
              const count = s.escalationStatus[key] ?? 0;
              const total = Object.values(s.escalationStatus).reduce((a, b) => a + b, 0) || 1;
              const pct = Math.round((count / total) * 100);
              return (
                <div key={key}>
                  <div className="flex justify-between text-xs mb-1">
                    <span className={`font-medium ${text} ${bg} px-2 py-0.5 rounded-full`}>{label}</span>
                    <span className="font-semibold text-slate-700">{count}</span>
                  </div>
                  <div className="h-1.5 bg-slate-100 rounded-full overflow-hidden">
                    <div className={`h-full ${color} rounded-full transition-all`} style={{ width: `${pct}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Hostel Breakdown */}
        <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-slate-800 flex items-center">
              <Building2 className="w-4 h-4 mr-2 text-indigo-600" />
              Hostel Overview
            </h3>
            <button
              onClick={() => onNavigate('dgm-hostels')}
              className="text-xs text-blue-600 hover:underline flex items-center cursor-pointer"
            >
              View All <ChevronRight className="w-3.5 h-3.5 ml-0.5" />
            </button>
          </div>
          {s.hostelStats.length === 0 ? (
            <div className="text-center py-8 text-slate-400 text-sm">
              <Building2 className="w-8 h-8 mx-auto mb-2 opacity-40" />
              No hostels assigned yet
            </div>
          ) : (
            <div className="space-y-3">
              {s.hostelStats.map((h) => (
                <div key={h.hostelId} className="border border-slate-100 rounded-lg p-3 hover:bg-slate-50 transition-colors">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-semibold text-slate-800">{h.hostelName}</span>
                    <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full font-medium">
                      {h.totalComplaints} complaints
                    </span>
                  </div>
                  <div className="flex space-x-3 text-[11px] text-slate-500">
                    <span className="text-green-600">✓ {h.resolvedComplaints} resolved</span>
                    {h.pendingEscalations > 0 && (
                      <span className="text-amber-600">⚠ {h.pendingEscalations} escalations</span>
                    )}
                    {h.overdueComplaints > 0 && (
                      <span className="text-red-600">🕐 {h.overdueComplaints} overdue</span>
                    )}
                    {h.reopenedComplaints > 0 && (
                      <span className="text-purple-600">↺ {h.reopenedComplaints} reopened</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Recent Pending Escalations */}
      <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-slate-800 flex items-center">
            <AlertTriangle className="w-4 h-4 mr-2 text-amber-500" />
            Pending Escalations
            {pendingEscalations.length > 0 && (
              <span className="ml-2 bg-amber-100 text-amber-700 text-[11px] font-bold px-2 py-0.5 rounded-full">
                {pendingEscalations.length}
              </span>
            )}
          </h3>
          <button
            onClick={() => onNavigate('dgm-escalations')}
            className="text-xs text-blue-600 hover:underline flex items-center cursor-pointer"
          >
            Review All <ChevronRight className="w-3.5 h-3.5 ml-0.5" />
          </button>
        </div>

        {pendingEscalations.length === 0 ? (
          <div className="text-center py-8">
            <CheckCircle2 className="w-10 h-10 text-green-400 mx-auto mb-2" />
            <p className="text-sm text-slate-500">No pending escalations</p>
            <p className="text-xs text-slate-400 mt-1">All escalations are up to date</p>
          </div>
        ) : (
          <div className="space-y-2">
            {pendingEscalations.slice(0, 5).map((esc) => (
              <div
                key={esc.id}
                className="flex items-center justify-between p-3 rounded-lg border border-amber-100 bg-amber-50/40 hover:bg-amber-50 transition-colors cursor-pointer"
                onClick={() => onNavigate('dgm-escalations')}
              >
                <div>
                  <p className="text-xs font-semibold text-slate-800">
                    {esc.complaintNumber} — {esc.escalatedByStudentName}
                  </p>
                  <p className="text-[11px] text-slate-500 mt-0.5 truncate max-w-xs">{esc.reason}</p>
                </div>
                <div className="flex items-center space-x-2">
                  <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                    esc.status === 'PENDING'
                      ? 'bg-amber-200 text-amber-800'
                      : 'bg-blue-200 text-blue-800'
                  }`}>
                    {esc.status.replace('_', ' ')}
                  </span>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </div>
              </div>
            ))}
            {pendingEscalations.length > 5 && (
              <button
                onClick={() => onNavigate('dgm-escalations')}
                className="w-full text-xs text-blue-600 hover:underline py-2 cursor-pointer"
              >
                + {pendingEscalations.length - 5} more escalations
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
