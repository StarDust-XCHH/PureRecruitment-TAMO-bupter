# MO 部分原型设计与详细说明

## 1. 原型范围与设计目标

本文档基于图片原型 [`img.png`](projectFile/Fenghao/imgStage1Prototype/img.png)、[`img_1.png`](projectFile/Fenghao/imgStage1Prototype/img_1.png)、[`img_2.png`](projectFile/Fenghao/imgStage1Prototype/img_2.png)、[`img_3.png`](projectFile/Fenghao/imgStage1Prototype/img_3.png)、[`img_4.png`](projectFile/Fenghao/imgStage1Prototype/img_4.png)、[`img_5.png`](projectFile/Fenghao/imgStage1Prototype/img_5.png) 以及本仓库中 MO 端现有 JSP、JavaScript、Servlet 与 JSON 数据结构整理而成，面向 Module Organizer（MO）角色，对“岗位发布—候选人查看—筛选决策回写”这一核心业务进行详细原型描述。

MO 模块的目标不是做一个完整的教学管理后台，而是聚焦 TA 招聘流程中的三个关键任务：

1. 让 MO 能快速查看当前招聘总体情况；
2. 让 MO 能独立发布课程岗位并维护课程信息；
3. 让 MO 能查看候选人资料，并直接完成录用/拒绝决策。

从代码结构来看，MO 工作台页面入口为 [`mo-home.jsp`](src/main/webapp/pages/mo/mo-home.jsp)，其内部包含三块主路由：

- 总览页：[`mo-route-dashboard.jspf`](src/main/webapp/pages/mo/routes/mo-route-dashboard.jspf)
- 岗位发布页：[`mo-route-jobs.jspf`](src/main/webapp/pages/mo/routes/mo-route-jobs.jspf)
- 应聘筛选页：[`mo-route-applicants.jspf`](src/main/webapp/pages/mo/routes/mo-route-applicants.jspf)

原型实现上，MO 模块复用了 TA 端已有布局与组件体系，这一点在页面头部引入的样式文件中可以直接看到，例如 [`mo-home.jsp`](src/main/webapp/pages/mo/mo-home.jsp:8) 复用了 TA 侧多个 CSS 文件，并在 [`mo-home.jsp`](src/main/webapp/pages/mo/mo-home.jsp:13) 额外加载了 MO 自有样式文件 [`mo-home.css`](src/main/webapp/assets/mo/css/mo-home.css)。这说明该模块在视觉上遵循“统一系统外观、局部功能定制”的设计原则。

---

## 2. 从图片原型得到的整体界面风格判断

根据提供的图片，可以总结出系统当前整体原型设计具有以下明显特征：

### 2.1 视觉风格

从 [`img_1.png`](projectFile/Fenghao/imgStage1Prototype/img_1.png) 到 [`img_5.png`](projectFile/Fenghao/imgStage1Prototype/img_5.png) 可以看出，项目整体采用偏深色的后台工作台风格：

- 左侧为固定纵向导航栏；
- 顶部和主体内容区采用卡片式信息容器；
- 主色为深蓝灰背景 + 紫蓝高亮按钮；
- 关键状态使用颜色胶囊（pill）突出，如高优先级、角色标签、状态标签；
- 大量模块通过“信息卡片 + 表格/列表 + 操作按钮”的方式组织内容。

这与 MO 现有页面中侧边导航、顶部栏、统计卡片、课程卡片和候选人卡片的实现方式完全一致，例如：

- 侧边导航定义在 [`mo-layout-sidebar.jspf`](src/main/webapp/pages/mo/partials/mo-layout-sidebar.jspf)
- 顶栏定义在 [`mo-layout-topbar.jspf`](src/main/webapp/pages/mo/partials/mo-layout-topbar.jspf)
- 欢迎卡片定义在 [`mo-welcome-card.jspf`](src/main/webapp/pages/mo/partials/mo-welcome-card.jspf)
- 总览统计卡片定义在 [`mo-route-dashboard.jspf`](src/main/webapp/pages/mo/routes/mo-route-dashboard.jspf:13)

因此，MO 原型在视觉上应被定位为：**与系统后台整体一致的中保真业务工作台原型**。

### 2.2 信息结构风格

图片中 ADMIN 原型体现出比较强的“后台管理”信息架构：

- 一级导航明确；
- 页面标题突出；
- 内容区域按任务分区；
- 操作尽量就地完成；
- 用状态和标签帮助快速浏览。

MO 端当前代码也遵循同样逻辑，只不过 ADMIN 的导航项是“数据看板/用户管理/公告管理/权限管理/系统设置”，而 MO 的导航项精简为：

- 岗位概览
- 岗位发布
- 应聘筛选

对应实现位于 [`mo-layout-sidebar.jspf`](src/main/webapp/pages/mo/partials/mo-layout-sidebar.jspf:8)。

这说明 MO 模块是一个围绕招聘任务的轻量后台，而不是复杂的全功能后台。

