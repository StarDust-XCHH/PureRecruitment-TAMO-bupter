package com.bupt.tarecruit.mo.dao;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.common.util.AuthUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

/**
 * 教师（MO）账户数据访问对象。
 *
 * <p>负责 MO 账号的注册、登录、个人资料及系统设置的持久化。</p>
 * <p>数据存储于 {@code mountDataTAMObupter/mo/} 目录下的三个 JSON 文件：</p>
 * <ul>
 *     <li>{@code mos.json} - MO 主账号及认证信息</li>
 *     <li>{@code profiles.json} - MO 个人资料</li>
 *     <li>{@code settings.json} - MO 系统设置</li>
 * </ul>
 *
 * <h3>线程安全</h3>
 * <p>所有写操作方法均使用 {@code synchronized} 关键字保证并发安全。</p>
 *
 * @see DataMountPaths
 * @see AuthUtils
 */
public class MoAccountDao {
    private static final String MO_SCHEMA = "mo";
    private static final String MO_ENTITY = "mos";
    private static final String PROFILE_SCHEMA = "mo";
    private static final String PROFILE_ENTITY = "profiles";
    private static final String SETTINGS_SCHEMA = "mo";
    private static final String SETTINGS_ENTITY = "settings";

    private static final Path MO_DIR_PATH = DataMountPaths.moDir();
    private static final Path MO_DATA_PATH = MO_DIR_PATH.resolve("mos.json");
    private static final Path PROFILE_DATA_PATH = MO_DIR_PATH.resolve("profiles.json");
    private static final Path SETTINGS_DATA_PATH = MO_DIR_PATH.resolve("settings.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * 获取 MO 数据目录路径。
     *
     * @return MO 数据目录的 Path 对象
     */
    public static Path getResolvedMoDataDir() {
        return MO_DIR_PATH;
    }

    /**
     * MO 用户登录。
     *
     * @param identifier 标识符（MO ID、用户名、邮箱或手机号）
     * @param password   密码
     * @return 登录结果，包含用户信息或错误消息
     * @throws IOException 文件读写异常
     */
    public synchronized MoLoginResult login(String identifier, String password) throws IOException {
        String normalizedIdentifier = trim(identifier);
        String rawPassword = password == null ? "" : password;

        if (normalizedIdentifier.isEmpty() || rawPassword.isEmpty()) {
            return MoLoginResult.failure(400, "请输入账号和密码");
        }

        List<Map<String, Object>> accounts = loadRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY);
        Map<String, Object> matchedAccount = null;

        for (Map<String, Object> account : accounts) {
            if (matchesIdentifier(account, normalizedIdentifier)) {
                matchedAccount = account;
                break;
            }
        }

        if (matchedAccount == null) {
            return MoLoginResult.failure(404, "账号不存在");
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
            saveRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY, accounts);
            return MoLoginResult.failure(401, "账号或密码错误");
        }

