import React, { useState, useEffect } from 'react';
import { Upload } from 'lucide-react';
import { Modal } from '../../components/common/Modal';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { ProtectedImage } from '../../components/common/ProtectedImage';
import { Student, Room } from '../../types';
import { apiService } from '../../services/api';

interface StudentFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (studentData: Partial<Student> & { password?: string }) => Promise<void>;
  studentToEdit?: Student | null;
}

export const StudentFormModal: React.FC<StudentFormModalProps> = ({
  isOpen,
  onClose,
  onSave,
  studentToEdit,
}) => {
  const [name, setName] = useState('');
  const [studentId, setStudentId] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [gender, setGender] = useState<'Male' | 'Female' | 'Other'>('Male');
  const [course, setCourse] = useState('B.Tech Computer Science');
  const [year, setYear] = useState('1');
  const [roomId, setRoomId] = useState('');
  const [guardianName, setGuardianName] = useState('');
  const [guardianPhone, setGuardianPhone] = useState('');
  const [status, setStatus] = useState<'Active' | 'Inactive' | 'Suspended'>('Active');
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [availableRooms, setAvailableRooms] = useState<Room[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    apiService.getRooms().then(setAvailableRooms);
  }, []);

  useEffect(() => {
    if (studentToEdit) {
      setName(studentToEdit.name);
      setStudentId(studentToEdit.studentId);
      setEmail(studentToEdit.email);
      setPassword('');
      setPhone(studentToEdit.phone);
      setGender(studentToEdit.gender);
      setCourse(studentToEdit.course);
      setYear(String(studentToEdit.year));
      setRoomId(studentToEdit.roomId);
      setGuardianName(studentToEdit.guardianName);
      setGuardianPhone(studentToEdit.guardianPhone);
      setStatus(studentToEdit.status);
      setPreviewUrl(studentToEdit.avatarUrl || null);
      setPhotoFile(null);
    } else {
      setName('');
      setStudentId(`STU-2026-${Math.floor(100 + Math.random() * 900)}`);
      setEmail('');
      setPassword('');
      setPhone('');
      setGender('Male');
      setCourse('B.Tech Computer Science');
      setYear('1');
      setRoomId('room-1');
      setGuardianName('');
      setGuardianPhone('');
      setStatus('Active');
      setPreviewUrl(null);
      setPhotoFile(null);
    }
  }, [studentToEdit, isOpen]);

  useEffect(() => {
    if (!previewUrl || !previewUrl.startsWith('blob:')) return;
    return () => {
      URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    const selectedRoom = availableRooms.find((r) => r.id === roomId);

    try {
      await onSave({
        name,
        studentId,
        email: email || `${name.toLowerCase().replace(/\s+/g, '.')}@hostelhub.edu`,
        ...(studentToEdit ? {} : { password }),
        phone,
        gender,
        course,
        year: Number(year),
        roomId,
        roomNumber: selectedRoom?.roomNumber || '101',
        block: selectedRoom?.block || 'Block A',
        guardianName,
        guardianPhone,
        status,
      });

      if (photoFile && studentToEdit?.id) {
        try {
          await apiService.uploadStudentPhoto(studentToEdit.id, photoFile);
        } catch (err) {
          console.error('Failed to upload photo:', err);
        }
      }

      onClose();
    } catch {
      // Keep modal open when validation or submission fails; parent displays toast
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={studentToEdit ? 'Edit Resident Profile' : 'Register New Resident'}
      size="lg"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Photo Upload Header */}
        <div className="flex items-center gap-4 p-3 bg-slate-50 border border-slate-200 rounded-lg">
          <div className="w-14 h-14 rounded-full overflow-hidden bg-slate-200 flex-shrink-0 flex items-center justify-center border border-slate-300">
            <ProtectedImage
              src={previewUrl}
              alt="Preview"
              className="w-full h-full object-cover"
              fallbackNode={<Upload className="w-6 h-6 text-slate-400" />}
            />
          </div>
          <div className="flex-1">
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Official Photo (Warden-managed only)
            </label>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) {
                  setPhotoFile(file);
                  setPreviewUrl(URL.createObjectURL(file));
                }
              }}
              className="text-xs text-slate-500 file:mr-2 file:py-1 file:px-2.5 file:rounded-md file:border-0 file:text-xs file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 cursor-pointer"
            />
            <p className="text-[11px] text-slate-400 mt-0.5">JPEG, PNG or WebP under 5MB</p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Full Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Vikram Malhotra"
            required
          />

          <Input
            label="Student ID"
            value={studentId}
            onChange={(e) => setStudentId(e.target.value)}
            required
          />

          <Input
            label="Institutional Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="e.g. vikram.m@hostelhub.edu"
            required
          />

          {!studentToEdit && (
            <Input
              label="Initial Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Minimum 6 characters"
              minLength={6}
              required
            />
          )}

          <Input
            label="Student Mobile"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="+91 98765 43210"
            required
          />

          <Select
            label="Gender"
            value={gender}
            onChange={(e) => setGender(e.target.value as 'Male' | 'Female' | 'Other')}
            options={[
              { value: 'Male', label: 'Male' },
              { value: 'Female', label: 'Female' },
              { value: 'Other', label: 'Other' },
            ]}
          />

          <Select
            label="Enrolled Year"
            value={year}
            onChange={(e) => setYear(e.target.value)}
            options={[
              { value: '1', label: 'Year 1 (Freshman)' },
              { value: '2', label: 'Year 2 (Sophomore)' },
              { value: '3', label: 'Year 3 (Junior)' },
              { value: '4', label: 'Year 4 (Senior)' },
            ]}
          />

          <Input
            label="Degree / Major"
            value={course}
            onChange={(e) => setCourse(e.target.value)}
            placeholder="e.g. B.Tech Computer Science"
            required
          />

          <Select
            label="Allocate Room"
            value={roomId}
            onChange={(e) => setRoomId(e.target.value)}
            options={availableRooms.map((r) => ({
              value: r.id,
              label: `${r.block} - Room ${r.roomNumber} (${r.occupied}/${r.capacity} beds filled)`,
            }))}
            required
          />

          <Input
            label="Guardian Name"
            value={guardianName}
            onChange={(e) => setGuardianName(e.target.value)}
            placeholder="e.g. Ramesh Malhotra"
            required
          />

          <Input
            label="Guardian Phone"
            value={guardianPhone}
            onChange={(e) => setGuardianPhone(e.target.value)}
            placeholder="+91 98111 22334"
            required
          />

          <Select
            label="Hostel Resident Status"
            value={status}
            onChange={(e) => setStatus(e.target.value as 'Active' | 'Inactive' | 'Suspended')}
            options={[
              { value: 'Active', label: 'Active Resident' },
              { value: 'Inactive', label: 'Checked Out / Inactive' },
              { value: 'Suspended', label: 'Disciplinary Suspended' },
            ]}
          />
        </div>

        <div className="pt-4 border-t border-slate-100 flex justify-end space-x-3">
          <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" isLoading={isSubmitting}>
            {studentToEdit ? 'Save Changes' : 'Register Student'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
