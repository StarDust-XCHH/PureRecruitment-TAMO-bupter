# MO 侧 `webapp` 文件说明（`src/main/webapp`）

> 说明：仓库中**没有**名为 `webapp/mo` 的单一目录；MO 相关资源分布在 **`pages/mo/`**（`JSP`/`JSPF` 页面与片段）与 **`assets/mo/`**（`CSS`/`JS`）。根路径均为：`src/main/webapp/`。

---

## 1. 总览

| 目录 | 作用 |
| --- | --- |
| **`pages/mo/`** | MO 工作台 **HTML 壳**：主页面 `mo-home.jsp`，以及 `include` 的 **`partials/`**、**`routes/`** 片段。 |
| **`assets/mo/css/`** | MO **专有样式**（在共享 **`assets/ta/css/`** 之上叠加）。 |
| **`assets/mo/js/`** | MO **前端逻辑**：`mo-api-prefix`、`mo-home` 启动器、**`modules/`** 下各功能模块。 |

**共享依赖**：`mo-home.jsp` 会大量 `<link>` **`assets/ta/css/ta-*.css`**，布局与组件基样式来自 TA 侧，MO 用 **`mo-home.css`** 做差异化。

---

## 2. `partials/` 与 `routes/` 的分工

二者都是 **`JSPF` 片段**，由 **`mo-home.jsp`** 用 `<%@ include %>` 拼进**同一页**；区别在**在页面里的角色**，而不是技术格式。

| | **`partials/`** | **`routes/`** |
| --- | --- | --- |
| **含义** | **壳与共用块**：侧栏、顶栏、欢迎区、**全局弹窗**、新手引导等，**不**随「当前主功能 Tab」切换而整块替换。 | **路由面板（route panel）**：主内容区里**一块对应一个导航项**的 HTML；由 JS **显示/隐藏**切换，模拟单页内的「多屏」。 |
| **在 DOM 中的位置** | 包在 **`#moApp`** 里：`aside`、**`main` 内的顶栏/欢迎卡**；弹窗、引导多在 **`</div><!-- app -->` 之后**或与主列并列，属于**叠加层或固定 chrome**。 | 放在 **`<main>` 内的 `<section class="routes">`** 里，与侧栏 **`data-route`** 一一对应。 |
| **与 JS 的约定** | 一般配合 **`settings.js`**（主题/语言/用户）、**`modal.js`**、各业务模块里 **`openModal`** 等；**不负责**「当前 route 名 → 哪个面板 `.active`」。 | 每个文件约定 **`data-route="<name>"`**、容器常带 **`id="route-<name>"`**（具体以片段为准）；**`route-nav.js`** 根据导航点击切换 **`.route`** 的 **`active`**，**同一时间只亮一块**。 |
| **典型文件** | `mo-layout-sidebar.jspf`、`mo-layout-topbar.jspf`、`mo-welcome-card.jspf`、`mo-modals.jspf`、`mo-onboarding.jspf` | `mo-route-dashboard.jspf`（`dashboard`）、`mo-route-jobs.jspf`（`jobs`）、`mo-route-applicants.jspf`（`applicants`） |

**记忆法**：**`routes/` = 主列里「换屏」的几张大面板**；**`partials/` = 围着主列的框、顶栏、侧栏，以及**弹窗/引导**等全局 UI**。  
**例外说明**：**`mo-route-profile.jspf`** 在仓库里按 **`profile` 路由**写好，但 **`mo-home.jsp` 未 include**；侧栏当前只有 **`dashboard` / `jobs` / `applicants`**。资料/密码走 **设置弹窗 + `profile.js`**。若日后要在主列增加 **`profile` 面板**，需在 **`mo-home.jsp`** 的 **`<section class="routes">`** 里 **include** 该片段，并与 **`route-nav.js`**、侧栏 **`data-route`** 一致。

---

## 3. `pages/mo/`（页面与片段）

