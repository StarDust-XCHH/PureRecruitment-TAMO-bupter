# MO JS modules

Each module exports a function `modules.<name> = function init…(app) { … }` registered on `window.MOApp.modules` and invoked from `mo-home.js`.

| Module | Responsibility |
| --- | --- |
| `settings.js` | MO session check (`mo-user` in `sessionStorage`, then `localStorage`), theme toggle, logout redirect, surfaces user name in UI. |
| `route-nav.js` | Sidebar nav: shows one `.route` panel by `data-route`, exposes `app.activateRoute`. |
| `modal.js` | TA-style modal overlay: open/close by `data-modal-target`, integrates with user menu. |
| `job-board.js` | Fetches lists from `/api/mo/jobs?moId=…` (仅本人课程), search/pagination, publish POST（`moId` + 课程字段；`ownerMoName` 由服务端用 `mos.json` 的 `name` 写入）, skill tags from `/api/mo/skill-tags`, exposes `app.getJobs` for dashboard. |
| `applicants.js` | Loads applicants with `courseCode` + `moId` (from `app.getMoUser()`) from `/api/mo/applicants`; unread dots, sidebar badge via `/api/mo/applicants/unread-count`; detail modal → `/api/mo/applicants/detail`, mark-read `/api/mo/applications/mark-read`, comments `/api/mo/applications/comment`, resume `/api/mo/applications/resume`; decisions POST `/api/mo/applications/select` with `moId`. Exposes `app.refreshMoApplicantUnreadBadge`. Notifies `app.onApplicantsLoaded`. |
| `dashboard.js` | Aggregates open job count and applicant stats using `app.getJobs` and `onApplicantsLoaded`. |
| `profile.js` | Profile route: resolves `moId` from `mo-user` the same way as `settings.js` (`sessionStorage` → `localStorage`, then legacy `moUser` in `localStorage`). Calls `../../api/mo/profile-settings` (relative to `pages/mo/mo-home.jsp`, consistent with `job-board` / `applicants`) for GET/POST profile, avatar under `../../mo-assets/…`, and password change via `action=password` (new password trimmed client-side to match server length rules; no password values logged). Exposes `window.MoProfileModule`. |

Shared pattern: IIFE, `'use strict'`, attach to `MOApp` singleton.
