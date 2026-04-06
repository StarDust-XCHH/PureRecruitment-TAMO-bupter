(function () {
    'use strict';

    const taApp = window.TAApp = window.TAApp || {};
    const modules = taApp.modules = taApp.modules || {};
    const DEFAULT_HISTORY_VISIBLE_COUNT = 2;

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
            },
            isSending: false,
            activeResponseId: '',
            historyExpanded: false,
            attachmentPanelExpanded: false
        };

        const thread = document.getElementById('aiAssistantThread');
        const statusBadge = document.getElementById('aiAssistantStatusBadge');
        const pendingCount = document.getElementById('aiPendingAttachmentCount');
        const pendingList = document.getElementById('aiPendingAttachmentList');
        const pendingPanel = document.getElementById('aiPendingAttachmentPanel');
        const pendingToggle = document.getElementById('aiPendingAttachmentToggle');
        const historyCount = document.getElementById('aiConversationHistoryCount');
        const historyList = document.getElementById('aiAssistantSessionHistory');
        const historyToggle = document.getElementById('aiConversationHistoryToggle');
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

        function normalizeInline(text) {
            let normalized = String(text || '');
            normalized = escapeHtml(normalized);
            normalized = normalized.replace(/`([^`]+)`/g, '<code>$1</code>');
            normalized = normalized.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
            normalized = normalized.replace(/__([^_]+)__/g, '<strong>$1</strong>');
            normalized = normalized.replace(/(^|[^*])\*([^*]+)\*(?!\*)/g, '$1<em>$2</em>');
            normalized = normalized.replace(/(^|[^_])_([^_]+)_(?!_)/g, '$1<em>$2</em>');
            normalized = normalized.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
            return normalized;
        }

        function renderMarkdown(markdownText) {
            const normalizedText = String(markdownText || '').replace(/\r\n/g, '\n');
            if (!normalizedText.trim()) {
                return '<p></p>';
            }

            const lines = normalizedText.split('\n');
            const html = [];
            let paragraph = [];
            let listBuffer = [];
            let listType = '';
            let inCodeBlock = false;
            let codeBuffer = [];

            function flushParagraph() {
                if (!paragraph.length) return;
                html.push('<p>' + normalizeInline(paragraph.join('<br>')) + '</p>');
                paragraph = [];
            }

            function flushList() {
                if (!listBuffer.length || !listType) return;
                html.push('<' + listType + ' class="ai-markdown-list">' + listBuffer.join('') + '</' + listType + '>');
                listBuffer = [];
                listType = '';
            }

            function flushCodeBlock() {
                if (!inCodeBlock) return;
                html.push('<pre class="ai-markdown-pre"><code>' + escapeHtml(codeBuffer.join('\n')) + '</code></pre>');
                inCodeBlock = false;
                codeBuffer = [];
            }

            lines.forEach((line) => {
                const trimmed = line.trim();

                if (trimmed.startsWith('```')) {
                    flushParagraph();
                    flushList();
                    if (inCodeBlock) {
                        flushCodeBlock();
                    } else {
                        inCodeBlock = true;
                        codeBuffer = [];
                    }
                    return;
                }

                if (inCodeBlock) {
                    codeBuffer.push(line);
                    return;
                }

                if (!trimmed) {
                    flushParagraph();
                    flushList();
                    return;
                }

                const headingMatch = trimmed.match(/^(#{1,6})\s+(.*)$/);
                if (headingMatch) {
                    flushParagraph();
                    flushList();
                    const level = headingMatch[1].length;
                    html.push('<h' + level + ' class="ai-markdown-heading ai-markdown-heading-' + level + '">' + normalizeInline(headingMatch[2]) + '</h' + level + '>');
                    return;
                }

                const orderedMatch = trimmed.match(/^(\d+)\.\s+(.*)$/);
                if (orderedMatch) {
                    flushParagraph();
                    if (listType && listType !== 'ol') {
                        flushList();
                    }
                    listType = 'ol';
                    listBuffer.push('<li>' + normalizeInline(orderedMatch[2]) + '</li>');
                    return;
                }

                const unorderedMatch = trimmed.match(/^[-*+]\s+(.*)$/);
                if (unorderedMatch) {
                    flushParagraph();
                    if (listType && listType !== 'ul') {
                        flushList();
                    }
                    listType = 'ul';
                    listBuffer.push('<li>' + normalizeInline(unorderedMatch[1]) + '</li>');
                    return;
                }

                if (trimmed === '---' || trimmed === '***') {
                    flushParagraph();
                    flushList();
                    html.push('<hr class="ai-markdown-divider">');
                    return;
                }

                if (trimmed.startsWith('>')) {
                    flushParagraph();
                    flushList();
                    html.push('<blockquote class="ai-markdown-blockquote"><p>' + normalizeInline(trimmed.replace(/^>\s?/, '')) + '</p></blockquote>');
                    return;
                }

                if (listBuffer.length) {
                    flushList();
                }
                paragraph.push(line);
            });

            flushParagraph();
            flushList();
            flushCodeBlock();

            return html.join('');
        }

        async function copyText(text) {
            const content = String(text || '');
            if (!content) {
                return;
            }
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(content);
                return;
            }
            const textarea = document.createElement('textarea');
            textarea.value = content;
            textarea.setAttribute('readonly', 'readonly');
            textarea.style.position = 'fixed';
            textarea.style.opacity = '0';
            textarea.style.pointerEvents = 'none';
            document.body.appendChild(textarea);
            textarea.focus();
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
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
                composerInput.disabled = !available || state.isSending;
            }
            if (fileInput) {
                fileInput.disabled = !available || state.isSending;
            }
            if (sendBtn) {
                sendBtn.disabled = !available || state.isSending;
            }
            chipButtons.forEach((button) => {
                button.disabled = !available || state.isSending;
            });
            if (!available) {
                setStatus('Disabled');
            }
        }

        function setHistoryExpanded(expanded) {
            state.historyExpanded = Boolean(expanded);
            if (historyList) {
                historyList.dataset.collapsed = state.historyExpanded ? 'false' : 'true';
                historyList.classList.toggle('is-collapsed', !state.historyExpanded);
            }
            if (historyToggle) {
                historyToggle.dataset.expanded = state.historyExpanded ? 'true' : 'false';
                historyToggle.setAttribute('aria-expanded', state.historyExpanded ? 'true' : 'false');
                historyToggle.textContent = state.historyExpanded ? '收起' : '展开';
            }
        }

        function setAttachmentPanelExpanded(expanded) {
            state.attachmentPanelExpanded = Boolean(expanded);
            if (pendingPanel) {
                pendingPanel.dataset.collapsed = state.attachmentPanelExpanded ? 'false' : 'true';
                pendingPanel.classList.toggle('is-collapsed', !state.attachmentPanelExpanded);
            }
            if (pendingToggle) {
                pendingToggle.dataset.expanded = state.attachmentPanelExpanded ? 'true' : 'false';
                pendingToggle.setAttribute('aria-expanded', state.attachmentPanelExpanded ? 'true' : 'false');
                pendingToggle.textContent = state.attachmentPanelExpanded ? '收起' : '展开';
            }
        }

        function updateHistoryCollapseState() {
            if (state.sessions.length <= DEFAULT_HISTORY_VISIBLE_COUNT) {
                setHistoryExpanded(false);
                if (historyToggle) {
                    historyToggle.hidden = true;
                }
                return;
            }
            if (historyToggle) {
                historyToggle.hidden = false;
            }
            setHistoryExpanded(state.historyExpanded);
        }

        function updateAttachmentCollapseState() {
            const hasPendingAttachments = state.pendingAttachments.length > 0;
            if (!hasPendingAttachments) {
                setAttachmentPanelExpanded(false);
            } else {
                setAttachmentPanelExpanded(state.attachmentPanelExpanded);
            }
        }

        function renderPendingAttachments() {
            if (pendingCount) {
                pendingCount.textContent = state.pendingAttachments.length + ' 份材料';
            }
            if (!pendingList) {
                updateAttachmentCollapseState();
                return;
            }
            if (!state.pendingAttachments.length) {
                pendingList.innerHTML = '<div class="ai-upload-file ai-upload-file-empty"><strong>暂无待发送附件</strong><span>你可以在这里上传文件，或从课程申请弹窗预载当前简历。</span></div>';
                updateAttachmentCollapseState();
                return;
            }
            pendingList.innerHTML = state.pendingAttachments.map((item) => {
                const sourceLabel = item.sourceType === 'course-apply' ? '来自课程申请' : '手动上传';
                return '<div class="ai-upload-file" data-attachment-id="' + escapeHtml(item.attachmentId) + '">' +
                    '<div class="ai-upload-file-main">' +
                    '<strong>' + escapeHtml(item.originalFileName) + '</strong>' +
                    '<span>' + escapeHtml(sourceLabel) + ' · ' + escapeHtml(formatFileSize(item.size)) + '</span>' +
                    '</div>' +
                    '<button class="ai-upload-file-remove" type="button" data-remove-pending-attachment="' + escapeHtml(item.attachmentId) + '" aria-label="移除附件">×</button>' +
                    '</div>';
            }).join('');
            updateAttachmentCollapseState();
        }

        async function removePendingAttachment(attachmentId) {
            const normalizedAttachmentId = String(attachmentId || '').trim();
            if (!normalizedAttachmentId) {
                return;
            }
            const taId = resolveTaId();
            if (!taId) {
                throw new Error('当前未获取到 TA 身份，请重新登录后再试。');
            }
            const formData = new FormData();
            formData.append('action', 'remove-pending');
            formData.append('taId', taId);
            formData.append('attachmentId', normalizedAttachmentId);

            const response = await fetch(resolveAiApiUrl(), {
                method: 'POST',
                body: formData
            });
            const payload = await response.json();
            if (!response.ok || !payload?.success) {
                throw new Error(payload?.message || '移除待发送附件失败');
            }
            state.pendingAttachments = Array.isArray(payload.data?.pendingAttachments) ? payload.data.pendingAttachments : [];
            state.attachmentPanelExpanded = state.pendingAttachments.length > 0 && state.attachmentPanelExpanded;
            if (payload.data?.serviceStatus) {
                applyServiceStatus(payload.data.serviceStatus);
            }
            renderPendingAttachments();
        }

        function getCurrentSession() {
            return state.sessions.find((item) => item.sessionId === state.sessionId) || null;
        }

        function formatSessionTime(value) {
            const raw = String(value || '').trim();
            if (!raw) return '未开始';
            const date = new Date(raw);
            if (Number.isNaN(date.getTime())) return raw;
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const hour = String(date.getHours()).padStart(2, '0');
            const minute = String(date.getMinutes()).padStart(2, '0');
            return month + '-' + day + ' ' + hour + ':' + minute;
        }

        function getSessionPreview(session) {
            const messages = Array.isArray(session?.messages) ? session.messages : [];
            const firstUserMessage = messages.find((item) => String(item?.role || '') === 'user' && String(item?.content || '').trim());
            const preview = String(firstUserMessage?.content || session?.title || '新会话').trim();
            return preview.length > 20 ? preview.slice(0, 20) + '…' : preview;
        }

        function renderSessionHistory() {
            if (historyCount) {
                historyCount.textContent = state.sessions.length + ' 条';
            }
            if (!historyList) {
                updateHistoryCollapseState();
                return;
            }
            if (!state.sessions.length) {
                historyList.innerHTML = '<div class="ai-session-history-empty">暂无历史会话</div>';
                updateHistoryCollapseState();
                return;
            }
            historyList.innerHTML = state.sessions.map((session) => {
                const sessionId = String(session?.sessionId || '').trim();
                const activeClass = sessionId && sessionId === state.sessionId ? ' active' : '';
                return '<button class="ai-session-item' + activeClass + '" type="button" data-session-id="' + escapeHtml(sessionId) + '">' +
                    '<span class="ai-session-item-title">' + escapeHtml(getSessionPreview(session)) + '</span>' +
                    '<span class="ai-session-item-meta">' + escapeHtml(formatSessionTime(session?.updatedAt || session?.createdAt || '')) + '</span>' +
                    '</button>';
            }).join('');
            updateHistoryCollapseState();
        }

        function normalizeMessage(message) {
            const artifact = message?.artifact && typeof message.artifact === 'object' ? message.artifact : null;
            return {
                messageId: String(message?.messageId || '').trim(),
                role: String(message?.role || '').trim(),
                type: String(message?.type || 'text').trim() || 'text',
                content: String(message?.content || ''),
                createdAt: String(message?.createdAt || ''),
                artifact: artifact
            };
        }

        function ensureSessionMessages(session) {
            if (!session) return [];
            if (!Array.isArray(session.messages)) {
                session.messages = [];
            }
            return session.messages;
        }

        function ensureSessionState() {
            let session = state.sessions.find((item) => item.sessionId === state.sessionId);
            if (!session) {
                session = {
                    sessionId: state.sessionId || '',
                    scene: state.scene || 'general_chat',
                    title: state.title || 'AI 助理对话',
                    status: 'active',
                    createdAt: '',
                    updatedAt: '',
                    provider: '',
                    context: state.context || {},
                    messages: [],
                    attachments: []
                };
                state.sessions = [session].concat(state.sessions.filter((item) => item && item.sessionId !== session.sessionId));
            }
            ensureSessionMessages(session);
            return session;
        }

        function syncStateFromSession(session) {
            if (!session) {
                state.sessionId = '';
                state.scene = 'general_chat';
                state.title = 'AI 助理对话';
                state.context = {};
                return;
            }
            state.sessionId = String(session.sessionId || '').trim();
            state.scene = String(session.scene || 'general_chat').trim() || 'general_chat';
            state.title = String(session.title || 'AI 助理对话').trim() || 'AI 助理对话';
            state.context = session.context && typeof session.context === 'object' ? { ...session.context } : {};
        }

        function openHistorySession(sessionId) {
            const normalizedSessionId = String(sessionId || '').trim();
            if (!normalizedSessionId) {
                return;
            }
            const session = state.sessions.find((item) => String(item?.sessionId || '').trim() === normalizedSessionId);
            if (!session) {
                return;
            }
            syncStateFromSession(session);
            state.activeResponseId = '';
            state.isSending = false;
            if (fileInput) {
                fileInput.value = '';
            }
            if (composerHint) {
                const preview = getSessionPreview(session);
                composerHint.textContent = '已切换到历史会话：' + preview;
            }
            if (state.serviceStatus.available) {
                setStatus('Ready');
            }
            renderSessionHistory();
            renderThread();
            applyServiceStatus(state.serviceStatus);
        }

        function renderMessageBubbleContent(message) {
            const artifact = message.artifact && typeof message.artifact === 'object' ? message.artifact : null;
            const messageHtml = message.role === 'assistant'
                ? renderMarkdown(message.content || '')
                : '<p>' + normalizeInline(message.content || '') + '</p>';
            const copyButton = '<button class="ai-copy-btn" type="button" data-copy-message="' + escapeHtml(message.messageId) + '">复制</button>';
            return '<div class="ai-message-main">' + messageHtml + '</div>' +
                '<div class="ai-message-actions">' + copyButton + '</div>' +
                (artifact && artifact.downloadUrl
                    ? '<div class="ai-message-download"><a class="pill-btn" href="' + escapeHtml(artifact.downloadUrl) + '" download>下载生成的 PDF</a></div>'
                    : '');
        }

        function renderThread() {
            if (!thread) return;
            const currentSession = getCurrentSession();
            if (!currentSession || !Array.isArray(currentSession.messages) || !currentSession.messages.length) {
                thread.innerHTML = '' +
                    '<div class="ai-chat-date">新会话</div>' +
                    '<article class="ai-message ai-message-assistant">' +
                    '  <div class="ai-avatar" aria-hidden="true">AI</div>' +
                    '  <div class="ai-bubble"><div class="ai-message-main"><p>' + escapeHtml(resolveDefaultAssistantMessage()) + '</p></div></div>' +
                    '</article>';
                return;
            }

            const messageHtml = currentSession.messages.map((message) => {
                const normalized = normalizeMessage(message);
                const isUser = normalized.role === 'user';
                return '<article class="ai-message ' + (isUser ? 'ai-message-user' : 'ai-message-assistant') + '" data-message-id="' + escapeHtml(normalized.messageId) + '">' +
                    '<div class="ai-avatar" aria-hidden="true">' + (isUser ? 'TA' : 'AI') + '</div>' +
                    '<div class="ai-bubble">' + renderMessageBubbleContent(normalized) + '</div>' +
                    '</article>';
            }).join('');

            thread.innerHTML = '<div class="ai-chat-date">当前会话</div>' + messageHtml;
            thread.scrollTop = thread.scrollHeight;
        }

        function hydrateConversation(data) {
            const incomingSessions = Array.isArray(data?.sessions)
                ? data.sessions.map((session) => ({
                    ...session,
                    messages: Array.isArray(session?.messages) ? session.messages.map(normalizeMessage) : []
                }))
                : [];
            const hasActiveDraftSession = !state.sessionId && String(composerInput?.value || '').trim() === '';
            const isCurrentDraftEmpty = !state.sessionId
                && !state.pendingAttachments.length
                && !state.isSending
                && (!getCurrentSession() || !Array.isArray(getCurrentSession().messages) || !getCurrentSession().messages.length);

            state.sessions = incomingSessions;
            state.pendingAttachments = Array.isArray(data?.pendingAttachments) ? data.pendingAttachments : [];
            if (data?.serviceStatus) {
                applyServiceStatus(data.serviceStatus);
            }
            if (!state.sessionId && state.sessions.length && !hasActiveDraftSession && !isCurrentDraftEmpty) {
                state.sessionId = String(state.sessions[0].sessionId || '').trim();
            }
            syncStateFromSession(getCurrentSession());
            renderPendingAttachments();
            renderSessionHistory();
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

        async function clearPendingAttachments() {
            const attachments = Array.isArray(state.pendingAttachments) ? state.pendingAttachments.slice() : [];
            if (!attachments.length) {
                state.pendingAttachments = [];
                state.attachmentPanelExpanded = false;
                if (fileInput) {
                    fileInput.value = '';
                }
                renderPendingAttachments();
                return;
            }

            const taId = resolveTaId();
            if (!taId) {
                throw new Error('当前未获取到 TA 身份，请重新登录后再试。');
            }

            for (const attachment of attachments) {
                const attachmentId = String(attachment?.attachmentId || '').trim();
                if (!attachmentId) {
                    continue;
                }
                const formData = new FormData();
                formData.append('action', 'remove-pending');
                formData.append('taId', taId);
                formData.append('attachmentId', attachmentId);

                const response = await fetch(resolveAiApiUrl(), {
                    method: 'POST',
                    body: formData
                });
                const payload = await response.json();
                if (!response.ok || !payload?.success) {
                    throw new Error(payload?.message || '清空待发送附件失败');
                }
                state.pendingAttachments = Array.isArray(payload.data?.pendingAttachments) ? payload.data.pendingAttachments : [];
                if (payload.data?.serviceStatus) {
                    applyServiceStatus(payload.data.serviceStatus);
                }
            }

            state.pendingAttachments = [];
            state.attachmentPanelExpanded = false;
            if (fileInput) {
                fileInput.value = '';
            }
            renderPendingAttachments();
        }

        async function startNewSession(options) {
            const sessionOptions = options && typeof options === 'object' ? options : {};
            const keepPendingAttachments = Boolean(sessionOptions.keepPendingAttachments);
            const nextScene = String(sessionOptions.scene || 'general_chat').trim() || 'general_chat';
            const nextTitle = String(sessionOptions.title || 'AI 助理对话').trim() || 'AI 助理对话';
            const nextContext = sessionOptions.context && typeof sessionOptions.context === 'object'
                ? { ...sessionOptions.context }
                : {};
            const nextHint = String(sessionOptions.hint || '').trim();
            const nextComposerValue = typeof sessionOptions.composerValue === 'string'
                ? sessionOptions.composerValue
                : null;

            setStatus('Loading');
            try {
                if (!keepPendingAttachments) {
                    await clearPendingAttachments();
                }
                state.sessionId = '';
                state.scene = nextScene;
                state.title = nextTitle;
                state.context = nextContext;
                state.activeResponseId = '';
                state.isSending = false;
                state.attachmentPanelExpanded = state.pendingAttachments.length > 0;
                if (composerInput) {
                    composerInput.value = nextComposerValue !== null ? nextComposerValue : '';
                }
                if (composerHint) {
                    composerHint.textContent = nextHint || (state.serviceStatus.available
                        ? '已切换到新的空白会话。你可以继续输入新问题，右侧仍可查阅历史会话。'
                        : (state.serviceStatus.message || '当前 AI 服务不可用'));
                }
                if (state.serviceStatus.available) {
                    setStatus('Ready');
                }
                renderPendingAttachments();
                renderSessionHistory();
                renderThread();
                applyServiceStatus(state.serviceStatus);
            } catch (error) {
                console.error('[TA-AI] start new session failed', error);
                setStatus('Error');
                if (composerHint) {
                    composerHint.textContent = error.message || '新建会话失败，待发送附件未能清空。';
                }
                window.alert(error.message || '新建会话失败，待发送附件未能清空。');
            }
        }

        function appendOptimisticUserMessage(message, responseId) {
            const session = ensureSessionState();
            const messages = ensureSessionMessages(session);
            const now = new Date().toISOString();
            messages.push(normalizeMessage({
                messageId: 'client_user_' + responseId,
                role: 'user',
                type: 'text',
                content: message,
                createdAt: now
            }));
            messages.push(normalizeMessage({
                messageId: 'client_assistant_' + responseId,
                role: 'assistant',
                type: 'text',
                content: '',
                createdAt: now
            }));
            renderThread();
        }

        function updateStreamingAssistantMessage(responseId, content, done, artifact) {
            const session = ensureSessionState();
            const messages = ensureSessionMessages(session);
            const targetId = 'client_assistant_' + responseId;
            let target = messages.find((item) => item.messageId === targetId);
            if (!target) {
                target = normalizeMessage({
                    messageId: targetId,
                    role: 'assistant',
                    type: 'text',
                    content: '',
                    createdAt: new Date().toISOString()
                });
                messages.push(target);
            }
            target.content = String(content || '');
            if (artifact && typeof artifact === 'object') {
                target.artifact = artifact;
            }
            if (done && !target.content.trim()) {
                target.content = 'AI 未返回可展示内容。';
            }
            renderThread();
        }

        function decodeStreamChunk(chunk) {
            const lines = String(chunk || '').split(/\r?\n/);
            const events = [];
            for (const rawLine of lines) {
                const line = rawLine.trim();
                if (!line || !line.startsWith('data:')) {
                    continue;
                }
                const jsonText = line.slice(5).trim();
                if (!jsonText) {
                    continue;
                }
                try {
                    events.push(JSON.parse(jsonText));
                } catch (error) {
                    console.warn('[TA-AI] stream chunk parse failed', error, jsonText);
                }
            }
            return events;
        }

        async function consumeChatStream(response, responseId) {
            if (!response.body || typeof response.body.getReader !== 'function') {
                throw new Error('当前浏览器不支持流式读取 AI 回复');
            }
            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = '';
            let fullText = '';
            let finalPayload = null;

            while (true) {
                const result = await reader.read();
                if (result.done) {
                    break;
                }
                buffer += decoder.decode(result.value, { stream: true });
                const segments = buffer.split(/\r?\n\r?\n/);
                buffer = segments.pop() || '';
                for (const segment of segments) {
                    const events = decodeStreamChunk(segment);
                    events.forEach((event) => {
                        if (event?.type === 'delta') {
                            const delta = String(event.delta || '');
                            if (delta) {
                                fullText += delta;
                                updateStreamingAssistantMessage(responseId, fullText, false, null);
                            }
                        } else if (event?.type === 'complete') {
                            finalPayload = event.data || null;
                            const finalText = String(event.data?.reply || fullText || '');
                            fullText = finalText;
                            updateStreamingAssistantMessage(responseId, fullText, true, event.data?.artifact || null);
                        } else if (event?.type === 'error') {
                            throw new Error(event.message || 'AI 对话失败');
                        }
                    });
                }
            }

            buffer += decoder.decode();
            if (buffer.trim()) {
                const events = decodeStreamChunk(buffer);
                events.forEach((event) => {
                    if (event?.type === 'delta') {
                        const delta = String(event.delta || '');
                        if (delta) {
                            fullText += delta;
                        }
                    } else if (event?.type === 'complete') {
                        finalPayload = event.data || null;
                        fullText = String(event.data?.reply || fullText || '');
                    } else if (event?.type === 'error') {
                        throw new Error(event.message || 'AI 对话失败');
                    }
                });
            }

            updateStreamingAssistantMessage(responseId, fullText, true, finalPayload?.artifact || null);
            return finalPayload || {
                reply: fullText,
                conversation: null,
                pendingAttachments: [],
                serviceStatus: state.serviceStatus,
                sessionId: state.sessionId
            };
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
            if (state.isSending) {
                return;
            }
            const message = String(composerInput?.value || '').trim();
            if (!message && !state.pendingAttachments.length) {
                window.alert('请先输入消息，或准备至少一个附件。');
                return;
            }

            const responseId = Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
            state.isSending = true;
            state.activeResponseId = responseId;
            appendOptimisticUserMessage(message, responseId);
            if (composerInput) {
                composerInput.value = '';
            }
            applyServiceStatus(state.serviceStatus);
            setStatus('Streaming');
            if (composerHint) {
                composerHint.textContent = 'AI 正在流式生成结果…';
            }

            try {
                const response = await fetch(resolveAiApiUrl() + '?stream=true', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        Accept: 'text/event-stream, application/json'
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

                if (!response.ok) {
                    let failureMessage = 'AI 对话失败';
                    try {
                        const payload = await response.json();
                        failureMessage = payload?.message || failureMessage;
                    } catch (error) {
                        const failureText = await response.text();
                        if (failureText) {
                            failureMessage = failureText;
                        }
                    }
                    throw new Error(failureMessage);
                }

                const result = await consumeChatStream(response, responseId);
                state.sessionId = String(result?.sessionId || state.sessionId || '').trim();
                if (result?.conversation) {
                    hydrateConversation(result.conversation);
                }
                state.pendingAttachments = [];
                state.attachmentPanelExpanded = false;
                if (fileInput) {
                    fileInput.value = '';
                }
                renderPendingAttachments();
                renderThread();
                if (result?.serviceStatus) {
                    applyServiceStatus(result.serviceStatus);
                }
                setStatus('Completed');
                if (composerHint) {
                    composerHint.textContent = 'AI 回复已完成。';
                }
            } catch (error) {
                console.error('[TA-AI] send failed', error);
                updateStreamingAssistantMessage(responseId, error.message || 'AI 助理处理失败，请稍后重试。', true, null);
                setStatus('Error');
                if (composerHint) {
                    composerHint.textContent = error.message || 'AI 助理处理失败，请稍后重试。';
                }
                window.alert(error.message || 'AI 助理处理失败，请稍后重试。');
            } finally {
                state.isSending = false;
                state.activeResponseId = '';
                applyServiceStatus(state.serviceStatus);
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
                    composerHint.textContent = '正在为课程申请创建新的 AI 会话并载入简历…';
                }
                if (typeof app.openModal === 'function') {
                    app.openModal('planner');
                }

                await startNewSession({
                    keepPendingAttachments: false,
                    scene: 'resume_optimize',
                    title: '课程申请简历优化',
                    context: {
                        courseCode: payload.courseCode || '',
                        applicationId: payload.applicationId || '',
                        sourcePath: payload.sourcePath || ''
                    },
                    composerValue: '请根据当前课程申请要求，帮我优化这份简历，并指出最值得优先调整的内容。',
                    hint: '新会话已创建，正在载入课程申请简历…'
                });

                await uploadPendingFile(payload.file, {
                    sourceType: 'course-apply',
                    sourcePath: payload.sourcePath || '',
                    courseCode: payload.courseCode || '',
                    applicationId: payload.applicationId || ''
                });

                state.attachmentPanelExpanded = true;
                if (composerHint) {
                    composerHint.textContent = '已为课程申请创建新会话，简历 PDF 已载入待发送附件，请确认消息后点击发送。';
                }
                setStatus('Ready');
                renderPendingAttachments();
                renderSessionHistory();
                renderThread();
            } catch (error) {
                console.error('[TA-AI] preload failed', error);
                setStatus('Error');
                if (composerHint) {
                    composerHint.textContent = error.message || '载入课程申请简历失败。';
                }
                window.alert(error.message || '载入课程申请简历失败。');
            }
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
            state.attachmentPanelExpanded = state.pendingAttachments.length > 0;
            if (payload.data?.serviceStatus) {
                applyServiceStatus(payload.data.serviceStatus);
            }
            renderPendingAttachments();
            return payload.data || {};
        }

        thread?.addEventListener('click', async (event) => {
            const copyButton = event.target.closest('[data-copy-message]');
            if (!copyButton) {
                return;
            }
            const messageId = copyButton.getAttribute('data-copy-message') || '';
            const currentSession = getCurrentSession();
            const message = currentSession?.messages?.find((item) => String(item?.messageId || '') === messageId);
            if (!message) {
                return;
            }
            const originalLabel = copyButton.textContent;
            try {
                await copyText(message.content || '');
                copyButton.textContent = '已复制';
                window.setTimeout(() => {
                    copyButton.textContent = originalLabel || '复制';
                }, 1500);
            } catch (error) {
                console.error('[TA-AI] copy failed', error);
                window.alert('复制失败，请手动选择内容后复制。');
            }
        });

        pendingList?.addEventListener('click', async (event) => {
            const removeButton = event.target.closest('[data-remove-pending-attachment]');
            if (!removeButton) {
                return;
            }
            const attachmentId = removeButton.getAttribute('data-remove-pending-attachment') || '';
            const originalLabel = removeButton.textContent;
            try {
                removeButton.disabled = true;
                removeButton.textContent = '…';
                setStatus('Loading');
                await removePendingAttachment(attachmentId);
                if (state.serviceStatus.available) {
                    setStatus('Ready');
                }
            } catch (error) {
                console.error('[TA-AI] remove pending attachment failed', error);
                setStatus('Error');
                removeButton.disabled = false;
                removeButton.textContent = originalLabel || '×';
                window.alert(error.message || '移除待发送附件失败。');
            }
        });

        historyList?.addEventListener('click', (event) => {
            const sessionButton = event.target.closest('[data-session-id]');
            if (!sessionButton) {
                return;
            }
            const sessionId = sessionButton.getAttribute('data-session-id') || '';
            openHistorySession(sessionId);
        });

        historyToggle?.addEventListener('click', () => {
            setHistoryExpanded(!state.historyExpanded);
        });

        pendingToggle?.addEventListener('click', () => {
            setAttachmentPanelExpanded(!state.attachmentPanelExpanded);
        });

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

        newSessionBtn?.addEventListener('click', () => {
            startNewSession();
        });
        sendBtn?.addEventListener('click', sendMessage);
        composerInput?.addEventListener('keydown', (event) => {
            if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
                event.preventDefault();
                sendMessage();
            }
        });

        app.openAiAssistantWithAttachment = preloadFromCourseApply;
        app.loadAiConversation = loadConversation;

        setHistoryExpanded(state.historyExpanded);
        setAttachmentPanelExpanded(state.attachmentPanelExpanded);
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
            renderSessionHistory();
            renderThread();
        });
    };
})();
