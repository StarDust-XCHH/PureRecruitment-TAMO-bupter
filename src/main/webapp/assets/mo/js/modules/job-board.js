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
        const fixedSkillsInput = document.getElementById('fixedSkillsInput');

        function parseCsv(text) {
            return String(text || '')
                .split(',')
                .map(function (s) { return s.trim(); })
                .filter(Boolean);
        }

        function readSelectedValues(selectId) {
            const el = document.getElementById(selectId);
            if (!el) return [];
            return Array.from(el.selectedOptions || []).map(function (opt) { return opt.value; }).filter(Boolean);
        }

        async function loadSkillTags() {
            if (!fixedSkillsInput) return;
            try {
                const res = await fetch('../../api/mo/skill-tags', { headers: { Accept: 'application/json' } });
                if (!res.ok) return;
                const payload = await res.json();
                const items = Array.isArray(payload.items) ? payload.items : [];
                if (!items.length) return;
                const selected = new Set(Array.from(fixedSkillsInput.selectedOptions || []).map(function (opt) { return opt.value; }));
                fixedSkillsInput.innerHTML = '';
                items.forEach(function (tag) {
                    const text = String(tag || '').trim();
                    if (!text) return;
                    const option = document.createElement('option');
                    option.value = text;
                    option.textContent = text;
                    if (selected.has(text)) option.selected = true;
                    fixedSkillsInput.appendChild(option);
                });
            } catch (err) {
                console.warn('[MO-JOBS] load skill tags failed', err);
            }
        }

        function parseAssessmentEvents(multiline) {
            const lines = String(multiline || '')
                .split(/\r?\n/)
                .map(function (line) { return line.trim(); })
                .filter(Boolean);
            return lines.map(function (line) {
                const parts = line.split('|');
                const weeks = parseCsv((parts[1] || '').trim())
                    .map(function (v) { return Number(v); })
                    .filter(function (n) { return Number.isFinite(n) && n >= 1 && n <= 20; });
                const uniqWeeks = Array.from(new Set(weeks)).sort(function (a, b) { return a - b; });
                return {
                    name: (parts[0] || '').trim(),
                    weeks: uniqWeeks,
                    description: (parts[2] || '').trim()
                };
            }).filter(function (entry) { return entry.name; });
        }

        function parseCustomSkills(multiline) {
            const lines = String(multiline || '')
                .split(/\r?\n/)
                .map(function (line) { return line.trim(); })
                .filter(Boolean);
            return lines.map(function (line) {
                const parts = line.split('|');
                return {
                    name: (parts[0] || '').trim(),
                    description: (parts[1] || '').trim()
                };
            }).filter(function (entry) { return entry.name; });
        }

        function parseTeachingWeeks() {
            const customWeeks = parseCsv(document.getElementById('teachingWeeksCustomInput').value)
                .map(function (v) { return Number(v); })
                .filter(function (n) { return Number.isFinite(n) && n >= 1 && n <= 20; });
            const uniqWeeks = Array.from(new Set(customWeeks)).sort(function (a, b) { return a - b; });
            return {
                weeks: uniqWeeks
            };
        }

        function weekText(teachingWeeks) {
            const weeks = teachingWeeks && Array.isArray(teachingWeeks.weeks) ? teachingWeeks.weeks : [];
            if (!weeks.length) return '--';
            return weeks.join(',');
        }

        function getDisplayCode(item) {
            return item.courseCode || item.jobId || '--';
        }

        function getDisplayTime(item) {
            const weeksText = weekText(item.teachingWeeks);
            if (weeksText !== '--') {
                return 'Week ' + weeksText;
            }
            return '--';
        }

        function getDisplayLocation(item) {
            return item && item.campus ? item.campus : '--';
        }

        function toTags(item) {
            if (item.requiredSkills && Array.isArray(item.requiredSkills.fixedTags)) {
                return item.requiredSkills.fixedTags.slice(0, 4);
            }
            return [];
        }

        function renderDetail(item) {
            const setText = function (id, text) {
                const el = document.getElementById(id);
                if (el) el.textContent = text || '--';
            };
            setText('jobDetailCode', getDisplayCode(item));
            setText('jobDetailName', item.courseName || '未命名岗位');
            setText('jobDetailMo', item.moName);
            setText('jobDetailDescription', item.recruitmentBrief || item.courseDescription);
            setText('jobDetailDate', item.teachingWeeks ? ('Week ' + weekText(item.teachingWeeks)) : '--');
            setText('jobDetailTime', '--');
            setText('jobDetailLocation', getDisplayLocation(item));
            setText('jobDetailStatus', item.recruitmentStatus || item.status || 'OPEN');

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
            }

            const jumpBtn = document.getElementById('jumpToApplicantsBtn');
            if (jumpBtn) {
                jumpBtn.onclick = function () {
                    if (typeof app.closeAllModals === 'function') app.closeAllModals();
                    if (typeof app.activateRoute === 'function') app.activateRoute('applicants');
                    if (typeof app.setApplicantCourse === 'function') app.setApplicantCourse(item.courseCode || item.jobId);
                };
            }
        }

        function renderBoard() {
            const keyword = (jobSearchInput.value || '').trim().toLowerCase();
            state.filteredJobs = state.jobs.filter(function (item) {
                const text = [
                    item.courseCode || item.jobId,
                    item.courseName,
                    item.recruitmentBrief || '',
                    item.courseDescription || '',
                    (item.requiredSkills && item.requiredSkills.fixedTags || []).join(' ')
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
                    '<div class="course-card-topline"><span class="job-code">' + getDisplayCode(item) + '</span><span class="pill">' + (item.moName || 'MO') + '</span></div>' +
                    '<h4>' + (item.courseName || '未命名岗位') + '</h4>' +
                    '<div class="job-tags">' + toTags(item).map(function (tag) { return '<span class="pill">' + tag + '</span>'; }).join('') + '</div>' +
                    '<div class="course-meta-stack">' +
                    '<div class="course-meta-item"><span class="course-meta-label">课程/学期安排</span><strong>' + getDisplayTime(item) + '</strong></div>' +
                    '<div class="course-meta-item"><span class="course-meta-label">招聘状态</span><strong>' + (item.recruitmentStatus || item.status || 'OPEN') + '</strong></div>' +
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
            const teachingWeeks = parseTeachingWeeks();
            const fixedSkills = readSelectedValues('fixedSkillsInput');
            const courseCodeInput = document.getElementById('courseCodeInput').value.trim();
            const courseNameInput = document.getElementById('courseNameInput').value.trim();
            const semesterInput = document.getElementById('semesterInput').value.trim();
            const recruitmentStatusInput = document.getElementById('recruitmentStatusInput').value.trim();
            const courseDescInput = document.getElementById('courseDescInput').value.trim();
            const studentCountRaw = document.getElementById('studentCountInput').value.trim();
            const taRecruitCountRaw = document.getElementById('taRecruitCountInput').value.trim();
            const campusRaw = document.getElementById('campusInput').value.trim();
            const applicationDeadlineRaw = document.getElementById('applicationDeadlineInput').value.trim();
            const recruitmentBriefRaw = document.getElementById('recruitmentBriefInput').value.trim();
            const moUsername = (moUser && moUser.username) ? moUser.username : 'MO';
            const ownerMoId = (moUser && (moUser.id || moUser.moId || moUser.userId))
                ? String(moUser.id || moUser.moId || moUser.userId).trim()
                : moUsername;

            if (!courseNameInput || !courseCodeInput || !recruitmentStatusInput || !courseDescInput || fixedSkills.length === 0) {
                publishStatus.textContent = '请填写必填项：课程名、课程编号、招聘状态、至少一个技能标签、岗位描述';
                return;
            }

            const body = {
                courseCode: courseCodeInput,
                moName: moUsername,
                ownerMoId: ownerMoId,
                ownerMoName: moUsername,
                courseName: courseNameInput,
                recruitmentStatus: recruitmentStatusInput,
                status: recruitmentStatusInput,
                assessmentEvents: parseAssessmentEvents(document.getElementById('assessmentEventsInput').value),
                requiredSkills: {
                    fixedTags: fixedSkills,
                    customSkills: parseCustomSkills(document.getElementById('customSkillsInput').value)
                },
                courseDescription: courseDescInput,
                source: 'mo-manual-v2'
            };
            if (semesterInput) body.semester = semesterInput;
            if (teachingWeeks.weeks.length) body.teachingWeeks = teachingWeeks;
            body.studentCount = studentCountRaw === '' ? -1 : (Number(studentCountRaw) || 0);
            if (taRecruitCountRaw !== '') body.taRecruitCount = Number(taRecruitCountRaw) || 0;
            if (campusRaw) body.campus = campusRaw;
            if (applicationDeadlineRaw) body.applicationDeadline = new Date(applicationDeadlineRaw).toISOString();
            if (recruitmentBriefRaw) body.recruitmentBrief = recruitmentBriefRaw;

            publishStatus.textContent = '发布中...';
            try {
                const res = await fetch('../../api/mo/jobs', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
                    body: JSON.stringify(body)
                });
                const payload = await res.json();
                if (!res.ok || payload.success === false) {
                    publishStatus.textContent = payload.message || '发布失败';
                    return;
                }
                publishStatus.textContent = '发布成功';
                publishForm.reset();
                await loadJobs();
            } catch (err) {
                publishStatus.textContent = (err && err.message) ? err.message : '发布失败';
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

        loadSkillTags();
        loadJobs();
    };
})();
