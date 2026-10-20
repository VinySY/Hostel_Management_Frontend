import React, { useEffect, useState } from 'react';
import { PlusCircle, Search, Eye } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { EmptyState } from '../../components/common/EmptyState';
import { apiService } from '../../services/api';
import { Complaint } from '../../types';
import { useAuth } from '../../context/AuthContext';

interface StudentComplaintsProps {
  onNavigateNew: () => void;
  onViewDetail: (complaintId: string) => void;
}

export const StudentComplaints: React.FC<StudentComplaintsProps> = ({
  onNavigateNew,
  onViewDetail,
}) => {
  const { user } = useAuth();
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    apiService.getComplaints(user?.studentId || 'STU-2023-089').then((res) => {
      setComplaints(res);
      setIsLoading(false);
    });
  }, [user]);

  const filtered = complaints.filter((c) => {
    const matchesSearch =
      c.complaintNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.subject.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.location.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || c.status === statusFilter;
    const matchesType = typeFilter === 'ALL' || c.type === typeFilter;
    return matchesSearch && matchesStatus && matchesType;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">My Complaints</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Submit, track, and review status updates for your maintenance requests
          </p>
        </div>
        <Button
          variant="primary"
          icon={<PlusCircle className="w-4 h-4" />}
          onClick={onNavigateNew}
        >
          Submit a Complaint
        </Button>
      </div>

      {/* Filter and Search Bar */}
      <Card className="!p-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
            <input
              type="text"
              placeholder="Search by ID, subject or location..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="Submitted">Submitted</option>
            <option value="Under Review">Under Review</option>
            <option value="In Progress">In Progress</option>
            <option value="Resolved">Resolved</option>
            <option value="Rejected">Rejected</option>
          </select>

          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="ALL">All Complaint Types</option>
            <option value="Mess">Mess</option>
            <option value="Cleaning">Cleaning</option>
            <option value="Room Maintenance">Room Maintenance</option>
            <option value="Electricity">Electricity</option>
            <option value="Water">Water</option>
            <option value="Plumbing">Plumbing</option>
            <option value="Other">Other</option>
          </select>
        </div>
      </Card>

      {/* Complaints Table */}
      {isLoading ? (
        <div className="p-12 text-center text-slate-400">Loading complaints history...</div>
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No complaints found"
          description="You currently have no complaints matching the selected filters or search terms."
          actionText="Submit New Complaint"
          onAction={onNavigateNew}
        />
      ) : (
        <Card className="!p-0 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Complaint ID</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Subject</th>
                  <th className="py-3 px-4">Location</th>
                  <th className="py-3 px-4">Submitted Date</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="py-3.5 px-4 font-mono font-semibold text-slate-700">
                      {c.complaintNumber}
                    </td>
                    <td className="py-3.5 px-4 text-slate-800 font-medium">{c.type}</td>
                    <td className="py-3.5 px-4 max-w-xs truncate font-medium text-slate-900">
                      {c.subject}
                    </td>
                    <td className="py-3.5 px-4 text-slate-500">{c.location}</td>
                    <td className="py-3.5 px-4 text-slate-400">
                      {new Date(c.createdAt).toLocaleDateString('en-GB', {
                        day: '2-digit',
                        month: 'short',
                        year: 'numeric',
                      })}
                    </td>
                    <td className="py-3.5 px-4">
                      <StatusBadge status={c.status} size="sm" />
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        icon={<Eye className="w-3.5 h-3.5" />}
                        onClick={() => onViewDetail(c.id)}
                      >
                        View
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
};
