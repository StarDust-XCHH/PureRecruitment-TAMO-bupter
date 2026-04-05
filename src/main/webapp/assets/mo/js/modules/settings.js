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
        const settingsUser = document.getElementById('moSettingsUsername');
        const settingsTheme = document.getElementById('moSettingsTheme');

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

        function ensureLoggedIn() {
            if (!getMoUser()) {
                window.location.replace('../../index.jsp');
                return false;
            }
            return true;
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

        if (!ensureLoggedIn()) return;

        const moUser = getMoUser();
        const displayName = moUser && (moUser.username || moUser.name) ? (moUser.username || moUser.name) : 'MO';
        if (welcomeName) welcomeName.textContent = displayName;
        if (userName) userName.textContent = displayName;
        if (settingsUser) settingsUser.textContent = displayName;

        applyTheme(localStorage.getItem('mo-theme') || 'dark');

        if (themeToggle) themeToggle.addEventListener('click', toggleTheme);
        if (logoutBtn) {
            logoutBtn.addEventListener('click', function () {
                sessionStorage.removeItem('mo-user');
                localStorage.removeItem('mo-user');
                window.location.replace('../../index.jsp');
            });
        }
    };
})();
