import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, UserRole } from '../types';
import { apiService, USE_REAL_BACKEND } from '../services/api';

interface AuthContextType {
  user: User | null;
  role: UserRole | null;
  isAuthenticated: boolean;
  isDemo: boolean;
  isLoading: boolean;
  login: (roleChoice: UserRole, credentials?: { email?: string; password?: string }) => Promise<void>;
  loginDemo: (roleChoice: UserRole) => Promise<void>;
  logout: () => void;
  switchRoleDemo: (role: UserRole) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const initializeAuth = () => {
      const savedUser = localStorage.getItem('hostelhub_user');
      const savedToken = localStorage.getItem('hostelhub_token');
      if (savedUser && savedToken) {
        setUser(JSON.parse(savedUser));
      } else if (import.meta.env.DEV && !USE_REAL_BACKEND) {
        // Default to student demo session for immediate preview readiness in mock mode only
        const defaultStudent = {
          id: 'usr-101',
          name: 'Rahul Sharma',
          email: 'rahul.sharma@hostelhub.edu',
          role: 'STUDENT' as UserRole,
          studentId: 'STU-2023-089',
          avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80',
        };
        setUser(defaultStudent);
        localStorage.setItem('hostelhub_user', JSON.stringify(defaultStudent));
        localStorage.setItem('hostelhub_token', 'mock-jwt-token-for-usr-101');
      } else {
        setUser(null);
      }
      setIsLoading(false);
    };

    initializeAuth();
  }, []);

  const login = async (roleChoice: UserRole, credentials?: { email?: string; password?: string }) => {
    setIsLoading(true);
    try {
      const response = await apiService.login(roleChoice, credentials);
      localStorage.setItem('hostelhub_token', response.token);
      localStorage.setItem('hostelhub_user', JSON.stringify(response.user));
      setUser(response.user);
    } finally {
      setIsLoading(false);
    }
  };

  const loginDemo = async (roleChoice: UserRole) => {
    setIsLoading(true);
    try {
      const response = await apiService.demoLogin(roleChoice);
      localStorage.setItem('hostelhub_token', response.token);
      localStorage.setItem('hostelhub_user', JSON.stringify(response.user));
      setUser(response.user);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('hostelhub_token');
    localStorage.removeItem('hostelhub_user');
    setUser(null);
  };

  const switchRoleDemo = async (newRole: UserRole) => {
    if (!import.meta.env.DEV || USE_REAL_BACKEND) {
      // In real backend or production mode, role cannot be switched without real authentication
      logout();
      return;
    }
    await login(newRole);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        role: user?.role ?? null,
        isAuthenticated: !!user,
        isDemo: Boolean(user?.demo),
        isLoading,
        login,
        loginDemo,
        logout,
        switchRoleDemo,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
};
