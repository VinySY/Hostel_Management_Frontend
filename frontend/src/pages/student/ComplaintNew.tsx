import React, { useState } from 'react';
import { ArrowLeft, Send } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { FileUploader } from '../../components/common/FileUploader';
import { apiService } from '../../services/api';
import { ComplaintType } from '../../types';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';

interface ComplaintNewProps {
  onBack: () => void;
  onSuccess: (complaintId: string) => void;
}

export const ComplaintNew: React.FC<ComplaintNewProps> = ({ onBack, onSuccess }) => {
  const { user, isDemo } = useAuth();
  const { showToast } = useToast();

  const [type, setType] = useState<ComplaintType>('Mess');
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [location, setLocation] = useState('Room 204, Block A');
  const [files, setFiles] = useState<File[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const validate = () => {
    const err: Record<string, string> = {};
    if (!subject.trim()) err.subject = 'Subject is required';
    if (!description.trim() || description.length < 15) {
      err.description = 'Please describe the issue thoroughly (minimum 15 characters)';
    }
    if (!location.trim()) err.location = 'Specific location is required';
    setErrors(err);
    return Object.keys(err).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      const created = await apiService.createComplaint(
        {
          type,
          subject,
          description,
          location,
          studentId: user?.studentId || 'STU-2023-089',
          studentName: user?.name || 'Rahul Sharma',
          roomNumber: '204',
          block: 'Block A',
        },
        files
      );

      showToast(`Complaint #${created.complaintNumber} lodged successfully!`, 'success');
      onSuccess(created.id);
    } catch (err) {
      const msg = err instanceof Error && err.message ? err.message : 'Failed to submit complaint. Please retry.';
      showToast(msg, 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center space-x-3">
        <Button variant="ghost" size="sm" icon={<ArrowLeft className="w-4 h-4" />} onClick={onBack}>
          Back
        </Button>
        <div>
          <h2 className="text-xl font-bold text-slate-900">Submit a Complaint</h2>
          <p className="text-xs text-slate-500">Provide complete details to expedite resolution by hostel authorities</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card title="Complaint Information">
          <div className="space-y-4">
            <Select
              label="Complaint Category"
              value={type}
              onChange={(e) => setType(e.target.value as ComplaintType)}
              options={[
                { value: 'Mess', label: 'Mess & Food Quality' },
                { value: 'Cleaning', label: 'Cleaning & Housekeeping' },
                { value: 'Room Maintenance', label: 'Room Maintenance & Carpentry' },
                { value: 'Electricity', label: 'Electricity & Appliances' },
                { value: 'Water', label: 'Water Supply' },
                { value: 'Plumbing', label: 'Plumbing & Washrooms' },
                { value: 'Other', label: 'Other Concerns' },
              ]}
              required
            />

            <Input
              label="Subject / Headline"
              placeholder="e.g. Ceiling fan regulator sparking and overheating"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              error={errors.subject}
              required
            />

            <Input
              label="Specific Location"
              placeholder="e.g. Block A, Room 204, Bed 1 switchboard"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              error={errors.location}
              helperText="Specify exact room, corridor, or dining section to help technicians locate it"
              required
            />

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Detailed Description <span className="text-red-500">*</span>
              </label>
              <textarea
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Explain the problem clearly. Mention since when you have been facing this issue..."
                className={`w-full rounded-lg border text-sm p-3.5 focus:outline-none focus:ring-2 ${
                  errors.description
                    ? 'border-red-300 focus:ring-red-200'
                    : 'border-slate-300 focus:border-blue-500 focus:ring-blue-100'
                }`}
              />
              {errors.description && (
                <p className="mt-1 text-xs text-red-600">{errors.description}</p>
              )}
            </div>
          </div>
        </Card>

        {/* Evidence Attachment */}
        <Card
          title="Evidence & Attachments (Optional)"
          subtitle="Add photos or video to help authorities diagnose the problem immediately"
        >
          <FileUploader files={files} onChange={setFiles} maxFiles={4} />
          <p className="text-[11px] text-slate-400 mt-2">
            Files will be transmitted as standard multipart/form-data payload compatible with Spring Boot endpoint.
          </p>
        </Card>

        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-end gap-3">
          {isDemo && (
            <span className="text-xs text-amber-800 bg-amber-50 border border-amber-200 px-3 py-1.5 rounded-lg">
              Submitting new complaints is disabled in Read-Only Demo Mode.
            </span>
          )}
          <div className="flex items-center justify-end space-x-3">
            <Button type="button" variant="outline" onClick={onBack} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              icon={<Send className="w-4 h-4" />}
              isLoading={isSubmitting}
              disabled={isDemo}
            >
              Submit Complaint
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
};
