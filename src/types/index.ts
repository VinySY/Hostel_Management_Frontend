export type UserRole = 'STUDENT' | 'WARDEN';

export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  avatar?: string;
  studentId?: string;
}

export type ComplaintType =
  | 'Mess'
  | 'Cleaning'
  | 'Room Maintenance'
  | 'Electricity'
  | 'Water'
  | 'Plumbing'
  | 'Other';

export type ComplaintStatus =
  | 'Submitted'
  | 'Under Review'
  | 'In Progress'
  | 'Resolved'
  | 'Rejected';

export type ComplaintPriority = 'Low' | 'Medium' | 'High' | 'Urgent';

export interface EvidenceFile {
  id: string;
  name: string;
  size: number;
  type: string;
  url: string;
}

export interface ComplaintTimelineItem {
  status: ComplaintStatus;
  timestamp: string;
  note?: string;
}

export interface Complaint {
  id: string;
  complaintNumber: string;
  studentId: string;
  studentName: string;
  roomId: string;
  roomNumber: string;
  block: string;
  type: ComplaintType;
  subject: string;
  description: string;
  location: string;
  priority: ComplaintPriority;
  status: ComplaintStatus;
  adminNotes?: string;
  attachments: EvidenceFile[];
  timeline: ComplaintTimelineItem[];
  createdAt: string;
  updatedAt: string;
}

export type RoomOccupancyStatus = 'Available' | 'Partially Occupied' | 'Full';

export interface RoomOccupant {
  studentId: string;
  name: string;
  email: string;
  course: string;
  year: number;
  phone: string;
}

export interface Room {
  id: string;
  roomNumber: string;
  block: string;
  floor: number;
  capacity: number;
  occupied: number;
  status: RoomOccupancyStatus;
  occupants: RoomOccupant[];
}

export interface Student {
  id: string;
  studentId: string;
  name: string;
  email: string;
  phone: string;
  gender: 'Male' | 'Female' | 'Other';
  course: string;
  year: number;
  block: string;
  roomNumber: string;
  roomId: string;
  guardianName: string;
  guardianPhone: string;
  status: 'Active' | 'Inactive' | 'Suspended';
}

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  link?: string;
  type: 'info' | 'success' | 'warning' | 'alert';
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface DashboardStats {
  totalStudents: number;
  totalRooms: number;
  availableBeds: number;
  openComplaints: number;
  resolvedComplaints: number;
  complaintsByType: Record<ComplaintType, number>;
  complaintsByStatus: Record<ComplaintStatus, number>;
}
