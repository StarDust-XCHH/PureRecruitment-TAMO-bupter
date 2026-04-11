package com.bupt.tarecruit.admin.dao;

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
 * 管理员账户数据访问对象。
 *
 * <p>负责 Admin 账号的注册、登录、个人资料及系统设置的持久化。</p>
 * <p>数据存储于 {@code mountDataTAMObupter/admin/} 目录下的三个 JSON 文件：</p>
 * <ul>
 *     <li>{@code admins.json} - Admin 主账号及认证信息</li>
 *     <li>{@code profiles.json} - Admin 个人资料（含权限）</li>
 *     <li>{@code settings.json} - Admin 系统设置</li>
 * </ul>
 *
 * <h3>线程安全</h3>
 * <p>所有写操作方法均使用 {@code synchronized} 关键字保证并发安全。</p>
 *
 * @see DataMountPaths
 * @see AuthUtils
 */
public class AdminAccountDao {
    private static final String ADMIN_SCHEMA = "admin";
    private static final String ADMIN_ENTITY = "admins";
    private static final String PROFILE_SCHEMA = "admin";
    private static final String PROFILE_ENTITY = "profiles";
    private static final String SETTINGS_SCHEMA = "admin";
    private static final String SETTINGS_ENTITY = "settings";

    private static final Path ADMIN_DIR_PATH = DataMountPaths.adminDir();
    private static final Path ADMIN_DATA_PATH = ADMIN_DIR_PATH.resolve("admins.json");
    private static final Path PROFILE_DATA_PATH = ADMIN_DIR_PATH.resolve("profiles.json");
    private static final Path SETTINGS_DATA_PATH = ADMIN_DIR_PATH.resolve("settings.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * 获取 Admin 数据目录路径。
     *
     * @return Admin 数据目录的 Path 对象
     */
    public static Path getResolvedAdminDataDir() {
        return ADMIN_DIR_PATH;
    }

    /**
     * Admin 用户登录。
     *
     * @param identifier 标识符（Admin ID、用户名或邮箱）
     * @param password   密码
     * @return 登录结果，包含用户信息或错误消息
     * @throws IOException 文件读写异常
     */
    public synchronized AdminLoginResult login(String identifier, String password) throws IOException {
        String normalizedIdentifier = trim(identifier);
        String rawPassword = password == null ? "" : password;

        if (normalizedIdentifier.isEmpty() || rawPassword.isEmpty()) {
            return AdminLoginResult.failure(400, "请输入账号和密码");
        }

        List<Map<String, Object>> accounts = loadRecords(ADMIN_DATA_PATH, ADMIN_SCHEMA, ADMIN_ENTITY);
        Map<String, Object> matchedAccount = null;

        for (Map<String, Object> account : accounts) {
            if (matchesIdentifier(account, normalizedIdentifier)) {
                matchedAccount = account;
                break;
            }
        }

        if (matchedAccount == null) {
            return AdminLoginResult.failure(404, "账号不存在");
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
            saveRecords(ADMIN_DATA_PATH, ADMIN_SCHEMA, ADMIN_ENTITY, accounts);
            return AdminLoginResult.failure(401, "账号或密码错误");
        }

        auth.put("failedAttempts", 0);
        auth.put("lastLoginAt", currentTime);
        matchedAccount.put("updatedAt", currentTime);
        saveRecords(ADMIN_DATA_PATH, ADMIN_SCHEMA, ADMIN_ENTITY, accounts);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("adminId", asString(matchedAccount.get("id")));
        data.put("name", asString(matchedAccount.get("name")));
        data.put("username", asString(matchedAccount.get("username")));
        data.put("email", asString(matchedAccount.get("email")));
        data.put("phone", asString(matchedAccount.get("phone")));
        data.put("department", asString(matchedAccount.get("department")));
        data.put("status", asString(matchedAccount.get("status")));
        data.put("loginAt", currentTime);
        data.put("isFirstLogin", isFirstLogin);
        return AdminLoginResult.success(data);
    }

    /**
     * Admin 用户注册。
     *
     * <p>同时初始化主账号、个人资料和系统设置三个数据文件。</p>
     *
     * @param adminId    Admin ID（自动添加 "ADMIN-" 前缀）
     * @param name       姓名
     * @param username   用户名
     * @param email      邮箱
     * @param phone      手机号
     * @param password   密码
     * @param department 部门
     * @param permissions 权限列表
     * @return 注册结果
     * @throws IOException 文件读写异常
     */
    public synchronized AdminRegisterResult register(String adminId, String name, String username,
                                                      String email, String phone, String password,
                                                      String department, List<String> permissions) throws IOException {
        List<Map<String, Object>> accounts = loadRecords(ADMIN_DATA_PATH, ADMIN_SCHEMA, ADMIN_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        // 规范化 adminId，确保有前缀
        String normalizedAdminId = adminId;
        if (!normalizedAdminId.toUpperCase().startsWith("ADMIN-")) {
            normalizedAdminId = "ADMIN-" + adminId;
        }

        // 唯一性检查
        for (Map<String, Object> account : accounts) {
            if (asString(account.get("id")).equalsIgnoreCase(normalizedAdminId))
                return AdminRegisterResult.failure(409, "Admin ID 已存在");
            if (asString(account.get("username")).equalsIgnoreCase(username))
                return AdminRegisterResult.failure(409, "用户名已被占用");
            if (!email.isBlank() && asString(account.get("email")).equalsIgnoreCase(email))
                return AdminRegisterResult.failure(409, "邮箱已被注册");
            if (!phone.isBlank() && asString(account.get("phone")).equals(phone))
                return AdminRegisterResult.failure(409, "手机号已被注册");
        }

        String currentTime = AuthUtils.nowIso();
        String salt = AuthUtils.generateSalt();
        String passwordHash = AuthUtils.hashPassword(password, salt);

        // 创建主账号记录
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("id", normalizedAdminId);
        account.put("name", name);
        account.put("username", username);
        account.put("email", email == null ? "" : email);
        account.put("phone", phone == null ? "" : phone);
        account.put("department", department == null ? "" : department);
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
        profiles.add(createDefaultProfile(normalizedAdminId, name, email, permissions, currentTime));
        settings.add(createDefaultSettings(normalizedAdminId, currentTime));

        saveRecords(ADMIN_DATA_PATH, ADMIN_SCHEMA, ADMIN_ENTITY, accounts);
        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("adminId", normalizedAdminId);
        data.put("username", username);
        data.put("createdAt", currentTime);
        return AdminRegisterResult.success(data);
    }

    /**
     * 生成新的 Admin ID。
     *
     * @return 格式为 "ADMIN-XXXXX" 的新 ID
     * @throws IOException 文件读写异常
     */
    public synchronized String generateNewAdminId() throws IOException {
        List<Map<String, Object>> accounts = loadRecords(ADMIN_DATA_PATH, ADMIN_SCHEMA, ADMIN_ENTITY);
        int maxNum = 0;
        for (Map<String, Object> account : accounts) {
            String id = asString(account.get("id"));
            if (id.matches("ADMIN-\\d+")) {
                try {
                    int num = Integer.parseInt(id.replace("ADMIN-", ""));
                    if (num > maxNum) {
                        maxNum = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("ADMIN-%05d", maxNum + 1);
    }

    /**
     * 获取 Admin 个人资料与设置。
     *
     * @param adminId Admin ID
     * @return 资料结果，包含账户、资料和设置信息
     * @throws IOException 文件读写异常
     */
    public synchronized ProfileResult getProfileSettings(String adminId) throws IOException {
        String normalizedAdminId = trim(adminId);
        if (normalizedAdminId.isEmpty()) {
            return ProfileResult.failure(400, "缺少 Admin 标识");
        }

        List<Map<String, Object>> accounts = loadRecords(ADMIN_DATA_PATH, ADMIN_SCHEMA, ADMIN_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        Map<String, Object> account = findAccountByAdminId(accounts, normalizedAdminId);
        if (account == null) {
            return ProfileResult.failure(404, "未找到对应 Admin 账号");
        }

        String now = AuthUtils.nowIso();
        Map<String, Object> profile = ensureProfileRecord(profiles, account, now);
        Map<String, Object> setting = ensureSettingsRecord(settings, normalizedAdminId, now);

        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        return ProfileResult.success(mapProfileData(account, profile, setting));
    }

    // ==================== 内部辅助方法 ====================

    private Map<String, Object> findAccountByAdminId(List<Map<String, Object>> accounts, String adminId) {
        for (Map<String, Object> account : accounts) {
            if (asString(account.get("id")).equalsIgnoreCase(adminId)) {
                return account;
            }
        }
        return null;
    }

    private Map<String, Object> ensureProfileRecord(List<Map<String, Object>> profiles,
                                                     Map<String, Object> account, String now) {
        String adminId = asString(account.get("id"));
        for (Map<String, Object> profile : profiles) {
            if (asString(profile.get("adminId")).equalsIgnoreCase(adminId)) {
                return profile;
            }
        }

        Map<String, Object> newProfile = createDefaultProfile(adminId, asString(account.get("name")),
                asString(account.get("email")), null, now);
        profiles.add(newProfile);
        return newProfile;
    }

    private Map<String, Object> ensureSettingsRecord(List<Map<String, Object>> settings, String adminId, String now) {
        for (Map<String, Object> setting : settings) {
            if (asString(setting.get("adminId")).equalsIgnoreCase(adminId)) {
                return setting;
            }
        }

        Map<String, Object> newSetting = createDefaultSettings(adminId, now);
        settings.add(newSetting);
        return newSetting;
    }

    private Map<String, Object> mapProfileData(Map<String, Object> account,
                                                Map<String, Object> profile,
                                                Map<String, Object> setting) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("adminId", asString(account.get("id")));
        data.put("name", asString(account.get("name")));
        data.put("username", asString(account.get("username")));
        data.put("email", asString(account.get("email")));
        data.put("phone", asString(account.get("phone")));
        data.put("department", asString(account.get("department")));
        data.put("status", asString(account.get("status")));
        data.put("realName", asString(profile.get("realName")));
        data.put("title", asString(profile.get("title")));
        data.put("permissions", profile.get("permissions"));
        data.put("theme", asString(setting.get("theme")));
        data.put("createdAt", asString(account.get("createdAt")));
        data.put("updatedAt", asString(account.get("updatedAt")));
        data.put("lastUpdatedAt", asString(profile.get("lastUpdatedAt")));
        return data;
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
            System.err.println("[ADMIN-DATA] JSON 解析失败：" + path);
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

    private Map<String, Object> createDefaultProfile(String adminId, String name, String email,
                                                       List<String> permissions, String now) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", "PROFILE-" + adminId);
        profile.put("adminId", adminId);
        profile.put("realName", name == null ? "" : name);
        profile.put("title", "Administrator");
        profile.put("contactEmail", email == null ? "" : email);
        profile.put("bio", "");
        // 默认权限
        List<String> defaultPermissions = permissions != null && !permissions.isEmpty() ? permissions :
                List.of("users:read", "users:write", "courses:read", "courses:write",
                        "notices:read", "notices:write", "settings:read", "settings:write");
        profile.put("permissions", defaultPermissions);
        profile.put("lastUpdatedAt", now);
        return profile;
    }

    private Map<String, Object> createDefaultSettings(String adminId, String now) {
        Map<String, Object> setting = new LinkedHashMap<>();
        setting.put("id", "SETTING-" + adminId);
        setting.put("adminId", adminId);
        setting.put("theme", "dark");

        Map<String, Object> notifications = new LinkedHashMap<>();
        notifications.put("userRegistered", true);
        notifications.put("coursePublished", true);
        notifications.put("systemAlert", true);
        setting.put("notifications", notifications);

        setting.put("dashboardLayout", "default");
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

        return storedId.equalsIgnoreCase(identifier)
                || storedId.equalsIgnoreCase("ADMIN-" + identifier)
                || storedUsername.equalsIgnoreCase(identifier)
                || storedEmail.equalsIgnoreCase(identifier);
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
     * Admin 登录结果封装。
     */
    public static class AdminLoginResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;

        private AdminLoginResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public static AdminLoginResult success(Map<String, Object> data) {
            return new AdminLoginResult(true, 200, "登录成功", data);
        }

        public static AdminLoginResult failure(int status, String message) {
            return new AdminLoginResult(false, status, message, null);
        }

        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    /**
     * Admin 注册结果封装。
     */
    public static class AdminRegisterResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;

        private AdminRegisterResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public static AdminRegisterResult success(Map<String, Object> data) {
            return new AdminRegisterResult(true, 201, "注册成功", data);
        }

        public static AdminRegisterResult failure(int status, String message) {
            return new AdminRegisterResult(false, status, message, null);
        }

        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    /**
     * Admin 个人资料查询结果。
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
}
