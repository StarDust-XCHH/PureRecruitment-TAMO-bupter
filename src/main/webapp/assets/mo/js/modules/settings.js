(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.settings = function initSettings(app) {
        const root = document.documentElement;
        const themeToggle = document.getElementById('themeToggle');
        const logoutBtn = document.getElementById('logoutBtn');
        const welcomeName = document.getElementById('welcomeName');
        const userName = document.getElementById('userName');
        const userTrigger = document.getElementById('userTrigger');
        const welcomeCard = document.getElementById('welcomeCard');
        const welcomeProfileHint = document.getElementById('welcomeProfileHint');
        const settingsUser = document.getElementById('moSettingsUsername');
        const settingsTheme = document.getElementById('moSettingsTheme');
        const settingsTabs = Array.from(document.querySelectorAll('.settings-tab'));
        const settingsPanels = Array.from(document.querySelectorAll('.settings-panel'));
        const profileAvatarBox = document.getElementById('profileAvatarBox');
        const avatarFileInput = document.getElementById('avatarFile');

        app.state = app.state || {};

        function getMoUser() {
            const raw = sessionStorage.getItem('mo-user') || localStorage.getItem('mo-user');
            if (!raw) return null;
            try {
                return JSON.parse(raw);
            } catch (e) {
                return null;
            }
        }

        app.getMoUser = getMoUser;

        function getUserData() {
            return app.state.settings ? app.state.settings.userData : null;
        }

        function setUserData(nextUser) {
            if (!app.state.settings) return;
            app.state.settings.userData = nextUser || null;
            if (nextUser) {
                const serialized = JSON.stringify(nextUser);
                sessionStorage.setItem('mo-user', serialized);
                localStorage.setItem('mo-user', serialized);
            } else {
                sessionStorage.removeItem('mo-user');
                localStorage.removeItem('mo-user');
            }
        }

        function debugOnboardingLog(stage, extra) {
            console.log('[MO-ONBOARD]', stage, {
                userData: getUserData(),
                hasOnboarding: !!document.getElementById('onboarding'),
                extra: extra || {}
            });
        }

        function ensureLoggedIn() {
            if (!getMoUser()) {
                window.location.replace('../../index.jsp');
                return false;
            }
            return true;
        }

        function openSettingsModal() {
            if (typeof app.openModal === 'function') app.openModal('settings');
            if (userTrigger) userTrigger.setAttribute('aria-expanded', 'true');
        }

        function openSettingsFromHint(event) {
            if (event) event.preventDefault();
            openSettingsModal();
        }

        function openSettingsFromWelcomeCard(event) {
            if (event) event.preventDefault();
            openSettingsModal();
        }

        function shouldIgnoreWelcomeCardTrigger(event) {
            if (!event) return false;
            const target = event.target;
            if (!target || !(target instanceof Element)) return false;
            return !!target.closest('a, button, input, textarea, select');
        }

        function applyTheme(theme) {
            const safeTheme = theme === 'light' ? 'light' : 'dark';
            if (safeTheme === 'light') {
                root.setAttribute('data-theme', 'light');
                if (themeToggle) themeToggle.textContent = '☀️';
            } else {
                root.removeAttribute('data-theme');
                if (themeToggle) themeToggle.textContent = '🌙';
            }
            localStorage.setItem('mo-theme', safeTheme);
            if (settingsTheme) settingsTheme.textContent = safeTheme;
        }

        function toggleTheme() {
            const current = localStorage.getItem('mo-theme') || 'dark';
            applyTheme(current === 'light' ? 'dark' : 'light');
        }

        function activateSettingsTab(target) {
            const safeTarget = String(target || 'profile').trim() || 'profile';
            settingsTabs.forEach((tab) => {
                const isActive = tab.dataset.settingsTab === safeTarget;
                tab.classList.toggle('active', isActive);
                tab.setAttribute('aria-selected', isActive ? 'true' : 'false');
            });
            settingsPanels.forEach((panel) => {
                panel.classList.toggle('active', panel.dataset.settingsPanel === safeTarget);
                panel.hidden = panel.dataset.settingsPanel !== safeTarget;
            });
        }

        if (!ensureLoggedIn()) return;

        const moUser = getMoUser();
        app.state.settings = {
            userData: moUser,
            guideActive: false
        };

        const displayName =
            moUser && (moUser.name || moUser.moName || moUser.username)
                ? String(moUser.name || moUser.moName || moUser.username).trim() || 'MO'
                : 'MO';
        if (welcomeName) welcomeName.textContent = displayName;
        if (userName) userName.textContent = displayName;
        if (settingsUser) settingsUser.textContent = displayName;

        applyTheme(localStorage.getItem('mo-theme') || 'dark');

        if (themeToggle) themeToggle.addEventListener('click', toggleTheme);
        if (logoutBtn) {
            logoutBtn.addEventListener('click', function () {
                if (!window.confirm('确认退出登录吗？')) return;
                setUserData(null);
                window.location.replace('../../index.jsp');
            });
        }

        settingsTabs.forEach((tab) => {
            tab.addEventListener('click', () => activateSettingsTab(tab.dataset.settingsTab));
        });
        activateSettingsTab('profile');

        userTrigger && userTrigger.addEventListener('click', openSettingsModal);
        userTrigger && userTrigger.addEventListener('keydown', function (event) {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                openSettingsModal();
            }
        });

        profileAvatarBox && profileAvatarBox.addEventListener('click', () => {
            avatarFileInput && avatarFileInput.click();
        });
        profileAvatarBox && profileAvatarBox.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                avatarFileInput && avatarFileInput.click();
            }
        });

        welcomeProfileHint && welcomeProfileHint.addEventListener('click', openSettingsFromHint);
        welcomeProfileHint && welcomeProfileHint.addEventListener('keydown', function (event) {
            if (event.key === 'Enter' || event.key === ' ') openSettingsFromHint(event);
        });

        welcomeCard && welcomeCard.addEventListener('click', function (event) {
            if (shouldIgnoreWelcomeCardTrigger(event)) return;
            openSettingsFromWelcomeCard(event);
        });
        welcomeCard && welcomeCard.addEventListener('keydown', function (event) {
            if (event.key === 'Enter' || event.key === ' ') openSettingsFromWelcomeCard(event);
        });

        app.getUserData = getUserData;
        app.setUserData = setUserData;
        app.debugOnboardingLog = debugOnboardingLog;
        app.openSettingsModal = openSettingsModal;
        app.activateSettingsTab = activateSettingsTab;
    };
})();
