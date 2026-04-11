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
            if (!iso) return { ok: false, msg: '申请截止时间无效，请检查日期与时间' };
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
        var compositeTarget = 'publish';

        const jobBoard = document.getElementById('jobBoard');
        const jobPagination = document.getElementById('jobPagination');
        const jobSearchInput = document.getElementById('jobSearchInput');
        const openCoursesCount = document.getElementById('openCoursesCount');
        const refreshJobsBtn = document.getElementById('refreshJobsBtn');
        const openJobPublishModalBtn = document.getElementById('openJobPublishModalBtn');
        const publishForm = document.getElementById('jobPublishForm');
        const publishStatus = document.getElementById('publishJobStatus');
        const moFixedSkillsPickerFlow = document.getElementById('moFixedSkillsPickerFlow');
        const publishTeachingWeeksSummary = document.getElementById('publishTeachingWeeksSummary');
        const editTeachingWeeksSummary = document.getElementById('editTeachingWeeksSummary');
        const publishFixedSkillsSummary = document.getElementById('publishFixedSkillsSummary');
        const editFixedSkillsSummary = document.getElementById('editFixedSkillsSummary');

        const moTeachingWeeksPickerGrid = document.getElementById('moTeachingWeeksPickerGrid');
        const teachingWeeksModal = document.getElementById('moTeachingWeeksModal');
        const fixedSkillsPickerModal = document.getElementById('moFixedSkillsPickerModal');
        const moAssessmentWeeksGrid = document.getElementById('moAssessmentWeeksGrid');

        const publishAssessmentList = document.getElementById('publishAssessmentList');
        const publishAssessmentEmpty = document.getElementById('publishAssessmentEmpty');
        const publishCustomSkillList = document.getElementById('publishCustomSkillList');
        const publishCustomSkillEmpty = document.getElementById('publishCustomSkillEmpty');
        const editAssessmentList = document.getElementById('editAssessmentList');
        const editAssessmentEmpty = document.getElementById('editAssessmentEmpty');
        const editCustomSkillList = document.getElementById('editCustomSkillList');
        const editCustomSkillEmpty = document.getElementById('editCustomSkillEmpty');

        const courseEditPanel = document.getElementById('courseEditPanel');
        const toggleCourseEditBtn = document.getElementById('toggleCourseEditBtn');
        const saveCourseEditBtn = document.getElementById('saveCourseEditBtn');
        const courseEditStatus = document.getElementById('courseEditStatus');

        const assessmentModal = document.getElementById('moCompositeAssessmentModal');
        const customSkillModal = document.getElementById('moCompositeCustomSkillModal');

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
                span.textContent = compact ? String(w) : ('第' + w + '周');
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
            return '已选第 ' + arr.join('、') + ' 周';
        }

        function updatePublishTeachingWeeksSummary() {
            if (!publishTeachingWeeksSummary) return;
            publishTeachingWeeksSummary.textContent = formatWeeksSummaryLine(publishTeachingWeeks)
                ? formatWeeksSummaryLine(publishTeachingWeeks)
                : '尚未选择授课周次（选填）。';
        }

        function updateEditTeachingWeeksSummary() {
            if (!editTeachingWeeksSummary) return;
            editTeachingWeeksSummary.textContent = formatWeeksSummaryLine(editTeachingWeeks)
                ? formatWeeksSummaryLine(editTeachingWeeks)
                : '未选择授课周次。';
        }

        function syncFixedSkillsPrimaryButtonLabels() {
            const pubBtn = document.getElementById('openPublishFixedSkillsBtn');
            const edBtn = document.getElementById('openEditFixedSkillsBtn');
            const pubHas = publishFixedTags.size > 0 || publishCustomSkills.length > 0;
            const edHas = editFixedTags.size > 0 || editCustomSkills.length > 0;
            if (pubBtn) pubBtn.textContent = pubHas ? '修改技能标签' : '＋ 添加技能标签';
            if (edBtn) edBtn.textContent = edHas ? '修改技能标签' : '＋ 添加技能标签';
        }

        function updatePublishFixedSkillsSummary() {
            const arr = getFixedTagsArrayFromSet(publishFixedTags);
            const customN = publishCustomSkills.length;
            const parts = [];
            if (arr.length) parts.push('已选标签 ' + arr.length + ' 项：' + arr.join('、'));
            if (customN) parts.push('其他技能 ' + customN + ' 项（详见下方列表）');
            if (publishFixedSkillsSummary) {
                publishFixedSkillsSummary.textContent = parts.length
                    ? parts.join('；')
                    : '尚未选择技能，可点击上方「＋ 添加技能标签」按钮。';
            }
            syncFixedSkillsPrimaryButtonLabels();
        }

        function updateEditFixedSkillsSummary() {
            const arr = getFixedTagsArrayFromSet(editFixedTags);
            const customN = editCustomSkills.length;
            const parts = [];
            if (arr.length) parts.push('已选标签 ' + arr.length + ' 项：' + arr.join('、'));
            if (customN) parts.push('其他技能 ' + customN + ' 项（详见下方列表）');
            if (editFixedSkillsSummary) {
                editFixedSkillsSummary.textContent = parts.length
                    ? parts.join('；')
                    : '尚未选择所需技能，可点击上方「＋ 添加技能标签」按钮。';
            }
            syncFixedSkillsPrimaryButtonLabels();
        }

        function renderFixedSkillsPickerFlow() {
            const container = moFixedSkillsPickerFlow;
            if (!container) return;
            container.innerHTML = '';
            (skillTagList || []).forEach(function (tag) {
                const t = String(tag || '').trim();
                if (!t) return;
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'mo-skill-chip' + (skillPickerWorkingSet.has(t) ? ' is-selected' : '');
                btn.textContent = t;
                btn.setAttribute('aria-pressed', skillPickerWorkingSet.has(t) ? 'true' : 'false');
                btn.addEventListener('click', function () {
                    if (skillPickerWorkingSet.has(t)) skillPickerWorkingSet.delete(t);
                    else skillPickerWorkingSet.add(t);
                    renderFixedSkillsPickerFlow();
                });
                container.appendChild(btn);
            });
            skillPickerPendingCustom.forEach(function (row, index) {
                const pill = document.createElement('button');
                pill.type = 'button';
                pill.className = 'mo-skill-chip mo-skill-chip--custom-pill';
                const label = document.createElement('span');
                label.className = 'mo-skill-chip__label';
                label.textContent = row.name || '';
                pill.appendChild(label);
                const tip = row.description ? (row.name + ' — ' + row.description) : row.name;
                pill.setAttribute('title', tip);
                pill.setAttribute('aria-label', '点击移除「' + (row.name || '') + '」');
                (function (idx) {
                    pill.addEventListener('click', function () {
                        skillPickerPendingCustom.splice(idx, 1);
                        renderFixedSkillsPickerFlow();
                    });
                })(index);
                container.appendChild(pill);
            });
            const otherBtn = document.createElement('button');
            otherBtn.type = 'button';
            otherBtn.className = 'mo-skill-chip mo-skill-chip--other';
            otherBtn.textContent = 'Other';
            otherBtn.setAttribute('aria-label', '添加其他技能');
            otherBtn.addEventListener('click', function () { openOtherSkillSubModal(); });
            container.appendChild(otherBtn);
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

        function toTags(item) {
            if (item.requiredSkills && Array.isArray(item.requiredSkills.fixedTags)) {
                return item.requiredSkills.fixedTags.slice(0, 4);
            }
            return [];
        }

        function moOwnerLabel(item) {
            return (item && (item.ownerMoName || item.ownerMoId)) || 'MO';
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
                    title.textContent = entry.name || '（未命名）';
                    main.appendChild(title);
                    const meta = document.createElement('div');
                    meta.className = 'mo-composite-item__meta';
                    meta.textContent = (entry.weeks && entry.weeks.length)
                        ? ('周次：' + entry.weeks.join(', '))
                        : '周次：—';
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
                    title.textContent = entry.name || '（未命名）';
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
                rm.textContent = '移除';
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
                li.appendChild(rm);
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
                el.classList.remove('mo-publish-modal--stack-top');
                const t = document.getElementById('moCompositeCustomSkillTitle');
                if (t) t.textContent = '添加自定义技能';
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
            const nameEl = document.getElementById('moCustomSkillNameInput');
            const descEl = document.getElementById('moCustomSkillDescInput');
            if (nameEl) nameEl.value = '';
            if (descEl) descEl.value = '';
            const titleEl = document.getElementById('moCompositeCustomSkillTitle');
            if (titleEl) titleEl.textContent = '其他技能';
            if (customSkillModal) customSkillModal.classList.add('mo-publish-modal--stack-top');
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
                if (skillPickerPendingCustom.some(function (r) { return r.name === name; })) {
                    return;
                }
                if (skillPickerWorkingSet.has(name)) {
                    return;
                }
                skillPickerPendingCustom.push(row);
                closeModal(customSkillModal);
                renderFixedSkillsPickerFlow();
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
            publishAssessments = [];
            publishCustomSkills = [];
            publishFixedTags.clear();
            publishTeachingWeeks = [];
            updatePublishTeachingWeeksSummary();
            updatePublishFixedSkillsSummary();
            renderCompositeList(publishAssessmentList, publishAssessments, publishAssessmentEmpty, 'assessment', 'publish');
            renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
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
            skillPickerTarget = target || 'publish';
            const src = skillPickerTarget === 'edit' ? editFixedTags : publishFixedTags;
            skillPickerWorkingSet = new Set(src);
            const srcCustom = skillPickerTarget === 'edit' ? editCustomSkills : publishCustomSkills;
            skillPickerPendingCustom = (srcCustom || []).map(function (x) {
                return { name: String(x.name || '').trim(), description: String(x.description || '').trim() };
            }).filter(function (x) { return x.name; });
            renderFixedSkillsPickerFlow();
            openModal(fixedSkillsPickerModal);
        }

        function confirmFixedSkillsPicker() {
            if (skillPickerWorkingSet.size < 1) {
                return;
            }
            const pending = skillPickerPendingCustom.map(function (r) {
                return { name: r.name, description: r.description || '' };
            });
            if (skillPickerTarget === 'edit') {
                editFixedTags.clear();
                skillPickerWorkingSet.forEach(function (t) { editFixedTags.add(t); });
                editCustomSkills = pending;
                updateEditFixedSkillsSummary();
                renderCompositeList(editCustomSkillList, editCustomSkills, editCustomSkillEmpty, 'custom', 'edit');
            } else {
                publishFixedTags.clear();
                skillPickerWorkingSet.forEach(function (t) { publishFixedTags.add(t); });
                publishCustomSkills = pending;
                updatePublishFixedSkillsSummary();
                renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
            }
            closeModal(fixedSkillsPickerModal);
        }

        function bindPickerModals() {
            document.querySelectorAll('[data-mo-close="teaching-weeks"]').forEach(function (n) {
                n.addEventListener('click', function () { closeModal(teachingWeeksModal); });
            });
            document.querySelectorAll('[data-mo-close="fixed-skills"]').forEach(function (n) {
                n.addEventListener('click', function () { closeModal(fixedSkillsPickerModal); });
            });
            const twOk = document.getElementById('moTeachingWeeksConfirmBtn');
            const fsOk = document.getElementById('moFixedSkillsConfirmBtn');
            if (twOk) twOk.addEventListener('click', confirmTeachingWeeksPicker);
            if (fsOk) fsOk.addEventListener('click', confirmFixedSkillsPicker);

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
            if (pubF) pubF.addEventListener('click', function () { openFixedSkillsPicker('publish'); });
            if (edF) edF.addEventListener('click', function () { openFixedSkillsPicker('edit'); });

            const clrPubSkills = document.getElementById('clearPublishFixedSkillsBtn');
            const clrEditSkills = document.getElementById('clearEditFixedSkillsBtn');
            if (clrPubSkills) clrPubSkills.addEventListener('click', function () {
                if (!window.confirm('确定清除所有已选技能标签及其他补充吗？')) return;
                publishFixedTags.clear();
                publishCustomSkills = [];
                updatePublishFixedSkillsSummary();
                renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
            });
            if (clrEditSkills) clrEditSkills.addEventListener('click', function () {
                if (!window.confirm('确定清除所有已选技能标签及其他补充吗？')) return;
                editFixedTags.clear();
                editCustomSkills = [];
                updateEditFixedSkillsSummary();
                renderCompositeList(editCustomSkillList, editCustomSkills, editCustomSkillEmpty, 'custom', 'edit');
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
            const publishForm = document.getElementById('jobPublishForm');
            const publishContent = document.querySelector('.mo-job-publish-modal .mo-publish-content');
            const navRoot = document.querySelector('.mo-job-publish-modal .mo-publish-nav');
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
                
                // 获取状态样式
                const status = item.recruitmentStatus || item.status || 'OPEN';
                const statusClass = status === 'OPEN' ? 'status-open' : 'status-closed';
                
                card.innerHTML =
                    '<div class="course-card-topline">' +
                        '<span class="job-code">' + getDisplayCode(item) + '</span>' +
                        '<span class="pill">' + moOwnerLabel(item) + '</span>' +
                        '<span class="course-status ' + statusClass + '">' + status + '</span>' +
                    '</div>' +
                    '<h4 class="course-card-title">' + (item.courseName || '未命名岗位') + '</h4>' +
                    '<p class="course-card-description">' + (item.recruitmentBrief || item.courseDescription || '暂无描述') + '</p>' +
                    '<div class="job-tags">' + toTags(item).map(function (tag) { return '<span class="pill">' + tag + '</span>'; }).join('') + '</div>' +
                    '<div class="course-meta-stack">' +
                        '<div class="course-meta-item">' +
                            '<span class="course-meta-label">学期</span>' +
                            '<strong>' + (item.semester || '--') + '</strong>' +
                        '</div>' +
                        '<div class="course-meta-item">' +
                            '<span class="course-meta-label">校区</span>' +
                            '<strong>' + getDisplayLocation(item) + '</strong>' +
                        '</div>' +
                        '<div class="course-meta-item">' +
                            '<span class="course-meta-label">TA 招聘</span>' +
                            '<strong>' + (item.taRecruitCount || 0) + ' 人</strong>' +
                        '</div>' +
                    '</div>' +
                    '<div class="course-card-hint">' +
                        '<span>点击查看详情</span>' +
                        '<span aria-hidden="true">→</span>' +
                    '</div>';
                
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

        function publishReportValidationError(msg, sectionId, focusEl) {
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

        async function publishJob(event) {
            event.preventDefault();
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
                publishStatus.textContent = '未登录或缺少 moId，无法发布';
                return;
            }

            const taNum = taRecruitCountRaw === '' ? NaN : Number(taRecruitCountRaw);
            const deadlineInput = document.getElementById('applicationDeadlineInput');
            const pubDl = readLocalApplicationDeadline(deadlineInput);

            if (!courseNameInput) {
                publishReportValidationError('请填写岗位/课程名称。', 'basic-info', document.getElementById('courseNameInput'));
                return;
            }
            if (!courseCodeInput) {
                publishReportValidationError('请填写课程编号。', 'basic-info', document.getElementById('courseCodeInput'));
                return;
            }
            if (!recruitmentStatusInput) {
                publishReportValidationError('请选择招聘状态（OPEN 或 CLOSED）。', 'basic-info', document.getElementById('recruitmentStatusOpen'));
                return;
            }
            if (!semesterInput) {
                publishReportValidationError('请选择学期类型：Spring 或 Fall（不可为「学期」占位项）。', 'basic-info', document.getElementById('semesterTermInput'));
                return;
            }
            if (Number.isNaN(taNum) || taNum < 1) {
                publishReportValidationError('请填写 TA 招聘人数（≥1 的整数）。', 'basic-info', document.getElementById('taRecruitCountInput'));
                return;
            }
            if (!campusRaw) {
                publishReportValidationError('请选择校区（Main 或 Shahe）。', 'basic-info', document.getElementById('campusInput'));
                return;
            }
            if (!pubDl.ok) {
                publishReportValidationError(pubDl.msg, 'basic-info', deadlineInput);
                return;
            }
            if (!courseDescInput) {
                publishReportValidationError('请填写课程介绍。', 'basic-info', document.getElementById('courseDescInput'));
                return;
            }
            if (fixedSkills.length === 0) {
                publishReportValidationError('请至少选择一项技能标签（可点击「＋ 添加技能标签」）。', 'required-skills', document.getElementById('openPublishFixedSkillsBtn'));
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
                resetPublishFormUi();
                if (typeof app.closeAllModals === 'function') app.closeAllModals();
                await loadJobs();
            } catch (err) {
                publishStatus.textContent = (err && err.message) ? err.message : '发布失败';
            }
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

            setVal('editCourseDescInput', item.courseDescription || '');
            setVal('editRecruitmentBriefInput', item.recruitmentBrief || '');
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

            if (!courseNameVal || !recruitmentStatusVal || !courseDescVal || fixedSkills.length === 0
                || !campusRaw || Number.isNaN(taNum) || taNum < 1) {
                if (courseEditStatus) courseEditStatus.textContent = '请填写课程名、招聘状态、校区、TA 招聘人数（≥1）、至少一项技能标签、岗位描述';
                return;
            }
            if (!semesterVal) {
                if (courseEditStatus) courseEditStatus.textContent = '请选择学期类型：Spring 或 Fall（不可为「（请选择学期）」）';
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
            const editDl = readLocalApplicationDeadline(document.getElementById('editApplicationDeadlineInput'));
            if (!editDl.ok) {
                if (courseEditStatus) courseEditStatus.textContent = editDl.msg;
                return;
            }
            body.applicationDeadline = editDl.iso || '';
            if (recruitmentBriefRaw) body.recruitmentBrief = recruitmentBriefRaw;

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

        if (publishForm) publishForm.addEventListener('submit', publishJob);
        if (openJobPublishModalBtn && typeof app.openModal === 'function') {
            openJobPublishModalBtn.addEventListener('click', function () {
                app.openModal('job-publish');
                if (publishStatus) publishStatus.textContent = '';
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
        refreshJobsBtn.addEventListener('click', loadJobs);

        buildWeekGrid(moTeachingWeeksPickerGrid, 'moTwPick', 'tw', false);
        buildWeekGrid(moAssessmentWeeksGrid, 'moAssessmentWeek', 'aw', true);

        bindCompositeModals();
        bindPickerModals();
        bindPublishNav();
        initWeekButtons();

        populateSemesterYearSelect(document.getElementById('semesterYearInput'), null);
        populateSemesterYearSelect(document.getElementById('editSemesterYearInput'), null);

        loadSkillTagsFromApi().then(function () {
            updatePublishTeachingWeeksSummary();
            updateEditTeachingWeeksSummary();
            updatePublishFixedSkillsSummary();
            updateEditFixedSkillsSummary();
            renderCompositeList(publishAssessmentList, publishAssessments, publishAssessmentEmpty, 'assessment', 'publish');
            renderCompositeList(publishCustomSkillList, publishCustomSkills, publishCustomSkillEmpty, 'custom', 'publish');
        });

        loadJobs();
    };
})();