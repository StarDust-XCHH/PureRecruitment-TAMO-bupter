(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.routeNav = function initRouteNavModule(app) {
        const navItems = Array.from(document.querySelectorAll('.nav-item'));
        const routes = Array.from(document.querySelectorAll('.route'));
        const routeOrder = ['profile', 'jobs', 'status'];
        const DEBUG_ROUTE = true;

        app.state.routeNav = {
            navItems,
            routes,
            routeOrder,
            currentFlowRoute: 'profile',
            scrollTicking: false
        };

        function debugRouteLog() {
            if (!DEBUG_ROUTE) return;
            console.log('[TA-ROUTE]', ...arguments);
        }

        function setActiveNav(route) {
            const safeRoute = String(route || '').trim() || 'profile';
            navItems.forEach((item) => {
                const active = item.dataset.route === safeRoute;
                item.classList.toggle('active', active);
                item.setAttribute('aria-current', active ? 'page' : 'false');
            });
            if (typeof app.applyGuideHighlight === 'function') app.applyGuideHighlight(safeRoute);
        }

        function isRouteInFlow(route, currentRoute) {
            const routeIndex = routeOrder.indexOf(route);
            const currentIndex = routeOrder.indexOf(currentRoute);
            if (routeIndex === -1 || currentIndex === -1) return false;
            return routeIndex <= currentIndex;
        }

        function activateRoute(route) {
            const safeRoute = String(route || '').trim() || 'profile';
            routes.forEach((panel) => {
                const panelRoute = String(panel.id || '').replace('route-', '');
                const active = isRouteInFlow(panelRoute, safeRoute);
                panel.classList.toggle('active', active);
                panel.hidden = false;
            });
            setActiveNav(safeRoute);
            app.state.routeNav.currentFlowRoute = safeRoute;
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

            routes.forEach((panel) => {
                const route = String(panel.id || '').replace('route-', '');
                if (!route || !routeOrder.includes(route)) return;

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
            app.state.routeNav.currentFlowRoute = focusRoute;
            activateRoute(focusRoute);
            debugRouteLog('scroll focus route =>', focusRoute);
        }

        function bindFlowScrollWatcher() {
            window.addEventListener('scroll', () => {
                if (app.state.routeNav.scrollTicking) return;
                app.state.routeNav.scrollTicking = true;
                requestAnimationFrame(() => {
                    syncNavByScroll();
                    app.state.routeNav.scrollTicking = false;
                });
            }, { passive: true });
        }

        navItems.forEach((item) => {
            item.addEventListener('click', (event) => {
                const route = String(item.getAttribute('data-route') || '').trim();
                debugRouteLog('nav click', { route, text: (item.textContent || '').trim() });
                if (!route) return;

                if (app.state.settings && app.state.settings.guideActive && typeof app.onGuideRouteNav === 'function') {
                    app.onGuideRouteNav(route);
                }

                activateRoute(route);
                requestAnimationFrame(() => {
                    const panel = document.getElementById('route-' + route);
                    if (panel) panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
                });
            });
        });

        document.querySelectorAll('[data-jump]').forEach((btn) => {
            btn.addEventListener('click', (event) => {
                event.stopPropagation();
                const targetRoute = btn.dataset.jump;
                const targetNav = document.querySelector('.nav-item[data-route="' + targetRoute + '"]');
                if (targetNav) targetNav.click();
            });
        });

        const initialNav = document.querySelector('.nav-item.active') || document.querySelector('.nav-item[data-route="profile"]');
        const initialRoute = initialNav ? initialNav.dataset.route : 'profile';
        app.state.routeNav.currentFlowRoute = String(initialRoute || '').trim() || 'profile';
        activateRoute(app.state.routeNav.currentFlowRoute);
        bindFlowScrollWatcher();
        requestAnimationFrame(() => syncNavByScroll());

        app.debugRouteLog = debugRouteLog;
        app.setActiveNav = setActiveNav;
        app.activateRoute = activateRoute;
        app.navigateToRoute = function navigateToRoute(route, options) {
            const safeRoute = String(route || '').trim() || 'profile';
            const panel = document.getElementById('route-' + safeRoute);
            activateRoute(safeRoute);
            const shouldSmoothScroll = options?.smooth !== false;
            const onAfterNavigate = typeof options?.afterNavigate === 'function' ? options.afterNavigate : null;
            if (panel) {
                panel.scrollIntoView({
                    behavior: shouldSmoothScroll ? 'smooth' : 'auto',
                    block: 'start'
                });
            }
            if (onAfterNavigate) {
                const delay = shouldSmoothScroll ? 380 : 0;
                window.setTimeout(() => onAfterNavigate(panel), delay);
            }
        };
        app.syncNavByScroll = syncNavByScroll;
        app.bindFlowScrollWatcher = bindFlowScrollWatcher;
    };
})();
