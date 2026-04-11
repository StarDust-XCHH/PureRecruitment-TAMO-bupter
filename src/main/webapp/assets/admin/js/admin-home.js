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
        // Load current logged-in user info
        loadCurrentAdmin();
        setupNavigation();
        setupEventListeners();
        loadDashboardData();
    }

    // Load current logged-in admin info
    async function loadCurrentAdmin() {
        // Try to get login info from localStorage
        const storedUser = localStorage.getItem('admin-user');
        if (!storedUser) {
            // If no login info, redirect to login page
            window.location.href = API_BASE + '/';
            return;
        }

        try {
            const user = JSON.parse(storedUser);
            currentAdmin = user;

            // Update sidebar display
            updateAdminDisplay(user);

            // Call API to get full info (including profile)
            const response = await fetch(`${API_BASE}/api/admin/auth?action=me&adminId=${user.id}&username=${user.username}`);
            if (response.ok) {
                const data = await response.json();
                if (data.success && data.data) {
                    currentAdmin = data.data;
                    updateAdminDisplay(currentAdmin);
                    // Update localStorage
                    localStorage.setItem('admin-user', JSON.stringify(currentAdmin));
                }
            }
        } catch (error) {
            console.error('Failed to load user info:', error);
        }
    }

    // Update sidebar display
    function updateAdminDisplay(user) {
        if (!user) return;

        // Update sidebar admin info
        const adminNameEl = document.getElementById('adminName');
        const adminRoleEl = document.getElementById('adminRole');
        const avatarEl = document.querySelector('.user-info .avatar');

        if (adminNameEl) {
            adminNameEl.textContent = user.name || user.username || 'Admin';
        }
        if (adminRoleEl) {
            const title = user.title || 'Admin';
            adminRoleEl.textContent = title;
        }
        if (avatarEl) {
            // Display first letter of username
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
            'dashboard': 'Dashboard',
            'users': 'Users',
            'courses': 'Courses',
            'notices': 'Notices',
            'permissions': 'Permissions',
            'settings': 'Settings'
        };
        pageTitle.textContent = titles[route] || 'Console';

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
        recentLogins.innerHTML = '<tr><td colspan="3" class="loading">Loading...</td></tr>';

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

            // Show recent login users
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
                    tbody.innerHTML = '<tr><td colspan="3" class="empty">No data</td></tr>';
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
        noticeList.innerHTML = '<div class="loading-state">Loading...</div>';

        const data = await fetchAPI(`${API_BASE}/api/admin/notices?limit=5`);
        if (data && data.success) {
            if (data.data && data.data.length > 0) {
                noticeList.innerHTML = data.data.map(notice => createNoticeCard(notice)).join('');
            } else {
                noticeList.innerHTML = '<div class="empty-state">No notices</div>';
            }
        } else {
            noticeList.innerHTML = '<div class="empty-state">No notices</div>';
        }
    }

    // Users
    async function loadUsers() {
        const tbody = document.getElementById('userTable');
        tbody.innerHTML = '<tr><td colspan="6" class="loading">Loading...</td></tr>';

        const data = await fetchAPI(`${API_BASE}/api/admin/users`);
        if (data && data.success) {
            allUsers = data.data || [];
            renderUsers(allUsers);
            updateUserStats();
        } else {
            tbody.innerHTML = '<tr><td colspan="6" class="error">Failed to load</td></tr>';
        }
    }

    function renderUsers(users) {
        const tbody = document.getElementById('userTable');
        if (!users || users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="empty">No data</td></tr>';
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
        const statusText = user.status === 'active' ? 'Active' : user.status === 'pending' ? 'Pending' : 'Inactive';
        const userId = user.id || user.username || '';

        return `
            <tr data-user-id="${userId}" data-user-role="${user.role || ''}">
                <td class="user-cell-id">${userId}</td>
                <td>${user.username || '-'}</td>
                <td>${user.email || '-'}</td>
                <td><span class="role-badge ${(user.role || '').toLowerCase()}">${user.role || '-'}</span></td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>
                    <div class="action-buttons">
                        <button class="btn sm secondary" onclick="editUser('${userId}')">Edit</button>
                        <button class="btn sm danger" onclick="deleteUser('${userId}')">
                            Delete
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    // Courses
    async function loadCourses() {
        const tbody = document.getElementById('courseTable');
        tbody.innerHTML = '<tr><td colspan="7" class="loading">Loading...</td></tr>';

        const data = await fetchAPI(`${API_BASE}/api/admin/courses`);
        if (data && data.success) {
            allCourses = data.data || [];
            renderCourses(allCourses);
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="error">Failed to load</td></tr>';
        }
    }

    function renderCourses(courses) {
        const tbody = document.getElementById('courseTable');
        if (!courses || courses.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="empty">No data</td></tr>';
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
        const statusText = course.status === 'OPEN' ? 'Open' : 'Closed';

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
                        <button class="btn sm secondary" onclick="viewCourse('${course.courseId}')">View</button>
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

        alert(`Course Details:
Course Code: ${course.courseId}
Course Name: ${course.courseName}
Semester: ${course.semester}
MO: ${course.moName}
Campus: ${course.campus || '-'}
Students: ${course.studentCount || 0}
TA Count: ${course.taRecruitCount || 0}
Deadline: ${deadline}
Skills: ${skills || 'None'}
Applications: ${course.applicationsTotal || 0}
Recruited: ${course.recruitedCount || 0}`);
    };

    // Notices
    async function loadNotices() {
        const noticeList = document.getElementById('allNotices');
        noticeList.innerHTML = '<div class="loading-state">Loading...</div>';

        const data = await fetchAPI(`${API_BASE}/api/admin/notices`);
        if (data && data.success) {
            allNotices = data.data || [];
            if (allNotices.length > 0) {
                noticeList.innerHTML = allNotices.map(notice => createNoticeCard(notice)).join('');
            } else {
                noticeList.innerHTML = '<div class="empty-state">No notices</div>';
            }
        } else {
            noticeList.innerHTML = '<div class="empty-state">No notices</div>';
        }
    }

    function createNoticeCard(notice) {
        const priorityClass = notice.priority || 'low';
        const priorityText = (notice.priority || 'low').toUpperCase();
        const priorityLabel = priorityText === 'HIGH' ? 'High' : priorityText === 'MEDIUM' ? 'Medium' : 'Low';

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
                    <span class="notice-meta">${notice.authorName || notice.author || 'Admin'}</span>
                    <div class="notice-actions">
                        <button class="btn sm secondary" onclick="editNotice(${notice.id})">Edit</button>
                        <button class="btn sm danger" onclick="deleteNotice(${notice.id})">Delete</button>
                    </div>
                </div>
            </div>
        `;
    }

    // Modals
    function showCreateNoticeModal() {
        const title = prompt('Enter notice title:');
        if (!title) return;

        const content = prompt('Enter notice content:');
        if (!content) return;

        const priority = prompt('Priority (high/medium/low, default: medium):') || 'medium';

        createNotice({ title, content, priority });
    }

    // CRUD Operations
    async function createNotice(notice) {
        const data = await fetchAPI(`${API_BASE}/api/admin/notices`, {
            method: 'POST',
            body: JSON.stringify(notice)
        });
        if (data && data.success) {
            alert('Notice created successfully');
            loadNotices();
            loadNoticesBrief();
        } else {
            alert('Failed to create notice');
        }
    }

    async function deleteUser(userId) {
        if (!confirm('Are you sure you want to delete this user?')) return;
        alert('Delete user feature coming soon');
    }

    // Actions
    function handleLogout() {
        if (confirm('Are you sure you want to logout?')) {
            // Clear login info
            localStorage.removeItem('admin-user');
            sessionStorage.clear();
            // Redirect to login page
            window.location.href = API_BASE + '/';
        }
    }

    // Global functions (for onclick attributes)
    window.editUser = function(userId) {
        const user = allUsers.find(u => (u.id || u.username) === userId);
        if (!user) {
            alert('User not found');
            return;
        }
        showEditUserModal(user);
    };

    window.deleteUser = deleteUser;

    window.editNotice = function(id) {
        alert(`Edit notice ${id} feature coming soon`);
    };

    window.deleteNotice = async function(id) {
        if (!confirm('Are you sure you want to delete this notice?')) return;

        const data = await fetchAPI(`${API_BASE}/api/admin/notices/${id}`, {
            method: 'DELETE'
        });
        if (data && data.success) {
            alert('Notice deleted');
            loadNotices();
            loadNoticesBrief();
        } else {
            alert('Failed to delete');
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

    // User Edit Modal
    window.showEditUserModal = function(user) {
        document.getElementById('editUserId').value = user.id || '';
        document.getElementById('editUserRole').value = user.role || '';
        document.getElementById('editUsername').value = user.username || '';
        document.getElementById('editName').value = user.name || user.realName || '';
        document.getElementById('editEmail').value = user.email || user.contactEmail || '';
        document.getElementById('editPhone').value = user.phone || '';
        document.getElementById('editDepartment').value = user.department || '';
        document.getElementById('editStatus').value = user.status || 'active';

        document.getElementById('editUserModal').style.display = 'flex';
    };

    window.closeEditUserModal = function() {
        document.getElementById('editUserModal').style.display = 'none';
        document.getElementById('editUserForm').reset();
    };

    window.saveUserEdit = async function() {
        const userId = document.getElementById('editUserId').value;
        const role = document.getElementById('editUserRole').value;

        if (!userId) {
            alert('User ID cannot be empty');
            return;
        }

        const updates = {
            username: document.getElementById('editUsername').value.trim(),
            name: document.getElementById('editName').value.trim(),
            email: document.getElementById('editEmail').value.trim(),
            phone: document.getElementById('editPhone').value.trim(),
            department: document.getElementById('editDepartment').value.trim(),
            status: document.getElementById('editStatus').value,
            role: role
        };

        // Remove empty strings
        Object.keys(updates).forEach(key => {
            if (updates[key] === '') {
                delete updates[key];
            }
        });

        try {
            const response = await fetch(`${API_BASE}/api/admin/users/${userId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(updates)
            });

            const data = await response.json();

            if (data && data.success) {
                alert('User updated successfully');
                closeEditUserModal();
                loadUsers();
            } else {
                alert('Update failed: ' + (data.message || 'Unknown error'));
            }
        } catch (error) {
            console.error('Failed to update user:', error);
            alert('Update failed, please try again');
        }
    };

    // Click outside modal to close
    document.getElementById('editUserModal')?.addEventListener('click', function(e) {
        if (e.target === this) {
            closeEditUserModal();
        }
    });

    // Start
    init();
})();
