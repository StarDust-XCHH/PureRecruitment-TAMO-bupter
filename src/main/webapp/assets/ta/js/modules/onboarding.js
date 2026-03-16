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
            { title: '完善你的档案信息', desc: '先补全个人资料与技能标签，让岗位推荐更精准。', selector: '.nav-item[data-route="profile"]', route: 'profile' },
            { title: '筛选并申请岗位', desc: '在职位大厅查看匹配度、职位详情与投递入口。', selector: '.nav-item[data-route="jobs"]', route: 'jobs' },
            { title: '查看申请状态', desc: '随时跟踪各个岗位的申请状态与反馈。', selector: '.nav-item[data-route="status"]', route: 'status' },
            { title: '打开设置中心', desc: '点击右上角用户入口，随时修改资料、头像、密码与主题。', selector: '#userTrigger', route: 'profile' }
        ];

        const guideRouteLookup = new Map(guideSteps.filter((step) => step.route).map((step, index) => [step.route, index]));
        const navItemLookup = new Map();
        navItems.forEach((item) => navItemLookup.set(item.dataset.route, item));

        let guideIndex = 0;
        const guidePadding = 12;

        function setGuideActive(active) {
            if (app.state.settings) {
                app.state.settings.guideActive = active;
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

        function placeGuideCard(rect) {
            if (!rect || !guideCard) return;
            const leftPos = rect.right + 40;
            const topPos = rect.top + rect.height / 2;
            guideCard.style.position = 'fixed';
            guideCard.style.left = leftPos + 'px';
            guideCard.style.top = topPos + 'px';
            guideCard.style.transform = 'translateY(-50%)';
            guideCard.style.margin = '0';
        }

        function setArrow(rect) {
            if (!rect || !guideArrow) return;
            guideArrow.className = 'guide-arrow';
            const leftPos = rect.right + 20;
            const topPos = rect.top + rect.height / 2 - 10;
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
            const step = guideSteps[guideIndex];
            const target = document.querySelector(step.selector);
            if (!target || !guideHighlight || !guideTitle || !guideDesc || !guideProgress || !guideBack) return;

            const rect = getSafeRect(target);
            guideHighlight.style.width = (rect.width + guidePadding) + 'px';
            guideHighlight.style.height = (rect.height + guidePadding) + 'px';
            guideHighlight.style.left = (rect.left - guidePadding / 2) + 'px';
            guideHighlight.style.top = (rect.top - guidePadding / 2) + 'px';

            placeGuideCard(rect);
            setArrow(rect);

            guideTitle.textContent = step.title;
            guideDesc.textContent = step.desc;
            guideProgress.innerHTML = '引导 ' + (guideIndex + 1) + ' / ' + guideSteps.length;
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
                appRoot?.classList.remove('dimmed');
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
            const step = guideSteps[guideIndex];

            if (step.route) {
                const targetNav = document.querySelector('.nav-item[data-route="' + step.route + '"]');
                if (targetNav && !targetNav.classList.contains('active')) {
                    targetNav.click();
                }
            }
            requestAnimationFrame(() => {
                positionGuide();
                setTimeout(positionGuide, 120);
            });
        }

        function onGuideRouteNav(route) {
            if (!app.state.settings?.guideActive) return;
            const index = guideRouteLookup.get(route);
            if (index !== undefined && index !== guideIndex) {
                guideIndex = index;
                requestAnimationFrame(() => positionGuide());
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

            requestAnimationFrame(() => {
                setTimeout(() => {
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
