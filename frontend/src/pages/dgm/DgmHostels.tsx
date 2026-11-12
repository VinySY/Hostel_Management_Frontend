import React, { useEffect, useState } from 'react';
import {
  Building2,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Loader2,
  RefreshCw,
  TrendingUp,
  Plus,
  Edit,
  Power,
  Layers,
  Users,
  MapPin
} from 'lucide-react';
import { apiService } from '../../services/api';
import { Hostel } from '../../types';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';

export const DgmHostels: React.FC = () => {
  const [hostels, setHostels] = useState<Hostel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal states
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [editingHostel, setEditingHostel] = useState<Hostel | null>(null);
  const [formName, setFormName] = useState('');
  const [formCode, setFormCode] = useState('');
  const [formAddress, setFormAddress] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formTotalFloors, setFormTotalFloors] = useState(4);
  const [formCapacity, setFormCapacity] = useState(120);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const fetchHostels = async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await apiService.getHostels();
      setHostels(list);
    } catch (err: any) {
      setError(err.message || 'Failed to load hostels');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHostels();
  }, []);

  const openAddModal = () => {
    setEditingHostel(null);
    setFormName('');
    setFormCode('');
    setFormAddress('');
    setFormDescription('');
    setFormTotalFloors(4);
    setFormCapacity(120);
    setFormError(null);
    setIsAddModalOpen(true);
  };

  const openEditModal = (h: Hostel) => {
    setEditingHostel(h);
    setFormName(h.name);
    setFormCode(h.code || '');
    setFormAddress(h.address || '');
    setFormDescription(h.description || '');
    setFormTotalFloors(h.totalFloors || 4);
    setFormCapacity(h.totalCapacity ?? 120);
    setFormError(null);
    setIsAddModalOpen(true);
  };

  const handleSaveHostel = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setFormError(null);

    const payload = {
      name: formName.trim(),
      code: formCode.trim() || undefined,
      address: formAddress.trim() || undefined,
      description: formDescription.trim() || undefined,
      totalFloors: Number(formTotalFloors),
      capacity: Number(formCapacity),
    };

    try {
      if (editingHostel) {
        await apiService.updateHostel(editingHostel.id, payload);
      } else {
        await apiService.createHostel(payload);
      }
      setIsAddModalOpen(false);
      await fetchHostels();
    } catch (err: any) {
      setFormError(err.message || 'Failed to save hostel');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggleActive = async (h: Hostel) => {
    const action = h.active ? 'deactivate' : 'activate';
    if (!window.confirm(`Are you sure you want to ${action} ${h.name}?`)) {
      return;
    }

    try {
      await apiService.toggleHostelStatus(h.id, !h.active);
      await fetchHostels();
    } catch (err: any) {
      alert(err.message || 'Failed to change hostel active status');
    }
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Institutional Hostel Administration</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Configure campus residences, floor capacities, structural allocations, and active operational statuses
          </p>
        </div>

        <Button
          variant="primary"
          icon={<Plus className="w-4 h-4" />}
          onClick={openAddModal}
          className="text-xs"
        >
          Add New Hostel
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64 text-slate-400">
          <Loader2 className="w-7 h-7 text-blue-600 animate-spin mr-2" />
          <span className="text-sm">Loading hostel records...</span>
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center">
          <AlertTriangle className="w-8 h-8 text-red-500 mx-auto mb-2" />
          <p className="text-sm text-red-700">{error}</p>
          <button
            onClick={fetchHostels}
            className="mt-3 text-xs text-red-600 hover:underline flex items-center mx-auto cursor-pointer"
          >
            <RefreshCw className="w-3.5 h-3.5 mr-1" /> Retry
          </button>
        </div>
      ) : hostels.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-xl p-10 text-center">
          <Building2 className="w-10 h-10 text-slate-300 mx-auto mb-3" />
          <p className="text-sm font-medium text-slate-500">No hostels registered yet</p>
          <Button variant="primary" size="sm" onClick={openAddModal} className="mt-4">
            Create First Hostel
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {hostels.map((h) => (
            <Card
              key={h.id}
              className={`!p-0 overflow-hidden border transition-all hover:shadow-md ${
                !h.active ? 'opacity-70 border-slate-200 bg-slate-50' : 'border-slate-200 bg-white'
              }`}
            >
              <div className="bg-gradient-to-r from-blue-700 to-indigo-800 p-4 text-white">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 bg-white/15 rounded-lg flex items-center justify-center">
                      <Building2 className="w-5 h-5 text-white" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-white">{h.name}</h3>
                      <p className="text-blue-200 text-xs font-mono">
                        {h.code || `HST-${h.id}`}
                      </p>
                    </div>
                  </div>
                  <span
                    className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      h.active
                        ? 'bg-emerald-500/20 text-emerald-200 border border-emerald-400/30'
                        : 'bg-red-500/20 text-red-200 border border-red-400/30'
                    }`}
                  >
                    {h.active ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                </div>
              </div>

              <div className="p-4 space-y-3">
                {h.address && (
                  <p className="text-xs text-slate-500 flex items-center gap-1.5 truncate">
                    <MapPin className="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
                    {h.address}
                  </p>
                )}

                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="p-2.5 bg-slate-50 border border-slate-100 rounded-lg">
                    <span className="text-[11px] text-slate-400 block mb-0.5 flex items-center gap-1">
                      <Layers className="w-3 h-3" /> Structure
                    </span>
                    <span className="font-semibold text-slate-800">
                      {h.totalFloors ? `${h.totalFloors} Floors` : `${h.totalRooms ?? 0} Rooms`}
                    </span>
                  </div>

                  <div className="p-2.5 bg-slate-50 border border-slate-100 rounded-lg">
                    <span className="text-[11px] text-slate-400 block mb-0.5 flex items-center gap-1">
                      <Users className="w-3 h-3" /> Max Capacity
                    </span>
                    <span className="font-semibold text-slate-800">{h.totalCapacity ?? 0} Beds</span>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs">
                  <Button
                    variant="outline"
                    size="sm"
                    icon={<Edit className="w-3.5 h-3.5" />}
                    onClick={() => openEditModal(h)}
                    className="text-xs"
                  >
                    Edit Details
                  </Button>

                  <Button
                    variant={h.active ? 'outline' : 'primary'}
                    size="sm"
                    icon={<Power className="w-3.5 h-3.5" />}
                    onClick={() => handleToggleActive(h)}
                    className={`text-xs ${
                      h.active ? 'hover:bg-red-50 hover:text-red-600 hover:border-red-200' : ''
                    }`}
                  >
                    {h.active ? 'Deactivate' : 'Activate'}
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Add / Edit Hostel Modal */}
      <Modal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        title={editingHostel ? `Edit Hostel: ${editingHostel.name}` : 'Register New Campus Hostel'}
        size="md"
      >
        <form onSubmit={handleSaveHostel} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700">
              {formError}
            </div>
          )}

          <Input
            label="Hostel Name"
            value={formName}
            onChange={(e) => setFormName(e.target.value)}
            placeholder="e.g. Ramanujan Hall of Residence"
            required
          />

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Hostel Code"
              value={formCode}
              onChange={(e) => setFormCode(e.target.value)}
              placeholder="e.g. RAM-01"
            />

            <Input
              label="Total Floors"
              type="number"
              min={1}
              max={15}
              value={String(formTotalFloors)}
              onChange={(e) => setFormTotalFloors(Number(e.target.value))}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Maximum Resident Capacity"
              type="number"
              min={10}
              max={2000}
              value={String(formCapacity)}
              onChange={(e) => setFormCapacity(Number(e.target.value))}
              required
            />

            <Input
              label="Campus Location / Address"
              value={formAddress}
              onChange={(e) => setFormAddress(e.target.value)}
              placeholder="e.g. North Campus, Sector 4"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Description / Notes
            </label>
            <textarea
              value={formDescription}
              onChange={(e) => setFormDescription(e.target.value)}
              rows={3}
              placeholder="Hostel overview, block layout details, amenities notes..."
              className="w-full p-2.5 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsAddModalOpen(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting}>
              {editingHostel ? 'Save Changes' : 'Create Hostel'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
