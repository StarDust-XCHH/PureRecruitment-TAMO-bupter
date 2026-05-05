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
        const refreshButton = document.getElementById('statusRefreshButton');

        const state = {
            loading: false,
            taId: '',
            items: [],
            details: {},
            appliedCourseCodes: []
        };
        const STATUS_SCROLL_OFFSET = 20;
        let highlightResetTimer = null;
        let hasAutoLoaded = false;

        function t(zh, en) {
            return typeof app.t === 'function' ? app.t(zh, en) : zh;
        }

        function escapeHtml(value) {
            const str = value == null ? '' : String(value);

            return str
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;'); // 也可以使用 '&apos;'
        }

        function safeText(value, fallback) {
            const text = String(value == null ? '' : value).trim();
            return text || (fallback || '');
        }

        function formatFileSize(bytes) {
            const size = Number(bytes);
            if (!Number.isFinite(size) || size <= 0) return '--';
            if (size >= 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB';
            if (size >= 1024) return Math.round(size / 1024) + ' KB';
            return size + ' B';
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
                latestStatusLabel: safeText(summary.latestStatusLabel, t('暂无申请', 'No applications yet'))
            };
        }

        function normalizeResumeView(resumeView) {
            const record = resumeView && typeof resumeView === 'object' ? resumeView : {};
            const downloadUrl = safeText(record.downloadUrl);
            return {
                fileName: safeText(record.fileName, t('简历文件', 'Resume file')),
                downloadUrl: downloadUrl,
                mimeType: safeText(record.mimeType),
                extension: safeText(record.extension),
                size: Number(record.size || 0),
                available: !!downloadUrl
            };
        }

        function normalizeItem(item) {
            const record = item && typeof item === 'object' ? item : {};
            return {
                applicationId: safeText(record.applicationId),
                courseCode: safeText(record.courseCode),
                courseName: safeText(record.courseName, t('未命名岗位', 'Untitled role')),
                status: safeText(record.status, t('处理中', 'In progress')),
                statusTone: safeText(record.statusTone, 'warn'),
                summary: safeText(record.summary || record.moComment),
                nextAction: safeText(record.nextAction || record.nextStep),
                updatedAt: safeText(record.updatedAt),
                timeline: normalizeArray(record.timeline),
                details: normalizeArray(record.details),
                notifications: normalizeArray(record.notifications),
                tags: normalizeArray(record.tags),
                category: safeText(record.category),
                matchLevel: safeText(record.matchLevel),
                resumeView: normalizeResumeView(record.resumeView)
            };
        }

        function setStatusMessage(text, tone) {
            if (!statusMessage) return;
            statusMessage.textContent = safeText(text, '');
            statusMessage.dataset.tone = safeText(tone, 'muted');
        }

        function setRefreshButtonState(loading) {
            if (!refreshButton) return;
            refreshButton.disabled = !!loading;
            refreshButton.textContent = loading ? t('刷新中...', 'Refreshing...') : t('刷新状态', 'Refresh Status');
        }

        function renderSummary(summaryData) {
            if (!summary) return;
            const data = normalizeSummary(summaryData);
            summary.innerHTML = [
                [t('当前申请数', 'Applications'), data.totalCount],
                [t('推进中', 'Active'), data.activeCount],
                [t('面试邀约', 'Interviews'), data.interviewCount],
                [t('待补充资料', 'Need Materials'), data.needMaterialCount],
                [t('已录用', 'Offers'), data.offerCount],
                [t('预计反馈时间', 'Estimated Feedback'), data.estimatedFeedbackTime],
                [t('最新状态', 'Latest Status'), data.latestStatusLabel]
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
                notifications.innerHTML = '<div class="message-item"><strong>' + t('暂无通知', 'No notifications') + '</strong><div class="muted">' + t('当前没有新的状态提醒。', 'There are no new status updates right now.') + '</div></div>';
                return;
            }

            notifications.innerHTML = merged.map((entry) => {
                const title = safeText(entry?.title, t('状态提醒', 'Status Update'));
                const content = safeText(entry?.content, t('暂无内容', 'No details available'));
                const meta = safeText(entry?.createdAt || entry?.time || entry?.tone);
                return ''
                    + '<div class="message-item">'
                    + '  <strong>' + escapeHtml(title) + '</strong>'
                    + '  <div class="muted">' + escapeHtml(content) + '</div>'
                    + (meta ? '  <div class="muted">' + escapeHtml(meta) + '</div>' : '')
                    + '</div>';
            }).join('');
        }

        function buildResumeActionHtml(item) {
            if (!item.resumeView.available) {
                return '<div class="status-resume-action muted">' + t('当前申请未找到可查看的简历文件。', 'No resume file is available for this application.') + '</div>';
            }
            const meta = [formatFileSize(item.resumeView.size), item.resumeView.extension ? String(item.resumeView.extension).toUpperCase() : '']
                .filter(Boolean)
                .join(' · ');
            return ''
                + '<div class="status-resume-action">'
                + '  <a class="status-resume-link" href="' + escapeHtml(item.resumeView.downloadUrl) + '" target="_blank" rel="noopener noreferrer">' + t('查看简历', 'View Resume') + '</a>'
                + '  <span class="muted">' + escapeHtml(item.resumeView.fileName) + (meta ? ' · ' + escapeHtml(meta) : '') + '</span>'
                + '</div>';
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
                        return '<div class="status-detail-row"><span>' + escapeHtml(safeText(detail?.label, t('字段', 'Field'))) + '</span><strong>' + escapeHtml(safeText(detail?.value, '--')) + '</strong></div>';
                    }).join('') + '</div>'
                    : '';

                const timelineHtml = item.timeline.length
                    ? '<div class="status-step-list">' + item.timeline.map((step) => {
                        const doneClass = step?.done ? ' is-done' : '';
                        return ''
                            + '<div class="status-step' + doneClass + '">'
                            + '  <strong>' + escapeHtml(safeText(step?.label, t('状态节点', 'Status Step'))) + '</strong>'
                            + '  <div class="muted">' + escapeHtml(safeText(step?.content, '')) + '</div>'
                            + '  <small class="muted">' + escapeHtml(safeText(step?.time, '--')) + '</small>'
                            + '</div>';
                    }).join('') + '</div>'
                    : '';

                const tagsHtml = item.tags.length
                    ? '<div class="status-tag-list">' + item.tags.map((tag) => '<span class="pill">' + escapeHtml(safeText(tag)) + '</span>').join('') + '</div>'
                    : '';

                const resumeActionHtml = buildResumeActionHtml(item);

                return ''
                    + '<div class="timeline-item' + (index === 0 ? ' open' : '') + '" data-status="' + escapeHtml(item.statusTone) + '" data-application-id="' + escapeHtml(item.applicationId) + '" data-course-code="' + escapeHtml(item.courseCode) + '">'
                    + '  <div class="timeline-node ' + escapeHtml(item.statusTone) + '"></div>'
                    + '  <strong>' + escapeHtml(item.courseName) + '</strong>'
                    + '  <div class="timeline-meta">' + escapeHtml(item.status) + ' · ' + escapeHtml(item.updatedAt || '--') + '</div>'
                    + '  <div class="accordion-content">'
                    + '      <p>' + escapeHtml(item.summary || t('暂无状态说明。', 'No status details available.')) + '</p>'
                    + '      <p>' + escapeHtml(item.nextAction || t('暂无下一步提示。', 'No next-step suggestion available.')) + '</p>'
                    +        tagsHtml
                    +        resumeActionHtml
                    +        detailsHtml
                    +        timelineHtml
                    + '  </div>'
                    + '</div>';
            }).join('');
        }

        function findTimelineItem(focus) {
            if (!timeline || !focus) return null;
            const applicationId = safeText(focus.applicationId);
            if (applicationId) {
                const byApplicationId = timeline.querySelector('[data-application-id="' + applicationId + '"]');
                if (byApplicationId) return byApplicationId;
            }

            const courseCode = safeText(focus.courseCode).toUpperCase();
            if (!courseCode) return null;
            return Array.from(timeline.querySelectorAll('.timeline-item')).find((item) => {
                return safeText(item.dataset.courseCode).toUpperCase() === courseCode;
            }) || null;
        }

        function focusTimelineItem(focus) {
            if (!focus || !timeline) return false;
            const targetItem = findTimelineItem(focus);
            if (!targetItem) return false;

            if (highlightResetTimer) {
                window.clearTimeout(highlightResetTimer);
                highlightResetTimer = null;
            }

            timeline.querySelectorAll('.timeline-item.is-submission-focus').forEach((item) => {
                item.classList.remove('is-submission-focus');
                item.style.removeProperty('--submission-pulse-count');
            });

            targetItem.classList.add('open');
            targetItem.classList.add('is-submission-focus');
            targetItem.style.setProperty('--submission-pulse-count', String(Math.max(1, Number(focus.pulseCount || 3))));

            const rect = targetItem.getBoundingClientRect();
            const targetTop = Math.max(0, window.scrollY + rect.top - STATUS_SCROLL_OFFSET);
            window.scrollTo({ top: targetTop, behavior: 'smooth' });

            highlightResetTimer = window.setTimeout(() => {
                targetItem.classList.remove('is-submission-focus');
                targetItem.style.removeProperty('--submission-pulse-count');
                highlightResetTimer = null;
            }, 3600);
            return true;
        }

        function bindTimelineToggle(scope) {
            (scope || document).querySelectorAll('.timeline-item').forEach((item) => {
                if (item.dataset.timelineBound === 'true') return;
                item.dataset.timelineBound = 'true';
                item.addEventListener('click', (event) => {
                    if (event.target.closest('.status-detail-row, .status-step, .pill, .status-resume-link')) return;
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

        async function loadStatusData(options) {
            if (!route || state.loading) return;
            state.loading = true;
            state.taId = resolveTaId();
            setRefreshButtonState(true);
            setStatusMessage(t('申请状态加载中...', 'Loading application status...'), 'muted');
            if (emptyState) emptyState.hidden = true;

            try {
                const response = await fetch(buildApiUrl(), {
                    headers: { 'Accept': 'application/json' }
                });
                const payload = await response.json();
                if (!response.ok || !payload?.success) {
                    throw new Error(payload?.message || t('读取申请状态失败', 'Failed to load application status'));
                }

                const items = normalizeArray(payload.items).map(normalizeItem);
                state.items = items;
                state.details = payload.details && typeof payload.details === 'object' ? payload.details : {};
                state.appliedCourseCodes = normalizeArray(payload.appliedCourseCodes).map((code) => safeText(code)).filter(Boolean);
                if (app && typeof app.setAppliedCourseCodes === 'function') {
                    app.setAppliedCourseCodes(state.appliedCourseCodes);
                }
                const appliedJobIds = normalizeArray(payload.appliedJobIds).map((id) => safeText(id)).filter(Boolean);
                if (app && typeof app.setAppliedJobIds === 'function') {
                    app.setAppliedJobIds(appliedJobIds);
                }

                renderTimeline(items);
                renderSummary(payload.summary);
                renderNotifications(items, payload.notifications || payload.messages);
                bindTimelineToggle(route);
                bindSidePanelToggle(route);
                const shouldConsumeFocus = options?.consumeFocus !== false;
                const focus = shouldConsumeFocus && typeof app.consumeStatusFocus === 'function'
                    ? app.consumeStatusFocus()
                    : null;
                const focused = focusTimelineItem(focus);

                if (!items.length) {
                    if (emptyState) emptyState.hidden = false;
                    setStatusMessage(t('当前还没有申请记录，可前往职位大厅投递岗位。', 'You do not have any applications yet. Visit the Jobs page to apply.'), 'muted');
                } else {
                    if (emptyState) emptyState.hidden = true;
                    setStatusMessage(focused
                        ? t('已同步 ' + items.length + ' 条申请状态，并已定位到最新申请。', 'Synced ' + items.length + ' application updates and focused on the latest submission.')
                        : t('已同步 ' + items.length + ' 条申请状态。', 'Synced ' + items.length + ' application updates.'), 'success');
                }
                hasAutoLoaded = true;
            } catch (error) {
                console.error('[TA-STATUS] load failed', error);
                renderTimeline([]);
                renderSummary({});
                renderNotifications([], []);
                state.appliedCourseCodes = [];
                if (app && typeof app.setAppliedCourseCodes === 'function') {
                    app.setAppliedCourseCodes([]);
                }
                if (app && typeof app.setAppliedJobIds === 'function') {
                    app.setAppliedJobIds([]);
                }
                if (emptyState) emptyState.hidden = false;
                setStatusMessage(error.message || t('申请状态加载失败', 'Failed to load application status'), 'error');
            } finally {
                state.loading = false;
                setRefreshButtonState(false);
            }
        }

        refreshButton?.addEventListener('click', () => loadStatusData({ consumeFocus: true }));

        if (!hasAutoLoaded) {
            window.setTimeout(() => {
                loadStatusData({ consumeFocus: true });
            }, 0);
        }

        app.loadStatusData = loadStatusData;
        app.refreshStatusData = loadStatusData;

        return {
            load: loadStatusData,
            refresh: loadStatusData,
            getAppliedCourseCodes() {
                return state.appliedCourseCodes.slice();
            }
        };
    };
})();
