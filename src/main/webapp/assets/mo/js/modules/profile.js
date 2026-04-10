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

    const PROFILE_API = (typeof window.moApiPath === 'function')
        ? window.moApiPath('/api/mo/profile-settings')
        : '../../api/mo/profile-settings';
    const ASSETS_BASE = (typeof window.moApiPath === 'function')
        ? window.moApiPath('/mo-assets')
        : '../../mo-assets';

    let currentMoId = null;
    let selectedAvatarFile = null;

    console.log('[MO-PROFILE] 模块加载完成，API地址:', PROFILE_API);

    /**
     * 初始化个人资料模块
     */
    function init(moApp) {
        console.log('[MO-PROFILE] init() 被调用');
        bindEvents();
        loadMoIdFromStorage();
        if (currentMoId) {
            console.log('[MO-PROFILE] 找到 MO ID:', currentMoId);
            loadProfile();
        } else {
            console.warn('[MO-PROFILE] 未找到 MO ID，无法加载资料');
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
            console.log('[MO-PROFILE] 找到 moProfileForm，绑定 submit 事件');
            profileForm.addEventListener('submit', handleProfileSubmit);
        } else {
            console.warn('[MO-PROFILE] 未找到 moProfileForm');
        }

        if (passwordForm) {
            console.log('[MO-PROFILE] 找到 moPasswordForm，绑定 submit 事件');
            passwordForm.addEventListener('submit', handlePasswordSubmit);
        } else {
            console.warn('[MO-PROFILE] 未找到 moPasswordForm - 这会导致密码修改功能失效！');
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
            const userData =
                sessionStorage.getItem('mo-user') ||
                localStorage.getItem('mo-user');
            const finalData = userData || localStorage.getItem('moUser');

            if (finalData) {
                const user = JSON.parse(finalData);
                console.log('[MO-PROFILE] 解析用户数据:', user);
                currentMoId = user.moId || user.id;
                console.log('[MO-PROFILE] 提取的 MO ID:', currentMoId);

                const moIdField = document.getElementById('moIdField');
                const moIdPasswordField = document.getElementById('moIdPasswordField');
                if (moIdField) moIdField.value = currentMoId;
                if (moIdPasswordField) moIdPasswordField.value = currentMoId;
            } else {
                console.error('[MO-PROFILE] sessionStorage / localStorage 中未找到 MO 用户数据');
                console.log('[MO-PROFILE] 可用的 localStorage 键:', Object.keys(localStorage));
            }
        } catch (e) {
            console.error('[MO-PROFILE] 加载用户信息失败:', e);
        }
    }

    /**
     * 加载个人资料
     */
    async function loadProfile() {
        if (!currentMoId) {
            console.warn('[MO-PROFILE] currentMoId 为空，跳过加载');
            return;
        }

        try {
            const url = `${PROFILE_API}?moId=${encodeURIComponent(currentMoId)}`;
            console.log('[MO-PROFILE] 请求资料:', url);

            const response = await fetch(url);
            console.log('[MO-PROFILE] 响应状态:', response.status);

            const result = await response.json();
            console.log('[MO-PROFILE] 响应数据:', result);

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
        console.log('[MO-PROFILE] 填充表单数据:', data);

        const fields = {
            moNameInput: data.name || '',
            contactEmailInput: data.contactEmail || data.email || '',
            bioInput: data.bio || '',
            skillsInput: Array.isArray(data.skills) ? data.skills.join('\n') : ''
        };

        Object.entries(fields).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.value = value;
                console.log(`[MO-PROFILE] 设置 ${id} = ${value}`);
            }
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
        formData.append('name', document.getElementById('moNameInput').value);
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
            console.error('[MO-PROFILE] 密码修改失败：currentMoId 为空');
            alert('请先登录');
            return;
        }

        const currentPassword = document.getElementById('currentPasswordInput').value;
        const newPassword = document.getElementById('newPasswordInput').value;
        const confirmPassword = document.getElementById('confirmPasswordInput').value;
        const newTrimmed = newPassword.trim();
        const confirmTrimmed = confirmPassword.trim();

        if (!currentPassword || !newTrimmed || !confirmTrimmed) {
            alert('请填写所有密码字段');
            return;
        }

        if (newTrimmed !== confirmTrimmed) {
            alert('两次输入的新密码不一致');
            return;
        }

        if (newTrimmed.length < 6) {
            alert('新密码至少需要 6 位字符');
            return;
        }

        const statusEl = document.getElementById('changePasswordStatus');
        if (statusEl) statusEl.textContent = '更新中...';

        const formData = new FormData();
        formData.append('moId', currentMoId);
        formData.append('action', 'password');
        formData.append('currentPassword', currentPassword);
        formData.append('newPassword', newTrimmed);

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

    // 注册到 moApp.modules 供 mo-home.js 初始化
    if (window.MOApp && window.MOApp.modules) {
        console.log('[MO-PROFILE] 注册到 MOApp.modules.profile');
        window.MOApp.modules.profile = init;
    }

    // 同时暴露到全局供直接调用
    window.MoProfileModule = {
        init,
        reload: loadProfile
    };
})();
