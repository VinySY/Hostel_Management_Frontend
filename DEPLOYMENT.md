# HostelSync - Deployment Guide

This guide covers deploying HostelSync to various environments.

## 🚨 Pre-Deployment Checklist

- [ ] All tests passing
- [ ] Environment variables configured
- [ ] Database credentials secured
- [ ] JWT secret changed from default
- [ ] CORS origins restricted to production domain
- [ ] File upload directory configured
- [ ] HTTPS enabled
- [ ] Production database created
- [ ] Backup strategy in place

---

## 📦 Build for Production

### Backend (JAR file)
```bash
cd backend
.\mvnw.cmd clean package -DskipTests

# Output: target/backend-0.0.1-SNAPSHOT.jar
```

### Frontend (Static files)
```bash
cd frontend
npm run build

# Output: dist/ directory with optimized assets
```

---

## 🔧 Environment Configuration

### Production Environment Variables

#### Backend (`backend/src/main/resources/application-prod.properties`)
Production configuration is included in `application-prod.properties` and activated by default in `backend/Dockerfile` via `SPRING_PROFILES_ACTIVE=prod`. Supply all secrets and environment-specific settings strictly via environment variables (no hardcoded production defaults):
```properties
# Database (TiDB Cloud Starter / MySQL 8 over TLS)
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.hikari.maximum-pool-size=${DB_POOL_MAX_SIZE:5}
spring.datasource.hikari.minimum-idle=${DB_POOL_MIN_IDLE:1}

# JPA (validate only — apply deployment/mysql/hostelsync_schema_tidb.sql first)
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# JWT (256-bit+ secret required, no fallback in prod)
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}

# Seeder, Schedulers & Storage
app.seeder.enabled=${ENABLE_DATA_SEEDER:false}
app.jobs.enabled=${ENABLE_SCHEDULED_JOBS:true}
app.storage.type=${STORAGE_TYPE:supabase}
app.file-base-url=${FILE_BASE_URL:/api/files}
app.upload.max-file-size-bytes=${UPLOAD_MAX_SIZE:52428800}

# One-Time Initial Bootstrap (disabled by default)
app.bootstrap.enabled=${ENABLE_INITIAL_BOOTSTRAP:false}
app.bootstrap.hostel.name=${BOOTSTRAP_HOSTEL_NAME:}
app.bootstrap.hostel.address=${BOOTSTRAP_HOSTEL_ADDRESS:}
app.bootstrap.dgm.name=${BOOTSTRAP_DGM_NAME:}
app.bootstrap.dgm.email=${BOOTSTRAP_DGM_EMAIL:}
app.bootstrap.dgm.password=${BOOTSTRAP_DGM_PASSWORD:}
app.bootstrap.dgm.employee-id=${BOOTSTRAP_DGM_EMPLOYEE_ID:}
app.bootstrap.dgm.designation=${BOOTSTRAP_DGM_DESIGNATION:}
app.bootstrap.dgm.office-location=${BOOTSTRAP_DGM_OFFICE_LOCATION:}
app.bootstrap.dgm.contact-extension=${BOOTSTRAP_DGM_CONTACT_EXTENSION:}
app.bootstrap.warden.name=${BOOTSTRAP_WARDEN_NAME:}
app.bootstrap.warden.email=${BOOTSTRAP_WARDEN_EMAIL:}
app.bootstrap.warden.password=${BOOTSTRAP_WARDEN_PASSWORD:}
app.bootstrap.warden.employee-id=${BOOTSTRAP_WARDEN_EMPLOYEE_ID:}
app.bootstrap.warden.phone=${BOOTSTRAP_WARDEN_PHONE:}

# Supabase Private Storage (Backend-Only Service Key)
app.supabase.url=${SUPABASE_URL:}
app.supabase.service-role-key=${SUPABASE_SERVICE_ROLE_KEY:}
app.supabase.bucket=${SUPABASE_BUCKET:hostelsync-files}

# CORS - Replace with exact frontend domain(s)
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS}

# Server, Logging & Safe Production Error Payloads
server.port=${PORT:8080}
server.forward-headers-strategy=framework
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-stacktrace=never
server.error.include-exception=false
logging.level.root=${LOG_LEVEL_ROOT:INFO}
```

