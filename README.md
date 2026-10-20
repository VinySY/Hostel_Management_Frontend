# HostelSync - College Hostel Management System

A full-stack web application for managing college hostels, built with Spring Boot backend and React frontend.

## 🏗️ Technology Stack

### Backend
- **Framework**: Spring Boot 4.0.8
- **Language**: Java 21
- **Database**: MySQL 8.0.46
- **Security**: JWT Authentication with Spring Security
- **Build Tool**: Maven
- **Server Port**: 8080

### Frontend
- **Framework**: React 19
- **Language**: TypeScript
- **Build Tool**: Vite 8.3.0
- **Styling**: Tailwind CSS
- **UI Components**: Radix UI, Lucide Icons
- **Client Port**: 3000

## 📋 Features

### For Students
- View personal profile and room assignment
- Submit complaints with file attachments
- Track complaint status and timeline
- View room details and roommates

### For Wardens
- Dashboard with statistics (students, rooms, beds, complaints)
- Manage student records (create, update, view)
- Allocate students to rooms
- Triage and update complaint status
- View all complaints and their history

### Security Features
- JWT-based stateless authentication
- Role-based access control (STUDENT, WARDEN)
- IDOR (Insecure Direct Object Reference) protection
- Secure file upload validation
- Password encryption with BCrypt

## 🚀 Getting Started

### Prerequisites

