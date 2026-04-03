package com.bupt.tarecruit.mo.dao;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.common.dao.RecruitmentCoursesDao;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.firstNonBlank;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getOptionalInt;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

public class MoRecruitmentDao {
    private static final Path TA_APPLICATION_STATUS = DataMountPaths.taApplicationStatus();
    private static final Path TA_PROFILES = DataMountPaths.taProfiles();
    private static final Path TA_ACCOUNTS = DataMountPaths.taAccounts();
    private static final String TA_SCHEMA = "ta";
    private static final String TA_ENTITY_TAS = "tas";
    private static final String TA_ENTITY_PROFILES = "profiles";
    private static final String TA_ENTITY_APPLICATION_STATUS = "application-status";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public JsonObject getPendingCourses() throws IOException {
        return RecruitmentCoursesDao.readJobBoard();
    }

    public JsonObject createCourse(JsonObject input) throws IOException {
        String now = Instant.now().toString();
        String courseName = trim(getAsString(input, "courseName"));
        if (courseName.isEmpty()) {
            throw new IllegalArgumentException("课程名称不能为空");
        }
        String jobId = "MOJOB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String courseCode = trim(getAsString(input, "courseCode"));
        if (courseCode.isEmpty()) {
            throw new IllegalArgumentException("课程编号不能为空");
        }
        String recruitmentStatus = firstNonBlank(
                trim(getAsString(input, "recruitmentStatus")),
                trim(getAsString(input, "status")));
        if (recruitmentStatus.isEmpty()) {
            throw new IllegalArgumentException("招聘状态不能为空");
        }
        String semester = trim(getAsString(input, "semester"));
        String applicationDeadline = trim(getAsString(input, "applicationDeadline"));

        JsonObject teachingWeeks = RecruitmentCoursesDao.normalizeTeachingWeeks(
                input.has("teachingWeeks") && input.get("teachingWeeks").isJsonObject()
                        ? input.getAsJsonObject("teachingWeeks")
                        : null);
        JsonArray assessmentEvents = RecruitmentCoursesDao.normalizeAssessmentEvents(
                input.has("assessmentEvents") ? input.get("assessmentEvents") : null);
        JsonObject requiredSkills = RecruitmentCoursesDao.normalizeRequiredSkills(
                input.has("requiredSkills") ? input.get("requiredSkills") : null);
        if (!RecruitmentCoursesDao.hasRequiredSkills(requiredSkills)) {
            throw new IllegalArgumentException("技能标签不能为空");
        }

        String courseDescription = trim(getAsString(input, "courseDescription"));
        if (courseDescription.isEmpty()) {
            throw new IllegalArgumentException("岗位描述不能为空");
        }
        String ownerMoName = firstNonBlank(
                trim(getAsString(input, "ownerMoName")),
                firstNonBlank(
                        trim(getAsString(input, "ownerMoId")), "MO"));
        String ownerMoId = firstNonBlank(
                trim(getAsString(input, "ownerMoId")), ownerMoName);
        String recruitmentBrief = trim(getAsString(input, "recruitmentBrief"));
        String workload = trim(getAsString(input, "workload"));
        String campus = RecruitmentCoursesDao.normalizeCampus(getAsString(input, "campus"));
        Integer taRecruitCount = getOptionalInt(input, "taRecruitCount");
        Integer studentCount = getOptionalInt(input, "studentCount");
        int normalizedStudentCount = studentCount == null ? -1 : studentCount;

        JsonObject item = new JsonObject();
        item.addProperty("jobId", jobId);
        item.addProperty("courseCode", courseCode);
        item.addProperty("courseName", courseName);
        item.addProperty("ownerMoId", ownerMoId);
        item.addProperty("ownerMoName", ownerMoName);
        if (!semester.isBlank()) {
            item.addProperty("semester", semester);
        }
        item.addProperty("status", recruitmentStatus);
        item.addProperty("recruitmentStatus", recruitmentStatus);
        item.addProperty("publishStatus", "PENDING_REVIEW");
        item.addProperty("visibility", "INTERNAL");
        item.addProperty("isArchived", false);
        item.addProperty("auditStatus", "PENDING");
        item.addProperty("auditComment", "");
        item.addProperty("priority", "NORMAL");
        item.addProperty("dataVersion", 1);
        item.addProperty("lastSyncedAt", "");
        item.addProperty("studentCount", normalizedStudentCount);
        item.addProperty("recruitedCount", 0);
        item.addProperty("applicationsTotal", 0);
        item.addProperty("applicationsPending", 0);
        item.addProperty("applicationsAccepted", 0);
        item.addProperty("applicationsRejected", 0);
        item.addProperty("lastApplicationAt", "");
        item.addProperty("lastSelectionAt", "");
        if (taRecruitCount != null) {
            item.addProperty("taRecruitCount", taRecruitCount);
        }
        if (!campus.isBlank()) {
            item.addProperty("campus", campus);
        }
        if (!applicationDeadline.isBlank()) {
            item.addProperty("applicationDeadline", applicationDeadline);
        }
        if (!teachingWeeks.getAsJsonArray("weeks").isEmpty()) {
            item.add("teachingWeeks", teachingWeeks);
        }
        if (!assessmentEvents.isEmpty()) {
            item.add("assessmentEvents", assessmentEvents);
        }
        item.add("requiredSkills", requiredSkills);
        item.addProperty("courseDescription", courseDescription);
        if (!recruitmentBrief.isBlank()) {
            item.addProperty("recruitmentBrief", recruitmentBrief);
        }
        if (!workload.isBlank()) {
            item.addProperty("workload", workload);
        }
        item.addProperty("createdAt", now);
        item.addProperty("updatedAt", now);
        String source = trim(getAsString(input, "source"));
        if (!source.isBlank()) {
            item.addProperty("source", source);
        }

        RecruitmentCoursesDao.appendPublishedJob(item);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("message", "课程发布成功");
        result.add("item", item.deepCopy());
        return result;
    }

