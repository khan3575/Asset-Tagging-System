# Frontend

Plain HTML/CSS/JS, served in dev by Vite. Talks to the Spring Boot backend through `/api/**`,
which Vite proxies to `http://localhost:8080` (see `vite.config.js`) so the browser sees one origin
and session cookies work with no CORS setup.

## Prerequisites

- Node.js and npm installed (tested with Node v26.5.0 / npm 11.17.0 — check yours with `node -v` / `npm -v`)

## Setup

```bash
cd frontend
npm install
```

## Running locally

Two things need to be running at the same time:

1. **Backend** (from the repo root):
   ```bash
   ./mvnw spring-boot:run
   ```
2. **Frontend** (from `frontend/`):
   ```bash
   npm run dev
   ```

Then open the URL Vite prints (usually `http://localhost:5173`).

If API calls fail with a connection error, check that the backend is actually running on port 8080 —
the frontend dev server doesn't start it for you.

## Build

```bash
npm run build
```

Outputs static files to `frontend/dist/`, deployable anywhere.
