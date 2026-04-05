package com.bupt.tarecruit.ta.dao;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

public class TaAiConversationDao {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public synchronized ConversationSnapshot getOrCreateConversation(String taId) throws IOException {
        String normalizedTaId = requireTaId(taId);
        JsonObject root = loadConversationRoot(normalizedTaId);
        return toSnapshot(root);
    }

    public synchronized ConversationSnapshot appendUserMessageAndPendingAttachments(String taId,
                                                                                     String sessionId,
                                                                                     String message,
                                                                                     String scene,
                                                                                     List<String> attachmentIds,
                                                                                     Map<String, Object> context) throws IOException {
        String normalizedTaId = requireTaId(taId);
        JsonObject root = loadConversationRoot(normalizedTaId);
        JsonObject session = requireSession(root, normalizedTaId, sessionId, scene, context, true);
        String now = nowIso();

        JsonArray messages = ensureArray(session, "messages");
        JsonObject userMessage = createMessage("user", "text", trim(message), now);
        messages.add(userMessage);

        JsonArray attachments = ensureArray(session, "attachments");
        if (attachmentIds != null) {
            for (String attachmentId : attachmentIds) {
                String normalizedAttachmentId = trim(attachmentId);
                if (normalizedAttachmentId.isEmpty()) {
                    continue;
                }
                JsonObject attachment = findAttachment(attachments, normalizedAttachmentId);
                if (attachment != null) {
                    attachment.addProperty("status", "linked");
                    attachment.addProperty("linkedAt", now);
                }
            }
        }

        touchSession(session, now);
        root.addProperty("updatedAt", now);
        saveConversationRoot(normalizedTaId, root);
        return toSnapshot(root);
    }

    public synchronized ConversationSnapshot appendAssistantResult(String taId,
                                                                   String sessionId,
                                                                   String assistantText,
                                                                   GeneratedFile generatedFile,
                                                                   String providerName) throws IOException {
        String normalizedTaId = requireTaId(taId);
        JsonObject root = loadConversationRoot(normalizedTaId);
        JsonObject session = requireSession(root, normalizedTaId, sessionId, "general_chat", Map.of(), false);
        String now = nowIso();

        JsonArray messages = ensureArray(session, "messages");
        JsonObject assistantMessage = createMessage("assistant", "text", trim(assistantText), now);
        if (generatedFile != null) {
            JsonObject artifact = new JsonObject();
            artifact.addProperty("attachmentId", generatedFile.attachmentId());
            artifact.addProperty("kind", "resume_optimized_pdf");
            artifact.addProperty("fileName", generatedFile.fileName());
            artifact.addProperty("downloadUrl", generatedFile.downloadUrl());
            artifact.addProperty("mimeType", generatedFile.mimeType());
            artifact.addProperty("size", generatedFile.size());
            assistantMessage.add("artifact", artifact);
        }
        messages.add(assistantMessage);

        if (generatedFile != null) {
            JsonArray attachments = ensureArray(session, "attachments");
            JsonObject generatedAttachment = new JsonObject();
            generatedAttachment.addProperty("attachmentId", generatedFile.attachmentId());
            generatedAttachment.addProperty("kind", "resume_optimized_pdf");
            generatedAttachment.addProperty("originalFileName", generatedFile.fileName());
            generatedAttachment.addProperty("storedPath", generatedFile.storedPath());
            generatedAttachment.addProperty("downloadUrl", generatedFile.downloadUrl());
            generatedAttachment.addProperty("mimeType", generatedFile.mimeType());
            generatedAttachment.addProperty("size", generatedFile.size());
            generatedAttachment.addProperty("status", "generated");
            generatedAttachment.addProperty("createdAt", now);
            generatedAttachment.addProperty("sha256", generatedFile.sha256());
            attachments.add(generatedAttachment);
        }

        if (providerName != null && !providerName.isBlank()) {
            session.addProperty("provider", providerName.trim());
        }

        touchSession(session, now);
        root.addProperty("updatedAt", now);
        saveConversationRoot(normalizedTaId, root);
        return toSnapshot(root);
    }