---

## 3. MO 模块的信息架构设计

### 3.1 页面入口与整体布局

MO 页面主入口为 [`mo-home.jsp`](src/main/webapp/pages/mo/mo-home.jsp)。其结构可以概括为：

1. 左侧固定导航区；
2. 右侧主工作区；
3. 主工作区内依次包含顶部栏、欢迎卡片、路由内容区；
4. 页面底部挂载统一模态框层。

从 [`mo-home.jsp`](src/main/webapp/pages/mo/mo-home.jsp:16) 到 [`mo-home.jsp`](src/main/webapp/pages/mo/mo-home.jsp:31) 可以看出，这一页面结构非常清晰：

- `#moApp` 是应用根容器；
- `aside.sidebar` 用于承载导航；
- `main.main` 用于承载主内容；
- `section.routes` 中挂载所有业务页面；
- 模态框单独通过 [`mo-modals.jspf`](src/main/webapp/pages/mo/partials/mo-modals.jspf) 管理。

### 3.2 导航设计

导航区内容定义在 [`mo-layout-sidebar.jspf`](src/main/webapp/pages/mo/partials/mo-layout-sidebar.jspf)。品牌区显示：

- 缩写 Logo：MO
- 完整角色名：Module Organizer
- 副标题：Recruitment Suite

该设计强调两层含义：

- 当前页面身份明确属于 MO；
- 当前模块职责是招聘套件，而不是通用教务系统。

导航项只有三个，分别对应三条主任务路径：

| 导航项 | 业务含义 | 对应路由 |
|---|---|---|
| 岗位概览 | 先看整体情况 | `dashboard` |
| 岗位发布 | 创建与维护课程招聘岗位 | `jobs` |
| 应聘筛选 | 查看候选人并做出决定 | `applicants` |

这种设计很合理，因为 MO 角色的主要任务是顺序性的：先了解当前情况，再发布岗位，再进行候选人筛选。

### 3.3 顶栏设计

顶部栏定义在 [`mo-layout-topbar.jspf`](src/main/webapp/pages/mo/partials/mo-layout-topbar.jspf)。其中包含：

- 欢迎语与页面说明；
- 当前用户头像按钮；
- 退出登录按钮；
- 主题切换按钮。

其交互逻辑由 [`settings.js`](src/main/webapp/assets/mo/js/modules/settings.js) 和 [`modal.js`](src/main/webapp/assets/mo/js/modules/modal.js) 驱动：

- 主题切换按钮点击后会调用 [`toggleTheme()`](src/main/webapp/assets/mo/js/modules/settings.js:47)，在亮色/暗色之间切换；
- 点击用户按钮会打开 settings 模态框，入口在 [`modal.js`](src/main/webapp/assets/mo/js/modules/modal.js:36)；
- 点击退出登录会清理 `sessionStorage` 与 `localStorage` 中的 `mo-user`，实现位于 [`settings.js`](src/main/webapp/assets/mo/js/modules/settings.js:63)。

### 3.4 欢迎卡片设计

欢迎卡片定义在 [`mo-welcome-card.jspf`](src/main/webapp/pages/mo/partials/mo-welcome-card.jspf)。当前文字写明：

- 这是 MO 工作台原型；
- 支持岗位发布、应聘者浏览、录用状态回写；
- 遵循 handout 中要求的 JSON 存储 + Servlet/JSP 方案。

这个欢迎卡虽然体量不大，但它在原型中承担了“解释产品边界”的作用，非常适合作为中保真原型第一页的定位说明。

---

## 4. MO 核心业务流程设计

MO 模块围绕一个完整闭环展开：

1. 登录进入 MO 工作台；
2. 在概览页查看当前开放岗位和候选池状态；
3. 在岗位发布页创建新的课程招聘岗位；
4. 打开某课程详情，确认标签、时间、地点与工作清单；
5. 跳转到应聘筛选页查看候选人；
6. 针对候选人执行录用或拒绝；
7. 决策结果回写到 TA 的申请状态数据文件中。

这个流程分别由以下模块支撑：

- 路由切换：[`route-nav.js`](src/main/webapp/assets/mo/js/modules/route-nav.js)
- 岗位加载与发布：[`job-board.js`](src/main/webapp/assets/mo/js/modules/job-board.js)
- 候选人加载与决策：[`applicants.js`](src/main/webapp/assets/mo/js/modules/applicants.js)
- 统计总览：[`dashboard.js`](src/main/webapp/assets/mo/js/modules/dashboard.js)
- 后端接口：[`MoJobBoardServlet.java`](src/main/java/com/bupt/tarecruit/mo/controller/MoJobBoardServlet.java)、[`MoApplicantsServlet.java`](src/main/java/com/bupt/tarecruit/mo/controller/MoApplicantsServlet.java)、[`MoApplicationDecisionServlet.java`](src/main/java/com/bupt/tarecruit/mo/controller/MoApplicationDecisionServlet.java)
- 数据访问与回写：[`MoRecruitmentDao.java`](src/main/java/com/bupt/tarecruit/mo/dao/MoRecruitmentDao.java)

