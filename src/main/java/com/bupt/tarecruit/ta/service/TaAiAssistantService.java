package com.bupt.tarecruit.ta.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.bupt.tarecruit.ta.dao.TaAiConversationDao;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.reactivex.Flowable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TaAiAssistantService {
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
    private static final int MAX_CHAT_HISTORY_MESSAGES = 12;
    private static final int MAX_FILE_PARSE_WAIT_RETRIES = 5;
    private static final long FILE_PARSE_WAIT_MILLIS = 1500L;
    private static final Gson GSON = new Gson();

    private final TaAiConversationDao conversationDao = new TaAiConversationDao();
    private final AiProvider provider;

    public TaAiAssistantService() {
        this(new QwenLongAiProvider());
    }

    TaAiAssistantService(AiProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public Map<String, Object> loadConversation(String taId) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>(conversationDao.getOrCreateConversation(taId).data());
        data.put("serviceStatus", buildServiceStatus());
        return data;
    }

    public Map<String, Object> getServiceStatus() {
        return buildServiceStatus();
    }

    public Map<String, Object> uploadPendingAttachment(String taId,
                                                       String originalFileName,
                                                       String contentType,
                                                       long size,
                                                       InputStream inputStream,
                                                       String sourceType,
                                                       String sourcePath,
                                                       String courseCode,
                                                       String applicationId) throws IOException {
        if (size > MAX_ATTACHMENT_SIZE) {
            throw new IOException("AI 附件大小不能超过 10MB");
        }
        byte[] bytes = readAllBytes(inputStream, MAX_ATTACHMENT_SIZE);
        TaAiConversationDao.PendingAttachment attachment = conversationDao.createPendingAttachment(
                taId,
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
        result.put("pendingAttachments", conversationDao.listPendingAttachments(taId));
        result.put("serviceStatus", buildServiceStatus());
        return result;
    }

    public Map<String, Object> sendMessage(String taId,
                                           String sessionId,
                                           String message,
                                           List<String> attachmentIds,
                                           String scene,
                                           String title,
                                           Map<String, Object> context) throws IOException {
        AiProviderStatus providerStatus = provider.getStatus();
        if (!providerStatus.available()) {
            throw new IOException(providerStatus.message().isBlank() ? "当前 AI 服务不可用" : providerStatus.message());
        }

        String normalizedMessage = message == null ? "" : message.trim();
        String resolvedSessionId = sessionId;
        if (resolvedSessionId == null || resolvedSessionId.trim().isEmpty()) {
            resolvedSessionId = conversationDao.createSession(taId, scene, title, context);
        }

        List<String> effectiveAttachmentIds = attachmentIds == null ? List.of() : attachmentIds;
        List<TaAiConversationDao.PendingAttachment> linkedAttachments = new ArrayList<>();
        for (String attachmentId : effectiveAttachmentIds) {
            TaAiConversationDao.PendingAttachment linkedAttachment = conversationDao.consumePendingAttachment(taId, attachmentId, resolvedSessionId);
            if (linkedAttachment != null) {
                linkedAttachments.add(linkedAttachment);
            }
        }

        conversationDao.appendUserMessageAndPendingAttachments(taId, resolvedSessionId, normalizedMessage, scene, effectiveAttachmentIds, context);

        TaAiConversationDao.ConversationSnapshot snapshotAfterUser = conversationDao.getOrCreateConversation(taId);
        AiChatRequest chatRequest = buildChatRequest(taId, resolvedSessionId, normalizedMessage, context, snapshotAfterUser.data(), linkedAttachments);
        AiChatResult aiChatResult = provider.chat(chatRequest);

        Map<String, Object> snapshot = conversationDao.appendAssistantResult(
                taId,
                resolvedSessionId,
                aiChatResult.text(),
                null,
                aiChatResult.providerName()
        ).data();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", resolvedSessionId);
        result.put("downloadUrl", "");
        result.put("generatedAttachmentId", "");
        result.put("generatedFileName", "");
        result.put("conversation", snapshot);
        result.put("pendingAttachments", conversationDao.listPendingAttachments(taId));
        result.put("serviceStatus", buildServiceStatus());
        return result;
    }

    public TaAiConversationDao.DownloadFile getGeneratedFile(String taId, String sessionId, String attachmentId) throws IOException {
        return conversationDao.findGeneratedFile(taId, sessionId, attachmentId);
    }

    private AiChatRequest buildChatRequest(String taId,
                                           String sessionId,
                                           String currentMessage,
                                           Map<String, Object> context,
                                           Map<String, Object> conversation,
                                           List<TaAiConversationDao.PendingAttachment> linkedAttachments) throws IOException {
        List<Message> providerMessages = new ArrayList<>();
        providerMessages.add(Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(buildSystemPrompt(context))
                .build());

        List<Map<String, Object>> sessions = castList(conversation.get("sessions"));
        Map<String, Object> currentSession = null;
        for (Map<String, Object> session : sessions) {
            if (sessionId.equals(String.valueOf(session.getOrDefault("sessionId", "")).trim())) {
                currentSession = session;
                break;
            }
        }

        List<Map<String, Object>> messages = currentSession == null
                ? List.of()
                : castList(currentSession.get("messages"));
        int startIndex = Math.max(0, messages.size() - MAX_CHAT_HISTORY_MESSAGES);
        for (int i = startIndex; i < messages.size(); i++) {
            Map<String, Object> item = messages.get(i);
            String role = String.valueOf(item.getOrDefault("role", "")).trim().toLowerCase(Locale.ROOT);
            if (!"user".equals(role) && !"assistant".equals(role) && !"system".equals(role)) {
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

        List<AiAttachmentRef> attachmentRefs = new ArrayList<>();
        for (TaAiConversationDao.PendingAttachment attachment : linkedAttachments) {
            Path attachmentPath = conversationDao.resolveStoredPathForService(attachment.storedPath());
            attachmentRefs.add(new AiAttachmentRef(
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

        return new AiChatRequest(taId, sessionId, currentMessage, context == null ? Map.of() : new LinkedHashMap<>(context), providerMessages, attachmentRefs);
    }

    private String buildSystemPrompt(Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 PureRecruitment TA 页面中的 AI 助理。")
                .append("你的职责是基于 TA 当前问题、历史会话和上传材料，提供真实、直接、可执行的建议。")
                .append("请使用简体中文回答，避免编造未在材料中出现的具体事实。")
                .append("如果用户上传了文件，请优先结合文件内容分析；如果文件暂时不可读取，请明确说明。")
                .append("回答请尽量结构化，必要时使用分点。不要声称已经生成或导出了 PDF，除非用户明确询问且系统真的提供了该能力。");

        if (context != null && !context.isEmpty()) {
            String courseCode = String.valueOf(context.getOrDefault("courseCode", "")).trim();
            String applicationId = String.valueOf(context.getOrDefault("applicationId", "")).trim();
            String sourcePath = String.valueOf(context.getOrDefault("sourcePath", "")).trim();
            if (!courseCode.isEmpty() || !applicationId.isEmpty() || !sourcePath.isEmpty()) {
                prompt.append(" 当前业务上下文：");
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
        AiProviderStatus status = provider.getStatus();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", status.providerName());
        result.put("available", status.available());
        result.put("configured", status.configured());
        result.put("message", status.message());
        result.put("defaultProvider", true);
        return result;
    }

    private byte[] readAllBytes(InputStream inputStream, long maxSize) throws IOException {
        try (InputStream in = inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxSize) {
                    throw new IOException("AI 附件大小不能超过 10MB");
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    interface AiProvider {
        AiProviderStatus getStatus();

        AiChatResult chat(AiChatRequest request) throws IOException;
    }

    record AiProviderStatus(String providerName,
                            boolean configured,
                            boolean available,
                            String message) {
    }

    record AiChatRequest(String taId,
                         String sessionId,
                         String userMessage,
                         Map<String, Object> context,
                         List<Message> messages,
                         List<AiAttachmentRef> attachments) {
    }

    record AiAttachmentRef(String attachmentId,
                           String fileName,
                           String mimeType,
                           Path path,
                           String sourceType,
                           String sourcePath,
                           String courseCode,
                           String applicationId) {
    }

    record AiChatResult(String providerName,
                        String text) {
    }

    static class QwenLongAiProvider implements AiProvider {
        private static final String API_KEY_ENV = "TONGYI_API_KEY";
        private static final String MODEL = "qwen-long";
        private static final String FILE_UPLOAD_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/files";
        private static final MediaType MEDIA_TYPE_PDF = MediaType.parse("application/pdf");
        private static final Duration CALL_TIMEOUT = Duration.ofMinutes(3);

        private final OkHttpClient httpClient = new OkHttpClient.Builder()
                .callTimeout(CALL_TIMEOUT)
                .build();

        @Override
        public AiProviderStatus getStatus() {
            String apiKey = readApiKey();
            if (apiKey.isEmpty()) {
                return new AiProviderStatus(MODEL, false, false, "服务端未读取到环境变量 TONGYI_API_KEY，AI 助理已禁用");
            }
            return new AiProviderStatus(MODEL, true, true, "已连接默认服务：qwen-long");
        }

        @Override
        public AiChatResult chat(AiChatRequest request) throws IOException {
            String apiKey = readApiKey();
            if (apiKey.isEmpty()) {
                throw new IOException("服务端未读取到环境变量 TONGYI_API_KEY，无法调用 AI 服务");
            }

            List<Message> providerMessages = new ArrayList<>(request.messages());
            for (AiAttachmentRef attachment : request.attachments()) {
                providerMessages.add(Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content("fileid://" + uploadAndEnsureParsed(apiKey, attachment))
                        .build());
            }

            Generation generation = new Generation();
            try {
                GenerationParam param = GenerationParam.builder()
                        .apiKey(apiKey)
                        .model(MODEL)
                        .messages(providerMessages)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .incrementalOutput(true)
                        .build();

                Flowable<GenerationResult> result = generation.streamCall(param);
                StringBuilder fullResponse = new StringBuilder();
                result.blockingForEach(item -> {
                    String incrementalText = extractAssistantText(item);
                    if (incrementalText != null && !incrementalText.isBlank()) {
                        fullResponse.append(incrementalText);
                    }
                });

                String text = fullResponse.toString().trim();
                if (text.isEmpty()) {
                    throw new IOException("AI 服务未返回有效内容");
                }
                return new AiChatResult(MODEL, text);
            } catch (ApiException ex) {
                throw new IOException(resolveApiExceptionMessage(ex), ex);
            } catch (RuntimeException ex) {
                throw new IOException("AI 服务调用失败: " + ex.getMessage(), ex);
            } catch (Exception ex) {
                if (ex instanceof IOException ioException) {
                    throw ioException;
                }
                throw new IOException("AI 服务调用失败: " + ex.getMessage(), ex);
            }
        }

        private String uploadAndEnsureParsed(String apiKey, AiAttachmentRef attachment) throws IOException {
            Path path = attachment.path();
            if (path == null || !Files.exists(path) || Files.isDirectory(path)) {
                throw new FileNotFoundException("找不到附件文件: " + (path == null ? "unknown" : path));
            }

            String fileId = uploadFile(apiKey, path, attachment.mimeType(), attachment.fileName());
            for (int attempt = 0; attempt <= MAX_FILE_PARSE_WAIT_RETRIES; attempt++) {
                try {
                    return ensureFileReady(apiKey, fileId, attachment.fileName());
                } catch (FileParsingPendingException ex) {
                    if (attempt >= MAX_FILE_PARSE_WAIT_RETRIES) {
                        throw new IOException("附件解析时间过长，请稍后重试：" + attachment.fileName(), ex);
                    }
                    sleepQuietly(FILE_PARSE_WAIT_MILLIS);
                }
            }
            throw new IOException("附件解析失败：" + attachment.fileName());
        }

        private String ensureFileReady(String apiKey, String fileId, String fileName) throws IOException {
            Generation generation = new Generation();
            try {
                List<Message> probeMessages = List.of(
                        Message.builder().role(Role.SYSTEM.getValue()).content("fileid://" + fileId).build(),
                        Message.builder().role(Role.USER.getValue()).content("请只回复“READY”。").build()
                );
                GenerationParam probeParam = GenerationParam.builder()
                        .apiKey(apiKey)
                        .model(MODEL)
                        .messages(probeMessages)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .incrementalOutput(false)
                        .build();
                generation.call(probeParam);
                return fileId;
            } catch (ApiException ex) {
                String message = ex.getMessage() == null ? "" : ex.getMessage();
                if (message.contains("file has not finished parsing")) {
                    throw new FileParsingPendingException(fileName, ex);
                }
                throw new IOException(resolveApiExceptionMessage(ex), ex);
            } catch (Exception ex) {
                if (ex instanceof IOException ioException) {
                    throw ioException;
                }
                throw new IOException("附件解析检测失败: " + ex.getMessage(), ex);
            }
        }

        private String uploadFile(String apiKey, Path path, String mimeType, String fileName) throws IOException {
            byte[] bytes = Files.readAllBytes(path);
            String resolvedMimeType = normalizeMimeType(mimeType, fileName);
            RequestBody fileBody = RequestBody.create(MEDIA_TYPE_PDF.equals(MediaType.parse(resolvedMimeType)) ? MEDIA_TYPE_PDF : MediaType.parse(resolvedMimeType), bytes);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, fileBody)
                    .addFormDataPart("purpose", "file-extract")
                    .build();

            Request request = new Request.Builder()
                    .url(FILE_UPLOAD_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseStr = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    throw new IOException("附件上传失败，状态码: " + response.code() + "，返回体: " + responseStr);
                }
                JsonObject jsonObject = JsonParser.parseString(responseStr).getAsJsonObject();
                String fileId = jsonObject.has("id") ? jsonObject.get("id").getAsString() : "";
                if (fileId.isBlank()) {
                    throw new IOException("无法解析 file_id: " + responseStr);
                }
                return fileId;
            }
        }

        private String normalizeMimeType(String mimeType, String fileName) {
            String normalized = mimeType == null ? "" : mimeType.trim();
            if (!normalized.isEmpty()) {
                return normalized;
            }
            String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
            if (lowerName.endsWith(".pdf")) {
                return "application/pdf";
            }
            if (lowerName.endsWith(".doc")) {
                return "application/msword";
            }
            if (lowerName.endsWith(".docx")) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            if (lowerName.endsWith(".txt")) {
                return "text/plain";
            }
            if (lowerName.endsWith(".md")) {
                return "text/markdown";
            }
            return "application/octet-stream";
        }

        private String resolveApiExceptionMessage(ApiException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            if (message.contains("file has not finished parsing")) {
                return "附件仍在解析中，请稍后重试";
            }
            return message.isBlank() ? "AI 服务调用异常" : message;
        }

        private String readApiKey() {
            String apiKey = System.getenv(API_KEY_ENV);
            return apiKey == null ? "" : apiKey.trim();
        }

        private void sleepQuietly(long millis) throws IOException {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("等待附件解析时被中断", e);
            }
        }

        private String extractAssistantText(GenerationResult result) {
            if (result == null || result.getOutput() == null) {
                return null;
            }
            if (result.getOutput().getChoices() == null
                    || result.getOutput().getChoices().isEmpty()
                    || result.getOutput().getChoices().get(0) == null
                    || result.getOutput().getChoices().get(0).getMessage() == null) {
                return result.getOutput().getText();
            }
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        }
    }

    static class FileParsingPendingException extends IOException {
        FileParsingPendingException(String fileName, Throwable cause) {
            super("文件尚未完成解析: " + fileName, cause);
        }
    }
}
