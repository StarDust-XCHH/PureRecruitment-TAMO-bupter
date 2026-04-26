# recruitment-courses 数据库 · DAO 分层说明

数据文件：`recruitment-courses.json`（与本文件同目录）。

## 已实现

- **共用 DAO**：`com.bupt.tarecruit.common.dao.RecruitmentCoursesDao`  
  负责该文件的读/写、v2 行归一化、**MO/TA 共用的 `readJobBoard()` 标准化列表载荷**、MO 发布时追加一条岗位并同步顶层信封字段。
- **MO 私有 DAO**（`MoRecruitmentDao`）：课程相关仅**委托**共用 DAO 完成「列表（归一化响应）」与「发布（追加落盘）」；选人/申请人仍读写 `ta/*.json`，逻辑未改。
- **TA 私有 DAO**（`TaAccountDao`）：岗位大厅读接口委托 `RecruitmentCoursesDao.readJobBoard()`（与 MO 列表同一标准化读），不再在本类内直接打开该 JSON。

## 暂未实现（后续可加）

以下仅作规划，**当前代码中无对应私有 DAO 或专用方法**：

- **Admin**：对 `publishStatus`、`visibility`、`auditStatus`、`isArchived`、`priority`、`dataVersion`、`lastSyncedAt` 等治理字段的审核与变更；归档、同步戳等。
- **TA**：根据申请/投递事件更新岗位上的 `applications*`、`recruitedCount`、`lastApplicationAt` 等流程统计（应与 `ta/application-status.json` 等编排，避免双写不一致）。

实现时请继续通过 `RecruitmentCoursesDao` 扩展「受控写」原语，并在 Admin/TA 包内写清权限与字段子集，避免 MO 发布覆盖下游已维护字段。
