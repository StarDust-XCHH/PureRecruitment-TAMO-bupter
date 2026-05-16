package com.bupt.tarecruit.common.ai;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.reactivex.Flowable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static com.bupt.tarecruit.common.ai.AiProvider.AiAttachmentRef;
import static com.bupt.tarecruit.common.ai.AiProvider.AiChatRequest;
import static com.bupt.tarecruit.common.ai.AiProvider.AiChatResult;
import static com.bupt.tarecruit.common.ai.AiProvider.AiProviderStatus;

public class QwenLongAiProvider implements AiProvider {
    private static final String API_KEY_ENV = "TONGYI_API_KEY";
    private static final String MODEL = "qwen-long";
    private static final String FILE_UPLOAD_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/files";
    private static final MediaType MEDIA_TYPE_PDF = MediaType.parse("application/pdf");
    private static final Duration CALL_TIMEOUT = Duration.ofMinutes(3);
    private static final int MAX_FILE_PARSE_WAIT_RETRIES = 5;
    private static final long FILE_PARSE_WAIT_MILLIS = 1500L;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(CALL_TIMEOUT)
            .build();

    private final String defaultSystemPrompt;

    public QwenLongAiProvider(String defaultSystemPrompt) {
        this.defaultSystemPrompt = defaultSystemPrompt == null || defaultSystemPrompt.isBlank()
                ? "你是 PureRecruitment 平台中的 AI 助理。请使用简体中文回答。"
                : defaultSystemPrompt.trim();
    }

    @Override
    public AiProviderStatus getStatus() {
        String apiKey = readApiKey();
        if (apiKey.isEmpty()) {
            return new AiProviderStatus(MODEL, false, false, "服务端未读取到环境变量 TONGYI_API_KEY，AI 助理已禁用");
        }
        return new AiProviderStatus(MODEL, true, true, "已连接默认服务：qwen-long");
    }

    @Override
    public AiChatResult chat(AiChatRequest request, Consumer<String> chunkConsumer) throws IOException {
        String apiKey = readApiKey();
        if (apiKey.isEmpty()) {
            throw new IOException("服务端未读取到环境变量 TONGYI_API_KEY，无法调用 AI 服务");
        }

        List<Message> providerMessages = new ArrayList<>();
        providerMessages.add(Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(resolveSystemPrompt(request))
                .build());
        for (AiAttachmentRef attachment : request.attachments()) {
            providerMessages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("fileid://" + uploadAndEnsureParsed(apiKey, attachment))
                    .build());
        }
        providerMessages.addAll(request.messages());

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
                String incrementalText = sanitizeAssistantText(extractAssistantText(item));
                if (incrementalText != null && !incrementalText.isBlank()) {
                    fullResponse.append(incrementalText);
                    if (chunkConsumer != null) {
                        chunkConsumer.accept(incrementalText);
                    }
                }
            });

            String text = sanitizeAssistantText(fullResponse.toString()).trim();
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
        RequestBody fileBody = RequestBody.create(
                MEDIA_TYPE_PDF.equals(MediaType.parse(resolvedMimeType)) ? MEDIA_TYPE_PDF : MediaType.parse(resolvedMimeType),
                bytes);
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

    private String resolveSystemPrompt(AiChatRequest request) {
        if (request == null || request.context() == null) {
            return defaultSystemPrompt;
        }
        Object prompt = request.context().get("_systemPrompt");
        String normalized = prompt == null ? "" : String.valueOf(prompt).trim();
        return normalized.isEmpty() ? defaultSystemPrompt : normalized;
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

    private String sanitizeAssistantText(String text) {
        String normalized = text == null ? "" : text;
        normalized = normalized.replace("\r\n", "\n");
        normalized = normalized.replaceAll("^```[a-zA-Z0-9_-]*\\s*", "");
        normalized = normalized.replaceAll("\\s*```$", "");
        return normalized;
    }
}
