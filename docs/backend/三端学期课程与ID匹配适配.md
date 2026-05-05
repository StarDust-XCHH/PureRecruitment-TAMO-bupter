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
| `TaAccountDao` | TA 申请列表合并 MO 状态时，不再用 `jobSlug`/`courseName` 与 MO 行对齐；**投递**使用含 `jobId` 的 `uniqueKey` 与分层判重，允许不同学期（不同 `jobId`）同课号多次投递（见 §7）。 |
| `TaApplicationUniqueKeys` | 规范键 `TAID::COURSECODE::JOBID`；历史键 `TAID::COURSECODE` 用于与旧数据对齐判重。 |
| `MoTaApplicationsMutationDao` | `findApplicationByTaAndCourse` 支持按 `jobId` 选对申请，且 `uniqueKey` 匹配与 §7 一致；`refreshCourseSnapshotsForCourseCode` 在能解析出岗位 `jobId` 时，避免误更新「同课号、不同岗位」的快照。 |

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
- **同课号、多学期 / 多岗位实例投递**：`uniqueKey` 已扩展为「TA + 课号 + `jobId`」（见 §7），在能拿到不同 `jobId` 的前提下允许分别保留有效申请；与 MO 决策、快照刷新等逻辑一致。
- **申请列表展示状态**：与 MO 侧 `application-status` 合并时，与 MO 端使用同一套 ID 优先规则，不再依赖课名或 slug 的弱匹配。

### 仍存在的问题与边界

- **投递入口仍按 `courseCode` 解析岗位**：与 §2 相同，`findNormalizedJobByCourseCode` 在岗位板存在多条同课号时只命中一条；若需**并发**两条同课号岗位分别投递，请求侧需将来能指明 `jobId`（或等价入口），否则业务上仍只能投中解析到的那一条。
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
| TA | 新快照含 `jobId`；状态合并走 ID；**投递**按 `jobId` 区分同课号多实例（§7） | 入参仅课号时仍只解析一条岗位；旧快照无 `jobId` |
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

---

## 7. 业务侧：允许 TA 投递「不同学期、同课号」及风险控制

### 目标

在保留旧数据行为的前提下，允许同一 TA 对**相同 `courseCode`、不同岗位实例（不同 `jobId`，通常对应不同学期或并开岗位）**分别发起有效投递，并避免与既有 MO/统计/决策逻辑冲突。

### 实现要点

1. **`uniqueKey`（`TaApplicationUniqueKeys`）**  
   - **新写入**：`TAID::COURSECODE::JOBID`（`jobId` 取自投递时解析到的规范化岗位 JSON）。  
   - **历史**：仍为 `TAID::COURSECODE` 的记录保留不动；新投递与之通过下面规则区分是否「同一岗位实例」。

2. **重复投递（409）规则（`TaAccountDao#createApplication`）**  
   - 若已存在**活跃**申请且其 `uniqueKey` 与本次规范键**完全相同** → 视为重复。  
   - 若已存在活跃申请且 `uniqueKey` 为**历史** `TAID::COURSECODE`：  
     - 仅当旧快照**无** `jobId` 且本次岗位也**无** `jobId`（极少见）→ 判重（与旧行为一致）。  
     - 若旧快照有 `jobId` 且与本次岗位 `jobId` **相同** → 判重。  
     - 若旧快照无 `jobId` 但本次岗位有 `jobId`（新学期、新实例）→ **不**判重，允许新投。  
     - 若旧快照有 `jobId` 与本次不同 → **不**判重。  

3. **`applicationId`**  
   - 当岗位带 `jobId` 时，在原有 `APP-{taId}-{courseCode}-{时间}` 形态中插入经净化的 **`jobId` 片段**，降低秒级时间戳碰撞概率，并便于日志区分实例。

4. **简历目录**  
   - 仍使用 `taResumeCourseDir(taId, courseCode)`；同一课号多申请依赖**不同的 `applicationId` 文件名**区分文件，**未**改为按 `jobId` 分目录（最小改动；若日后文件量或合规要求提高，可再拆目录）。

5. **MO 侧按 TA+课号查找申请（`MoTaApplicationsMutationDao#findApplicationByTaAndCourse`）**  
   - `uniqueKey` 匹配逻辑与规范键 / 历史键一致，便于在传入 `jobId` 时选中正确活跃申请。

### 风险与缓解

| 风险 | 说明 | 缓解 |
|------|------|------|
| 同课号多岗**并发**、请求只带课号 | 投递时仍通过 `findNormalizedJobByCourseCode` 解析**单条**岗位，无法在无额外参数时选中「第二条同课号岗」 | 文档化；后续在 TA 入参或前端增加 `jobId`（或专用接口） |
| 旧 `uniqueKey` 无 `jobId` 与新学期同学号 | 已通过「历史键 + 快照 `jobId`」组合判重区分 | §7 与 §2；必要时运维对旧数据补 `courseSnapshot.jobId` 或依赖 MO 刷新快照 |
| 简历同目录 | 多申请共享课号目录 | 依赖 `applicationId` 唯一文件名；见上 |

### 与 §6 的关系

§6 解决的是 **MO 状态行 ↔ TA 申请** 合并时「新快照有 `jobId` 勿误挂无 ID 旧 MO 行」。§7 解决的是 **TA 能否对多实例分别投递** 及 **与 MO 查找一致**。二者互补，旧数据在无 `jobId` 时仍走兼容分支。
