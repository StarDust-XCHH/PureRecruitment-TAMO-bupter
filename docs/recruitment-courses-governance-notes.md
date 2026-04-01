# Recruitment Courses Governance Notes / 招聘课程（recruitment-courses）治理说明

## Scope / 范围

**EN:** This note clarifies which fields are currently written by MO publish, and which fields are placeholder-only for future TA/Admin workflows.

**中文：** 本文说明 MO 发布时实际写入哪些字段，以及哪些字段仅为占位、留待后续 TA/Admin 流程更新。

**Current implementation status / 当前实现状态：**

- **EN:** Implemented now: MO publish writes MO-owned fields and initializes placeholders.
- **中文：** 已实现：MO 发布写入 MO 侧业务字段，并初始化占位字段。
- **EN:** Not implemented now: TA/Admin-side business logic that updates governance/process placeholders.
- **中文：** 未实现：由 TA/Admin 侧业务逻辑更新 governance / process 类占位字段。

## Read vs write (MO list API) / 读取与写回（MO 列表接口）

**EN:** `GET /api/mo/jobs` applies v2 normalization when building the JSON **response** only. It does **not** persist that normalization to `recruitment-courses.json`, so unrelated courses are not rewritten when the list is refreshed.

**中文：** `GET /api/mo/jobs` 仅在组装**响应**时对记录做 v2 归一化，**不会**把归一化结果写回 `recruitment-courses.json`，因此刷新岗位列表不会改写磁盘上其它课程的数据。

## Field Ownership / 字段归属

**EN:** MO publish writes:

**中文：** MO 发布写入：

- `jobId`, `courseCode`, `courseName`
- `moName`, `ownerMoId`, `ownerMoName`
- `semester`, `recruitmentStatus`, compatibility `status`
- `courseDescription`, `recruitmentBrief`, `requiredSkills`
- `teachingWeeks`, `assessmentEvents`
- `studentCount`, `taRecruitCount`, `campus`, `applicationDeadline`
- timestamps and source fields

**EN:** Placeholder fields initialized by MO publish (default only):

**中文：** 由 MO 发布初始化、仅作默认值的占位字段：

- **Governance placeholders / Governance 占位：**
  - `publishStatus = "PENDING_REVIEW"`
  - `visibility = "INTERNAL"`
  - `isArchived = false`
  - `auditStatus = "PENDING"`
  - `auditComment = ""`
  - `priority = "NORMAL"`
  - `dataVersion = 1`
  - `lastSyncedAt = ""`
- **Process placeholders / Process 占位：**
  - `recruitedCount = 0`
  - `applicationsTotal = 0`
  - `applicationsPending = 0`
  - `applicationsAccepted = 0`
  - `applicationsRejected = 0`
  - `lastApplicationAt = ""`
  - `lastSelectionAt = ""`

## Future Workflow Responsibilities (Not Implemented Yet) / 后续流程职责（尚未实现）

**EN:** TA/Admin workflow should update:

**中文：** TA/Admin 流程应负责更新：

- **EN:** application counters and timestamps when applications arrive or state changes.
- **中文：** 申请到达或状态变化时的 application 计数与时间戳。
- **EN:** `recruitedCount` when selections are confirmed.
- **中文：** 确认录用结果时的 `recruitedCount`。
- **EN:** governance states (`publishStatus`, `visibility`, `auditStatus`, `isArchived`) through Admin policy.
- **中文：** 依 Admin policy 维护的 governance 状态（`publishStatus`、`visibility`、`auditStatus`、`isArchived`）。

**EN:** MO publish should not overwrite downstream-owned values once lifecycle management is enabled.

**中文：** 在启用 lifecycle 管理后，MO 发布不应再覆盖由下游持有的字段值。

## Compatibility Notes / 兼容性说明

**EN:** `status` is kept as a compatibility mirror of `recruitmentStatus` for existing readers.

**中文：** `status` 作为 `recruitmentStatus` 的 compatibility mirror 保留，供既有读取方使用。

**EN:** Any client that supports new schema should prefer:

**中文：** 支持新 schema 的客户端应优先使用：

- `recruitmentStatus` for hiring progression.
- **中文：** `recruitmentStatus` — hiring progression。
- `publishStatus` for platform visibility/governance lifecycle.
- **中文：** `publishStatus` — platform visibility / governance lifecycle。