这说明 MO 原型并不是静态页面，而是一个已经具备真实数据读写链路的交互原型。

---

## 5. 页面一：MO 招聘总览原型设计

### 5.1 页面目标

总览页对应 [`mo-route-dashboard.jspf`](src/main/webapp/pages/mo/routes/mo-route-dashboard.jspf)。其目标不是展示全部细节，而是帮助 MO 在最短时间内回答四个问题：

1. 当前有多少开放岗位？
2. 当前候选池规模如何？
3. 已经录用了多少人？
4. 还有多少候选人待处理？

### 5.2 页面组成

页面由一个总览卡片构成，包含：

- 标题“MO 招聘总览”；
- 说明文字“面向用户故事 7/8/9：发布岗位、查看应聘、完成选择”；
- 角色与学期标签；
- 四张统计卡片。

四张统计卡片在 [`mo-route-dashboard.jspf`](src/main/webapp/pages/mo/routes/mo-route-dashboard.jspf:14) 至 [`mo-route-dashboard.jspf`](src/main/webapp/pages/mo/routes/mo-route-dashboard.jspf:53) 中定义，分别为：

- 开放岗位 [`dashOpenJobs`](src/main/webapp/pages/mo/routes/mo-route-dashboard.jspf:22)
- 候选池规模 [`dashCandidates`](src/main/webapp/pages/mo/routes/mo-route-dashboard.jspf:32)
- 已录用 [`dashAccepted`](src/main/webapp/pages/mo/routes/mo-route-dashboard.jspf:42)
- 待决策 [`dashPending`](src/main/webapp/pages/mo/routes/mo-route-dashboard.jspf:52)

### 5.3 数据来源与统计逻辑

统计逻辑由 [`dashboard.js`](src/main/webapp/assets/mo/js/modules/dashboard.js) 实现：

- 开放岗位数来自 [`app.getJobs()`](src/main/webapp/assets/mo/js/modules/dashboard.js:16) 返回的岗位数据；
- 候选池规模来自最近一次已加载候选人数组；
- 已录用数量通过筛选状态为“已录用”的候选人得到，见 [`dashboard.js`](src/main/webapp/assets/mo/js/modules/dashboard.js:18)；
- 待决策数量通过筛选既不是“已录用”也不是“未录用”的候选人得到，见 [`dashboard.js`](src/main/webapp/assets/mo/js/modules/dashboard.js:19)。

这意味着当前总览页是“轻汇总”模式：

- 它不是汇总全系统所有课程的全部候选人；
- 它是基于当前已加载岗位和候选数据进行统计；
- 更适合做 MO 当前操作上下文中的即时反馈。

### 5.4 交互设计

第一张统计卡“开放岗位”支持点击打开摘要模态框，通过 `data-modal-target="summary"` 触发，定义在 [`mo-route-dashboard.jspf`](src/main/webapp/pages/mo/routes/mo-route-dashboard.jspf:14)。

模态框内容位于 [`mo-modals.jspf`](src/main/webapp/pages/mo/partials/mo-modals.jspf:47)，其作用是：

- 解释该页面能做什么；
- 用简洁的三条说明概括岗位发布、应聘查看、筛选回写三个关键能力；
- 帮助首次进入页面的用户理解模块定位。

### 5.5 原型设计评价

从原型角度看，总览页比较适合放在登录后的首页，因为：

- 信息量少但关键信息完整；
- 可快速建立“系统当前状态感”；
- 不要求用户立刻进入复杂操作；
- 与后台系统中常见的数据卡片模式一致。

如果后续迭代，可以增加：

- 最近发布岗位列表；
- 最近处理候选人记录；
- 即将开课的岗位提醒；
- 待处理候选人排序建议。

但在当前阶段，这一总览页已经足以支撑中保真原型的核心目标。

---

## 6. 页面二：岗位发布与管理原型设计

### 6.1 页面目标

岗位发布页对应 [`mo-route-jobs.jspf`](src/main/webapp/pages/mo/routes/mo-route-jobs.jspf)。它承担 MO 模块最重要的输入任务：让 MO 创建课程岗位，并查看当前已开放的课程招聘列表。

### 6.2 页面结构

整个页面可以拆成四个区块：

1. 页面头部信息区；
2. 岗位发布表单区；
3. 搜索框；
4. 岗位卡片列表 + 分页区。

#### 6.2.1 页面头部信息区

头部包括：

- 标题“岗位发布与管理”；
- 说明文字：发布课程岗位后会写入 [`pending-recruitment-courses.json`](mountDataTAMObupter/mo/pending-recruitment-courses.json)；
- 开放课程数量胶囊；
- 刷新列表按钮。

这里的设计非常重要，因为它直接把“页面操作”和“数据落点”联系起来，便于演示原型时说明系统不是假数据展示，而是有真实 JSON 存储的。

