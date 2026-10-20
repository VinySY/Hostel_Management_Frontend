import React, { useState, useEffect } from 'react';
import { Modal } from '../../components/common/Modal';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Student, Room } from '../../types';
import { apiService } from '../../services/api';

interface StudentFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (studentData: Partial<Student>) => Promise<void>;
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
  const [phone, setPhone] = useState('');
  const [gender, setGender] = useState<'Male' | 'Female' | 'Other'>('Male');
  const [course, setCourse] = useState('B.Tech Computer Science');
  const [year, setYear] = useState('1');
  const [roomId, setRoomId] = useState('');
  const [guardianName, setGuardianName] = useState('');
  const [guardianPhone, setGuardianPhone] = useState('');
  const [status, setStatus] = useState<'Active' | 'Inactive' | 'Suspended'>('Active');
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
      setPhone(studentToEdit.phone);
      setGender(studentToEdit.gender);
      setCourse(studentToEdit.course);
      setYear(String(studentToEdit.year));
      setRoomId(studentToEdit.roomId);
      setGuardianName(studentToEdit.guardianName);
      setGuardianPhone(studentToEdit.guardianPhone);
      setStatus(studentToEdit.status);
    } else {
      setName('');
      setStudentId(`STU-2026-${Math.floor(100 + Math.random() * 900)}`);
      setEmail('');
      setPhone('');
      setGender('Male');
      setCourse('B.Tech Computer Science');
      setYear('1');
      setRoomId('room-1');
      setGuardianName('');
      setGuardianPhone('');
      setStatus('Active');
    }
  }, [studentToEdit, isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    const selectedRoom = availableRooms.find((r) => r.id === roomId);

    try {
      await onSave({
        name,
        studentId,
        email: email || `${name.toLowerCase().replace(/\s+/g, '.')}@hostelhub.edu`,
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
      onClose();
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
          />

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
