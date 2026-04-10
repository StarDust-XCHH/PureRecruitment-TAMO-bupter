
        package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.common.util.ServletJsonResponseWriter;
import com.bupt.tarecruit.mo.dao.MoAccountDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * MO 登录 Servlet。
 *
 * <p>处理教师用户的登录请求，支持多种标识符（MO ID、用户名、邮箱、手机号）。</p>
 *
 * <h3>URL 映射</h3>
 * <pre>{@code POST /api/mo/login}</pre>
 *
 * <h3>请求参数</h3>
 * <ul>
 *     <li>{@code identifier} - 标识符（必填）</li>
 *     <li>{@code password} - 密码（必填）</li>
 * </ul>
 *
 * <h3>响应示例</h3>
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "登录成功",
 *   "data": {
 *     "moId": "MO-10001",
 *     "name": "张老师",
 *     "username": "zhangsan",
 *     "email": "zhangsan@bupt.edu.cn",
 *     "isFirstLogin": false
 *   }
 * }
 * }</pre>
 *
 * @see MoAccountDao
 */
@WebServlet(name = "moLoginServlet", value = "/api/mo/login")
public class MoLoginServlet extends HttpServlet {
    private final MoAccountDao moAccountDao = new MoAccountDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String identifier = req.getParameter("identifier");
        String password = req.getParameter("password");

        // 调试日志
        System.out.println("\n>>>>>> [DEBUG] MO 登录 POST 请求 <<<<<<");
        System.out.println("Identifier: " + identifier);
        System.out.println("Password Length: " + (password != null ? password.length() : 0));

        try {
            MoAccountDao.MoLoginResult result = moAccountDao.login(identifier, password);

            System.out.println("MO 登录结果状态码：" + result.getStatus());
            System.out.println("MO 登录结果消息：" + result.getMessage());

            ApiResponse<?> response = result.isSuccess()
                    ? ApiResponse.success(result.getMessage(), result.getData())
                    : ApiResponse.failure(result.getMessage());

            ServletJsonResponseWriter.write(resp, result.getStatus(), response);
        } catch (Exception e) {
            System.err.println("[ERROR] MO 登录逻辑执行异常:");
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, 500,
                    ApiResponse.failure("服务器内部错误：" + e.getMessage()));
        }
        System.out.println(">>>>>> [DEBUG] 请求处理结束 <<<<<<\n");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("[DEBUG] MO 登录接口收到 GET 请求，只允许 POST");
        ServletJsonResponseWriter.write(resp, 405,
                ApiResponse.failure("请使用 POST 登录"));
    }
}
