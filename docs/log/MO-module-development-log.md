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
   - **`readJobBoard()`**：对磁盘 `items` 做内存归一化并组装响应（含 `schema`、`version`、`generatedAt`、`count`），供 MO 与 TA **同源读取**岗位大厅列表；读路径不写回磁盘。
   - **`appendPublishedJob`**：MO 发布岗位时在文件末尾追加一条，并同步 `meta` 与顶层信封字段后落盘。
   - 提供发布与展示所需的领域归一化与校验（教学周、考核事件、技能标签、校区、`normalizeJobItem` 等），保证列表与详情字段形状一致。
   - **`findNormalizedJobByCourseCode`**：按课程编码查询单条归一化岗位，供 MO 在录用/拒绝决策时展示课程名称等信息。
   - 提供按 `jobId`、治理字段、`ownerMoId` 等筛选/查询的静态方法，便于后续管理端或列表能力直接复用。

2. **`MoRecruitmentDao`**
   - 岗位待办列表、发布落盘、选人决策中的课程信息查询均通过 **`RecruitmentCoursesDao`** 完成，与 TA 侧读取同一套规范化结果。
   - TA 账户、档案、申请状态等 JSON 仍由本类负责；结构化文件的创建与 **`meta`** 维护通过 **`ensureStructuredFile(Path, entity)`**（`ta` schema、`1.0` 版本）统一处理。

3. **TA 岗位数据入口**
   - **`TaAccountDao.getPendingJobBoardData`** 使用 **`readJobBoard()`**，与 MO 岗位列表数据来源与展示形状一致。

4. **文档与挂载说明**
   - 更新 **`common/dao/README`**、**`mo/dao/README`** 等包内说明；**`mountDataTAMObupter/common/recruitment-courses-dao-notes.md`** 记录 Admin/TA 对岗位库的专有写操作尚未在本包实现等事项。

### 需要解决的问题

- 与 Entry 02 中「服务端校验、岗位编辑、TA 字段生命周期」等待办项相同；本阶段以 **共用 DAO 与委托调用** 为主，**不扩展新的业务规则**。
- 预留的按 `jobId`/治理/`ownerMoId` 查询能力尚未在 HTTP 层暴露，需产品化时再接线。

### 备注

- 契约与行为仍以 **`docs/api/mo-job-board-api-v2.md`** 为准。

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
## 模板

### Entry 05 - [本次开发主题]

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

