import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { LoginPage } from './pages/Login/LoginPage';
import { Sidebar } from './components/layout/Sidebar';
import { Header } from './components/layout/Header';

// Student views
import { StudentDashboard } from './pages/student/StudentDashboard';
import { StudentRoom } from './pages/student/StudentRoom';
import { StudentComplaints } from './pages/student/StudentComplaints';
import { ComplaintNew } from './pages/student/ComplaintNew';
import { ComplaintDetail } from './pages/student/ComplaintDetail';
import { StudentProfile } from './pages/student/StudentProfile';

// Warden views
import { WardenDashboard } from './pages/warden/WardenDashboard';
import { StudentList } from './pages/warden/StudentList';
import { RoomList } from './pages/warden/RoomList';
import { WardenComplaints } from './pages/warden/WardenComplaints';
import { WardenProfile } from './pages/warden/WardenProfile';

const MainLayout: React.FC = () => {
  const { isAuthenticated, role } = useAuth();
  const [currentTab, setCurrentTab] = useState<string>(
    role === 'WARDEN' ? 'warden-dashboard' : 'student-dashboard'
  );
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
  const [activeComplaintId, setActiveComplaintId] = useState<string | null>(null);

  // Sync tab if user switches role
  React.useEffect(() => {
    if (role === 'WARDEN' && !currentTab.startsWith('warden-')) {
      setCurrentTab('warden-dashboard');
    } else if (role === 'STUDENT' && !currentTab.startsWith('student-')) {
      setCurrentTab('student-dashboard');
    }
  }, [role, currentTab]);

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const getPageTitle = () => {
    switch (currentTab) {
      case 'student-dashboard':
        return 'Student Resident Dashboard';
      case 'student-room':
        return 'My Room Allocation';
      case 'student-complaints':
        return 'Complaints & Requests';
      case 'student-complaint-new':
        return 'Submit Maintenance Complaint';
      case 'student-complaint-detail':
        return 'Complaint Ticket Details';
      case 'student-profile':
        return 'Resident Profile';

      case 'warden-dashboard':
        return 'Hostel Administration Dashboard';
      case 'warden-students':
        return 'Resident Students Roster';
      case 'warden-rooms':
        return 'Room Inventory & Occupancy';
      case 'warden-complaints':
        return 'Complaints & Maintenance Desk';
      case 'warden-profile':
        return 'Warden Official Profile';
      default:
        return 'HostelHub';
    }
  };

  const renderContent = () => {
    // Student Routes
    if (role === 'STUDENT') {
      switch (currentTab) {
        case 'student-dashboard':
          return (
            <StudentDashboard
              onNavigate={(tab, complaintId) => {
                if (complaintId) setActiveComplaintId(complaintId);
                setCurrentTab(tab);
              }}
            />
          );
        case 'student-room':
          return <StudentRoom />;
        case 'student-complaints':
          return (
            <StudentComplaints
              onNavigateNew={() => setCurrentTab('student-complaint-new')}
              onViewDetail={(id) => {
                setActiveComplaintId(id);
                setCurrentTab('student-complaint-detail');
              }}
            />
          );
        case 'student-complaint-new':
          return (
            <ComplaintNew
              onBack={() => setCurrentTab('student-complaints')}
              onSuccess={(id) => {
                setActiveComplaintId(id);
                setCurrentTab('student-complaint-detail');
              }}
            />
          );
        case 'student-complaint-detail':
          return (
            <ComplaintDetail
              complaintId={activeComplaintId || 'cmp-1024'}
              onBack={() => setCurrentTab('student-complaints')}
            />
          );
        case 'student-profile':
          return <StudentProfile />;
        default:
          return (
            <StudentDashboard
              onNavigate={(tab) => setCurrentTab(tab)}
            />
          );
      }
    }

    // Warden Routes
    if (role === 'WARDEN') {
      switch (currentTab) {
        case 'warden-dashboard':
          return (
            <WardenDashboard
              onNavigate={(tab, complaintId) => {
                if (typeof complaintId === 'string') setActiveComplaintId(complaintId);
                setCurrentTab(tab);
              }}
            />
          );
        case 'warden-students':
          return <StudentList />;
        case 'warden-rooms':
          return <RoomList />;
        case 'warden-complaints':
          return <WardenComplaints initialComplaintId={activeComplaintId} />;
        case 'warden-profile':
          return <WardenProfile />;
        default:
          return (
            <WardenDashboard
              onNavigate={(tab) => setCurrentTab(tab)}
            />
          );
      }
    }

    return null;
  };

  return (
    <div className="min-h-screen bg-slate-50 flex">
      {/* Sidebar */}
      <Sidebar
        currentTab={currentTab}
        onSelectTab={(tab) => {
          setActiveComplaintId(null);
          setCurrentTab(tab);
        }}
        isMobileOpen={isMobileNavOpen}
        onMobileClose={() => setIsMobileNavOpen(false)}
      />

      {/* Main Content Area */}
      <div className="flex-1 lg:pl-64 flex flex-col min-w-0">
        <Header
          pageTitle={getPageTitle()}
          onOpenMobileMenu={() => setIsMobileNavOpen(true)}
          onNavigateProfile={() => {
            setCurrentTab(role === 'WARDEN' ? 'warden-profile' : 'student-profile');
          }}
        />

        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          {renderContent()}
        </main>
      </div>
    </div>
  );
};

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <MainLayout />
      </AuthProvider>
    </ToastProvider>
  );
}
