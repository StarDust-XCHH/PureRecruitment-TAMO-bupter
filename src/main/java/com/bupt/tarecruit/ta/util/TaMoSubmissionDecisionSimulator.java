package com.bupt.tarecruit.ta.util;

import com.bupt.tarecruit.mo.dao.MoRecruitmentDao;
import com.google.gson.JsonObject;

/**
 * 独立运行的 MO -> TA 申请状态模拟工具。
 * <p>
 * 说明：
 * 1. 不修改 MO 模块代码，仅直接调用 {@link MoRecruitmentDao#decideApplication(String, String, String, String)}
 * 2. 用于本地模拟 MO 对 TA 已提交简历的处理结果（录用 / 拒绝）
 * 3. 运行后，TA 侧申请状态页可通过既有后端接口读取到最新结果
 * <p>
 * 命令示例（项目根目录）：
 * <pre>
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.ta.util.TaMoSubmissionDecisionSimulator -Dexec.args="CS101 TA-10001 selected 已进入后续排班流程"
 * mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.ta.util.TaMoSubmissionDecisionSimulator -Dexec.args="CS101 TA-10001 rejected 本轮岗位名额已满，感谢投递"
 * </pre>
 */
public final class TaMoSubmissionDecisionSimulator {

    private static final String DECISION_SELECTED = "selected";
    private static final String DECISION_REJECTED = "rejected";

    private TaMoSubmissionDecisionSimulator() {
    }

    public static void main(String[] args) throws Exception {
        String courseCode;
        String taId;
        String decision;
        String comment;

        if (args != null && args.length >= 3) {
            courseCode = trim(args[0]);
            taId = trim(args[1]);
            decision = normalizeDecision(args[2]);
            comment = joinComment(args, 3);
        } else {
            JsonObject latestApplication = findLatestSubmittedApplication();
            if (latestApplication == null) {
                printUsageAndExit("参数不足，且未发现可自动识别的已投递申请");
                return;
            }
            courseCode = trim(getString(latestApplication, "courseCode"));
            taId = trim(getString(latestApplication, "taId"));
            decision = promptDecision();
            comment = promptComment(decision);
            System.out.println("[TA-MO-SIMULATOR] 未传入参数，已自动选择最新申请记录");
            System.out.println("[TA-MO-SIMULATOR] auto courseCode = " + courseCode);
            System.out.println("[TA-MO-SIMULATOR] auto taId = " + taId);
            System.out.println("[TA-MO-SIMULATOR] auto decision = " + decision);
        }

        if (courseCode.isEmpty()) {
            printUsageAndExit("courseCode 不能为空");
            return;
        }
        if (taId.isEmpty()) {
            printUsageAndExit("taId 不能为空");
            return;
        }
        if (!DECISION_SELECTED.equals(decision) && !DECISION_REJECTED.equals(decision)) {
            printUsageAndExit("decision 仅支持 selected 或 rejected");
            return;
        }

        MoRecruitmentDao recruitmentDao = new MoRecruitmentDao();
        JsonObject result = recruitmentDao.decideApplication(courseCode, taId, decision, comment);

        System.out.println("[TA-MO-SIMULATOR] 执行完成");
        System.out.println("[TA-MO-SIMULATOR] courseCode = " + courseCode);
        System.out.println("[TA-MO-SIMULATOR] taId = " + taId);
        System.out.println("[TA-MO-SIMULATOR] decision = " + decision);
        System.out.println("[TA-MO-SIMULATOR] status = " + getString(result, "status"));
        System.out.println("[TA-MO-SIMULATOR] updatedAt = " + getString(result, "updatedAt"));
        System.out.println("[TA-MO-SIMULATOR] message = " + getString(result, "message"));
    }

    private static String normalizeDecision(String value) {
        String normalized = trim(value).toLowerCase();
        if ("1".equals(normalized) || "pass".equals(normalized) || "approve".equals(normalized) || "accepted".equals(normalized) || "通过".equals(normalized)) {
            return DECISION_SELECTED;
        }
        if ("2".equals(normalized) || "reject".equals(normalized) || "refuse".equals(normalized) || "rejected".equals(normalized) || "拒绝".equals(normalized)) {
            return DECISION_REJECTED;
        }
        return normalized;
    }

    private static String promptDecision() throws Exception {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        while (true) {
            System.out.println("[TA-MO-SIMULATOR] 请选择处理结果：1=通过，2=拒绝");
            System.out.print("[TA-MO-SIMULATOR] 输入 1 或 2 后回车: ");
            String input = reader.readLine();
            String decision = normalizeDecision(input);
            if (DECISION_SELECTED.equals(decision) || DECISION_REJECTED.equals(decision)) {
                return decision;
            }
            System.out.println("[TA-MO-SIMULATOR] 无效输入，请重新输入 1(通过) 或 2(拒绝)");
        }
    }

    private static String promptComment(String decision) throws Exception {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        String hint = DECISION_SELECTED.equals(decision) ? "已进入后续排班流程" : "本轮岗位名额已满，感谢投递";
        System.out.print("[TA-MO-SIMULATOR] 可选填写备注，直接回车则使用默认文案（示例：" + hint + "）: ");
        return trim(reader.readLine());
    }

    private static JsonObject findLatestSubmittedApplication() throws Exception {
        java.nio.file.Path applicationsPath = com.bupt.tarecruit.common.config.DataMountPaths.taApplications();
        if (!java.nio.file.Files.exists(applicationsPath)) {
            return null;
        }
        String json = java.nio.file.Files.readString(applicationsPath);
        JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("items") || !root.get("items").isJsonArray()) {
            return null;
        }

        JsonObject latest = null;
        String latestUpdatedAt = "";
        for (com.google.gson.JsonElement element : root.getAsJsonArray("items")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            String currentTaId = trim(getString(item, "taId"));
            String currentCourseCode = trim(getString(item, "courseCode"));
            String updatedAt = trim(getString(item, "updatedAt"));
            if (currentTaId.isEmpty() || currentCourseCode.isEmpty()) {
                continue;
            }
            if (latest == null || updatedAt.compareTo(latestUpdatedAt) > 0) {
                latest = item;
                latestUpdatedAt = updatedAt;
            }
        }
        return latest;
    }

    private static String joinComment(String[] args, int startIndex) {
        if (args == null || startIndex >= args.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            String current = trim(args[i]);
            if (current.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static void printUsageAndExit(String message) {
        System.err.println("[TA-MO-SIMULATOR] " + message);
        System.err.println("用法: <courseCode> <taId> <decision:selected|rejected> [comment...]");
        System.err.println("示例1: CS101 TA-10001 selected 已进入后续排班流程");
        System.err.println("示例2: EBU9900 TA-10258 rejected 本轮岗位名额已满");
        System.err.println("补充: 若不传参运行，将自动读取最新一条已投递申请，并提示你选择 通过/拒绝");
        System.exit(1);
    }
}
