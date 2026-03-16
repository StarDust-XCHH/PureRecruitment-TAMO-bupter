package com.bupt.tarecruit.ta.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.common.util.ServletJsonResponseWriter;
import com.bupt.tarecruit.ta.dao.TaAccountDao;
import com.bupt.tarecruit.ta.model.TaRegisterRequest;
import com.bupt.tarecruit.ta.service.TaRegistrationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "taRegisterServlet", value = "/api/ta/register")
public class TaRegisterServlet extends HttpServlet {
    private final TaRegistrationService registrationService = new TaRegistrationService();
    private final TaAccountDao taAccountDao = new TaAccountDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, ApiResponse.failure("请使用 POST 请求"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String normalizedTaId = registrationService.normalizeTaId(req.getParameter("taId"));
        TaRegisterRequest registerRequest = new TaRegisterRequest(
                normalizedTaId,
                req.getParameter("name"),
                req.getParameter("username"),
                req.getParameter("email"),
                req.getParameter("phone"),
                req.getParameter("password"),
                req.getParameter("confirmPassword")
        );

        ApiResponse<Void> validationResult = registrationService.validate(registerRequest);
        if (!validationResult.isSuccess()) {
            ServletJsonResponseWriter.write(resp, HttpServletResponse.SC_BAD_REQUEST, validationResult);
            return;
        }

        TaAccountDao.TaRegisterResult result = taAccountDao.register(
                normalizedTaId,
                registerRequest.getName().trim(),
                registerRequest.getUsername().trim(),
                registerRequest.getEmail().trim(),
                registerRequest.getPhone().trim(),
                registerRequest.getPassword()
        );

        ApiResponse<?> response = result.isSuccess()
                ? ApiResponse.success(result.getMessage(), result.getData())
                : ApiResponse.failure(result.getMessage());
        ServletJsonResponseWriter.write(resp, result.getStatus(), response);
    }
}
