import React, { useState, useEffect } from 'react';
import { Modal } from '../../components/common/Modal';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Room, FurnitureHistoryItem } from '../../types';
import { apiService } from '../../services/api';
import { History, Wrench, ShieldAlert } from 'lucide-react';

interface FurnitureModalProps {
  isOpen: boolean;
  onClose: () => void;
  room: Room;
  onUpdated: () => void;
}

export const FurnitureModal: React.FC<FurnitureModalProps> = ({
  isOpen,
  onClose,
  room,
  onUpdated,
}) => {
  const [activeTab, setActiveTab] = useState<'update' | 'history'>('update');
  const [furnitureType, setFurnitureType] = useState<'BED' | 'STUDY_TABLE' | 'LOCKER'>('BED');
  const [serviceable, setServiceable] = useState(room.capacity);
  const [damaged, setDamaged] = useState(0);
  const [missing, setMissing] = useState(0);
  const [underRepair, setUnderRepair] = useState(0);
  const [actionType, setActionType] = useState('INVENTORY_AUDIT');
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // History state
  const [history, setHistory] = useState<FurnitureHistoryItem[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);

  useEffect(() => {
    if (room.furnitureInventory) {
      const item = room.furnitureInventory.find((f) => f.furnitureType === furnitureType);
      if (item) {
        setServiceable(item.serviceableCount);
        setDamaged(item.damagedCount);
        setMissing(item.missingCount);
        setUnderRepair(item.underRepairCount);
      } else {
        setServiceable(room.capacity);
        setDamaged(0);
        setMissing(0);
        setUnderRepair(0);
      }
    }
  }, [furnitureType, room]);

  const loadHistory = async () => {
    setIsLoadingHistory(true);
    try {
      const data = await apiService.getFurnitureHistory(room.id);
      setHistory(data);
    } catch {
      setHistory([]);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'history') {
      loadHistory();
    }
  }, [activeTab]);

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);
    try {
      await apiService.updateFurniture(room.id, {
        furnitureType,
        serviceableCount: Number(serviceable),
        damagedCount: Number(damaged),
        missingCount: Number(missing),
        underRepairCount: Number(underRepair),
        actionType,
        notes,
      });
      onUpdated();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to update furniture inventory');
    } finally {
      setIsSubmitting(false);
    }
  };

  const currentShortage = Math.max(0, room.capacity - serviceable);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Furniture Inventory & Maintenance — Room ${room.roomNumber}`}
      size="lg"
    >
      <div className="space-y-4">
        {/* Tab switch */}
        <div className="flex border-b border-slate-200">
          <button
            type="button"
            onClick={() => setActiveTab('update')}
            className={`px-4 py-2 text-xs font-semibold border-b-2 -mb-px transition-colors flex items-center space-x-1.5 ${
              activeTab === 'update'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Wrench className="w-3.5 h-3.5" />
            <span>Update State & Report Damage</span>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('history')}
            className={`px-4 py-2 text-xs font-semibold border-b-2 -mb-px transition-colors flex items-center space-x-1.5 ${
              activeTab === 'history'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <History className="w-3.5 h-3.5" />
            <span>Maintenance & Replacement History</span>
          </button>
        </div>

        {activeTab === 'update' ? (
          <form onSubmit={handleUpdate} className="space-y-4 pt-1">
            {error && (
              <div className="p-3 bg-red-50 text-red-700 text-xs rounded-lg border border-red-200">
                {error}
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Select
                label="Furniture Item Category"
                value={furnitureType}
                onChange={(e) => setFurnitureType(e.target.value as any)}
                options={[
                  { value: 'BED', label: `Bed (Baseline Required: ${room.capacity})` },
                  { value: 'STUDY_TABLE', label: `Study Table (Baseline Required: ${room.capacity})` },
                  { value: 'LOCKER', label: `Locker (Baseline Required: ${room.capacity})` },
                ]}
              />

              <Select
                label="Action / Audit Reason"
                value={actionType}
                onChange={(e) => setActionType(e.target.value)}
                options={[
                  { value: 'INVENTORY_AUDIT', label: 'Periodic Physical Audit' },
                  { value: 'DAMAGED_REPORTED', label: 'Report Damaged Item' },
                  { value: 'SENT_FOR_REPAIR', label: 'Sent Item for Repair' },
                  { value: 'REPAIRED', label: 'Item Repaired & Restored' },
                  { value: 'REPLACED_NEW', label: 'Replaced with New Item' },
                  { value: 'MISSING_REPORTED', label: 'Report Missing Item' },
                ]}
              />

              <Input
                label="Serviceable (In Good Working Condition)"
                type="number"
                min="0"
                value={String(serviceable)}
                onChange={(e) => setServiceable(Number(e.target.value))}
                required
              />

              <Input
                label="Damaged Count"
                type="number"
                min="0"
                value={String(damaged)}
                onChange={(e) => setDamaged(Number(e.target.value))}
                required
              />

              <Input
                label="Missing Count"
                type="number"
                min="0"
                value={String(missing)}
                onChange={(e) => setMissing(Number(e.target.value))}
                required
              />

              <Input
                label="Under Repair / Maintenance"
                type="number"
                min="0"
                value={String(underRepair)}
                onChange={(e) => setUnderRepair(Number(e.target.value))}
                required
              />
            </div>

            {currentShortage > 0 && (
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg flex items-center text-xs text-amber-800">
                <ShieldAlert className="w-4 h-4 text-amber-600 mr-2 flex-shrink-0" />
                <span>
                  <strong>Shortage Alert:</strong> Room capacity is {room.capacity}, but only {serviceable} {furnitureType.toLowerCase()}s are serviceable. Discrepancy: <strong>{currentShortage} unit(s) short</strong>.
                </span>
              </div>
            )}

            <Input
              label="Audit / Maintenance Notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="e.g. Study table leg replaced; bed frame repainted"
            />

            <div className="pt-3 border-t border-slate-100 flex justify-end space-x-2">
              <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={isSubmitting}>
                Save Inventory State
              </Button>
            </div>
          </form>
        ) : (
          <div className="space-y-3 pt-1">
            {isLoadingHistory ? (
              <div className="p-8 text-center text-slate-400 text-xs">Loading history trail...</div>
            ) : history.length === 0 ? (
              <div className="p-8 text-center text-slate-400 text-xs border border-dashed rounded-lg bg-slate-50">
                No past maintenance or replacement events recorded for Room {room.roomNumber}.
              </div>
            ) : (
              <div className="max-h-72 overflow-y-auto space-y-2 pr-1">
                {history.map((item) => (
                  <div key={item.id} className="p-3 border border-slate-200 rounded-lg bg-slate-50 text-xs flex justify-between items-start">
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="font-bold text-slate-800">{item.furnitureType}</span>
                        <span className="px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 text-[10px] font-semibold">
                          {item.actionType}
                        </span>
                      </div>
                      <p className="text-slate-600 mt-1">{item.notes || 'Routine inventory update'}</p>
                      <p className="text-[10px] text-slate-400 mt-1 font-mono">
                        Logged by {item.recordedBy} · {new Date(item.recordedAt).toLocaleString()}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="pt-3 border-t border-slate-100 flex justify-end">
              <Button variant="outline" size="sm" onClick={onClose}>
                Close
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};
