# `MoRecruitmentDao`

File-backed DAO for MO recruitment flows. It reads and writes:

- **`DataMountPaths.moRecruitmentCourses()`** — shared `recruitment-courses.json` (v2 `mo-ta-job-board` schema): list/normalize jobs, create published jobs with normalized fields and placeholders, and keep legacy rows migrated on read.
- **`DataMountPaths.taApplicationStatus()`** — TA application records for applicant lists and selection decisions.
- **`DataMountPaths.taProfiles()`** / **`taAccounts()`** — joined when resolving applicant display data.

Main operations include loading the job list (`getPendingCourses`), creating a course/job (`createCourse`), fetching applicants by `courseCode`, and persisting MO decisions (`decideApplication`). Uses Gson with pretty printing; synchronized methods for safe concurrent access to the same files.

`getPendingCourses` returns a v2-normalized JSON payload in memory but **does not rewrite** `recruitment-courses.json`, so listing jobs does not mutate other courses’ stored fields. The file is updated on `createCourse` and other write paths. On publish, `syncJobBoardFileEnvelope` updates top-level `count` / `generatedAt` (and `schema` / `version`) to match `items`.
