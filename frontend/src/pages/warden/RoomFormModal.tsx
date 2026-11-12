import React, { useState, useEffect } from 'react';
import { Modal } from '../../components/common/Modal';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Room, Hostel } from '../../types';
import { apiService } from '../../services/api';

interface RoomFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (roomData: any) => Promise<void>;
  roomToEdit?: Room | null;
}

export const RoomFormModal: React.FC<RoomFormModalProps> = ({
  isOpen,
  onClose,
  onSave,
  roomToEdit,
}) => {
  const [roomNumber, setRoomNumber] = useState('');
  const [block, setBlock] = useState('Block A');
  const [floor, setFloor] = useState(1);
  const [capacity, setCapacity] = useState(2);
  const [hostelId, setHostelId] = useState<number>(1);
  const [hostels, setHostels] = useState<Hostel[]>([]);
  const [coolingEquipment, setCoolingEquipment] = useState<'NONE' | 'COOLER' | 'AC'>('COOLER');
  const [coolingMaintenanceStatus, setCoolingMaintenanceStatus] = useState<
    'OPERATIONAL' | 'MAINTENANCE_REQUIRED' | 'REPLACEMENT_NEEDED'
  >('OPERATIONAL');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiService.getHostels().then((h) => {
      setHostels(h);
      if (h.length > 0 && !roomToEdit) {
        setHostelId(h[0].id);
      }
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (roomToEdit) {
      setRoomNumber(roomToEdit.roomNumber);
      setBlock(roomToEdit.block || 'Block A');
      setFloor(roomToEdit.floor || 1);
      setCapacity(roomToEdit.capacity || 2);
      setHostelId(roomToEdit.hostelId ? Number(roomToEdit.hostelId) : 1);
      setCoolingEquipment(roomToEdit.coolingEquipment || 'NONE');
      setCoolingMaintenanceStatus(roomToEdit.coolingMaintenanceStatus || 'OPERATIONAL');
    } else {
      setRoomNumber('');
      setBlock('Block A');
      setFloor(1);
      setCapacity(2);
      setCoolingEquipment('COOLER');
      setCoolingMaintenanceStatus('OPERATIONAL');
    }
    setError(null);
  }, [roomToEdit, isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!roomNumber.trim()) {
      setError('Room number is required');
      return;
    }

    if (capacity <= 0) {
      setError('Capacity must be a positive number');
      return;
    }

    setIsSubmitting(true);
    try {
      if (roomToEdit) {
        await onSave({
          id: roomToEdit.id,
          capacity,
          coolingEquipment,
          coolingMaintenanceStatus,
        });
      } else {
        await onSave({
          roomNumber: roomNumber.trim(),
          block,
          floor: Number(floor),
          capacity: Number(capacity),
          hostelId: Number(hostelId),
          coolingEquipment,
        });
      }
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to save room details');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={roomToEdit ? `Edit Room ${roomToEdit.roomNumber}` : 'Add New Hostel Room'}
      size="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 text-red-700 text-xs rounded-lg border border-red-200">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {!roomToEdit ? (
            <>
              <Input
                label="Room Number"
                value={roomNumber}
                onChange={(e) => setRoomNumber(e.target.value)}
                placeholder="e.g. 101, 204"
                required
              />

              <Select
                label="Hostel"
                value={String(hostelId)}
                onChange={(e) => setHostelId(Number(e.target.value))}
                options={hostels.map((h) => ({
                  value: String(h.id),
                  label: h.name,
                }))}
              />

              <Input
                label="Block / Wing"
                value={block}
                onChange={(e) => setBlock(e.target.value)}
                placeholder="e.g. Block A, East Wing"
                required
              />

              <Input
                label="Floor Number"
                type="number"
                min="0"
                max="20"
                value={String(floor)}
                onChange={(e) => setFloor(Number(e.target.value))}
                required
              />
            </>
          ) : (
            <div className="sm:col-span-2 p-3 bg-slate-50 rounded-lg text-xs text-slate-600">
              <p>
                <span className="font-semibold text-slate-800">Room:</span> {roomToEdit.roomNumber} ({roomToEdit.block}, Floor {roomToEdit.floor})
              </p>
              <p className="text-slate-500 mt-1">
                Current Occupancy: <span className="font-semibold text-slate-700">{roomToEdit.occupied} beds</span>
              </p>
            </div>
          )}

          <Select
            label="Student Capacity"
            value={String(capacity)}
            onChange={(e) => setCapacity(Number(e.target.value))}
            options={[
              { value: '1', label: '1 Bed (Single Occupancy)' },
              { value: '2', label: '2 Beds (Double Sharing)' },
              { value: '3', label: '3 Beds (Triple Sharing)' },
              { value: '4', label: '4 Beds (Quad Sharing)' },
              { value: '6', label: '6 Beds (Dormitory)' },
            ]}
          />

          <Select
            label="Room Cooling Equipment"
            value={coolingEquipment}
            onChange={(e) => setCoolingEquipment(e.target.value as any)}
            options={[
              { value: 'NONE', label: 'None (Standard Ceiling Fan Only)' },
              { value: 'COOLER', label: 'One Room Cooler' },
              { value: 'AC', label: 'One Air Conditioner (AC)' },
            ]}
          />

          {roomToEdit && coolingEquipment !== 'NONE' && (
            <div className="sm:col-span-2">
              <Select
                label="Cooling Maintenance Condition"
                value={coolingMaintenanceStatus}
                onChange={(e) => setCoolingMaintenanceStatus(e.target.value as any)}
                options={[
                  { value: 'OPERATIONAL', label: 'Operational & Functioning Properly' },
                  { value: 'MAINTENANCE_REQUIRED', label: 'Maintenance / Servicing Required' },
                  { value: 'REPLACEMENT_NEEDED', label: 'Defective / Replacement Needed' },
                ]}
              />
            </div>
          )}
        </div>

        <div className="p-3 bg-blue-50/70 border border-blue-100 rounded-lg text-xs text-blue-800 space-y-1">
          <p className="font-semibold">Furniture Baseline Formula:</p>
          <p className="text-[11px] text-blue-700">
            Every room automatically requires 1 bed, 1 study table, and 1 locker per student slot. For capacity {capacity}, the system tracks baseline requirements of {capacity} beds, {capacity} study tables, and {capacity} lockers.
          </p>
        </div>

        <div className="pt-3 border-t border-slate-100 flex justify-end space-x-2">
          <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" isLoading={isSubmitting}>
            {roomToEdit ? 'Save Changes' : 'Create Room'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
