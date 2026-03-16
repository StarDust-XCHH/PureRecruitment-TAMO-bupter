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
            // 处理 null 或 undefined，确保返回字符串
            const str = value == null ? '' : String(value);

            return str
                .replace(/&/g, '&amp;')   // 必须最先替换 &
                .replace(/</g, '&lt;')    // 替换 <
                .replace(/>/g, '&gt;')    // 替换 >
                .replace(/"/g, '&quot;')  // 替换 "
                .replace(/'/g, '&#39;');  // 修复了这里的单引号报错，并转义为实体
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
                latestStatusLabel: safeText(summary.latestStatusLabel, '暂无申请')
            };
        }

        function normalizeItem(item) {
            const record = item && typeof item === 'object' ? item : {};
            return {
                applicationId: safeText(record.applicationId),
                courseName: safeText(record.courseName, '未命名岗位'),
                status: safeText(record.status, '处理中'),
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
                ['当前申请数', data.totalCount],
                ['推进中', data.activeCount],
                ['面试邀约', data.interviewCount],
                ['待补充资料', data.needMaterialCount],
                ['已录用', data.offerCount],
                ['预计反馈时间', data.estimatedFeedbackTime],
                ['最新状态', data.latestStatusLabel]
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
                notifications.innerHTML = '<div class="message-item"><strong>暂无通知</strong><div class="muted">当前没有新的状态提醒。</div></div>';
                return;
            }

            notifications.innerHTML = merged.map((entry) => {
                const title = safeText(entry?.title, '状态提醒');
                const content = safeText(entry?.content, '暂无内容');
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
                        return '<div class="status-detail-row"><span>' + escapeHtml(safeText(detail?.label, '字段')) + '</span><strong>' + escapeHtml(safeText(detail?.value, '--')) + '</strong></div>';
                    }).join('') + '</div>'
                    : '';

                const timelineHtml = item.timeline.length
                    ? '<div class="status-step-list">' + item.timeline.map((step) => {
                        const doneClass = step?.done ? ' is-done' : '';
                        return ''
                            + '<div class="status-step' + doneClass + '">'
                            + '  <strong>' + escapeHtml(safeText(step?.label, '状态节点')) + '</strong>'
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
                    + '  <div style="color: var(--muted); font-size: 12px; margin-top: 6px;">' + escapeHtml(item.status) + ' · ' + escapeHtml(item.updatedAt || '--') + '</div>'
                    + '  <div class="accordion-content">'
                    + '      <p>' + escapeHtml(item.summary || '暂无状态说明。') + '</p>'
                    + '      <p>' + escapeHtml(item.nextAction || '暂无下一步提示。') + '</p>'
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

        async function loadStatusData() {
            if (!route || state.loading) return;
            state.loading = true;
            state.taId = resolveTaId();
            setStatusMessage('申请状态加载中...', 'muted');
            if (emptyState) emptyState.hidden = true;

            try {
                const response = await fetch(buildApiUrl(), {
                    headers: { 'Accept': 'application/json' }
                });
                const payload = await response.json();
                if (!response.ok || !payload?.success) {
                    throw new Error(payload?.message || '读取申请状态失败');
                }

                const items = normalizeArray(payload.items).map(normalizeItem);
                state.items = items;
                state.details = payload.details && typeof payload.details === 'object' ? payload.details : {};

                renderTimeline(items);
                renderSummary(payload.summary);
                renderNotifications(items, payload.notifications || payload.messages);
                bindTimelineToggle(route);

                if (!items.length) {
                    if (emptyState) emptyState.hidden = false;
                    setStatusMessage('当前还没有申请记录，可前往职位大厅投递岗位。', 'muted');
                } else {
                    if (emptyState) emptyState.hidden = true;
                    setStatusMessage('已同步 ' + items.length + ' 条申请状态。', 'success');
                }
            } catch (error) {
                console.error('[TA-STATUS] load failed', error);
                renderTimeline([]);
                renderSummary({});
                renderNotifications([], []);
                if (emptyState) emptyState.hidden = false;
                setStatusMessage(error.message || '申请状态加载失败', 'error');
            } finally {
                state.loading = false;
            }
        }

        bindTimelineToggle(document);
        loadStatusData();
        app.bindTimelineToggle = bindTimelineToggle;
        app.loadStatusData = loadStatusData;
        app.state.status = state;
    };
})();
