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

        function t(zh, en) {
            return typeof app.t === 'function' ? app.t(zh, en) : zh;
        }

        function buildHeroScenarios() {
            return [
                {
                    key: 'assistant',
                    badge: t('🤖 AI 助理', '🤖 AI Assistant'),
                    title: t('你的 TA 智能助手已经就绪。', 'Your TA smart assistant is ready.'),
                    desc: t(
                        '现在可以在工作台中直接打开 AI 对话面板，后续接入 API 后即可围绕简历优化、课程材料总结、岗位匹配和试讲准备进行辅助分析。',
                        'You can now open the AI chat panel directly from the workspace and use it for resume improvement, material summarization, role matching, and mock teaching preparation.'
                    ),
                    primary: t('打开 AI 助理', 'Open AI Assistant'),
                    secondary: t('查看下一个建议', 'View Next Suggestion'),
                    modalTarget: 'planner',
                    routeJump: 'profile'
                },
                {
                    key: 'opportunity',
                    badge: t('✨ 极佳匹配', '✨ Great Match'),
                    title: t('发现一个与你技能点高度重合的新岗位！', 'A new role closely matching your skills is available!'),
                    desc: t(
                        '刚刚发布了一个【自主导航与机器人学 TA】的空缺。系统分析了你的历史经历，发现你在 ROS、SLAM 算法以及 STM32 硬件部署方面的实战经验与该岗位 95% 匹配，目前竞争者较少。',
                        'A new Autonomous Navigation and Robotics TA role has just been posted. Based on your experience in ROS, SLAM algorithms, and STM32 deployment, the system estimates a 95% match and low current competition.'
                    ),
                    primary: t('一键投递简历', 'Apply with Resume'),
                    secondary: t('查看岗位详情', 'View Role Details'),
                    routeJump: 'jobs'
                },
                {
                    key: 'growth',
                    badge: t('💡 竞争力提升', '💡 Competitiveness Boost'),
                    title: t('你的硬件开发经历很棒，但还可以更出彩。', 'Your hardware development experience is strong, but it can stand out even more.'),
                    desc: t(
                        '目前简历的整体竞争力为 84%。如果能在简历中补充一下你最近在做的那个带有 2D 激光雷达和 IMU 传感器融合的项目细节，击败率有望突破 90%。',
                        'Your current resume competitiveness is 84%. If you add more detail about your recent project involving 2D LiDAR and IMU sensor fusion, your competitiveness could exceed 90%.'
                    ),
                    primary: t('立即去更新简历', 'Update Resume Now'),
                    secondary: t('稍后处理', 'Handle Later'),
                    modalTarget: 'checklist',
                    routeJump: 'profile'
                }
            ];
        }

        let heroIndex = 0;

        function renderHeroScenario(index) {
            const heroScenarios = buildHeroScenarios();
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
                btn.textContent = btn.dataset.applyLabelDone || t('✓ 已申请', '✓ Applied');
            });
        });

        renderHeroScenario(heroIndex);
        app.refreshLanguageBindings = function refreshLanguageBindings() {
            renderHeroScenario(heroIndex);
        };
    };
})();
