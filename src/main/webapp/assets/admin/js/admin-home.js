(function () {
    // API Base URL - from JSP global variable
    const API_BASE = typeof CONTEXT_PATH !== 'undefined' ? CONTEXT_PATH : '';

    // DOM Elements
    const adminApp = document.getElementById('adminApp');
    const navItems = document.querySelectorAll('.nav-item');
    const routes = document.querySelectorAll('.route');
    const pageTitle = document.getElementById('pageTitle');
    const logoutBtn = document.getElementById('logoutBtn');
    const refreshBtn = document.getElementById('refreshBtn');
    const userSearch = document.getElementById('userSearch');
    const roleFilter = document.getElementById('roleFilter');
    const refreshUsersBtn = document.getElementById('refreshUsersBtn');
    const createNoticeBtn = document.getElementById('createNoticeBtn');
    const courseSearch = document.getElementById('courseSearch');
    const courseStatusFilter = document.getElementById('courseStatusFilter');

    // Current state
    let allUsers = [];
    let allCourses = [];
    let allNotices = [];
    let currentAdmin = null;

    // Initialize
    function init() {
        // 加载当前登录用户信息
        loadCurrentAdmin();
        setupNavigation();
        setupEventListeners();
        loadDashboardData();
    }

    // 加载当前登录的管理员信息
    async function loadCurrentAdmin() {
        // 尝试从 localStorage 获取登录信息
        const storedUser = localStorage.getItem('admin-user');
        if (!storedUser) {
            // 如果没有登录信息，跳转到登录页
            window.location.href = API_BASE + '/';
            return;
        }

        try {
            const user = JSON.parse(storedUser);
            currentAdmin = user;

            // 更新侧边栏显示
            updateAdminDisplay(user);

            // 调用API获取完整信息（包含profile）
            const response = await fetch(`${API_BASE}/api/admin/auth?action=me&adminId=${user.id}&username=${user.username}`);
            if (response.ok) {
                const data = await response.json();
                if (data.success && data.data) {
                    currentAdmin = data.data;
                    updateAdminDisplay(currentAdmin);
                    // 更新本地存储
                    localStorage.setItem('admin-user', JSON.stringify(currentAdmin));
                }
            }
        } catch (error) {
            console.error('加载用户信息失败:', error);
        }
    }

    // 更新侧边栏显示
    function updateAdminDisplay(user) {
        if (!user) return;

        // 更新侧边栏管理员信息
        const adminNameEl = document.getElementById('adminName');
        const adminRoleEl = document.getElementById('adminRole');
        const avatarEl = document.querySelector('.user-info .avatar');

        if (adminNameEl) {
            adminNameEl.textContent = user.name || user.username || '管理员';
        }
        if (adminRoleEl) {
            const title = user.title || '管理员';
            adminRoleEl.textContent = title;
        }
        if (avatarEl) {
            // 显示用户名首字母
            const displayName = user.name || user.username || 'A';
            avatarEl.textContent = displayName.charAt(0).toUpperCase();
        }
    }

    // Navigation
    function setupNavigation() {
        navItems.forEach(item => {
            item.addEventListener('click', () => {
                const route = item.dataset.route;
                navigateTo(route);
            });
        });
    }

    function navigateTo(route) {
        navItems.forEach(nav => {
            nav.classList.toggle('active', nav.dataset.route === route);
        });

        routes.forEach(r => {
            r.classList.toggle('active', r.id === `route-${route}`);
        });

        const titles = {
            'dashboard': '数据看板',
            'users': '用户管理',
            'courses': '课程管理',
            'notices': '公告管理',
            'permissions': '权限管理',
            'settings': '系统设置'
        };
        pageTitle.textContent = titles[route] || '控制台';

        // Load data for specific routes
        if (route === 'users') loadUsers();
        if (route === 'courses') loadCourses();
        if (route === 'notices') loadNotices();
    }

    // Event Listeners
    function setupEventListeners() {
        logoutBtn.addEventListener('click', handleLogout);
        refreshBtn.addEventListener('click', loadDashboardData);
        userSearch.addEventListener('input', debounce(filterUsers, 300));
        roleFilter.addEventListener('change', filterUsers);
        refreshUsersBtn.addEventListener('click', loadUsers);
        createNoticeBtn.addEventListener('click', showCreateNoticeModal);
        courseSearch.addEventListener('input', debounce(filterCourses, 300));
        courseStatusFilter.addEventListener('change', filterCourses);
    }

    // API Helpers
    async function fetchAPI(url, options = {}) {
        try {
            const response = await fetch(url, {
                ...options,
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers
                }
            });
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('API Error:', error);
            return null;
        }
    }

    // Dashboard
    async function loadDashboardData() {
        document.getElementById('totalUsers').textContent = '-';
        document.getElementById('taUsers').textContent = '-';
        document.getElementById('moUsers').textContent = '-';
        document.getElementById('activeJobs').textContent = '-';

        // Load recent logins
        const recentLogins = document.getElementById('recentLogins');
        recentLogins.innerHTML = '<tr><td colspan="3" class="loading">加载中...</td></tr>';

        // Load user stats
        loadUserStats();

        // Load recent notices
        loadNoticesBrief();
    }

    async function loadUserStats() {
        const data = await fetchAPI(`${API_BASE}/api/admin/users/stats`);
        if (data && data.success && data.data) {
            document.getElementById('totalUsers').textContent = data.data.totalUsers || 0;
            document.getElementById('taUsers').textContent = data.data.taUsers || 0;
            document.getElementById('moUsers').textContent = data.data.moUsers || 0;
            document.getElementById('activeJobs').textContent = data.data.adminUsers || 0;

            // 显示最近登录用户
            const usersData = await fetchAPI(`${API_BASE}/api/admin/users`);
            if (usersData && usersData.success) {
                const recentUsers = usersData.data
                    .filter(u => u.lastLoginAt && u.lastLoginAt !== '')
                    .sort((a, b) => {
                        if (!a.lastLoginAt) return 1;
                        if (!b.lastLoginAt) return -1;
                        return new Date(b.lastLoginAt) - new Date(a.lastLoginAt);
                    })
                    .slice(0, 5);

                const tbody = document.getElementById('recentLogins');
                if (recentUsers.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="3" class="empty">暂无数据</td></tr>';
                } else {
                    tbody.innerHTML = recentUsers.map(user => `
                        <tr>
                            <td>${user.username || '-'}</td>
                            <td><span class="role-badge ${(user.role || '').toLowerCase()}">${user.role || '-'}</span></td>
                            <td>${formatDate(user.lastLoginAt)}</td>
                        </tr>
                    `).join('');
                }
            }
        }
    }

    async function loadNoticesBrief() {
        const noticeList = document.getElementById('noticeList');
        noticeList.innerHTML = '<div class="loading-state">加载中...</div>';

        const data = await fetchAPI(`${API_BASE}/api/admin/notices?limit=5`);
        if (data && data.success) {
            if (data.data && data.data.length > 0) {
                noticeList.innerHTML = data.data.map(notice => createNoticeCard(notice)).join('');
            } else {
                noticeList.innerHTML = '<div class="empty-state">暂无公告</div>';
            }
        } else {
            noticeList.innerHTML = '<div class="empty-state">暂无公告</div>';
        }
    }

    // Users
    async function loadUsers() {
        const tbody = document.getElementById('userTable');
        tbody.innerHTML = '<tr><td colspan="6" class="loading">加载中...</td></tr>';

        const data = await fetchAPI(`${API_BASE}/api/admin/users`);
        if (data && data.success) {
            allUsers = data.data || [];
            renderUsers(allUsers);
            updateUserStats();
        } else {
            tbody.innerHTML = '<tr><td colspan="6" class="error">加载失败</td></tr>';
        }
    }

    function renderUsers(users) {
        const tbody = document.getElementById('userTable');
        if (!users || users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="empty">暂无数据</td></tr>';
            return;
        }

        tbody.innerHTML = users.map(user => createUserRow(user)).join('');
    }

    function updateUserStats() {
        const total = allUsers.length;
        const taCount = allUsers.filter(u => u.role === 'TA').length;
        const moCount = allUsers.filter(u => u.role === 'MO').length;

        document.getElementById('totalUsers').textContent = total;
        document.getElementById('taUsers').textContent = taCount;
        document.getElementById('moUsers').textContent = moCount;
    }

    function filterUsers() {
        const searchTerm = userSearch.value.toLowerCase();
        const role = roleFilter.value;

        let filtered = allUsers;

        if (searchTerm) {
            filtered = filtered.filter(u =>
                (u.username && u.username.toLowerCase().includes(searchTerm)) ||
                (u.email && u.email.toLowerCase().includes(searchTerm)) ||
                (u.name && u.name.toLowerCase().includes(searchTerm))
            );
        }

        if (role) {
            filtered = filtered.filter(u => u.role === role);
        }

        renderUsers(filtered);
    }

    function createUserRow(user) {
        const statusClass = user.status === 'active' ? 'active' : user.status === 'pending' ? 'pending' : 'inactive';
        const statusText = user.status === 'active' ? '正常' : user.status === 'pending' ? '待审核' : '禁用';

        return `
            <tr>
                <td class="user-cell-id">${user.id || user.username || '-'}</td>
                <td>${user.username || '-'}</td>
                <td>${user.email || '-'}</td>
                <td><span class="role-badge ${(user.role || '').toLowerCase()}">${user.role || '-'}</span></td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>
                    <div class="action-buttons">
                        <button class="btn sm secondary" onclick="editUser('${user.id || user.username}')">编辑</button>
                        <button class="btn sm danger" onclick="deleteUser('${user.id || user.username}')">
                            删除
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    // Courses
    async function loadCourses() {
        const tbody = document.getElementById('courseTable');
        tbody.innerHTML = '<tr><td colspan="7" class="loading">加载中...</td></tr>';

        const data = await fetchAPI(`${API_BASE}/api/admin/courses`);
        if (data && data.success) {
            allCourses = data.data || [];
            renderCourses(allCourses);
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="error">加载失败</td></tr>';
        }
    }

    function renderCourses(courses) {
        const tbody = document.getElementById('courseTable');
        if (!courses || courses.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="empty">暂无数据</td></tr>';
            return;
        }

        tbody.innerHTML = courses.map(course => createCourseRow(course)).join('');
    }

    function filterCourses() {
        const searchTerm = courseSearch.value.toLowerCase();
        const status = courseStatusFilter.value;

        let filtered = allCourses;

        if (searchTerm) {
            filtered = filtered.filter(c =>
                (c.courseId && c.courseId.toLowerCase().includes(searchTerm)) ||
                (c.courseName && c.courseName.toLowerCase().includes(searchTerm)) ||
                (c.moName && c.moName.toLowerCase().includes(searchTerm))
            );
        }

        if (status) {
            filtered = filtered.filter(c => c.status === status);
        }

        renderCourses(filtered);
    }

    function createCourseRow(course) {
        const statusClass = course.status === 'OPEN' ? 'active' : 'inactive';
        const statusText = course.status === 'OPEN' ? '开放' : '关闭';

        return `
            <tr>
                <td class="course-cell-id">${course.courseId || '-'}</td>
                <td>${course.courseName || '-'}</td>
                <td>${course.semester || '-'}</td>
                <td>${course.moName || '-'}</td>
                <td>${course.taRecruitCount || 0}</td>
                <td>${course.applicationsTotal || 0}</td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>
                    <div class="action-buttons">
                        <button class="btn sm secondary" onclick="viewCourse('${course.courseId}')">详情</button>
                    </div>
                </td>
            </tr>
        `;
    }

    window.viewCourse = function(courseId) {
        const course = allCourses.find(c => c.courseId === courseId);
        if (!course) return;

        const skills = (course.skills || []).map(s => `<span class="skill-tag">${s}</span>`).join('');
        const deadline = course.applicationDeadline ? formatDate(course.applicationDeadline) : '-';

        alert(`课程详情：
课程代码：${course.courseId}
课程名称：${course.courseName}
学期：${course.semester}
负责MO：${course.moName}
校区：${course.campus || '-'}
学生数：${course.studentCount || 0}
TA招募数：${course.taRecruitCount || 0}
申请截止：${deadline}
技能要求：${skills || '无'}
申请状态：${course.applicationsTotal || 0}人申请
已招募：${course.recruitedCount || 0}人`);
    };

    // Notices
    async function loadNotices() {
        const noticeList = document.getElementById('allNotices');
        noticeList.innerHTML = '<div class="loading-state">加载中...</div>';

        const data = await fetchAPI(`${API_BASE}/api/admin/notices`);
        if (data && data.success) {
            allNotices = data.data || [];
            if (allNotices.length > 0) {
                noticeList.innerHTML = allNotices.map(notice => createNoticeCard(notice)).join('');
            } else {
                noticeList.innerHTML = '<div class="empty-state">暂无公告</div>';
            }
        } else {
            noticeList.innerHTML = '<div class="empty-state">暂无公告</div>';
        }
    }

    function createNoticeCard(notice) {
        const priorityClass = notice.priority || 'low';
        const priorityText = (notice.priority || 'low').toUpperCase();
        const priorityLabel = priorityText === 'HIGH' ? '高' : priorityText === 'MEDIUM' ? '中' : '低';

        return `
            <div class="notice-card">
                <div class="notice-header">
                    <div>
                        <div class="notice-title">${notice.title || '-'}</div>
                        <div class="notice-meta">${notice.createdAt || notice.createTime || '-'}</div>
                    </div>
                    <span class="notice-priority ${priorityClass}">${priorityLabel}</span>
                </div>
                <div class="notice-content">${notice.content || '-'}</div>
                <div class="notice-footer">
                    <span class="notice-meta">${notice.authorName || notice.author || '管理员'}</span>
                    <div class="notice-actions">
                        <button class="btn sm secondary" onclick="editNotice(${notice.id})">编辑</button>
                        <button class="btn sm danger" onclick="deleteNotice(${notice.id})">删除</button>
                    </div>
                </div>
            </div>
        `;
    }

    // Modals
    function showCreateNoticeModal() {
        const title = prompt('请输入公告标题:');
        if (!title) return;

        const content = prompt('请输入公告内容:');
        if (!content) return;

        const priority = prompt('优先级 (high/medium/low，默认为 medium):') || 'medium';

        createNotice({ title, content, priority });
    }

    // CRUD Operations
    async function createNotice(notice) {
        const data = await fetchAPI(`${API_BASE}/api/admin/notices`, {
            method: 'POST',
            body: JSON.stringify(notice)
        });
        if (data && data.success) {
            alert('公告创建成功');
            loadNotices();
            loadNoticesBrief();
        } else {
            alert('公告创建失败');
        }
    }

    async function deleteUser(userId) {
        if (!confirm('确定要删除该用户吗？')) return;
        alert('删除用户功能待开发');
    }

    // Actions
    function handleLogout() {
        if (confirm('确定要退出登录吗？')) {
            // 清除登录信息
            localStorage.removeItem('admin-user');
            sessionStorage.clear();
            // 跳转到登录页面
            window.location.href = API_BASE + '/';
        }
    }

    // Global functions (for onclick attributes)
    window.editUser = function(id) {
        alert(`编辑用户 ${id} 功能待开发`);
    };

    window.deleteUser = deleteUser;

    window.editNotice = function(id) {
        alert(`编辑公告 ${id} 功能待开发`);
    };

    window.deleteNotice = async function(id) {
        if (!confirm('确定要删除这条公告吗？')) return;

        const data = await fetchAPI(`${API_BASE}/api/admin/notices/${id}`, {
            method: 'DELETE'
        });
        if (data && data.success) {
            alert('公告已删除');
            loadNotices();
            loadNoticesBrief();
        } else {
            alert('删除失败');
        }
    };

    // Utility
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    function formatDate(dateStr) {
        if (!dateStr) return '-';
        try {
            const date = new Date(dateStr);
            if (isNaN(date.getTime())) return dateStr;
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            return dateStr;
        }
    }

    // Start
    init();
})();
