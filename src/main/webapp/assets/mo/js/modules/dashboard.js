(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.dashboard = function initDashboard(app) {
        const dashOpenJobs = document.getElementById('dashOpenJobs');
        const dashCandidates = document.getElementById('dashCandidates');
        const dashAccepted = document.getElementById('dashAccepted');
        const dashPending = document.getElementById('dashPending');
        const dashSemesterPill = document.getElementById('dashSemesterPill');
        const dashRoleContextPill = document.getElementById('dashRoleContextPill');
        const dashWorkflowHint = document.getElementById('dashWorkflowHint');

        const summaryEls = {
            open: document.getElementById('summarySnapshotOpen'),
            candidates: document.getElementById('summarySnapshotCandidates'),
            accepted: document.getElementById('summarySnapshotAccepted'),
            pending: document.getElementById('summarySnapshotPending'),
            unread: document.getElementById('summarySnapshotUnread'),
            jobList: document.getElementById('summaryJobList'),
            jobEmpty: document.getElementById('summaryJobEmpty'),
            nextHint: document.getElementById('summaryNextHint')
        };

        let latestApplicants = [];

        function apiUrl(path) {
            var p = path.charAt(0) === '/' ? path : '/' + path;
            if (typeof window.moApiPath === 'function') {
                return window.moApiPath(p);
            }
            return '../../' + p.replace(/^\//, '');
        }

        function getMoId() {
            const u = typeof app.getMoUser === 'function' ? app.getMoUser() : null;
            if (!u) return '';
            return (u.moId || u.id || '').trim();
        }

        function semesterSummaryFromJobs(jobs) {
            const seen = {};
            (jobs || []).forEach(function (j) {
                const s = j && j.semester != null ? String(j.semester).trim() : '';
                if (s) seen[s] = true;
            });
            const keys = Object.keys(seen);
            if (keys.length === 0) return '—';
            if (keys.length === 1) return keys[0];
            keys.sort();
            return keys.slice(0, 2).join('、') + (keys.length > 2 ? '…' : '');
        }

        function updateContextPills() {
            const jobs = typeof app.getJobs === 'function' ? app.getJobs() : [];
            if (dashSemesterPill) {
                dashSemesterPill.textContent = '学期：' + semesterSummaryFromJobs(jobs);
            }
            if (dashRoleContextPill) {
                const u = typeof app.getMoUser === 'function' ? app.getMoUser() : null;
                const name = u && (u.name || u.moName || u.username) ? String(u.name || u.moName || u.username).trim() : '';
                dashRoleContextPill.textContent = name ? 'MO · ' + name : 'Module Organizer';
            }
        }

        function updateWorkflowHint(pendingCount) {
            if (!dashWorkflowHint) return;
            const p = typeof pendingCount === 'number' ? pendingCount : 0;
            if (p > 0) {
                dashWorkflowHint.hidden = false;
                dashWorkflowHint.textContent =
                    '当前有 ' + p + ' 条申请待决策，建议在侧栏进入「应聘筛选」优先处理。';
            } else {
                dashWorkflowHint.hidden = true;
                dashWorkflowHint.textContent = '';
            }
        }

        function isPendingDecision(item) {
            return item.status !== '已录用' && item.status !== '未录用';
        }

        /** 与总览「候选池规模」一致：按 TA 去重（优先 taId，否则姓名，再否则 applicationId） */
        function applicantIdentityKey(item) {
            if (!item) return 'x';
            var tid = item.taId != null && String(item.taId).trim() !== '' ? String(item.taId).trim() : '';
            if (tid) return 'ta:' + tid;
            var nm = item.name != null && String(item.name).trim() !== '' ? String(item.name).trim() : '';
            if (nm) return 'nm:' + nm;
            if (item.applicationId != null && String(item.applicationId).trim() !== '') {
                return 'app:' + String(item.applicationId).trim();
            }
            return 'anon';
        }

        function uniqueTaCount(applicants) {
            var seen = new Set();
            (applicants || []).forEach(function (item) {
                seen.add(applicantIdentityKey(item));
            });
            return seen.size;
        }

        function groupApplicantsByCourse(applicants) {
            var buckets = {};
            (applicants || []).forEach(function (item) {
                var code = item.courseCode != null && String(item.courseCode).trim() !== ''
                    ? String(item.courseCode).trim()
                    : '—';
                var cname = item.courseName != null && String(item.courseName).trim() !== ''
                    ? String(item.courseName).trim()
                    : '';
                var key = code + '\x01' + cname;
                if (!buckets[key]) {
                    buckets[key] = { courseCode: code, courseName: cname, items: [] };
                }
                buckets[key].items.push(item);
            });
            return Object.keys(buckets).sort(function (a, b) {
                return a.localeCompare(b, 'zh-CN');
            }).map(function (k) {
                return buckets[k];
            });
        }

        function computeStats(jobs, applicants) {
            const acc = applicants.filter(function (item) { return item.status === '已录用'; }).length;
            const pend = applicants.filter(isPendingDecision).length;
            return {
                open: jobs.length,
                candidates: uniqueTaCount(applicants),
                accepted: acc,
                pending: pend
            };
        }

        function populateSummaryModal() {
            const jobs = typeof app.getJobs === 'function' ? app.getJobs() : [];
            const applicants = Array.isArray(latestApplicants) ? latestApplicants : [];
            const st = computeStats(jobs, applicants);

            if (summaryEls.open) summaryEls.open.textContent = String(st.open);
            if (summaryEls.candidates) summaryEls.candidates.textContent = String(st.candidates);
            if (summaryEls.accepted) summaryEls.accepted.textContent = String(st.accepted);
            if (summaryEls.pending) summaryEls.pending.textContent = String(st.pending);
            if (summaryEls.unread) summaryEls.unread.textContent = '…';

            if (summaryEls.jobList) {
                summaryEls.jobList.innerHTML = '';
                const slice = (jobs || []).slice(0, 12);
                slice.forEach(function (j) {
                    const code = j.courseCode != null && String(j.courseCode).trim() !== ''
                        ? String(j.courseCode).trim()
                        : (j.jobId != null ? String(j.jobId) : '—');
                    const name = j.courseName != null && String(j.courseName).trim() !== ''
                        ? String(j.courseName).trim()
                        : '';
                    const li = document.createElement('li');
                    li.className = 'mo-summary-job-row';
                    const label = document.createElement('span');
                    label.className = 'mo-summary-job-row__label';
                    label.textContent = name ? code + ' · ' + name : code;
                    li.appendChild(label);
                    const btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'mo-dash-detail-link mo-summary-job-row__detail';
                    btn.textContent = '查看详情';
                    btn.addEventListener('click', function (e) {
                        e.stopPropagation();
                        if (typeof app.showMoCourseJobDetail === 'function') {
                            app.showMoCourseJobDetail(j);
                        }
                    });
                    li.appendChild(btn);
                    summaryEls.jobList.appendChild(li);
                });
            }
            if (summaryEls.jobEmpty) {
                summaryEls.jobEmpty.hidden = (jobs || []).length > 0;
            }

            if (summaryEls.nextHint) {
                if (st.pending > 0) {
                    summaryEls.nextHint.textContent =
                        '建议：前往「应聘筛选」处理 ' + st.pending + ' 条待决策申请。';
                } else if (st.open === 0) {
                    summaryEls.nextHint.textContent =
                        '建议：在「课程管理」中发布岗位以接收 TA 申请。';
                } else {
                    summaryEls.nextHint.textContent =
                        '当前暂无待决策队列；可持续关注侧栏未读提示与新投递。';
                }
            }

            const moId = getMoId();
            if (!moId || !summaryEls.unread) {
                if (summaryEls.unread) summaryEls.unread.textContent = moId ? '—' : '—';
                return;
            }
            fetch(apiUrl('/api/mo/applicants/unread-count') + '?moId=' + encodeURIComponent(moId), {
                headers: { Accept: 'application/json' }
            })
                .then(function (res) { return res.json(); })
                .then(function (payload) {
                    const n = typeof payload.unreadCount === 'number' ? payload.unreadCount : 0;
                    if (summaryEls.unread) summaryEls.unread.textContent = String(n);
                })
                .catch(function () {
                    if (summaryEls.unread) summaryEls.unread.textContent = '—';
                });
        }

        function attachApplicantDetailButton(hostEl, item, layout) {
            if (!hostEl) return;
            layout = layout || 'under-submit';
            const actions = document.createElement('div');
            actions.className = 'mo-dash-cand-item__actions';
            if (layout === 'compact-aside') {
                actions.classList.add('mo-dash-cand-item__actions--compact-aside');
            } else {
                actions.classList.add('mo-dash-cand-item__actions--under-submit');
            }
            if (!item || !item.applicationId) {
                const hint = document.createElement('span');
                hint.className = 'muted mo-dash-detail-hint';
                hint.textContent = '无申请 ID';
                actions.appendChild(hint);
            } else {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'mo-dash-detail-link';
                btn.textContent = '查看详情';
                btn.addEventListener('click', function (e) {
                    e.stopPropagation();
                    if (typeof app.openMoApplicantDetail === 'function') {
                        app.openMoApplicantDetail(item);
                    }
                });
                actions.appendChild(btn);
            }
            hostEl.appendChild(actions);
        }

        function appendApplicantRow(ul, item) {
            if (!ul || !item) return;
            const li = document.createElement('li');
            li.className = 'mo-dash-cand-item';

            const wrap = document.createElement('div');
            wrap.className = 'mo-dash-cand-item__compact-wrap';

            const main = document.createElement('div');
            main.className = 'mo-dash-cand-item__compact-main';

            const nameRow = document.createElement('div');
            nameRow.className = 'mo-dash-cand-item__row';
            const nameEl = document.createElement('span');
            nameEl.className = 'mo-dash-cand-item__name';
            nameEl.textContent = item.name || item.taId || '—';
            nameRow.appendChild(nameEl);
            main.appendChild(nameRow);

            const meta = document.createElement('div');
            meta.className = 'mo-dash-cand-item__meta muted';
            const cc = item.courseCode != null && String(item.courseCode).trim() !== ''
                ? String(item.courseCode).trim()
                : '—';
            const cn = item.courseName != null && String(item.courseName).trim() !== ''
                ? String(item.courseName).trim()
                : '';
            const tid = item.taId != null && String(item.taId).trim() !== '' ? String(item.taId).trim() : '';
            meta.textContent = cc + (cn ? ' · ' + cn : '') + (tid ? ' · ' + tid : '');
            main.appendChild(meta);

            const subStr = formatSubmittedShort(item.submittedAt);
            if (subStr) {
                const dtEl = document.createElement('div');
                dtEl.className = 'mo-dash-cand-item__dt muted';
                dtEl.textContent = '投递 · ' + subStr;
                main.appendChild(dtEl);
            }

            const aside = document.createElement('div');
            aside.className = 'mo-dash-cand-item__compact-aside';
            const statusEl = document.createElement('span');
            statusEl.className = 'mo-dash-cand-item__status';
            statusEl.textContent = item.status || '—';
            aside.appendChild(statusEl);
            attachApplicantDetailButton(aside, item, 'compact-aside');

            wrap.appendChild(main);
            wrap.appendChild(aside);
            li.appendChild(wrap);
            ul.appendChild(li);
        }

        function formatSubmittedShort(iso) {
            if (iso == null || String(iso).trim() === '') return '';
            try {
                var d = new Date(iso);
                if (isNaN(d.getTime())) return '';
                return d.toLocaleString('zh-CN', {
                    month: 'numeric',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit'
                });
            } catch (e) {
                return '';
            }
        }

        /** 课程分组内：左栏姓名/TA/投递；右栏状态 +「查看详情」（状态正下方） */
        function appendApplicantRowCompact(ul, item) {
            if (!ul || !item) return;
            const li = document.createElement('li');
            li.className = 'mo-dash-cand-item mo-dash-cand-item--compact';

            const wrap = document.createElement('div');
            wrap.className = 'mo-dash-cand-item__compact-wrap';

            const main = document.createElement('div');
            main.className = 'mo-dash-cand-item__compact-main';

            const nameRow = document.createElement('div');
            nameRow.className = 'mo-dash-cand-item__row';
            const nameEl = document.createElement('span');
            nameEl.className = 'mo-dash-cand-item__name';
            nameEl.textContent = item.name || item.taId || '—';
            nameRow.appendChild(nameEl);
            if (item.unread) {
                const unreadEl = document.createElement('span');
                unreadEl.className = 'mo-dash-cand-item__unread';
                unreadEl.textContent = '未读';
                unreadEl.setAttribute('title', '未读投递');
                nameRow.appendChild(unreadEl);
            }
            main.appendChild(nameRow);

            const tid = item.taId != null && String(item.taId).trim() !== '' ? String(item.taId).trim() : '';
            if (tid) {
                const meta = document.createElement('div');
                meta.className = 'mo-dash-cand-item__meta muted';
                meta.textContent = tid;
                main.appendChild(meta);
            }
            const subStr = formatSubmittedShort(item.submittedAt);
            if (subStr) {
                const dtEl = document.createElement('div');
                dtEl.className = 'mo-dash-cand-item__dt muted';
                dtEl.textContent = '投递 · ' + subStr;
                main.appendChild(dtEl);
            }

            const aside = document.createElement('div');
            aside.className = 'mo-dash-cand-item__compact-aside';
            const statusEl = document.createElement('span');
            statusEl.className = 'mo-dash-cand-item__status';
            statusEl.textContent = item.status || '—';
            aside.appendChild(statusEl);
            attachApplicantDetailButton(aside, item, 'compact-aside');

            wrap.appendChild(main);
            wrap.appendChild(aside);
            li.appendChild(wrap);
            ul.appendChild(li);
        }

        function populateCandidatesPoolModal() {
            const applicants = Array.isArray(latestApplicants) ? latestApplicants : [];
            const uniq = uniqueTaCount(applicants);
            const nApps = applicants.length;
            const countEl = document.getElementById('candidatesPoolCountLine');
            const emptyEl = document.getElementById('candidatesPoolEmpty');
            const groupedEl = document.getElementById('candidatesPoolGrouped');
            if (countEl) {
                countEl.textContent =
                    '共 ' + uniq + ' 位候选 TA（去重），' + nApps + ' 条投递；总览卡片数字为去重人数。';
            }
            if (groupedEl) {
                groupedEl.innerHTML = '';
                const groups = groupApplicantsByCourse(applicants);
                groups.forEach(function (g, idx) {
                    const details = document.createElement('details');
                    details.className = 'mo-dash-cand-group mo-dash-cand-group--accordion';
                    if (idx === 0) {
                        details.setAttribute('open', '');
                    }
                    const summary = document.createElement('summary');
                    summary.className = 'mo-dash-cand-group__summary';
                    var title = g.courseCode;
                    if (g.courseName) {
                        title += ' · ' + g.courseName;
                    }
                    summary.textContent = title + '（' + g.items.length + ' 条）';
                    const ul = document.createElement('ul');
                    ul.className = 'mo-dash-cand-list mo-dash-cand-list--in-group';
                    g.items.forEach(function (item) {
                        appendApplicantRowCompact(ul, item);
                    });
                    details.appendChild(summary);
                    details.appendChild(ul);
                    groupedEl.appendChild(details);
                });
            }
            if (emptyEl) {
                emptyEl.hidden = applicants.length > 0;
            }
        }

        function populateAcceptedModal() {
            const applicants = Array.isArray(latestApplicants) ? latestApplicants : [];
            const hired = applicants.filter(function (item) {
                return item.status === '已录用';
            });
            const countEl = document.getElementById('acceptedListCountLine');
            const emptyEl = document.getElementById('acceptedListEmpty');
            const listEl = document.getElementById('acceptedListDetail');
            if (countEl) {
                countEl.textContent = '共 ' + hired.length + ' 条已录用。';
            }
            if (listEl) {
                listEl.innerHTML = '';
                hired.forEach(function (item) {
                    appendApplicantRow(listEl, item);
                });
            }
            if (emptyEl) {
                emptyEl.hidden = hired.length > 0;
            }
        }

        function populatePendingModal() {
            const applicants = Array.isArray(latestApplicants) ? latestApplicants : [];
            const pending = applicants.filter(isPendingDecision);
            const countEl = document.getElementById('pendingListCountLine');
            const emptyEl = document.getElementById('pendingListEmpty');
            const listEl = document.getElementById('pendingListDetail');
            if (countEl) {
                countEl.textContent = '共 ' + pending.length + ' 条待决策（与总览「待决策」数字一致）。';
            }
            if (listEl) {
                listEl.innerHTML = '';
                pending.forEach(function (item) {
                    appendApplicantRow(listEl, item);
                });
            }
            if (emptyEl) {
                emptyEl.hidden = pending.length > 0;
            }
        }

        function render() {
            const jobs = typeof app.getJobs === 'function' ? app.getJobs() : [];
            const applicants = Array.isArray(latestApplicants) ? latestApplicants : [];
            const accepted = applicants.filter(function (item) { return item.status === '已录用'; }).length;
            const pending = applicants.filter(isPendingDecision).length;

            if (dashOpenJobs) dashOpenJobs.textContent = String(jobs.length);
            if (dashCandidates) dashCandidates.textContent = String(uniqueTaCount(applicants));
            if (dashAccepted) dashAccepted.textContent = String(accepted);
            if (dashPending) dashPending.textContent = String(pending);

            updateContextPills();
            updateWorkflowHint(pending);
        }

        app.onApplicantsLoaded = function (items) {
            latestApplicants = items || [];
            render();
        };
        app.loadDashboard = render;

        document.querySelectorAll('[data-modal-target="summary"]').forEach(function (node) {
            node.addEventListener(
                'click',
                function () {
                    populateSummaryModal();
                },
                true
            );
        });

        document.querySelectorAll('[data-modal-target="candidates-pool"]').forEach(function (node) {
            node.addEventListener(
                'click',
                function () {
                    populateCandidatesPoolModal();
                },
                true
            );
        });

        document.querySelectorAll('[data-modal-target="accepted-list"]').forEach(function (node) {
            node.addEventListener(
                'click',
                function () {
                    populateAcceptedModal();
                },
                true
            );
        });

        document.querySelectorAll('[data-modal-target="pending-list"]').forEach(function (node) {
            node.addEventListener(
                'click',
                function () {
                    populatePendingModal();
                },
                true
            );
        });

        render();
    };
})();
