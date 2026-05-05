package com.bupt.tarecruit.ta.controller;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.ta.dao.TaAccountDao;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet(name = "taApplicationStatusServlet", value = "/api/ta/application-status")
public class TaApplicationStatusServlet extends HttpServlet {
    private final Gson gson = new Gson();
    private final TaAccountDao taAccountDao = new TaAccountDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String taId = trim(req.getParameter("taId"));
        try {
            TaAccountDao.ApplicationStatusResult result = taAccountDao.getApplicationStatus(taId);
            resp.setStatus(result.getStatus());
            try (PrintWriter writer = resp.getWriter()) {
                writer.write(buildResponse(result, taId));
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = resp.getWriter()) {
                writer.write(buildErrorResponse("读取申请状态失败: " + e.getMessage()));
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            writer.write(buildErrorResponse("请使用 GET 请求读取申请状态"));
        }
    }

    private String buildResponse(TaAccountDao.ApplicationStatusResult result, String taId) {
        JsonArray items = result.getItems() == null ? new JsonArray() : result.getItems();
        JsonArray enrichedItems = enrichItemsWithResumeView(items, taId);
        JsonObject summary = result.getSummary() == null ? new JsonObject() : result.getSummary();
        JsonArray notifications = result.getMessages() == null ? new JsonArray() : result.getMessages();

        JsonObject payload = new JsonObject();
        payload.addProperty("success", result.isSuccess());
        payload.addProperty("message", result.getMessage());
        payload.add("items", enrichedItems);
        payload.add("summary", summary);
        payload.add("notifications", notifications);
        payload.add("messages", notifications.deepCopy());
        payload.add("details", buildDetailsMap(enrichedItems));
        payload.add("appliedCourseCodes", buildAppliedCourseCodes(enrichedItems));
        payload.add("appliedJobIds", buildAppliedJobIds(enrichedItems));
        return gson.toJson(payload);
    }

    private String buildErrorResponse(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("success", false);
        payload.addProperty("message", message == null ? "" : message);
        payload.add("items", new JsonArray());
        payload.add("summary", new JsonObject());
        payload.add("notifications", new JsonArray());
        payload.add("messages", new JsonArray());
        payload.add("details", new JsonObject());
        payload.add("appliedCourseCodes", new JsonArray());
        payload.add("appliedJobIds", new JsonArray());
        return gson.toJson(payload);
    }

