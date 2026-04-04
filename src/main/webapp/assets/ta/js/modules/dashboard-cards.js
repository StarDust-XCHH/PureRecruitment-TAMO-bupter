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
                badge: '⏰ 紧急待办',
                title: 'Seele，明天下午有一场微处理器课程的 TA 面试。',
                desc: '教授留言提醒你需要准备一段关于 C 语言和底层通信逻辑的试讲。点击下方按钮查看面试指南与注意事项。',
                primary: '查看面试详情',
                secondary: '已准备妥当',
                modalTarget: 'planner',
                routeJump: 'profile'
            },
            {
                key: 'opportunity',
                badge: '✨ 极佳匹配',
                title: '发现一个与你技能点高度重合的新岗位！',
                desc: '刚刚发布了一个【自主导航与机器人学 TA】的空缺。系统分析了你的历史经历，发现你在 ROS、SLAM 算法以及 STM32 硬件部署方面的实战经验与该岗位 95% 匹配，目前竞争者较少。',
                primary: '一键投递简历',
                secondary: '查看岗位详情',
                routeJump: 'jobs'
            },
            {
                key: 'growth',
                badge: '💡 竞争力提升',
                title: '你的硬件开发经历很棒，但还可以更出彩。',
                desc: '目前简历的整体竞争力为 84%。如果能在简历中补充一下你最近在做的那个带有 2D 激光雷达和 IMU 传感器融合的项目细节，击败率有望突破 90%。',
                primary: '立即去更新简历',
                secondary: '稍后处理',
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
            });
        });

        renderHeroScenario(heroIndex);
    };
})();
