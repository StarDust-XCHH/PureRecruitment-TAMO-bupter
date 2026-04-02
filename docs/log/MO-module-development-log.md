# MO Module Development Log

> This log is designed for continuous extension by all contributors.

## Entry 01 - Existing MO UI Progress

**At 2026-04-01 02:00 (UTC+8), by Fenghao.**

### 已完成开发内容（MO界面）

基于现有 MO 原型页面与文档，已完成以下界面开发内容：

1. **MO工作台首页（Dashboard）**
  - 已实现招聘总览信息卡片展示。
  - 可快速查看关键指标：开放岗位数、候选人数、已录用人数、待处理申请数。
  - 页面结构采用统一侧边栏 + 顶栏 + 卡片化布局，便于快速浏览。
2. **岗位发布与岗位列表页面（Job Publishing / Job Board）**
  - 已实现岗位发布表单，支持课程名称、时间地点、标签、任务清单、描述等字段录入。
  - 已实现岗位列表展示，并包含搜索、分页、刷新等基础管理能力。
  - 页面支持“发布岗位 + 浏览岗位”的连续操作流程。
3. **课程详情弹窗（Course Detail Modal）**
  - 已实现课程详情弹窗展示，包含课程上下文、标签与任务清单等信息。
  - 已打通“从岗位详情跳转到候选人筛选”的操作入口，减少页面来回切换。
4. **应聘筛选与决策页面（Applicant Screening）**
  - 已实现按课程查看候选人列表。
  - 已实现候选人状态展示与录用/拒绝操作按钮。
  - 已支持决策结果写回申请状态数据，保证与TA侧状态一致性。
5. **端到端流程联通**
  - 已形成从“查看总览 -> 发布岗位 -> 查看岗位详情 -> 筛选候选人 -> 给出录用结果”的闭环流程。
  - 当前原型已满足 MO 角色核心业务演示需求。

### 需要解决的问题

1. **岗位管理能力仍需补全**
  - 目前以发布与查看为主，后续需补充岗位编辑、关闭岗位等功能。
2. **候选人信息深度不足**
  - 当前筛选页以基础信息为主，后续需支持更完整的候选人资料查看（如可用时间、简历预览等）。
3. **决策交互还可加强**
  - 录用/拒绝流程建议增加确认弹窗与操作反馈，降低误操作风险。
4. **仪表盘分析能力有待提升**
  - 目前以静态总览指标为主，后续可加入趋势信息与逾期提醒，增强管理价值。
5. **视觉与可用性细节需持续优化**
  - 深色模式下部分文字/图标对比度、卡片内容排布等细节仍需进一步打磨。

### 新增问题（待优先解决）

1. **后端逻辑未完成**
  - 包括 MO 注册与登录、岗位发布、信息总览、对候选人的操作（录用/拒绝）等核心后端流程仍需实现。
2. **主界面过长**
  - 可考虑将“发布岗位”和“筛选候选人”拆分为二级页面或弹窗，以优化信息密度和阅读体验。
3. **动画未完全实现**
  - 目前存在导览跳转问题、当前步骤框未高亮等交互问题。
4. **首次登录指引缺失**
  - 首次登录的新手引导尚未实现（可参照 MO 设计方向补全）。
5. **整体动画与视觉效果仍可加强**
  - 建议进一步优化过渡动画与视觉反馈（可参照 MO 风格统一方案）。

#### 备注

- 部分后端逻辑需依赖 TA 端操作，TA 端又需要 MO 的数据，遇到“死锁”状态。为妥善解决，拟开发流程如下：`2026-04-04` 前完成 MO 注册和登录、岗位发布功能，确定数据存储的 JSON 格式并交由 TA 端开发；`2026-04-05` 至 `2026-04-08` 着重优化界面，后续待定。

---

## Entry 02 - MO-TA 招聘岗位 API v2 与数据落盘

**At 2026-04-02 07:00 (UTC+8), by Fenghao.**

### 新增/修改内容

1. **Job Board API v2（`/api/mo/jobs`）**
  - `GET`：返回 `schema: mo-ta-job-board`、`version: 2.0` 的岗位列表；数据源为 `mountDataTAMObupter/common/recruitment-courses.json`；对 `items` 做内存规范化，不写回磁盘。
  - `POST`：`MoJobBoardServlet` 将请求体整体交给 `MoRecruitmentDao.createCourse(body)` 落盘；补齐治理占位字段与 `status` / `recruitmentStatus` 兼容镜像。
