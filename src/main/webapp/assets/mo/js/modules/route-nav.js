(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.routeNav = function initRouteNav(app) {
        const navItems = Array.from(document.querySelectorAll('#moApp .nav-item'));
        const routes = Array.from(document.querySelectorAll('#moApp .routes .route'));
        const routeOrder = ['dashboard', 'jobs', 'applicants', 'shortlist'];

        app.state = app.state || {};
        app.state.routeNav = {
            currentFlowRoute: 'dashboard',
            routeOrder: routeOrder
        };

        function scrollMoWorkspaceTop() {
            const routesEl = document.querySelector('#moApp .routes');
            const mainEl = document.querySelector('#moApp .main');
            if (routesEl) routesEl.scrollTop = 0;
            if (mainEl) mainEl.scrollTop = 0;
            window.scrollTo(0, 0);
        }

        function activateRoute(routeName, options) {
            const safe = routeName || 'dashboard';
            const fromScroll = options && options.fromScroll === true;
            navItems.forEach(function (item) {
                const active = item.dataset.route === safe;
                item.classList.toggle('active', active);
                item.setAttribute('aria-current', active ? 'page' : 'false');
            });
            routes.forEach(function (panel) {
                const active = panel.dataset.route === safe;
                panel.classList.toggle('active', active);
            });
            app.state.routeNav.currentFlowRoute = safe;
            if (!fromScroll && safe === 'applicants' && typeof app.onApplicantsRouteActivate === 'function') {
                app.onApplicantsRouteActivate();
            }
            if (!fromScroll && safe === 'shortlist' && typeof app.onShortlistRouteActivate === 'function') {
                app.onShortlistRouteActivate();
            }
            scrollMoWorkspaceTop();
        }

        navItems.forEach(function (item) {
            item.addEventListener('click', function () {
                const route = item.dataset.route;
                if (app.state.settings && app.state.settings.guideActive && typeof app.onGuideRouteNav === 'function') {
                    app.onGuideRouteNav(route);
                }
                activateRoute(route);
            });
        });

        document.querySelectorAll('#moApp [data-mo-go-route]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                const r = btn.getAttribute('data-mo-go-route');
                if (!r) return;
                const targetNav = document.querySelector('#moApp .nav-item[data-route="' + r + '"]');
                if (targetNav) {
                    targetNav.click();
                } else if (typeof app.activateRoute === 'function') {
                    activateRoute(r);
                }
            });
        });

        app.activateRoute = activateRoute;
        activateRoute('dashboard');
    };
})();
