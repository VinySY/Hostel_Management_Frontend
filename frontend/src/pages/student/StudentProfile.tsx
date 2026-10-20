import React, { useEffect, useState } from 'react';
import { User, Phone, Shield, Save } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { StatusBadge } from '../../components/common/StatusBadge';
import { apiService } from '../../services/api';
import { Student } from '../../types';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';

export const StudentProfile: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [student, setStudent] = useState<Student | null>(null);
  const [phone, setPhone] = useState('');
  const [guardianPhone, setGuardianPhone] = useState('');
  const [guardianName, setGuardianName] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    apiService.getStudentProfile(user?.studentId || 'STU-2023-089').then((res) => {
      setStudent(res);
      setPhone(res.phone);
      setGuardianName(res.guardianName);
      setGuardianPhone(res.guardianPhone);
      setIsLoading(false);
    });
  }, [user]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!student) return;

    setIsSaving(true);
    try {
      const updated = await apiService.updateStudentProfile(student.id, {
        phone,
        guardianName,
        guardianPhone,
      });
      setStudent(updated);
      showToast('Contact details successfully updated!', 'success');
    } catch {
      showToast('Failed to update details.', 'error');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading || !student) {
    return (
      <div className="p-8 text-center text-slate-400">
        <div className="animate-spin w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full mx-auto mb-2" />
        Loading profile...
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center space-x-4">
        <img
          src={user?.avatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80'}
          alt={student.name}
          className="w-16 h-16 rounded-full border-2 border-white shadow-md object-cover"
        />
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-xl font-bold text-slate-900">{student.name}</h2>
            <StatusBadge status={student.status} size="sm" />
          </div>
          <p className="text-xs text-slate-500 font-mono">
            {student.studentId} • {student.course} (Year {student.year})
          </p>
        </div>
      </div>

      <form onSubmit={handleSave} className="space-y-6">
        {/* Academic and Room Info (Read-only managed by administration) */}
        <Card title="Institutional & Room Record" subtitle="Managed by Hostel Administrative Office">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
            <div className="p-3 bg-slate-50 rounded-lg">
              <span className="text-slate-400 block mb-0.5">Assigned Room</span>
              <span className="font-semibold text-slate-800 text-sm">Room {student.roomNumber}</span>
            </div>
            <div className="p-3 bg-slate-50 rounded-lg">
              <span className="text-slate-400 block mb-0.5">Hostel Block</span>
              <span className="font-semibold text-slate-800 text-sm">{student.block}</span>
            </div>
            <div className="p-3 bg-slate-50 rounded-lg">
              <span className="text-slate-400 block mb-0.5">Gender Record</span>
              <span className="font-semibold text-slate-800 text-sm">{student.gender}</span>
            </div>
            <div className="p-3 bg-slate-50 rounded-lg sm:col-span-3 flex items-center text-slate-500">
              <Shield className="w-4 h-4 text-blue-500 mr-2 flex-shrink-0" />
              Room reallocations must be requested directly through the Warden's desk.
            </div>
          </div>
        </Card>

        {/* Contact Information (Editable) */}
        <Card title="Personal & Emergency Contact">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Email Address"
              value={student.email}
              disabled
              helperText="Official university email cannot be altered"
            />
            <Input
              label="Primary Phone Number"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              leftIcon={<Phone className="w-4 h-4" />}
              required
            />
            <Input
              label="Guardian / Parent Name"
              value={guardianName}
              onChange={(e) => setGuardianName(e.target.value)}
              leftIcon={<User className="w-4 h-4" />}
              required
            />
            <Input
              label="Guardian Contact Phone"
              value={guardianPhone}
              onChange={(e) => setGuardianPhone(e.target.value)}
              leftIcon={<Phone className="w-4 h-4" />}
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
              Save Profile Updates
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
};
