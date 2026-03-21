<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN" data-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>管理员控制台</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/css/admin-layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/css/admin-dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/css/admin-users.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/css/admin-notices.css">
</head>
<body>
<div class="app" id="adminApp">
    <aside class="sidebar">
        <div class="sidebar-header">
            <div class="logo">ADMIN</div>
            <div class="admin-info">
                <div class="admin-name" id="adminName">管理员</div>
                <div class="admin-role" id="adminRole">超级管理员</div>
            </div>
        </div>
        <nav class="sidebar-nav">
            <button class="nav-item active" data-route="dashboard">
                <span class="icon">📊</span>
                <span class="text">数据看板</span>
            </button>
            <button class="nav-item" data-route="users">
                <span class="icon">👥</span>
                <span class="text">用户管理</span>
            </button>
            <button class="nav-item" data-route="notices">
                <span class="icon">📢</span>
                <span class="text">公告管理</span>
            </button>
            <button class="nav-item" data-route="permissions">
                <span class="icon">🔐</span>
                <span class="text">权限管理</span>
            </button>
            <button class="nav-item" data-route="settings">
                <span class="icon">⚙️</span>
                <span class="text">系统设置</span>
            </button>
        </nav>
        <div class="sidebar-footer">
            <button class="logout-btn" id="logoutBtn">
                <span class="icon">🚪</span>
                <span class="text">退出登录</span>
            </button>
        </div>
    </aside>

    <main class="main">
        <header class="topbar">
            <div class="topbar-left">
                <h1 class="page-title" id="pageTitle">数据看板</h1>
            </div>
            <div class="topbar-right">
                <button class="topbar-btn" id="refreshBtn">
                    <span>🔄</span>
                </button>
                <div class="user-info">
                    <span class="avatar">A</span>
                </div>
            </div>
        </header>

        <section class="routes">
            <!-- Dashboard Route -->
            <div class="route active" id="route-dashboard">
                <div class="dashboard-stats">
                    <div class="stat-card">
                        <div class="stat-icon">👤</div>
                        <div class="stat-info">
                            <div class="stat-value" id="totalUsers">0</div>
                            <div class="stat-label">总用户数</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">🎓</div>
                        <div class="stat-info">
                            <div class="stat-value" id="taUsers">0</div>
                            <div class="stat-label">TA 用户</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">👨‍🏫</div>
                        <div class="stat-info">
                            <div class="stat-value" id="moUsers">0</div>
                            <div class="stat-label">MO 用户</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">💼</div>
                        <div class="stat-info">
                            <div class="stat-value" id="activeJobs">0</div>
                            <div class="stat-label">活跃岗位</div>
                        </div>
                    </div>
                </div>

                <div class="dashboard-sections">
                    <div class="dashboard-section">
                        <h3>最近登录用户</h3>
                        <div class="table-container">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>用户名</th>
                                        <th>角色</th>
                                        <th>登录时间</th>
                                    </tr>
                                </thead>
                                <tbody id="recentLogins">
                                    <tr><td colspan="3">暂无数据</td></tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div class="dashboard-section">
                        <h3>系统公告</h3>
                        <div class="notice-list" id="noticeList">
                            <div class="empty-state">暂无公告</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Users Route -->
            <div class="route" id="route-users">
                <div class="toolbar">
                    <input type="text" class="search-input" id="userSearch" placeholder="搜索用户名/邮箱...">
                    <select class="filter-select" id="roleFilter">
                        <option value="">全部角色</option>
                        <option value="TA">TA</option>
                        <option value="MO">MO</option>
                        <option value="ADMIN">管理员</option>
                    </select>
                </div>
                <div class="table-container">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>用户名</th>
                                <th>邮箱</th>
                                <th>角色</th>
                                <th>状态</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody id="userTable">
                            <tr><td colspan="6">加载中...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Notices Route -->
            <div class="route" id="route-notices">
                <div class="toolbar">
                    <button class="btn primary" id="createNoticeBtn">新建公告</button>
                </div>
                <div class="notice-list" id="allNotices">
                    <div class="empty-state">加载中...</div>
                </div>
            </div>

            <!-- Permissions Route -->
            <div class="route" id="route-permissions">
                <div class="empty-state">
                    <div class="empty-icon">🔐</div>
                    <div class="empty-text">权限管理功能开发中</div>
                </div>
            </div>

            <!-- Settings Route -->
            <div class="route" id="route-settings">
                <div class="empty-state">
                    <div class="empty-icon">⚙️</div>
                    <div class="empty-text">系统设置功能开发中</div>
                </div>
            </div>
        </section>
    </main>
</div>

<script src="${pageContext.request.contextPath}/assets/admin/js/admin-home.js"></script>
</body>
</html>
