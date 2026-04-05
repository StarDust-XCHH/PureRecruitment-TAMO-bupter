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

/**
 * MO 查看单条 TA 申请详情（档案快照、事件、评论、决策摘要）。
 */
@WebServlet(name = "moApplicantDetailServlet", value = "/api/mo/applicants/detail")
public class MoApplicantDetailServlet extends HttpServlet {

    private final MoRecruitmentDao recruitmentDao = new MoRecruitmentDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String moId = trim(req.getParameter("moId"));
        String applicationId = trim(req.getParameter("applicationId"));
        if (moId.isEmpty() || applicationId.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(ApiResponse.failure("缺少 moId 或 applicationId")));
            return;
        }
        try {
            JsonObject detail = recruitmentDao.getApplicantDetail(moId, applicationId);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(detail));
        } catch (IllegalArgumentException ex) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(ApiResponse.failure(ex.getMessage())));
        } catch (Exception ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(ApiResponse.failure("读取详情失败: " + ex.getMessage())));
        }
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
