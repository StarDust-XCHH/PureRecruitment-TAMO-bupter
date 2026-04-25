<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en" data-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MO Workspace</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-dashboard-cards.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-jobs.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-modal.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/mo/css/mo-home.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-settings.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-profile.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/ta/css/ta-onboarding.css">

    <!-- 内联样式：精简布局 -->
    <style>
        /* 强制覆盖 .main 的样式 */
        main.mo-dashboard-view {
            padding: 10px 20px 20px 20px !important;
            gap: 10px !important;
            height: calc(100vh - 20px) !important;
            max-height: calc(100vh - 20px) !important;
            overflow: hidden !important;
            display: flex !important;
            flex-direction: column !important;
        }

        /* 隐藏欢迎卡片 */
        main.mo-dashboard-view .welcome-card {
            display: none !important;
        }

        /* 隐藏路由容器 */
        main.mo-dashboard-view .routes {
            display: none !important;
        }

        /* 顶栏调整：让统计卡片与欢迎语同行 */
        main.mo-dashboard-view .topbar {
            align-items: center !important;
            gap: 20px !important;
        }

        main.mo-dashboard-view .greeting {
            display: flex !important;
            align-items: center !important;
            gap: 20px !important;
        }

        main.mo-dashboard-view .greeting h1 {
            margin: 0 !important;
            white-space: nowrap !important;
        }

        /* 招聘总览统计卡片（内联样式） */
        .mo-dashboard-stats-inline {
            display: flex !important;
            gap: 16px !important;
            align-items: center !important;
        }

        .mo-stat-item {
            display: flex !important;
            align-items: center !important;
            gap: 8px !important;
            padding: 6px 12px !important;
            background: var(--card) !important;
            border: 1px solid var(--card-border) !important;
            border-radius: 8px !important;
            transition: all 0.2s !important;
        }

        .mo-stat-item:hover {
            border-color: var(--primary) !important;
            box-shadow: 0 0 8px var(--primary-glow) !important;
        }

        .mo-stat-icon {
            font-size: 16px !important;
            line-height: 1 !important;
        }

        .mo-stat-content {
            display: flex !important;
            flex-direction: column !important;
            gap: 2px !important;
        }

        .mo-stat-label {
            font-size: 10px !important;
            color: var(--muted) !important;
            line-height: 1 !important;
        }

        .mo-stat-value {
            font-size: 16px !important;
            font-weight: 700 !important;
            color: var(--text) !important;
            line-height: 1 !important;
        }

        /* 布局容器：只包含下方两个模块 */
        .mo-layout-container {
            display: flex !important;
            flex: 1 !important;
            min-height: 0 !important;
            gap: 10px !important;
        }

        /* 面板通用样式 */
        .mo-panel {
            background: var(--card) !important;
            border: 1px solid var(--card-border) !important;
            border-radius: 10px !important;
            display: flex !important;
            flex-direction: column !important;
            min-width: 0 !important;
            overflow: hidden !important;
        }

        /* 左侧：课程管理（占 4 份） */
        .mo-panel-left {
            flex: 4 !important;
        }

        /* 右侧：应聘筛选（占 3 份） */
        .mo-panel-right {
            flex: 3 !important;
        }

        /* 面板头部 */
        .mo-panel-head {
            padding: 8px 12px !important;
            border-bottom: 1px solid var(--card-border) !important;
            display: flex !important;
            justify-content: space-between !important;
            align-items: center !important;
            height: 36px !important;
            flex-shrink: 0 !important;
        }

        .mo-panel-head h3 {
            margin: 0 !important;
            font-size: 14px !important;
            color: var(--primary) !important;
            font-weight: 700 !important;
        }

        .mo-mini-btn {
            background: transparent !important;
            border: 1px solid var(--card-border) !important;
            color: var(--muted) !important;
            font-size: 11px !important;
            padding: 3px 10px !important;
            border-radius: 4px !important;
            cursor: pointer !important;
            height: 24px !important;
            line-height: 1 !important;
        }

        .mo-mini-btn:hover {
            border-color: var(--primary) !important;
            color: var(--primary) !important;
        }

        /* 面板内容区 */
        .mo-panel-body {
            flex: 1 !important;
            overflow-y: auto !important;
            padding: 10px !important;
            min-height: 0 !important;
        }

        /* 隐藏子页面大标题 */
        .mo-panel-body .route h1,
        .mo-panel-body .overview-top h1,
        .mo-panel-body .jobs-hall-card h1 {
            display: none !important;
        }

        /* 隐藏子页面描述 */
        .mo-panel-body .muted,
        .mo-panel-body .mo-dashboard-lead p {
            display: none !important;
        }

        /* ===== 课程管理模块内部元素缩小 ===== */

        /* 工具栏按钮缩小 */
        .mo-panel-left .pill-btn,
        .mo-panel-left .refresh-btn {
            height: 28px !important;
            padding: 4px 12px !important;
            font-size: 11px !important;
            border-radius: 14px !important;
        }

        /* 发布课程按钮 */
        .mo-panel-left #openJobPublishModalBtn {
            height: 28px !important;
            padding: 4px 14px !important;
            font-size: 12px !important;
            font-weight: 600 !important;
        }

        /* 开放课程计数标签 */
        .mo-panel-left #openCoursesCount {
            height: 26px !important;
            padding: 4px 10px !important;
            font-size: 11px !important;
            line-height: 1.5 !important;
        }

        /* 搜索框缩小 */
        .mo-panel-left .job-search-box {
            height: 28px !important;
            margin-bottom: 8px !important;
        }

        .mo-panel-left .job-search-box input {
            height: 26px !important;
            font-size: 12px !important;
            padding: 4px 8px !important;
        }

        .mo-panel-left .job-search-icon {
            font-size: 14px !important;
        }

        /* 课程卡片缩小 */
        .mo-panel-left .job-board {
            gap: 8px !important;
        }

        .mo-panel-left .job-card {
            padding: 10px !important;
            border-radius: 8px !important;
        }

        .mo-panel-left .job-card h3 {
            font-size: 14px !important;
            margin-bottom: 4px !important;
        }

        .mo-panel-left .job-card .job-meta {
            font-size: 11px !important;
        }

        .mo-panel-left .tag {
            font-size: 10px !important;
            padding: 2px 6px !important;
        }

        /* ===== 应聘筛选模块内部元素优化 ===== */

        /* 下拉选择框缩小 */
        .mo-panel-right select {
            height: 28px !important;
            font-size: 12px !important;
            padding: 4px 8px !important;
            border-radius: 6px !important;
        }

        /* 筛选工具栏 */
        .mo-panel-right .mo-applicants-toolbar {
            gap: 8px !important;
            margin-bottom: 8px !important;
        }

        .mo-panel-right .mo-applicants-toolbar label {
            font-size: 11px !important;
        }

        /* 刷新按钮 */
        .mo-panel-right .refresh-btn {
            height: 26px !important;
            padding: 3px 10px !important;
            font-size: 11px !important;
        }

        /* 【重点调整】申请人卡片样式优化，使其与课程卡片协调 */
        .mo-panel-right .applicant-card {
            padding: 14px !important;
            border-radius: 10px !important;
            min-height: 160px !important;
            display: flex !important;
            flex-direction: column !important;
            gap: 10px !important;
        }

        .mo-panel-right .applicant-card h4 {
            font-size: 15px !important;
            margin: 0 !important;
            font-weight: 700 !important;
        }

        .mo-panel-right .applicant-card .applicant-meta {
            font-size: 12px !important;
            line-height: 1.5 !important;
        }

        /* 申请人卡片内的课程编号 */
        .mo-panel-right .applicant-card .course-code {
            font-size: 13px !important;
            font-weight: 600 !important;
            color: var(--primary) !important;
            margin-bottom: 4px !important;
        }

        /* 状态标签 */
        .mo-panel-right .status-badge {
            font-size: 11px !important;
            padding: 3px 10px !important;
            border-radius: 12px !important;
        }

        /* 申请人卡片的 TA 编号 */
        .mo-panel-right .ta-id {
            font-size: 11px !important;
            color: var(--muted) !important;
        }

        /* 查看详情按钮 */
        .mo-panel-right .detail-btn {
            margin-top: auto !important;
            align-self: flex-end !important;
            padding: 6px 16px !important;
            font-size: 12px !important;
            border-radius: 16px !important;
        }

        /* 隐藏分页 */
        .mo-panel-body .job-pagination,
        .mo-panel-body .applicant-pagination {
            display: none !important;
        }

        /* 滚动条美化 */
        .mo-panel-body::-webkit-scrollbar {
            width: 4px;
        }

        .mo-panel-body::-webkit-scrollbar-thumb {
            background: var(--card-border);
            border-radius: 2px;
        }

        /* 响应式：小屏幕恢复垂直堆叠 */
        @media (max-width: 1200px) {
            .mo-layout-container {
                flex-direction: column !important;
            }

            main.mo-dashboard-view {
                height: auto !important;
                overflow: visible !important;
            }

            .mo-dashboard-stats-inline {
                flex-wrap: wrap !important;
            }
        }
    </style>
