import React from 'react';
import {
  LayoutDashboard,
  Home,
  AlertCircle,
  PlusCircle,
  Users,
  Building,
  User as UserIcon,
  LogOut,
  X,
  Shield,
  Layers,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface SidebarProps {
  currentTab: string;
  onSelectTab: (tab: string) => void;
  isMobileOpen: boolean;
  onMobileClose: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentTab,
  onSelectTab,
  isMobileOpen,
  onMobileClose,
}) => {
  const { role, logout, user, switchRoleDemo } = useAuth();

  const studentNavItems = [
    { id: 'student-dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'student-room', label: 'My Room', icon: Home },
    { id: 'student-complaints', label: 'My Complaints', icon: AlertCircle },
    { id: 'student-complaint-new', label: 'Submit Complaint', icon: PlusCircle },
    { id: 'student-profile', label: 'Profile', icon: UserIcon },
  ];

  const wardenNavItems = [
    { id: 'warden-dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'warden-students', label: 'Students', icon: Users },
    { id: 'warden-rooms', label: 'Rooms', icon: Building },
    { id: 'warden-complaints', label: 'Complaints', icon: AlertCircle },
    { id: 'warden-profile', label: 'Profile', icon: UserIcon },
  ];

  const navItems = role === 'WARDEN' ? wardenNavItems : studentNavItems;

  const handleNavClick = (id: string) => {
    onSelectTab(id);
    onMobileClose();
  };

  return (
    <>
      {/* Mobile backdrop */}
      {isMobileOpen && (
        <div
          className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-40 lg:hidden"
          onClick={onMobileClose}
        />
      )}

      {/* Sidebar container */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-50 w-64 bg-white border-r border-slate-200 flex flex-col transition-transform duration-300 lg:translate-x-0 ${
          isMobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand header */}
        <div className="h-16 px-6 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold shadow-sm">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold text-slate-900 tracking-tight text-lg">HostelHub</span>
              <p className="text-[10px] text-slate-400 font-medium tracking-wide uppercase">Smart System</p>
            </div>
          </div>
          <button
            onClick={onMobileClose}
            className="lg:hidden text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* User Role Badge */}
        <div className="px-6 py-3 bg-slate-50 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Shield className="w-4 h-4 text-blue-600" />
            <span className="text-xs font-semibold text-slate-700 uppercase tracking-wider">
              {role === 'WARDEN' ? 'Warden Portal' : 'Student Portal'}
            </span>
          </div>
          <button
            onClick={() => switchRoleDemo(role === 'WARDEN' ? 'STUDENT' : 'WARDEN')}
            className="text-[11px] text-blue-600 hover:underline font-medium cursor-pointer"
            title="Switch between student and warden view for demo"
          >
            Switch to {role === 'WARDEN' ? 'Student' : 'Warden'}
          </button>
        </div>

        {/* Navigation list */}
        <div className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = currentTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => handleNavClick(item.id)}
                className={`w-full flex items-center space-x-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-colors cursor-pointer ${
                  isActive
                    ? 'bg-blue-50 text-blue-600 font-semibold'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                }`}
              >
                <Icon className={`w-5 h-5 ${isActive ? 'text-blue-600' : 'text-slate-400'}`} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>

        {/* Footer with logged in profile info & Logout */}
        <div className="p-4 border-t border-slate-100 bg-slate-50/50">
          <div className="flex items-center space-x-3 mb-3">
            <img
              src={user?.avatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80'}
              alt={user?.name || 'User'}
              className="w-9 h-9 rounded-full object-cover border border-slate-200"
            />
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-slate-800 truncate">{user?.name}</p>
              <p className="text-[11px] text-slate-400 truncate">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={logout}
            className="w-full flex items-center justify-center space-x-2 text-xs font-medium text-slate-600 hover:text-red-600 hover:bg-red-50 p-2 rounded-lg transition-colors border border-slate-200 cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>
    </>
  );
};
