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
        const clearCourseFilterBtn = document.getElementById('clearApplicantCourseFilterBtn');
        const refreshBtn = document.getElementById('refreshApplicantsBtn');
        const statusText = document.getElementById('applicantsStatusText');
        const list = document.getElementById('applicantList');
        const paginationEl = document.getElementById('applicantPagination');
        const emptyState = document.getElementById('applicantEmptyState');
        const detailModal = document.getElementById('moApplicantDetailModal');
        const detailBody = document.getElementById('moApplicantDetailBody');
        const detailClose = document.getElementById('moApplicantDetailClose');
        const navBadge = document.getElementById('navApplicantsBadge');
        const routeApplicantsEl = document.getElementById('route-applicants');

        let currentDetailApplicationId = '';

        function scrollApplicantsPanelToTop() {
            if (routeApplicantsEl && typeof routeApplicantsEl.scrollIntoView === 'function') {
                routeApplicantsEl.scrollIntoView({ block: 'start', behavior: 'smooth' });
            }
        }

        const APPLICANT_PAGE_SIZE = 6;
        let applicantListCache = [];
        let applicantPageIndex = 1;
        /** 从课程详情「进入应聘筛选」预选的课程编码，在岗位下拉加载后再应用 */
        let pendingApplicantCourseCode = null;

        function getMoId() {
            const u = typeof app.getMoUser === 'function' ? app.getMoUser() : null;
            if (!u) return '';
            return (u.moId || u.id || '').trim();
        }

        function jobOptionValue(job) {
            if (!job) return '';
            return job.courseCode != null && String(job.courseCode).trim() !== ''
                ? String(job.courseCode).trim()
                : (job.jobId != null ? String(job.jobId).trim() : '');
        }

        function jobOptionLabel(job) {
            if (!job) return '';
            var code = job.courseCode != null && String(job.courseCode).trim() !== ''
                ? String(job.courseCode).trim()
                : (job.jobId != null ? String(job.jobId).trim() : '--');
            var name = job.courseName != null && String(job.courseName).trim() !== ''
                ? String(job.courseName).trim()
                : '未命名岗位';
            return code + ' · ' + name;
        }

        function setStatus(text) {
            if (statusText) statusText.textContent = text || '';
        }

        function updateClearCourseFilterButton() {
            if (!clearCourseFilterBtn || !courseSelect) return;
            const hasFilter = (courseSelect.value || '').trim() !== '';
            clearCourseFilterBtn.hidden = !hasFilter;
            clearCourseFilterBtn.setAttribute('aria-hidden', hasFilter ? 'false' : 'true');
        }

        function renderCourses(jobs) {
            const prev = courseSelect.value;
            courseSelect.innerHTML = '';
            const optAll = document.createElement('option');
            optAll.value = '';
            optAll.textContent = '全部课程';
            courseSelect.appendChild(optAll);
            (jobs || []).forEach(function (job) {
                const val = jobOptionValue(job);
                if (!val) return;
                const opt = document.createElement('option');
                opt.value = val;
                opt.textContent = jobOptionLabel(job);
                courseSelect.appendChild(opt);
            });
            if (Array.from(courseSelect.options).some(function (o) { return o.value === prev; })) {
                courseSelect.value = prev;
            } else {
                courseSelect.value = '';
            }
            updateClearCourseFilterButton();
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

        /** 与列表接口一致：taSnapshot.applicationIntent */
        function formatSnapshotIntent(ts) {
            if (!ts || ts.applicationIntent == null) return '';
            return String(ts.applicationIntent).trim();
        }

        /** 与后端 joinSnapshotSkills 一致：taSnapshot.skills 字符串数组，逗号拼接 */
        function formatSnapshotSkills(ts) {
            if (!ts || !Array.isArray(ts.skills)) return '';
            var parts = [];
            ts.skills.forEach(function (s) {
                if (s == null) return;
                var t = String(s).trim();
                if (t) parts.push(t);
            });
            return parts.join(', ');
        }

        function detailKvOrDash(text) {
            var v = (text == null ? '' : String(text)).trim();
            return v ? escapeHtml(v) : '--';
        }

        /** 与列表 status 文案对应，用于投递状态色块（与课程管理 OPEN/CLOSED 徽章同类） */
        function applicantStatusClass(status) {
            var s = (status == null ? '' : String(status)).trim();
            if (s === '已录用') return 'mo-applicant-status--hired';
            if (s === '未录用') return 'mo-applicant-status--rejected';
            if (s === '审核中') return 'mo-applicant-status--review';
            if (s === '已投递') return 'mo-applicant-status--submitted';
            return 'mo-applicant-status--neutral';
        }

        /**
         * @param {Array|undefined} items 传入时表示新数据并重置到第 1 页；不传则仅按当前页从缓存重绘（用于翻页）
         */
        function renderApplicants(items) {
            if (items !== undefined) {
                applicantListCache = Array.isArray(items) ? items : [];
                applicantPageIndex = 1;
            }

            list.innerHTML = '';
            var total = applicantListCache.length;
            emptyState.hidden = total > 0;

            var totalPages = Math.max(1, Math.ceil(total / APPLICANT_PAGE_SIZE));
            if (applicantPageIndex > totalPages) {
                applicantPageIndex = totalPages;
            }
            if (applicantPageIndex < 1) {
                applicantPageIndex = 1;
            }

            if (paginationEl) {
                paginationEl.innerHTML = '';
                if (total === 0) {
                    paginationEl.hidden = true;
                } else {
                    paginationEl.hidden = false;
                    for (var p = 1; p <= totalPages; p++) {
                        (function (pageNum) {
                            var btn = document.createElement('button');
                            btn.type = 'button';
                            btn.className = 'job-page-btn' + (pageNum === applicantPageIndex ? ' active' : '');
                            btn.textContent = String(pageNum);
                            btn.setAttribute('aria-label', '第 ' + pageNum + ' 页');
                            if (pageNum === applicantPageIndex) {
                                btn.setAttribute('aria-current', 'page');
                            }
                            btn.addEventListener('click', function () {
                                applicantPageIndex = pageNum;
                                renderApplicants();
                                scrollApplicantsPanelToTop();
                            });
                            paginationEl.appendChild(btn);
                        }(p));
                    }
                }
            }

            if (total === 0) {
                return;
            }

            var start = (applicantPageIndex - 1) * APPLICANT_PAGE_SIZE;
            var pageRows = applicantListCache.slice(start, start + APPLICANT_PAGE_SIZE);

            pageRows.forEach(function (item) {
                const card = document.createElement('div');
                card.className = 'mo-applicant-board-card job-card course-job-card';
                card.setAttribute('tabindex', '-1');
                const unreadDot = item.unread ? '<span class="mo-unread-dot" title="未读"></span>' : '';

                card.innerHTML =
                    '<div class="course-card-topline">' +
                    '<span class="job-code">' + escapeHtml(item.courseCode || '--') + '</span>' +
                    '<span class="pill course-mo-badge">' + escapeHtml(item.taId || '--') + '</span>' +
                    '<span class="mo-applicant-status-badge ' + applicantStatusClass(item.status) + '">' +
                    escapeHtml(item.status || '未知') + '</span>' +
                    '</div>' +
                    '<h4 class="course-card-title mo-applicant-card__title">' + escapeHtml(item.name || item.taId) + unreadDot + '</h4>' +
                    '<p class="course-card-description">' +
                    '课程 · ' + escapeHtml(item.courseCode || '--') + ' · ' + escapeHtml(item.courseName || '--') +
                    '</p>' +
                    '<div class="course-card-hint mo-applicant-card__hint">' +
                    '<button type="button" class="pill-btn ghost mo-applicant-view-btn">查看详情</button>' +
                    '</div>';

                const viewBtn = card.querySelector('.mo-applicant-view-btn');
                if (viewBtn) {
                    viewBtn.addEventListener('click', function (e) {
                        e.stopPropagation();
                        openDetail(item);
                    });
                }

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
                if (!res.ok || d.success === false) {
                    detailBody.innerHTML = '<p class="mo-status-warn">' + escapeHtml(d.message || '加载失败') + '</p>';
                } else {
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
                    '<dt>意向</dt><dd>' + detailKvOrDash(formatSnapshotIntent(ts)) + '</dd>' +
                    '<dt>技能</dt><dd>' + detailKvOrDash(formatSnapshotSkills(ts)) + '</dd>' +
                    '</dl>' +
                    '<div class="mo-detail-block mo-detail-block--resume">' +
                    '<div class="mo-detail-section-title">简历</div>' +
                    '<div class="mo-detail-resume-row"><a class="pill-btn" href="' + resumeUrl + '" target="_blank" rel="noopener">查看简历</a></div>' +
                    '</div>' +
                    '<div class="mo-applicant-actions mo-applicant-detail-actions mo-applicant-detail-actions--after-resume">' +
                    '<button type="button" class="pill-btn" id="moApplicantAcceptBtn">录用</button>' +
                    '<button type="button" class="pill-btn ghost" id="moApplicantRejectBtn">拒绝</button>' +
                    '</div>' +
                    '<section class="mo-detail-plate" aria-label="MO 评论">' +
                    '<div class="mo-detail-section-title">MO 评论</div>' +
                    '<div class="mo-comment-list" id="moCommentList">' + (commentsHtml || '<span class="muted">暂无</span>') + '</div>' +
                    '<div class="mo-form-grid">' +
                    '<label class="full">添加评论<textarea id="moNewCommentText" rows="2" placeholder="输入评论"></textarea></label>' +
                    '</div>' +
                    '<button type="button" class="pill-btn" id="moSubmitCommentBtn">提交评论</button>' +
                    '</section>' +
                    '<section class="mo-detail-plate" aria-label="流程事件">' +
                    '<div class="mo-detail-section-title">流程事件</div>' +
                    '<div class="mo-comment-list">' + (eventsHtml || '<span class="muted">暂无</span>') + '</div>' +
                    '</section>';

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
                            if (!cr.ok || cp.success === false) {
                                window.alert(cp.message || '失败');
                                return;
                            }
                            textarea.value = '';
                            openDetail(item);
                        } catch (err) {
                            window.alert(err.message || '评论失败');
                        }
                    });
                }

                const acceptDetailBtn = document.getElementById('moApplicantAcceptBtn');
                const rejectDetailBtn = document.getElementById('moApplicantRejectBtn');
                if (acceptDetailBtn) {
                    acceptDetailBtn.addEventListener('click', function () {
                        decide(item, 'selected');
                    });
                }
                if (rejectDetailBtn) {
                    rejectDetailBtn.addEventListener('click', function () {
                        decide(item, 'rejected');
                    });
                }
                }
            } catch (err) {
                detailBody.innerHTML = '<p class="mo-status-warn">' + escapeHtml(err.message || '加载失败') + '</p>';
            }

            await loadApplicants();
            await refreshNavUnreadBadge();
        }

        async function loadApplicants() {
            const code = (courseSelect.value || '').trim();
            const moId = getMoId();
            if (!moId) {
                setStatus('未登录或缺少 moId');
                renderApplicants([]);
                return;
            }
            setStatus('加载中...');
            try {
                var url = apiUrl('/api/mo/applicants') + '?moId=' + encodeURIComponent(moId);
                if (code) {
                    url += '&courseCode=' + encodeURIComponent(code);
                }
                const res = await fetch(url);
                const payload = await res.json();
                if (!res.ok || payload.success === false) {
                    setStatus(payload.message || '加载失败');
                    renderApplicants([]);
                    return;
                }
                const items = Array.isArray(payload.items) ? payload.items : [];
                renderApplicants(items);
                const unread = typeof payload.unreadCount === 'number' ? payload.unreadCount : 0;
                var scopeHint = code ? '' : '（全部课程）';
                setStatus('已加载 ' + items.length + ' 名申请人' + scopeHint + (unread > 0 ? ' · 未读 ' + unread + ' 条' : ''));
                if (typeof app.onApplicantsLoaded === 'function') app.onApplicantsLoaded(items);
                await refreshNavUnreadBadge();
            } catch (err) {
                setStatus(err.message || '加载失败');
                renderApplicants([]);
            }
        }

        async function decide(item, decision) {
            const code = (item && item.courseCode ? String(item.courseCode) : '').trim() || (courseSelect.value || '').trim();
            const moId = getMoId();
            const actionText = decision === 'selected' ? '录用' : '拒绝';
            const comment = window.prompt('请输入' + actionText + '备注（可选）', '');
            if (comment === null) return;
            if (!code) {
                setStatus('缺少课程编号，无法提交');
                return;
            }
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
                if (!res.ok || payload.success === false) {
                    setStatus(payload.message || '操作失败');
                    return;
                }
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

        async function refreshCourseOptionsFromApi() {
            if (!courseSelect) return;
            const moId = getMoId();
            if (!moId) {
                renderCourses([]);
                return;
            }
            try {
                const res = await fetch(apiUrl('/api/mo/jobs') + '?moId=' + encodeURIComponent(moId), {
                    headers: { Accept: 'application/json' }
                });
                const payload = await res.json();
                renderCourses(Array.isArray(payload.items) ? payload.items : []);
            } catch (e) {
                renderCourses([]);
            }
        }

        app.onJobsUpdated = function (jobs) {
            renderCourses(jobs);
            loadApplicants();
        };

        /** 进入「应聘筛选」或需强制同步岗位列表时调用（与课程管理同一数据源） */
        app.onApplicantsRouteActivate = function () {
            refreshCourseOptionsFromApi().then(function () {
                if (pendingApplicantCourseCode) {
                    var preset = pendingApplicantCourseCode;
                    pendingApplicantCourseCode = null;
                    app.setApplicantCourse(preset);
                } else {
                    loadApplicants();
                }
            });
        };

        /** 关闭弹窗后跳转应聘筛选并选中指定课程（先于 activateRoute 写入 pending，避免与异步下拉竞态） */
        app.navigateToApplicantsWithCourse = function (courseCode) {
            var c = courseCode == null ? '' : String(courseCode).trim();
            pendingApplicantCourseCode = c || null;
            if (typeof app.activateRoute === 'function') {
                app.activateRoute('applicants');
            }
        };

        app.setApplicantCourse = function (courseCode) {
            if (!courseCode) return;
            const raw = String(courseCode).trim();
            if (!raw) return;
            if (Array.from(courseSelect.options).some(function (o) { return o.value === raw; })) {
                courseSelect.value = raw;
            } else {
                courseSelect.value = '';
            }
            updateClearCourseFilterButton();
            loadApplicants();
        };
        app.refreshMoApplicantUnreadBadge = refreshNavUnreadBadge;

        refreshBtn.addEventListener('click', function () {
            refreshCourseOptionsFromApi().then(function () {
                loadApplicants();
            });
        });
        courseSelect.addEventListener('change', function () {
            updateClearCourseFilterButton();
            loadApplicants();
        });
        if (clearCourseFilterBtn) {
            clearCourseFilterBtn.addEventListener('click', function () {
                courseSelect.value = '';
                updateClearCourseFilterButton();
                loadApplicants();
            });
        }

        refreshNavUnreadBadge();
        refreshCourseOptionsFromApi();
        updateClearCourseFilterButton();
    };
})();
