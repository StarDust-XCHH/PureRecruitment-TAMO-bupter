/**
 * MO 个人资料管理模块
 *
 * 功能：
 * - 加载个人资料
 * - 更新个人资料（含头像上传）
 * - 修改密码
 * - 头像预览与管理
 */

(function () {
    'use strict';

    const API_BASE = window.CONTEXT_PATH || '';
    const PROFILE_API = `${API_BASE}/api/mo/profile-settings`;
    const ASSETS_BASE = `${API_BASE}/mo-assets`;

    let currentMoId = null;
    let selectedAvatarFile = null;

    /**
     * 初始化个人资料模块
     */
    function init() {
        bindEvents();
        loadMoIdFromStorage();
        if (currentMoId) {
            loadProfile();
        }
    }

    /**
     * 绑定事件监听器
     */
    function bindEvents() {
        const profileForm = document.getElementById('moProfileForm');
        const passwordForm = document.getElementById('moPasswordForm');
        const uploadAvatarBtn = document.getElementById('uploadAvatarBtn');
        const avatarFileInput = document.getElementById('avatarFileInput');

        if (profileForm) {
            profileForm.addEventListener('submit', handleProfileSubmit);
        }

        if (passwordForm) {
            passwordForm.addEventListener('submit', handlePasswordSubmit);
        }

        if (uploadAvatarBtn && avatarFileInput) {
            uploadAvatarBtn.addEventListener('click', () => avatarFileInput.click());
            avatarFileInput.addEventListener('change', handleAvatarSelect);
        }
    }

    /**
     * 从本地存储加载 MO ID
     */
    function loadMoIdFromStorage() {
        try {
            const userData = localStorage.getItem('moUser');
            if (userData) {
                const user = JSON.parse(userData);
                currentMoId = user.moId || user.id;

                const moIdField = document.getElementById('moIdField');
                const moIdPasswordField = document.getElementById('moIdPasswordField');
                if (moIdField) moIdField.value = currentMoId;
                if (moIdPasswordField) moIdPasswordField.value = currentMoId;
            }
        } catch (e) {
            console.error('[MO-PROFILE] 加载用户信息失败:', e);
        }
    }

    /**
     * 加载个人资料
     */
    async function loadProfile() {
        if (!currentMoId) return;

        try {
            const response = await fetch(`${PROFILE_API}?moId=${encodeURIComponent(currentMoId)}`);
            const result = await response.json();

            if (result.success && result.data) {
                populateProfileForm(result.data);
            } else {
                console.warn('[MO-PROFILE] 加载资料失败:', result.message);
            }
        } catch (error) {
            console.error('[MO-PROFILE] 网络请求失败:', error);
        }
    }

    /**
     * 填充表单数据
     */
    function populateProfileForm(data) {
        const fields = {
            realNameInput: data.realName || data.name || '',
            contactEmailInput: data.contactEmail || data.email || '',
            bioInput: data.bio || '',
            skillsInput: Array.isArray(data.skills) ? data.skills.join('\n') : ''
        };

        Object.entries(fields).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) element.value = value;
        });

        if (data.avatar) {
            showAvatarPreview(`${ASSETS_BASE}/${data.avatar}`);
        }

        updateSaveStatus('已加载');
    }

    /**
     * 处理头像选择
     */
    function handleAvatarSelect(event) {
        const file = event.target.files[0];
        if (!file) return;

        if (file.size > 10 * 1024 * 1024) {
            alert('头像大小不能超过 10MB');
            event.target.value = '';
            return;
        }

        selectedAvatarFile = file;
        const reader = new FileReader();
        reader.onload = (e) => {
            showAvatarPreview(e.target.result);
        };
        reader.readAsDataURL(file);
    }

    /**
     * 显示头像预览
     */
    function showAvatarPreview(src) {
        const preview = document.getElementById('avatarPreview');
        const placeholder = document.getElementById('avatarPlaceholder');

        if (preview && placeholder) {
            preview.src = src;
            preview.style.display = 'block';
            placeholder.style.display = 'none';
        }
    }

    /**
     * 处理资料提交
     */
    async function handleProfileSubmit(event) {
        event.preventDefault();

        if (!currentMoId) {
            alert('请先登录');
            return;
        }

        const formData = new FormData();
        formData.append('moId', currentMoId);
        formData.append('realName', document.getElementById('realNameInput').value);
        formData.append('contactEmail', document.getElementById('contactEmailInput').value);
        formData.append('bio', document.getElementById('bioInput').value);

        const skillsText = document.getElementById('skillsInput').value;
        if (skillsText.trim()) {
            const skills = skillsText.split(/[\n,]/).map(s => s.trim()).filter(s => s);
            skills.forEach(skill => formData.append('skills[]', skill));
        }

        if (selectedAvatarFile) {
            formData.append('avatarFile', selectedAvatarFile);
        }

        const statusEl = document.getElementById('saveProfileStatus');
        if (statusEl) statusEl.textContent = '保存中...';

        try {
            const response = await fetch(PROFILE_API, {
                method: 'POST',
                body: formData
            });
            const result = await response.json();

            if (result.success) {
                if (statusEl) statusEl.textContent = '✓ 保存成功';
                updateSaveStatus('已保存');
                selectedAvatarFile = null;
                setTimeout(() => {
                    if (statusEl) statusEl.textContent = '';
                }, 3000);
            } else {
                if (statusEl) statusEl.textContent = `✗ ${result.message}`;
                alert(`保存失败: ${result.message}`);
            }
        } catch (error) {
            console.error('[MO-PROFILE] 保存失败:', error);
            if (statusEl) statusEl.textContent = '✗ 网络错误';
            alert('保存失败，请检查网络连接');
        }
    }

    /**
     * 处理密码修改提交
     */
    async function handlePasswordSubmit(event) {
        event.preventDefault();

        if (!currentMoId) {
            alert('请先登录');
            return;
        }

        const currentPassword = document.getElementById('currentPasswordInput').value;
        const newPassword = document.getElementById('newPasswordInput').value;
        const confirmPassword = document.getElementById('confirmPasswordInput').value;

        if (!currentPassword || !newPassword || !confirmPassword) {
            alert('请填写所有密码字段');
            return;
        }

        if (newPassword !== confirmPassword) {
            alert('两次输入的新密码不一致');
            return;
        }

        if (newPassword.length < 6) {
            alert('新密码至少需要 6 位字符');
            return;
        }

        const statusEl = document.getElementById('changePasswordStatus');
        if (statusEl) statusEl.textContent = '更新中...';

        const formData = new FormData();
        formData.append('moId', currentMoId);
        formData.append('action', 'password');
        formData.append('currentPassword', currentPassword);
        formData.append('newPassword', newPassword);

        try {
            const response = await fetch(PROFILE_API, {
                method: 'POST',
                body: formData
            });
            const result = await response.json();

            if (result.success) {
                if (statusEl) statusEl.textContent = '✓ 密码已更新';
                document.getElementById('moPasswordForm').reset();
                setTimeout(() => {
                    if (statusEl) statusEl.textContent = '';
                }, 3000);
            } else {
                if (statusEl) statusEl.textContent = `✗ ${result.message}`;
                alert(`更新失败: ${result.message}`);
            }
        } catch (error) {
            console.error('[MO-PROFILE] 密码更新失败:', error);
            if (statusEl) statusEl.textContent = '✗ 网络错误';
            alert('更新失败，请检查网络连接');
        }
    }

    /**
     * 更新保存状态显示
     */
    function updateSaveStatus(status) {
        const statusEl = document.getElementById('profileSaveStatus');
        if (statusEl) {
            statusEl.textContent = status;
        }
    }

    window.MoProfileModule = {
        init,
        reload: loadProfile
    };
})();
