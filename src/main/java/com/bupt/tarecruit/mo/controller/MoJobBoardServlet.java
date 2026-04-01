package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.mo.dao.MoRecruitmentDao;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "moJobBoardServlet", value = "/api/mo/jobs")
public class MoJobBoardServlet extends HttpServlet {
    private static final String JOB_BOARD_SCHEMA = "mo-ta-job-board";
    private static final String JOB_BOARD_VERSION = "2.0";
    private final MoRecruitmentDao recruitmentDao = new MoRecruitmentDao();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonObject payload = recruitmentDao.getPendingCourses();
            writeJson(resp, 200, payload);
        } catch (Exception ex) {
            writeJson(resp, 500, buildV2Error("读取 MO 岗位列表失败: " + ex.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonObject body = JsonParser.parseReader(req.getReader()).getAsJsonObject();
            String moName = body.has("moName") && !body.get("moName").isJsonNull()
                    ? body.get("moName").getAsString()
                    : "MO";
            JsonObject payload = recruitmentDao.createCourse(moName, body);
            writeJson(resp, 201, payload);
        } catch (IllegalStateException | ClassCastException ex) {
            writeJson(resp, 400, buildV2Error("请求体不是合法 JSON 对象"));
        } catch (IllegalArgumentException ex) {
            writeJson(resp, 400, buildV2Error(ex.getMessage()));
        } catch (Exception ex) {
            writeJson(resp, 500, buildV2Error("发布岗位失败: " + ex.getMessage()));
        }
    }

    private void writeJson(HttpServletResponse resp, int status, JsonObject payload) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(payload));
    }

    private JsonObject buildV2Error(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("schema", JOB_BOARD_SCHEMA);
        payload.addProperty("version", JOB_BOARD_VERSION);
        payload.addProperty("success", false);
        payload.addProperty("message", message);
        payload.add("items", new JsonArray());
        return payload;
    }
}
