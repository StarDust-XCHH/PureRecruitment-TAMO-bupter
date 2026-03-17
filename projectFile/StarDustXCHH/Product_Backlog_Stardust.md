# Product Backlog - StarDustXCHH

## Scope and Method

This backlog was reverse-engineered from the current Java Web codebase and expanded with feasible future enhancements for the Teaching Assistant recruitment scenario. The implemented stories were inferred from existing JSP pages, Servlets, JavaScript modules, CSS interactions, JSON-backed persistence, and utility/security logic. Proposed stories extend the current product direction without departing from the academic recruitment context.

## Story Format

| Story ID | Story Name | Description | Priority | Iteration (Sprint) number | Acceptance Criteria | Estimation | Notes | Date started (or planned) | Date finished (or planned) |
|---|---|---|---|---|---|---|---|---|---|
| US-001 | Dual-Role Login Tabs | I want to choose my login identity before entering the system, so that I can start from the correct entrance without confusion. | High | Sprint 1 | 1. The landing page displays two visible role tabs for TA and MO.\n2. Clicking a tab updates the active visual state and hidden role value.\n3. The selected role persists for the current login attempt. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-002 | Glassmorphism Login Card | I want the login page to look clean and professional, so that I feel confident using the platform. | Medium | Sprint 1 | 1. The main login card uses translucent background, border blur, and elevated shadow styling.\n2. Hovering over the card triggers a subtle 3D lift effect.\n3. The layout remains readable against the animated gradient background. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-003 | Dark and Light Theme Toggle | I want to switch between dark and light appearance, so that I can use the platform comfortably in different environments. | High | Sprint 1 | 1. A theme toggle is available on both login and registration cards.\n2. Switching theme updates colors, borders, and glow accents immediately.\n3. The selected theme is stored locally and restored on revisit. | M | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-004 | Animated Ambient Background | I want the welcome page to feel lively and modern, so that the first impression of the system is more engaging. | Low | Sprint 1 | 1. The landing page shows at least two floating glow layers.\n2. The glow animation loops smoothly without blocking user interaction.\n3. Decorative layers do not overlap form controls in a disruptive way. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-005 | TA-Only Registration Entry Guard | I want to be guided to the currently available registration path, so that I do not try to use an option that is not open yet. | High | Sprint 1 | 1. Clicking Register while MO is selected shows an inline error message.\n2. The registration card opens only when the TA tab is active.\n3. The guard happens before any registration form is shown. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-006 | Registration Card Transition Overlay | I want the registration area to open in a focused way, so that I can clearly see I am entering a new step. | Medium | Sprint 1 | 1. Opening registration hides the login card and reveals the registration card.\n2. A semi-transparent blurred overlay is activated behind the registration card.\n3. Closing the registration card returns the user to the login view smoothly. | M | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-007 | Auto-Generated TA ID | I want the system to prepare my TA ID automatically, so that I do not need to create a system code by myself. | High | Sprint 1 | 1. Opening the registration card fills the TA ID field automatically.\n2. The TA ID field is read-only in the form.\n3. The generated TA ID is submitted with the registration request. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-008 | Inline Login Validation | I want the page to tell me immediately when I miss required login information, so that I can correct it quickly. | High | Sprint 1 | 1. Submitting an empty username or password prevents the request.\n2. An inline error message is shown below the login form.\n3. Field error states are reset before each validation cycle. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-009 | Identifier-Based Login | I want to sign in with the account detail I remember best, so that I can access my account more easily. | High | Sprint 1 | 1. The login form label explicitly indicates username, email, or phone support.\n2. The backend accepts a generic identifier parameter and matches multiple account fields.\n3. Successful login returns user profile data including TA ID and username. | M | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-010 | Login Loading Feedback | I want clear feedback while the system is checking my login, so that I know my request is being processed. | Medium | Sprint 1 | 1. Submitting the login form disables the login button.\n2. The button text changes to a loading label during the request.\n3. The button returns to its default label after success or failure. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-011 | API Error-Aware Login Messaging | I want clear messages when login fails, so that I understand whether the problem is my input or the system. | High | Sprint 1 | 1. A 404 login API response shows a dedicated interface error.\n2. Business failures display the backend message when available.\n3. Network exceptions show a server connection failure notice. | M | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-012 | Session and Local User Snapshot | I want the system to remember my basic information after sign-in, so that entering my workspace feels faster and smoother. | Medium | Sprint 1 | 1. Successful login or registration serializes user information to session storage.\n2. The same payload is also saved to local storage.\n3. Stored data includes TA ID, role, and login timestamp. | M | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-013 | Post-Login TA Workspace Redirect | I want to enter my TA workspace immediately after login, so that I can continue my tasks without extra steps. | High | Sprint 1 | 1. Successful login redirects to the TA home page.\n2. Successful registration also redirects to the same TA workspace.\n3. Redirect uses front-end navigation only after a successful API response. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-014 | Registration Form Validation | I want the registration form to point out mistakes before submission, so that I can complete sign-up correctly the first time. | High | Sprint 1 | 1. Empty required fields trigger inline error messaging.\n2. Email and phone inputs are validated against explicit patterns.\n3. Passwords shorter than six characters or mismatched passwords are rejected before submission. | M | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-015 | Registration Submission to Backend | I want my registration details to be saved successfully, so that I can create an account and start using the platform. | High | Sprint 1 | 1. The front end submits TA registration through a POST request to the TA registration API.\n2. On success, the response returns created user metadata such as TA ID and username.\n3. On failure, the page displays the backend error message without redirecting. | M | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-016 | Hidden Admin Entrance Easter Egg | I want the unfinished admin entrance to stay out of normal users' way, so that only intended people can discover it for now. | Medium | Sprint 1 | 1. The admin panel stays hidden by default on the landing page.\n2. Clicking the brand title five times within a short interval reveals the panel.\n3. The page scrolls to the revealed admin area automatically. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-017 | Placeholder Admin Login Messaging | I want the admin entry to explain that it is not ready yet, so that I do not expect a working feature prematurely. | Low | Sprint 1 | 1. Clicking the admin login button does not submit to a backend API.\n2. A visible inline message explains that the admin login interface is reserved but not integrated.\n3. The message appears in the admin panel area only. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-018 | UTF-8 Request and Response Handling | I want the system to display and submit Chinese and English content correctly, so that names, labels, and messages are always readable. | High | Sprint 1 | 1. A server-side filter applies UTF-8 encoding to all requests.\n2. The same filter applies UTF-8 to all responses.\n3. Forms and JSON responses preserve multilingual text without garbling. | S | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-019 | Password Hashing with Salt | I want my password to be protected properly, so that my account feels safer to use. | High | Sprint 1 | 1. Registration generates a per-user salt.\n2. The backend stores only password hash and salt values in the auth object.\n3. Login verifies the hash instead of comparing raw password text. | M | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-020 | Failed Login Attempt Counter | I want the system to notice repeated failed sign-in attempts, so that suspicious behavior can be controlled later. | Medium | Sprint 1 | 1. Incorrect password submissions increment failedAttempts in account data.\n2. A successful login resets failedAttempts to zero.\n3. Updated account data is persisted after each login outcome. | M | Module: Login/Index\nStatus: Implemented | Started | Done |
| US-021 | Multi-Route TA Home Layout | I want one workspace for my profile, job search, and application progress, so that I do not need to jump across separate pages. | High | Sprint 1 | 1. The TA home page loads shared layout, sidebar, topbar, and route sections.\n2. Profile, jobs, and status routes are rendered inside the same application shell.\n3. Core route styles and scripts are loaded centrally on the TA home page. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-022 | Profile Overview Insight Cards | I want to see my key recruitment information at a glance, so that I can understand my current situation quickly. | High | Sprint 1 | 1. The profile route displays at least four insight cards with labels and key metrics.\n2. Each card exposes a click target for a related modal or future action.\n3. Metrics and subtitles are visually separated using card sections and iconography. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-023 | Hero Recommendation Panel | I want the system to highlight what I should improve next, so that I can increase my chances step by step. | Medium | Sprint 1 | 1. The profile route includes a hero card with title, description, badge, and action buttons.\n2. The content is written in advisory language tied to profile quality.\n3. Primary and secondary actions are visually distinguishable. | S | Module: TA Module\nStatus: Implemented | Started | Done |
| US-024 | Job Hall Search and Pagination | I want to search and browse open courses easily, so that I can find suitable opportunities without wasting time. | High | Sprint 1 | 1. A search input filters visible job cards by keyword.\n2. The jobs board paginates results with numbered buttons.\n3. Pagination re-renders card visibility and scrolls the board into view when switching pages. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-025 | Keyboard-Accessible Course Cards | I want the course list to remain easy to use even without a mouse, so that the platform is more accessible. | Medium | Sprint 1 | 1. Each course card is focusable with tabindex and button-like semantics.\n2. Pressing Enter or Space opens the course detail modal.\n3. Mouse click interaction remains supported in parallel. | S | Module: TA Module\nStatus: Implemented | Started | Done |
| US-026 | Live Job Board Refresh from Backend | I want to refresh the course list and see the latest opportunities, so that I do not miss newly opened positions. | High | Sprint 1 | 1. Clicking Refresh sends a GET request to the TA jobs API.\n2. The refresh button shows disabled and spinning states during loading.\n3. Returned course data replaces the visible job cards and updates the open course count badge. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-027 | Course Detail Modal Rendering | I want to see clear course details before applying, so that I can judge whether the role suits me. | High | Sprint 1 | 1. Selecting a course opens a modal showing code, name, MO, date, time, location, and student count.\n2. The modal also renders tags, checklist items, and application suggestions from backend data.\n3. The apply button is contextually tied to the currently active course. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-028 | Application Status Timeline | I want to track the progress of each application clearly, so that I always know what is happening next. | High | Sprint 1 | 1. The status route loads application items from the backend using the TA ID.\n2. Each application is rendered as a timeline item with status, updated time, summary, and next action.\n3. Timeline items can expand or collapse to reveal additional details and step history. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-029 | Status Summary and Notification Panels | I want a quick summary of my application counts and reminders, so that I can monitor progress without opening every record. | High | Sprint 1 | 1. The status page shows summary metrics such as total, active, interview, material-needed, and offer counts.\n2. The notification panel aggregates both global and per-application status reminders.\n3. Both side panels support collapsible interaction with proper expanded state attributes. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-030 | Empty State for No Applications | I want a clear message when I have not applied anywhere yet, so that I know what I should do next. | Medium | Sprint 1 | 1. When no application items are returned, the timeline area becomes empty.\n2. A visible empty-state card explains that there are no application records yet.\n3. The status banner message changes to a neutral guidance message. | S | Module: TA Module\nStatus: Implemented | Started | Done |
| US-031 | Profile Settings Center | I want one place to update my personal information, security settings, and preferences, so that account maintenance feels simple. | High | Sprint 1 | 1. The settings modal contains tabbed sections for profile, security, and preferences.\n2. The active tab updates visible content while preserving the modal shell.\n3. Settings changes require explicit save actions rather than silent auto-save. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-032 | Profile Data Load and Persist | I want my profile information to stay saved after editing, so that I do not need to enter the same details again and again. | High | Sprint 1 | 1. The front end requests profile settings through the profile-settings API using the current TA ID.\n2. Saving profile data submits real name, application intent, email, bio, and skills to the backend.\n3. The backend persists both profile and settings metadata and returns updated data for the UI. | L | Module: TA Module\nStatus: Implemented | Started | Done |
| US-033 | Skill Tag Editing Experience | I want to add and remove my skills easily, so that my profile reflects what I can actually do. | Medium | Sprint 1 | 1. The profile settings form allows skills to be entered and rendered as visible tags.\n2. Editable tags provide a remove action with a clear close icon.\n3. Duplicate or blank skills are normalized out before final persistence. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-034 | Dirty-State Profile Indicator | I want the page to tell me when I still have unsaved profile changes, so that I do not lose my edits by accident. | Medium | Sprint 1 | 1. Editing profile text or skill tags updates the settings status badge.\n2. The badge differentiates loaded, editing, and dirty states visually.\n3. Returning to the original value removes the unsaved-change warning. | S | Module: TA Module\nStatus: Implemented | Started | Done |
| US-035 | Avatar Upload with Client-Side Cropping | I want to adjust my profile picture before saving it, so that my avatar looks centered and presentable. | High | Sprint 1 | 1. Selecting a supported avatar file opens a dedicated crop modal.\n2. The crop modal supports drag and zoom interactions plus preview canvas output.\n3. Confirming the crop generates a square PNG file for later upload. | L | Module: TA Module\nStatus: Implemented | Started | Done |
| US-036 | Avatar File Restrictions and Processing | I want profile pictures to upload smoothly and stay consistent, so that the platform always shows clean and usable avatars. | High | Sprint 1 | 1. The system accepts only PNG, JPEG, WEBP, and GIF image uploads.\n2. Files larger than 10 MB are rejected with a clear error message.\n3. The backend crops/resizes the avatar to a square asset and stores it under the TA data image directory. | L | Module: TA Module\nStatus: Implemented | Started | Done |
| US-037 | Secure Avatar Asset Serving | I want my saved avatar to appear reliably across the platform, so that my account feels complete and personalized. | High | Sprint 1 | 1. Avatar assets are served through a dedicated servlet route.\n2. The backend blocks directory traversal and non-owned invalid file path access by normalizing paths inside the TA data root.\n3. Served images include a cache header and inferred content type. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-038 | Password Update Workflow | I want to change my password inside the system, so that I can keep my account secure without outside help. | High | Sprint 1 | 1. The security tab provides fields for current password, new password, and confirmation.\n2. The backend rejects empty input, short passwords, same-password replacement, and wrong current password.\n3. A successful update writes a new salt and password hash to persistent storage. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-039 | JSON-Based TA Data Persistence | I want my account, profile, and application information to remain available over time, so that my progress is not lost between sessions. | High | Sprint 1 | 1. Account, profile, settings, and application records are saved under mounted JSON files.\n2. Structured files maintain meta information including schema, entity, version, and updatedAt.\n3. Missing files are created automatically with default structures when required. | L | Module: TA Module\nStatus: Implemented | Started | Done |
| US-040 | Default Record Bootstrap for New TA | I want my account to be ready for use immediately after registration, so that I can start exploring the workspace without setup delays. | High | Sprint 1 | 1. Registration creates not only the account record but also corresponding default profile and settings entries.\n2. Default records are generated even if the JSON files were initially empty.\n3. The TA can open the settings center without manual administrator preparation. | M | Module: TA Module\nStatus: Implemented | Started | Done |
| US-041 | AI Course Recommendation Engine | I want the system to suggest the courses that fit me best, so that I can focus on the most promising opportunities first. | High | Sprint 2 | 1. The recommendation logic calculates a transparent match score per open course.\n2. The UI displays ranked suggestions with explanatory factors such as skill overlap and schedule fit.\n3. Recommendation scores update after profile edits or new applications. | L | Module: TA Module\nStatus: Proposed | Planned | Story extends the existing recommendation radar and course matching direction. | Planned | Planned |
| US-042 | One-Click Course Application Persistence | I want to apply for a course directly from its detail view, so that I can complete my interest submission in one clear step. | High | Sprint 2 | 1. Clicking Apply sends a create-application request for the active course.\n2. A successful application adds a new record to TA application storage and updates the visible button state.\n3. The status route reflects the new application without manual data seeding. | L | Module: TA Module\nStatus: Proposed | Planned | Natural next step from the current course-detail modal and status tracking skeleton. | Planned | Planned |
| US-043 | MO Interview Reminder Automation | I want timely reminders before interviews or document deadlines, so that I do not miss important recruitment steps. | High | Sprint 2 | 1. The system generates reminder notifications for interview times and pending document deadlines.\n2. Reminders appear in the notification center and can be surfaced in the dashboard.\n3. Reminder timing is configurable at least at 24-hour and same-day levels. | M | Module: TA Module\nStatus: Proposed | Planned | Extends the current notification center into an operational reminder service. | Planned | Planned |
| US-044 | Availability and Schedule Matching | I want to record when I am available, so that the system can help me avoid courses that clash with my schedule. | High | Sprint 2 | 1. A schedule editor allows the TA to define recurring and ad-hoc availability windows.\n2. Course matching logic warns about conflicts before application submission.\n3. The planner and recommendation modules consume the same availability data source. | L | Module: TA Module\nStatus: Proposed | Planned | Fits the existing planner modal and future TA scheduling domain described in the repository structure. | Planned | Planned |
| US-045 | Resume Attachment and Version History | I want to upload and manage different resume versions, so that I can choose the most suitable one for each opportunity. | High | Sprint 2 | 1. The settings center or profile area supports resume upload with file metadata.\n2. Multiple versions can be listed with upload timestamp and active version marker.\n3. Applications can reference the selected resume version used at submission time. | L | Module: TA Module\nStatus: Proposed | Planned | High-value addition for academic recruitment workflows. | Planned | Planned |
| US-046 | Smart Profile Completeness Scoring | I want the platform to show how complete my profile is, so that I know which missing information may reduce my chances. | Medium | Sprint 2 | 1. The completeness score uses rules based on biography, skills, avatar, contact email, and academic information.\n2. Missing fields are surfaced in a checklist with measurable impact.\n3. Saving profile data recalculates the score immediately. | M | Module: TA Module\nStatus: Proposed | Planned | Evolves the current donut chart and checklist modal into a real scoring engine. | Planned | Planned |
| US-047 | Session Validation Filter for TA Workspace | I want only signed-in users to enter personal work areas, so that account information stays private and protected. | High | Sprint 2 | 1. Requests to protected TA pages are intercepted by an authentication filter.\n2. Missing or invalid session state redirects the user back to the login page or returns a 401 response for APIs.\n3. The filter respects role-specific access boundaries. | L | Module: Login/Index\nStatus: Proposed | Planned | Security hardening beyond the current browser-storage-based bootstrap. | Planned | Planned |
| US-048 | Brute-Force Lockout and Alerting | I want repeated suspicious login attempts to be limited, so that accounts are better protected from abuse. | High | Sprint 2 | 1. Reaching a configurable failed-attempt threshold temporarily blocks further login attempts.\n2. The UI shows a clear lockout message with retry guidance.\n3. The backend records lockout metadata and optionally surfaces administrative alerts. | M | Module: Login/Index\nStatus: Proposed | Planned | Builds directly on the existing failedAttempts counter. | Planned | Planned |
| US-049 | Responsive Mobile Optimization | I want the platform to work well on phones and tablets, so that I can check jobs and updates when I am away from my computer. | Medium | Sprint 2 | 1. Key cards, forms, and modals reflow for narrow viewports without clipping.\n2. Search, status timeline, and settings actions remain operable by touch.\n3. Typography and spacing remain readable at mobile breakpoints. | M | Module: Login/Index and TA Module\nStatus: Proposed | Planned | Existing layout already uses flexible widths and can be extended into full mobile support. | Planned | Planned |
| US-050 | Visual Analytics Dashboard | I want charts and trend views of my recruitment activity, so that I can understand my progress more clearly over time. | Medium | Sprint 3 | 1. The dashboard displays at least three visual metrics such as applications by status, match score trend, and interview conversion.\n2. Chart data can be filtered by semester or date range.\n3. Empty data states are handled gracefully with explanatory messages. | L | Module: TA Module\nStatus: Proposed | Planned | Suitable extension of the current dashboard card framework and status aggregation logic. | Planned | Planned |
| US-051 | Explainable Match Reasons in Job Hall | I want recommended courses to tell me why they suit me, so that I can trust the suggestions and decide faster. | Medium | Sprint 2 | 1. Recommended cards show a short explanation such as matching skills, teaching history, or available time slot.\n2. A user can inspect the main factors contributing to the match score.\n3. Explanations are generated consistently for every ranked recommendation. | M | Module: TA Module\nStatus: Proposed | Planned | Complements the AI recommendation engine with transparency. | Planned | Planned |
| US-052 | Bulk Profile Import from Student Systems | I want to bring in my student information automatically, so that I spend less time filling in repeated details by hand. | Medium | Sprint 3 | 1. The profile area supports import from a structured source such as CSV or student-system API.\n2. Imported fields are previewed before overwrite confirmation.\n3. The system logs which fields were imported versus manually edited. | L | Module: TA Module\nStatus: Proposed | Planned | Practical productivity feature for large-scale TA recruitment onboarding. | Planned | Planned |

