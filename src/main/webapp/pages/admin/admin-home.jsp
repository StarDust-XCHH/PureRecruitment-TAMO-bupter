<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN" data-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin console</title>
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
                <div class="admin-name" id="adminName">Admin</div>
                <div class="admin-role" id="adminRole">Super admin</div>
            </div>
        </div>
        <nav class="sidebar-nav">
            <button class="nav-item active" data-route="dashboard">
                <span class="icon">📊</span>
                <span class="text">Dashboard</span>
            </button>
            <button class="nav-item" data-route="users">
                <span class="icon">👥</span>
                <span class="text">User management</span>
            </button>
            <button class="nav-item" data-route="notices">
                <span class="icon">📢</span>
                <span class="text">Notices</span>
            </button>
            <button class="nav-item" data-route="permissions">
                <span class="icon">🔐</span>
                <span class="text">Permissions</span>
            </button>
            <button class="nav-item" data-route="settings">
                <span class="icon">⚙️</span>
                <span class="text">System settings</span>
            </button>
        </nav>
        <div class="sidebar-footer">
            <button class="logout-btn" id="logoutBtn">
                <span class="icon">🚪</span>
                <span class="text">Sign out</span>
            </button>
        </div>
    </aside>

    <main class="main">
        <header class="topbar">
            <div class="topbar-left">
                <h1 class="page-title" id="pageTitle">Dashboard</h1>
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
                            <div class="stat-label">Total users</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">🎓</div>
                        <div class="stat-info">
                            <div class="stat-value" id="taUsers">0</div>
                            <div class="stat-label">TA users</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">👨‍🏫</div>
                        <div class="stat-info">
                            <div class="stat-value" id="moUsers">0</div>
                            <div class="stat-label">MO users</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">💼</div>
                        <div class="stat-info">
                            <div class="stat-value" id="activeJobs">0</div>
                            <div class="stat-label">Active postings</div>
                        </div>
                    </div>
                </div>

                <div class="dashboard-sections">
                    <div class="dashboard-section">
                        <h3>Recent sign-ins</h3>
                        <div class="table-container">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Username</th>
                                        <th>Role</th>
                                        <th>Signed in at</th>
                                    </tr>
                                </thead>
                                <tbody id="recentLogins">
                                    <tr><td colspan="3">No data</td></tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div class="dashboard-section">
                        <h3>System notices</h3>
                        <div class="notice-list" id="noticeList">
                            <div class="empty-state">No notices</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Users Route -->
            <div class="route" id="route-users">
                <div class="toolbar">
                    <input type="text" class="search-input" id="userSearch" placeholder="Search username or email...">
                    <select class="filter-select" id="roleFilter">
                        <option value="">All roles</option>
                        <option value="TA">TA</option>
                        <option value="MO">MO</option>
                        <option value="ADMIN">Admin</option>
                    </select>
                </div>
                <div class="table-container">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Username</th>
                                <th>Email</th>
                                <th>Role</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="userTable">
                            <tr><td colspan="6">Loading...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Notices Route -->
            <div class="route" id="route-notices">
                <div class="toolbar">
                    <button class="btn primary" id="createNoticeBtn">New notice</button>
                </div>
                <div class="notice-list" id="allNotices">
                    <div class="empty-state">Loading...</div>
                </div>
            </div>

            <!-- Permissions Route -->
            <div class="route" id="route-permissions">
                <div class="empty-state">
                    <div class="empty-icon">🔐</div>
                    <div class="empty-text">Permissions management is not available yet</div>
                </div>
            </div>

            <!-- Settings Route -->
            <div class="route" id="route-settings">
                <div class="empty-state">
                    <div class="empty-icon">⚙️</div>
                    <div class="empty-text">System settings are not available yet</div>
                </div>
            </div>
        </section>
    </main>
</div>

<script src="${pageContext.request.contextPath}/assets/admin/js/admin-home.js"></script>
</body>
</html>
