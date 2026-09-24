import React, { useState } from 'react';
import { Mail, Phone, Award, Shield, Save } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';

export const WardenProfile: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [phone, setPhone] = useState('+91 98765 00001');
  const [officeLocation, setOfficeLocation] = useState('Ground Floor, Block A Administrative Wing');
  const [dutyHours, setDutyHours] = useState('09:00 AM - 06:00 PM (Mon-Sat)');
  const [isSaving, setIsSaving] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setTimeout(() => {
      setIsSaving(false);
      showToast('Warden administrative contact updated successfully!', 'success');
    }, 400);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center space-x-4">
        <img
          src={user?.avatar || 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=150&q=80'}
          alt={user?.name}
          className="w-16 h-16 rounded-full border-2 border-white shadow-md object-cover"
        />
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-xl font-bold text-slate-900">{user?.name}</h2>
            <span className="px-2 py-0.5 rounded text-xs font-semibold bg-blue-100 text-blue-700">
              Chief Resident Warden
            </span>
          </div>
          <p className="text-xs text-slate-500 font-mono mt-0.5">
            Hostel Affairs Division • Employee ID: WDN-0042
          </p>
        </div>
      </div>

      <form onSubmit={handleSave} className="space-y-6">
        <Card title="Warden Duties & Campus Authorization">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div className="p-3 bg-slate-50 rounded-lg">
              <span className="text-slate-400 block mb-0.5">Assigned Jurisdiction</span>
              <span className="font-semibold text-slate-800 text-sm">Block A & Block B (Boys & Girls)</span>
            </div>
            <div className="p-3 bg-slate-50 rounded-lg">
              <span className="text-slate-400 block mb-0.5">Administrative Level</span>
              <span className="font-semibold text-slate-800 text-sm">Disciplinary Committee Chair</span>
            </div>
            <div className="p-3 bg-slate-50 rounded-lg sm:col-span-2 flex items-center text-slate-500">
              <Shield className="w-4 h-4 text-blue-600 mr-2 flex-shrink-0" />
              Authorized to approve room allocations, maintenance dispatches, and student leave certificates.
            </div>
          </div>
        </Card>

        <Card title="Official Contact Details">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Official Institutional Email"
              value={user?.email || 'warden.vance@hostelhub.edu'}
              disabled
              leftIcon={<Mail className="w-4 h-4" />}
            />

            <Input
              label="Duty Office Direct Phone"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              leftIcon={<Phone className="w-4 h-4" />}
              required
            />

            <Input
              label="Office Location on Campus"
              value={officeLocation}
              onChange={(e) => setOfficeLocation(e.target.value)}
              leftIcon={<Award className="w-4 h-4" />}
              required
            />

            <Input
              label="Official Office Consultation Hours"
              value={dutyHours}
              onChange={(e) => setDutyHours(e.target.value)}
              required
            />
          </div>

          <div className="mt-5 flex justify-end">
            <Button
              type="submit"
              variant="primary"
              icon={<Save className="w-4 h-4" />}
              isLoading={isSaving}
            >
              Update Warden Profile
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
};
