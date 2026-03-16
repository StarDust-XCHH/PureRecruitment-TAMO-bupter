package com.bupt.tarecruit.ta.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.common.util.ServletJsonResponseWriter;
import com.bupt.tarecruit.ta.dao.TaAccountDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "taLoginServlet", value = "/api/ta/login")
public class TaLoginServlet extends HttpServlet {
    private final TaAccountDao taAccountDao = new TaAccountDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, ApiResponse.failure("请使用 POST 请求"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String identifier = req.getParameter("identifier");
        String password = req.getParameter("password");

        TaAccountDao.TaLoginResult result = taAccountDao.login(identifier, password);
        ApiResponse<?> response = result.isSuccess()
                ? ApiResponse.success(result.getMessage(), result.getData())
                : ApiResponse.failure(result.getMessage());
        ServletJsonResponseWriter.write(resp, result.getStatus(), response);
    }
}
