package com.bupt.tarecruit.mo.service;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 面向 MO 开发者的 TA 申请/简历读取服务。
 * <p>
 * 该类只读消费 TA 侧上传体系，不修改任何 TA 或 MO 存储。
 * 使用方式：MO 端开发者可直接实例化并调用方法，按课程、TA、申请 ID 等维度读取投递记录与简历信息。
 */
public class MoTaApplicationReadService {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Path APPLICATIONS_PATH = DataMountPaths.taApplications();
    private static final Path APPLICATION_EVENTS_PATH = DataMountPaths.taApplicationEvents();
    private static final Path TA_ACCOUNTS_PATH = DataMountPaths.taAccounts();
    private static final Path TA_PROFILES_PATH = DataMountPaths.taProfiles();
    private static final Path TA_RESUME_ROOT = DataMountPaths.taResumeRoot();

    public List<TaApplicationRecord> getAllApplications() throws IOException {
        return loadApplicationRecords();
    }

    public List<TaApplicationRecord> getActiveApplications() throws IOException {
        return loadApplicationRecords().stream()
                .filter(TaApplicationRecord::active)
                .sorted(APPLICATION_ORDER)
                .toList();
    }

    public List<TaApplicationRecord> getApplicationsByCourseCode(String courseCode) throws IOException {
        String normalizedCourseCode = normalizeUpper(courseCode);
        if (normalizedCourseCode.isBlank()) {
            return List.of();
        }
        return loadApplicationRecords().stream()
                .filter(record -> normalizedCourseCode.equalsIgnoreCase(normalizeUpper(record.courseCode())))
                .sorted(APPLICATION_ORDER)
                .toList();
    }

    /**
     * 在 {@link #getApplicationsByCourseCode(String)} 基础上，按已发布岗位的 {@code jobId} 收窄到同一岗位实例。
     * 若 {@code jobId} 为空，则行为与 {@code getApplicationsByCourseCode} 相同。
     * 若 TA 申请快照中缺少 {@code jobId}，仍视为可能属于当前岗位（兼容旧数据）。
     */
    public List<TaApplicationRecord> getApplicationsForCourseScopedToJob(String courseCode, String jobId) throws IOException {
        String normalizedCourseCode = normalizeUpper(courseCode);
        if (normalizedCourseCode.isBlank()) {
            return List.of();
        }
        String normalizedJobId = trimToEmpty(jobId);
        return loadApplicationRecords().stream()
                .filter(record -> normalizedCourseCode.equalsIgnoreCase(normalizeUpper(record.courseCode())))
                .filter(record -> {
                    if (normalizedJobId.isBlank()) {
                        return true;
                    }
                    String snapJobId = trimToEmpty(getAsString(record.courseSnapshot(), "jobId"));
                    if (snapJobId.isBlank()) {
                        return true;
                    }
                    return normalizedJobId.equalsIgnoreCase(snapJobId);
                })
                .sorted(APPLICATION_ORDER)
                .toList();
    }

    public CourseApplicationsView getCourseApplicationsView(String courseCode) throws IOException {
        List<TaApplicationRecord> records = getApplicationsByCourseCode(courseCode);
        if (records.isEmpty()) {
            return new CourseApplicationsView(normalizeUpper(courseCode), "", List.of(), 0, 0, List.of());
        }
        String resolvedCourseCode = records.get(0).courseCode();
        String resolvedCourseName = records.get(0).courseName();
        List<CourseResumeEntry> resumeEntries = new ArrayList<>();
        Set<String> taNames = new LinkedHashSet<>();
        int activeCount = 0;
        for (TaApplicationRecord record : records) {
            if (record.active()) {
                activeCount++;
            }
            taNames.add(record.taName());
            resumeEntries.add(new CourseResumeEntry(
                    record.applicationId(),
                    record.taId(),
                    record.taName(),
                    record.taStudentId(),
                    record.status(),
                    record.statusLabel(),
                    record.submittedAt(),
                    record.updatedAt(),
                    record.resumeMeta(),
                    record.resumeAbsolutePath()
            ));
        }
        return new CourseApplicationsView(
                resolvedCourseCode,
                resolvedCourseName,
                records,
                records.size(),
                activeCount,
                List.copyOf(resumeEntries)
        );
    }

