# HostelHub — Smart Hostel Management System

A modern, responsive university hostel management frontend built with React, Vite, TypeScript, and Tailwind CSS.

---

## Key Features

### For Students
- **Student Dashboard**: Room overview, roommates list, quick complaint submission, recent tickets summary.
- **My Room**: Room capacity, occupancy details, roommate directory with contact info, hostel emergency directory.
- **Complaint Management**: Submit complaints with categories (*Mess, Cleaning, Room Maintenance, Electricity, Water, Plumbing, Other*), location specifications, and multiple photo/video evidence uploads.
- **Ticket Tracking**: Full timeline tracking (*Submitted &rarr; Under Review &rarr; In Progress &rarr; Resolved*), warden administrative responses, and status badges.
- **Resident Profile**: Academic info, room record, editable personal and guardian emergency contacts.

### For Wardens & Administrators
- **Administrative Dashboard**: Real-time KPI metrics (total students, total rooms, vacant beds, active complaints), complaints distribution by category, actionable urgent tickets.
- **Student Residents Directory**: Searchable directory by name, student ID, course, or room. Complete CRUD modal with automated bed allocation checks.
- **Room Inventory & Occupancy**: Visual room occupancy bars, block filters, vacant bed counters, and occupant inspection modal.
- **Complaints & Maintenance Desk**: Multi-criteria filters (*status, category, priority, keyword*), triage modal for updating status, setting priority, and attaching public admin progress notes.
- **Warden Profile**: Official duties, consultation hours, and campus emergency contact configuration.

---

## Project Structure

```text
src/
├── components/
│   ├── common/         # Reusable primitives: Button, Input, Select, StatusBadge, Card, Modal, ConfirmDialog, FileUploader, EmptyState
│   └── layout/         # Responsive Sidebar (with mobile drawer) and Header (with notifications & user menu)
├── context/
│   ├── AuthContext.tsx # User session, mock JWT, quick role switcher for demo
│   └── ToastContext.tsx# Floating user feedback alerts
├── mock/
│   └── data.ts         # Realistic mock dataset for rooms, students, complaints, and notifications
├── pages/
│   ├── Login/          # Branded authentication screen with one-click demo presets
│   ├── student/        # Student dashboard, room view, complaints list, submission form, detail & profile
│   └── warden/         # Warden dashboard, student roster, room inventory, triage desk & profile
├── services/
│   └── api.ts          # Centralized REST service layer prepared for Spring Boot endpoints
├── types/
│   └── index.ts        # Comprehensive TypeScript domain interfaces
├── App.tsx             # Root layout & page router
├── main.tsx            # React root mount
└── index.css           # Tailwind CSS configuration & theme variables
```

---

## Spring Boot REST API Integration Guide

The frontend is built specifically to connect cleanly to a **Java + Spring Boot + MySQL** backend.

### 1. Configure the API Base URL
In `.env`:
```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

### 2. Enable Real Backend Mode
In `src/services/api.ts`, switch the flag:
```typescript
const USE_REAL_BACKEND = true;
```

### 3. Expected Spring Boot Endpoints
| HTTP Method | Endpoint | Description | Payload / Notes |
|-------------|----------|-------------|-----------------|
| `POST` | `/api/auth/login` | Authenticate user | `{ email, password }` &rarr; returns `{ token, user }` |
| `GET` | `/api/auth/me` | Fetch authenticated user | Header: `Authorization: Bearer <token>` |
| `GET` | `/api/students` | Get all students | Query params: `?block=&search=` |
| `POST` | `/api/students` | Create student record | JSON body of `Student` |
| `PUT` | `/api/students/{id}` | Update student record | JSON body of partial `Student` |
| `DELETE` | `/api/students/{id}` | Remove student record | - |
| `GET` | `/api/rooms` | Get all rooms with occupants | - |
| `GET` | `/api/rooms/{id}` | Get single room details | - |
| `GET` | `/api/complaints` | Get complaints queue | Query params: `?studentId=&status=` |
| `POST` | `/api/complaints` | Lodges a complaint | **`multipart/form-data`** (`complaint` JSON blob + `files` array) |
| `PATCH` | `/api/complaints/{id}/status` | Update status/priority | `{ status, adminNotes, priority }` |
| `GET` | `/api/dashboard/stats` | Aggregated metrics | Total students, rooms, beds, category breakdowns |
| `GET` | `/api/notifications` | Alerts & notifications | - |