#### 6.2.2 岗位发布表单区

发布表单定义在 [`mo-route-jobs.jspf`](src/main/webapp/pages/mo/routes/mo-route-jobs.jspf:17)。字段包括：

| 字段 | 输入控件 | 是否必填 | 说明 |
|---|---|---|---|
| 课程名称 | 文本框 | 是 | 课程或岗位主题 |
| 课程日期 | 日期选择器 | 是 | 上课日期 |
| 课程时间 | 文本框 | 是 | 时间段，如 14:00-16:00 |
| 上课地点 | 文本框 | 否 | 教室或实验室 |
| 标签 | 文本框 | 否 | 逗号分隔，生成 `keywordTags` |
| 工作清单 | 文本框 | 否 | 逗号分隔，生成 `checklist` |
| 课程描述 | 多行文本 | 否 | 岗位背景与课程说明 |

提交按钮是“发布岗位”，旁边有发布状态文字区 [`publishJobStatus`](src/main/webapp/pages/mo/routes/mo-route-jobs.jspf:27)。

### 6.3 表单交互与数据处理

前端表单提交逻辑位于 [`publishJob()`](src/main/webapp/assets/mo/js/modules/job-board.js:154)。主要流程如下：

1. 阻止表单默认提交；
2. 从会话中读取当前 MO 用户，读取逻辑见 [`getMoUser()`](src/main/webapp/assets/mo/js/modules/settings.js:16)；
3. 组装请求体，包含：
   - `moName`
   - `courseName`
   - `courseDate`
   - `courseTime`
   - `courseLocation`
   - `keywordTags`
   - `checklist`
   - `courseDescription`
4. 调用 `POST ../../api/mo/jobs`；
5. 成功后显示“发布成功”，清空表单，并重新加载岗位列表。

后端接口入口是 [`MoJobBoardServlet.doPost()`](src/main/java/com/bupt/tarecruit/mo/controller/MoJobBoardServlet.java:32)。真正的数据写入逻辑在 [`createCourse()`](src/main/java/com/bupt/tarecruit/mo/dao/MoRecruitmentDao.java:57)。

### 6.4 发布后的字段生成规则

从 [`createCourse()`](src/main/java/com/bupt/tarecruit/mo/dao/MoRecruitmentDao.java:65) 到 [`createCourse()`](src/main/java/com/bupt/tarecruit/mo/dao/MoRecruitmentDao.java:102) 可以提炼出完整字段设计：

| 字段 | 生成方式 | 说明 |
|---|---|---|
| `courseName` | 表单输入 | 课程名称 |
| `courseCode` | 自动生成 | 由课程名生成编码，再拼接随机后缀 |
| `moName` | 当前登录 MO 或默认 MO | 发布人 |
| `courseDate` | 表单输入 | 上课日期 |
| `courseTime` | 表单输入 | 上课时间 |
| `courseLocation` | 表单输入或默认“待安排” | 上课地点 |
| `studentCount` | 固定为 0 | 初始学生数 |
| `status` | 固定为“等待招聘 TA” | 招聘状态 |
| `workload` | 固定默认文案 | 工作量说明 |
| `courseDescription` | 表单输入或默认文案 | 课程描述 |
| `keywordTags` | 由逗号分隔字符串解析 | 课程标签 |
| `checklist` | 由逗号分隔字符串解析 | 工作任务清单 |
| `suggestion` | 默认推荐语 | 筛选建议 |
| `createdAt` | 当前时间 | 创建时间 |
| `source` | 固定 `mo-manual` | 数据来源 |

课程编号生成函数为 [`buildCourseCode()`](src/main/java/com/bupt/tarecruit/mo/dao/MoRecruitmentDao.java:367)，它的规则是：

- 前缀固定 `MO-`；
- 取课程名中的中英文和数字字符；
- 最长截到 12 位；
- 再追加 4 位随机后缀。

这一规则适合原型阶段，因为：

- 便于快速生成唯一编号；
- 兼容中文课程名；
- 编码可读性比纯 UUID 更强。

### 6.5 岗位列表展示设计

岗位列表容器为 [`jobBoard`](src/main/webapp/pages/mo/routes/mo-route-jobs.jspf:35)。渲染逻辑在 [`renderBoard()`](src/main/webapp/assets/mo/js/modules/job-board.js:78)。每个岗位卡片展示：

- 课程编号；
- MO 名称；
- 课程名称；
- 若干标签；
- 课程日期 + 时间；
- 招聘状态；
- “点击查看详情”的提示。

这种卡片式而非表格式的岗位展示，更适合中保真原型，原因是：

- 便于突出课程主题；
- 标签和时间等信息更容易视觉聚合；
- 后续扩展详情弹窗比较自然；
- 与系统当前卡片风格统一。

### 6.6 搜索与分页设计

