package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.mo.dao.MoAiConversationDao;
import com.bupt.tarecruit.mo.service.MoAiAssistantService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

@WebServlet(name = "moAiFileDownloadServlet", value = "/api/mo/ai/files/download")
public class MoAiFileDownloadServlet extends HttpServlet {
    private static final int BUFFER_SIZE = 8192;
    private final MoAiAssistantService aiAssistantService = new MoAiAssistantService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        String moId = trim(req.getParameter("moId"));
        String sessionId = trim(req.getParameter("sessionId"));
        String attachmentId = trim(req.getParameter("attachmentId"));
        if (moId.isEmpty() || sessionId.isEmpty() || attachmentId.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing AI result download parameters");
            return;
        }

        MoAiConversationDao.DownloadFile downloadFile = aiAssistantService.getGeneratedFile(moId, sessionId, attachmentId);
        if (downloadFile == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "AI result file not found");
            return;
        }

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType(trim(downloadFile.mimeType()).isEmpty() ? "application/pdf" : downloadFile.mimeType());
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + encodeFileName(downloadFile.fileName()) + "\"");
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setContentLengthLong(downloadFile.size());

        try (InputStream input = java.nio.file.Files.newInputStream(downloadFile.path());
             var output = resp.getOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        }
    }

    private String encodeFileName(String fileName) {
        return fileName == null ? "ai-result.pdf" : fileName.replace("\r", "").replace("\n", "").replace("\"", "");
    }
}