## Summary

- Implemented stories identified: **40**
- Proposed stories brainstormed: **12**
- Total backlog items: **52**

## Main Innovation Directions

1. **AI-assisted recruitment intelligence**: course recommendation ranking, explainable match reasons, and smart completeness scoring.
2. **Workflow automation**: one-click application persistence, interview/material reminders, and profile import acceleration.
3. **Security hardening**: session validation filters and brute-force lockout built on top of the current salted-password and failure-counter implementation.
4. **Planning and analytics**: availability matching, mobile optimization, and visual recruitment dashboards.



---


# 中文版


## 产品待办列表 (Product Backlog) - StarDustXCHH

## 范围与方法

此待办列表通过对当前 Java Web 代码库进行逆向工程，并结合助教 (TA) 招聘场景中可行的新功能扩展而成。已实现的 User Story (用户故事) 是根据现有的 JSP 页面、Servlet、JavaScript 模块、CSS 交互、JSON 持久化逻辑以及安全工具类推导得出的。建议开发的故事则在不脱离学术招聘背景的前提下，对当前产品方向进行了延伸。

## 故事格式

| 故事 ID | 故事名称 | 描述 | 优先级 | 迭代 (Sprint) 编号 | 验收标准 | 估算 | 备注 | 开始日期 (或计划) | 完成日期 (或计划) |
|---|---|---|---|---|---|---|---|---|---|
| US-001 | 双角色登录选项卡 | 我希望在进入系统前选择登录身份，以便从正确的入口进入，避免混淆。 | 高 | Sprint 1 | 1. 登录页显示 TA 和 MO 两个可见的角色选项卡。\n2. 点击选项卡可更新视觉状态和隐藏的角色值。\n3. 所选角色在当前登录尝试中保持不变。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-002 | 玻璃拟态登录卡片 | 我希望登录页面看起来整洁专业，让我对使用该平台充满信心。 | 中 | Sprint 1 | 1. 主登录卡片采用半透明背景、边框模糊和悬浮阴影样式。\n2. 悬停在卡片上会触发细微的 3D 抬升效果。\n3. 在动态渐变背景下布局保持清晰可读。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-003 | 深色与浅色主题切换 | 我希望能在深色和浅色外观之间切换，以便在不同环境下舒适地使用平台。 | 高 | Sprint 1 | 1. 登录和注册卡片上均提供主题切换开关。\n2. 切换主题立即更新颜色、边框和发光点缀。\n3. 所选主题存储在本地并在重新访问时恢复。 | M | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-004 | 动态环境背景 | 我希望欢迎页面感觉生动现代，使系统第一印象更具吸引力。 | 低 | Sprint 1 | 1. 落地页显示至少两个浮动的发光层。\n2. 发光动画平滑循环，不阻塞用户交互。\n3. 装饰层不会干扰表单控件的操作。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-005 | 仅限助教注册的入口守卫 | 我希望被引导至当前可用的注册路径，以免尝试使用尚未开放的选项。 | 高 | Sprint 1 | 1. 在选中 MO 时点击注册会显示行内错误提示。\n2. 注册卡片仅在 TA 选项卡激活时打开。\n3. 守卫逻辑在显示任何注册表单之前执行。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-006 | 注册卡片转换遮罩 | 我希望注册区域以聚焦的方式打开，以便清楚地看到我正进入新步骤。 | 中 | Sprint 1 | 1. 打开注册时隐藏登录卡片并显示注册卡片。\n2. 注册卡片后方激活半透明模糊遮罩。\n3. 关闭注册卡片可平滑返回登录视图。 | M | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-007 | 自动生成助教 ID | 我希望系统自动准备我的助教 ID，这样我就不需要自己创建系统代码。 | 高 | Sprint 1 | 1. 打开注册卡片时自动填充助教 ID 字段。\n2. 表单中的助教 ID 字段为只读。\n3. 生成的助教 ID 随注册请求一同提交。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-008 | 行内登录校验 | 我希望页面在我漏填登录信息时立即告知，以便快速纠正。 | 高 | Sprint 1 | 1. 提交空用户名或密码会阻止请求。\n2. 登录表单下方显示行内错误信息。\n3. 字段错误状态在每次校验周期前重置。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-009 | 基于标识符的登录 | 我希望用我记得最清楚的账号详情登录，以便更轻松地访问账户。 | 高 | Sprint 1 | 1. 登录表单标签明确指示支持用户名、邮箱或电话。\n2. 后端接收通用标识符参数并匹配多个账户字段。\n3. 成功登录返回包含助教 ID 和用户名的个人资料。 | M | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-010 | 登录加载反馈 | 我希望在系统检查登录信息时有清晰反馈，以便确认请求正在处理。 | 中 | Sprint 1 | 1. 提交登录表单后禁用登录按钮。\n2. 请求期间按钮文字切换为加载标签。\n3. 成功或失败后按钮恢复默认状态。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-011 | API 错误感知登录提示 | 我希望登录失败时有明确消息，以便了解是输入错误还是系统问题。 | 高 | Sprint 1 | 1. 404 登录 API 响应显示特定的接口错误。\n2. 业务失败时显示后端返回的消息。\n3. 网络异常显示服务器连接失败通知。 | M | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-012 | 会话与本地用户快照 | 我希望系统在登录后记住我的基本信息，使进入工作区的感觉更快更流畅。 | 中 | Sprint 1 | 1. 成功登录或注册后将用户信息序列化至会话存储。\n2. 同一数据包也保存至本地存储。\n3. 存储数据包括助教 ID、角色和登录时间戳。 | M | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-013 | 登录后助教工作区跳转 | 我希望在登录后立即进入助教工作区，以便无需额外步骤即可继续任务。 | 高 | Sprint 1 | 1. 成功登录后跳转至助教主页。\n2. 成功注册后同样跳转至该助教工作区。\n3. 仅在 API 响应成功后通过前端导航进行跳转。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-014 | 注册表单校验 | 我希望注册表单在提交前指出错误，以便我能一次性正确完成注册。 | 高 | Sprint 1 | 1. 必填项为空会触发行内错误提示。\n2. 邮箱和电话输入根据特定模式进行校验。\n3. 密码少于六位或两次输入不匹配将被拒绝提交。 | M | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-015 | 注册提交至后端 | 我希望我的注册详情能成功保存，以便创建账户并开始使用平台。 | 高 | Sprint 1 | 1. 前端通过 POST 请求向助教注册 API 提交数据。\n2. 成功后返回创建的用户元数据（如助教 ID）。\n3. 失败时页面显示后端错误消息而不跳转。 | M | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-016 | 隐藏的管理员入口彩蛋 | 我希望未完成的管理员入口不干扰普通用户，仅让特定人员发现。 | 中 | Sprint 1 | 1. 落地页默认隐藏管理员面板。\n2. 短时间内点击品牌标题五次可触发面板显示。\n3. 页面自动滚动至显现的管理员区域。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-017 | 占位管理员登录消息 | 我希望管理员入口能说明其尚未就绪，以免我过早期待可用功能。 | 低 | Sprint 1 | 1. 点击管理员登录按钮不提交至后端 API。\n2. 显示行内消息说明管理员登录接口已预留但未集成。\n3. 消息仅在管理员面板区域显示。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-018 | UTF-8 请求与响应处理 | 我希望系统正确显示和提交中英文内容，使姓名、标签和消息始终可读。 | 高 | Sprint 1 | 1. 服务端过滤器对所有请求应用 UTF-8 编码。\n2. 同样的过滤器对所有响应应用 UTF-8。\n3. 表单和 JSON 响应保持多语言文本不乱码。 | S | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-019 | 带盐值的密码哈希存储 | 我希望我的密码得到妥善保护，使我的账户使用起来更安全。 | 高 | Sprint 1 | 1. 注册时为每个用户生成独立盐值。\n2. 后端在认证对象中仅存储密码哈希和盐值。\n3. 登录时验证哈希值而非比对明文。 | M | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-020 | 登录失败尝试计数器 | 我希望系统记录连续失败的登录尝试，以便后续控制可疑行为。 | 中 | Sprint 1 | 1. 密码错误提交会增加账户数据中的 `failedAttempts`。\n2. 登录成功会将 `failedAttempts` 重置为零。\n3. 每次登录结果后均会持久化更新账户数据。 | M | 模块: Login/Index\n状态: 已实现 | 已开始 | 已完成 |
| US-021 | 多路由助教主页布局 | 我希望在一个工作区内处理个人资料、职位搜索和申请进度，无需跳转多个页面。 | 高 | Sprint 1 | 1. 助教主页加载共享布局、侧边栏、顶栏和路由区域。\n2. 资料、职位和状态路由在同一个应用壳内渲染。\n3. 核心路由样式和脚本在助教主页集中加载。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-022 | 个人资料概览洞察卡片 | 我希望一眼看到关键的招聘信息，以便快速了解当前状况。 | 高 | Sprint 1 | 1. 资料路由显示至少四个带有标签和关键指标的洞察卡片。\n2. 每个卡片暴露一个点击目标用于触发弹窗或未来操作。\n3. 指标和副标题通过卡片分区和图标进行视觉分离。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-023 | 核心推荐面板 (Hero Card) | 我希望系统突出显示我接下来应改进的地方，以便逐步增加录取机会。 | 中 | Sprint 1 | 1. 资料路由包含一个带有标题、描述、徽章和操作按钮的核心卡片。\n2. 内容采用与资料质量相关的建议性语言。\n3. 主要和次要操作在视觉上有明显区别。 | S | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-024 | 职位大厅搜索与分页 | 我希望轻松搜索和浏览开放课程，以便不浪费时间找到合适机会。 | 高 | Sprint 1 | 1. 通过搜索输入框按关键字过滤可见的职位卡片。\n2. 职位板块通过数字按钮对结果进行分页。\n3. 分页切换时重新渲染卡片可见性并滚动回视图顶部。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-025 | 键盘可访问的课程卡片 | 我希望即使没有鼠标也能轻松使用课程列表，提高平台的易用性。 | 中 | Sprint 1 | 1. 每个课程卡片可通过 `tabindex` 聚焦，并具有类似按钮的语义。\n2. 按下回车或空格键可打开课程详情弹窗。\n3. 同时保留鼠标点击交互支持。 | S | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-026 | 后端实时职位列表刷新 | 我希望刷新课程列表并查看最新机会，以免错过新开放的岗位。 | 高 | Sprint 1 | 1. 点击刷新按钮向助教职位 API 发送 GET 请求。\n2. 加载期间刷新按钮显示禁用和旋转状态。\n3. 返回的课程数据替换当前卡片并更新开放课程计数徽章。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-027 | 课程详情弹窗渲染 | 我希望在申请前看到清晰的课程详情，以便判断该岗位是否适合我。 | 高 | Sprint 1 | 1. 选择课程后打开弹窗，显示代码、名称、MO、时间、地点和选课人数。\n2. 弹窗还根据后端数据渲染标签、检查清单和申请建议。\n3. 申请按钮与当前激活的课程上下文关联。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-028 | 申请状态时间轴 | 我希望清晰追踪每个申请的进度，以便随时了解后续流程。 | 高 | Sprint 1 | 1. 状态路由通过助教 ID 从后端加载申请项。\n2. 每个申请渲染为带有状态、更新时间、摘要和后续操作的时间轴项。\n3. 时间轴项可展开或折叠以显示详细步骤历史。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-029 | 状态摘要与通知面板 | 我希望快速汇总申请数量和提醒信息，无需打开每条记录即可监控进度。 | 高 | Sprint 1 | 1. 状态页显示总数、进行中、面试中、需补件和录用等汇总指标。\n2. 通知面板汇聚全局和单项申请的状态提醒。\n3. 两个侧面板均支持折叠交互。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-030 | 无申请时的空状态提示 | 我希望在尚未申请任何职位时看到明确消息，以便了解接下来该做什么。 | 中 | Sprint 1 | 1. 未返回申请项时，时间轴区域变为空白。\n2. 显示明显的空状态卡片，解释目前尚无记录。\n3. 状态横幅消息切换为中性的引导性文字。 | S | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-031 | 个人设置中心 | 我希望在一个地方更新资料、安全设置和偏好，使账户维护变得简单。 | 高 | Sprint 1 | 1. 设置弹窗包含资料、安全和偏好的选项卡分区。\n2. 切换选项卡时更新内容并保留弹窗外壳。\n3. 设置更改需要明确的保存操作，而非静默自动保存。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-032 | 资料数据的加载与持久化 | 我希望编辑后的资料信息能保持保存，这样我就不需要反复输入相同详情。 | 高 | Sprint 1 | 1. 前端通过助教 ID 请求个人设置 API。\n2. 保存资料时将真实姓名、申请意向、邮箱、简介和技能提交至后端。\n3. 后端持久化存储资料元数据并返回更新后的数据给 UI。 | L | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-033 | 技能标签编辑体验 | 我希望轻松添加和移除技能，使我的资料反映我的真实能力。 | 中 | Sprint 1 | 1. 资料设置表单允许输入技能并渲染为可见标签。\n2. 可编辑标签提供带有清晰关闭图标的移除操作。\n3. 在最终持久化前对重复或空白技能进行规范化处理。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-034 | 资料未保存状态指示 (Dirty-State) | 我希望页面告知我何时仍有未保存的更改，以免不小心丢失编辑内容。 | 中 | Sprint 1 | 1. 编辑资料文本或技能标签会更新设置状态徽章。\n2. 徽章在视觉上区分“已加载”、“编辑中”和“未保存”状态。\n3. 恢复原始值后会移除未保存警告。 | S | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-035 | 带前端裁剪的头像上传 | 我希望在保存前调整个人头像，使头像看起来居中且得体。 | 高 | Sprint 1 | 1. 选择支持的头像文件后打开专用裁剪弹窗。\n2. 裁剪弹窗支持拖拽、缩放交互以及预览画布输出。\n3. 确认裁剪后生成正方形 PNG 文件供后续上传。 | L | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-036 | 头像文件限制与处理 | 我希望头像上传顺畅且保持一致，使平台始终显示清晰可用的头像。 | 高 | Sprint 1 | 1. 系统仅接受 PNG, JPEG, WEBP 和 GIF 图片。\n2. 大于 10 MB 的文件会被拒绝并显示明确错误提示。\n3. 后端将头像裁剪/缩放为正方形资源并存储在助教数据目录下。 | L | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-037 | 安全的头像资源读取 | 我希望保存的头像能可靠地显示在平台上，使我的账户感觉完整且个性化。 | 高 | Sprint 1 | 1. 头像资源通过专用的 Servlet 路由提供。\n2. 后端通过规范化路径防止目录遍历和越权访问。\n3. 提供的图片包含缓存头和推断的内容类型。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-038 | 密码更新工作流 | 我希望在系统内修改密码，以便在无需外部帮助的情况下保持账户安全。 | 高 | Sprint 1 | 1. 安全选项卡提供原密码、新密码和确认密码字段。\n2. 后端拒绝空输入、短密码、与旧密码相同及原密码错误的请求。\n3. 成功更新后向持久化存储写入新的盐值和哈希。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-039 | 基于 JSON 的助教数据持久化 | 我希望账号、资料和申请信息能长期保留，使我的进度不会在会话间丢失。 | 高 | Sprint 1 | 1. 账户、资料、设置和申请记录保存在挂载的 JSON 文件中。\n2. 结构化文件维护元信息，包括 Schema、实体类型、版本和更新时间。\n3. 缺失的文件会在需要时自动创建默认结构。 | L | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-040 | 新助教的默认记录初始化 | 我希望注册后账号立即就绪，以便我能直接开始探索工作区。 | 高 | Sprint 1 | 1. 注册时不仅创建账户记录，还创建对应的默认资料和设置条目。\n2. 即使 JSON 文件最初为空，也会生成默认记录。\n3. 助教无需管理员手动准备即可打开设置中心。 | M | 模块: TA Module\n状态: 已实现 | 已开始 | 已完成 |
| US-041 | AI 课程推荐引擎 | 我希望系统建议最适合我的课程，以便我能优先关注最有希望的机会。 | 高 | Sprint 2 | 1. 推荐逻辑为每门开放课程计算透明的匹配分。\n2. UI 显示按排名推荐的列表，并说明匹配因素（如技能重合、时间匹配）。\n3. 推荐分数在资料修改或新申请后更新。 | L | 模块: TA Module\n状态: 建议开发 | 计划中 | 扩展现有的推荐雷达和课程匹配方向。 | 计划中 | 计划中 |
| US-042 | 一键课程申请持久化 | 我希望直接从详情页申请课程，以便在一个清晰步骤内完成意向提交。 | 高 | Sprint 2 | 1. 点击申请按钮向活跃课程发送创建申请请求。\n2. 成功申请后向助教存储添加新记录并更新按钮状态。\n3. 状态路由无需手动刷新即可反映新申请。 | L | 模块: TA Module\n状态: 建议开发 | 计划中 | 当前课程详情弹窗和状态追踪骨架的自然下一步。 | 计划中 | 计划中 |
| US-043 | MO 面试自动化提醒 | 我希望在面试或材料截止日期前收到及时提醒，以免错过重要招聘步骤。 | 高 | Sprint 2 | 1. 系统为面试时间和待办截止日期生成通知提醒。\n2. 提醒显示在通知中心并可悬浮在仪表盘上。\n3. 提醒时间至少支持 24 小时和当天两个层级。 | M | 模块: TA Module\n状态: 建议开发 | 计划中 | 将现有通知中心扩展为运营提醒服务。 | 计划中 | 计划中 |
| US-044 | 可用时间与课程表匹配 | 我希望记录我的空余时间，以便系统帮我避开与课表冲突的职位。 | 高 | Sprint 2 | 1. 时间表编辑器允许助教定义周期性及临时的空闲窗口。\n2. 课程匹配逻辑在提交申请前对时间冲突进行警告。\n3. 规划器和推荐模块共用同一可用性数据源。 | L | 模块: TA Module\n状态: 建议开发 | 计划中 | 符合现有规划器弹窗及未来助教调度领域的架构。 | 计划中 | 计划中 |
| US-045 | 简历附件与版本管理 | 我希望上传并管理不同版本的简历，以便为每个机会选择最合适的附件。 | 高 | Sprint 2 | 1. 设置中心支持带元数据的简历上传。\n2. 支持列出多个版本，带上传时间戳和激活标记。\n3. 申请记录可引用提交时选用的简历版本。 | L | 模块: TA Module\n状态: 建议开发 | 计划中 | 学术招聘工作流的高价值补充。 | 计划中 | 计划中 |
| US-046 | 智能资料完整度评分 | 我希望平台显示资料完整度，以便了解哪些缺失信息可能降低我的被录取率。 | 中 | Sprint 2 | 1. 完整度评分基于简介、技能、头像、联系方式等规则计算。\n2. 缺失字段在清单中列出并标明对评分的影响。\n3. 保存资料后立即重新计算分数。 | M | 模块: TA Module\n状态: 建议开发 | 计划中 | 将现有的圆环图和检查清单演进为真实的评分引擎。 | 计划中 | 计划中 |
| US-047 | 助教工作区会话验证过滤器 | 我希望仅限已登录用户进入个人工作区，以保护账户隐私。 | 高 | Sprint 2 | 1. 对受保护助教页面的请求由认证过滤器拦截。\n2. 缺失或无效的会话状态将重定向至登录页或返回 401 API 响应。\n3. 过滤器尊重基于角色的访问权限边界。 | L | 模块: Login/Index\n状态: 建议开发 | 计划中 | 在现有基于浏览器存储的逻辑之上进行安全加固。 | 计划中 | 计划中 |
| US-048 | 暴力破解锁定与告警 | 我希望限制重复的可疑登录尝试，使账户得到更好的保护。 | 高 | Sprint 2 | 1. 达到可配置的失败尝试阈值后临时锁定登录。\n2. UI 显示清晰的锁定消息和重试引导。\n3. 后端记录锁定元数据并可向管理端发送告警。 | M | 模块: Login/Index\n状态: 建议开发 | 计划中 | 直接基于现有的 `failedAttempts` 计数器构建。 | 计划中 | 计划中 |
| US-049 | 响应式移动端优化 | 我希望平台在手机和平板上表现良好，以便在外出时查看职位和状态。 | 中 | Sprint 2 | 1. 关键卡片、表单和弹窗在窄屏下自动重排不溢出。\n2. 搜索、状态时间轴和设置操作在触摸屏上保持可用。\n3. 移动端断点下的字号和间距保持可读性。 | M | 模块: Login/Index & TA\n状态: 建议开发 | 计划中 | 现有布局已使用弹性宽度，可扩展至全移动端支持。 | 计划中 | 计划中 |
| US-050 | 可视化分析仪表盘 | 我希望查看招聘活动的图表和趋势，以便更清晰地了解进度。 | 中 | Sprint 3 | 1. 仪表盘显示至少三个视觉指标：申请状态分布、匹配度趋势和面试转化率。\n2. 图表数据可按学期或日期范围过滤。\n3. 优雅处理空数据状态并显示解释性消息。 | L | 模块: TA Module\n状态: 建议开发 | 计划中 | 适合扩展现有的仪表盘卡片框架。 | 计划中 | 计划中 |
| US-051 | 职位大厅的可解释匹配原因 | 我希望推荐的课程能告知我为何适合，以便我信任建议并快速决定。 | 中 | Sprint 2 | 1. 推荐卡片显示简短解释（如：匹配技能、授课经历或空闲时间）。\n2. 用户可以查看贡献匹配分的主要因素。\n3. 为每个排序后的推荐项生成一致的解释。 | M | 模块: TA Module\n状态: 建议开发 | 计划中 | 增强 AI 推荐引擎的透明度。 | 计划中 | 计划中 |
| US-052 | 从学生系统批量导入资料 | 我希望自动导入学生信息，减少手动填写重复详情的时间。 | 中 | Sprint 3 | 1. 资料区域支持从 CSV 或学生系统 API 导入。\n2. 导入字段在覆盖确认前提供预览。\n3. 系统记录哪些字段是导入的，哪些是手动编辑的。 | L | 模块: TA Module\n状态: 建议开发 | 计划中 | 大规模助教招聘入职的实用生产力功能。 | 计划中 | 计划中 |

