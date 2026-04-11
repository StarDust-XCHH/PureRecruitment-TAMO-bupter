(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.settings = function initSettings(app) {
        const root = document.documentElement;
        const themeToggle = document.getElementById('themeToggle');
        const logoutBtn = document.getElementById('logoutBtn');
        const languageToggle = document.getElementById('languageToggle');
        const welcomeName = document.getElementById('welcomeName');
        const welcomeTitle = document.getElementById('welcomeTitle');
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
        const storedLanguage = localStorage.getItem('mo-language');

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

        function resolveWelcomeName(user) {
            if (!user) return 'MO';
            return String(user.name || user.moName || user.username || 'MO').trim() || 'MO';
        }

        function getCurrentLanguage() {
            /** MO 默认英文；仅当用户曾手动选中文（localStorage mo-language=zh）时为 zh */
            return app.state.settings && app.state.settings.currentLanguage === 'zh' ? 'zh' : 'en';
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
                const text = getTranslationValue(el, lang, 'text');
                if (text) el.textContent = text;
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

        function t(zh, en) {
            return getCurrentLanguage() === 'en' ? en : zh;
        }

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

        function applyWelcomeTitle() {
            const currentTitle = document.getElementById('welcomeTitle');
            const currentName = document.getElementById('welcomeName');
            const displayName = resolveWelcomeName(getUserData());
            if (currentName) currentName.textContent = displayName;
            if (!currentName && currentTitle) currentTitle.textContent = t('欢迎回来，', 'Welcome back, ') + displayName;
            if (userName) userName.textContent = displayName;
            if (settingsUser) settingsUser.textContent = displayName;
        }

        function applyLanguage(reason) {
            const lang = getCurrentLanguage();
            root.lang = lang === 'en' ? 'en' : 'zh-CN';
            document.querySelectorAll('[data-i18n], [data-i18n-html], [data-i18n-placeholder], [data-i18n-aria-label], [data-i18n-title]').forEach(function (el) {
                applyI18nToElement(el, lang);
            });
            localStorage.setItem('mo-language', lang);
            applyWelcomeTitle();
            if (typeof app.refreshProfileLanguage === 'function') {
                app.refreshProfileLanguage(reason || 'language');
            }
            if (typeof app.refreshJobBoardLanguage === 'function') {
                app.refreshJobBoardLanguage();
            }
            if (typeof app.refreshApplicantsLanguage === 'function') {
                app.refreshApplicantsLanguage();
            }
            if (typeof app.refreshOnboardingLanguage === 'function') {
                app.refreshOnboardingLanguage(reason || 'language');
            }
            if (reason === 'toggle' && typeof app.refreshMoWorkspaceAll === 'function') {
                void app.refreshMoWorkspaceAll();
            } else if (typeof app.loadDashboard === 'function') {
                app.loadDashboard();
            }
        }

        function toggleLanguage() {
            if (!app.state.settings) return;
            app.state.settings.currentLanguage = getCurrentLanguage() === 'en' ? 'zh' : 'en';
            applyLanguage('toggle');
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
            guideActive: false,
            currentLanguage: storedLanguage === 'zh' ? 'zh' : 'en'
        };

        applyTheme(localStorage.getItem('mo-theme') || 'dark');
        applyLanguage('init');
        requestAnimationFrame(function () {
            applyLanguage('raf');
        });

        if (themeToggle) themeToggle.addEventListener('click', toggleTheme);
        if (languageToggle) languageToggle.addEventListener('click', toggleLanguage);
        if (logoutBtn) {
            logoutBtn.addEventListener('click', function () {
                if (!window.confirm(t('确认退出登录吗？', 'Are you sure you want to log out?'))) return;
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
        app.applyLanguage = applyLanguage;
        app.toggleLanguage = toggleLanguage;
        app.getCurrentLanguage = getCurrentLanguage;
        app.t = t;
    };
})();
