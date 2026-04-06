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

**At 2026-04-03 04:00 (UTC+8), by Fenghao.**

### 新增/修改内容

1. **共用 `RecruitmentCoursesDao`（`recruitment-courses.json`）**
  - 统一岗位主数据文件的路径解析、并发锁与契约常量（`mo-ta-job-board` / `jobs` / `2.0`）。
  - `**readJobBoard()`**：对磁盘 `items` 做内存归一化并组装响应（含 `schema`、`version`、`generatedAt`、`count`），供 MO 与 TA **同源读取**岗位大厅列表；读路径不写回磁盘。
  - `**appendPublishedJob`**：MO 发布岗位时在文件末尾追加一条，并同步 `meta` 与顶层信封字段后落盘。
  - 提供发布与展示所需的领域归一化与校验（教学周、考核事件、技能标签、校区、`normalizeJobItem` 等），保证列表与详情字段形状一致。
  - `**findNormalizedJobByCourseCode`**：按课程编码查询单条归一化岗位，供 MO 在录用/拒绝决策时展示课程名称等信息。
  - 提供按 `jobId`、治理字段、`ownerMoId` 等筛选/查询的静态方法，便于后续管理端或列表能力直接复用。
2. `**MoRecruitmentDao`**
  - 岗位待办列表、发布落盘、选人决策中的课程信息查询均通过 `**RecruitmentCoursesDao`** 完成，与 TA 侧读取同一套规范化结果。
  - TA 账户、档案、申请状态等 JSON 仍由本类负责；结构化文件的创建与 `**meta**` 维护通过 `**ensureStructuredFile(Path, entity)**`（`ta` schema、`1.0` 版本）统一处理。
3. **TA 岗位数据入口**
  - `**TaAccountDao.getPendingJobBoardData`** 使用 `**readJobBoard()`**，与 MO 岗位列表数据来源与展示形状一致。
4. **文档与挂载说明**
  - 更新 `**common/dao/README`**、`**mo/dao/README`** 等包内说明；`**mountDataTAMObupter/common/recruitment-courses-dao-notes.md**` 记录 Admin/TA 对岗位库的专有写操作尚未在本包实现等事项。

### 需要解决的问题

- 与 Entry 02 中「服务端校验、岗位编辑、TA 字段生命周期」等待办项相同；本阶段以 **共用 DAO 与委托调用** 为主，**不扩展新的业务规则**。
- 预留的按 `jobId`/治理/`ownerMoId` 查询能力尚未在 HTTP 层暴露，需产品化时再接线。

### 备注

- 契约与行为仍以 `**docs/api/mo-job-board-api-v2.md`** 为准。

---

## Entry 04 - MO 个人资料管理功能实现

**At 2026-04-04 15:20 (UTC+8), by Bowen.**

### 新增/修改内容

1. **MoAccountDao 扩展（资料管理核心）**
  - 新增 `getProfileSettings(String moId)`：读取 MO 的个人资料、账户信息和系统设置，自动补全缺失的 profile/setting 记录。
    - 新增 `saveProfileSettings(ProfileUpdateInput input)`：保存 MO 的个人资料（真实姓名、联系邮箱、个人简介、技能标签、头像路径），同步更新账户和设置文件。
    - 新增 `updatePassword(String moId, String currentPassword, String newPassword)`：验证旧密码后更新新密码，使用 SHA-256 + 随机盐加密存储。
    - 新增辅助方法：`findAccountByMoId()`、`ensureProfileRecord()`、`ensureSettingsRecord()`、`mapProfileData()`、`trimToNull()`、`trimToEmpty()`、`normalizeSkills()`。
    - 新增三个内部静态类：`ProfileResult`（查询结果）、`ProfileUpdateInput`（更新输入）、`PasswordUpdateResult`（密码更新结果）。
    - 新增静态方法 `getResolvedMoDataDir()`：供 Servlet 获取 MO 数据目录路径。
    - 导入 `java.util.Objects` 用于密码比较。
