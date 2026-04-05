(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.modal = function initModalModule(app) {
        const modalOverlay = document.getElementById('taModalOverlay');
        const modalPanels = modalOverlay ? Array.from(modalOverlay.querySelectorAll('.ta-modal-panel')) : [];
        const statCards = Array.from(document.querySelectorAll('.stat-card[data-modal-target], .insight-card[data-modal-target]'));
        const userTrigger = document.getElementById('userTrigger');
        const DEBUG_MODAL = true;

        app.state.modal = {
            modalOverlay,
            modalPanels,
            statCards,
            userTrigger,
            currentModal: null
        };

        function getAppRoot() {
            return document.getElementById('appRoot') || document.getElementById('taApp');
        }

        function logModalState(stage, extra) {
            if (!DEBUG_MODAL) return;
            const overlayStyle = modalOverlay ? window.getComputedStyle(modalOverlay) : null;
            const appRoot = getAppRoot();
            console.log('[TA-MODAL]', stage, {
                hasOverlay: !!modalOverlay,
                overlayDisplay: overlayStyle && overlayStyle.display,
                overlayPointerEvents: overlayStyle && overlayStyle.pointerEvents,
                overlayZIndex: overlayStyle && overlayStyle.zIndex,
                appModalOpen: !!appRoot?.classList.contains('modal-open'),
                activePanels: modalPanels.filter((panel) => panel.classList.contains('active')).map((panel) => panel.dataset.modal),
                ...(extra || {})
            });
        }

        function logGlobalMaskState(stage, sourceEvent) {
            const onboardingEl = document.getElementById('onboarding');
            const overlayEl = modalOverlay;
            const onboardingStyle = onboardingEl ? window.getComputedStyle(onboardingEl) : null;
            const overlayStyle = overlayEl ? window.getComputedStyle(overlayEl) : null;
            const appRoot = getAppRoot();

            const pointX = window.innerWidth ? Math.floor(window.innerWidth / 2) : 0;
            const pointY = window.innerHeight ? Math.floor(window.innerHeight / 2) : 0;
            let hitClass = null;
            let hitId = null;
            let hitTag = null;

            try {
                const hit = document.elementFromPoint(pointX, pointY);
                hitClass = hit?.className;
                hitId = hit?.id;
                hitTag = hit?.tagName;
            } catch (error) {
                console.warn('[TA-MASK] elementFromPoint failed:', error);
            }

            console.log('[TA-MASK]', stage, {
                eventType: sourceEvent?.type,
                eventTarget: sourceEvent?.target?.className || sourceEvent?.target?.id || sourceEvent?.target?.tagName,
                guideActive: !!app.state.settings?.guideActive,
                appDimmed: !!appRoot?.classList.contains('dimmed'),
                appModalOpen: !!appRoot?.classList.contains('modal-open'),
                onboardingShowClass: !!onboardingEl?.classList.contains('show'),
                onboardingDisplay: onboardingStyle?.display,
                onboardingOpacity: onboardingStyle?.opacity,
                onboardingPointerEvents: onboardingStyle?.pointerEvents,
                onboardingZIndex: onboardingStyle?.zIndex,
                modalOverlayShowClass: !!overlayEl?.classList.contains('show'),
                modalOverlayDisplay: overlayStyle?.display,
                modalOverlayPointerEvents: overlayStyle?.pointerEvents,
                modalOverlayZIndex: overlayStyle?.zIndex,
                centerHit: { x: pointX, y: pointY, hitTag, hitId, hitClass }
            });
        }

        function closeAllModals() {
            if (!modalOverlay) return;
            const appRoot = getAppRoot();
            modalOverlay.classList.remove('show');
            modalPanels.forEach((panel) => panel.classList.remove('active'));
            modalOverlay.setAttribute('aria-hidden', 'true');
            appRoot?.classList.remove('modal-open');
            app.state.modal.currentModal = null;
            userTrigger?.setAttribute('aria-expanded', 'false');
            logModalState('close');
        }

        function dismissModalStack() {
            if (!modalOverlay) return;
            const overlayClickEvent = new MouseEvent('click', {
                bubbles: true,
                cancelable: true,
                view: window
            });
            modalOverlay.dispatchEvent(overlayClickEvent);
            if (modalOverlay.classList.contains('show')) {
                closeAllModals();
            }
        }

        function closeModal(target) {
            if (!modalOverlay) return;
            const safeTarget = String(target || '').trim();
            if (!safeTarget) {
                closeAllModals();
                return;
            }
            const panel = modalPanels.find((item) => item.dataset.modal === safeTarget);
            if (!panel) {
                closeAllModals();
                return;
            }
            panel.classList.remove('active');
            const hasActivePanel = modalPanels.some((item) => item.classList.contains('active'));
            if (!hasActivePanel) {
                closeAllModals();
                return;
            }
            app.state.modal.currentModal = modalPanels.find((item) => item.classList.contains('active'))?.dataset.modal || null;
            userTrigger?.setAttribute('aria-expanded', app.state.modal.currentModal === 'settings' ? 'true' : 'false');
            logModalState('close-one', { target: safeTarget, currentModal: app.state.modal.currentModal });
        }

        function openModal(target, options) {
            if (!modalOverlay) return;
            const panel = modalPanels.find((item) => item.dataset.modal === target);
            if (!panel) return;
            const appRoot = getAppRoot();
            const keepStack = !!options?.keepStack;
            modalOverlay.classList.add('show');
            if (!keepStack) {
                modalPanels.forEach((item) => item.classList.remove('active'));
            }
            panel.classList.add('active');
            modalOverlay.setAttribute('aria-hidden', 'false');
            appRoot?.classList.add('modal-open');
            app.state.modal.currentModal = target;
            userTrigger?.setAttribute('aria-expanded', target === 'settings' ? 'true' : 'false');
            logModalState('open', { target, keepStack });
        }

        statCards.forEach((card) => {
            card.addEventListener('click', (event) => {
                logModalState('card-click', { target: card.dataset.modalTarget, eventTarget: event.target?.className });
                openModal(card.dataset.modalTarget);
            });
        });

        userTrigger?.addEventListener('click', () => {
            openModal('settings');
        });
        userTrigger?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                openModal('settings');
            }
        });

        modalOverlay?.addEventListener('click', (event) => {
            logModalState('overlay-click', { eventTarget: event.target?.className });
            logGlobalMaskState('overlay-click', event);
            if (event.target === modalOverlay) {
                closeAllModals();
            }
        });

        modalPanels.forEach((panel) => {
            panel.querySelectorAll('[data-modal-close]').forEach((btn) => {
                btn.addEventListener('click', closeAllModals);
            });
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && !document.querySelector('.ta-avatar-crop-modal.active')) {
                closeAllModals();
            }
        });

        if (modalOverlay) logModalState('init');

        app.openModal = openModal;
        app.closeModal = closeModal;
        app.closeAllModals = closeAllModals;
        app.dismissModalStack = dismissModalStack;
        app.logModalState = logModalState;
        app.logGlobalMaskState = logGlobalMaskState;
    };
})();
