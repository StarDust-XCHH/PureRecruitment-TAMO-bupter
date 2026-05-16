package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.common.dao.ModulesCatalogDao;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "moModulesCatalogServlet", value = {"/api/mo/modules-catalog", "/api/common/modules-catalog"})
public class MoModulesCatalogServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject payload = ModulesCatalogDao.readCatalog();
        resp.setStatus(200);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(payload));
    }
}
