package com.bupt.tarecruit.ta.service;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.ta.model.TaRegisterRequest;

import java.util.regex.Pattern;

public class TaRegistrationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{7,15}$");

    public ApiResponse<Void> validate(TaRegisterRequest request) {
        if (request == null) {
            return ApiResponse.failure("注册信息不能为空");
        }

        String name = trim(request.getName());
        String username = trim(request.getUsername());
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        String password = request.getPassword() == null ? "" : request.getPassword();
        String confirmPassword = request.getConfirmPassword() == null ? "" : request.getConfirmPassword();

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            return ApiResponse.failure("姓名、用户名、邮箱、手机号不能为空");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ApiResponse.failure("邮箱格式不正确");
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return ApiResponse.failure("手机号格式不正确");
        }

        if (password.length() < 6) {
            return ApiResponse.failure("密码至少 6 位");
        }

        if (!password.equals(confirmPassword)) {
            return ApiResponse.failure("两次输入的密码不一致");
        }

        return ApiResponse.success("校验通过", null);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
