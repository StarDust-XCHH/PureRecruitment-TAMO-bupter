package com.bupt.tarecruit.mo.util;

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
 * 独立运行的 MO AI 数据清理工具。
 * <p>
 * 用途：
 * 1. 清空 MO AI conversation JSON（删除每个 MO 的历史会话、消息、附件关联、生成文件索引）
 * 2. 删除 MO AI 附件目录（temp / uploaded / generated）中的所有实际文件
 * 3. 删除 MO AI 导出目录中的所有文件
 * 4. 重新创建空的 AI 根目录结构，便于后续继续使用
 * <p>
 * 注意：只清理 MO AI 相关数据，不修改 TA 模块与非 AI 的 MO 申请旁路 JSON。
 * <p>
 * 运行方式（项目根目录）：
 * {@code mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.mo.util.MoAiDataCleanupTool}
 */
public final class MoAiDataCleanupTool {

    private MoAiDataCleanupTool() {
    }

    public static void main(String[] args) throws Exception {
        Path aiRoot = DataMountPaths.moAiRoot();
        Path conversationRoot = DataMountPaths.moAiConversationRoot();
        Path attachmentsRoot = aiRoot.resolve("attachments");
        Path exportsRoot = aiRoot.resolve("exports");

        deleteDirectoryContents(conversationRoot);
        deleteDirectoryContents(attachmentsRoot);
        deleteDirectoryContents(exportsRoot);

        Files.createDirectories(conversationRoot);
        Files.createDirectories(attachmentsRoot);
        Files.createDirectories(exportsRoot);

        writeReadmeIfMissing(aiRoot.resolve("README.md"));

        System.out.println("[MO-AI-CLEANUP] 清理完成");
        System.out.println("[MO-AI-CLEANUP] ai-root = " + aiRoot.toAbsolutePath());
        System.out.println("[MO-AI-CLEANUP] conversations = " + conversationRoot.toAbsolutePath());
        System.out.println("[MO-AI-CLEANUP] attachments = " + attachmentsRoot.toAbsolutePath());
        System.out.println("[MO-AI-CLEANUP] exports = " + exportsRoot.toAbsolutePath());
    }

    private static void writeReadmeIfMissing(Path path) throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        Files.writeString(
                path,
                "MO AI data root.\n",
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
                    .forEach(MoAiDataCleanupTool::deletePathQuietly);
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
                    .forEach(MoAiDataCleanupTool::deletePathQuietly);
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
            System.out.println("[MO-AI-CLEANUP] deleted " + type + " => " + path.toAbsolutePath());
        } catch (IOException ex) {
            throw new RuntimeException("删除 MO AI 路径失败: " + path.toAbsolutePath(), ex);
        }
    }
}
