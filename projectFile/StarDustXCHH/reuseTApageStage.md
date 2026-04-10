# 阶段 1 页面骨架与设置中心入口

## 1.1 本阶段目标

先让后续 AI 在新项目中搭出三模块 TA 工作台的完整页面骨架，但必须同时带上设置中心相关入口与弹窗骨架。

本阶段必须处理：

- 左侧导航三项
- 顶部栏
- 右上角用户触发器
- welcome card
- TA 工作台四个卡片容器骨架
- onboarding 容器
- profile jobs status 三个 route 容器
- 设置中心相关 modal 骨架
- 头像裁切相关 modal 骨架
- 职位详情与追踪详情 modal 骨架

本阶段先不要求：

- 最终动画细节落地
- 接口联调
- JSON 数据接入

## 1.2 本阶段应产出的目标文件

```text
src/main/webapp/pages/ta/
├─ ta-home.jsp
├─ partials/
│  ├─ ta-layout-sidebar.jspf
│  ├─ ta-layout-topbar.jspf
│  ├─ ta-welcome-card.jspf
│  ├─ ta-dashboard-cards.jspf
│  ├─ ta-onboarding.jspf
│  └─ ta-modals.jspf
└─ routes/
   ├─ ta-route-profile.jspf
   ├─ ta-route-jobs.jspf
   └─ ta-route-status.jspf
```

## 1.3 给后续 AI 的任务说明模板

```text
现在只做 TA 工作台的页面骨架迁移。
请基于我接下来提供的参考源码，重组出新的页面结构。

要求：
1. 左侧导航只保留 3 项：个人档案、职位大厅、申请状态
2. 保留右上角用户触发器与设置中心入口
3. 保留设置中心相关弹窗骨架，包括头像设置、头像裁切、资料设置、密码修改
4. 保留 TA 工作台首页四个卡片容器骨架，后续我还要移植视觉效果和动效
5. 保留申请状态页与投递追踪详情的时间线骨架
6. 删除 当前在职、AI 赋能、聊天主模块
7. 页面结构先完整，复杂交互后续阶段再补
```

## 1.4 本阶段不要给 AI 的文件

- [`ta-route-cv.jsp`](src/main/webapp/jsp/ta/routes/ta-route-cv.jsp)
- [`ta-route-ai.jsp`](src/main/webapp/jsp/ta/routes/ta-route-ai.jsp)
- [`06-current-work.jspf`](src/main/webapp/jsp/ta/scripts/modules/06-current-work.jspf)
- [`ta-home-scripts-ai.jspf`](src/main/webapp/jsp/ta/scripts/ta-home-scripts-ai.jspf)
- [`ChatServlet.java`](src/main/java/com/bupt/tarecruitment/controller/ChatServlet.java)

## 1.5 本阶段源码粘贴区

### 阶段 1 文件 01 - [`ta-home.jsp`](src/main/webapp/jsp/ta/ta-home.jsp)

用途：页面总入口

```jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>TA Recruitment Suite</title>
    <%@ include file="ta-home-style.jspf" %>
</head>
<body>

<%@ include file="ta-home-body.jspf" %>




<%@ include file="ta-home-scripts.jspf" %>




</body>
</html>
```

### 阶段 1 文件 02 - [`ta-home-body.jspf`](src/main/webapp/jsp/ta/ta-home-body.jspf)

用途：页面骨架来源，含 sidebar topbar welcome routes onboarding 以及右上角用户入口

```jsp
<div class="app" id="appRoot">
    <aside class="sidebar" id="sidebar">
        <div class="brand">
            <div class="logo">TA</div>
            <div>
                <div>Teaching Assistant</div>
                <small style="color: var(--muted)">Recruitment Suite</small>
            </div>
        </div>
        <nav class="nav">
            <div class="nav-item active" data-route="profile">
                <span>👤</span> <span>个人档案</span>
            </div>
            <div class="nav-item" data-route="cv">
                <span>📂</span> <span>当前在职</span>
            </div>
            <div class="nav-item" data-route="jobs">
                <span>🛰️</span> <span>职位大厅</span>
            </div>
            <div class="nav-item" data-route="status">
                <span>🧭</span> <span>申请状态</span>
            </div>
            <div class="nav-item" data-route="ai">
                <span>🤖</span> <span>AI 赋能</span>
            </div>
        </nav>
    </aside>
    <main class="main">
        <div class="topbar">
            <div class="greeting">
                <h1 id="welcomeTitle">欢迎回来，<span id="welcomeName">TA</span></h1>
                <p>在这里管理你的档案、申请与 AI 增强分析。</p>
            </div>
            <div style="display:flex; align-items:center; gap:12px;">
                <div class="user-trigger" id="userTrigger" role="button" tabindex="0" aria-haspopup="dialog" aria-expanded="false">
                    <div class="user-avatar" aria-hidden="true">S</div>
                    <span class="user-name" id="userName">Seele</span>
                    <span class="user-caret" aria-hidden="true">▾</span>
                </div>
                <button class="logout-btn" id="logoutBtn" type="button">退出登录</button>
                <button class="theme-toggle" id="themeToggle" aria-label="toggle theme">🌙</button>
            </div>
        </div>

        <div class="welcome-card actionable" id="welcomeCard" role="button" tabindex="0" aria-label="首次登录提示，点击打开个人信息设置">
            <div class="badge">新</div>
            <div>
                <h3>首次登录提示</h3>
                <p>欢迎加入 TA 招聘系统！完成个人档案与简历上传后，AI 会为你生成专属分析与推荐。</p>
                <a class="hint hint-link" id="welcomeProfileHint" role="button" tabindex="0">建议从右上角的个人信息开始</a>
            </div>
        </div>

        <section class="routes">
            <%@ include file="routes/ta-route-profile.jsp" %>
            <%@ include file="routes/ta-route-cv.jsp" %>
            <%@ include file="routes/ta-route-jobs.jsp" %>
            <%@ include file="routes/ta-route-status.jsp" %>
            <%@ include file="routes/ta-route-ai.jsp" %>
        </section>
    </main>
</div>

<div class="onboarding" id="onboarding">
    <div class="guide-highlight" id="guideHighlight"></div>
    <div class="guide-arrow" id="guideArrow"></div>
    <div class="guide-card" id="guideCard">
        <div class="guide-progress" id="guideProgress">引导 1 / 4</div>
        <h3 id="guideTitle">欢迎加入 TA 星轨之旅</h3>
        <p class="muted" id="guideDesc">我们将带你快速了解核心入口。</p>
        <div class="guide-actions">
            <button class="guide-btn ghost" id="guideBack">上一步</button>
            <div style="display:flex; gap:10px;">
                <button class="guide-btn ghost" id="skipBtn">跳过指引</button>
                <button class="guide-btn primary" id="guideNext">下一步</button>
            </div>
        </div>
    </div>
</div>
```

