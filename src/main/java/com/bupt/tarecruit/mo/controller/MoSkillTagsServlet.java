package com.bupt.tarecruit.mo.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "moSkillTagsServlet", value = {"/api/mo/skill-tags", "/api/common/skill-tags"})
public class MoSkillTagsServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("schema", "skill-tags");
        payload.addProperty("version", "1.0");
        payload.add("items", buildSkillTags());
        writeJson(resp, payload);
    }

    private JsonArray buildSkillTags() {
        JsonArray tags = new JsonArray();
        tags.add("Python");
        tags.add("Java");
        tags.add("C/C++");
        tags.add("JavaScript");
        tags.add("TypeScript");
        tags.add("SQL");
        tags.add("Linux");
        tags.add("Git");
        tags.add("Data Structures");
        tags.add("Algorithms");
        tags.add("Machine Learning");
        tags.add("Computer Networks");
        tags.add("Operating Systems");
        tags.add("Database");
        tags.add("Software Engineering");
        tags.add("Web Development");
        return tags;
    }

    private void writeJson(HttpServletResponse resp, JsonObject payload) throws IOException {
        resp.setStatus(200);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(payload));
    }
}
