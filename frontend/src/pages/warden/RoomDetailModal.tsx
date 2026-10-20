import React from 'react';
import { Mail, Phone, Award, Users } from 'lucide-react';
import { Modal } from '../../components/common/Modal';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Button } from '../../components/common/Button';
import { Room } from '../../types';

interface RoomDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  room: Room | null;
}

export const RoomDetailModal: React.FC<RoomDetailModalProps> = ({
  isOpen,
  onClose,
  room,
}) => {
  if (!room) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Room ${room.roomNumber} Overview`} size="lg">
      <div className="space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div>
            <h4 className="font-bold text-slate-900 text-lg">
              Room {room.roomNumber} — {room.block}
            </h4>
            <p className="text-xs text-slate-400">
              Floor {room.floor} • Capacity: {room.capacity} beds • {room.occupied} occupied
            </p>
          </div>
          <StatusBadge status={room.status} />
        </div>

        {/* Occupancy bar */}
        <div>
          <div className="flex justify-between text-xs font-medium text-slate-600 mb-1.5">
            <span>Bed Occupancy</span>
            <span className="font-bold text-slate-900">
              {room.occupied} of {room.capacity} beds taken
            </span>
          </div>
          <div className="w-full bg-slate-100 h-2.5 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all ${
                room.occupied === room.capacity
                  ? 'bg-slate-500'
                  : room.occupied > 0
                  ? 'bg-amber-500'
                  : 'bg-emerald-500'
              }`}
              style={{ width: `${(room.occupied / room.capacity) * 100}%` }}
            />
          </div>
        </div>

        {/* Current Occupants list */}
        <div>
          <h5 className="text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2.5 flex items-center">
            <Users className="w-3.5 h-3.5 mr-1.5 text-blue-600" /> Current Residents
          </h5>

          {room.occupants.length === 0 ? (
            <div className="p-6 text-center text-xs text-slate-400 border border-dashed rounded-lg bg-slate-50">
              This room is currently completely vacant and ready for new student allocation.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {room.occupants.map((occ, i) => (
                <div key={occ.studentId} className="p-3.5 rounded-xl border border-slate-200 bg-slate-50/50">
                  <div className="flex items-center space-x-2.5 mb-2">
                    <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-xs">
                      {occ.name.charAt(0)}
                    </div>
                    <div>
                      <p className="font-semibold text-slate-900 text-xs">{occ.name}</p>
                      <p className="text-[10px] text-slate-400 font-mono">Bed #{i + 1} · {occ.studentId}</p>
                    </div>
                  </div>

                  <div className="space-y-1 text-[11px] text-slate-600 pt-2 border-t border-slate-200/50">
                    <div className="flex items-center">
                      <Award className="w-3 h-3 mr-1.5 text-slate-400" />
                      <span>{occ.course} (Yr {occ.year})</span>
                    </div>
                    <div className="flex items-center">
                      <Mail className="w-3 h-3 mr-1.5 text-slate-400" />
                      <span className="truncate">{occ.email}</span>
                    </div>
                    <div className="flex items-center">
                      <Phone className="w-3 h-3 mr-1.5 text-slate-400" />
                      <span>{occ.phone}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="pt-3 border-t border-slate-100 flex justify-end">
          <Button variant="outline" size="sm" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </Modal>
  );
};
