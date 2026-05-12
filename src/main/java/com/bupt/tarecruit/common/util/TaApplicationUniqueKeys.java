package com.bupt.tarecruit.common.util;

import java.util.Locale;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

/**
 * TA 投递在 {@code applications.json} 中的 {@code uniqueKey} 规范，用于区分「同课号、不同岗位实例（学期）」。
 */
public final class TaApplicationUniqueKeys {

    private TaApplicationUniqueKeys() {
    }

    /**
     * 规范键：{@code TAID::COURSECODE}；若 {@code jobId} 非空则为 {@code TAID::COURSECODE::JOBID}。
     */
    public static String canonical(String taId, String courseCode, String jobId) {
        String t = trim(taId).toUpperCase(Locale.ROOT);
        String c = trim(courseCode).toUpperCase(Locale.ROOT);
        String j = trim(jobId).toUpperCase(Locale.ROOT);
        if (j.isEmpty()) {
            return t + "::" + c;
        }
        return t + "::" + c + "::" + j;
    }
}
