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

        // 【后端控制台日志】
        System.out.println("\n>>>>>> [DEBUG] 收到登录 POST 请求 <<<<<<");
        System.out.println("Identifier: " + identifier);
        System.out.println("Password Length: " + (password != null ? password.length() : 0));

        try {
            TaAccountDao.TaLoginResult result = taAccountDao.login(identifier, password);

            System.out.println("登录结果状态码: " + result.getStatus());
            System.out.println("登录结果消息: " + result.getMessage());

            ApiResponse<?> response = result.isSuccess()
                    ? ApiResponse.success(result.getMessage(), result.getData())
                    : ApiResponse.failure(result.getMessage());

            ServletJsonResponseWriter.write(resp, result.getStatus(), response);
        } catch (Exception e) {
            System.err.println("[ERROR] 登录逻辑执行异常:");
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, 500, ApiResponse.failure("服务器内部错误: " + e.getMessage()));
        }
        System.out.println(">>>>>> [DEBUG] 请求处理结束 <<<<<<\n");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("[DEBUG] 收到 GET 请求，登录接口只允许 POST");
        ServletJsonResponseWriter.write(resp, 405, ApiResponse.failure("请使用 POST 登录"));
    }
}