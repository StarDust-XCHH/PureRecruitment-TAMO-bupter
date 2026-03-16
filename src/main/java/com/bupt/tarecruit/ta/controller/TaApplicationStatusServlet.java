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

@WebServlet(name = "taApplicationStatusServlet", value = "/api/ta/application-status")
public class TaApplicationStatusServlet extends HttpServlet {
    private final Gson gson = new Gson();
    private final TaAccountDao taAccountDao = new TaAccountDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String taId = trim(req.getParameter("taId"));
        try {
            TaAccountDao.ApplicationStatusResult result = taAccountDao.getApplicationStatus(taId);
            resp.setStatus(result.getStatus());
            try (PrintWriter writer = resp.getWriter()) {
                writer.write(buildResponse(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter writer = resp.getWriter()) {
                writer.write(buildErrorResponse("读取申请状态失败: " + e.getMessage()));
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            writer.write(buildErrorResponse("请使用 GET 请求读取申请状态"));
        }
    }

    private String buildResponse(TaAccountDao.ApplicationStatusResult result) {
        JsonArray items = result.getItems() == null ? new JsonArray() : result.getItems();
        JsonObject summary = result.getSummary() == null ? new JsonObject() : result.getSummary();
        JsonArray notifications = result.getMessages() == null ? new JsonArray() : result.getMessages();

        JsonObject payload = new JsonObject();
        payload.addProperty("success", result.isSuccess());
        payload.addProperty("message", result.getMessage());
        payload.add("items", items);
        payload.add("summary", summary);
        payload.add("notifications", notifications);
        payload.add("messages", notifications.deepCopy());
        payload.add("details", buildDetailsMap(items));
        return gson.toJson(payload);
    }

    private String buildErrorResponse(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("success", false);
        payload.addProperty("message", message == null ? "" : message);
        payload.add("items", new JsonArray());
        payload.add("summary", new JsonObject());
        payload.add("notifications", new JsonArray());
        payload.add("messages", new JsonArray());
        payload.add("details", new JsonObject());
        return gson.toJson(payload);
    }

    private JsonObject buildDetailsMap(JsonArray items) {
        JsonObject details = new JsonObject();
        if (items == null) {
            return details;
        }
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isJsonObject()) {
                continue;
            }
            JsonObject item = items.get(i).getAsJsonObject();
            String applicationId = trim(item.has("applicationId") && !item.get("applicationId").isJsonNull()
                    ? item.get("applicationId").getAsString()
                    : "");
            if (applicationId.isEmpty()) {
                continue;
            }
            details.add(applicationId, item.deepCopy());
        }
        return details;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
