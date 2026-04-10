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
 * 独立运行的 TA AI 数据清理工具。
 * <p>
 * 用途：
 * 1. 清空 TA AI conversation JSON（删除每个 TA 的历史会话、消息、附件关联、生成文件索引）
 * 2. 删除 TA AI 附件目录（temp / uploaded / generated）中的所有实际文件
 * 3. 删除 TA AI 导出目录中的所有文件
 * 4. 重新创建空的 AI 根目录结构，便于后续继续使用
 * <p>
 * 注意：
 * 1. 该工具只清理 TA AI 相关数据，不修改 MO 模块，不修改 TA 非 AI 申请数据
 * 2. conversation JSON 被删除后，前端下次访问 AI 助理时会由既有逻辑自动重建为空结构
 * 3. 该工具适合本地开发或测试环境进行“AI 数据全量重置”
 * <p>
 * 运行方式（项目根目录）：
 * {@code mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.ta.util.TaAiDataCleanupTool}
 */
public final class TaAiDataCleanupTool {

    private TaAiDataCleanupTool() {
    }

    public static void main(String[] args) throws Exception {
        Path aiRoot = DataMountPaths.taAiRoot();
        Path conversationRoot = DataMountPaths.taAiConversationRoot();
        Path attachmentsRoot = aiRoot.resolve("attachments");
        Path exportsRoot = aiRoot.resolve("exports");

        deleteDirectoryContents(conversationRoot);
        deleteDirectoryContents(attachmentsRoot);
        deleteDirectoryContents(exportsRoot);

        Files.createDirectories(conversationRoot);
        Files.createDirectories(attachmentsRoot);
        Files.createDirectories(exportsRoot);

        writeReadmeIfMissing(aiRoot.resolve("README.md"));

        System.out.println("[TA-AI-CLEANUP] 清理完成");
        System.out.println("[TA-AI-CLEANUP] ai-root = " + aiRoot.toAbsolutePath());
        System.out.println("[TA-AI-CLEANUP] conversations = " + conversationRoot.toAbsolutePath());
        System.out.println("[TA-AI-CLEANUP] attachments = " + attachmentsRoot.toAbsolutePath());
        System.out.println("[TA-AI-CLEANUP] exports = " + exportsRoot.toAbsolutePath());
    }

    private static void writeReadmeIfMissing(Path path) throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        Files.writeString(
                path,
                "TA AI data root.\n",
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
                    .forEach(TaAiDataCleanupTool::deletePathQuietly);
        }

        removeEmptyDirectories(directory);
    }

    private static void removeEmptyDirectories(Path rootDirectory) throws IOException {
        if (!Files.exists(rootDirectory)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(rootDirectory)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(rootDirectory))
                    .filter(Files::isDirectory)
                    .forEach(TaAiDataCleanupTool::deletePathQuietly);
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
            System.out.println("[TA-AI-CLEANUP] deleted " + type + " => " + path.toAbsolutePath());
        } catch (IOException ex) {
            throw new RuntimeException("删除 AI 路径失败: " + path.toAbsolutePath(), ex);
        }
    }
}