2. **MoProfileSettingsServlet（新建）**
  - URL 映射：`/api/mo/profile-settings`（资料管理 API）和 `/mo-assets/*`（静态资源服务）。
    - `GET /api/mo/profile-settings?moId=xxx`：返回 MO 的完整个人资料（账户 + 资料 + 设置）。
    - `POST /api/mo/profile-settings`：支持两种操作：
      - 默认操作：更新个人资料（含头像上传）
      - `action=password`：修改密码
    - 头像处理功能：
      - 支持 PNG/JPG/WEBP/GIF 格式，最大 10MB
      - 自动裁剪为正方形并缩放到 256x256 像素
      - 文件名格式：`{moId}_{timestamp}_{uuid}.{ext}`
      - 存储路径：`mountDataTAMObupter/mo/image/`
      - 上传成功后删除旧头像文件
    - 静态资源服务：通过 `/mo-assets/image/xxx.png` 访问头像文件，设置缓存头 `Cache-Control: public, max-age=86400`。
    - 完整的错误处理和用户反馈机制。
3. **前端页面与交互**
  - 新增 `mo-route-profile.jspf`：个人资料管理页面，包含：
  - 头像上传与预览区域
  - 基本信息编辑表单（真实姓名、联系邮箱、个人简介、技能标签）
  - 密码修改表单（当前密码、新密码、确认密码）
  - 实时保存状态提示
    - 新增 `profile.js` 模块：前端交互逻辑，包括：
      - 从 localStorage 自动加载当前 MO ID
      - 异步加载个人资料并填充表单
      - 头像选择与实时预览（FileReader API）
      - 资料提交（FormData + Fetch API）
      - 密码修改验证与提交
      - 友好的用户反馈（成功/失败提示）
    - 更新 `mo-home.jsp`：引入 `mo-route-profile.jspf` 路由和 `profile.js` 脚本。
    - 更新 `mo-layout-sidebar.jspf`：添加"个人资料"导航菜单项（⚙️ 图标）。
4. **数据存储结构**
  - 复用现有的三个 JSON 文件：
  - `mos.json`：主账户信息（name、email 等会同步更新）
  - `profiles.json`：个人资料（realName、contactEmail、bio、skills、avatar、lastUpdatedAt）
  - `settings.json`：系统设置（avatar、theme、profileSaved、profileSavedAt、lastProfileSyncStatus 等）
    - 头像文件存储在 `mountDataTAMObupter/mo/image/` 目录。

---

## Entry 05 - MO 个人资料/改密前端健壮性与文档同步

**At 2026-04-04 22:20 (UTC+8), by Fenghao.**

### 新增/修改内容

1. `**assets/mo/js/modules/profile.js`**
  - **接口与静态资源路径**：`PROFILE_API`、`ASSETS_BASE` 改为相对 `pages/mo/mo-home.jsp` 的 `../../api/mo/profile-settings` 与 `../../mo-assets`，与 `job-board.js`、`applicants.js` 一致，避免非根 context 部署时请求落到站点根路径导致 404。
  - **改密安全与体验**：移除遍历 `FormData` 及敏感调试输出，避免在浏览器控制台泄露当前密码/新密码明文；新密码与确认密码以 **trim 后** 校验长度（≥6）与一致性，提交 `newPassword` 为 trim 后内容，与后端 `MoAccountDao.updatePassword` 中对 `trim().length()` 的规则一致；当前密码仍原样提交以匹配登录哈希。
  - **登录态读取**：`loadMoIdFromStorage` 与 `settings.js` 对齐，优先 `sessionStorage` 的 `mo-user`，其次 `localStorage` 的 `mo-user`，再兜底 `localStorage` 的 `moUser`，减少仅一侧存储有数据时资料/改密不可用的问题。
2. **文档**
  - `assets/mo/js/modules/README.md`：补充 `profile.js` 职责说明，并明确 `settings.js` 的存储读取顺序。
  - `assets/mo/js/README.md`：`mo-home.js` 启动顺序中补上 `profile`。
  - `pages/mo/routes/README.md`：路由表增加 `mo-route-profile.jspf`。
  - `pages/mo/README.md`：路由数量与模块加载顺序与当前 `mo-home.jsp` / `mo-home.js` 一致（含 profile）。

### 需要解决的问题

- 与 Entry 04 相同的后端/产品项仍适用：例如资料与改密接口的会话绑定策略、服务端对新密码统一 trim 落盘等若需更强一致性，可在 DAO 层再收紧（当前以前端 trim 提交为主）。
- Entry 01～03 中已列的 MO 能力缺口（岗位编辑、校验体系等）不受影响，仍按原 backlog 推进。