| 文件路径 | 作用 |
| --- | --- |
| **`pages/mo/mo-home.jsp`** | MO 工作台**唯一主入口**：文档结构、引入 CSS/JS、`include` 各片段；末尾 **`mo-home.js`** 启动 **`MOApp.modules`**。 |
| **`pages/mo/README.md`** | 本目录结构说明（与代码同步维护为佳）。 |
| **`pages/mo/partials/mo-layout-sidebar.jspf`** | **侧栏**：导航项（`data-route`）、应聘人员 **未读徽标** 等。 |
| **`pages/mo/partials/mo-layout-topbar.jspf`** | **顶栏**：主题、语言、用户菜单触发等。 |
| **`pages/mo/partials/mo-welcome-card.jspf`** | **欢迎区**卡片。 |
| **`pages/mo/partials/mo-modals.jspf`** | **全局弹窗** DOM（发布岗位、课程详情、设置、申请人详情等），与 **`modal.js`**、`job-board.js`、`applicants.js`、`profile.js` 等配合。 |
| **`pages/mo/partials/mo-onboarding.jspf`** | **新手引导** UI 壳，与 **`onboarding.js`** 配合。 |
| **`pages/mo/routes/mo-route-dashboard.jspf`** | **招聘总览**内容区（`dashboard`）。 |
| **`pages/mo/routes/mo-route-jobs.jspf`** | **课程/岗位管理**内容区（`jobs`）。 |
| **`pages/mo/routes/mo-route-applicants.jspf`** | **应聘筛选**内容区（`applicants`）。 |
| **`pages/mo/routes/mo-route-profile.jspf`** | **Profile 独立路由片段**（若存在）。**当前 `mo-home.jsp` 未 include**；个人资料与密码多在 **设置弹窗** + **`profile.js`**。 |
| **`pages/mo/partials/README.md`**、`routes/README.md` | 子目录说明。 |

---

## 4. `assets/mo/css/`（样式）

| 文件路径 | 作用 |
| --- | --- |
| **`assets/mo/css/mo-home.css`** | MO **主样式**：在 TA 共享样式之上做 MO 专属布局与组件覆盖。 |
| **`assets/mo/css/mo-styles.css`** | 备用样式文件；**`mo-home.jsp` 当前未 `<link>` 本文件**（主入口仅引用 **`mo-home.css`** + TA 侧 `ta-*.css`）。若其它页面或后续改动引用，以实际 `<link>` 为准。 |
| **`assets/mo/css/README.md`** | 样式分层与维护约定。 |

---

## 5. `assets/mo/js/`（脚本）

| 文件路径 | 作用 |
| --- | --- |
| **`assets/mo/js/mo-api-prefix.js`** | 定义 **`window.moApiPath(path)`**，为 API 路径拼接 **`context path`**，避免部署子路径时 **404**。 |
| **`assets/mo/js/mo-home.js`** | **模块启动器**：依次调用 **`MOApp.modules`** 中已注册的 **`settings` → `routeNav` → … → `onboarding`**；含 **`__MO_HOME_BOOTSTRAPPED__`** 防重复初始化。 |
| **`assets/mo/js/modules/settings.js`** | 登录校验、主题、**`i18n`**、退出、设置入口；注册 **`app.t`、`getMoUser`** 等。 |
| **`assets/mo/js/modules/route-nav.js`** | 侧栏与 **`.route`** 区块切换、滚动时同步高亮；**`activateRoute`**。 |
| **`assets/mo/js/modules/modal.js`** | 弹窗层 **`openModal` / `closeAllModals`**（`#taModalOverlay`）。 |
| **`assets/mo/js/modules/job-board.js`** | **岗位**：列表、发布/编辑、**`/api/mo/jobs`**、技能标签等。 |
| **`assets/mo/js/modules/applicants.js`** | **应聘人员**：列表、详情、未读、评论、决策、相关 **`/api/mo/applicants*`** 与 **`/api/mo/applications/*`**。 |
| **`assets/mo/js/modules/dashboard.js`** | **总览**指标与摘要（聚合 jobs/applicants 数据）。 |
| **`assets/mo/js/modules/profile.js`** | **资料与密码**（**`/api/mo/profile-settings`**、头像等）。 |
| **`assets/mo/js/modules/onboarding.js`** | **新手引导**流程。 |

---

## 6. 与主学习笔记的关系

- **`projectFile/mo-architecture-techstack-study-guide.md`**：架构分层、`MOApp`、`moApiPath`、HTTP、`DAO`、课程约束与中期考核等。
- **本文档**：仅聚焦 **`webapp` 下 MO 相关文件路径与职责**，便于快速查表与复习。
</think>


<｜tool▁calls▁begin｜><｜tool▁call▁begin｜>
Read