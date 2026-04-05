package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.mo.dao.MoRecruitmentDao;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * MO 对某条申请添加评论（存于 {@code mo-application-comments.json}）。
 */
@WebServlet(name = "moApplicationCommentServlet", value = "/api/mo/applications/comment")
public class MoApplicationCommentServlet extends HttpServlet {

    private final MoRecruitmentDao recruitmentDao = new MoRecruitmentDao();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        try {
            JsonObject body = JsonParser.parseReader(req.getReader()).getAsJsonObject();
            String moId = readString(body, "moId");
            String applicationId = readString(body, "applicationId");
            String text = readString(body, "text");
            if (moId.isEmpty() || applicationId.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(gson.toJson(ApiResponse.failure("缺少 moId 或 applicationId")));
                return;
            }
            JsonObject result = recruitmentDao.addApplicationComment(moId, applicationId, text);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(result));
        } catch (IllegalArgumentException ex) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(gson.toJson(ApiResponse.failure(ex.getMessage())));
        } catch (Exception ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(ApiResponse.failure("保存评论失败: " + ex.getMessage())));
        }
    }

    private static String readString(JsonObject body, String key) {
        if (body == null || !body.has(key) || body.get(key).isJsonNull()) {
            return "";
        }
        return body.get(key).getAsString().trim();
    }
}
