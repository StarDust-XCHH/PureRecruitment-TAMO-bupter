package com.bupt.tarecruit.ta.controller;

import com.bupt.tarecruit.ta.dao.TaAccountDao;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet(name = "taJobBoardServlet", value = "/api/ta/jobs")
public class TaJobBoardServlet extends HttpServlet {
    private static final String JOB_BOARD_TAG = "[TA-JOB-BOARD]";
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Gson gson = new Gson();
    private final TaAccountDao taAccountDao = new TaAccountDao();

    @Override
    public void init() throws ServletException {
        System.out.println(buildLogLine("INIT", "mapping=/api/ta/jobs, dataRoot=" + TaAccountDao.getResolvedDataRoot()));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        long startTime = System.currentTimeMillis();
        logInfo("REQUEST", "method=" + request.getMethod() + ", requestURI=" + request.getRequestURI());

        try (PrintWriter writer = response.getWriter()) {
            JsonObject payload = taAccountDao.getPendingJobBoardData();
            if (payload == null || !payload.has("items") || !payload.get("items").isJsonArray()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writer.write(buildErrorResponse("暂无开放招聘的岗位数据或数据文件不存在"));
                logInfo("RESPONSE", "status=" + response.getStatus() + ", durationMs=" + (System.currentTimeMillis() - startTime));
                return;
            }

            response.setStatus(HttpServletResponse.SC_OK);
            writer.write(gson.toJson(payload));
            logInfo("RESPONSE", "status=" + response.getStatus()
                    + ", items=" + payload.getAsJsonArray("items").size()
                    + ", durationMs=" + (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            logError("ERROR", e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(buildErrorResponse("服务器内部错误，读取岗位数据失败"));
            }
        }
    }

    private void logInfo(String phase, String message) {
        System.out.println(buildLogLine(phase, message));
    }

    private void logError(String phase, String message) {
        System.err.println(buildLogLine(phase, message));
    }

    private String buildLogLine(String phase, String message) {
        return LocalDateTime.now().format(LOG_TIME_FORMATTER) + " " + JOB_BOARD_TAG + " [" + phase + "] " + message;
    }

    private String buildErrorResponse(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("schema", "error");
        payload.addProperty("success", false);
        payload.addProperty("message", message);
        payload.add("items", new JsonArray());
        return gson.toJson(payload).getBytes(StandardCharsets.UTF_8).length > 0 ? gson.toJson(payload) : "{\"schema\":\"error\",\"success\":false,\"message\":\"" + message + "\",\"items\":[]}";
    }
}