    public List<TaApplicationRecord> getApplicationsByTaId(String taId) throws IOException {
        String normalizedTaId = normalizeUpper(taId);
        if (normalizedTaId.isBlank()) {
            return List.of();
        }
        return loadApplicationRecords().stream()
                .filter(record -> normalizedTaId.equalsIgnoreCase(normalizeUpper(record.taId())))
                .sorted(APPLICATION_ORDER)
                .toList();
    }

    public List<TaApplicationRecord> getApplicationsByTaName(String taName) throws IOException {
        String normalizedName = normalizeLower(taName);
        if (normalizedName.isBlank()) {
            return List.of();
        }
        return loadApplicationRecords().stream()
                .filter(record -> containsIgnoreCase(record.taName(), normalizedName)
                        || containsIgnoreCase(record.taRealName(), normalizedName))
                .sorted(APPLICATION_ORDER)
                .toList();
    }

    public TaApplicationsView getTaApplicationsViewByName(String taName) throws IOException {
        List<TaApplicationRecord> records = getApplicationsByTaName(taName);
        if (records.isEmpty()) {
            return new TaApplicationsView("", "", "", List.of(), 0, 0, List.of());
        }
        TaApplicationRecord first = records.get(0);
        List<TaResumeEntry> resumeEntries = new ArrayList<>();
        int activeCount = 0;
        for (TaApplicationRecord record : records) {
            if (record.active()) {
                activeCount++;
            }
            resumeEntries.add(new TaResumeEntry(
                    record.applicationId(),
                    record.courseCode(),
                    record.courseName(),
                    record.status(),
                    record.statusLabel(),
                    record.submittedAt(),
                    record.updatedAt(),
                    record.resumeMeta(),
                    record.resumeAbsolutePath()
            ));
        }
        return new TaApplicationsView(
                first.taId(),
                first.taName(),
                first.taRealName(),
                records,
                records.size(),
                activeCount,
                List.copyOf(resumeEntries)
        );
    }

    public TaApplicationRecord getApplicationById(String applicationId) throws IOException {
        String normalizedApplicationId = trimToEmpty(applicationId);
        if (normalizedApplicationId.isBlank()) {
            return null;
        }
        return loadApplicationRecords().stream()
                .filter(record -> normalizedApplicationId.equalsIgnoreCase(record.applicationId()))
                .findFirst()
                .orElse(null);
    }

