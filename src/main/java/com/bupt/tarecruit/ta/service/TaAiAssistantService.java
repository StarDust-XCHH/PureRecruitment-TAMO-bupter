package com.bupt.tarecruit.ta.service;

import com.bupt.tarecruit.ta.dao.TaAiConversationDao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaAiAssistantService {
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
    private static final DateTimeFormatter PDF_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final TaAiConversationDao conversationDao = new TaAiConversationDao();

    public Map<String, Object> loadConversation(String taId) throws IOException {
        return conversationDao.getOrCreateConversation(taId).data();
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
        return result;
    }

    public Map<String, Object> sendMessage(String taId,
                                           String sessionId,
                                           String message,
                                           List<String> attachmentIds,
                                           String scene,
                                           String title,
                                           Map<String, Object> context) throws IOException {
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

        conversationDao.appendUserMessageAndPendingAttachments(taId, resolvedSessionId, message, scene, effectiveAttachmentIds, context);

        String assistantText = buildAssistantReply(message, linkedAttachments, context);
        TaAiConversationDao.GeneratedFile generatedFile = null;
        if (!linkedAttachments.isEmpty()) {
            TaAiConversationDao.PendingAttachment primaryAttachment = linkedAttachments.get(0);
            byte[] pdfBytes = buildPseudoPdf(primaryAttachment, message, context, assistantText);
            generatedFile = conversationDao.registerGeneratedPdf(
                    taId,
                    resolvedSessionId,
                    deriveGeneratedPdfName(primaryAttachment.originalFileName()),
                    pdfBytes
            );
        }

        Map<String, Object> snapshot = conversationDao.appendAssistantResult(
                taId,
                resolvedSessionId,
                assistantText,
                generatedFile,
                "mock-ai-provider"
        ).data();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", resolvedSessionId);
        result.put("downloadUrl", generatedFile == null ? "" : generatedFile.downloadUrl());
        result.put("generatedAttachmentId", generatedFile == null ? "" : generatedFile.attachmentId());
        result.put("generatedFileName", generatedFile == null ? "" : generatedFile.fileName());
        result.put("conversation", snapshot);
        result.put("pendingAttachments", conversationDao.listPendingAttachments(taId));
        return result;
    }

    public TaAiConversationDao.DownloadFile getGeneratedFile(String taId, String sessionId, String attachmentId) throws IOException {
        return conversationDao.findGeneratedFile(taId, sessionId, attachmentId);
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

    private String buildAssistantReply(String message,
                                       List<TaAiConversationDao.PendingAttachment> attachments,
                                       Map<String, Object> context) {
        StringBuilder builder = new StringBuilder();
        builder.append("我已经根据你提供的材料生成了一版重新导出的 PDF 简历。");
        if (!attachments.isEmpty()) {
            builder.append(" 本次处理的主附件是《")
                    .append(attachments.get(0).originalFileName())
                    .append("》。");
        }
        String courseCode = context == null ? "" : String.valueOf(context.getOrDefault("courseCode", "")).trim();
        if (!courseCode.isEmpty()) {
            builder.append(" 我参考了课程编码 ")
                    .append(courseCode)
                    .append(" 的申请语境，强化了与岗位匹配相关的表述。");
        }
        if (message != null && !message.trim().isEmpty()) {
            builder.append(" 你刚才的要求是：“")
                    .append(message.trim())
                    .append("”。");
        }
        builder.append(" 结果文件已准备好，可直接下载查看。当前版本为模拟 Provider 生成，后续可在服务层替换为真实大模型调用。");
        return builder.toString();
    }

    private byte[] buildPseudoPdf(TaAiConversationDao.PendingAttachment attachment,
                                  String message,
                                  Map<String, Object> context,
                                  String assistantText) {
        StringBuilder builder = new StringBuilder();
        builder.append("%PDF-1.4\n");
        builder.append("% Mock generated pdf for TA AI assistant\n");
        builder.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        builder.append("2 0 obj << /Type /Pages /Count 1 /Kids [3 0 R] >> endobj\n");
        builder.append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R >> endobj\n");
        String content = buildPdfTextBody(attachment, message, context, assistantText);
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        builder.append("4 0 obj << /Length ").append(contentBytes.length).append(" >> stream\n");
        builder.append(content);
        builder.append("\nendstream endobj\n");
        builder.append("xref\n0 5\n0000000000 65535 f \n");
        builder.append("trailer << /Root 1 0 R /Size 5 >>\nstartxref\n0\n%%EOF");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String buildPdfTextBody(TaAiConversationDao.PendingAttachment attachment,
                                    String message,
                                    Map<String, Object> context,
                                    String assistantText) {
        String courseCode = context == null ? "" : String.valueOf(context.getOrDefault("courseCode", "")).trim();
        String applicationId = context == null ? "" : String.valueOf(context.getOrDefault("applicationId", "")).trim();
        String sourcePath = attachment == null ? "" : attachment.sourcePath();

        StringBuilder body = new StringBuilder();
        body.append("BT\n/F1 12 Tf\n72 760 Td\n");
        body.append("(TA AI Optimized Resume Export) Tj\n0 -18 Td\n");
        body.append("(Generated At: ").append(escapePdfText(PDF_TIME_FORMATTER.format(Instant.now()))).append(") Tj\n0 -18 Td\n");
        body.append("(Source File: ").append(escapePdfText(attachment == null ? "unknown" : attachment.originalFileName())).append(") Tj\n0 -18 Td\n");
        body.append("(Course Code: ").append(escapePdfText(courseCode.isEmpty() ? "N/A" : courseCode)).append(") Tj\n0 -18 Td\n");
        body.append("(Application Id: ").append(escapePdfText(applicationId.isEmpty() ? "N/A" : applicationId)).append(") Tj\n0 -18 Td\n");
        body.append("(Source Path: ").append(escapePdfText(sourcePath.isEmpty() ? "N/A" : sourcePath)).append(") Tj\n0 -24 Td\n");
        body.append("(Optimization Request) Tj\n0 -18 Td\n");
        body.append("(").append(escapePdfText(message == null || message.isBlank() ? "请帮我优化当前课程申请简历。" : message.trim())).append(") Tj\n0 -24 Td\n");
        body.append("(Assistant Summary) Tj\n0 -18 Td\n");
        body.append("(").append(escapePdfText(assistantText)).append(") Tj\nET");
        return body.toString();
    }

    private String deriveGeneratedPdfName(String originalFileName) {
        String normalized = originalFileName == null ? "resume" : originalFileName.trim();
        if (normalized.isEmpty()) {
            normalized = "resume";
        }
        int dotIndex = normalized.lastIndexOf('.');
        String baseName = dotIndex > 0 ? normalized.substring(0, dotIndex) : normalized;
        baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return baseName + "-ai-optimized.pdf";
    }

    private String escapePdfText(String value) {
        String normalized = value == null ? "" : value;
        String asciiSafe = normalized.replaceAll("[^\\x20-\\x7E]", " ");
        return asciiSafe
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
