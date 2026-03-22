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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String identifier = req.getParameter("identifier");
        String password = req.getParameter("password");

        // Backend debug logging
        System.out.println("\n>>>>>> [DEBUG] Login POST received <<<<<<");
        System.out.println("Identifier: " + identifier);
        System.out.println("Password Length: " + (password != null ? password.length() : 0));

        try {
            TaAccountDao.TaLoginResult result = taAccountDao.login(identifier, password);

            System.out.println("Login result status: " + result.getStatus());
            System.out.println("Login result message: " + result.getMessage());

            ApiResponse<?> response = result.isSuccess()
                    ? ApiResponse.success(result.getMessage(), result.getData())
                    : ApiResponse.failure(result.getMessage());

            ServletJsonResponseWriter.write(resp, result.getStatus(), response);
        } catch (Exception e) {
            System.err.println("[ERROR] Login handler failed:");
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, 500, ApiResponse.failure("Server error: " + e.getMessage()));
        }
        System.out.println(">>>>>> [DEBUG] Request finished <<<<<<\n");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("[DEBUG] GET is not allowed for login; use POST");
        ServletJsonResponseWriter.write(resp, 405, ApiResponse.failure("Use POST to sign in"));
    }
}