    public synchronized PendingAttachment createPendingAttachment(String taId,
                                                                  String originalFileName,
                                                                  String contentType,
                                                                  long size,
                                                                  byte[] bytes,
                                                                  String sourceType,
                                                                  String sourcePath,
                                                                  String courseCode,
                                                                  String applicationId) throws IOException {
        String normalizedTaId = requireTaId(taId);
        if (bytes == null || bytes.length == 0) {
            throw new IOException("附件内容为空");
        }

        String safeFileName = sanitizeFileName(originalFileName);
        String attachmentId = "att_" + UUID.randomUUID().toString().replace("-", "");
        String storedFileName = attachmentId + "_" + safeFileName;
        String now = nowIso();

        Path tempRoot = DataMountPaths.taAiAttachmentTempRoot(normalizedTaId);
        Files.createDirectories(tempRoot);
        Path target = tempRoot.resolve(storedFileName).toAbsolutePath().normalize();
        Files.write(target, bytes);

        JsonObject root = loadConversationRoot(normalizedTaId);
        JsonArray pendingAttachments = ensureArray(root, "pendingAttachments");

        JsonObject attachment = new JsonObject();
        attachment.addProperty("attachmentId", attachmentId);
        attachment.addProperty("status", "pending");
        attachment.addProperty("kind", "resume_source");
        attachment.addProperty("originalFileName", safeFileName);
        attachment.addProperty("storedFileName", storedFileName);
        attachment.addProperty("storedPath", relativizeToDataRoot(target));
        attachment.addProperty("mimeType", trim(contentType));
        attachment.addProperty("size", size > 0 ? size : bytes.length);
        attachment.addProperty("createdAt", now);
        attachment.addProperty("sha256", sha256Hex(bytes));
        attachment.addProperty("sourceType", trim(sourceType));
        attachment.addProperty("sourcePath", trim(sourcePath));
        attachment.addProperty("courseCode", trim(courseCode));
        attachment.addProperty("applicationId", trim(applicationId));
        pendingAttachments.add(attachment);

        root.addProperty("updatedAt", now);
        saveConversationRoot(normalizedTaId, root);

        return new PendingAttachment(
                attachmentId,
                safeFileName,
                trim(contentType),
                size > 0 ? size : bytes.length,
                relativizeToDataRoot(target),
                trim(sourceType),
                trim(sourcePath),
                trim(courseCode),
                trim(applicationId)
        );
    }

