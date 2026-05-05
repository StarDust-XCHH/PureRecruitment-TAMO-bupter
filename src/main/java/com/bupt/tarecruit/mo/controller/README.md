# MO servlets (`com.bupt.tarecruit.mo.controller`)

JSON servlet endpoints for the Module Organizer (MO) workspace.

| Servlet | URL pattern | Role |
| --- | --- | --- |
| `MoJobBoardServlet` | `/api/mo/jobs` | `GET` lists jobs for caller `moId`; `POST` creates a job with owner resolved from `moId`. |
| `MoApplicantsServlet` | `/api/mo/applicants` | `GET` requires `moId`. No `jobId`: merged applicants for all owned jobs. With `jobId` (+ optional `courseCode` cross-check): one opening. `courseCode` alone returns 400. |
| `MoApplicantShortlistServlet` | `/api/mo/applicants/shortlist` | `GET` by `moId`; `POST` with `moId`, `jobId`, `applicationId` (optional `taId`/`name`) adds shortlist entry; `DELETE` with `moId`, `applicationId` removes entry. Rows persist `jobId` + `courseCode`. |
| `MoApplicantDetailServlet` | `/api/mo/applicants/detail` | `GET` single-application detail (`moId`, `applicationId`). |
| `MoApplicantsUnreadServlet` | `/api/mo/applicants/unread-count` | `GET` unread count for MO sidebar badge. |
| `MoApplicationMarkReadServlet` | `/api/mo/applications/mark-read` | `POST` marks read and syncs TA application to `UNDER_REVIEW`. |
| `MoApplicationCommentServlet` | `/api/mo/applications/comment` | `POST` appends MO-only comments. |
| `MoApplicantResumeServlet` | `/api/mo/applications/resume` | `GET` streams resume after ownership checks. |
| `MoApplicationDecisionServlet` | `/api/mo/applications/select` | `POST` decision endpoint (`jobId` required, `moId` required). |
| `MoSkillTagsServlet` | `/api/mo/skill-tags`, `/api/common/skill-tags` | `GET` returns fixed skill-tag dictionary. |
| `MoLoginServlet` | `/api/mo/login` | `POST` MO login. |
| `MoRegisterServlet` | `/api/mo/register` | `POST` MO registration. |
| `MoProfileSettingsServlet` | `/api/mo/profile-settings`, `/mo-assets/*` | `GET`/`POST` profile and password; static avatar assets. |

## Notes

- Applicant ownership-sensitive operations use job-level identity (`jobId`) to avoid ambiguity when one `courseCode` has multiple openings.
- MO shortlist persistence file: `mountDataTAMObupter/mo/mo-applicant-shortlist.json`.
# MO servlets (`com.bupt.tarecruit.mo.controller`)

JSON servlet endpoints for the Module Organizer (MO) workspace.

| Servlet | URL pattern | Role |
| --- | --- | --- |
| `MoJobBoardServlet` | `/api/mo/jobs` | `GET` lists jobs for caller `moId`; `POST` creates a job with owner resolved from `moId`. |
| `MoApplicantsServlet` | `/api/mo/applicants` | `GET` requires `moId`. No `jobId`: merged applicants for all owned jobs. With `jobId` (+ optional `courseCode` cross-check): one opening. `courseCode` alone returns 400. |
| `MoApplicantShortlistServlet` | `/api/mo/applicants/shortlist` | `GET` by `moId`; `POST` with `moId`, `jobId`, `applicationId` (optional `taId`/`name`) adds shortlist entry; `DELETE` with `moId`, `applicationId` removes entry. Rows persist `jobId` + `courseCode`. |
| `MoApplicantDetailServlet` | `/api/mo/applicants/detail` | `GET` single-application detail (`moId`, `applicationId`). |
| `MoApplicantsUnreadServlet` | `/api/mo/applicants/unread-count` | `GET` unread count for MO sidebar badge. |
| `MoApplicationMarkReadServlet` | `/api/mo/applications/mark-read` | `POST` marks read and syncs TA application to `UNDER_REVIEW`. |
| `MoApplicationCommentServlet` | `/api/mo/applications/comment` | `POST` appends MO-only comments. |
| `MoApplicantResumeServlet` | `/api/mo/applications/resume` | `GET` streams resume after ownership checks. |
| `MoApplicationDecisionServlet` | `/api/mo/applications/select` | `POST` decision endpoint (`jobId` required, `moId` required). |
| `MoSkillTagsServlet` | `/api/mo/skill-tags`, `/api/common/skill-tags` | `GET` returns fixed skill-tag dictionary. |
| `MoLoginServlet` | `/api/mo/login` | `POST` MO login. |
| `MoRegisterServlet` | `/api/mo/register` | `POST` MO registration. |
| `MoProfileSettingsServlet` | `/api/mo/profile-settings`, `/mo-assets/*` | `GET`/`POST` profile and password; static avatar assets. |

## Notes

