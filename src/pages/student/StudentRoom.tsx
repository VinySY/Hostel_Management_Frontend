import React, { useEffect, useState } from 'react';
import { Mail, Phone, Award, CheckCircle } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { StatusBadge } from '../../components/common/StatusBadge';
import { apiService } from '../../services/api';
import { Room } from '../../types';
import { useAuth } from '../../context/AuthContext';

export const StudentRoom: React.FC = () => {
  const { user } = useAuth();
  const [room, setRoom] = useState<Room | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchRoomDetails = async () => {
      try {
        setIsLoading(true);
        const studentId = user?.studentId || user?.id;
        if (studentId) {
          const student = await apiService.getStudentProfile(studentId);
          if (student?.roomId) {
            const roomData = await apiService.getRoomById(student.roomId);
            setRoom(roomData);
            return;
          }
        }
      } catch (err) {
        console.error('Failed to load room allocation details', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchRoomDetails();
  }, [user]);

  if (isLoading || !room) {
    return (
      <div className="p-8 text-center text-slate-400">
        <div className="animate-spin w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full mx-auto mb-2" />
        Loading room allocation details...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Room Details: {room.roomNumber}</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            {room.block} • Floor {room.floor} • Residential Hostel Complex
          </p>
        </div>
        <StatusBadge status={room.status} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card title="Room Specifications" className="md:col-span-1">
          <div className="space-y-3.5 text-xs">
            <div className="flex justify-between py-2 border-b border-slate-100">
              <span className="text-slate-400">Room Number</span>
              <span className="font-semibold text-slate-800">{room.roomNumber}</span>
            </div>
            <div className="flex justify-between py-2 border-b border-slate-100">
              <span className="text-slate-400">Block Name</span>
              <span className="font-semibold text-slate-800">{room.block}</span>
            </div>
            <div className="flex justify-between py-2 border-b border-slate-100">
              <span className="text-slate-400">Floor Level</span>
              <span className="font-semibold text-slate-800">{room.floor}nd Floor</span>
            </div>
            <div className="flex justify-between py-2 border-b border-slate-100">
              <span className="text-slate-400">Total Capacity</span>
              <span className="font-semibold text-slate-800">{room.capacity} Beds</span>
            </div>
            <div className="flex justify-between py-2 border-b border-slate-100">
              <span className="text-slate-400">Current Occupants</span>
              <span className="font-semibold text-blue-600">{room.occupied} Students</span>
            </div>
            <div className="flex justify-between py-2">
              <span className="text-slate-400">Vacant Beds</span>
              <span className="font-semibold text-emerald-600">{room.capacity - room.occupied} Available</span>
            </div>
          </div>
        </Card>

        <Card title="Roommate Directory" subtitle="Current residents sharing this room" className="md:col-span-2">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {room.occupants.map((occ, idx) => (
              <div key={occ.studentId} className="p-4 rounded-xl border border-slate-200 bg-slate-50/50">
                <div className="flex items-center space-x-3 mb-3">
                  <div className="w-10 h-10 rounded-full bg-blue-100 text-blue-600 font-bold flex items-center justify-center text-sm">
                    {occ.name.charAt(0)}
                  </div>
                  <div>
                    <h4 className="font-semibold text-slate-800 text-sm">{occ.name}</h4>
                    <span className="text-[11px] text-slate-400 font-mono">Bed #{idx + 1} • {occ.studentId}</span>
                  </div>
                </div>

                <div className="space-y-1.5 text-xs text-slate-600 border-t border-slate-200/60 pt-3">
                  <div className="flex items-center">
                    <Award className="w-3.5 h-3.5 mr-2 text-slate-400" />
                    <span>{occ.course} (Year {occ.year})</span>
                  </div>
                  <div className="flex items-center">
                    <Mail className="w-3.5 h-3.5 mr-2 text-slate-400" />
                    <span className="truncate">{occ.email}</span>
                  </div>
                  <div className="flex items-center">
                    <Phone className="w-3.5 h-3.5 mr-2 text-slate-400" />
                    <span>{occ.phone}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      {/* Facilities and Guidelines */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card title="Room Inventory & Amenities">
          <ul className="space-y-2 text-xs text-slate-600">
            <li className="flex items-center"><CheckCircle className="w-4 h-4 text-emerald-500 mr-2" /> Study Desks and Ergonomic Chairs (3 Sets)</li>
            <li className="flex items-center"><CheckCircle className="w-4 h-4 text-emerald-500 mr-2" /> Wooden Wardrobes with Lock Provision (3 Units)</li>
            <li className="flex items-center"><CheckCircle className="w-4 h-4 text-emerald-500 mr-2" /> High-speed Campus Wi-Fi 6 Access Point</li>
            <li className="flex items-center"><CheckCircle className="w-4 h-4 text-emerald-500 mr-2" /> 24/7 Power Backup & Water Supply</li>
          </ul>
        </Card>

        <Card title="Hostel Emergency Contacts">
          <div className="space-y-2.5 text-xs text-slate-700">
            <div className="flex justify-between items-center p-2 rounded bg-slate-50">
              <span className="font-medium">Warden Duty Office (Block A)</span>
              <span className="font-mono text-blue-600 font-semibold">+91 98765 00001</span>
            </div>
            <div className="flex justify-between items-center p-2 rounded bg-slate-50">
              <span className="font-medium">Campus Medical & Ambulance</span>
              <span className="font-mono text-red-600 font-semibold">108 / +91 98765 00108</span>
            </div>
            <div className="flex justify-between items-center p-2 rounded bg-slate-50">
              <span className="font-medium">24x7 Electrician & Maintenance Desk</span>
              <span className="font-mono text-slate-800 font-semibold">+91 98765 00009</span>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
};
