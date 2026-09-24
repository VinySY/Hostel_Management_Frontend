import React, { useEffect, useState } from 'react';
import { Search, SlidersHorizontal, CheckSquare } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { EmptyState } from '../../components/common/EmptyState';
import { WardenComplaintDetailModal } from './WardenComplaintDetailModal';
import { apiService } from '../../services/api';
import { Complaint, ComplaintPriority, ComplaintStatus } from '../../types';
import { useToast } from '../../context/ToastContext';

interface WardenComplaintsProps {
  initialComplaintId?: string | null;
}

export const WardenComplaints: React.FC<WardenComplaintsProps> = ({ initialComplaintId }) => {
  const { showToast } = useToast();
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchComplaints = async () => {
    try {
      setIsLoading(true);
      const data = await apiService.getComplaints();
      setComplaints(data);
      if (initialComplaintId) {
        const found = data.find((c) => c.id === initialComplaintId || c.complaintNumber === initialComplaintId);
        if (found) setSelectedComplaint(found);
      }
    } catch {
      showToast('Error loading complaints queue', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchComplaints();
  }, [initialComplaintId]);

  const handleUpdateComplaint = async (
    id: string,
    status: ComplaintStatus,
    adminNotes: string,
    priority: ComplaintPriority
  ) => {
    try {
      const updated = await apiService.updateComplaintStatus(id, status, adminNotes, priority);
      showToast(`Complaint #${updated.complaintNumber} updated to ${status}`, 'success');
      fetchComplaints();
    } catch {
      showToast('Could not update complaint ticket', 'error');
    }
  };

  const filtered = complaints.filter((c) => {
    const matchesSearch =
      c.complaintNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.studentName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.subject.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.location.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || c.status === statusFilter;
    const matchesPriority = priorityFilter === 'ALL' || c.priority === priorityFilter;
    const matchesCategory = categoryFilter === 'ALL' || c.type === categoryFilter;
    return matchesSearch && matchesStatus && matchesPriority && matchesCategory;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Hostel Complaints & Maintenance Desk</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Review student tickets, assign maintenance teams, track progress and resolve issues
          </p>
        </div>
      </div>

      {/* Multi-filter Toolbar */}
      <Card className="!p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
            <input
              type="text"
              placeholder="Search ID, student, room, subject..."
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
            <option value="Submitted">Submitted (New)</option>
            <option value="Under Review">Under Review</option>
            <option value="In Progress">In Progress</option>
            <option value="Resolved">Resolved</option>
            <option value="Rejected">Rejected</option>
          </select>

          <select
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value)}
            className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="ALL">All Priorities</option>
            <option value="Urgent">Urgent</option>
            <option value="High">High</option>
            <option value="Medium">Medium</option>
            <option value="Low">Low</option>
          </select>

          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="ALL">All Categories</option>
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
        <div className="p-12 text-center text-slate-400">Loading complaints queue...</div>
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No complaints match filters"
          description="Try resetting search keywords or status/priority filters."
          icon={<SlidersHorizontal className="w-6 h-6 text-slate-400" />}
        />
      ) : (
        <Card className="!p-0 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Ticket</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4">Student & Room</th>
                  <th className="py-3 px-4">Subject & Location</th>
                  <th className="py-3 px-4">Priority</th>
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
                    <td className="py-3.5 px-4">
                      <p className="font-semibold text-slate-900">{c.studentName}</p>
                      <p className="text-[11px] text-slate-400">
                        {c.block}, Rm {c.roomNumber}
                      </p>
                    </td>
                    <td className="py-3.5 px-4 max-w-xs">
                      <p className="font-medium text-slate-900 truncate">{c.subject}</p>
                      <p className="text-[11px] text-slate-400 truncate">{c.location}</p>
                    </td>
                    <td className="py-3.5 px-4">
                      <StatusBadge status={c.priority} size="sm" />
                    </td>
                    <td className="py-3.5 px-4">
                      <StatusBadge status={c.status} size="sm" />
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        icon={<CheckSquare className="w-3.5 h-3.5" />}
                        onClick={() => setSelectedComplaint(c)}
                      >
                        Triage / Resolve
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <WardenComplaintDetailModal
        isOpen={!!selectedComplaint}
        onClose={() => setSelectedComplaint(null)}
        complaint={selectedComplaint}
        onUpdate={handleUpdateComplaint}
      />
    </div>
  );
};
