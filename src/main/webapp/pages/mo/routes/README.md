# MO route sections (`routes`)

JSPF fragments: each defines one **route panel** (`data-route` + `id="route-<name>"`) toggled by `assets/mo/js/modules/route-nav.js`.

| File | Route | Content |
| --- | --- | --- |
| `mo-route-dashboard.jspf` | `dashboard` | Unified masthead + four tiles: open modules, pending-only card (opens `candidates-pipeline` modal = pending list only), hired, MO AI placeholder. |
| `mo-route-jobs.jspf` | `jobs` | Job list, search/pagination, and publish form for `/api/mo/jobs`. |
| `mo-route-applicants.jspf` | `applicants` | Per-course applicant table (sorted by submitted time, actions column), MO selection actions, `#moApplicantDetailModal` (detail / resume / workflow), and `#moApplicantMessagesModal` for MO-only messages (`applicants.js`). |
| `mo-route-profile.jspf` | `profile` | Profile form, avatar upload, password change; front-end logic in `assets/mo/js/modules/profile.js`. |

Only one route panel should be `.active` at a time; the nav module sets initial route to `dashboard`.
