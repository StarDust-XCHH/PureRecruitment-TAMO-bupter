package com.bupt.tarecruit.ta.dao;

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

/**
 * 助教账户数据访问对象
 * 修复了 ID 匹配逻辑和密码处理一致性问题
 */
public class TaAccountDao {
    private static final String DATA_MOUNT_ENV = "mountDataTAMObupter";
    private static final Path DEFAULT_DATA_ROOT = Path.of("mountDataTAMObupter");

    private static final String TA_SCHEMA = "ta";
    private static final String TA_ENTITY = "tas";
    private static final String PROFILE_SCHEMA = "ta";
    private static final String PROFILE_ENTITY = "profiles";
    private static final String SETTINGS_SCHEMA = "ta";
    private static final String SETTINGS_ENTITY = "settings";

    private static final ResolvedDataRoot RESOLVED_DATA_ROOT = resolveDataRoot();
    private static final Path TA_DATA_PATH = RESOLVED_DATA_ROOT.rootPath().resolve("ta").resolve("tas.json");
    private static final Path PROFILE_DATA_PATH = RESOLVED_DATA_ROOT.rootPath().resolve("ta").resolve("profiles.json");
    private static final Path SETTINGS_DATA_PATH = RESOLVED_DATA_ROOT.rootPath().resolve("ta").resolve("settings.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * 用户登录逻辑
     */
    public synchronized TaLoginResult login(String identifier, String password) throws IOException {
        String normalizedIdentifier = trim(identifier);
        // 注意：这里不使用 trim()，确保与注册时保存的原始密码（可能包含空格）一致
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
        Map<String, Object> auth = (Map<String, Object>) matchedAccount.get("auth");
        String salt = asString(auth.get("passwordSalt"));
        String passwordHash = asString(auth.get("passwordHash"));
        String currentTime = AuthUtils.nowIso();

        // 校验密码哈希
        if (!AuthUtils.hashPassword(rawPassword, salt).equals(passwordHash)) {
            // 失败次数增加，并处理可能存在的 Double 类型
            int currentFailures = asInt(auth.get("failedAttempts"));
            auth.put("failedAttempts", currentFailures + 1);
            matchedAccount.put("updatedAt", currentTime);
            saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
            return TaLoginResult.failure(401, "账号或密码错误");
        }

        // 登录成功，重置失败次数
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
        data.put("isFirstLogin", false);
        return TaLoginResult.success(data);
    }

    /**
     * 用户注册逻辑
     */
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
        auth.put("lastLoginAt", currentTime);
        auth.put("failedAttempts", 0);
        account.put("auth", auth);
        accounts.add(account);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", "PROFILE-" + taId);
        profile.put("taId", taId);
        profile.put("studentId", taId);
        profile.put("skills", new ArrayList<>());
        profile.put("lastUpdatedAt", currentTime);
        profiles.add(profile);

        Map<String, Object> setting = new LinkedHashMap<>();
        setting.put("id", "SETTING-" + taId);
        setting.put("taId", taId);
        setting.put("theme", "dark");
        setting.put("createdAt", currentTime);
        settings.add(setting);

        saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taId", taId);
        data.put("username", username);
        data.put("createdAt", currentTime);
        return TaRegisterResult.success(data);
    }

    private List<Map<String, Object>> loadRecords(Path path, String schema, String entity) throws IOException {
        ensureDataFile(path, schema, entity);
        String content = Files.readString(path, StandardCharsets.UTF_8).trim();
        if (content.isEmpty() || content.equals("[]")) return new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (root.has("items")) {
                JsonArray itemsArray = root.getAsJsonArray("items");
                return GSON.fromJson(itemsArray, new TypeToken<List<Map<String, Object>>>() {}.getType());
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
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private void ensureDataFile(Path path, String schema, String entity) throws IOException {
        if (Files.notExists(path.getParent())) Files.createDirectories(path.getParent());
        if (Files.notExists(path)) saveRecords(path, schema, entity, new ArrayList<>());
    }

    /**
     * 【核心匹配逻辑修复】
     * 顺序：完整 ID -> 简写 ID -> 用户名 -> 邮箱 -> 手机号
     */
    private boolean matchesIdentifier(Map<String, Object> account, String identifier) {
        String storedId = asString(account.get("id")); // "TA-10258"
        String storedUsername = asString(account.get("username")); // "Seele"
        String storedEmail = asString(account.get("email"));
        String storedPhone = asString(account.get("phone"));

        return storedId.equalsIgnoreCase(identifier)                // 匹配 TA-10258
                || storedId.equalsIgnoreCase("TA-" + identifier)    // 匹配 10258
                || storedUsername.equalsIgnoreCase(identifier)      // 匹配 Seele
                || storedEmail.equalsIgnoreCase(identifier)         // 匹配邮箱
                || storedPhone.equals(identifier);                   // 匹配手机号
    }

    private static ResolvedDataRoot resolveDataRoot() {
        String envValue = System.getenv(DATA_MOUNT_ENV);
        if (envValue == null || envValue.trim().isEmpty()) {
            return new ResolvedDataRoot(false, DEFAULT_DATA_ROOT.toAbsolutePath().normalize());
        }
        return new ResolvedDataRoot(true, Path.of(envValue.trim()).toAbsolutePath().normalize());
    }

    public static String getDataMountStatusMessage() {
        return "[TA-DATA] 环境变量 " + DATA_MOUNT_ENV + " "
                + (RESOLVED_DATA_ROOT.fromEnvironment() ? "已挂载" : "未挂载，使用默认目录")
                + "；数据根目录=" + RESOLVED_DATA_ROOT.rootPath();
    }

    private String trim(String value) { return value == null ? "" : value.trim(); }
    private String asString(Object value) { return value == null ? "" : String.valueOf(value); }

    /**
     * 解决 Gson 将数字解析为 Double (如 0.0) 的兼容性转换
     */
    private int asInt(Object value) {
        if (value instanceof Number num) return num.intValue();
        try {
            return (int) Double.parseDouble(asString(value));
        } catch (Exception e) { return 0; }
    }

    private record ResolvedDataRoot(boolean fromEnvironment, Path rootPath) {}

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
        public static TaRegisterResult success(Map<String, Object> data) { return new TaRegisterResult(true, 200, "注册成功", data); }
        public static TaRegisterResult failure(int status, String message) { return new TaRegisterResult(false, status, message, null); }
        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }
}