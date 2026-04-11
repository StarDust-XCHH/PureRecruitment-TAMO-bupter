package com.bupt.tarecruit.admin.controller;

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
import java.util.Map;

/**
 * Admin 公告管理 Servlet。
 * 提供公告 CRUD 接口。
 * 当前使用内存存储，后续可扩展为文件存储。
 */
@WebServlet("/api/admin/notices/*")
public class AdminNoticeServlet extends HttpServlet {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    // 内存存储，正式环境应使用数据库或文件
    private static final Map<Integer, Map<String, Object>> notices = new HashMap<>();
    private static int nextId = 1;

    static {
        // 初始化示例数据
        addNotice("System Maintenance Notice", "System will be under maintenance from 22:00-24:00 tonight. Please save your work in advance.", "high");
        addNotice("Spring Semester TA Recruitment Started", "Spring semester TA recruitment is now officially open. Please post job information promptly.", "medium");
        addNotice("New Feature Released", "Admin dashboard now includes data statistics feature. Welcome to try and share feedback.", "low");
    }

    private static void addNotice(String title, String content, String priority) {
        Map<String, Object> notice = new HashMap<>();
        notice.put("id", nextId);
        notice.put("title", title);
        notice.put("content", content);
        notice.put("priority", priority);
        notice.put("author", "admin");
        notice.put("authorName", "Admin");
        notice.put("createdAt", java.time.LocalDateTime.now().toString());
        notices.put(nextId++, notice);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        // /api/admin/notices - 获取所有公告
        if (pathInfo == null || pathInfo.equals("/")) {
            getAllNotices(req, resp);
        }
        else {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                    ApiResponse.failure("未找到该接口"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            createNotice(req, resp);
        }
        else {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                    ApiResponse.failure("未找到该接口"));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.startsWith("/")) {
            String idStr = pathInfo.substring(1);
            try {
                int id = Integer.parseInt(idStr);
                deleteNotice(id, resp);
                return;
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                ApiResponse.failure("未找到该公告"));
    }

    private void getAllNotices(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String limitStr = req.getParameter("limit");
        int limit = limitStr != null ? Integer.parseInt(limitStr) : Integer.MAX_VALUE;

        var result = notices.values().stream()
                .sorted((a, b) -> {
                    Integer idA = (Integer) a.get("id");
                    Integer idB = (Integer) b.get("id");
                    return idB.compareTo(idA); // 降序排列
                })
                .limit(limit)
                .toList();

        ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                ApiResponse.success("获取公告列表成功", result));
    }

    private void createNotice(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = new String(req.getInputStream().readAllBytes(), "UTF-8");
        Map<String, Object> data = GSON.fromJson(body, Map.class);

        String title = (String) data.getOrDefault("title", "");
        String content = (String) data.getOrDefault("content", "");
        String priority = (String) data.getOrDefault("priority", "medium");

        if (title.isEmpty() || content.isEmpty()) {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_BAD_REQUEST,
                    ApiResponse.failure("标题和内容不能为空"));
            return;
        }

        Map<String, Object> notice = new HashMap<>();
        notice.put("id", nextId);
        notice.put("title", title);
        notice.put("content", content);
        notice.put("priority", priority);
        notice.put("author", "admin");
        notice.put("authorName", "Admin");
        notice.put("createdAt", java.time.LocalDateTime.now().toString());

        notices.put(nextId++, notice);

        ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_CREATED,
                ApiResponse.success("Notice created successfully", notice));
    }

    private void deleteNotice(int id, HttpServletResponse resp) throws IOException {
        if (notices.containsKey(id)) {
            notices.remove(id);
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                    ApiResponse.success("删除成功", null));
        } else {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                    ApiResponse.failure("公告不存在"));
        }
    }
}
