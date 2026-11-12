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
- Role-based access control (`STUDENT`, `WARDEN`, `DGM`)
- IDOR (Insecure Direct Object Reference) protection
- Secure file upload validation (JPEG, PNG, WEBP, MP4, WEBM, MOV up to 50 MB)
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

3. Configure local database credentials without modifying tracked files:
   Create the gitignored `backend/src/main/resources/application-local.properties` (or pass `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` via environment variables). Never commit real database passwords into `application.properties`.

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

3. Run the application (the `local` profile loads the ignored
   `application-local.properties` and enables the development demo seeder):
```bash
# Windows (quote the -D argument so PowerShell does not split it)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

# Linux/Mac
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

   If you cannot use a local profile file, supply the database credentials as
   environment variables and enable the seeder explicitly:
   `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ENABLE_DATA_SEEDER=true`.
   Never enable the seeder in production.

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
# Leave blank to call the backend on the same hostname as the browser (port 8080).
# This is required when the app is opened from another device on the LAN.
VITE_API_BASE_URL=
```

4. Start development server:
```bash
npm run dev
```

5. Access the application at http://localhost:3000

## 🔐 Demo Credentials (Development Only)

When explicitly enabled for local development (`ENABLE_DATA_SEEDER=true` or `-Dspring-boot.run.profiles=local`), `DataSeeder` creates development demo accounts. **Keep `ENABLE_DATA_SEEDER=false` in production.**

### Student Account
- **Email**: rahul.sharma@hostelhub.edu
- **Password**: studentPass2026
- **Details**: Room 204 (Block A), B.Tech Computer Science

### Warden Account
- **Email**: warden.vance@hostelhub.edu
- **Password**: wardenPass2026
- **Details**: Dr. Arthur Vance, Chief Warden

### DGM Account
- **Email**: dgm.director@hostelhub.edu
- **Password**: dgmPass2026
- **Details**: Dr. Rajesh Verma, Director General of Hostels

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

### Frontend Checks
```bash
cd frontend
node --test src/utils/wardenForm.test.mjs
npm run lint
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

#### Backend (`application.properties` / `application-prod.properties`)
```properties
# Database
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/hostelsync}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}

# JWT
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}

# Seeder, Bootstrap & Storage
app.seeder.enabled=${ENABLE_DATA_SEEDER:false}
app.bootstrap.enabled=${ENABLE_INITIAL_BOOTSTRAP:false}
app.storage.type=${STORAGE_TYPE:local}
app.upload.dir=${UPLOAD_DIR:uploads}
app.upload.max-file-size-bytes=${UPLOAD_MAX_SIZE:52428800}

# CORS
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS}
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
- For local storage (`STORAGE_TYPE=local`), verify `uploads/` directory exists and is writable
- For production (`STORAGE_TYPE=supabase`), verify `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, and private bucket `hostelsync-files`
- Check file size is under 50 MB (`UPLOAD_MAX_SIZE=52428800`)
- Verify file type is allowed (`JPEG`, `PNG`, `WEBP`, `MP4`, `WEBM`, `MOV`; PDFs are not supported)

## 📝 Development Notes

### Seeded Data (`ENABLE_DATA_SEEDER=true` local dev only)
When `DataSeeder` is enabled on an empty local database, it creates:
- 1 Hostel: "HostelHub Campus Hostel"
- 5 Rooms: 101, 204, 205, 302, 304
- 7 Students: Rahul, Aman, Arjun, Devansh, Tanya, Karan, Rohan
- 1 Warden: Dr. Arthur Vance
- 1 DGM: Dr. Rajesh Verma

### Security Boundaries
- Students can only view/edit their own allowed profile fields and complaints
- Wardens manage students, rooms, notices, and complaints
- DGMs manage hostels, wardens, escalations, and SLA oversight
- Role enforcement at controller and service layers prevents privilege escalation
- JWT tokens expire after 24 hours (configurable via `JWT_EXPIRATION_MS`)

### File Upload
- Stored in `backend/uploads/` locally (`STORAGE_TYPE=local`) or private Supabase bucket `hostelsync-files` in production (`STORAGE_TYPE=supabase`)
- Filename format: `<UUID>.<ext>`
- Maximum size: 50 MB (`UPLOAD_MAX_SIZE=52428800`, configurable)
- Allowed types: `JPEG`, `PNG`, `WEBP`, `MP4`, `WEBM`, `MOV`

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
