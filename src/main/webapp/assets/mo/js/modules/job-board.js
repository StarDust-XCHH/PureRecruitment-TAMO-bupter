(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    modules.jobBoard = function initJobBoard(app) {
        function apiUrl(path) {
            var p = path.charAt(0) === '/' ? path : '/' + path;
            if (typeof window.moApiPath === 'function') {
                return window.moApiPath(p);
            }
            return '../../' + p.replace(/^\//, '');
        }

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

        const courseEditPanel = document.getElementById('courseEditPanel');
        const toggleCourseEditBtn = document.getElementById('toggleCourseEditBtn');
        const saveCourseEditBtn = document.getElementById('saveCourseEditBtn');
        const courseEditStatus = document.getElementById('courseEditStatus');
        const editFixedSkillsInput = document.getElementById('editFixedSkillsInput');

        let currentDetailJob = null;

        function readMoUserFromStorage() {
            const raw = sessionStorage.getItem('mo-user') || localStorage.getItem('mo-user');
            if (!raw) return null;
            try {
                return JSON.parse(raw);
            } catch (e) {
                return null;
            }
        }

        function getMoIdForApi() {
            const u = typeof app.getMoUser === 'function' ? app.getMoUser() : null;
            let id = u && (u.moId || u.id) ? String(u.moId || u.id).trim() : '';
            if (!id) {
                const fb = readMoUserFromStorage();
                id = fb && (fb.moId || fb.id) ? String(fb.moId || fb.id).trim() : '';
            }
            return id;
        }

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

        async function loadSkillTagsInto(selectEl) {
            if (!selectEl) return;
            try {
                const res = await fetch(apiUrl('/api/mo/skill-tags'), { headers: { Accept: 'application/json' } });
                if (!res.ok) return;
                const payload = await res.json();
                const items = Array.isArray(payload.items) ? payload.items : [];
                if (!items.length) return;
                const selected = new Set(Array.from(selectEl.selectedOptions || []).map(function (opt) { return opt.value; }));
                selectEl.innerHTML = '';
                items.forEach(function (tag) {
                    const text = String(tag || '').trim();
                    if (!text) return;
                    const option = document.createElement('option');
                    option.value = text;
                    option.textContent = text;
                    if (selected.has(text)) option.selected = true;
                    selectEl.appendChild(option);
                });
            } catch (err) {
                console.warn('[MO-JOBS] load skill tags failed', err);
            }
        }

        async function loadSkillTags() {
            await loadSkillTagsInto(fixedSkillsInput);
            await loadSkillTagsInto(editFixedSkillsInput);
        }

        function isoToDatetimeLocal(iso) {
            if (!iso) return '';
            const d = new Date(iso);
            if (Number.isNaN(d.getTime())) return '';
            const pad = function (n) { return String(n).padStart(2, '0'); };
            return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
        }

        function formatAssessmentEventsForEdit(arr) {
            if (!arr || !arr.length) return '';
            return arr.map(function (ev) {
                const w = (ev.weeks || []).join(',');
                return (ev.name || '') + '|' + w + '|' + (ev.description || '');
            }).join('\n');
        }

        function formatCustomSkillsForEdit(rs) {
            if (!rs || !Array.isArray(rs.customSkills)) return '';
            return rs.customSkills.map(function (c) {
                return (c.name || '') + '|' + (c.description || '');
            }).join('\n');
        }

        async function populateCourseEditForm(item) {
            if (!item) return;
            const codeEl = document.getElementById('editCourseCodeDisplay');
            if (codeEl) codeEl.textContent = item.courseCode || item.jobId || '--';
            const setVal = function (id, v) {
                const el = document.getElementById(id);
                if (el) el.value = v == null ? '' : String(v);
            };
            setVal('editCourseNameInput', item.courseName || '');
            setVal('editSemesterInput', item.semester || '');
            setVal('editRecruitmentStatusInput', item.recruitmentStatus || item.status || 'OPEN');
            setVal('editStudentCountInput', item.studentCount != null && item.studentCount >= 0 ? item.studentCount : '');
            setVal('editTaRecruitCountInput', item.taRecruitCount != null ? item.taRecruitCount : '');
            const campusEl = document.getElementById('editCampusInput');
            if (campusEl) campusEl.value = item.campus || '';
            setVal('editApplicationDeadlineInput', isoToDatetimeLocal(item.applicationDeadline));
            const weeks = item.teachingWeeks && Array.isArray(item.teachingWeeks.weeks) ? item.teachingWeeks.weeks.join(',') : '';
            setVal('editTeachingWeeksInput', weeks);
            setVal('editAssessmentEventsInput', formatAssessmentEventsForEdit(item.assessmentEvents));
            setVal('editCustomSkillsInput', formatCustomSkillsForEdit(item.requiredSkills));
            setVal('editCourseDescInput', item.courseDescription || '');
            setVal('editRecruitmentBriefInput', item.recruitmentBrief || '');
            if (editFixedSkillsInput) {
                const tags = item.requiredSkills && Array.isArray(item.requiredSkills.fixedTags) ? item.requiredSkills.fixedTags : [];
                const selected = new Set(tags.map(function (t) { return String(t).trim(); }).filter(Boolean));
                await loadSkillTagsInto(editFixedSkillsInput);
                Array.from(editFixedSkillsInput.options || []).forEach(function (opt) {
                    opt.selected = selected.has(opt.value);
                });
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

        function moOwnerLabel(item) {
            return (item && (item.ownerMoName || item.ownerMoId)) || 'MO';
        }

        function renderDetail(item) {
            currentDetailJob = item;
            if (courseEditPanel) courseEditPanel.style.display = 'none';
            if (courseEditStatus) courseEditStatus.textContent = '';
            const setText = function (id, text) {
                const el = document.getElementById(id);
                if (el) el.textContent = text || '--';
            };
            setText('jobDetailCode', getDisplayCode(item));
            setText('jobDetailName', item.courseName || '未命名岗位');
            setText('jobDetailMo', moOwnerLabel(item));
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
                    item.ownerMoName || '',
                    item.ownerMoId || '',
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
                    '<div class="course-card-topline"><span class="job-code">' + getDisplayCode(item) + '</span><span class="pill">' + moOwnerLabel(item) + '</span></div>' +
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
                const moId = getMoIdForApi();
                if (!moId) {
                    console.warn('[MO-JOBS] 缺少 moId，无法加载岗位列表');
                    state.jobs = [];
                    state.page = 1;
                    renderBoard();
                    return;
                }
                const res = await fetch(
                    apiUrl('/api/mo/jobs') + '?moId=' + encodeURIComponent(moId),
                    { headers: { Accept: 'application/json' } }
                );
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
            const sessionMoId = getMoIdForApi();
            if (!sessionMoId) {
                publishStatus.textContent = '未登录或缺少 moId，无法发布';
                return;
            }

            if (!courseNameInput || !courseCodeInput || !recruitmentStatusInput || !courseDescInput || fixedSkills.length === 0) {
                publishStatus.textContent = '请填写必填项：课程名、课程编号、招聘状态、至少一个技能标签、岗位描述';
                return;
            }

            const body = {
                moId: sessionMoId,
                courseCode: courseCodeInput,
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
                const res = await fetch(
                    apiUrl('/api/mo/jobs') + '?moId=' + encodeURIComponent(sessionMoId),
                    {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json;charset=UTF-8' },
                        body: JSON.stringify(body)
                    }
                );
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

        async function saveCourseEdit() {
            if (!currentDetailJob) {
                if (courseEditStatus) courseEditStatus.textContent = '未选择课程';
                return;
            }
            const sessionMoId = getMoIdForApi();
            if (!sessionMoId) {
                if (courseEditStatus) courseEditStatus.textContent = '未登录或缺少 moId';
                return;
            }
            const courseCode = String(currentDetailJob.courseCode || '').trim();
            const courseNameInput = document.getElementById('editCourseNameInput');
            const recruitmentStatusInput = document.getElementById('editRecruitmentStatusInput');
            const courseDescInput = document.getElementById('editCourseDescInput');
            const courseNameVal = courseNameInput ? courseNameInput.value.trim() : '';
            const recruitmentStatusVal = recruitmentStatusInput ? recruitmentStatusInput.value.trim() : '';
            const courseDescVal = courseDescInput ? courseDescInput.value.trim() : '';
            const fixedSkills = readSelectedValues('editFixedSkillsInput');
            if (!courseNameVal || !recruitmentStatusVal || !courseDescVal || fixedSkills.length === 0) {
                if (courseEditStatus) courseEditStatus.textContent = '请填写课程名、招聘状态、至少一个技能标签、岗位描述';
                return;
            }
            const teachingWeeks = (function () {
                const customWeeks = parseCsv(document.getElementById('editTeachingWeeksInput').value)
                    .map(function (v) { return Number(v); })
                    .filter(function (n) { return Number.isFinite(n) && n >= 1 && n <= 20; });
                const uniqWeeks = Array.from(new Set(customWeeks)).sort(function (a, b) { return a - b; });
                return { weeks: uniqWeeks };
            })();
            const semesterInput = document.getElementById('editSemesterInput');
            const studentCountRaw = document.getElementById('editStudentCountInput').value.trim();
            const taRecruitCountRaw = document.getElementById('editTaRecruitCountInput').value.trim();
            const campusRaw = document.getElementById('editCampusInput').value.trim();
            const applicationDeadlineRaw = document.getElementById('editApplicationDeadlineInput').value.trim();
            const recruitmentBriefRaw = document.getElementById('editRecruitmentBriefInput').value.trim();

            const body = {
                moId: sessionMoId,
                courseCode: courseCode,
                courseName: courseNameVal,
                recruitmentStatus: recruitmentStatusVal,
                status: recruitmentStatusVal,
                assessmentEvents: parseAssessmentEvents(document.getElementById('editAssessmentEventsInput').value),
                requiredSkills: {
                    fixedTags: fixedSkills,
                    customSkills: parseCustomSkills(document.getElementById('editCustomSkillsInput').value)
                },
                courseDescription: courseDescVal,
                source: 'mo-manual-v2-edit'
            };
            body.semester = semesterInput ? semesterInput.value.trim() : '';
            body.teachingWeeks = teachingWeeks;
            body.studentCount = studentCountRaw === '' ? -1 : (Number(studentCountRaw) || 0);
            body.campus = campusRaw;
            body.applicationDeadline = applicationDeadlineRaw
                ? new Date(applicationDeadlineRaw).toISOString()
                : '';
            if (recruitmentBriefRaw) body.recruitmentBrief = recruitmentBriefRaw;
            if (taRecruitCountRaw !== '') {
                body.taRecruitCount = Number(taRecruitCountRaw) || 0;
            } else {
                body.taRecruitCount = null;
            }

            if (courseEditStatus) courseEditStatus.textContent = '保存中...';
            try {
                const res = await fetch(
                    apiUrl('/api/mo/jobs') + '?moId=' + encodeURIComponent(sessionMoId),
                    {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json;charset=UTF-8' },
                        body: JSON.stringify(body)
                    }
                );
                const payload = await res.json();
                if (!res.ok || payload.success === false) {
                    if (courseEditStatus) courseEditStatus.textContent = payload.message || '保存失败';
                    return;
                }
                if (courseEditStatus) courseEditStatus.textContent = '已保存';
                if (payload.item) {
                    currentDetailJob = payload.item;
                    renderDetail(currentDetailJob);
                }
                await loadJobs();
            } catch (err) {
                if (courseEditStatus) courseEditStatus.textContent = (err && err.message) ? err.message : '保存失败';
            }
        }

        app.getJobs = function () { return state.jobs.slice(); };
        app.loadJobs = loadJobs;

        if (toggleCourseEditBtn && courseEditPanel) {
            toggleCourseEditBtn.addEventListener('click', function () {
                const opening = courseEditPanel.style.display === 'none' || courseEditPanel.style.display === '';
                courseEditPanel.style.display = opening ? 'block' : 'none';
                if (opening) populateCourseEditForm(currentDetailJob);
                if (courseEditStatus) courseEditStatus.textContent = '';
            });
        }
        if (saveCourseEditBtn) {
            saveCourseEditBtn.addEventListener('click', function () { saveCourseEdit(); });
        }

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
