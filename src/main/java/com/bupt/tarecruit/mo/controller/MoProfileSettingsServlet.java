package com.bupt.tarecruit.mo.controller;

import com.bupt.tarecruit.common.model.ApiResponse;
import com.bupt.tarecruit.common.util.ServletJsonResponseWriter;
import com.bupt.tarecruit.mo.dao.MoAccountDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * MO 个人资料与设置 Servlet。
 *
 * <p>处理 MO 用户的资料查询、更新、头像上传和密码修改。</p>
 *
 * <p>URL 映射：</p>
 * <ul>
 *     <li>{@code /api/mo/profile-settings} - 资料管理 API</li>
 *     <li>{@code /mo-assets/*} - 静态资源服务（头像等）</li>
 * </ul>
 */
@WebServlet(name = "moProfileSettingsServlet", value = {"/api/mo/profile-settings", "/mo-assets/*"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 10 * 1024 * 1024,
        maxRequestSize = 12 * 1024 * 1024
)
public class MoProfileSettingsServlet extends HttpServlet {
    private static final long MAX_AVATAR_SIZE = 10L * 1024 * 1024;
    private static final int AVATAR_SIZE = 256;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif"
    );

    private final MoAccountDao moAccountDao = new MoAccountDao();
    private Path moDataDir;
    private Path avatarDir;

    @Override
    public void init() throws ServletException {
        this.moDataDir = MoAccountDao.getResolvedMoDataDir();
        this.avatarDir = moDataDir.resolve("image");
        try {
            Files.createDirectories(avatarDir);
        } catch (IOException e) {
            throw new ServletException("初始化头像目录失败", e);
        }
        System.out.println("[MO-PROFILE] dataDir=" + moDataDir.toAbsolutePath());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
        if ("/mo-assets".equals(servletPath)) {
            serveAsset(req, resp);
            return;
        }

        String moId = trim(req.getParameter("moId"));
        String action = trim(req.getParameter("action"));
        if ("password".equalsIgnoreCase(action)) {
            ServletJsonResponseWriter.write(resp, 405, ApiResponse.failure("请使用 POST 更新密码"));
            return;
        }

        try {
            MoAccountDao.ProfileResult result = moAccountDao.getProfileSettings(moId);
            if (result.isSuccess()) {
                ServletJsonResponseWriter.write(resp, result.getStatus(), ApiResponse.success(result.getMessage(), result.getData()));
            } else {
                ServletJsonResponseWriter.write(resp, result.getStatus(), ApiResponse.failure(result.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, 500, ApiResponse.failure("读取设置中心资料失败: " + e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String moId = trim(req.getParameter("moId"));
        String action = trim(req.getParameter("action"));

        if ("password".equalsIgnoreCase(action)) {
            handlePasswordUpdate(req, resp, moId);
            return;
        }

        String name = trim(req.getParameter("name"));
        String contactEmail = trim(req.getParameter("contactEmail"));
        String bio = trim(req.getParameter("bio"));
        List<String> skills = extractSkills(req.getParameterValues("skills[]"), req.getParameter("skills"));

        String previousAvatar = null;
        String avatarPath = null;
        try {
            Part avatarPart = req.getPart("avatarFile");
            if (avatarPart != null && avatarPart.getSize() > 0) {
                MoAccountDao.ProfileResult current = moAccountDao.getProfileSettings(moId);
                if (!current.isSuccess()) {
                    ServletJsonResponseWriter.write(resp, current.getStatus(), ApiResponse.failure(current.getMessage()));
                    return;
                }
                previousAvatar = current.getData() == null ? null : String.valueOf(current.getData().getOrDefault("avatar", ""));
                avatarPath = storeAvatar(moId, avatarPart);
            }
        } catch (IllegalStateException ex) {
            ServletJsonResponseWriter.write(resp, 400, ApiResponse.failure("头像大小不能超过 10MB"));
            return;
        }

        MoAccountDao.ProfileUpdateInput input = new MoAccountDao.ProfileUpdateInput(
                moId,
                name,
                contactEmail,
                bio,
                skills,
                avatarPath == null ? nullSafe(previousAvatar) : avatarPath
        );

        try {
            MoAccountDao.ProfileResult result = moAccountDao.saveProfileSettings(input);
            if (result.isSuccess()) {
                if (avatarPath != null) {
                    deleteOldAvatar(previousAvatar, avatarPath);
                }
                ServletJsonResponseWriter.write(resp, result.getStatus(), ApiResponse.success(result.getMessage(), result.getData()));
            } else {
                if (avatarPath != null) {
                    deleteStoredAvatarQuietly(avatarPath);
                }
                ServletJsonResponseWriter.write(resp, result.getStatus(), ApiResponse.failure(result.getMessage()));
            }
        } catch (Exception e) {
            if (avatarPath != null) {
                deleteStoredAvatarQuietly(avatarPath);
            }
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, 500, ApiResponse.failure("保存设置中心资料失败: " + e.getMessage()));
        }
    }

    private void handlePasswordUpdate(HttpServletRequest req, HttpServletResponse resp, String moId) throws IOException {
        String currentPassword = req.getParameter("currentPassword");
        String newPassword = req.getParameter("newPassword");
        try {
            MoAccountDao.PasswordUpdateResult result = moAccountDao.updatePassword(moId, currentPassword, newPassword);
            if (result.isSuccess()) {
                ServletJsonResponseWriter.write(resp, result.getStatus(), ApiResponse.success(result.getMessage(), null));
            } else {
                ServletJsonResponseWriter.write(resp, result.getStatus(), ApiResponse.failure(result.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ServletJsonResponseWriter.write(resp, 500, ApiResponse.failure("密码更新失败: " + e.getMessage()));
        }
    }

    private void serveAsset(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        String relativePath = trimLeadingSlash(pathInfo);
        if (relativePath.isBlank()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path target = moDataDir.resolve(relativePath).normalize();
        if (!target.startsWith(moDataDir) || !Files.exists(target) || Files.isDirectory(target)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(target);
        if (contentType == null) {
            String lower = target.toString().toLowerCase();
            if (lower.endsWith(".png")) contentType = "image/png";
            else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (lower.endsWith(".gif")) contentType = "image/gif";
            else contentType = "application/octet-stream";
        }

        resp.setContentType(contentType);
        resp.setHeader("Cache-Control", "public, max-age=86400");
        resp.setContentLengthLong(Files.size(target));
        try (InputStream inputStream = Files.newInputStream(target)) {
            inputStream.transferTo(resp.getOutputStream());
        }
    }

    private String storeAvatar(String moId, Part avatarPart) throws IOException, ServletException {
        if (trim(moId).isEmpty()) {
            throw new ServletException("缺少 MO 标识，无法保存头像");
        }
        if (avatarPart.getSize() > MAX_AVATAR_SIZE) {
            throw new ServletException("头像大小不能超过 10MB");
        }

        String contentType = trim(avatarPart.getContentType()).toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ServletException("头像格式仅支持 PNG / JPG / WEBP / GIF");
        }

        String extension = switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".png";
        };

        Files.createDirectories(avatarDir);
        String fileName = moId + "_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = avatarDir.resolve(fileName);

        try (InputStream inputStream = avatarPart.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                throw new ServletException("无法解析上传的图片，请检查图片格式");
            }
            BufferedImage croppedSquare = cropToSquare(originalImage);
            BufferedImage resizedImage = resizeAvatar(croppedSquare, AVATAR_SIZE, AVATAR_SIZE);
            String format = resolveOutputFormat(extension);
            boolean writeSuccess = ImageIO.write(resizedImage, format, target.toFile());
            if (!writeSuccess) {
                throw new ServletException("头像写入失败，请稍后重试");
            }
        }

        return "image/" + fileName;
    }

    private BufferedImage cropToSquare(BufferedImage originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int side = Math.min(width, height);
        int x = (width - side) / 2;
        int y = (height - side) / 2;
        BufferedImage square = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = square.createGraphics();
        try {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, side, side);
            g2d.drawImage(originalImage, 0, 0, side, side, x, y, x + side, y + side, null);
        } finally {
            g2d.dispose();
        }
        return square;
    }

    private BufferedImage resizeAvatar(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, targetWidth, targetHeight);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }
        return resized;
    }

    private String resolveOutputFormat(String extension) {
        if (".jpg".equalsIgnoreCase(extension) || ".jpeg".equalsIgnoreCase(extension)) {
            return "jpg";
        }
        return "png";
    }

    private void deleteOldAvatar(String oldAvatarPath, String newAvatarPath) {
        String normalizedOldPath = trimLeadingSlash(oldAvatarPath);
        String normalizedNewPath = trimLeadingSlash(newAvatarPath);
        if (normalizedOldPath.isBlank() || normalizedOldPath.equals(normalizedNewPath)) {
            return;
        }
        deleteStoredAvatarQuietly(normalizedOldPath);
    }

    private void deleteStoredAvatarQuietly(String avatarPath) {
        String normalizedPath = trimLeadingSlash(avatarPath);
        if (normalizedPath.isBlank()) {
            return;
        }
        Path targetAvatar = moDataDir.resolve(normalizedPath).normalize();
        if (!targetAvatar.startsWith(avatarDir) || Files.isDirectory(targetAvatar)) {
            return;
        }
        try {
            Files.deleteIfExists(targetAvatar);
        } catch (IOException ex) {
            System.err.println("[MO-PROFILE] 删除头像失败: " + targetAvatar + ", reason=" + ex.getMessage());
        }
    }

    private List<String> extractSkills(String[] skillArray, String skillText) {
        List<String> skills = new ArrayList<>();
        if (skillArray != null) {
            for (String item : skillArray) {
                addSkill(skills, item);
            }
        }
        if (skillText != null && !skillText.isBlank()) {
            String[] parts = skillText.split(",");
            for (String part : parts) {
                addSkill(skills, part);
            }
        }
        return skills;
    }

    private void addSkill(List<String> skills, String value) {
        String normalized = trim(value);
        if (!normalized.isBlank() && !skills.contains(normalized)) {
            skills.add(normalized);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimLeadingSlash(String value) {
        String normalized = value == null ? "" : value.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