搜索框 [`jobSearchInput`](src/main/webapp/pages/mo/routes/mo-route-jobs.jspf:33) 支持搜索：

- 课程编号；
- 课程名称；
- 标签。

匹配逻辑见 [`renderBoard()`](src/main/webapp/assets/mo/js/modules/job-board.js:79)，通过拼接上述文本后统一小写匹配实现。

分页规则位于 [`job-board.js`](src/main/webapp/assets/mo/js/modules/job-board.js:89)：

- 每页 6 个岗位；
- 自动根据总数生成页码按钮；
- 点击页码切换当前展示项。

对于可能存在十几个甚至几十个课程招聘岗位的场景，这一设计已经足够支持原型演示。

### 6.7 课程详情模态框设计

点击课程卡片后会调用 [`renderDetail()`](src/main/webapp/assets/mo/js/modules/job-board.js:33)，并打开 `course-detail` 模态框，该模态框定义在 [`mo-modals.jspf`](src/main/webapp/pages/mo/partials/mo-modals.jspf:2)。

模态框展示内容包括：

- 课程编号；
- 课程名称；
- MO 名称；
- 课程描述；
- 课程日期；
- 课程时间；
- 上课地点；
- 招聘状态；
- 课程标签；
- TA 工作清单；
- 下一步操作提示。

右侧“下一步”区域提供一个关键按钮：[`jumpToApplicantsBtn`](src/main/webapp/pages/mo/partials/mo-modals.jspf:40)，点击后会：

1. 关闭模态框；
2. 跳转到 applicants 路由；
3. 自动把当前课程编号设置为候选人筛选页的当前课程。

对应逻辑位于 [`renderDetail()`](src/main/webapp/assets/mo/js/modules/job-board.js:68)。

这是一个非常好的原型设计点，因为它把“岗位管理”和“候选人筛选”两个流程自然衔接起来，避免用户重复选择课程。

### 6.8 数据文件设计说明

课程岗位数据存储在 [`pending-recruitment-courses.json`](mountDataTAMObupter/mo/pending-recruitment-courses.json)。从样例数据可看出，每条记录至少包含：

- 课程基础信息；
- 招聘状态；
- 工作量；
- 标签；
- 检查清单；
- 建议语。

这说明 MO 端当前原型数据模型偏重“招聘运营信息”，而不仅是基础课表信息。这样的模型更有利于后续进行候选人匹配展示。

---

## 7. 页面三：应聘筛选原型设计

### 7.1 页面目标

应聘筛选页对应 [`mo-route-applicants.jspf`](src/main/webapp/pages/mo/routes/mo-route-applicants.jspf)。它承担 MO 模块最关键的决策任务：查看某课程下可供考虑的候选人，并做出录用/拒绝决定。

### 7.2 页面结构

该页面由三部分组成：

1. 页面标题与说明区；
2. 课程选择工具栏；
3. 候选人列表与空状态区。

页面说明文字明确指出：支持查看 TA 资料，并将录用/拒绝结果回写到 TA 申请状态数据。这与实际后端实现完全一致。

### 7.3 课程选择工具栏设计

工具栏定义在 [`mo-route-applicants.jspf`](src/main/webapp/pages/mo/routes/mo-route-applicants.jspf:14)。包含：

- “选择课程岗位”标签；
- 下拉框 [`applicantCourseSelect`](src/main/webapp/pages/mo/routes/mo-route-applicants.jspf:16)；
- 刷新候选人按钮；
- 状态提示文本。

课程下拉框的选项由岗位列表自动同步填充，逻辑见 [`app.onJobsUpdated`](src/main/webapp/assets/mo/js/modules/applicants.js:130)。其优点是：

- 无需单独维护课程候选人页的课程列表；
- 新发布岗位后可立即在筛选页中出现；
- 降低页面间数据不一致的风险。

### 7.4 候选人数据聚合逻辑

候选人加载逻辑位于 [`loadApplicants()`](src/main/webapp/assets/mo/js/modules/applicants.js:85)，请求接口为：

- `GET ../../api/mo/applicants?courseCode=...`

后端入口为 [`MoApplicantsServlet.doGet()`](src/main/java/com/bupt/tarecruit/mo/controller/MoApplicantsServlet.java:22)，数据聚合核心在 [`getApplicantsForCourse()`](src/main/java/com/bupt/tarecruit/mo/dao/MoRecruitmentDao.java:105)。

该方法会同时读取三份数据：

- TA 账号数据 [`tas.json`](mountDataTAMObupter/ta/tas.json)
- TA 档案数据 [`profiles.json`](mountDataTAMObupter/ta/profiles.json)
- TA 申请状态数据 [`application-status.json`](mountDataTAMObupter/ta/application-status.json)

聚合后每个候选人行数据包含：