### 阶段 1 文件 03 - [`ta-route-profile.jsp`](src/main/webapp/jsp/ta/routes/ta-route-profile.jsp)

用途：个人档案 route 主容器

```jsp
<div class="route active" id="route-profile" data-route="profile">
    <div class="overview">
        <div class="card">
            <%@ include file="partials/ta-route-profile-summary.jspf" %>
            <div class="hero-card" id="heroCard" aria-live="polite">
                <div class="hero-panel">
                    <div class="hero-content">
                        <span class="hero-badge" id="heroBadge">⏰ 紧急待办</span>
                        <h3 class="hero-title" id="heroTitle">Seele，明天下午有一场微处理器课程的 TA 面试。</h3>
                        <p class="hero-desc" id="heroDesc">教授留言提醒你需要准备一段关于 C 语言和底层通信逻辑的试讲。点击下方按钮查看面试指南与注意事项。</p>
                        <div class="hero-actions">
                            <button class="hero-btn primary" type="button" id="heroPrimary">查看面试详情</button>
                            <button class="hero-btn ghost" type="button" id="heroSecondary">已准备妥当</button>
                        </div>
                    </div>
                    <div class="hero-visual" aria-hidden="true">
                        <div class="hero-orb"></div>
                        <div class="hero-lines"></div>
                        <span class="hero-visual-text">Focus · Next Best Action</span>
                    </div>
                </div>
            </div>
    </div>
</div>
</div>

<%@ include file="partials/modals/ta-profile-modals.jspf" %>

```

### 阶段 1 文件 04 - [`ta-route-profile-summary.jspf`](src/main/webapp/jsp/ta/routes/partials/ta-route-profile-summary.jspf)

用途：个人档案摘要卡 头像区 设置入口相关结构

```jsp
<div class="overview-top">
    <div>
        <h1>我的 TA 工作台</h1>
        <p class="muted">实时同步档案、投递与 AI 赋能进度。</p>
    </div>
    <div class="overview-badges">
        <span class="pill">身份：Teaching Assistant</span>
        <span class="pill">学期：2025 Spring</span>
        <span class="pill">状态：申请活跃</span>
    </div>
</div>
<div class="stat-grid insight-grid" style="margin-top:16px;">
    <div class="insight-card track-card" data-modal-target="track">
        <div class="insight-card-header">
            <span class="insight-icon plane-icon" aria-hidden="true"></span>
            <div>
                <p class="insight-label">投递追踪 (Application Status)</p>
                <p class="insight-subtitle">我的申请到哪一步了？</p>
            </div>
        </div>
        <div class="insight-core-number">3</div>
        <p class="insight-meta muted">2 待处理 | 1 面试中</p>
    </div>
    <div class="insight-card radar-card" data-modal-target="recommend">
        <div class="insight-card-header">
            <span class="insight-icon radar-icon" aria-hidden="true"></span>
            <div>
                <p class="insight-label">岗位雷达 (New Matches)</p>
                <p class="insight-subtitle">今日 AI 为你筛出的新机会</p>
            </div>
        </div>
        <div class="insight-core-number accent-positive">+5</div>
        <p class="insight-meta muted">匹配技能：Java / Python / 数据结构</p>
    </div>
    <div class="insight-card profile-strength-card" data-modal-target="checklist">
        <div class="insight-card-header">
            <span class="insight-icon badge-icon" aria-hidden="true"></span>
            <div>
                <p class="insight-label">简历竞争力 (Profile Strength)</p>
                <p class="insight-subtitle">硬件条件是否具备优势？</p>
            </div>
        </div>
        <div class="profile-strength-content">
            <div class="donut-chart" style="--donut-degree:302deg;" aria-hidden="true">
                <span>84%</span>
            </div>
            <div class="profile-strength-text">
                <p class="insight-meta">评级：极佳 (S)</p>
                <p class="insight-subtext">已击败 90% 候选人</p>
            </div>
        </div>
    </div>
    <div class="insight-card upcoming-card" data-modal-target="planner">
        <div class="insight-card-header">
            <span class="insight-icon calendar-icon" aria-hidden="true"></span>
            <div>
                <p class="insight-label">待办日程 (Upcoming Action)</p>
                <p class="insight-subtitle">近期有哪些紧急事项？</p>
            </div>
        </div>
        <div class="insight-core-number accent-warn">2</div>
        <p class="insight-meta muted">明日：1 场助教面试 & 1 门课程</p>
    </div>
</div>
```