### 备注

- 后端 `MoProfileSettingsServlet` / `MoAccountDao` 本次未改；行为变更集中在 MO 端 `profile.js` 与配套 README。

---

## Entry 06 - Git 误操作与分支以谁为准

**At 2026-04-05 00:33 (UTC+8), by Fenghao.**

### 说明

- 合并 / 切换分支等 **Git 误操作导致本地状态混乱**，与 Entry 05 相关的 MO 个人资料、改密前端及 README 曾在工作区出现回退或不同步。
- **截至本时间点，问题已处理完毕**：已在分支 `**Fenghao/MOPwdEdit_Restart`** 上按 Entry 05 所述范围，将 `profile.js` 与配套 README 等 **文件级修改重新对齐**；开发日志 Entry 05 内容保持不变作为功能说明。
- **权威版本**：后续以 `**Fenghao/MOPwdEdit_Restart` 分支当前提交与工作区为准**（若再合并入主干或其它分支，应以此分支上的实现为对照，避免重复出现回退）。

### 备注

- 本条目仅记录过程与约定，**不涉及新的产品功能变更**。

---

## Entry 07 - MO 候选人详情、未读/已读、评论与岗位归属收紧

**At 2026-04-06 02:30 (UTC+8), by Fenghao.**

### 新增/修改内容

1. **HTTP API（`mo.controller`）**
  - 新增 `**MoApplicantDetailServlet`**（`/api/mo/applicants/detail`）：按 `**moId` + `applicationId`** 返回单条申请详情。
  - 新增 `**MoApplicantResumeServlet**`（`/api/mo/applications/resume`）：校验岗位归属后流式返回简历文件。
  - 新增 `**MoApplicantsUnreadServlet**`（`/api/mo/applicants/unread-count`）：按 `**moId**` 汇总未读数量，供侧边栏角标。
  - 新增 `**MoApplicationMarkReadServlet**`（`/api/mo/applications/mark-read`）：`POST` 标记 MO 已读，并经 DAO 将 TA 侧 `**applications.json**` 中对应申请同步为 `**UNDER_REVIEW**`（不修改 TA 模块 Java，由 MO 侧变更 DAO 写入）。
  - 新增 `**MoApplicationCommentServlet**`（`/api/mo/applications/comment`）：`POST` 追加 MO 端评论线程（仅存 MO 旁路 JSON）。
  - `**MoApplicantsServlet**`：列表响应补充 `**unreadCount**` 等与未读/筛选相关的字段（与 `MoRecruitmentDao` 对齐）。
  - `**MoApplicationDecisionServlet**`：请求体增加 `**moId**`，调用 `**decideApplication(courseCode, taId, moId, decision, comment)**` 五参重载，在 DAO 层解析岗位归属后再写 `**application-status.json**`。
  - `**MoJobBoardServlet**`：`GET` 仅返回 **当前 `moId` 名下** 岗位（`ownerMoId` 过滤）；`POST` 发布时 `**ownerMoId` 以 query/body 的 `moId` 为准**，不信任客户端随意提交的 `ownerMoId`。
  - `**MoProfileSettingsServlet`**：小幅调整（与资料/静态资源路径一致化）。
  - 更新 `**mo/controller/README.md`**：登记上述 URL、契约与 `**mo-application-read-state.json` / `mo-application-comments.json`** 数据说明。
2. **DAO 与挂载路径**
  - `**DataMountPaths`**：增加 MO 申请旁路文件路径解析（已读状态、评论 JSON）。
  - 新增 `**MoApplicationReadStateDao`**：维护 `**mo-application-read-state.json`**，按 `**(moId, applicationId)**` 记录已读时间，支撑未读红点与列表统计。
  - 新增 `**MoApplicationCommentsDao**`：维护 `**mo-application-comments.json**`，按 `**applicationId**` 追加/读取 MO 评论。
  - 新增 `**MoTaApplicationsMutationDao**`：对 `**mountDataTAMObupter/ta/applications.json**` 做受控写入（例如已读联动状态），与 TA 只读/状态服务解耦边界清晰。
  - `**MoRecruitmentDao**` 大幅扩展：课程维度申请人列表、详情、未读统计、已读、评论、录用/拒绝决策等与 `**MoTaApplicationReadService**`、`**application-status.json**`、`**RecruitmentCoursesDao**` 的组合调用；`**ownerMoId` 仅与账号 `id` 比对**；发布岗位时 `**ownerMoName` 取自 `mos.json` 的 `name`**，`**profiles.json` 不再依赖 `realName` 字段**（与账户展示名合并策略一致）。
  - `**MoAccountDao`**：配合上述资料/姓名字段策略的调整（与提交说明中「`realName` 合并到 `name`」方向一致）。
  - 更新 `**mo/dao/README.md`**、`**common/config/README.md`**（`DataMountPaths` 与 MO 旁路路径说明）、`**mo/service/README.md**` 等包内文档。
