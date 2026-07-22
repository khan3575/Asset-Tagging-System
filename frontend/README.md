# Frontend

Plain HTML/JS, styled with Bootstrap 5 (via its Sass source, not the CDN build), served in dev by
Vite. Talks to the Spring Boot backend through `/api/**`, which Vite proxies to
`http://localhost:8080` (see `vite.config.js`) so the browser sees one origin and session cookies
work with no CORS setup.

## Prerequisites

- Node.js and npm installed (tested with Node v26.5.0 / npm 11.17.0 — check yours with `node -v` / `npm -v`)

## Setup

```bash
cd frontend
npm install
```

`bootstrap` and `sass` are already in `package.json`; `npm install` pulls in both — no extra config
needed for Vite to compile `.scss` files.

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

## Styling: Bootstrap via Sass

Bootstrap is installed from npm as Sass source (`node_modules/bootstrap/scss/`), not the precompiled
CDN CSS. This lets us set our own colors/fonts/spacing *before* Bootstrap generates its CSS, so
Bootstrap's own classes (`.btn-primary`, `.text-danger`, etc.) already use our values — no
after-the-fact overriding needed.

All of it is wired up in `src/main.js`:

```js
import './style.scss'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
```

`style.scss` configures Bootstrap in one place, at the top of the file:

```scss
@use "bootstrap/scss/bootstrap" as bs with (
  $primary: #6c4f3d,
  $secondary: #a67c52,
  $border-radius: .75rem,
  $font-family-base: ('Poppins', sans-serif)
  // any Bootstrap $variable can be set here — see node_modules/bootstrap/scss/_variables.scss
  // for the full list of what's available
);

// your own custom rules go below this line, so they win the cascade against Bootstrap's
```

A few conventions to follow when adding styles:

- **Never edit files inside `node_modules/bootstrap/`** — that's vendor code, wiped on every
  `npm install`. All customization happens in `src/style.scss` (or files it imports).
- **Reach for a Bootstrap utility class in your HTML/JS templates first** (`d-flex`, `mb-3`,
  `text-center`, ...) before writing custom CSS — that's most of what Bootstrap is for.
- **When you do write custom CSS, prefer Bootstrap's CSS custom properties** (`var(--bs-primary)`,
  `var(--bs-border-radius)`) over its Sass variables (`bs.$primary`) for just applying a value —
  the CSS variable is resolved live in the browser and stays correct if Bootstrap's dark mode
  (`data-bs-theme="dark"`) is ever turned on. Reach for the Sass variable/mixins (`bs.$variable`,
  `@include bs.media-breakpoint-up(...)`) only for things that must happen at build time, like
  breakpoints or color math.
- `bootstrap.bundle.min.js` includes Popper, so dropdowns/tooltips/popovers/modals work without a
  separate Popper install.