### 阶段 1 文件 05 - [`ta-profile-modals.jspf`](src/main/webapp/jsp/ta/routes/partials/modals/ta-profile-modals.jspf)

用途：统一 modal 骨架来源，必须保留设置中心、头像裁切、密码修改、职位详情、追踪详情相关结构

```jsp
<div class="ta-modal-overlay" id="taModalOverlay" aria-hidden="true">
    <div class="ta-modal-panel" data-modal="track" role="dialog" aria-labelledby="modalTrackTitle">
        <button class="modal-close" type="button" aria-label="关闭" data-modal-close>&times;</button>
        <div class="modal-head">
            <h3 id="modalTrackTitle">投递追踪详情</h3>
            <p class="muted">查看每个岗位的里程碑，快速定位卡点并安排下一步动作。</p>
        </div>
        <div class="modal-body">
            <div class="timeline">
                <div class="timeline-item open">
                    <span class="timeline-node success"></span>
                    <div class="timeline-title">EBU6304 · 数据结构实验班</div>
                    <p class="muted">当前状态：面试已完成（02-16）</p>
                    <div class="accordion-content">
                        <p>阶段：已投递 → AI 预筛 → 面试 → 待 Offer</p>
                    </div>
                </div>
                <div class="timeline-item">
                    <span class="timeline-node warn"></span>
                    <div class="timeline-title">EBU6402 · 信号分析助教</div>
                    <p class="muted">当前状态：等待笔试材料</p>
                    <div class="accordion-content">
                        <p>阶段：已投递 → 需补交成绩单 → 待笔试 → 未安排面试</p>
                    </div>
                </div>
                <div class="timeline-item">
                    <span class="timeline-node"></span>
                    <div class="timeline-title">EBU6310 · 智能系统 TA</div>
                    <p class="muted">当前状态：AI 预筛中</p>
                    <div class="accordion-content">
                        <p>阶段：已投递 → 等待 AI 匹配 → 待人工审核 → 未开始</p>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="ta-modal-panel wide" data-modal="recommend" role="dialog" aria-labelledby="modalRecommendTitle">
        <button class="modal-close" type="button" aria-label="关闭" data-modal-close>&times;</button>
        <div class="modal-head">
            <h3 id="modalRecommendTitle">推荐岗位列表</h3>
            <p class="muted">根据你的技能画像与空闲时间实时筛出的岗位，支持一键申请。</p>
        </div>
        <div class="modal-body">
            <div class="modal-list">
                <div class="modal-list-item">
                    <div>
                        <p class="list-title">EBU6402 信号处理 / Lab TA</p>
                        <span class="match-tag high">匹配度 92%</span>
                    </div>
                    <button class="pill-btn" type="button">一键申请</button>
                </div>
                <div class="modal-list-item">
                    <div>
                        <p class="list-title">EBU6310 智能系统 / 课程助教</p>
                        <span class="match-tag">匹配度 88%</span>
                    </div>
                    <button class="pill-btn ghost" type="button">加入候选</button>
                </div>
                <div class="modal-list-item">
                    <div>
                        <p class="list-title">EBU6225 机器学习导论 / 讨论班</p>
                        <span class="match-tag mid">匹配度 81%</span>
                    </div>
                    <button class="pill-btn" type="button">预约沟通</button>
                </div>
            </div>
        </div>
    </div>

    <div class="ta-modal-panel" data-modal="checklist" role="dialog" aria-labelledby="modalChecklistTitle">
        <button class="modal-close" type="button" aria-label="关闭" data-modal-close>&times;</button>
        <div class="modal-head">
            <h3 id="modalChecklistTitle">简历完善检查清单</h3>
            <p class="muted">根据最新 AI 扫描结果，需要补齐如下信息项以提升通过率。</p>
        </div>
        <div class="modal-body">
            <ul class="resume-checklist">
                <li>
                    <div>
                        <strong>缺少项目经历</strong>
                        <span class="impact">-10%</span>
                        <p class="muted">建议补充近期课程项目或科研成果。</p>
                    </div>
                    <a class="jump-link" href="#bio">跳转到自我介绍</a>
                </li>
                <li>
                    <div>
                        <strong>教育背景未细化</strong>
                        <span class="impact">-5%</span>
                        <p class="muted">建议标注 GPA / 主修核心课程。</p>
                    </div>
                    <a class="jump-link" href="#fullName">前往基本信息</a>
                </li>
                <li>
                    <div>
                        <strong>技能标签缺少 TA 技能</strong>
                        <span class="impact">-3%</span>
                        <p class="muted">可补充“课堂管理”、“作业批改”等关键词。</p>
                    </div>
                    <a class="jump-link" href="#skillsInput">补充技能标签</a>
                </li>
            </ul>
        </div>
    </div>

    <div class="ta-modal-panel drawer" data-modal="planner" role="dialog" aria-labelledby="modalPlannerTitle">
        <button class="modal-close" type="button" aria-label="关闭" data-modal-close>&times;</button>
        <div class="modal-head">
            <h3 id="modalPlannerTitle">日程与任务规划</h3>
            <p class="muted">集中管理课程、TA 任务与空闲时间，实时评估排班负荷。</p>
        </div>
        <div class="modal-body">
            <div class="planner-layout">
                <section class="calendar-pane">
                    <div class="calendar-header">
                        <strong>Week 08 · 2025</strong>
                        <span class="muted">08/18 - 08/24</span>
                    </div>
                    <div class="calendar-grid">
                        <div class="calendar-cell">
                            <span class="day-label">Mon</span>
                            <div class="calendar-slot busy">09:00 - 11:00 · 数据结构助教</div>
                            <div class="calendar-slot">14:00 - 15:00 · 自习</div>
                        </div>
                        <div class="calendar-cell">
                            <span class="day-label">Tue</span>
                            <div class="calendar-slot">10:00 - 11:30 · 面试模拟</div>
                        </div>
                        <div class="calendar-cell">
                            <span class="day-label">Wed</span>
                            <div class="calendar-slot busy">13:30 - 16:30 · 信号处理实验班</div>
                        </div>
                        <div class="calendar-cell">
                            <span class="day-label">Thu</span>
                            <div class="calendar-slot">09:00 - 10:00 · AI 扫描报告</div>
                            <div class="calendar-slot busy">19:00 - 21:00 · 夜间答疑</div>
                        </div>
                        <div class="calendar-cell">
                            <span class="day-label">Fri</span>
                            <div class="calendar-slot">15:00 - 16:00 · Offer 跟进</div>
                        </div>
                    </div>
                </section>
                <section class="planner-form">
                    <h4>添加日程</h4>
                    <label class="planner-field">
                        <span>事件名称</span>
                        <input type="text" placeholder="如：EBU6304 课堂演练">
                    </label>
                    <label class="planner-field">
                        <span>事件类型</span>
                        <select>
                            <option value="course">课程</option>
                            <option value="ta">TA 任务</option>
                            <option value="interview">面试/沟通</option>
                        </select>
                    </label>
                    <div class="planner-field grid">
                        <label>
                            <span>开始时间</span>
                            <input type="time" value="09:00">
                        </label>
                        <label>
                            <span>结束时间</span>
                            <input type="time" value="11:00">
                        </label>
                    </div>
                    <label class="planner-field">
                        <span>日期</span>
                        <input type="date">
                    </label>
                    <label class="planner-field">
                        <span>备注</span>
                        <textarea rows="3" placeholder="可记录授课内容、准备物料等"></textarea>
                    </label>
                    <button class="pill-btn primary" type="button">保存日程</button>
                </section>
            </div>
        </div>
    </div>

    <div class="ta-modal-panel wide job-detail-modal" data-modal="course-detail" role="dialog" aria-labelledby="modalCourseDetailTitle">
        <button class="modal-close" type="button" aria-label="关闭" data-modal-close>&times;</button>
        <div class="modal-head">
            <h3 id="modalCourseDetailTitle">课程招聘详情</h3>
            <p class="muted">查看课程介绍、招聘重点与申请说明，确认后即可一键申请。</p>
        </div>
        <div class="modal-body">
            <div class="job-detail-shell">
                <section class="job-detail-main">
                    <div class="job-detail-hero">
                        <div class="job-detail-title-row">
                            <div>
                                <div class="job-code" id="jobDetailCode">MO-SE-628</div>
                                <h4 id="jobDetailName">软件工程基础训练营</h4>
                            </div>
                            <span class="course-mo-badge" id="jobDetailMo">开课 MO</span>
                        </div>
                        <p class="job-detail-summary" id="jobDetailDescription">课程结合企业用人需求设计，围绕软件工程基础训练营组织学习任务，适合作为招聘前置储备课程进行训练。</p>
                        <div class="job-detail-meta-grid">
                            <div class="job-detail-meta">
                                <span>课程日期</span>
                                <strong id="jobDetailDate">2026-03-20</strong>
                            </div>
                            <div class="job-detail-meta">
                                <span>课程时间</span>
                                <strong id="jobDetailTime">17:00-19:00</strong>
                            </div>
                            <div class="job-detail-meta">
                                <span>上课地点</span>
                                <strong id="jobDetailLocation">教一楼 A201</strong>
                            </div>
                            <div class="job-detail-meta">
                                <span>学生人数</span>
                                <strong id="jobDetailStudentCount">36 人</strong>
                            </div>
                            <div class="job-detail-meta">
                                <span>招聘状态</span>
                                <strong id="jobDetailStatus">等待招聘 TA</strong>
                            </div>
                            <div class="job-detail-meta">
                                <span>建议到岗</span>
                                <strong id="jobDetailWorkload">课前准备 + 课堂支持 + 课后答疑</strong>
                            </div>
                        </div>
                    </div>
                    <div class="job-detail-section">
                        <h5>课程标签</h5>
                        <div class="job-detail-tags" id="jobDetailTags">
                            <span class="skill-tag">软件工程</span>
                            <span class="skill-tag">基础训练营</span>
                            <span class="skill-tag">3月课程</span>
                        </div>
                    </div>
                    <div class="job-detail-section">
                        <h5>TA 工作内容</h5>
                        <ul class="job-detail-checklist" id="jobDetailChecklist">
                            <li>协助 MO 完成课程准备与签到组织</li>
                            <li>维护课堂互动，支持作业与实验答疑</li>
                            <li>课后整理反馈，配合完成学习效果跟踪</li>
                        </ul>
                    </div>
                </section>
                <aside class="job-detail-side">
                    <div class="job-detail-side-card">
                        <h5>申请建议</h5>
                        <p id="jobDetailSuggestion">若你近期在软件工程、课程助教或项目实践方向有相关经历，建议优先申请该课程。</p>
                        <button class="apply-btn" type="button" id="jobDetailApplyBtn" data-apply-label-default="Apply for this course" data-apply-label-done="✓ Applied to this course">Apply for this course</button>
                    </div>
                    <div class="job-detail-side-card">
                        <h5>投递提醒</h5>
                        <p>提交申请后，系统会自动记录到投递追踪，并根据课程标签更新你的推荐匹配权重。</p>
                    </div>
                </aside>
            </div>
        </div>
    </div>

    <div class="ta-modal-panel ta-settings-modal" data-modal="settings" role="dialog" aria-labelledby="modalSettingsTitle">
        <button class="modal-close" type="button" aria-label="关闭" data-modal-close>&times;</button>
        <div class="modal-head">
            <h3 id="modalSettingsTitle">个人设置中心</h3>
            <p class="muted">快速更新档案、账号安全与偏好设置，资料修改需点击保存后才会写入。</p>
        </div>
        <div class="modal-body settings-body">
            <aside class="settings-tabs" role="tablist" aria-label="设置菜单">
                <button class="settings-tab active" type="button" data-settings-tab="profile" role="tab" aria-selected="true">
                    <span>👤</span> 基础资料
                </button>
                <button class="settings-tab" type="button" data-settings-tab="security" role="tab" aria-selected="false">
                    <span>🔒</span> 账号与安全
                </button>
                <button class="settings-tab" type="button" data-settings-tab="preferences" role="tab" aria-selected="false">
                    <span>⚙️</span> 系统偏好
                </button>
            </aside>
            <section class="settings-content">
                <div class="settings-panel active" data-settings-panel="profile" role="tabpanel">
                    <div class="settings-section">
                        <div class="settings-profile-toolbar">
                            <div class="settings-status-group">
                                <span class="settings-status-badge" id="profileSyncStatus">已加载</span>
                                <small class="muted" id="profileLastUpdated">最近更新：--</small>
                            </div>
                            <div class="settings-action-group">
                                <button class="pill-btn" type="button" id="saveProfileBtn">保存</button>
                            </div>
                        </div>
                        <div class="settings-avatar" role="button" tabindex="0" id="profileAvatarBox" aria-label="更换头像">
                            <span class="settings-avatar-ring"></span>
                            <img id="profileAvatarPreview" class="settings-avatar-image" alt="TA 头像预览" hidden>
                        </div>
                        <label class="settings-field full settings-avatar-upload-field" for="avatarFile">
                            <span>头像上传</span>
                            <input id="avatarFile" type="file" accept="image/png,image/jpeg,image/webp,image/gif" hidden>
                            <small class="muted" id="avatarUploadHint">支持 PNG / JPG / WEBP / GIF，大小限制 10MB。点击头像即可更换。</small>
                        </label>
                        <div class="settings-grid">
                            <label class="settings-field">
                                <span>真实姓名</span>
                                <input id="fullName" type="text" placeholder="请输入真实姓名">
                            </label>
                            <label class="settings-field">
                                <span>申请意向</span>
                                <input id="applicationIntent" type="text" placeholder="如：数据库 / 操作系统 / 计算机网络">
                            </label>
                            <label class="settings-field">
                                <span>学号</span>
                                <input id="studentId" type="text" placeholder="系统自动分配" readonly disabled>
                            </label>
                            <label class="settings-field">
                                <span>联系邮箱</span>
                                <input id="contactEmail" type="email" placeholder="name@bupt.edu.cn">
                            </label>
                        </div>
                        <label class="settings-field full">
                            <span>自我介绍</span>
                            <textarea id="bio" rows="4" placeholder="简单介绍你的研究方向、授课经验或教学风格"></textarea>
                        </label>
                        <div class="settings-field full">
                            <span>技能标签</span>
                            <div class="skills-input" id="skillsInput" data-readonly="false">
                                <input id="skillEntry" type="text" placeholder="输入技能后回车">
                            </div>
                            <small class="muted">回车可新增标签，点击标签右侧 × 可删除。</small>
                        </div>
                    </div>
                </div>
                <div class="settings-panel" data-settings-panel="security" role="tabpanel">
                    <div class="settings-section">
                        <div class="settings-subtitle">修改密码</div>
                        <div class="settings-grid stack">
                            <label class="settings-field">
                                <span>当前密码</span>
                                <input id="currentPassword" type="password" placeholder="请输入当前密码">
                            </label>
                            <label class="settings-field">
                                <span>新密码</span>
                                <input id="newPassword" type="password" placeholder="至少 6 位">
                            </label>
                            <label class="settings-field">
                                <span>确认新密码</span>
                                <input id="confirmPassword" type="password" placeholder="再次输入新密码">
                            </label>
                        </div>
                        <div class="settings-action-row">
                            <span class="muted" id="passwordSaveStatus">尚未保存</span>
                            <button class="pill-btn" type="button" id="savePasswordBtn">保存</button>
                        </div>
                    </div>
                </div>
                <div class="settings-panel" data-settings-panel="preferences" role="tabpanel">
                    <div class="settings-section">
                        <label class="settings-field full">
                            <span>界面主题</span>
                            <select id="themeSelect">
                                <option value="system">跟随系统</option>
                                <option value="light">浅色模式</option>
                                <option value="dark">深色模式</option>
                            </select>
                        </label>
                    </div>
                </div>
            </section>
        </div>
    </div>

    <div class="ta-modal-panel wide ta-avatar-crop-modal" data-modal="avatar-crop" role="dialog" aria-labelledby="avatarCropTitle" aria-modal="true">
        <button class="modal-close" type="button" aria-label="关闭头像裁切窗口" data-avatar-crop-close>&times;</button>
        <div class="modal-head">
            <h3 id="avatarCropTitle">裁切头像</h3>
            <p class="muted">请先将头像裁切为 1:1 正方形，确认后系统才会继续上传。</p>
        </div>
        <div class="modal-body avatar-crop-body">
            <div class="avatar-crop-layout">
                <div class="avatar-crop-stage-wrap">
                    <div class="avatar-crop-stage" id="avatarCropStage" aria-label="头像裁切区域">
                        <img id="avatarCropImage" class="avatar-crop-image" alt="头像裁切预览" hidden>
                        <div class="avatar-crop-mask" aria-hidden="true"></div>
                        <div class="avatar-crop-frame" aria-hidden="true"></div>
                    </div>
                </div>
                <aside class="avatar-crop-side">
                    <div class="avatar-crop-preview-card">
                        <span class="avatar-crop-side-title">裁切结果预览</span>
                        <div class="avatar-crop-preview-box">
                            <canvas id="avatarCropPreviewCanvas" width="160" height="160" aria-label="裁切结果预览"></canvas>
                        </div>
                    </div>
                    <label class="settings-field full avatar-crop-zoom-field" for="avatarCropZoomRange">
                        <span>缩放</span>
                        <input id="avatarCropZoomRange" type="range" min="1" max="3" step="0.01" value="1">
                    </label>
                    <small class="muted" id="avatarCropHint">拖动图片调整取景，滑动缩放条可放大或缩小。</small>
                    <div class="avatar-crop-actions">
                        <button class="pill-btn ghost" type="button" id="avatarCropCancelBtn">取消</button>
                        <button class="pill-btn" type="button" id="avatarCropConfirmBtn">确认裁切</button>
                    </div>
                </aside>
            </div>
        </div>
    </div>
</div>
```