3. **MO 前端（`assets/mo`、`pages/mo`）**
  - 新增 `**mo-api-prefix.js`**：统一 API 根路径前缀，与其它模块一致，避免非根 context 下 404。
  - `**applicants.js`**：对接详情、未读、已读、评论、简历下载等接口；与岗位列表跳转、弹窗交互联动。
  - `**job-board.js`**、`**mo-home.css**`：岗位列表与候选人流程相关的展示与样式补充。
  - `**mo-layout-sidebar.jspf**`：侧边栏 **未读角标**（拉取 unread-count）。
  - `**mo-route-applicants.jspf`**、`**mo-home.jsp`**：路由与脚本挂载调整。
  - `**profile.js**`、`**settings.js**`：小幅度与存储键/API 前缀对齐。
  - 更新 `**assets/mo/js/modules/README.md**`、`**pages/mo/README.md**`、`**pages/mo/routes/README.md**`。
4. **开发辅助（与 MO 测试数据一致）**
  - `**com.bupt.tarecruit.tools.DevApplicationDataCleanupTool`**（及 `**tools/ta-mo-submission-cleanup/`** 下 README、`**run-dev-application-data-cleanup.ps1**`）：一键对齐 TA 申请/AI 清理与 **MO 已读/评论旁路 JSON** 空状态，便于联调后重置（主类名避免与 `TaSubmissionCleanupTool` 混淆）。

### 需要解决的问题

- MO 已读与 TA `**applications.json` / `application-status.json`** 的时序与并发下是否仍需更强事务语义，需在高压联调中观察。
- 评论仅存在 MO 侧 JSON，若未来需 TA 可见或审计，需单独产品设计与同步策略。可更新为公用评论，即TA和MO均可发布评论至公用磁盘数据。
- Entry 01～03 中已列的岗位编辑、校验体系、仪表盘增强等 backlog 仍适用。

### 备注

- 本条依据 **当前分支相对 `master` 的 diff**（MO 相关包、`DataMountPaths`、`assets/mo`、`pages/mo` 及 MO 旁路数据说明）整理；**挂载目录下示例 JSON**（如 `mo-application-*.json`、`mos.json` 等）若随 diff 变更，以工作区与数据治理文档为准。

---

## Entry 08 - 岗位编辑、申请/名额统计同步与 jobId 治理

**At 2026-04-06 05:02 (UTC+8), by Fenghao.**

### 新增/修改内容

1. **已发布课程的编辑能力**
  MO 可在岗位大厅流程中对已有课程/岗位信息发起编辑并保存，与列表与详情展示保持一致，补全此前「仅能发布与浏览」的缺口。
2. **课程维度申请数量统计与数据同步**
  在岗位库侧维护与课程相关的申请统计，并在 MO 岗位界面侧同步展示；实现上当前以课程编码为关联维度，需在数据唯一性与边界场景上持续关注（见下节）。
3. **TA 招聘人数校验与统计联动**
  发布或变更岗位时对计划招收 TA 人数做约束校验，并与上述统计信息一并更新，避免名额字段与实际情况长期脱节。
4. **岗位 `jobId` 的唯一性生成与校验**
  为岗位条目提供稳定、唯一的业务标识生成与冲突检测，降低列表、筛选与后续决策链路中因标识重复或缺失导致的不一致风险。
5. **分支集成与配套调整**
  合并 `Fenghao/MO-Applicants` 相关能力时的冲突已处理；样例挂载数据、课程生成脚本及开发用申请数据清理说明随岗位字段与流程做了对齐，便于联调与重置环境。

