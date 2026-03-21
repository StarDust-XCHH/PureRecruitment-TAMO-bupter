(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.applicants = function initApplicants(app) {
        const courseSelect = document.getElementById('applicantCourseSelect');
        const refreshBtn = document.getElementById('refreshApplicantsBtn');
        const statusText = document.getElementById('applicantsStatusText');
        const list = document.getElementById('applicantList');
        const emptyState = document.getElementById('applicantEmptyState');

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

        function renderApplicants(items) {
            list.innerHTML = '';
            const rows = Array.isArray(items) ? items : [];
            emptyState.hidden = rows.length > 0;
            if (rows.length === 0) return;

            rows.forEach(function (item) {
                const card = document.createElement('div');
                card.className = 'mo-applicant-card';
                const toneClass = item.status === '已录用' ? 'mo-status-ok' : (item.status === '可邀请' ? '' : 'mo-status-warn');

                card.innerHTML =
                    '<div class="mo-applicant-head">' +
                    '<div><strong>' + (item.name || item.taId) + '</strong><div class="muted">' + item.taId + '</div></div>' +
                    '<div class="' + toneClass + '">' + (item.status || '未知') + '</div>' +
                    '</div>' +
                    '<div class="mo-applicant-meta">' +
                    '<span>邮箱：' + (item.email || '--') + '</span>' +
                    '<span>意向：' + (item.intent || '--') + '</span>' +
                    '</div>' +
                    '<div class="muted">技能：' + (item.skills || '--') + '</div>' +
                    '<div class="muted">备注：' + (item.comment || '--') + '</div>';

                const actions = document.createElement('div');
                actions.className = 'mo-applicant-actions';

                const acceptBtn = document.createElement('button');
                acceptBtn.className = 'pill-btn';
                acceptBtn.type = 'button';
                acceptBtn.textContent = '录用';
                acceptBtn.addEventListener('click', function () {
                    decide(item.taId, 'selected');
                });

                const rejectBtn = document.createElement('button');
                rejectBtn.className = 'pill-btn ghost';
                rejectBtn.type = 'button';
                rejectBtn.textContent = '拒绝';
                rejectBtn.addEventListener('click', function () {
                    decide(item.taId, 'rejected');
                });

                actions.appendChild(acceptBtn);
                actions.appendChild(rejectBtn);
                card.appendChild(actions);
                list.appendChild(card);
            });
        }

        async function loadApplicants() {
            const code = courseSelect.value;
            if (!code) {
                renderApplicants([]);
                setStatus('请先选择课程');
                return;
            }
            setStatus('加载中...');
            try {
                const res = await fetch('../../api/mo/applicants?courseCode=' + encodeURIComponent(code));
                const payload = await res.json();
                const items = Array.isArray(payload.items) ? payload.items : [];
                renderApplicants(items);
                setStatus('已加载 ' + items.length + ' 名候选人');
                if (typeof app.onApplicantsLoaded === 'function') app.onApplicantsLoaded(items);
            } catch (err) {
                setStatus('加载失败');
            }
        }

        async function decide(taId, decision) {
            const code = courseSelect.value;
            const actionText = decision === 'selected' ? '录用' : '拒绝';
            const comment = window.prompt('请输入' + actionText + '备注（可选）', '');
            try {
                const res = await fetch('../../api/mo/applications/select', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
                    body: JSON.stringify({
                        courseCode: code,
                        taId: taId,
                        decision: decision,
                        comment: comment || ''
                    })
                });
                const payload = await res.json();
                if (!res.ok || payload.success === false) throw new Error(payload.message || '操作失败');
                setStatus('已完成：' + actionText + ' ' + taId);
                await loadApplicants();
                if (typeof app.loadDashboard === 'function') app.loadDashboard();
            } catch (err) {
                setStatus(err.message || '保存失败');
            }
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

        refreshBtn.addEventListener('click', loadApplicants);
        courseSelect.addEventListener('change', loadApplicants);
    };
})();
