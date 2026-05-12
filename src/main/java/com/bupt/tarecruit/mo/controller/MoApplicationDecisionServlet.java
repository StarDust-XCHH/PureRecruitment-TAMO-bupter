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

@WebServlet(name = "moApplicationDecisionServlet", value = "/api/mo/applications/select")
public class MoApplicationDecisionServlet extends HttpServlet {
    private final MoRecruitmentDao recruitmentDao = new MoRecruitmentDao();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JsonObject body = JsonParser.parseReader(req.getReader()).getAsJsonObject();
            String jobId = body.has("jobId") && !body.get("jobId").isJsonNull()
                    ? body.get("jobId").getAsString().trim()
                    : "";
            String courseCode = body.has("courseCode") && !body.get("courseCode").isJsonNull()
                    ? body.get("courseCode").getAsString()
                    : "";
            String taId = body.has("taId") && !body.get("taId").isJsonNull()
                    ? body.get("taId").getAsString()
                    : "";
            String decision = body.has("decision") && !body.get("decision").isJsonNull()
                    ? body.get("decision").getAsString()
                    : "";
            String comment = body.has("comment") && !body.get("comment").isJsonNull()
                    ? body.get("comment").getAsString()
                    : "";
            String moId = body.has("moId") && !body.get("moId").isJsonNull()
                    ? body.get("moId").getAsString().trim()
                    : "";

            if (jobId.isBlank()) {
                writeJson(resp, 400, gson.toJsonTree(ApiResponse.failure("缺少 jobId")).getAsJsonObject());
                return;
            }

            JsonObject payload = recruitmentDao.decideApplicationForJob(jobId, courseCode, taId, moId, decision, comment);
            writeJson(resp, 200, payload);
        } catch (IllegalArgumentException ex) {
            writeJson(resp, 400, gson.toJsonTree(ApiResponse.failure(ex.getMessage())).getAsJsonObject());
        } catch (Exception ex) {
            writeJson(resp, 500, gson.toJsonTree(ApiResponse.failure("保存筛选结果失败: " + ex.getMessage())).getAsJsonObject());
        }
    }

    private void writeJson(HttpServletResponse resp, int status, JsonObject payload) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(payload));
    }
}
