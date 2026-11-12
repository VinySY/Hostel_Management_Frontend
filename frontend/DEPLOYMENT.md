# HostelSync Frontend Deployment Guide

This guide covers configuring and deploying the HostelSync React + TypeScript + Vite frontend (`frontend/`) for local development and free-tier Vercel hosting.

---

## 1. Environment Variables

Copy `.env.example` to `.env` for local development (`frontend/.env` is gitignored):

| Variable | Local Development | Production (Vercel) | Notes |
| --- | --- | --- | --- |
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | `https://<your-render-backend>.onrender.com/api` | Base URL of the Spring Boot REST API (trailing slashes are normalized automatically). |
| `VITE_USE_REAL_BACKEND` | `true` | `true` | Enables live Spring Boot JWT API requests. Always enforced as `true` in `vite build` production bundles. |
| `VITE_ENABLE_DEMO_LOGIN` | `true` (or `false`) | Omit / `false` | Controls the development-only **Fast Demo Access** bar on the login page. Automatically stripped from production builds (`import.meta.env.DEV === false`). |
| `VITE_APP_NAME` | `HostelSync` | `HostelSync` | Application display name. |
| `VITE_APP_ENV` | `development` | `production` | Environment label. |

> **Security Warning:** Every `VITE_*` variable is embedded into the public client-side JavaScript bundle at build time. **Never** put backend secrets (`JWT_SECRET`, `DB_PASSWORD`, `SUPABASE_SERVICE_ROLE_KEY`, database URLs, or private API keys) in `VITE_*` variables or `.env` files shipped with the frontend.

---

## 2. Local Development Setup

1. Ensure the Spring Boot backend is running on port `8080` (`http://localhost:8080/api`).
2. Install frontend dependencies and start the Vite dev server on port `3000`:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
3. Run static type checking and production bundling locally before deploying:
   ```bash
   npm run lint
   npm run build
   ```

---

## 3. Deploying to Vercel (Free Tier)

### Project Settings in Vercel
- **Framework Preset:** `Vite`
- **Root Directory:** `frontend`
- **Build Command:** `npm run build`
- **Output Directory:** `dist`
- **Install Command:** `npm install`

### Environment Variables in Vercel
In **Project Settings → Environment Variables**, configure at minimum:
- `VITE_API_BASE_URL` = `https://<your-render-backend>.onrender.com/api`
- `VITE_USE_REAL_BACKEND` = `true`
- `VITE_APP_NAME` = `HostelSync`
- `VITE_APP_ENV` = `production`

Do **not** enable `VITE_ENABLE_DEMO_LOGIN` in production (Vite production builds already exclude demo login UI and demo credentials via `import.meta.env.DEV` dead-code elimination).

### Client-Side SPA Routing (`vercel.json`)
`frontend/vercel.json` configures a SPA fallback rewrite:
```json
{
  "rewrites": [
    {
      "source": "/((?!api/).*)",
      "destination": "/index.html"
    }
  ]
}
```
This ensures direct page refreshes or deep links (such as `/student/complaints`, `/warden/rooms`, or `/dgm/dashboard`) serve `index.html` while leaving any `/api/*` paths excluded.

---

## 4. Verifying Frontend-to-Backend Connectivity

After deploying both the Render backend and Vercel frontend:

1. **CORS Configuration:** Confirm the backend `CORS_ALLOWED_ORIGINS` environment variable includes your exact Vercel deployment origin (`https://<your-app>.vercel.app`, plus any preview domain if used).
2. **Authentication Test:** Open the deployed Vercel URL, sign in with real STUDENT, WARDEN, and DGM credentials, and verify `POST <VITE_API_BASE_URL>/auth/login` returns `200 OK` with a JWT and routes to each role's dashboard.
3. **Protected File Fetch Test:** Open a student profile photo or complaint attachment and verify browser network requests to `/api/files/...` include `Authorization: Bearer <token>` and render successfully without exposing signed storage URLs or credentials in query parameters.
4. **Deep-Link Reload Test:** Refresh the browser on a nested route (for example `/student/notices`) and confirm Vercel returns `index.html` without a `404`.

