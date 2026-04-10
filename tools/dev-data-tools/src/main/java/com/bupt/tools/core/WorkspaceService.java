package com.bupt.tools.core;

import com.bupt.tools.model.CommandResult;
import com.bupt.tools.model.DiffEntry;
import com.bupt.tools.support.FileSupport;
import com.bupt.tools.support.JsonSupport;
import com.bupt.tools.support.SyncRules;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WorkspaceService {
    public CommandResult initWorkspace(WorkspacePaths paths) throws IOException {
        validateBaseline(paths.baselineRoot(), paths.workspaceRoot());
        FileSupport.ensureDirectory(paths.workspaceRoot());
        copyBaselineToWorkspace(paths.baselineRoot(), paths.workspaceRoot());
        writeWorkspaceReadme(paths.workspaceRoot(), paths.baselineRoot());
        return CommandResult.success("工作副本初始化完成: " + paths.workspaceRoot());
    }

    public CommandResult resetWorkspace(WorkspacePaths paths) throws IOException {
        validateBaseline(paths.baselineRoot(), paths.workspaceRoot());
        FileSupport.resetDirectory(paths.workspaceRoot());
        copyBaselineToWorkspace(paths.baselineRoot(), paths.workspaceRoot());
        writeWorkspaceReadme(paths.workspaceRoot(), paths.baselineRoot());
        return CommandResult.success("工作副本已重置: " + paths.workspaceRoot());
    }

    public List<DiffEntry> diffWorkspace(WorkspacePaths paths) throws IOException {
        validateBaseline(paths.baselineRoot(), paths.workspaceRoot());
        Set<Path> relativePaths = collectComparableRelativePaths(paths.baselineRoot(), paths.workspaceRoot());
        List<DiffEntry> result = new ArrayList<>();
        for (Path relativePath : relativePaths) {
            Path baselineFile = paths.baselineRoot().resolve(relativePath);
            Path workspaceFile = paths.workspaceRoot().resolve(relativePath);
            boolean baselineExists = Files.exists(baselineFile);
            boolean workspaceExists = Files.exists(workspaceFile);

            if (!baselineExists && workspaceExists) {
                result.add(new DiffEntry("ADDED", relativePath.toString(), "工作副本新增文件"));
                continue;
            }
            if (baselineExists && !workspaceExists) {
                result.add(new DiffEntry("REMOVED", relativePath.toString(), "工作副本缺少基线文件"));
                continue;
            }
            if (JsonSupport.isJsonFile(baselineFile) && JsonSupport.isJsonFile(workspaceFile)) {
                if (!JsonSupport.jsonEqualsIgnoringVolatileFields(baselineFile, workspaceFile)) {
                    result.add(new DiffEntry("CHANGED", relativePath.toString(), JsonSupport.summarizeDifference(baselineFile, workspaceFile)));
                }
                continue;
            }
            long mismatch = Files.mismatch(baselineFile, workspaceFile);
            if (mismatch != -1L) {
                result.add(new DiffEntry("CHANGED", relativePath.toString(), "非 JSON 文件内容不同"));
            }
        }
        return result;
    }

    public CommandResult syncWorkspace(WorkspacePaths paths) throws IOException {
        validateBaseline(paths.baselineRoot(), paths.workspaceRoot());
        List<DiffEntry> diffEntries = diffWorkspace(paths);
        if (diffEntries.isEmpty()) {
            return CommandResult.success("未发现需要同步的有效差异");
        }
        for (DiffEntry entry : diffEntries) {
            Path baselineFile = paths.baselineRoot().resolve(entry.relativePath());
            Path workspaceFile = paths.workspaceRoot().resolve(entry.relativePath());
            switch (entry.type()) {
                case "ADDED" -> FileSupport.copyFile(workspaceFile, baselineFile);
                case "REMOVED" -> Files.deleteIfExists(baselineFile);
                case "CHANGED" -> syncChangedFile(baselineFile, workspaceFile);
                default -> throw new IllegalStateException("未知差异类型: " + entry.type());
            }
        }
        return CommandResult.success("已根据忽略规则将工作副本同步回基线，共处理 " + diffEntries.size() + " 项差异");
    }

    private void syncChangedFile(Path baselineFile, Path workspaceFile) throws IOException {
        if (JsonSupport.isJsonFile(baselineFile) && JsonSupport.isJsonFile(workspaceFile)) {
            JsonSupport.writeJson(baselineFile, JsonSupport.mergeWorkspaceIntoBaseline(baselineFile, workspaceFile));
            return;
        }
        FileSupport.copyFile(workspaceFile, baselineFile);
    }

    private void copyBaselineToWorkspace(Path baselineRoot, Path workspaceRoot) throws IOException {
        for (Path file : FileSupport.listRegularFiles(baselineRoot)) {
            Path relativePath = baselineRoot.relativize(file);
            if (SyncRules.shouldIgnorePath(relativePath)) {
                continue;
            }
            FileSupport.copyFile(file, workspaceRoot.resolve(relativePath));
        }
    }

    private Set<Path> collectComparableRelativePaths(Path baselineRoot, Path workspaceRoot) throws IOException {
        Set<Path> paths = new LinkedHashSet<>();
        for (Path file : FileSupport.listRegularFiles(baselineRoot)) {
            Path relativePath = baselineRoot.relativize(file);
            if (!SyncRules.shouldIgnorePath(relativePath)) {
                paths.add(relativePath);
            }
        }
        for (Path file : FileSupport.listRegularFiles(workspaceRoot)) {
            Path relativePath = workspaceRoot.relativize(file);
            if (!SyncRules.shouldIgnorePath(relativePath) && !String.valueOf(relativePath.getFileName()).equalsIgnoreCase("README.md")) {
                paths.add(relativePath);
            }
        }
        return paths;
    }

    private void writeWorkspaceReadme(Path workspaceRoot, Path baselineRoot) throws IOException {
        String content = "# 本地工作副本\n\n"
                + "此目录由 [`WorkspaceService.java`](tools/dev-data-tools/src/main/java/com/bupt/tools/core/WorkspaceService.java) 自动生成。\n\n"
                + "## 用途\n\n"
                + "- 作为开发调试使用的本地 JSON 工作副本\n"
                + "- 来源基线目录：`" + baselineRoot + "`\n"
                + "- 建议加入 Git 忽略，避免提交运行态脏数据\n\n"
                + "## 说明\n\n"
                + "- 运行时应优先让本地环境读取本目录\n"
                + "- 如需重置，请使用 `workspace reset`\n"
                + "- 如需筛选后同步，请使用 `workspace sync`\n";
        Files.writeString(workspaceRoot.resolve("README.md"), content, StandardCharsets.UTF_8);
    }

    private void validateBaseline(Path baselineRoot, Path workspaceRoot) {
        if (baselineRoot == null || !Files.exists(baselineRoot)) {
            throw new IllegalArgumentException("基线目录不存在: " + baselineRoot);
        }
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("工作副本目录不能为空");
        }
    }
}