> **Important:** Keep `ENABLE_DATA_SEEDER=false` in production (never seed demo accounts on a live database). Enable `ENABLE_INITIAL_BOOTSTRAP=true` only during initial setup of an empty production database, then disable it immediately afterward.

#### Cloud Production Stack (Render Free + Vercel Hobby + TiDB Cloud Starter + Supabase)
- **Database (TiDB Cloud Starter):** Import [`deployment/mysql/hostelsync_schema_tidb.sql`](deployment/mysql/hostelsync_schema_tidb.sql) (`23` tables) into `hostelsync` before starting the backend, and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` with TLS verification enabled.
- **Storage (Supabase):** Create a **private** bucket named `hostelsync-files` and set `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` (backend-only), and `SUPABASE_BUCKET=hostelsync-files`.
- **Backend (Render Web Service):**
  - **Root Directory:** `backend`
  - **Runtime:** `Docker` (uses [`backend/Dockerfile`](backend/Dockerfile) with Java 21, non-root execution, `SPRING_PROFILES_ACTIVE=prod`, and Render 512 MB RAM JVM options)
  - **Health Check Path:** `/api/health`
  - **Required Env Vars:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`
  - **Optional / Defaulted Env Vars:** `STORAGE_TYPE` (`supabase`), `SUPABASE_BUCKET` (`hostelsync-files`), `FILE_BASE_URL` (`/api/files`), `UPLOAD_MAX_SIZE` (`52428800`), `JWT_EXPIRATION_MS` (`86400000`), `DB_POOL_MAX_SIZE` (`5`), `DB_POOL_MIN_IDLE` (`1`), `ENABLE_SCHEDULED_JOBS` (`true`), `ENABLE_DATA_SEEDER` (`false`), `ENABLE_INITIAL_BOOTSTRAP` (`false`), `LOG_LEVEL_ROOT` (`INFO`)
- **Frontend (Vercel Hobby):** See [frontend/DEPLOYMENT.md](frontend/DEPLOYMENT.md) (Root Directory `frontend`, SPA rewrite via [`frontend/vercel.json`](frontend/vercel.json)).

#### Safe First-Startup Bootstrap Procedure (Empty Production Database)
1. Ensure TiDB `hostelsync` contains only the imported schema (`0` existing users/hostels) and `ENABLE_DATA_SEEDER` is `false`.
2. Temporarily configure in Render:
   - `ENABLE_INITIAL_BOOTSTRAP=true`
   - Required: `BOOTSTRAP_HOSTEL_NAME`, `BOOTSTRAP_DGM_NAME`, `BOOTSTRAP_DGM_EMAIL`, `BOOTSTRAP_DGM_PASSWORD` (`12–100` characters)
   - Optional Hostel/DGM metadata: `BOOTSTRAP_HOSTEL_ADDRESS`, `BOOTSTRAP_DGM_EMPLOYEE_ID`, `BOOTSTRAP_DGM_DESIGNATION`, `BOOTSTRAP_DGM_OFFICE_LOCATION`, `BOOTSTRAP_DGM_CONTACT_EXTENSION`
   - Optional initial Warden *(only created if all four required fields are provided)*: `BOOTSTRAP_WARDEN_NAME`, `BOOTSTRAP_WARDEN_EMAIL`, `BOOTSTRAP_WARDEN_PASSWORD` (`12–100` characters), `BOOTSTRAP_WARDEN_EMPLOYEE_ID`, optional `BOOTSTRAP_WARDEN_PHONE`
