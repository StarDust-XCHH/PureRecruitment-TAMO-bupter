(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.status = function initStatusModule(app) {
        const route = document.getElementById('route-status');
        const timeline = document.getElementById('timeline');
        const summary = document.getElementById('statusSummary');
        const notifications = document.getElementById('statusNotifications');
        const emptyState = document.getElementById('statusEmptyState');
        const statusMessage = document.getElementById('statusMessage');

        const state = {
            loading: false,
            taId: '',
            items: [],
            details: {}
        };

        function escapeHtml(value) {
            const str = value == null ? '' : String(value);

            return str
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        function safeText(value, fallback) {
            const text = String(value == null ? '' : value).trim();
            return text || (fallback || '');
        }

        function resolveTaId() {
            const userData = typeof app.getUserData === 'function' ? app.getUserData() : null;
            return safeText(userData?.taId || userData?.id);
        }

        function buildApiUrl() {
            const taId = resolveTaId();
            const params = new URLSearchParams();
            if (taId) params.set('taId', taId);
            return '../../api/ta/application-status' + (params.toString() ? ('?' + params.toString()) : '');
        }

        function normalizeArray(value) {
            return Array.isArray(value) ? value : [];
        }

        function normalizeSummary(summaryData) {
            const summary = summaryData && typeof summaryData === 'object' ? summaryData : {};
            return {
                totalCount: Number(summary.totalCount || 0),
                activeCount: Number(summary.activeCount || 0),
                interviewCount: Number(summary.interviewCount || 0),
                needMaterialCount: Number(summary.needMaterialCount || 0),
                offerCount: Number(summary.offerCount || 0),
                estimatedFeedbackTime: safeText(summary.estimatedFeedbackTime, '--'),
                latestStatusLabel: safeText(summary.latestStatusLabel, 'No applications yet')
            };
        }

        function normalizeItem(item) {
            const record = item && typeof item === 'object' ? item : {};
            return {
                applicationId: safeText(record.applicationId),
                courseName: safeText(record.courseName, 'Untitled posting'),
                status: safeText(record.status, 'In progress'),
                statusTone: safeText(record.statusTone, 'warn'),
                summary: safeText(record.summary || record.moComment),
                nextAction: safeText(record.nextAction || record.nextStep),
                updatedAt: safeText(record.updatedAt),
                timeline: normalizeArray(record.timeline),
                details: normalizeArray(record.details),
                notifications: normalizeArray(record.notifications),
                tags: normalizeArray(record.tags),
                category: safeText(record.category),
                matchLevel: safeText(record.matchLevel)
            };
        }

        function setStatusMessage(text, tone) {
            if (!statusMessage) return;
            statusMessage.textContent = safeText(text, '');
            statusMessage.dataset.tone = safeText(tone, 'muted');
        }

        function renderSummary(summaryData) {
            if (!summary) return;
            const data = normalizeSummary(summaryData);
            summary.innerHTML = [
                ['Applications', data.totalCount],
                ['In progress', data.activeCount],
                ['Interview invites', data.interviewCount],
                ['Pending materials', data.needMaterialCount],
                ['Hired', data.offerCount],
                ['Expected feedback', data.estimatedFeedbackTime],
                ['Latest status', data.latestStatusLabel]
            ].map(([label, value]) => {
                return '<div class="status-line"><span>' + escapeHtml(label) + '</span><span class="pill">' + escapeHtml(value) + '</span></div>';
            }).join('');
        }

        function renderNotifications(items, extraNotifications) {
            if (!notifications) return;
            const merged = [];
            normalizeArray(extraNotifications).forEach((item) => merged.push(item));
            items.forEach((item) => {
                item.notifications.forEach((entry) => merged.push(entry));
            });

            if (!merged.length) {
                notifications.innerHTML = '<div class="message-item"><strong>No notifications</strong><div class="muted">You are all caught up for now.</div></div>';
                return;
            }

            notifications.innerHTML = merged.map((entry) => {
                const title = safeText(entry?.title, 'Update');
                const content = safeText(entry?.content, 'No details');
                const meta = safeText(entry?.createdAt || entry?.time || entry?.tone);
                return ''
                    + '<div class="message-item">'
                    + '  <strong>' + escapeHtml(title) + '</strong>'
                    + '  <div class="muted">' + escapeHtml(content) + '</div>'
                    + (meta ? '  <div class="muted">' + escapeHtml(meta) + '</div>' : '')
                    + '</div>';
            }).join('');
        }

        function renderTimeline(items) {
            if (!timeline) return;
            if (!items.length) {
                timeline.innerHTML = '';
                return;
            }

            timeline.innerHTML = items.map((item, index) => {
                const detailsHtml = item.details.length
                    ? '<div class="status-detail-list">' + item.details.map((detail) => {
                        return '<div class="status-detail-row"><span>' + escapeHtml(safeText(detail?.label, 'Field')) + '</span><strong>' + escapeHtml(safeText(detail?.value, '--')) + '</strong></div>';
                    }).join('') + '</div>'
                    : '';

                const timelineHtml = item.timeline.length
                    ? '<div class="status-step-list">' + item.timeline.map((step) => {
                        const doneClass = step?.done ? ' is-done' : '';
                        return ''
                            + '<div class="status-step' + doneClass + '">'
                            + '  <strong>' + escapeHtml(safeText(step?.label, 'Milestone')) + '</strong>'
                            + '  <div class="muted">' + escapeHtml(safeText(step?.content, '')) + '</div>'
                            + '  <small class="muted">' + escapeHtml(safeText(step?.time, '--')) + '</small>'
                            + '</div>';
                    }).join('') + '</div>'
                    : '';

                const tagsHtml = item.tags.length
                    ? '<div class="status-tag-list">' + item.tags.map((tag) => '<span class="pill">' + escapeHtml(safeText(tag)) + '</span>').join('') + '</div>'
                    : '';

                return ''
                    + '<div class="timeline-item' + (index === 0 ? ' open' : '') + '" data-status="' + escapeHtml(item.statusTone) + '" data-application-id="' + escapeHtml(item.applicationId) + '">'
                    + '  <div class="timeline-node ' + escapeHtml(item.statusTone) + '"></div>'
                    + '  <strong>' + escapeHtml(item.courseName) + '</strong>'
                    + '  <div class="timeline-meta">' + escapeHtml(item.status) + ' · ' + escapeHtml(item.updatedAt || '--') + '</div>'
                    + '  <div class="accordion-content">'
                    + '      <p>' + escapeHtml(item.summary || 'No status summary yet.') + '</p>'
                    + '      <p>' + escapeHtml(item.nextAction || 'No next step provided.') + '</p>'
                    +        tagsHtml
                    +        detailsHtml
                    +        timelineHtml
                    + '  </div>'
                    + '</div>';
            }).join('');
        }

        function bindTimelineToggle(scope) {
            (scope || document).querySelectorAll('.timeline-item').forEach((item) => {
                if (item.dataset.timelineBound === 'true') return;
                item.dataset.timelineBound = 'true';
                item.addEventListener('click', (event) => {
                    if (event.target.closest('.status-detail-row, .status-step, .pill')) return;
                    item.classList.toggle('open');
                });
            });
        }

        function bindSidePanelToggle(scope) {
            (scope || document).querySelectorAll('[data-collapsible]').forEach((panel) => {
                if (panel.dataset.collapsibleBound === 'true') return;
                const toggle = panel.querySelector('.status-box-toggle');
                const body = panel.querySelector('.status-box-body');
                if (!toggle || !body) return;

                panel.dataset.collapsibleBound = 'true';
                toggle.addEventListener('click', () => {
                    const expanded = toggle.getAttribute('aria-expanded') !== 'false';
                    const nextExpanded = !expanded;
                    toggle.setAttribute('aria-expanded', String(nextExpanded));
                    body.hidden = !nextExpanded;
                    panel.classList.toggle('is-collapsed', !nextExpanded);
                });
            });
        }

        async function loadStatusData() {
            if (!route || state.loading) return;
            state.loading = true;
            state.taId = resolveTaId();
            setStatusMessage('Loading application status…', 'muted');
            if (emptyState) emptyState.hidden = true;

            try {
                const response = await fetch(buildApiUrl(), {
                    headers: { 'Accept': 'application/json' }
                });
                const payload = await response.json();
                if (!response.ok || !payload?.success) {
                    throw new Error(payload?.message || 'Failed to load application status');
                }

                const items = normalizeArray(payload.items).map(normalizeItem);
                state.items = items;
                state.details = payload.details && typeof payload.details === 'object' ? payload.details : {};

                renderTimeline(items);
                renderSummary(payload.summary);
                renderNotifications(items, payload.notifications || payload.messages);
                bindTimelineToggle(route);
                bindSidePanelToggle(route);

                if (!items.length) {
                    if (emptyState) emptyState.hidden = false;
                    setStatusMessage('No applications yet—browse the job board to apply.', 'muted');
                } else {
                    if (emptyState) emptyState.hidden = true;
                    setStatusMessage('Synced ' + items.length + ' application status(es).', 'success');
                }
            } catch (error) {
                console.error('[TA-STATUS] load failed', error);
                renderTimeline([]);
                renderSummary({});
                renderNotifications([], []);
                if (emptyState) emptyState.hidden = false;
                setStatusMessage(error.message || 'Could not load application status', 'error');
            } finally {
                state.loading = false;
            }
        }

        bindTimelineToggle(document);
        bindSidePanelToggle(document);
        loadStatusData();
        app.bindTimelineToggle = bindTimelineToggle;
        app.bindSidePanelToggle = bindSidePanelToggle;
        app.loadStatusData = loadStatusData;
        app.state.status = state;
    };
})();