    public synchronized List<Map<String, Object>> listPendingAttachments(String taId) throws IOException {
        String normalizedTaId = requireTaId(taId);
        JsonObject root = loadConversationRoot(normalizedTaId);
        JsonArray pendingAttachments = ensureArray(root, "pendingAttachments");
        List<Map<String, Object>> items = new ArrayList<>();
        for (JsonElement element : pendingAttachments) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject attachment = element.getAsJsonObject();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("attachmentId", getAsString(attachment, "attachmentId"));
            item.put("status", getAsString(attachment, "status"));
            item.put("kind", getAsString(attachment, "kind"));
            item.put("originalFileName", getAsString(attachment, "originalFileName"));
            item.put("storedPath", getAsString(attachment, "storedPath"));
            item.put("mimeType", getAsString(attachment, "mimeType"));
            item.put("size", attachment.has("size") ? attachment.get("size").getAsLong() : 0L);
            item.put("createdAt", getAsString(attachment, "createdAt"));
            item.put("sourceType", getAsString(attachment, "sourceType"));
            item.put("sourcePath", getAsString(attachment, "sourcePath"));
            item.put("courseCode", getAsString(attachment, "courseCode"));
            item.put("applicationId", getAsString(attachment, "applicationId"));
            items.add(item);
        }
        items.sort(Comparator.comparing(item -> String.valueOf(item.get("createdAt")), Comparator.reverseOrder()));
        return items;
    }

    public synchronized PendingAttachment findPendingAttachment(String taId, String attachmentId) throws IOException {
        String normalizedTaId = requireTaId(taId);
        String normalizedAttachmentId = trim(attachmentId);
        if (normalizedAttachmentId.isEmpty()) {
            return null;
        }

        JsonObject root = loadConversationRoot(normalizedTaId);
        JsonArray pendingAttachments = ensureArray(root, "pendingAttachments");
        for (JsonElement element : pendingAttachments) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject attachment = element.getAsJsonObject();
            if (normalizedAttachmentId.equals(getAsString(attachment, "attachmentId"))) {
                return new PendingAttachment(
                        getAsString(attachment, "attachmentId"),
                        getAsString(attachment, "originalFileName"),
                        getAsString(attachment, "mimeType"),
                        attachment.has("size") ? attachment.get("size").getAsLong() : 0L,
                        getAsString(attachment, "storedPath"),
                        getAsString(attachment, "sourceType"),
                        getAsString(attachment, "sourcePath"),
                        getAsString(attachment, "courseCode"),
                        getAsString(attachment, "applicationId")
                );
            }
        }
        return null;
    }

    public synchronized PendingAttachment consumePendingAttachment(String taId, String attachmentId, String sessionId) throws IOException {
        String normalizedTaId = requireTaId(taId);
        String normalizedAttachmentId = trim(attachmentId);
        if (normalizedAttachmentId.isEmpty()) {
            return null;
        }

        JsonObject root = loadConversationRoot(normalizedTaId);
        JsonArray pendingAttachments = ensureArray(root, "pendingAttachments");
        JsonObject found = null;
        int foundIndex = -1;
        for (int i = 0; i < pendingAttachments.size(); i++) {
            JsonElement element = pendingAttachments.get(i);
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject attachment = element.getAsJsonObject();
            if (normalizedAttachmentId.equals(getAsString(attachment, "attachmentId"))) {
                found = attachment;
                foundIndex = i;
                break;
            }
        }
        if (found == null) {
            return null;
        }

        Path currentPath = resolveStoredPath(getAsString(found, "storedPath"));
        String storedFileName = getAsString(found, "storedFileName");
        Path uploadedRoot = DataMountPaths.taAiAttachmentUploadedRoot(normalizedTaId);
        Files.createDirectories(uploadedRoot);
        Path uploadedTarget = uploadedRoot.resolve(storedFileName).toAbsolutePath().normalize();
        if (Files.exists(currentPath) && !currentPath.equals(uploadedTarget)) {
            Files.move(currentPath, uploadedTarget, StandardCopyOption.REPLACE_EXISTING);
        }

        found.addProperty("storedPath", relativizeToDataRoot(uploadedTarget));
        found.addProperty("status", "linked");
        found.addProperty("linkedSessionId", trim(sessionId));
        found.addProperty("linkedAt", nowIso());

        pendingAttachments.remove(foundIndex);
        root.addProperty("updatedAt", nowIso());
        saveConversationRoot(normalizedTaId, root);

        return new PendingAttachment(
                getAsString(found, "attachmentId"),
                getAsString(found, "originalFileName"),
                getAsString(found, "mimeType"),
                found.has("size") ? found.get("size").getAsLong() : 0L,
                getAsString(found, "storedPath"),
                getAsString(found, "sourceType"),
                getAsString(found, "sourcePath"),
                getAsString(found, "courseCode"),
                getAsString(found, "applicationId")
        );
    }

    public synchronized String createSession(String taId, String scene, String title, Map<String, Object> context) throws IOException {
        String normalizedTaId = requireTaId(taId);
        JsonObject root = loadConversationRoot(normalizedTaId);
        String sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "");
        String now = nowIso();

        JsonArray sessions = ensureArray(root, "sessions");
        JsonObject session = new JsonObject();
        session.addProperty("sessionId", sessionId);
        session.addProperty("scene", trim(scene).isEmpty() ? "general_chat" : trim(scene));
        session.addProperty("title", trim(title).isEmpty() ? "AI 助理对话" : trim(title));
        session.addProperty("status", "active");
        session.addProperty("createdAt", now);
        session.addProperty("updatedAt", now);
        session.add("messages", new JsonArray());
        session.add("attachments", new JsonArray());
        session.add("context", toJsonObject(context));
        sessions.add(session);

        root.addProperty("updatedAt", now);
        saveConversationRoot(normalizedTaId, root);
        return sessionId;
    }

    public synchronized GeneratedFile registerGeneratedPdf(String taId,
                                                           String sessionId,
                                                           String originalFileName,
                                                           byte[] bytes) throws IOException {
        String normalizedTaId = requireTaId(taId);
        if (bytes == null || bytes.length == 0) {
            throw new IOException("生成的 PDF 内容为空");
        }
        String attachmentId = "gen_" + UUID.randomUUID().toString().replace("-", "");
        String fileName = attachmentId + "_" + sanitizePdfFileName(originalFileName);

        Path generatedRoot = DataMountPaths.taAiAttachmentGeneratedRoot(normalizedTaId);
        Files.createDirectories(generatedRoot);
        Path generatedPath = generatedRoot.resolve(fileName).toAbsolutePath().normalize();
        Files.write(generatedPath, bytes);

        return new GeneratedFile(
                attachmentId,
                fileName,
                "application/pdf",
                bytes.length,
                relativizeToDataRoot(generatedPath),
                "../../api/ta/ai/files/download?taId=" + encodeQuery(normalizedTaId) + "&sessionId=" + encodeQuery(sessionId) + "&attachmentId=" + encodeQuery(attachmentId),
                sha256Hex(bytes)
        );
    }

    public synchronized DownloadFile findGeneratedFile(String taId, String sessionId, String attachmentId) throws IOException {
        String normalizedTaId = requireTaId(taId);
        JsonObject root = loadConversationRoot(normalizedTaId);
        JsonObject session = requireSession(root, normalizedTaId, sessionId, "general_chat", Map.of(), false);
        JsonArray attachments = ensureArray(session, "attachments");
        JsonObject attachment = findAttachment(attachments, trim(attachmentId));
        if (attachment == null) {
            return null;
        }
        Path path = resolveStoredPath(getAsString(attachment, "storedPath"));
        if (!Files.exists(path) || Files.isDirectory(path)) {
            return null;
        }
        return new DownloadFile(
                getAsString(attachment, "originalFileName"),
                getAsString(attachment, "mimeType"),
                path,
                attachment.has("size") ? attachment.get("size").getAsLong() : Files.size(path)
        );
    }

    private ConversationSnapshot toSnapshot(JsonObject root) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("schema", getAsString(root, "schema"));
        data.put("version", root.has("version") ? root.get("version").getAsInt() : 1);
        data.put("taId", getAsString(root, "taId"));
        data.put("updatedAt", getAsString(root, "updatedAt"));
        data.put("sessions", extractSessions(root));
        data.put("pendingAttachments", extractPendingAttachments(root));
        return new ConversationSnapshot(data);
    }

    private List<Map<String, Object>> extractSessions(JsonObject root) {
        List<Map<String, Object>> sessions = new ArrayList<>();
        JsonArray sessionArray = ensureArray(root, "sessions");
        for (JsonElement element : sessionArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject session = element.getAsJsonObject();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionId", getAsString(session, "sessionId"));
            item.put("scene", getAsString(session, "scene"));
            item.put("title", getAsString(session, "title"));
            item.put("status", getAsString(session, "status"));
            item.put("createdAt", getAsString(session, "createdAt"));
            item.put("updatedAt", getAsString(session, "updatedAt"));
            item.put("provider", getAsString(session, "provider"));
            item.put("context", extractFlatObject(session.getAsJsonObject("context")));
            item.put("messages", extractMessages(session));
            item.put("attachments", extractSessionAttachments(session));
            sessions.add(item);
        }
        sessions.sort(Comparator.comparing(item -> String.valueOf(item.get("updatedAt")), Comparator.reverseOrder()));
        return sessions;
    }

    private List<Map<String, Object>> extractMessages(JsonObject session) {
        List<Map<String, Object>> messages = new ArrayList<>();
        JsonArray messageArray = ensureArray(session, "messages");
        for (JsonElement element : messageArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject message = element.getAsJsonObject();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("messageId", getAsString(message, "messageId"));
            item.put("role", getAsString(message, "role"));
            item.put("type", getAsString(message, "type"));
            item.put("content", getAsString(message, "content"));
            item.put("createdAt", getAsString(message, "createdAt"));
            if (message.has("artifact") && message.get("artifact").isJsonObject()) {
                item.put("artifact", extractFlatObject(message.getAsJsonObject("artifact")));
            }
            messages.add(item);
        }
        return messages;
    }

    private List<Map<String, Object>> extractSessionAttachments(JsonObject session) {
        List<Map<String, Object>> attachments = new ArrayList<>();
        JsonArray array = ensureArray(session, "attachments");
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            attachments.add(extractFlatObject(element.getAsJsonObject()));
        }
        return attachments;
    }

    private List<Map<String, Object>> extractPendingAttachments(JsonObject root) {
        List<Map<String, Object>> attachments = new ArrayList<>();
        JsonArray array = ensureArray(root, "pendingAttachments");
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            attachments.add(extractFlatObject(element.getAsJsonObject()));
        }
        return attachments;
    }

    private Map<String, Object> extractFlatObject(JsonObject object) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (object == null) {
            return map;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull()) {
                map.put(entry.getKey(), null);
            } else if (value.isJsonPrimitive()) {
                if (value.getAsJsonPrimitive().isBoolean()) {
                    map.put(entry.getKey(), value.getAsBoolean());
                } else if (value.getAsJsonPrimitive().isNumber()) {
                    map.put(entry.getKey(), value.getAsNumber());
                } else {
                    map.put(entry.getKey(), value.getAsString());
                }
            } else {
                map.put(entry.getKey(), value.toString());
            }
        }
        return map;
    }

    private JsonObject createMessage(String role, String type, String content, String now) {
        JsonObject message = new JsonObject();
        message.addProperty("messageId", "msg_" + UUID.randomUUID().toString().replace("-", ""));
        message.addProperty("role", trim(role));
        message.addProperty("type", trim(type).isEmpty() ? "text" : trim(type));
        message.addProperty("content", trim(content));
        message.addProperty("createdAt", now);
        return message;
    }

    private JsonObject loadConversationRoot(String taId) throws IOException {
        Path file = DataMountPaths.taAiConversationFile(taId);
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            JsonObject root = createEmptyConversationRoot(taId);
            saveConversationRoot(taId, root);
            return root;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                JsonObject root = createEmptyConversationRoot(taId);
                saveConversationRoot(taId, root);
                return root;
            }
            JsonObject root = parsed.getAsJsonObject();
            ensureArray(root, "sessions");
            ensureArray(root, "pendingAttachments");
            if (!root.has("schema")) {
                root.addProperty("schema", "ta-ai-conversations.v1");
            }
            if (!root.has("version")) {
                root.addProperty("version", 1);
            }
            if (!root.has("taId")) {
                root.addProperty("taId", taId);
            }
            if (!root.has("updatedAt")) {
                root.addProperty("updatedAt", nowIso());
            }
            return root;
        }
    }

    private void saveConversationRoot(String taId, JsonObject root) throws IOException {
        Path file = DataMountPaths.taAiConversationFile(taId);
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "ta-ai-", ".json");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private JsonObject createEmptyConversationRoot(String taId) {
        JsonObject root = new JsonObject();
        root.addProperty("schema", "ta-ai-conversations.v1");
        root.addProperty("version", 1);
        root.addProperty("taId", taId);
        root.addProperty("updatedAt", nowIso());
        root.add("sessions", new JsonArray());
        root.add("pendingAttachments", new JsonArray());
        return root;
    }

    private JsonObject requireSession(JsonObject root,
                                      String taId,
                                      String sessionId,
                                      String scene,
                                      Map<String, Object> context,
                                      boolean createIfMissing) throws IOException {
        String normalizedSessionId = trim(sessionId);
        JsonArray sessions = ensureArray(root, "sessions");
        for (JsonElement element : sessions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject session = element.getAsJsonObject();
            if (normalizedSessionId.equals(getAsString(session, "sessionId"))) {
                ensureArray(session, "messages");
                ensureArray(session, "attachments");
                if (!session.has("context")) {
                    session.add("context", toJsonObject(context));
                }
                return session;
            }
        }
        if (!createIfMissing) {
            throw new IOException("未找到会话: " + normalizedSessionId);
        }
        String newSessionId = normalizedSessionId.isEmpty() ? createSession(taId, scene, "AI 助理对话", context) : normalizedSessionId;
        JsonObject reloadedRoot = loadConversationRoot(taId);
        JsonArray reloadedSessions = ensureArray(reloadedRoot, "sessions");
        for (JsonElement element : reloadedSessions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject session = element.getAsJsonObject();
            if (newSessionId.equals(getAsString(session, "sessionId"))) {
                root.entrySet().clear();
                for (Map.Entry<String, JsonElement> entry : reloadedRoot.entrySet()) {
                    root.add(entry.getKey(), entry.getValue());
                }
                return session;
            }
        }
        throw new IOException("创建会话失败");
    }

    private JsonArray ensureArray(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            object.add(key, new JsonArray());
        }
        return object.getAsJsonArray(key);
    }

    private JsonObject findAttachment(JsonArray attachments, String attachmentId) {
        for (JsonElement element : attachments) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject attachment = element.getAsJsonObject();
            if (attachmentId.equals(getAsString(attachment, "attachmentId"))) {
                return attachment;
            }
        }
        return null;
    }

    private void touchSession(JsonObject session, String now) {
        session.addProperty("updatedAt", now);
        if (!session.has("status")) {
            session.addProperty("status", "active");
        }
    }

    private JsonObject toJsonObject(Map<String, Object> context) {
        JsonObject object = new JsonObject();
        if (context == null) {
            return object;
        }
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                object.add(entry.getKey(), null);
            } else if (value instanceof Number number) {
                object.addProperty(entry.getKey(), number);
            } else if (value instanceof Boolean bool) {
                object.addProperty(entry.getKey(), bool);
            } else {
                object.addProperty(entry.getKey(), String.valueOf(value));
            }
        }
        return object;
    }

    private String requireTaId(String taId) throws IOException {
        String normalizedTaId = trim(taId);
        if (normalizedTaId.isEmpty()) {
            throw new IOException("缺少 TA 标识");
        }
        return normalizedTaId;
    }

    private String nowIso() {
        return Instant.now().toString();
    }

    private String sanitizeFileName(String fileName) {
        String normalized = trim(fileName);
        if (normalized.isEmpty()) {
            normalized = "attachment.pdf";
        }
        return normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizePdfFileName(String fileName) {
        String normalized = sanitizeFileName(fileName);
        if (!normalized.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            normalized = normalized + ".pdf";
        }
        return normalized;
    }

    private String sha256Hex(byte[] bytes) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 不可用", e);
        }
    }

    private String relativizeToDataRoot(Path path) {
        Path root = DataMountPaths.root().toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (normalizedPath.startsWith(root)) {
            return root.relativize(normalizedPath).toString().replace('\\', '/');
        }
        return normalizedPath.toString().replace('\\', '/');
    }

    public synchronized Path resolveStoredPathForService(String storedPath) {
        Path path = Path.of(trim(storedPath));
        if (path.isAbsolute()) {
            return path.toAbsolutePath().normalize();
        }
        return DataMountPaths.root().resolve(path).toAbsolutePath().normalize();
    }

    private Path resolveStoredPath(String storedPath) {
        return resolveStoredPathForService(storedPath);
    }

    private String encodeQuery(String value) {
        return trim(value).replace(" ", "%20");
    }

    public record ConversationSnapshot(Map<String, Object> data) {
    }

    public record PendingAttachment(String attachmentId,
                                    String originalFileName,
                                    String mimeType,
                                    long size,
                                    String storedPath,
                                    String sourceType,
                                    String sourcePath,
                                    String courseCode,
                                    String applicationId) {
    }

    public record GeneratedFile(String attachmentId,
                                String fileName,
                                String mimeType,
                                long size,
                                String storedPath,
                                String downloadUrl,
                                String sha256) {
    }

    public record DownloadFile(String fileName,
                               String mimeType,
                               Path path,
                               long size) {
    }
}
