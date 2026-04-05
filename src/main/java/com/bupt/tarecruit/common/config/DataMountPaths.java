package com.bupt.tarecruit.common.config;

import java.nio.file.Path;

/**
 * 解析 MO/TA 挂载数据根目录。类加载时读取环境变量 {@link #DATA_MOUNT_ENV}；未设置或为空时，
 * 使用 {@link #DEFAULT_DATA_ROOT} 并转为绝对路径（与进程工作目录有关）。
 * <p>
 * 应用启动时 {@link com.bupt.tarecruit.common.listener.DataMountStartupListener} 会将
 * {@link #root()}、{@link #fromEnvironment()} 打到标准输出（{@code [data-mount]} 行），与 TA 侧
 * {@link com.bupt.tarecruit.ta.dao.TaAccountDao#getDataMountStatusMessage()} 对照使用。
 * 包级说明见 {@code com/bupt/tarecruit/common/README.md}。
 */
public final class DataMountPaths {
    /** 环境变量名：指向数据根目录；未设置则回退 {@link #DEFAULT_DATA_ROOT}。 */
    public static final String DATA_MOUNT_ENV = "mountDataTAMObupter";
    /** 未设置环境变量时使用的相对路径名（会经 {@link Path#toAbsolutePath()} 解析）。 */
    public static final Path DEFAULT_DATA_ROOT = Path.of("mountDataTAMObupter");

    private static final ResolvedDataRoot RESOLVED = resolveDataRoot();

    private DataMountPaths() {
    }

    /** 当前解析后的数据根目录（绝对路径、已规范化）。 */
    public static Path root() {
        return RESOLVED.rootPath();
    }

    /** {@code true} 表示根路径来自环境变量 {@link #DATA_MOUNT_ENV}；{@code false} 表示使用默认相对目录。 */
    public static boolean fromEnvironment() {
        return RESOLVED.fromEnvironment();
    }

    /** Reserved for MO-only files under {@code <root>/mo}; not all MO data lives here. */
    @SuppressWarnings("unused")
    public static Path moDir() {
        return root().resolve("mo");
    }

    /** Returns path to MO accounts file ({@code mos.json}). */
    public static Path moAccounts() {
        return moDir().resolve("mos.json");
    }

    /** Returns path to MO profiles file ({@code profiles.json}). */
    public static Path moProfiles() {
        return moDir().resolve("profiles.json");
    }

    /** Returns path to MO settings file ({@code settings.json}). */
    @SuppressWarnings("unused")
    public static Path moSettings() {
        return moDir().resolve("settings.json");
    }

    public static Path taDir() {
        return root().resolve("ta");
    }

    public static Path moRecruitmentCourses() {
        return root().resolve("common").resolve("recruitment-courses.json");
    }

    public static Path taAccounts() {
        return taDir().resolve("tas.json");
    }

    public static Path taProfiles() {
        return taDir().resolve("profiles.json");
    }

    public static Path taApplicationStatus() {
        return taDir().resolve("application-status.json");
    }

    public static Path taApplications() {
        return taDir().resolve("applications.json");
    }

    public static Path taApplicationEvents() {
        return taDir().resolve("application-events.json");
    }

    public static Path taResumeRoot() {
        return taDir().resolve("resume");
    }

    public static Path taResumeDir(String taId) {
        return taResumeRoot().resolve(safePathSegment(taId));
    }

    public static Path taResumeCourseDir(String taId, String courseCode) {
        return taResumeDir(taId).resolve(safePathSegment(courseCode));
    }

    public static Path taAiRoot() {
        return taDir().resolve("ai");
    }

    public static Path taAiConversationRoot() {
        return taAiRoot().resolve("conversations");
    }

    public static Path taAiConversationFile(String taId) {
        return taAiConversationRoot().resolve(safePathSegment(taId) + ".json");
    }

    public static Path taAiAttachmentRoot(String taId) {
        return taAiRoot().resolve("attachments").resolve(safePathSegment(taId));
    }

    public static Path taAiAttachmentTempRoot(String taId) {
        return taAiAttachmentRoot(taId).resolve("temp");
    }

    public static Path taAiAttachmentUploadedRoot(String taId) {
        return taAiAttachmentRoot(taId).resolve("uploaded");
    }

    public static Path taAiAttachmentGeneratedRoot(String taId) {
        return taAiAttachmentRoot(taId).resolve("generated");
    }

    public static Path taAiExportRoot(String taId) {
        return taAiRoot().resolve("exports").resolve(safePathSegment(taId));
    }

    private static String safePathSegment(String value) {
        String normalized = value == null ? "unknown" : value.trim();
        if (normalized.isEmpty()) {
            normalized = "unknown";
        }
        return normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static ResolvedDataRoot resolveDataRoot() {
        String envValue = System.getenv(DATA_MOUNT_ENV);
        if (envValue == null || envValue.trim().isEmpty()) {
            return new ResolvedDataRoot(false, DEFAULT_DATA_ROOT.toAbsolutePath().normalize());
        }
        return new ResolvedDataRoot(true, Path.of(envValue.trim()).toAbsolutePath().normalize());
    }

    private record ResolvedDataRoot(boolean fromEnvironment, Path rootPath) {
    }
}
