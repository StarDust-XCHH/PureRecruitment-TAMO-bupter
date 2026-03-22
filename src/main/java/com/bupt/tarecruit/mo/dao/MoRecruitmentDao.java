package com.bupt.tarecruit.mo.dao;

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
    private static final String DATA_MOUNT_ENV = "mountDataTAMObupter";
    private static final Path DEFAULT_DATA_ROOT = Path.of("mountDataTAMObupter");

    private static final Path ROOT_PATH = resolveDataRoot();
    private static final Path MO_DIR = ROOT_PATH.resolve("mo");
    private static final Path TA_DIR = ROOT_PATH.resolve("ta");

    private static final Path MO_PENDING_COURSES = MO_DIR.resolve("pending-recruitment-courses.json");
    private static final Path TA_APPLICATION_STATUS = TA_DIR.resolve("application-status.json");
    private static final Path TA_PROFILES = TA_DIR.resolve("profiles.json");
    private static final Path TA_ACCOUNTS = TA_DIR.resolve("tas.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public synchronized JsonObject getPendingCourses() throws IOException {
        JsonObject root = ensureStructuredFile(
                MO_PENDING_COURSES,
                "mo-pending-recruitment-courses",
                "pending-recruitment-courses"
        );

        JsonArray items = root.getAsJsonArray("items");
        JsonObject payload = new JsonObject();
        payload.addProperty("schema", "mo-pending-recruitment-courses");
        payload.addProperty("generatedAt", getMetaUpdatedAt(root));
        payload.addProperty("count", items.size());
        payload.add("items", items.deepCopy());
        return payload;
    }

    public synchronized JsonObject createCourse(String moName, JsonObject input) throws IOException {
        JsonObject root = ensureStructuredFile(
                MO_PENDING_COURSES,
                "mo-pending-recruitment-courses",
                "pending-recruitment-courses"
        );
        JsonArray items = root.getAsJsonArray("items");

        String courseName = trim(getAsString(input, "courseName"));
        String courseDate = trim(getAsString(input, "courseDate"));
        String courseTime = trim(getAsString(input, "courseTime"));
        String courseLocation = trim(getAsString(input, "courseLocation"));
        String courseDescription = trim(getAsString(input, "courseDescription"));
        String rawTags = trim(getAsString(input, "keywordTags"));
        String rawChecklist = trim(getAsString(input, "checklist"));

        if (courseName.isEmpty() || courseDate.isEmpty() || courseTime.isEmpty()) {
            throw new IllegalArgumentException("Course name, date, and time are required");
        }

        JsonObject item = new JsonObject();
        item.addProperty("courseName", courseName);
        item.addProperty("courseCode", buildCourseCode(courseName));
        item.addProperty("moName", moName.isBlank() ? "MO" : moName);
        item.addProperty("courseDate", courseDate);
        item.addProperty("courseTime", courseTime);
        item.addProperty("courseLocation", courseLocation.isBlank() ? "TBD" : courseLocation);
        item.addProperty("studentCount", 0);
        item.addProperty("status", "Recruiting TA");
        item.addProperty("workload", "Prep, in-class support, and after-class Q&A");
        item.addProperty("courseDescription", courseDescription.isBlank() ? "New course published by the MO; open for TA applications." : courseDescription);
        item.add("keywordTags", parseCsvToArray(rawTags));
        item.add("checklist", parseCsvToArray(rawChecklist));
        item.addProperty("suggestion", "Prioritize TA applicants whose background matches the course focus.");
        item.addProperty("createdAt", Instant.now().toString());
        item.addProperty("source", "mo-manual");

        items.add(item);
        updateMeta(root, "mo-pending-recruitment-courses", "pending-recruitment-courses");
        writeJson(MO_PENDING_COURSES, root);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("message", "Course published successfully");
        result.add("item", item.deepCopy());
        return result;
    }

    public synchronized JsonArray getApplicantsForCourse(String courseCode) throws IOException {
        String normalizedCourseCode = trim(courseCode);
        if (normalizedCourseCode.isBlank()) {
            return new JsonArray();
        }

        JsonObject accountRoot = ensureStructuredFile(TA_ACCOUNTS, "ta", "tas");
        JsonObject profileRoot = ensureStructuredFile(TA_PROFILES, "ta", "profiles");
        JsonObject appRoot = ensureStructuredFile(TA_APPLICATION_STATUS, "ta", "application-status");

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
            row.addProperty("name", firstNonBlank(getAsString(account, "name"), "Unnamed TA"));
            row.addProperty("username", getAsString(account, "username"));
            row.addProperty("email", firstNonBlank(getAsString(profile, "contactEmail"), getAsString(account, "email")));
            row.addProperty("intent", getAsString(profile, "applicationIntent"));
            row.addProperty("skills", joinArrayAsText(profile == null ? null : profile.get("skills")));
            row.addProperty("bio", getAsString(profile, "bio"));
            row.addProperty("avatar", getAsString(profile, "avatar"));

            if (latest == null) {
                row.addProperty("applicationId", "");
                row.addProperty("status", "Invitable");
                row.addProperty("updatedAt", "");
                row.addProperty("comment", "No application on file for this course; the MO may invite proactively.");
            } else {
                row.addProperty("applicationId", getAsString(latest, "applicationId"));
                row.addProperty("status", firstNonBlank(getAsString(latest, "status"), "Under review"));
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
            throw new IllegalArgumentException("courseCode and taId are required");
        }
        if (!"selected".equals(normalizedDecision) && !"rejected".equals(normalizedDecision)) {
            throw new IllegalArgumentException("decision must be selected or rejected");
        }

        JsonObject appRoot = ensureStructuredFile(TA_APPLICATION_STATUS, "ta", "application-status");
        JsonArray appItems = appRoot.getAsJsonArray("items");
        JsonObject course = findCourseByCode(normalizedCourseCode);

        String now = Instant.now().toString();
        String statusText = "selected".equals(normalizedDecision) ? "Hired" : "Not selected";
        String summaryText = normalizedComment.isBlank()
                ? ("selected".equals(normalizedDecision) ? "The MO has confirmed this TA for the role." : "The MO has closed the recruitment process for this candidate.")
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
        target.addProperty("nextAction", "selected".equals(normalizedDecision) ? "Please wait for contract and scheduling details." : "You may apply for other course postings.");
        target.addProperty("nextStep", target.get("nextAction").getAsString());
        target.addProperty("updatedAt", now);
        target.addProperty("category", "MO recruitment");
        target.addProperty("matchLevel", "selected".equals(normalizedDecision) ? "High" : "Medium");

        updateMeta(appRoot, "ta", "application-status");
        writeJson(TA_APPLICATION_STATUS, appRoot);

        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        payload.addProperty("message", "selected".equals(normalizedDecision) ? "TA marked as hired" : "TA marked as not selected");
        payload.addProperty("taId", normalizedTaId);
        payload.addProperty("courseCode", normalizedCourseCode);
        payload.addProperty("status", statusText);
        payload.addProperty("updatedAt", now);
        return payload;
    }

    private JsonObject findCourseByCode(String courseCode) throws IOException {
        JsonObject courses = ensureStructuredFile(MO_PENDING_COURSES, "mo-pending-recruitment-courses", "pending-recruitment-courses");
        for (JsonElement item : courses.getAsJsonArray("items")) {
            if (!item.isJsonObject()) {
                continue;
            }
            JsonObject obj = item.getAsJsonObject();
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

    private JsonObject ensureStructuredFile(Path path, String schema, String entity) throws IOException {
        Files.createDirectories(path.getParent());
        if (!Files.exists(path)) {
            JsonObject root = new JsonObject();
            JsonObject meta = new JsonObject();
            meta.addProperty("schema", schema);
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
                updateMeta(root, schema, entity);
                return root;
            }
        }
        JsonObject fallback = new JsonObject();
        fallback.add("meta", new JsonObject());
        fallback.add("items", new JsonArray());
        updateMeta(fallback, schema, entity);
        return fallback;
    }

    private void updateMeta(JsonObject root, String schema, String entity) {
        JsonObject meta = root.getAsJsonObject("meta");
        meta.addProperty("schema", schema);
        meta.addProperty("entity", entity);
        meta.addProperty("version", "1.0");
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

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static Path resolveDataRoot() {
        String envValue = System.getenv(DATA_MOUNT_ENV);
        if (envValue == null || envValue.trim().isEmpty()) {
            return DEFAULT_DATA_ROOT.toAbsolutePath().normalize();
        }
        return Path.of(envValue.trim()).toAbsolutePath().normalize();
    }
}
