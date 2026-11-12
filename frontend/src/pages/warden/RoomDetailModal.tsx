import React, { useState } from 'react';
import { Mail, Phone, Award, Users, Wind, Layers, Wrench, Edit, AlertTriangle } from 'lucide-react';
import { Modal } from '../../components/common/Modal';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Button } from '../../components/common/Button';
import { Room } from '../../types';
import { FurnitureModal } from './FurnitureModal';
import { RoomFormModal } from './RoomFormModal';
import { apiService } from '../../services/api';

interface RoomDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  room: Room | null;
  onRoomUpdated: () => void;
}

export const RoomDetailModal: React.FC<RoomDetailModalProps> = ({
  isOpen,
  onClose,
  room,
  onRoomUpdated,
}) => {
  const [isFurnitureModalOpen, setIsFurnitureModalOpen] = useState(false);
  const [isEditRoomOpen, setIsEditRoomOpen] = useState(false);

  if (!room) return null;

  const coolingLabel =
    room.coolingEquipment === 'AC'
      ? 'One Air Conditioner (AC)'
      : room.coolingEquipment === 'COOLER'
      ? 'One Room Cooler'
      : 'None (Standard Ceiling Fan)';

  const coolingStatusBadge = () => {
    if (room.coolingEquipment === 'NONE') return null;
    if (room.coolingMaintenanceStatus === 'OPERATIONAL') {
      return (
        <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
          Operational
        </span>
      );
    }
    if (room.coolingMaintenanceStatus === 'MAINTENANCE_REQUIRED') {
      return (
        <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-50 text-amber-700 border border-amber-200">
          Maintenance Required
        </span>
      );
    }
    return (
      <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-red-50 text-red-700 border border-red-200">
        Replacement Needed
      </span>
    );
  };

  const handleSaveEdit = async (data: any) => {
    await apiService.updateRoom(room.id, data);
    onRoomUpdated();
  };

  return (
    <>
      <Modal isOpen={isOpen} onClose={onClose} title={`Room ${room.roomNumber} Overview`} size="lg">
        <div className="space-y-5">
          {/* Header */}
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div>
              <h4 className="font-bold text-slate-900 text-lg">
                Room {room.roomNumber} — {room.block}
              </h4>
              <p className="text-xs text-slate-400">
                Floor {room.floor} • Capacity: {room.capacity} beds • {room.occupied} occupied
              </p>
            </div>
            <div className="flex items-center space-x-2">
              <StatusBadge status={room.status} />
              <Button
                variant="outline"
                size="sm"
                icon={<Edit className="w-3.5 h-3.5" />}
                onClick={() => setIsEditRoomOpen(true)}
              >
                Edit Room
              </Button>
            </div>
          </div>

          {/* Occupancy bar */}
          <div>
            <div className="flex justify-between text-xs font-medium text-slate-600 mb-1.5">
              <span>Bed Occupancy</span>
              <span className="font-bold text-slate-900">
                {room.occupied} of {room.capacity} beds taken ({Math.round((room.occupied / room.capacity) * 100)}%)
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

          {/* Cooling Equipment Section */}
          <div className="p-3.5 rounded-xl border border-slate-200 bg-slate-50/60 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-9 h-9 rounded-lg bg-blue-100 text-blue-700 flex items-center justify-center">
                <Wind className="w-5 h-5" />
              </div>
              <div>
                <span className="text-[11px] uppercase tracking-wider text-slate-400 font-semibold block">
                  Cooling Equipment
                </span>
                <span className="text-xs font-bold text-slate-800">{coolingLabel}</span>
              </div>
            </div>
            <div>{coolingStatusBadge()}</div>
          </div>

          {/* Furniture Inventory Tracking */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <h5 className="text-xs font-semibold text-slate-700 uppercase tracking-wider flex items-center">
                <Layers className="w-3.5 h-3.5 mr-1.5 text-blue-600" /> Furniture Inventory (1 Bed, 1 Table, 1 Locker per Bed)
              </h5>
              <Button
                variant="outline"
                size="sm"
                icon={<Wrench className="w-3.5 h-3.5" />}
                onClick={() => setIsFurnitureModalOpen(true)}
              >
                Manage Furniture & Audits
              </Button>
            </div>

            <div className="border border-slate-200 rounded-xl overflow-hidden bg-white text-xs">
              <table className="w-full text-left">
                <thead className="bg-slate-50 text-[11px] font-semibold text-slate-500 uppercase border-b border-slate-200">
                  <tr>
                    <th className="py-2.5 px-3">Item Type</th>
                    <th className="py-2.5 px-3 text-center">Required (N={room.capacity})</th>
                    <th className="py-2.5 px-3 text-center">Serviceable</th>
                    <th className="py-2.5 px-3 text-center">Damaged</th>
                    <th className="py-2.5 px-3 text-center">Missing</th>
                    <th className="py-2.5 px-3 text-center">Repairing</th>
                    <th className="py-2.5 px-3 text-center">Shortage</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {(room.furnitureInventory && room.furnitureInventory.length > 0
                    ? room.furnitureInventory
                    : [
                        { furnitureType: 'BED', requiredCount: room.capacity, serviceableCount: room.capacity, damagedCount: 0, missingCount: 0, underRepairCount: 0, shortage: 0 },
                        { furnitureType: 'STUDY_TABLE', requiredCount: room.capacity, serviceableCount: room.capacity, damagedCount: 0, missingCount: 0, underRepairCount: 0, shortage: 0 },
                        { furnitureType: 'LOCKER', requiredCount: room.capacity, serviceableCount: room.capacity, damagedCount: 0, missingCount: 0, underRepairCount: 0, shortage: 0 },
                      ]
                  ).map((item: any) => {
                    const typeLabel =
                      item.furnitureType === 'BED'
                        ? 'Bed'
                        : item.furnitureType === 'STUDY_TABLE'
                        ? 'Study Table'
                        : 'Locker';
                    const hasShortage = item.shortage > 0;
                    return (
                      <tr key={item.furnitureType} className={hasShortage ? 'bg-amber-50/50' : ''}>
                        <td className="py-2.5 px-3 font-semibold text-slate-800">{typeLabel}</td>
                        <td className="py-2.5 px-3 text-center text-slate-600">{item.requiredCount}</td>
                        <td className="py-2.5 px-3 text-center font-bold text-emerald-600">{item.serviceableCount}</td>
                        <td className="py-2.5 px-3 text-center text-red-600">{item.damagedCount || 0}</td>
                        <td className="py-2.5 px-3 text-center text-slate-400">{item.missingCount || 0}</td>
                        <td className="py-2.5 px-3 text-center text-amber-600">{item.underRepairCount || 0}</td>
                        <td className="py-2.5 px-3 text-center">
                          {hasShortage ? (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-red-100 text-red-700 flex items-center justify-center">
                              <AlertTriangle className="w-3 h-3 mr-0.5" /> -{item.shortage}
                            </span>
                          ) : (
                            <span className="text-slate-400 font-mono">—</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Current Occupants list */}
          <div>
            <h5 className="text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2.5 flex items-center">
              <Users className="w-3.5 h-3.5 mr-1.5 text-blue-600" /> Current Residents
            </h5>

            {room.occupants.length === 0 ? (
              <div className="p-6 text-center text-xs text-slate-400 border border-dashed rounded-lg bg-slate-50">
                This room is currently vacant and ready for new student allocation.
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

      {/* Furniture Modal */}
      {isFurnitureModalOpen && (
        <FurnitureModal
          isOpen={isFurnitureModalOpen}
          onClose={() => setIsFurnitureModalOpen(false)}
          room={room}
          onUpdated={() => {
            onRoomUpdated();
          }}
        />
      )}

      {/* Room Form Modal (Edit mode) */}
      {isEditRoomOpen && (
        <RoomFormModal
          isOpen={isEditRoomOpen}
          onClose={() => setIsEditRoomOpen(false)}
          roomToEdit={room}
          onSave={handleSaveEdit}
        />
      )}
    </>
  );
};
