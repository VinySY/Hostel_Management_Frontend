import React, { useEffect, useState } from 'react';
import {
  Users,
  Plus,
  Shield,
  Layers,
  Phone,
  Mail,
  Edit,
  Power,
  CheckCircle2,
  AlertCircle,
  Building2,
  Check
} from 'lucide-react';
import { apiService } from '../../services/api';
import { Warden, Hostel } from '../../types';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { EmptyState } from '../../components/common/EmptyState';
import {
  WARDEN_PASSWORD_MAX_LENGTH,
  WARDEN_PASSWORD_MIN_LENGTH,
  buildCreateWardenPayload,
  validateWardenCreatePassword,
} from '../../utils/wardenForm';

export const DgmWardens: React.FC = () => {
  const [wardens, setWardens] = useState<Warden[]>([]);
  const [hostels, setHostels] = useState<Hostel[]>([]);
  const [selectedHostelFilter, setSelectedHostelFilter] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(true);

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingWarden, setEditingWarden] = useState<Warden | null>(null);
  const [floorAssignmentWarden, setFloorAssignmentWarden] = useState<Warden | null>(null);

  // Form states
  const [employeeId, setEmployeeId] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [hostelId, setHostelId] = useState<number>(1);
  const [selectedFloors, setSelectedFloors] = useState<number[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fetchData = async () => {
    setIsLoading(true);
    try {
      const [wList, hList] = await Promise.all([
        apiService.getWardens(),
        apiService.getHostels(),
      ]);
      setWardens(wList);
      setHostels(hList);
      if (hList.length > 0) {
        setHostelId(hList[0].id);
      }
    } catch (err) {
      console.error('Failed to load warden roster:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const openCreateModal = () => {
    setEditingWarden(null);
    setEmployeeId(`WAR-${Math.floor(100 + Math.random() * 900)}`);
    setName('');
    setEmail('');
    setPassword('');
    setPhone('');
    setSelectedFloors([1]);
    setErrorMessage(null);
    setIsCreateModalOpen(true);
  };

  const closeCreateModal = () => {
    setIsCreateModalOpen(false);
    setEditingWarden(null);
    setPassword('');
    setErrorMessage(null);
  };

  const openFloorModal = (w: Warden) => {
    setFloorAssignmentWarden(w);
    setSelectedFloors(w.assignedFloors || []);
    setErrorMessage(null);
  };

  const handleSaveWarden = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!editingWarden) {
      const passwordValidationError = validateWardenCreatePassword(password);
      if (passwordValidationError) {
        setErrorMessage(passwordValidationError);
        return;
      }
      if (!hostelId) {
        setErrorMessage('Assigned hostel is required');
        return;
      }
    }

    setIsSubmitting(true);

    try {
      if (editingWarden) {
        await apiService.updateWarden(editingWarden.id, {
          name: name.trim(),
          phone: phone.trim(),
          email: email.trim(),
        });
      } else {
        await apiService.createWarden(
          buildCreateWardenPayload({
            employeeId,
            name,
            email,
            phone,
            hostelId,
            password,
            assignedFloors: selectedFloors,
          })
        );
      }
      closeCreateModal();
      await fetchData();
    } catch (err: any) {
      setErrorMessage(err?.message || 'Failed to save warden record');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSaveFloors = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!floorAssignmentWarden) return;

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      await apiService.assignWardenFloors(floorAssignmentWarden.id, selectedFloors);
      setFloorAssignmentWarden(null);
      await fetchData();
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to assign floors');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggleStatus = async (w: Warden) => {
    const action = w.active ? 'deactivate' : 'reactivate';
    if (!window.confirm(`Are you sure you want to ${action} ${w.name}?`)) return;

    try {
      await apiService.toggleWardenStatus(w.id, !w.active);
      await fetchData();
    } catch (err: any) {
      alert(err.message || 'Failed to change warden operational status');
    }
  };

  const toggleFloor = (fl: number) => {
    setSelectedFloors((prev) =>
      prev.includes(fl) ? prev.filter((f) => f !== fl) : [...prev, fl].sort((a, b) => a - b)
    );
  };

  const filteredWardens = wardens.filter((w) => {
    if (selectedHostelFilter === 'ALL') return true;
    return String(w.hostelId) === selectedHostelFilter;
  });

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Warden Administration & Multi-Floor Delegations</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Oversee hostel supervisory personnel, assign floor-specific administrative accountability, and manage active rosters
          </p>
        </div>

        <div className="flex items-center gap-3">
          <select
            value={selectedHostelFilter}
            onChange={(e) => setSelectedHostelFilter(e.target.value)}
            className="px-3 py-1.5 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500 font-medium text-slate-700"
          >
            <option value="ALL">All Hostels</option>
            {hostels.map((h) => (
              <option key={h.id} value={String(h.id)}>
                {h.name}
              </option>
            ))}
          </select>

          <Button
            variant="primary"
            icon={<Plus className="w-4 h-4" />}
            onClick={openCreateModal}
            className="text-xs"
          >
            Register Warden
          </Button>
        </div>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-slate-400">Loading warden delegations...</div>
      ) : filteredWardens.length === 0 ? (
        <EmptyState
          title="No wardens found"
          description="Click 'Register Warden' to recruit and assign supervisory staff."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredWardens.map((w) => (
            <Card
              key={w.id}
              className={`!p-5 border transition-all ${
                !w.active ? 'opacity-60 bg-slate-50' : 'bg-white hover:shadow-md'
              }`}
            >
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-sm">
                    {w.name.charAt(0)}
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-slate-900">{w.name}</h3>
                    <p className="text-[11px] font-mono text-slate-400">{w.employeeId}</p>
                  </div>
                </div>

                <span
                  className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                    w.active
                      ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                      : 'bg-red-50 text-red-700 border border-red-200'
                  }`}
                >
                  {w.active ? 'ACTIVE' : 'DEACTIVATED'}
                </span>
              </div>

              <div className="space-y-1.5 text-xs text-slate-600 mb-4">
                <div className="flex items-center gap-2">
                  <Mail className="w-3.5 h-3.5 text-slate-400" />
                  <span className="truncate">{w.email}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Phone className="w-3.5 h-3.5 text-slate-400" />
                  <span>{w.phone}</span>
                </div>
                {w.hostelName && (
                  <div className="flex items-center gap-2">
                    <Building2 className="w-3.5 h-3.5 text-slate-400" />
                    <span className="font-semibold text-slate-700">{w.hostelName}</span>
                  </div>
                )}
              </div>

              {/* Assigned Floors */}
              <div className="p-3 bg-slate-50 border border-slate-100 rounded-lg mb-4">
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-[11px] font-bold text-slate-600 uppercase tracking-wider flex items-center gap-1">
                    <Layers className="w-3 h-3 text-slate-400" /> Delegated Floors
                  </span>
                  <button
                    onClick={() => openFloorModal(w)}
                    className="text-[11px] text-blue-600 hover:underline font-semibold cursor-pointer"
                  >
                    Edit Floors
                  </button>
                </div>

                <div className="flex flex-wrap gap-1.5">
                  {w.assignedFloors && w.assignedFloors.length > 0 ? (
                    w.assignedFloors.map((fl) => (
                      <span
                        key={fl}
                        className="px-2 py-0.5 bg-blue-50 text-blue-700 border border-blue-200 rounded text-[11px] font-bold"
                      >
                        Floor {fl}
                      </span>
                    ))
                  ) : (
                    <span className="text-xs text-slate-400 italic">No floors assigned</span>
                  )}
                </div>
              </div>

              <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs">
                <Button
                  variant="outline"
                  size="sm"
                  icon={<Edit className="w-3.5 h-3.5" />}
                  onClick={() => {
                    setEditingWarden(w);
                    setName(w.name);
                    setEmail(w.email);
                    setPassword('');
                    setPhone(w.phone);
                    setErrorMessage(null);
                    setIsCreateModalOpen(true);
                  }}
                  className="text-xs"
                >
                  Edit Profile
                </Button>

                <Button
                  variant={w.active ? 'outline' : 'primary'}
                  size="sm"
                  icon={<Power className="w-3.5 h-3.5" />}
                  onClick={() => handleToggleStatus(w)}
                  className={`text-xs ${
                    w.active ? 'hover:bg-red-50 hover:text-red-600 hover:border-red-200' : ''
                  }`}
                >
                  {w.active ? 'Deactivate' : 'Reactivate'}
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Register / Edit Warden Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={closeCreateModal}
        title={editingWarden ? `Edit Warden: ${editingWarden.name}` : 'Register Hostel Warden'}
        size="md"
      >
        <form onSubmit={handleSaveWarden} className="space-y-4">
          {errorMessage && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          {!editingWarden && (
            <Input
              label="Employee ID"
              value={employeeId}
              onChange={(e) => setEmployeeId(e.target.value)}
              placeholder="e.g. WAR-102"
              required
            />
          )}

          <Input
            label="Full Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Dr. Rajesh Vance"
            required
          />

          <Input
            label="Institutional Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="e.g. warden.vance@hostelhub.edu"
            required
          />

          {!editingWarden && (
            <Input
              label="Initial Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={`${WARDEN_PASSWORD_MIN_LENGTH} to ${WARDEN_PASSWORD_MAX_LENGTH} characters`}
              minLength={WARDEN_PASSWORD_MIN_LENGTH}
              maxLength={WARDEN_PASSWORD_MAX_LENGTH}
              autoComplete="new-password"
              helperText={`Must be between ${WARDEN_PASSWORD_MIN_LENGTH} and ${WARDEN_PASSWORD_MAX_LENGTH} characters`}
              required
            />
          )}

          <Input
            label="Mobile Phone"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="+91 98765 43210"
            required
          />

          {!editingWarden && (
            <Select
              label="Assigned Hostel"
              value={String(hostelId)}
              onChange={(e) => setHostelId(Number(e.target.value))}
              options={hostels.map((h) => ({
                value: String(h.id),
                label: h.name,
              }))}
            />
          )}

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={closeCreateModal}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting}>
              {editingWarden ? 'Save Changes' : 'Register Warden'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Multi-floor Delegation Modal */}
      {floorAssignmentWarden && (
        <Modal
          isOpen={!!floorAssignmentWarden}
          onClose={() => setFloorAssignmentWarden(null)}
          title={`Assign Floors: ${floorAssignmentWarden.name}`}
          size="md"
        >
          <form onSubmit={handleSaveFloors} className="space-y-4">
            <p className="text-xs text-slate-500">
              Select which residential floors this warden is responsible for handling. Complaints and resident grievances on these floors will be assigned to this warden.
            </p>

            <div className="grid grid-cols-3 gap-2.5">
              {[1, 2, 3, 4, 5, 6, 7, 8].map((fl) => {
                const isSelected = selectedFloors.includes(fl);
                return (
                  <button
                    key={fl}
                    type="button"
                    onClick={() => toggleFloor(fl)}
                    className={`p-3 rounded-lg border text-xs font-bold flex items-center justify-between cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                        : 'bg-white text-slate-700 border-slate-200 hover:border-blue-300'
                    }`}
                  >
                    <span>Floor {fl}</span>
                    {isSelected && <Check className="w-3.5 h-3.5" />}
                  </button>
                );
              })}
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
              <Button
                type="button"
                variant="outline"
                onClick={() => setFloorAssignmentWarden(null)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button type="submit" variant="primary" isLoading={isSubmitting}>
                Save Delegations
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};
