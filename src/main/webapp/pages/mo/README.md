# MO workspace pages

Server-rendered entry for the Module Organizer home UI.

- **`mo-home.jsp`** — Shell layout: sidebar, top bar, welcome card, and four route sections (dashboard, jobs, applicants, profile). Reuses shared TA layout/component styles plus MO-specific `assets/mo/css/mo-home.css`. Loads MO JS modules in order: `settings`, `route-nav`, `modal`, `job-board`, `applicants`, `dashboard`, `profile`, then `mo-home.js` bootstrap.

Includes are under `partials/` (layout, welcome, modals) and `routes/` (per-route markup). No JSP logic beyond composition; client behavior lives in `assets/mo/js/`.
