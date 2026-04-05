package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.mo.dao.MoRecruitmentDao;
import com.bupt.tarecruit.mo.service.MoTaApplicationReadService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MO 下载/预览 TA 投递简历（校验 {@code moId} 与岗位归属）。
 */
@WebServlet(name = "moApplicantResumeServlet", value = "/api/mo/applications/resume")
public class MoApplicantResumeServlet extends HttpServlet {

    private static final int BUFFER_SIZE = 8192;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        String moId = trim(req.getParameter("moId"));
        String applicationId = trim(req.getParameter("applicationId"));
        if (moId.isEmpty() || applicationId.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 moId 或 applicationId");
            return;
        }

        try {
            new MoRecruitmentDao().getApplicantDetail(moId, applicationId);
        } catch (IllegalArgumentException ex) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
            return;
        }

        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        MoTaApplicationReadService.ResumeBinaryPayload payload = readService.readResumeBinary(applicationId);
        if (payload == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "简历不存在");
            return;
        }

        Path target = payload.absolutePath();
        String fileName = payload.originalFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = payload.storedFileName();
        }
        if (fileName == null || fileName.isBlank()) {
            fileName = "resume";
        }

        String contentType = Files.probeContentType(target);
        if (contentType == null || contentType.isBlank()) {
            contentType = resolveContentType(fileName);
        }

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType(contentType);
        resp.setHeader("Content-Disposition", "inline; filename=\"" + encodeFileName(fileName) + "\"");
        resp.setHeader("Cache-Control", "private, max-age=300");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setContentLengthLong(Files.size(target));

        try (var input = Files.newInputStream(target);
             var output = resp.getOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        }
    }

    private static String resolveContentType(String fileName) {
        String lower = trim(fileName).toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".doc")) {
            return "application/msword";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }

    private static String encodeFileName(String fileName) {
        return fileName == null ? "resume" : fileName.replace("\r", "").replace("\n", "").replace("\"", "");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
