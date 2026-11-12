import {
  AuthResponse,
  Complaint,
  ComplaintPriority,
  ComplaintStatus,
  ComplaintType,
  DashboardStats,
  NotificationItem,
  Room,
  RoomFurnitureItem,
  Student,
  User,
  UserRole,
  SlaConfiguration,
  ComplaintAssignment,
  Escalation,
  DgmDashboardResponse,
  AuditLog,
  Hostel,
  Warden,
  Notice,
  NoticeCategory,
  Survey,
  SurveyAnalytics,
  SurveySubmissionData,
  FeedbackTask,
  FurnitureHistoryItem,
} from '../types';
import {
  mockComplaints,
  mockNotifications,
  mockRooms,
  mockStudents,
  mockUsers,
} from '../mock/data';
import { fetchProtectedFileBlob } from '../utils/fileAccess';
import { submitCreateWardenRequest } from '../utils/wardenForm';

/**
 * Dynamically resolves the Spring Boot REST API base URL:
 * - Uses VITE_API_BASE_URL when configured (stripping trailing slashes).
 * - In production without VITE_API_BASE_URL, logs an explicit configuration error.
 * - In development, falls back to window.location.hostname with port 8080.
 */
export const getApiBaseUrl = (): string => {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (envUrl && typeof envUrl === 'string' && envUrl.trim() !== '') {
    return envUrl.trim().replace(/\/+$/, '');
  }
  if (import.meta.env.PROD) {
    console.error('VITE_API_BASE_URL is not configured for production build.');
    return '/api';
  }
  if (typeof window !== 'undefined' && window.location && window.location.hostname) {
    const protocol = window.location.protocol || 'http:';
    return `${protocol}//${window.location.hostname}:8080/api`;
  }
  return 'http://localhost:8080/api';
};

const API_BASE_URL = getApiBaseUrl();

/**
 * Enables real Spring Boot JWT API requests when VITE_USE_REAL_BACKEND=true or in production builds.
 */
export const USE_REAL_BACKEND =
  import.meta.env.VITE_USE_REAL_BACKEND === 'true' || import.meta.env.PROD;

interface RawRoomFurniture {
  type?: RoomFurnitureItem['furnitureType'];
  furnitureType?: RoomFurnitureItem['furnitureType'];
  required?: number;
  requiredCount?: number;
  serviceable?: number;
  serviceableCount?: number;
  damaged?: number;
  damagedCount?: number;
  missing?: number;
  missingCount?: number;
  underRepair?: number;
  underRepairCount?: number;
  shortage?: number;
}

type RawRoom = Omit<Room, 'furnitureInventory'> & {
  furnitureInventory?: RawRoomFurniture[];
};

const normalizeRoom = (room: RawRoom): Room => ({
  ...room,
  furnitureInventory: room.furnitureInventory?.map((item) => ({
    furnitureType: item.furnitureType ?? item.type ?? 'BED',
    requiredCount: item.requiredCount ?? item.required ?? 0,
    serviceableCount: item.serviceableCount ?? item.serviceable ?? 0,
    damagedCount: item.damagedCount ?? item.damaged ?? 0,
    missingCount: item.missingCount ?? item.missing ?? 0,
    underRepairCount: item.underRepairCount ?? item.underRepair ?? 0,
    shortage: item.shortage ?? 0,
  })),
});

export interface DgmProfile {
  id: string;
  name: string;
  email: string;
  employeeId?: string;
  designation?: string;
  officeLocation?: string;
  contactExtension?: string;
  avatarUrl?: string;
}

