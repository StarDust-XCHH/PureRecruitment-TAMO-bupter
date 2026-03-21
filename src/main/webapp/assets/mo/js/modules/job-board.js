(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.jobBoard = function initJobBoard(app) {
        const state = {
            jobs: [],
            page: 1,
            pageSize: 6,
            filteredJobs: []
        };

        const jobBoard = document.getElementById('jobBoard');
        const jobPagination = document.getElementById('jobPagination');
        const jobSearchInput = document.getElementById('jobSearchInput');
        const openCoursesCount = document.getElementById('openCoursesCount');
        const refreshJobsBtn = document.getElementById('refreshJobsBtn');
        const publishForm = document.getElementById('jobPublishForm');
        const publishStatus = document.getElementById('publishJobStatus');

        function toTags(item) {
            if (!Array.isArray(item.keywordTags)) return [];
            return item.keywordTags.slice(0, 4);
        }

        function toChecklist(item) {
            if (!Array.isArray(item.checklist)) return [];
            return item.checklist;
        }

        function renderDetail(item) {
            const setText = function (id, text) {
                const el = document.getElementById(id);
                if (el) el.textContent = text || '--';
            };
            setText('jobDetailCode', item.courseCode);
            setText('jobDetailName', item.courseName);
            setText('jobDetailMo', item.moName);
            setText('jobDetailDescription', item.courseDescription);
            setText('jobDetailDate', item.courseDate);
            setText('jobDetailTime', item.courseTime);
            setText('jobDetailLocation', item.courseLocation);
            setText('jobDetailStatus', item.status);

            const tags = document.getElementById('jobDetailTags');
            if (tags) {
                tags.innerHTML = '';
                toTags(item).forEach(function (tag) {
                    const span = document.createElement('span');
                    span.className = 'pill';
                    span.textContent = tag;
                    tags.appendChild(span);
                });
            }

            const checklist = document.getElementById('jobDetailChecklist');
            if (checklist) {
                checklist.innerHTML = '';
                toChecklist(item).forEach(function (entry) {
                    const li = document.createElement('li');
                    li.textContent = entry;
                    checklist.appendChild(li);
                });
            }

            const jumpBtn = document.getElementById('jumpToApplicantsBtn');
            if (jumpBtn) {
                jumpBtn.onclick = function () {
                    if (typeof app.closeAllModals === 'function') app.closeAllModals();
                    if (typeof app.activateRoute === 'function') app.activateRoute('applicants');
                    if (typeof app.setApplicantCourse === 'function') app.setApplicantCourse(item.courseCode);
                };
            }
        }

        function renderBoard() {
            const keyword = (jobSearchInput.value || '').trim().toLowerCase();
            state.filteredJobs = state.jobs.filter(function (item) {
                const text = [
                    item.courseCode,
                    item.courseName,
                    (item.keywordTags || []).join(' ')
                ].join(' ').toLowerCase();
                return !keyword || text.includes(keyword);
            });

            const totalPages = Math.max(1, Math.ceil(state.filteredJobs.length / state.pageSize));
            if (state.page > totalPages) state.page = 1;
            const start = (state.page - 1) * state.pageSize;
            const pageItems = state.filteredJobs.slice(start, start + state.pageSize);

            jobBoard.innerHTML = '';
            pageItems.forEach(function (item) {
                const card = document.createElement('div');
                card.className = 'job-card course-job-card';
                card.setAttribute('tabindex', '0');
                card.setAttribute('role', 'button');
                card.innerHTML =
                    '<div class="course-card-topline"><span class="job-code">' + item.courseCode + '</span><span class="pill">' + (item.moName || 'MO') + '</span></div>' +
                    '<h4>' + item.courseName + '</h4>' +
                    '<div class="job-tags">' + toTags(item).map(function (tag) { return '<span class="pill">' + tag + '</span>'; }).join('') + '</div>' +
                    '<div class="course-meta-stack">' +
                    '<div class="course-meta-item"><span class="course-meta-label">课程时间</span><strong>' + item.courseDate + ' · ' + item.courseTime + '</strong></div>' +
                    '<div class="course-meta-item"><span class="course-meta-label">招聘状态</span><strong>' + (item.status || '等待招聘 TA') + '</strong></div>' +
                    '</div>' +
                    '<div class="course-card-hint"><span>点击查看详情</span><span aria-hidden="true">→</span></div>';
                card.addEventListener('click', function () {
                    renderDetail(item);
                    if (typeof app.openModal === 'function') app.openModal('course-detail');
                });
                card.addEventListener('keydown', function (e) {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        card.click();
                    }
                });
                jobBoard.appendChild(card);
            });

            jobPagination.innerHTML = '';
            for (let i = 1; i <= totalPages; i += 1) {
                const btn = document.createElement('button');
                btn.className = 'job-page-btn' + (i === state.page ? ' active' : '');
                btn.type = 'button';
                btn.textContent = String(i);
                btn.addEventListener('click', function () {
                    state.page = i;
                    renderBoard();
                });
                jobPagination.appendChild(btn);
            }

            openCoursesCount.textContent = '开放课程 ' + state.jobs.length;
            if (typeof app.onJobsUpdated === 'function') app.onJobsUpdated(state.jobs);
        }

        async function loadJobs() {
            refreshJobsBtn.disabled = true;
            try {
                const res = await fetch('../../api/mo/jobs', { headers: { Accept: 'application/json' } });
                const payload = await res.json();
                state.jobs = Array.isArray(payload.items) ? payload.items : [];
                state.page = 1;
                renderBoard();
            } catch (err) {
                console.error('[MO-JOBS] load failed', err);
            } finally {
                refreshJobsBtn.disabled = false;
            }
        }

        async function publishJob(event) {
            event.preventDefault();
            const moUser = typeof app.getMoUser === 'function' ? app.getMoUser() : null;
            const body = {
                moName: (moUser && moUser.username) ? moUser.username : 'MO',
                courseName: document.getElementById('courseNameInput').value,
                courseDate: document.getElementById('courseDateInput').value,
                courseTime: document.getElementById('courseTimeInput').value,
                courseLocation: document.getElementById('courseLocationInput').value,
                keywordTags: document.getElementById('courseTagsInput').value,
                checklist: document.getElementById('courseChecklistInput').value,
                courseDescription: document.getElementById('courseDescInput').value
            };

            publishStatus.textContent = '发布中...';
            try {
                const res = await fetch('../../api/mo/jobs', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
                    body: JSON.stringify(body)
                });
                const payload = await res.json();
                if (!res.ok || payload.success === false) {
                    throw new Error(payload.message || '发布失败');
                }
                publishStatus.textContent = '发布成功';
                publishForm.reset();
                await loadJobs();
            } catch (err) {
                publishStatus.textContent = err.message || '发布失败';
            }
        }

        app.getJobs = function () { return state.jobs.slice(); };
        app.loadJobs = loadJobs;

        publishForm.addEventListener('submit', publishJob);
        jobSearchInput.addEventListener('input', function () {
            state.page = 1;
            renderBoard();
        });
        refreshJobsBtn.addEventListener('click', loadJobs);

        loadJobs();
    };
})();
