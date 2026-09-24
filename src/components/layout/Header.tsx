import React, { useState, useEffect } from 'react';
import { Bell, Menu, Search, Check } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { apiService } from '../../services/api';
import { NotificationItem } from '../../types';

interface HeaderProps {
  pageTitle: string;
  onOpenMobileMenu: () => void;
  onNavigateProfile?: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  pageTitle,
  onOpenMobileMenu,
  onNavigateProfile,
}) => {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);

  useEffect(() => {
    apiService.getNotifications().then(setNotifications);
  }, []);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const markAllAsRead = () => {
    notifications.forEach((n) => apiService.markNotificationAsRead(n.id));
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  return (
    <header className="h-16 bg-white border-b border-slate-200 sticky top-0 z-30 flex items-center justify-between px-4 sm:px-6">
      <div className="flex items-center space-x-3">
        <button
          onClick={onOpenMobileMenu}
          className="lg:hidden text-slate-500 hover:text-slate-800 p-1.5 rounded-lg hover:bg-slate-100 cursor-pointer"
        >
          <Menu className="w-5 h-5" />
        </button>
        <h1 className="text-lg font-semibold text-slate-800">{pageTitle}</h1>
      </div>

      <div className="flex items-center space-x-3 sm:space-x-4">
        {/* Search bar */}
        <div className="relative hidden md:block w-56 lg:w-72">
          <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
          <input
            type="text"
            placeholder="Search records, rooms, complaints..."
            className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-blue-500 focus:bg-white text-slate-800"
          />
        </div>

        {/* Notification Dropdown */}
        <div className="relative">
          <button
            onClick={() => setShowNotifications(!showNotifications)}
            className="relative p-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
          >
            <Bell className="w-5 h-5" />
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-red-600 ring-2 ring-white" />
            )}
          </button>

          {showNotifications && (
            <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-xl shadow-lg border border-slate-200 py-2 z-50">
              <div className="px-4 py-2 border-b border-slate-100 flex items-center justify-between">
                <div>
                  <h4 className="text-sm font-semibold text-slate-900">Notifications</h4>
                  <p className="text-xs text-slate-400">{unreadCount} unread alerts</p>
                </div>
                {unreadCount > 0 && (
                  <button
                    onClick={markAllAsRead}
                    className="text-xs text-blue-600 hover:underline flex items-center cursor-pointer"
                  >
                    <Check className="w-3.5 h-3.5 mr-1" /> Mark all read
                  </button>
                )}
              </div>
              <div className="max-h-72 overflow-y-auto divide-y divide-slate-50">
                {notifications.map((notif) => (
                  <div
                    key={notif.id}
                    className={`p-3.5 hover:bg-slate-50 text-left transition-colors ${
                      !notif.read ? 'bg-blue-50/40' : ''
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-semibold text-slate-800">{notif.title}</span>
                      <span className="text-[10px] text-slate-400">{notif.timestamp}</span>
                    </div>
                    <p className="text-xs text-slate-600 leading-relaxed">{notif.message}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* User preview */}
        <div
          onClick={onNavigateProfile}
          className="flex items-center space-x-2 pl-2 border-l border-slate-200 cursor-pointer group"
        >
          <img
            src={user?.avatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80'}
            alt="User"
            className="w-8 h-8 rounded-full border border-slate-200 object-cover"
          />
          <div className="hidden sm:block text-left">
            <span className="block text-xs font-semibold text-slate-800 group-hover:text-blue-600 transition-colors">
              {user?.name}
            </span>
            <span className="block text-[10px] text-slate-400 uppercase tracking-wider">
              {user?.role}
            </span>
          </div>
        </div>
      </div>
    </header>
  );
};