    private JsonArray enrichItemsWithResumeView(JsonArray items, String taId) {
        JsonArray enriched = new JsonArray();
        JsonObject applicationMap = loadApplicationMapById();
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isJsonObject()) {
                continue;
            }
            JsonObject item = items.get(i).getAsJsonObject().deepCopy();
            String applicationId = trim(item.has("applicationId") && !item.get("applicationId").isJsonNull()
                    ? item.get("applicationId").getAsString()
                    : "");
            JsonObject source = applicationMap.has(applicationId) && applicationMap.get(applicationId).isJsonObject()
                    ? applicationMap.getAsJsonObject(applicationId)
                    : null;
            JsonObject resume = source != null && source.has("resume") && source.get("resume").isJsonObject()
                    ? source.getAsJsonObject("resume")
                    : null;
            if (resume != null) {
                JsonObject resumeView = buildResumeView(resume, taId);
                if (resumeView != null) {
                    item.add("resumeView", resumeView);
                }
            }
            enriched.add(item);
        }
        return enriched;
    }

    private JsonObject loadApplicationMapById() {
        JsonObject result = new JsonObject();
        Path applicationsPath = DataMountPaths.taApplications();
        if (!Files.exists(applicationsPath)) {
            return result;
        }
        try {
            String json = Files.readString(applicationsPath, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("items") || !root.get("items").isJsonArray()) {
                return result;
            }
            JsonArray items = root.getAsJsonArray("items");
            for (JsonElement element : items) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject item = element.getAsJsonObject();
                String applicationId = trim(item.has("applicationId") && !item.get("applicationId").isJsonNull()
                        ? item.get("applicationId").getAsString()
                        : "");
                if (!applicationId.isEmpty()) {
                    result.add(applicationId, item);
                }
            }
        } catch (Exception ignored) {
            return result;
        }
        return result;
    }

    private JsonObject buildResumeView(JsonObject resume, String taId) {
        if (resume == null) {
            return null;
        }
        String relativePath = trim(resume.has("relativePath") && !resume.get("relativePath").isJsonNull()
                ? resume.get("relativePath").getAsString()
                : "");
        if (relativePath.isEmpty()) {
            return null;
        }
        Path target = DataMountPaths.taDir().resolve(relativePath).normalize();
        Path taResumeRoot = DataMountPaths.taResumeRoot().resolve(trim(taId)).toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (trim(taId).isEmpty() || !normalizedTarget.startsWith(taResumeRoot) || !Files.exists(normalizedTarget) || Files.isDirectory(normalizedTarget)) {
            return null;
        }

        JsonObject resumeView = new JsonObject();
        resumeView.addProperty("fileName", firstNonBlank(
                resume.has("originalFileName") && !resume.get("originalFileName").isJsonNull() ? resume.get("originalFileName").getAsString() : "",
                resume.has("storedFileName") && !resume.get("storedFileName").isJsonNull() ? resume.get("storedFileName").getAsString() : "简历文件"
        ));
        resumeView.addProperty("relativePath", relativePath);
        resumeView.addProperty("mimeType", trim(resume.has("mimeType") && !resume.get("mimeType").isJsonNull() ? resume.get("mimeType").getAsString() : ""));
        if (resume.has("extension") && !resume.get("extension").isJsonNull()) {
            resumeView.add("extension", resume.get("extension").deepCopy());
        }
        if (resume.has("size") && !resume.get("size").isJsonNull()) {
            resumeView.add("size", resume.get("size").deepCopy());
        }
        resumeView.addProperty("downloadUrl", "../../api/ta/applications/resume?taId=" + encodeQueryParam(taId) + "&path=" + encodeQueryParam(relativePath));
        return resumeView;
    }

    private JsonArray buildAppliedCourseCodes(JsonArray items) {
        JsonArray courseCodes = new JsonArray();
        if (items == null) {
            return courseCodes;
        }
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isJsonObject()) {
                continue;
            }
            JsonObject item = items.get(i).getAsJsonObject();
            String courseCode = trim(item.has("courseCode") && !item.get("courseCode").isJsonNull()
                    ? item.get("courseCode").getAsString()
                    : "");
            if (!courseCode.isEmpty()) {
                courseCodes.add(courseCode);
            }
        }
        return courseCodes;
    }

    private JsonArray buildAppliedJobIds(JsonArray items) {
        JsonArray jobIds = new JsonArray();
        if (items == null) {
            return jobIds;
        }
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isJsonObject()) {
                continue;
            }
            JsonObject item = items.get(i).getAsJsonObject();
            String jobId = trim(item.has("jobId") && !item.get("jobId").isJsonNull()
                    ? item.get("jobId").getAsString()
                    : "");
            if (!jobId.isEmpty() && seen.add(jobId)) {
                jobIds.add(jobId);
            }
        }
        return jobIds;
    }

    private JsonObject buildDetailsMap(JsonArray items) {
        JsonObject details = new JsonObject();
        if (items == null) {
            return details;
        }
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isJsonObject()) {
                continue;
            }
            JsonObject item = items.get(i).getAsJsonObject();
            String applicationId = trim(item.has("applicationId") && !item.get("applicationId").isJsonNull()
                    ? item.get("applicationId").getAsString()
                    : "");
            if (applicationId.isEmpty()) {
                continue;
            }
            details.add(applicationId, item.deepCopy());
        }
        return details;
    }

    private String encodeQueryParam(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
