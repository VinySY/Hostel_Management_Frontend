import React, { useState } from 'react';
import { Eye, EyeOff, Layers, Lock, Mail, ShieldCheck, UserCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { useToast } from '../../context/ToastContext';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const { showToast } = useToast();

  const [email, setEmail] = useState('rahul.sharma@hostelhub.edu');
  const [password, setPassword] = useState('password123');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedRole, setSelectedRole] = useState<'STUDENT' | 'WARDEN'>('STUDENT');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      showToast('Please enter both email and password', 'error');
      return;
    }

    setIsLoading(true);
    try {
      await login(selectedRole, { email, password });
      showToast(`Welcome back! Logged in as ${selectedRole.toLowerCase()}.`, 'success');
    } catch {
      showToast('Login failed. Please check your credentials.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDemoSelect = (role: 'STUDENT' | 'WARDEN') => {
    setSelectedRole(role);
    if (role === 'STUDENT') {
      setEmail('rahul.sharma@hostelhub.edu');
      setPassword('studentPass2026');
    } else {
      setEmail('warden.vance@hostelhub.edu');
      setPassword('wardenPass2026');
    }
  };

  return (
    <div className="min-h-screen flex flex-col md:flex-row bg-slate-50">
      {/* Left side brand presentation */}
      <div className="w-full md:w-1/2 bg-gradient-to-br from-blue-700 to-indigo-900 text-white p-8 md:p-14 flex flex-col justify-between">
        <div>
          <div className="flex items-center space-x-3 mb-8">
            <div className="w-10 h-10 rounded-xl bg-white/10 backdrop-blur-md flex items-center justify-center text-white border border-white/20">
              <Layers className="w-6 h-6 text-white" />
            </div>
            <div>
              <span className="text-2xl font-bold tracking-tight text-white">HostelHub</span>
              <p className="text-xs text-blue-200">Smart Hostel Management System</p>
            </div>
          </div>

          <div className="max-w-md my-auto pt-6">
            <h2 className="text-3xl sm:text-4xl font-extrabold leading-tight">
              Streamlined living, campus safety, and frictionless hostel administration.
            </h2>
            <p className="mt-4 text-blue-100 text-sm leading-relaxed">
              Designed for modern universities and residential campuses to manage rooms, complaints,
              and resident life effortlessly.
            </p>
          </div>
        </div>

        {/* Feature Highlights */}
        <div className="mt-8 pt-8 border-t border-white/15 grid grid-cols-2 gap-4 text-xs text-blue-100">
          <div>
            <span className="font-semibold text-white block mb-0.5">Real-time Complaints</span>
            Photo evidence, status tracking & resolution workflows.
          </div>
          <div>
            <span className="font-semibold text-white block mb-0.5">Room Inventory</span>
            Instant vacancy tracking, roommate coordination & allocation.
          </div>
        </div>
      </div>

      {/* Right side form */}
      <div className="w-full md:w-1/2 flex items-center justify-center p-6 sm:p-10">
        <div className="w-full max-w-md">
          <div className="mb-6">
            <h3 className="text-2xl font-bold text-slate-900">Sign in to HostelHub</h3>
            <p className="text-sm text-slate-500 mt-1">
              Enter your credentials to access your portal
            </p>
          </div>

          {/* Quick Demo Selector */}
          <div className="mb-6 p-3.5 bg-blue-50 border border-blue-100 rounded-xl">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-blue-900 flex items-center">
                <ShieldCheck className="w-3.5 h-3.5 mr-1 text-blue-600" /> Fast Demo Access:
              </span>
              <span className="text-[10px] text-blue-700 bg-blue-200/50 px-2 py-0.5 rounded font-mono">
                Development Only
              </span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleDemoSelect('STUDENT')}
                className={`py-1.5 px-3 rounded-lg text-xs font-medium border flex items-center justify-center space-x-1.5 transition-colors cursor-pointer ${
                  selectedRole === 'STUDENT'
                    ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                    : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'
                }`}
              >
                <UserCheck className="w-3.5 h-3.5" />
                <span>Student Demo</span>
              </button>
              <button
                type="button"
                onClick={() => handleDemoSelect('WARDEN')}
                className={`py-1.5 px-3 rounded-lg text-xs font-medium border flex items-center justify-center space-x-1.5 transition-colors cursor-pointer ${
                  selectedRole === 'WARDEN'
                    ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                    : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'
                }`}
              >
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>Warden Demo</span>
              </button>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Email or Student ID"
              type="text"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. rahul.sharma@hostelhub.edu"
              leftIcon={<Mail className="w-4 h-4" />}
              required
            />

            <div>
              <Input
                label="Password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                leftIcon={<Lock className="w-4 h-4" />}
                rightIcon={
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="text-slate-400 hover:text-slate-600 focus:outline-none cursor-pointer"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                }
                required
              />
            </div>

            <div className="flex items-center justify-between text-xs">
              <label className="flex items-center text-slate-600 cursor-pointer">
                <input
                  type="checkbox"
                  defaultChecked
                  className="rounded border-slate-300 text-blue-600 focus:ring-blue-500 mr-2"
                />
                Remember me
              </label>
              <a
                href="#forgot"
                onClick={(e) => {
                  e.preventDefault();
                  showToast('Password reset link sent to registered email address.', 'info');
                }}
                className="text-blue-600 hover:underline font-medium"
              >
                Forgot password?
              </a>
            </div>

            <Button type="submit" variant="primary" className="w-full mt-2" isLoading={isLoading}>
              Sign In to HostelHub
            </Button>
          </form>

          <div className="mt-8 text-center text-xs text-slate-400">
            Prepared for Spring Boot REST API integration (<code className="font-mono text-slate-500">/api/auth/login</code>)
          </div>
        </div>
      </div>
    </div>
  );
};
