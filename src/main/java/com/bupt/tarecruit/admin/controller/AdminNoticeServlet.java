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
        addNotice("系统维护通知", "系统将于今晚 22:00-24:00 进行维护升级，请提前保存工作。", "high");
        addNotice("春季学期 TA 招募开始", "春季学期 TA 招募已正式开始，请各位 MO 及时发布岗位信息。", "medium");
        addNotice("新功能上线", "管理员后台新增数据统计功能，欢迎体验反馈。", "low");
    }

    private static void addNotice(String title, String content, String priority) {
        Map<String, Object> notice = new HashMap<>();
        notice.put("id", nextId);
        notice.put("title", title);
        notice.put("content", content);
        notice.put("priority", priority);
        notice.put("author", "admin");
        notice.put("authorName", "管理员");
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
                    ApiResponse.error("未找到该接口"));
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
                    ApiResponse.error("未找到该接口"));
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
                ApiResponse.error("未找到该公告"));
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
                ApiResponse.success(result));
    }

    private void createNotice(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = new String(req.getInputStream().readAllBytes(), "UTF-8");
        Map<String, Object> data = GSON.fromJson(body, Map.class);

        String title = (String) data.getOrDefault("title", "");
        String content = (String) data.getOrDefault("content", "");
        String priority = (String) data.getOrDefault("priority", "medium");

        if (title.isEmpty() || content.isEmpty()) {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_BAD_REQUEST,
                    ApiResponse.error("标题和内容不能为空"));
            return;
        }

        Map<String, Object> notice = new HashMap<>();
        notice.put("id", nextId);
        notice.put("title", title);
        notice.put("content", content);
        notice.put("priority", priority);
        notice.put("author", "admin");
        notice.put("authorName", "管理员");
        notice.put("createdAt", java.time.LocalDateTime.now().toString());

        notices.put(nextId++, notice);

        ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_CREATED,
                ApiResponse.success(notice));
    }

    private void deleteNotice(int id, HttpServletResponse resp) throws IOException {
        if (notices.containsKey(id)) {
            notices.remove(id);
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                    ApiResponse.success("删除成功"));
        } else {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                    ApiResponse.error("公告不存在"));
        }
    }
}
