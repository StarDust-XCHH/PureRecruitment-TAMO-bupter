(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.applicants = function initApplicants(app) {
        function apiUrl(path) {
            var p = path.charAt(0) === '/' ? path : '/' + path;
            if (typeof window.moApiPath === 'function') {
                return window.moApiPath(p);
            }
            return '../../' + p.replace(/^\//, '');
        }

        const courseSelect = document.getElementById('applicantCourseSelect');
        const refreshBtn = document.getElementById('refreshApplicantsBtn');
        const statusText = document.getElementById('applicantsStatusText');
        const list = document.getElementById('applicantList');
        const emptyState = document.getElementById('applicantEmptyState');
        const detailModal = document.getElementById('moApplicantDetailModal');
        const detailBody = document.getElementById('moApplicantDetailBody');
        const detailClose = document.getElementById('moApplicantDetailClose');
        const navBadge = document.getElementById('navApplicantsBadge');

        let currentDetailApplicationId = '';

        function getMoId() {
            const u = typeof app.getMoUser === 'function' ? app.getMoUser() : null;
            if (!u) return '';
            return (u.moId || u.id || '').trim();
        }

        function optionLabel(item) {
            return item.courseCode + ' · ' + item.courseName;
        }

        function setStatus(text) {
            if (statusText) statusText.textContent = text || '';
        }

        function renderCourses(jobs) {
            const prev = courseSelect.value;
            courseSelect.innerHTML = '';
            (jobs || []).forEach(function (job) {
                const opt = document.createElement('option');
                opt.value = job.courseCode;
                opt.textContent = optionLabel(job);
                courseSelect.appendChild(opt);
            });
            if (prev && Array.from(courseSelect.options).some(function (o) { return o.value === prev; })) {
                courseSelect.value = prev;
            }
        }

        async function refreshNavUnreadBadge() {
            const moId = getMoId();
            if (!navBadge || !moId) return;
            try {
                const res = await fetch(apiUrl('/api/mo/applicants/unread-count') + '?moId=' + encodeURIComponent(moId));
                const payload = await res.json();
                const n = typeof payload.unreadCount === 'number' ? payload.unreadCount : 0;
                if (n > 0) {
                    navBadge.hidden = false;
                    navBadge.textContent = n > 9 ? '9+' : String(n);
                } else {
                    navBadge.hidden = true;
                }
            } catch (e) {
                navBadge.hidden = true;
            }
        }

        function escapeHtml(s) {
            return String(s == null ? '' : s)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;');
        }

        function renderApplicants(items) {
            list.innerHTML = '';
            const rows = Array.isArray(items) ? items : [];
            emptyState.hidden = rows.length > 0;
            if (rows.length === 0) return;

            rows.forEach(function (item) {
                const card = document.createElement('div');
                card.className = 'mo-applicant-card';
                const toneClass = item.status === '已录用' ? 'mo-status-ok'
                    : (item.status === '未录用' ? 'mo-status-warn' : 'mo-status-warn');
                const unreadDot = item.unread ? '<span class="mo-unread-dot" title="未读"></span>' : '';

                card.innerHTML =
                    '<div class="mo-applicant-head">' +
                    '<div><strong>' + escapeHtml(item.name || item.taId) + '</strong>' + unreadDot +
                    '<div class="muted">' + escapeHtml(item.taId) + '</div>' +
                    '<div class="muted" style="font-size:12px">申请 ' + escapeHtml(item.applicationId || '') + '</div></div>' +
                    '<div class="' + toneClass + '">' + escapeHtml(item.status || '未知') + '</div>' +
                    '</div>' +
                    '<div class="mo-applicant-meta">' +
                    '<span>邮箱：' + escapeHtml(item.email || '--') + '</span>' +
                    '<span>手机：' + escapeHtml(item.phone || '--') + '</span>' +
                    '<span>学号：' + escapeHtml(item.studentId || '--') + '</span>' +
                    '<span>意向：' + escapeHtml(item.intent || '--') + '</span>' +
                    '</div>' +
                    '<div class="muted">技能：' + escapeHtml(item.skills || '--') + '</div>' +
                    '<div class="muted">摘要：' + escapeHtml(item.comment || '--') + '</div>';

                const actions = document.createElement('div');
                actions.className = 'mo-applicant-actions';

                const viewBtn = document.createElement('button');
                viewBtn.className = 'pill-btn ghost';
                viewBtn.type = 'button';
                viewBtn.textContent = '查看详情';
                viewBtn.addEventListener('click', function () {
                    openDetail(item);
                });

                const acceptBtn = document.createElement('button');
                acceptBtn.className = 'pill-btn';
                acceptBtn.type = 'button';
                acceptBtn.textContent = '录用';
                acceptBtn.addEventListener('click', function () {
                    decide(item, 'selected');
                });

                const rejectBtn = document.createElement('button');
                rejectBtn.className = 'pill-btn ghost';
                rejectBtn.type = 'button';
                rejectBtn.textContent = '拒绝';
                rejectBtn.addEventListener('click', function () {
                    decide(item, 'rejected');
                });

                actions.appendChild(viewBtn);
                actions.appendChild(acceptBtn);
                actions.appendChild(rejectBtn);
                card.appendChild(actions);
                list.appendChild(card);
            });
        }

        function closeDetailModal() {
            if (!detailModal) return;
            detailModal.hidden = true;
            detailModal.setAttribute('aria-hidden', 'true');
            currentDetailApplicationId = '';
        }

        async function markRead(applicationId) {
            const moId = getMoId();
            if (!moId || !applicationId) return;
            try {
                await fetch(apiUrl('/api/mo/applications/mark-read'), {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
                    body: JSON.stringify({ moId: moId, applicationId: applicationId })
                });
            } catch (e) { /* ignore */ }
        }

        async function openDetail(item) {
            const moId = getMoId();
            if (!detailModal || !detailBody || !moId || !item.applicationId) return;
            currentDetailApplicationId = item.applicationId;
            detailBody.innerHTML = '<p class="muted">加载中…</p>';
            detailModal.hidden = false;
            detailModal.setAttribute('aria-hidden', 'false');

            await markRead(item.applicationId);

            try {
                const res = await fetch(apiUrl('/api/mo/applicants/detail') + '?moId=' + encodeURIComponent(moId)
                    + '&applicationId=' + encodeURIComponent(item.applicationId));
                const d = await res.json();
                if (!res.ok || d.success === false) throw new Error(d.message || '加载失败');

                const ts = d.taSnapshot || {};
                const resume = d.resume || {};
                const resumeUrl = apiUrl('/api/mo/applications/resume') + '?moId=' + encodeURIComponent(moId)
                    + '&applicationId=' + encodeURIComponent(item.applicationId);

                let eventsHtml = '';
                (Array.isArray(d.events) ? d.events : []).forEach(function (ev) {
                    eventsHtml += '<div class="mo-comment-item"><strong>' + escapeHtml(ev.label || ev.eventType) + '</strong> '
                        + '<span class="muted">' + escapeHtml(ev.time || '') + '</span><br>'
                        + escapeHtml(ev.content || '') + '</div>';
                });

                let commentsHtml = '';
                (Array.isArray(d.comments) ? d.comments : []).forEach(function (c) {
                    commentsHtml += '<div class="mo-comment-item"><span class="muted">' + escapeHtml(c.createdAt || '')
                        + ' · ' + escapeHtml(c.moId || '') + '</span><br>' + escapeHtml(c.text || '') + '</div>';
                });

                detailBody.innerHTML =
                    '<dl class="mo-detail-kv">' +
                    '<dt>申请 ID</dt><dd>' + escapeHtml(d.applicationId) + '</dd>' +
                    '<dt>TA ID</dt><dd>' + escapeHtml(d.taId) + '</dd>' +
                    '<dt>课程</dt><dd>' + escapeHtml(d.courseCode) + ' ' + escapeHtml(d.courseName) + '</dd>' +
                    '<dt>姓名</dt><dd>' + escapeHtml((ts.name || ts.realName || '').trim()) + '</dd>' +
                    '<dt>学号</dt><dd>' + escapeHtml(ts.studentId || '') + '</dd>' +
                    '<dt>邮箱</dt><dd>' + escapeHtml(ts.contactEmail || '') + '</dd>' +
                    '<dt>电话</dt><dd>' + escapeHtml(ts.phone || '') + '</dd>' +
                    '<dt>简介</dt><dd>' + escapeHtml(ts.bio || '') + '</dd>' +
                    '</dl>' +
                    '<div class="mo-detail-section-title">简历</div>' +
                    '<p><a class="pill-btn" href="' + resumeUrl + '" target="_blank" rel="noopener">打开简历（' + escapeHtml(resume.originalFileName || '文件') + '）</a></p>' +
                    '<div class="mo-detail-section-title">流程事件</div>' +
                    '<div class="mo-comment-list">' + (eventsHtml || '<span class="muted">暂无</span>') + '</div>' +
                    '<div class="mo-detail-section-title">MO 评论</div>' +
                    '<div class="mo-comment-list" id="moCommentList">' + (commentsHtml || '<span class="muted">暂无</span>') + '</div>' +
                    '<div class="mo-form-grid">' +
                    '<label class="full">添加评论<textarea id="moNewCommentText" rows="2" placeholder="输入评论"></textarea></label>' +
                    '</div>' +
                    '<button type="button" class="pill-btn" id="moSubmitCommentBtn">提交评论</button>';

                const submitBtn = document.getElementById('moSubmitCommentBtn');
                const textarea = document.getElementById('moNewCommentText');
                if (submitBtn && textarea) {
                    submitBtn.addEventListener('click', async function () {
                        const text = (textarea.value || '').trim();
                        if (!text) return;
                        try {
                            const cr = await fetch(apiUrl('/api/mo/applications/comment'), {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json;charset=UTF-8' },
                                body: JSON.stringify({
                                    moId: moId,
                                    applicationId: item.applicationId,
                                    text: text
                                })
                            });
                            const cp = await cr.json();
                            if (!cr.ok || cp.success === false) throw new Error(cp.message || '失败');
                            textarea.value = '';
                            openDetail(item);
                        } catch (err) {
                            window.alert(err.message || '评论失败');
                        }
                    });
                }
            } catch (err) {
                detailBody.innerHTML = '<p class="mo-status-warn">' + escapeHtml(err.message || '加载失败') + '</p>';
            }

            await loadApplicants();
            await refreshNavUnreadBadge();
        }

        async function loadApplicants() {
            const code = courseSelect.value;
            const moId = getMoId();
            if (!code) {
                renderApplicants([]);
                setStatus('请先选择课程');
                return;
            }
            if (!moId) {
                setStatus('未登录或缺少 moId');
                renderApplicants([]);
                return;
            }
            setStatus('加载中...');
            try {
                const res = await fetch(apiUrl('/api/mo/applicants') + '?courseCode=' + encodeURIComponent(code)
                    + '&moId=' + encodeURIComponent(moId));
                const payload = await res.json();
                if (!res.ok || payload.success === false) throw new Error(payload.message || '加载失败');
                const items = Array.isArray(payload.items) ? payload.items : [];
                renderApplicants(items);
                const unread = typeof payload.unreadCount === 'number' ? payload.unreadCount : 0;
                setStatus('已加载 ' + items.length + ' 名申请人' + (unread > 0 ? ' · 未读 ' + unread + ' 条' : ''));
                if (typeof app.onApplicantsLoaded === 'function') app.onApplicantsLoaded(items);
                await refreshNavUnreadBadge();
            } catch (err) {
                setStatus(err.message || '加载失败');
                renderApplicants([]);
            }
        }

        async function decide(item, decision) {
            const code = courseSelect.value;
            const moId = getMoId();
            const actionText = decision === 'selected' ? '录用' : '拒绝';
            const comment = window.prompt('请输入' + actionText + '备注（可选）', '');
            if (comment === null) return;
            try {
                const res = await fetch(apiUrl('/api/mo/applications/select'), {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
                    body: JSON.stringify({
                        courseCode: code,
                        taId: item.taId,
                        moId: moId,
                        decision: decision,
                        comment: comment || ''
                    })
                });
                const payload = await res.json();
                if (!res.ok || payload.success === false) throw new Error(payload.message || '操作失败');
                setStatus('已完成：' + actionText + ' ' + item.taId);
                await loadApplicants();
                if (typeof app.loadDashboard === 'function') app.loadDashboard();
                if (currentDetailApplicationId === item.applicationId && detailModal && !detailModal.hidden) {
                    openDetail(item);
                }
            } catch (err) {
                setStatus(err.message || '保存失败');
            }
        }

        if (detailClose) detailClose.addEventListener('click', closeDetailModal);
        if (detailModal) {
            detailModal.addEventListener('click', function (e) {
                if (e.target && e.target.getAttribute('data-close-applicant-detail')) {
                    closeDetailModal();
                }
            });
        }

        app.onJobsUpdated = function (jobs) {
            renderCourses(jobs);
            loadApplicants();
        };
        app.setApplicantCourse = function (courseCode) {
            if (!courseCode) return;
            courseSelect.value = courseCode;
            loadApplicants();
        };
        app.refreshMoApplicantUnreadBadge = refreshNavUnreadBadge;

        refreshBtn.addEventListener('click', loadApplicants);
        courseSelect.addEventListener('change', loadApplicants);

        refreshNavUnreadBadge();
    };
})();
