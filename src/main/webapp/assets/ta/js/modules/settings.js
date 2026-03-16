(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.settings = function initSettingsModule(app) {
        const root = document.documentElement;
        const themeToggle = document.getElementById('themeToggle');
        const logoutBtn = document.getElementById('logoutBtn');
        const welcomeTitle = document.getElementById('welcomeTitle');
        const welcomeName = document.getElementById('welcomeName');
        const userTrigger = document.getElementById('userTrigger');
        const userName = document.getElementById('userName');
        const welcomeProfileHint = document.getElementById('welcomeProfileHint');
        const welcomeCard = document.getElementById('welcomeCard');
        const themeSelect = document.getElementById('themeSelect');
        const settingsTabs = Array.from(document.querySelectorAll('.settings-tab'));
        const settingsPanels = Array.from(document.querySelectorAll('.settings-panel'));
        const storedTheme = localStorage.getItem('ta-theme');
        const DEBUG_THEME = true;

        let userRaw = sessionStorage.getItem('ta-user') || localStorage.getItem('ta-user');
        let userData = null;

        try {
            userData = userRaw ? JSON.parse(userRaw) : null;
        } catch (error) {
            console.warn('[TA-ONBOARD] ta-user 解析失败：', error, userRaw);
        }

        app.state.settings = {
            root,
            themeToggle,
            logoutBtn,
            welcomeTitle,
            welcomeName,
            userTrigger,
            userName,
            welcomeProfileHint,
            welcomeCard,
            themeSelect,
            settingsTabs,
            settingsPanels,
            userRaw,
            userData,
            storedTheme,
            guideActive: false,
            currentSettingsTab: 'profile'
        };

        function debugThemeLog(stage, extra) {
            if (!DEBUG_THEME) return;
            console.log('[TA-THEME]', stage, extra || {});
        }

        function resolveWelcomeName(data) {
            if (!data) return 'TA';
            return data.username || data.name || data.account || 'TA';
        }

        function getUserData() {
            return app.state.settings.userData;
        }

        function setUserData(nextUser) {
            app.state.settings.userData = nextUser || null;
            if (nextUser) {
                const serialized = JSON.stringify(nextUser);
                sessionStorage.setItem('ta-user', serialized);
                if (localStorage.getItem('ta-user')) {
                    localStorage.setItem('ta-user', serialized);
                }
                app.state.settings.userRaw = serialized;
            } else {
                sessionStorage.removeItem('ta-user');
                localStorage.removeItem('ta-user');
                app.state.settings.userRaw = '';
            }
        }

        function applyWelcomeTitle(reason) {
            if (!welcomeTitle) {
                console.warn('[TA-WELCOME] welcomeTitle not found:', reason);
                return;
            }
            const displayName = resolveWelcomeName(getUserData());
            if (welcomeName) {
                welcomeName.textContent = displayName;
            } else {
                welcomeTitle.textContent = '欢迎回来，' + displayName;
            }
            if (userName) userName.textContent = displayName;
            debugThemeLog('welcome-title-updated', { reason, displayName });
        }

        function debugOnboardingLog(stage, extra) {
            console.log('[TA-ONBOARD]', stage, {
                userRaw: app.state.settings.userRaw,
                userData: getUserData(),
                hasOnboarding: !!document.getElementById('onboarding'),
                ...(extra || {})
            });
        }

        function redirectToLogin() {
            window.location.replace('../../index.jsp');
        }

        function ensureLoggedIn() {
            const hasUser = sessionStorage.getItem('ta-user') || localStorage.getItem('ta-user');
            if (!hasUser) {
                redirectToLogin();
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

        function applyThemeChoice(value) {
            const safeValue = value || 'system';
            if (safeValue === 'light') {
                root.setAttribute('data-theme', 'light');
                localStorage.setItem('ta-theme', 'light');
                if (themeToggle) themeToggle.textContent = '☀️';
            } else if (safeValue === 'dark') {
                root.removeAttribute('data-theme');
                localStorage.setItem('ta-theme', 'dark');
                if (themeToggle) themeToggle.textContent = '🌙';
            } else {
                root.removeAttribute('data-theme');
                localStorage.setItem('ta-theme', 'system');
                if (themeToggle) themeToggle.textContent = '🌙';
            }
            if (themeSelect) themeSelect.value = safeValue;
            debugThemeLog('theme-applied', { value: safeValue });
        }

        function toggleTheme() {
            const current = localStorage.getItem('ta-theme') || 'dark';
            if (current === 'light') applyThemeChoice('dark');
            else applyThemeChoice('light');
        }

        function activateSettingsTab(target) {
            const safeTarget = String(target || 'profile').trim() || 'profile';
            app.state.settings.currentSettingsTab = safeTarget;
            settingsTabs.forEach((tab) => {
                const isActive = tab.dataset.settingsTab === safeTarget;
                tab.classList.toggle('active', isActive);
                tab.setAttribute('aria-selected', isActive ? 'true' : 'false');
            });
            settingsPanels.forEach((panel) => {
                panel.classList.toggle('active', panel.dataset.settingsPanel === safeTarget);
            });
        }

        if (storedTheme === 'light') {
            applyThemeChoice('light');
        } else if (storedTheme === 'system') {
            applyThemeChoice('system');
        } else {
            applyThemeChoice('dark');
        }

        if (!ensureLoggedIn()) {
            throw new Error('未登录，已重定向到登录页');
        }

        applyWelcomeTitle('init');
        requestAnimationFrame(() => applyWelcomeTitle('raf'));
        window.addEventListener('pageshow', () => applyWelcomeTitle('pageshow'));

        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => {
                if (!window.confirm('确认退出登录吗？')) return;
                setUserData(null);
                redirectToLogin();
            });
        }

        window.addEventListener('pageshow', () => {
            if (!ensureLoggedIn()) return;
            history.replaceState({ taHome: true }, '', window.location.href);
        });
        history.replaceState({ taHome: true }, '', window.location.href);

        themeToggle?.addEventListener('click', toggleTheme);
        themeSelect?.addEventListener('change', (event) => applyThemeChoice(event.target.value));

        settingsTabs.forEach((tab) => {
            tab.addEventListener('click', () => activateSettingsTab(tab.dataset.settingsTab));
        });
        activateSettingsTab('profile');

        userTrigger?.addEventListener('click', openSettingsModal);
        userTrigger?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                openSettingsModal();
            }
        });

        welcomeProfileHint?.addEventListener('click', openSettingsFromHint);
        welcomeProfileHint?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') openSettingsFromHint(event);
        });

        welcomeCard?.addEventListener('click', (event) => {
            if (shouldIgnoreWelcomeCardTrigger(event)) return;
            openSettingsFromWelcomeCard(event);
        });
        welcomeCard?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') openSettingsFromWelcomeCard(event);
        });

        app.resolveWelcomeName = resolveWelcomeName;
        app.applyWelcomeTitle = applyWelcomeTitle;
        app.debugOnboardingLog = debugOnboardingLog;
        app.ensureLoggedIn = ensureLoggedIn;
        app.redirectToLogin = redirectToLogin;
        app.openSettingsModal = openSettingsModal;
        app.activateSettingsTab = activateSettingsTab;
        app.applyThemeChoice = applyThemeChoice;
        app.getUserData = getUserData;
        app.setUserData = setUserData;
    };
})();
