
# MO Data Access Objects (`com.bupt.tarecruit.mo.dao`)

File-backed DAO classes for MO module, handling both recruitment flows and user authentication.

## Classes

### `MoRecruitmentDao`
Manages MO recruitment operations:
- **Job board file** (`recruitment-courses.json`) via `RecruitmentCoursesDao`
- **TA applicant data** from `taApplicationStatus()`, `taProfiles()`, `taAccounts()`
- Methods: `getPendingCourses()`, `createCourse()`, `getApplicantsForCourse()`, `decideApplication()`

### `MoAccountDao`
Manages MO user authentication and profile operations:
- **Account file** (`mos.json`) - MO main accounts with credentials
- **Profile file** (`profiles.json`) - User personal information
- **Settings file** (`settings.json`) - System preferences
- Methods: `login()`, `register()`, and internal helpers

## Thread Safety

All write methods in `MoAccountDao` use `synchronized` keyword to prevent concurrent modification issues.

## Data Location

MO data files are stored under `mountDataTAMObupter/mo/` directory, configured via `DataMountPaths.moDir()`.