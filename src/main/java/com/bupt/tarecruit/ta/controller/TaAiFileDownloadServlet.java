package com.bupt.tarecruit.ta.controller;

import com.bupt.tarecruit.ta.dao.TaAiConversationDao;
import com.bupt.tarecruit.ta.service.TaAiAssistantService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

@WebServlet(name = "taAiFileDownloadServlet", value = "/api/ta/ai/files/download")
public class TaAiFileDownloadServlet extends HttpServlet {
    private static final int BUFFER_SIZE = 8192;
    private final TaAiAssistantService aiAssistantService = new TaAiAssistantService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        String taId = trim(req.getParameter("taId"));
        String sessionId = trim(req.getParameter("sessionId"));
        String attachmentId = trim(req.getParameter("attachmentId"));
        if (taId.isEmpty() || sessionId.isEmpty() || attachmentId.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 AI 结果文件下载参数");
            return;
        }

        TaAiConversationDao.DownloadFile downloadFile = aiAssistantService.getGeneratedFile(taId, sessionId, attachmentId);
        if (downloadFile == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "AI 结果文件不存在");
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
