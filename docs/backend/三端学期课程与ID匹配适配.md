# 三端对不同学期 / 多岗位实例的适配说明（ID 匹配改造后）

本文说明在后端将「MO 流程状态 ↔ TA 投递」的**匹配**从课程编号 / 名称 / slug 改为以 **`applicationId`、`jobId`** 为主之后，**MO 端、TA 端、管理端**对「同一 `courseCode` 多学期或多岗位实例」的覆盖程度与剩余问题。前端未改动。

---

## 1. 本次后端匹配相关改动（摘要）

| 区域 | 行为 |
|------|------|
| `RecruitmentCoursesDao.taFacingCourseSnapshotFromNormalizedJob` | 在 TA 可见的 `courseSnapshot` 中写入 `jobId`，与岗位板一致。 |
| `MoTaApplicationReadService` | 新增 `getApplicationsForCourseScopedToJob(courseCode, jobId)`：在课程编号一致的前提下，用快照中的 `jobId` 收窄到当前岗位实例（快照无 `jobId` 时仍保留，兼容旧数据）。 |
| `MoTaApplicationStatusMatcher` | 统一规则：`applicationId` 一致，或 `jobId` 一致；或（仅历史）MO 行无 `applicationId`/`jobId` 且 **TA 快照中也无 `jobId`** 时，才用同 `courseCode` 回退，避免新数据误挂旧 MO 行。 |
| `MoRecruitmentDao` | 申请人列表、统计、详情、录用决策时，合并 `application-status.json` 与 TA 申请均走上述 ID 规则；决策写入的 MO 状态行会带上 `jobId`。 |
| `TaAccountDao` | TA 申请列表合并 MO 状态时，不再用 `jobSlug`/`courseName` 与 MO 行对齐。 |
| `MoTaApplicationsMutationDao` | `findApplicationByTaAndCourse` 支持按 `jobId` 选对申请；`refreshCourseSnapshotsForCourseCode` 在能解析出岗位 `jobId` 时，避免误更新「同课号、不同岗位」的快照。 |

---

## 2. MO 端（岗位负责人）

### 实现度

- **与 TA 流程状态的关联**：已按 `applicationId`、`jobId` 为主对齐，避免仅靠课名或 slug 误连到另一条 MO 记录。
- **申请人列表与统计**：在已知当前页面所指的**已发布岗位**（由 `courseCode` 解析出的那条岗位 JSON，含 `jobId`）时，会只统计、展示 `courseSnapshot.jobId` 与该岗位一致（或快照尚无 `jobId`）的投递，减少同课号多岗时的串单。
- **录用/拒绝**：在解析出的岗位带 `jobId` 时，会优先选中该岗位对应的 TA 申请记录，并在 `application-status` 中写入 `jobId`。

### 仍存在的问题与边界

- **入口仍多为 `courseCode`**：通过 `findNormalizedJobByCourseCode` 解析岗位时，若岗位板存在**多条相同 `courseCode` 的发布项**，实现上仍只命中**第一条**，MO 接口无法在无前端、无额外参数的情况下区分「用户想操作的是哪一条」。这是既有数据模型与 API 形态的限制，而非本次 ID 匹配能单独消除的。
- **旧数据**：TA 申请若从未写入 `courseSnapshot.jobId`，在「同课号多岗」场景下，仍可能被当前 MO 列表的宽泛策略一并纳入（刻意保留的兼容行为）；随数据刷新或重新投递，会逐渐带上 `jobId` 而变精确。

---

## 3. TA 端（助教）

### 实现度

- **新投递**：`courseSnapshot` 会包含 `jobId`（来自当时解析的岗位），便于后续学期或并开岗位区分实例。
- **申请列表展示状态**：与 MO 侧 `application-status` 合并时，与 MO 端使用同一套 ID 优先规则，不再依赖课名或 slug 的弱匹配。

### 仍存在的问题与边界

- **投递唯一性仍以 `uniqueKey`（TA + 课号）等既有逻辑为准**：若业务上允许「同一课号、不同 `jobId`」多次投递，需要后续在**非本次「匹配」范围**内调整去重与目录策略；当前未改 TA 投递与去重逻辑。
- **历史申请**：无 `jobId` 的快照在 MO 侧列表中仍可能被同一 `courseCode` 视图包含，直到快照被 MO 刷新或相关维护逻辑更新。

---

## 4. 管理端（Admin）

### 实现度

- 本次**未**改动 Admin 的 Servlet / 业务路径；管理端若仅做课程元数据浏览或导出，行为与改造前一致。
- 岗位数据本身已长期包含 `jobId`（岗位板规范化逻辑），与本次在快照、MO 状态中强调 `jobId` 一致。

### 仍存在的问题与边界

- 若管理工具或脚本仍**仅以 `courseCode`** 做批量关联（未带 `jobId`），在「同课号多岗」下语义仍可能模糊；建议在运维脚本中改为以 **`jobId`** 为主键操作单条岗位。

---

## 5. 小结

| 端 | 多学期 / 多岗位实例适配 | 主要剩余风险 |
|----|-------------------------|--------------|
| MO | ID 合并与列表收窄已加强；决策与状态行可带 `jobId` | 请求仍常只带 `courseCode` 时，`findNormalizedJobByCourseCode` 对重复课号只能解析一条 |
| TA | 新快照含 `jobId`；状态合并走 ID | 旧快照无 `jobId`；课号级去重未扩展为多岗 |
| Admin | 无行为变更 | 管理脚本若只用课号，多岗场景需人工区分或改用 `jobId` |

建议在后续迭代中（若允许动接口或前端）：为 MO 与决策类接口增加 **`jobId`（或 `applicationId`）显式参数**，并与岗位板唯一主键对齐，以从入口上消除「同课号多岗」的歧义。

---

## 6. 最小化风险补丁（兼容旧数据）

### 动机

在「同 `courseCode`、多 `jobId`」已存在的前提下，若 TA 申请的 `courseSnapshot` 已带有 **`jobId`**，而 `application-status` 中某条历史行仅有 **`taId` + `courseCode`**（尚未写入 `jobId` / `applicationId`），原先仅用课号回退仍可能把**新岗位**上的申请与**旧岗位**上的 MO 结论对齐，存在串单风险。

### 行为调整（`MoTaApplicationStatusMatcher`）

- **不变**：`applicationId` 双向一致，或 `jobId` 双向一致，仍优先命中。
- **收紧**：仅当 MO 行上 `applicationId`、`jobId` 皆空，且 **TA 传入上下文中的 `jobId` 也为空**（典型为旧快照、尚未刷新）时，才允许用 **`courseCode` 一致** 与历史 MO 行对齐。
- **旧数据**：无 `jobId` 的快照与无 ID 的 MO 历史行之间的课号级对齐**仍然可用**；快照在 MO 刷新或重新投递写入 `jobId` 后，将走 ID 路径，不再误挂无 ID 的旧行。

### 残余说明

- 若多条 MO 历史行均为「仅课号、无 ID」，且 TA 快照也无 `jobId`，仍可能命中多条中的**最新 `updatedAt`** 一条；该情形仅存在于早期数据，风险面已缩小。
- `getApplicationsForCourseScopedToJob` 对「快照无 `jobId`」的保留策略未改，以便 MO 列表仍能展示旧投递；与上条收紧配合后，**状态合并**侧对已带 `jobId` 的投递更保守、更安全。