2. **DAO 与挂载路径**
  - 扩展 `MoRecruitmentDao` 以读写 v2 字段；新增 `DataMountPaths` 统一解析 common/TA 挂载路径；增加 `syncJobBoardFileEnvelope`，在写入后同步顶层 `count`、`generatedAt` 等信封字段。
3. **技能标签接口**
  - 新增 `MoSkillTagsServlet`：`GET /api/mo/skill-tags` 与别名 `GET /api/common/skill-tags`，供发布表单拉取固定标签列表。
4. **MO 前端岗位模块**
  - 更新 `job-board.js`、`mo-route-jobs.jspf` 与 `mo-home.css`，对接 v2 字段与发布/列表流程；MO 所有者展示统一经 `moOwnerLabel`（优先 `ownerMoName` / `ownerMoId`），搜索索引不再依赖已废弃的 `moName`。
5. **契约与数据字段（`moName` 弃用）**
  - 新发布不再写入 `moName`；契约与治理文档仅以 `ownerMoId`、`ownerMoName` 表示 MO 身份。`GET` 归一化会从响应中去掉历史 `moName` 键，并在缺省 `ownerMoName` 时可用磁盘上的遗留 `moName` 回填一次。
6. **数据与文档**
  - 以 `recruitment-courses.json` 为岗位主数据；移除旧版待审核示例 JSON；样例数据中补充完整学期级字段（教学周、评估事件、技能等）便于联调。
  - 文档目录整理：契约 `docs/api/mo-job-board-api-v2.md`，后端交互 `docs/backend/mo-ta-interaction-log.md`，数据治理 `docs/database/recruitment-courses-governance-notes.md`；本日志位于 `docs/log/`（原 `projectFile/MO-module-development-log.md` 已迁出并删除）。

### 需要解决的问题

- 服务端校验与错误码体系仍为轻量实现，后续可按契约收紧。
- 岗位编辑、关闭、归档等管理能力尚未在 MO 端闭环。
- TA 端对 v2 新字段的生命周期更新与 MO 规范化路径是否完全一致，需联调确认。
- 其他 Entry 1 中提出的问题。

### 备注

- 契约细节以 `docs/api/mo-job-board-api-v2.md` 为准；TA 端 `GET /api/ta/jobs` 已读同一文件，行为以磁盘为准，与 MO `GET` 内存规范化路径不同，集成时注意字段来源。

---

## Entry 03 - 岗位库共用 DAO（`RecruitmentCoursesDao`）

**At 2026-04-02 03:00 (UTC+8), by Fenghao.**

### 新增/修改内容

1. **共用 DAO**：新增 `com.bupt.tarecruit.common.dao.RecruitmentCoursesDao`，集中 `recruitment-courses.json` 的路径、文件锁、**标准化读** `readJobBoard()`（MO/TA 共用）、发布追加落盘及 `findNormalizedJobByCourseCode`。
2. **MO**：`MoRecruitmentDao` 中岗位列表/发布/选人查课程名改为委托上述类；与 TA 申请数据相关的读写仍留在本 DAO。
3. **TA**：`TaAccountDao.getPendingJobBoardData` 与 MO 列表一致，委托 **`readJobBoard()`**，不再使用单独的磁盘透传读路径。
4. **文档**：`common/dao/README` 说明公用接口；`mountDataTAMObupter/common/recruitment-courses-dao-notes.md` 标注 Admin/TA 对岗位库专有写操作尚未实现。

### 需要解决的问题

- 与 Entry 02 中「校验、岗位编辑、TA 字段生命周期」等条目相同；本重构不新增业务功能。

### 备注

- 契约与行为仍以 `docs/api/mo-job-board-api-v2.md` 为准。

---

## 模板

### Entry 04 - [本次开发主题]

**At [YYYY-MM-DD HH:MM (UTC+8)], by [Name].**

#### 新增/修改内容

- [功能或页面1]
- [功能或页面2]
- [功能或页面3]

#### 需要解决的问题

- [问题1]
- [问题2]
- [问题3]

#### 备注（可选）

- [联调情况、依赖、风险、下次计划]

