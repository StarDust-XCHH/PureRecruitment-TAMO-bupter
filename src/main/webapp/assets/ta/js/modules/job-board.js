(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.jobBoard = function initJobBoardModule(app) {
        const jobSearchInput = document.getElementById('jobSearchInput');
        const jobPagination = document.getElementById('jobPagination');
        const jobsRoute = document.getElementById('route-jobs');
        const jobsHallHeading = document.getElementById('jobsHallHeading');
        const jobBoard = document.getElementById('jobBoard');

        const JOBS_PER_PAGE = 6;
        const JOBS_SCROLL_OFFSET = 24;
        const STATUS_ROUTE_KEY = 'status';

        let courseJobCards = [];
        let courseDetailState = {};
        let activeCourseCode = null;
        let currentJobsPage = '1';
        let isJobFetching = false;
        let hasLoadedJobs = false;

        function t(zh, en) {
            return typeof app.t === 'function' ? app.t(zh, en) : zh;
        }

        function getAppliedCourseCodes() {
            if (typeof app.getAppliedCourseCodes === 'function') {
                return app.getAppliedCourseCodes();
            }
            if (Array.isArray(app.appliedCourseCodes)) {
                return app.appliedCourseCodes.slice();
            }
            return [];
        }

        function isAppliedCourse(courseCode) {
            const normalized = String(courseCode || '').trim().toUpperCase();
            if (!normalized) return false;
            return getAppliedCourseCodes().some((code) => String(code || '').trim().toUpperCase() === normalized);
        }

        function scrollJobsBoardIntoView() {
            const target = jobsHallHeading || jobBoard || jobsRoute;
            if (!target) return;
            const targetRect = target.getBoundingClientRect();
            const targetTop = Math.max(0, window.scrollY + targetRect.top - JOBS_SCROLL_OFFSET);
            window.scrollTo({ top: targetTop, behavior: 'smooth' });
        }

        function formatFileSize(bytes) {
            const size = Number(bytes);
            if (!Number.isFinite(size) || size <= 0) return '--';
            if (size >= 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB';
            if (size >= 1024) return Math.round(size / 1024) + ' KB';
            return size + ' B';
        }

        function normalizeTaWorkContents(contents) {
            if (!Array.isArray(contents)) return [t('待确定', 'To be confirmed')];
            const normalizedContents = contents
                .map((content) => typeof content === 'string' ? content.trim() : '')
                .filter((content) => content);
            return normalizedContents.length > 0 ? normalizedContents : [t('待确定', 'To be confirmed')];
        }

        function syncApplyModal(course) {
            if (!course) return;

            const applyCourseCode = document.getElementById('jobApplyCourseCode');
            const applyCourseName = document.getElementById('jobApplyCourseName');
            const applyCourseMo = document.getElementById('jobApplyCourseMo');
            const applyCourseSummary = document.getElementById('jobApplyCourseSummary');
            const resumeInput = document.getElementById('jobResumeFileInput');
            const resumeMeta = document.getElementById('jobResumeFileMeta');
            const resumeFileName = document.getElementById('jobResumeFileName');
            const resumeFileSize = document.getElementById('jobResumeFileSize');
            const submitBtn = document.getElementById('jobResumeSubmitBtn');
            const aiOptimizeBtn = document.getElementById('jobResumeAiOptimizeBtn');
            const resumeTrigger = document.querySelector('.resume-upload-trigger');

            if (applyCourseCode) applyCourseCode.textContent = course.code;
            if (applyCourseName) applyCourseName.textContent = course.name;
            if (applyCourseMo) applyCourseMo.textContent = course.mo;
            if (applyCourseSummary) {
                applyCourseSummary.textContent = t(
                    '请上传用于申请“' + course.name + '”的最新简历，建议突出与课程标签、答疑支持和课堂管理相关的经历。',
                    'Please upload your latest resume for "' + course.name + '" and highlight experience related to course topics, Q&A support, and class management.'
                );
            }

            if (resumeInput) {
                resumeInput.value = '';
                resumeInput.dataset.courseCode = course.code;
                resumeInput.dataset.courseName = course.name;
            }
            if (resumeMeta) resumeMeta.hidden = true;
            if (resumeFileName) resumeFileName.textContent = t('未选择', 'Not selected');
            if (resumeFileSize) resumeFileSize.textContent = '--';
            if (resumeTrigger) resumeTrigger.textContent = t('选择简历文件', 'Choose Resume');
            if (submitBtn) {
                submitBtn.textContent = t('提交课程申请', 'Submit Application');
                submitBtn.dataset.courseCode = course.code;
                submitBtn.disabled = false;
            }
            if (aiOptimizeBtn) {
                aiOptimizeBtn.textContent = t('AI优化后再发送', 'Optimize with AI Before Sending');
                aiOptimizeBtn.dataset.courseCode = course.code;
                aiOptimizeBtn.dataset.courseName = course.name;
                aiOptimizeBtn.disabled = false;
            }
        }

        function renderCourseDetail(courseCode) {
            const course = courseDetailState[courseCode];
            if (!course) return;
            activeCourseCode = courseCode;

            const jobDetailCode = document.getElementById('jobDetailCode');
            const jobDetailName = document.getElementById('jobDetailName');
            const jobDetailMo = document.getElementById('jobDetailMo');
            const jobDetailStudentCount = document.getElementById('jobDetailStudentCount');
            const jobDetailDescription = document.getElementById('jobDetailDescription');
            const jobDetailTags = document.getElementById('jobDetailTags');
            const jobDetailChecklist = document.getElementById('jobDetailChecklist');
            const jobDetailSuggestion = document.getElementById('jobDetailSuggestion');
            const jobDetailApplyBtn = document.getElementById('jobDetailApplyBtn');

            if (jobDetailCode) jobDetailCode.textContent = course.code;
            if (jobDetailName) jobDetailName.textContent = course.name;
            if (jobDetailMo) jobDetailMo.textContent = course.mo;
            if (jobDetailStudentCount) jobDetailStudentCount.textContent = course.studentCountText;
            if (jobDetailDescription) jobDetailDescription.textContent = course.description;
            if (jobDetailSuggestion) jobDetailSuggestion.textContent = course.suggestion;

            if (jobDetailTags) {
                jobDetailTags.innerHTML = '';
                (course.tags || []).forEach((tag) => {
                    const tagElement = document.createElement('span');
                    tagElement.className = 'skill-tag';
                    tagElement.textContent = tag;
                    jobDetailTags.appendChild(tagElement);
                });
            }

            if (jobDetailChecklist) {
                jobDetailChecklist.innerHTML = '';
                normalizeTaWorkContents(course.taWorkContents).forEach((item) => {
                    const li = document.createElement('li');
                    li.textContent = item;
                    jobDetailChecklist.appendChild(li);
                });
            }

            if (jobDetailApplyBtn) {
                jobDetailApplyBtn.classList.remove('applied');
                jobDetailApplyBtn.textContent = jobDetailApplyBtn.dataset.applyLabelDefault || t('申请该课程', 'Apply for this course');
                jobDetailApplyBtn.dataset.courseCode = course.code;
                jobDetailApplyBtn.disabled = false;
            }

            syncApplyModal(course);
        }

        function syncCourseJobCards() {
            const currentJobBoard = document.getElementById('jobBoard');
            courseJobCards = currentJobBoard
                ? Array.from(currentJobBoard.querySelectorAll('[data-job-detail-card]'))
                : [];
            return courseJobCards;
        }

        function renderJobsBoardState(message, stateClassName) {
            if (!jobBoard) return;
            jobBoard.innerHTML = '<div class="job-board-state ' + stateClassName + '">' + message + '</div>';
            courseJobCards = [];
        }

        function bindCardEvents() {
            const cards = syncCourseJobCards();
            if (!cards.length) return;

            cards.forEach((card) => {
                const openCardDetail = (event) => {
                    event?.preventDefault();
                    event?.stopPropagation();
                    const courseCode = card.dataset.courseCode;
                    renderCourseDetail(courseCode);
                    if (typeof app.openModal === 'function') app.openModal('course-detail');
                };

                card.addEventListener('click', openCardDetail);
                card.addEventListener('keydown', (event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        openCardDetail(event);
                    }
                });
            });
        }

        function getCardMatchesKeyword(card, keyword) {
            if (!keyword) return true;
            return card.textContent.toLowerCase().includes(keyword);
        }

        function getSortedJobItems(items) {
            const normalizedItems = Array.isArray(items) ? items.slice() : [];
            return normalizedItems.sort((leftItem, rightItem) => {
                const leftStatus = typeof leftItem?.recruitmentStatus === 'string'
                    ? leftItem.recruitmentStatus.trim().toUpperCase()
                    : '';
                const rightStatus = typeof rightItem?.recruitmentStatus === 'string'
                    ? rightItem.recruitmentStatus.trim().toUpperCase()
                    : '';
                const leftPriority = (leftStatus === 'CLOSE' || leftStatus === 'CLOSED') ? 1 : 0;
                const rightPriority = (rightStatus === 'CLOSE' || rightStatus === 'CLOSED') ? 1 : 0;
                if (leftPriority !== rightPriority) {
                    return leftPriority - rightPriority;
                }
                return 0;
            });
        }

        function getMatchedCards(keyword) {
            const matchedCards = Array.from(courseJobCards).filter((card) => getCardMatchesKeyword(card, keyword));
            return matchedCards.sort((leftCard, rightCard) => {
                const leftPriority = Number(leftCard.dataset.statusPriority || '1');
                const rightPriority = Number(rightCard.dataset.statusPriority || '1');
                if (leftPriority !== rightPriority) {
                    return leftPriority - rightPriority;
                }

                const leftOriginalOrder = Number(leftCard.dataset.originalOrder || '0');
                const rightOriginalOrder = Number(rightCard.dataset.originalOrder || '0');
                return leftOriginalOrder - rightOriginalOrder;
            });
        }

        function getTotalJobPages(matchedCards) {
            return Math.max(1, Math.ceil(matchedCards.length / JOBS_PER_PAGE));
        }

        function createJobPaginationButton(page, isActive) {
            const btn = document.createElement('button');
            btn.className = 'job-page-btn' + (isActive ? ' active' : '');
            btn.type = 'button';
            btn.dataset.jobPage = String(page);
            btn.textContent = String(page);
            btn.setAttribute('aria-current', isActive ? 'page' : 'false');

            btn.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                currentJobsPage = String(page);
                if (jobsRoute) jobsRoute.style.minHeight = jobsRoute.offsetHeight + 'px';
                renderJobsBoard({ shouldFocusBoard: true });
                setTimeout(() => {
                    if (jobsRoute) jobsRoute.style.minHeight = '';
                }, 400);
            });
            return btn;
        }

        function renderJobPagination(totalPages) {
            if (!jobPagination) return;
            jobPagination.innerHTML = '';
            if (totalPages <= 1) {
                jobPagination.hidden = true;
                return;
            }
            jobPagination.hidden = false;
            for (let page = 1; page <= totalPages; page += 1) {
                jobPagination.appendChild(createJobPaginationButton(page, String(page) === currentJobsPage));
            }
        }

        function renderJobsBoard(options) {
            const shouldFocusBoard = !!options?.shouldFocusBoard;
            const staleState = jobBoard?.querySelector('.job-board-state');
            if (staleState) staleState.remove();

            syncCourseJobCards();

            const keyword = jobSearchInput?.value.trim().toLowerCase() || '';
            const matchedCards = getMatchedCards(keyword);

            if (!courseJobCards.length) {
                renderJobPagination(0);
                if (hasLoadedJobs) {
                    renderJobsBoardState(t('当前没有可展示的课程卡片。', 'No course cards are currently available.'), 'job-board-state-empty');
                }
                return;
            }

            if (!matchedCards.length) {
                courseJobCards.forEach((card) => {
                    card.hidden = true;
                });
                renderJobPagination(0);
                if (jobBoard) {
                    const emptyState = document.createElement('div');
                    emptyState.className = 'job-board-state job-board-state-empty';
                    emptyState.textContent = t('没有匹配的课程，请尝试其他关键词。', 'No matching courses found. Try other keywords.');
                    jobBoard.appendChild(emptyState);
                }
                return;
            }

            const totalPages = getTotalJobPages(matchedCards);
            if (Number(currentJobsPage) > totalPages) currentJobsPage = '1';

            const currentPageIndex = Number(currentJobsPage) - 1;
            const startIndex = currentPageIndex * JOBS_PER_PAGE;
            const endIndex = startIndex + JOBS_PER_PAGE;
            const visibleCards = new Set(matchedCards.slice(startIndex, endIndex));

            if (jobBoard) {
                matchedCards.forEach((card) => jobBoard.appendChild(card));
            }

            courseJobCards.forEach((card) => {
                card.hidden = !visibleCards.has(card);
            });

            renderJobPagination(totalPages);
            if (shouldFocusBoard) scrollJobsBoardIntoView();
        }

        function resolveJobBoardApiUrl() {
            return '../../api/ta/jobs';
        }

        function resolveJobApplicationApiUrl() {
            return '../../api/ta/applications';
        }

        function getCurrentTaId() {
            const userData = typeof app.getUserData === 'function' ? app.getUserData() : null;
            return String(userData?.taId || userData?.id || '').trim();
        }

        function isAllowedResumeFile(file) {
            if (!file) return false;
            const name = String(file.name || '').toLowerCase();
            return name.endsWith('.pdf') || name.endsWith('.doc') || name.endsWith('.docx');
        }

        function openStatusRoute(afterNavigate) {
            if (typeof app.navigateToRoute === 'function') {
                app.navigateToRoute(STATUS_ROUTE_KEY, {
                    smooth: true,
                    afterNavigate: afterNavigate
                });
                return;
            }

            const statusNav = document.querySelector('.nav-item[data-route="' + STATUS_ROUTE_KEY + '"]');
            if (statusNav) {
                statusNav.click();
                if (typeof afterNavigate === 'function') {
                    window.setTimeout(() => afterNavigate(document.getElementById('route-' + STATUS_ROUTE_KEY)), 380);
                }
                return;
            }

            const statusRoute = document.getElementById('route-status');
            if (statusRoute) {
                document.querySelectorAll('.route').forEach((routeElement) => {
                    routeElement.classList.toggle('active', routeElement === statusRoute);
                });
                statusRoute.classList.add('active');
                statusRoute.scrollIntoView({ behavior: 'smooth', block: 'start' });
                if (typeof afterNavigate === 'function') {
                    window.setTimeout(() => afterNavigate(statusRoute), 380);
                }
            }
        }

        function focusSubmittedApplication(result, courseCode) {
            if (typeof app.setStatusFocus !== 'function') return;
            app.setStatusFocus({
                courseCode: courseCode,
                applicationId: result?.applicationId || result?.id || '',
                pulseCount: 3,
                source: 'resume-submit'
            });
        }

        async function submitCourseApplication(courseCode, selectedFile) {
            const taId = getCurrentTaId();
            if (!taId) throw new Error(t('当前未获取到 TA 身份，请重新登录后再试。', 'TA identity is missing. Please log in again.'));
            if (!courseCode) throw new Error(t('缺少课程编号，无法提交申请。', 'Missing course code. Unable to submit the application.'));
            if (!selectedFile) throw new Error(t('请先选择简历文件。', 'Please choose a resume file first.'));
            if (!isAllowedResumeFile(selectedFile)) throw new Error(t('仅支持上传 PDF / DOC / DOCX 简历文件。', 'Only PDF / DOC / DOCX resume files are supported.'));
            if (selectedFile.size > 10 * 1024 * 1024) throw new Error(t('简历文件大小不能超过 10MB。', 'The resume file must be smaller than 10MB.'));

            const formData = new FormData();
            formData.append('taId', taId);
            formData.append('courseCode', courseCode);
            formData.append('resumeFile', selectedFile, selectedFile.name);

            const response = await fetch(resolveJobApplicationApiUrl(), {
                method: 'POST',
                body: formData
            });
            const payload = await response.json();
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || (t('申请提交失败，status=', 'Application submission failed, status=') + response.status));
            }
            return payload.data || {};
        }

        async function fetchAndRefreshJobs() {
            if (isJobFetching) return;

            const btn = document.getElementById('refreshJobsBtn');
            const btnIcon = btn ? btn.querySelector('.refresh-icon') : null;
            const btnText = btn ? btn.querySelector('.refresh-text') : null;
            const requestUrl = resolveJobBoardApiUrl();

            try {
                isJobFetching = true;
                if (!hasLoadedJobs) {
                    renderJobsBoardState(t('课程列表加载中...', 'Loading courses...'), 'job-board-state-loading');
                }
                if (btn) btn.disabled = true;
                if (btnIcon) btnIcon.classList.add('spinning');
                if (btnText) btnText.textContent = t('更新中...', 'Refreshing...');

                console.log('[JOB-BOARD][fetch] 请求地址=', requestUrl);

                const response = await fetch(requestUrl, {
                    method: 'GET',
                    headers: { Accept: 'application/json' }
                });

                if (!response.ok) throw new Error(t('网络请求失败，status=', 'Network request failed, status=') + response.status);

                const data = await response.json();

                if (data.schema === 'error' || !data.items) {
                    console.error('后端未返回正确数据');
                    return;
                }

                const appliedCourseCodes = getAppliedCourseCodes();
                const appliedCourseSet = new Set(appliedCourseCodes.map((code) => String(code || '').trim().toUpperCase()).filter(Boolean));
                const visibleItems = getSortedJobItems(data.items).filter((item) => !appliedCourseSet.has(String(item.courseCode || '').trim().toUpperCase()));

                const countBadge = document.getElementById('openCoursesCount');
                if (countBadge) {
                    countBadge.textContent = t('开放课程 ', 'Open Courses ') + visibleItems.length;
                }

                courseDetailState = {};
                const newCardsHTML = [];

                visibleItems.forEach(function (item) {
                    const keywordTags = Array.isArray(item.keywordTags) ? item.keywordTags : [];
                    const checklist = Array.isArray(item.checklist) ? item.checklist : [];
                    const taWorkContents = normalizeTaWorkContents(item.taWorkContents);
                    const studentCount = Number.isFinite(Number(item.studentCount)) ? Number(item.studentCount) : 0;
                    const studentCountText = studentCount > 0 ? studentCount + t(' 人', ' students') : t('待确认', 'To be confirmed');
                    const courseMoName = item.ownerMoName || item.moName || t('待分配', 'To be assigned');
                    const primaryTagText = keywordTags.length > 0 ? keywordTags[0] : t('暂无标签', 'No tags');
                    const descriptionText = item.recruitmentBrief || item.courseDescription || t('暂无课程简介', 'No course description yet');
                    const suggestionText = item.suggestion || t('建议结合自身技能标签和课程方向进行投递。', 'Consider applying based on your skill tags and course interests.');
                    const recruitmentStatus = typeof item.recruitmentStatus === 'string' && item.recruitmentStatus.trim()
                        ? item.recruitmentStatus.trim().toUpperCase()
                        : t('待确定', 'TBD');
                    const statusPriority = (recruitmentStatus === 'CLOSE' || recruitmentStatus === 'CLOSED') ? 1 : 0;

                    courseDetailState[item.courseCode] = {
                        code: item.courseCode,
                        name: item.courseName,
                        mo: courseMoName,
                        studentCount: studentCount,
                        studentCountText: studentCountText,
                        description: descriptionText,
                        tags: keywordTags,
                        checklist: checklist,
                        taWorkContents: taWorkContents,
                        suggestion: suggestionText,
                        recruitmentStatus: recruitmentStatus
                    };

                    const tagHTML = keywordTags.slice(0, 3).map(function (tag) {
                        return '<span class="skill-tag">' + tag + '</span>';
                    }).join('');

                    newCardsHTML.push(
                        '<div class="job-card course-job-card" tabindex="0" role="button" aria-label="' + t('查看 ', 'View ') + item.courseName + t(' 详情', ' details') + '" data-job-detail-card data-course-code="' + item.courseCode + '" data-status-priority="' + statusPriority + '" data-original-order="' + newCardsHTML.length + '">' +
                        '<div class="course-card-topline">' +
                        '<span class="job-code">' + item.courseCode + '</span>' +
                        '<span class="course-status-badge course-status-badge-' + recruitmentStatus.toLowerCase() + '">' + recruitmentStatus + '</span>' +
                        '</div>' +
                        '<h4>' + item.courseName + '</h4>' +
                        '<p class="course-card-summary">' + (item.courseDescription || t('暂无课程简介', 'No course description yet')) + '</p>' +
                        '<div class="job-tags">' +
                        tagHTML +
                        '</div>' +
                        '<div class="course-meta-stack">' +
                        '<div class="course-meta-item">' +
                        '<span class="course-meta-label">' + t('开课MO', 'Course MO') + '</span>' +
                        '<strong>' + courseMoName + '</strong>' +
                        '</div>' +
                        '<div class="course-meta-item">' +
                        '<span class="course-meta-label">' + t('课程标签', 'Course Tag') + '</span>' +
                        '<strong>' + primaryTagText + '</strong>' +
                        '</div>' +
                        '<div class="course-meta-item">' +
                        '<span class="course-meta-label">' + t('学生人数', 'Students') + '</span>' +
                        '<strong>' + studentCountText + '</strong>' +
                        '</div>' +
                        '</div>' +
                        '<div class="course-card-hint">' +
                        '<span>' + t('点击查看详情', 'Click to view details') + '</span>' +
                        '<span aria-hidden="true">→</span>' +
                        '</div>' +
                        '</div>'
                    );
                });

                const currentJobBoard = document.getElementById('jobBoard');
                if (currentJobBoard) {
                    currentJobBoard.innerHTML = newCardsHTML.join('');
                }

                activeCourseCode = null;
                hasLoadedJobs = true;
                bindCardEvents();
                currentJobsPage = '1';
                renderJobsBoard();
                if (!visibleItems.length) {
                    renderJobsBoardState(t('当前职位大厅没有可再次申请的课程，已申请课程可在申请状态中查看。', 'There are no more courses available to apply for. Applied courses can be viewed in Application Status.'), 'job-board-state-empty');
                }
            } catch (error) {
                hasLoadedJobs = true;
                renderJobsBoardState(t('课程列表加载失败，请点击“刷新列表”重试。', 'Failed to load courses. Please click “Refresh” to try again.'), 'job-board-state-error');
                renderJobPagination(0);
                console.error('拉取岗位数据异常:', error);
            } finally {
                isJobFetching = false;
                if (btn) btn.disabled = false;
                if (btnIcon) btnIcon.classList.remove('spinning');
                if (btnText) btnText.textContent = t('刷新列表', 'Refresh');
            }
        }

        jobSearchInput?.addEventListener('input', () => {
            if (!hasLoadedJobs) return;
            currentJobsPage = '1';
            renderJobsBoard();
        });

        const refreshJobsBtn = document.getElementById('refreshJobsBtn');
        if (refreshJobsBtn) {
            refreshJobsBtn.addEventListener('click', fetchAndRefreshJobs);
        }

        const jobDetailApplyBtn = document.getElementById('jobDetailApplyBtn');
        jobDetailApplyBtn?.addEventListener('click', (event) => {
            event.preventDefault();
            const courseCode = jobDetailApplyBtn.dataset.courseCode || activeCourseCode;
            const course = courseDetailState[courseCode];
            if (!course || isAppliedCourse(courseCode)) return;
            syncApplyModal(course);
            if (typeof app.openModal === 'function') app.openModal('course-apply');
        });

        const jobResumeFileInput = document.getElementById('jobResumeFileInput');
        jobResumeFileInput?.addEventListener('change', () => {
            const file = jobResumeFileInput.files && jobResumeFileInput.files[0];
            const resumeMeta = document.getElementById('jobResumeFileMeta');
            const resumeFileName = document.getElementById('jobResumeFileName');
            const resumeFileSize = document.getElementById('jobResumeFileSize');
            const submitBtn = document.getElementById('jobResumeSubmitBtn');
            const aiOptimizeBtn = document.getElementById('jobResumeAiOptimizeBtn');
            const resumeTrigger = document.querySelector('.resume-upload-trigger');

            if (!file) {
                if (resumeMeta) resumeMeta.hidden = true;
                if (resumeFileName) resumeFileName.textContent = t('未选择', 'Not selected');
                if (resumeFileSize) resumeFileSize.textContent = '--';
                if (resumeTrigger) resumeTrigger.textContent = t('选择简历文件', 'Choose Resume');
                if (submitBtn) submitBtn.textContent = t('提交课程申请', 'Submit Application');
                if (aiOptimizeBtn) aiOptimizeBtn.disabled = false;
                return;
            }

            if (resumeMeta) resumeMeta.hidden = false;
            if (resumeFileName) resumeFileName.textContent = file.name;
            if (resumeFileSize) resumeFileSize.textContent = formatFileSize(file.size);
            if (resumeTrigger) resumeTrigger.textContent = t('重新选择', 'Choose Again');
            if (submitBtn) submitBtn.textContent = t('提交课程申请 · 已就绪', 'Submit Application · Ready');
            if (aiOptimizeBtn) aiOptimizeBtn.textContent = t('AI优化后再发送', 'Optimize with AI Before Sending');
        });

        const jobResumeSubmitBtn = document.getElementById('jobResumeSubmitBtn');
        jobResumeSubmitBtn?.addEventListener('click', async (event) => {
            event.preventDefault();
            const resumeInput = document.getElementById('jobResumeFileInput');
            const selectedFile = resumeInput?.files && resumeInput.files[0];
            const courseCode = (jobResumeSubmitBtn.dataset.courseCode || resumeInput?.dataset.courseCode || activeCourseCode || '').trim();
            if (!selectedFile) {
                resumeInput?.click();
                return;
            }

            const defaultText = t('提交课程申请', 'Submit Application');
            jobResumeSubmitBtn.disabled = true;
            jobResumeSubmitBtn.textContent = t('提交中...', 'Submitting...');

            try {
                const result = await submitCourseApplication(courseCode, selectedFile);
                jobResumeSubmitBtn.textContent = t('已提交', 'Submitted');

                if (jobDetailApplyBtn) {
                    jobDetailApplyBtn.classList.add('applied');
                    jobDetailApplyBtn.textContent = t('已申请', 'Applied');
                    jobDetailApplyBtn.disabled = true;
                }

                const resumeTrigger = document.querySelector('.resume-upload-trigger');
                if (resumeTrigger) resumeTrigger.textContent = t('已上传', 'Uploaded');
                if (typeof app.setAppliedCourseCodes === 'function') {
                    app.setAppliedCourseCodes(getAppliedCourseCodes().concat([courseCode]));
                }
                focusSubmittedApplication(result, courseCode);
                if (typeof app.dismissModalStack === 'function') {
                    app.dismissModalStack();
                } else if (typeof app.closeAllModals === 'function') {
                    app.closeAllModals();
                } else if (typeof app.closeModal === 'function') {
                    app.closeModal('course-apply');
                    app.closeModal('course-detail');
                }
                if (typeof app.loadStatusData === 'function') {
                    openStatusRoute(() => {
                        window.setTimeout(() => {
                            app.loadStatusData({ consumeFocus: true });
                        }, 80);
                    });
                } else {
                    openStatusRoute();
                }
                fetchAndRefreshJobs();
                console.log('[TA-APPLICATION] submit success', result);
            } catch (error) {
                console.error('[TA-APPLICATION] submit failed', error);
                jobResumeSubmitBtn.disabled = false;
                jobResumeSubmitBtn.textContent = defaultText;
                window.alert(error.message || t('申请提交失败，请稍后重试。', 'Application submission failed. Please try again later.'));
            }
        });

        const jobResumeAiOptimizeBtn = document.getElementById('jobResumeAiOptimizeBtn');
        jobResumeAiOptimizeBtn?.addEventListener('click', async (event) => {
            event.preventDefault();
            const resumeInput = document.getElementById('jobResumeFileInput');
            const selectedFile = resumeInput?.files && resumeInput.files[0];
            const courseCode = (jobResumeAiOptimizeBtn.dataset.courseCode || resumeInput?.dataset.courseCode || activeCourseCode || '').trim();
            const courseName = (jobResumeAiOptimizeBtn.dataset.courseName || resumeInput?.dataset.courseName || '').trim();
            if (!selectedFile) {
                resumeInput?.click();
                return;
            }
            if (!isAllowedResumeFile(selectedFile)) {
                window.alert(t('仅支持上传 PDF / DOC / DOCX 简历文件。', 'Only PDF / DOC / DOCX resume files are supported.'));
                return;
            }
            if (typeof app.openAiAssistantWithAttachment !== 'function') {
                window.alert(t('AI 助理模块尚未完成初始化，请刷新页面后重试。', 'The AI assistant is not initialized yet. Please refresh and try again.'));
                return;
            }

            jobResumeAiOptimizeBtn.disabled = true;
            jobResumeAiOptimizeBtn.textContent = t('载入中...', 'Loading...');
            try {
                await app.openAiAssistantWithAttachment({
                    file: selectedFile,
                    courseCode: courseCode,
                    courseName: courseName,
                    sourcePath: courseCode ? 'course-apply/' + courseCode + '/' + selectedFile.name : selectedFile.name,
                    applicationId: ''
                });
            } catch (error) {
                console.error('[TA-AI] preload from course apply failed', error);
                window.alert(error.message || t('载入 AI 助理失败，请稍后重试。', 'Failed to load the AI assistant. Please try again later.'));
            } finally {
                jobResumeAiOptimizeBtn.disabled = false;
                jobResumeAiOptimizeBtn.textContent = t('AI优化后再发送', 'Optimize with AI Before Sending');
            }
        });

        renderJobsBoardState(t('课程列表加载中...', 'Loading courses...'), 'job-board-state-loading');
        renderJobPagination(0);
        fetchAndRefreshJobs();

        app.activeCourseCode = () => activeCourseCode;
        app.refreshJobBoard = fetchAndRefreshJobs;
        Object.defineProperty(app, 'jobDetailApplyBtn', {
            get: () => document.getElementById('jobDetailApplyBtn')
        });
    };
})();
