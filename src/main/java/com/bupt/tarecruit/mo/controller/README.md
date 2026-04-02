# MO servlets (`com.bupt.tarecruit.mo.controller`)

Jakarta servlets exposing JSON APIs for the Module Organizer (MO) workspace. They delegate persistence to `MoRecruitmentDao` unless noted.

| Servlet | URL pattern | Role |
| --- | --- | --- |
| `MoJobBoardServlet` | `/api/mo/jobs` | `GET` lists published jobs (v2 schema); `POST` creates a job from JSON and persists to shared recruitment data. |
| `MoApplicantsServlet` | `/api/mo/applicants` | `GET` lists applicants for a course; requires `courseCode`. |
| `MoApplicationDecisionServlet` | `/api/mo/applications/select` | `POST` records a selection decision (`courseCode`, `taId`, `decision`, optional `comment`). |
| `MoSkillTagsServlet` | `/api/mo/skill-tags`, `/api/common/skill-tags` | `GET` returns a fixed list of skill tag strings for job publish UI (also exposed under `common` for reuse). |

Responses use UTF-8 JSON. Error bodies for the job board follow the v2 envelope (`schema`, `version`, `success`, `message`, `items`).
