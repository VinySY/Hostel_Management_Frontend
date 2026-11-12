import React, { useEffect, useState } from 'react';
import { User, Phone, Shield, Building, Award, Mail, Calendar, AlertCircle } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { StatusBadge } from '../../components/common/StatusBadge';
import { ProtectedImage } from '../../components/common/ProtectedImage';
import { apiService } from '../../services/api';
import { Student } from '../../types';
import { useAuth } from '../../context/AuthContext';

export const StudentProfile: React.FC = () => {
  const { user } = useAuth();
  const [student, setStudent] = useState<Student | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);
    setError(null);

    // Call /me directly to fetch the authenticated student's real official record
    apiService
      .getStudentProfile()
      .then((res) => {
        if (isMounted) {
          setStudent(res);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          console.error('Failed to load student profile:', err);
          setError('Unable to load official student profile record. Please check backend connection.');
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [user]);

  if (isLoading) {
    return (
      <div className="p-12 text-center text-slate-500">
        <div className="animate-spin w-8 h-8 border-2 border-blue-600 border-t-transparent rounded-full mx-auto mb-3" />
        <p className="text-sm font-medium">Loading official student profile...</p>
      </div>
    );
  }

  if (error || !student) {
    return (
      <div className="max-w-2xl mx-auto p-6 bg-red-50 border border-red-200 rounded-xl text-center">
        <AlertCircle className="w-8 h-8 text-red-500 mx-auto mb-2" />
        <p className="text-sm font-semibold text-red-800">{error || 'Student record not found'}</p>
        <p className="text-xs text-red-600 mt-1">
          Please contact the hostel administration desk if your official record has not yet been registered.
        </p>
      </div>
    );
  }

  const fallbackAvatar = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80';
  const avatarSrc = student.avatarUrl || user?.avatar || fallbackAvatar;

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Student Banner / Identity Header */}
      <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm flex flex-col sm:flex-row items-center sm:items-start space-y-4 sm:space-y-0 sm:space-x-5">
        <ProtectedImage
          src={avatarSrc}
          fallbackSrc={fallbackAvatar}
          alt={student.name}
          className="w-20 h-20 rounded-full border-2 border-white shadow-md object-cover bg-slate-100 flex-shrink-0"
        />
        <div className="text-center sm:text-left flex-1 min-w-0">
          <div className="flex flex-col sm:flex-row sm:items-center space-y-1 sm:space-y-0 sm:space-x-3">
            <h2 className="text-2xl font-bold text-slate-900">{student.name}</h2>
            <StatusBadge status={student.status} size="sm" />
          </div>
          <p className="text-xs text-slate-500 font-mono mt-1">
            Student ID: <span className="font-semibold text-slate-800">{student.studentId}</span>
          </p>
          <p className="text-xs text-slate-600 mt-0.5">
            {student.course} · Year {student.year}
          </p>
          <div className="mt-2.5 inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-medium bg-blue-50 text-blue-700 border border-blue-100">
            <Shield className="w-3.5 h-3.5 mr-1 text-blue-600" />
            Official University Resident Record
          </div>
        </div>
      </div>

      {/* Institutional & Room Record (Read-only managed by administration) */}
      <Card title="Hostel Allocation & Academic Record" subtitle="Managed officially by Hostel Administrative Office (Read-Only)">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-100">
            <span className="text-slate-400 block mb-0.5 flex items-center">
              <Building className="w-3.5 h-3.5 mr-1 text-slate-400" />
              Hostel Building
            </span>
            <span className="font-semibold text-slate-800 text-sm">
              {student.hostelName || student.block || 'Hostel Campus'}
            </span>
          </div>

          <div className="p-3 bg-slate-50 rounded-lg border border-slate-100">
            <span className="text-slate-400 block mb-0.5 flex items-center">
              <Building className="w-3.5 h-3.5 mr-1 text-slate-400" />
              Assigned Room
            </span>
            <span className="font-semibold text-slate-800 text-sm">
              {student.roomNumber ? `Room ${student.roomNumber}` : 'Unallocated'}
            </span>
            {student.block && <span className="text-[11px] text-slate-400 block">({student.block})</span>}
          </div>

          <div className="p-3 bg-slate-50 rounded-lg border border-slate-100">
            <span className="text-slate-400 block mb-0.5 flex items-center">
              <Award className="w-3.5 h-3.5 mr-1 text-slate-400" />
              Academic Year
            </span>
            <span className="font-semibold text-slate-800 text-sm">
              Year {student.year} ({student.gender})
            </span>
          </div>

          <div className="p-3 bg-blue-50/60 rounded-lg sm:col-span-3 flex items-center text-xs text-blue-800 border border-blue-100">
            <Shield className="w-4 h-4 text-blue-600 mr-2 flex-shrink-0" />
            <span>
              Official student records and room reallocations are managed exclusively by the Warden's desk. To update any institutional information or photo, please contact your hostel warden.
            </span>
          </div>
        </div>
      </Card>

      {/* Personal & Emergency Contact Information (Official Record) */}
      <Card title="Official Contact & Guardian Information" subtitle="Registered contact details on institutional file">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-3.5 rounded-lg border border-slate-200 bg-white">
            <span className="text-slate-400 block text-[11px] mb-1 flex items-center">
              <Mail className="w-3.5 h-3.5 mr-1 text-slate-400" />
              Institutional Email
            </span>
            <span className="font-semibold text-slate-800 text-sm block truncate">{student.email}</span>
          </div>

          <div className="p-3.5 rounded-lg border border-slate-200 bg-white">
            <span className="text-slate-400 block text-[11px] mb-1 flex items-center">
              <Phone className="w-3.5 h-3.5 mr-1 text-slate-400" />
              Registered Mobile Phone
            </span>
            <span className="font-semibold text-slate-800 text-sm block font-mono">{student.phone || 'Not provided'}</span>
          </div>

          <div className="p-3.5 rounded-lg border border-slate-200 bg-white">
            <span className="text-slate-400 block text-[11px] mb-1 flex items-center">
              <User className="w-3.5 h-3.5 mr-1 text-slate-400" />
              Guardian / Parent Name
            </span>
            <span className="font-semibold text-slate-800 text-sm block">{student.guardianName || 'Not recorded'}</span>
          </div>

          <div className="p-3.5 rounded-lg border border-slate-200 bg-white">
            <span className="text-slate-400 block text-[11px] mb-1 flex items-center">
              <Phone className="w-3.5 h-3.5 mr-1 text-slate-400" />
              Emergency Guardian Contact
            </span>
            <span className="font-semibold text-slate-800 text-sm block font-mono">{student.guardianPhone || 'Not recorded'}</span>
          </div>
        </div>
      </Card>
    </div>
  );
};
