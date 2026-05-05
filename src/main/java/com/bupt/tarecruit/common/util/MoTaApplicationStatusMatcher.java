package com.bupt.tarecruit.common.util;

import com.google.gson.JsonObject;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

/**
 * 将 MO 侧 {@code application-status.json} 中的条目与 TA 投递上下文对齐时使用的匹配规则。
 * <p>
 * 仅通过 {@code applicationId} 或 {@code jobId}（与上下文一致）匹配，不再按单独 {@code courseCode} 回退，
 * 避免同课号多岗位时串单。
 */
public final class MoTaApplicationStatusMatcher {

    private MoTaApplicationStatusMatcher() {
    }

    /**
     * @param moRow         {@code application-status} 中的一条
     * @param taId          TA 标识（必相等）
     * @param applicationId TA 申请 {@code applicationId}，可为空
     * @param jobId         岗位 {@code jobId}（通常来自 TA 的 {@code courseSnapshot.jobId}），可为空
     * @param courseCode    保留参数供调用方统一签名；匹配逻辑不单独依赖课号
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

        if (!ctxAppId.isBlank() && !rowAppId.isBlank() && ctxAppId.equalsIgnoreCase(rowAppId)) {
            return true;
        }
        if (!ctxJobId.isBlank() && !rowJobId.isBlank() && ctxJobId.equalsIgnoreCase(rowJobId)) {
            return true;
        }
        return false;
    }
}
