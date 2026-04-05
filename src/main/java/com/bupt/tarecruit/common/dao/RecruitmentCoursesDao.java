package com.bupt.tarecruit.common.dao;

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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.firstNonBlank;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsInt;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

/**
 * Shared file access for {@code mountDataTAMObupter/common/recruitment-courses.json}.
 * MO and TA job-board list reads share {@link #readJobBoard()} (v2-normalized items, no write).
 * <p>
 * Gson field helpers ({@code getAsString}, {@code trim}, etc.) live in
 * {@link com.bupt.tarecruit.common.util.GsonJsonObjectUtils}; this class uses them internally via static import.
 */
public final class RecruitmentCoursesDao {

    private static final Object FILE_LOCK = new Object();
    private static final Path FILE = DataMountPaths.moRecruitmentCourses();

    public static final String JOB_BOARD_SCHEMA = "mo-ta-job-board";
    public static final String JOB_BOARD_ENTITY = "jobs";
    public static final String JOB_BOARD_VERSION = "2.0";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private RecruitmentCoursesDao() {
    }

    /**
     * Standard read for MO and TA list APIs: same v2 envelope and per-item {@link #normalizeJobItem(JsonObject)};
     * does not write the file.
     */
    public static JsonObject readJobBoard() throws IOException {
        synchronized (FILE_LOCK) {
            JsonObject root = ensureJobBoardRoot();

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
            return payload;
        }
    }

    /**
     * Append one published job row, sync envelope, persist.
     */
    public static void appendPublishedJob(JsonObject item) throws IOException {
        synchronized (FILE_LOCK) {
            JsonObject root = ensureJobBoardRoot();
            JsonArray items = root.getAsJsonArray("items");
            items.add(item);
            updateMeta(root, JOB_BOARD_SCHEMA, JOB_BOARD_ENTITY, JOB_BOARD_VERSION);
            syncJobBoardFileEnvelope(root, items);
            writeJson(root);
        }
    }

