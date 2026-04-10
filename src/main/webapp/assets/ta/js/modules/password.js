(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.password = function initPasswordModule(app) {
        const currentPassword = document.getElementById('currentPassword');
        const newPassword = document.getElementById('newPassword');
        const confirmPassword = document.getElementById('confirmPassword');
        const savePasswordBtn = document.getElementById('savePasswordBtn');
        const passwordSaveStatus = document.getElementById('passwordSaveStatus');

        function t(zh, en) {
            return typeof app.t === 'function' ? app.t(zh, en) : zh;
        }

        function setPasswordStatus(text, tone) {
            if (!passwordSaveStatus) return;
            passwordSaveStatus.textContent = text;
            passwordSaveStatus.classList.remove('is-dirty', 'is-error', 'is-success');
            if (tone) passwordSaveStatus.classList.add(tone);
        }

        function resetPasswordForm() {
            if (currentPassword) currentPassword.value = '';
            if (newPassword) newPassword.value = '';
            if (confirmPassword) confirmPassword.value = '';
        }

        async function requestPasswordUpdate(payload) {
            const formData = new FormData();
            Object.entries(payload || {}).forEach(([key, value]) => {
                if (value != null) formData.append(key, value);
            });
            formData.append('action', 'password');
            const response = await fetch('../../api/ta/profile-settings', {
                method: 'POST',
                body: formData
            });
            const result = await response.json().catch(() => ({ success: false, message: t('返回数据解析失败', 'Failed to parse response data') }));
            if (!response.ok || !result.success) {
                throw new Error(result.message || t('请求失败', 'Request failed'));
            }
            return result;
        }

        async function savePasswordUpdate() {
            if (!app.profileState?.taId) {
                setPasswordStatus(t('缺少登录信息', 'Missing login information'), 'is-error');
                return;
            }
            if (!currentPassword?.value) {
                setPasswordStatus(t('请输入当前密码', 'Please enter your current password'), 'is-error');
                return;
            }
            if (!newPassword?.value) {
                setPasswordStatus(t('请输入新密码', 'Please enter a new password'), 'is-error');
                return;
            }
            if (newPassword.value.length < 6) {
                setPasswordStatus(t('新密码至少 6 位', 'The new password must be at least 6 characters'), 'is-error');
                return;
            }
            if (newPassword.value !== confirmPassword?.value) {
                setPasswordStatus(t('两次输入的新密码不一致', 'The new passwords do not match'), 'is-error');
                return;
            }
            if (currentPassword.value === newPassword.value) {
                setPasswordStatus(t('新密码不能与当前密码相同', 'The new password cannot be the same as the current password'), 'is-error');
                return;
            }

            if (savePasswordBtn) savePasswordBtn.disabled = true;
            setPasswordStatus(t('保存中...', 'Saving...'), null);
            try {
                await requestPasswordUpdate({
                    taId: app.profileState.taId,
                    currentPassword: currentPassword.value,
                    newPassword: newPassword.value
                });
                setPasswordStatus(t('密码更新成功', 'Password updated successfully'), 'is-success');
                resetPasswordForm();
            } catch (error) {
                console.error('[TA-SECURITY] save password failed', error);
                setPasswordStatus(error.message || t('保存失败', 'Save failed'), 'is-error');
            } finally {
                if (savePasswordBtn) savePasswordBtn.disabled = false;
            }
        }

        newPassword?.addEventListener('input', () => setPasswordStatus(t('尚未保存', 'Not saved yet'), null));
        currentPassword?.addEventListener('input', () => setPasswordStatus(t('尚未保存', 'Not saved yet'), null));
        confirmPassword?.addEventListener('input', () => setPasswordStatus(t('尚未保存', 'Not saved yet'), null));
        savePasswordBtn?.addEventListener('click', savePasswordUpdate);

        app.setPasswordStatus = setPasswordStatus;
        app.savePasswordUpdate = savePasswordUpdate;
    };
})();
