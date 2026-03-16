package com.bupt.tarecruit.ta.dao;

import com.bupt.tarecruit.common.util.AuthUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaAccountDao {
    private static final String DATA_MOUNT_ENV = "mountDataTAMObupter";
    private static final Path DEFAULT_DATA_ROOT = Path.of("mountDataTAMObupter");
    private static final String TA_SCHEMA = "ta";
    private static final String TA_ENTITY = "tas";
    private static final String PROFILE_SCHEMA = "ta";
    private static final String PROFILE_ENTITY = "profiles";
    private static final String SETTINGS_SCHEMA = "ta";
    private static final String SETTINGS_ENTITY = "settings";

    private static final Path TA_DATA_PATH = resolveDataPath("ta", "tas.json");
    private static final Path PROFILE_DATA_PATH = resolveDataPath("ta", "profiles.json");
    private static final Path SETTINGS_DATA_PATH = resolveDataPath("ta", "settings.json");

    public TaLoginResult login(String identifier, String password) throws IOException {
        String normalizedIdentifier = trim(identifier);
        String normalizedPassword = password == null ? "" : password;

        if (normalizedIdentifier.isEmpty() || normalizedPassword.isEmpty()) {
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

        if (!AuthUtils.hashPassword(normalizedPassword, salt).equals(passwordHash)) {
            auth.put("failedAttempts", asInt(auth.get("failedAttempts")) + 1);
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
        data.put("isFirstLogin", false);
        return TaLoginResult.success(data);
    }

    public TaRegisterResult register(String taId, String name, String username, String email, String phone, String password) throws IOException {
        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        for (Map<String, Object> account : accounts) {
            if (asString(account.get("id")).equals(taId)) {
                return TaRegisterResult.failure(409, "TA ID 已存在，请刷新后重试");
            }
            if (asString(account.get("username")).equalsIgnoreCase(username)) {
                return TaRegisterResult.failure(409, "用户名已被占用");
            }
            if (asString(account.get("email")).equalsIgnoreCase(email)) {
                return TaRegisterResult.failure(409, "邮箱已被注册");
            }
            if (asString(account.get("phone")).equals(phone)) {
                return TaRegisterResult.failure(409, "手机号已被注册");
            }
        }

        String currentTime = AuthUtils.nowIso();
        String salt = AuthUtils.generateSalt();
        String passwordHash = AuthUtils.hashPassword(password, salt);

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("passwordHash", passwordHash);
        auth.put("passwordSalt", salt);
        auth.put("lastLoginAt", currentTime);
        auth.put("failedAttempts", 0);

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
        account.put("auth", auth);
        accounts.add(account);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", "PROFILE-" + taId);
        profile.put("taId", taId);
        profile.put("avatar", "");
        profile.put("realName", "");
        profile.put("applicationIntent", "");
        profile.put("studentId", taId);
        profile.put("contactEmail", "");
        profile.put("bio", "");
        profile.put("skills", new ArrayList<>());
        profile.put("availabilityHoursPerWeek", 0);
        profile.put("semester", "");
        profile.put("title", "TA");
        profile.put("lastUpdatedAt", currentTime);
        profiles.add(profile);

        Map<String, Object> setting = new LinkedHashMap<>();
        setting.put("id", "SETTING-" + taId);
        setting.put("taId", taId);
        setting.put("theme", "dark");
        setting.put("onboardingStep", 0);
        setting.put("guideCompleted", false);
        setting.put("createdAt", currentTime);
        settings.add(setting);

        saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taId", taId);
        data.put("name", name);
        data.put("username", username);
        data.put("email", email);
        data.put("phone", phone);
        data.put("createdAt", currentTime);
        return TaRegisterResult.success(data);
    }

    private boolean matchesIdentifier(Map<String, Object> account, String identifier) {
        return asString(account.get("username")).equalsIgnoreCase(identifier)
                || asString(account.get("email")).equalsIgnoreCase(identifier)
                || asString(account.get("phone")).equals(identifier);
    }

    private List<Map<String, Object>> loadRecords(Path path, String schema, String entity) throws IOException {
        ensureDataFile(path, schema, entity);
        String content = Files.readString(path, StandardCharsets.UTF_8).trim();
        if (content.isEmpty() || content.equals("[]")) {
            return new ArrayList<>();
        }
        if (content.startsWith("{")) {
            Map<String, Object> root = SimpleJsonParser.parseObject(content);
            Object items = root.get("items");
            if (items instanceof List<?> list) {
                return castRecordList(list);
            }
            return new ArrayList<>();
        }
        return SimpleJsonParser.parseArray(content);
    }

    private void saveRecords(Path path, String schema, String entity, List<Map<String, Object>> records) throws IOException {
        ensureDataFile(path, schema, entity);
        Files.writeString(path, SimpleJsonWriter.writeRootObject(schema, entity, records), StandardCharsets.UTF_8);
    }

    private void ensureDataFile(Path path, String schema, String entity) throws IOException {
        if (Files.notExists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        if (Files.notExists(path)) {
            Files.writeString(path, SimpleJsonWriter.writeRootObject(schema, entity, new ArrayList<>()), StandardCharsets.UTF_8);
            return;
        }

        String content = Files.readString(path, StandardCharsets.UTF_8).trim();
        if (content.isEmpty() || content.equals("[]")) {
            Files.writeString(path, SimpleJsonWriter.writeRootObject(schema, entity, new ArrayList<>()), StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castRecordList(List<?> list) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                results.add((Map<String, Object>) map);
            }
        }
        return results;
    }

    private static Path resolveDataPath(String subDirectory, String fileName) {
        String envValue = System.getenv(DATA_MOUNT_ENV);
        Path root = (envValue == null || envValue.trim().isEmpty())
                ? DEFAULT_DATA_ROOT
                : Path.of(envValue.trim());
        return root.resolve(subDirectory).resolve(fileName);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static class TaLoginResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;

        private TaLoginResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public static TaLoginResult success(Map<String, Object> data) {
            return new TaLoginResult(true, 200, "登录成功", data);
        }

        public static TaLoginResult failure(int status, String message) {
            return new TaLoginResult(false, status, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getData() {
            return data;
        }
    }

    public static class TaRegisterResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;

        private TaRegisterResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public static TaRegisterResult success(Map<String, Object> data) {
            return new TaRegisterResult(true, 200, "注册成功", data);
        }

        public static TaRegisterResult failure(int status, String message) {
            return new TaRegisterResult(false, status, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getData() {
            return data;
        }
    }

    static final class SimpleJsonWriter {
        private SimpleJsonWriter() {
        }

        static String writeRootObject(String schema, String entity, List<Map<String, Object>> records) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("schema", schema);
            meta.put("entity", entity);
            meta.put("version", "1.0");
            meta.put("updatedAt", Instant.now().toString());

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("meta", meta);
            root.put("items", records);
            return writeObject(root, 0);
        }

        static String writeArray(List<Map<String, Object>> records) {
            StringBuilder builder = new StringBuilder();
            builder.append("[\n");
            for (int i = 0; i < records.size(); i++) {
                builder.append("  ").append(writeObject(records.get(i), 1));
                if (i < records.size() - 1) {
                    builder.append(',');
                }
                builder.append('\n');
            }
            builder.append(']');
            return builder.toString();
        }

        @SuppressWarnings("unchecked")
        private static String writeValue(Object value, int indent) {
            if (value == null) {
                return "null";
            }
            if (value instanceof String stringValue) {
                return quote(stringValue);
            }
            if (value instanceof Number || value instanceof Boolean) {
                return String.valueOf(value);
            }
            if (value instanceof Map<?, ?> map) {
                return writeObject((Map<String, Object>) map, indent);
            }
            if (value instanceof List<?> list) {
                StringBuilder builder = new StringBuilder();
                if (list.isEmpty()) {
                    builder.append("[]");
                    return builder.toString();
                }
                builder.append("[\n");
                for (int i = 0; i < list.size(); i++) {
                    builder.append("  ".repeat(indent + 1))
                            .append(writeValue(list.get(i), indent + 1));
                    if (i < list.size() - 1) {
                        builder.append(',');
                    }
                    builder.append('\n');
                }
                builder.append("  ".repeat(indent)).append(']');
                return builder.toString();
            }
            return quote(String.valueOf(value));
        }

        private static String writeObject(Map<String, Object> map, int indent) {
            StringBuilder builder = new StringBuilder();
            builder.append("{\n");
            int index = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                builder.append("  ".repeat(indent + 1))
                        .append(quote(entry.getKey()))
                        .append(": ")
                        .append(writeValue(entry.getValue(), indent + 1));
                if (index < map.size() - 1) {
                    builder.append(',');
                }
                builder.append('\n');
                index++;
            }
            builder.append("  ".repeat(indent)).append('}');
            return builder.toString();
        }

        private static String quote(String value) {
            return '"' + value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t") + '"';
        }
    }

    static final class SimpleJsonParser {
        private static final Pattern FIELD_PATTERN = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(.+)");

        private SimpleJsonParser() {
        }

        static List<Map<String, Object>> parseArray(String json) {
            String trimmed = json.trim();
            if (trimmed.length() < 2) {
                return new ArrayList<>();
            }
            String body = trimmed.substring(1, trimmed.length() - 1).trim();
            List<Map<String, Object>> results = new ArrayList<>();
            if (body.isEmpty()) {
                return results;
            }

            int depth = 0;
            int start = -1;
            for (int i = 0; i < body.length(); i++) {
                char ch = body.charAt(i);
                if (ch == '{') {
                    if (depth == 0) {
                        start = i;
                    }
                    depth++;
                } else if (ch == '}') {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        results.add(parseObject(body.substring(start, i + 1)));
                        start = -1;
                    }
                }
            }
            return results;
        }

        static Map<String, Object> parseObject(String json) {
            Map<String, Object> result = new LinkedHashMap<>();
            String body = json.trim();
            body = body.substring(1, body.length() - 1).trim();
            List<String> fields = splitTopLevel(body);
            for (String field : fields) {
                Matcher matcher = FIELD_PATTERN.matcher(field.trim());
                if (!matcher.matches()) {
                    continue;
                }
                String key = matcher.group(1);
                String value = matcher.group(2).trim();
                result.put(key, parseValue(value));
            }
            return result;
        }

        private static Object parseValue(String value) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
                return unquote(value);
            }
            if (value.startsWith("{")) {
                return parseObject(value);
            }
            if (value.startsWith("[")) {
                return parseList(value);
            }
            if ("null".equals(value)) {
                return null;
            }
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.parseBoolean(value);
            }
            try {
                if (value.contains(".")) {
                    return Double.parseDouble(value);
                }
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                return value;
            }
        }

        private static List<Object> parseList(String json) {
            String body = json.substring(1, json.length() - 1).trim();
            List<Object> result = new ArrayList<>();
            if (body.isEmpty()) {
                return result;
            }
            List<String> values = splitTopLevel(body);
            for (String value : values) {
                result.add(parseValue(value.trim()));
            }
            return result;
        }

        private static List<String> splitTopLevel(String body) {
            List<String> parts = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            int objectDepth = 0;
            int arrayDepth = 0;
            boolean inString = false;
            boolean escaping = false;

            for (int i = 0; i < body.length(); i++) {
                char ch = body.charAt(i);
                current.append(ch);

                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (ch == '\\') {
                    escaping = true;
                    continue;
                }
                if (ch == '"') {
                    inString = !inString;
                    continue;
                }
                if (inString) {
                    continue;
                }
                if (ch == '{') {
                    objectDepth++;
                } else if (ch == '}') {
                    objectDepth--;
                } else if (ch == '[') {
                    arrayDepth++;
                } else if (ch == ']') {
                    arrayDepth--;
                } else if (ch == ',' && objectDepth == 0 && arrayDepth == 0) {
                    current.deleteCharAt(current.length() - 1);
                    parts.add(current.toString());
                    current.setLength(0);
                }
            }
            if (!current.isEmpty()) {
                parts.add(current.toString());
            }
            return parts;
        }

        private static String unquote(String value) {
            String body = value.substring(1, value.length() - 1);
            return body
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\\", "\\");
        }
    }
}
