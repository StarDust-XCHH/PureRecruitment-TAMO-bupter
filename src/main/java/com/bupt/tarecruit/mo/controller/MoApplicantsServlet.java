package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.mo.dao.MoRecruitmentDao;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
        String courseCode = req.getParameter("courseCode");
        if (courseCode == null || courseCode.trim().isBlank()) {
            writeJson(resp, 400, gson.toJsonTree(ApiResponse.failure("Missing courseCode parameter")).getAsJsonObject());
            return;
        }

        try {
            JsonArray items = recruitmentDao.getApplicantsForCourse(courseCode);
            JsonObject payload = new JsonObject();
            payload.addProperty("success", true);
            payload.addProperty("courseCode", courseCode);
            payload.addProperty("count", items.size());
            payload.add("items", items);
            writeJson(resp, 200, payload);
        } catch (Exception ex) {
            writeJson(resp, 500, gson.toJsonTree(ApiResponse.failure("Failed to load applicants: " + ex.getMessage())).getAsJsonObject());
        }
    }

    private void writeJson(HttpServletResponse resp, int status, JsonObject payload) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(payload));
    }
}
