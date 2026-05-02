(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.onboarding = function initMoOnboardingModule(app) {
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
        const appRoot = document.getElementById('moApp');
        const userTrigger = document.getElementById('userTrigger');
        const navItems = Array.from(document.querySelectorAll('.nav-item'));

        function t(zh, en) {
            return typeof app.t === 'function' ? app.t(zh, en) : zh;
        }

        function buildGuideSteps() {
            return [
                {
                    title: t('从总览了解招聘态势', 'See recruitment metrics at a glance'),
                    desc: t('在「招聘总览」查看开放岗位、候选规模与待决策数量。', 'On the Recruitment Dashboard, review open modules, candidate volume, and pending decisions.'),
                    selector: '.nav-item[data-route="dashboard"]',
                    route: 'dashboard'
                },
                {
                    title: t('管理课程与发布岗位', 'Manage modules and post openings'),
                    desc: t('在「课程管理」中浏览列表，并点击「发布岗位」在弹窗中填写课程信息、技能标签与招聘状态。', 'In Module Management, browse the list and tap Post to enter module details, skills, and status.'),
                    selector: '.nav-item[data-route="jobs"]',
                    route: 'jobs'
                },
                {
                    title: t('筛选应聘者并做出决策', 'Screen applicants and decide'),
                    desc: t('在「人选投递」查看资料并完成录用或拒绝；意向人选可在「候选短名单」管理。', 'In Candidate applications, review profiles and hire or reject. Use Shortlist for your shortlist.'),
                    selector: '.nav-item[data-route="applicants"]',
                    route: 'applicants'
                },
                {
                    title: t('打开账号与界面设置', 'Account and display settings'),
                    desc: t('点击右上角用户入口，查看账号信息与主题等偏好。', 'Use the top-right user menu for account details and theme preferences.'),
                    selector: '#userTrigger',
                    route: 'profile',
                    align: 'left',
                    scrollTarget: 'topbar',
                    overlayMode: 'focus-topbar'
                }
            ];
        }

        const guideRouteLookup = new Map();
        const navItemLookup = new Map();
        navItems.forEach(function (item) {
            navItemLookup.set(item.dataset.route, item);
        });

        let guideIndex = 0;
        const guidePadding = 12;
        const guideCardGap = 40;
        const guideArrowGap = 20;
        const viewportPadding = 24;

        function refreshGuideRouteLookup() {
            guideRouteLookup.clear();
            buildGuideSteps().forEach(function (step, index) {
                if (step.route) guideRouteLookup.set(step.route, index);
            });
        }

        function setGuideActive(active) {
            if (app.state.settings) {
                app.state.settings.guideActive = active;
            }
        }

        function getCurrentStep() {
            return buildGuideSteps()[guideIndex] || null;
        }

        function syncOverlayMode() {
            if (!onboarding) return;
            const step = getCurrentStep();
            onboarding.dataset.overlayMode = step && step.overlayMode ? step.overlayMode : 'default';
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
            if (!step || !step.scrollTarget) return;

            if (step.scrollTarget === 'topbar') {
                const topbar = document.querySelector('.topbar');
                const topbarTop = topbar ? topbar.offsetTop : 0;
                setWindowScrollTop(topbarTop);
                requestAnimationFrame(function () { setWindowScrollTop(topbarTop); });
                setTimeout(function () { setWindowScrollTop(topbarTop); }, 80);
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
            const cardWidth = Math.min(
                guideCard.offsetWidth || guideCard.getBoundingClientRect().width || 460,
                window.innerWidth - viewportPadding * 2
            );
            const cardHeight = guideCard.offsetHeight || guideCard.getBoundingClientRect().height || 0;
            const viewportWidth = window.innerWidth;
            const viewportHeight = window.innerHeight;
            const targetCenterY = rect.top + rect.height / 2;
            const maxTop = Math.max(viewportPadding, viewportHeight - cardHeight - viewportPadding);
            const preferredTop = targetCenterY - cardHeight / 2;
            const top = Math.min(Math.max(preferredTop, viewportPadding), maxTop);

            const canPlaceRight = rect.right + guideCardGap + cardWidth <= viewportWidth - viewportPadding;
            const canPlaceLeft = rect.left - guideCardGap - cardWidth >= viewportPadding;
            const forceLeft = step && step.align === 'left';
            const placeLeft = forceLeft ? (canPlaceLeft || !canPlaceRight) : (!canPlaceRight && canPlaceLeft);

            const left = placeLeft
                ? Math.max(viewportPadding, rect.left - guideCardGap - cardWidth)
                : Math.min(viewportWidth - viewportPadding - cardWidth, rect.right + guideCardGap);

            return {
                left: left,
                top: top,
                placeLeft: placeLeft,
                targetCenterY: targetCenterY
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
            if (!app.state.settings || !app.state.settings.guideActive) return;
            navItems.forEach(function (item) { item.classList.remove('highlight'); });
            if (activeRoute === 'settings') {
                if (userTrigger) userTrigger.classList.add('highlight');
                return;
            }
            if (userTrigger) userTrigger.classList.remove('highlight');
            const target = navItemLookup.get(activeRoute);
            if (target) target.classList.add('highlight');
        }

        function positionGuide() {
            const step = getCurrentStep();
            const target = step ? document.querySelector(step.selector) : null;
            if (!target || !guideHighlight || !guideTitle || !guideDesc || !guideProgress || !guideBack || !guideNext) return;

            syncOverlayMode();
            const rect = getSafeRect(target);
            guideHighlight.style.width = (rect.width + guidePadding) + 'px';
            guideHighlight.style.height = (rect.height + guidePadding) + 'px';
            guideHighlight.style.left = (rect.left - guidePadding / 2) + 'px';
            guideHighlight.style.top = (rect.top - guidePadding / 2) + 'px';

            placeGuideCard(rect);
            setArrow(rect);

            const steps = buildGuideSteps();
            guideTitle.textContent = step.title;
            guideDesc.textContent = step.desc;
            guideProgress.textContent = t('引导 ', 'Guide ') + (guideIndex + 1) + ' / ' + steps.length;
            guideBack.disabled = guideIndex === 0;
            guideBack.style.opacity = guideIndex === 0 ? '0.4' : '1';
            guideNext.textContent = guideIndex === steps.length - 1 ? t('完成', 'Done') : t('下一步', 'Next');

            if (step.selector === '#userTrigger') applyGuideHighlight('settings');
            else if (step.route) applyGuideHighlight(step.route);
        }

        function closeGuide() {
            if (onboarding) onboarding.style.opacity = '0';
            setTimeout(function () {
                if (onboarding) {
                    onboarding.classList.remove('show');
                    onboarding.style.display = 'none';
                    delete onboarding.dataset.overlayMode;
                }
                if (appRoot) {
                    appRoot.classList.remove('dimmed');
                    appRoot.classList.remove('dimmed-topbar-focus');
                    appRoot.classList.add('pop');
                    setTimeout(function () { appRoot.classList.remove('pop'); }, 400);
                }
            }, 400);

            setGuideActive(false);
            navItems.forEach(function (item) { item.classList.remove('highlight'); });
            if (userTrigger) userTrigger.classList.remove('highlight');

            const userData = typeof app.getUserData === 'function' ? app.getUserData() : null;
            if (userData) {
                userData.isFirstLogin = false;
                var moId = String(userData.moId || '').trim();
                if (!userData.onboardingKey && moId) {
                    userData.onboardingKey = 'mo-onboarding:' + moId;
                }
                if (userData.onboardingKey) {
                    localStorage.setItem(userData.onboardingKey, '1');
                }
                if (typeof app.setUserData === 'function') app.setUserData(userData);
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
            const steps = buildGuideSteps();
            if (stepIndex < 0 || stepIndex >= steps.length) return;
            guideIndex = stepIndex;
            const step = getCurrentStep();

            syncOverlayMode();
            if (appRoot) {
                appRoot.classList.toggle('dimmed-topbar-focus', !!(step && step.overlayMode === 'focus-topbar'));
            }
            scrollGuideStepIntoView(step);

            if (step && step.route) {
                const targetNav = document.querySelector('.nav-item[data-route="' + step.route + '"]');
                if (targetNav && !targetNav.classList.contains('active')) {
                    targetNav.click();
                }
            }
            requestAnimationFrame(function () {
                scrollGuideStepIntoView(step);
                positionGuide();
                setTimeout(function () {
                    scrollGuideStepIntoView(step);
                    positionGuide();
                }, 120);
            });
        }

        function onGuideRouteNav(route) {
            if (!app.state.settings || !app.state.settings.guideActive) return;
            const index = guideRouteLookup.get(route);
            if (index !== undefined && index !== guideIndex) {
                guideIndex = index;
                const step = getCurrentStep();
                syncOverlayMode();
                if (appRoot) {
                    appRoot.classList.toggle('dimmed-topbar-focus', !!(step && step.overlayMode === 'focus-topbar'));
                }
                scrollGuideStepIntoView(step);
                requestAnimationFrame(function () {
                    scrollGuideStepIntoView(step);
                    positionGuide();
                });
            }
        }

        guideNext && guideNext.addEventListener('click', function () {
            const steps = buildGuideSteps();
            if (guideIndex < steps.length - 1) goToStep(guideIndex + 1);
            else closeGuide();
        });

        guideBack && guideBack.addEventListener('click', function () {
            if (guideIndex > 0) goToStep(guideIndex - 1);
        });

        skipBtn && skipBtn.addEventListener('click', closeGuide);

        function resolveMoOnboarding(userData) {
            if (!userData || typeof userData !== 'object') {
                return { show: false, reason: 'no-user' };
            }
            var moId = String(userData.moId || '').trim();
            var doneKey = userData.onboardingKey || (moId ? 'mo-onboarding:' + moId : '');
            if (doneKey && localStorage.getItem(doneKey) === '1') {
                return { show: false, reason: 'local-done' };
            }
            var v = userData.isFirstLogin;
            if (v === false || v === 'false' || v === 0 || v === '0') {
                return { show: false, reason: 'not-first-login' };
            }
            if (v === true || v === 'true' || v === 1 || v === '1') {
                return { show: true, reason: 'flag-true' };
            }
            return { show: true, reason: 'flag-missing' };
        }

        var storedUser = typeof app.getMoUser === 'function' ? app.getMoUser() : null;
        var stateUser = typeof app.getUserData === 'function' ? app.getUserData() : null;
        var userData = storedUser || stateUser;
        var onboardingDecision = resolveMoOnboarding(userData);
        var isFirstLogin = onboardingDecision.show;

        var welcomeCardEl = document.getElementById('welcomeCard');

        app.debugOnboardingLog && app.debugOnboardingLog('init-check', {
            isFirstLogin: isFirstLogin,
            reason: onboardingDecision.reason,
            hasIsFirstLoginKey: userData ? Object.prototype.hasOwnProperty.call(userData, 'isFirstLogin') : false
        });

        refreshGuideRouteLookup();

        if (welcomeCardEl) {
            if (isFirstLogin) welcomeCardEl.classList.add('show');
            else welcomeCardEl.classList.remove('show');
        }

        if (isFirstLogin) {
            setGuideActive(true);
            if (onboarding) onboarding.style.display = 'block';
            if (appRoot) appRoot.classList.add('dimmed');
            syncOverlayMode();
            if (appRoot) {
                appRoot.classList.toggle('dimmed-topbar-focus', !!(getCurrentStep() && getCurrentStep().overlayMode === 'focus-topbar'));
            }
            scrollGuideStepIntoView(getCurrentStep());

            requestAnimationFrame(function () {
                setTimeout(function () {
                    scrollGuideStepIntoView(getCurrentStep());
                    positionGuide();
                    onboarding && onboarding.classList.add('show');

                    window.addEventListener('resize', positionGuide);
                    window.addEventListener('scroll', positionGuide, true);
                }, 150);
            });
        } else {
            app.debugOnboardingLog && app.debugOnboardingLog('skip-guide', { reason: 'isFirstLogin=false' });
        }

        app.onGuideRouteNav = onGuideRouteNav;
        app.refreshOnboardingLanguage = function refreshOnboardingLanguage() {
            refreshGuideRouteLookup();
            if (app.state.settings && app.state.settings.guideActive) {
                positionGuide();
            }
        };
    };
})();
