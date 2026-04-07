(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.routeNav = function initRouteNav(app) {
        const navItems = Array.from(document.querySelectorAll('.nav-item'));
        const routes = Array.from(document.querySelectorAll('.route'));

        function activateRoute(routeName) {
            const safe = routeName || 'dashboard';
            navItems.forEach(function (item) {
                const active = item.dataset.route === safe;
                item.classList.toggle('active', active);
                item.setAttribute('aria-current', active ? 'page' : 'false');
            });
            routes.forEach(function (panel) {
                const active = panel.dataset.route === safe;
                panel.classList.toggle('active', active);
            });
        }

        navItems.forEach(function (item) {
            item.addEventListener('click', function () {
                const route = item.dataset.route;
                if (app.state.settings && app.state.settings.guideActive && typeof app.onGuideRouteNav === 'function') {
                    app.onGuideRouteNav(route);
                }
                activateRoute(route);
                const target = document.getElementById('route-' + route);
                if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });
        });

        app.activateRoute = activateRoute;
        activateRoute('dashboard');
    };
})();