const getAuthHeaders = (): HeadersInit => {
  const token = localStorage.getItem('hostelhub_token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
};

const simulateDelay = (ms: number = 300) =>
  new Promise((resolve) => setTimeout(resolve, ms));

// In-memory state for local mock mutation
let localComplaints: Complaint[] = [...mockComplaints];
let localStudents: Student[] = [...mockStudents];
let localRooms: Room[] = [...mockRooms];
let localNotifications: NotificationItem[] = [...mockNotifications];

export const apiService = {
  // Authentication
  async login(roleChoice: UserRole, credentials?: { email?: string; password?: string }): Promise<AuthResponse> {
    if (USE_REAL_BACKEND) {
      const email = credentials?.email?.trim();
      const password = credentials?.password;

      if (!email || !password) {
        throw new Error('Please enter both email and password.');
      }

      let response: Response;
      try {
        response = await fetch(`${API_BASE_URL}/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password }),
        });
      } catch {
        throw new Error(
          `Unable to reach the authentication server at ${API_BASE_URL}. Please verify the backend is running.`
        );
      }

      if (!response.ok) {
        // Surface the backend's actual message instead of replacing every
        // failure (401, 400, 500, CORS/network) with a credentials error.
        let backendMessage = '';
        try {
          const errorBody = await response.json();
          backendMessage = typeof errorBody?.message === 'string' ? errorBody.message.trim() : '';
        } catch {
          // Response was not JSON; fall through to a status-based message.
        }

        if (response.status === 401) {
          throw new Error(backendMessage || 'Invalid email or password.');
        }
        if (response.status === 400) {
          throw new Error(backendMessage || 'The login request is invalid.');
        }
        if (response.status >= 500) {
          throw new Error('The server could not process the login request. Please try again.');
        }
        throw new Error(backendMessage || `Login failed (HTTP ${response.status}).`);
      }

      return response.json();
    }

    await simulateDelay(400);
    const user = roleChoice === 'DGM' ? mockUsers.dgm : (roleChoice === 'WARDEN' ? mockUsers.warden : mockUsers.student);
    const token = `mock-jwt-token-for-${user.id}`;
    return { token, user };
  },

  async getCurrentUser(): Promise<User> {
    if (USE_REAL_BACKEND) {
      const response = await fetch(`${API_BASE_URL}/auth/me`, { headers: getAuthHeaders() });
      if (!response.ok) throw new Error('Failed to retrieve user profile');
      return response.json();
    }
    await simulateDelay(150);
    const saved = localStorage.getItem('hostelhub_user');
    return saved ? JSON.parse(saved) : mockUsers.student;
  },

  // Student Profile
  async getStudentProfile(studentId?: string): Promise<Student> {
    if (USE_REAL_BACKEND) {
      const endpoint = studentId && studentId.trim() ? `${API_BASE_URL}/students/${studentId}` : `${API_BASE_URL}/students/me`;
      const res = await fetch(endpoint, { headers: getAuthHeaders() });
      if (!res.ok) {
        throw new Error(`Failed to load student profile: ${res.status}`);
      }
      return res.json();
    }
    await simulateDelay(200);
    const target = studentId || 'STU-2023-089';
    const found = localStudents.find((s) => s.studentId === target || s.id === target);
    if (!found) throw new Error('Student profile not found');
    return found;
  },

  async uploadStudentPhoto(studentDbId: string | number, file: File): Promise<Student> {
    if (USE_REAL_BACKEND) {
      const formData = new FormData();
      formData.append('file', file);
      const token = localStorage.getItem('hostelhub_token') || localStorage.getItem('hostelsync_token');
      const res = await fetch(`${API_BASE_URL}/students/${studentDbId}/photo`, {
        method: 'POST',
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: formData,
      });
      if (!res.ok) throw new Error('Failed to upload student photo');
      return res.json();
    }
    await simulateDelay(300);
    const idStr = String(studentDbId);
    const idx = localStudents.findIndex((s) => s.id === idStr || s.studentId === idStr);
    if (idx !== -1) {
      localStudents[idx].avatarUrl = URL.createObjectURL(file);
      return localStudents[idx];
    }
    return localStudents[0];
  },

  async updateStudentProfile(studentId: string, updates: Partial<Student>): Promise<Student> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/students/${studentId}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(updates),
      });
      return res.json();
    }
    await simulateDelay(300);
    const index = localStudents.findIndex((s) => s.studentId === studentId || s.id === studentId);
    if (index === -1) throw new Error('Student not found');
    localStudents[index] = { ...localStudents[index], ...updates };
    return localStudents[index];
  },

  // Rooms
  async getRooms(): Promise<Room[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/rooms`, { headers: getAuthHeaders() });
      const data: RawRoom[] = await res.json();
      return data.map(normalizeRoom);
    }
    await simulateDelay(250);
    return [...localRooms];
  },

  async getRoomById(id: string): Promise<Room> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/rooms/${id}`, { headers: getAuthHeaders() });
      const data: RawRoom = await res.json();
      return normalizeRoom(data);
    }
    await simulateDelay(200);
    const room = localRooms.find((r) => r.id === id || r.roomNumber === id);
    if (!room) throw new Error('Room not found');
    return room;
  },

  // Students Management (Warden)
  async getStudents(): Promise<Student[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/students`, { headers: getAuthHeaders() });
      return res.json();
    }
    await simulateDelay(300);
    return [...localStudents];
  },

  async createStudent(studentData: Omit<Student, 'id'> & { password?: string }): Promise<Student> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/students`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(studentData),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to register student');
      }
      return res.json();
    }
    await simulateDelay(350);
    const { password: _ignoredPassword, ...studentProfile } = studentData;
    const newStudent: Student = {
      ...studentProfile,
      id: `stu-${Date.now()}`,
    };
    localStudents.unshift(newStudent);
    return newStudent;
  },

  async updateStudent(id: string, updates: Partial<Student>): Promise<Student> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/students/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(updates),
      });
      return res.json();
    }
    await simulateDelay(300);
    const idx = localStudents.findIndex((s) => s.id === id);
    if (idx === -1) throw new Error('Student not found');
    localStudents[idx] = { ...localStudents[idx], ...updates };
    return localStudents[idx];
  },

  async deleteStudent(id: string): Promise<boolean> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/students/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      return res.ok;
    }
    await simulateDelay(300);
    localStudents = localStudents.filter((s) => s.id !== id);
    return true;
  },

  // Complaints
  async getComplaints(filterStudentId?: string): Promise<Complaint[]> {
    if (USE_REAL_BACKEND) {
      const url = filterStudentId
        ? `${API_BASE_URL}/complaints?studentId=${filterStudentId}`
        : `${API_BASE_URL}/complaints`;
      const res = await fetch(url, { headers: getAuthHeaders() });
      return res.json();
    }
    await simulateDelay(250);
    if (filterStudentId) {
      return localComplaints.filter((c) => c.studentId === filterStudentId);
    }
    return [...localComplaints];
  },

  async getComplaintById(id: string): Promise<Complaint> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/complaints/${id}`, { headers: getAuthHeaders() });
      return res.json();
    }
    await simulateDelay(150);
    const complaint = localComplaints.find((c) => c.id === id || c.complaintNumber === id);
    if (!complaint) throw new Error('Complaint not found');
    return complaint;
  },

  /**
   * Prepares and sends multipart/form-data for Spring Boot backend integration.
   */
  async createComplaint(
    data: {
      type: ComplaintType;
      subject: string;
      description: string;
      location: string;
      priority?: ComplaintPriority;
      studentId: string;
      studentName: string;
      roomNumber: string;
      block: string;
    },
    files: File[]
  ): Promise<Complaint> {
    if (USE_REAL_BACKEND) {
      const formData = new FormData();
      formData.append(
        'complaint',
        new Blob([JSON.stringify(data)], { type: 'application/json' })
      );
      files.forEach((file) => formData.append('files', file));

      const token = localStorage.getItem('hostelhub_token');
      const res = await fetch(`${API_BASE_URL}/complaints`, {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      });
      return res.json();
    }

    await simulateDelay(500);

    const attachments = files.map((f, i) => ({
      id: `att-${Date.now()}-${i}`,
      name: f.name,
      size: f.size,
      type: f.type,
      url: URL.createObjectURL(f),
    }));

    const newComplaint: Complaint = {
      id: `cmp-${Math.floor(1000 + Math.random() * 9000)}`,
      complaintNumber: `CMP-${Math.floor(1000 + Math.random() * 9000)}`,
      studentId: data.studentId,
      studentName: data.studentName,
      roomId: 'room-2',
      roomNumber: data.roomNumber,
      block: data.block,
      type: data.type,
      subject: data.subject,
      description: data.description,
      location: data.location,
      priority: data.priority || 'Medium',
      status: 'Submitted',
      adminNotes: '',
      attachments,
      timeline: [
        {
          status: 'Submitted',
          timestamp: new Date().toISOString(),
          note: 'Complaint submitted by student',
        },
      ],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    localComplaints.unshift(newComplaint);
    return newComplaint;
  },

  async updateComplaintStatus(
    id: string,
    status: ComplaintStatus,
    adminNotes?: string,
    priority?: ComplaintPriority
  ): Promise<Complaint> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/complaints/${id}/status`, {
        method: 'PATCH',
        headers: getAuthHeaders(),
        body: JSON.stringify({ status, adminNotes, priority }),
      });
      return res.json();
    }

    await simulateDelay(350);
    const complaint = localComplaints.find((c) => c.id === id || c.complaintNumber === id);
    if (!complaint) throw new Error('Complaint not found');

    complaint.status = status;
    if (adminNotes !== undefined) complaint.adminNotes = adminNotes;
    if (priority) complaint.priority = priority;
    complaint.updatedAt = new Date().toISOString();

    complaint.timeline.push({
      status,
      timestamp: new Date().toISOString(),
      note: adminNotes ? `Admin Note: ${adminNotes}` : `Status updated to ${status}`,
    });

    return { ...complaint };
  },

  // Notifications
  async getNotifications(): Promise<NotificationItem[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/notifications`, { headers: getAuthHeaders() });
      return res.json();
    }
    await simulateDelay(150);
    return [...localNotifications];
  },

  async markNotificationAsRead(id: string): Promise<void> {
    if (USE_REAL_BACKEND) {
      await fetch(`${API_BASE_URL}/notifications/${id}/read`, {
        method: 'POST',
        headers: getAuthHeaders(),
      });
      return;
    }
    localNotifications = localNotifications.map((n) =>
      n.id === id ? { ...n, read: true } : n
    );
  },

  // Warden Dashboard Stats
  async getDashboardStats(): Promise<DashboardStats> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/dashboard/stats`, { headers: getAuthHeaders() });
      return res.json();
    }
    await simulateDelay(300);

    const totalStudents = localStudents.length;
    const totalRooms = localRooms.length;
    const totalCapacity = localRooms.reduce((acc, r) => acc + r.capacity, 0);
    const totalOccupied = localRooms.reduce((acc, r) => acc + r.occupied, 0);
    const availableBeds = Math.max(0, totalCapacity - totalOccupied);

    const openComplaints = localComplaints.filter(
      (c) => c.status !== 'Resolved' && c.status !== 'Rejected'
    ).length;
    const resolvedComplaints = localComplaints.filter((c) => c.status === 'Resolved').length;

    const complaintsByType: Record<ComplaintType, number> = {
      Mess: 0,
      Cleaning: 0,
      'Room Maintenance': 0,
      Electricity: 0,
      Water: 0,
      Plumbing: 0,
      Other: 0,
    };

    const complaintsByStatus: Record<ComplaintStatus, number> = {
      Submitted: 0,
      'Under Review': 0,
      'In Progress': 0,
      Resolved: 0,
      Rejected: 0,
      Overdue: 0,
    };

    localComplaints.forEach((c) => {
      if (complaintsByType[c.type] !== undefined) complaintsByType[c.type]++;
      if (complaintsByStatus[c.status] !== undefined) complaintsByStatus[c.status]++;
    });

    return {
      totalStudents,
      totalRooms,
      availableBeds,
      openComplaints,
      resolvedComplaints,
      complaintsByType,
      complaintsByStatus,
    };
  },

  // Complaint Reopening & Assignment
  async reopenComplaint(id: string, reason?: string): Promise<Complaint> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/complaints/${id}/reopen`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ reason }),
      });
      if (!res.ok) throw new Error('Failed to reopen complaint');
      return res.json();
    }
    await simulateDelay(300);
    const complaint = localComplaints.find((c) => c.id === id || c.complaintNumber === id);
    if (!complaint) throw new Error('Complaint not found');
    complaint.status = 'Under Review';
    complaint.timeline.push({
      status: 'Under Review',
      timestamp: new Date().toISOString(),
      note: reason ? `Complaint reopened: ${reason}` : 'Complaint reopened by student',
    });
    return { ...complaint };
  },

  async assignComplaint(id: string, wardenId: number, notes?: string): Promise<ComplaintAssignment> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/complaints/${id}/assign`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ wardenId, notes }),
      });
      if (!res.ok) throw new Error('Failed to assign complaint');
      return res.json();
    }
    await simulateDelay(300);
    return {
      id: Date.now(),
      complaintId: Number(id) || 1,
      complaintNumber: `CMP-${id}`,
      assignedWardenId: wardenId,
      assignedWardenName: 'Assigned Warden',
      assignedById: 1,
      assignedByName: 'Current Staff',
      notes,
      assignedAt: new Date().toISOString(),
    };
  },

  async getAssignmentHistory(id: string): Promise<ComplaintAssignment[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/complaints/${id}/history`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to fetch assignment history');
      return res.json();
    }
    await simulateDelay(200);
    return [];
  },

  // SLA Configurations
  async getSlaConfigurations(): Promise<SlaConfiguration[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/sla-configurations`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to fetch SLA configurations');
      return res.json();
    }
    await simulateDelay(200);
    return [
      { id: 1, priority: 'Urgent', deadlineHours: 2, reminderThresholdPercent: 50 },
      { id: 2, priority: 'High', deadlineHours: 8, reminderThresholdPercent: 50 },
      { id: 3, priority: 'Medium', deadlineHours: 24, reminderThresholdPercent: 50 },
      { id: 4, priority: 'Low', deadlineHours: 48, reminderThresholdPercent: 50 },
    ];
  },

  async updateSlaConfiguration(
    idOrPriority: string | number,
    data: { priority?: ComplaintPriority; deadlineHours: number; reminderThresholdPercent: number }
  ): Promise<SlaConfiguration> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/sla-configurations/${idOrPriority}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) throw new Error('Failed to update SLA configuration');
      return res.json();
    }
    await simulateDelay(300);
    return {
      id: typeof idOrPriority === 'number' ? idOrPriority : 1,
      priority: data.priority || 'Medium',
      deadlineHours: data.deadlineHours,
      reminderThresholdPercent: data.reminderThresholdPercent,
    };
  },

  // Notification Enhancements
  async markAllNotificationsAsRead(): Promise<number> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/notifications/mark-all-read`, {
        method: 'POST',
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error('Failed to mark all notifications as read');
      return res.json();
    }
    await simulateDelay(200);
    const count = localNotifications.filter((n) => !n.read).length;
    localNotifications = localNotifications.map((n) => ({ ...n, read: true }));
    return count;
  },

  async getUnreadNotificationCount(): Promise<number> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/notifications/unread/count`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to fetch unread notification count');
      return res.json();
    }
    await simulateDelay(100);
    return localNotifications.filter((n) => !n.read).length;
  },

  // DGM Dashboard & Escalations
  async getDgmDashboard(): Promise<DgmDashboardResponse> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/dgm/dashboard`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load DGM dashboard');
      return res.json();
    }
    await simulateDelay(300);
    return {
      totalComplaints: localComplaints.length,
      pendingEscalations: 0,
      resolvedEscalations: 0,
      pendingAssignments: 0,
      complaintsByHostel: {},
      escalationStatus: { PENDING: 0, IN_REVIEW: 0, RESOLVED: 0, REJECTED: 0 },
      hostelStats: [],
    };
  },

  async getDgmPendingEscalations(): Promise<Escalation[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/dgm/escalations/pending`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load pending escalations');
      return res.json();
    }
    await simulateDelay(200);
    return [];
  },

  async handleDgmEscalation(
    escalationId: number | string,
    data: { status: 'RESOLVED' | 'REJECTED' | 'DISMISSED' | 'IN_REVIEW'; response: string }
  ): Promise<Escalation> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/dgm/escalations/${escalationId}/handle`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) throw new Error('Failed to handle escalation');
      return res.json();
    }
    await simulateDelay(300);
    return {
      id: Number(escalationId),
      complaintId: 1,
      complaintNumber: 'CMP-100',
      escalatedByStudentId: 1,
      escalatedByStudentName: 'Student',
      reason: 'Escalation',
      status: data.status === 'DISMISSED' ? 'REJECTED' : data.status,
      dgmResponse: data.response,
      escalatedAt: new Date().toISOString(),
      handledAt: new Date().toISOString(),
    };
  },

  async getEscalationHistory(complaintId: number | string): Promise<Escalation[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/dgm/escalations/${complaintId}/history`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to fetch escalation history');
      return res.json();
    }
    await simulateDelay(200);
    return [];
  },

  async escalateComplaint(
    complaintId: string | number,
    reason: string,
    details?: string
  ): Promise<Escalation> {
    if (USE_REAL_BACKEND) {
      const savedUserRaw = localStorage.getItem('hostelhub_user');
      let role: UserRole | undefined;
      if (savedUserRaw) {
        try {
          role = (JSON.parse(savedUserRaw) as Partial<User>).role;
        } catch {
          role = undefined;
        }
      }
      const endpoint =
        role === 'DGM'
          ? `${API_BASE_URL}/dgm/complaints/${complaintId}/escalate`
          : `${API_BASE_URL}/complaints/${complaintId}/escalate`;
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          reason,
          details: details || 'Executive intervention initiated due to SLA breach.',
        }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to escalate complaint');
      }
      return res.json();
    }
    await simulateDelay(250);
    const idx = localComplaints.findIndex((c) => c.id === String(complaintId));
    if (idx !== -1) {
      localComplaints[idx] = {
        ...localComplaints[idx],
        escalated: true,
        escalatedAt: new Date().toISOString(),
      };
    }
    return {
      id: Date.now(),
      complaintId: Number(complaintId) || 0,
      complaintNumber: idx !== -1 ? localComplaints[idx].complaintNumber : `CMP-${complaintId}`,
      escalatedByStudentId: 1,
      escalatedByStudentName: idx !== -1 ? localComplaints[idx].studentName : 'Resident Student',
      reason,
      details,
      status: 'PENDING',
      escalatedAt: new Date().toISOString(),
    };
  },

  // Audit Logs
  async getComplaintAuditLogs(complaintId: number | string): Promise<AuditLog[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/audit-logs/complaint/${complaintId}`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to fetch complaint audit logs');
      const data = await res.json();
      return data.content || data;
    }
    await simulateDelay(200);
    return [];
  },

  // Room & Furniture Enhancements
  async createRoom(data: {
    roomNumber: string;
    hostelId: number;
    floor: number;
    block: string;
    capacity: number;
    coolingEquipment?: string;
  }): Promise<Room> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/rooms`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to create room');
      }
      const created: RawRoom = await res.json();
      return normalizeRoom(created);
    }
    await simulateDelay(300);
    const newRoom: Room = {
      id: `room-${Date.now()}`,
      roomNumber: data.roomNumber,
      block: data.block,
      floor: data.floor,
      capacity: data.capacity,
      occupied: 0,
      status: 'Available',
      coolingEquipment: (data.coolingEquipment as any) || 'NONE',
      coolingMaintenanceStatus: 'OPERATIONAL',
      occupants: [],
    };
    localRooms.push(newRoom);
    return newRoom;
  },

  async updateRoom(
    id: string | number,
    data: {
      capacity?: number;
      coolingEquipment?: string;
      coolingMaintenanceStatus?: string;
      active?: boolean;
    }
  ): Promise<Room> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/rooms/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to update room');
      }
      const updated: RawRoom = await res.json();
      return normalizeRoom(updated);
    }
    await simulateDelay(250);
    const idx = localRooms.findIndex((r) => r.id === String(id));
    if (idx !== -1) {
      localRooms[idx] = { ...localRooms[idx], ...data as any };
      return localRooms[idx];
    }
    throw new Error('Room not found');
  },

  async updateFurniture(
    roomId: string | number,
    data: {
      furnitureType: string;
      serviceableCount: number;
      damagedCount: number;
      missingCount: number;
      underRepairCount: number;
      actionType?: string;
      notes?: string;
    }
  ): Promise<Room> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/rooms/${roomId}/furniture`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to update furniture inventory');
      }
      const updated: RawRoom = await res.json();
      return normalizeRoom(updated);
    }
    await simulateDelay(250);
    return localRooms[0];
  },

  async getFurnitureHistory(roomId: string | number): Promise<FurnitureHistoryItem[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/rooms/${roomId}/furniture/history`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error('Failed to fetch furniture history');
      return res.json();
    }
    await simulateDelay(200);
    return [];
  },

  // Hostels (DGM)
  async getHostels(includeInactive = false): Promise<Hostel[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/hostels?includeInactive=${includeInactive}`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error('Failed to load hostels');
      return res.json();
    }
    await simulateDelay(200);
    return [
      { id: 1, name: 'Cauvery Hostel', code: 'CV-01', totalFloors: 4, totalRooms: 40, active: true, roomCount: 5, studentCount: 7, wardenCount: 1, complaintCount: 2 },
    ];
  },

  async getHostelById(id: number | string): Promise<Hostel> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/hostels/${id}`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load hostel');
      return res.json();
    }
    await simulateDelay(200);
    return { id: Number(id), name: 'Cauvery Hostel', active: true };
  },

  async getHostelDetails(id: number | string): Promise<any> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/hostels/${id}/details`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load hostel details');
      return res.json();
    }
    await simulateDelay(250);
    return { id: Number(id), name: 'Cauvery Hostel', rooms: [], students: [], wardens: [], complaints: [] };
  },

  async createHostel(data: { name: string; code?: string; address?: string; description?: string; totalFloors?: number }): Promise<Hostel> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/hostels`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to create hostel');
      }
      return res.json();
    }
    await simulateDelay(300);
    return { id: Date.now(), name: data.name, active: true };
  },

  async updateHostel(id: number | string, data: Partial<Hostel>): Promise<Hostel> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/hostels/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to update hostel');
      }
      return res.json();
    }
    await simulateDelay(250);
    return { id: Number(id), name: data.name || '', active: true, ...data };
  },

  async toggleHostelStatus(id: number | string, active: boolean): Promise<Hostel> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/hostels/${id}/status`, {
        method: 'PATCH',
        headers: getAuthHeaders(),
        body: JSON.stringify({ active }),
      });
      if (!res.ok) throw new Error('Failed to toggle hostel active state');
      return res.json();
    }
    await simulateDelay(250);
    return { id: Number(id), name: 'Hostel', active };
  },

  // Warden Administration (DGM)
  async getWardens(hostelId?: number): Promise<Warden[]> {
    if (USE_REAL_BACKEND) {
      const url = hostelId ? `${API_BASE_URL}/dgm/wardens?hostelId=${hostelId}` : `${API_BASE_URL}/dgm/wardens`;
      const res = await fetch(url, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load wardens');
      return res.json();
    }
    await simulateDelay(200);
    return [
      { id: 1, employeeId: 'WAR-001', name: 'Warden Vance', email: 'warden.vance@hostelhub.edu', phone: '+91 98765 43210', active: true, assignedFloors: [1, 2] },
    ];
  },

  async getWardenById(id: number | string): Promise<Warden> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/dgm/wardens/${id}`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load warden details');
      return res.json();
    }
    await simulateDelay(200);
    return { id: Number(id), employeeId: 'WAR-001', name: 'Warden Vance', email: 'warden.vance@hostelhub.edu', phone: '+91 98765 43210', active: true, assignedFloors: [] };
  },

  async createWarden(data: {
    employeeId: string;
    name: string;
    email: string;
    phone: string;
    hostelId?: number;
    password: string;
    assignedFloors?: number[];
  }): Promise<Warden> {
    if (USE_REAL_BACKEND) {
      return submitCreateWardenRequest(fetch, API_BASE_URL, getAuthHeaders(), data);
    }
    await simulateDelay(300);
    return { id: Date.now(), employeeId: data.employeeId, name: data.name, email: data.email, phone: data.phone, active: true, assignedFloors: data.assignedFloors || [] };
  },

  async updateWarden(id: number | string, data: Partial<Warden>): Promise<Warden> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/dgm/wardens/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to update warden');
      }
      return res.json();
    }
    await simulateDelay(250);
    return { id: Number(id), employeeId: 'WAR', name: data.name || '', email: data.email || '', phone: data.phone || '', active: true, assignedFloors: [] };
  },

  async assignWardenFloors(id: number | string, floorNumbers: number[]): Promise<Warden> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/dgm/wardens/${id}/assignments`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ floorNumbers }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to update floor assignments');
      }
      return res.json();
    }
    await simulateDelay(250);
    return { id: Number(id), employeeId: 'WAR', name: 'Warden', email: '', phone: '', active: true, assignedFloors: floorNumbers };
  },

  async toggleWardenStatus(id: number | string, active: boolean): Promise<Warden> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/dgm/wardens/${id}/status`, {
        method: 'PATCH',
        headers: getAuthHeaders(),
        body: JSON.stringify({ active }),
      });
      if (!res.ok) throw new Error('Failed to toggle warden active status');
      return res.json();
    }
    await simulateDelay(200);
    return { id: Number(id), employeeId: 'WAR', name: 'Warden', email: '', phone: '', active, assignedFloors: [] };
  },

  // Notices & Announcements
  async getNotices(all = false): Promise<Notice[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/notices?all=${all}`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load notices');
      return res.json();
    }
    await simulateDelay(200);
    return [
      {
        id: 1,
        title: 'Water Supply Maintenance',
        message: 'Water supply in Hostel A will be suspended temporarily from 2 PM to 4 PM for pipe repairs.',
        category: 'MAINTENANCE',
        priority: 'HIGH',
        active: true,
        authorName: 'Warden Vance',
        authorRole: 'WARDEN',
        createdAt: new Date().toISOString(),
      },
    ];
  },

  async createNotice(data: {
    title: string;
    message: string;
    category: NoticeCategory;
    priority: Notice['priority'];
    hostelId?: number;
    expiresAt?: string;
  }): Promise<Notice> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/notices`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to publish notice');
      }
      return res.json();
    }
    await simulateDelay(250);
    return {
      id: Date.now(),
      title: data.title,
      message: data.message,
      category: data.category,
      priority: data.priority,
      active: true,
      authorName: 'You',
      authorRole: 'STAFF',
      createdAt: new Date().toISOString(),
    };
  },

  async toggleNoticeStatus(id: number, active: boolean): Promise<Notice> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/notices/${id}/status`, {
        method: 'PATCH',
        headers: getAuthHeaders(),
        body: JSON.stringify({ active }),
      });
      if (!res.ok) throw new Error('Failed to update notice status');
      return res.json();
    }
    await simulateDelay(200);
    return { id, title: 'Notice', message: '', category: 'GENERAL', priority: 'MEDIUM', active, authorName: '', authorRole: '', createdAt: '' };
  },

  // Surveys & Analytics
  async getSurveys(): Promise<Survey[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/surveys`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load surveys');
      return res.json();
    }
    await simulateDelay(200);
    return [];
  },

  async getActiveSurveys(): Promise<Survey[]> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/surveys/active`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load active surveys');
      return res.json();
    }
    await simulateDelay(200);
    return [];
  },

  async createSurvey(data: {
    title: string;
    description: string;
    hostelId?: number;
    endDate?: string;
  }): Promise<Survey> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/surveys`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          title: data.title,
          description: data.description,
          targetHostelId: data.hostelId,
          endDate: data.endDate,
        }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to create survey');
      }
      return res.json();
    }
    await simulateDelay(300);
    return {
      id: Date.now(),
      title: data.title,
      description: data.description,
      targetHostelId: data.hostelId,
      endDate: data.endDate,
      active: true,
    };
  },

  async submitSurvey(id: number, data: SurveySubmissionData): Promise<{ message: string }> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/surveys/${id}/submit`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to submit feedback survey');
      }
      return res.json();
    }
    await simulateDelay(300);
    return { message: 'Survey submitted successfully' };
  },

  async getSurveyAnalytics(id: number): Promise<SurveyAnalytics> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/surveys/${id}/analytics`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load survey analytics');
      return res.json();
    }
    await simulateDelay(250);
    return {
      surveyId: id,
      surveyTitle: 'Annual Student Satisfaction Survey',
      totalResponses: 38,
      overallSatisfactionScore: 4.1,
      q1CleanlinessAvg: 4.2,
      q2WashroomsAvg: 3.8,
      q3FurnitureAvg: 4.1,
      q4DrinkingWaterAvg: 4.4,
      q5WifiAvg: 3.7,
      q6ElectricityAvg: 4.5,
      q7FoodAvg: 3.9,
      q8SecurityAvg: 4.6,
      q9WardenSupportAvg: 4.2,
      q10ComplaintResolutionAvg: 4.0,
      comments: [],
    };
  },

  async toggleSurveyStatus(id: number, active: boolean): Promise<Survey> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/surveys/${id}/status`, {
        method: 'PATCH',
        headers: getAuthHeaders(),
        body: JSON.stringify({ active }),
      });
      if (!res.ok) throw new Error('Failed to toggle survey active status');
      return res.json();
    }
    await simulateDelay(200);
    return { id, title: 'Survey', description: '', active };
  },

  // Feedback Tasks Forwarding
  async getFeedbackTasks(hostelId?: number): Promise<FeedbackTask[]> {
    if (USE_REAL_BACKEND) {
      const url = hostelId ? `${API_BASE_URL}/feedback-tasks?hostelId=${hostelId}` : `${API_BASE_URL}/feedback-tasks`;
      const res = await fetch(url, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load feedback action tasks');
      return res.json();
    }
    await simulateDelay(200);
    return [];
  },

  async createFeedbackTask(data: {
    title: string;
    description: string;
    surveyId?: number;
    hostelId: number;
    wardenId: number;
    category?: string;
    priority?: string;
  }): Promise<FeedbackTask> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/feedback-tasks`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to forward feedback task');
      }
      return res.json();
    }
    await simulateDelay(250);
    return {
      id: Date.now(),
      title: data.title,
      description: data.description,
      hostelId: data.hostelId,
      hostelName: 'Hostel',
      wardenId: data.wardenId,
      wardenName: 'Warden',
      category: data.category || 'GENERAL',
      priority: data.priority || 'MEDIUM',
      status: 'PENDING',
      createdBy: 'DGM',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
  },

  async updateFeedbackTask(
    id: number,
    data: { status: string; resolutionNotes?: string }
  ): Promise<FeedbackTask> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/feedback-tasks/${id}`, {
        method: 'PATCH',
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to update feedback task');
      }
      return res.json();
    }
    await simulateDelay(200);
    return {
      id,
      title: 'Task',
      description: '',
      hostelId: 1,
      hostelName: 'Hostel',
      wardenId: 1,
      wardenName: 'Warden',
      category: 'GENERAL',
      priority: 'MEDIUM',
      status: data.status as any,
      resolutionNotes: data.resolutionNotes,
      createdBy: 'DGM',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
  },

  async getDgmProfile(): Promise<DgmProfile> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/dgm/profile`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Failed to load DGM profile');
      return res.json();
    }
    await simulateDelay(200);
    return {
      id: 'dgm-1',
      name: 'Dr. Arvind Mehta',
      email: 'dgm.director@hostelhub.edu',
      employeeId: 'DGM-001',
      designation: 'Deputy General Manager - Hostels',
      officeLocation: 'Admin Block, Room 102',
    };
  },

  async fetchProtectedFile(url: string, signal?: AbortSignal): Promise<Blob> {
    if (USE_REAL_BACKEND) {
      const token =
        localStorage.getItem('hostelhub_token') || localStorage.getItem('hostelsync_token');
      return fetchProtectedFileBlob(url, API_BASE_URL, token, signal);
    }
    const response = await fetch(url, { signal });
    if (!response.ok) {
      throw new Error(`Failed to load file (HTTP ${response.status}).`);
    }
    return response.blob();
  },
};