| 字段 | 来源 | 说明 |
|---|---|---|
| `taId` | TA 账号 | 候选人唯一编号 |
| `name` | TA 账号 | 真实姓名或展示名 |
| `username` | TA 账号 | 用户名 |
| `email` | 档案优先，账号兜底 | 联系方式 |
| `intent` | 档案 | 申请意向 |
| `skills` | 档案 | 技能文本 |
| `bio` | 档案 | 个人介绍 |
| `avatar` | 档案 | 头像路径 |
| `applicationId` | 申请状态 | 当前课程对应申请编号 |
| `status` | 申请状态 | 如审核中、已录用、未录用、可邀请 |
| `updatedAt` | 申请状态 | 最新更新时间 |
| `comment` | 申请状态 | 摘要或 MO 备注 |

### 7.5 “可邀请”状态的设计含义

一个很重要的设计细节在 [`getApplicantsForCourse()`](src/main/java/com/bupt/tarecruit/mo/dao/MoRecruitmentDao.java:145)。当某 TA 没有该课程对应投递记录时，系统仍然会把 TA 作为候选人列出来，并赋值：

- `status = 可邀请`
- `comment = 暂无该课程投递记录，可由 MO 主动邀请。`

这意味着当前原型中的候选池并不只是“已申请者列表”，而是“课程相关候选池”。这是一个非常有价值的设计：

- MO 不必被动等待投递；
- 可以从现有 TA 池中主动筛选人选；
- 更贴近真实招聘中的“主动邀约”场景。

### 7.6 候选人卡片设计

候选人卡片渲染逻辑位于 [`renderApplicants()`](src/main/webapp/assets/mo/js/modules/applicants.js:36)。每张卡片展示：

- 姓名 / TA 编号；
- 当前状态；
- 邮箱；
- 意向；
- 技能；
- 备注；
- 录用按钮；
- 拒绝按钮。

状态显示规则在 [`renderApplicants()`](src/main/webapp/assets/mo/js/modules/applicants.js:45)：

- “已录用”显示成功态样式；
- “可邀请”使用默认样式；
- 其他状态倾向警示态。

这说明当前设计希望 MO 能一眼区分：

- 已结束且结果正向的候选人；
- 尚未投递但可考虑邀请的候选人；
- 仍在流程中或风险较高的候选人。

### 7.7 空状态设计

当没有课程或没有候选人时，页面显示空状态区域 [`applicantEmptyState`](src/main/webapp/pages/mo/routes/mo-route-applicants.jspf:22)。提示文案为：

- 暂无候选人；
- 请先选择岗位或发布新岗位，系统将聚合 TA 候选池。

这是一个比较到位的原型设计，因为它不仅说明“当前没有数据”，还提示用户下一步操作。

### 7.8 录用 / 拒绝决策交互

操作按钮逻辑在 [`decide()`](src/main/webapp/assets/mo/js/modules/applicants.js:105)。交互流程如下：

1. 用户点击“录用”或“拒绝”；
2. 系统弹出 `prompt` 让用户填写备注；
3. 前端调用 `POST ../../api/mo/applications/select`；
4. 请求体包含：
   - `courseCode`
   - `taId`
   - `decision`
   - `comment`
5. 成功后刷新候选人列表；
6. 同时刷新总览统计。

虽然这里使用的是浏览器 `prompt`，交互形式比较基础，但对于当前中保真原型是可接受的，因为它已经完整体现了“决策时附带说明”的业务要求。

### 7.9 决策回写逻辑

后端入口为 [`MoApplicationDecisionServlet.doPost()`](src/main/java/com/bupt/tarecruit/mo/controller/MoApplicationDecisionServlet.java:22)，核心写入逻辑为 [`decideApplication()`](src/main/java/com/bupt/tarecruit/mo/dao/MoRecruitmentDao.java:167)。

其规则如下：

#### 7.9.1 参数校验

- `courseCode` 不能为空；
- `taId` 不能为空；
- `decision` 只能是 `selected` 或 `rejected`。

#### 7.9.2 状态映射

- `selected` → `已录用`
- `rejected` → `未录用`

见 [`decideApplication()`](src/main/java/com/bupt/tarecruit/mo/dao/MoRecruitmentDao.java:185)。

#### 7.9.3 若原本没有申请记录

系统会自动创建一条新的申请状态记录，字段包括：

- `applicationId`
- `taId`
- `courseName`
- `jobSlug`
- `tags`
- `timeline`
- `details`
- `notifications`

相关逻辑位于 [`decideApplication()`](src/main/java/com/bupt/tarecruit/mo/dao/MoRecruitmentDao.java:190)。

这正好与前文提到的“可邀请”设计形成闭环：即使 TA 没投递，MO 也能直接决策，系统会补建一条招聘流程记录。

#### 7.9.4 回写字段

被更新的关键字段包括：

- `status`
- `statusTone`
- `summary`
- `moComment`
- `nextAction`
- `nextStep`
- `updatedAt`
- `category`
- `matchLevel`

这说明 MO 的决策结果不是简单改一个状态，而是会影响 TA 端后续能看到的完整申请状态说明。

#### 7.9.5 数据落点

