import React, { createContext, useContext, useState, useCallback } from 'react';
import { AlertCircle, CheckCircle, Info, X, XCircle } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

interface ToastItem {
  id: string;
  type: ToastType;
  message: string;
}

interface ToastContextType {
  showToast: (message: string, type?: ToastType) => void;
  removeToast: (id: string) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback((message: string, type: ToastType = 'info') => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, type, message }]);

    setTimeout(() => {
      removeToast(id);
    }, 4000);
  }, [removeToast]);

  return (
    <ToastContext.Provider value={{ showToast, removeToast }}>
      {children}
      {/* Toast Notification Container */}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col space-y-2 pointer-events-none max-w-sm w-full px-4">
        {toasts.map((toast) => {
          const config = {
            success: { bg: 'bg-emerald-50 border-emerald-200 text-emerald-900', icon: CheckCircle, iconColor: 'text-emerald-600' },
            error: { bg: 'bg-rose-50 border-rose-200 text-rose-900', icon: XCircle, iconColor: 'text-rose-600' },
            warning: { bg: 'bg-amber-50 border-amber-200 text-amber-900', icon: AlertCircle, iconColor: 'text-amber-600' },
            info: { bg: 'bg-blue-50 border-blue-200 text-blue-900', icon: Info, iconColor: 'text-blue-600' },
          }[toast.type];

          const IconComponent = config.icon;

          return (
            <div
              key={toast.id}
              className={`pointer-events-auto flex items-start p-3.5 rounded-lg border shadow-lg transition-all animate-slide-in ${config.bg}`}
            >
              <IconComponent className={`w-5 h-5 mr-3 mt-0.5 flex-shrink-0 ${config.iconColor}`} />
              <p className="text-sm font-medium flex-1 leading-snug">{toast.message}</p>
              <button
                onClick={() => removeToast(toast.id)}
                className="ml-2 text-slate-400 hover:text-slate-600 focus:outline-none"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used within ToastProvider');
  return context;
};
