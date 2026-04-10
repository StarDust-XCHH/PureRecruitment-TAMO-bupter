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
        else {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                    ApiResponse.error("未找到该接口"));
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
                    ApiResponse.success(result));

        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.error("获取用户列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户统计信息
     */
    private void getUserStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> stats = adminDataDao.getUserStats();

            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                    ApiResponse.success(stats));

        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.error("获取统计数据失败: " + e.getMessage()));
        }
    }
}