1. **Java 21** - [Download](https://adoptium.net/)
2. **Node.js 18+** and npm - [Download](https://nodejs.org/)
3. **MySQL 8.0.46** - [Download](https://dev.mysql.com/downloads/mysql/)

### Database Setup

1. Start MySQL server
2. Create database:
```sql
CREATE DATABASE hostelsync CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. Update database credentials in `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hostelsync
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD_HERE
```

### Backend Setup

1. Navigate to backend directory:
```bash
cd backend
```

2. Build the project:
```bash
# Windows
.\mvnw.cmd clean install

# Linux/Mac
./mvnw clean install
```

3. Run the application:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

4. Verify backend is running:
```bash
curl http://localhost:8080/api/health
```

Expected response: `{"status":"UP","service":"HostelSync Backend"}`

### Frontend Setup

1. Navigate to frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Create `.env` file in frontend directory:
```env
VITE_USE_REAL_BACKEND=true
VITE_API_BASE_URL=http://localhost:8080/api
```

4. Start development server:
```bash
npm run dev
```

5. Access the application at http://localhost:3000

## 🔐 Demo Credentials

The application seeds demo data on first startup.

### Student Account
- **Email**: rahul.sharma@hostelhub.edu
- **Password**: studentPass2026
- **Details**: Room 101, Bed A, Computer Science

### Warden Account
- **Email**: warden.vance@hostelhub.edu
- **Password**: wardenPass2026
- **Details**: Dr. Arthur Vance, Chief Warden

## 📁 Project Structure

```
HostelSync/
├── backend/
│   ├── src/main/java/com/hostelsync/backend/
│   │   ├── config/          # Security, data seeding
│   │   ├── controller/      # REST API endpoints
│   │   ├── dto/             # Data transfer objects
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Custom exceptions
│   │   ├── repository/      # Data access layer
│   │   ├── security/        # JWT filter, UserDetails
│   │   └── service/         # Business logic
│   ├── uploads/             # File storage directory
│   └── pom.xml
│
└── frontend/
    ├── src/
    │   ├── components/      # Reusable UI components
    │   ├── contexts/        # React contexts (Auth)
    │   ├── lib/             # Utilities (API client)
    │   ├── pages/           # Route components
    │   └── types/           # TypeScript definitions
    ├── .env                 # Environment configuration
    └── package.json
```

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/login` - Login with email/password

### Students (WARDEN only)
- `GET /api/students` - List all students
- `POST /api/students` - Create new student
- `GET /api/students/:id` - Get student by ID
- `PATCH /api/students/:id` - Update student

### Rooms
- `GET /api/rooms` - List all rooms (with occupancy)
- `GET /api/rooms/:id` - Get room details

### Complaints
- `POST /api/complaints` - Create complaint (multipart)
- `GET /api/complaints` - List complaints (filtered by role)
- `GET /api/complaints/:id` - Get complaint by ID
- `PATCH /api/complaints/:id/status` - Update complaint status (WARDEN only)

### Dashboard (WARDEN only)
- `GET /api/dashboard/stats` - Get system statistics

### Files
- `GET /api/files/:filename` - Download file

## 🧪 Testing

### Backend Tests
```bash
cd backend
.\mvnw.cmd test
```

### Frontend Tests
```bash
cd frontend
npm run test
```

### Build for Production
```bash
# Backend
cd backend
.\mvnw.cmd clean package

# Frontend
cd frontend
npm run build
```

## 🔧 Configuration

### Environment Variables

#### Backend (`application.properties`)
```properties
# Database
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/hostelsync}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:your_password}

# JWT
app.jwt.secret=${JWT_SECRET:your-secret-key-at-least-256-bits-long-for-HS256}
app.jwt.expiration-ms=${JWT_EXPIRATION:86400000}

# File Upload
app.upload.dir=${UPLOAD_DIR:./uploads}
app.upload.max-file-size=${MAX_FILE_SIZE:10485760}

# CORS
app.cors.allowed-origins=${CORS_ORIGINS:http://localhost:3000}
```

#### Frontend (`.env`)
```env
VITE_USE_REAL_BACKEND=true
VITE_API_BASE_URL=http://localhost:8080/api
```

### Production Considerations

1. **Database Password**: Never commit real passwords. Use environment variables.
2. **JWT Secret**: Generate a secure random key (at least 256 bits for HS256).
3. **CORS Origins**: Restrict to your actual frontend domain.
4. **File Storage**: Consider cloud storage (S3, Azure Blob) for production.
5. **HTTPS**: Always use HTTPS in production.

## 📊 Database Schema

### Core Entities
- **User**: Authentication and authorization
- **Student**: Student profile linked to User (1-to-1)
- **Warden**: Warden profile linked to User (1-to-1)
- **Hostel**: Hostel information
- **Room**: Room details with capacity
- **Complaint**: Complaint records with status/priority
- **ComplaintTimeline**: Status change history
- **ComplaintAttachment**: File attachments

### Relationships
- User ↔ Student (1-to-1)
- User ↔ Warden (1-to-1)
- Hostel → Rooms (1-to-many)
- Room → Students (1-to-many, optional)
- Student → Complaints (1-to-many)
- Complaint → Timeline (1-to-many)
- Complaint → Attachments (1-to-many)

## 🐛 Troubleshooting

### Backend won't start
- Verify MySQL is running: `mysql -u root -p`
- Check database exists: `SHOW DATABASES;`
- Verify Java version: `java -version` (should be 21)

### Frontend can't connect to backend
- Verify backend is running: `curl http://localhost:8080/api/health`
- Check `.env` file has correct `VITE_API_BASE_URL`
- Clear browser cache and restart dev server

### Login fails
- Check browser console for errors
- Verify credentials match seeded data
- Check backend logs for authentication errors

### File upload fails
- Verify `uploads/` directory exists and is writable
- Check file size is under 10MB
- Verify file type is allowed (images, PDFs)

## 📝 Development Notes

### Seeded Data
On first startup, the backend automatically seeds:
- 1 Hostel: "HostelHub - A-Block"
- 5 Rooms: 101, 204, 205, 302, 304
- 7 Students: Rahul, Aman, Arjun, Devansh, Tanya, Karan, Rohan
- 1 Warden: Dr. Arthur Vance

### Security Boundaries
- Students can only view/edit their own profile
- Students can only view their own complaints
- Wardens can view all students and complaints
- Role enforcement at service layer prevents privilege escalation
- JWT tokens expire after 24 hours (configurable)

### File Upload
- Files stored locally in `backend/uploads/`
- Filename format: `UUID_originalname.ext`
- Maximum size: 10MB (configurable)
- Allowed types: images, PDFs (extensible)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## 📄 License

This project is for educational purposes.

## 👥 Authors

Built as a college hostel management system demonstration.

---

**Note**: This is a development setup. For production deployment, implement additional security measures, use environment-specific configurations, and follow DevOps best practices.