        auth.put("failedAttempts", 0);
        auth.put("lastLoginAt", currentTime);
        matchedAccount.put("updatedAt", currentTime);
        saveRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY, accounts);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moId", asString(matchedAccount.get("id")));
        data.put("name", asString(matchedAccount.get("name")));
        data.put("username", asString(matchedAccount.get("username")));
        data.put("email", asString(matchedAccount.get("email")));
        data.put("phone", asString(matchedAccount.get("phone")));
        data.put("department", asString(matchedAccount.get("department")));
        data.put("status", asString(matchedAccount.get("status")));
        data.put("loginAt", currentTime);
        data.put("isFirstLogin", isFirstLogin);
        return MoLoginResult.success(data);
    }

    /**
     * MO 用户注册。
     *
     * <p>同时初始化主账号、个人资料和系统设置三个数据文件。</p>
     *
     * @param moId     MO ID（自动添加 "MO-" 前缀）
     * @param name     姓名
     * @param username 用户名
     * @param email    邮箱
     * @param phone    手机号
     * @param password 密码
     * @return 注册结果
     * @throws IOException 文件读写异常
     */
    public synchronized MoRegisterResult register(String moId, String name, String username,
                                                  String email, String phone, String password) throws IOException {
        List<Map<String, Object>> accounts = loadRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        // 唯一性检查
        for (Map<String, Object> account : accounts) {
            if (asString(account.get("id")).equalsIgnoreCase(moId))
                return MoRegisterResult.failure(409, "MO ID 已存在");
            if (asString(account.get("username")).equalsIgnoreCase(username))
                return MoRegisterResult.failure(409, "用户名已被占用");
            if (asString(account.get("email")).equalsIgnoreCase(email))
                return MoRegisterResult.failure(409, "邮箱已被注册");
            if (asString(account.get("phone")).equals(phone))
                return MoRegisterResult.failure(409, "手机号已被注册");
        }

        String currentTime = AuthUtils.nowIso();
        String salt = AuthUtils.generateSalt();
        String passwordHash = AuthUtils.hashPassword(password, salt);

        // 创建主账号记录
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("id", moId);
        account.put("name", name);
        account.put("username", username);
        account.put("email", email);
        account.put("phone", phone);
        account.put("department", "");
        account.put("status", "active");
        account.put("createdAt", currentTime);
        account.put("updatedAt", currentTime);

        // 创建认证信息
        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("passwordHash", passwordHash);
        auth.put("passwordSalt", salt);
        auth.put("lastLoginAt", "");
        auth.put("failedAttempts", 0);
        account.put("auth", auth);
        accounts.add(account);

        // 创建默认资料和设置
        profiles.add(createDefaultProfile(moId, name, email, currentTime));
        settings.add(createDefaultSettings(moId, currentTime));

        saveRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY, accounts);
        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moId", moId);
        data.put("username", username);
        data.put("createdAt", currentTime);
        return MoRegisterResult.success(data);
    }

    /**
     * 获取 MO 个人资料与设置。
     *
     * @param moId MO ID
     * @return 资料结果，包含账户、资料和设置信息
     * @throws IOException 文件读写异常
     */
    public synchronized ProfileResult getProfileSettings(String moId) throws IOException {
        String normalizedMoId = trim(moId);
        if (normalizedMoId.isEmpty()) {
            return ProfileResult.failure(400, "缺少 MO 标识");
        }

        List<Map<String, Object>> accounts = loadRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        Map<String, Object> account = findAccountByMoId(accounts, normalizedMoId);
        if (account == null) {
            return ProfileResult.failure(404, "未找到对应 MO 账号");
        }

        String now = AuthUtils.nowIso();
        Map<String, Object> profile = ensureProfileRecord(profiles, account, now);
        Map<String, Object> setting = ensureSettingsRecord(settings, normalizedMoId, now);

        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        return ProfileResult.success(mapProfileData(account, profile, setting));
    }

    /**
     * 保存 MO 个人资料与设置。
     *
     * @param input 资料更新输入
     * @return 保存结果
     * @throws IOException 文件读写异常
     */
    public synchronized ProfileResult saveProfileSettings(ProfileUpdateInput input) throws IOException {
        if (input == null || trim(input.moId()).isEmpty()) {
            return ProfileResult.failure(400, "缺少 MO 标识");
        }

        List<Map<String, Object>> accounts = loadRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        Map<String, Object> account = findAccountByMoId(accounts, trim(input.moId()));
        if (account == null) {
            return ProfileResult.failure(404, "未找到对应 MO 账号");
        }

        String now = AuthUtils.nowIso();
        Map<String, Object> profile = ensureProfileRecord(profiles, account, now);
        Map<String, Object> setting = ensureSettingsRecord(settings, trim(input.moId()), now);

        String normalizedRealName = trimToNull(input.realName());
        String normalizedEmail = trimToEmpty(input.contactEmail());
        String normalizedBio = trimToEmpty(input.bio());
        String normalizedAvatar = trimToEmpty(input.avatar());
        List<String> normalizedSkills = normalizeSkills(input.skills());

        profile.put("realName", normalizedRealName == null ? asString(account.get("name")) : normalizedRealName);
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

        saveRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY, accounts);
        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        return ProfileResult.success(mapProfileData(account, profile, setting));
    }

    /**
     * 更新 MO 密码。
     *
     * @param moId            MO ID
     * @param currentPassword 当前密码
     * @param newPassword     新密码
     * @return 密码更新结果
     * @throws IOException 文件读写异常
     */
    public synchronized PasswordUpdateResult updatePassword(String moId, String currentPassword, String newPassword) throws IOException {
        String normalizedMoId = trim(moId);
        String normalizedCurrentPassword = currentPassword == null ? "" : currentPassword;
        String normalizedNewPassword = newPassword == null ? "" : newPassword;

        if (normalizedMoId.isEmpty()) {
            return PasswordUpdateResult.failure(400, "缺少 MO 标识");
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

        List<Map<String, Object>> accounts = loadRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY);
        Map<String, Object> account = findAccountByMoId(accounts, normalizedMoId);
        if (account == null) {
            return PasswordUpdateResult.failure(404, "未找到对应 MO 账号");
        }

        Map<String, Object> auth = ensureAuthMap(account);
        String salt = asString(auth.get("passwordSalt"));
        String passwordHash = asString(auth.get("passwordHash"));
        if (!AuthUtils.hashPassword(normalizedCurrentPassword, salt).equals(passwordHash)) {
            int currentFailures = asInt(auth.get("failedAttempts"));
            auth.put("failedAttempts", currentFailures + 1);
            saveRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY, accounts);
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
        saveRecords(MO_DATA_PATH, MO_SCHEMA, MO_ENTITY, accounts);
        return PasswordUpdateResult.success();
    }

    // ==================== 内部辅助方法 ====================

    private Map<String, Object> findAccountByMoId(List<Map<String, Object>> accounts, String moId) {
        for (Map<String, Object> account : accounts) {
            if (asString(account.get("id")).equalsIgnoreCase(moId)) {
                return account;
            }
        }
        return null;
    }

    private Map<String, Object> ensureProfileRecord(List<Map<String, Object>> profiles,
                                                    Map<String, Object> account, String now) {
        String moId = asString(account.get("id"));
        for (Map<String, Object> profile : profiles) {
            if (asString(profile.get("moId")).equalsIgnoreCase(moId)) {
                return profile;
            }
        }

        Map<String, Object> newProfile = createDefaultProfile(moId, asString(account.get("name")),
                asString(account.get("email")), now);
        profiles.add(newProfile);
        return newProfile;
    }

    private Map<String, Object> ensureSettingsRecord(List<Map<String, Object>> settings, String moId, String now) {
        for (Map<String, Object> setting : settings) {
            if (asString(setting.get("moId")).equalsIgnoreCase(moId)) {
                return setting;
            }
        }

        Map<String, Object> newSetting = createDefaultSettings(moId, now);
        settings.add(newSetting);
        return newSetting;
    }

    private Map<String, Object> mapProfileData(Map<String, Object> account,
                                               Map<String, Object> profile,
                                               Map<String, Object> setting) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moId", asString(account.get("id")));
        data.put("name", asString(account.get("name")));
        data.put("username", asString(account.get("username")));
        data.put("email", asString(account.get("email")));
        data.put("phone", asString(account.get("phone")));
        data.put("department", asString(account.get("department")));
        data.put("status", asString(account.get("status")));
        data.put("realName", asString(profile.get("realName")));
        data.put("contactEmail", asString(profile.get("contactEmail")));
        data.put("bio", asString(profile.get("bio")));
        data.put("avatar", asString(profile.get("avatar")));
        data.put("skills", profile.get("skills"));
        data.put("theme", asString(setting.get("theme")));
        data.put("createdAt", asString(account.get("createdAt")));
        data.put("updatedAt", asString(account.get("updatedAt")));
        data.put("lastUpdatedAt", asString(profile.get("lastUpdatedAt")));
        return data;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> normalizeSkills(List<String> skills) {
        if (skills == null) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String skill : skills) {
            String trimmed = trim(skill);
            if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private List<Map<String, Object>> loadRecords(Path path, String schema, String entity) throws IOException {
        ensureDataFile(path, schema, entity);
        String content = Files.readString(path, StandardCharsets.UTF_8).trim();
        if (content.isEmpty() || content.equals("[]")) return new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (root.has("items")) {
                JsonArray itemsArray = root.getAsJsonArray("items");
                List<Map<String, Object>> result = GSON.fromJson(itemsArray,
                        new TypeToken<List<Map<String, Object>>>() {}.getType());
                return result == null ? new ArrayList<>() : result;
            }
        } catch (Exception e) {
            System.err.println("[MO-DATA] JSON 解析失败：" + path);
            e.printStackTrace();
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

    private Map<String, Object> createDefaultProfile(String moId, String name, String email, String now) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", "PROFILE-" + moId);
        profile.put("moId", moId);
        profile.put("avatar", "");
        profile.put("realName", name == null ? "" : name);
        profile.put("title", "MO");
        profile.put("contactEmail", email == null ? "" : email);
        profile.put("bio", "");
        profile.put("skills", new ArrayList<>());
        profile.put("lastUpdatedAt", now);
        return profile;
    }

    private Map<String, Object> createDefaultSettings(String moId, String now) {
        Map<String, Object> setting = new LinkedHashMap<>();
        setting.put("id", "SETTING-" + moId);
        setting.put("moId", moId);
        setting.put("theme", "dark");
        setting.put("avatar", "");
        setting.put("createdAt", now);
        setting.put("updatedAt", now);
        return setting;
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

    private boolean matchesIdentifier(Map<String, Object> account, String identifier) {
        String storedId = asString(account.get("id"));
        String storedUsername = asString(account.get("username"));
        String storedEmail = asString(account.get("email"));
        String storedPhone = asString(account.get("phone"));

        return storedId.equalsIgnoreCase(identifier)
                || storedId.equalsIgnoreCase("MO-" + identifier)
                || storedUsername.equalsIgnoreCase(identifier)
                || storedEmail.equalsIgnoreCase(identifier)
                || storedPhone.equals(identifier);
    }

    // ==================== 类型转换工具 ====================

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value) {
        if (value instanceof Number num) return num.intValue();
        try {
            return (int) Double.parseDouble(asString(value));
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== 结果类 ====================

    /**
     * MO 登录结果封装。
     */
    public static class MoLoginResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;

        private MoLoginResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public static MoLoginResult success(Map<String, Object> data) {
            return new MoLoginResult(true, 200, "登录成功", data);
        }

        public static MoLoginResult failure(int status, String message) {
            return new MoLoginResult(false, status, message, null);
        }

        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    /**
     * MO 注册结果封装。
     */
    public static class MoRegisterResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;

        private MoRegisterResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public static MoRegisterResult success(Map<String, Object> data) {
            return new MoRegisterResult(true, 201, "注册成功", data);
        }

        public static MoRegisterResult failure(int status, String message) {
            return new MoRegisterResult(false, status, message, null);
        }

        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    /**
     * MO 个人资料查询结果。
     */
    public static class ProfileResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;

        private ProfileResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public static ProfileResult success(Map<String, Object> data) {
            return new ProfileResult(true, 200, "读取成功", data);
        }

        public static ProfileResult failure(int status, String message) {
            return new ProfileResult(false, status, message, null);
        }

        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    /**
     * MO 个人资料更新输入。
     */
    public static class ProfileUpdateInput {
        private final String moId;
        private final String realName;
        private final String contactEmail;
        private final String bio;
        private final List<String> skills;
        private final String avatar;

        public ProfileUpdateInput(String moId, String realName, String contactEmail,
                                  String bio, List<String> skills, String avatar) {
            this.moId = moId;
            this.realName = realName;
            this.contactEmail = contactEmail;
            this.bio = bio;
            this.skills = skills;
            this.avatar = avatar;
        }

        public String moId() { return moId; }
        public String realName() { return realName; }
        public String contactEmail() { return contactEmail; }
        public String bio() { return bio; }
        public List<String> skills() { return skills; }
        public String avatar() { return avatar; }
    }

    /**
     * MO 密码更新结果。
     */
    public static class PasswordUpdateResult {
        private final boolean success;
        private final int status;
        private final String message;

        private PasswordUpdateResult(boolean success, int status, String message) {
            this.success = success;
            this.status = status;
            this.message = message;
        }

        public static PasswordUpdateResult success() {
            return new PasswordUpdateResult(true, 200, "密码更新成功");
        }

        public static PasswordUpdateResult failure(int status, String message) {
            return new PasswordUpdateResult(false, status, message);
        }

        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
    }
}