- Applicant ownership-sensitive operations use job-level identity (`jobId`) to avoid ambiguity when one `courseCode` has multiple openings.
- MO shortlist persistence file: `mountDataTAMObupter/mo/mo-applicant-shortlist.json`.
# MO servlets (`com.bupt.tarecruit.mo.controller`)

JSON servlet endpoints for the Module Organizer (MO) workspace.

| Servlet | URL pattern | Role |
| --- | --- | --- |
| `MoJobBoardServlet` | `/api/mo/jobs` | `GET` lists jobs for caller `moId`; `POST` creates a job with owner resolved from `moId`. |
| `MoApplicantsServlet` | `/api/mo/applicants` | `GET` requires `moId`. No `jobId`: merged applicants for all owned jobs. With `jobId` (+ optional `courseCode` cross-check): one opening. `courseCode` alone returns 400. |
| `MoApplicantShortlistServlet` | `/api/mo/applicants/shortlist` | `GET` by `moId`; `POST` with `moId`, `jobId`, `applicationId` (optional `taId`/`name`) adds shortlist entry; `DELETE` with `moId`, `applicationId` removes entry. Rows persist `jobId` + `courseCode`. |
| `MoApplicantDetailServlet` | `/api/mo/applicants/detail` | `GET` single-application detail (`moId`, `applicationId`). |
| `MoApplicantsUnreadServlet` | `/api/mo/applicants/unread-count` | `GET` unread count for MO sidebar badge. |
| `MoApplicationMarkReadServlet` | `/api/mo/applications/mark-read` | `POST` marks read and syncs TA application to `UNDER_REVIEW`. |
| `MoApplicationCommentServlet` | `/api/mo/applications/comment` | `POST` appends MO-only comments. |
| `MoApplicantResumeServlet` | `/api/mo/applications/resume` | `GET` streams resume after ownership checks. |
| `MoApplicationDecisionServlet` | `/api/mo/applications/select` | `POST` decision endpoint (`jobId` required, `moId` required). |
| `MoSkillTagsServlet` | `/api/mo/skill-tags`, `/api/common/skill-tags` | `GET` returns fixed skill-tag dictionary. |
| `MoLoginServlet` | `/api/mo/login` | `POST` MO login. |
| `MoRegisterServlet` | `/api/mo/register` | `POST` MO registration. |
| `MoProfileSettingsServlet` | `/api/mo/profile-settings`, `/mo-assets/*` | `GET`/`POST` profile and password; static avatar assets. |

## Notes

- Applicant ownership-sensitive operations are jobId-based to avoid ambiguity when one `courseCode` has multiple openings.
- MO shortlist file: `mountDataTAMObupter/mo/mo-applicant-shortlist.json`.
# MO servlets (`com.bupt.tarecruit.mo.controller`)

Jakarta servlets exposing JSON APIs for the Module Organizer (MO) workspace.

| Servlet | URL pattern | Role |
| --- | --- | --- |
| `MoJobBoardServlet` | `/api/mo/jobs` | `GET` lists jobs for caller `moId`; `POST` creates a job with owner resolved from `moId`. |
| `MoApplicantsServlet` | `/api/mo/applicants` | `GET` requires `moId`. No `jobId`: merged applicants for all owned jobs. With `jobId` (+ optional `courseCode` cross-check): one opening. `courseCode` alone returns 400. |
| `MoApplicantShortlistServlet` | `/api/mo/applicants/shortlist` | `GET` by `moId`; `POST` with `moId`, `jobId`, `applicationId` (optional `taId`/`name`) adds shortlist entry; `DELETE` with `moId`, `applicationId` removes entry. Rows persist `jobId` + `courseCode`. |
| `MoApplicantDetailServlet` | `/api/mo/applicants/detail` | `GET` single-application detail (`moId`, `applicationId`). |
| `MoApplicantsUnreadServlet` | `/api/mo/applicants/unread-count` | `GET` unread count for MO sidebar badge. |
| `MoApplicationMarkReadServlet` | `/api/mo/applications/mark-read` | `POST` marks read and syncs TA application to `UNDER_REVIEW`. |
| `MoApplicationCommentServlet` | `/api/mo/applications/comment` | `POST` appends MO-only comments. |
| `MoApplicantResumeServlet` | `/api/mo/applications/resume` | `GET` streams resume after ownership checks. |
| `MoApplicationDecisionServlet` | `/api/mo/applications/select` | `POST` decision endpoint (`jobId` required, `moId` required). |
| `MoSkillTagsServlet` | `/api/mo/skill-tags`, `/api/common/skill-tags` | `GET` returns fixed skill-tag dictionary. |
| `MoLoginServlet` | `/api/mo/login` | `POST` MO login. |
| `MoRegisterServlet` | `/api/mo/register` | `POST` MO registration. |
| `MoProfileSettingsServlet` | `/api/mo/profile-settings`, `/mo-assets/*` | `GET`/`POST` profile and password; static avatar assets. |

## Notes

- Applicant ownership-sensitive operations use job-level identity (`jobId`) to avoid ambiguity when one `courseCode` has multiple openings.
- MO shortlist persistence file: `mountDataTAMObupter/mo/mo-applicant-shortlist.json`.
