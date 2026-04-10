(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.settings = function initSettingsModule(app) {
        const root = document.documentElement;
        const themeToggle = document.getElementById('themeToggle');
        const logoutBtn = document.getElementById('logoutBtn');
        const languageToggle = document.getElementById('languageToggle');
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
        const storedLanguage = localStorage.getItem('ta-language');
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
            languageToggle,
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
            storedLanguage,
            guideActive: false,
            currentSettingsTab: 'profile',
            currentLanguage: storedLanguage === 'en' ? 'en' : 'zh'
        };

        function debugThemeLog(stage, extra) {
            if (!DEBUG_THEME) return;
            console.log('[TA-THEME]', stage, extra || {});
        }

        function resolveWelcomeName(data) {
            if (!data) return 'TA';
            return data.username || data.name || data.account || 'TA';
        }

        function getCurrentLanguage() {
            return app.state.settings.currentLanguage === 'en' ? 'en' : 'zh';
        }

        function getTranslationValue(el, lang, type) {
            if (!el) return '';
            const suffix = lang === 'en' ? 'en' : 'zh';
            if (type === 'html') {
                const raw = el.getAttribute('data-i18n-html') || '';
                const parts = raw.split('|');
                return suffix === 'en' ? (parts[1] || parts[0] || '') : (parts[0] || '');
            }
            if (type === 'aria-label') {
                return el.getAttribute('data-i18n-aria-label-' + suffix) || '';
            }
            if (type === 'placeholder') {
                return el.getAttribute('data-i18n-placeholder-' + suffix) || '';
            }
            if (type === 'title') {
                return el.getAttribute('data-i18n-title-' + suffix) || '';
            }
            return el.getAttribute('data-i18n-' + suffix) || '';
        }

        function applyI18nToElement(el, lang) {
            if (!el) return;
            if (el.hasAttribute('data-i18n-html')) {
                el.innerHTML = getTranslationValue(el, lang, 'html');
                return;
            }
            if (el.hasAttribute('data-i18n')) {
                const value = getTranslationValue(el, lang, 'text');
                if (value) el.textContent = value;
            }
            if (el.hasAttribute('data-i18n-placeholder')) {
                const placeholder = getTranslationValue(el, lang, 'placeholder');
                if (placeholder) el.setAttribute('placeholder', placeholder);
            }
            if (el.hasAttribute('data-i18n-aria-label')) {
                const ariaLabel = getTranslationValue(el, lang, 'aria-label');
                if (ariaLabel) el.setAttribute('aria-label', ariaLabel);
            }
            if (el.hasAttribute('data-i18n-title')) {
                const title = getTranslationValue(el, lang, 'title');
                if (title) el.setAttribute('title', title);
            }
        }

        function refreshWelcomeNodes() {
            app.state.settings.welcomeTitle = document.getElementById('welcomeTitle');
            app.state.settings.welcomeName = document.getElementById('welcomeName');
        }

        function applyLanguage(reason) {
            const lang = getCurrentLanguage();
            root.lang = lang === 'en' ? 'en' : 'zh-CN';
            document.querySelectorAll('[data-i18n], [data-i18n-html], [data-i18n-placeholder], [data-i18n-aria-label], [data-i18n-title]').forEach((el) => {
                applyI18nToElement(el, lang);
            });
            refreshWelcomeNodes();
            app.state.settings.welcomeTitle = document.getElementById('welcomeTitle');
            app.state.settings.welcomeName = document.getElementById('welcomeName');
            app.state.settings.languageToggle = document.getElementById('languageToggle');
            localStorage.setItem('ta-language', lang);
            applyWelcomeTitle(reason || 'language');
            if (typeof app.refreshLanguageBindings === 'function') {
                app.refreshLanguageBindings(lang);
            }
            debugThemeLog('language-applied', { reason, lang });
        }

        function toggleLanguage() {
            app.state.settings.currentLanguage = getCurrentLanguage() === 'zh' ? 'en' : 'zh';
            applyLanguage('toggle');
        }

        function t(zh, en) {
            return getCurrentLanguage() === 'en' ? en : zh;
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
            const currentWelcomeTitle = document.getElementById('welcomeTitle');
            const currentWelcomeName = document.getElementById('welcomeName');
            if (!currentWelcomeTitle) {
                console.warn('[TA-WELCOME] welcomeTitle not found:', reason);
                return;
            }
            const displayName = resolveWelcomeName(getUserData());
            if (currentWelcomeName) {
                currentWelcomeName.textContent = displayName;
            } else {
                currentWelcomeTitle.textContent = t('欢迎回来，', 'Welcome back, ') + displayName;
            }
            if (userName) userName.textContent = displayName;
            app.state.settings.welcomeTitle = currentWelcomeTitle;
            app.state.settings.welcomeName = currentWelcomeName;
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

        applyLanguage('init');
        applyWelcomeTitle('init');
        requestAnimationFrame(() => applyWelcomeTitle('raf'));
        window.addEventListener('pageshow', () => {
            applyLanguage('pageshow');
            applyWelcomeTitle('pageshow');
        });

        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => {
                if (!window.confirm(t('确认退出登录吗？', 'Are you sure you want to log out?'))) return;
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
        languageToggle?.addEventListener('click', toggleLanguage);
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
        app.applyLanguage = applyLanguage;
        app.toggleLanguage = toggleLanguage;
        app.getCurrentLanguage = getCurrentLanguage;
        app.t = t;
        app.getUserData = getUserData;
        app.setUserData = setUserData;
    };
})();
