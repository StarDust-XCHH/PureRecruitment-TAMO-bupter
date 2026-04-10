# MO route sections (`routes`)

JSPF fragments: each defines one **route panel** (`data-route` + `id="route-<name>"`) toggled by `assets/mo/js/modules/route-nav.js`.

| File | Route | Content |
| --- | --- | --- |
| `mo-route-dashboard.jspf` | `dashboard` | Overview stats and insight cards tied to dashboard JS. |
| `mo-route-jobs.jspf` | `jobs` | Job list, search/pagination, and publish form for `/api/mo/jobs`. |
| `mo-route-applicants.jspf` | `applicants` | Per-course applicant list, MO selection actions, and `#moApplicantDetailModal` for detail / resume / comments (driven by `applicants.js`). |
| `mo-route-profile.jspf` | `profile` | Profile form, avatar upload, password change; front-end logic in `assets/mo/js/modules/profile.js`. |

Only one route panel should be `.active` at a time; the nav module sets initial route to `dashboard`.
