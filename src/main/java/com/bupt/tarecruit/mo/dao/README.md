# MO Data Access Objects (`com.bupt.tarecruit.mo.dao`)

File-backed DAO classes for MO module, handling both recruitment flows and user authentication.

## Classes

### `MoRecruitmentDao`
Manages MO recruitment operations:
- **Job board file** (`recruitment-courses.json`) via `RecruitmentCoursesDao`
- **Applicant list & decisions**: reads TA `applications.json` through `MoTaApplicationReadService`, merges `application-status.json`, enforces **`ownerMoId`**（仅账号 `id`）**=== `moId`** for course-scoped APIs
- **Publish**: `ownerMoName` 取自 **`mos.json` 的 `name`**；MO `profiles.json` 不再使用 `realName` 字段
- Methods (non-exhaustive): `getJobBoardForMo(moId)` (filters by `ownerMoId`), `createCourse(input, actingMoId)` (sets `ownerMoId` = `actingMoId`), `getApplicantsForCourse(courseCode, moId)`, `assertMoOwnsCourse()`, `markApplicationReadByMo()`, `getApplicantDetail()`, `addApplicationComment()`, `countUnreadApplicantsForMo()`, `decideApplication(...)` (4-arg resolves `moId` from job; 5-arg for explicit `moId`)

### `MoAccountDao`
Manages MO user authentication and profile operations:
- **Account file** (`mos.json`) - MO main accounts with credentials
- **Profile file** (`profiles.json`) - User personal information
- **Settings file** (`settings.json`) - System preferences
- Methods: `login()`, `register()`, and internal helpers

### `MoApplicationReadStateDao`
- File: `mountDataTAMObupter/mo/mo-application-read-state.json`
- Tracks MO “read” state per `(moId, applicationId)` for unread badges vs `applications.json` `updatedAt`

### `MoApplicationCommentsDao`
- File: `mountDataTAMObupter/mo/mo-application-comments.json`
- Append/list MO comments per `applicationId` (MO-only storage)

### `MoTaApplicationsMutationDao`
- Controlled writes to TA `applications.json` (e.g. set `UNDER_REVIEW` when MO marks an application read)
- Does not modify TA module Java code; used from MO servlets/DAO only

## Thread Safety

`synchronized` is used on DAO methods that mutate shared JSON files (`MoAccountDao`, `MoRecruitmentDao`, and the smaller MO DAOs above) to reduce concurrent write corruption risk.

## Data Location

MO data files are stored under `mountDataTAMObupter/mo/` directory, configured via `DataMountPaths.moDir()`. TA files touched in workflows live under `mountDataTAMObupter/ta/` (`applications.json`, `application-status.json`, …).
