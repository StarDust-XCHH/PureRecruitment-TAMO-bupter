package com.bupt.tarecruit.ta.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.common.util.ServletJsonResponseWriter;
import com.bupt.tarecruit.ta.service.TaAiAssistantService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
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

@WebServlet(name = "taAiAssistantServlet", value = "/api/ta/ai")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 10 * 1024 * 1024,
        maxRequestSize = 12 * 1024 * 1024
)
public class TaAiAssistantServlet extends HttpServlet {
    private final TaAiAssistantService aiAssistantService = new TaAiAssistantService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String action = trim(req.getParameter("action"));
        if ("status".equalsIgnoreCase(action)) {
            ServletJsonResponseWriter.write(resp, 200, ApiResponse.success("AI 服务状态读取成功", aiAssistantService.getServiceStatus()));
            return;
        }

        String taId = trim(req.getParameter("taId"));
        if (taId.isEmpty()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("缺少 TA 标识"));
            return;
        }

        Map<String, Object> data = aiAssistantService.loadConversation(taId);
        ServletJsonResponseWriter.write(resp, 200, ApiResponse.success("AI 会话读取成功", data));
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
            if ("chat".equalsIgnoreCase(action)) {
                handleChat(req, resp);
                return;
            }
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("不支持的 AI 操作"));
        } catch (Exception ex) {
            ex.printStackTrace();
            ServletJsonResponseWriter.write(resp, 500, ApiResponse.failure("AI 助理操作失败: " + ex.getMessage()));
        }
    }

    private void handleUploadPending(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String taId = trim(req.getParameter("taId"));
        if (taId.isEmpty()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("缺少 TA 标识"));
            return;
        }

        Part filePart;
        try {
            filePart = req.getPart("file");
        } catch (IllegalStateException ex) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("AI 附件大小不能超过 10MB"));
            return;
        }
        if (filePart == null || filePart.getSize() <= 0) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("请先选择需要载入 AI 对话的文件"));
            return;
        }

        String originalFileName = extractFileName(filePart);
        Map<String, Object> result = aiAssistantService.uploadPendingAttachment(
                taId,
                originalFileName,
                trim(filePart.getContentType()),
                filePart.getSize(),
                filePart.getInputStream(),
                trim(req.getParameter("sourceType")),
                trim(req.getParameter("sourcePath")),
                trim(req.getParameter("courseCode")),
                trim(req.getParameter("applicationId"))
        );
        ServletJsonResponseWriter.write(resp, 200, ApiResponse.success("AI 待发送附件已载入", result));
    }

    private void handleChat(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = readJsonBody(req);
        String taId = getAsString(body, "taId");
        if (taId.isBlank()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("缺少 TA 标识"));
            return;
        }

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

        Map<String, Object> result = aiAssistantService.sendMessage(taId, sessionId, message, attachmentIds, scene, title, context);
        ServletJsonResponseWriter.write(resp, 200, ApiResponse.success("AI 对话完成", result));
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
}
