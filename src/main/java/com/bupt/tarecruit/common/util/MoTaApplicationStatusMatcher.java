package com.bupt.tarecruit.common.util;

import com.google.gson.JsonObject;

import java.util.Locale;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

/**
 * 将 MO 侧 {@code application-status.json} 中的条目与 TA 投递上下文对齐时使用的匹配规则。
 * <p>
 * 优先使用 {@code applicationId}、{@code jobId}；对历史数据（MO 行上尚未写入 ID）仅在
 * <strong>TA 上下文快照中也无 {@code jobId}</strong> 时，才回退为与 {@code courseCode} 一致，
 * 以免同课号多岗时把新投递误并到无 {@code jobId} 的旧 MO 行。
 */
public final class MoTaApplicationStatusMatcher {

    private MoTaApplicationStatusMatcher() {
    }

    /**
     * @param moRow         {@code application-status} 中的一条
     * @param taId          TA 标识（必相等）
     * @param applicationId TA 申请 {@code applicationId}，可为空
     * @param jobId         岗位 {@code jobId}（通常来自 TA 的 {@code courseSnapshot.jobId}），可为空
     * @param courseCode    课程编号；仅在与无 ID 的 MO 历史行对齐且上下文无 {@code jobId} 时使用
     */
    public static boolean moStatusRowMatchesTaContext(
            JsonObject moRow,
            String taId,
            String applicationId,
            String jobId,
            String courseCode) {
        if (moRow == null || trim(taId).isBlank()) {
            return false;
        }
        if (!trim(taId).equalsIgnoreCase(trim(getAsString(moRow, "taId")))) {
            return false;
        }
        String rowAppId = trim(getAsString(moRow, "applicationId"));
        String rowJobId = trim(getAsString(moRow, "jobId"));
        String ctxAppId = trim(applicationId);
        String ctxJobId = trim(jobId);
        String rowCc = trim(getAsString(moRow, "courseCode")).toUpperCase(Locale.ROOT);
        String ctxCc = trim(courseCode).toUpperCase(Locale.ROOT);

        if (!ctxAppId.isBlank() && !rowAppId.isBlank() && ctxAppId.equalsIgnoreCase(rowAppId)) {
            return true;
        }
        if (!ctxJobId.isBlank() && !rowJobId.isBlank() && ctxJobId.equalsIgnoreCase(rowJobId)) {
            return true;
        }
        if (rowAppId.isBlank()
                && rowJobId.isBlank()
                && ctxJobId.isBlank()
                && !ctxCc.isBlank()
                && rowCc.equals(ctxCc)) {
            return true;
        }
        return false;
    }
}
