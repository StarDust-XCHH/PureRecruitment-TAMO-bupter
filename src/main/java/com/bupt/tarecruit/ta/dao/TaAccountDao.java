package com.bupt.tarecruit.ta.dao;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.common.dao.RecruitmentCoursesDao;
import com.bupt.tarecruit.common.util.AuthUtils;
import com.bupt.tarecruit.mo.dao.MoRecruitmentDao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

/**
 * 助教账户与申请数据访问对象。
 *
 * <p>在既有账号/资料能力基础上，扩展 TA 职位申请上传链路：</p>
 * <ul>
 *     <li>application 主数据维护（applications.json）</li>
 *     <li>application 事件维护（application-events.json）</li>
 *     <li>简历目录写入与文件元数据保存</li>
 *     <li>从新申请主数据派生 TA 状态页数据</li>
 * </ul>
 */
public class TaAccountDao {
    private static final String TA_SCHEMA = "ta";
    private static final String TA_ENTITY = "tas";
    private static final String PROFILE_SCHEMA = "ta";
    private static final String PROFILE_ENTITY = "profiles";
    private static final String SETTINGS_SCHEMA = "ta";
    private static final String SETTINGS_ENTITY = "settings";
    private static final String APPLICATION_SCHEMA = "ta";
    private static final String APPLICATION_ENTITY = "applications";
    private static final String APPLICATION_EVENT_ENTITY = "application-events";
    private static final DateTimeFormatter APPLICATION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private static final Path TA_DIR_PATH = DataMountPaths.taDir();
    private static final Path TA_DATA_PATH = TA_DIR_PATH.resolve("tas.json");
    private static final Path PROFILE_DATA_PATH = TA_DIR_PATH.resolve("profiles.json");
    private static final Path SETTINGS_DATA_PATH = TA_DIR_PATH.resolve("settings.json");
    private static final Path APPLICATION_DATA_PATH = DataMountPaths.taApplications();
    private static final Path APPLICATION_EVENT_PATH = DataMountPaths.taApplicationEvents();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public synchronized TaLoginResult login(String identifier, String password) throws IOException {
        String normalizedIdentifier = trim(identifier);
        String rawPassword = password == null ? "" : password;

        if (normalizedIdentifier.isEmpty() || rawPassword.isEmpty()) {
            return TaLoginResult.failure(400, "请输入账号和密码");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        Map<String, Object> matchedAccount = null;

        for (Map<String, Object> account : accounts) {
            if (matchesIdentifier(account, normalizedIdentifier)) {
                matchedAccount = account;
                break;
            }
        }

        if (matchedAccount == null) {
            return TaLoginResult.failure(404, "账号不存在");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> auth = ensureAuthMap(matchedAccount);
        String salt = asString(auth.get("passwordSalt"));
        String passwordHash = asString(auth.get("passwordHash"));
        String currentTime = AuthUtils.nowIso();
        String lastLoginAt = asString(auth.get("lastLoginAt"));
        boolean isFirstLogin = lastLoginAt.isBlank();

        if (!AuthUtils.hashPassword(rawPassword, salt).equals(passwordHash)) {
            int currentFailures = asInt(auth.get("failedAttempts"));
            auth.put("failedAttempts", currentFailures + 1);
            matchedAccount.put("updatedAt", currentTime);
            saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
            return TaLoginResult.failure(401, "账号或密码错误");
        }

        auth.put("failedAttempts", 0);
        auth.put("lastLoginAt", currentTime);
        matchedAccount.put("updatedAt", currentTime);
        saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taId", asString(matchedAccount.get("id")));
        data.put("name", asString(matchedAccount.get("name")));
        data.put("username", asString(matchedAccount.get("username")));
        data.put("email", asString(matchedAccount.get("email")));
        data.put("phone", asString(matchedAccount.get("phone")));
        data.put("department", asString(matchedAccount.get("department")));
        data.put("status", asString(matchedAccount.get("status")));
        data.put("loginAt", currentTime);
        data.put("isFirstLogin", isFirstLogin);
        return TaLoginResult.success(data);
    }

    public synchronized TaRegisterResult register(String taId, String name, String username, String email, String phone, String password) throws IOException {
        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        for (Map<String, Object> account : accounts) {
            if (asString(account.get("id")).equalsIgnoreCase(taId)) return TaRegisterResult.failure(409, "TA ID 已存在");
            if (asString(account.get("username")).equalsIgnoreCase(username)) return TaRegisterResult.failure(409, "用户名已被占用");
            if (asString(account.get("email")).equalsIgnoreCase(email)) return TaRegisterResult.failure(409, "邮箱已被注册");
            if (asString(account.get("phone")).equals(phone)) return TaRegisterResult.failure(409, "手机号已被注册");
        }

        String currentTime = AuthUtils.nowIso();
        String salt = AuthUtils.generateSalt();
        String passwordHash = AuthUtils.hashPassword(password, salt);

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("id", taId);
        account.put("name", name);
        account.put("username", username);
        account.put("email", email);
        account.put("phone", phone);
        account.put("department", "");
        account.put("status", "active");
        account.put("createdAt", currentTime);
        account.put("updatedAt", currentTime);

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("passwordHash", passwordHash);
        auth.put("passwordSalt", salt);
        auth.put("lastLoginAt", "");
        auth.put("failedAttempts", 0);
        account.put("auth", auth);
        accounts.add(account);

        profiles.add(createDefaultProfile(taId, name, email, currentTime));
        settings.add(createDefaultSettings(taId, currentTime));

        saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taId", taId);
        data.put("username", username);
        data.put("createdAt", currentTime);
        return TaRegisterResult.success(data);
    }

    public synchronized ProfileResult getProfileSettings(String taId) throws IOException {
        String normalizedTaId = trim(taId);
        if (normalizedTaId.isEmpty()) {
            return ProfileResult.failure(400, "缺少 TA 标识");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        Map<String, Object> account = findAccountByTaId(accounts, normalizedTaId);
        if (account == null) {
            return ProfileResult.failure(404, "未找到对应 TA 账号");
        }

        String now = AuthUtils.nowIso();
        Map<String, Object> profile = ensureProfileRecord(profiles, account, now);
        Map<String, Object> setting = ensureSettingsRecord(settings, normalizedTaId, now);

        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        return ProfileResult.success(mapProfileData(account, profile, setting));
    }

    public synchronized ProfileResult saveProfileSettings(ProfileUpdateInput input) throws IOException {
        if (input == null || trim(input.taId()).isEmpty()) {
            return ProfileResult.failure(400, "缺少 TA 标识");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        Map<String, Object> account = findAccountByTaId(accounts, trim(input.taId()));
        if (account == null) {
            return ProfileResult.failure(404, "未找到对应 TA 账号");
        }

        String now = AuthUtils.nowIso();
        Map<String, Object> profile = ensureProfileRecord(profiles, account, now);
        Map<String, Object> setting = ensureSettingsRecord(settings, trim(input.taId()), now);

        String normalizedRealName = trimToNull(input.realName());
        String normalizedIntent = trimToEmpty(input.applicationIntent());
        String normalizedEmail = trimToEmpty(input.contactEmail());
        String normalizedBio = trimToEmpty(input.bio());
        String normalizedAvatar = trimToEmpty(input.avatar());
        List<String> normalizedSkills = normalizeSkills(input.skills());

        profile.put("realName", normalizedRealName == null ? asString(account.get("name")) : normalizedRealName);
        profile.put("applicationIntent", normalizedIntent);
        profile.put("studentId", firstNonBlank(asString(profile.get("studentId")), asString(account.get("id"))));
        profile.put("contactEmail", normalizedEmail.isBlank() ? asString(account.get("email")) : normalizedEmail);
        profile.put("bio", normalizedBio);
        profile.put("avatar", normalizedAvatar);
        setting.put("avatar", normalizedAvatar);
        profile.put("skills", normalizedSkills);
        profile.put("lastUpdatedAt", now);

        if (normalizedRealName != null) {
            account.put("name", normalizedRealName);
        }
        if (!normalizedEmail.isBlank()) {
            account.put("email", normalizedEmail);
        }
        account.put("updatedAt", now);

        setting.put("profileSaved", true);
        setting.put("profileSavedAt", now);
        setting.put("lastProfileSyncStatus", "success");
        setting.put("lastProfileSyncMessage", "资料保存成功");
        setting.put("updatedAt", now);

        saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        return ProfileResult.success(mapProfileData(account, profile, setting));
    }

    public synchronized PasswordUpdateResult updatePassword(String taId, String currentPassword, String newPassword) throws IOException {
        String normalizedTaId = trim(taId);
        String normalizedCurrentPassword = currentPassword == null ? "" : currentPassword;
        String normalizedNewPassword = newPassword == null ? "" : newPassword;

        if (normalizedTaId.isEmpty()) {
            return PasswordUpdateResult.failure(400, "缺少 TA 标识");
        }
        if (normalizedCurrentPassword.isBlank() || normalizedNewPassword.isBlank()) {
            return PasswordUpdateResult.failure(400, "请输入当前密码和新密码");
        }
        if (normalizedNewPassword.trim().length() < 6) {
            return PasswordUpdateResult.failure(400, "新密码至少 6 位");
        }
        if (Objects.equals(normalizedCurrentPassword, normalizedNewPassword)) {
            return PasswordUpdateResult.failure(400, "新密码不能与当前密码相同");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        Map<String, Object> account = findAccountByTaId(accounts, normalizedTaId);
        if (account == null) {
            return PasswordUpdateResult.failure(404, "未找到对应 TA 账号");
        }

        Map<String, Object> auth = ensureAuthMap(account);
        String salt = asString(auth.get("passwordSalt"));
        String passwordHash = asString(auth.get("passwordHash"));
        if (!AuthUtils.hashPassword(normalizedCurrentPassword, salt).equals(passwordHash)) {
            int currentFailures = asInt(auth.get("failedAttempts"));
            auth.put("failedAttempts", currentFailures + 1);
            saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
            return PasswordUpdateResult.failure(401, "当前密码不正确");
        }

        String now = AuthUtils.nowIso();
        String nextSalt = AuthUtils.generateSalt();
        auth.put("passwordSalt", nextSalt);
        auth.put("passwordHash", AuthUtils.hashPassword(normalizedNewPassword, nextSalt));
        auth.put("failedAttempts", 0);
        if (asString(auth.get("lastLoginAt")).isBlank()) {
            auth.put("lastLoginAt", now);
        }
        account.put("updatedAt", now);
        saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
        return PasswordUpdateResult.success();
    }

    public synchronized ApplicationStatusResult getApplicationStatus(String taId) throws IOException {
        String normalizedTaId = trim(taId);
        if (normalizedTaId.isEmpty()) {
            return ApplicationStatusResult.failure(400, "缺少 TA 标识");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        Map<String, Object> account = findAccountByTaId(accounts, normalizedTaId);
        if (account == null) {
            return ApplicationStatusResult.failure(404, "未找到对应 TA 账号");
        }

        ensureStructuredJsonFile(APPLICATION_DATA_PATH, APPLICATION_SCHEMA, APPLICATION_ENTITY, buildDefaultApplicationsRoot());
        ensureStructuredJsonFile(APPLICATION_EVENT_PATH, APPLICATION_SCHEMA, APPLICATION_EVENT_ENTITY, buildDefaultApplicationEventsRoot());
        JsonObject applicationRoot = loadStructuredJson(APPLICATION_DATA_PATH, APPLICATION_SCHEMA, APPLICATION_ENTITY);
        JsonObject eventRoot = loadStructuredJson(APPLICATION_EVENT_PATH, APPLICATION_SCHEMA, APPLICATION_EVENT_ENTITY);
        JsonObject moStatusRoot = loadMoStatusRoot();
        JsonArray items = applicationRoot.getAsJsonArray("items");
        JsonArray eventItems = eventRoot.getAsJsonArray("items");

        JsonArray userItems = new JsonArray();
        JsonObject summary = new JsonObject();
        summary.addProperty("totalCount", 0);
        summary.addProperty("interviewCount", 0);
        summary.addProperty("needMaterialCount", 0);
        summary.addProperty("offerCount", 0);
        summary.addProperty("closedCount", 0);
        summary.addProperty("activeCount", 0);
        summary.addProperty("estimatedFeedbackTime", "3-5 工作日");
        summary.addProperty("latestStatusLabel", "暂无申请");

        int interviewCount = 0;
        int needMaterialCount = 0;
        int offerCount = 0;
        int closedCount = 0;
        String latestUpdatedAt = "";
        String latestStatusLabel = "暂无申请";

        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (!normalizedTaId.equalsIgnoreCase(getAsString(item, "taId"))) {
                continue;
            }
            JsonObject normalizedItem = normalizeApplicationRecord(mergeMoStatusIntoApplication(item, moStatusRoot), eventItems);
            userItems.add(normalizedItem);
            String status = getAsString(normalizedItem, "status");
            if (containsAny(status, "面试", "INTERVIEW")) {
                interviewCount++;
            }
            if (containsAny(status, "补充资料", "MATERIAL", "资料")) {
                needMaterialCount++;
            }
            if (containsAny(status, "录用", "ACCEPTED", "OFFER")) {
                offerCount++;
            }
            if (containsAny(status, "关闭", "淘汰", "未通过", "REJECTED", "WITHDRAWN")) {
                closedCount++;
            }
            String updatedAt = getAsString(normalizedItem, "updatedAt");
            if (latestUpdatedAt.isEmpty() || updatedAt.compareTo(latestUpdatedAt) > 0) {
                latestUpdatedAt = updatedAt;
                latestStatusLabel = status.isBlank() ? "处理中" : status;
            }
        }

        sortByUpdatedAtDesc(userItems);

        summary.addProperty("totalCount", userItems.size());
        summary.addProperty("interviewCount", interviewCount);
        summary.addProperty("needMaterialCount", needMaterialCount);
        summary.addProperty("offerCount", offerCount);
        summary.addProperty("closedCount", closedCount);
        summary.addProperty("activeCount", Math.max(userItems.size() - closedCount, 0));
        summary.addProperty("latestStatusLabel", latestStatusLabel);

        JsonArray messages = new JsonArray();
        JsonObject uploadNotice = new JsonObject();
        uploadNotice.addProperty("title", "投递提醒");
        uploadNotice.addProperty("content", userItems.size() > 0 ? "已记录你的课程申请与简历文件，可在本页持续跟踪进度。" : "当前还没有申请记录，可前往职位大厅上传简历并投递。");
        messages.add(uploadNotice);

        JsonObject duplicateNotice = new JsonObject();
        duplicateNotice.addProperty("title", "唯一申请规则");
        duplicateNotice.addProperty("content", "同一 TA 对同一课程仅允许保留一条有效申请，请确认简历版本后再提交。");
        messages.add(duplicateNotice);

        JsonObject summaryNotice = new JsonObject();
        summaryNotice.addProperty("title", "状态汇总");
        summaryNotice.addProperty("content", userItems.size() > 0 ? "当前共有 " + userItems.size() + " 条申请记录，最新进度为“" + latestStatusLabel + "”。" : "当前暂无申请状态，系统已准备好新的上传体系。"
        );
        messages.add(summaryNotice);

        return ApplicationStatusResult.success(userItems, summary, messages);
    }

    public synchronized ApplicationSubmitResult createApplication(ApplicationCreateInput input) throws IOException {
        if (input == null || trim(input.taId()).isEmpty()) {
            return ApplicationSubmitResult.failure(400, "缺少 TA 标识");
        }
        if (trim(input.courseCode()).isEmpty()) {
            return ApplicationSubmitResult.failure(400, "缺少课程编号");
        }
        if (trim(input.originalFileName()).isEmpty()) {
            return ApplicationSubmitResult.failure(400, "缺少简历文件名");
        }
        if (input.resumeStream() == null) {
            return ApplicationSubmitResult.failure(400, "缺少简历文件内容");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        Map<String, Object> account = findAccountByTaId(accounts, trim(input.taId()));
        if (account == null) {
            return ApplicationSubmitResult.failure(404, "未找到对应 TA 账号");
        }
        Map<String, Object> profile = findProfileByTaId(profiles, trim(input.taId()));
        if (profile == null) {
            profile = createDefaultProfile(trim(input.taId()), asString(account.get("name")), asString(account.get("email")), AuthUtils.nowIso());
            profiles.add(profile);
            saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        }

        JsonObject course = RecruitmentCoursesDao.findNormalizedJobByCourseCode(trim(input.courseCode()));
        if (course == null) {
            return ApplicationSubmitResult.failure(404, "未找到对应课程或课程尚未开放申请");
        }

        ensureStructuredJsonFile(APPLICATION_DATA_PATH, APPLICATION_SCHEMA, APPLICATION_ENTITY, buildDefaultApplicationsRoot());
        ensureStructuredJsonFile(APPLICATION_EVENT_PATH, APPLICATION_SCHEMA, APPLICATION_EVENT_ENTITY, buildDefaultApplicationEventsRoot());
        JsonObject applicationRoot = loadStructuredJson(APPLICATION_DATA_PATH, APPLICATION_SCHEMA, APPLICATION_ENTITY);
        JsonArray applicationItems = applicationRoot.getAsJsonArray("items");

        String taId = trim(input.taId());
        String courseCode = trim(input.courseCode()).toUpperCase(Locale.ROOT);
        String uniqueKey = buildUniqueKey(taId, courseCode);
        for (JsonElement element : applicationItems) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject existing = element.getAsJsonObject();
            if (uniqueKey.equalsIgnoreCase(getAsString(existing, "uniqueKey")) && getAsBoolean(existing, "active", true)) {
                return ApplicationSubmitResult.failure(409, "你已申请过该课程，请勿重复投递");
            }
        }

        String applicationId = buildApplicationId(taId, courseCode);
        String submittedAt = Instant.now().toString();
        String extension = normalizeResumeExtension(input.originalFileName(), input.contentType());
        Path courseDir = DataMountPaths.taResumeCourseDir(taId, courseCode);
        Files.createDirectories(courseDir);
        String storedFileName = applicationId + "_resume" + extension;
        Path storedFile = courseDir.resolve(storedFileName).normalize();
        Path resumeRoot = DataMountPaths.taResumeRoot().toAbsolutePath().normalize();
        if (!storedFile.toAbsolutePath().normalize().startsWith(resumeRoot)) {
            return ApplicationSubmitResult.failure(500, "简历存储路径非法");
        }

        long size;
        String sha256;
        try (InputStream in = input.resumeStream()) {
            Files.copy(in, storedFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            deleteFileQuietly(storedFile);
            throw ex;
        }

        try {
            size = Files.size(storedFile);
            sha256 = calculateSha256(storedFile);
        } catch (Exception ex) {
            deleteFileQuietly(storedFile);
            throw ex;
        }

        try {
            JsonObject applicationRecord = buildApplicationRecord(applicationId, uniqueKey, taId, courseCode, submittedAt, course, account, profile,
                    input.originalFileName(), storedFileName, DataMountPaths.taDir().relativize(storedFile).toString().replace('\\', '/'),
                    trimToEmpty(input.contentType()), extension, size, sha256);
            applicationItems.add(applicationRecord);
            applicationRoot.getAsJsonObject("meta").addProperty("updatedAt", submittedAt);
            writeStructuredJson(APPLICATION_DATA_PATH, applicationRoot);

            JsonObject eventRoot = loadStructuredJson(APPLICATION_EVENT_PATH, APPLICATION_SCHEMA, APPLICATION_EVENT_ENTITY);
            JsonArray eventItems = eventRoot.getAsJsonArray("items");
            eventItems.add(buildApplicationEvent(applicationId, taId, "SUBMITTED", "已投递申请", "系统已记录你的岗位申请与简历文件。", submittedAt, true));
            eventRoot.getAsJsonObject("meta").addProperty("updatedAt", submittedAt);
            writeStructuredJson(APPLICATION_EVENT_PATH, eventRoot);

            try {
                new MoRecruitmentDao().syncPublishedJobApplicationStatsForCourse(courseCode);
            } catch (Exception syncEx) {
                System.err.println("[TA-APPLY] recruitment stats sync skipped: " + syncEx.getMessage());
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("applicationId", applicationId);
            payload.addProperty("courseCode", courseCode);
            payload.addProperty("courseName", getAsString(course, "courseName"));
            payload.addProperty("status", "SUBMITTED");
            payload.addProperty("statusLabel", "已投递");
            payload.addProperty("submittedAt", submittedAt);
            payload.addProperty("resumeFileName", storedFileName);
            payload.addProperty("resumeRelativePath", DataMountPaths.taDir().relativize(storedFile).toString().replace('\\', '/'));
            return ApplicationSubmitResult.success(payload);
        } catch (Exception ex) {
            deleteFileQuietly(storedFile);
            throw ex;
        }
    }

    public JsonObject getPendingJobBoardData() throws IOException {
        return RecruitmentCoursesDao.readJobBoard();
    }

    private JsonObject buildApplicationRecord(String applicationId,
                                              String uniqueKey,
                                              String taId,
                                              String courseCode,
                                              String submittedAt,
                                              JsonObject course,
                                              Map<String, Object> account,
                                              Map<String, Object> profile,
                                              String originalFileName,
                                              String storedFileName,
                                              String relativePath,
                                              String contentType,
                                              String extension,
                                              long size,
                                              String sha256) {
        JsonObject item = new JsonObject();
        item.addProperty("applicationId", applicationId);
        item.addProperty("uniqueKey", uniqueKey);
        item.addProperty("taId", taId);
        item.addProperty("courseCode", courseCode);
        item.add("courseSnapshot", buildCourseSnapshot(course));
        item.add("taSnapshot", buildTaSnapshot(account, profile));

        JsonObject resume = new JsonObject();
        resume.addProperty("originalFileName", originalFileName);
        resume.addProperty("storedFileName", storedFileName);
        resume.addProperty("relativePath", relativePath);
        resume.addProperty("extension", extension.replaceFirst("^\\.", ""));
        resume.addProperty("mimeType", contentType);
        resume.addProperty("size", size);
        resume.addProperty("sha256", sha256);
        item.add("resume", resume);

        item.addProperty("status", "SUBMITTED");
        item.addProperty("statusLabel", "已投递");
        item.addProperty("statusTone", "warn");
        item.addProperty("summary", "简历已提交，等待课程负责人查看与初步筛选。");
        item.addProperty("nextAction", "请保持联系方式畅通，等待后续通知。");
        item.addProperty("active", true);
        item.addProperty("submittedAt", submittedAt);
        item.addProperty("updatedAt", submittedAt);
        return item;
    }

    private JsonObject buildCourseSnapshot(JsonObject course) {
        return RecruitmentCoursesDao.taFacingCourseSnapshotFromNormalizedJob(course);
    }

    private JsonObject buildTaSnapshot(Map<String, Object> account, Map<String, Object> profile) {
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("taId", asString(account.get("id")));
        snapshot.addProperty("realName", firstNonBlank(asString(profile.get("realName")), asString(account.get("name"))));
        snapshot.addProperty("studentId", firstNonBlank(asString(profile.get("studentId")), asString(account.get("id"))));
        snapshot.addProperty("contactEmail", firstNonBlank(asString(profile.get("contactEmail")), asString(account.get("email"))));
        snapshot.addProperty("phone", asString(account.get("phone")));
        snapshot.addProperty("department", asString(account.get("department")));
        snapshot.addProperty("applicationIntent", asString(profile.get("applicationIntent")));
        JsonArray skills = new JsonArray();
        for (String skill : asStringList(profile.get("skills"))) {
            skills.add(skill);
        }
        snapshot.add("skills", skills);
        snapshot.addProperty("bio", asString(profile.get("bio")));
        snapshot.addProperty("avatar", asString(profile.get("avatar")));
        return snapshot;
    }

    private JsonObject buildApplicationEvent(String applicationId, String taId, String eventType, String label, String content, String time, boolean done) {
        JsonObject event = new JsonObject();
        event.addProperty("eventId", applicationId + "-EVT-" + APPLICATION_TIME_FORMATTER.format(Instant.now()));
        event.addProperty("applicationId", applicationId);
        event.addProperty("taId", taId);
        event.addProperty("eventType", eventType);
        event.addProperty("label", label);
        event.addProperty("content", content);
        event.addProperty("time", time);
        event.addProperty("done", done);
        return event;
    }

    private JsonObject normalizeApplicationRecord(JsonObject item, JsonArray eventItems) {
        JsonObject normalized = new JsonObject();
        String applicationId = getAsString(item, "applicationId");
        JsonObject courseSnapshot = item.has("courseSnapshot") && item.get("courseSnapshot").isJsonObject()
                ? item.getAsJsonObject("courseSnapshot") : new JsonObject();

        String statusCode = firstNonBlank(getAsString(item, "status"), "SUBMITTED");
        String statusLabel = firstNonBlank(getAsString(item, "statusLabel"), mapStatusLabel(statusCode));
        String summary = firstNonBlank(getAsString(item, "summary"), "申请已提交，等待查看。");
        String nextAction = firstNonBlank(getAsString(item, "nextAction"), defaultNextAction(statusCode));
        String updatedAt = firstNonBlank(getAsString(item, "updatedAt"), getAsString(item, "submittedAt"));

        normalized.addProperty("applicationId", applicationId);
        normalized.addProperty("taId", getAsString(item, "taId"));
        normalized.addProperty("courseCode", firstNonBlank(getAsString(item, "courseCode"), getAsString(courseSnapshot, "courseCode")));
        normalized.addProperty("courseName", firstNonBlank(getAsString(courseSnapshot, "courseName"), "未命名岗位"));
        normalized.addProperty("status", statusLabel);
        normalized.addProperty("statusCode", statusCode);
        normalized.addProperty("statusTone", firstNonBlank(getAsString(item, "statusTone"), mapStatusTone(statusCode)));
        normalized.addProperty("summary", summary);
        normalized.addProperty("nextAction", nextAction);
        normalized.addProperty("updatedAt", updatedAt);
        normalized.addProperty("category", firstNonBlank(getAsString(courseSnapshot, "semester"), "课程申请"));
        normalized.addProperty("matchLevel", "--");
        normalized.add("timeline", collectTimeline(applicationId, eventItems, updatedAt, summary));
        normalized.add("details", buildApplicationDetails(item, courseSnapshot));
        normalized.add("notifications", buildApplicationNotifications(item));
        normalized.add("tags", buildApplicationTags(statusLabel));
        normalized.addProperty("moComment", summary);
        normalized.addProperty("nextStep", nextAction);
        return normalized;
    }

    private JsonArray collectTimeline(String applicationId, JsonArray eventItems, String fallbackTime, String fallbackSummary) {
        JsonArray timeline = new JsonArray();
        for (JsonElement element : eventItems) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (!applicationId.equalsIgnoreCase(getAsString(item, "applicationId"))) {
                continue;
            }
            JsonObject step = new JsonObject();
            step.addProperty("label", firstNonBlank(getAsString(item, "label"), "状态节点"));
            step.addProperty("content", firstNonBlank(getAsString(item, "content"), fallbackSummary));
            step.addProperty("time", firstNonBlank(getAsString(item, "time"), fallbackTime));
            step.addProperty("done", getAsBoolean(item, "done", true));
            timeline.add(step);
        }
        if (timeline.isEmpty()) {
            JsonObject step = new JsonObject();
            step.addProperty("label", "已投递申请");
            step.addProperty("content", fallbackSummary);
            step.addProperty("time", fallbackTime);
            step.addProperty("done", true);
            timeline.add(step);
        }
        return timeline;
    }

    private JsonArray buildApplicationDetails(JsonObject item, JsonObject courseSnapshot) {
        JsonArray details = new JsonArray();
        details.add(detailRow("岗位编号", firstNonBlank(getAsString(courseSnapshot, "courseCode"), getAsString(item, "courseCode"))));
        details.add(detailRow("课程名称", firstNonBlank(getAsString(courseSnapshot, "courseName"), "--")));
        details.add(detailRow("授课教师", firstNonBlank(getAsString(courseSnapshot, "ownerMoName"), "待分配")));
        details.add(detailRow("校区", firstNonBlank(getAsString(courseSnapshot, "campus"), "待确认")));
        JsonObject resume = item.has("resume") && item.get("resume").isJsonObject() ? item.getAsJsonObject("resume") : new JsonObject();
        details.add(detailRow("简历文件", firstNonBlank(getAsString(resume, "originalFileName"), getAsString(resume, "storedFileName"))));
        details.add(detailRow("提交时间", firstNonBlank(getAsString(item, "submittedAt"), getAsString(item, "updatedAt"))));
        return details;
    }

    private JsonObject detailRow(String label, String value) {
        JsonObject row = new JsonObject();
        row.addProperty("label", label);
        row.addProperty("value", value);
        return row;
    }

    private JsonArray buildApplicationNotifications(JsonObject item) {
        JsonArray notifications = new JsonArray();
        JsonObject notice = new JsonObject();
        notice.addProperty("title", "申请已记录");
        notice.addProperty("content", firstNonBlank(getAsString(item, "summary"), "你的简历已进入岗位申请记录。"));
        notice.addProperty("tone", firstNonBlank(getAsString(item, "statusTone"), "info"));
        notice.addProperty("createdAt", firstNonBlank(getAsString(item, "updatedAt"), getAsString(item, "submittedAt")));
        notifications.add(notice);
        return notifications;
    }

    private JsonArray buildApplicationTags(String statusLabel) {
        JsonArray tags = new JsonArray();
        tags.add("已投递");
        if (!statusLabel.isBlank() && !"已投递".equals(statusLabel)) {
            tags.add(statusLabel);
        }
        return tags;
    }

    private String mapStatusLabel(String statusCode) {
        String code = trimToEmpty(statusCode).toUpperCase(Locale.ROOT);
        return switch (code) {
            case "UNDER_REVIEW" -> "审核中";
            case "NEED_MORE_INFO" -> "待补充资料";
            case "INTERVIEW" -> "面试中";
            case "ACCEPTED" -> "已录用";
            case "REJECTED" -> "未通过";
            case "WITHDRAWN" -> "已撤回";
            default -> "已投递";
        };
    }

    private String mapStatusTone(String statusCode) {
        String code = trimToEmpty(statusCode).toUpperCase(Locale.ROOT);
        return switch (code) {
            case "ACCEPTED" -> "success";
            case "REJECTED", "WITHDRAWN" -> "error";
            case "UNDER_REVIEW", "NEED_MORE_INFO", "INTERVIEW" -> "warn";
            default -> "warn";
        };
    }

    private JsonObject loadMoStatusRoot() throws IOException {
        Path statusPath = DataMountPaths.taApplicationStatus();
        if (!Files.exists(statusPath)) {
            return null;
        }
        JsonElement parsed;
        try (Reader reader = Files.newBufferedReader(statusPath, StandardCharsets.UTF_8)) {
            parsed = JsonParser.parseReader(reader);
        }
        if (parsed == null || !parsed.isJsonObject()) {
            return null;
        }
        JsonObject root = parsed.getAsJsonObject();
        if (!root.has("items") || !root.get("items").isJsonArray()) {
            return null;
        }
        return root;
    }

    private JsonObject mergeMoStatusIntoApplication(JsonObject applicationItem, JsonObject moStatusRoot) {
        if (applicationItem == null || moStatusRoot == null) {
            return applicationItem;
        }
        JsonObject latestMoStatus = findMoStatusForApplication(applicationItem, moStatusRoot.getAsJsonArray("items"));
        if (latestMoStatus == null) {
            return applicationItem;
        }

        JsonObject merged = applicationItem.deepCopy();
        merged.addProperty("status", mapMoStatusToTaStatusCode(getAsString(latestMoStatus, "status")));
        merged.addProperty("statusLabel", firstNonBlank(getAsString(latestMoStatus, "status"), getAsString(merged, "statusLabel")));
        merged.addProperty("statusTone", firstNonBlank(getAsString(latestMoStatus, "statusTone"), getAsString(merged, "statusTone")));
        merged.addProperty("summary", firstNonBlank(getAsString(latestMoStatus, "summary"), getAsString(merged, "summary")));
        merged.addProperty("nextAction", firstNonBlank(getAsString(latestMoStatus, "nextAction"), getAsString(merged, "nextAction")));
        merged.addProperty("updatedAt", firstNonBlank(getAsString(latestMoStatus, "updatedAt"), getAsString(merged, "updatedAt")));
        if (latestMoStatus.has("matchLevel") && !latestMoStatus.get("matchLevel").isJsonNull()) {
            merged.add("matchLevel", latestMoStatus.get("matchLevel").deepCopy());
        }
        return merged;
    }

    private JsonObject findMoStatusForApplication(JsonObject applicationItem, JsonArray moStatusItems) {
        if (applicationItem == null || moStatusItems == null) {
            return null;
        }
        String taId = getAsString(applicationItem, "taId");
        String courseCode = getAsString(applicationItem, "courseCode");
        JsonObject courseSnapshot = applicationItem.has("courseSnapshot") && applicationItem.get("courseSnapshot").isJsonObject()
                ? applicationItem.getAsJsonObject("courseSnapshot") : new JsonObject();
        String courseName = getAsString(courseSnapshot, "courseName");
        JsonObject latest = null;
        for (JsonElement element : moStatusItems) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (!taId.equalsIgnoreCase(getAsString(item, "taId"))) {
                continue;
            }
            String jobSlug = trimToEmpty(getAsString(item, "jobSlug")).toLowerCase(Locale.ROOT);
            String courseCodeSlug = trimToEmpty(courseCode).toLowerCase(Locale.ROOT);
            boolean matched = (!courseCodeSlug.isBlank() && jobSlug.equals(courseCodeSlug))
                    || (!courseName.isBlank() && courseName.equalsIgnoreCase(getAsString(item, "courseName")));
            if (!matched) {
                continue;
            }
            if (latest == null || getAsString(item, "updatedAt").compareTo(getAsString(latest, "updatedAt")) > 0) {
                latest = item;
            }
        }
        return latest;
    }

    private String mapMoStatusToTaStatusCode(String moStatusLabel) {
        String label = trimToEmpty(moStatusLabel);
        if (containsAny(label, "录用", "通过")) {
            return "ACCEPTED";
        }
        if (containsAny(label, "拒绝", "未录用", "未通过")) {
            return "REJECTED";
        }
        return label.isBlank() ? "SUBMITTED" : label;
    }

    private String defaultNextAction(String statusCode) {
        String code = trimToEmpty(statusCode).toUpperCase(Locale.ROOT);
        return switch (code) {
            case "UNDER_REVIEW" -> "请等待课程负责人查看简历结果。";
            case "NEED_MORE_INFO" -> "请按通知补充相关材料后重新等待审核。";
            case "INTERVIEW" -> "请留意面试通知并做好课程相关准备。";
            case "ACCEPTED" -> "请留意后续签约和排班通知。";
            case "REJECTED" -> "可继续关注其他开放课程并重新申请。";
            case "WITHDRAWN" -> "该申请已结束，可投递其他课程。";
            default -> "请保持联系方式畅通，等待后续通知。";
        };
    }

    private void sortByUpdatedAtDesc(JsonArray items) {
        if (items == null || items.size() <= 1) {
            return;
        }
        List<JsonObject> sorted = new ArrayList<>();
        for (JsonElement element : items) {
            if (element != null && element.isJsonObject()) {
                sorted.add(element.getAsJsonObject());
            }
        }
        sorted.sort((left, right) -> getAsString(right, "updatedAt").compareTo(getAsString(left, "updatedAt")));
        items.asList().clear();
        for (JsonObject item : sorted) {
            items.add(item);
        }
    }

    private String buildApplicationId(String taId, String courseCode) {
        return "APP-" + taId + "-" + courseCode + "-" + APPLICATION_TIME_FORMATTER.format(Instant.now());
    }

    private String buildUniqueKey(String taId, String courseCode) {
        return taId.toUpperCase(Locale.ROOT) + "::" + courseCode.toUpperCase(Locale.ROOT);
    }

    private String normalizeResumeExtension(String fileName, String contentType) {
        String lower = trimToEmpty(fileName).toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return ".pdf";
        }
        if (lower.endsWith(".docx")) {
            return ".docx";
        }
        if (lower.endsWith(".doc")) {
            return ".doc";
        }
        String mime = trimToEmpty(contentType).toLowerCase(Locale.ROOT);
        if (mime.contains("pdf")) {
            return ".pdf";
        }
        if (mime.contains("wordprocessingml")) {
            return ".docx";
        }
        if (mime.contains("msword")) {
            return ".doc";
        }
        return ".pdf";
    }

    private String calculateSha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 不可用", ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private void deleteFileQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignore) {
            // ignore cleanup failure
        }
    }

    private List<Map<String, Object>> loadRecords(Path path, String schema, String entity) throws IOException {
        ensureDataFile(path, schema, entity);
        String content = Files.readString(path, StandardCharsets.UTF_8).trim();
        if (content.isEmpty() || content.equals("[]")) return new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (root.has("items")) {
                JsonArray itemsArray = root.getAsJsonArray("items");
                List<Map<String, Object>> result = GSON.fromJson(itemsArray, new TypeToken<List<Map<String, Object>>>() {}.getType());
                return result == null ? new ArrayList<>() : result;
            }
        } catch (Exception e) {
            System.err.println("[TA-DATA] JSON 解析失败: " + path);
        }
        return new ArrayList<>();
    }

    private void saveRecords(Path path, String schema, String entity, List<Map<String, Object>> records) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("schema", schema);
        meta.put("entity", entity);
        meta.put("version", "1.0");
        meta.put("updatedAt", Instant.now().toString());

        root.put("meta", meta);
        root.put("items", records);
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private void ensureDataFile(Path path, String schema, String entity) throws IOException {
        if (Files.notExists(path.getParent())) Files.createDirectories(path.getParent());
        if (Files.notExists(path)) saveRecords(path, schema, entity, new ArrayList<>());
    }

    private JsonObject loadStructuredJson(Path path, String schema, String entity) throws IOException {
        ensureStructuredJsonFile(path, schema, entity, null);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                ensureMeta(object, schema, entity);
                if (!object.has("items") || !object.get("items").isJsonArray()) {
                    object.add("items", new JsonArray());
                }
                return object;
            }
        }
        JsonObject fallback = new JsonObject();
        JsonObject meta = new JsonObject();
        meta.addProperty("schema", schema);
        meta.addProperty("entity", entity);
        meta.addProperty("version", "1.0");
        meta.addProperty("updatedAt", "");
        fallback.add("meta", meta);
        fallback.add("items", new JsonArray());
        return fallback;
    }

    private void ensureStructuredJsonFile(Path path, String schema, String entity, JsonObject defaultRoot) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            return;
        }
        JsonObject root = defaultRoot == null ? new JsonObject() : defaultRoot.deepCopy();
        if (!root.has("meta") || !root.get("meta").isJsonObject()) {
            JsonObject meta = new JsonObject();
            meta.addProperty("schema", schema);
            meta.addProperty("entity", entity);
            meta.addProperty("version", "1.0");
            meta.addProperty("updatedAt", Instant.now().toString());
            root.add("meta", meta);
        }
        if (!root.has("items") || !root.get("items").isJsonArray()) {
            root.add("items", new JsonArray());
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private void writeStructuredJson(Path path, JsonObject root) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private JsonObject buildDefaultApplicationsRoot() {
        JsonObject root = new JsonObject();
        JsonObject meta = new JsonObject();
        meta.addProperty("schema", APPLICATION_SCHEMA);
        meta.addProperty("entity", APPLICATION_ENTITY);
        meta.addProperty("version", "1.0");
        meta.addProperty("updatedAt", Instant.now().toString());
        root.add("meta", meta);
        root.add("items", new JsonArray());
        return root;
    }

    private JsonObject buildDefaultApplicationEventsRoot() {
        JsonObject root = new JsonObject();
        JsonObject meta = new JsonObject();
        meta.addProperty("schema", APPLICATION_SCHEMA);
        meta.addProperty("entity", APPLICATION_EVENT_ENTITY);
        meta.addProperty("version", "1.0");
        meta.addProperty("updatedAt", Instant.now().toString());
        root.add("meta", meta);
        root.add("items", new JsonArray());
        return root;
    }

    private void ensureMeta(JsonObject root, String schema, String entity) {
        JsonObject meta = root.has("meta") && root.get("meta").isJsonObject() ? root.getAsJsonObject("meta") : new JsonObject();
        meta.addProperty("schema", firstNonBlank(getAsString(meta, "schema"), schema));
        meta.addProperty("entity", firstNonBlank(getAsString(meta, "entity"), entity));
        meta.addProperty("version", firstNonBlank(getAsString(meta, "version"), "1.0"));
        if (!meta.has("updatedAt") || meta.get("updatedAt").isJsonNull()) {
            meta.addProperty("updatedAt", Instant.now().toString());
        }
        root.add("meta", meta);
    }

    private Map<String, Object> createDefaultProfile(String taId, String name, String email, String currentTime) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", "PROFILE-" + taId);
        profile.put("taId", taId);
        profile.put("avatar", "");
        profile.put("realName", name == null ? "" : name);
        profile.put("applicationIntent", "");
        profile.put("studentId", taId);
        profile.put("contactEmail", email == null ? "" : email);
        profile.put("bio", "");
        profile.put("skills", new ArrayList<>());
        profile.put("availabilityHoursPerWeek", 0);
        profile.put("semester", "");
        profile.put("title", "TA");
        profile.put("lastUpdatedAt", currentTime);
        return profile;
    }

    private Map<String, Object> createDefaultSettings(String taId, String currentTime) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("id", "SETTING-" + taId);
        settings.put("taId", taId);
        settings.put("profileSaved", false);
        settings.put("profileSavedAt", "");
        settings.put("lastProfileSyncStatus", "idle");
        settings.put("lastProfileSyncMessage", "");
        settings.put("avatar", "");
        settings.put("notifyApplication", true);
        settings.put("notifyRecommendation", true);
        settings.put("notifyInterview", true);
        settings.put("updatedAt", currentTime);
        return settings;
    }

    private ProfileData mapProfileData(Map<String, Object> account, Map<String, Object> profile, Map<String, Object> setting) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taId", asString(account.get("id")));
        data.put("realName", firstNonBlank(asString(profile.get("realName")), asString(account.get("name"))));
        data.put("applicationIntent", asString(profile.get("applicationIntent")));
        data.put("studentId", firstNonBlank(asString(profile.get("studentId")), asString(account.get("id"))));
        data.put("contactEmail", firstNonBlank(asString(profile.get("contactEmail")), asString(account.get("email"))));
        data.put("bio", asString(profile.get("bio")));
        data.put("avatar", firstNonBlank(asString(profile.get("avatar")), asString(setting.get("avatar"))));
        data.put("skills", normalizeSkills(asStringList(profile.get("skills"))));
        data.put("lastUpdatedAt", firstNonBlank(asString(profile.get("lastUpdatedAt")), asString(setting.get("updatedAt"))));
        return new ProfileData(data);
    }

    private boolean matchesIdentifier(Map<String, Object> account, String identifier) {
        return identifier.equalsIgnoreCase(asString(account.get("id")))
                || identifier.equalsIgnoreCase(asString(account.get("username")))
                || identifier.equalsIgnoreCase(asString(account.get("email")))
                || identifier.equals(asString(account.get("phone")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ensureAuthMap(Map<String, Object> account) {
        Object current = account.get("auth");
        if (current instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("passwordHash", "");
        auth.put("passwordSalt", "");
        auth.put("lastLoginAt", "");
        auth.put("failedAttempts", 0);
        account.put("auth", auth);
        return auth;
    }

    private Map<String, Object> findAccountByTaId(List<Map<String, Object>> accounts, String taId) {
        for (Map<String, Object> account : accounts) {
            if (taId.equalsIgnoreCase(asString(account.get("id")))) {
                return account;
            }
        }
        return null;
    }

    private Map<String, Object> findProfileByTaId(List<Map<String, Object>> profiles, String taId) {
        for (Map<String, Object> profile : profiles) {
            if (taId.equalsIgnoreCase(asString(profile.get("taId")))) {
                return profile;
            }
        }
        return null;
    }

    private Map<String, Object> findSettingByTaId(List<Map<String, Object>> settings, String taId) {
        for (Map<String, Object> setting : settings) {
            if (taId.equalsIgnoreCase(asString(setting.get("taId")))) {
                return setting;
            }
        }
        return null;
    }

    private Map<String, Object> ensureProfileRecord(List<Map<String, Object>> profiles, Map<String, Object> account, String currentTime) {
        String taId = asString(account.get("id"));
        Map<String, Object> profile = findProfileByTaId(profiles, taId);
        if (profile == null) {
            profile = createDefaultProfile(taId, asString(account.get("name")), asString(account.get("email")), currentTime);
            profiles.add(profile);
        }
        return profile;
    }

    private Map<String, Object> ensureSettingsRecord(List<Map<String, Object>> settings, String taId, String currentTime) {
        Map<String, Object> setting = findSettingByTaId(settings, taId);
        if (setting == null) {
            setting = createDefaultSettings(taId, currentTime);
            settings.add(setting);
        }
        return setting;
    }

    private boolean containsAny(String value, String... candidates) {
        String text = value == null ? "" : value.toLowerCase();
        for (String candidate : candidates) {
            if (text.contains(candidate.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String trimToEmpty(String value) { return trim(value); }
    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed.isEmpty() ? null : trimmed;
    }
    private String asString(Object value) { return value == null ? "" : String.valueOf(value); }
    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(asString(value));
    }

    private boolean getAsBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private int asInt(Object value) {
        if (value instanceof Number num) return num.intValue();
        try {
            return (int) Double.parseDouble(asString(value));
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                String text = trimToEmpty(asString(item));
                if (!text.isBlank()) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private List<String> normalizeSkills(List<String> source) {
        List<String> normalized = new ArrayList<>();
        if (source == null) {
            return normalized;
        }
        for (String item : source) {
            String text = trimToEmpty(item);
            if (!text.isBlank() && !normalized.contains(text)) {
                normalized.add(text);
            }
        }
        return normalized;
    }

    private String firstNonBlank(String primary, String fallback) {
        String first = trim(primary);
        return first.isEmpty() ? trim(fallback) : first;
    }

    public static Path getResolvedDataRoot() {
        return DataMountPaths.root();
    }

    public static Path getResolvedTaDataDir() {
        return DataMountPaths.taDir();
    }

    public static String getDataMountStatusMessage() {
        return "TA data root=" + getResolvedTaDataDir().toAbsolutePath();
    }

    public static class ProfileData {
        private final Map<String, Object> data;

        public ProfileData(Map<String, Object> data) {
            this.data = data;
        }

        public Map<String, Object> toMap() {
            return data;
        }
    }

    public static class TaLoginResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;
        private TaLoginResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success; this.status = status; this.message = message; this.data = data;
        }
        public static TaLoginResult success(Map<String, Object> data) { return new TaLoginResult(true, 200, "登录成功", data); }
        public static TaLoginResult failure(int status, String message) { return new TaLoginResult(false, status, message, null); }
        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    public static class TaRegisterResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;
        private TaRegisterResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success; this.status = status; this.message = message; this.data = data;
        }
        public static TaRegisterResult success(Map<String, Object> data) { return new TaRegisterResult(true, 201, "注册成功", data); }
        public static TaRegisterResult failure(int status, String message) { return new TaRegisterResult(false, status, message, null); }
        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    public record ProfileUpdateInput(String taId, String realName, String applicationIntent, String contactEmail,
                                     String bio, List<String> skills, String avatar) {
    }

    public record ApplicationCreateInput(String taId,
                                         String courseCode,
                                         String originalFileName,
                                         String contentType,
                                         InputStream resumeStream) {
    }

    public static class ProfileResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;
        private ProfileResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success; this.status = status; this.message = message; this.data = data;
        }
        public static ProfileResult success(ProfileData data) { return new ProfileResult(true, 200, "操作成功", data.toMap()); }
        public static ProfileResult failure(int status, String message) { return new ProfileResult(false, status, message, null); }
        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    public static class PasswordUpdateResult {
        private final boolean success;
        private final int status;
        private final String message;
        private PasswordUpdateResult(boolean success, int status, String message) {
            this.success = success; this.status = status; this.message = message;
        }
        public static PasswordUpdateResult success() { return new PasswordUpdateResult(true, 200, "密码更新成功"); }
        public static PasswordUpdateResult failure(int status, String message) { return new PasswordUpdateResult(false, status, message); }
        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
    }

    public static class ApplicationSubmitResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final JsonObject data;

        private ApplicationSubmitResult(boolean success, int status, String message, JsonObject data) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public static ApplicationSubmitResult success(JsonObject data) {
            return new ApplicationSubmitResult(true, 201, "申请提交成功", data);
        }

        public static ApplicationSubmitResult failure(int status, String message) {
            return new ApplicationSubmitResult(false, status, message, new JsonObject());
        }

        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public JsonObject getData() { return data; }
    }

    public static class ApplicationStatusResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final JsonArray items;
        private final JsonObject summary;
        private final JsonArray messages;

        private ApplicationStatusResult(boolean success, int status, String message, JsonArray items, JsonObject summary, JsonArray messages) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.items = items;
            this.summary = summary;
            this.messages = messages;
        }

        public static ApplicationStatusResult success(JsonArray items, JsonObject summary, JsonArray messages) {
            return new ApplicationStatusResult(true, 200, "获取成功", items, summary, messages);
        }

        public static ApplicationStatusResult failure(int status, String message) {
            return new ApplicationStatusResult(false, status, message, new JsonArray(), new JsonObject(), new JsonArray());
        }

        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public JsonArray getItems() { return items; }
        public JsonObject getSummary() { return summary; }
        public JsonArray getMessages() { return messages; }
    }
}
