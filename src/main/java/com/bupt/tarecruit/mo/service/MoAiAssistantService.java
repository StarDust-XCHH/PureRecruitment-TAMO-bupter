package com.bupt.tarecruit.mo.service;

import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.bupt.tarecruit.common.ai.AiProvider;
import com.bupt.tarecruit.common.ai.QwenLongAiProvider;
import com.bupt.tarecruit.mo.dao.MoAiConversationDao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class MoAiAssistantService {
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
    private static final int MAX_CHAT_HISTORY_MESSAGES = 12;

    private final MoAiConversationDao conversationDao = new MoAiConversationDao();
    private final AiProvider provider;

    public MoAiAssistantService() {
        this(new QwenLongAiProvider("You are the AI assistant in the PureRecruitment MO workspace. Reply in English."));
    }

    MoAiAssistantService(AiProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public Map<String, Object> loadConversation(String moId) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>(conversationDao.getOrCreateConversation(moId).data());
        data.put("serviceStatus", buildServiceStatus());
        return data;
    }

    public Map<String, Object> getServiceStatus() {
        return buildServiceStatus();
    }

    public Map<String, Object> uploadPendingAttachment(String moId,
                                                       String originalFileName,
                                                       String contentType,
                                                       long size,
                                                       InputStream inputStream,
                                                       String sourceType,
                                                       String sourcePath,
                                                       String courseCode,
                                                       String applicationId) throws IOException {
        if (size > MAX_ATTACHMENT_SIZE) {
            throw new IOException("AI attachment size cannot exceed 10MB");
        }
        byte[] bytes = readAllBytes(inputStream, MAX_ATTACHMENT_SIZE);
        MoAiConversationDao.PendingAttachment attachment = conversationDao.createPendingAttachment(
                moId,
                originalFileName,
                contentType,
                size > 0 ? size : bytes.length,
                bytes,
                sourceType,
                sourcePath,
                courseCode,
                applicationId
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attachmentId", attachment.attachmentId());
        result.put("originalFileName", attachment.originalFileName());
        result.put("mimeType", attachment.mimeType());
        result.put("size", attachment.size());
        result.put("storedPath", attachment.storedPath());
        result.put("sourceType", attachment.sourceType());
        result.put("sourcePath", attachment.sourcePath());
        result.put("courseCode", attachment.courseCode());
        result.put("applicationId", attachment.applicationId());
        result.put("pendingAttachments", conversationDao.listPendingAttachments(moId));
        result.put("serviceStatus", buildServiceStatus());
        return result;
    }

    public Map<String, Object> removePendingAttachment(String moId, String attachmentId) throws IOException {
        conversationDao.removePendingAttachment(moId, attachmentId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pendingAttachments", conversationDao.listPendingAttachments(moId));
        result.put("serviceStatus", buildServiceStatus());
        return result;
    }

    public Map<String, Object> sendMessage(String moId,
                                           String sessionId,
                                           String message,
                                           List<String> attachmentIds,
                                           String scene,
                                           String title,
                                           Map<String, Object> context) throws IOException {
        return sendMessageStream(moId, sessionId, message, attachmentIds, scene, title, context, null);
    }

    public Map<String, Object> sendMessageStream(String moId,
                                                 String sessionId,
                                                 String message,
                                                 List<String> attachmentIds,
                                                 String scene,
                                                 String title,
                                                 Map<String, Object> context,
                                                 Consumer<String> chunkConsumer) throws IOException {
        AiProvider.AiProviderStatus providerStatus = provider.getStatus();
        if (!providerStatus.available()) {
            throw new IOException(providerStatus.message().isBlank()
                    ? "AI service is currently unavailable"
                    : localizeUserMessage(providerStatus.message()));
        }

        String normalizedMessage = message == null ? "" : message.trim();
        String resolvedSessionId = sessionId;
        if (resolvedSessionId == null || resolvedSessionId.trim().isEmpty()) {
            resolvedSessionId = "sess_stream_" + UUID.randomUUID().toString().replace("-", "");
        }

        List<String> effectiveAttachmentIds = attachmentIds == null ? List.of() : attachmentIds;
        Map<String, Object> existingConversation = conversationDao.getOrCreateConversation(moId).data();
        AiProvider.AiChatRequest chatRequest = buildChatRequest(
                moId,
                resolvedSessionId,
                normalizedMessage,
                context,
                existingConversation,
                effectiveAttachmentIds,
                scene,
                title
        );
        AiProvider.AiChatResult aiChatResult;
        try {
            aiChatResult = provider.chat(chatRequest, chunkConsumer);
        } catch (IOException ex) {
            throw new IOException(localizeUserMessage(ex.getMessage()), ex);
        }

        if (sessionId == null || sessionId.trim().isEmpty()) {
            resolvedSessionId = conversationDao.createSession(moId, scene, title, context);
        }

        for (String attachmentId : effectiveAttachmentIds) {
            conversationDao.consumePendingAttachment(moId, attachmentId, resolvedSessionId);
        }

        conversationDao.appendUserMessageAndPendingAttachments(moId, resolvedSessionId, normalizedMessage, scene, effectiveAttachmentIds, context);

        Map<String, Object> snapshot = conversationDao.appendAssistantResult(
                moId,
                resolvedSessionId,
                aiChatResult.text(),
                null,
                aiChatResult.providerName()
        ).data();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", resolvedSessionId);
        result.put("reply", aiChatResult.text());
        result.put("downloadUrl", "");
        result.put("generatedAttachmentId", "");
        result.put("generatedFileName", "");
        result.put("artifact", null);
        result.put("conversation", snapshot);
        result.put("pendingAttachments", conversationDao.listPendingAttachments(moId));
        result.put("serviceStatus", buildServiceStatus());
        return result;
    }

    public MoAiConversationDao.DownloadFile getGeneratedFile(String moId, String sessionId, String attachmentId) throws IOException {
        return conversationDao.findGeneratedFile(moId, sessionId, attachmentId);
    }

    private AiProvider.AiChatRequest buildChatRequest(String moId,
                                                      String sessionId,
                                                      String currentMessage,
                                                      Map<String, Object> context,
                                                      Map<String, Object> conversation,
                                                      List<String> attachmentIds,
                                                      String scene,
                                                      String title) throws IOException {
        List<Message> providerMessages = new ArrayList<>();
        String systemPrompt = buildSystemPrompt(context);

        List<Map<String, Object>> sessions = castList(conversation.get("sessions"));
        Map<String, Object> currentSession = null;
        for (Map<String, Object> session : sessions) {
            if (sessionId.equals(String.valueOf(session.getOrDefault("sessionId", "")).trim())) {
                currentSession = session;
                break;
            }
        }

        if (currentSession == null) {
            currentSession = new LinkedHashMap<>();
            currentSession.put("sessionId", sessionId);
            currentSession.put("scene", scene == null ? "general_chat" : scene);
            currentSession.put("title", title == null ? "MO AI Chat" : title);
            currentSession.put("messages", List.of());
            currentSession.put("attachments", List.of());
            currentSession.put("context", context == null ? Map.of() : new LinkedHashMap<>(context));
        }

        List<Map<String, Object>> messages = castList(currentSession.get("messages"));
        int startIndex = Math.max(0, messages.size() - MAX_CHAT_HISTORY_MESSAGES);
        for (int i = startIndex; i < messages.size(); i++) {
            Map<String, Object> item = messages.get(i);
            String role = String.valueOf(item.getOrDefault("role", "")).trim().toLowerCase(Locale.ROOT);
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            String content = String.valueOf(item.getOrDefault("content", "")).trim();
            if (content.isEmpty()) {
                continue;
            }
            providerMessages.add(Message.builder()
                    .role(role)
                    .content(content)
                    .build());
        }

        if (!currentMessage.isBlank()) {
            providerMessages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(currentMessage)
                    .build());
        }

        List<AiProvider.AiAttachmentRef> attachmentRefs = new ArrayList<>();
        for (String attachmentId : attachmentIds == null ? List.<String>of() : attachmentIds) {
            MoAiConversationDao.PendingAttachment attachment = conversationDao.findPendingAttachment(moId, attachmentId);
            if (attachment == null) {
                continue;
            }
            Path attachmentPath = conversationDao.resolveStoredPathForService(attachment.storedPath());
            attachmentRefs.add(new AiProvider.AiAttachmentRef(
                    attachment.attachmentId(),
                    attachment.originalFileName(),
                    attachment.mimeType(),
                    attachmentPath,
                    attachment.sourceType(),
                    attachment.sourcePath(),
                    attachment.courseCode(),
                    attachment.applicationId()
            ));
        }

        Map<String, Object> requestContext = context == null ? new LinkedHashMap<>() : new LinkedHashMap<>(context);
        requestContext.put("_systemPrompt", systemPrompt);
        return new AiProvider.AiChatRequest(moId, sessionId, currentMessage, requestContext, providerMessages, attachmentRefs);
    }

    private String buildSystemPrompt(Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are the AI assistant in the PureRecruitment MO workspace.")
                .append("Use the MO recruitment context, conversation history, and uploaded materials (job briefs, applicant resumes, etc.) to give honest, direct, actionable screening and hiring advice.")
                .append("Reply in English. Do not invent facts that are not supported by the materials.")
                .append("If the user uploaded files, prioritize their content; if a file is not readable yet, say so clearly.")
                .append("Structure answers with bullet points when helpful. Do not claim a PDF was generated or exported unless the user explicitly asks and the system actually provides that capability.")
                .append("Return plain text only; do not wrap the entire answer in ```markdown or ```text fences.");

        if (context != null && !context.isEmpty()) {
            String courseCode = String.valueOf(context.getOrDefault("courseCode", "")).trim();
            String applicationId = String.valueOf(context.getOrDefault("applicationId", "")).trim();
            String sourcePath = String.valueOf(context.getOrDefault("sourcePath", "")).trim();
            if (!courseCode.isEmpty() || !applicationId.isEmpty() || !sourcePath.isEmpty()) {
                prompt.append(" Current context:");
                if (!courseCode.isEmpty()) {
                    prompt.append("courseCode=").append(courseCode).append("; ");
                }
                if (!applicationId.isEmpty()) {
                    prompt.append("applicationId=").append(applicationId).append("; ");
                }
                if (!sourcePath.isEmpty()) {
                    prompt.append("sourcePath=").append(sourcePath).append("; ");
                }
            }
        }
        return prompt.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private Map<String, Object> buildServiceStatus() {
        AiProvider.AiProviderStatus status = provider.getStatus();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", status.providerName());
        result.put("available", status.available());
        result.put("configured", status.configured());
        result.put("message", localizeUserMessage(status.message()));
        result.put("defaultProvider", true);
        return result;
    }

    private String localizeUserMessage(String message) {
        String normalized = message == null ? "" : message.trim();
        if (normalized.isEmpty()) {
            return "AI service status unknown";
        }
        if (normalized.contains("TONGYI_API_KEY") && normalized.contains("禁用")) {
            return "TONGYI_API_KEY is not set on the server; AI assistant is disabled.";
        }
        if (normalized.contains("已连接默认服务")) {
            return "Connected: qwen-long";
        }
        if (normalized.contains("TONGYI_API_KEY") && normalized.contains("无法调用")) {
            return "TONGYI_API_KEY is not set on the server; cannot call AI service.";
        }
        if ("AI 服务未返回有效内容".equals(normalized)) {
            return "AI service returned no valid content.";
        }
        if (normalized.startsWith("AI 服务调用失败:")) {
            return "AI service call failed:" + normalized.substring("AI 服务调用失败:".length());
        }
        if (normalized.startsWith("找不到附件文件:")) {
            return "Attachment file not found:" + normalized.substring("找不到附件文件:".length());
        }
        if (normalized.startsWith("附件解析时间过长，请稍后重试：")) {
            return "Attachment parsing took too long, try again later:" + normalized.substring("附件解析时间过长，请稍后重试：".length());
        }
        if (normalized.startsWith("附件解析失败：")) {
            return "Attachment parsing failed:" + normalized.substring("附件解析失败：".length());
        }
        if (normalized.startsWith("附件解析检测失败:")) {
            return "Attachment parsing check failed:" + normalized.substring("附件解析检测失败:".length());
        }
        if (normalized.startsWith("附件上传失败")) {
            return "Attachment upload failed" + normalized.substring("附件上传失败".length());
        }
        if (normalized.startsWith("无法解析 file_id:")) {
            return "Cannot parse file_id:" + normalized.substring("无法解析 file_id:".length());
        }
        if ("附件仍在解析中，请稍后重试".equals(normalized)) {
            return "Attachment is still being parsed; try again later.";
        }
        if ("AI 服务调用异常".equals(normalized)) {
            return "AI service error";
        }
        if ("等待附件解析时被中断".equals(normalized)) {
            return "Interrupted while waiting for attachment parsing";
        }
        if (normalized.startsWith("文件尚未完成解析:")) {
            return "File parsing not complete yet:" + normalized.substring("文件尚未完成解析:".length());
        }
        return normalized;
    }

    private byte[] readAllBytes(InputStream inputStream, long maxSize) throws IOException {
        try (InputStream in = inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxSize) {
                    throw new IOException("AI attachment size cannot exceed 10MB");
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }
}
