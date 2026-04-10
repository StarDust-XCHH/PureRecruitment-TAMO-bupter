package com.bupt.tools.cli;

import com.bupt.tools.core.WorkspacePaths;
import com.bupt.tools.core.WorkspaceService;
import com.bupt.tools.model.CommandResult;
import com.bupt.tools.model.DiffEntry;
import com.bupt.tools.support.SyncRules;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class DevDataToolsApplication {
    public static void main(String[] args) {
        int exitCode = new DevDataToolsApplication().run(args);
        System.exit(exitCode);
    }

    int run(String[] args) {
        if (args == null || args.length < 2) {
            printUsage();
            return 1;
        }

        String group = args[0];
        String command = args[1];
        WorkspaceService workspaceService = new WorkspaceService();
        WorkspacePaths workspacePaths = resolvePaths(args);

        try {
            if ("workspace".equalsIgnoreCase(group)) {
                return handleWorkspaceCommand(command, workspaceService, workspacePaths);
            }
            System.err.println("不支持的命令组: " + group);
            printUsage();
            return 1;
        } catch (Exception exception) {
            System.err.println("执行失败: " + exception.getMessage());
            return 2;
        }
    }

    private int handleWorkspaceCommand(String command, WorkspaceService workspaceService, WorkspacePaths workspacePaths) throws IOException {
        return switch (command.toLowerCase()) {
            case "init" -> printResult(workspaceService.initWorkspace(workspacePaths));
            case "reset" -> printResult(workspaceService.resetWorkspace(workspacePaths));
            case "diff" -> printDiff(workspaceService.diffWorkspace(workspacePaths));
            case "sync" -> printResult(workspaceService.syncWorkspace(workspacePaths));
            default -> {
                System.err.println("不支持的 workspace 命令: " + command);
                printUsage();
                yield 1;
            }
        };
    }

    private WorkspacePaths resolvePaths(String[] args) {
        Path baseline = Path.of("mountDataTAMObupter").toAbsolutePath().normalize();
        Path workspace = baseline.resolve(".workspace");

        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--baseline=")) {
                baseline = Path.of(arg.substring("--baseline=".length())).toAbsolutePath().normalize();
                workspace = baseline.resolve(".workspace");
            } else if (arg.startsWith("--workspace=")) {
                workspace = Path.of(arg.substring("--workspace=".length())).toAbsolutePath().normalize();
            }
        }
        return new WorkspacePaths(baseline, workspace);
    }

    private int printResult(CommandResult result) {
        if (result.success()) {
            System.out.println(result.message());
            return 0;
        }
        System.err.println(result.message());
        return 1;
    }

    private int printDiff(List<DiffEntry> diffEntries) {
        if (diffEntries.isEmpty()) {
            System.out.println("未发现需要关注的有效差异。默认已忽略运行态字段: " + SyncRules.ignoredFields());
            return 0;
        }
        System.out.println("发现差异 " + diffEntries.size() + " 项:");
        for (DiffEntry diffEntry : diffEntries) {
            System.out.println("- [" + diffEntry.type() + "] " + diffEntry.relativePath() + " -> " + diffEntry.detail());
        }
        return 0;
    }

    private void printUsage() {
        System.out.println("用法:");
        System.out.println("  workspace init [--baseline=路径] [--workspace=路径]");
        System.out.println("  workspace reset [--baseline=路径] [--workspace=路径]");
        System.out.println("  workspace diff [--baseline=路径] [--workspace=路径]");
        System.out.println("  workspace sync [--baseline=路径] [--workspace=路径]");
        System.out.println();
        System.out.println("默认基线目录: mountDataTAMObupter");
        System.out.println("默认工作副本目录: mountDataTAMObupter/.workspace");
    }
}
