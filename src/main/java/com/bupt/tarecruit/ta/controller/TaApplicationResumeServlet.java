package com.bupt.tarecruit.ta.controller;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.ta.dao.TaAccountDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet(name = "taApplicationResumeServlet", value = "/api/ta/applications/resume")
public class TaApplicationResumeServlet extends HttpServlet {
    private static final int BUFFER_SIZE = 8192;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        String taId = trim(req.getParameter("taId"));
        String relativePath = trim(req.getParameter("path"));
        if (taId.isEmpty() || relativePath.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少简历查看参数");
            return;
        }

        Path taRoot = DataMountPaths.taDir().toAbsolutePath().normalize();
        Path taResumeRoot = DataMountPaths.taResumeRoot().resolve(taId).toAbsolutePath().normalize();
        Path target = taRoot.resolve(relativePath).toAbsolutePath().normalize();
        if (!target.startsWith(taResumeRoot) || !Files.exists(target) || Files.isDirectory(target)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "简历文件不存在");
            return;
        }

        String fileName = target.getFileName() == null ? "resume" : target.getFileName().toString();
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

    private String resolveContentType(String fileName) {
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

    private String encodeFileName(String fileName) {
        return fileName == null ? "resume" : fileName.replace("\r", "").replace("\n", "").replace("\"", "");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
