# `MoRecruitmentDao`

File-backed DAO for MO recruitment flows.

**Job board file (`recruitment-courses.json`)** is handled by **`com.bupt.tarecruit.common.dao.RecruitmentCoursesDao`**: `getPendingCourses` delegates to **`readJobBoard()`** (same standardized read as TA); `createCourse` delegates append/persist after building the new row. MO does not add other course-DB features beyond publish here.

This class still reads/writes TA-side JSON directly for:

- **`DataMountPaths.taApplicationStatus()`** — applicant lists and MO selection decisions.
- **`DataMountPaths.taProfiles()`** / **`taAccounts()`** — joined for applicant display.

`synchronized` remains on methods that touch TA files. Listing jobs does not rewrite `recruitment-courses.json`; publish updates that file via `RecruitmentCoursesDao`.