    public static JsonObject findNormalizedJobByCourseCode(String courseCode) throws IOException {
        synchronized (FILE_LOCK) {
            JsonObject courses = ensureJobBoardRoot();
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
    }

    /**
     * Returns one normalized job row by {@code jobId}, or {@code null} if missing or blank id.
     */
    @SuppressWarnings("unused")
    public static JsonObject findNormalizedJobByJobId(String jobId) throws IOException {
        String needle = trim(jobId);
        if (needle.isEmpty()) {
            return null;
        }
        synchronized (FILE_LOCK) {
            JsonObject courses = ensureJobBoardRoot();
            for (JsonElement item : courses.getAsJsonArray("items")) {
                if (!item.isJsonObject()) {
                    continue;
                }
                JsonObject obj = normalizeJobItem(item.getAsJsonObject());
                if (needle.equalsIgnoreCase(getAsString(obj, "jobId"))) {
                    return obj;
                }
            }
            return null;
        }
    }

    /**
     * All normalized rows whose {@code ownerMoId} equals {@code ownerMoId} (ignore case). Blank {@code ownerMoId} yields an empty array.
     */
    @SuppressWarnings("unused")
    public static JsonArray findNormalizedJobsByOwnerMoId(String ownerMoId) throws IOException {
        String needle = trim(ownerMoId);
        JsonArray out = new JsonArray();
        if (needle.isEmpty()) {
            return out;
        }
        synchronized (FILE_LOCK) {
            JsonObject root = ensureJobBoardRoot();
            for (JsonElement item : root.getAsJsonArray("items")) {
                if (!item.isJsonObject()) {
                    continue;
                }
                JsonObject obj = normalizeJobItem(item.getAsJsonObject());
                if (needle.equalsIgnoreCase(getAsString(obj, "ownerMoId"))) {
                    out.add(obj);
                }
            }
            return out;
        }
    }

    /**
     * Normalized rows matching governance filters. Each non-blank argument must match (AND). Ignore-case for string equality.
     * If all three arguments are blank, returns every item (same shape as {@link #readJobBoard()} {@code items}).
     */
    @SuppressWarnings("unused")
    public static JsonArray findNormalizedJobsByGovernance(String publishStatus, String auditStatus, String visibility)
            throws IOException {
        String fPub = trim(publishStatus);
        String fAud = trim(auditStatus);
        String fVis = trim(visibility);
        synchronized (FILE_LOCK) {
            JsonObject root = ensureJobBoardRoot();
            JsonArray out = new JsonArray();
            for (JsonElement item : root.getAsJsonArray("items")) {
                if (!item.isJsonObject()) {
                    continue;
                }
                JsonObject obj = normalizeJobItem(item.getAsJsonObject());
                if (!fPub.isEmpty() && !fPub.equalsIgnoreCase(getAsString(obj, "publishStatus"))) {
                    continue;
                }
                if (!fAud.isEmpty() && !fAud.equalsIgnoreCase(getAsString(obj, "auditStatus"))) {
                    continue;
                }
                if (!fVis.isEmpty() && !fVis.equalsIgnoreCase(getAsString(obj, "visibility"))) {
                    continue;
                }
                out.add(obj);
            }
            return out;
        }
    }

    /**
     * Whether some {@code items[]} row has this {@code jobId} on disk (raw field, ignore case). Blank id returns {@code false}.
     * Does not use {@link #normalizeJobItem(JsonObject)} so missing ids are not synthesized for this check.
     */
    @SuppressWarnings("unused")
    public static boolean existsJobId(String jobId) throws IOException {
        String needle = trim(jobId);
        if (needle.isEmpty()) {
            return false;
        }
        synchronized (FILE_LOCK) {
            JsonObject root = ensureJobBoardRoot();
            for (JsonElement item : root.getAsJsonArray("items")) {
                if (!item.isJsonObject()) {
                    continue;
                }
                if (needle.equalsIgnoreCase(getAsString(item.getAsJsonObject(), "jobId"))) {
                    return true;
                }
            }
            return false;
        }
    }

    public static JsonObject normalizeTeachingWeeks(JsonObject source) {
        JsonObject out = new JsonObject();
        JsonArray weeks = new JsonArray();
        if (source != null && source.has("weeks") && source.get("weeks").isJsonArray()) {
            List<Integer> values = new ArrayList<>();
            for (JsonElement week : source.getAsJsonArray("weeks")) {
                if (week == null || week.isJsonNull()) {
                    continue;
                }
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

    public static JsonArray normalizeAssessmentEvents(JsonElement source) {
        JsonArray out = new JsonArray();
        if (source == null || source.isJsonNull() || !source.isJsonArray()) {
            return out;
        }
        for (JsonElement element : source.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject in = element.getAsJsonObject();
            String name = trim(getAsString(in, "name"));
            if (name.isBlank()) {
                continue;
            }
            JsonObject row = new JsonObject();
            row.addProperty("name", name);
            JsonArray weeks = new JsonArray();
            List<Integer> weekValues = new ArrayList<>();
            if (in.has("weeks") && in.get("weeks").isJsonArray()) {
                for (JsonElement week : in.getAsJsonArray("weeks")) {
                    if (week == null || week.isJsonNull()) {
                        continue;
                    }
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

    public static JsonObject normalizeRequiredSkills(JsonElement source) {
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
                    if (!e.isJsonObject()) {
                        continue;
                    }
                    JsonObject obj = e.getAsJsonObject();
                    String name = trim(getAsString(obj, "name"));
                    if (name.isBlank()) {
                        continue;
                    }
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

    public static boolean hasRequiredSkills(JsonObject skills) {
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

    public static String normalizeCampus(String value) {
        String campus = trim(value);
        if ("Main".equalsIgnoreCase(campus)) {
            return "Main";
        }
        if ("Shahe".equalsIgnoreCase(campus)) {
            return "Shahe";
        }
        return "";
    }

    public static JsonObject normalizeJobItem(JsonObject source) {
        JsonObject item = source.deepCopy();
        String legacyMoName = trim(getAsString(item, "moName"));
        item.remove("moName");
        String courseName = firstNonBlank(getAsString(item, "courseName"), "Untitled TA Job");
        item.addProperty("courseName", courseName);
        String ownerMoName = firstNonBlank(trim(getAsString(item, "ownerMoName")),
                firstNonBlank(legacyMoName, "MO"));
        item.addProperty("ownerMoName", ownerMoName);
        item.addProperty("ownerMoId", trim(getAsString(item, "ownerMoId")));
        if (trim(getAsString(item, "semester")).isBlank()) {
            item.remove("semester");
        } else {
            item.addProperty("semester", trim(getAsString(item, "semester")));
        }
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
        if (trim(getAsString(item, "applicationDeadline")).isBlank()) {
            item.remove("applicationDeadline");
        } else {
            item.addProperty("applicationDeadline", trim(getAsString(item, "applicationDeadline")));
        }
        item.add("requiredSkills", normalizeRequiredSkills(item.get("requiredSkills")));
        item.addProperty("courseDescription", getAsString(item, "courseDescription"));
        String recruitmentBrief = trim(getAsString(item, "recruitmentBrief"));
        if (recruitmentBrief.isBlank()) {
            item.remove("recruitmentBrief");
        } else {
            item.addProperty("recruitmentBrief", recruitmentBrief);
        }
        item.remove("workload");
        item.remove("suggestion");
        item.remove("checklist");

        String now = Instant.now().toString();
        item.addProperty("createdAt", firstNonBlank(getAsString(item, "createdAt"), now));
        item.addProperty("updatedAt", firstNonBlank(getAsString(item, "updatedAt"), now));
        String sourceText = trim(getAsString(item, "source"));
        if (sourceText.isBlank()) {
            item.remove("source");
        } else {
            item.addProperty("source", sourceText);
        }
        return item;
    }

    private static JsonObject ensureJobBoardRoot() throws IOException {
        return ensureJobBoardStructuredFile(FILE);
    }

    private static JsonObject ensureJobBoardStructuredFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        if (!Files.exists(path)) {
            JsonObject root = new JsonObject();
            JsonObject meta = new JsonObject();
            meta.addProperty("schema", JOB_BOARD_SCHEMA);
            meta.addProperty("entity", JOB_BOARD_ENTITY);
            meta.addProperty("version", JOB_BOARD_VERSION);
            meta.addProperty("updatedAt", Instant.now().toString());
            root.add("meta", meta);
            root.add("items", new JsonArray());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
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
                updateMeta(root, JOB_BOARD_SCHEMA, JOB_BOARD_ENTITY, JOB_BOARD_VERSION);
                return root;
            }
        }
        JsonObject fallback = new JsonObject();
        fallback.add("meta", new JsonObject());
        fallback.add("items", new JsonArray());
        updateMeta(fallback, JOB_BOARD_SCHEMA, JOB_BOARD_ENTITY, JOB_BOARD_VERSION);
        return fallback;
    }

    private static void updateMeta(JsonObject root, String schema, String entity, String version) {
        JsonObject meta = root.getAsJsonObject("meta");
        meta.addProperty("schema", schema);
        meta.addProperty("entity", entity);
        meta.addProperty("version", version);
        meta.addProperty("updatedAt", Instant.now().toString());
    }

    private static void syncJobBoardFileEnvelope(JsonObject root, JsonArray items) {
        String now = Instant.now().toString();
        root.addProperty("schema", JOB_BOARD_SCHEMA);
        root.addProperty("version", JOB_BOARD_VERSION);
        root.addProperty("generatedAt", now);
        root.addProperty("count", items.size());
    }

    private static void writeJson(JsonObject root) throws IOException {
        Files.createDirectories(FILE.getParent());
        try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private static String getMetaUpdatedAt(JsonObject root) {
        if (!root.has("meta") || !root.get("meta").isJsonObject()) {
            return "";
        }
        JsonObject meta = root.getAsJsonObject("meta");
        return getAsString(meta, "updatedAt");
    }

    private static JsonArray parseCsvToArray(String csv) {
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

    private static JsonArray parseStringList(JsonElement element) {
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

    private static String buildCourseCode(String courseName) {
        String base = "MO-" + courseName.replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5]", "");
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        return (base.length() > 12 ? base.substring(0, 12) : base) + "-" + suffix;
    }
}
