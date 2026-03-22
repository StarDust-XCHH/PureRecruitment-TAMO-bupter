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
            const result = await response.json().catch(() => ({ success: false, message: 'Invalid response payload' }));
            if (!response.ok || !result.success) {
                throw new Error(result.message || 'Request failed');
            }
            return result;
        }

        async function savePasswordUpdate() {
            if (!app.profileState?.taId) {
                setPasswordStatus('Missing sign-in information', 'is-error');
                return;
            }
            if (!currentPassword?.value) {
                setPasswordStatus('Enter your current password', 'is-error');
                return;
            }
            if (!newPassword?.value) {
                setPasswordStatus('Enter a new password', 'is-error');
                return;
            }
            if (newPassword.value.length < 6) {
                setPasswordStatus('New password must be at least 6 characters', 'is-error');
                return;
            }
            if (newPassword.value !== confirmPassword?.value) {
                setPasswordStatus('New passwords do not match', 'is-error');
                return;
            }
            if (currentPassword.value === newPassword.value) {
                setPasswordStatus('New password must differ from the current one', 'is-error');
                return;
            }

            if (savePasswordBtn) savePasswordBtn.disabled = true;
            setPasswordStatus('Saving…', null);
            try {
                await requestPasswordUpdate({
                    taId: app.profileState.taId,
                    currentPassword: currentPassword.value,
                    newPassword: newPassword.value
                });
                setPasswordStatus('Password updated', 'is-success');
                resetPasswordForm();
            } catch (error) {
                console.error('[TA-SECURITY] save password failed', error);
                setPasswordStatus(error.message || 'Save failed', 'is-error');
            } finally {
                if (savePasswordBtn) savePasswordBtn.disabled = false;
            }
        }

        newPassword?.addEventListener('input', () => setPasswordStatus('Not saved yet', null));
        currentPassword?.addEventListener('input', () => setPasswordStatus('Not saved yet', null));
        confirmPassword?.addEventListener('input', () => setPasswordStatus('Not saved yet', null));
        savePasswordBtn?.addEventListener('click', savePasswordUpdate);

        app.setPasswordStatus = setPasswordStatus;
        app.savePasswordUpdate = savePasswordUpdate;
    };
})();
