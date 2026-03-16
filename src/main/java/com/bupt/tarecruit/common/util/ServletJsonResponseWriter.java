package com.bupt.tarecruit.common.util;

import com.bupt.tarecruit.common.model.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class ServletJsonResponseWriter {
    private ServletJsonResponseWriter() {
    }

    public static void write(HttpServletResponse response, int status, ApiResponse<?> apiResponse) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(JsonUtils.toJson(apiResponse));
    }
}
