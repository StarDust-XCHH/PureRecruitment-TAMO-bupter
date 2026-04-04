(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.jobBoard = function initJobBoardModule(app) {
        // 仅保留最外层常驻容器的缓存（如果它们也会动态重建，建议也放进方法内实时查询）
        const jobSearchInput = document.getElementById('jobSearchInput');
        const jobPagination = document.getElementById('jobPagination');
        const jobsRoute = document.getElementById('route-jobs');
        const jobsHallHeading = document.getElementById('jobsHallHeading');
        const jobBoard = document.getElementById('jobBoard');

        const JOBS_PER_PAGE = 6;
        const JOBS_SCROLL_OFFSET = 24;

        let courseJobCards = []; // 改为用数组/NodeList维护当前挂载的卡片
        let courseDetailState = {};
        let activeCourseCode = null;
        let currentJobsPage = '1';
        let isJobFetching = false;

        function scrollJobsBoardIntoView() {
            const target = jobsHallHeading || jobBoard || jobsRoute;
            if (!target) return;
            const targetRect = target.getBoundingClientRect();
            const targetTop = Math.max(0, window.scrollY + targetRect.top - JOBS_SCROLL_OFFSET);
            window.scrollTo({ top: targetTop, behavior: 'smooth' });
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
                (course.checklist || []).forEach((item) => {
                    const li = document.createElement('li');
                    li.textContent = item;
                    jobDetailChecklist.appendChild(li);
                });
            }

            if (jobDetailApplyBtn) {
                jobDetailApplyBtn.classList.remove('applied');
                jobDetailApplyBtn.textContent = jobDetailApplyBtn.dataset.applyLabelDefault || 'Apply';
                jobDetailApplyBtn.dataset.courseCode = course.code;
            }
        }

        function bindCardEvents() {
            const currentJobBoard = document.getElementById('jobBoard');
            if (!currentJobBoard) return;

            courseJobCards = currentJobBoard.querySelectorAll('[data-job-detail-card]');

            courseJobCards.forEach((card) => {
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

        function getMatchedCards(keyword) {
            return Array.from(courseJobCards).filter((card) => getCardMatchesKeyword(card, keyword));
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
            const keyword = jobSearchInput?.value.trim().toLowerCase() || '';
            const matchedCards = getMatchedCards(keyword);
            const totalPages = getTotalJobPages(matchedCards);

            if (Number(currentJobsPage) > totalPages) currentJobsPage = '1';

            const currentPageIndex = Number(currentJobsPage) - 1;
            const startIndex = currentPageIndex * JOBS_PER_PAGE;
            const endIndex = startIndex + JOBS_PER_PAGE;
            const visibleCards = new Set(matchedCards.slice(startIndex, endIndex));

            courseJobCards.forEach((card) => {
                card.hidden = !visibleCards.has(card);
            });
            renderJobPagination(totalPages);
            if (shouldFocusBoard) scrollJobsBoardIntoView();
        }

        function resolveJobBoardApiUrl() {
            return '../../api/ta/jobs';
        }

        async function fetchAndRefreshJobs() {
            if (isJobFetching) return;

            const btn = document.getElementById('refreshJobsBtn');
            const btnIcon = btn ? btn.querySelector('.refresh-icon') : null;
            const btnText = btn ? btn.querySelector('.refresh-text') : null;
            const requestUrl = resolveJobBoardApiUrl();

            try {
                isJobFetching = true;
                if (btn) btn.disabled = true;
                if (btnIcon) btnIcon.classList.add('spinning');
                if (btnText) btnText.textContent = '更新中...';

                console.log('[JOB-BOARD][fetch] 请求地址=', requestUrl);

                const response = await fetch(requestUrl, {
                    method: 'GET',
                    headers: { Accept: 'application/json' }
                });

                if (!response.ok) throw new Error('网络请求失败，status=' + response.status);

                const data = await response.json();

                if (data.schema === 'error' || !data.items) {
                    console.error('后端未返回正确数据');
                    return;
                }

                const countBadge = document.getElementById('openCoursesCount');
                if (countBadge) {
                    countBadge.textContent = '开放课程 ' + data.items.length;
                }

                courseDetailState = {};
                const newCardsHTML = [];

                data.items.forEach(function (item) {
                    const keywordTags = Array.isArray(item.keywordTags) ? item.keywordTags : [];
                    const checklist = Array.isArray(item.checklist) ? item.checklist : [];
                    const studentCount = Number.isFinite(Number(item.studentCount)) ? Number(item.studentCount) : 0;
                    const studentCountText = studentCount > 0 ? studentCount + ' 人' : '待确认';
                    const courseMoName = item.ownerMoName || item.moName || '待分配';
                    const primaryTagText = keywordTags.length > 0 ? keywordTags[0] : '暂无标签';
                    const descriptionText = item.recruitmentBrief || item.courseDescription || '暂无课程简介';
                    const suggestionText = item.suggestion || '建议结合自身技能标签和课程方向进行投递。';

                    courseDetailState[item.courseCode] = {
                        code: item.courseCode,
                        name: item.courseName,
                        mo: courseMoName,
                        studentCount: studentCount,
                        studentCountText: studentCountText,
                        description: descriptionText,
                        tags: keywordTags,
                        checklist: checklist,
                        suggestion: suggestionText
                    };

                    const tagHTML = keywordTags.slice(0, 3).map(function (tag) {
                        return '<span class="skill-tag">' + tag + '</span>';
                    }).join('');

                    newCardsHTML.push(
                        '<div class="job-card course-job-card" tabindex="0" role="button" aria-label="查看 ' + item.courseName + ' 详情" data-job-detail-card data-course-code="' + item.courseCode + '">' +
                        '<div class="course-card-topline">' +
                        '<span class="job-code">' + item.courseCode + '</span>' +
                        '<span class="course-mo-badge">' + courseMoName + '</span>' +
                        '</div>' +
                        '<h4>' + item.courseName + '</h4>' +
                        '<p class="course-card-summary">' + (item.courseDescription || '暂无课程简介') + '</p>' +
                        '<div class="job-tags">' +
                        tagHTML +
                        '</div>' +
                        '<div class="course-meta-stack">' +
                        '<div class="course-meta-item">' +
                        '<span class="course-meta-label">开课MO</span>' +
                        '<strong>' + courseMoName + '</strong>' +
                        '</div>' +
                        '<div class="course-meta-item">' +
                        '<span class="course-meta-label">课程标签</span>' +
                        '<strong>' + primaryTagText + '</strong>' +
                        '</div>' +
                        '<div class="course-meta-item">' +
                        '<span class="course-meta-label">学生人数</span>' +
                        '<strong>' + studentCountText + '</strong>' +
                        '</div>' +
                        '</div>' +
                        '<div class="course-card-hint">' +
                        '<span>点击查看详情</span>' +
                        '<span aria-hidden="true">→</span>' +
                        '</div>' +
                        '</div>'
                    );
                });

                const currentJobBoard = document.getElementById('jobBoard');
                if (currentJobBoard) {
                    currentJobBoard.innerHTML = newCardsHTML.join('');
                }

                bindCardEvents();
                currentJobsPage = '1';
                renderJobsBoard();
            } catch (error) {
                console.error('拉取岗位数据异常:', error);
            } finally {
                isJobFetching = false;
                if (btn) btn.disabled = false;
                if (btnIcon) btnIcon.classList.remove('spinning');
                if (btnText) btnText.textContent = '刷新列表';
            }
        }

        jobSearchInput?.addEventListener('input', () => {
            currentJobsPage = '1';
            renderJobsBoard();
        });

        const refreshJobsBtn = document.getElementById('refreshJobsBtn');
        if (refreshJobsBtn) {
            refreshJobsBtn.addEventListener('click', fetchAndRefreshJobs);
        }

        bindCardEvents();
        renderJobsBoard();
        fetchAndRefreshJobs();

        app.activeCourseCode = () => activeCourseCode;
        Object.defineProperty(app, 'jobDetailApplyBtn', {
            get: () => document.getElementById('jobDetailApplyBtn')
        });
    };
})();
