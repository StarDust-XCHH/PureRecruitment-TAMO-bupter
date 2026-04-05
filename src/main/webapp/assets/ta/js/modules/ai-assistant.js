(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};

    modules.aiAssistant = function initAiAssistantModule(app) {
        const state = app.state.aiAssistant = app.state.aiAssistant || {
            taId: '',
            sessionId: '',
            scene: 'general_chat',
            title: 'AI 助理对话',
            context: {},
            pendingAttachments: [],
            sessions: [],
            serviceStatus: {
                provider: '',
                available: false,
                configured: false,
                message: '正在检测 AI 服务状态…',
                defaultProvider: true
            }
        };

        const thread = document.getElementById('aiAssistantThread');
        const statusBadge = document.getElementById('aiAssistantStatusBadge');
        const pendingCount = document.getElementById('aiPendingAttachmentCount');
        const pendingList = document.getElementById('aiPendingAttachmentList');
        const composerInput = document.getElementById('aiAssistantComposerInput');
        const sendBtn = document.getElementById('aiAssistantSendBtn');
        const fileInput = document.getElementById('aiAssistantFileInput');
        const composerHint = document.getElementById('aiComposerHint');
        const newSessionBtn = document.getElementById('aiAssistantNewSessionBtn');
        const chipButtons = Array.from(document.querySelectorAll('.ai-chat-chip[data-ai-prompt]'));
        const serviceBanner = document.getElementById('aiAssistantServiceBanner');
        const serviceText = document.getElementById('aiAssistantServiceText');

        function resolveTaId() {
            const userData = typeof app.getUserData === 'function' ? app.getUserData() : null;
            state.taId = String(userData?.taId || userData?.id || state.taId || '').trim();
            return state.taId;
        }

        function resolveAiApiUrl() {
            return '../../api/ta/ai';
        }

        function resolveDefaultAssistantMessage() {
            if (!state.serviceStatus.available) {
                return state.serviceStatus.message || '服务端当前未配置 AI API，AI 助理功能已禁用。';
            }
            return '你好，我是你的 AI 助理。你可以直接输入问题，也可以从课程申请弹窗预载简历后再发送，我会结合当前会话与附件内容继续回答。';
        }

        function formatFileSize(bytes) {
            const size = Number(bytes);
            if (!Number.isFinite(size) || size <= 0) return '--';
            if (size >= 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB';
            if (size >= 1024) return Math.round(size / 1024) + ' KB';
            return size + ' B';
        }

        function escapeHtml(value) {
            return String(value || '')
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        function setStatus(text) {
            if (statusBadge) {
                statusBadge.textContent = text || 'Ready';
            }
        }

        function normalizeServiceStatus(serviceStatus) {
            return {
                provider: String(serviceStatus?.provider || '').trim(),
                available: Boolean(serviceStatus?.available),
                configured: Boolean(serviceStatus?.configured),
                message: String(serviceStatus?.message || 'AI 服务状态未知').trim(),
                defaultProvider: Boolean(serviceStatus?.defaultProvider)
            };
        }

        function applyServiceStatus(serviceStatus) {
            state.serviceStatus = normalizeServiceStatus(serviceStatus);
            const available = state.serviceStatus.available;
            const providerName = state.serviceStatus.provider || '未配置';
            const statusMessage = state.serviceStatus.message || 'AI 服务状态未知';

            if (serviceBanner) {
                serviceBanner.dataset.available = available ? 'true' : 'false';
            }
            if (serviceText) {
                serviceText.textContent = available
                    ? '当前服务：' + providerName + ' · ' + statusMessage
                    : '当前服务不可用 · ' + statusMessage;
            }
            if (composerInput) {
                composerInput.disabled = !available;
            }
            if (fileInput) {
                fileInput.disabled = !available;
            }
            if (sendBtn) {
                sendBtn.disabled = !available;
            }
            chipButtons.forEach((button) => {
                button.disabled = !available;
            });
            if (!available) {
                setStatus('Disabled');
            }
        }

        function renderPendingAttachments() {
            if (pendingCount) {
                pendingCount.textContent = state.pendingAttachments.length + ' 份材料';
            }
            if (!pendingList) return;
            if (!state.pendingAttachments.length) {
                pendingList.innerHTML = '<div class="ai-upload-file ai-upload-file-empty"><strong>暂无待发送附件</strong><span>你可以在这里上传文件，或从课程申请弹窗预载当前简历。</span></div>';
                return;
            }
            pendingList.innerHTML = state.pendingAttachments.map((item) => {
                const sourceLabel = item.sourceType === 'course-apply' ? '来自课程申请' : '手动上传';
                return '<div class="ai-upload-file" data-attachment-id="' + escapeHtml(item.attachmentId) + '">' +
                    '<strong>' + escapeHtml(item.originalFileName) + '</strong>' +
                    '<span>' + escapeHtml(sourceLabel) + ' · ' + escapeHtml(formatFileSize(item.size)) + '</span>' +
                    '</div>';
            }).join('');
        }

        function renderThread() {
            if (!thread) return;
            const currentSession = state.sessions.find((item) => item.sessionId === state.sessionId) || state.sessions[0] || null;
            if (!currentSession || !Array.isArray(currentSession.messages) || !currentSession.messages.length) {
                thread.innerHTML = '' +
                    '<div class="ai-chat-date">今天</div>' +
                    '<article class="ai-message ai-message-assistant">' +
                    '  <div class="ai-avatar" aria-hidden="true">AI</div>' +
                    '  <div class="ai-bubble"><p>' + escapeHtml(resolveDefaultAssistantMessage()) + '</p></div>' +
                    '</article>';
                return;
            }

            const messageHtml = currentSession.messages.map((message) => {
                const isUser = message.role === 'user';
                const artifact = message.artifact && typeof message.artifact === 'object' ? message.artifact : null;
                return '<article class="ai-message ' + (isUser ? 'ai-message-user' : 'ai-message-assistant') + '">' +
                    '<div class="ai-avatar" aria-hidden="true">' + (isUser ? 'TA' : 'AI') + '</div>' +
                    '<div class="ai-bubble">' +
                    '<p>' + escapeHtml(message.content || '') + '</p>' +
                    (artifact && artifact.downloadUrl
                        ? '<div class="ai-message-download"><a class="pill-btn" href="' + escapeHtml(artifact.downloadUrl) + '" download>下载生成的 PDF</a></div>'
                        : '') +
                    '</div>' +
                    '</article>';
            }).join('');

            thread.innerHTML = '<div class="ai-chat-date">当前会话</div>' + messageHtml;
            thread.scrollTop = thread.scrollHeight;
        }

        function hydrateConversation(data) {
            state.sessions = Array.isArray(data?.sessions) ? data.sessions : [];
            state.pendingAttachments = Array.isArray(data?.pendingAttachments) ? data.pendingAttachments : [];
            if (data?.serviceStatus) {
                applyServiceStatus(data.serviceStatus);
            }
            if (!state.sessionId && state.sessions.length) {
                state.sessionId = String(state.sessions[0].sessionId || '').trim();
            }
            renderPendingAttachments();
            renderThread();
        }

        async function fetchServiceStatus() {
            const response = await fetch(resolveAiApiUrl() + '?action=status', {
                method: 'GET',
                headers: { Accept: 'application/json' }
            });
            const payload = await response.json();
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || 'AI 服务状态读取失败');
            }
            applyServiceStatus(payload.data || {});
        }

        async function loadConversation() {
            const taId = resolveTaId();
            if (!taId) return;
            await fetchServiceStatus();
            const response = await fetch(resolveAiApiUrl() + '?taId=' + encodeURIComponent(taId), {
                method: 'GET',
                headers: { Accept: 'application/json' }
            });
            const payload = await response.json();
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || 'AI 会话读取失败');
            }
            hydrateConversation(payload.data || {});
            if (state.serviceStatus.available) {
                setStatus('Ready');
            }
        }

        function startNewSession() {
            state.sessionId = '';
            state.scene = 'general_chat';
            state.title = 'AI 助理对话';
            state.context = {};
            state.pendingAttachments = [];
            if (composerInput) {
                composerInput.value = '';
            }
            if (fileInput) {
                fileInput.value = '';
            }
            if (composerHint) {
                composerHint.textContent = state.serviceStatus.available
                    ? '已切换到新会话。你可以直接输入问题，或先上传附件后再发送。'
                    : (state.serviceStatus.message || '当前 AI 服务不可用');
            }
            if (state.serviceStatus.available) {
                setStatus('Ready');
            }
            renderPendingAttachments();
            renderThread();
        }

        async function uploadPendingFile(file, meta) {
            if (!state.serviceStatus.available) {
                throw new Error(state.serviceStatus.message || '当前 AI 服务不可用，无法上传附件');
            }
            const taId = resolveTaId();
            if (!taId) throw new Error('当前未获取到 TA 身份，请重新登录后再试。');
            const formData = new FormData();
            formData.append('action', 'upload-pending');
            formData.append('taId', taId);
            formData.append('file', file, file.name);
            formData.append('sourceType', meta?.sourceType || 'manual');
            formData.append('sourcePath', meta?.sourcePath || '');
            formData.append('courseCode', meta?.courseCode || '');
            formData.append('applicationId', meta?.applicationId || '');

            const response = await fetch(resolveAiApiUrl(), {
                method: 'POST',
                body: formData
            });
            const payload = await response.json();
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || 'AI 附件载入失败');
            }
            state.pendingAttachments = Array.isArray(payload.data?.pendingAttachments) ? payload.data.pendingAttachments : state.pendingAttachments;
            if (payload.data?.serviceStatus) {
                applyServiceStatus(payload.data.serviceStatus);
            }
            renderPendingAttachments();
            return payload.data || {};
        }

        async function sendMessage() {
            const taId = resolveTaId();
            if (!taId) {
                window.alert('当前未获取到 TA 身份，请重新登录后再试。');
                return;
            }
            if (!state.serviceStatus.available) {
                window.alert(state.serviceStatus.message || '当前 AI 服务不可用。');
                return;
            }
            const message = String(composerInput?.value || '').trim();
            if (!message && !state.pendingAttachments.length) {
                window.alert('请先输入消息，或准备至少一个附件。');
                return;
            }

            sendBtn.disabled = true;
            setStatus('Sending');
            if (composerHint) {
                composerHint.textContent = 'AI 正在生成结果，请稍候…';
            }

            try {
                const response = await fetch(resolveAiApiUrl(), {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        Accept: 'application/json'
                    },
                    body: JSON.stringify({
                        action: 'chat',
                        taId: taId,
                        sessionId: state.sessionId,
                        scene: state.scene,
                        title: state.title,
                        message: message,
                        attachmentIds: state.pendingAttachments.map((item) => item.attachmentId),
                        context: state.context
                    })
                });
                const payload = await response.json();
                if (!response.ok || !payload?.success) {
                    throw new Error(payload?.message || 'AI 对话失败');
                }

                state.sessionId = String(payload.data?.sessionId || state.sessionId || '').trim();
                hydrateConversation(payload.data?.conversation || {});
                state.pendingAttachments = Array.isArray(payload.data?.pendingAttachments) ? payload.data.pendingAttachments : [];
                renderPendingAttachments();
                renderThread();
                if (composerInput) composerInput.value = '';
                setStatus('Completed');
                if (composerHint) {
                    composerHint.textContent = 'AI 回复已完成。';
                }
            } catch (error) {
                console.error('[TA-AI] send failed', error);
                setStatus('Error');
                if (composerHint) {
                    composerHint.textContent = error.message || 'AI 助理处理失败，请稍后重试。';
                }
                window.alert(error.message || 'AI 助理处理失败，请稍后重试。');
            } finally {
                sendBtn.disabled = !state.serviceStatus.available;
            }
        }

        async function preloadFromCourseApply(payload) {
            if (!payload?.file) {
                window.alert('请先在课程申请弹窗中选择简历文件。');
                return;
            }
            if (!state.serviceStatus.available) {
                window.alert(state.serviceStatus.message || '当前 AI 服务不可用。');
                return;
            }
            try {
                setStatus('Loading');
                if (composerHint) {
                    composerHint.textContent = '正在将课程申请简历载入 AI 待发送附件…';
                }
                if (typeof app.openModal === 'function') {
                    app.openModal('planner');
                }
                await uploadPendingFile(payload.file, {
                    sourceType: 'course-apply',
                    sourcePath: payload.sourcePath || '',
                    courseCode: payload.courseCode || '',
                    applicationId: payload.applicationId || ''
                });
                state.scene = 'resume_optimize';
                state.title = '课程申请简历优化';
                state.context = {
                    courseCode: payload.courseCode || '',
                    applicationId: payload.applicationId || '',
                    sourcePath: payload.sourcePath || ''
                };
                state.sessionId = '';
                if (composerInput && !composerInput.value.trim()) {
                    composerInput.value = '请根据当前课程申请要求，帮我优化这份简历，并指出最值得优先调整的内容。';
                }
                if (composerHint) {
                    composerHint.textContent = '课程申请简历已载入待发送附件，请确认消息后点击发送。';
                }
                setStatus('Ready');
                renderPendingAttachments();
            } catch (error) {
                console.error('[TA-AI] preload failed', error);
                setStatus('Error');
                if (composerHint) {
                    composerHint.textContent = error.message || '载入课程申请简历失败。';
                }
                window.alert(error.message || '载入课程申请简历失败。');
            }
        }

        chipButtons.forEach((button) => {
            button.addEventListener('click', () => {
                if (!state.serviceStatus.available) {
                    return;
                }
                if (composerInput) {
                    composerInput.value = button.dataset.aiPrompt || '';
                    composerInput.focus();
                }
            });
        });

        fileInput?.addEventListener('change', async () => {
            const files = fileInput.files ? Array.from(fileInput.files) : [];
            if (!files.length) return;
            try {
                setStatus('Loading');
                for (const file of files) {
                    await uploadPendingFile(file, { sourceType: 'manual' });
                }
                if (composerHint) {
                    composerHint.textContent = '附件已加入待发送区，可以继续输入消息或直接发送。';
                }
                setStatus('Ready');
            } catch (error) {
                console.error('[TA-AI] upload failed', error);
                setStatus('Error');
                window.alert(error.message || '附件上传失败。');
            } finally {
                fileInput.value = '';
            }
        });

        newSessionBtn?.addEventListener('click', startNewSession);
        sendBtn?.addEventListener('click', sendMessage);
        composerInput?.addEventListener('keydown', (event) => {
            if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
                event.preventDefault();
                sendMessage();
            }
        });

        app.openAiAssistantWithAttachment = preloadFromCourseApply;
        app.loadAiConversation = loadConversation;

        applyServiceStatus(state.serviceStatus);
        loadConversation().catch((error) => {
            console.error('[TA-AI] initial load failed', error);
            applyServiceStatus({
                provider: '',
                available: false,
                configured: false,
                message: error.message || 'AI 服务初始化失败',
                defaultProvider: true
            });
            renderPendingAttachments();
            renderThread();
        });
    };
})();
