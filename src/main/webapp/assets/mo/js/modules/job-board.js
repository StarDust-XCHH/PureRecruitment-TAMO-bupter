(function () {
    'use strict';

    const moApp = window.MOApp = window.MOApp || {};
    const modules = moApp.modules = moApp.modules || {};

    var FALLBACK_SKILL_TAGS = [
        'Python', 'Java', 'C/C++', 'JavaScript', 'TypeScript', 'SQL', 'Linux', 'Git',
        'Data Structures', 'Algorithms', 'Machine Learning', 'Computer Networks',
        'Operating Systems', 'Database', 'Software Engineering', 'Web Development'
    ];

    modules.jobBoard = function initJobBoard(app) {
        function t(zh, en) {
            return typeof app.t === 'function' ? app.t(zh, en) : zh;
        }

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
            pageSize: 4,
            filteredJobs: [],
            hiredByJobId: {}
        };

        /** 申请截止：与 datetime-local 一致，按本机本地墙钟解析，提交为 UTC ISO */
        function padDeadline2(n) {
            return n < 10 ? '0' + n : String(n);
        }

        function parseDatetimeLocalToUtcIso(value) {
            const s = value != null ? String(value).trim() : '';
            if (!s) return null;
            const m = s.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/);
            if (!m) return null;
            const y = parseInt(m[1], 10);
            const mo = parseInt(m[2], 10);
            const d = parseInt(m[3], 10);
            const h = parseInt(m[4], 10);
            const mi = parseInt(m[5], 10);
            const sec = m[6] != null ? parseInt(m[6], 10) : 0;
            const dt = new Date(y, mo - 1, d, h, mi, sec, 0);
            if (Number.isNaN(dt.getTime())) return null;
            return dt.toISOString();
        }

        function readLocalApplicationDeadline(inputEl) {
            if (!inputEl) return { ok: true, iso: null };
            const raw = inputEl.value != null ? String(inputEl.value).trim() : '';
            if (!raw) return { ok: true, iso: null };
            const iso = parseDatetimeLocalToUtcIso(raw);
            if (!iso) return { ok: false, msg: t('申请截止时间无效，请检查日期与时间', 'Invalid deadline; check date and time') };
            return { ok: true, iso: iso };
        }

        function applyIsoToDatetimeLocalInput(iso, inputEl) {
            if (!inputEl) return;
            if (!iso) {
                inputEl.value = '';
                return;
            }
            const dt = new Date(iso);
            if (Number.isNaN(dt.getTime())) {
                inputEl.value = '';
                return;
            }
            inputEl.value = dt.getFullYear() + '-' + padDeadline2(dt.getMonth() + 1) + '-' + padDeadline2(dt.getDate())
                + 'T' + padDeadline2(dt.getHours()) + ':' + padDeadline2(dt.getMinutes());
        }

        var skillTagList = FALLBACK_SKILL_TAGS.slice();
        var publishAssessments = [];
        var publishCustomSkills = [];
        var publishFixedTags = new Set();
        var publishTeachingWeeks = [];
        var editAssessments = [];
        var editCustomSkills = [];
        var editFixedTags = new Set();
        var editTeachingWeeks = [];
        var weekPickerTarget = 'publish';
        var skillPickerTarget = 'publish';
        var skillPickerWorkingSet = new Set();
        /** 技能弹窗内「Other」待确认的其他技能（与岗位 customSkills 对应，不写入系统标签库） */
        var skillPickerPendingCustom = [];
        /** 从技能选择弹窗内点 Other，确认后写入 skillPickerPendingCustom */
        var addingOtherForFixedPicker = false;
        /** 自定义技能弹窗：null 为新增；非负整数为 publishCustomSkills / editCustomSkills 中要修改的下标 */
        var editingCustomSkillIndex = null;
        var compositeTarget = 'publish';

        const jobBoard = document.getElementById('jobBoard');
        const jobPagination = document.getElementById('jobPagination');
        const routeJobsEl = document.getElementById('route-jobs');

        function scrollJobsPanelToTop() {
            if (routeJobsEl && typeof routeJobsEl.scrollIntoView === 'function') {
                routeJobsEl.scrollIntoView({ block: 'start', behavior: 'smooth' });
            }
        }
        const jobSearchInput = document.getElementById('jobSearchInput');
        const jobSemesterFilter = document.getElementById('jobSemesterFilter');
        const jobCampusFilter = document.getElementById('jobCampusFilter');
        const jobStatusFilter = document.getElementById('jobStatusFilter');
        const jobFillFilter = document.getElementById('jobFillFilter');
        const clearJobFiltersBtn = document.getElementById('clearJobFiltersBtn');
        const openCoursesCount = document.getElementById('openCoursesCount');
        const refreshJobsBtn = document.getElementById('refreshJobsBtn');
        const openJobPublishModalBtn = document.getElementById('openJobPublishModalBtn');
        const publishForm = document.getElementById('jobPublishForm');
        const publishStatus = document.getElementById('publishJobStatus');
        const moFixedSkillsPickerFlow = document.getElementById('moFixedSkillsPickerFlow'); // legacy modal flow (kept for compatibility)
        const publishFixedSkillsInlineFlow = document.getElementById('publishFixedSkillsInlineFlow');
        const editFixedSkillsInlineFlow = document.getElementById('editFixedSkillsInlineFlow');
        const publishTeachingWeeksSummary = document.getElementById('publishTeachingWeeksSummary');
        const editTeachingWeeksSummary = document.getElementById('editTeachingWeeksSummary');
        const publishFixedSkillsSummary = document.getElementById('publishFixedSkillsSummary');
        const editFixedSkillsSummary = document.getElementById('editFixedSkillsSummary');

        const moTeachingWeeksPickerGrid = document.getElementById('moTeachingWeeksPickerGrid');
        const teachingWeeksModal = document.getElementById('moTeachingWeeksModal');
        const fixedSkillsPickerModal = document.getElementById('moFixedSkillsPickerModal'); // legacy modal (no longer used)
        const moAssessmentWeeksGrid = document.getElementById('moAssessmentWeeksGrid');

        const publishAssessmentList = document.getElementById('publishAssessmentList');
        const publishAssessmentEmpty = document.getElementById('publishAssessmentEmpty');
        const publishCustomSkillList = document.getElementById('publishCustomSkillList');
        const publishCustomSkillEmpty = document.getElementById('publishCustomSkillEmpty');
        const editAssessmentList = document.getElementById('editAssessmentList');
        const editAssessmentEmpty = document.getElementById('editAssessmentEmpty');
        const editCustomSkillList = document.getElementById('editCustomSkillList');
        const editCustomSkillEmpty = document.getElementById('editCustomSkillEmpty');

        const courseEditForm = document.getElementById('courseEditForm');
        const toggleCourseEditBtn = document.getElementById('toggleCourseEditBtn');
        const courseEditStatus = document.getElementById('courseEditStatus');

        const assessmentModal = document.getElementById('moCompositeAssessmentModal');
        const customSkillModal = document.getElementById('moCompositeCustomSkillModal');

        let currentDetailJob = null;

        function clearPublishSkillsInlineError() {
            var el = document.getElementById('publishSkillsFieldError');
            if (el) {
                el.textContent = '';
                el.hidden = true;
            }
        }

        function clearEditSkillsInlineError() {
            var el = document.getElementById('editSkillsFieldError');
            if (el) {
                el.textContent = '';
                el.hidden = true;
            }
        }

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

        function buildSemesterFromPickers(yearEl, termEl) {
            const y = yearEl && yearEl.value ? String(yearEl.value).trim() : '';
            const t = termEl && termEl.value ? String(termEl.value).trim() : '';
            if (!y || !t) return '';
            return y + '-' + t;
        }

        /** 日历「当年」，用于学期年份锚点与默认选中 */
        function getDefaultSemesterYearAnchor() {
            return new Date().getFullYear();
        }

        /**
         * 填充年份下拉：锚点年前 1 年～后 3 年；无有效 preferredYear 时默认选中当年。
         * preferredYear 为有效数字时选中该年（若不在范围内则追加该年并排序）。
         */
        function populateSemesterYearSelect(selectEl, preferredYear) {
            if (!selectEl) return;
            const anchor = getDefaultSemesterYearAnchor();
            const years = [];
            for (let i = anchor - 1; i <= anchor + 3; i += 1) {
                years.push(i);
            }
            let pref = null;
            if (preferredYear != null && preferredYear !== '') {
                const n = Number(preferredYear);
                if (Number.isFinite(n)) {
                    pref = n;
                    if (years.indexOf(pref) === -1) {
                        years.push(pref);
                        years.sort(function (a, b) { return a - b; });
                    }
                }
            }
            selectEl.innerHTML = '';
            years.forEach(function (y) {
                const opt = document.createElement('option');
                opt.value = String(y);
                opt.textContent = String(y);
                selectEl.appendChild(opt);
            });
            const pick = pref != null && years.indexOf(pref) >= 0 ? pref : anchor;
            selectEl.value = String(pick);
        }

        function applySemesterToPickers(semesterStr, yearEl, termEl) {
            const s = String(semesterStr || '').trim();
            const m = s.match(/^(\d{4})-(Spring|Fall)$/i);
            if (!m) {
                populateSemesterYearSelect(yearEl, null);
                if (termEl) termEl.value = '';
                return;
            }
            let term = m[2].charAt(0).toUpperCase() + m[2].slice(1).toLowerCase();
            if (term !== 'Spring' && term !== 'Fall') {
                term = '';
            }
            const yNum = Number(m[1]);
            populateSemesterYearSelect(yearEl, yNum);
            if (yearEl) yearEl.value = String(yNum);
            if (termEl) termEl.value = term;
        }

        function buildWeekGrid(container, inputName, idPrefix, compact) {
            if (!container) return;
            container.innerHTML = '';
            for (let w = 1; w <= 20; w += 1) {
                const id = idPrefix + '-' + w;
                const label = document.createElement('label');
                label.className = 'mo-week-chip';
                const cb = document.createElement('input');
                cb.type = 'checkbox';
                cb.name = inputName;
                cb.value = String(w);
                cb.id = id;
                label.appendChild(cb);
                const span = document.createElement('span');
                span.textContent = compact ? String(w) : t('第' + w + '周', 'Week ' + w);
                label.appendChild(span);
                container.appendChild(label);
            }
        }

        function getWeeksFromGrid(container) {
            if (!container) return [];
            return Array.from(container.querySelectorAll('input[type="checkbox"]:checked'))
                .map(function (c) { return Number(c.value); })
                .filter(function (n) { return Number.isFinite(n) && n >= 1 && n <= 20; })
                .sort(function (a, b) { return a - b; });
        }

        function syncMoWeekLabelCheckedState(container) {
            if (!container) return;
            container.querySelectorAll('label').forEach(function (label) {
                const input = label.querySelector('input[type="checkbox"]');
                if (input) label.classList.toggle('checked', input.checked);
            });
        }

        function setWeeksOnGrid(container, weeks) {
            if (!container) return;
            const set = new Set((weeks || []).map(function (w) { return Number(w); }));
            container.querySelectorAll('input[type="checkbox"]').forEach(function (c) {
                c.checked = set.has(Number(c.value));
            });
            syncMoWeekLabelCheckedState(container);
        }

        function clearTeachingWeeksPresetHighlight() {
            document.querySelectorAll('#moTeachingWeeksModal .mo-week-preset').forEach(function (b) {
                b.classList.remove('is-active');
            });
        }

        function clearWeekGrid(container) {
            setWeeksOnGrid(container, []);
        }

        function formatWeeksSummaryLine(weeks) {
            const arr = (weeks || []).slice().sort(function (a, b) { return a - b; });
            if (!arr.length) return '';
            return t('已选第 ' + arr.join('、') + ' 周', 'Weeks ' + arr.join(', '));
        }

        function updatePublishTeachingWeeksSummary() {
            if (!publishTeachingWeeksSummary) return;
            publishTeachingWeeksSummary.textContent = formatWeeksSummaryLine(publishTeachingWeeks)
                ? formatWeeksSummaryLine(publishTeachingWeeks)
                : t('尚未选择授课周次（选填）。', 'No teaching weeks selected yet (optional).');
        }

        function updateEditTeachingWeeksSummary() {
            if (!editTeachingWeeksSummary) return;
            editTeachingWeeksSummary.textContent = formatWeeksSummaryLine(editTeachingWeeks)
                ? formatWeeksSummaryLine(editTeachingWeeks)
                : t('未选择授课周次。', 'No teaching weeks selected.');
        }

        function syncFixedSkillsPrimaryButtonLabels() {
            const pubBtn = document.getElementById('openPublishFixedSkillsBtn');
            const edBtn = document.getElementById('openEditFixedSkillsBtn');
            const pubHas = publishFixedTags.size > 0 || publishCustomSkills.length > 0;
            const edHas = editFixedTags.size > 0 || editCustomSkills.length > 0;
            if (pubBtn) pubBtn.textContent = pubHas ? t('修改技能标签', 'Edit skill tags') : t('＋ 添加技能标签', '+ Add skill tags');
            if (edBtn) edBtn.textContent = edHas ? t('修改技能标签', 'Edit skill tags') : t('＋ 添加技能标签', '+ Add skill tags');
        }

        function updatePublishFixedSkillsSummary() {
            const arr = getFixedTagsArrayFromSet(publishFixedTags);
            const customN = publishCustomSkills.length;
            const parts = [];
            if (arr.length) {
                parts.push(t('已选标签 ' + arr.length + ' 项：', 'Selected ' + arr.length + ' tag(s): ') + arr.join(t('、', ', ')));
            }
            if (customN) {
                parts.push(t('其他技能 ' + customN + ' 项（详见下方列表）', customN + ' custom skill(s) (see list below)'));
            }
            if (publishFixedSkillsSummary) {
                publishFixedSkillsSummary.textContent = parts.length
                    ? parts.join(t('；', '; '))
                    : t('尚未选择技能，可点击上方「＋ 添加技能标签」按钮。', 'No skills selected yet. Use “+ Add skill tags” above.');
            }
            if (arr.length > 0) clearPublishSkillsInlineError();
            syncFixedSkillsPrimaryButtonLabels();
        }

        function updateEditFixedSkillsSummary() {
            const arr = getFixedTagsArrayFromSet(editFixedTags);
            const customN = editCustomSkills.length;
            const parts = [];
            if (arr.length) {
                parts.push(t('已选标签 ' + arr.length + ' 项：', 'Selected ' + arr.length + ' tag(s): ') + arr.join(t('、', ', ')));
            }
            if (customN) {
                parts.push(t('其他技能 ' + customN + ' 项（详见下方列表）', customN + ' custom skill(s) (see list below)'));
            }
            if (editFixedSkillsSummary) {
                editFixedSkillsSummary.textContent = parts.length
                    ? parts.join(t('；', '; '))
                    : t('尚未选择所需技能，可点击上方「＋ 添加技能标签」按钮。', 'No required skills selected yet. Use “+ Add skill tags” above.');
            }
            if (arr.length > 0) clearEditSkillsInlineError();
            syncFixedSkillsPrimaryButtonLabels();
        }

        function getInlineSkillState(target) {
            const isEdit = target === 'edit';
            return {
                fixedSet: isEdit ? editFixedTags : publishFixedTags,
                customArr: isEdit ? editCustomSkills : publishCustomSkills,
                container: isEdit ? editFixedSkillsInlineFlow : publishFixedSkillsInlineFlow
            };
        }

        function removeCustomSkillByName(target, name) {
            const s = getInlineSkillState(target);
            const key = String(name || '').trim().toLowerCase();
            if (!key) return;
            const next = (s.customArr || []).filter(function (r) {
                const n = r && r.name != null ? String(r.name).trim().toLowerCase() : '';
                return n && n !== key;
            });
            if (target === 'edit') editCustomSkills = next;
            else publishCustomSkills = next;
            if (target === 'edit') {
                renderCompositeList(editCustomSkillList, editCustomSkills, editCustomSkillEmpty, 'custom', 'edit');
                updateEditFixedSkillsSummary();
            } else {
                renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
                updatePublishFixedSkillsSummary();
            }
            renderInlineFixedSkillsPicker(target);
        }

        function renderFixedSkillsPickerFlowInto(container, target) {
            if (!container) return;
            const s = getInlineSkillState(target);
            container.innerHTML = '';

            (skillTagList || []).forEach(function (tag) {
                const tagText = String(tag || '').trim();
                if (!tagText) return;
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'mo-skill-chip' + (s.fixedSet.has(tagText) ? ' is-selected' : '');
                btn.textContent = tagText;
                btn.setAttribute('aria-pressed', s.fixedSet.has(tagText) ? 'true' : 'false');
                btn.addEventListener('click', function () {
                    if (s.fixedSet.has(tagText)) s.fixedSet.delete(tagText);
                    else s.fixedSet.add(tagText);
                    if (target === 'edit') updateEditFixedSkillsSummary();
                    else updatePublishFixedSkillsSummary();
                    renderInlineFixedSkillsPicker(target);
                });
                container.appendChild(btn);
            });

            // custom pills keep the original order: right after fixed tags
            (s.customArr || []).forEach(function (row) {
                const name = row && row.name != null ? String(row.name).trim() : '';
                if (!name) return;
                const pill = document.createElement('button');
                pill.type = 'button';
                pill.className = 'mo-skill-chip mo-skill-chip--custom-pill';
                const label = document.createElement('span');
                label.className = 'mo-skill-chip__label';
                label.textContent = name;
                pill.appendChild(label);
                const desc = row && row.description != null ? String(row.description).trim() : '';
                const tip = desc ? (name + ' — ' + desc) : name;
                pill.setAttribute('title', tip);
                pill.setAttribute('aria-label', t('点击移除「', 'Remove “') + name + t('」', '”'));
                pill.addEventListener('click', function () {
                    removeCustomSkillByName(target, name);
                });
                container.appendChild(pill);
            });

            const otherBtn = document.createElement('button');
            otherBtn.type = 'button';
            otherBtn.className = 'mo-skill-chip mo-skill-chip--other';
            otherBtn.textContent = 'Other';
            otherBtn.setAttribute('aria-label', t('添加其他技能', 'Add other skill'));
            otherBtn.addEventListener('click', function () {
                skillPickerTarget = target || 'publish';
                openOtherSkillSubModal();
            });
            container.appendChild(otherBtn);
        }

        function renderInlineFixedSkillsPicker(target) {
            const t0 = target === 'edit' ? 'edit' : 'publish';
            const s = getInlineSkillState(t0);
            if (s.container) {
                renderFixedSkillsPickerFlowInto(s.container, t0);
            }
            // keep legacy modal flow updated if present (but modal is no longer used)
            if (moFixedSkillsPickerFlow) {
                skillPickerTarget = t0;
                skillPickerWorkingSet = new Set(s.fixedSet);
                skillPickerPendingCustom = (s.customArr || []).map(function (x) {
                    return { name: String(x.name || '').trim(), description: String(x.description || '').trim() };
                }).filter(function (x) { return x.name; });
                renderFixedSkillsPickerFlowInto(moFixedSkillsPickerFlow, t0);
            }
        }

        function getFixedTagsArrayFromSet(set) {
            return Array.from(set || []).map(function (s) { return String(s || '').trim(); }).filter(Boolean);
        }

        async function loadSkillTagsFromApi() {
            try {
                const res = await fetch(apiUrl('/api/mo/skill-tags'), { headers: { Accept: 'application/json' } });
                if (!res.ok) return;
                const payload = await res.json();
                const items = Array.isArray(payload.items) ? payload.items : [];
                if (items.length) {
                    skillTagList = items.map(function (x) { return String(x || '').trim(); }).filter(Boolean);
                } else {
                    skillTagList = FALLBACK_SKILL_TAGS.slice();
                }
            } catch (err) {
                console.warn('[MO-JOBS] load skill tags failed', err);
                skillTagList = FALLBACK_SKILL_TAGS.slice();
            }
        }

        function weekText(teachingWeeks) {
            const weeks = teachingWeeks && Array.isArray(teachingWeeks.weeks) ? teachingWeeks.weeks : [];
            if (!weeks.length) return '--';
            return weeks.join(',');
        }

        function getDisplayCode(item) {
            return item.courseCode || item.jobId || '--';
        }

        function getDisplayLocation(item) {
            return item && item.campus ? item.campus : '--';
        }

        function getCampusDisplayForCard(item) {
            var raw = String(getDisplayLocation(item) || '').trim().toLowerCase();
            if (raw === 'shahe') return 'Shahe Campus';
            if (raw === 'main') return 'Main Campus';
            return getDisplayLocation(item);
        }

        function getJobId(item) {
            return item && item.jobId != null ? String(item.jobId).trim() : '';
        }

        function getJobCapacity(item) {
            return item && item.taRecruitCount != null && Number(item.taRecruitCount) >= 0
                ? Number(item.taRecruitCount)
                : 0;
        }

        function getJobStatusNormalized(item) {
            var st = String((item && (item.recruitmentStatus || item.status)) || 'OPEN').trim().toUpperCase();
            return st === 'CLOSED' ? 'CLOSED' : 'OPEN';
        }

        function getHiredCountForJob(item) {
            var jobId = getJobId(item);
            if (!jobId) return 0;
            return Number(state.hiredByJobId[jobId] || 0);
        }

        function isJobFilled(item) {
            var cap = getJobCapacity(item);
            if (cap <= 0) return false;
            return getHiredCountForJob(item) >= cap;
        }

        function semesterSortValue(item) {
            var sem = String((item && item.semester) || '').trim();
            var m = sem.match(/^(\d{4})-(Spring|Fall)$/i);
            if (!m) return -1;
            var year = Number(m[1]) || 0;
            var term = String(m[2] || '').toLowerCase() === 'fall' ? 2 : 1;
            return year * 10 + term;
        }

        function courseCodeForSort(item) {
            var code = String((item && item.courseCode) || '').trim();
            if (code) return code;
            return String((item && item.jobId) || '').trim();
        }

        function compareJobsDefaultOrder(a, b) {
            var statusRankA = getJobStatusNormalized(a) === 'OPEN' ? 0 : 1;
            var statusRankB = getJobStatusNormalized(b) === 'OPEN' ? 0 : 1;
            if (statusRankA !== statusRankB) return statusRankA - statusRankB;

            var semA = semesterSortValue(a);
            var semB = semesterSortValue(b);
            if (semA !== semB) return semB - semA;

            var fillRankA = isJobFilled(a) ? 1 : 0; // 未满在前
            var fillRankB = isJobFilled(b) ? 1 : 0;
            if (fillRankA !== fillRankB) return fillRankA - fillRankB;

            return courseCodeForSort(a).localeCompare(courseCodeForSort(b), undefined, {
                numeric: true,
                sensitivity: 'base'
            });
        }

        function setHiredCountsFromApplicants(items) {
            var m = {};
            (items || []).forEach(function (x) {
                if (!x || x.status !== '已录用') return;
                var jid = x.jobId != null ? String(x.jobId).trim() : '';
                if (!jid) return;
                m[jid] = (m[jid] || 0) + 1;
            });
            state.hiredByJobId = m;
        }

        async function loadApplicantsForHiringSummary() {
            var moId = getMoIdForApi();
            if (!moId) {
                state.hiredByJobId = {};
                return;
            }
            try {
                var res = await fetch(
                    apiUrl('/api/mo/applicants') + '?moId=' + encodeURIComponent(moId),
                    { headers: { Accept: 'application/json' } }
                );
                var payload = await res.json();
                if (!res.ok || payload.success === false) {
                    state.hiredByJobId = {};
                    return;
                }
                setHiredCountsFromApplicants(Array.isArray(payload.items) ? payload.items : []);
            } catch (e) {
                state.hiredByJobId = {};
            }
        }

        function updateJobsFilterResetButton() {
            if (!clearJobFiltersBtn) return;
            var active = false;
            if (jobSemesterFilter && (jobSemesterFilter.value || '').trim()) active = true;
            if (jobCampusFilter && (jobCampusFilter.value || '').trim()) active = true;
            if (jobStatusFilter && (jobStatusFilter.value || '').trim()) active = true;
            if (jobFillFilter && (jobFillFilter.value || '').trim()) active = true;
            clearJobFiltersBtn.disabled = !active;
        }

        function renderJobsFilterOptions() {
            if (!jobSemesterFilter || !jobCampusFilter) return;
            var prevSem = (jobSemesterFilter.value || '').trim();
            var prevCampus = (jobCampusFilter.value || '').trim();
            var semSet = new Set();
            var campusSet = new Set();
            (state.jobs || []).forEach(function (item) {
                var sem = item && item.semester != null ? String(item.semester).trim() : '';
                var campus = item && item.campus != null ? String(item.campus).trim() : '';
                if (sem) semSet.add(sem);
                if (campus) campusSet.add(campus);
            });
            var semesters = Array.from(semSet).sort();
            var campuses = Array.from(campusSet).sort();

            jobSemesterFilter.innerHTML =
                '<option value="">' + t('全部学期', 'All semesters') + '</option>' +
                semesters.map(function (s) {
                    return '<option value="' + s + '">' + s + '</option>';
                }).join('');
            jobCampusFilter.innerHTML =
                '<option value="">' + t('全部校区', 'All campuses') + '</option>' +
                campuses.map(function (s) {
                    return '<option value="' + s + '">' + s + '</option>';
                }).join('');

            if (prevSem && semesters.indexOf(prevSem) >= 0) jobSemesterFilter.value = prevSem;
            if (prevCampus && campuses.indexOf(prevCampus) >= 0) jobCampusFilter.value = prevCampus;
            updateJobsFilterResetButton();
        }

        /** 岗位技能标签：系统固定标签 + Other 自定义（requiredSkills.customSkills[].name），去重 */
        function getJobSkillTagsList(item) {
            const out = [];
            const seen = new Set();
            const rs = item && item.requiredSkills;
            if (!rs) return out;
            if (Array.isArray(rs.fixedTags)) {
                rs.fixedTags.forEach(function (t) {
                    const s = String(t == null ? '' : t).trim();
                    if (!s) return;
                    const k = s.toLowerCase();
                    if (seen.has(k)) return;
                    seen.add(k);
                    out.push(s);
                });
            }
            if (Array.isArray(rs.customSkills)) {
                rs.customSkills.forEach(function (c) {
                    const s = c && c.name != null ? String(c.name).trim() : '';
                    if (!s) return;
                    const k = s.toLowerCase();
                    if (seen.has(k)) return;
                    seen.add(k);
                    out.push(s);
                });
            }
            return out;
        }

        /**
         * 课程详情侧栏：按顺序输出固定标签与自定义标签（含说明，用于悬停提示）
         */
        function getJobDetailSkillTagEntries(item) {
            const out = [];
            const seen = new Set();
            const rs = item && item.requiredSkills;
            if (!rs) return out;
            if (Array.isArray(rs.fixedTags)) {
                rs.fixedTags.forEach(function (t) {
                    const s = String(t == null ? '' : t).trim();
                    if (!s) return;
                    const k = s.toLowerCase();
                    if (seen.has(k)) return;
                    seen.add(k);
                    out.push({ type: 'fixed', label: s });
                });
            }
            if (Array.isArray(rs.customSkills)) {
                rs.customSkills.forEach(function (c) {
                    const name = c && c.name != null ? String(c.name).trim() : '';
                    if (!name) return;
                    const k = name.toLowerCase();
                    if (seen.has(k)) return;
                    seen.add(k);
                    const desc = c && c.description != null ? String(c.description).trim() : '';
                    out.push({ type: 'custom', label: name, description: desc });
                });
            }
            return out;
        }

        /**
         * @param {number} [maxCount] 传入正数时截断（课程列表卡片）；详情页不传则展示全部
         */
        function toTags(item, maxCount) {
            const list = getJobSkillTagsList(item);
            if (typeof maxCount === 'number' && maxCount > 0) {
                return list.slice(0, maxCount);
            }
            return list;
        }

        var JOB_CARD_TAGS_MAX = 3;

        /** MO 课程列表卡片：最多展示若干标签，未展示完时在末尾显示「等，共x个」（x 为标签总数） */
        function buildCourseCardTagsHtml(item) {
            var all = getJobSkillTagsList(item);
            var shown = all.slice(0, JOB_CARD_TAGS_MAX);
            var html = shown.map(function (tag) {
                return '<span class="pill">' + tag + '</span>';
            }).join('');
            if (all.length > JOB_CARD_TAGS_MAX) {
                html += '<span class="job-tags-more muted">' + t('等，共' + all.length + '个', all.length + ' tags total') + '</span>';
            }
            return html;
        }

        function renderCompositeList(ul, arr, emptyEl, type, target) {
            if (!ul) return;
            ul.innerHTML = '';
            (arr || []).forEach(function (entry, index) {
                const li = document.createElement('li');
                li.className = 'mo-composite-item';
                const main = document.createElement('div');
                main.className = 'mo-composite-item__main';
                if (type === 'assessment') {
                    const title = document.createElement('div');
                    title.className = 'mo-composite-item__title';
                    title.textContent = entry.name || t('（未命名）', '(Untitled)');
                    main.appendChild(title);
                    const meta = document.createElement('div');
                    meta.className = 'mo-composite-item__meta';
                    meta.textContent = (entry.weeks && entry.weeks.length)
                        ? (t('周次：', 'Weeks: ') + entry.weeks.join(', '))
                        : (t('周次：', 'Weeks: ') + '—');
                    main.appendChild(meta);
                    if (entry.description) {
                        const desc = document.createElement('div');
                        desc.className = 'mo-composite-item__desc';
                        desc.textContent = entry.description;
                        main.appendChild(desc);
                    }
                } else {
                    const title = document.createElement('div');
                    title.className = 'mo-composite-item__title';
                    title.textContent = entry.name || t('（未命名）', '(Untitled)');
                    main.appendChild(title);
                    if (entry.description) {
                        const desc = document.createElement('div');
                        desc.className = 'mo-composite-item__desc';
                        desc.textContent = entry.description;
                        main.appendChild(desc);
                    }
                }
                const rm = document.createElement('button');
                rm.type = 'button';
                rm.className = 'mo-composite-remove';
                rm.textContent = t('移除', 'Remove');
                rm.addEventListener('click', function () {
                    if (target === 'publish') {
                        if (type === 'assessment') publishAssessments.splice(index, 1);
                        else publishCustomSkills.splice(index, 1);
                        renderCompositeList(publishAssessmentList, publishAssessments, publishAssessmentEmpty, 'assessment', 'publish');
                        renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
                        if (type === 'custom') updatePublishFixedSkillsSummary();
                    } else {
                        if (type === 'assessment') editAssessments.splice(index, 1);
                        else editCustomSkills.splice(index, 1);
                        renderCompositeList(editAssessmentList, editAssessments, editAssessmentEmpty, 'assessment', 'edit');
                        renderCompositeList(editCustomSkillList, editCustomSkills, editCustomSkillEmpty, 'custom', 'edit');
                        if (type === 'custom') updateEditFixedSkillsSummary();
                    }
                });
                li.appendChild(main);
                if (type === 'custom') {
                    const actions = document.createElement('div');
                    actions.className = 'mo-composite-item__actions';
                    const editBtn = document.createElement('button');
                    editBtn.type = 'button';
                    editBtn.className = 'mo-composite-edit';
                    editBtn.textContent = t('修改', 'Edit');
                    editBtn.addEventListener('click', function (e) {
                        e.stopPropagation();
                        openCompositeCustomSkillEdit(target, index);
                    });
                    actions.appendChild(editBtn);
                    actions.appendChild(rm);
                    li.appendChild(actions);
                } else {
                    li.appendChild(rm);
                }
                ul.appendChild(li);
            });
            if (emptyEl) {
                const hide = (arr || []).length > 0;
                emptyEl.hidden = hide;
                emptyEl.setAttribute('aria-hidden', hide ? 'true' : 'false');
            }
        }

        function openModal(el) {
            if (!el) return;
            el.hidden = false;
            el.setAttribute('aria-hidden', 'false');
        }

        function closeModal(el) {
            if (!el) return;
            if (el === customSkillModal) {
                addingOtherForFixedPicker = false;
                editingCustomSkillIndex = null;
                el.classList.remove('mo-publish-modal--stack-top');
                const titleEl = document.getElementById('moCompositeCustomSkillTitle');
                if (titleEl) titleEl.textContent = t('添加自定义技能', 'Add custom skill');
                const cOk = document.getElementById('moCompositeCustomSkillConfirmBtn');
                if (cOk) cOk.textContent = t('加入列表', 'Add to list');
            }
            el.hidden = true;
            el.setAttribute('aria-hidden', 'true');
        }

        function openAssessmentModal(target) {
            compositeTarget = target || 'publish';
            const nameEl = document.getElementById('moAssessmentNameInput');
            const descEl = document.getElementById('moAssessmentDescInput');
            if (nameEl) nameEl.value = '';
            if (descEl) descEl.value = '';
            clearWeekGrid(moAssessmentWeeksGrid);
            openModal(assessmentModal);
            if (nameEl) nameEl.focus();
        }

        function openOtherSkillSubModal() {
            addingOtherForFixedPicker = true;
            editingCustomSkillIndex = null;
            compositeTarget = skillPickerTarget || compositeTarget || 'publish';
            const nameEl = document.getElementById('moCustomSkillNameInput');
            const descEl = document.getElementById('moCustomSkillDescInput');
            if (nameEl) nameEl.value = '';
            if (descEl) descEl.value = '';
            const titleEl = document.getElementById('moCompositeCustomSkillTitle');
            if (titleEl) titleEl.textContent = t('其他技能', 'Other skill');
            const cOk = document.getElementById('moCompositeCustomSkillConfirmBtn');
            if (cOk) cOk.textContent = t('加入列表', 'Add to list');
            if (customSkillModal) customSkillModal.classList.add('mo-publish-modal--stack-top');
            openModal(customSkillModal);
            if (nameEl) nameEl.focus();
        }

        function openCompositeCustomSkillEdit(target, index) {
            addingOtherForFixedPicker = false;
            compositeTarget = target || 'publish';
            editingCustomSkillIndex = typeof index === 'number' ? index : null;
            const arr = compositeTarget === 'edit' ? editCustomSkills : publishCustomSkills;
            const entry = (arr && arr[editingCustomSkillIndex]) ? arr[editingCustomSkillIndex] : {};
            const nameEl = document.getElementById('moCustomSkillNameInput');
            const descEl = document.getElementById('moCustomSkillDescInput');
            const titleEl = document.getElementById('moCompositeCustomSkillTitle');
            const cOk = document.getElementById('moCompositeCustomSkillConfirmBtn');
            if (nameEl) nameEl.value = entry.name || '';
            if (descEl) descEl.value = entry.description || '';
            if (titleEl) titleEl.textContent = t('修改自定义技能', 'Edit custom skill');
            if (cOk) cOk.textContent = t('保存', 'Save');
            if (customSkillModal) customSkillModal.classList.remove('mo-publish-modal--stack-top');
            openModal(customSkillModal);
            if (nameEl) nameEl.focus();
        }

        function confirmAssessmentModal() {
            const name = (document.getElementById('moAssessmentNameInput').value || '').trim();
            if (!name) {
                return;
            }
            const weeks = getWeeksFromGrid(moAssessmentWeeksGrid);
            const description = (document.getElementById('moAssessmentDescInput').value || '').trim();
            const row = { name: name, weeks: weeks, description: description };
            if (compositeTarget === 'edit') {
                editAssessments.push(row);
                renderCompositeList(editAssessmentList, editAssessments, editAssessmentEmpty, 'assessment', 'edit');
            } else {
                publishAssessments.push(row);
                renderCompositeList(publishAssessmentList, publishAssessments, publishAssessmentEmpty, 'assessment', 'publish');
            }
            closeModal(assessmentModal);
        }

        function confirmCustomSkillModal() {
            const name = (document.getElementById('moCustomSkillNameInput').value || '').trim();
            if (!name) return;
            const description = (document.getElementById('moCustomSkillDescInput').value || '').trim();
            const row = { name: name, description: description };
            if (addingOtherForFixedPicker) {
                const target = skillPickerTarget === 'edit' ? 'edit' : 'publish';
                const s = getInlineSkillState(target);
                const key = name.toLowerCase();
                if (Array.from(s.fixedSet).some(function (t) { return String(t || '').trim().toLowerCase() === key; })) return;
                if ((s.customArr || []).some(function (r) { return r && r.name != null && String(r.name).trim().toLowerCase() === key; })) return;

                if (target === 'edit') {
                    editCustomSkills.push(row);
                    renderCompositeList(editCustomSkillList, editCustomSkills, editCustomSkillEmpty, 'custom', 'edit');
                    updateEditFixedSkillsSummary();
                } else {
                    publishCustomSkills.push(row);
                    renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
                    updatePublishFixedSkillsSummary();
                }
                closeModal(customSkillModal);
                renderInlineFixedSkillsPicker(target);
                return;
            }
            if (editingCustomSkillIndex !== null && editingCustomSkillIndex >= 0) {
                if (compositeTarget === 'edit') {
                    if (editCustomSkills[editingCustomSkillIndex]) {
                        editCustomSkills[editingCustomSkillIndex] = row;
                    }
                    renderCompositeList(editCustomSkillList, editCustomSkills, editCustomSkillEmpty, 'custom', 'edit');
                    updateEditFixedSkillsSummary();
                } else {
                    if (publishCustomSkills[editingCustomSkillIndex]) {
                        publishCustomSkills[editingCustomSkillIndex] = row;
                    }
                    renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
                    updatePublishFixedSkillsSummary();
                }
                editingCustomSkillIndex = null;
                closeModal(customSkillModal);
                return;
            }
            if (compositeTarget === 'edit') {
                editCustomSkills.push(row);
                renderCompositeList(editCustomSkillList, editCustomSkills, editCustomSkillEmpty, 'custom', 'edit');
                updateEditFixedSkillsSummary();
            } else {
                publishCustomSkills.push(row);
                renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
                updatePublishFixedSkillsSummary();
            }
            closeModal(customSkillModal);
        }

        function bindCompositeModals() {
            document.addEventListener('keydown', function (e) {
                if (e.key !== 'Escape') return;
                if (customSkillModal && !customSkillModal.hidden) {
                    closeModal(customSkillModal);
                    return;
                }
                if (assessmentModal && !assessmentModal.hidden) {
                    closeModal(assessmentModal);
                    return;
                }
                if (teachingWeeksModal && !teachingWeeksModal.hidden) {
                    closeModal(teachingWeeksModal);
                    return;
                }
                if (fixedSkillsPickerModal && !fixedSkillsPickerModal.hidden) {
                    closeModal(fixedSkillsPickerModal);
                }
            });
            document.querySelectorAll('[data-mo-close="assessment"]').forEach(function (n) {
                n.addEventListener('click', function () { closeModal(assessmentModal); });
            });
            document.querySelectorAll('[data-mo-close="custom-skill"]').forEach(function (n) {
                n.addEventListener('click', function () { closeModal(customSkillModal); });
            });
            const aBtn = document.getElementById('moCompositeAssessmentConfirmBtn');
            const cBtn = document.getElementById('moCompositeCustomSkillConfirmBtn');
            if (aBtn) aBtn.addEventListener('click', confirmAssessmentModal);
            if (cBtn) cBtn.addEventListener('click', confirmCustomSkillModal);
            const pubA = document.getElementById('openPublishAssessmentModalBtn');
            const edA = document.getElementById('openEditAssessmentModalBtn');
            if (pubA) pubA.addEventListener('click', function () { openAssessmentModal('publish'); });
            if (edA) edA.addEventListener('click', function () { openAssessmentModal('edit'); });
        }

        function getRecruitmentStatusPublish() {
            const el = document.querySelector('#jobPublishForm input[name="recruitmentStatus"]:checked');
            return el ? el.value.trim() : 'OPEN';
        }

        function getRecruitmentStatusEdit() {
            const el = document.querySelector('input[name="editRecruitmentStatus"]:checked');
            return el ? el.value.trim() : 'OPEN';
        }

        function setRecruitmentStatusEdit(val) {
            const v = (val || 'OPEN').toUpperCase() === 'CLOSED' ? 'CLOSED' : 'OPEN';
            const openEl = document.getElementById('editRecruitmentStatusOpen');
            const closedEl = document.getElementById('editRecruitmentStatusClosed');
            if (openEl) openEl.checked = v === 'OPEN';
            if (closedEl) closedEl.checked = v === 'CLOSED';
        }

        function resetPublishFormUi() {
            clearPublishSkillsInlineError();
            publishAssessments = [];
            publishCustomSkills = [];
            publishFixedTags.clear();
            publishTeachingWeeks = [];
            updatePublishTeachingWeeksSummary();
            updatePublishFixedSkillsSummary();
            renderCompositeList(publishAssessmentList, publishAssessments, publishAssessmentEmpty, 'assessment', 'publish');
            renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
            renderInlineFixedSkillsPicker('publish');
            const ro = document.getElementById('recruitmentStatusOpen');
            const rc = document.getElementById('recruitmentStatusClosed');
            if (ro) ro.checked = true;
            if (rc) rc.checked = false;
            applySemesterToPickers(
                '',
                document.getElementById('semesterYearInput'),
                document.getElementById('semesterTermInput')
            );
            const pubDlEl = document.getElementById('applicationDeadlineInput');
            if (pubDlEl) pubDlEl.value = '';
        }

        function openTeachingWeeksPicker(target) {
            weekPickerTarget = target || 'publish';
            const weeks = weekPickerTarget === 'edit' ? editTeachingWeeks : publishTeachingWeeks;
            clearTeachingWeeksPresetHighlight();
            setWeeksOnGrid(moTeachingWeeksPickerGrid, weeks);
            openModal(teachingWeeksModal);
        }

        function confirmTeachingWeeksPicker() {
            const weeks = getWeeksFromGrid(moTeachingWeeksPickerGrid);
            if (weekPickerTarget === 'edit') {
                editTeachingWeeks = weeks;
                updateEditTeachingWeeksSummary();
            } else {
                publishTeachingWeeks = weeks;
                updatePublishTeachingWeeksSummary();
            }
            closeModal(teachingWeeksModal);
        }

        function openFixedSkillsPicker(target) {
            // legacy entry-point: the picker is now inline in the publish/edit panel
            skillPickerTarget = target || 'publish';
            renderInlineFixedSkillsPicker(skillPickerTarget === 'edit' ? 'edit' : 'publish');
        }

        function confirmFixedSkillsPicker() {
            // modal confirm is no longer used (inline selection applies immediately)
            if (fixedSkillsPickerModal && !fixedSkillsPickerModal.hidden) {
                closeModal(fixedSkillsPickerModal);
            }
        }

        function bindPickerModals() {
            document.querySelectorAll('[data-mo-close="teaching-weeks"]').forEach(function (n) {
                n.addEventListener('click', function () { closeModal(teachingWeeksModal); });
            });
            const twOk = document.getElementById('moTeachingWeeksConfirmBtn');
            if (twOk) twOk.addEventListener('click', confirmTeachingWeeksPicker);

            const pubW = document.getElementById('openPublishTeachingWeeksBtn');
            const clrW = document.getElementById('clearPublishTeachingWeeksBtn');
            const edW = document.getElementById('openEditTeachingWeeksBtn');
            const clrEdW = document.getElementById('clearEditTeachingWeeksBtn');
            const pubF = document.getElementById('openPublishFixedSkillsBtn');
            const edF = document.getElementById('openEditFixedSkillsBtn');
            if (pubW) pubW.addEventListener('click', function () { openTeachingWeeksPicker('publish'); });
            if (clrW) clrW.addEventListener('click', function () {
                publishTeachingWeeks = [];
                updatePublishTeachingWeeksSummary();
            });
            if (edW) edW.addEventListener('click', function () { openTeachingWeeksPicker('edit'); });
            if (clrEdW) clrEdW.addEventListener('click', function () {
                editTeachingWeeks = [];
                updateEditTeachingWeeksSummary();
            });
            if (pubF) pubF.addEventListener('click', function () {
                skillPickerTarget = 'publish';
                renderInlineFixedSkillsPicker('publish');
                const el = publishFixedSkillsInlineFlow;
                if (el && typeof el.scrollIntoView === 'function') {
                    el.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
                }
            });
            if (edF) edF.addEventListener('click', function () {
                skillPickerTarget = 'edit';
                renderInlineFixedSkillsPicker('edit');
                const el = editFixedSkillsInlineFlow;
                if (el && typeof el.scrollIntoView === 'function') {
                    el.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
                }
            });

            const clrPubSkills = document.getElementById('clearPublishFixedSkillsBtn');
            const clrEditSkills = document.getElementById('clearEditFixedSkillsBtn');
            if (clrPubSkills) clrPubSkills.addEventListener('click', function () {
                if (!window.confirm(t('确定清除所有已选技能标签及其他补充吗？', 'Clear all selected skill tags and custom entries?'))) return;
                publishFixedTags.clear();
                publishCustomSkills = [];
                updatePublishFixedSkillsSummary();
                renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
                renderInlineFixedSkillsPicker('publish');
            });
            if (clrEditSkills) clrEditSkills.addEventListener('click', function () {
                if (!window.confirm(t('确定清除所有已选技能标签及其他补充吗？', 'Clear all selected skill tags and custom entries?'))) return;
                editFixedTags.clear();
                editCustomSkills = [];
                updateEditFixedSkillsSummary();
                renderCompositeList(editCustomSkillList, editCustomSkills, editCustomSkillEmpty, 'custom', 'edit');
                renderInlineFixedSkillsPicker('edit');
            });

            document.querySelectorAll('#moTeachingWeeksModal [data-week-preset]').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    if (btn.classList.contains('is-active')) {
                        if (moTeachingWeeksPickerGrid) setWeeksOnGrid(moTeachingWeeksPickerGrid, []);
                        clearTeachingWeeksPresetHighlight();
                        return;
                    }
                    var mode = btn.getAttribute('data-week-preset');
                    var weeks = [];
                    var w;
                    if (mode === 'all') {
                        for (w = 1; w <= 20; w += 1) weeks.push(w);
                    } else if (mode === 'odd') {
                        for (w = 1; w <= 20; w += 2) weeks.push(w);
                    } else if (mode === 'even') {
                        for (w = 2; w <= 20; w += 2) weeks.push(w);
                    } else {
                        return;
                    }
                    if (moTeachingWeeksPickerGrid) setWeeksOnGrid(moTeachingWeeksPickerGrid, weeks);
                    document.querySelectorAll('#moTeachingWeeksModal .mo-week-preset').forEach(function (b) {
                        b.classList.toggle('is-active', b === btn);
                    });
                });
            });
        }

        function bindPublishNav() {
            const publishPanel = document.querySelector('[data-modal="job-publish"]');
            const publishForm = document.getElementById('jobPublishForm');
            const publishContent = publishPanel ? publishPanel.querySelector('.mo-publish-content') : null;
            const navRoot = publishPanel ? publishPanel.querySelector('.mo-publish-nav') : null;
            var navLinks = navRoot ? navRoot.querySelectorAll('.mo-publish-nav-link') : [];

            function getSections() {
                return publishForm ? publishForm.querySelectorAll('.mo-publish-section[id]') : [];
            }

            /** 区块在右侧滚动区内的纵向位置（与 scrollTop 同坐标系；不能用 offsetTop，因 offsetParent 常为 form/grid 而非滚动容器） */
            function sectionTopInScroller(sectionEl) {
                if (!publishContent || !sectionEl) return 0;
                return (
                    sectionEl.getBoundingClientRect().top -
                    publishContent.getBoundingClientRect().top +
                    publishContent.scrollTop
                );
            }

            /** 左侧导航点击触发程序化滚动时，暂不根据 scroll 更新高亮（避免与目标不一致） */
            var ignorePublishNavScrollSpy = false;

            function setPublishNavActiveBySectionId(sectionId) {
                if (!sectionId || !navLinks.length) return;
                navLinks.forEach(function (l) {
                    var on = l.getAttribute('href') === '#' + sectionId;
                    l.classList.toggle('active', on);
                    if (on) {
                        l.setAttribute('aria-current', 'true');
                    } else {
                        l.removeAttribute('aria-current');
                    }
                });
            }

            function updateActiveFromScroll() {
                if (ignorePublishNavScrollSpy) return;
                var sections = getSections();
                if (!publishContent || !sections.length || !navLinks.length) return;
                var scrollTop = publishContent.scrollTop;
                /** 与 CSS scroll-margin-top 大致对齐，避免「短区块」时误判到下一节 */
                var offset = 56;
                var activeId = sections[0].id;
                var i;
                for (i = 0; i < sections.length; i++) {
                    var sec = sections[i];
                    var topIn = sectionTopInScroller(sec);
                    if (topIn - offset <= scrollTop + 0.5) {
                        activeId = sec.id;
                    }
                }
                navLinks.forEach(function (link) {
                    var on = link.getAttribute('href') === '#' + activeId;
                    link.classList.toggle('active', on);
                    if (on) {
                        link.setAttribute('aria-current', 'true');
                    } else {
                        link.removeAttribute('aria-current');
                    }
                });
            }

            navLinks.forEach(function (link) {
                link.addEventListener('click', function (e) {
                    e.preventDefault();
                    var targetId = link.getAttribute('href').substring(1);
                    var targetSection = document.getElementById(targetId);
                    if (!targetSection || !publishContent) return;
                    ignorePublishNavScrollSpy = true;
                    setPublishNavActiveBySectionId(targetId);
                    /** scrollIntoView 由浏览器对齐到滚动容器，比手动 scrollTop 更稳；配合 CSS scroll-margin */
                    targetSection.scrollIntoView({ block: 'start', behavior: 'auto', inline: 'nearest' });
                    requestAnimationFrame(function () {
                        ignorePublishNavScrollSpy = false;
                        setPublishNavActiveBySectionId(targetId);
                    });
                });
            });

            if (publishContent) {
                publishContent.addEventListener('scroll', updateActiveFromScroll, { passive: true });
            }
            window.addEventListener('resize', updateActiveFromScroll);

            updateActiveFromScroll();

            app.syncPublishJobNav = function () {
                if (publishContent) publishContent.scrollTop = 0;
                updateActiveFromScroll();
            };

            /** 校验失败时滚动右侧内容区并同步左侧导航高亮（与导航点击逻辑一致） */
            app.scrollPublishJobToSection = function (sectionId) {
                if (!sectionId) return;
                var targetSection = document.getElementById(sectionId);
                if (!targetSection || !publishContent) return;
                ignorePublishNavScrollSpy = true;
                setPublishNavActiveBySectionId(sectionId);
                targetSection.scrollIntoView({ block: 'start', behavior: 'auto', inline: 'nearest' });
                requestAnimationFrame(function () {
                    ignorePublishNavScrollSpy = false;
                    setPublishNavActiveBySectionId(sectionId);
                });
            };
        }

        function bindEditCourseNav() {
            const editPanel = document.querySelector('[data-modal="course-edit"]');
            const editForm = document.getElementById('courseEditForm');
            const editContent = editPanel ? editPanel.querySelector('.mo-publish-content') : null;
            const navRoot = editPanel ? editPanel.querySelector('.mo-publish-nav') : null;
            var navLinks = navRoot ? navRoot.querySelectorAll('.mo-publish-nav-link') : [];

            function getSections() {
                return editForm ? editForm.querySelectorAll('.mo-publish-section[id]') : [];
            }

            function sectionTopInScroller(sectionEl) {
                if (!editContent || !sectionEl) return 0;
                return (
                    sectionEl.getBoundingClientRect().top -
                    editContent.getBoundingClientRect().top +
                    editContent.scrollTop
                );
            }

            var ignoreEditNavScrollSpy = false;

            function setEditNavActiveBySectionId(sectionId) {
                if (!sectionId || !navLinks.length) return;
                navLinks.forEach(function (l) {
                    var on = l.getAttribute('href') === '#' + sectionId;
                    l.classList.toggle('active', on);
                    if (on) {
                        l.setAttribute('aria-current', 'true');
                    } else {
                        l.removeAttribute('aria-current');
                    }
                });
            }

            function updateActiveFromScroll() {
                if (ignoreEditNavScrollSpy) return;
                var sections = getSections();
                if (!editContent || !sections.length || !navLinks.length) return;
                var scrollTop = editContent.scrollTop;
                var offset = 56;
                var activeId = sections[0].id;
                var i;
                for (i = 0; i < sections.length; i += 1) {
                    var sec = sections[i];
                    var topIn = sectionTopInScroller(sec);
                    if (topIn - offset <= scrollTop + 0.5) {
                        activeId = sec.id;
                    }
                }
                navLinks.forEach(function (link) {
                    var on = link.getAttribute('href') === '#' + activeId;
                    link.classList.toggle('active', on);
                    if (on) {
                        link.setAttribute('aria-current', 'true');
                    } else {
                        link.removeAttribute('aria-current');
                    }
                });
            }

            navLinks.forEach(function (link) {
                link.addEventListener('click', function (e) {
                    e.preventDefault();
                    var targetId = link.getAttribute('href').substring(1);
                    var targetSection = document.getElementById(targetId);
                    if (!targetSection || !editContent) return;
                    ignoreEditNavScrollSpy = true;
                    setEditNavActiveBySectionId(targetId);
                    targetSection.scrollIntoView({ block: 'start', behavior: 'auto', inline: 'nearest' });
                    requestAnimationFrame(function () {
                        ignoreEditNavScrollSpy = false;
                        setEditNavActiveBySectionId(targetId);
                    });
                });
            });

            if (editContent) {
                editContent.addEventListener('scroll', updateActiveFromScroll, { passive: true });
            }
            window.addEventListener('resize', updateActiveFromScroll);

            updateActiveFromScroll();

            app.syncEditCourseNav = function () {
                if (editContent) editContent.scrollTop = 0;
                updateActiveFromScroll();
            };

            app.scrollEditCourseToSection = function (sectionId) {
                if (!sectionId) return;
                var targetSection = document.getElementById(sectionId);
                if (!targetSection || !editContent) return;
                ignoreEditNavScrollSpy = true;
                setEditNavActiveBySectionId(sectionId);
                targetSection.scrollIntoView({ block: 'start', behavior: 'auto', inline: 'nearest' });
                requestAnimationFrame(function () {
                    ignoreEditNavScrollSpy = false;
                    setEditNavActiveBySectionId(sectionId);
                });
            };
        }

        // 初始化周次选择按钮为圆形
        function initWeekButtons() {
            const weekGrids = document.querySelectorAll('.mo-week-grid');
            weekGrids.forEach(grid => {
                const labels = grid.querySelectorAll('label');
                labels.forEach(label => {
                    label.className = 'mo-week-btn';
                    const input = label.querySelector('input');
                    const span = label.querySelector('span');
                    if (input) {
                        input.style.display = 'none';
                    }
                    if (span) {
                        // 只显示数字，去掉"第"和"周"
                        span.textContent = span.textContent.replace(/[^0-9]/g, '');
                    }
                });

                // 添加点击事件，实现选中效果
                grid.addEventListener('click', function(e) {
                    if (e.target.classList.contains('mo-week-btn') || e.target.parentElement.classList.contains('mo-week-btn')) {
                        const btn = e.target.classList.contains('mo-week-btn') ? e.target : e.target.parentElement;
                        const input = btn.querySelector('input');
                        if (input) {
                            input.checked = !input.checked;
                            btn.classList.toggle('checked', input.checked);
                        }
                        if (grid.id === 'moTeachingWeeksPickerGrid') {
                            clearTeachingWeeksPresetHighlight();
                        }
                    }
                });
            });
        }

        function formatDetailDeadline(iso) {
            if (iso == null || typeof iso !== 'string') return '--';
            var s = iso.trim();
            if (!s) return '--';
            try {
                var d = new Date(s);
                if (isNaN(d.getTime())) return s;
                var locale = (typeof app.getCurrentLanguage === 'function' && app.getCurrentLanguage() === 'en') ? 'en-GB' : 'zh-CN';
                return d.toLocaleString(locale, {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit'
                });
            } catch (e) {
                return s;
            }
        }

        function renderDetail(item) {
            currentDetailJob = item;
            if (courseEditStatus) courseEditStatus.textContent = '';
            const setText = function (id, text) {
                const el = document.getElementById(id);
                if (el) el.textContent = text || '--';
            };
            setText('jobDetailCode', getDisplayCode(item));
            setText('jobDetailName', item.courseName || t('未命名岗位', 'Untitled opening'));
            setText('jobDetailCourseIntro', item.courseDescription);
            setText('jobDetailRecruitmentBrief', item.recruitmentBrief);
            setText('jobDetailSemester', item.semester || '--');
            setText('jobDetailTeachingWeeks', item.teachingWeeks ? ('Week ' + weekText(item.teachingWeeks)) : '--');
            setText('jobDetailCampus', getDisplayLocation(item));
            setText('jobDetailStatus', item.recruitmentStatus || item.status || 'OPEN');
            setText(
                'jobDetailTaRecruit',
                item.taRecruitCount != null && item.taRecruitCount >= 0 ? String(item.taRecruitCount) + t(' 人', '') : '--'
            );
            setText(
                'jobDetailStudentCount',
                item.studentCount != null && item.studentCount >= 0 ? String(item.studentCount) + t(' 人', '') : '--'
            );
            setText('jobDetailApplyDeadline', formatDetailDeadline(item.applicationDeadline));
            setText(
                'jobDetailJobRecordId',
                item.jobId != null && String(item.jobId).trim() !== '' ? String(item.jobId).trim() : '--'
            );

            const tags = document.getElementById('jobDetailTags');
            if (tags) {
                tags.innerHTML = '';
                getJobDetailSkillTagEntries(item).forEach(function (entry) {
                    const span = document.createElement('span');
                    if (entry.type === 'custom') {
                        span.className = 'pill pill--mo-custom-skill';
                        if (entry.description) {
                            span.classList.add('pill--mo-custom-skill--has-desc');
                            span.setAttribute('title', entry.description);
                        }
                    } else {
                        span.className = 'pill';
                    }
                    span.textContent = entry.label;
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
                    var preset = item.jobId != null && String(item.jobId).trim() !== ''
                        ? String(item.jobId).trim()
                        : '';
                    if (typeof app.closeAllModals === 'function') app.closeAllModals();
                    if (typeof app.navigateToApplicantsWithCourse === 'function') {
                        app.navigateToApplicantsWithCourse(preset);
                    } else if (typeof app.activateRoute === 'function') {
                        app.activateRoute('applicants');
                        if (typeof app.setApplicantCourse === 'function') app.setApplicantCourse(preset);
                    }
                };
            }
        }

        function renderBoard() {
            const keyword = (jobSearchInput.value || '').trim().toLowerCase();
            var semesterFilterVal = jobSemesterFilter ? String(jobSemesterFilter.value || '').trim() : '';
            var campusFilterVal = jobCampusFilter ? String(jobCampusFilter.value || '').trim() : '';
            var statusFilterVal = jobStatusFilter ? String(jobStatusFilter.value || '').trim() : '';
            var fillFilterVal = jobFillFilter ? String(jobFillFilter.value || '').trim() : '';
            state.filteredJobs = state.jobs.filter(function (item) {
                const text = [
                    item.courseCode || item.jobId,
                    item.courseName,
                    item.ownerMoName || '',
                    item.ownerMoId || '',
                    item.recruitmentBrief || '',
                    item.courseDescription || '',
                    (item.requiredSkills && item.requiredSkills.fixedTags || []).join(' '),
                    (item.requiredSkills && Array.isArray(item.requiredSkills.customSkills)
                        ? item.requiredSkills.customSkills.map(function (c) {
                            return c && c.name != null ? String(c.name) : '';
                        }).join(' ')
                        : '')
                ].join(' ').toLowerCase();
                if (keyword && !text.includes(keyword)) return false;
                if (semesterFilterVal && String(item.semester || '').trim() !== semesterFilterVal) return false;
                if (campusFilterVal && String(item.campus || '').trim() !== campusFilterVal) return false;
                if (statusFilterVal && getJobStatusNormalized(item) !== statusFilterVal) return false;
                if (fillFilterVal === 'full' && !isJobFilled(item)) return false;
                if (fillFilterVal === 'not_full' && isJobFilled(item)) return false;
                return true;
            });
            state.filteredJobs.sort(compareJobsDefaultOrder);

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
                
                // 获取状态样式
                const status = getJobStatusNormalized(item);
                const statusClass = status === 'OPEN' ? 'status-open' : 'status-closed';
                const filled = isJobFilled(item);
                const fillClass = filled ? 'course-fill-state--full' : 'course-fill-state--not-full';
                const fillText = filled ? t('已满', 'Filled') : t('未满', 'Not full');
                
                card.innerHTML =
                    '<div class="course-card-topline">' +
                        '<span class="job-code">' + getDisplayCode(item) + '</span>' +
                        '<span class="course-status-group">' +
                            '<span class="course-status ' + statusClass + '">' + status + '</span>' +
                            '<span class="course-fill-state ' + fillClass + '">' + fillText + '</span>' +
                        '</span>' +
                    '</div>' +
                    '<h4 class="course-card-title">' + (item.courseName || t('未命名岗位', 'Untitled opening')) + '</h4>' +
                    '<p class="course-card-description">' + (item.recruitmentBrief || item.courseDescription || t('暂无描述', 'No description')) + '</p>' +
                    '<div class="job-tags">' + buildCourseCardTagsHtml(item) + '</div>' +
                    '<div class="course-meta-footer">' +
                        '<div class="course-meta-tags-row">' +
                            '<span class="pill course-meta-tag">' + (item.semester || '--') + '</span>' +
                            '<span class="pill course-meta-tag">' + getCampusDisplayForCard(item) + '</span>' +
                            '<span class="pill course-meta-tag">' + t('TA 职位', 'TA Positions') + ': ' + getHiredCountForJob(item) + ' / ' + getJobCapacity(item) + '</span>' +
                        '</div>' +
                        '<span class="course-card-hint-inline">' + t('点击查看详情', 'View details') + ' <span aria-hidden="true">→</span></span>' +
                    '</div>' +
                    '';
                
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
                    scrollJobsPanelToTop();
                });
                jobPagination.appendChild(btn);
            }

            openCoursesCount.textContent = t('开放课程 ', 'Open ') + state.jobs.length;
            if (typeof app.onJobsUpdated === 'function') app.onJobsUpdated(state.jobs);
        }

        async function loadJobs() {
            if (!(app.state && app.state.moBulkRefreshInProgress)) {
                refreshJobsBtn.disabled = true;
            }
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
                await loadApplicantsForHiringSummary();
                renderJobsFilterOptions();
                state.page = 1;
                renderBoard();
            } catch (err) {
                console.error('[MO-JOBS] load failed', err);
            } finally {
                if (!(app.state && app.state.moBulkRefreshInProgress)) {
                    refreshJobsBtn.disabled = false;
                }
            }
        }

        function publishReportValidationError(msg, sectionId, focusEl) {
            clearPublishSkillsInlineError();
            publishStatus.textContent = msg;
            if (sectionId && typeof app.scrollPublishJobToSection === 'function') {
                app.scrollPublishJobToSection(sectionId);
            }
            if (focusEl && typeof focusEl.focus === 'function') {
                requestAnimationFrame(function () {
                    requestAnimationFrame(function () {
                        try {
                            focusEl.focus({ preventScroll: true });
                        } catch (e) {
                            focusEl.focus();
                        }
                    });
                });
            }
        }

        function editReportValidationError(msg, sectionId, focusEl) {
            clearEditSkillsInlineError();
            if (courseEditStatus) courseEditStatus.textContent = msg;
            if (sectionId && typeof app.scrollEditCourseToSection === 'function') {
                app.scrollEditCourseToSection(sectionId);
            }
            if (focusEl && typeof focusEl.focus === 'function') {
                requestAnimationFrame(function () {
                    requestAnimationFrame(function () {
                        try {
                            focusEl.focus({ preventScroll: true });
                        } catch (e) {
                            focusEl.focus();
                        }
                    });
                });
            }
        }

        async function publishJob(event) {
            event.preventDefault();
            clearPublishSkillsInlineError();
            const teachingWeeks = {
                weeks: publishTeachingWeeks.slice().sort(function (a, b) { return a - b; })
            };
            const fixedSkills = getFixedTagsArrayFromSet(publishFixedTags);
            const courseCodeInput = document.getElementById('courseCodeInput').value.trim();
            const courseNameInput = document.getElementById('courseNameInput').value.trim();
            const semesterInput = buildSemesterFromPickers(
                document.getElementById('semesterYearInput'),
                document.getElementById('semesterTermInput')
            );
            const recruitmentStatusInput = getRecruitmentStatusPublish();
            const courseDescInput = document.getElementById('courseDescInput').value.trim();
            const studentCountRaw = document.getElementById('studentCountInput').value.trim();
            const taRecruitCountRaw = document.getElementById('taRecruitCountInput').value.trim();
            const campusRaw = document.getElementById('campusInput').value.trim();
            const recruitmentBriefRaw = document.getElementById('recruitmentBriefInput').value.trim();
            const sessionMoId = getMoIdForApi();
            if (!sessionMoId) {
                publishStatus.textContent = t('未登录或无法验证身份，无法发布', 'Not signed in; cannot publish');
                return;
            }

            const taNum = taRecruitCountRaw === '' ? NaN : Number(taRecruitCountRaw);
            const deadlineInput = document.getElementById('applicationDeadlineInput');
            const pubDl = readLocalApplicationDeadline(deadlineInput);

            if (!courseNameInput) {
                publishReportValidationError(t('请填写岗位/课程名称。', 'Enter the opening / module name.'), 'basic-info', document.getElementById('courseNameInput'));
                return;
            }
            if (!courseCodeInput) {
                publishReportValidationError(t('请填写课程编号。', 'Enter the module code.'), 'basic-info', document.getElementById('courseCodeInput'));
                return;
            }
            if (!recruitmentStatusInput) {
                publishReportValidationError(t('请选择招聘状态（OPEN 或 CLOSED）。', 'Choose recruitment status (OPEN or CLOSED).'), 'basic-info', document.getElementById('recruitmentStatusOpen'));
                return;
            }
            if (!semesterInput) {
                publishReportValidationError(t('请选择学期类型：Spring 或 Fall（不可为「学期」占位项）。', 'Choose Spring or Fall (not the placeholder).'), 'basic-info', document.getElementById('semesterTermInput'));
                return;
            }
            if (Number.isNaN(taNum) || taNum < 1) {
                publishReportValidationError(t('请填写 TA 招聘人数（≥1 的整数）。', 'Enter TA openings (integer ≥ 1).'), 'basic-info', document.getElementById('taRecruitCountInput'));
                return;
            }
            if (!campusRaw) {
                publishReportValidationError(t('请选择校区（Main 或 Shahe）。', 'Choose a campus (Main or Shahe).'), 'basic-info', document.getElementById('campusInput'));
                return;
            }
            if (!pubDl.ok) {
                publishReportValidationError(pubDl.msg, 'basic-info', deadlineInput);
                return;
            }
            if (!courseDescInput) {
                publishReportValidationError(t('请填写课程介绍。', 'Enter the module introduction.'), 'basic-info', document.getElementById('courseDescInput'));
                return;
            }
            if (fixedSkills.length === 0) {
                if (publishStatus) publishStatus.textContent = '';
                var pubSkErr = document.getElementById('publishSkillsFieldError');
                if (pubSkErr) {
                    pubSkErr.textContent = t('请添加技能标签', 'Add at least one skill tag');
                    pubSkErr.hidden = false;
                }
                if (typeof app.scrollPublishJobToSection === 'function') {
                    app.scrollPublishJobToSection('required-skills');
                }
                var pubFlow = document.getElementById('publishFixedSkillsInlineFlow');
                if (pubFlow && typeof pubFlow.focus === 'function') {
                    requestAnimationFrame(function () {
                        requestAnimationFrame(function () {
                            try {
                                pubFlow.focus({ preventScroll: true });
                            } catch (e) {
                                pubFlow.focus();
                            }
                        });
                    });
                } else if (pubFlow && typeof pubFlow.scrollIntoView === 'function') {
                    pubFlow.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
                }
                return;
            }

            const body = {
                moId: sessionMoId,
                courseCode: courseCodeInput,
                courseName: courseNameInput,
                semester: semesterInput,
                recruitmentStatus: recruitmentStatusInput,
                status: recruitmentStatusInput,
                campus: campusRaw,
                taRecruitCount: taNum,
                assessmentEvents: publishAssessments.map(function (e) {
                    return {
                        name: e.name,
                        weeks: e.weeks || [],
                        description: e.description || ''
                    };
                }),
                requiredSkills: {
                    fixedTags: fixedSkills,
                    customSkills: publishCustomSkills.slice()
                },
                courseDescription: courseDescInput,
                source: 'mo-manual-v2'
            };
            if (teachingWeeks.weeks.length) body.teachingWeeks = teachingWeeks;
            body.studentCount = studentCountRaw === '' ? -1 : (Number(studentCountRaw) || 0);
            if (pubDl.iso) body.applicationDeadline = pubDl.iso;
            if (recruitmentBriefRaw) body.recruitmentBrief = recruitmentBriefRaw;

            publishStatus.textContent = t('发布中...', 'Publishing…');
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
                    publishStatus.textContent = payload.message || t('发布失败', 'Publish failed');
                    return;
                }
                publishStatus.textContent = t('发布成功', 'Published');
                if (window.MoToast && typeof window.MoToast.show === 'function') {
                    window.MoToast.show({
                        type: 'success',
                        message: t('岗位发布成功', 'Opening published successfully')
                    });
                }
                publishForm.reset();
                resetPublishFormUi();
                if (typeof app.closeAllModals === 'function') app.closeAllModals();
                await loadJobs();
            } catch (err) {
                publishStatus.textContent = (err && err.message) ? err.message : t('发布失败', 'Publish failed');
            }
        }

        async function populateCourseEditForm(item) {
            if (!item) return;
            clearEditSkillsInlineError();
            const codeEl = document.getElementById('editCourseCodeDisplay');
            if (codeEl) codeEl.textContent = item.courseCode || item.jobId || '--';
            const setVal = function (id, v) {
                const el = document.getElementById(id);
                if (el) el.value = v == null ? '' : String(v);
            };
            setVal('editCourseNameInput', item.courseName || '');
            applySemesterToPickers(
                item.semester || '',
                document.getElementById('editSemesterYearInput'),
                document.getElementById('editSemesterTermInput')
            );
            setRecruitmentStatusEdit(item.recruitmentStatus || item.status || 'OPEN');
            setVal('editStudentCountInput', item.studentCount != null && item.studentCount >= 0 ? item.studentCount : '');
            setVal('editTaRecruitCountInput', item.taRecruitCount != null && item.taRecruitCount >= 1 ? item.taRecruitCount : '');
            const campusEl = document.getElementById('editCampusInput');
            if (campusEl) campusEl.value = item.campus || '';
            applyIsoToDatetimeLocalInput(item.applicationDeadline, document.getElementById('editApplicationDeadlineInput'));
            const weeks = item.teachingWeeks && Array.isArray(item.teachingWeeks.weeks) ? item.teachingWeeks.weeks : [];
            editTeachingWeeks = weeks.map(Number).filter(function (n) { return n >= 1 && n <= 20; })
                .sort(function (a, b) { return a - b; });
            updateEditTeachingWeeksSummary();

            editAssessments = (item.assessmentEvents || []).map(function (ev) {
                return {
                    name: ev.name || '',
                    weeks: Array.isArray(ev.weeks) ? ev.weeks.slice() : [],
                    description: ev.description || ''
                };
            });
            editCustomSkills = [];
            if (item.requiredSkills && Array.isArray(item.requiredSkills.customSkills)) {
                item.requiredSkills.customSkills.forEach(function (c) {
                    editCustomSkills.push({ name: c.name || '', description: c.description || '' });
                });
            }
            renderCompositeList(editAssessmentList, editAssessments, editAssessmentEmpty, 'assessment', 'edit');
            renderCompositeList(editCustomSkillList, editCustomSkills, editCustomSkillEmpty, 'custom', 'edit');

            editFixedTags.clear();
            if (item.requiredSkills && Array.isArray(item.requiredSkills.fixedTags)) {
                item.requiredSkills.fixedTags.forEach(function (t) {
                    const x = String(t).trim();
                    if (x) editFixedTags.add(x);
                });
            }
            await loadSkillTagsFromApi();
            updateEditFixedSkillsSummary();
            renderInlineFixedSkillsPicker('edit');

            setVal('editCourseDescInput', item.courseDescription || '');
            setVal('editRecruitmentBriefInput', item.recruitmentBrief || '');
        }

        async function saveCourseEdit(event) {
            if (event && typeof event.preventDefault === 'function') event.preventDefault();
            clearEditSkillsInlineError();
            if (!currentDetailJob) {
                if (courseEditStatus) courseEditStatus.textContent = t('未选择课程', 'No module selected');
                return;
            }
            const sessionMoId = getMoIdForApi();
            if (!sessionMoId) {
                if (courseEditStatus) courseEditStatus.textContent = t('未登录或无法验证身份', 'Not signed in');
                return;
            }
            const courseCode = String(currentDetailJob.courseCode || '').trim();
            const courseNameInput = document.getElementById('editCourseNameInput');
            const courseDescInput = document.getElementById('editCourseDescInput');
            const courseNameVal = courseNameInput ? courseNameInput.value.trim() : '';
            const recruitmentStatusVal = getRecruitmentStatusEdit();
            const courseDescVal = courseDescInput ? courseDescInput.value.trim() : '';
            const fixedSkills = getFixedTagsArrayFromSet(editFixedTags);
            const semesterVal = buildSemesterFromPickers(
                document.getElementById('editSemesterYearInput'),
                document.getElementById('editSemesterTermInput')
            );
            const studentCountRaw = document.getElementById('editStudentCountInput').value.trim();
            const taRecruitCountRaw = document.getElementById('editTaRecruitCountInput').value.trim();
            const campusRaw = document.getElementById('editCampusInput').value.trim();
            const recruitmentBriefRaw = document.getElementById('editRecruitmentBriefInput').value.trim();
            const taNum = taRecruitCountRaw === '' ? NaN : Number(taRecruitCountRaw);
            const deadlineInputEl = document.getElementById('editApplicationDeadlineInput');
            const editDl = readLocalApplicationDeadline(deadlineInputEl);

            if (!courseNameVal) {
                editReportValidationError(t('请填写岗位/课程名称。', 'Enter the opening / module name.'), 'edit-basic-info', courseNameInput);
                return;
            }
            if (!recruitmentStatusVal) {
                editReportValidationError(t('请选择招聘状态（OPEN 或 CLOSED）。', 'Choose recruitment status (OPEN or CLOSED).'), 'edit-basic-info', document.getElementById('editRecruitmentStatusOpen'));
                return;
            }
            if (!semesterVal) {
                editReportValidationError(t('请选择学期类型：Spring 或 Fall（不可为「学期」占位项）。', 'Choose Spring or Fall (not the placeholder).'), 'edit-basic-info', document.getElementById('editSemesterTermInput'));
                return;
            }
            if (Number.isNaN(taNum) || taNum < 1) {
                editReportValidationError(t('请填写 TA 招聘人数（≥1 的整数）。', 'Enter TA openings (integer ≥ 1).'), 'edit-basic-info', document.getElementById('editTaRecruitCountInput'));
                return;
            }
            if (!campusRaw) {
                editReportValidationError(t('请选择校区（Main 或 Shahe）。', 'Choose a campus (Main or Shahe).'), 'edit-basic-info', document.getElementById('editCampusInput'));
                return;
            }
            if (!editDl.ok) {
                editReportValidationError(editDl.msg, 'edit-basic-info', deadlineInputEl);
                return;
            }
            if (!courseDescVal) {
                editReportValidationError(t('请填写课程介绍。', 'Enter the module introduction.'), 'edit-basic-info', courseDescInput);
                return;
            }
            if (fixedSkills.length === 0) {
                if (courseEditStatus) courseEditStatus.textContent = '';
                var edSkErr = document.getElementById('editSkillsFieldError');
                if (edSkErr) {
                    edSkErr.textContent = t('请添加技能标签', 'Add at least one skill tag');
                    edSkErr.hidden = false;
                }
                if (typeof app.scrollEditCourseToSection === 'function') {
                    app.scrollEditCourseToSection('edit-required-skills');
                }
                var edFlow = document.getElementById('editFixedSkillsInlineFlow');
                if (edFlow && typeof edFlow.focus === 'function') {
                    requestAnimationFrame(function () {
                        requestAnimationFrame(function () {
                            try {
                                edFlow.focus({ preventScroll: true });
                            } catch (e) {
                                edFlow.focus();
                            }
                        });
                    });
                } else if (edFlow && typeof edFlow.scrollIntoView === 'function') {
                    edFlow.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
                }
                return;
            }

            const teachingWeeks = {
                weeks: editTeachingWeeks.slice().sort(function (a, b) { return a - b; })
            };

            const body = {
                moId: sessionMoId,
                courseCode: courseCode,
                courseName: courseNameVal,
                semester: semesterVal,
                recruitmentStatus: recruitmentStatusVal,
                status: recruitmentStatusVal,
                campus: campusRaw,
                taRecruitCount: taNum,
                assessmentEvents: editAssessments.map(function (e) {
                    return { name: e.name, weeks: e.weeks || [], description: e.description || '' };
                }),
                requiredSkills: {
                    fixedTags: fixedSkills,
                    customSkills: editCustomSkills.slice()
                },
                courseDescription: courseDescVal,
                source: 'mo-manual-v2-edit'
            };
            body.teachingWeeks = teachingWeeks;
            body.studentCount = studentCountRaw === '' ? -1 : (Number(studentCountRaw) || 0);
            body.applicationDeadline = editDl.iso || '';
            if (recruitmentBriefRaw) body.recruitmentBrief = recruitmentBriefRaw;

            if (courseEditStatus) courseEditStatus.textContent = t('保存中...', 'Saving…');
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
                    if (courseEditStatus) courseEditStatus.textContent = payload.message || t('保存失败', 'Save failed');
                    return;
                }
                if (courseEditStatus) courseEditStatus.textContent = t('已保存', 'Saved');
                if (window.MoToast && typeof window.MoToast.show === 'function') {
                    window.MoToast.show({
                        type: 'success',
                        message: t('课程信息已保存', 'Module details saved')
                    });
                }
                if (payload.item) {
                    currentDetailJob = payload.item;
                    renderDetail(currentDetailJob);
                }
                await loadJobs();
                if (typeof app.openModal === 'function') app.openModal('course-detail');
            } catch (err) {
                if (courseEditStatus) courseEditStatus.textContent = (err && err.message) ? err.message : t('保存失败', 'Save failed');
            }
        }

        app.getJobs = function () { return state.jobs.slice(); };
        app.loadJobs = loadJobs;

        /** 总览弹窗等：关闭摘要类弹窗后打开「课程岗位详情」 */
        app.showMoCourseJobDetail = function (item) {
            if (!item) return;
            if (typeof app.closeAllModals === 'function') {
                app.closeAllModals();
            }
            renderDetail(item);
            if (typeof app.openModal === 'function') {
                app.openModal('course-detail');
            }
        };

        if (toggleCourseEditBtn && typeof app.openModal === 'function') {
            toggleCourseEditBtn.addEventListener('click', function () {
                if (!currentDetailJob) return;
                if (courseEditStatus) courseEditStatus.textContent = '';
                clearEditSkillsInlineError();
                app.openModal('course-edit');
                populateCourseEditForm(currentDetailJob);
                requestAnimationFrame(function () {
                    if (typeof app.syncEditCourseNav === 'function') app.syncEditCourseNav();
                    var nameEl = document.getElementById('editCourseNameInput');
                    if (nameEl && typeof nameEl.focus === 'function') nameEl.focus();
                });
            });
        }
        if (courseEditForm) {
            courseEditForm.addEventListener('submit', saveCourseEdit);
        }

        if (publishForm) publishForm.addEventListener('submit', publishJob);
        if (openJobPublishModalBtn && typeof app.openModal === 'function') {
            openJobPublishModalBtn.addEventListener('click', function () {
                app.openModal('job-publish');
                if (publishStatus) publishStatus.textContent = '';
                clearPublishSkillsInlineError();
                renderInlineFixedSkillsPicker('publish');
                applySemesterToPickers(
                    '',
                    document.getElementById('semesterYearInput'),
                    document.getElementById('semesterTermInput')
                );
                const oDl = document.getElementById('applicationDeadlineInput');
                if (oDl) oDl.value = '';
                requestAnimationFrame(function () {
                    if (typeof app.syncPublishJobNav === 'function') app.syncPublishJobNav();
                    const first = document.getElementById('courseNameInput');
                    if (first && typeof first.focus === 'function') first.focus();
                });
            });
        }
        jobSearchInput.addEventListener('input', function () {
            state.page = 1;
            renderBoard();
        });
        if (jobSemesterFilter) {
            jobSemesterFilter.addEventListener('change', function () {
                state.page = 1;
                updateJobsFilterResetButton();
                renderBoard();
            });
        }
        if (jobCampusFilter) {
            jobCampusFilter.addEventListener('change', function () {
                state.page = 1;
                updateJobsFilterResetButton();
                renderBoard();
            });
        }
        if (jobStatusFilter) {
            jobStatusFilter.addEventListener('change', function () {
                state.page = 1;
                updateJobsFilterResetButton();
                renderBoard();
            });
        }
        if (jobFillFilter) {
            jobFillFilter.addEventListener('change', function () {
                state.page = 1;
                updateJobsFilterResetButton();
                renderBoard();
            });
        }
        if (clearJobFiltersBtn) {
            clearJobFiltersBtn.addEventListener('click', function () {
                if (jobSemesterFilter) jobSemesterFilter.value = '';
                if (jobCampusFilter) jobCampusFilter.value = '';
                if (jobStatusFilter) jobStatusFilter.value = '';
                if (jobFillFilter) jobFillFilter.value = '';
                state.page = 1;
                updateJobsFilterResetButton();
                renderBoard();
            });
        }
        refreshJobsBtn.addEventListener('click', function () {
            if (typeof app.refreshMoWorkspaceAll === 'function') {
                void app.refreshMoWorkspaceAll();
                return;
            }
            loadJobs();
        });

        buildWeekGrid(moTeachingWeeksPickerGrid, 'moTwPick', 'tw', false);
        buildWeekGrid(moAssessmentWeeksGrid, 'moAssessmentWeek', 'aw', true);

        bindCompositeModals();
        bindPickerModals();
        bindPublishNav();
        bindEditCourseNav();
        initWeekButtons();

        populateSemesterYearSelect(document.getElementById('semesterYearInput'), null);
        populateSemesterYearSelect(document.getElementById('editSemesterYearInput'), null);

        function refreshWeekGridLabels(container, compact) {
            if (!container) return;
            var idx = 0;
            container.querySelectorAll('label.mo-week-chip span').forEach(function (span) {
                idx += 1;
                span.textContent = compact ? String(idx) : t('第' + idx + '周', 'Week ' + idx);
            });
        }

        function refreshJobBoardLanguage() {
            refreshWeekGridLabels(moTeachingWeeksPickerGrid, false);
            refreshWeekGridLabels(moAssessmentWeeksGrid, true);
            updatePublishTeachingWeeksSummary();
            updateEditTeachingWeeksSummary();
            updatePublishFixedSkillsSummary();
            updateEditFixedSkillsSummary();
            renderCompositeList(publishAssessmentList, publishAssessments, publishAssessmentEmpty, 'assessment', 'publish');
            renderCompositeList(editAssessmentList, editAssessments, editAssessmentEmpty, 'assessment', 'edit');
            renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
            renderCompositeList(editCustomSkillList, editCustomSkills, editCustomSkillEmpty, 'custom', 'edit');
            renderInlineFixedSkillsPicker('publish');
            renderInlineFixedSkillsPicker('edit');
            if (currentDetailJob) renderDetail(currentDetailJob);
            if (jobBoard) renderBoard();
        }

        app.refreshJobBoardLanguage = refreshJobBoardLanguage;

        loadSkillTagsFromApi().then(function () {
            updatePublishTeachingWeeksSummary();
            updateEditTeachingWeeksSummary();
            updatePublishFixedSkillsSummary();
            updateEditFixedSkillsSummary();
            renderCompositeList(publishAssessmentList, publishAssessments, publishAssessmentEmpty, 'assessment', 'publish');
            renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
            renderInlineFixedSkillsPicker('publish');
            renderInlineFixedSkillsPicker('edit');
        });

        loadJobs();
    };
})();