# MO JS modules

Each module exports a function `modules.<name> = function init…(app) { … }` registered on `window.MOApp.modules` and invoked from `mo-home.js`.

| Module | Responsibility |
| --- | --- |
| `settings.js` | MO session check (`mo-user` in storage), theme toggle, logout redirect, surfaces user name in UI. |
| `route-nav.js` | Sidebar nav: shows one `.route` panel by `data-route`, exposes `app.activateRoute`. |
| `modal.js` | TA-style modal overlay: open/close by `data-modal-target`, integrates with user menu. |
| `job-board.js` | Fetches and lists jobs from `/api/mo/jobs`, search/pagination, publish form POST, optional skill tags from `/api/mo/skill-tags`, exposes `app.getJobs` for dashboard. |
| `applicants.js` | Loads applicants by selected `courseCode` from `/api/mo/applicants`, renders list, can POST decisions via `/api/mo/applications/select`; notifies `app.onApplicantsLoaded`. |
| `dashboard.js` | Aggregates open job count and applicant stats using `app.getJobs` and `onApplicantsLoaded`. |

Shared pattern: IIFE, `'use strict'`, attach to `MOApp` singleton.