最终数据写回 [`application-status.json`](mountDataTAMObupter/ta/application-status.json)。

因此，从原型角度看，这一页的最大价值是：**实现了 MO 端操作对 TA 端状态反馈的闭环联动**。

---

## 8. 模态框与辅助交互设计

### 8.1 模态框体系

MO 所有弹窗统一由 [`modal.js`](src/main/webapp/assets/mo/js/modules/modal.js) 管理，弹层容器为 [`taModalOverlay`](src/main/webapp/pages/mo/partials/mo-modals.jspf:1)。当前主要有三个模态框：

1. `course-detail`：课程详情；
2. `summary`：模块概览说明；
3. `settings`：MO 设置。

统一弹层的好处是：

- 保证交互一致；
- 降低页面复杂度；
- 更符合后台系统的组件化设计。

### 8.2 设置模态框

设置模态框定义在 [`mo-modals.jspf`](src/main/webapp/pages/mo/partials/mo-modals.jspf:62)。包含：

- 当前账号；
- 角色；
- 当前主题。

对应填充值由 [`settings.js`](src/main/webapp/assets/mo/js/modules/settings.js:54) 到 [`settings.js`](src/main/webapp/assets/mo/js/modules/settings.js:60) 提供。

当前设置项较少，说明该原型遵循“最小必要设置”原则：不做复杂账户配置，只保留演示工作流所必需的身份与主题状态。

### 8.3 键盘与关闭交互

在 [`modal.js`](src/main/webapp/assets/mo/js/modules/modal.js:50) 中，按 `Escape` 可关闭所有弹窗；点击遮罩层也可关闭。这保证了原型的基础可用性。

---

## 9. 路由与页面切换设计

路由切换由 [`activateRoute()`](src/main/webapp/assets/mo/js/modules/route-nav.js:11) 实现。当前是单页式工作台切换方式，而不是多页面跳转。其设计优势包括：

- 切换快；
- 状态保持容易；
- 适合原型演示；
- 岗位列表与候选人页之间联动更顺畅。

点击侧边导航项后，系统会：

1. 更新导航激活态；
2. 更新路由面板显示；
3. 将目标区域滚动到可见位置。

特别是从课程详情直接跳到候选人页的流程，增强了任务连续性。

---

## 10. 数据模型与仓库代码一致性说明

### 10.1 MO 岗位数据模型

MO 岗位数据文件为 [`pending-recruitment-courses.json`](mountDataTAMObupter/mo/pending-recruitment-courses.json)。从现有数据样本看，字段覆盖了：

- 课程基础信息；
- 招聘状态；
- 工作量说明；
- 课程描述；
- 标签与清单；
- 推荐建议。

这与岗位卡片、详情弹窗、搜索功能完全对应。

### 10.2 TA 资料模型

TA 资料主要来自：

- 账号数据 [`tas.json`](mountDataTAMObupter/ta/tas.json)
- 个人档案 [`profiles.json`](mountDataTAMObupter/ta/profiles.json)

例如在 [`profiles.json`](mountDataTAMObupter/ta/profiles.json) 中，存在：

- `avatar`
- `realName`
- `applicationIntent`
- `contactEmail`
- `bio`
- `skills`

虽然当前应聘筛选页尚未展示头像和 bio 的详细信息，但数据层已经具备后续扩展基础。

### 10.3 TA 申请状态模型

TA 申请状态数据文件为 [`application-status.json`](mountDataTAMObupter/ta/application-status.json)。它不只是一个状态字段，而是完整的进度模型，包含：

- `status`
- `summary`
- `nextAction`
- `timeline`
- `details`
- `notifications`
- `moComment`

这说明系统设计本身已经具备“MO 做决策，TA 看反馈”的双端联动潜力。MO 模块当前只是先实现了最关键的写入入口。

---

## 11. 结合图片原型对 MO 原型的详细定位说明

从提供的图片看，ADMIN 原型已经展示了典型后台系统的几种页面形态：

- 看板型页面；
- 列表型页面；
- 卡片型页面；
- 占位型页面（功能开发中）。

MO 原型应该在这一体系下采用以下对应方式：

| 图片体现的模式 | MO 中的对应设计 |
|---|---|
| 数据看板 | 招聘总览页 |
| 用户管理列表 | 候选人筛选页 |
| 公告卡片列表 | 岗位卡片列表 |
| 设置/权限占位页 | MO 设置模态框与轻量设置 |

这意味着 MO 原型不是完全独立设计，而是应嵌入整个系统后台的统一交互语言中。换言之，MO 模块的视觉与结构应该让用户一进入就感觉“这是同一个系统里的另一个角色工作台”。

基于这套逻辑，MO 原型的设计关键词可以总结为：

- 统一后台风格；
- 轻量招聘闭环；
- 卡片化展示；
- 低学习成本；
- 操作结果可回写。

---

## 12. MO 原型中的角色任务说明

### 12.1 MO 的核心职责

