package com.bupt.tarecruit.mo.dao;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MoRecruitmentDao {
    private static final Path MO_PENDING_COURSES = DataMountPaths.moRecruitmentCourses();
    private static final Path TA_APPLICATION_STATUS = DataMountPaths.taApplicationStatus();
    private static final Path TA_PROFILES = DataMountPaths.taProfiles();
    private static final Path TA_ACCOUNTS = DataMountPaths.taAccounts();
    private static final String JOB_BOARD_SCHEMA = "mo-ta-job-board";
    private static final String JOB_BOARD_ENTITY = "jobs";
    private static final String JOB_BOARD_VERSION = "2.0";
    private static final String TA_SCHEMA = "ta";
    private static final String TA_ENTITY_TAS = "tas";
    private static final String TA_ENTITY_PROFILES = "profiles";
    private static final String TA_ENTITY_APPLICATION_STATUS = "application-status";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public synchronized JsonObject getPendingCourses() throws IOException {
        JsonObject root = ensureStructuredFile(
                MO_PENDING_COURSES,
                JOB_BOARD_SCHEMA,
                JOB_BOARD_ENTITY,
                JOB_BOARD_VERSION
        );

        JsonArray items = root.getAsJsonArray("items");
        JsonArray normalizedItems = new JsonArray();
        for (JsonElement element : items) {
            if (element != null && element.isJsonObject()) {
                normalizedItems.add(normalizeJobItem(element.getAsJsonObject()));
            }
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("schema", JOB_BOARD_SCHEMA);
        payload.addProperty("version", JOB_BOARD_VERSION);
        payload.addProperty("generatedAt", getMetaUpdatedAt(root));
        payload.addProperty("count", normalizedItems.size());
        payload.add("items", normalizedItems);

        // Read path does not write the file: avoids rewriting every item (e.g. stripping optional
        // empty keys on unrelated courses) whenever MO lists jobs. Normalization is for the response
        // only; persistence happens on createCourse / other explicit writes. Legacy rows on disk may
        // stay pre-v2-shaped until edited or migrated elsewhere.
        return payload;
    }

    public synchronized JsonObject createCourse(String moName, JsonObject input) throws IOException {
        JsonObject root = ensureStructuredFile(
                MO_PENDING_COURSES,
                JOB_BOARD_SCHEMA,
                JOB_BOARD_ENTITY,
                JOB_BOARD_VERSION
        );
        JsonArray items = root.getAsJsonArray("items");

        String now = Instant.now().toString();
        String finalMoName = moName == null || moName.isBlank() ? "MO" : moName.trim();
        String courseName = trim(getAsString(input, "courseName"));
        if (courseName.isEmpty()) {
            throw new IllegalArgumentException("课程名称不能为空");
        }
        String jobId = "MOJOB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String courseCode = trim(getAsString(input, "courseCode"));
        if (courseCode.isEmpty()) {
            throw new IllegalArgumentException("课程编号不能为空");
        }
        String recruitmentStatus = firstNonBlank(trim(getAsString(input, "recruitmentStatus")), trim(getAsString(input, "status")));
        if (recruitmentStatus.isEmpty()) {
            throw new IllegalArgumentException("招聘状态不能为空");
        }
        String semester = trim(getAsString(input, "semester"));
        String applicationDeadline = trim(getAsString(input, "applicationDeadline"));

        JsonObject teachingWeeks = normalizeTeachingWeeks(input.has("teachingWeeks") && input.get("teachingWeeks").isJsonObject()
                ? input.getAsJsonObject("teachingWeeks")
                : null);
        JsonArray assessmentEvents = normalizeAssessmentEvents(input.has("assessmentEvents") ? input.get("assessmentEvents") : null);
        JsonObject requiredSkills = normalizeRequiredSkills(input.has("requiredSkills") ? input.get("requiredSkills") : null);
        if (!hasRequiredSkills(requiredSkills)) {
            throw new IllegalArgumentException("技能标签不能为空");
        }

        String courseDescription = trim(getAsString(input, "courseDescription"));
        if (courseDescription.isEmpty()) {
            throw new IllegalArgumentException("岗位描述不能为空");
        }
        String ownerMoName = firstNonBlank(trim(getAsString(input, "ownerMoName")), finalMoName);
        String ownerMoId = firstNonBlank(trim(getAsString(input, "ownerMoId")), ownerMoName);
        String recruitmentBrief = trim(getAsString(input, "recruitmentBrief"));
        String workload = trim(getAsString(input, "workload"));
        String campus = normalizeCampus(getAsString(input, "campus"));
        Integer taRecruitCount = getOptionalInt(input, "taRecruitCount");
        Integer studentCount = getOptionalInt(input, "studentCount");
        int normalizedStudentCount = studentCount == null ? -1 : studentCount;

        JsonObject item = new JsonObject();
        item.addProperty("jobId", jobId);
        item.addProperty("courseCode", courseCode);
        item.addProperty("courseName", courseName);
        item.addProperty("moName", finalMoName);
        item.addProperty("ownerMoId", ownerMoId);
        item.addProperty("ownerMoName", ownerMoName);
        if (!semester.isBlank()) {
            item.addProperty("semester", semester);
        }
        item.addProperty("status", recruitmentStatus); // compatibility field for existing readers
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

        items.add(item);
        updateMeta(root, JOB_BOARD_SCHEMA, JOB_BOARD_ENTITY, JOB_BOARD_VERSION);
        writeJson(MO_PENDING_COURSES, root);

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

        JsonObject accountRoot = ensureTaStructuredFile(TA_ACCOUNTS, TA_ENTITY_TAS);
        JsonObject profileRoot = ensureTaStructuredFile(TA_PROFILES, TA_ENTITY_PROFILES);
        JsonObject appRoot = ensureTaStructuredFile(TA_APPLICATION_STATUS, TA_ENTITY_APPLICATION_STATUS);

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

        JsonObject appRoot = ensureTaStructuredFile(TA_APPLICATION_STATUS, TA_ENTITY_APPLICATION_STATUS);
        JsonArray appItems = appRoot.getAsJsonArray("items");
        JsonObject course = findCourseByCode(normalizedCourseCode);

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

    private JsonObject findCourseByCode(String courseCode) throws IOException {
        JsonObject courses = ensureStructuredFile(MO_PENDING_COURSES, JOB_BOARD_SCHEMA, JOB_BOARD_ENTITY, JOB_BOARD_VERSION);
        for (JsonElement item : courses.getAsJsonArray("items")) {
            if (!item.isJsonObject()) {
                continue;
            }
            JsonObject obj = normalizeJobItem(item.getAsJsonObject());
            if (courseCode.equalsIgnoreCase(getAsString(obj, "courseCode"))) {
                return obj;
            }
        }
        return null;
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

    private JsonObject ensureTaStructuredFile(Path path, String entity) throws IOException {
        return ensureStructuredFile(path, TA_SCHEMA, entity, "1.0");
    }

    private JsonObject ensureStructuredFile(Path path, String schema, String entity, String version) throws IOException {
        Files.createDirectories(path.getParent());
        if (!Files.exists(path)) {
            JsonObject root = new JsonObject();
            JsonObject meta = new JsonObject();
            meta.addProperty("schema", schema);
            meta.addProperty("entity", entity);
            meta.addProperty("version", version);
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
                updateMeta(root, schema, entity, version);
                return root;
            }
        }
        JsonObject fallback = new JsonObject();
        fallback.add("meta", new JsonObject());
        fallback.add("items", new JsonArray());
        updateMeta(fallback, schema, entity, version);
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

    private String getMetaUpdatedAt(JsonObject root) {
        if (!root.has("meta") || !root.get("meta").isJsonObject()) {
            return "";
        }
        JsonObject meta = root.getAsJsonObject("meta");
        return getAsString(meta, "updatedAt");
    }

    private JsonArray parseCsvToArray(String csv) {
        JsonArray array = new JsonArray();
        if (csv == null || csv.isBlank()) {
            return array;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            String value = trim(part);
            if (!value.isBlank()) {
                array.add(value);
            }
        }
        return array;
    }

    private JsonArray parseStringList(JsonElement element) {
        JsonArray result = new JsonArray();
        if (element == null || element.isJsonNull()) {
            return result;
        }
        if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item != null && !item.isJsonNull()) {
                    String text = trim(item.getAsString());
                    if (!text.isBlank()) {
                        result.add(text);
                    }
                }
            }
            return result;
        }
        if (element.isJsonPrimitive()) {
            return parseCsvToArray(element.getAsString());
        }
        return result;
    }

    private JsonObject normalizeTeachingWeeks(JsonObject source) {
        JsonObject out = new JsonObject();
        JsonArray weeks = new JsonArray();
        if (source != null && source.has("weeks") && source.get("weeks").isJsonArray()) {
            List<Integer> values = new ArrayList<>();
            for (JsonElement week : source.getAsJsonArray("weeks")) {
                if (week == null || week.isJsonNull()) continue;
                try {
                    int w = week.getAsInt();
                    if (w >= 1 && w <= 20 && !values.contains(w)) {
                        values.add(w);
                    }
                } catch (Exception ignore) {
                    // keep parsing remaining values
                }
            }
            values.sort(Integer::compareTo);
            for (Integer i : values) {
                weeks.add(i);
            }
        }
        out.add("weeks", weeks);
        return out;
    }

    private JsonArray normalizeAssessmentEvents(JsonElement source) {
        JsonArray out = new JsonArray();
        if (source == null || source.isJsonNull() || !source.isJsonArray()) {
            return out;
        }
        for (JsonElement element : source.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject in = element.getAsJsonObject();
            String name = trim(getAsString(in, "name"));
            if (name.isBlank()) continue;
            JsonObject row = new JsonObject();
            row.addProperty("name", name);
            JsonArray weeks = new JsonArray();
            List<Integer> weekValues = new ArrayList<>();
            if (in.has("weeks") && in.get("weeks").isJsonArray()) {
                for (JsonElement week : in.getAsJsonArray("weeks")) {
                    if (week == null || week.isJsonNull()) continue;
                    try {
                        int w = week.getAsInt();
                        if (w >= 1 && w <= 20 && !weekValues.contains(w)) {
                            weekValues.add(w);
                        }
                    } catch (Exception ignore) {
                        // keep parsing remaining values
                    }
                }
            } else if (in.has("week")) {
                int legacyWeek = getAsInt(in, "week", 0);
                if (legacyWeek >= 1 && legacyWeek <= 20) {
                    weekValues.add(legacyWeek);
                }
            }
            weekValues.sort(Integer::compareTo);
            for (Integer w : weekValues) {
                weeks.add(w);
            }
            row.add("weeks", weeks);
            row.addProperty("description", getAsString(in, "description"));
            out.add(row);
        }
        return out;
    }

    private JsonObject normalizeRequiredSkills(JsonElement source) {
        JsonObject out = new JsonObject();
        JsonArray fixedTags = new JsonArray();
        JsonArray customSkills = new JsonArray();
        if (source != null && source.isJsonObject()) {
            JsonObject in = source.getAsJsonObject();
            if (in.has("fixedTags")) {
                fixedTags = parseStringList(in.get("fixedTags"));
            }
            if (in.has("customSkills") && in.get("customSkills").isJsonArray()) {
                for (JsonElement e : in.getAsJsonArray("customSkills")) {
                    if (!e.isJsonObject()) continue;
                    JsonObject obj = e.getAsJsonObject();
                    String name = trim(getAsString(obj, "name"));
                    if (name.isBlank()) continue;
                    JsonObject row = new JsonObject();
                    row.addProperty("name", name);
                    row.addProperty("description", getAsString(obj, "description"));
                    customSkills.add(row);
                }
            }
        }
        out.add("fixedTags", fixedTags);
        out.add("customSkills", customSkills);
        return out;
    }

    private boolean hasRequiredSkills(JsonObject skills) {
        if (skills == null) {
            return false;
        }
        JsonArray fixed = skills.has("fixedTags") && skills.get("fixedTags").isJsonArray()
                ? skills.getAsJsonArray("fixedTags")
                : new JsonArray();
        JsonArray custom = skills.has("customSkills") && skills.get("customSkills").isJsonArray()
                ? skills.getAsJsonArray("customSkills")
                : new JsonArray();
        return !fixed.isEmpty() || !custom.isEmpty();
    }

    private String normalizeCampus(String value) {
        String campus = trim(value);
        if ("Main".equalsIgnoreCase(campus)) {
            return "Main";
        }
        if ("Shahe".equalsIgnoreCase(campus)) {
            return "Shahe";
        }
        return "";
    }

    private JsonObject normalizeJobItem(JsonObject source) {
        JsonObject item = source.deepCopy();
        String courseName = firstNonBlank(getAsString(item, "courseName"), "Untitled TA Job");
        item.addProperty("courseName", courseName);
        item.addProperty("moName", firstNonBlank(getAsString(item, "moName"), "MO"));
        item.addProperty("ownerMoName", firstNonBlank(getAsString(item, "ownerMoName"), getAsString(item, "moName")));
        item.addProperty("ownerMoId", firstNonBlank(getAsString(item, "ownerMoId"), getAsString(item, "ownerMoName")));
        if (trim(getAsString(item, "semester")).isBlank()) item.remove("semester");
        else item.addProperty("semester", trim(getAsString(item, "semester")));
        String recruitmentStatus = firstNonBlank(getAsString(item, "recruitmentStatus"), firstNonBlank(getAsString(item, "status"), "OPEN"));
        item.addProperty("recruitmentStatus", recruitmentStatus);
        item.addProperty("status", recruitmentStatus);
        item.addProperty("publishStatus", firstNonBlank(getAsString(item, "publishStatus"), "PENDING_REVIEW"));
        item.addProperty("visibility", firstNonBlank(getAsString(item, "visibility"), "INTERNAL"));
        item.addProperty("isArchived", item.has("isArchived") && !item.get("isArchived").isJsonNull() && item.get("isArchived").getAsBoolean());
        item.addProperty("auditStatus", firstNonBlank(getAsString(item, "auditStatus"), "PENDING"));
        item.addProperty("auditComment", firstNonBlank(getAsString(item, "auditComment"), ""));
        item.addProperty("priority", firstNonBlank(getAsString(item, "priority"), "NORMAL"));
        item.addProperty("dataVersion", Math.max(1, getAsInt(item, "dataVersion", 1)));
        item.addProperty("lastSyncedAt", firstNonBlank(getAsString(item, "lastSyncedAt"), ""));
        item.addProperty("jobId", firstNonBlank(getAsString(item, "jobId"), "MOJOB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT)));
        item.addProperty("courseCode", firstNonBlank(getAsString(item, "courseCode"), buildCourseCode(courseName)));
        item.addProperty("studentCount", getAsInt(item, "studentCount", -1));
        item.addProperty("recruitedCount", Math.max(0, getAsInt(item, "recruitedCount", 0)));
        item.addProperty("applicationsTotal", Math.max(0, getAsInt(item, "applicationsTotal", 0)));
        item.addProperty("applicationsPending", Math.max(0, getAsInt(item, "applicationsPending", 0)));
        item.addProperty("applicationsAccepted", Math.max(0, getAsInt(item, "applicationsAccepted", 0)));
        item.addProperty("applicationsRejected", Math.max(0, getAsInt(item, "applicationsRejected", 0)));
        item.addProperty("lastApplicationAt", firstNonBlank(getAsString(item, "lastApplicationAt"), ""));
        item.addProperty("lastSelectionAt", firstNonBlank(getAsString(item, "lastSelectionAt"), ""));
        if (item.has("taRecruitCount")) {
            item.addProperty("taRecruitCount", getAsInt(item, "taRecruitCount", 0));
        }
        if (item.has("campus")) {
            String campus = normalizeCampus(getAsString(item, "campus"));
            if (campus.isBlank()) {
                item.remove("campus");
            } else {
                item.addProperty("campus", campus);
            }
        }
        if (item.has("teachingWeeks") && item.get("teachingWeeks").isJsonObject()) {
            JsonObject teachingWeeks = normalizeTeachingWeeks(item.getAsJsonObject("teachingWeeks"));
            if (teachingWeeks.getAsJsonArray("weeks").isEmpty()) {
                item.remove("teachingWeeks");
            } else {
                item.add("teachingWeeks", teachingWeeks);
            }
        } else {
            item.remove("teachingWeeks");
        }
        JsonArray assessmentEvents = normalizeAssessmentEvents(item.get("assessmentEvents"));
        if (!assessmentEvents.isEmpty()) {
            item.add("assessmentEvents", assessmentEvents);
        } else {
            item.remove("assessmentEvents");
        }
        item.remove("customLabels");
        if (trim(getAsString(item, "applicationDeadline")).isBlank()) item.remove("applicationDeadline");
        else item.addProperty("applicationDeadline", trim(getAsString(item, "applicationDeadline")));
        item.add("requiredSkills", normalizeRequiredSkills(item.get("requiredSkills")));
        item.addProperty("courseDescription", getAsString(item, "courseDescription"));
        String recruitmentBrief = trim(getAsString(item, "recruitmentBrief"));
        if (recruitmentBrief.isBlank()) item.remove("recruitmentBrief");
        else item.addProperty("recruitmentBrief", recruitmentBrief);
        String workload = trim(getAsString(item, "workload"));
        if (workload.isBlank()) item.remove("workload");
        else item.addProperty("workload", workload);
        item.remove("suggestion");
        item.remove("checklist");

        String now = Instant.now().toString();
        item.addProperty("createdAt", firstNonBlank(getAsString(item, "createdAt"), now));
        item.addProperty("updatedAt", firstNonBlank(getAsString(item, "updatedAt"), now));
        String sourceText = trim(getAsString(item, "source"));
        if (sourceText.isBlank()) item.remove("source");
        else item.addProperty("source", sourceText);
        return item;
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

    private String buildCourseCode(String courseName) {
        String base = "MO-" + courseName.replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5]", "");
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        return (base.length() > 12 ? base.substring(0, 12) : base) + "-" + suffix;
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

    private String firstNonBlank(String primary, String fallback) {
        String first = trim(primary);
        return first.isBlank() ? trim(fallback) : first;
    }

    private String getAsString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private int getAsInt(JsonObject object, String key, int fallback) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private Integer getOptionalInt(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ex) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

}
