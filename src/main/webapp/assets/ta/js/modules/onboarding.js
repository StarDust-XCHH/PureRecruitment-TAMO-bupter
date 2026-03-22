(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.onboarding = function initOnboardingModule(app) {
        const onboarding = document.getElementById('onboarding');
        const skipBtn = document.getElementById('skipBtn');
        const guideBack = document.getElementById('guideBack');
        const guideNext = document.getElementById('guideNext');
        const guideTitle = document.getElementById('guideTitle');
        const guideDesc = document.getElementById('guideDesc');
        const guideProgress = document.getElementById('guideProgress');
        const guideArrow = document.getElementById('guideArrow');
        const guideHighlight = document.getElementById('guideHighlight');
        const guideCard = document.getElementById('guideCard');
        const appRoot = document.getElementById('appRoot') || document.getElementById('taApp');
        const userTrigger = document.getElementById('userTrigger');
        const navItems = Array.from(document.querySelectorAll('.nav-item'));

        const guideSteps = [
            { title: 'Complete your profile', desc: 'Add your details and skills so job recommendations stay relevant.', selector: '.nav-item[data-route="profile"]', route: 'profile' },
            { title: 'Browse and apply', desc: 'Use the job hall to review fit, details, and how to apply.', selector: '.nav-item[data-route="jobs"]', route: 'jobs' },
            { title: 'Track applications', desc: 'Follow status and feedback for each role you applied to.', selector: '.nav-item[data-route="status"]', route: 'status' },
            { title: 'Open settings', desc: 'Use the user menu (top right) to edit profile, avatar, password, and theme.', selector: '#userTrigger', route: 'profile', align: 'left', scrollTarget: 'topbar', overlayMode: 'focus-topbar' }
        ];

        const guideRouteLookup = new Map(guideSteps.filter((step) => step.route).map((step, index) => [step.route, index]));
        const navItemLookup = new Map();
        navItems.forEach((item) => navItemLookup.set(item.dataset.route, item));

        let guideIndex = 0;
        const guidePadding = 12;
        const guideCardGap = 40;
        const guideArrowGap = 20;
        const viewportPadding = 24;

        function setGuideActive(active) {
            if (app.state.settings) {
                app.state.settings.guideActive = active;
            }
        }

        function getCurrentStep() {
            return guideSteps[guideIndex] || null;
        }

        function syncOverlayMode() {
            if (!onboarding) return;
            const step = getCurrentStep();
            onboarding.dataset.overlayMode = step?.overlayMode || 'default';
        }

        function setWindowScrollTop(top) {
            const safeTop = Math.max(0, Math.round(top || 0));
            document.documentElement.scrollTop = safeTop;
            document.body.scrollTop = safeTop;
            try {
                window.scrollTo({ top: safeTop, behavior: 'auto' });
            } catch (error) {
                window.scrollTo(0, safeTop);
            }
        }

        function scrollGuideStepIntoView(step) {
            if (!step?.scrollTarget) return;

            if (step.scrollTarget === 'topbar') {
                const topbar = document.querySelector('.topbar');
                const topbarTop = topbar ? topbar.offsetTop : 0;
                setWindowScrollTop(topbarTop);
                requestAnimationFrame(() => setWindowScrollTop(topbarTop));
                setTimeout(() => setWindowScrollTop(topbarTop), 80);
                return;
            }

            if (step.scrollTarget === 'top') {
                setWindowScrollTop(0);
            }
        }

        function getSafeRect(target) {
            const rect = target.getBoundingClientRect();
            return {
                left: rect.left,
                right: rect.right,
                top: rect.top,
                bottom: rect.bottom,
                width: Math.max(rect.width, 1),
                height: Math.max(rect.height, 1)
            };
        }

        function getGuidePlacement(rect) {
            if (!rect || !guideCard) return null;

            const step = getCurrentStep();
            const cardWidth = Math.min(guideCard.offsetWidth || guideCard.getBoundingClientRect().width || 460, window.innerWidth - viewportPadding * 2);
            const cardHeight = guideCard.offsetHeight || guideCard.getBoundingClientRect().height || 0;
            const viewportWidth = window.innerWidth;
            const viewportHeight = window.innerHeight;
            const targetCenterY = rect.top + rect.height / 2;
            const maxTop = Math.max(viewportPadding, viewportHeight - cardHeight - viewportPadding);
            const preferredTop = targetCenterY - cardHeight / 2;
            const top = Math.min(Math.max(preferredTop, viewportPadding), maxTop);

            const canPlaceRight = rect.right + guideCardGap + cardWidth <= viewportWidth - viewportPadding;
            const canPlaceLeft = rect.left - guideCardGap - cardWidth >= viewportPadding;
            const forceLeft = step?.align === 'left';
            const placeLeft = forceLeft ? (canPlaceLeft || !canPlaceRight) : (!canPlaceRight && canPlaceLeft);

            const left = placeLeft
                ? Math.max(viewportPadding, rect.left - guideCardGap - cardWidth)
                : Math.min(viewportWidth - viewportPadding - cardWidth, rect.right + guideCardGap);

            return {
                left,
                top,
                placeLeft,
                targetCenterY
            };
        }

        function placeGuideCard(rect) {
            if (!rect || !guideCard) return;
            const placement = getGuidePlacement(rect);
            if (!placement) return;

            guideCard.style.position = 'fixed';
            guideCard.style.left = placement.left + 'px';
            guideCard.style.top = placement.top + 'px';
            guideCard.style.transform = 'none';
            guideCard.style.margin = '0';
        }

        function setArrow(rect) {
            if (!rect || !guideArrow) return;
            const placement = getGuidePlacement(rect);
            if (!placement) return;

            guideArrow.className = placement.placeLeft ? 'guide-arrow guide-arrow-right' : 'guide-arrow';
            const leftPos = placement.placeLeft
                ? placement.left + (guideCard.offsetWidth || guideCard.getBoundingClientRect().width || 0) + guideArrowGap
                : rect.right + guideArrowGap;
            const topPos = placement.targetCenterY - 10;
            guideArrow.style.position = 'fixed';
            guideArrow.style.left = leftPos + 'px';
            guideArrow.style.top = topPos + 'px';
            guideArrow.style.margin = '0';
        }

        function applyGuideHighlight(activeRoute) {
            if (!app.state.settings?.guideActive) return;
            navItems.forEach((item) => item.classList.remove('highlight'));
            if (activeRoute === 'settings') {
                userTrigger?.classList.add('highlight');
                return;
            }
            userTrigger?.classList.remove('highlight');
            const target = navItemLookup.get(activeRoute);
            if (target) target.classList.add('highlight');
        }

        function positionGuide() {
            const step = getCurrentStep();
            const target = step ? document.querySelector(step.selector) : null;
            if (!target || !guideHighlight || !guideTitle || !guideDesc || !guideProgress || !guideBack) return;

            syncOverlayMode();
            const rect = getSafeRect(target);
            guideHighlight.style.width = (rect.width + guidePadding) + 'px';
            guideHighlight.style.height = (rect.height + guidePadding) + 'px';
            guideHighlight.style.left = (rect.left - guidePadding / 2) + 'px';
            guideHighlight.style.top = (rect.top - guidePadding / 2) + 'px';

            placeGuideCard(rect);
            setArrow(rect);

            guideTitle.textContent = step.title;
            guideDesc.textContent = step.desc;
            guideProgress.innerHTML = 'Step ' + (guideIndex + 1) + ' / ' + guideSteps.length;
            guideBack.disabled = guideIndex === 0;
            guideBack.style.opacity = guideIndex === 0 ? '0.4' : '1';

            if (step.selector === '#userTrigger') applyGuideHighlight('settings');
            else if (step.route) applyGuideHighlight(step.route);
        }

        function closeGuide() {
            if (onboarding) onboarding.style.opacity = '0';
            setTimeout(() => {
                onboarding?.classList.remove('show');
                if (onboarding) onboarding.style.display = 'none';
                if (onboarding) delete onboarding.dataset.overlayMode;
                appRoot?.classList.remove('dimmed');
                appRoot?.classList.remove('dimmed-topbar-focus');
                appRoot?.classList.add('pop');
                setTimeout(() => appRoot?.classList.remove('pop'), 400);
            }, 400);

            setGuideActive(false);
            navItems.forEach((item) => item.classList.remove('highlight'));
            userTrigger?.classList.remove('highlight');

            const userData = typeof app.getUserData === 'function' ? app.getUserData() : null;
            if (userData) {
                userData.isFirstLogin = false;
                if (userData.onboardingKey) {
                    localStorage.setItem(userData.onboardingKey, '1');
                }
                app.setUserData?.(userData);
            }

            window.removeEventListener('resize', positionGuide);
            window.removeEventListener('scroll', positionGuide, true);

            try {
                window.scrollTo({ top: 0, behavior: 'smooth' });
            } catch (error) {
                document.documentElement.scrollTop = 0;
                document.body.scrollTop = 0;
            }
        }

        function goToStep(stepIndex) {
            if (stepIndex < 0 || stepIndex >= guideSteps.length) return;
            guideIndex = stepIndex;
            const step = getCurrentStep();

            syncOverlayMode();
            appRoot?.classList.toggle('dimmed-topbar-focus', step?.overlayMode === 'focus-topbar');
            scrollGuideStepIntoView(step);

            if (step?.route) {
                const targetNav = document.querySelector('.nav-item[data-route="' + step.route + '"]');
                if (targetNav && !targetNav.classList.contains('active')) {
                    targetNav.click();
                }
            }
            requestAnimationFrame(() => {
                scrollGuideStepIntoView(step);
                positionGuide();
                setTimeout(() => {
                    scrollGuideStepIntoView(step);
                    positionGuide();
                }, 120);
            });
        }

        function onGuideRouteNav(route) {
            if (!app.state.settings?.guideActive) return;
            const index = guideRouteLookup.get(route);
            if (index !== undefined && index !== guideIndex) {
                guideIndex = index;
                const step = getCurrentStep();
                syncOverlayMode();
                appRoot?.classList.toggle('dimmed-topbar-focus', step?.overlayMode === 'focus-topbar');
                scrollGuideStepIntoView(step);
                requestAnimationFrame(() => {
                    scrollGuideStepIntoView(step);
                    positionGuide();
                });
            }
        }

        guideNext?.addEventListener('click', () => {
            if (guideIndex < guideSteps.length - 1) goToStep(guideIndex + 1);
            else closeGuide();
        });

        guideBack?.addEventListener('click', () => {
            if (guideIndex > 0) goToStep(guideIndex - 1);
        });

        skipBtn?.addEventListener('click', closeGuide);

        const userData = typeof app.getUserData === 'function' ? app.getUserData() : null;
        const welcomeCardEl = document.getElementById('welcomeCard');
        const isFirstLogin = userData ? (userData.isFirstLogin === true || userData.isFirstLogin === 'true') : true;
        app.debugOnboardingLog?.('init-check', { isFirstLogin });

        if (welcomeCardEl) {
            if (isFirstLogin) welcomeCardEl.classList.add('show');
            else welcomeCardEl.classList.remove('show');
        }

        if (isFirstLogin) {
            setGuideActive(true);
            if (onboarding) onboarding.style.display = 'block';
            appRoot?.classList.add('dimmed');
            syncOverlayMode();
            appRoot?.classList.toggle('dimmed-topbar-focus', getCurrentStep()?.overlayMode === 'focus-topbar');
            scrollGuideStepIntoView(getCurrentStep());

            requestAnimationFrame(() => {
                setTimeout(() => {
                    scrollGuideStepIntoView(getCurrentStep());
                    positionGuide();
                    onboarding?.classList.add('show');

                    const onboardingStyle = onboarding ? window.getComputedStyle(onboarding) : null;
                    const guideCardStyle = guideCard ? window.getComputedStyle(guideCard) : null;
                    app.debugOnboardingLog?.('guide-opened', {
                        guideIndex,
                        onboardingDisplay: onboardingStyle?.display,
                        onboardingOpacity: onboardingStyle?.opacity,
                        onboardingZIndex: onboardingStyle?.zIndex,
                        guideCardLeft: guideCardStyle?.left,
                        guideCardTop: guideCardStyle?.top,
                        guideCardDisplay: guideCardStyle?.display,
                        guideCardVisibility: guideCardStyle?.visibility
                    });

                    window.addEventListener('resize', positionGuide);
                    window.addEventListener('scroll', positionGuide, true);
                }, 150);
            });
        } else {
            app.debugOnboardingLog?.('skip-guide', { reason: 'isFirstLogin=false' });
        }

        app.applyGuideHighlight = applyGuideHighlight;
        app.onGuideRouteNav = onGuideRouteNav;
    };
})();
