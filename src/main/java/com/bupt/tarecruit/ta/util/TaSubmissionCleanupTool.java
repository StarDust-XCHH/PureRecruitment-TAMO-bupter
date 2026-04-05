package com.bupt.tarecruit.ta.util;

import com.bupt.tarecruit.common.config.DataMountPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 独立运行的 TA 申请数据清理工具。
 * <p>
 * 用途：
 * 1. 清空 {@code applications.json}
 * 2. 清空 {@code application-events.json}
 * 3. 清空 {@code application-status.json}，以便同时移除该 TA 在本门课上的简历通过/拒绝状态
 * 4. 删除 {@code resume/} 目录下所有 TA 简历文件与子目录
 * <p>
 * 运行方式（项目根目录）：
 * {@code mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.ta.util.TaSubmissionCleanupTool}
 */
public final class TaSubmissionCleanupTool {

    private static final String EMPTY_APPLICATIONS_JSON = "{\n"
            + "  \"schema\": \"ta-applications.v1\",\n"
            + "  \"version\": 1,\n"
            + "  \"items\": []\n"
            + "}\n";

    private static final String EMPTY_APPLICATION_EVENTS_JSON = "{\n"
            + "  \"schema\": \"ta-application-events.v1\",\n"
            + "  \"version\": 1,\n"
            + "  \"items\": []\n"
            + "}\n";

    private static final String EMPTY_APPLICATION_STATUS_JSON = "{\n"
            + "  \"schema\": \"ta-application-status.v1\",\n"
            + "  \"version\": 1,\n"
            + "  \"items\": []\n"
            + "}\n";

    private TaSubmissionCleanupTool() {
    }

    public static void main(String[] args) throws Exception {
        Path taDir = DataMountPaths.taDir();
        Path applicationsPath = DataMountPaths.taApplications();
        Path applicationEventsPath = DataMountPaths.taApplicationEvents();
        Path applicationStatusPath = DataMountPaths.taApplicationStatus();
        Path resumeRoot = DataMountPaths.taResumeRoot();

        Files.createDirectories(taDir);

        writeJsonFile(applicationsPath, EMPTY_APPLICATIONS_JSON);
        writeJsonFile(applicationEventsPath, EMPTY_APPLICATION_EVENTS_JSON);
        writeJsonFile(applicationStatusPath, EMPTY_APPLICATION_STATUS_JSON);
        deleteDirectoryContents(resumeRoot);
        Files.createDirectories(resumeRoot);

        System.out.println("[TA-CLEANUP] 清理完成");
        System.out.println("[TA-CLEANUP] applications = " + applicationsPath.toAbsolutePath());
        System.out.println("[TA-CLEANUP] application-events = " + applicationEventsPath.toAbsolutePath());
        System.out.println("[TA-CLEANUP] application-status = " + applicationStatusPath.toAbsolutePath());
        System.out.println("[TA-CLEANUP] resume-root = " + resumeRoot.toAbsolutePath());
    }

    private static void writeJsonFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private static void deleteDirectoryContents(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(directory))
                    .forEach(TaSubmissionCleanupTool::deletePathQuietly);
        }

        removeEmptyDirectories(directory);
    }

    private static void removeEmptyDirectories(Path rootDirectory) throws IOException {
        try (Stream<Path> stream = Files.walk(rootDirectory)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(rootDirectory))
                    .filter(Files::isDirectory)
                    .forEach(TaSubmissionCleanupTool::deletePathQuietly);
        }
    }

    private static void deletePathQuietly(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> childStream = Files.list(path)) {
                    if (childStream.findAny().isPresent()) {
                        return;
                    }
                }
            }

            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            Files.deleteIfExists(path);
            String type = attributes.isDirectory() ? "DIR" : "FILE";
            System.out.println("[TA-CLEANUP] deleted " + type + " => " + path.toAbsolutePath());
        } catch (IOException ex) {
            throw new RuntimeException("删除路径失败: " + path.toAbsolutePath(), ex);
        }
    }
}
