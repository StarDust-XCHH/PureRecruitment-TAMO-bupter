# MO servlets (`com.bupt.tarecruit.mo.controller`)

Jakarta servlets exposing JSON APIs for the Module Organizer (MO) workspace. They delegate persistence to `MoRecruitmentDao` and `MoAccountDao` unless noted.

| Servlet | URL pattern | Role |
| --- | --- | --- |
| `MoJobBoardServlet` | `/api/mo/jobs` | `GET` lists jobs **for the caller’s `moId`** (query `moId`, matches `items[].ownerMoId`); `POST` creates a job: **`ownerMoId` is taken from query/body `moId`**, not from client `ownerMoId`. |
| `MoApplicantsServlet` | `/api/mo/applicants` | `GET` lists **submitted** applicants for a course; query **`courseCode`** and **`moId`** (must own the job via `ownerMoId`). Returns `items`, `count`, `unreadCount`. |
| `MoApplicantDetailServlet` | `/api/mo/applicants/detail` | `GET` single-application detail: **`moId`**, **`applicationId`**. |
| `MoApplicantsUnreadServlet` | `/api/mo/applicants/unread-count` | `GET` **`moId`** — unread count for sidebar badge. |
| `MoApplicationMarkReadServlet` | `/api/mo/applications/mark-read` | `POST` JSON `moId`, `applicationId` — MO read + sync TA `applications.json` to `UNDER_REVIEW`. |
| `MoApplicationCommentServlet` | `/api/mo/applications/comment` | `POST` JSON `moId`, `applicationId`, `text` — append MO-only comment thread. |
| `MoApplicantResumeServlet` | `/api/mo/applications/resume` | `GET` **`moId`**, **`applicationId`** — stream resume after ownership check. |
| `MoApplicationDecisionServlet` | `/api/mo/applications/select` | `POST` JSON `courseCode`, `taId`, **`moId`**, `decision` (`selected` \| `rejected`), optional `comment` → `application-status.json`. |
| `MoSkillTagsServlet` | `/api/mo/skill-tags`, `/api/common/skill-tags` | `GET` returns a fixed list of skill tag strings for job publish UI (also exposed under `common` for reuse). |
| `MoLoginServlet` | `/api/mo/login` | `POST` authenticates MO users with identifier (MO ID/username/email/phone) and password; returns user profile on success. |
| `MoRegisterServlet` | `/api/mo/register` | `POST` registers new MO account with validation (moId, name, username, email, phone, password); auto-initializes account, profile, and settings JSON files. |
| `MoProfileSettingsServlet` | `/api/mo/profile-settings`, `/mo-assets/*` | `GET`/`POST` profile and password; static avatar assets under `/mo-assets`. |

Responses use UTF-8 JSON. Error bodies for the job board follow the v2 envelope (`schema`, `version`, `success`, `message`, `items`).

## Authentication Flow

1. **Registration**: MO users register via `/api/mo/register` with required fields
2. **Login**: Authenticated via `/api/mo/login` using any identifier (MO ID, username, email, or phone)
3. **Session**: Client stores returned user data for subsequent API calls
4. **Security**: Passwords are hashed with SHA-256 and random salt before storage

## Data Files

MO authentication data is stored in three JSON files under `mountDataTAMObupter/mo/`:
- `mos.json` - Main account records with credentials
- `profiles.json` - User profiles (real name, bio, skills)
- `settings.json` - System preferences (theme, avatar)

Applicant workflow also uses (see `docs/backend/mo-ta-interaction-log.md`):
- `mo-application-read-state.json` — per `(moId, applicationId)` read timestamps
- `mo-application-comments.json` — MO comment threads keyed by `applicationId`

All write operations are synchronized for thread safety.
