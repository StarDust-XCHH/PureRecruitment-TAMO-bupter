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
  - Returns job board payload in v2 shape:
    - `schema: mo-ta-job-board`
    - `version: 2.0`
    - `generatedAt`
    - `count`
    - `items`
  - Reads from:
    - `mountDataTAMObupter/common/recruitment-courses.json`
  - Normalizes each item **in memory** for the response only; **does not** write `recruitment-courses.json`. Listing jobs therefore does not rewrite or “clean up” other courses on disk.

- `POST /api/mo/jobs`
  - Creates one MO job using semester-scale fields (see `docs/mo-job-board-api-v2.md`).
  - Writes to:
    - `mountDataTAMObupter/common/recruitment-courses.json`
  - Adds/normalizes v2 fields.
  - Requires: `courseCode`, `courseName`, `recruitmentStatus` (or compatibility `status`), `requiredSkills`, `courseDescription`.
  - Initializes placeholder/governance fields if missing (no TA/Admin workflow logic is executed here).

- `GET /api/mo/skill-tags`
  - Returns fixed skill tag dictionary for MO form rendering.

- `GET /api/common/skill-tags`
  - Alias endpoint of MO skill tags for contract-level reuse.

- `GET /api/mo/applicants?courseCode=...`
  - Reads TA account/profile/application status data.
  - Data sources:
    - `mountDataTAMObupter/ta/tas.json`
    - `mountDataTAMObupter/ta/profiles.json`
    - `mountDataTAMObupter/ta/application-status.json`

- `POST /api/mo/applications/select`
  - Writes MO selection decision back to TA application status data.
  - Writes to:
    - `mountDataTAMObupter/ta/application-status.json`
  - Fields updated/created include:
    - `status`
    - `statusTone`
    - `summary`
    - `moComment`
    - `nextAction`
    - `nextStep`
    - `updatedAt`
    - `jobSlug`

## TA UI Impact

- No TA page/JSP/CSS/JS file was modified.
- TA interface behavior remains unchanged.

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