3. Deploy/start the Render service once. The transactional bootstrap creates the initial `Hostel`, BCrypt-hashed `DGM` user & profile, `HostelDgmAssignment`, and optional `Warden`.
4. Verify `/api/health` returns `{"status":"UP","service":"HostelSync Backend"}` and confirm the bootstrapped DGM can log in on Vercel and view the assigned hostel.
5. **Immediately** set `ENABLE_INITIAL_BOOTSTRAP=false` and remove every `BOOTSTRAP_*` variable in Render.
6. Trigger a clean restart/redeploy and verify normal startup and DGM login still succeed with `ENABLE_DATA_SEEDER=false`.

#### Frontend
Configure Vercel environment variables (see [frontend/DEPLOYMENT.md](frontend/DEPLOYMENT.md) and `frontend/.env.example`):
```env
VITE_USE_REAL_BACKEND=true
VITE_API_BASE_URL=https://api.hostelsync.yourdomain.com/api
```

> **Free-Tier Cloud Guide:** For deploying with **Render Free** (`backend/Dockerfile`), **Vercel Hobby** (`frontend/vercel.json`), **TiDB Cloud Starter** (`deployment/mysql/hostelsync_schema_tidb.sql`), and **Supabase Private Storage**, also refer to [frontend/DEPLOYMENT.md](frontend/DEPLOYMENT.md), [SUPABASE_SETUP.md](SUPABASE_SETUP.md), and [deployment/mysql/README.md](deployment/mysql/README.md).

---

## 🌐 Deployment Options

### Option 1: Traditional Server (VPS/EC2)

#### 1. Server Setup
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Java 21
sudo apt install openjdk-21-jdk -y

# Install MySQL
sudo apt install mysql-server -y
sudo mysql_secure_installation

# Install Nginx
sudo apt install nginx -y

# Install Node.js (for serving frontend if needed)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install nodejs -y
```

#### 2. Database Setup
```bash
sudo mysql -u root -p

# In MySQL:
CREATE DATABASE hostelsync;
CREATE USER 'hostelsync_user'@'localhost' IDENTIFIED BY 'secure_password_here';
GRANT ALL PRIVILEGES ON hostelsync.* TO 'hostelsync_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### 3. Backend Deployment
```bash
# Create app directory
sudo mkdir -p /opt/hostelsync/backend
sudo mkdir -p /var/hostelsync/uploads

# Copy JAR file
sudo cp backend-0.0.1-SNAPSHOT.jar /opt/hostelsync/backend/

# Create systemd service
sudo nano /etc/systemd/system/hostelsync-backend.service
```

**Service file content:**
```ini
[Unit]
Description=HostelSync Backend
After=syslog.target mysql.service

[Service]
User=hostelsync
ExecStart=/usr/bin/java -jar /opt/hostelsync/backend/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
SuccessExitStatus=143
Environment="DB_URL=jdbc:mysql://localhost:3306/hostelsync"
Environment="DB_USERNAME=hostelsync_user"
Environment="DB_PASSWORD=secure_password_here"
Environment="JWT_SECRET=your-super-secure-jwt-secret-at-least-256-bits-long"
Environment="UPLOAD_DIR=/var/hostelsync/uploads"
Environment="CORS_ALLOWED_ORIGINS=https://hostelsync.yourdomain.com"

[Install]
WantedBy=multi-user.target
```

```bash
# Create user
sudo useradd -r -s /bin/false hostelsync

# Set permissions
sudo chown -R hostelsync:hostelsync /opt/hostelsync
sudo chown -R hostelsync:hostelsync /var/hostelsync

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable hostelsync-backend
sudo systemctl start hostelsync-backend

# Check status
sudo systemctl status hostelsync-backend
```

#### 4. Frontend Deployment with Nginx
```bash
# Copy frontend build
sudo mkdir -p /var/www/hostelsync
sudo cp -r frontend/dist/* /var/www/hostelsync/

# Configure Nginx
sudo nano /etc/nginx/sites-available/hostelsync
```

