# MO-TA Interaction Log

## Purpose

This log tracks MO-side changes that interact with TA-side interfaces or TA data files.

## Updated Scope (v2)

- Semester-scale TA recruitment is supported on MO job publishing.
- MO job payload now includes long-horizon planning fields:
  - `teachingWeeks`
  - `assessmentEvents`
  - `requiredSkills`
  - `recruitmentBrief` (long-form text)
  - `ownerMoId`
  - `ownerMoName`
  - `semester`
  - `recruitmentStatus` (and compatibility mirror `status`)
  - `applicationDeadline`
  - `taRecruitCount`
  - `recruitedCount` (initialized as `0` by MO publish; later updates are downstream workflow responsibility)
  - `campus` (`Main` / `Shahe`)
  - governance/process placeholder fields initialized by MO publish:
    - `publishStatus`, `visibility`, `isArchived`
    - `auditStatus`, `auditComment`, `priority`, `dataVersion`, `lastSyncedAt`
    - `applicationsTotal`, `applicationsPending`, `applicationsAccepted`, `applicationsRejected`, `lastApplicationAt`, `lastSelectionAt`

## New/Updated MO Interfaces (v2)

- `GET /api/mo/jobs`
  - Query **`moId`**（必填）: 仅返回 `items[].ownerMoId` 与该账号 **`id`** 相同的岗位（忽略大小写）；`ownerMoId` 应为 MO 主键 id，非用户名。
  - Returns job board payload in v2 shape:
    - `schema: mo-ta-job-board`
    - `version: 2.0`
    - `generatedAt`
    - `count`
    - `items`
  - Reads from:
    - `mountDataTAMObupter/common/recruitment-courses.json`
  - Normalizes each **filtered** item **in memory** for the response only; **does not** write `recruitment-courses.json`.

- `POST /api/mo/jobs`
  - Creates one MO job using semester-scale fields (see `docs/mo-job-board-api-v2.md`).
  - **身份**：查询参数或 JSON 根字段 **`moId`**（必填其一，查询优先）；持久化的 **`ownerMoId`** 取该值（账号 id）。**`ownerMoName`** 由服务端从 **`mos.json` 的 `name`** 写入；`username` / `realName` 仅存在于个人资料数据，不写入岗位。
  - Writes to:
    - `mountDataTAMObupter/common/recruitment-courses.json`
  - Adds/normalizes v2 fields.
  - Requires: `courseCode`, `courseName`, `recruitmentStatus` (or compatibility `status`), `requiredSkills`, `courseDescription`.
  - Initializes placeholder/governance fields if missing (no TA/Admin workflow logic is executed here).

- `GET /api/mo/skill-tags`
  - Returns fixed skill tag dictionary for MO form rendering.

- `GET /api/common/skill-tags`
  - Alias endpoint of MO skill tags for contract-level reuse.

- `GET /api/mo/applicants?courseCode=...&moId=...` (**required** `moId`)
  - Lists **real submissions** for the course from `mountDataTAMObupter/ta/applications.json` (via `MoTaApplicationReadService`), merged with MO decision rows in `application-status.json`.
  - **Authorization**: `moId` must match the job’s `ownerMoId` in `recruitment-courses.json` for that `courseCode` (403 otherwise).
  - Response includes `items` (per applicant: `applicationId`, `taId`, contact fields, `status`, `comment` preview, `unread`, `resumeFileName`, …), `count`, and `unreadCount`.
  - Data sources (read): `applications.json`, `application-status.json`, MO read-state file (below); TA account/profile snapshots are embedded in application records where present.

- `GET /api/mo/applicants/detail?moId=...&applicationId=...`
  - Full detail for one application: `taSnapshot`, `courseSnapshot`, `resume` meta, `events` (from `application-events.json`), `comments` (MO-only file), `moDecision` overlay from `application-status.json` if any.
  - Same `ownerMoId` / `moId` check as list API.