### 需要解决的问题

- **按 `courseCode` 维度的统计与查询** 在多课号、历史数据或重名场景下存在潜在歧义，后续宜结合 `jobId` 或文件内稳定主键收紧关联策略。
- Entry 01～07 中已列的通用 backlog（岗位关闭/归档、服务端校验与错误码体系、仪表盘增强等）仍然适用。

### 备注

- 本条依据当前分支相对 `**master`** 的提交记录与变更范围整理，**不涉及逐文件清单**；契约与字段细节仍以 `docs/api/mo-job-board-api-v2.md` 及包内 README 为准。

---

## MO 模块待解决问题小结（本时间点汇总）

**At 2026-04-06 05:14 (UTC+8).**

> 由 Entry 01～08 各条「需要解决的问题 / 新增问题」合并、去重后整理；**已在后续条目中落地的能力不再重复列出**（例如：岗位大厅 v2 API 与落盘、共用岗位库 DAO、MO 个人资料与改密、候选人详情/简历/未读与评论与录用决策后端、岗位归属过滤、**岗位编辑**、申请与名额统计及 `jobId` **治理** 等）。后续，在问题陆续解决和落地后，应定期重新撰写新的小结。

### 岗位与主数据

- **岗位生命周期仍不完整**：关闭、下架或归档等管理能力尚未在 MO 端形成闭环（Entry 01、02）。
- **服务端校验与错误码偏轻量**：需按契约系统化收紧，并与前端错误展示对齐（Entry 02、07、08）。
- **按 `courseCode` 的统计与关联存在歧义风险**：多课号、历史数据或重名场景下可能与真实岗位条目错位，宜结合 `**jobId` 或文件内稳定主键** 收紧策略（Entry 08）。
- **TA 侧对 v2 新字段的生命周期**：与 MO 规范化路径是否始终一致，需持续联调确认（Entry 02、03）。
- **DAO 层预留能力尚未产品化**：按 `jobId`、治理字段、`ownerMoId` 等的筛选/查询能力未在 HTTP 层统一暴露，待有管理端或列表需求时再接线（Entry 03）。

### 候选人、状态与协作

- **已读 / 状态多数据源**：MO 已读与 TA `applications.json`、`application-status.json` 等在时序与并发下是否需更强一致性语义，需在高压联调中验证（Entry 07）。
- **评论范围**：当前评论仅存 MO 旁路数据；若需 TA 可见、对账或审计，需单独设计与同步策略（例如公用评论线程）（Entry 07）。
- **候选人信息仍可加深**：除已有详情与简历外，可用时间等更完整画像与展示仍可加强（Entry 01）。
- **录用/拒绝交互**：确认弹窗、防误触与明确成功/失败反馈仍可加强（Entry 01）。

### 仪表盘、导航与体验

- **仪表盘偏静态**：总览指标之外，趋势、逾期或待办提醒等管理向能力仍不足（Entry 01）。
- **主界面信息密度**：单页过长时，可考虑将发布岗位、筛选候选人等拆为二级页或更清晰的导航结构（Entry 01）。
- **导览与动画**：步骤高亮、跳转与过渡动画等与原型一致的体验仍不完整（Entry 01）。
- **首次登录与新手引导**：尚未按 MO 设计方向补全（Entry 01）。
- **视觉与可用性**：深色模式对比度、卡片排版等细节需持续打磨（Entry 01）。

### 账户、安全与工程约定

- **MO 注册、登录与工作台总览**：Entry 01 中「核心后端未完成」里，岗位发布与候选人决策等已后续落地；若注册/登录或仪表盘指标仍依赖临时方案、或与挂载数据/API 未完全对齐，需收尾并写入契约与运维说明（Entry 01）。
- **个人资料与改密接口的安全模型**：若仍以显式 `moId` 等与前端存储为主，需评估升级为与服务端会话强绑定；新密码是否在 DAO 层统一 `trim` 等与前端规则完全一致，可按需要收紧（Entry 05）。
- **分支与回退约定**：历史 Git 误操作教训见 Entry 06；合并功能时仍以约定分支上的实现为对照，避免重复回退。

---

## 模板

### Entry 09 - [本次开发主题]

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

