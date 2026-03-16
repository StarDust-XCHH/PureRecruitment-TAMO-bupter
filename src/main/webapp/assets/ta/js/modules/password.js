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
            const result = await response.json().catch(() => ({ success: false, message: '返回数据解析失败' }));
            if (!response.ok || !result.success) {
                throw new Error(result.message || '请求失败');
            }
            return result;
        }

        async function savePasswordUpdate() {
            if (!app.profileState?.taId) {
                setPasswordStatus('缺少登录信息', 'is-error');
                return;
            }
            if (!currentPassword?.value) {
                setPasswordStatus('请输入当前密码', 'is-error');
                return;
            }
            if (!newPassword?.value) {
                setPasswordStatus('请输入新密码', 'is-error');
                return;
            }
            if (newPassword.value.length < 6) {
                setPasswordStatus('新密码至少 6 位', 'is-error');
                return;
            }
            if (newPassword.value !== confirmPassword?.value) {
                setPasswordStatus('两次输入的新密码不一致', 'is-error');
                return;
            }
            if (currentPassword.value === newPassword.value) {
                setPasswordStatus('新密码不能与当前密码相同', 'is-error');
                return;
            }

            if (savePasswordBtn) savePasswordBtn.disabled = true;
            setPasswordStatus('保存中...', null);
            try {
                await requestPasswordUpdate({
                    taId: app.profileState.taId,
                    currentPassword: currentPassword.value,
                    newPassword: newPassword.value
                });
                setPasswordStatus('密码更新成功', 'is-success');
                resetPasswordForm();
            } catch (error) {
                console.error('[TA-SECURITY] save password failed', error);
                setPasswordStatus(error.message || '保存失败', 'is-error');
            } finally {
                if (savePasswordBtn) savePasswordBtn.disabled = false;
            }
        }

        newPassword?.addEventListener('input', () => setPasswordStatus('尚未保存', null));
        currentPassword?.addEventListener('input', () => setPasswordStatus('尚未保存', null));
        confirmPassword?.addEventListener('input', () => setPasswordStatus('尚未保存', null));
        savePasswordBtn?.addEventListener('click', savePasswordUpdate);

        app.setPasswordStatus = setPasswordStatus;
        app.savePasswordUpdate = savePasswordUpdate;
    };
})();
