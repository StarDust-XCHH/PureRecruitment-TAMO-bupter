(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.profile = function initProfileModule(app) {
        const skillEntry = document.getElementById('skillEntry');
        const skillsInput = document.getElementById('skillsInput');
        const fullNameInput = document.getElementById('fullName');
        const applicationIntentInput = document.getElementById('applicationIntent');
        const studentIdInput = document.getElementById('studentId');
        const contactEmailInput = document.getElementById('contactEmail');
        const bioInput = document.getElementById('bio');
        const avatarFileInput = document.getElementById('avatarFile');
        const avatarUploadHint = document.getElementById('avatarUploadHint');
        const saveProfileBtn = document.getElementById('saveProfileBtn');
        const profileSyncStatus = document.getElementById('profileSyncStatus');
        const profileLastUpdated = document.getElementById('profileLastUpdated');
        const profileAvatarBox = document.getElementById('profileAvatarBox');
        const profileAvatarPreview = document.getElementById('profileAvatarPreview');
        const avatarCropModal = document.querySelector('[data-modal="avatar-crop"]');
        const avatarCropImage = document.getElementById('avatarCropImage');
        const avatarCropStage = document.getElementById('avatarCropStage');
        const avatarCropPreviewCanvas = document.getElementById('avatarCropPreviewCanvas');
        const avatarCropZoomRange = document.getElementById('avatarCropZoomRange');
        const avatarCropHint = document.getElementById('avatarCropHint');
        const avatarCropConfirmBtn = document.getElementById('avatarCropConfirmBtn');
        const avatarCropCancelBtn = document.getElementById('avatarCropCancelBtn');
        const avatarCropCloseButtons = Array.from(document.querySelectorAll('[data-avatar-crop-close]'));
        const avatarCropModalOverlay = document.getElementById('taModalOverlay');
        const appShell = document.querySelector('.app');
        const profileFieldElements = [fullNameInput, applicationIntentInput, contactEmailInput, bioInput].filter(Boolean);
        const MAX_AVATAR_SIZE = 10 * 1024 * 1024;
        const AVATAR_CROP_OUTPUT_SIZE = 512;
        let userData = typeof app.getUserData === 'function' ? app.getUserData() : null;

        const profileState = {
            taId: (userData && (userData.taId || userData.id)) || '',
            original: null,
            current: null,
            editable: true,
            loading: false,
            saving: false,
            pendingAvatarFile: null,
            avatarChanged: false,
            avatarPreviewUrl: ''
        };

        const avatarCropState = {
            file: null,
            objectUrl: '',
            outputFileName: '',
            naturalWidth: 0,
            naturalHeight: 0,
            baseScale: 1,
            scale: 1,
            minScale: 1,
            maxScale: 3,
            offsetX: 0,
            offsetY: 0,
            dragging: false,
            dragStartX: 0,
            dragStartY: 0,
            startOffsetX: 0,
            startOffsetY: 0,
            activePointerId: null,
            isOpen: false
        };

        app.state.profile = { profileState, avatarCropState };

        function safeText(value) { return value == null ? '' : String(value).trim(); }

        function escapeHtml(value) {
            return safeText(value)
                .replaceAll('&', '&amp;')
                .replaceAll('<', '&lt;')
                .replaceAll('>', '&gt;')
                .replaceAll('"', '&quot;')
                .replaceAll("'", '&#39;');
        }

        function refreshUserData() {
            userData = typeof app.getUserData === 'function' ? app.getUserData() : userData;
            return userData;
        }

        function revokeProfilePreviewUrl() {
            if (profileState.avatarPreviewUrl && profileState.avatarPreviewUrl.startsWith('blob:')) {
                URL.revokeObjectURL(profileState.avatarPreviewUrl);
            }
            profileState.avatarPreviewUrl = '';
        }

        function revokeAvatarCropObjectUrl() {
            if (avatarCropState.objectUrl && avatarCropState.objectUrl.startsWith('blob:')) {
                URL.revokeObjectURL(avatarCropState.objectUrl);
            }
            avatarCropState.objectUrl = '';
        }

        function normalizeSkills(skills) {
            if (!Array.isArray(skills)) return [];
            return skills.map((item) => safeText(item)).filter((item, index, array) => item && array.indexOf(item) === index);
        }

        function normalizeProfileData(data) {
            refreshUserData();
            return {
                taId: safeText(data?.taId || profileState.taId),
                realName: safeText(data?.realName || userData?.name),
                applicationIntent: safeText(data?.applicationIntent),
                studentId: safeText(data?.studentId || profileState.taId),
                contactEmail: safeText(data?.contactEmail || userData?.email),
                bio: safeText(data?.bio),
                avatar: safeText(data?.avatar),
                skills: normalizeSkills(data?.skills),
                lastUpdatedAt: safeText(data?.lastUpdatedAt)
            };
        }

        function cloneProfileData(data) { return normalizeProfileData(JSON.parse(JSON.stringify(data || {}))); }

        function formatUpdatedTime(value) {
            if (!value) return '--';
            const date = new Date(value);
            if (Number.isNaN(date.getTime())) return value;
            return date.toLocaleString('zh-CN', { hour12: false });
        }

        function setProfileStatus(text, tone) {
            if (!profileSyncStatus) return;
            profileSyncStatus.textContent = text;
            profileSyncStatus.classList.remove('is-dirty', 'is-error', 'is-success');
            if (tone) profileSyncStatus.classList.add(tone);
        }

        function updateProfileLastUpdated(value) {
            if (profileLastUpdated) profileLastUpdated.textContent = '最近更新：' + formatUpdatedTime(value);
        }

        function buildAvatarAssetUrl(value) {
            const avatar = safeText(value);
            if (!avatar) return '';
            if (/^(?:https?:|data:|blob:)/i.test(avatar)) return avatar;
            const normalizedPath = avatar.replace(/^\/+/, '');
            return '../../ta-assets/' + normalizedPath.split('/').map((segment) => encodeURIComponent(segment)).join('/');
        }

        function updateTopbarAvatar(value) {
            const avatarEl = document.querySelector('.user-avatar');
            if (!avatarEl) return;
            const avatarUrl = buildAvatarAssetUrl(value);
            const displayName = typeof app.resolveWelcomeName === 'function' ? app.resolveWelcomeName(refreshUserData()) : 'TA';
            const fallbackLetter = (displayName.slice(0, 1) || 'T').toUpperCase();
            if (avatarUrl) {
                avatarEl.style.backgroundImage = 'url("' + avatarUrl + '")';
                avatarEl.style.backgroundSize = 'cover';
                avatarEl.style.backgroundPosition = 'center';
                avatarEl.style.backgroundRepeat = 'no-repeat';
                avatarEl.textContent = '';
                avatarEl.setAttribute('aria-label', displayName + ' 的头像');
            } else {
                avatarEl.style.backgroundImage = '';
                avatarEl.textContent = fallbackLetter;
                avatarEl.setAttribute('aria-label', displayName + ' 的头像占位');
            }
        }

        function updateAvatarPreview(value) {
            if (!profileAvatarBox) return;
            const avatarUrl = buildAvatarAssetUrl(value);
            const displayName = typeof app.resolveWelcomeName === 'function' ? app.resolveWelcomeName(refreshUserData()) : 'TA';
            const fallbackLetter = (displayName.slice(0, 1) || 'T').toUpperCase();
            if (profileAvatarPreview) {
                if (avatarUrl) {
                    profileAvatarPreview.src = avatarUrl;
                    profileAvatarPreview.hidden = false;
                } else {
                    profileAvatarPreview.removeAttribute('src');
                    profileAvatarPreview.hidden = true;
                }
            }

            if (avatarUrl) {
                profileAvatarBox.classList.add('has-image');
                profileAvatarBox.style.backgroundImage = 'url("' + avatarUrl + '")';
                profileAvatarBox.style.backgroundSize = 'cover';
                profileAvatarBox.style.backgroundPosition = 'center';
                profileAvatarBox.style.backgroundRepeat = 'no-repeat';
                profileAvatarBox.textContent = '';
                profileAvatarBox.setAttribute('aria-label', displayName + ' 的头像，点击可更换');
            } else {
                profileAvatarBox.classList.remove('has-image');
                profileAvatarBox.style.backgroundImage = '';
                profileAvatarBox.style.backgroundSize = '';
                profileAvatarBox.style.backgroundPosition = '';
                profileAvatarBox.style.backgroundRepeat = '';
                profileAvatarBox.textContent = fallbackLetter;
                profileAvatarBox.setAttribute('aria-label', displayName + ' 的头像占位，点击可更换');
            }
        }

        function setAvatarHint(text, tone) {
            if (!avatarUploadHint) return;
            avatarUploadHint.textContent = text;
            avatarUploadHint.classList.remove('is-error', 'is-success');
            if (tone) avatarUploadHint.classList.add(tone);
        }

        function setAvatarCropHint(text, tone) {
            if (!avatarCropHint) return;
            avatarCropHint.textContent = text;
            avatarCropHint.classList.remove('is-error', 'is-success');
            if (tone) avatarCropHint.classList.add(tone);
        }

        function createSkillTag(skill, editable) {
            const span = document.createElement('span');
            span.className = 'skill-tag' + (editable ? ' skill-tag-editable' : '');
            span.dataset.skill = skill;
            if (editable) {
                span.innerHTML = '<span>' + escapeHtml(skill) + '</span><button type="button" class="skill-tag-remove" aria-label="删除技能 ' + escapeHtml(skill) + '">×</button>';
                span.querySelector('.skill-tag-remove')?.addEventListener('click', () => {
                    if (!profileState.editable || !profileState.current) return;
                    profileState.current.skills = profileState.current.skills.filter((item) => item !== skill);
                    renderSkillTags(profileState.current.skills, true);
                    syncDirtyState();
                });
            } else {
                span.textContent = skill;
            }
            return span;
        }

        function renderSkillTags(skills, editable) {
            if (!skillsInput || !skillEntry) return;
            const existingTags = skillsInput.querySelectorAll('.skill-tag');
            existingTags.forEach((node) => node.remove());
            normalizeSkills(skills).forEach((skill) => {
                skillsInput.insertBefore(createSkillTag(skill, editable), skillEntry);
            });
            skillsInput.dataset.readonly = editable ? 'false' : 'true';
        }

        function setProfileEditable(editable) {
            profileState.editable = editable;
            profileFieldElements.forEach((element) => {
                element.readOnly = !editable;
            });
            if (studentIdInput) {
                studentIdInput.readOnly = true;
                studentIdInput.disabled = true;
            }
            if (avatarFileInput) avatarFileInput.disabled = !editable;
            if (skillEntry) {
                skillEntry.readOnly = !editable;
                skillEntry.placeholder = editable ? '输入技能后回车' : '资料暂不可编辑';
            }
            if (saveProfileBtn) saveProfileBtn.disabled = !editable;
            if (profileState.current) renderSkillTags(profileState.current.skills, editable);
        }

        function profilesEqual(left, right) {
            const a = normalizeProfileData(left || {});
            const b = normalizeProfileData(right || {});
            const sameProfile = JSON.stringify({ ...a, lastUpdatedAt: '' }) === JSON.stringify({ ...b, lastUpdatedAt: '' });
            return sameProfile && !profileState.avatarChanged;
        }

        function readProfileFromForm() {
            return normalizeProfileData({
                taId: profileState.taId,
                realName: fullNameInput?.value,
                applicationIntent: applicationIntentInput?.value,
                studentId: profileState.original?.studentId || profileState.current?.studentId || studentIdInput?.value || profileState.taId,
                contactEmail: contactEmailInput?.value,
                bio: bioInput?.value,
                avatar: profileState.current?.avatar || profileState.original?.avatar || '',
                skills: Array.from(skillsInput?.querySelectorAll('.skill-tag') || []).map((node) => node.dataset.skill || node.textContent),
                lastUpdatedAt: profileState.current?.lastUpdatedAt || profileState.original?.lastUpdatedAt || ''
            });
        }

        function fillProfileForm(data) {
            const normalized = normalizeProfileData(data);
            if (fullNameInput) fullNameInput.value = normalized.realName;
            if (applicationIntentInput) applicationIntentInput.value = normalized.applicationIntent;
            if (studentIdInput) studentIdInput.value = normalized.studentId;
            if (contactEmailInput) contactEmailInput.value = normalized.contactEmail;
            if (bioInput) bioInput.value = normalized.bio;
            updateAvatarPreview(normalized.avatar);
            renderSkillTags(normalized.skills, profileState.editable);
            updateProfileLastUpdated(normalized.lastUpdatedAt);
        }

        function syncDirtyState() {
            if (!profileState.current) return;
            profileState.current = readProfileFromForm();
            if (!profileState.editable) {
                setProfileStatus('已加载', null);
                return;
            }
            const dirty = !profilesEqual(profileState.current, profileState.original);
            setProfileStatus(dirty ? '待保存' : '编辑中', dirty ? 'is-dirty' : null);
        }

        function resetPendingAvatarState(options) {
            const restoreAvatar = options?.restoreAvatar !== false;
            revokeProfilePreviewUrl();
            profileState.pendingAvatarFile = null;
            profileState.avatarChanged = false;
            if (avatarFileInput) avatarFileInput.value = '';
            if (restoreAvatar && profileState.current) {
                const avatarValue = profileState.original?.avatar || profileState.current?.avatar || '';
                profileState.current.avatar = avatarValue;
                updateAvatarPreview(avatarValue);
            }
            syncDirtyState();
        }

        function clampAvatarCropOffsets() {
            if (!avatarCropStage) return;
            const stageRect = avatarCropStage.getBoundingClientRect();
            const scaledWidth = avatarCropState.naturalWidth * avatarCropState.baseScale * avatarCropState.scale;
            const scaledHeight = avatarCropState.naturalHeight * avatarCropState.baseScale * avatarCropState.scale;
            const maxOffsetX = Math.max(0, (scaledWidth - stageRect.width) / 2);
            const maxOffsetY = Math.max(0, (scaledHeight - stageRect.height) / 2);
            avatarCropState.offsetX = Math.min(maxOffsetX, Math.max(-maxOffsetX, avatarCropState.offsetX));
            avatarCropState.offsetY = Math.min(maxOffsetY, Math.max(-maxOffsetY, avatarCropState.offsetY));
        }

        function renderAvatarCropPreview() {
            const canvas = avatarCropPreviewCanvas;
            if (!canvas || !avatarCropImage || !avatarCropStage || !avatarCropState.naturalWidth || !avatarCropState.naturalHeight) return;
            const ctx = canvas.getContext('2d');
            if (!ctx) return;

            const stageSize = avatarCropStage.clientWidth || avatarCropStage.getBoundingClientRect().width || 0;
            if (!stageSize) return;

            const scaledWidth = avatarCropState.naturalWidth * avatarCropState.baseScale * avatarCropState.scale;
            const scaledHeight = avatarCropState.naturalHeight * avatarCropState.baseScale * avatarCropState.scale;
            const drawX = stageSize / 2 - scaledWidth / 2 + avatarCropState.offsetX;
            const drawY = stageSize / 2 - scaledHeight / 2 + avatarCropState.offsetY;
            const ratio = AVATAR_CROP_OUTPUT_SIZE / stageSize;

            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.save();
            ctx.imageSmoothingEnabled = true;
            ctx.imageSmoothingQuality = 'high';
            ctx.scale(canvas.width / AVATAR_CROP_OUTPUT_SIZE, canvas.height / AVATAR_CROP_OUTPUT_SIZE);
            ctx.drawImage(
                avatarCropImage,
                drawX * ratio,
                drawY * ratio,
                scaledWidth * ratio,
                scaledHeight * ratio
            );
            ctx.restore();
        }

        function renderAvatarCropStage() {
            if (!avatarCropImage || !avatarCropStage || !avatarCropState.isOpen) return;
            clampAvatarCropOffsets();
            const width = avatarCropState.naturalWidth * avatarCropState.baseScale;
            const height = avatarCropState.naturalHeight * avatarCropState.baseScale;
            avatarCropImage.style.width = width + 'px';
            avatarCropImage.style.height = height + 'px';
            avatarCropImage.style.transform = 'translate(calc(-50% + ' + avatarCropState.offsetX + 'px), calc(-50% + ' + avatarCropState.offsetY + 'px)) scale(' + avatarCropState.scale + ')';
            renderAvatarCropPreview();
        }

        function setAvatarCropOpen(open) {
            if (!avatarCropModal || !avatarCropModalOverlay) return;
            avatarCropState.isOpen = open;
            avatarCropModal.classList.toggle('active', open);
            avatarCropModalOverlay.classList.toggle('show', open);
            avatarCropModalOverlay.setAttribute('aria-hidden', open ? 'false' : 'true');
            if (appShell) appShell.classList.toggle('modal-open', open);
        }

        function teardownAvatarCropState() {
            if (avatarCropStage) avatarCropStage.classList.remove('is-dragging');
            avatarCropState.dragging = false;
            avatarCropState.activePointerId = null;
            avatarCropState.file = null;
            avatarCropState.outputFileName = '';
            avatarCropState.naturalWidth = 0;
            avatarCropState.naturalHeight = 0;
            avatarCropState.baseScale = 1;
            avatarCropState.scale = 1;
            avatarCropState.minScale = 1;
            avatarCropState.maxScale = 3;
            avatarCropState.offsetX = 0;
            avatarCropState.offsetY = 0;
            revokeAvatarCropObjectUrl();
            if (avatarCropImage) {
                avatarCropImage.hidden = true;
                avatarCropImage.removeAttribute('src');
                avatarCropImage.style.transform = '';
                avatarCropImage.style.width = '';
                avatarCropImage.style.height = '';
            }
        }

        function closeAvatarCropModal(cancelled) {
            if (!avatarCropState.isOpen) return;
            setAvatarCropOpen(false);
            teardownAvatarCropState();
            if (cancelled) {
                if (avatarFileInput) avatarFileInput.value = '';
                setAvatarHint('已取消裁切，当前头像未变更。', null);
            }
        }

        function openAvatarCropModal(file) {
            if (!file || !avatarCropImage || !avatarCropStage) return;
            teardownAvatarCropState();
            avatarCropState.file = file;
            avatarCropState.outputFileName = file.name.replace(/\.[^.]+$/, '') + '-square.png';
            avatarCropState.objectUrl = URL.createObjectURL(file);
            avatarCropImage.onload = function () {
                const stageRect = avatarCropStage.getBoundingClientRect();
                const stageSize = stageRect.width || avatarCropStage.clientWidth || 0;
                if (!stageSize) {
                    setAvatarCropHint('裁切区域初始化失败，请重试。', 'is-error');
                    closeAvatarCropModal(true);
                    return;
                }
                avatarCropState.naturalWidth = avatarCropImage.naturalWidth;
                avatarCropState.naturalHeight = avatarCropImage.naturalHeight;
                avatarCropState.baseScale = Math.max(stageSize / avatarCropState.naturalWidth, stageSize / avatarCropState.naturalHeight);
                avatarCropState.minScale = 1;
                avatarCropState.maxScale = Math.max(3, Math.ceil(Math.max(avatarCropState.naturalWidth, avatarCropState.naturalHeight) / Math.max(1, Math.min(avatarCropState.naturalWidth, avatarCropState.naturalHeight)) * 100) / 100 + 1.5);
                avatarCropState.scale = 1;
                avatarCropState.offsetX = 0;
                avatarCropState.offsetY = 0;
                if (avatarCropZoomRange) {
                    avatarCropZoomRange.min = String(avatarCropState.minScale);
                    avatarCropZoomRange.max = String(avatarCropState.maxScale);
                    avatarCropZoomRange.step = '0.01';
                    avatarCropZoomRange.value = String(avatarCropState.scale);
                }
                avatarCropImage.hidden = false;
                setAvatarCropHint('拖动图片调整取景，滑动缩放条可放大或缩小。', null);
                renderAvatarCropStage();
            };
            avatarCropImage.onerror = function () {
                setAvatarCropHint('图片读取失败，请更换文件后重试。', 'is-error');
                closeAvatarCropModal(true);
            };
            avatarCropImage.src = avatarCropState.objectUrl;
            setAvatarCropOpen(true);
        }

        async function exportCroppedAvatarFile() {
            if (!avatarCropStage || !avatarCropImage) throw new Error('裁切区域未初始化');
            const canvas = document.createElement('canvas');
            canvas.width = AVATAR_CROP_OUTPUT_SIZE;
            canvas.height = AVATAR_CROP_OUTPUT_SIZE;
            const ctx = canvas.getContext('2d');
            if (!ctx) throw new Error('浏览器不支持头像裁切');

            const stageSize = avatarCropStage.clientWidth || avatarCropStage.getBoundingClientRect().width || 0;
            if (!stageSize) throw new Error('裁切区域尺寸异常');

            const scaledWidth = avatarCropState.naturalWidth * avatarCropState.baseScale * avatarCropState.scale;
            const scaledHeight = avatarCropState.naturalHeight * avatarCropState.baseScale * avatarCropState.scale;
            const drawX = stageSize / 2 - scaledWidth / 2 + avatarCropState.offsetX;
            const drawY = stageSize / 2 - scaledHeight / 2 + avatarCropState.offsetY;
            const ratio = AVATAR_CROP_OUTPUT_SIZE / stageSize;

            ctx.imageSmoothingEnabled = true;
            ctx.imageSmoothingQuality = 'high';
            ctx.drawImage(
                avatarCropImage,
                drawX * ratio,
                drawY * ratio,
                scaledWidth * ratio,
                scaledHeight * ratio
            );

            const blob = await new Promise((resolve) => {
                canvas.toBlob(resolve, 'image/png', 0.92);
            });
            if (!blob) throw new Error('头像导出失败，请重试');

            return new File([blob], avatarCropState.outputFileName || 'avatar-square.png', {
                type: 'image/png',
                lastModified: Date.now()
            });
        }

        async function requestProfileSettings(method, payload) {
            let queryString = '';
            if (method === 'GET') {
                const queryParams = new URLSearchParams();
                Object.entries(payload || {}).forEach(([key, value]) => {
                    if (value != null) queryParams.append(key, value);
                });
                queryString = queryParams.toString();
            }
            const url = method === 'GET'
                ? '../../api/ta/profile-settings' + (queryString ? ('?' + queryString) : '')
                : '../../api/ta/profile-settings';
            const options = { method: method };
            if (method !== 'GET') {
                const formData = new FormData();
                Object.entries(payload || {}).forEach(([key, value]) => {
                    if (Array.isArray(value)) {
                        value.forEach((item) => formData.append('skills[]', item));
                    } else if (value instanceof Blob) {
                        const fileName = value instanceof File ? value.name : 'avatar-upload.bin';
                        formData.append(key, value, fileName);
                    } else if (value != null) {
                        formData.append(key, value);
                    }
                });
                options.body = formData;
            }

            const response = await fetch(url, options);
            const result = await response.json().catch(() => ({ success: false, message: '返回数据解析失败' }));
            if (!response.ok || !result.success) throw new Error(result.message || '请求失败');
            return result.data || {};
        }

        async function loadProfileSettings() {
            if (!profileState.taId) {
                setProfileStatus('缺少登录信息', 'is-error');
                return;
            }
            profileState.loading = true;
            setProfileStatus('加载中...', null);
            try {
                const data = await requestProfileSettings('GET', { taId: profileState.taId });
                const normalized = normalizeProfileData(data);
                revokeProfilePreviewUrl();
                profileState.original = cloneProfileData(normalized);
                profileState.current = cloneProfileData(normalized);
                profileState.pendingAvatarFile = null;
                profileState.avatarChanged = false;
                if (avatarFileInput) avatarFileInput.value = '';
                fillProfileForm(normalized);
                setProfileEditable(true);
                setProfileStatus('已加载', null);
                setAvatarHint('支持 PNG / JPG / WEBP / GIF，大小限制 10MB。点击头像即可更换。', null);

                updateTopbarAvatar(normalized.avatar);
                if (userData) {
                    userData.avatar = normalized.avatar;
                    app.setUserData?.(userData);
                }
            } catch (error) {
                console.error('[TA-PROFILE] load failed', error);
                setProfileStatus('加载失败', 'is-error');
            } finally {
                profileState.loading = false;
            }
        }

        async function saveProfileSettings() {
            if (!profileState.editable || profileState.saving) return;
            const payload = readProfileFromForm();
            payload.avatarFile = profileState.pendingAvatarFile;
            profileState.saving = true;
            setProfileStatus('保存中...', null);
            try {
                const saved = await requestProfileSettings('POST', payload);
                const normalized = normalizeProfileData(saved);
                revokeProfilePreviewUrl();
                profileState.original = cloneProfileData(normalized);
                profileState.current = cloneProfileData(normalized);
                profileState.pendingAvatarFile = null;
                profileState.avatarChanged = false;
                if (avatarFileInput) avatarFileInput.value = '';
                fillProfileForm(normalized);
                setProfileEditable(true);
                setProfileStatus('保存成功', 'is-success');
                setAvatarHint('头像已同步到服务器。', 'is-success');
                const mergedUser = {
                    ...(refreshUserData() || {}),
                    taId: normalized.taId || profileState.taId,
                    name: normalized.realName || userData?.name || '',
                    email: normalized.contactEmail || userData?.email || '',
                    avatar: safeText(normalized.avatar)
                };
                userData = mergedUser;
                app.setUserData?.(mergedUser);
                app.applyWelcomeTitle?.('profile-saved');
                const userNameEl = document.getElementById('userName');
                if (userNameEl) userNameEl.textContent = app.resolveWelcomeName ? app.resolveWelcomeName(userData) : 'TA';
                updateTopbarAvatar(normalized.avatar);
            } catch (error) {
                console.error('[TA-PROFILE] save failed', error);
                setProfileStatus(error.message || '保存失败', 'is-error');
            } finally {
                profileState.saving = false;
            }
        }

        async function confirmAvatarCrop() {
            if (!avatarCropState.isOpen || !avatarCropState.file) return;
            if (avatarCropConfirmBtn) avatarCropConfirmBtn.disabled = true;
            try {
                const croppedFile = await exportCroppedAvatarFile();
                if (croppedFile.size > MAX_AVATAR_SIZE) {
                    throw new Error('裁切后的头像超过 10MB，请缩小后重试');
                }
                revokeProfilePreviewUrl();
                const objectUrl = URL.createObjectURL(croppedFile);
                profileState.avatarPreviewUrl = objectUrl;
                profileState.pendingAvatarFile = croppedFile;
                profileState.avatarChanged = true;
                if (profileState.current) profileState.current.avatar = objectUrl;
                updateAvatarPreview(objectUrl);
                setAvatarHint('已完成 1:1 裁切，正在自动保存头像...', 'is-success');
                closeAvatarCropModal(false);
                syncDirtyState();
                await saveProfileSettings();
            } catch (error) {
                console.error('[TA-PROFILE] crop confirm failed', error);
                setAvatarCropHint(error.message || '裁切失败，请重试。', 'is-error');
                if (error && error.message) {
                    setAvatarHint(error.message, 'is-error');
                }
            } finally {
                if (avatarCropConfirmBtn) avatarCropConfirmBtn.disabled = false;
            }
        }

        function applyAvatarCropScale(nextScale) {
            const normalizedScale = Math.min(avatarCropState.maxScale, Math.max(avatarCropState.minScale, nextScale));
            avatarCropState.scale = normalizedScale;
            if (avatarCropZoomRange) avatarCropZoomRange.value = String(normalizedScale);
            renderAvatarCropStage();
        }

        function triggerAvatarPicker() {
            if (!profileState.editable || !avatarFileInput || avatarFileInput.disabled) return;
            avatarFileInput.click();
        }

        function handleAvatarCropPointerDown(event) {
            if (!avatarCropState.isOpen || !avatarCropStage) return;
            avatarCropState.dragging = true;
            avatarCropState.activePointerId = event.pointerId;
            avatarCropState.dragStartX = event.clientX;
            avatarCropState.dragStartY = event.clientY;
            avatarCropState.startOffsetX = avatarCropState.offsetX;
            avatarCropState.startOffsetY = avatarCropState.offsetY;
            avatarCropStage.classList.add('is-dragging');
            if (avatarCropStage.setPointerCapture) avatarCropStage.setPointerCapture(event.pointerId);
        }

        function handleAvatarCropPointerMove(event) {
            if (!avatarCropState.dragging || avatarCropState.activePointerId !== event.pointerId) return;
            avatarCropState.offsetX = avatarCropState.startOffsetX + (event.clientX - avatarCropState.dragStartX);
            avatarCropState.offsetY = avatarCropState.startOffsetY + (event.clientY - avatarCropState.dragStartY);
            renderAvatarCropStage();
        }

        function handleAvatarCropPointerUp(event) {
            if (avatarCropState.activePointerId !== event.pointerId) return;
            avatarCropState.dragging = false;
            avatarCropState.activePointerId = null;
            if (avatarCropStage) avatarCropStage.classList.remove('is-dragging');
            if (avatarCropStage && avatarCropStage.releasePointerCapture) {
                try {
                    avatarCropStage.releasePointerCapture(event.pointerId);
                } catch (error) {
                    console.debug('[TA-PROFILE] releasePointerCapture skipped', error);
                }
            }
        }

        profileFieldElements.forEach((element) => element.addEventListener('input', syncDirtyState));

        avatarFileInput?.addEventListener('change', () => {
            const file = avatarFileInput.files?.[0] || null;
            if (!file) {
                resetPendingAvatarState({ restoreAvatar: true });
                setAvatarHint('支持 PNG / JPG / WEBP / GIF，大小限制 10MB。点击头像即可更换。', null);
                return;
            }

            if (file.size > MAX_AVATAR_SIZE) {
                resetPendingAvatarState({ restoreAvatar: true });
                setAvatarHint('头像大小不能超过 10MB。', 'is-error');
                return;
            }

            if (!/^image\/(png|jpeg|webp|gif)$/i.test(file.type || '')) {
                resetPendingAvatarState({ restoreAvatar: true });
                setAvatarHint('头像格式仅支持 PNG / JPG / WEBP / GIF。', 'is-error');
                return;
            }

            setAvatarHint('图片已选择，请先完成 1:1 裁切。', null);
            openAvatarCropModal(file);
        });

        avatarCropZoomRange?.addEventListener('input', () => {
            const nextScale = Number(avatarCropZoomRange.value || avatarCropState.minScale);
            if (Number.isNaN(nextScale)) return;
            applyAvatarCropScale(nextScale);
        });

        avatarCropStage?.addEventListener('wheel', (event) => {
            if (!avatarCropState.isOpen) return;
            event.preventDefault();
            const direction = event.deltaY > 0 ? -1 : 1;
            const step = event.ctrlKey ? 0.03 : 0.08;
            applyAvatarCropScale(avatarCropState.scale + direction * step);
        }, { passive: false });

        avatarCropStage?.addEventListener('pointerdown', handleAvatarCropPointerDown);
        avatarCropStage?.addEventListener('pointermove', handleAvatarCropPointerMove);
        avatarCropStage?.addEventListener('pointerup', handleAvatarCropPointerUp);
        avatarCropStage?.addEventListener('pointercancel', handleAvatarCropPointerUp);
        avatarCropStage?.addEventListener('lostpointercapture', () => {
            avatarCropState.dragging = false;
            avatarCropState.activePointerId = null;
            avatarCropStage.classList.remove('is-dragging');
        });

        avatarCropConfirmBtn?.addEventListener('click', confirmAvatarCrop);
        avatarCropCancelBtn?.addEventListener('click', () => closeAvatarCropModal(true));
        avatarCropCloseButtons.forEach((button) => {
            button.addEventListener('click', () => closeAvatarCropModal(true));
        });

        avatarCropModalOverlay?.addEventListener('click', (event) => {
            if (event.target !== avatarCropModalOverlay) return;
            if (avatarCropState.isOpen) closeAvatarCropModal(true);
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && avatarCropState.isOpen) {
                event.preventDefault();
                closeAvatarCropModal(true);
            }
        });

        profileAvatarBox?.addEventListener('click', triggerAvatarPicker);
        profileAvatarBox?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                triggerAvatarPicker();
            }
        });

        if (skillEntry && skillsInput) {
            skillEntry.addEventListener('keydown', (event) => {
                if (!profileState.editable) return;
                if (event.key === 'Enter' && skillEntry.value.trim()) {
                    event.preventDefault();
                    const nextSkill = safeText(skillEntry.value);
                    const currentSkills = normalizeSkills(profileState.current?.skills || []);
                    if (!currentSkills.includes(nextSkill)) currentSkills.push(nextSkill);
                    profileState.current = {
                        ...(profileState.current || normalizeProfileData({ taId: profileState.taId })),
                        skills: currentSkills
                    };
                    renderSkillTags(currentSkills, true);
                    skillEntry.value = '';
                    syncDirtyState();
                }
            });
        }

        saveProfileBtn?.addEventListener('click', saveProfileSettings);
        window.addEventListener('beforeunload', () => {
            revokeProfilePreviewUrl();
            revokeAvatarCropObjectUrl();
        });

        updateTopbarAvatar(refreshUserData()?.avatar);
        loadProfileSettings();

        app.profileState = profileState;
        app.updateTopbarAvatar = updateTopbarAvatar;
        app.saveProfileSettings = saveProfileSettings;
        app.loadProfileSettings = loadProfileSettings;
    };
})();
