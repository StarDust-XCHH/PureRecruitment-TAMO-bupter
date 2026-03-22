package com.bupt.tarecruit.ta.service;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.ta.model.TaRegisterRequest;

import java.util.regex.Pattern;

public class TaRegistrationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{7,15}$");

    public ApiResponse<Void> validate(TaRegisterRequest request) {
        if (request == null) {
            return ApiResponse.failure("Registration payload is required");
        }

        String taId = trim(request.getTaId());
        String name = trim(request.getName());
        String username = trim(request.getUsername());
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        String password = request.getPassword() == null ? "" : request.getPassword();
        String confirmPassword = request.getConfirmPassword() == null ? "" : request.getConfirmPassword();

        if (taId.isEmpty()) {
            return ApiResponse.failure("TA ID is missing—reopen the registration panel and try again");
        }

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            return ApiResponse.failure("Name, username, email, and phone are required");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ApiResponse.failure("Invalid email format");
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return ApiResponse.failure("Invalid phone number format");
        }

        if (password.length() < 6) {
            return ApiResponse.failure("Password must be at least 6 characters");
        }

        if (!password.equals(confirmPassword)) {
            return ApiResponse.failure("Passwords do not match");
        }

        return ApiResponse.success("Validation passed", null);
    }

    public String normalizeTaId(String taId) {
        String normalized = trim(taId).toUpperCase();
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.startsWith("TA-")) {
            return normalized;
        }
        return "TA-" + normalized.replaceAll("^TA", "");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
