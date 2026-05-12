package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.mo.dao.MoRecruitmentDao;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "moApplicantsServlet", value = "/api/mo/applicants")
public class MoApplicantsServlet extends HttpServlet {
    private final MoRecruitmentDao recruitmentDao = new MoRecruitmentDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String jobId = req.getParameter("jobId") == null ? "" : req.getParameter("jobId").trim();
        String courseCode = req.getParameter("courseCode") == null ? "" : req.getParameter("courseCode").trim();
        String moId = req.getParameter("moId") == null ? "" : req.getParameter("moId").trim();
        if (moId.isBlank()) {
            writeJson(resp, 400, gson.toJsonTree(ApiResponse.failure("缺少 moId 参数")).getAsJsonObject());
            return;
        }

        try {
            JsonObject payload;
            if (jobId.isBlank() && courseCode.isBlank()) {
                payload = recruitmentDao.getApplicantsForAllMoCourses(moId);
            } else if (!jobId.isBlank()) {
                payload = recruitmentDao.getApplicantsForJob(jobId, moId, courseCode);
            } else {
                writeJson(resp, 400, gson.toJsonTree(ApiResponse.failure("按岗位筛选时必须提供 jobId（不可仅使用 courseCode）")).getAsJsonObject());
                return;
            }
            writeJson(resp, 200, payload);
        } catch (IllegalArgumentException ex) {
            writeJson(resp, 403, gson.toJsonTree(ApiResponse.failure(ex.getMessage())).getAsJsonObject());
        } catch (Exception ex) {
            writeJson(resp, 500, gson.toJsonTree(ApiResponse.failure("读取应聘者数据失败: " + ex.getMessage())).getAsJsonObject());
        }
    }

    private void writeJson(HttpServletResponse resp, int status, JsonObject payload) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(payload));
    }
}
