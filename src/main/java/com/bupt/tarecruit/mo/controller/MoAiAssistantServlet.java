package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.common.util.ServletJsonResponseWriter;
import com.bupt.tarecruit.mo.service.MoAiAssistantService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

@WebServlet(name = "moAiAssistantServlet", value = "/api/mo/ai")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 10 * 1024 * 1024,
        maxRequestSize = 12 * 1024 * 1024
)
public class MoAiAssistantServlet extends HttpServlet {
    private static final Gson SSE_GSON = new GsonBuilder().disableHtmlEscaping().create();
    private final MoAiAssistantService aiAssistantService = new MoAiAssistantService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String action = trim(req.getParameter("action"));
        if ("status".equalsIgnoreCase(action)) {
            ServletJsonResponseWriter.write(resp, 200, ApiResponse.success("AI service status loaded", aiAssistantService.getServiceStatus()));
            return;
        }

        String moId = trim(req.getParameter("moId"));
        if (moId.isEmpty()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("Missing MO identifier"));
            return;
        }

        Map<String, Object> data = aiAssistantService.loadConversation(moId);
        ServletJsonResponseWriter.write(resp, 200, ApiResponse.success("AI conversation loaded", data));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String action = trim(req.getParameter("action"));
        if (action.isEmpty()) {
            action = resolveActionFromJson(req);
        }

        try {
            if ("upload-pending".equalsIgnoreCase(action)) {
                handleUploadPending(req, resp);
                return;
            }
            if ("remove-pending".equalsIgnoreCase(action)) {
                handleRemovePendingAttachment(req, resp);
                return;
            }
            if ("chat".equalsIgnoreCase(action)) {
                if ("true".equalsIgnoreCase(trim(req.getParameter("stream")))) {
                    handleChatStream(req, resp);
                } else {
                    handleChat(req, resp);
                }
                return;
            }
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("Unsupported AI action"));
        } catch (Exception ex) {
            ex.printStackTrace();
            ServletJsonResponseWriter.write(resp, 500, ApiResponse.failure("AI assistant request failed: " + ex.getMessage()));
        }
    }

    private void handleUploadPending(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String moId = trim(req.getParameter("moId"));
        if (moId.isEmpty()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("Missing MO identifier"));
            return;
        }

        Part filePart;
        try {
            filePart = req.getPart("file");
        } catch (IllegalStateException ex) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("AI attachment size cannot exceed 10MB"));
            return;
        }
        if (filePart == null || filePart.getSize() <= 0) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("Choose a file to add to the AI chat"));
            return;
        }

        String originalFileName = extractFileName(filePart);
        Map<String, Object> result = aiAssistantService.uploadPendingAttachment(
                moId,
                originalFileName,
                trim(filePart.getContentType()),
                filePart.getSize(),
                filePart.getInputStream(),
                trim(req.getParameter("sourceType")),
                trim(req.getParameter("sourcePath")),
                trim(req.getParameter("courseCode")),
                trim(req.getParameter("applicationId"))
        );
        ServletJsonResponseWriter.write(resp, 200, ApiResponse.success("Pending AI attachment loaded", result));
    }

    private void handleRemovePendingAttachment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String moId = trim(req.getParameter("moId"));
        String attachmentId = trim(req.getParameter("attachmentId"));
        if (moId.isEmpty()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("Missing MO identifier"));
            return;
        }
        if (attachmentId.isEmpty()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("Missing attachment identifier to remove"));
            return;
        }

        Map<String, Object> result = aiAssistantService.removePendingAttachment(moId, attachmentId);
        ServletJsonResponseWriter.write(resp, 200, ApiResponse.success("Pending attachment removed", result));
    }

    private void handleChat(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ChatRequest chatRequest = parseChatRequest(req);
        if (chatRequest.moId().isBlank()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("Missing MO identifier"));
            return;
        }

        Map<String, Object> result = aiAssistantService.sendMessage(
                chatRequest.moId(),
                chatRequest.sessionId(),
                chatRequest.message(),
                chatRequest.attachmentIds(),
                chatRequest.scene(),
                chatRequest.title(),
                chatRequest.context()
        );
        ServletJsonResponseWriter.write(resp, 200, ApiResponse.success("AI chat completed", result));
    }

    private void handleChatStream(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ChatRequest chatRequest = parseChatRequest(req);
        if (chatRequest.moId().isBlank()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("Missing MO identifier"));
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/event-stream;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");

        ServletOutputStream outputStream = resp.getOutputStream();
        try {
            Map<String, Object> result = aiAssistantService.sendMessageStream(
                    chatRequest.moId(),
                    chatRequest.sessionId(),
                    chatRequest.message(),
                    chatRequest.attachmentIds(),
                    chatRequest.scene(),
                    chatRequest.title(),
                    chatRequest.context(),
                    delta -> {
                        try {
                            writeSseEvent(outputStream, "delta", Map.of("delta", delta));
                        } catch (IOException ioException) {
                            throw new RuntimeException(ioException);
                        }
                    }
            );
            writeSseEvent(outputStream, "complete", Map.of("data", result));
        } catch (Exception ex) {
            ex.printStackTrace();
            writeSseEvent(outputStream, "error", Map.of("message", ex.getMessage() == null ? "AI assistant request failed" : ex.getMessage()));
        }
    }

    private ChatRequest parseChatRequest(HttpServletRequest req) throws IOException {
        JsonObject body = readJsonBody(req);
        String moId = getAsString(body, "moId");
        String sessionId = getAsString(body, "sessionId");
        String message = getAsString(body, "message");
        String scene = getAsString(body, "scene");
        String title = getAsString(body, "title");

        List<String> attachmentIds = new ArrayList<>();
        if (body.has("attachmentIds") && body.get("attachmentIds").isJsonArray()) {
            body.getAsJsonArray("attachmentIds").forEach(item -> attachmentIds.add(item.getAsString()));
        }

        Map<String, Object> context = new LinkedHashMap<>();
        if (body.has("context") && body.get("context").isJsonObject()) {
            JsonObject contextObject = body.getAsJsonObject("context");
            contextObject.entrySet().forEach(entry -> {
                if (entry.getValue() == null || entry.getValue().isJsonNull()) {
                    context.put(entry.getKey(), null);
                } else if (entry.getValue().isJsonPrimitive()) {
                    if (entry.getValue().getAsJsonPrimitive().isBoolean()) {
                        context.put(entry.getKey(), entry.getValue().getAsBoolean());
                    } else if (entry.getValue().getAsJsonPrimitive().isNumber()) {
                        context.put(entry.getKey(), entry.getValue().getAsNumber());
                    } else {
                        context.put(entry.getKey(), entry.getValue().getAsString());
                    }
                } else {
                    context.put(entry.getKey(), entry.getValue().toString());
                }
            });
        }
        return new ChatRequest(moId, sessionId, message, scene, title, attachmentIds, context);
    }

    private JsonObject readJsonBody(HttpServletRequest req) throws IOException {
        try (Reader reader = new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private String resolveActionFromJson(HttpServletRequest req) {
        String contentType = trim(req.getContentType()).toLowerCase();
        if (!contentType.contains("application/json")) {
            return "";
        }
        return "chat";
    }

    private String extractFileName(Part part) {
        String submitted = part.getSubmittedFileName();
        if (submitted == null) {
            return "attachment";
        }
        String normalized = submitted.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }

    private void writeSseEvent(ServletOutputStream outputStream, String type, Map<String, Object> payload) throws IOException {
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("type", type);
        eventPayload.putAll(payload);
        String json = SSE_GSON.toJson(eventPayload);
        outputStream.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private record ChatRequest(String moId,
                               String sessionId,
                               String message,
                               String scene,
                               String title,
                               List<String> attachmentIds,
                               Map<String, Object> context) {
    }
}
