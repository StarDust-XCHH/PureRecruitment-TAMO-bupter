(function () {
    'use strict';

    if (window.__TA_HOME_BOOTSTRAPPED__) return;
    window.__TA_HOME_BOOTSTRAPPED__ = true;

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    taApp.state = taApp.state || {};

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
            throw new Error('[TA-HOME] module not registered: ' + name);
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