### 阶段 1 文件 06 - [`ta-route-jobs.jsp`](src/main/webapp/jsp/ta/routes/ta-route-jobs.jsp)

用途：职位大厅 route 主结构与工作台卡片引用结构来源

```jsp
<div class="route" id="route-jobs" data-route="jobs">
    <div class="card jobs-hall-card">
        <div class="overview-top" id="jobsHallHeading">
            <div>
                <h1>职位大厅</h1>
                <p class="muted">浏览等待招聘的课程卡片，点击查看课程介绍并完成申请。</p>
            </div>
            <div class="overview-badges">
                <span class="pill" id="openCoursesCount">开放课程 8</span>
                <button id="refreshJobsBtn" class="refresh-btn pill">
                    <span class="refresh-icon">↻</span>
                    <span class="refresh-text">刷新列表</span>
                </button>
            </div>
        </div>
        <label class="job-search-box" for="jobSearchInput">
            <span class="job-search-icon" aria-hidden="true">⌕</span>
            <input id="jobSearchInput" type="text" placeholder="搜索课程编号 / 名称 / 标签">
        </label>
        <div class="job-board course-board" id="jobBoard">
            <div class="job-card course-job-card" tabindex="0" role="button" aria-label="查看 软件工程基础训练营 详情" data-job-detail-card data-course-code="MO-SE-628">
                <div class="course-card-topline">
                    <span class="job-code">MO-SE-628</span>
                    <span class="course-mo-badge">开课 MO</span>
                </div>
                <h4>软件工程基础训练营</h4>
                <div class="job-tags">
                    <span class="skill-tag">软件工程</span>
                    <span class="skill-tag">基础训练营</span>
                    <span class="skill-tag">3月课程</span>
                </div>
                <div class="course-meta-stack">
                    <div class="course-meta-item">
                        <span class="course-meta-label">课程时间</span>
                        <strong>03-20 · 17:00-19:00</strong>
                    </div>
                    <div class="course-meta-item">
                        <span class="course-meta-label">招聘状态</span>
                        <strong>等待招募 TA</strong>
                    </div>
                </div>
                <div class="course-card-hint">
                    <span>点击查看详情</span>
                    <span aria-hidden="true">→</span>
                </div>
            </div>
            <div class="job-card course-job-card" tabindex="0" role="button" aria-label="查看 编译原理方法论专题 详情" data-job-detail-card data-course-code="MO-PL-403">
                <div class="course-card-topline">
                    <span class="job-code">MO-PL-403</span>
                    <span class="course-mo-badge">开课 MO</span>
                </div>
                <h4>编译原理方法论专题</h4>
                <div class="job-tags">
                    <span class="skill-tag">编译原理</span>
                    <span class="skill-tag">方法论专题</span>
                    <span class="skill-tag">项目实践</span>
                </div>
                <div class="course-meta-stack">
                    <div class="course-meta-item">
                        <span class="course-meta-label">课程时间</span>
                        <strong>03-16 · 12:30-14:30</strong>
                    </div>
                    <div class="course-meta-item">
                        <span class="course-meta-label">招聘状态</span>
                        <strong>等待招募 TA</strong>
                    </div>
                </div>
                <div class="course-card-hint">
                    <span>点击查看详情</span>
                    <span aria-hidden="true">→</span>
                </div>
            </div>
            <div class="job-card course-job-card" tabindex="0" role="button" aria-label="查看 数据科学综合实验 详情" data-job-detail-card data-course-code="MO-DS-820">
                <div class="course-card-topline">
                    <span class="job-code">MO-DS-820</span>
                    <span class="course-mo-badge">开课 MO</span>
                </div>
                <h4>数据科学综合实验</h4>
                <div class="job-tags">
                    <span class="skill-tag">数据科学</span>
                    <span class="skill-tag">综合实验</span>
                    <span class="skill-tag">案例分析</span>
                </div>
                <div class="course-meta-stack">
                    <div class="course-meta-item">
                        <span class="course-meta-label">课程时间</span>
                        <strong>03-20 · 16:30-18:30</strong>
                    </div>
                    <div class="course-meta-item">
                        <span class="course-meta-label">招聘状态</span>
                        <strong>等待招募 TA</strong>
                    </div>
                </div>
                <div class="course-card-hint">
                    <span>点击查看详情</span>
                    <span aria-hidden="true">→</span>
                </div>
            </div>
            <div class="job-card course-job-card" tabindex="0" role="button" aria-label="查看 机器学习进阶实战 详情" data-job-detail-card data-course-code="MO-ML-284">
                <div class="course-card-topline">
                    <span class="job-code">MO-ML-284</span>
                    <span class="course-mo-badge">开课 MO</span>
                </div>
                <h4>机器学习进阶实战</h4>
                <div class="job-tags">
                    <span class="skill-tag">机器学习</span>
                    <span class="skill-tag">进阶实战</span>
                    <span class="skill-tag">岗位训练</span>
                </div>
                <div class="course-meta-stack">
                    <div class="course-meta-item">
                        <span class="course-meta-label">课程时间</span>
                        <strong>03-23 · 12:30-14:30</strong>
                    </div>
                    <div class="course-meta-item">
                        <span class="course-meta-label">招聘状态</span>
                        <strong>等待招募 TA</strong>
                    </div>
                </div>
                <div class="course-card-hint">
                    <span>点击查看详情</span>
                    <span aria-hidden="true">→</span>
                </div>
            </div>
            <div class="job-card course-job-card" tabindex="0" role="button" aria-label="查看 自然语言处理案例研修 详情" data-job-detail-card data-course-code="MO-NLP-217">
                <div class="course-card-topline">
                    <span class="job-code">MO-NLP-217</span>
                    <span class="course-mo-badge">开课 MO</span>
                </div>
                <h4>自然语言处理案例研修</h4>
                <div class="job-tags">
                    <span class="skill-tag">自然语言处理</span>
                    <span class="skill-tag">案例研修</span>
                    <span class="skill-tag">岗位匹配</span>
                </div>
                <div class="course-meta-stack">
                    <div class="course-meta-item">
                        <span class="course-meta-label">课程时间</span>
                        <strong>03-16 · 17:00-19:00</strong>
                    </div>
                    <div class="course-meta-item">
                        <span class="course-meta-label">招聘状态</span>
                        <strong>等待招募 TA</strong>
                    </div>
                </div>
                <div class="course-card-hint">
                    <span>点击查看详情</span>
                    <span aria-hidden="true">→</span>
                </div>
            </div>
            <div class="job-card course-job-card" tabindex="0" role="button" aria-label="查看 分布式系统工程实践 详情" data-job-detail-card data-course-code="MO-DC-552">
                <div class="course-card-topline">
                    <span class="job-code">MO-DC-552</span>
                    <span class="course-mo-badge">开课 MO</span>
                </div>
                <h4>分布式系统工程实践</h4>
                <div class="job-tags">
                    <span class="skill-tag">分布式系统</span>
                    <span class="skill-tag">工程实践</span>
                    <span class="skill-tag">服务治理</span>
                </div>
                <div class="course-meta-stack">
                    <div class="course-meta-item">
                        <span class="course-meta-label">课程时间</span>
                        <strong>03-24 · 19:00-21:00</strong>
                    </div>
                    <div class="course-meta-item">
                        <span class="course-meta-label">招聘状态</span>
                        <strong>等待招募 TA</strong>
                    </div>
                </div>
                <div class="course-card-hint">
                    <span>点击查看详情</span>
                    <span aria-hidden="true">→</span>
                </div>
            </div>
            <div class="job-card course-job-card" tabindex="0" role="button" aria-label="查看 数据库系统性能优化 详情" data-job-detail-card data-course-code="MO-DB-611">
                <div class="course-card-topline">
                    <span class="job-code">MO-DB-611</span>
                    <span class="course-mo-badge">开课 MO</span>
                </div>
                <h4>数据库系统性能优化</h4>
                <div class="job-tags">
                    <span class="skill-tag">数据库</span>
                    <span class="skill-tag">性能优化</span>
                    <span class="skill-tag">SQL 调优</span>
                </div>
                <div class="course-meta-stack">
                    <div class="course-meta-item">
                        <span class="course-meta-label">课程时间</span>
                        <strong>03-27 · 14:00-16:00</strong>
                    </div>
                    <div class="course-meta-item">
                        <span class="course-meta-label">招聘状态</span>
                        <strong>等待招募 TA</strong>
                    </div>
                </div>
                <div class="course-card-hint">
                    <span>点击查看详情</span>
                    <span aria-hidden="true">→</span>
                </div>
            </div>
            <div class="job-card course-job-card" tabindex="0" role="button" aria-label="查看 前端性能与交互架构 详情" data-job-detail-card data-course-code="MO-FE-344">
                <div class="course-card-topline">
                    <span class="job-code">MO-FE-344</span>
                    <span class="course-mo-badge">开课 MO</span>
                </div>
                <h4>前端性能与交互架构</h4>
                <div class="job-tags">
                    <span class="skill-tag">前端工程</span>
                    <span class="skill-tag">性能优化</span>
                    <span class="skill-tag">交互设计</span>
                </div>
                <div class="course-meta-stack">
                    <div class="course-meta-item">
                        <span class="course-meta-label">课程时间</span>
                        <strong>03-29 · 10:00-12:00</strong>
                    </div>
                    <div class="course-meta-item">
                        <span class="course-meta-label">招聘状态</span>
                        <strong>等待招募 TA</strong>
                    </div>
                </div>
                <div class="course-card-hint">
                    <span>点击查看详情</span>
                    <span aria-hidden="true">→</span>
                </div>
            </div>
        </div>
        <div class="job-pagination" aria-label="职位大厅分页" id="jobPagination"></div>
        <div class="job-extra course-job-extra">
            <div class="task-row">
                <span>系统提示：本周内优先开放 MO 待招聘课程，点击卡片可查看完整课程介绍与申请入口。</span>
                <span class="pill">本周新增 3 门</span>
            </div>
            <div class="task-row">
                <span>推荐策略：优先展示与你技能画像和最近投递方向更接近的课程。</span>
                <span class="pill">推荐匹配度 88%</span>
            </div>
        </div>
    </div>
</div>
```

