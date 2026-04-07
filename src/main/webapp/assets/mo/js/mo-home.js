(function () {
    'use strict';

    if (window.__MO_HOME_BOOTSTRAPPED__) return;
    window.__MO_HOME_BOOTSTRAPPED__ = true;

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};
    moApp.state = moApp.state || {};

    function runModule(name) {
        if (typeof modules[name] === 'function') {
            modules[name](moApp);
        }
    }

    [
        'settings',
        'routeNav',
        'modal',
        'jobBoard',
        'applicants',
        'dashboard',
        'profile',
        'onboarding'
    ].forEach(runModule);
})();