</head>
<body>
<div class="app" id="moApp">
    <aside class="sidebar">
        <%@ include file="partials/mo-layout-sidebar.jspf" %>
    </aside>
    <main class="main mo-dashboard-view">
        <%@ include file="partials/mo-layout-topbar.jspf" %>

        <!-- 布局容器：只包含课程管理和应聘筛选 -->
        <div class="mo-layout-container">

            <!-- 左侧：课程管理（占 4 份） -->
            <div class="mo-panel mo-panel-left">
                <div class="mo-panel-head">
                    <h3>课程管理</h3>
                    <button class="mo-mini-btn" type="button" onclick="document.querySelector('[data-route=jobs]').click()">查看更多</button>
                </div>
                <div class="mo-panel-body">
                    <%@ include file="routes/mo-route-jobs.jspf" %>
                </div>
            </div>

            <!-- 右侧：应聘筛选（占 3 份） -->
            <div class="mo-panel mo-panel-right">
                <div class="mo-panel-head">
                    <h3>应聘筛选</h3>
                    <button class="mo-mini-btn" type="button" onclick="document.querySelector('[data-route=applicants]').click()">查看更多</button>
                </div>
                <div class="mo-panel-body">
                    <%@ include file="routes/mo-route-applicants.jspf" %>
                </div>
            </div>

        </div>
    </main>
</div>

<%@ include file="partials/mo-modals.jspf" %>
<%@ include file="partials/mo-onboarding.jspf" %>

<script>window.__APP_CONTEXT_PATH__ = '${pageContext.request.contextPath}';</script>
<script src="${pageContext.request.contextPath}/assets/mo/js/mo-api-prefix.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/settings.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/route-nav.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/modal.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/job-board.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/applicants.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/dashboard.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/profile.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/modules/onboarding.js"></script>
<script src="${pageContext.request.contextPath}/assets/mo/js/mo-home.js"></script>
</body>
</html>