### 阶段 1 文件 07 - [`ta-route-status.jsp`](src/main/webapp/jsp/ta/routes/ta-route-status.jsp)

用途：申请状态 route 主结构与 timeline 结构来源

```jsp
<div class="route" id="route-status" data-route="status">
    <div class="grid-2">
        <div class="card">
            <h1>申请状态追踪</h1>
            <div class="timeline" id="timeline">
                <div class="timeline-item" data-status="warn">
                    <div class="timeline-node warn"></div>
                    <strong>EBU6304 软件工程助教</strong>
                    <div style="color: var(--muted); font-size: 12px; margin-top: 6px;">审核中</div>
                    <div class="accordion-content">
                        <p>MO 评语：教学大纲理解良好。</p>
                        <p>下一步：等待面试安排通知。</p>
                    </div>
                </div>
                <div class="timeline-item" data-status="danger">
                    <div class="timeline-node danger"></div>
                    <strong>EBU6301 计算机网络助教</strong>
                    <div style="color: var(--muted); font-size: 12px; margin-top: 6px;">已关闭</div>
                    <div class="accordion-content">
                        <p>MO 评语：岗位名额已满。</p>
                        <p>建议：关注后续补招批次。</p>
                    </div>
                </div>
                <div class="timeline-item" data-status="success">
                    <div class="timeline-node success"></div>
                    <strong>EBU6402 数据库助教</strong>
                    <div style="color: var(--muted); font-size: 12px; margin-top: 6px;">已录用</div>
                    <div class="accordion-content">
                        <p>MO 评语：经验匹配度高。</p>
                        <p>后续：请留意签约通知。</p>
                    </div>
                </div>
            </div>
        </div>
        <div class="card">
            <h1>状态汇总</h1>
            <div class="status-summary">
                <div class="status-line">
                    <span>面试邀约</span>
                    <span class="pill">1</span>
                </div>
                <div class="status-line">
                    <span>待补充资料</span>
                    <span class="pill">2</span>
                </div>
                <div class="status-line">
                    <span>预计反馈时间</span>
                    <span class="pill">3-5 工作日</span>
                </div>
            </div>
            <div class="message-list" style="margin-top:16px;">
                <div class="message-item">
                    <strong>MO 提醒</strong>
                    <div class="muted">请提交课程项目截图用于教学展示。</div>
                </div>
                <div class="message-item">
                    <strong>系统通知</strong>
                    <div class="muted">AI 推荐已更新，新增 2 个岗位。</div>
                </div>
            </div>
        </div>
    </div>
</div>
```

## 1.6 本阶段完成标志

应该得到：

- 左侧三导航
- 右上角用户入口
- 设置中心 modal 骨架
- 头像与密码相关弹窗骨架
- 首页四卡片骨架
- 状态追踪页时间线骨架
- 三个 route 容器

---
