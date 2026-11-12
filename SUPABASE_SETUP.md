# Supabase Private Storage Setup Guide (`hostelsync-files`)

HostelSync uses a **private** Supabase Storage bucket in production so uploaded complaint evidence and student profile photos persist across Render container restarts and redeployments while remaining protected behind Spring Boot JWT authorization.

---

## 1. Create a Free Supabase Project

1. Sign in at [supabase.com](https://supabase.com) (free tier, no credit card required).
2. Click **New Project**.
3. Choose an Organization, enter a project name (e.g., `hostelsync-storage`), set a strong database password (not used by HostelSync, since HostelSync uses TiDB for MySQL), and pick a region close to your Render service.
4. Wait ~1–2 minutes for provisioning to finish.
5. Open **Project Settings → API** (or **Data API / API Keys**) and note:
   - **Project URL** (`https://<project-ref>.supabase.co`) → maps to `SUPABASE_URL`
   - **`service_role` secret key** → maps to `SUPABASE_SERVICE_ROLE_KEY`

> **Security Rule:** Never place `SUPABASE_SERVICE_ROLE_KEY` in `frontend/.env*`, Vercel environment variables, or any Git-tracked file. It belongs **only** on the Render backend.

---

## 2. Create the Private `hostelsync-files` Bucket

1. In the Supabase Dashboard sidebar, open **Storage**.
2. Click **New bucket**.
3. Set **Name** to `hostelsync-files`.
4. Leave **Public bucket** **OFF** (Private).
5. Optionally set the file size limit to `50 MB` (the Supabase Free Tier maximum per file; matches Spring Boot's `50MB` multipart limit).
6. Click **Save**.

No public RLS policies are needed because the Spring Boot backend authenticates to Supabase using the `service_role` key and enforces HostelSync's own JWT + role/ownership checks before returning files.

---

## 3. Required Environment Variables (Render Backend Only)

Configure these variables in **Render Dashboard → Your Backend Web Service → Environment**:

| Variable | Value | Notes |
|---|---|---|
| `STORAGE_TYPE` | `supabase` | Default in `application-prod.properties` |
| `SUPABASE_URL` | `https://<project-ref>.supabase.co` | Your Supabase Project URL |
| `SUPABASE_SERVICE_ROLE_KEY` | `<your-service-role-key>` | Backend-only secret |
| `SUPABASE_BUCKET` | `hostelsync-files` | Default bucket name |
| `FILE_BASE_URL` | `/api/files` | Default; keeps relative `/api/files/{name}` links |
| `UPLOAD_MAX_SIZE` | `52428800` | Optional (bytes); defaults to `52428800` (50 MB) |

---

## 4. Manually Migrate Existing Local `.webp` Files

Your local `backend/uploads/` folder currently contains two existing files:
- `84af4e71-c2a7-4303-9cc8-ce399e57d264.webp`
- `97976d64-6820-4b2d-b4ee-44c0e80abb7b.webp`

To preserve existing database references (`/api/files/{storedFileName}` and `complaint_attachments.stored_file_name`) without modifying database records:

1. Open **Supabase Dashboard → Storage → `hostelsync-files`**.
2. Click **Upload files** at the **root** of the `hostelsync-files` bucket (do **not** place them inside a subfolder).
3. Select both `.webp` files from `C:\HostelSync\backend\uploads\`.
4. Verify the uploaded object names in Supabase match the exact UUID filenames above.

---

## 5. Switching Between Local and Supabase Storage

- **Local Development (`STORAGE_TYPE=local`)**:
  - `application.properties` defaults to `app.storage.type=${STORAGE_TYPE:local}`.
  - Files are stored in `backend/uploads/` with **no Supabase credentials required**.
- **Production (`STORAGE_TYPE=supabase`)**:
  - `application-prod.properties` defaults to `app.storage.type=${STORAGE_TYPE:supabase}`.
  - Spring Boot fails fast at startup if `SUPABASE_URL` or `SUPABASE_SERVICE_ROLE_KEY` is missing, and never silently falls back to local disk.

---

## 6. How to Verify Uploads & Downloads After Deployment

1. **Upload Verification**:
   - Log in as a **Student** and submit a complaint with an image attachment, or log in as a **Warden** and upload a student profile photo.
   - Open **Supabase Dashboard → Storage → `hostelsync-files`** and confirm the new UUID-named file appears.
2. **Authorized Download Verification**:
   - View the complaint as the owning **Student**, **Warden**, or **DGM** — the attachment preview/download should load via `/api/files/{uuid}` with `Authorization: Bearer <token>`.
   - Log in as a **different Student** and attempt to fetch the same `/api/files/{uuid}` URL with their JWT — the backend must return `403 Forbidden`.
   - Fetch `/api/files/{uuid}` without a token (or open directly in an incognito tab) — it must be rejected.

---

## 7. Supabase Free-Tier Limitations

- **Total Storage**: **1 GB** per project.
- **Monthly Egress (Bandwidth)**: **5 GB** per month.
- **Max File Size**: **50 MB** per file (matches HostelSync's 50 MB backend limit).
- **7-Day Inactivity Pausing**: Supabase automatically pauses Free projects after **7 days of zero API/database activity**. If paused, uploaded files remain preserved and can be restored from the Supabase Dashboard (for up to 1 year).