    public List<TaApplicationEventRecord> getApplicationEvents(String applicationId) throws IOException {
        String normalizedApplicationId = trimToEmpty(applicationId);
        if (normalizedApplicationId.isBlank()) {
            return List.of();
        }
        JsonArray eventItems = readJsonItems(APPLICATION_EVENTS_PATH);
        List<TaApplicationEventRecord> records = new ArrayList<>();
        for (JsonElement element : eventItems) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (!normalizedApplicationId.equalsIgnoreCase(getAsString(item, "applicationId"))) {
                continue;
            }
            records.add(new TaApplicationEventRecord(
                    getAsString(item, "eventId"),
                    getAsString(item, "applicationId"),
                    getAsString(item, "taId"),
                    getAsString(item, "eventType"),
                    getAsString(item, "label"),
                    getAsString(item, "content"),
                    getAsString(item, "time"),
                    getAsBoolean(item, "done", true)
            ));
        }
        records.sort(Comparator.comparing(TaApplicationEventRecord::time, Comparator.nullsLast(String::compareTo)).reversed());
        return List.copyOf(records);
    }

    public ResumeBinaryPayload readResumeBinary(String applicationId) throws IOException {
        TaApplicationRecord record = getApplicationById(applicationId);
        if (record == null) {
            return null;
        }
        Path path = record.resumeAbsolutePath();
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            return null;
        }
        byte[] content = Files.readAllBytes(path);
        return new ResumeBinaryPayload(
                record.applicationId(),
                record.taId(),
                record.taName(),
                record.courseCode(),
                record.courseName(),
                path,
                record.resumeMeta().originalFileName(),
                record.resumeMeta().storedFileName(),
                record.resumeMeta().mimeType(),
                record.resumeMeta().sha256(),
                content
        );
    }

    public JsonObject exportApplicationsAsJson() throws IOException {
        JsonObject result = new JsonObject();
        result.addProperty("generatedAt", Instant.now().toString());
        result.addProperty("applicationCount", getAllApplications().size());
        result.add("items", GSON.toJsonTree(getAllApplications()));
        return result;
    }

    private List<TaApplicationRecord> loadApplicationRecords() throws IOException {
        JsonArray applicationItems = readJsonItems(APPLICATIONS_PATH);
        Map<String, JsonObject> accountMap = loadTaAccountMap();
        Map<String, JsonObject> profileMap = loadTaProfileMap();
        List<TaApplicationRecord> records = new ArrayList<>();

        for (JsonElement element : applicationItems) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            JsonObject courseSnapshot = getObject(item, "courseSnapshot");
            JsonObject taSnapshot = getObject(item, "taSnapshot");
            JsonObject resume = getObject(item, "resume");

            String taId = firstNonBlank(
                    getAsString(item, "taId"),
                    getAsString(taSnapshot, "taId")
            );
            JsonObject account = accountMap.getOrDefault(normalizeUpper(taId), new JsonObject());
            JsonObject profile = profileMap.getOrDefault(normalizeUpper(taId), new JsonObject());

            String courseCode = firstNonBlank(
                    getAsString(item, "courseCode"),
                    getAsString(courseSnapshot, "courseCode")
            );
            String taName = firstNonBlank(
                    getAsString(taSnapshot, "realName"),
                    getAsString(account, "name"),
                    getAsString(profile, "realName")
            );
            String taRealName = firstNonBlank(
                    getAsString(profile, "realName"),
                    getAsString(taSnapshot, "realName"),
                    getAsString(account, "name")
            );
            String taStudentId = firstNonBlank(
                    getAsString(taSnapshot, "studentId"),
                    getAsString(profile, "studentId"),
                    taId
            );
            String taEmail = firstNonBlank(
                    getAsString(taSnapshot, "contactEmail"),
                    getAsString(profile, "contactEmail"),
                    getAsString(account, "email")
            );
            String taPhone = firstNonBlank(
                    getAsString(taSnapshot, "phone"),
                    getAsString(account, "phone")
            );
            String courseName = firstNonBlank(
                    getAsString(courseSnapshot, "courseName"),
                    getAsString(item, "courseName")
            );
            String moName = firstNonBlank(
                    getAsString(courseSnapshot, "ownerMoName"),
                    getAsString(courseSnapshot, "moName")
            );

            ResumeMeta resumeMeta = new ResumeMeta(
                    getAsString(resume, "originalFileName"),
                    getAsString(resume, "storedFileName"),
                    getAsString(resume, "relativePath"),
                    getAsString(resume, "extension"),
                    getAsString(resume, "mimeType"),
                    getAsLong(resume, "size", 0L),
                    getAsString(resume, "sha256")
            );

            Path resumeAbsolutePath = resolveResumePath(resumeMeta.relativePath());

            records.add(new TaApplicationRecord(
                    getAsString(item, "applicationId"),
                    getAsString(item, "uniqueKey"),
                    taId,
                    taName,
                    taRealName,
                    taStudentId,
                    taEmail,
                    taPhone,
                    courseCode,
                    courseName,
                    moName,
                    getAsString(item, "status"),
                    getAsString(item, "statusLabel"),
                    getAsString(item, "statusTone"),
                    getAsString(item, "summary"),
                    getAsString(item, "nextAction"),
                    getAsBoolean(item, "active", true),
                    getAsString(item, "submittedAt"),
                    getAsString(item, "updatedAt"),
                    resumeMeta,
                    resumeAbsolutePath,
                    courseSnapshot.deepCopy(),
                    taSnapshot.deepCopy(),
                    item.deepCopy()
            ));
        }

        records.sort(APPLICATION_ORDER);
        return List.copyOf(records);
    }

    private Map<String, JsonObject> loadTaAccountMap() throws IOException {
        JsonArray items = readJsonItems(TA_ACCOUNTS_PATH);
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            String taId = getAsString(item, "id");
            if (!taId.isBlank()) {
                map.put(normalizeUpper(taId), item.deepCopy());
            }
        }
        return map;
    }

    private Map<String, JsonObject> loadTaProfileMap() throws IOException {
        JsonArray items = readJsonItems(TA_PROFILES_PATH);
        Map<String, JsonObject> map = new LinkedHashMap<>();
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            String taId = firstNonBlank(getAsString(item, "taId"), getAsString(item, "id"));
            if (!taId.isBlank()) {
                map.put(normalizeUpper(taId), item.deepCopy());
            }
        }
        return map;
    }

    private JsonArray readJsonItems(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new JsonArray();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return new JsonArray();
            }
            JsonObject root = rootElement.getAsJsonObject();
            if (!root.has("items") || !root.get("items").isJsonArray()) {
                return new JsonArray();
            }
            return root.getAsJsonArray("items");
        }
    }

    private Path resolveResumePath(String relativePath) {
        String normalized = trimToEmpty(relativePath);
        if (normalized.isBlank()) {
            return null;
        }
        Path taDir = DataMountPaths.taDir().toAbsolutePath().normalize();
        Path resolved = taDir.resolve(normalized).normalize();
        if (resolved.startsWith(taDir) && resolved.startsWith(TA_RESUME_ROOT.toAbsolutePath().normalize())) {
            return resolved;
        }
        return null;
    }

    private JsonObject getObject(JsonObject source, String memberName) {
        if (source == null || memberName == null || !source.has(memberName) || !source.get(memberName).isJsonObject()) {
            return new JsonObject();
        }
        return source.getAsJsonObject(memberName);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = trimToEmpty(value);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private static String getAsString(JsonObject source, String memberName) {
        if (source == null || memberName == null || !source.has(memberName) || source.get(memberName).isJsonNull()) {
            return "";
        }
        try {
            return trimToEmpty(source.get(memberName).getAsString());
        } catch (Exception ex) {
            return trimToEmpty(String.valueOf(source.get(memberName)));
        }
    }

    private static long getAsLong(JsonObject source, String memberName, long defaultValue) {
        if (source == null || memberName == null || !source.has(memberName) || source.get(memberName).isJsonNull()) {
            return defaultValue;
        }
        try {
            return source.get(memberName).getAsLong();
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static boolean getAsBoolean(JsonObject source, String memberName, boolean defaultValue) {
        if (source == null || memberName == null || !source.has(memberName) || source.get(memberName).isJsonNull()) {
            return defaultValue;
        }
        try {
            return source.get(memberName).getAsBoolean();
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeUpper(String value) {
        return trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private static String normalizeLower(String value) {
        return trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private static boolean containsIgnoreCase(String value, String normalizedNeedle) {
        return normalizeLower(value).contains(normalizedNeedle);
    }

    private static final Comparator<TaApplicationRecord> APPLICATION_ORDER = Comparator
            .comparing(TaApplicationRecord::updatedAt, Comparator.nullsLast(String::compareTo)).reversed()
            .thenComparing(TaApplicationRecord::applicationId, Comparator.nullsLast(String::compareTo));

    public record ResumeMeta(
            String originalFileName,
            String storedFileName,
            String relativePath,
            String extension,
            String mimeType,
            long size,
            String sha256
    ) {
    }

    public record TaApplicationRecord(
            String applicationId,
            String uniqueKey,
            String taId,
            String taName,
            String taRealName,
            String taStudentId,
            String taEmail,
            String taPhone,
            String courseCode,
            String courseName,
            String moName,
            String status,
            String statusLabel,
            String statusTone,
            String summary,
            String nextAction,
            boolean active,
            String submittedAt,
            String updatedAt,
            ResumeMeta resumeMeta,
            Path resumeAbsolutePath,
            JsonObject courseSnapshot,
            JsonObject taSnapshot,
            JsonObject rawRecord
    ) {
    }

    public record TaApplicationEventRecord(
            String eventId,
            String applicationId,
            String taId,
            String eventType,
            String label,
            String content,
            String time,
            boolean done
    ) {
    }

    public record CourseResumeEntry(
            String applicationId,
            String taId,
            String taName,
            String taStudentId,
            String status,
            String statusLabel,
            String submittedAt,
            String updatedAt,
            ResumeMeta resumeMeta,
            Path resumeAbsolutePath
    ) {
    }

    public record CourseApplicationsView(
            String courseCode,
            String courseName,
            List<TaApplicationRecord> applications,
            int totalCount,
            int activeCount,
            List<CourseResumeEntry> resumes
    ) {
    }

    public record TaResumeEntry(
            String applicationId,
            String courseCode,
            String courseName,
            String status,
            String statusLabel,
            String submittedAt,
            String updatedAt,
            ResumeMeta resumeMeta,
            Path resumeAbsolutePath
    ) {
    }

    public record TaApplicationsView(
            String taId,
            String taName,
            String taRealName,
            List<TaApplicationRecord> applications,
            int totalCount,
            int activeCount,
            List<TaResumeEntry> resumes
    ) {
    }

    public record ResumeBinaryPayload(
            String applicationId,
            String taId,
            String taName,
            String courseCode,
            String courseName,
            Path absolutePath,
            String originalFileName,
            String storedFileName,
            String mimeType,
            String sha256,
            byte[] content
    ) {
    }
}
