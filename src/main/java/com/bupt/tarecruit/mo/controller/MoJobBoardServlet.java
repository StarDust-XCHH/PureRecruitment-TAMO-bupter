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

@WebServlet(name = "moJobBoardServlet", value = "/api/mo/jobs")
public class MoJobBoardServlet extends HttpServlet {
    private final MoRecruitmentDao recruitmentDao = new MoRecruitmentDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JsonObject payload = recruitmentDao.getPendingCourses();
            writeJson(resp, 200, payload);
        } catch (Exception ex) {
            writeJson(resp, 500, gson.toJsonTree(ApiResponse.failure("读取 MO 岗位列表失败: " + ex.getMessage())).getAsJsonObject());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JsonObject body = JsonParser.parseReader(req.getReader()).getAsJsonObject();
            String moName = body.has("moName") && !body.get("moName").isJsonNull()
                    ? body.get("moName").getAsString()
                    : "MO";
            JsonObject payload = recruitmentDao.createCourse(moName, body);
            writeJson(resp, 201, payload);
        } catch (IllegalArgumentException ex) {
            writeJson(resp, 400, gson.toJsonTree(ApiResponse.failure(ex.getMessage())).getAsJsonObject());
        } catch (Exception ex) {
            writeJson(resp, 500, gson.toJsonTree(ApiResponse.failure("发布岗位失败: " + ex.getMessage())).getAsJsonObject());
        }
    }

    private void writeJson(HttpServletResponse resp, int status, JsonObject payload) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(payload));
    }
}
