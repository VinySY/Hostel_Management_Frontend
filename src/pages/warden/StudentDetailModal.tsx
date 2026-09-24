import React from 'react';
import { Mail, Phone, User, Award, Home, Shield } from 'lucide-react';
import { Modal } from '../../components/common/Modal';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Button } from '../../components/common/Button';
import { Student } from '../../types';

interface StudentDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  student: Student | null;
  onEdit: () => void;
}

export const StudentDetailModal: React.FC<StudentDetailModalProps> = ({
  isOpen,
  onClose,
  student,
  onEdit,
}) => {
  if (!student) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Student Resident Profile" size="md">
      <div className="space-y-4">
        {/* Header summary */}
        <div className="flex items-center space-x-3 pb-3 border-b border-slate-100">
          <div className="w-12 h-12 rounded-full bg-blue-100 text-blue-600 font-bold flex items-center justify-center text-lg">
            {student.name.charAt(0)}
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h4 className="font-bold text-slate-900 text-base">{student.name}</h4>
              <StatusBadge status={student.status} size="sm" />
            </div>
            <p className="text-xs text-slate-400 font-mono">{student.studentId} • {student.gender}</p>
          </div>
        </div>

        {/* Room & Academic */}
        <div className="grid grid-cols-2 gap-3 text-xs">
          <div className="p-3 bg-slate-50 rounded-lg">
            <span className="text-slate-400 flex items-center mb-1">
              <Home className="w-3.5 h-3.5 mr-1 text-slate-400" /> Room Allocation
            </span>
            <span className="font-semibold text-slate-800">
              {student.block}, Room {student.roomNumber}
            </span>
          </div>
          <div className="p-3 bg-slate-50 rounded-lg">
            <span className="text-slate-400 flex items-center mb-1">
              <Award className="w-3.5 h-3.5 mr-1 text-slate-400" /> Academic Course
            </span>
            <span className="font-semibold text-slate-800">
              {student.course} (Yr {student.year})
            </span>
          </div>
        </div>

        {/* Contact Info */}
        <div className="space-y-2 text-xs">
          <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[11px]">
            Direct Contact Information
          </h5>
          <div className="p-3 border border-slate-100 rounded-lg space-y-2 text-slate-600">
            <div className="flex items-center justify-between">
              <span className="flex items-center text-slate-400">
                <Mail className="w-3.5 h-3.5 mr-2" /> Email:
              </span>
              <span className="font-medium text-slate-800">{student.email}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="flex items-center text-slate-400">
                <Phone className="w-3.5 h-3.5 mr-2" /> Phone:
              </span>
              <span className="font-mono font-medium text-slate-800">{student.phone}</span>
            </div>
          </div>
        </div>

        {/* Guardian Contact */}
        <div className="space-y-2 text-xs">
          <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[11px]">
            Parent / Guardian Record
          </h5>
          <div className="p-3 border border-slate-100 rounded-lg space-y-2 text-slate-600 bg-slate-50/50">
            <div className="flex items-center justify-between">
              <span className="flex items-center text-slate-400">
                <User className="w-3.5 h-3.5 mr-2" /> Name:
              </span>
              <span className="font-medium text-slate-800">{student.guardianName}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="flex items-center text-slate-400">
                <Shield className="w-3.5 h-3.5 mr-2" /> Contact:
              </span>
              <span className="font-mono font-medium text-slate-800">{student.guardianPhone}</span>
            </div>
          </div>
        </div>

        <div className="pt-3 border-t border-slate-100 flex justify-end space-x-2">
          <Button variant="outline" size="sm" onClick={onClose}>
            Close
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() => {
              onClose();
              onEdit();
            }}
          >
            Edit Student Record
          </Button>
        </div>
      </div>
    </Modal>
  );
};
