package com.bupt.tarecruit.admin.controller;

import com.bupt.tarecruit.admin.dao.AdminDataDao;
import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.common.util.ServletJsonResponseWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin 数据管理 Servlet。
 * 提供用户数据查询接口。
 */
@WebServlet("/api/admin/users/*")
public class AdminUserServlet extends HttpServlet {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final AdminDataDao adminDataDao = new AdminDataDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        // /api/admin/users - 获取所有用户
        if (pathInfo == null || pathInfo.equals("/")) {
            getAllUsers(req, resp);
        }
        // /api/admin/users/stats - 获取统计信息
        else if (pathInfo.equals("/stats")) {
            getUserStats(req, resp);
        }
        // /api/admin/users/{id} - 获取单个用户
        else if (pathInfo.matches("/[A-Za-z0-9-]+")) {
            getUserById(req, resp, pathInfo.substring(1));
        }
        else {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                    ApiResponse.failure("未找到该接口"));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        // /api/admin/users/{id} - 更新用户
        if (pathInfo != null && pathInfo.matches("/[A-Za-z0-9-]+")) {
            updateUser(req, resp, pathInfo.substring(1));
        } else {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                    ApiResponse.failure("未找到该接口"));
        }
    }

    /**
     * 获取单个用户详情
     */
    private void getUserById(HttpServletRequest req, HttpServletResponse resp, String userId) throws IOException {
        try {
            String role = req.getParameter("role");
            Map<String, Object> user = null;

            if ("TA".equalsIgnoreCase(role)) {
                user = adminDataDao.getTaUsersWithSettings().stream()
                        .filter(u -> userId.equalsIgnoreCase(String.valueOf(u.getOrDefault("id", ""))))
                        .findFirst().orElse(null);
            } else if ("MO".equalsIgnoreCase(role)) {
                user = adminDataDao.getMoUsersWithProfiles().stream()
                        .filter(u -> userId.equalsIgnoreCase(String.valueOf(u.getOrDefault("id", ""))))
                        .findFirst().orElse(null);
            } else if ("ADMIN".equalsIgnoreCase(role)) {
                user = adminDataDao.getAdminUsersWithProfiles().stream()
                        .filter(u -> userId.equalsIgnoreCase(String.valueOf(u.getOrDefault("id", ""))))
                        .findFirst().orElse(null);
            } else {
                // 尝试在所有用户中查找
                user = adminDataDao.getAllUsers().stream()
                        .filter(u -> userId.equalsIgnoreCase(String.valueOf(u.getOrDefault("id", ""))))
                        .findFirst().orElse(null);
            }

            if (user == null) {
                ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                        ApiResponse.failure("用户不存在"));
                return;
            }

            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                    ApiResponse.success("获取用户成功", user));

        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.failure("获取用户失败: " + e.getMessage()));
        }
    }

    /**
     * 更新用户信息
     */
    private void updateUser(HttpServletRequest req, HttpServletResponse resp, String userId) throws IOException {
        try {
            String body = new String(req.getInputStream().readAllBytes(), "UTF-8");
            Map<String, Object> updates = GSON.fromJson(body, Map.class);

            if (updates == null || updates.isEmpty()) {
                ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_BAD_REQUEST,
                        ApiResponse.failure("请提供要更新的字段"));
                return;
            }

            // 移除不允许前端直接修改的字段
            updates.remove("id");
            updates.remove("createdAt");
            updates.remove("updatedAt");
            updates.remove("auth");
            updates.remove("passwordHash");
            updates.remove("passwordSalt");

            // 确定用户类型
            String role = (String) updates.remove("role");

            Map<String, Object> updatedUser = null;
            if ("TA".equalsIgnoreCase(role)) {
                updatedUser = adminDataDao.updateTaUser(userId, updates);
            } else if ("MO".equalsIgnoreCase(role)) {
                updatedUser = adminDataDao.updateMoUser(userId, updates);
            } else if ("ADMIN".equalsIgnoreCase(role)) {
                updatedUser = adminDataDao.updateAdminUser(userId, updates);
            } else {
                // 尝试自动识别用户类型
                List<Map<String, Object>> allUsers = adminDataDao.getAllUsers();
                String detectedRole = null;
                for (Map<String, Object> u : allUsers) {
                    if (userId.equalsIgnoreCase(String.valueOf(u.getOrDefault("id", "")))) {
                        detectedRole = String.valueOf(u.getOrDefault("role", ""));
                        break;
                    }
                }

                if (detectedRole == null) {
                    ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                            ApiResponse.failure("用户不存在"));
                    return;
                }

                switch (detectedRole) {
                    case "TA" -> updatedUser = adminDataDao.updateTaUser(userId, updates);
                    case "MO" -> updatedUser = adminDataDao.updateMoUser(userId, updates);
                    case "ADMIN" -> updatedUser = adminDataDao.updateAdminUser(userId, updates);
                }
            }

            if (updatedUser == null) {
                ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                        ApiResponse.failure("用户不存在"));
                return;
            }

            // 添加角色信息
            updatedUser.put("role", role != null ? role :
                    (userId.startsWith("TA-") ? "TA" : userId.startsWith("MO-") ? "MO" : "ADMIN"));

            // 移除敏感信息
            Object auth = updatedUser.remove("auth");
            if (auth instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> authMap = (Map<String, Object>) auth;
                updatedUser.put("lastLoginAt", authMap.getOrDefault("lastLoginAt", ""));
                updatedUser.put("failedAttempts", authMap.getOrDefault("failedAttempts", 0));
            }

            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                    ApiResponse.success("更新成功", updatedUser));

        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.failure("更新用户失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有用户列表（TA + MO）
     */
    private void getAllUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String roleFilter = req.getParameter("role");
            List<Map<String, Object>> allUsers;

            // 根据角色筛选
            if ("TA".equalsIgnoreCase(roleFilter)) {
                allUsers = adminDataDao.getTaUsersWithProfiles();
            } else if ("MO".equalsIgnoreCase(roleFilter)) {
                allUsers = adminDataDao.getMoUsersWithProfiles();
            } else if ("ADMIN".equalsIgnoreCase(roleFilter)) {
                allUsers = adminDataDao.getAdminUsersWithProfiles();
            } else {
                allUsers = adminDataDao.getAllUsers();
            }

            // 转换为前端需要的格式
            List<Map<String, Object>> result = allUsers.stream().map(user -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", user.getOrDefault("id", ""));
                item.put("username", user.getOrDefault("username", ""));
                item.put("name", user.getOrDefault("name", user.getOrDefault("realName", "")));
                item.put("email", user.getOrDefault("email", user.getOrDefault("contactEmail", "")));
                item.put("phone", user.getOrDefault("phone", ""));
                item.put("status", user.getOrDefault("status", "active"));
                item.put("lastLoginAt", user.getOrDefault("lastLoginAt", ""));
                item.put("role", user.getOrDefault("role", "TA"));
                item.put("title", user.getOrDefault("title", ""));
                item.put("bio", user.getOrDefault("bio", ""));
                item.put("theme", user.getOrDefault("theme", "dark"));
                item.put("profileSaved", user.getOrDefault("profileSaved", false));
                return item;
            }).toList();

            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                    ApiResponse.success("获取用户列表成功", result));

        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.failure("获取用户列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户统计信息
     */
    private void getUserStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> stats = adminDataDao.getUserStats();

            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                    ApiResponse.success("获取统计数据成功", stats));

        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.failure("获取统计数据失败: " + e.getMessage()));
        }
    }
}
