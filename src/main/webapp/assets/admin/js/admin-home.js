(function () {
    // DOM Elements
    const adminApp = document.getElementById('adminApp');
    const navItems = document.querySelectorAll('.nav-item');
    const routes = document.querySelectorAll('.route');
    const pageTitle = document.getElementById('pageTitle');
    const logoutBtn = document.getElementById('logoutBtn');
    const refreshBtn = document.getElementById('refreshBtn');
    const userSearch = document.getElementById('userSearch');
    const roleFilter = document.getElementById('roleFilter');
    const createNoticeBtn = document.getElementById('createNoticeBtn');

    // Mock Data
    const mockUsers = [
        { id: 'TA-10001', username: 'zhangsan', email: 'zhangsan@bupt.edu.cn', role: 'TA', status: 'active', lastLogin: '2026-03-21 10:30' },
        { id: 'TA-10002', username: 'lisi', email: 'lisi@bupt.edu.cn', role: 'TA', status: 'active', lastLogin: '2026-03-21 09:15' },
        { id: 'MO-20001', username: 'wangwu', email: 'wangwu@bupt.edu.cn', role: 'MO', status: 'active', lastLogin: '2026-03-21 11:00' },
        { id: 'TA-10003', username: 'zhaoliu', email: 'zhaoliu@bupt.edu.cn', role: 'TA', status: 'pending', lastLogin: null },
        { id: 'MO-20002', username: 'qianqi', email: 'qianqi@bupt.edu.cn', role: 'MO', status: 'active', lastLogin: '2026-03-20 16:45' },
    ];

    const mockNotices = [
        { id: 1, title: '系统维护通知', content: '系统将于今晚 22:00-24:00 进行维护升级，请提前保存工作。', priority: 'high', createdAt: '2026-03-21 10:00' },
        { id: 2, title: '春季学期 TA 招募开始', content: '春季学期 TA 招募已正式开始，请各位 MO 及时发布岗位信息。', priority: 'medium', createdAt: '2026-03-20 14:30' },
        { id: 3, title: '新功能上线', content: '管理员后台新增数据统计功能，欢迎体验反馈。', priority: 'low', createdAt: '2026-03-19 09:00' },
    ];

    // Initialize
    function init() {
        setupNavigation();
        setupEventListeners();
        loadDashboardData();
        loadUsers();
        loadNotices();
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
        // Update active nav item
        navItems.forEach(nav => {
            nav.classList.toggle('active', nav.dataset.route === route);
        });

        // Update active route
        routes.forEach(r => {
            r.classList.toggle('active', r.id === `route-${route}`);
        });

        // Update page title
        const titles = {
            'dashboard': '数据看板',
            'users': '用户管理',
            'notices': '公告管理',
            'permissions': '权限管理',
            'settings': '系统设置'
        };
        pageTitle.textContent = titles[route] || '控制台';
    }

    // Event Listeners
    function setupEventListeners() {
        logoutBtn.addEventListener('click', handleLogout);
        refreshBtn.addEventListener('click', refreshData);
        userSearch.addEventListener('input', debounce(filterUsers, 300));
        roleFilter.addEventListener('change', filterUsers);
        createNoticeBtn.addEventListener('click', showCreateNoticeModal);
    }

    // Dashboard
    function loadDashboardData() {
        document.getElementById('totalUsers').textContent = mockUsers.length;
        document.getElementById('taUsers').textContent = mockUsers.filter(u => u.role === 'TA').length;
        document.getElementById('moUsers').textContent = mockUsers.filter(u => u.role === 'MO').length;
        document.getElementById('activeJobs').textContent = Math.floor(Math.random() * 20) + 10;

        // Recent logins
        const recentLogins = mockUsers
            .filter(u => u.lastLogin)
            .sort((a, b) => new Date(b.lastLogin) - new Date(a.lastLogin))
            .slice(0, 5);

        const tbody = document.getElementById('recentLogins');
        if (recentLogins.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3">暂无数据</td></tr>';
        } else {
            tbody.innerHTML = recentLogins.map(user => `
                <tr>
                    <td>${user.username}</td>
                    <td><span class="role-badge ${user.role.toLowerCase()}">${user.role}</span></td>
                    <td>${user.lastLogin}</td>
                </tr>
            `).join('');
        }

        // Recent notices
        const noticeList = document.getElementById('noticeList');
        noticeList.innerHTML = mockNotices.slice(0, 2).map(notice => createNoticeCard(notice)).join('');
    }

    // Users
    function loadUsers() {
        const tbody = document.getElementById('userTable');
        tbody.innerHTML = mockUsers.map(user => createUserRow(user)).join('');
    }

    function filterUsers() {
        const searchTerm = userSearch.value.toLowerCase();
        const role = roleFilter.value;

        let filtered = mockUsers;

        if (searchTerm) {
            filtered = filtered.filter(u =>
                u.username.toLowerCase().includes(searchTerm) ||
                u.email.toLowerCase().includes(searchTerm)
            );
        }

        if (role) {
            filtered = filtered.filter(u => u.role === role);
        }

        const tbody = document.getElementById('userTable');
        tbody.innerHTML = filtered.map(user => createUserRow(user)).join('');
    }

    function createUserRow(user) {
        const statusClass = user.status === 'active' ? 'active' : user.status === 'pending' ? 'pending' : 'inactive';
        const statusText = user.status === 'active' ? '正常' : user.status === 'pending' ? '待审核' : '禁用';

        return `
            <tr>
                <td class="user-cell-id">${user.id}</td>
                <td>${user.username}</td>
                <td>${user.email}</td>
                <td><span class="role-badge ${user.role.toLowerCase()}">${user.role}</span></td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>
                    <div class="action-buttons">
                        <button class="btn sm secondary" onclick="editUser('${user.id}')">编辑</button>
                        <button class="btn sm ${user.status === 'active' ? 'danger' : 'primary'}" onclick="toggleUserStatus('${user.id}')">
                            ${user.status === 'active' ? '禁用' : '启用'}
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    // Notices
    function loadNotices() {
        const noticeList = document.getElementById('allNotices');
        noticeList.innerHTML = mockNotices.map(notice => createNoticeCard(notice)).join('');
    }

    function createNoticeCard(notice) {
        return `
            <div class="notice-card">
                <div class="notice-header">
                    <div>
                        <div class="notice-title">${notice.title}</div>
                        <div class="notice-meta">${notice.createdAt}</div>
                    </div>
                    <span class="notice-priority ${notice.priority}">${notice.priority.toUpperCase()}</span>
                </div>
                <div class="notice-content">${notice.content}</div>
                <div class="notice-footer">
                    <span class="notice-meta">管理员</span>
                    <div class="notice-actions">
                        <button class="btn sm secondary" onclick="editNotice(${notice.id})">编辑</button>
                        <button class="btn sm danger" onclick="deleteNotice(${notice.id})">删除</button>
                    </div>
                </div>
            </div>
        `;
    }

    // Actions
    function handleLogout() {
        if (confirm('确定要退出登录吗？')) {
            window.location.href = '${pageContext.request.contextPath}/index.jsp';
        }
    }

    function refreshData() {
        alert('数据已刷新');
        loadDashboardData();
        loadUsers();
        loadNotices();
    }

    function showCreateNoticeModal() {
        alert('创建公告功能开发中');
    }

    // Global functions (for onclick attributes)
    window.editUser = function(id) {
        alert(`编辑用户 ${id} 功能开发中`);
    };

    window.toggleUserStatus = function(id) {
        if (confirm('确定要更改用户状态吗？')) {
            alert(`用户 ${id} 状态已更改`);
        }
    };

    window.editNotice = function(id) {
        alert(`编辑公告 ${id} 功能开发中`);
    };

    window.deleteNotice = function(id) {
        if (confirm('确定要删除这条公告吗？')) {
            alert(`公告 ${id} 已删除`);
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

    // Start
    init();
})();
