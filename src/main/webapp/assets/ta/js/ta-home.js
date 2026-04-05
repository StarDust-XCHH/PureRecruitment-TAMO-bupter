(function () {
    'use strict';

    if (window.__TA_HOME_BOOTSTRAPPED__) return;
    window.__TA_HOME_BOOTSTRAPPED__ = true;

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    taApp.state = taApp.state || {};
    taApp.state.appliedCourseCodes = Array.isArray(taApp.state.appliedCourseCodes)
        ? taApp.state.appliedCourseCodes
        : [];
    taApp.state.statusFocus = taApp.state.statusFocus && typeof taApp.state.statusFocus === 'object'
        ? taApp.state.statusFocus
        : null;

    taApp.setAppliedCourseCodes = function setAppliedCourseCodes(courseCodes) {
        const normalized = Array.isArray(courseCodes)
            ? courseCodes.map((code) => String(code || '').trim().toUpperCase()).filter(Boolean)
            : [];
        taApp.state.appliedCourseCodes = Array.from(new Set(normalized));
        if (typeof taApp.refreshJobBoard === 'function') {
            taApp.refreshJobBoard();
        }
    };

    taApp.getAppliedCourseCodes = function getAppliedCourseCodes() {
        return Array.isArray(taApp.state.appliedCourseCodes)
            ? taApp.state.appliedCourseCodes.slice()
            : [];
    };

    taApp.setStatusFocus = function setStatusFocus(statusFocus) {
        taApp.state.statusFocus = statusFocus && typeof statusFocus === 'object'
            ? {
                courseCode: String(statusFocus.courseCode || '').trim().toUpperCase(),
                applicationId: String(statusFocus.applicationId || '').trim(),
                pulseCount: Number(statusFocus.pulseCount || 3) || 3,
                source: String(statusFocus.source || '').trim()
            }
            : null;
    };

    taApp.consumeStatusFocus = function consumeStatusFocus() {
        const focus = taApp.state.statusFocus;
        taApp.state.statusFocus = null;
        return focus;
    };

    function getDomRefs() {
        return {
            appShell: document.getElementById('taApp'),
            appRoot: document.getElementById('appRoot') || document.getElementById('taApp'),
            root: document.documentElement,
            navItems: document.querySelectorAll('.nav-item'),
            routes: document.querySelectorAll('.route'),
            modalOverlay: document.getElementById('taModalOverlay')
        };
    }

    function runModule(name) {
        if (typeof modules[name] !== 'function') {
            throw new Error('[TA-HOME] 模块未注册：' + name);
        }
        modules[name](taApp);
    }

    taApp.dom = getDomRefs();

    [
        'settings',
        'routeNav',
        'modal',
        'profile',
        'password',
        'jobBoard',
        'dashboardCards',
        'status',
        'onboarding'
    ].forEach(runModule);
})();
