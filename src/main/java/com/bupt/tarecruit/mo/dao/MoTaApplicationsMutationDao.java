package com.bupt.tarecruit.mo.dao;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.common.dao.RecruitmentCoursesDao;
import com.bupt.tarecruit.common.util.TaApplicationUniqueKeys;
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
import java.util.Locale;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

/**
 * MO 侧对 TA {@code applications.json} 的受控写入（如标记审核中），不修改 TA 模块 Java 代码。
 */
public final class MoTaApplicationsMutationDao {

    private static final Path PATH = DataMountPaths.taApplications();
    private static final String SCHEMA = "ta";
    private static final String ENTITY = "applications";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public synchronized JsonObject findApplicationItem(String applicationId) throws IOException {
        String id = trim(applicationId);
        if (id.isEmpty()) {
            return null;
        }
        JsonObject root = loadRoot();
        JsonArray items = root.getAsJsonArray("items");
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (id.equalsIgnoreCase(getAsString(item, "applicationId"))) {
                return item;
            }
        }
        return null;
    }

    public synchronized JsonObject findApplicationByTaAndCourse(String taId, String courseCode) throws IOException {
        return findApplicationByTaAndCourse(taId, courseCode, "");
    }

    /**
     * 在 TA+课程编号基础上，可选按 {@code courseSnapshot.jobId} 与岗位 {@code jobId} 对齐，避免同一 {@code courseCode} 多学期岗位时误选。
     */
    public synchronized JsonObject findApplicationByTaAndCourse(String taId, String courseCode, String jobId) throws IOException {
        String t = trim(taId);
        String c = trim(courseCode).toUpperCase(Locale.ROOT);
        String j = trim(jobId);
        if (t.isEmpty() || c.isEmpty()) {
            return null;
        }
        JsonObject root = loadRoot();
        JsonArray items = root.getAsJsonArray("items");
        String canonicalKey = TaApplicationUniqueKeys.canonical(t, c, j);
        String legacyKey = TaApplicationUniqueKeys.legacyKey(t, c);
        JsonObject latest = null;
        String latestUpdated = "";
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (!t.equalsIgnoreCase(getAsString(item, "taId"))) {
                continue;
            }
            if (!j.isEmpty()) {
                JsonObject snap = item.has("courseSnapshot") && item.get("courseSnapshot").isJsonObject()
                        ? item.getAsJsonObject("courseSnapshot")
                        : new JsonObject();
                String snapJob = trim(getAsString(snap, "jobId"));
                if (!snapJob.isEmpty() && !j.equalsIgnoreCase(snapJob)) {
                    continue;
                }
            }
            String cc = trim(getAsString(item, "courseCode")).toUpperCase(Locale.ROOT);
            String uk = trim(getAsString(item, "uniqueKey")).toUpperCase(Locale.ROOT);
            boolean keyOrCodeMatch = c.equals(cc)
                    || canonicalKey.equalsIgnoreCase(uk)
                    || legacyKey.equalsIgnoreCase(uk);
            if (!keyOrCodeMatch) {
                continue;
            }
            boolean active = !item.has("active") || item.get("active").isJsonNull() || item.get("active").getAsBoolean();
            if (!active) {
                continue;
            }
            String u = getAsString(item, "updatedAt");
            if (latest == null || u.compareTo(latestUpdated) > 0) {
                latest = item;
                latestUpdated = u;
            }
        }
        return latest;
    }

    /**
     * MO 已读后，将 TA 主申请记录置为审核中（与 {@code TaAccountDao} 中 UNDER_REVIEW 语义一致）。
     */
    public synchronized void markUnderReview(String applicationId) throws IOException {
        String id = trim(applicationId);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("applicationId 不能为空");
        }
        JsonObject root = loadRoot();
        JsonArray items = root.getAsJsonArray("items");
        JsonObject item = null;
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject candidate = element.getAsJsonObject();
            if (id.equalsIgnoreCase(getAsString(candidate, "applicationId"))) {
                item = candidate;
                break;
            }
        }
        if (item == null) {
            throw new IllegalArgumentException("未找到申请记录: " + applicationId);
        }
        String now = Instant.now().toString();
        item.addProperty("status", "UNDER_REVIEW");
        item.addProperty("statusLabel", "审核中");
        item.addProperty("statusTone", "warn");
        item.addProperty("summary", "课程负责人已查看你的申请，正在审核中。");
        item.addProperty("nextAction", "请等待课程负责人查看简历结果。");
        item.addProperty("updatedAt", now);
        touchMeta(root);
        writeRoot(root);
    }

    /**
     * 将岗位板中当前课程的规范化信息写回所有 {@code courseCode} 匹配的申请的 {@code courseSnapshot}（与 TA 投递时形状一致）。
     */
    public synchronized void refreshCourseSnapshotsForCourseCode(String courseCode) throws IOException {
        String c = trim(courseCode).toUpperCase(Locale.ROOT);
        if (c.isEmpty()) {
            return;
        }
        JsonObject job = RecruitmentCoursesDao.findNormalizedJobByCourseCode(c);
        if (job == null) {
            return;
        }
        String needleJobId = trim(getAsString(job, "jobId"));
        if (!Files.exists(PATH)) {
            return;
        }
        JsonObject snapshot = RecruitmentCoursesDao.taFacingCourseSnapshotFromNormalizedJob(job);
        JsonObject root = loadRoot();
        JsonArray items = root.getAsJsonArray("items");
        boolean changed = false;
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            String ic = trim(getAsString(item, "courseCode")).toUpperCase(Locale.ROOT);
            if (!c.equals(ic)) {
                continue;
            }
            if (!needleJobId.isEmpty()) {
                JsonObject prevSnap = item.has("courseSnapshot") && item.get("courseSnapshot").isJsonObject()
                        ? item.getAsJsonObject("courseSnapshot")
                        : new JsonObject();
                String prevJob = trim(getAsString(prevSnap, "jobId"));
                if (!prevJob.isEmpty() && !needleJobId.equalsIgnoreCase(prevJob)) {
                    continue;
                }
            }
            item.add("courseSnapshot", snapshot.deepCopy());
            changed = true;
        }
        if (changed) {
            touchMeta(root);
            writeRoot(root);
        }
    }

    private JsonObject loadRoot() throws IOException {
        Files.createDirectories(PATH.getParent());
        if (!Files.exists(PATH)) {
            throw new IOException("applications.json 不存在");
        }
        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IOException("applications.json 格式无效");
            }
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has("items") || !root.get("items").isJsonArray()) {
                throw new IOException("applications.json 缺少 items");
            }
            return root;
        }
    }

    private void touchMeta(JsonObject root) {
        if (!root.has("meta") || !root.get("meta").isJsonObject()) {
            JsonObject meta = new JsonObject();
            meta.addProperty("schema", SCHEMA);
            meta.addProperty("entity", ENTITY);
            meta.addProperty("version", "1.0");
            meta.addProperty("updatedAt", Instant.now().toString());
            root.add("meta", meta);
            return;
        }
        root.getAsJsonObject("meta").addProperty("updatedAt", Instant.now().toString());
    }

    private void writeRoot(JsonObject root) throws IOException {
        Files.createDirectories(PATH.getParent());
        try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }
}