## 总结

- 已实现故事识别：**40** 个
- 建议开发故事头脑风暴：**12** 个
- 待办列表总项数：**52** 个

## 主要创新方向

1. **AI 辅助招聘智能**：课程推荐排名、可解释的匹配原因以及智能完整度评分。
2. **工作流自动化**：一键申请持久化、面试/材料提醒以及资料导入加速。
3. **安全加固**：在现有的加盐密码和失败计数实现基础上，增加会话验证过滤器和暴力破解锁定。
4. **规划与分析**：可用性匹配、移动端优化以及可视化的招聘仪表盘。



# 原型设计

![img.png](imgStage1Prototype/img.png)

![img_1.png](imgStage1Prototype/img_1.png)

![img_2.png](imgStage1Prototype/img_2.png)

![img_3.png](imgStage1Prototype/img_3.png)

![img_4.png](imgStage1Prototype/img_4.png)

![img_5.png](imgStage1Prototype/img_5.png)

![img_6.png](imgStage1Prototype/img_6.png)

![img_7.png](imgStage1Prototype/img_7.png)

![img_8.png](imgStage1Prototype/img_8.png)

![img_9.png](imgStage1Prototype/img_9.png)

![img_10.png](imgStage1Prototype/img_10.png)

![img_11.png](imgStage1Prototype/img_11.png)

![img_12.png](imgStage1Prototype/img_12.png)

![img_13.png](imgStage1Prototype/img_13.png)

![img_14.png](imgStage1Prototype/img_14.png)

![img_15.png](imgStage1Prototype/img_15.png)


![img_16.png](imgStage1Prototype/img_16.png)

![img_17.png](imgStage1Prototype/img_17.png)


![img_18.png](imgStage1Prototype/img_18.png)

![img_19.png](imgStage1Prototype/img_19.png)

![img_20.png](imgStage1Prototype/img_20.png)

![img_21.png](imgStage1Prototype/img_21.png)

![img_22.png](imgStage1Prototype/img_22.png)

![img_23.png](imgStage1Prototype/img_23.png)

