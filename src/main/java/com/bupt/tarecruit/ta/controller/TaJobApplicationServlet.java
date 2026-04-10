package com.bupt.tarecruit.ta.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.common.util.ServletJsonResponseWriter;
import com.bupt.tarecruit.ta.dao.TaAccountDao;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@WebServlet(name = "taJobApplicationServlet", value = "/api/ta/applications")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 10 * 1024 * 1024,
        maxRequestSize = 12 * 1024 * 1024
)
public class TaJobApplicationServlet extends HttpServlet {
    private static final long MAX_RESUME_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final TaAccountDao taAccountDao = new TaAccountDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String taId = trim(req.getParameter("taId"));
        String courseCode = trim(req.getParameter("courseCode"));
        Part resumePart;
        try {
            resumePart = req.getPart("resumeFile");
        } catch (IllegalStateException ex) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("简历文件大小不能超过 10MB"));
            return;
        }

        if (taId.isEmpty()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("缺少 TA 标识"));
            return;
        }
        if (courseCode.isEmpty()) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("缺少课程编号"));
            return;
        }
        if (resumePart == null || resumePart.getSize() <= 0) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("请上传简历文件"));
            return;
        }
        if (resumePart.getSize() > MAX_RESUME_SIZE) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("简历文件大小不能超过 10MB"));
            return;
        }

        String originalFileName = extractFileName(resumePart);
        String extension = resolveExtension(originalFileName);
        String contentType = trim(resumePart.getContentType()).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("简历文件仅支持 PDF / DOC / DOCX"));
            return;
        }
        if (!contentType.isBlank() && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("简历 MIME 类型不受支持，请上传标准 PDF / DOC / DOCX 文件"));
            return;
        }

        try (InputStream inputStream = resumePart.getInputStream()) {
            TaAccountDao.ApplicationSubmitResult result = taAccountDao.createApplication(
                    new TaAccountDao.ApplicationCreateInput(
                            taId,
                            courseCode,
                            originalFileName,
                            contentType,
                            inputStream
                    )
            );
            if (result.isSuccess()) {
                ServletJsonResponseWriter.write(resp, result.getStatus(), ApiResponse.success(result.getMessage(), gsonToMap(result.getData())));
            } else {
                ServletJsonResponseWriter.write(resp, result.getStatus(), ApiResponse.failure(result.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, 500, ApiResponse.failure("提交课程申请失败: " + e.getMessage()));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        ServletJsonResponseWriter.write(resp, 405, ApiResponse.failure("请使用 POST 提交课程申请"));
    }

    private Map<String, Object> gsonToMap(JsonObject object) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (object == null) {
            return result;
        }
        object.entrySet().forEach(entry -> {
            if (entry.getValue() == null || entry.getValue().isJsonNull()) {
                result.put(entry.getKey(), null);
            } else if (entry.getValue().isJsonPrimitive()) {
                if (entry.getValue().getAsJsonPrimitive().isBoolean()) {
                    result.put(entry.getKey(), entry.getValue().getAsBoolean());
                } else if (entry.getValue().getAsJsonPrimitive().isNumber()) {
                    result.put(entry.getKey(), entry.getValue().getAsNumber());
                } else {
                    result.put(entry.getKey(), entry.getValue().getAsString());
                }
            } else {
                result.put(entry.getKey(), entry.getValue().toString());
            }
        });
        return result;
    }

    private String extractFileName(Part part) {
        String submitted = part.getSubmittedFileName();
        if (submitted == null) {
            return "resume-upload";
        }
        String normalized = submitted.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }

    private String resolveExtension(String fileName) {
        String normalized = trim(fileName).toLowerCase(Locale.ROOT);
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(dotIndex + 1);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
