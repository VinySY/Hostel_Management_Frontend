export type UserRole = 'STUDENT' | 'WARDEN' | 'DGM';

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
  | 'Rejected'
  | 'Overdue';

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
  hostelId?: string;
  hostelName?: string;
  floor?: number | null;
  escalated?: boolean;
  slaBreached?: boolean;
  slaDeadline?: string;
  overdueAt?: string;
  escalatedAt?: string;
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

export interface RoomFurnitureItem {
  furnitureType: 'BED' | 'STUDY_TABLE' | 'LOCKER';
  requiredCount: number;
  serviceableCount: number;
  damagedCount: number;
  missingCount: number;
  underRepairCount: number;
  shortage: number;
}

export interface Room {
  id: string;
  roomNumber: string;
  block: string;
  floor: number;
  capacity: number;
  occupied: number;
  status: RoomOccupancyStatus;
  active?: boolean;
  hostelId?: string;
  hostelName?: string;
  coolingEquipment?: 'NONE' | 'COOLER' | 'AC';
  coolingMaintenanceStatus?: 'OPERATIONAL' | 'MAINTENANCE_REQUIRED' | 'REPLACEMENT_NEEDED';
  furnitureInventory?: RoomFurnitureItem[];
  hasMaintenanceIssues?: boolean;
  hasShortage?: boolean;
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
  avatarUrl?: string;
  hostelName?: string;
  hostelId?: string;
  floor?: number;
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

export interface SlaConfiguration {
  id: number;
  priority: ComplaintPriority;
  deadlineHours: number;
  reminderThresholdPercent: number;
}

export interface ComplaintAssignment {
  id: number;
  complaintId: number;
  complaintNumber: string;
  assignedWardenId: number;
  assignedWardenName: string;
  assignedById: number;
  assignedByName: string;
  notes?: string;
  assignedAt: string;
}

export interface Escalation {
  id: number;
  complaintId: number;
  complaintNumber: string;
  escalatedByStudentId: number;
  escalatedByStudentName: string;
  reason: string;
  details?: string;
  status: 'PENDING' | 'IN_REVIEW' | 'RESOLVED' | 'REJECTED';
  handledByDgmId?: number;
  handledByDgmName?: string;
  dgmResponse?: string;
  escalatedAt: string;
  handledAt?: string;
}

export interface DgmHostelStat {
  hostelId: number;
  hostelName: string;
  totalComplaints: number;
  overdueComplaints: number;
  pendingEscalations: number;
  resolvedComplaints: number;
  reopenedComplaints: number;
}

export interface DgmDashboardResponse {
  totalComplaints: number;
  pendingEscalations: number;
  resolvedEscalations: number;
  pendingAssignments: number;
  complaintsByHostel: Record<string, number>;
  escalationStatus: Record<string, number>;
  hostelStats: DgmHostelStat[];
}

export interface AuditLog {
  id: number;
  entityType: string;
  entityId: number;
  action: string;
  userId: number;
  userName: string;
  userRole: string;
  previousValue?: string;
  newValue?: string;
  reason?: string;
  clientInfo?: string;
  timestamp: string;
}

export interface FurnitureHistoryItem {
  id: number;
  roomId: number;
  roomNumber: string;
  furnitureType: string;
  actionType: string;
  quantityChanged: number;
  notes?: string;
  recordedBy: string;
  recordedAt: string;
}

export interface Hostel {
  id: number;
  name: string;
  code?: string;
  address?: string;
  description?: string;
  totalFloors?: number;
  totalRooms?: number;
  totalCapacity?: number;
  totalOccupancy?: number;
  totalStudents?: number;
  wardensCount?: number;
  activeComplaintsCount?: number;
  active: boolean;
  roomCount?: number;
  studentCount?: number;
  wardenCount?: number;
  complaintCount?: number;
}

export interface Warden {
  id: number;
  employeeId: string;
  name: string;
  email: string;
  phone: string;
  hostelId?: number;
  hostelName?: string;
  active: boolean;
  assignedFloors: number[];
  openComplaintsCount?: number;
}

export type NoticeCategory =
  | 'GENERAL'
  | 'MAINTENANCE'
  | 'MESS'
  | 'SECURITY'
  | 'EVENT'
  | 'EMERGENCY';

export interface Notice {
  id: number;
  title: string;
  message: string;
  category: NoticeCategory;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  hostelId?: number;
  hostelName?: string;
  active: boolean;
  authorName: string;
  authorRole: string;
  createdAt: string;
  expiresAt?: string;
}

export interface SurveyComment {
  id: number;
  studentName: string;
  studentId?: string;
  hostelName?: string;
  roomNumber?: string;
  comments: string;
  submittedAt: string;
}

export interface SurveyAnalytics {
  surveyId: number;
  surveyTitle: string;
  totalResponses: number;
  overallSatisfactionScore: number;
  q1CleanlinessAvg: number;
  q2WashroomsAvg: number;
  q3FurnitureAvg: number;
  q4DrinkingWaterAvg: number;
  q5WifiAvg: number;
  q6ElectricityAvg: number;
  q7FoodAvg: number;
  q8SecurityAvg: number;
  q9WardenSupportAvg: number;
  q10ComplaintResolutionAvg: number;
  comments: SurveyComment[];
}

export interface Survey {
  id: number;
  title: string;
  description: string;
  targetHostelId?: number;
  targetHostelName?: string;
  startDate?: string;
  endDate?: string;
  active: boolean;
  responseCount?: number;
  hasSubmitted?: boolean;
}

export interface SurveySubmissionData {
  q1Cleanliness: number;
  q2Washrooms: number;
  q3Furniture: number;
  q4DrinkingWater: number;
  q5Wifi: number;
  q6Electricity: number;
  q7Food: number;
  q8Security: number;
  q9WardenSupport: number;
  q10ComplaintResolution: number;
  comments?: string;
}

export interface FeedbackTask {
  id: number;
  title: string;
  description: string;
  surveyId?: number;
  hostelId: number;
  hostelName: string;
  wardenId: number;
  wardenName: string;
  category: string;
  priority: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'RESOLVED';
  resolutionNotes?: string;
  resolvedAt?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

