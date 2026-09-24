import {
  AuthResponse,
  Complaint,
  ComplaintPriority,
  ComplaintStatus,
  ComplaintType,
  DashboardStats,
  NotificationItem,
  Room,
  Student,
  User,
} from '../types';
import {
  mockComplaints,
  mockNotifications,
  mockRooms,
  mockStudents,
  mockUsers,
} from '../mock/data';

// Base API configuration referencing Spring Boot REST endpoint
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * Standard HTTP helper ready for Spring Boot JWT Authentication.
 * When backend is live, toggle USE_REAL_BACKEND to true or set VITE_USE_REAL_BACKEND=true in .env
 */
export const USE_REAL_BACKEND = import.meta.env.VITE_USE_REAL_BACKEND === 'true' || false;

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
  async login(roleChoice: 'STUDENT' | 'WARDEN', _credentials?: { email?: string; password?: string }): Promise<AuthResponse> {
    if (USE_REAL_BACKEND) {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(_credentials),
      });
      if (!response.ok) throw new Error('Authentication failed. Check credentials.');
      return response.json();
    }

    await simulateDelay(400);
    const user = roleChoice === 'WARDEN' ? mockUsers.warden : mockUsers.student;
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
  async getStudentProfile(studentId: string): Promise<Student> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/students/${studentId}`, { headers: getAuthHeaders() });
      return res.json();
    }
    await simulateDelay(200);
    const found = localStudents.find((s) => s.studentId === studentId || s.id === studentId);
    if (!found) throw new Error('Student profile not found');
    return found;
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
      return res.json();
    }
    await simulateDelay(250);
    return [...localRooms];
  },

  async getRoomById(id: string): Promise<Room> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/rooms/${id}`, { headers: getAuthHeaders() });
      return res.json();
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

  async createStudent(studentData: Omit<Student, 'id'>): Promise<Student> {
    if (USE_REAL_BACKEND) {
      const res = await fetch(`${API_BASE_URL}/students`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(studentData),
      });
      return res.json();
    }
    await simulateDelay(350);
    const newStudent: Student = {
      ...studentData,
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
};
