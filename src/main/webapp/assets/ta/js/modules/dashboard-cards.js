(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.dashboardCards = function initDashboardCardsModule(app) {
        const heroBadge = document.getElementById('heroBadge');
        const heroTitle = document.getElementById('heroTitle');
        const heroDesc = document.getElementById('heroDesc');
        const heroPrimary = document.getElementById('heroPrimary');
        const heroSecondary = document.getElementById('heroSecondary');
        const applyButtons = Array.from(document.querySelectorAll('.apply-btn, #jobDetailApplyBtn'));

        const heroScenarios = [
            {
                key: 'urgent',
                badge: '⏰ Action needed',
                title: 'Seele, you have a microprocessors TA interview tomorrow afternoon.',
                desc: 'Your instructor asked you to prepare a short demo on C and low-level communication. Open the guide for tips and what to bring.',
                primary: 'Interview details',
                secondary: 'I am ready',
                modalTarget: 'planner',
                routeJump: 'profile'
            },
            {
                key: 'opportunity',
                badge: '✨ Strong match',
                title: 'A new role lines up closely with your skills.',
                desc: 'An Autonomous Navigation & Robotics TA opening just went live. Based on your background in ROS, SLAM, and STM32 deployment, you are about a 95% match and competition looks light so far.',
                primary: 'Apply now',
                secondary: 'View posting',
                routeJump: 'jobs'
            },
            {
                key: 'growth',
                badge: '💡 Stand out more',
                title: 'Your hardware experience is solid—one more detail could lift your profile.',
                desc: 'Overall strength is around 84%. Adding specifics on your recent 2D LiDAR + IMU fusion project could push you past 90%.',
                primary: 'Update profile',
                secondary: 'Remind me later',
                modalTarget: 'checklist',
                routeJump: 'profile'
            }
        ];

        let heroIndex = 0;

        function renderHeroScenario(index) {
            const scenario = heroScenarios[index];
            if (!scenario || !heroBadge || !heroTitle || !heroDesc || !heroPrimary || !heroSecondary) return;
            heroBadge.textContent = scenario.badge;
            heroTitle.textContent = scenario.title;
            heroDesc.textContent = scenario.desc;
            heroPrimary.textContent = scenario.primary;
            heroSecondary.textContent = scenario.secondary;
            heroPrimary.dataset.modalTarget = scenario.modalTarget || '';
            heroPrimary.dataset.jump = scenario.routeJump || '';
            heroSecondary.dataset.next = String((index + 1) % heroScenarios.length);
        }

        function runHeroPrimary() {
            const modalTarget = heroPrimary?.dataset.modalTarget;
            const routeJump = heroPrimary?.dataset.jump;
            if (routeJump) {
                const targetNav = document.querySelector('.nav-item[data-route="' + routeJump + '"]');
                targetNav?.click();
            }
            if (modalTarget) {
                app.openModal?.(modalTarget);
            }
        }

        heroPrimary?.addEventListener('click', (event) => {
            event.stopPropagation();
            runHeroPrimary();
        });

        heroSecondary?.addEventListener('click', (event) => {
            event.stopPropagation();
            const next = Number(heroSecondary.dataset.next || 0);
            heroIndex = Number.isNaN(next) ? 0 : next;
            renderHeroScenario(heroIndex);
        });

        applyButtons.forEach((btn) => {
            btn.addEventListener('click', (event) => {
                if (btn.classList.contains('applied')) return;
                const ripple = document.createElement('span');
                ripple.className = 'ripple';
                const rect = btn.getBoundingClientRect();
                ripple.style.left = (event.clientX - rect.left) + 'px';
                ripple.style.top = (event.clientY - rect.top) + 'px';
                btn.appendChild(ripple);
                setTimeout(() => ripple.remove(), 600);

                for (let i = 0; i < 6; i += 1) {
                    const particle = document.createElement('span');
                    particle.className = 'micro-particle';
                    particle.style.left = (rect.width / 2) + 'px';
                    particle.style.top = (rect.height / 2) + 'px';
                    particle.style.setProperty('--dx', ((Math.random() - 0.5) * 60) + 'px');
                    particle.style.setProperty('--dy', ((Math.random() - 0.5) * 60) + 'px');
                    btn.appendChild(particle);
                    setTimeout(() => particle.remove(), 600);
                }

                btn.classList.add('applied');
                btn.textContent = btn.dataset.applyLabelDone || '✓ Applied';
                if (btn.id === 'jobDetailApplyBtn' && app.jobDetailStatus) {
                    app.jobDetailStatus.textContent = 'Applied';
                }
            });
        });

        renderHeroScenario(heroIndex);
    };
})();
