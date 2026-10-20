# HostelSync - Testing Guide

This guide provides step-by-step instructions for testing all features of the HostelSync application.

## Prerequisites

- Both servers running:
  - Backend: http://localhost:8080
  - Frontend: http://localhost:3000
- Demo data seeded (automatic on first startup)

## Test Accounts

### Student Account
- **Email**: rahul.sharma@hostelhub.edu
- **Password**: studentPass2026
- **Role**: STUDENT
- **Room**: 101, Bed A

### Warden Account
- **Email**: warden.vance@hostelhub.edu
- **Password**: wardenPass2026
- **Role**: WARDEN

---

## Student Workflow Tests

### 1. Student Login
1. Open http://localhost:3000
2. Enter student credentials
3. Click "Sign in"
4. **Expected**: Redirect to student dashboard

### 2. Student Dashboard
1. After login, verify dashboard displays:
   - Welcome message with student name
   - "My Room" card (Room 101)
   - "My Complaints" section
   - Navigation sidebar with: Dashboard, My Room, Complaints, Profile

### 3. View Room Details
1. Click "My Room" in sidebar
2. **Expected**: 
   - Room number: 101
   - Capacity: 3 beds
   - Current occupancy: 2/3
   - List of roommates (Aman Patel)
   - Bed assignment: Bed A

### 4. Submit New Complaint
1. Click "Complaints" → "New Complaint" button
2. Fill form:
   - **Type**: Select "Room Maintenance"
   - **Priority**: Select "High"
   - **Description**: "Broken ceiling fan in Room 101"
3. Optionally upload a file (image or PDF)
4. Click "Submit Complaint"
5. **Expected**: 
   - Success message
   - Redirect to complaints list
   - New complaint appears with status "Submitted"

### 5. View Complaint Details
1. Click on the complaint you just created
2. **Expected**:
   - Complaint number (e.g., CMP-0001)
   - Full details (type, priority, description)
   - Timeline with "Submitted" entry
   - Attached files (if uploaded)

### 6. View Profile
1. Click "Profile" in sidebar
2. **Expected**:
   - Full name, email, phone
   - Student ID, course, year
   - Room assignment
   - Status (Active)

### 7. Logout
1. Click "Logout" button (top right)
2. **Expected**: Redirect to login page

---

## Warden Workflow Tests

### 8. Warden Login
1. Go to http://localhost:3000
2. Enter warden credentials
3. Click "Sign in"
4. **Expected**: Redirect to warden dashboard

### 9. Warden Dashboard
1. After login, verify dashboard displays:
   - Statistics cards:
     - Total Students (7)
     - Total Rooms (5)
     - Available Beds (calculated)
     - Open Complaints (count)
   - Recent complaints list
   - Navigation: Dashboard, Students, Rooms, Complaints

### 10. View All Students
1. Click "Students" in sidebar
2. **Expected**:
   - Table with 7 students
   - Columns: Name, Email, Room, Status
   - Search functionality (if implemented)

### 11. Create New Student
1. Click "Add Student" button
2. Fill form:
   - **Name**: Test Student
   - **Email**: test.student@hostelhub.edu
   - **Password**: testPass123
   - **Phone**: 9876543210
   - **Student ID**: ST2026008
   - **Course**: MCA
   - **Year**: 1
   - **Guardian Name**: Test Guardian
   - **Guardian Contact**: 9876543211
   - **Room**: Select "204" (or any room with available beds)
3. Click "Create Student"
4. **Expected**:
   - Success message
   - New student appears in list
   - Room occupancy updated

### 12. Update Student Details
1. Click on any student (e.g., "Rahul Sharma")
2. Click "Edit" button
3. Update phone number: 9999999999
4. Click "Save"
5. **Expected**:
   - Success message
   - Updated phone number displayed

### 13. View All Rooms
1. Click "Rooms" in sidebar
2. **Expected**:
   - List of 5 rooms (101, 204, 205, 302, 304)
   - Each shows: Room number, capacity, current occupancy
   - Available beds count

### 14. View Room Details
1. Click on "Room 101"
2. **Expected**:
   - Room details (number, capacity, hostel)
   - List of current occupants with bed assignments
   - Available beds

### 15. Triage Complaints
1. Click "Complaints" in sidebar
2. **Expected**:
   - All complaints from all students (not just own)
   - Filter options (status, type, priority)
   - Sort options (date, priority)

### 16. Update Complaint Status
1. Click on the complaint created by Rahul
2. Click "Update Status" button
3. Select new status: "Under Review"
4. Add comment: "Maintenance team notified"
5. Click "Update"
6. **Expected**:
   - Status changed to "Under Review"
   - New timeline entry added with comment
   - Student receives notification (if implemented)

### 17. View Complaint Timeline
1. On complaint details page, scroll to timeline
2. **Expected**:
   - All status changes listed chronologically
   - Each entry shows: date, old status → new status, comment
   - Updated by: warden name

### 18. Download Attachment
1. If complaint has attachments, click download icon
2. **Expected**: File downloads successfully

### 19. Mark Complaint Resolved
1. Update complaint status to "Resolved"
2. Add comment: "Ceiling fan replaced"
3. **Expected**:
   - Status changes to "Resolved"
   - Complaint marked complete

---

## Security Tests

### 20. IDOR Protection - Students
1. Login as Rahul (student)
2. Note complaint ID of another student (if visible in dev tools)
3. Try to access: http://localhost:8080/api/complaints/{other_student_complaint_id}
4. **Expected**: 403 Forbidden error

