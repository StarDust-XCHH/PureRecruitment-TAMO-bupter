package com.bupt.tarecruit.tools;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.mo.dao.MoRecruitmentDao;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 测试环境用：<strong>TA 申请数据 + TA AI 数据 + MO 侧与申请相关的旁路 JSON</strong> 一次性对齐清理。
 * <p>
 * 典型场景：只运行了 {@link TaSubmissionCleanupTool} 后，{@code mo-application-read-state.json}、
 * {@code mo-application-comments.json} 仍引用已不存在的 {@code applicationId}，导致未读红点、评论等残留；
 * 本工具在<strong>不修改</strong>既有 {@link TaSubmissionCleanupTool} / {@link TaAiDataCleanupTool} 源码的前提下，
 * 通过调用其 {@code main} 并重写 MO 两个文件，使双端与「零申请」状态一致。
 * 清理结束后会调用 {@link MoRecruitmentDao#syncAllPublishedJobApplicationStatsFromTa()}，按当前 TA 数据重算并写回
 * {@code recruitment-courses.json} 中的申请/已录用统计；本工具<strong>不直接改写</strong>课程 JSON。
 * <p>
 * <strong>不清理</strong>：课程岗位条目本身（仅刷新统计字段）、MO/TA 账号与 profile（{@code mos.json}、{@code tas.json} 等）。
 * <p>
 * 命令行参数（可选）：
 * <ul>
 *     <li>（无参）— 在终端打印菜单，由数字键选择模式；若 stdin 不可用则退化为「全部清理」。清理完成后若 stdin 可用，可再选择是否运行 {@code tools/genMoCourses.py import}</li>
 *     <li>{@code --mo-only} — 仅重置 MO 两个 JSON（TA 已手动清过或仅需修不同步）</li>
 *     <li>{@code --ta-only} — 仅调用两个 TA 工具，不写 MO 文件</li>
 *     <li>{@code --skip-ai} — 跳过 {@link TaAiDataCleanupTool}，其余与无参相同</li>
 *     <li>{@code --no-gen-mo-courses-offer} — 清理结束后不询问是否运行 {@code tools/genMoCourses.py import}（Excel→JSON）</li>
 * </ul>
 * <p>
 * 运行示例（仓库根目录）：见 {@code tools/ta-mo-submission-cleanup/README.md}。
 * <pre>{@code
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.tools.DevApplicationDataCleanupTool
 * }</pre>
 */
public final class DevApplicationDataCleanupTool {

    /** {@link #runGenMoCoursesImport}：无法启动子进程（含未安装 Python、PATH 无解释器等）。 */
    private static final int GEN_MO_COURSES_NO_PYTHON = -2;

    private static final String MO_SCHEMA = "mo";

    private DevApplicationDataCleanupTool() {
    }

    /**
     * 仅重置 MO 端「已读」「评论」结构化文件（与 {@code MoApplicationReadStateDao} / {@code MoApplicationCommentsDao} 契约一致）。
     */
    public static void resetMoApplicationSidecarFiles() throws IOException {
        Path readState = DataMountPaths.moApplicationReadState();
        Path comments = DataMountPaths.moApplicationComments();
        writeMoEmptyStructured(readState, "mo-application-read-state");
        writeMoEmptyStructured(comments, "mo-application-comments");
        System.out.println("[DUAL-CLEANUP] MO read-state => " + readState.toAbsolutePath());
        System.out.println("[DUAL-CLEANUP] MO comments   => " + comments.toAbsolutePath());
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
        boolean skipGenMoCoursesOffer;
        if (hasEffectiveCliArgs(args)) {
            CliParseResult parsed = parseCliArgs(args);
            mode = parsed.mode();
            skipGenMoCoursesOffer = parsed.skipGenMoCoursesOffer();
        } else {
            mode = promptInteractiveMode();
            if (mode == null) {
                return;
            }
            skipGenMoCoursesOffer = false;
        }

        runCleanup(mode);
        if (!skipGenMoCoursesOffer) {
            maybeOfferGenMoCoursesImport();
        }
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
            System.out.println("  1 — TA 申请 + TA AI + MO 已读/评论（推荐，等同原无参行为）");
            System.out.println("  2 — 仅 MO 已读/评论 JSON（--mo-only）");
            System.out.println("  3 — 仅 TA（申请 + AI，不写 MO）（--ta-only）");
            System.out.println("  4 — TA 申请 + MO，跳过 TA AI（--skip-ai）");
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

    private record CliParseResult(Mode mode, boolean skipGenMoCoursesOffer) {
    }

    private static CliParseResult parseCliArgs(String[] args) {
        boolean moOnly = false;
        boolean taOnly = false;
        boolean skipAi = false;
        boolean skipGenMoCoursesOffer = false;
        for (String a : args) {
            if (a == null || a.trim().isEmpty()) {
                continue;
            }
            switch (a.trim()) {
                case "--mo-only" -> moOnly = true;
                case "--ta-only" -> taOnly = true;
                case "--skip-ai" -> skipAi = true;
                case "--no-gen-mo-courses-offer" -> skipGenMoCoursesOffer = true;
                default -> {
                    System.err.println("[DUAL-CLEANUP] 未知参数: " + a);
                    System.err.println("[DUAL-CLEANUP] 可用: --mo-only | --ta-only | --skip-ai | --no-gen-mo-courses-offer");
                    System.exit(2);
                }
            }
        }
        if (moOnly && taOnly) {
            System.err.println("[DUAL-CLEANUP] --mo-only 与 --ta-only 不能同时使用");
            System.exit(2);
        }
        return new CliParseResult(new Mode(moOnly, taOnly, skipAi), skipGenMoCoursesOffer);
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
            System.out.println("[DUAL-CLEANUP] --- MO：已读状态 + 申请评论 ---");
            resetMoApplicationSidecarFiles();
        }

        System.out.println("[DUAL-CLEANUP] --- 课程岗位：MoRecruitmentDao 按 TA 数据同步申请/已录用统计 ---");
        new MoRecruitmentDao().syncAllPublishedJobApplicationStatsFromTa();

        System.out.println("[DUAL-CLEANUP] 完成。");
    }

    /**
     * 在仓库根（{@code user.dir}）下执行 {@code tools/genMoCourses.py import}，并把当前数据挂载下的
     * {@code recruitment-courses.json} 以绝对路径传给 {@code --courses-json}，与 {@link DataMountPaths} 一致。
     */
    private static void maybeOfferGenMoCoursesImport() {
        if (System.in == null) {
            return;
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            System.out.println();
            System.out.println("[DUAL-CLEANUP] 是否运行 tools/genMoCourses.py import（按 Excel 覆盖写 recruitment-courses.json）？");
            System.out.println("  y / yes / 是 — 执行；其它或直接回车 — 跳过");
            System.out.print("> ");
            System.out.flush();
            String line = in.readLine();
            if (line == null || !affirmsGenMoCoursesImport(line.trim())) {
                System.out.println("[DUAL-CLEANUP] 已跳过 Excel→JSON。");
                return;
            }
            Path repoRoot = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
            Path script = repoRoot.resolve("tools").resolve("genMoCourses.py");
            if (!Files.isRegularFile(script)) {
                System.err.println("[DUAL-CLEANUP] 未找到脚本: " + script.toAbsolutePath());
                return;
            }
            Path coursesJson = DataMountPaths.moRecruitmentCourses().toAbsolutePath().normalize();
            int code = runGenMoCoursesImport(repoRoot, script, coursesJson);
            if (code == 0) {
                System.out.println("[DUAL-CLEANUP] genMoCourses import 已完成。");
            } else if (code != GEN_MO_COURSES_NO_PYTHON) {
                System.err.println("[DUAL-CLEANUP] genMoCourses import 失败（退出码 " + code + "）。");
                System.err.println("[DUAL-CLEANUP] 若提示缺少 pandas/openpyxl，请执行: pip install pandas openpyxl");
            }
            // code == GEN_MO_COURSES_NO_PYTHON：已在 runGenMoCoursesImport 内打印说明（含未装 Python 等情况）
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[DUAL-CLEANUP] genMoCourses 被中断。");
        } catch (IOException e) {
            System.err.println("[DUAL-CLEANUP] 读取输入时出错: " + e.getMessage());
        }
    }

    /** 无法启动子进程时调用：不打栈，不影响已将双端清理视为成功的语义。 */
    private static void printGenMoCoursesLaunchSkipped(String osMessage) {
        System.err.println("[DUAL-CLEANUP] 未能启动 tools/genMoCourses.py（Excel→JSON 已跳过）。");
        System.err.println("[DUAL-CLEANUP] 双端清理本身已成功完成，不受影响。");
        if (osMessage != null && !osMessage.isBlank()) {
            System.err.println("[DUAL-CLEANUP] 系统说明: " + osMessage);
        }
        System.err.println("[DUAL-CLEANUP] 常见原因：本机未安装 Python、未加入 PATH，或解释器名称不同；");
        System.err.println("[DUAL-CLEANUP] 可安装 Python 3 并 pip install pandas openpyxl，或在仓库根手动运行:");
        System.err.println("[DUAL-CLEANUP]   python tools/genMoCourses.py import");
        System.err.println("[DUAL-CLEANUP] 若不需要此步骤，可加: --no-gen-mo-courses-offer");
    }

    private static boolean affirmsGenMoCoursesImport(String s) {
        if (s.isEmpty()) {
            return false;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        return lower.equals("y") || lower.equals("yes") || s.equals("是");
    }

    /**
     * @return {@code 0} 成功；{@link #GEN_MO_COURSES_NO_PYTHON} 表示无法启动子进程（已在控制台打印说明）；否则为子进程退出码
     */
    private static int runGenMoCoursesImport(Path repoRoot, Path script, Path coursesJson)
            throws InterruptedException {
        String scriptArg = script.toAbsolutePath().toString();
        String coursesArg = coursesJson.toString();
        List<String[]> prefixes = List.of(
                new String[]{"python"},
                new String[]{"python3"},
                new String[]{"py", "-3"}
        );
        IOException lastIo = null;
        for (String[] prefix : prefixes) {
            List<String> cmd = new ArrayList<>();
            Collections.addAll(cmd, prefix);
            cmd.add(scriptArg);
            cmd.add("import");
            cmd.add("--courses-json");
            cmd.add(coursesArg);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(repoRoot.toFile());
            pb.inheritIO();
            try {
                Process proc = pb.start();
                return proc.waitFor();
            } catch (IOException e) {
                lastIo = e;
            }
        }
        IOException failure = Objects.requireNonNull(lastIo, "expected IOException after failed launches");
        String msg = failure.getMessage() != null ? failure.getMessage() : failure.getClass().getSimpleName();
        printGenMoCoursesLaunchSkipped(msg);
        return GEN_MO_COURSES_NO_PYTHON;
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
