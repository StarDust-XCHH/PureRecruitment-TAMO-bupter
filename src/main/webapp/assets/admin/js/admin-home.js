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
        { id: 1, title: 'Maintenance notice', content: 'Maintenance is scheduled tonight 22:00-24:00. Please save your work.', priority: 'high', createdAt: '2026-03-21 10:00' },
        { id: 2, title: 'Spring TA recruitment open', content: 'Spring TA recruitment is open—MOs should publish postings soon.', priority: 'medium', createdAt: '2026-03-20 14:30' },
        { id: 3, title: 'New feature', content: 'The admin console now includes basic analytics—feedback welcome.', priority: 'low', createdAt: '2026-03-19 09:00' },
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
            'dashboard': 'Dashboard',
            'users': 'User management',
            'notices': 'Notices',
            'permissions': 'Permissions',
            'settings': 'System settings'
        };
        pageTitle.textContent = titles[route] || 'Console';
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
            tbody.innerHTML = '<tr><td colspan="3">No data</td></tr>';
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
        const statusText = user.status === 'active' ? 'Active' : user.status === 'pending' ? 'Pending review' : 'Suspended';

        return `
            <tr>
                <td class="user-cell-id">${user.id}</td>
                <td>${user.username}</td>
                <td>${user.email}</td>
                <td><span class="role-badge ${user.role.toLowerCase()}">${user.role}</span></td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>
                    <div class="action-buttons">
                        <button class="btn sm secondary" onclick="editUser('${user.id}')">Edit</button>
                        <button class="btn sm ${user.status === 'active' ? 'danger' : 'primary'}" onclick="toggleUserStatus('${user.id}')">
                            ${user.status === 'active' ? 'Suspended' : 'Enable'}
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
                    <span class="notice-meta">Admin</span>
                    <div class="notice-actions">
                        <button class="btn sm secondary" onclick="editNotice(${notice.id})">Edit</button>
                        <button class="btn sm danger" onclick="deleteNotice(${notice.id})">Delete</button>
                    </div>
                </div>
            </div>
        `;
    }

    // Actions
    function handleLogout() {
        if (confirm('Sign out?')) {
            window.location.href = '${pageContext.request.contextPath}/index.jsp';
        }
    }

    function refreshData() {
        alert('Data refreshed');
        loadDashboardData();
        loadUsers();
        loadNotices();
    }

    function showCreateNoticeModal() {
        alert('Creating notices is not available yet');
    }

    // Global functions (for onclick attributes)
    window.editUser = function(id) {
        alert(`Edit user ${id} is not available yet`);
    };

    window.toggleUserStatus = function(id) {
        if (confirm('Change user status?')) {
            alert(`User ${id} status updated`);
        }
    };

    window.editNotice = function(id) {
        alert(`Edit notice ${id} is not available yet`);
    };

    window.deleteNotice = function(id) {
        if (confirm('Delete this notice?')) {
            alert(`Notice ${id} deleted`);
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