### 21. IDOR Protection - API Direct Access
1. Get Rahul's JWT token (from browser dev tools → Application → Local Storage)
2. Use curl or Postman:
```bash
curl -H "Authorization: Bearer {RAHUL_TOKEN}" \
     http://localhost:8080/api/students
```
3. **Expected**: 403 Forbidden (students can't access student list endpoint)

### 22. Role Enforcement - Dashboard
1. Login as student
2. Try to access warden endpoints:
```bash
curl -H "Authorization: Bearer {STUDENT_TOKEN}" \
     http://localhost:8080/api/dashboard/stats
```
3. **Expected**: 403 Forbidden

### 23. Unauthorized Access
1. Logout (or use incognito window)
2. Try to access: http://localhost:8080/api/rooms
3. **Expected**: 401 Unauthorized

### 24. Invalid JWT Token
1. Try API call with fake token:
```bash
curl -H "Authorization: Bearer fake.jwt.token" \
     http://localhost:8080/api/rooms
```
2. **Expected**: 401 Unauthorized

---

## Edge Cases & Validation Tests

### 25. Duplicate Email
1. Login as warden
2. Try to create student with existing email (rahul.sharma@hostelhub.edu)
3. **Expected**: 409 Conflict error

### 26. Room Over-Capacity
1. Try to assign student to Room 205 (already at capacity: 2/2)
2. **Expected**: 400 Bad Request - "Room is at full capacity"

### 27. Invalid Login
1. Try login with wrong password
2. **Expected**: 401 Unauthorized - "Invalid credentials"

### 28. File Upload Size Limit
1. Try to upload file > 10MB with complaint
2. **Expected**: 400 Bad Request - "File size exceeds limit"

### 29. Missing Required Fields
1. Try to create complaint without description
2. **Expected**: 400 Bad Request - Validation error

### 30. XSS Prevention
1. Try to submit complaint with description: `<script>alert('XSS')</script>`
2. **Expected**: Content escaped, no script execution

---

## API Endpoint Tests (Manual with curl)

### Health Check
```bash
curl http://localhost:8080/api/health
```
Expected: `{"status":"UP","service":"HostelSync Backend"}`

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"rahul.sharma@hostelhub.edu","password":"studentPass2026"}'
```
Expected: `{"token":"eyJ...", "email":"...", "role":"STUDENT"}`

### Get Rooms (Authenticated)
```bash
curl http://localhost:8080/api/rooms \
  -H "Authorization: Bearer {YOUR_TOKEN}"
```
Expected: Array of rooms with occupancy data

### Create Complaint (Multipart)
```bash
curl -X POST http://localhost:8080/api/complaints \
  -H "Authorization: Bearer {STUDENT_TOKEN}" \
  -F "type=Room_Maintenance" \
  -F "priority=High" \
  -F "description=Test complaint" \
  -F "file=@path/to/image.jpg"
```
Expected: Created complaint with ID and complaint number

---

## Performance Tests

### 31. Load Test - Concurrent Logins
1. Use tool like Apache Bench or JMeter
2. Simulate 10 concurrent login requests
3. **Expected**: All succeed with <500ms response time

### 32. Load Test - Dashboard Stats
1. Hit `/api/dashboard/stats` with 20 concurrent requests
2. **Expected**: All succeed with <1000ms response time

---

## Browser Compatibility Tests

### 33. Cross-Browser Testing
Test on:
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

**Expected**: All features work consistently

---

## Mobile Responsiveness Tests

### 34. Mobile View
1. Open browser dev tools
2. Toggle device toolbar (Ctrl+Shift+M)
3. Test on various screen sizes:
   - iPhone SE (375x667)
   - iPad (768x1024)
   - Desktop (1920x1080)
4. **Expected**: UI adapts gracefully, no layout breaks

---

## Regression Tests (After Code Changes)

After any code changes, verify:
1. ✅ Login works for both roles
2. ✅ Dashboard loads with correct data
3. ✅ Room occupancy calculates correctly
4. ✅ Complaint CRUD operations work
5. ✅ File upload/download works
6. ✅ Role-based access enforced
7. ✅ IDOR protection active
8. ✅ Validation errors display properly

---

## Automated Test Commands

### Backend Unit Tests
```bash
cd backend
.\mvnw.cmd test
```

### Frontend Tests
```bash
cd frontend
npm run test
```

### Build Verification
```bash
# Backend
cd backend
.\mvnw.cmd clean package

# Frontend
cd frontend
npm run build
```

---

## Test Data Reset

To reset database and reseed:
1. Stop backend server
2. Drop database:
```sql
DROP DATABASE hostelsync;
CREATE DATABASE hostelsync;
```
3. Restart backend (auto-seeds)

---

## Known Issues / Limitations

1. **File Storage**: Files stored locally, not in cloud (for demo purposes)
2. **Email Notifications**: Not implemented (stub only)
3. **Password Reset**: Not implemented
4. **Profile Picture Upload**: Not implemented
5. **Complaint Comments**: Warden can add comment on status change only
6. **Real-time Updates**: No WebSocket notifications (requires page refresh)

---

## Success Criteria

All tests pass if:
- ✅ All user workflows complete without errors
- ✅ Security boundaries enforced (no unauthorized access)
- ✅ Data integrity maintained (no orphaned records)
- ✅ Validation prevents invalid data
- ✅ File uploads work correctly
- ✅ Role-based features accessible only to authorized users
- ✅ API responses match documented schemas
- ✅ UI responsive on all screen sizes

---

## Reporting Issues

If you find a bug:
1. Note the exact steps to reproduce
2. Capture error messages (frontend console + backend logs)
3. Note your browser and OS
4. Check if issue persists after cache clear
5. Report with all details above

---

**Happy Testing! 🧪**
