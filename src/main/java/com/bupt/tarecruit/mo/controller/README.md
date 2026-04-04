# MO servlets (`com.bupt.tarecruit.mo.controller`)

Jakarta servlets exposing JSON APIs for the Module Organizer (MO) workspace. They delegate persistence to `MoRecruitmentDao` and `MoAccountDao` unless noted.

| Servlet | URL pattern | Role |
| --- | --- | --- |
| `MoJobBoardServlet` | `/api/mo/jobs` | `GET` lists published jobs (v2 schema); `POST` creates a job from JSON and persists to shared recruitment data. |
| `MoApplicantsServlet` | `/api/mo/applicants` | `GET` lists applicants for a course; requires `courseCode`. |
| `MoApplicationDecisionServlet` | `/api/mo/applications/select` | `POST` records a selection decision (`courseCode`, `taId`, `decision`, optional `comment`). |
| `MoSkillTagsServlet` | `/api/mo/skill-tags`, `/api/common/skill-tags` | `GET` returns a fixed list of skill tag strings for job publish UI (also exposed under `common` for reuse). |
| `MoLoginServlet` | `/api/mo/login` | `POST` authenticates MO users with identifier (MO ID/username/email/phone) and password; returns user profile on success. |
| `MoRegisterServlet` | `/api/mo/register` | `POST` registers new MO account with validation (moId, name, username, email, phone, password); auto-initializes account, profile, and settings JSON files. |

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

All write operations are synchronized for thread safety.
