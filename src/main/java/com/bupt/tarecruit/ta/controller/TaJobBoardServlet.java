package com.bupt.tarecruit.ta.controller;

import com.bupt.tarecruit.common.dao.RecruitmentCoursesDao;
import com.bupt.tarecruit.ta.dao.TaAccountDao;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet(name = "taJobBoardServlet", value = "/api/ta/jobs")
public class TaJobBoardServlet extends HttpServlet {
    private static final String JOB_BOARD_TAG = "[TA-JOB-BOARD]";
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Gson gson = new Gson();
    private final TaAccountDao taAccountDao = new TaAccountDao();

    @Override
    public void init() throws ServletException {
        System.out.println(buildLogLine("INIT", "mapping=/api/ta/jobs, dataRoot=" + TaAccountDao.getResolvedDataRoot()));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        long startTime = System.currentTimeMillis();
        logInfo("REQUEST", "method=" + request.getMethod() + ", requestURI=" + request.getRequestURI());

        try (PrintWriter writer = response.getWriter()) {
            JsonObject payload = taAccountDao.getPendingJobBoardData();
            if (payload == null || !payload.has("items") || !payload.get("items").isJsonArray()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writer.write(buildErrorResponse("暂无开放招聘的岗位数据或数据文件不存在"));
                logInfo("RESPONSE", "status=" + response.getStatus() + ", durationMs=" + (System.currentTimeMillis() - startTime));
                return;
            }

            JsonObject taPayload = buildTaJobBoardPayload(payload);
            response.setStatus(HttpServletResponse.SC_OK);
            writer.write(gson.toJson(taPayload));
            logInfo("RESPONSE", "status=" + response.getStatus()
                    + ", items=" + taPayload.getAsJsonArray("items").size()
                    + ", durationMs=" + (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            logError("ERROR", e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(buildErrorResponse("服务器内部错误，读取岗位数据失败"));
            }
        }
    }

    private JsonObject buildTaJobBoardPayload(JsonObject sourcePayload) {
        JsonObject taPayload = new JsonObject();
        taPayload.addProperty("schema", RecruitmentCoursesDao.JOB_BOARD_SCHEMA);
        if (sourcePayload.has("version")) {
            taPayload.add("version", sourcePayload.get("version"));
        }
        if (sourcePayload.has("generatedAt")) {
            taPayload.add("generatedAt", sourcePayload.get("generatedAt"));
        }

        JsonArray taItems = new JsonArray();
        JsonArray sourceItems = sourcePayload.getAsJsonArray("items");
        for (JsonElement element : sourceItems) {
            if (element != null && element.isJsonObject()) {
                taItems.add(mapToTaJobItem(element.getAsJsonObject()));
            }
        }

        taPayload.addProperty("count", taItems.size());
        taPayload.add("items", taItems);
        return taPayload;
    }

    private JsonObject mapToTaJobItem(JsonObject item) {
        JsonObject taItem = new JsonObject();

        String courseCode = readString(item, "courseCode", "");
        String courseName = readString(item, "courseName", "未命名课程");
        String ownerMoName = readString(item, "ownerMoName", "待分配");
        String courseDescription = readString(item, "courseDescription", "暂无课程简介");
        String recruitmentBrief = readString(item, "recruitmentBrief", "");
        String campus = readString(item, "campus", "");
        int studentCount = readInt(item, "studentCount", -1);
        int taRecruitCount = Math.max(0, readInt(item, "taRecruitCount", 0));

        JsonArray keywordTags = extractKeywordTags(item);
        JsonArray checklist = buildChecklist(item, ownerMoName, taRecruitCount, campus);

        taItem.addProperty("courseCode", courseCode);
        taItem.addProperty("courseName", courseName);
        taItem.addProperty("ownerMoName", ownerMoName);
        taItem.addProperty("studentCount", studentCount);
        taItem.addProperty("courseDescription", courseDescription);
        taItem.add("keywordTags", keywordTags);
        taItem.add("checklist", checklist);
        taItem.addProperty("suggestion", buildSuggestion(courseName, recruitmentBrief, keywordTags, campus));

        if (!recruitmentBrief.isBlank()) {
            taItem.addProperty("recruitmentBrief", recruitmentBrief);
        }
        if (item.has("applicationDeadline") && !item.get("applicationDeadline").isJsonNull()) {
            taItem.add("applicationDeadline", item.get("applicationDeadline"));
        }
        if (item.has("semester") && !item.get("semester").isJsonNull()) {
            taItem.add("semester", item.get("semester"));
        }
        if (item.has("taRecruitCount") && !item.get("taRecruitCount").isJsonNull()) {
            taItem.add("taRecruitCount", item.get("taRecruitCount"));
        }
        if (item.has("campus") && !item.get("campus").isJsonNull()) {
            taItem.add("campus", item.get("campus"));
        }

        return taItem;
    }

    private JsonArray extractKeywordTags(JsonObject item) {
        JsonArray tags = new JsonArray();
        JsonObject requiredSkills = item.has("requiredSkills") && item.get("requiredSkills").isJsonObject()
                ? item.getAsJsonObject("requiredSkills") : null;
        if (requiredSkills == null) {
            return tags;
        }

        if (requiredSkills.has("fixedTags") && requiredSkills.get("fixedTags").isJsonArray()) {
            for (JsonElement tag : requiredSkills.getAsJsonArray("fixedTags")) {
                if (tag != null && !tag.isJsonNull()) {
                    String text = tag.getAsString().trim();
                    if (!text.isEmpty()) {
                        tags.add(text);
                    }
                }
            }
        }
        if (requiredSkills.has("customSkills") && requiredSkills.get("customSkills").isJsonArray()) {
            for (JsonElement skill : requiredSkills.getAsJsonArray("customSkills")) {
                if (skill != null && skill.isJsonObject()) {
                    String name = readString(skill.getAsJsonObject(), "name", "");
                    if (!name.isBlank()) {
                        tags.add(name);
                    }
                }
            }
        }
        return tags;
    }

    private JsonArray buildChecklist(JsonObject item, String ownerMoName, int taRecruitCount, String campus) {
        JsonArray checklist = new JsonArray();
        checklist.add("协助 " + ownerMoName + " 完成课程答疑与课堂支持");

        int studentCount = readInt(item, "studentCount", -1);
        if (studentCount > 0) {
            checklist.add("面向约 " + studentCount + " 名学生提供作业反馈与学习支持");
        } else {
            checklist.add("根据课程进度提供作业反馈、实验支持与学习跟进");
        }

        if (taRecruitCount > 0) {
            checklist.add("与教学团队协作完成本次招聘的 " + taRecruitCount + " 个 TA 岗位分工");
        } else {
            checklist.add("配合教学团队完成课堂组织、资料整理与课后反馈");
        }

        JsonArray assessmentEvents = item.has("assessmentEvents") && item.get("assessmentEvents").isJsonArray()
                ? item.getAsJsonArray("assessmentEvents") : null;
        if (assessmentEvents != null && !assessmentEvents.isEmpty()) {
            JsonObject firstEvent = assessmentEvents.get(0).isJsonObject() ? assessmentEvents.get(0).getAsJsonObject() : null;
            String eventName = firstEvent == null ? "" : readString(firstEvent, "name", "");
            if (!eventName.isBlank()) {
                checklist.add("在 " + eventName + " 等关键教学节点提供现场支持");
            }
        }

        if (!campus.isBlank()) {
            checklist.add("工作校区：" + campus);
        }
        return checklist;
    }

    private String buildSuggestion(String courseName, String recruitmentBrief, JsonArray keywordTags, String campus) {
        if (!recruitmentBrief.isBlank()) {
            return recruitmentBrief;
        }

        StringBuilder suggestion = new StringBuilder();
        suggestion.append("如果你希望参与《").append(courseName).append("》的教学支持工作");
        if (keywordTags != null && !keywordTags.isEmpty()) {
            suggestion.append("，且具备 ").append(keywordTags.get(0).getAsString()).append(" 等相关能力");
        }
        suggestion.append("，建议优先投递");
        if (!campus.isBlank()) {
            suggestion.append("（校区：").append(campus).append("）");
        }
        suggestion.append("。");
        return suggestion.toString();
    }

    private String readString(JsonObject source, String key, String defaultValue) {
        if (source == null || !source.has(key) || source.get(key).isJsonNull()) {
            return defaultValue;
        }
        String value = source.get(key).getAsString();
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private int readInt(JsonObject source, String key, int defaultValue) {
        if (source == null || !source.has(key) || source.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return source.get(key).getAsInt();
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private void logInfo(String phase, String message) {
        System.out.println(buildLogLine(phase, message));
    }

    private void logError(String phase, String message) {
        System.err.println(buildLogLine(phase, message));
    }

    private String buildLogLine(String phase, String message) {
        return LocalDateTime.now().format(LOG_TIME_FORMATTER) + " " + JOB_BOARD_TAG + " [" + phase + "] " + message;
    }

    private String buildErrorResponse(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("schema", "error");
        payload.addProperty("success", false);
        payload.addProperty("message", message);
        payload.add("items", new JsonArray());
        return gson.toJson(payload).getBytes(StandardCharsets.UTF_8).length > 0 ? gson.toJson(payload) : "{\"schema\":\"error\",\"success\":false,\"message\":\"" + message + "\",\"items\":[]}";
    }
}