从当前代码和原型可以明确，MO 的职责不是管理用户权限，也不是维护系统公告，而是：

- 发布课程招聘岗位；
- 审阅候选人；
- 做出 TA 招聘决策。

因此 MO 页面没有 ADMIN 那样复杂的菜单，这是符合角色职责边界的。

### 12.2 与 TA 端的关系

MO 模块与 TA 模块存在明显的数据耦合：

- MO 读取 TA 档案数据做候选池聚合；
- MO 写回 TA 的申请状态数据；
- TA 端后续可基于这些状态显示自己的申请进度。

这种关系使得 MO 原型具有跨角色协同意义，而不是单端孤立原型。

### 12.3 与 ADMIN 端的关系

结合图片原型，ADMIN 更偏全局系统管理，而 MO 更偏业务执行。两者形成：

- ADMIN：管理平台秩序；
- MO：执行招聘业务；
- TA：参与招聘流程。

MO 恰好处于系统业务链条的中间层。

---

## 13. 当前原型的优点与不足

### 13.1 优点

#### 13.1.1 业务闭环完整

当前 MO 原型已经具备完整闭环：

- 发布岗位；
- 查看候选人；
- 做出决策；
- 回写 TA 状态。

#### 13.1.2 数据结构真实

不是纯静态页面，而是直接连接 JSON 数据与 Servlet API。

#### 13.1.3 信息架构清晰

三个页面对应三大任务，没有冗余菜单。

#### 13.1.4 组件复用程度高

复用了 TA 端布局与弹窗机制，减少重复开发。

#### 13.1.5 符合图片中的后台系统风格

深色、卡片化、左侧导航、高亮操作按钮等，都与图片原型一致。

### 13.2 不足

#### 13.2.1 候选人详情展示不够深入

目前候选人卡片只展示基础信息，尚未展开头像、履历、时间可用性等更完整档案。

#### 13.2.2 决策交互较基础

目前备注输入使用 `prompt`，不够优雅，后续可以改为专用确认模态框。

#### 13.2.3 总览统计是局部型统计

目前统计依赖最近一次加载的候选人，后续可改成真正的全局汇总接口。

#### 13.2.4 缺少岗位编辑与关闭招聘功能

当前只支持发布和查看，没有修改、撤回、结束招聘等更完整管理操作。

---

## 14. 后续可扩展的 MO 原型迭代方向

基于当前仓库实现，MO 模块后续可以自然扩展以下能力：

### 14.1 候选人详情侧边栏 / 模态框

展示：

- 头像；
- 个人简介；
- 技能标签；
- 每周可用时长；
- 历史申请记录；
- 简历附件链接。

### 14.2 岗位状态管理

增加：

- 编辑岗位；
- 暂停招聘；
- 结束招聘；
- 标记已满员。

### 14.3 更精细的筛选动作

在“录用/拒绝”之外增加：

- 邀请面试；
- 待补充材料；
- 进入候补；
- 收藏候选人。

### 14.4 全局统计与提醒

例如：

- 即将开课岗位提醒；
- 长时间未处理候选人提醒；
- 热门技能分布；
- 课程维度招聘完成率。

### 14.5 与图片原型风格进一步统一

可增加：

- 与 ADMIN 一致的卡片间距与标题层级；
- 更明确的状态徽章色彩系统；
- 更标准的空状态插画与操作按钮。

---

## 15. 结论

综合图片原型与仓库现有实现，MO 部分原型应被定义为一个**面向 Module Organizer 的中保真招聘工作台原型**。它以统一后台风格为视觉基础，以“总览—发布—筛选”三段式结构为信息主线，围绕 TA 招聘业务形成了较完整的交互闭环。

从实现角度看，MO 模块已经具备以下核心特征：

- 页面结构清晰，入口集中在 [`mo-home.jsp`](src/main/webapp/pages/mo/mo-home.jsp)；
- 路由清晰，主功能分布在三个 route 文件中；
- 前端逻辑模块化，分别由 [`job-board.js`](src/main/webapp/assets/mo/js/modules/job-board.js)、[`applicants.js`](src/main/webapp/assets/mo/js/modules/applicants.js)、[`dashboard.js`](src/main/webapp/assets/mo/js/modules/dashboard.js)、[`route-nav.js`](src/main/webapp/assets/mo/js/modules/route-nav.js)、[`modal.js`](src/main/webapp/assets/mo/js/modules/modal.js)、[`settings.js`](src/main/webapp/assets/mo/js/modules/settings.js) 负责；
- 后端接口清晰，由三个 Servlet 提供服务；
- 数据写入真实落在 JSON 文件中，符合课程 handout 要求。

如果将该原型用于阶段汇报或文档展示，可以把 MO 模块的价值概括为：

**MO 能在统一系统中快速发布课程岗位、聚合查看 TA 候选池，并将录用决策及时回写给 TA 申请状态，形成从岗位创建到结果反馈的完整招聘管理链路。**