- `GET /api/mo/applicants/unread-count?moId=...`
  - Count of unread applications across all jobs owned by that MO (used for sidebar badge). Unread = no read record or `applications.json` `updatedAt` newer than `lastReadAt` for `(moId, applicationId)`.

- `POST /api/mo/applications/mark-read`
  - JSON body: `moId`, `applicationId`.
  - Writes `lastReadAt` to `mountDataTAMObupter/mo/mo-application-read-state.json`.
  - Updates the matching row in `mountDataTAMObupter/ta/applications.json` to **`UNDER_REVIEW`** / **审核中** (MO `MoTaApplicationsMutationDao`), so TA status APIs reflect “under review” after MO opens the detail view.

- `POST /api/mo/applications/comment`
  - JSON body: `moId`, `applicationId`, `text`.
  - Appends to `mountDataTAMObupter/mo/mo-application-comments.json` (MO-only thread; does not change TA servlet code).

- `GET /api/mo/applications/resume?moId=...&applicationId=...`
  - Streams resume file after the same ownership/detail checks; path resolved from `applications.json` → `resume.relativePath` under `ta/resume/`.

- `POST /api/mo/applications/select`
  - JSON body: `courseCode`, `taId`, **`moId`** (required), `decision` (`selected` | `rejected`), optional `comment`.
  - Writes MO selection to `mountDataTAMObupter/ta/application-status.json`, aligned with real `applicationId` from `applications.json` when present.
  - Typical fields on the status row: `status` (e.g. 已录用 / 未录用), `statusTone`, `summary`, `moComment`, `nextAction`, `nextStep`, `updatedAt`, `jobSlug`, `courseCode`, `courseName`, `taId`, `ownerMoId`, optional `comments` array preserved across updates.

- **MO-only data files** (under `mountDataTAMObupter/mo/`):
  - `mo-application-read-state.json` — schema `mo` / entity `mo-application-read-state`; items: `moId`, `applicationId`, `lastReadAt`.
  - `mo-application-comments.json` — schema `mo` / entity `mo-application-comments`; items group `comments[]` by `applicationId`.

- **Compatibility**: `MoRecruitmentDao.decideApplication(courseCode, taId, decision, comment)` (4-arg) still resolves `moId` from the job’s `ownerMoId` for local tools; HTTP clients should pass `moId` explicitly on the 5-arg path.

## TA UI Impact

- No TA page/JSP/CSS/JS file was modified for this workflow.
- TA-visible data may change when MO marks read (`applications.json` → `UNDER_REVIEW`) or writes `application-status.json` (录用/拒绝); that is intentional shared JSON behavior, not a TA module code change.

## Admin UI Impact

- No Admin page/JSP/CSS/JS file was modified.
- Admin interface behavior remains unchanged.

## Data Contract Notes

- Canonical MO job-board contract is documented in:
  - `docs/mo-job-board-api-v2.md`
- Current MO backend behavior:
  - accepts v2 fields in `POST /api/mo/jobs`
  - returns normalized v2 payload in `GET /api/mo/jobs` (normalization is **not** persisted by that GET)
  - initializes `recruitedCount=0` and other placeholder fields on publish
  - keeps compatibility mirror field `status = recruitmentStatus` for existing readers
  - uses shared path resolver in `common` package for MO/TA JSON mount paths
  - job-board file access is implemented in `com.bupt.tarecruit.common.dao.RecruitmentCoursesDao` (`readJobBoard` for MO + TA lists, `appendPublishedJob` for publish, lookups for MO selection)

- Current TA backend behavior for job board:
  - `GET /api/ta/jobs` reads from the same `mountDataTAMObupter/common/recruitment-courses.json` file via **`RecruitmentCoursesDao.readJobBoard()`** (same v2 normalization as MO `GET /api/mo/jobs`); TA-side lifecycle field updates on job rows are not implemented here

## Notes

- MO pages are added under `pages/mo`.
- MO JS modules are added under `assets/mo/js`.
- MO backend logic is added under `com.bupt.tarecruit.mo`.
