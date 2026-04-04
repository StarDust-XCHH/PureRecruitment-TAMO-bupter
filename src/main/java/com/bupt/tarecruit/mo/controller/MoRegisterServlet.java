
        package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.common.util.ServletJsonResponseWriter;
import com.bupt.tarecruit.mo.dao.MoAccountDao;
import com.bupt.tarecruit.mo.model.MoRegisterRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * MO 注册 Servlet。
 *
 * <p>处理教师用户的注册请求，包括表单验证、唯一性检查和数据持久化。</p>
 *
 * <h3>URL 映射</h3>
 * <pre>{@code POST /api/mo/register}</pre>
 *
 * <h3>请求参数</h3>
 * <ul>
 *     <li>{@code moId} - MO ID（必填，自动添加 "MO-" 前缀）</li>
 *     <li>{@code name} - 姓名（必填）</li>
 *     <li>{@code username} - 用户名（必填）</li>
 *     <li>{@code email} - 邮箱（必填）</li>
 *     <li>{@code phone} - 手机号（必填）</li>
 *     <li>{@code password} - 密码（必填，至少 6 位）</li>
 *     <li>{@code confirmPassword} - 确认密码（必填）</li>
 * </ul>
 *
 * <h3>响应示例</h3>
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "注册成功",
 *   "data": {
 *     "moId": "MO-10001",
 *     "username": "zhangsan",
 *     "createdAt": "2026-04-02T10:00:00Z"
 *   }
 * }
 * }</pre>
 *
 * @see MoAccountDao
 * @see MoRegisterRequest
 */
@WebServlet(name = "moRegisterServlet", value = "/api/mo/register")
public class MoRegisterServlet extends HttpServlet {
    private final MoAccountDao moAccountDao = new MoAccountDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        ServletJsonResponseWriter.write(resp,
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                ApiResponse.failure("请使用 POST 请求"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String normalizedMoId = normalizeMoId(req.getParameter("moId"));

        MoRegisterRequest registerRequest = new MoRegisterRequest(
                normalizedMoId,
                req.getParameter("name"),
                req.getParameter("username"),
                req.getParameter("email"),
                req.getParameter("phone"),
                req.getParameter("password"),
                req.getParameter("confirmPassword")
        );

        // 表单验证
        ApiResponse<Void> validationResult = validate(registerRequest);
        if (!validationResult.isSuccess()) {
            ServletJsonResponseWriter.write(resp,
                    HttpServletResponse.SC_BAD_REQUEST,
                    validationResult);
            return;
        }

        // 执行注册
        MoAccountDao.MoRegisterResult result = moAccountDao.register(
                normalizedMoId,
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

    /**
     * 标准化 MO ID 格式。
     *
     * @param moId 原始 MO ID
     * @return 标准化后的 MO ID（格式：MO-XXXXX）
     */
    private String normalizeMoId(String moId) {
        if (moId == null || moId.trim().isEmpty()) {
            return "";
        }
        String trimmed = moId.trim();
        if (!trimmed.toUpperCase().startsWith("MO-")) {
            return "MO-" + trimmed.toUpperCase();
        }
        return trimmed.toUpperCase();
    }

    /**
     * 验证注册表单。
     *
     * @param request 注册请求
     * @return 验证结果
     */
    private ApiResponse<Void> validate(MoRegisterRequest request) {
        if (request.getMoId() == null || request.getMoId().trim().isEmpty()) {
            return ApiResponse.failure("MO ID 不能为空");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ApiResponse.failure("姓名不能为空");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ApiResponse.failure("用户名不能为空");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ApiResponse.failure("邮箱不能为空");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            return ApiResponse.failure("手机号不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ApiResponse.failure("密码至少需要 6 位");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.failure("两次输入的密码不一致");
        }
        return ApiResponse.success("验证通过", null);
    }
}

