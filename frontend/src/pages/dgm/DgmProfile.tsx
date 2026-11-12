import React, { useEffect, useState } from 'react';
import { User, Mail, Building2, Phone, Shield, Loader2, AlertTriangle } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { ProtectedImage } from '../../components/common/ProtectedImage';
import { apiService } from '../../services/api';

interface DgmProfileData {
  id: string;
  name: string;
  email: string;
  role: string;
  employeeId?: string;
  designation?: string;
  officeLocation?: string;
  contactExtension?: string;
  avatar?: string;
}

export const DgmProfile: React.FC = () => {
  const { user } = useAuth();
  const [profile, setProfile] = useState<DgmProfileData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProfile = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await apiService.getDgmProfile();
        setProfile({
          id: String(data.id ?? user?.id ?? ''),
          name: data.name ?? user?.name ?? '',
          email: data.email ?? user?.email ?? '',
          role: 'DGM',
          employeeId: data.employeeId,
          designation: data.designation,
          officeLocation: data.officeLocation,
          contactExtension: data.contactExtension,
          avatar: data.avatarUrl ?? user?.avatar,
        });
      } catch (err) {
        console.error('DGM profile fetch error:', err);
        // Fallback to auth context data
        if (user) {
          setProfile({
            id: user.id,
            name: user.name,
            email: user.email,
            role: 'DGM',
          });
        } else {
          setError('Could not load DGM profile.');
        }
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [user]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-7 h-7 text-blue-600 animate-spin" />
        <span className="ml-3 text-slate-600 text-sm">Loading profile…</span>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center">
        <AlertTriangle className="w-8 h-8 text-red-500 mx-auto mb-2" />
        <p className="text-sm text-red-700">{error ?? 'Profile unavailable.'}</p>
      </div>
    );
  }

  const initials = profile.name
    .split(' ')
    .map((n) => n[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  const fields: { label: string; value?: string; icon: React.ReactNode }[] = [
    { label: 'Email', value: profile.email, icon: <Mail className="w-4 h-4 text-slate-400" /> },
    { label: 'Employee ID', value: profile.employeeId, icon: <Shield className="w-4 h-4 text-slate-400" /> },
    { label: 'Designation', value: profile.designation, icon: <User className="w-4 h-4 text-slate-400" /> },
    { label: 'Office Location', value: profile.officeLocation, icon: <Building2 className="w-4 h-4 text-slate-400" /> },
    { label: 'Contact Extension', value: profile.contactExtension, icon: <Phone className="w-4 h-4 text-slate-400" /> },
  ];

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Avatar & header */}
      <div className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
        <div className="bg-gradient-to-r from-indigo-700 to-blue-600 h-28" />
        <div className="px-6 pb-6">
          <div className="-mt-12 flex items-end space-x-4 mb-4">
            <ProtectedImage
              src={profile.avatar}
              alt={profile.name}
              className="w-20 h-20 rounded-2xl border-4 border-white object-cover shadow"
              fallbackNode={
                <div className="w-20 h-20 rounded-2xl border-4 border-white bg-indigo-600 flex items-center justify-center text-white text-2xl font-bold shadow">
                  {initials}
                </div>
              }
            />
            <div className="pb-1">
              <h2 className="text-xl font-bold text-slate-900">{profile.name}</h2>
              <div className="flex items-center space-x-2 mt-1">
                <span className="text-xs font-semibold bg-indigo-100 text-indigo-700 px-2.5 py-0.5 rounded-full">
                  Director General of Maintenance
                </span>
              </div>
            </div>
          </div>

          <div className="space-y-3">
            {fields.filter((f) => f.value).map((f) => (
              <div key={f.label} className="flex items-center space-x-3 py-2.5 border-b border-slate-50 last:border-0">
                <div className="w-8 h-8 bg-slate-50 rounded-lg flex items-center justify-center flex-shrink-0">
                  {f.icon}
                </div>
                <div>
                  <p className="text-[11px] text-slate-400 font-medium uppercase tracking-wide">{f.label}</p>
                  <p className="text-sm font-medium text-slate-800">{f.value}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Security note */}
      <div className="bg-blue-50 border border-blue-100 rounded-xl px-4 py-3 flex items-start space-x-3">
        <Shield className="w-4 h-4 text-blue-600 mt-0.5 flex-shrink-0" />
        <div className="text-xs text-blue-700">
          <p className="font-semibold mb-0.5">Authenticated via JWT</p>
          <p>Your session is role-validated by the backend. Hostel access is enforced server-side based on your DGM assignment.</p>
        </div>
      </div>
    </div>
  );
};
