import React, { useState } from 'react';
import { Eye } from 'lucide-react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { DEMO_READ_ONLY_NOTICE } from './utils/demoAuth';
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
import { StudentNotices } from './pages/student/StudentNotices';
import { StudentSurveys } from './pages/student/StudentSurveys';

// Warden views
import { WardenDashboard } from './pages/warden/WardenDashboard';
import { StudentList } from './pages/warden/StudentList';
import { RoomList } from './pages/warden/RoomList';
import { WardenComplaints } from './pages/warden/WardenComplaints';
import { WardenTasks } from './pages/warden/WardenTasks';
import { WardenNotices } from './pages/warden/WardenNotices';
import { WardenProfile } from './pages/warden/WardenProfile';

// DGM views
import { DgmDashboard } from './pages/dgm/DgmDashboard';
import { DgmEscalations } from './pages/dgm/DgmEscalations';
import { DgmOverdue } from './pages/dgm/DgmOverdue';
import { DgmHostels } from './pages/dgm/DgmHostels';
import { DgmWardens } from './pages/dgm/DgmWardens';
import { DgmSurveys } from './pages/dgm/DgmSurveys';
import { DgmProfile } from './pages/dgm/DgmProfile';

/** Returns the default landing tab for each role. */
const defaultTab = (role: string | null): string => {
  if (role === 'WARDEN') return 'warden-dashboard';
  if (role === 'DGM') return 'dgm-dashboard';
  return 'student-dashboard';
};

const MainLayout: React.FC = () => {
  const { isAuthenticated, isDemo, role, logout } = useAuth();
  const [currentTab, setCurrentTab] = useState<string>(defaultTab(role));
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
  const [activeComplaintId, setActiveComplaintId] = useState<string | null>(null);

  // Sync tab when role changes (e.g. after login or demo switch)
  React.useEffect(() => {
    if (role === 'WARDEN' && !currentTab.startsWith('warden-')) {
      setCurrentTab('warden-dashboard');
    } else if (role === 'STUDENT' && !currentTab.startsWith('student-')) {
      setCurrentTab('student-dashboard');
    } else if (role === 'DGM' && !currentTab.startsWith('dgm-')) {
      setCurrentTab('dgm-dashboard');
    }
  }, [role]); // intentionally omit currentTab to avoid loop

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const getPageTitle = () => {
    switch (currentTab) {
      // Student
      case 'student-dashboard':       return 'Student Resident Dashboard';
      case 'student-room':            return 'My Room Allocation';
      case 'student-complaints':      return 'Complaints & Requests';
      case 'student-complaint-new':   return 'Submit Maintenance Complaint';
      case 'student-complaint-detail': return 'Complaint Ticket Details';
      case 'student-notices':         return 'Hostel Notices & Announcements';
      case 'student-surveys':         return 'Student Feedback Survey';
      case 'student-profile':         return 'Resident Profile';

      // Warden
      case 'warden-dashboard':        return 'Hostel Administration Dashboard';
      case 'warden-students':         return 'Resident Students Roster';
      case 'warden-rooms':            return 'Room Inventory & Occupancy';
      case 'warden-complaints':       return 'Complaints & Maintenance Desk';
      case 'warden-tasks':            return 'Feedback Action Tasks';
      case 'warden-notices':          return 'Publish Hostel Notices';
      case 'warden-profile':          return 'Warden Official Profile';

      // DGM
      case 'dgm-dashboard':           return 'DGM Overview Dashboard';
      case 'dgm-escalations':         return 'Escalation Management';
      case 'dgm-overdue':             return 'Overdue Complaints & Accountability';
      case 'dgm-hostels':             return 'Campus Hostels Management';
      case 'dgm-wardens':             return 'Warden Administration & Floors';
      case 'dgm-surveys':             return 'Quality Surveys & Analytics';
      case 'dgm-profile':             return 'DGM Profile';

      default: return 'HostelSync';
    }
  };

  const renderContent = () => {
    // ─── Student Routes ───────────────────────────────────────────
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
        case 'student-notices':
          return <StudentNotices />;
        case 'student-surveys':
          return <StudentSurveys />;
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

    // ─── Warden Routes ────────────────────────────────────────────
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
        case 'warden-tasks':
          return <WardenTasks />;
        case 'warden-notices':
          return <WardenNotices />;
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

    // ─── DGM Routes ───────────────────────────────────────────────
    if (role === 'DGM') {
      switch (currentTab) {
        case 'dgm-dashboard':
          return (
            <DgmDashboard
              onNavigate={(tab) => setCurrentTab(tab)}
            />
          );
        case 'dgm-escalations':
          return <DgmEscalations />;
        case 'dgm-overdue':
          return <DgmOverdue />;
        case 'dgm-hostels':
          return <DgmHostels />;
        case 'dgm-wardens':
          return <DgmWardens />;
        case 'dgm-surveys':
          return <DgmSurveys />;
        case 'dgm-profile':
          return <DgmProfile />;
        default:
          return (
            <DgmDashboard
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
            if (role === 'WARDEN') setCurrentTab('warden-profile');
            else if (role === 'DGM') setCurrentTab('dgm-profile');
            else setCurrentTab('student-profile');
          }}
        />

        {isDemo && (
          <div
            role="status"
            className="bg-amber-50 border-b border-amber-200 px-4 sm:px-6 lg:px-8 py-2.5 text-xs text-amber-900 flex flex-wrap items-center justify-between gap-2"
          >
            <div className="flex items-center space-x-2">
              <Eye className="w-4 h-4 text-amber-700 shrink-0" />
              <span>
                <strong className="font-semibold">Read-Only Demo Mode ({role})</strong> —{' '}
                {DEMO_READ_ONLY_NOTICE.replace(/^Read-Only Demo Mode:\s*/i, '')}
              </span>
            </div>
            <button
              type="button"
              onClick={logout}
              className="text-xs font-semibold text-amber-900 underline hover:text-amber-700 cursor-pointer"
            >
              Exit Demo
            </button>
          </div>
        )}

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