    public synchronized JsonArray getApplicantsForCourse(String courseCode) throws IOException {
        String normalizedCourseCode = trim(courseCode);
        if (normalizedCourseCode.isBlank()) {
            return new JsonArray();
        }

        JsonObject accountRoot = ensureStructuredFile(TA_ACCOUNTS, TA_ENTITY_TAS);
        JsonObject profileRoot = ensureStructuredFile(TA_PROFILES, TA_ENTITY_PROFILES);
        JsonObject appRoot = ensureStructuredFile(TA_APPLICATION_STATUS, TA_ENTITY_APPLICATION_STATUS);

        JsonArray accountItems = accountRoot.getAsJsonArray("items");
        JsonArray profileItems = profileRoot.getAsJsonArray("items");
        JsonArray appItems = appRoot.getAsJsonArray("items");

        List<JsonObject> rows = new ArrayList<>();
        String targetSlug = normalizeSlug(normalizedCourseCode);

        for (JsonElement accountElement : accountItems) {
            if (!accountElement.isJsonObject()) {
                continue;
            }
            JsonObject account = accountElement.getAsJsonObject();
            String taId = getAsString(account, "id");
            if (taId.isBlank()) {
                continue;
            }

            JsonObject profile = findProfile(profileItems, taId);
            JsonObject latest = findLatestApplicationForCourse(appItems, taId, normalizedCourseCode, targetSlug);

            JsonObject row = new JsonObject();
            row.addProperty("taId", taId);
            row.addProperty("name", firstNonBlank(getAsString(account, "name"), "未命名 TA"));
            row.addProperty("username", getAsString(account, "username"));
            row.addProperty("email", firstNonBlank(getAsString(profile, "contactEmail"), getAsString(account, "email")));
            row.addProperty("intent", getAsString(profile, "applicationIntent"));
            row.addProperty("skills", joinArrayAsText(profile == null ? null : profile.get("skills")));
            row.addProperty("bio", getAsString(profile, "bio"));
            row.addProperty("avatar", getAsString(profile, "avatar"));

            if (latest == null) {
                row.addProperty("applicationId", "");
                row.addProperty("status", "可邀请");
                row.addProperty("updatedAt", "");
                row.addProperty("comment", "暂无该课程投递记录，可由 MO 主动邀请。");
            } else {
                row.addProperty("applicationId", getAsString(latest, "applicationId"));
                row.addProperty("status", firstNonBlank(getAsString(latest, "status"), "审核中"));
                row.addProperty("updatedAt", getAsString(latest, "updatedAt"));
                row.addProperty("comment", firstNonBlank(getAsString(latest, "summary"), getAsString(latest, "moComment")));
            }
            rows.add(row);
        }

        rows.sort(Comparator.comparing(o -> getAsString(o, "name"), String.CASE_INSENSITIVE_ORDER));
        JsonArray result = new JsonArray();
        for (JsonObject row : rows) {
            result.add(row);
        }
        return result;
    }

