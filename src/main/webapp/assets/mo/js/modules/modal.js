(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.modal = function initModal(app) {
        const overlay = document.getElementById('taModalOverlay');
        const panels = overlay ? Array.from(overlay.querySelectorAll('.ta-modal-panel')) : [];
        const userTrigger = document.getElementById('userTrigger');

        function openModal(name) {
            if (!overlay) return;
            var isAiAssistant = name === 'mo-ai-assistant';
            overlay.classList.add('show');
            overlay.classList.toggle('ai-assistant-overlay', isAiAssistant);
            overlay.setAttribute('aria-hidden', 'false');
            panels.forEach(function (panel) {
                var active = panel.dataset.modal === name;
                panel.classList.toggle('active', active);
                panel.classList.toggle('is-landscape-modal', active && isAiAssistant);
            });
            if (name === 'settings' && userTrigger) userTrigger.setAttribute('aria-expanded', 'true');
        }

        function closeAllModals() {
            if (!overlay) return;
            overlay.classList.remove('show', 'ai-assistant-overlay');
            overlay.setAttribute('aria-hidden', 'true');
            panels.forEach(function (panel) {
                panel.classList.remove('active', 'is-landscape-modal');
            });
            if (userTrigger) userTrigger.setAttribute('aria-expanded', 'false');
        }

        document.querySelectorAll('[data-modal-target]').forEach(function (node) {
            node.addEventListener('click', function () {
                openModal(node.dataset.modalTarget);
            });
        });

        if (userTrigger) {
            userTrigger.addEventListener('click', function () { openModal('settings'); });
        }
        if (overlay) {
            overlay.addEventListener('click', function (event) {
                if (event.target === overlay) closeAllModals();
            });
        }
        panels.forEach(function (panel) {
            panel.querySelectorAll('[data-modal-close]').forEach(function (btn) {
                btn.addEventListener('click', closeAllModals);
            });
        });

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') closeAllModals();
        });

        app.openModal = openModal;
        app.closeAllModals = closeAllModals;
    };
})();
