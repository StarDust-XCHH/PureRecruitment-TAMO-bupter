package com.bupt.tarecruit.tools;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.mo.dao.MoRecruitmentDao;
import com.bupt.tarecruit.mo.util.MoAiDataCleanupTool;
import com.bupt.tarecruit.ta.util.TaAiDataCleanupTool;
import com.bupt.tarecruit.ta.util.TaSubmissionCleanupTool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * 测试环境用：<strong>TA 申请数据 + TA/MO AI 数据 + MO 侧与申请相关的旁路 JSON</strong> 一次性对齐清理。
 * <p>
 * 典型场景：只运行了 {@link TaSubmissionCleanupTool} 后，{@code mo-application-read-state.json}、
 * {@code mo-application-comments.json}、{@code mo-applicant-shortlist.json} 仍引用已不存在的 {@code applicationId}，
 * 导致未读红点、评论、短名单等残留；
 * 本工具在<strong>不修改</strong>既有 {@link TaSubmissionCleanupTool} / {@link TaAiDataCleanupTool} 源码的前提下，
 * 通过调用其 {@code main}、{@link MoAiDataCleanupTool} 并重写 MO 侧三个申请旁路文件，使双端与「零申请」状态一致。
 * 清理结束后会调用 {@link MoRecruitmentDao#syncAllPublishedJobApplicationStatsFromTa()}，按当前 TA 数据重算并写回
 * {@code recruitment-courses.json} 中的申请/已录用统计；本工具<strong>不直接改写</strong>课程 JSON。
 * <p>
 * <strong>不清理</strong>：课程岗位条目本身（仅刷新统计字段）、MO/TA 账号与 profile（{@code mos.json}、{@code tas.json} 等）。
 * <p>
 * 命令行参数（可选）：
 * <ul>
 *     <li>（无参）— 在终端打印菜单，由数字键选择模式；若 stdin 不可用则退化为「全部清理」</li>
 *     <li>{@code --mo-only} — 仅重置 MO 申请旁路三个 JSON（TA 已手动清过或仅需修不同步）</li>
 *     <li>{@code --ta-only} — 仅调用两个 TA 工具，不写 MO 文件</li>
 *     <li>{@code --skip-ai} — 跳过 {@link TaAiDataCleanupTool} 与 {@link MoAiDataCleanupTool}，其余与无参相同</li>
 * </ul>
 * <p>
 * 运行示例（仓库根目录）：见 {@code tools/ta-mo-submission-cleanup/README.md}。
 * <pre>{@code
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.tools.DevApplicationDataCleanupTool
 * }</pre>
 */
public final class DevApplicationDataCleanupTool {

    private static final String MO_SCHEMA = "mo";

    private DevApplicationDataCleanupTool() {
    }

    /**
     * 仅重置 MO 端「已读」「评论」「候选短名单」结构化文件（与 {@code MoApplicationReadStateDao}、
     * {@code MoApplicationCommentsDao}、{@code MoApplicantShortlistDao} 契约一致）。
     */
    public static void resetMoApplicationSidecarFiles() throws IOException {
        Path readState = DataMountPaths.moApplicationReadState();
        Path comments = DataMountPaths.moApplicationComments();
        Path shortlist = DataMountPaths.moApplicantShortlist();
        writeMoEmptyStructured(readState, "mo-application-read-state");
        writeMoEmptyStructured(comments, "mo-application-comments");
        writeMoEmptyStructured(shortlist, "mo-applicant-shortlist");
        System.out.println("[DUAL-CLEANUP] MO read-state => " + readState.toAbsolutePath());
        System.out.println("[DUAL-CLEANUP] MO comments   => " + comments.toAbsolutePath());
        System.out.println("[DUAL-CLEANUP] MO shortlist => " + shortlist.toAbsolutePath());
    }

    private static void writeMoEmptyStructured(Path path, String entity) throws IOException {
        String now = Instant.now().toString();
        String body = "{\n"
                + "  \"meta\": {\n"
                + "    \"schema\": \"" + MO_SCHEMA + "\",\n"
                + "    \"entity\": \"" + entity + "\",\n"
                + "    \"version\": \"1.0\",\n"
                + "    \"updatedAt\": \"" + now + "\"\n"
                + "  },\n"
                + "  \"items\": []\n"
                + "}\n";
        Files.createDirectories(path.getParent());
        Files.writeString(
                path,
                body,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    public static void main(String[] args) throws Exception {
        Mode mode;
        if (hasEffectiveCliArgs(args)) {
            mode = parseCliMode(args);
        } else {
            mode = promptInteractiveMode();
            if (mode == null) {
                return;
            }
        }

        runCleanup(mode);
    }

    private static boolean hasEffectiveCliArgs(String[] args) {
        if (args == null) {
            return false;
        }
        for (String a : args) {
            if (a != null && !a.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 无参时在终端选择；stdin 不可读时退化为「全部清理」。
     */
    private static Mode promptInteractiveMode() throws IOException {
        if (System.in == null) {
            System.out.println("[DUAL-CLEANUP] 无 stdin，按「全部清理」执行。");
            return Mode.all();
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            System.out.println();
            System.out.println("[DUAL-CLEANUP] 请选择清理模式（输入数字后回车）：");
            System.out.println("  1 — TA 申请 + TA/MO AI + MO 已读/评论/短名单（推荐，等同原无参行为）");
            System.out.println("  2 — 仅 MO 已读/评论/短名单 + MO AI（--mo-only）");
            System.out.println("  3 — 仅 TA（申请 + AI，不写 MO）（--ta-only）");
            System.out.println("  4 — TA 申请 + MO（已读/评论/短名单），跳过 TA/MO AI（--skip-ai）");
            System.out.println("  0 — 退出");
            System.out.print("> ");
            System.out.flush();
            String line = in.readLine();
            if (line == null) {
                System.out.println("[DUAL-CLEANUP] 无输入，按「全部清理」执行。");
                return Mode.all();
            }
            String key = line.trim();
            return switch (key) {
                case "1", "" -> Mode.all();
                case "2" -> Mode.forMoOnly();
                case "3" -> Mode.forTaOnly();
                case "4" -> Mode.forSkipAi();
                case "0" -> null;
                default -> {
                    System.err.println("[DUAL-CLEANUP] 无效选项: " + key);
                    System.exit(2);
                    yield null;
                }
            };
        }
    }

    private static Mode parseCliMode(String[] args) {
        boolean moOnly = false;
        boolean taOnly = false;
        boolean skipAi = false;
        for (String a : args) {
            if (a == null || a.trim().isEmpty()) {
                continue;
            }
            switch (a.trim()) {
                case "--mo-only" -> moOnly = true;
                case "--ta-only" -> taOnly = true;
                case "--skip-ai" -> skipAi = true;
                default -> {
                    System.err.println("[DUAL-CLEANUP] 未知参数: " + a);
                    System.err.println("[DUAL-CLEANUP] 可用: --mo-only | --ta-only | --skip-ai");
                    System.exit(2);
                }
            }
        }
        if (moOnly && taOnly) {
            System.err.println("[DUAL-CLEANUP] --mo-only 与 --ta-only 不能同时使用");
            System.exit(2);
        }
        return new Mode(moOnly, taOnly, skipAi);
    }

    private static void runCleanup(Mode mode) throws Exception {
        System.out.println("[DUAL-CLEANUP] data-root = " + DataMountPaths.root().toAbsolutePath());
        System.out.println("[DUAL-CLEANUP] from-env  = " + DataMountPaths.fromEnvironment());

        boolean runTa = !mode.moOnly;
        boolean runMo = !mode.taOnly;

        if (runTa) {
            System.out.println("[DUAL-CLEANUP] --- TA：申请 / 事件 / 状态 / 简历（复用 TaSubmissionCleanupTool）---");
            TaSubmissionCleanupTool.main(new String[0]);
            if (!mode.skipAi) {
                System.out.println("[DUAL-CLEANUP] --- TA：AI 数据（复用 TaAiDataCleanupTool）---");
                TaAiDataCleanupTool.main(new String[0]);
            } else {
                System.out.println("[DUAL-CLEANUP] --- 已跳过 TA AI（--skip-ai）---");
            }
        }

        if (runMo) {
            System.out.println("[DUAL-CLEANUP] --- MO：已读状态 + 申请评论 + 候选短名单 ---");
            resetMoApplicationSidecarFiles();
            if (!mode.skipAi) {
                System.out.println("[DUAL-CLEANUP] --- MO：AI 数据（复用 MoAiDataCleanupTool）---");
                MoAiDataCleanupTool.main(new String[0]);
            } else {
                System.out.println("[DUAL-CLEANUP] --- 已跳过 MO AI（--skip-ai）---");
            }
        }

        System.out.println("[DUAL-CLEANUP] --- 课程岗位：MoRecruitmentDao 按 TA 数据同步申请/已录用统计 ---");
        new MoRecruitmentDao().syncAllPublishedJobApplicationStatsFromTa();

        System.out.println("[DUAL-CLEANUP] 清理完成。");
        System.out.println("[DUAL-CLEANUP] 提示：如有课程修改操作，请运行 \"tools/genMoCourses.py\"，选择 import 模式（mode2），将 \"recruitment-courses.json\" 从 Excel 文档复原。");
    }

    private record Mode(boolean moOnly, boolean taOnly, boolean skipAi) {
        static Mode all() {
            return new Mode(false, false, false);
        }

        static Mode forMoOnly() {
            return new Mode(true, false, false);
        }

        static Mode forTaOnly() {
            return new Mode(false, true, false);
        }

        static Mode forSkipAi() {
            return new Mode(false, false, true);
        }
    }
}
