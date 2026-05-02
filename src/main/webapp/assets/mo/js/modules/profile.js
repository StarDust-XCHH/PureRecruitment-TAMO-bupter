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

    let moAppRef = null;

    function t(zh, en) {
        return moAppRef && typeof moAppRef.t === 'function' ? moAppRef.t(zh, en) : zh;
    }

    let profileSaveKind = 'loaded';
    let profileSaveErrorExtra = '';
    let passwordStatusKind = 'idle';
    let passwordSaveErrorExtra = '';

    function applyProfileSaveStatus() {
        const el = document.getElementById('profileSaveStatus');
        if (!el) return;
        if (profileSaveKind === 'error' && profileSaveErrorExtra) {
            el.textContent = '✗ ' + profileSaveErrorExtra;
            return;
        }
        const map = {
            loaded: function () { return t('已加载', 'Loaded'); },
            saving: function () { return t('保存中...', 'Saving...'); },
            saved: function () { return t('✓ 已保存', '✓ Saved'); }
        };
        el.textContent = map[profileSaveKind] ? map[profileSaveKind]() : '';
    }

    function setProfileSaveKind(kind, extraErr) {
        profileSaveKind = kind;
        profileSaveErrorExtra = extraErr != null ? String(extraErr) : '';
        applyProfileSaveStatus();
    }

    function applyPasswordSaveStatus() {
        const el = document.getElementById('passwordSaveStatus');
        if (!el) return;
        if (passwordStatusKind === 'error' && passwordSaveErrorExtra) {
            el.textContent = '✗ ' + passwordSaveErrorExtra;
            return;
        }
        const map = {
            idle: function () { return t('尚未保存', 'Not saved'); },
            updating: function () { return t('更新中...', 'Updating...'); },
            saved: function () { return t('✓ 密码已更新', '✓ Password updated'); }
        };
        el.textContent = map[passwordStatusKind] ? map[passwordStatusKind]() : '';
    }

    function setPasswordStatusKind(kind, extraErr) {
        passwordStatusKind = kind;
        passwordSaveErrorExtra = extraErr != null ? String(extraErr) : '';
        applyPasswordSaveStatus();
    }

    function refreshProfileLanguage() {
        applyProfileSaveStatus();
        applyPasswordSaveStatus();
    }

    let currentMoId = null;
    let selectedAvatarFile = null;
    let avatarChanged = false;

    console.log('[MO-PROFILE] 模块加载完成，API地址:', PROFILE_API);

    /**
     * 初始化个人资料模块
     */
    function init(moApp) {
        moAppRef = moApp;
        moAppRef.refreshProfileLanguage = refreshProfileLanguage;
        console.log('[MO-PROFILE] init() 被调用');
        bindEvents();
        loadMoIdFromStorage();
        if (currentMoId) {
            console.log('[MO-PROFILE] 找到 MO ID:', currentMoId);
            loadProfile();
        } else {
            console.warn('[MO-PROFILE] 未找到 MO ID，无法加载资料');
        }

        applyPasswordSaveStatus();
    }

    /**
     * 绑定事件监听器
     */
    function bindEvents() {
        const saveProfileBtn = document.getElementById('saveProfileBtn');
        const changePasswordBtn = document.getElementById('changePasswordBtn');
        const avatarFileInput = document.getElementById('avatarFile');

        if (saveProfileBtn) {
            console.log('[MO-PROFILE] 找到 saveProfileBtn，绑定 click 事件');
            saveProfileBtn.addEventListener('click', handleProfileSubmit);
        } else {
            console.warn('[MO-PROFILE] 未找到 saveProfileBtn');
        }

        if (changePasswordBtn) {
            console.log('[MO-PROFILE] 找到 changePasswordBtn，绑定 click 事件');
            changePasswordBtn.addEventListener('click', handlePasswordSubmit);
        } else {
            console.warn('[MO-PROFILE] 未找到 changePasswordBtn');
        }

        if (avatarFileInput) {
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
            updateAvatarPreview(data.avatar);
        }

        setProfileSaveKind('loaded');
    }

    /**
     * 更新头像预览
     */
    function updateAvatarPreview(avatarPath) {
        const profileAvatarBox = document.getElementById('profileAvatarBox');
        if (!profileAvatarBox) return;

        const avatarUrl = avatarPath.startsWith('http') ? avatarPath : `${ASSETS_BASE}/${avatarPath}`;
        profileAvatarBox.style.backgroundImage = `url('${avatarUrl}')`;
        profileAvatarBox.style.backgroundSize = 'cover';
        profileAvatarBox.style.backgroundPosition = 'center';
        profileAvatarBox.style.backgroundRepeat = 'no-repeat';
        profileAvatarBox.classList.add('has-image');
        profileAvatarBox.textContent = '';
    }

    /**
     * 处理头像选择
     */
    function handleAvatarSelect(event) {
        const file = event.target.files[0];
        if (!file) return;

        if (file.size > 10 * 1024 * 1024) {
            alert(t('头像大小不能超过 10MB', 'Avatar must be 10MB or smaller'));
            event.target.value = '';
            return;
        }

        selectedAvatarFile = file;
        avatarChanged = true;

        const reader = new FileReader();
        reader.onload = (e) => {
            const profileAvatarBox = document.getElementById('profileAvatarBox');
            if (profileAvatarBox) {
                profileAvatarBox.style.backgroundImage = `url('${e.target.result}')`;
                profileAvatarBox.style.backgroundSize = 'cover';
                profileAvatarBox.style.backgroundPosition = 'center';
                profileAvatarBox.style.backgroundRepeat = 'no-repeat';
                profileAvatarBox.classList.add('has-image');
                profileAvatarBox.textContent = '';
            }
        };
        reader.readAsDataURL(file);
    }

    /**
     * 处理资料提交
     */
    async function handleProfileSubmit(event) {
        if (event) event.preventDefault();

        if (!currentMoId) {
            alert(t('请先登录', 'Please sign in first'));
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

        if (selectedAvatarFile && avatarChanged) {
            formData.append('avatarFile', selectedAvatarFile);
        }

        setProfileSaveKind('saving');

        try {
            const response = await fetch(PROFILE_API, {
                method: 'POST',
                body: formData
            });
            const result = await response.json();

            if (result.success) {
                setProfileSaveKind('saved');
                selectedAvatarFile = null;
                avatarChanged = false;
                if (window.MoToast && typeof window.MoToast.show === 'function') {
                    window.MoToast.show({
                        type: 'success',
                        message: t('个人资料已保存', 'Profile saved')
                    });
                }
                setTimeout(function () {
                    setProfileSaveKind('loaded');
                }, 3000);
            } else {
                setProfileSaveKind('error', result.message);
                alert(t('保存失败: ', 'Save failed: ') + (result.message || ''));
            }
        } catch (error) {
            console.error('[MO-PROFILE] 保存失败:', error);
            setProfileSaveKind('error', t('网络错误', 'Network error'));
            alert(t('保存失败，请检查网络连接', 'Save failed. Check your network connection.'));
        }
    }

    /**
     * 处理密码修改提交
     */
    async function handlePasswordSubmit(event) {
        if (event) event.preventDefault();

        if (!currentMoId) {
            console.error('[MO-PROFILE] 密码修改失败：currentMoId 为空');
            alert(t('请先登录', 'Please sign in first'));
            return;
        }

        const currentPassword = document.getElementById('currentPasswordInput').value;
        const newPassword = document.getElementById('newPasswordInput').value;
        const confirmPassword = document.getElementById('confirmPasswordInput').value;
        const newTrimmed = newPassword.trim();
        const confirmTrimmed = confirmPassword.trim();

        if (!currentPassword || !newTrimmed || !confirmTrimmed) {
            alert(t('请填写所有密码字段', 'Please fill in all password fields'));
            return;
        }

        if (newTrimmed !== confirmTrimmed) {
            alert(t('两次输入的新密码不一致', 'The new passwords do not match'));
            return;
        }

        if (newTrimmed.length < 6) {
            alert(t('新密码至少需要 6 位字符', 'New password must be at least 6 characters'));
            return;
        }

        setPasswordStatusKind('updating');

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
                setPasswordStatusKind('saved');
                document.getElementById('currentPasswordInput').value = '';
                document.getElementById('newPasswordInput').value = '';
                document.getElementById('confirmPasswordInput').value = '';
                if (window.MoToast && typeof window.MoToast.show === 'function') {
                    window.MoToast.show({
                        type: 'success',
                        message: t('密码已更新', 'Password updated')
                    });
                }
                setTimeout(function () {
                    setPasswordStatusKind('idle');
                }, 3000);
            } else {
                setPasswordStatusKind('error', result.message);
                alert(t('更新失败: ', 'Update failed: ') + (result.message || ''));
            }
        } catch (error) {
            console.error('[MO-PROFILE] 密码更新失败:', error);
            setPasswordStatusKind('error', t('网络错误', 'Network error'));
            alert(t('更新失败，请检查网络连接', 'Update failed. Check your network connection.'));
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