**Nginx configuration:**
```nginx
server {
    listen 80;
    server_name hostelsync.yourdomain.com;

    # Redirect HTTP to HTTPS (after SSL setup)
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name hostelsync.yourdomain.com;

    # SSL certificates (use Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/hostelsync.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/hostelsync.yourdomain.com/privkey.pem;

    # Frontend
    root /var/www/hostelsync;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend API proxy
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
}
```

```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/hostelsync /etc/nginx/sites-enabled/

# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

#### 5. SSL Setup (Let's Encrypt)
```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d hostelsync.yourdomain.com
sudo systemctl reload nginx
```

---

### Option 2: Docker Deployment

#### 1. Backend Dockerfile
Create `backend/Dockerfile`:
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2. Frontend Dockerfile
Create `frontend/Dockerfile`:
```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

#### 3. Docker Compose
Create `docker-compose.prod.yml`:
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: hostelsync
      MYSQL_USER: hostelsync_user
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - hostelsync-network
    restart: unless-stopped

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:mysql://mysql:3306/hostelsync
      DB_USERNAME: hostelsync_user
      DB_PASSWORD: ${MYSQL_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      UPLOAD_DIR: /app/uploads
      CORS_ALLOWED_ORIGINS: ${FRONTEND_URL}
    volumes:
      - uploads:/app/uploads
    depends_on:
      - mysql
    networks:
      - hostelsync-network
    restart: unless-stopped

  frontend:
    build: ./frontend
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - backend
    networks:
      - hostelsync-network
    restart: unless-stopped

volumes:
  mysql-data:
  uploads:

networks:
  hostelsync-network:
```

#### 4. Deploy with Docker
```bash
# Create .env file
cat > .env << EOF
MYSQL_ROOT_PASSWORD=secure_root_password
MYSQL_PASSWORD=secure_user_password
JWT_SECRET=your-super-secure-jwt-secret-at-least-256-bits
FRONTEND_URL=https://hostelsync.yourdomain.com
EOF

# Build and start
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose -f docker-compose.prod.yml logs -f

# Stop
docker-compose -f docker-compose.prod.yml down
```

---

### Option 3: Cloud Platform (AWS Example)

#### AWS Elastic Beanstalk
1. Install AWS CLI and EB CLI
2. Initialize:
```bash
cd backend
eb init -p java-21 hostelsync-backend
```

3. Create environment:
```bash
eb create hostelsync-prod --instance-type t3.medium --database
```

4. Set environment variables:
```bash
eb setenv DB_USERNAME=admin \
          DB_PASSWORD=your_password \
          JWT_SECRET=your_secret \
          CORS_ALLOWED_ORIGINS=https://your-frontend.s3-website.amazonaws.com
```

5. Deploy:
```bash
eb deploy
```

#### AWS S3 + CloudFront (Frontend)
```bash
# Build frontend
cd frontend
npm run build

# Upload to S3
aws s3 sync dist/ s3://hostelsync-frontend

# Invalidate CloudFront cache
aws cloudfront create-invalidation --distribution-id YOUR_DIST_ID --paths "/*"
```

---

## 🔍 Post-Deployment Verification

### 1. Health Check
```bash
curl https://api.hostelsync.yourdomain.com/api/health
```
Expected: `{"status":"UP","service":"HostelSync Backend"}`

### 2. Frontend Access
Open browser: https://hostelsync.yourdomain.com

### 3. Test Login
Sign in on the deployed frontend using your bootstrapped DGM (or Warden/Student) production credentials — never enable development demo accounts (`ENABLE_DATA_SEEDER`) in production.

### 4. Check Logs
```bash
# Systemd service
sudo journalctl -u hostelsync-backend -f

# Docker
docker-compose logs -f backend

# Log files
tail -f /var/log/hostelsync/application.log
```

---

## 🔐 Security Hardening

### 1. Database
- Use non-root MySQL user
- Strong passwords (20+ chars, alphanumeric + symbols)
- Restrict network access (localhost only if on same server)
- Enable SSL connections
- Regular backups

### 2. Application
- Change JWT secret (never use default)
- Rotate JWT secret periodically
- Use environment variables (never hardcode secrets)
- Enable HTTPS only
- Restrict CORS to specific domain
- Rate limiting (Spring Security)
- Input validation enabled

### 3. Server
- Keep OS updated
- Configure firewall (ufw/iptables)
- Disable root SSH login
- Use SSH keys (not passwords)
- Install fail2ban
- Regular security updates

### 4. File Uploads
- Validate file types (`JPEG`, `PNG`, `WEBP`, `MP4`, `WEBM`, `MOV`)
- Validate maximum file size (`50 MB` default via `UPLOAD_MAX_SIZE=52428800`)
- Store in a private Supabase Storage bucket (`hostelsync-files`) in production (never in a public folder or web root)
- Serve downloads strictly through JWT-authenticated backend proxy endpoints (`/api/files/{storedFileName}`) with role/ownership checks

---

## 📊 Monitoring & Logging

### 1. Application Monitoring
```bash
# Public health check endpoint
GET /api/health
# Authentication service ping
GET /api/auth/ping
```

### 2. Log Aggregation
Consider using:
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Graylog
- Splunk
- CloudWatch (AWS)

### 3. Uptime Monitoring
- UptimeRobot
- Pingdom
- StatusCake

---

## 🔄 Backup Strategy

### Database Backup
```bash
# Daily backup script
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u hostelsync_user -p hostelsync > /backup/hostelsync_$DATE.sql
gzip /backup/hostelsync_$DATE.sql

# Keep last 7 days
find /backup -name "hostelsync_*.sql.gz" -mtime +7 -delete
```

### Add to crontab:
```bash
0 2 * * * /opt/scripts/backup-db.sh
```

### Uploaded Files Backup
```bash
# Sync uploads to S3
aws s3 sync /var/hostelsync/uploads s3://hostelsync-backups/uploads
```

---

## 🚀 CI/CD Pipeline (GitHub Actions Example)

Create `.github/workflows/deploy.yml`:
```yaml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Test Backend
        run: cd backend && ./mvnw test
      - name: Test Frontend
        run: cd frontend && node --test src/utils/wardenForm.test.mjs && npm run lint && npm run build

  deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Deploy to server
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /opt/hostelsync
            git pull
            cd backend && ./mvnw clean package -DskipTests
            sudo systemctl restart hostelsync-backend
            cd ../frontend && npm ci && npm run build
            sudo cp -r dist/* /var/www/hostelsync/
            sudo systemctl reload nginx
```

---

## 📞 Support & Troubleshooting

### Common Issues

**Issue**: Backend won't start
- Check Java version: `java -version`
- Check database connection
- Review logs: `journalctl -u hostelsync-backend`

**Issue**: 502 Bad Gateway
- Backend not running
- Check Nginx proxy configuration
- Verify backend port is 8080

**Issue**: CORS errors
- Update `CORS_ALLOWED_ORIGINS` to include frontend domain
- Restart backend service

---

## 📋 Deployment Checklist

Production deployment checklist:
- [ ] Environment variables configured
- [ ] Database created and secured
- [ ] Backend JAR built and deployed
- [ ] Frontend built and served
- [ ] Nginx/Apache configured
- [ ] SSL certificates installed
- [ ] Firewall rules configured
- [ ] Database backup scheduled
- [ ] Monitoring enabled
- [ ] Logs accessible
- [ ] Health check passing
- [ ] Login tested
- [ ] All critical features tested
- [ ] Performance acceptable (<2s page load)
- [ ] Security scan completed
- [ ] Documentation updated

---

**Remember**: Never commit secrets to version control. Use environment variables or secret management services (AWS Secrets Manager, HashiCorp Vault, etc.).
