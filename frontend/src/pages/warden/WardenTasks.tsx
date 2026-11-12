import React, { useState, useEffect } from 'react';
import { CheckSquare, Clock, CheckCircle2, AlertTriangle, ArrowRight, FileText, Send } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { EmptyState } from '../../components/common/EmptyState';
import { Modal } from '../../components/common/Modal';
import { apiService } from '../../services/api';
import { FeedbackTask } from '../../types';

export const WardenTasks: React.FC = () => {
  const [tasks, setTasks] = useState<FeedbackTask[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedTask, setSelectedTask] = useState<FeedbackTask | null>(null);
  const [newStatus, setNewStatus] = useState<string>('IN_PROGRESS');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);

  const fetchTasks = async () => {
    setIsLoading(true);
    try {
      const data = await apiService.getFeedbackTasks();
      setTasks(data);
    } catch (err) {
      console.error('Failed to load feedback tasks:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTasks();
  }, []);

  const handleUpdateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTask) return;

    setIsUpdating(true);
    try {
      await apiService.updateFeedbackTask(selectedTask.id, {
        status: newStatus,
        resolutionNotes: resolutionNotes.trim() || undefined,
      });
      setSelectedTask(null);
      setResolutionNotes('');
      await fetchTasks();
    } catch (err) {
      console.error('Failed to update task:', err);
    } finally {
      setIsUpdating(false);
    }
  };

  const filteredTasks = tasks.filter((t) => {
    if (statusFilter === 'ALL') return true;
    return t.status === statusFilter;
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200 flex items-center gap-1">
            <Clock className="w-3 h-3" /> PENDING
          </span>
        );
      case 'IN_PROGRESS':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200 flex items-center gap-1">
            <Clock className="w-3 h-3 text-blue-500 animate-spin" /> IN PROGRESS
          </span>
        );
      case 'RESOLVED':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1">
            <CheckCircle2 className="w-3 h-3" /> RESOLVED
          </span>
        );
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700">{status}</span>;
    }
  };

  const getPriorityBadge = (priority: string) => {
    switch (priority) {
      case 'HIGH':
        return <span className="text-[10px] font-bold text-red-600 bg-red-50 border border-red-200 px-1.5 py-0.5 rounded">HIGH</span>;
      case 'MEDIUM':
        return <span className="text-[10px] font-medium text-amber-700 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded">MEDIUM</span>;
      default:
        return <span className="text-[10px] font-medium text-slate-600 bg-slate-100 px-1.5 py-0.5 rounded">LOW</span>;
    }
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Feedback Action Tasks</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Action items forwarded by the Director General of Hostels from student feedback surveys
          </p>
        </div>

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 font-medium text-slate-700"
        >
          <option value="ALL">All Task Statuses</option>
          <option value="PENDING">Pending Attention</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="RESOLVED">Resolved</option>
        </select>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-slate-400">Loading feedback action items...</div>
      ) : filteredTasks.length === 0 ? (
        <EmptyState
          title="No action tasks"
          description="There are currently no forwarded feedback tasks matching the selected filter."
        />
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {filteredTasks.map((task) => (
            <Card
              key={task.id}
              className="!p-5 hover:border-blue-200 transition-all shadow-xs"
            >
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 mb-2">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xs font-mono font-semibold text-slate-400">TASK-#{task.id}</span>
                    <span className="text-[11px] font-semibold text-slate-600 uppercase tracking-wide px-1.5 py-0.5 bg-slate-100 rounded">
                      {task.category}
                    </span>
                    {getPriorityBadge(task.priority)}
                  </div>
                  <h3 className="text-sm font-bold text-slate-900">{task.title}</h3>
                </div>
                <div className="flex items-center gap-2 self-start">
                  {getStatusBadge(task.status)}
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setSelectedTask(task);
                      setNewStatus(task.status);
                      setResolutionNotes(task.resolutionNotes || '');
                    }}
                    className="text-xs"
                  >
                    Update Status
                  </Button>
                </div>
              </div>

              <div className="p-3 bg-slate-50 border border-slate-100 rounded-lg text-xs text-slate-700 mb-3 whitespace-pre-line">
                {task.description}
              </div>

              {task.resolutionNotes && (
                <div className="p-3 bg-emerald-50/70 border border-emerald-100 rounded-lg text-xs text-emerald-900 mb-3">
                  <span className="font-bold block text-[11px] text-emerald-800 uppercase tracking-wider mb-0.5">
                    Warden Resolution Notes:
                  </span>
                  {task.resolutionNotes}
                </div>
              )}

              <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-100 text-[11px] text-slate-400">
                <span>Forwarded by: <strong className="text-slate-600">{task.createdBy}</strong></span>
                <span>Assigned to: <strong className="text-slate-600">{task.wardenName}</strong> ({task.hostelName})</span>
                <span>Updated: {new Date(task.updatedAt).toLocaleDateString()}</span>
              </div>
            </Card>
          ))}
        </div>
      )}

      {selectedTask && (
        <Modal
          isOpen={!!selectedTask}
          onClose={() => setSelectedTask(null)}
          title={`Update Task #${selectedTask.id}`}
          size="md"
        >
          <form onSubmit={handleUpdateTask} className="space-y-4">
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-700">
              <strong className="block text-slate-900 mb-1">{selectedTask.title}</strong>
              <p>{selectedTask.description}</p>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Task Lifecycle Status</label>
              <select
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 font-medium text-slate-800"
              >
                <option value="PENDING">PENDING - Awaiting Action</option>
                <option value="IN_PROGRESS">IN_PROGRESS - Work Underway</option>
                <option value="RESOLVED">RESOLVED - Action Complete</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Resolution Notes / Action Taken
              </label>
              <textarea
                value={resolutionNotes}
                onChange={(e) => setResolutionNotes(e.target.value)}
                rows={3}
                placeholder="Explain what steps or contractor repairs were executed to resolve this issue..."
                className="w-full p-2.5 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
              <Button type="button" variant="outline" onClick={() => setSelectedTask(null)}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={isUpdating}>
                Save Task Progress
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};
