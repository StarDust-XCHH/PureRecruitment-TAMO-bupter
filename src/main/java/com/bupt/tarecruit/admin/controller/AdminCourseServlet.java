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
 * Admin 课程管理 Servlet。
 * 提供课程数据查询接口。
 */
@WebServlet("/api/admin/courses/*")
public class AdminCourseServlet extends HttpServlet {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final AdminDataDao adminDataDao = new AdminDataDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        // /api/admin/courses - 获取所有课程
        if (pathInfo == null || pathInfo.equals("/")) {
            getAllCourses(req, resp);
        }
        // /api/admin/courses/stats - 获取课程统计
        else if (pathInfo.equals("/stats")) {
            getCourseStats(req, resp);
        }
        else {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_NOT_FOUND,
                    ApiResponse.failure("未找到该接口"));
        }
    }

    /**
     * 获取所有课程列表
     */
    private void getAllCourses(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<Map<String, Object>> courses = adminDataDao.getAllCourses();

            // 转换为前端需要的格式
            List<Map<String, Object>> result = courses.stream().map(course -> {
                Map<String, Object> item = new HashMap<>();
                item.put("courseId", course.getOrDefault("courseId", ""));
                item.put("courseName", course.getOrDefault("courseName", ""));
                item.put("semester", course.getOrDefault("semester", ""));
                item.put("moId", course.getOrDefault("moId", ""));
                item.put("moName", course.getOrDefault("moName", ""));
                item.put("status", course.getOrDefault("status", ""));
                item.put("studentCount", course.getOrDefault("studentCount", 0));
                item.put("taRecruitCount", course.getOrDefault("taRecruitCount", 0));
                item.put("campus", course.getOrDefault("campus", ""));
                item.put("jobId", course.getOrDefault("jobId", ""));
                item.put("applicationDeadline", course.getOrDefault("applicationDeadline", ""));
                item.put("skills", course.getOrDefault("skills", List.of()));
                item.put("applicationsTotal", course.getOrDefault("applicationsTotal", 0));
                item.put("applicationsPending", course.getOrDefault("applicationsPending", 0));
                item.put("applicationsAccepted", course.getOrDefault("applicationsAccepted", 0));
                item.put("recruitedCount", course.getOrDefault("recruitedCount", 0));
                item.put("createdAt", course.getOrDefault("createdAt", ""));
                return item;
            }).toList();

            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                    ApiResponse.success("获取课程列表成功", result));

        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.failure("获取课程列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取课程统计信息
     */
    private void getCourseStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> stats = adminDataDao.getCourseStats();

            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_OK,
                    ApiResponse.success("获取统计数据成功", stats));

        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.failure("获取统计数据失败: " + e.getMessage()));
        }
    }
}
