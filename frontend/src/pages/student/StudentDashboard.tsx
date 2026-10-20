import React, { useEffect, useState } from 'react';
import { Home, AlertCircle, CheckCircle, ArrowRight, PlusCircle, Users } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { apiService } from '../../services/api';
import { Complaint, Room } from '../../types';
import { useAuth } from '../../context/AuthContext';

interface StudentDashboardProps {
  onNavigate: (tab: string, complaintId?: string) => void;
}

export const StudentDashboard: React.FC<StudentDashboardProps> = ({ onNavigate }) => {
  const { user } = useAuth();
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [room, setRoom] = useState<Room | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setIsLoading(true);
        const studentId = user?.studentId || user?.id || 'STU-2023-089';
        const [complaintsRes, studentProfile] = await Promise.all([
          apiService.getComplaints(studentId),
          apiService.getStudentProfile(studentId).catch(() => null),
        ]);
        setComplaints(complaintsRes);
        if (studentProfile?.roomId) {
          const roomRes = await apiService.getRoomById(studentProfile.roomId);
          setRoom(roomRes);
        }
      } catch (err) {
        console.error('Failed to fetch dashboard data', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [user]);

  const openComplaintsCount = complaints.filter(
    (c) => c.status !== 'Resolved' && c.status !== 'Rejected'
  ).length;
  const resolvedComplaintsCount = complaints.filter((c) => c.status === 'Resolved').length;

  if (isLoading) {
    return (
      <div className="p-8 text-center text-slate-400">
        <div className="animate-spin w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full mx-auto mb-2" />
        Loading your dashboard...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-700 rounded-2xl p-6 text-white shadow-sm flex flex-col sm:flex-row justify-between sm:items-center gap-4">
        <div>
          <span className="text-blue-100 text-xs font-semibold tracking-wider uppercase">Student Resident Portal</span>
          <h2 className="text-2xl font-bold mt-0.5">Good Morning, {user?.name.split(' ')[0]}</h2>
          <p className="text-blue-100 text-sm mt-1">
            Room {room?.roomNumber || '204'} • {room?.block || 'Block A'}
          </p>
        </div>
        <div className="flex flex-wrap gap-2.5">
          <Button
            variant="secondary"
            size="sm"
            icon={<PlusCircle className="w-4 h-4" />}
            onClick={() => onNavigate('student-complaint-new')}
            className="bg-white text-blue-700 hover:bg-blue-50 border-0"
          >
            Submit Complaint
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => onNavigate('student-room')}
            className="text-white border-white/30 hover:bg-white/10"
          >
            View Room Details
          </Button>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="!p-4">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-blue-50 text-blue-600">
              <Home className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium">Room</p>
              <h3 className="text-xl font-bold text-slate-800">{room?.roomNumber || '204'}</h3>
            </div>
          </div>
        </Card>

        <Card className="!p-4">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium">Room Status</p>
              <h3 className="text-base font-bold text-slate-800">Occupied</h3>
            </div>
          </div>
        </Card>

        <Card className="!p-4">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-amber-50 text-amber-600">
              <AlertCircle className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium">Open Complaints</p>
              <h3 className="text-xl font-bold text-slate-800">{openComplaintsCount}</h3>
            </div>
          </div>
        </Card>

        <Card className="!p-4">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-purple-50 text-purple-600">
              <CheckCircle className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium">Resolved</p>
              <h3 className="text-xl font-bold text-slate-800">{resolvedComplaintsCount}</h3>
            </div>
          </div>
        </Card>
      </div>

      {/* Grid: Room Summary & Recent Complaints */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Room Info card */}
        <Card
          title="Room Information"
          subtitle="Block details and roommate status"
          headerAction={
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onNavigate('student-room')}
              className="text-xs"
            >
              Full details
            </Button>
          }
        >
          {room && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-3 bg-slate-50 rounded-lg">
                  <span className="text-slate-400 block">Block</span>
                  <span className="font-semibold text-slate-800">{room.block}</span>
                </div>
                <div className="p-3 bg-slate-50 rounded-lg">
                  <span className="text-slate-400 block">Floor</span>
                  <span className="font-semibold text-slate-800">{room.floor}nd Floor</span>
                </div>
                <div className="p-3 bg-slate-50 rounded-lg">
                  <span className="text-slate-400 block">Capacity</span>
                  <span className="font-semibold text-slate-800">{room.capacity} beds</span>
                </div>
                <div className="p-3 bg-slate-50 rounded-lg">
                  <span className="text-slate-400 block">Occupancy</span>
                  <span className="font-semibold text-blue-600">{room.occupied} / {room.capacity} occupied</span>
                </div>
              </div>

              <div>
                <h4 className="text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                  Assigned Roommates
                </h4>
                <div className="space-y-2">
                  {room.occupants.map((occ) => (
                    <div
                      key={occ.studentId}
                      className="flex items-center justify-between p-2 rounded-lg border border-slate-100 text-xs"
                    >
                      <div>
                        <p className="font-semibold text-slate-800">{occ.name}</p>
                        <p className="text-slate-400 text-[11px]">{occ.course}</p>
                      </div>
                      <span className="text-[11px] text-slate-500 font-mono">{occ.phone}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </Card>

        {/* Recent Complaints Card */}
        <div className="lg:col-span-2">
          <Card
            title="Recent Complaints"
            subtitle="Track progress of submitted maintenance issues"
            headerAction={
              <Button
                variant="ghost"
                size="sm"
                icon={<ArrowRight className="w-4 h-4" />}
                onClick={() => onNavigate('student-complaints')}
              >
                View All
              </Button>
            }
          >
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-xs font-semibold text-slate-500">
                    <th className="pb-3">ID</th>
                    <th className="pb-3">Type</th>
                    <th className="pb-3">Subject</th>
                    <th className="pb-3">Date</th>
                    <th className="pb-3">Status</th>
                    <th className="pb-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50 text-xs">
                  {complaints.slice(0, 4).map((c) => (
                    <tr key={c.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-3 font-mono font-medium text-slate-600">{c.complaintNumber}</td>
                      <td className="py-3 text-slate-700 font-medium">{c.type}</td>
                      <td className="py-3 text-slate-800 max-w-xs truncate">{c.subject}</td>
                      <td className="py-3 text-slate-400">
                        {new Date(c.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' })}
                      </td>
                      <td className="py-3">
                        <StatusBadge status={c.status} size="sm" />
                      </td>
                      <td className="py-3 text-right">
                        <button
                          onClick={() => onNavigate('student-complaint-detail', c.id)}
                          className="text-blue-600 hover:underline font-medium text-xs cursor-pointer"
                        >
                          Details
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
    </div>
  );
};
