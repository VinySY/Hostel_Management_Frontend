import React, { useEffect, useState } from 'react';
import {
  Users,
  Building,
  Bed,
  AlertCircle,
  ArrowRight,
  TrendingUp,
  Clock,
} from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { apiService } from '../../services/api';
import { Complaint, DashboardStats } from '../../types';

interface WardenDashboardProps {
  onNavigate: (tab: string, itemData?: unknown) => void;
}

export const WardenDashboard: React.FC<WardenDashboardProps> = ({ onNavigate }) => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentComplaints, setRecentComplaints] = useState<Complaint[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      try {
        const [statsData, complaintsData] = await Promise.all([
          apiService.getDashboardStats(),
          apiService.getComplaints(),
        ]);
        setStats(statsData);
        setRecentComplaints(complaintsData);
      } catch (err) {
        console.error('Failed to load warden dashboard', err);
      } finally {
        setIsLoading(false);
      }
    };
    loadData();
  }, []);

  if (isLoading || !stats) {
    return (
      <div className="p-8 text-center text-slate-400">
        <div className="animate-spin w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full mx-auto mb-2" />
        Loading warden overview...
      </div>
    );
  }

  const pendingUrgentCount = recentComplaints.filter(
    (c) => (c.priority === 'Urgent' || c.priority === 'High') && c.status !== 'Resolved'
  ).length;

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-slate-900 text-white rounded-2xl p-6 shadow-sm flex flex-col md:flex-row justify-between md:items-center gap-4">
        <div>
          <span className="text-blue-400 text-xs font-semibold tracking-wider uppercase">
            Administrative Management Hub
          </span>
          <h2 className="text-2xl font-bold mt-1">Hostel Overview & Operations</h2>
          <p className="text-slate-400 text-xs mt-1">
            Block A & Block B • Campus Residential Facility
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="primary"
            size="sm"
            onClick={() => onNavigate('warden-complaints')}
          >
            Review Pending Complaints ({stats.openComplaints})
          </Button>
          <Button
            variant="outline"
            size="sm"
            className="text-white border-slate-700 hover:bg-slate-800"
            onClick={() => onNavigate('warden-students')}
          >
            Resident Roster
          </Button>
        </div>
      </div>

      {/* Primary KPI Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="!p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-500">Total Registered Students</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{stats.totalStudents}</h3>
            </div>
            <div className="p-3 rounded-xl bg-blue-50 text-blue-600">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 flex items-center text-xs text-slate-400">
            <span className="text-emerald-600 font-semibold flex items-center mr-1">
              <TrendingUp className="w-3.5 h-3.5 mr-0.5" /> 100%
            </span>
            Active enrollment
          </div>
        </Card>

        <Card className="!p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-500">Total Hostel Rooms</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{stats.totalRooms}</h3>
            </div>
            <div className="p-3 rounded-xl bg-indigo-50 text-indigo-600">
              <Building className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 text-xs text-slate-400">
            Across 2 blocks and 4 floors
          </div>
        </Card>

        <Card className="!p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-500">Available Vacant Beds</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{stats.availableBeds}</h3>
            </div>
            <div className="p-3 rounded-xl bg-emerald-50 text-emerald-600">
              <Bed className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 text-xs text-emerald-600 font-medium">
            Ready for instant allocation
          </div>
        </Card>

        <Card className="!p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-500">Active Open Complaints</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{stats.openComplaints}</h3>
            </div>
            <div className="p-3 rounded-xl bg-amber-50 text-amber-600">
              <AlertCircle className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 text-xs text-rose-600 font-medium flex items-center">
            <Clock className="w-3.5 h-3.5 mr-1" />
            {pendingUrgentCount} High/Urgent priority
          </div>
        </Card>
      </div>

      {/* Two Columns: Category Breakdown & Quick Actions / Recent Tickets */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Complaints distribution */}
        <Card title="Complaints by Category" subtitle="Distribution across maintenance teams" className="lg:col-span-1">
          <div className="space-y-3 pt-1">
            {Object.entries(stats.complaintsByType).map(([type, count]) => {
              const total = stats.openComplaints + stats.resolvedComplaints || 1;
              const percent = Math.round((count / total) * 100);
              return (
                <div key={type}>
                  <div className="flex justify-between text-xs font-medium text-slate-700 mb-1">
                    <span>{type}</span>
                    <span className="font-semibold text-slate-900">{count} ({percent}%)</span>
                  </div>
                  <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                    <div
                      className="bg-blue-600 h-2 rounded-full transition-all duration-500"
                      style={{ width: `${percent}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </Card>

        {/* Complaints Requiring Immediate Attention */}
        <Card
          title="Recent Actionable Complaints"
          subtitle="Tickets awaiting triage or status update"
          className="lg:col-span-2"
          headerAction={
            <Button
              variant="ghost"
              size="sm"
              icon={<ArrowRight className="w-4 h-4" />}
              onClick={() => onNavigate('warden-complaints')}
            >
              All Complaints
            </Button>
          }
        >
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-100 text-slate-400 font-semibold uppercase">
                  <th className="pb-3">ID</th>
                  <th className="pb-3">Student & Room</th>
                  <th className="pb-3">Subject</th>
                  <th className="pb-3">Priority</th>
                  <th className="pb-3">Status</th>
                  <th className="pb-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {recentComplaints.slice(0, 5).map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50 transition-colors">
                    <td className="py-3 font-mono font-semibold text-slate-700">{c.complaintNumber}</td>
                    <td className="py-3">
                      <p className="font-medium text-slate-800">{c.studentName}</p>
                      <p className="text-[11px] text-slate-400">
                        {c.block}, Rm {c.roomNumber}
                      </p>
                    </td>
                    <td className="py-3 text-slate-700 max-w-xs truncate">{c.subject}</td>
                    <td className="py-3">
                      <StatusBadge status={c.priority} size="sm" />
                    </td>
                    <td className="py-3">
                      <StatusBadge status={c.status} size="sm" />
                    </td>
                    <td className="py-3 text-right">
                      <button
                        onClick={() => onNavigate('warden-complaints', c.id)}
                        className="text-blue-600 hover:underline font-semibold cursor-pointer"
                      >
                        Manage
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  );
};