    public synchronized JsonObject decideApplication(String courseCode, String taId, String decision, String comment) throws IOException {
        String normalizedCourseCode = trim(courseCode);
        String normalizedTaId = trim(taId);
        String normalizedDecision = trim(decision).toLowerCase(Locale.ROOT);
        String normalizedComment = trim(comment);

        if (normalizedCourseCode.isBlank() || normalizedTaId.isBlank()) {
            throw new IllegalArgumentException("courseCode 与 taId 不能为空");
        }
        if (!"selected".equals(normalizedDecision) && !"rejected".equals(normalizedDecision)) {
            throw new IllegalArgumentException("decision 仅支持 selected 或 rejected");
        }

        JsonObject appRoot = ensureStructuredFile(TA_APPLICATION_STATUS, TA_ENTITY_APPLICATION_STATUS);
        JsonArray appItems = appRoot.getAsJsonArray("items");
        JsonObject course = RecruitmentCoursesDao.findNormalizedJobByCourseCode(normalizedCourseCode);

        String now = Instant.now().toString();
        String statusText = "selected".equals(normalizedDecision) ? "已录用" : "未录用";
        String summaryText = normalizedComment.isBlank()
                ? ("selected".equals(normalizedDecision) ? "MO 已确认录用该 TA。" : "MO 已结束该候选人的招聘流程。")
                : normalizedComment;

        JsonObject target = findLatestApplicationForCourse(appItems, normalizedTaId, normalizedCourseCode, normalizeSlug(normalizedCourseCode));
        if (target == null) {
            target = new JsonObject();
            target.addProperty("applicationId", "APP-" + normalizedTaId + "-" + sanitizeCode(normalizedCourseCode));
            target.addProperty("taId", normalizedTaId);
            target.addProperty("courseName", course == null ? normalizedCourseCode : getAsString(course, "courseName"));
            target.addProperty("jobSlug", normalizeSlug(normalizedCourseCode));
            target.add("tags", new JsonArray());
            target.add("timeline", new JsonArray());
            target.add("details", new JsonArray());
            target.add("notifications", new JsonArray());
            appItems.add(target);
        }

        target.addProperty("status", statusText);
        target.addProperty("statusTone", "selected".equals(normalizedDecision) ? "success" : "danger");
        target.addProperty("summary", summaryText);
        target.addProperty("moComment", summaryText);
        target.addProperty("nextAction", "selected".equals(normalizedDecision) ? "请等待签约与排班通知。" : "可继续申请其他课程岗位。");
        target.addProperty("nextStep", target.get("nextAction").getAsString());
        target.addProperty("updatedAt", now);
        target.addProperty("category", "MO 招聘流程");
        target.addProperty("matchLevel", "selected".equals(normalizedDecision) ? "高" : "中");

        updateMeta(appRoot, TA_SCHEMA, TA_ENTITY_APPLICATION_STATUS, "1.0");
        writeJson(TA_APPLICATION_STATUS, appRoot);

        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        payload.addProperty("message", "selected".equals(normalizedDecision) ? "已录用该 TA" : "已拒绝该 TA");
        payload.addProperty("taId", normalizedTaId);
        payload.addProperty("courseCode", normalizedCourseCode);
        payload.addProperty("status", statusText);
        payload.addProperty("updatedAt", now);
        return payload;
    }

    private JsonObject findProfile(JsonArray profiles, String taId) {
        for (JsonElement element : profiles) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject profile = element.getAsJsonObject();
            if (taId.equalsIgnoreCase(getAsString(profile, "taId"))) {
                return profile;
            }
        }
        return null;
    }

    private JsonObject findLatestApplicationForCourse(JsonArray apps, String taId, String courseCode, String courseSlug) {
        JsonObject latest = null;
        for (JsonElement element : apps) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject app = element.getAsJsonObject();
            if (!taId.equalsIgnoreCase(getAsString(app, "taId"))) {
                continue;
            }
            String appSlug = normalizeSlug(getAsString(app, "jobSlug"));
            String appCourseName = getAsString(app, "courseName");
            boolean matched = appSlug.equals(courseSlug)
                    || appCourseName.toLowerCase(Locale.ROOT).contains(courseCode.toLowerCase(Locale.ROOT));
            if (!matched) {
                continue;
            }
            if (latest == null || getAsString(app, "updatedAt").compareTo(getAsString(latest, "updatedAt")) > 0) {
                latest = app;
            }
        }
        return latest;
    }

    private JsonObject ensureStructuredFile(Path path, String entity) throws IOException {
        Files.createDirectories(path.getParent());
        if (!Files.exists(path)) {
            JsonObject root = new JsonObject();
            JsonObject meta = new JsonObject();
            meta.addProperty("schema", TA_SCHEMA);
            meta.addProperty("entity", entity);
            meta.addProperty("version", "1.0");
            meta.addProperty("updatedAt", Instant.now().toString());
            root.add("meta", meta);
            root.add("items", new JsonArray());
            writeJson(path, root);
            return root;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
                if (!root.has("meta") || !root.get("meta").isJsonObject()) {
                    root.add("meta", new JsonObject());
                }
                if (!root.has("items") || !root.get("items").isJsonArray()) {
                    root.add("items", new JsonArray());
                }
                updateMeta(root, TA_SCHEMA, entity, "1.0");
                return root;
            }
        }
        JsonObject fallback = new JsonObject();
        fallback.add("meta", new JsonObject());
        fallback.add("items", new JsonArray());
        updateMeta(fallback, TA_SCHEMA, entity, "1.0");
        return fallback;
    }

    private void updateMeta(JsonObject root, String schema, String entity, String version) {
        JsonObject meta = root.getAsJsonObject("meta");
        meta.addProperty("schema", schema);
        meta.addProperty("entity", entity);
        meta.addProperty("version", version);
        meta.addProperty("updatedAt", Instant.now().toString());
    }

    private void writeJson(Path path, JsonObject root) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private String joinArrayAsText(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (JsonElement item : element.getAsJsonArray()) {
            if (item != null && !item.isJsonNull()) {
                String text = trim(item.getAsString());
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
        }
        return String.join(", ", values);
    }

    private String normalizeSlug(String text) {
        return sanitizeCode(text).toLowerCase(Locale.ROOT);
    }

    private String sanitizeCode(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

}
