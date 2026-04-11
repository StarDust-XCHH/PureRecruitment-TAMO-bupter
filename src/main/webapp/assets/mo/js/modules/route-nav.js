(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.routeNav = function initRouteNav(app) {
        const navItems = Array.from(document.querySelectorAll('.nav-item'));
        const routes = Array.from(document.querySelectorAll('.route'));
        const routeOrder = ['dashboard', 'jobs', 'applicants'];

        app.state = app.state || {};
        app.state.routeNav = {
            scrollTicking: false,
            currentFlowRoute: 'dashboard',
            routeOrder: routeOrder
        };

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
            if (!fromScroll && safe === 'applicants') {
                const applicantsPanel = document.getElementById('route-applicants');
                if (applicantsPanel && typeof applicantsPanel.scrollIntoView === 'function') {
                    requestAnimationFrame(function () {
                        applicantsPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    });
                }
            }
        }

        function resolveFlowRouteFromScroll() {
            if (!routes.length) return app.state.routeNav.currentFlowRoute;

            const scrollTop = window.scrollY || window.pageYOffset || 0;
            const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 1;
            const docHeight = Math.max(
                document.body.scrollHeight, document.documentElement.scrollHeight,
                document.body.offsetHeight, document.documentElement.offsetHeight
            );

            if (scrollTop + viewportHeight >= docHeight - 4) {
                return routeOrder[routeOrder.length - 1];
            }

            const viewportCenter = viewportHeight * 0.42;
            let bestRoute = app.state.routeNav.currentFlowRoute;
            let bestScore = Number.NEGATIVE_INFINITY;

            routes.forEach(function (panel) {
                const route = String(panel.dataset.route || '').trim();
                if (!route || routeOrder.indexOf(route) === -1) return;

                const rect = panel.getBoundingClientRect();
                const visibleTop = Math.max(rect.top, 0);
                const visibleBottom = Math.min(rect.bottom, viewportHeight);
                const visibleHeight = Math.max(0, visibleBottom - visibleTop);
                const panelHeight = Math.max(rect.height, 1);
                const visibleRatio = visibleHeight / panelHeight;

                const panelCenter = rect.top + rect.height / 2;
                const centerDistance = Math.abs(panelCenter - viewportCenter);
                const centerScore = 1 - Math.min(centerDistance / viewportHeight, 1);

                const topBias = rect.top <= viewportCenter ? 0.04 : 0;
                const score = visibleRatio * 0.72 + centerScore * 0.28 + topBias;

                if (score > bestScore) {
                    bestScore = score;
                    bestRoute = route;
                }
            });

            return bestRoute;
        }

        function syncNavByScroll() {
            if (app.state.settings && app.state.settings.guideActive) return;
            const focusRoute = resolveFlowRouteFromScroll();
            if (!focusRoute || focusRoute === app.state.routeNav.currentFlowRoute) return;
            activateRoute(focusRoute, { fromScroll: true });
        }

        function bindFlowScrollWatcher() {
            window.addEventListener('scroll', function () {
                if (app.state.routeNav.scrollTicking) return;
                app.state.routeNav.scrollTicking = true;
                requestAnimationFrame(function () {
                    syncNavByScroll();
                    app.state.routeNav.scrollTicking = false;
                });
            }, { passive: true });
        }

        navItems.forEach(function (item) {
            item.addEventListener('click', function () {
                const route = item.dataset.route;
                if (app.state.settings && app.state.settings.guideActive && typeof app.onGuideRouteNav === 'function') {
                    app.onGuideRouteNav(route);
                }
                activateRoute(route);
                if (route !== 'applicants') {
                    const target = document.getElementById('route-' + route);
                    if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            });
        });

        app.activateRoute = activateRoute;
        activateRoute('dashboard');
        bindFlowScrollWatcher();
        requestAnimationFrame(function () {
            syncNavByScroll();
        });
        app.syncNavByScroll = syncNavByScroll;
    };
})();
