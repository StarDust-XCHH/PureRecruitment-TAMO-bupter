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
 * 当前 MO 名下岗位中，尚未标记已读的申请数量（侧栏红点）。
 */
@WebServlet(name = "moApplicantsUnreadServlet", value = "/api/mo/applicants/unread-count")
public class MoApplicantsUnreadServlet extends HttpServlet {

    private final MoRecruitmentDao recruitmentDao = new MoRecruitmentDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String moId = trim(req.getParameter("moId"));
        if (moId.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(ApiResponse.failure("缺少 moId")));
            return;
        }
        try {
            int n = recruitmentDao.countUnreadApplicantsForMo(moId);
            JsonObject payload = new JsonObject();
            payload.addProperty("success", true);
            payload.addProperty("moId", moId);
            payload.addProperty("unreadCount", n);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(payload));
        } catch (Exception ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(ApiResponse.failure("统计未读失败: " + ex.getMessage())));
        }
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
