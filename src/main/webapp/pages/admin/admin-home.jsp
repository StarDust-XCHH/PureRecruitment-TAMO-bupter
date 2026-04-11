<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN" data-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Console</title>
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
                <div class="admin-role" id="adminRole">Super Admin</div>
            </div>
        </div>
        <nav class="sidebar-nav">
            <button class="nav-item active" data-route="dashboard">
                <span class="icon">📊</span>
                <span class="text">Dashboard</span>
            </button>
            <button class="nav-item" data-route="users">
                <span class="icon">👥</span>
                <span class="text">Users</span>
            </button>
            <button class="nav-item" data-route="courses">
                <span class="icon">📚</span>
                <span class="text">Courses</span>
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
                <span class="text">Settings</span>
            </button>
        </nav>
        <div class="sidebar-footer">
            <button class="logout-btn" id="logoutBtn">
                <span class="icon">🚪</span>
                <span class="text">Logout</span>
            </button>
        </div>
    </aside>

    <main class="main">
        <header class="topbar">
            <div class="topbar-left">
                <h1 class="page-title" id="pageTitle">Dashboard</h1>
            </div>
            <div class="topbar-right">
                <button class="theme-toggle-btn" id="themeToggleBtn" title="Toggle Theme">
                    <span class="toggle-icon">🌙</span>
                    <span class="toggle-text" id="themeToggleText">Dark</span>
                </button>
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
                            <div class="stat-value" id="totalUsers">-</div>
                            <div class="stat-label">Total Users</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">🎓</div>
                        <div class="stat-info">
                            <div class="stat-value" id="taUsers">-</div>
                            <div class="stat-label">TA Users</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">👨‍🏫</div>
                        <div class="stat-info">
                            <div class="stat-value" id="moUsers">-</div>
                            <div class="stat-label">MO Users</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon">💼</div>
                        <div class="stat-info">
                            <div class="stat-value" id="activeJobs">-</div>
                            <div class="stat-label">Admin Users</div>
                        </div>
                    </div>
                </div>

                <div class="dashboard-sections">
                    <div class="dashboard-section">
                        <h3>Recent Logins</h3>
                        <div class="table-container">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Username</th>
                                        <th>Role</th>
                                        <th>Login Time</th>
                                    </tr>
                                </thead>
                                <tbody id="recentLogins">
                                    <tr><td colspan="3" class="loading">Loading...</td></tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div class="dashboard-section">
                        <h3>System Notices</h3>
                        <div class="notice-list" id="noticeList">
                            <div class="empty-state">No notices</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Users Route -->
            <div class="route" id="route-users">
                <div class="toolbar">
                    <input type="text" class="search-input" id="userSearch" placeholder="Search username/email...">
                    <select class="filter-select" id="roleFilter">
                        <option value="">All Roles</option>
                        <option value="TA">TA</option>
                        <option value="MO">MO</option>
                        <option value="ADMIN">Admin</option>
                    </select>
                    <button class="btn primary" id="refreshUsersBtn">Refresh</button>
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
                            <tr><td colspan="6" class="loading">Loading...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Courses Route -->
            <div class="route" id="route-courses">
                <div class="toolbar">
                    <input type="text" class="search-input" id="courseSearch" placeholder="Search course name/ID...">
                    <select class="filter-select" id="courseStatusFilter">
                        <option value="">All Status</option>
                        <option value="OPEN">Open</option>
                        <option value="CLOSED">Closed</option>
                    </select>
                </div>
                <div class="table-container">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Course ID</th>
                                <th>Course Name</th>
                                <th>Semester</th>
                                <th>MO</th>
                                <th>TA Count</th>
                                <th>Applicants</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="courseTable">
                            <tr><td colspan="8" class="loading">Loading...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Notices Route -->
            <div class="route" id="route-notices">
                <div class="toolbar">
                    <button class="btn primary" id="createNoticeBtn">New Notice</button>
                </div>
                <div class="notice-list" id="allNotices">
                    <div class="loading-state">Loading...</div>
                </div>
            </div>

            <!-- Permissions Route -->
            <div class="route" id="route-permissions">
                <div class="empty-state">
                    <div class="empty-icon">🔐</div>
                    <div class="empty-text">Permission management coming soon</div>
                </div>
            </div>

            <!-- Settings Route -->
            <div class="route" id="route-settings">
                <div class="empty-state">
                    <div class="empty-icon">⚙️</div>
                    <div class="empty-text">System settings coming soon</div>
                </div>
            </div>
        </section>
    </main>
</div>

<!-- Edit User Modal -->
<div class="modal-overlay" id="editUserModal" style="display: none;">
    <div class="modal">
        <div class="modal-header">
            <h3>Edit User</h3>
            <button class="modal-close" onclick="closeEditUserModal()">&times;</button>
        </div>
        <div class="modal-body">
            <form id="editUserForm">
                <input type="hidden" id="editUserId">
                <input type="hidden" id="editUserRole">
                <div class="form-group">
                    <label>Username</label>
                    <input type="text" id="editUsername" placeholder="Enter username">
                </div>
                <div class="form-group">
                    <label>Name</label>
                    <input type="text" id="editName" placeholder="Enter name" disabled>
                </div>
                <div class="form-group">
                    <label>Email</label>
                    <input type="email" id="editEmail" placeholder="Enter email">
                </div>
                <div class="form-group">
                    <label>Phone</label>
                    <input type="text" id="editPhone" placeholder="Enter phone">
                </div>
                <div class="form-group">
                    <label>Department</label>
                    <input type="text" id="editDepartment" placeholder="Enter department">
                </div>
                <div class="form-group">
                    <label>Status</label>
                    <select id="editStatus">
                        <option value="active">Active</option>
                        <option value="inactive">Disabled</option>
                    </select>
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="btn secondary" onclick="closeEditUserModal()">Cancel</button>
            <button class="btn primary" onclick="saveUserEdit()">Save</button>
        </div>
    </div>
</div>

<script>
    const CONTEXT_PATH = '${pageContext.request.contextPath}';
</script>
<script src="${pageContext.request.contextPath}/assets/admin/js/admin-home.js"></script>
</body>
</html>
