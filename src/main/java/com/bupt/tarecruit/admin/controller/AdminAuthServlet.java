package com.bupt.tarecruit.admin.controller;

import com.bupt.tarecruit.admin.dao.AdminAccountDao;
import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.common.util.ServletJsonResponseWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin 认证 Servlet。
 * 处理管理员登录、初始化等操作。
 */
@WebServlet("/api/admin/auth")
public class AdminAuthServlet extends HttpServlet {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Path ADMIN_ACCOUNTS_PATH = DataMountPaths.adminAccounts();
    private final AdminAccountDao adminDao = new AdminAccountDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");

        switch (action) {
            case "login" -> login(req, resp);
            case "init" -> initAdmin(req, resp);
            default -> ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                    ApiResponse.failure("未找到该接口"));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");

        switch (action) {
            case "me" -> getCurrentUser(req, resp);
            default -> ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                    ApiResponse.failure("未找到该接口"));
        }
    }

    /**
     * 获取当前登录用户信息
     */
    private void getCurrentUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 从请求头获取用户ID (前端会在登录后将用户信息存储并传递)
        String adminId = req.getHeader("X-Admin-Id");
        String username = req.getHeader("X-Admin-Username");

        if (adminId == null || adminId.isEmpty()) {
            // 尝试从查询参数获取
            adminId = req.getParameter("adminId");
        }
        if (username == null || username.isEmpty()) {
            username = req.getParameter("username");
        }

        if (adminId == null || adminId.isEmpty()) {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    ApiResponse.failure("未登录或会话已过期"));
            return;
        }

        try {
            // 从 admins.json 读取完整信息
            if (!Files.exists(ADMIN_ACCOUNTS_PATH)) {
                ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                        ApiResponse.failure("管理员数据文件不存在"));
                return;
            }

            String content = Files.readString(ADMIN_ACCOUNTS_PATH, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(content, JsonObject.class);

            if (!root.has("items")) {
                ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                        ApiResponse.failure("管理员数据格式错误"));
                return;
            }

            var items = root.getAsJsonArray("items");
            JsonObject targetAdmin = null;

            // 查找匹配的管理员
            for (var item : items) {
                JsonObject admin = item.getAsJsonObject();
                String storedId = getJsonString(admin, "id");
                String storedUsername = getJsonString(admin, "username");

                if ((adminId != null && adminId.equals(storedId)) ||
                    (username != null && username.equals(storedUsername))) {
                    targetAdmin = admin;
                    break;
                }
            }

            if (targetAdmin == null) {
                ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                        ApiResponse.failure("用户不存在"));
                return;
            }

            // 构建用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", getJsonString(targetAdmin, "id"));
            userInfo.put("username", getJsonString(targetAdmin, "username"));
            userInfo.put("name", getJsonString(targetAdmin, "name"));
            userInfo.put("email", getJsonString(targetAdmin, "email"));
            userInfo.put("phone", getJsonString(targetAdmin, "phone"));
            userInfo.put("department", getJsonString(targetAdmin, "department"));
            userInfo.put("status", getJsonString(targetAdmin, "status"));
            userInfo.put("role", "ADMIN");
            userInfo.put("lastLoginAt", getJsonString(targetAdmin, "auth.lastLoginAt"));

            // 从 profiles.json 获取详细信息
            try {
                AdminAccountDao.ProfileResult profileResult = adminDao.getProfileSettings(getJsonString(targetAdmin, "id"));
                if (profileResult.isSuccess()) {
                    Map<String, Object> profile = profileResult.getData();
                    userInfo.put("realName", profile.get("realName"));
                    userInfo.put("title", profile.get("title"));
                    userInfo.put("bio", profile.get("bio"));
                    userInfo.put("permissions", profile.get("permissions"));
                }
            } catch (Exception e) {
                // profile 可能不存在，继续使用基本信息
            }

            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                    ApiResponse.success("获取用户信息成功", userInfo));

        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.failure("获取用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 管理员登录
     */
    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_BAD_REQUEST,
                    ApiResponse.failure("用户名和密码不能为空"));
            return;
        }

        try {
            // 读取管理员账户数据
            if (!Files.exists(ADMIN_ACCOUNTS_PATH)) {
                ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        ApiResponse.failure("管理员数据文件不存在"));
                return;
            }

            String content = Files.readString(ADMIN_ACCOUNTS_PATH, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(content, JsonObject.class);

            if (!root.has("items")) {
                ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        ApiResponse.failure("管理员数据格式错误"));
                return;
            }

            var items = root.getAsJsonArray("items");
            String loginTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // 遍历查找匹配的管理员
            for (var item : items) {
                JsonObject admin = item.getAsJsonObject();
                String storedUsername = getJsonString(admin, "username");
                String storedPasswordHash = getJsonString(admin, "auth.passwordHash");
                String storedPasswordSalt = getJsonString(admin, "auth.passwordSalt");
                String status = getJsonString(admin, "status");

                if (storedUsername.equals(username) && "active".equals(status)) {
                    // 验证密码
                    String inputHash = hashPassword(password, storedPasswordSalt);
                    if (inputHash.equals(storedPasswordHash)) {
                        // 登录成功，更新最后登录时间
                        admin.getAsJsonObject("auth").addProperty("lastLoginAt", loginTime);
                        admin.getAsJsonObject("auth").addProperty("failedAttempts", 0);
                        admin.addProperty("updatedAt", loginTime);

                        // 保存更新后的数据
                        root.addProperty("updatedAt", loginTime + "Z");
                        Files.writeString(ADMIN_ACCOUNTS_PATH, GSON.toJson(root), StandardCharsets.UTF_8);

                        // 返回用户信息
                        Map<String, Object> result = new HashMap<>();
                        result.put("id", getJsonString(admin, "id"));
                        result.put("username", storedUsername);
                        result.put("name", getJsonString(admin, "name"));
                        result.put("email", getJsonString(admin, "email"));
                        result.put("role", "ADMIN");
                        result.put("loginAt", loginTime);

                        ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                                ApiResponse.success("登录成功", result));
                        return;
                    }
                }
            }

            // 登录失败
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    ApiResponse.failure("用户名或密码错误"));

        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.failure("登录失败: " + e.getMessage()));
        }
    }

    /**
     * SHA-256 哈希密码 (与 AuthUtils.hashPassword 一致)
     */
    private String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPassword = salt + password;  // salt 在前，password 在后
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 初始化默认管理员账号（用于首次部署或数据丢失后恢复）
     */
    private void initAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // 检查是否已有管理员账号
            AdminAccountDao.ProfileResult checkResult = adminDao.getProfileSettings("ADMIN-00001");
            if (checkResult.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("message", "管理员账号已存在");
                data.put("adminId", "ADMIN-00001");
                data.put("username", "admin");
                data.put("password", "admin123");
                ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                        ApiResponse.success("管理员账号已存在", data));
                return;
            }

            // 创建默认管理员账号
            AdminAccountDao.AdminRegisterResult result = adminDao.register(
                    "ADMIN-00001",
                    "Administrator",
                    "admin",
                    "admin@bupt.edu.cn",
                    "",
                    "admin123",
                    "System Administration",
                    java.util.List.of("users:read", "users:write", "courses:read", "courses:write",
                            "notices:read", "notices:write", "settings:read", "settings:write")
            );

            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("adminId", "ADMIN-00001");
                data.put("username", "admin");
                data.put("password", "admin123");
                data.put("message", "请首次登录后立即修改密码");
                ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_CREATED,
                        ApiResponse.success("初始化成功", data));
            } else {
                ServletJsonResponseWriter.write(resp, result.getStatus(),
                        ApiResponse.failure(result.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.failure("初始化失败: " + e.getMessage()));
        }
    }

    /**
     * 安全获取 JSON 字符串值
     */
    private String getJsonString(JsonObject obj, String path) {
        String[] parts = path.split("\\.");
        JsonObject current = obj;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                return "";
            }
            current = current.getAsJsonObject(parts[i]);
        }
        String lastKey = parts[parts.length - 1];
        if (!current.has(lastKey)) {
            return "";
        }
        return current.get(lastKey).getAsString();
    }
}
