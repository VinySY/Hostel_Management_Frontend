import React, { useEffect, useState } from 'react';
import { Plus, Search, Edit2, Trash2, Eye } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { EmptyState } from '../../components/common/EmptyState';
import { StudentFormModal } from './StudentFormModal';
import { StudentDetailModal } from './StudentDetailModal';
import { apiService } from '../../services/api';
import { Student } from '../../types';
import { useToast } from '../../context/ToastContext';

export const StudentList: React.FC = () => {
  const { showToast } = useToast();
  const [students, setStudents] = useState<Student[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [blockFilter, setBlockFilter] = useState('ALL');
  const [isLoading, setIsLoading] = useState(true);

  // Modals state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [studentToEdit, setStudentToEdit] = useState<Student | null>(null);
  const [viewingStudent, setViewingStudent] = useState<Student | null>(null);
  const [studentToDelete, setStudentToDelete] = useState<Student | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchStudents = async () => {
    try {
      setIsLoading(true);
      const data = await apiService.getStudents();
      setStudents(data);
    } catch {
      showToast('Failed to load students roster', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchStudents();
  }, []);

  const handleSaveStudent = async (studentData: Partial<Student>) => {
    try {
      if (studentToEdit) {
        await apiService.updateStudent(studentToEdit.id, studentData);
        showToast(`Student record for ${studentData.name} updated`, 'success');
      } else {
        await apiService.createStudent(studentData as Omit<Student, 'id'>);
        showToast(`New resident ${studentData.name} registered`, 'success');
      }
      fetchStudents();
    } catch {
      showToast('Error saving student record', 'error');
    }
  };

  const handleDeleteConfirm = async () => {
    if (!studentToDelete) return;
    setIsDeleting(true);
    try {
      await apiService.deleteStudent(studentToDelete.id);
      showToast(`Student ${studentToDelete.name} removed from roster`, 'success');
      setStudentToDelete(null);
      fetchStudents();
    } catch {
      showToast('Could not delete student record', 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  const filtered = students.filter((s) => {
    const matchesSearch =
      s.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.studentId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.roomNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.course.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesBlock = blockFilter === 'ALL' || s.block === blockFilter;
    return matchesSearch && matchesBlock;
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Student Residents Directory</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Manage student registrations, room allocations, and resident profiles
          </p>
        </div>
        <Button
          variant="primary"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => {
            setStudentToEdit(null);
            setIsFormOpen(true);
          }}
        >
          Register Student
        </Button>
      </div>

      {/* Filter and Search Bar */}
      <Card className="!p-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="relative sm:col-span-2">
            <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
            <input
              type="text"
              placeholder="Search by student name, ID number, course or room..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <select
            value={blockFilter}
            onChange={(e) => setBlockFilter(e.target.value)}
            className="w-full px-3 py-2 text-xs border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="ALL">All Residential Blocks</option>
            <option value="Block A">Block A</option>
            <option value="Block B">Block B</option>
          </select>
        </div>
      </Card>

      {/* Table view */}
      {isLoading ? (
        <div className="p-12 text-center text-slate-400">Loading student directory...</div>
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No students match criteria"
          description="Try modifying search keywords or block filter."
          actionText="Register Student"
          onAction={() => {
            setStudentToEdit(null);
            setIsFormOpen(true);
          }}
        />
      ) : (
        <Card className="!p-0 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Student ID</th>
                  <th className="py-3 px-4">Name</th>
                  <th className="py-3 px-4">Course & Year</th>
                  <th className="py-3 px-4">Room & Block</th>
                  <th className="py-3 px-4">Phone</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((s) => (
                  <tr key={s.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="py-3.5 px-4 font-mono font-semibold text-slate-700">
                      {s.studentId}
                    </td>
                    <td className="py-3.5 px-4">
                      <p className="font-semibold text-slate-900">{s.name}</p>
                      <p className="text-[11px] text-slate-400">{s.email}</p>
                    </td>
                    <td className="py-3.5 px-4 text-slate-700">
                      {s.course} <span className="text-slate-400">· Yr {s.year}</span>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="font-medium text-slate-800">Room {s.roomNumber}</span>
                      <span className="block text-[11px] text-slate-400">{s.block}</span>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-600">{s.phone}</td>
                    <td className="py-3.5 px-4">
                      <StatusBadge status={s.status} size="sm" />
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end space-x-1">
                        <button
                          onClick={() => setViewingStudent(s)}
                          className="p-1 text-slate-500 hover:text-blue-600 rounded hover:bg-slate-100 cursor-pointer"
                          title="View Details"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => {
                            setStudentToEdit(s);
                            setIsFormOpen(true);
                          }}
                          className="p-1 text-slate-500 hover:text-amber-600 rounded hover:bg-slate-100 cursor-pointer"
                          title="Edit Student"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => setStudentToDelete(s)}
                          className="p-1 text-slate-500 hover:text-red-600 rounded hover:bg-slate-100 cursor-pointer"
                          title="Remove Student"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Modals */}
      <StudentFormModal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSave={handleSaveStudent}
        studentToEdit={studentToEdit}
      />

      <StudentDetailModal
        isOpen={!!viewingStudent}
        onClose={() => setViewingStudent(null)}
        student={viewingStudent}
        onEdit={() => {
          if (viewingStudent) {
            setStudentToEdit(viewingStudent);
            setIsFormOpen(true);
          }
        }}
      />

      <ConfirmDialog
        isOpen={!!studentToDelete}
        onClose={() => setStudentToDelete(null)}
        onConfirm={handleDeleteConfirm}
        title="Confirm Student De-registration"
        message={`Are you sure you want to remove ${studentToDelete?.name} (${studentToDelete?.studentId}) from the hostel registry? This will free up their bed allocation.`}
        confirmText="Remove Resident"
        isLoading={isDeleting}
      />
    </div>
  );
};
