package com.bupt.tarecruit.mo.dao;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.common.dao.RecruitmentCoursesDao;
import com.bupt.tarecruit.common.util.MoTaApplicationStatusMatcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.bupt.tarecruit.mo.service.MoTaApplicationReadService;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.firstNonBlank;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsInt;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getOptionalInt;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

public class MoRecruitmentDao {
    private static final Path TA_APPLICATION_STATUS = DataMountPaths.taApplicationStatus();
    private static final String TA_SCHEMA = "ta";
    private static final String TA_ENTITY_APPLICATION_STATUS = "application-status";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * 岗位上的展示名：取自 {@code mos.json} 账号字段 {@code name}（与 profile 无关；不使用 {@code username}）。
     * 仅当查不到账号时，才回退请求体中的 {@code ownerMoName}，最后回退 {@code ownerMoId}。
     */
    private static String resolveOwnerMoName(String ownerMoId, JsonObject input) throws IOException {
        MoAccountDao.ProfileResult pr = new MoAccountDao().getProfileSettings(ownerMoId);
        if (pr.isSuccess() && pr.getData() != null) {
            Object n = pr.getData().get("name");
            if (n != null) {
                String accountName = String.valueOf(n).trim();
                if (!accountName.isBlank()) {
                    return accountName;
                }
            }
        }
        String fromInput = trim(getAsString(input, "ownerMoName"));
        if (!fromInput.isBlank()) {
            return fromInput;
        }
        return trim(ownerMoId);
    }

    private static boolean jobOwnerMoIdMatchesMo(String moId, String ownerMoIdOnJob) {
        String m = trim(moId);
        String o = trim(ownerMoIdOnJob);
        return !m.isBlank() && !o.isBlank() && o.equalsIgnoreCase(m);
    }

    private static void assertMoCampusPresent(String normalizedCampus) {
        if (normalizedCampus == null || normalizedCampus.isBlank()) {
            throw new IllegalArgumentException("请选择校区");
        }
    }

    private static void assertPositiveTaRecruitCount(Integer taRecruitCount) {
        if (taRecruitCount == null || taRecruitCount < 1) {
            throw new IllegalArgumentException("TA 招聘人数为必填且须为正整数");
        }
    }

    /**
     * 当前 MO 可见的岗位：仅当 {@code item.ownerMoId} 与登录 {@code moId} 相同（忽略大小写）。
     */
    public JsonObject getJobBoardForMo(String moId) throws IOException {
        String m = new MoAccountDao().resolveCanonicalMoId(moId);
        if (m.isBlank()) {
            throw new IllegalArgumentException("缺少 moId");
        }
        JsonObject full = RecruitmentCoursesDao.readJobBoard();
        JsonArray items = full.getAsJsonArray("items");
        JsonArray owned = new JsonArray();
        for (JsonElement e : items) {
            if (e == null || !e.isJsonObject()) {
                continue;
            }
            JsonObject j = e.getAsJsonObject();
            if (jobOwnerMoIdMatchesMo(m, getAsString(j, "ownerMoId"))) {
                owned.add(j.deepCopy());
            }
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("schema", getAsString(full, "schema"));
        payload.addProperty("version", getAsString(full, "version"));
        if (full.has("generatedAt") && !full.get("generatedAt").isJsonNull()) {
            payload.add("generatedAt", full.get("generatedAt").deepCopy());
        }
        payload.addProperty("count", owned.size());
        payload.add("items", owned);
        return payload;
    }

    /**
     * 发布课程：{@code ownerMoId} 必须为 {@code actingMoId}，不可由客户端伪造。
     */
    public JsonObject createCourse(JsonObject input, String actingMoId) throws IOException {
        String now = Instant.now().toString();
        String courseName = trim(getAsString(input, "courseName"));
        if (courseName.isEmpty()) {
            throw new IllegalArgumentException("课程名称不能为空");
        }
        String jobId = RecruitmentCoursesDao.allocateUniqueMoJobId();
        String courseCode = trim(getAsString(input, "courseCode"));
        if (courseCode.isEmpty()) {
            throw new IllegalArgumentException("课程编号不能为空");
        }
        String recruitmentStatus = firstNonBlank(
                trim(getAsString(input, "recruitmentStatus")),
                trim(getAsString(input, "status")));
        if (recruitmentStatus.isEmpty()) {
            throw new IllegalArgumentException("招聘状态不能为空");
        }
        String semester = trim(getAsString(input, "semester"));
        String applicationDeadline = trim(getAsString(input, "applicationDeadline"));

        JsonObject teachingWeeks = RecruitmentCoursesDao.normalizeTeachingWeeks(
                input.has("teachingWeeks") && input.get("teachingWeeks").isJsonObject()
                        ? input.getAsJsonObject("teachingWeeks")
                        : null);
        JsonArray assessmentEvents = RecruitmentCoursesDao.normalizeAssessmentEvents(
                input.has("assessmentEvents") ? input.get("assessmentEvents") : null);
        JsonObject requiredSkills = RecruitmentCoursesDao.normalizeRequiredSkills(
                input.has("requiredSkills") ? input.get("requiredSkills") : null);
        if (!RecruitmentCoursesDao.hasRequiredSkills(requiredSkills)) {
            throw new IllegalArgumentException("技能标签不能为空");
        }

        String courseDescription = trim(getAsString(input, "courseDescription"));
        if (courseDescription.isEmpty()) {
            throw new IllegalArgumentException("岗位描述不能为空");
        }
        String ownerMoId = new MoAccountDao().resolveCanonicalMoId(actingMoId);
        if (ownerMoId.isBlank()) {
            throw new IllegalArgumentException("缺少 moId");
        }
        String ownerMoName = resolveOwnerMoName(ownerMoId, input);
        String recruitmentBrief = trim(getAsString(input, "recruitmentBrief"));
        String workload = trim(getAsString(input, "workload"));
        String campus = RecruitmentCoursesDao.normalizeCampus(getAsString(input, "campus"));
        assertMoCampusPresent(campus);
        Integer taRecruitCount = getOptionalInt(input, "taRecruitCount");
        assertPositiveTaRecruitCount(taRecruitCount);
        Integer studentCount = getOptionalInt(input, "studentCount");
        int normalizedStudentCount = studentCount == null ? -1 : studentCount;

        JsonObject item = new JsonObject();
        item.addProperty("jobId", jobId);
        item.addProperty("courseCode", courseCode);
        item.addProperty("courseName", courseName);
        item.addProperty("ownerMoId", ownerMoId);
        item.addProperty("ownerMoName", ownerMoName);
        if (!semester.isBlank()) {
            item.addProperty("semester", semester);
        }
        item.addProperty("status", recruitmentStatus);
        item.addProperty("recruitmentStatus", recruitmentStatus);
        item.addProperty("publishStatus", "PENDING_REVIEW");
        item.addProperty("visibility", "INTERNAL");
        item.addProperty("isArchived", false);
        item.addProperty("auditStatus", "PENDING");
        item.addProperty("auditComment", "");
        item.addProperty("priority", "NORMAL");
        item.addProperty("dataVersion", 1);
        item.addProperty("lastSyncedAt", "");
        item.addProperty("studentCount", normalizedStudentCount);
        item.addProperty("recruitedCount", 0);
        item.addProperty("applicationsTotal", 0);
        item.addProperty("applicationsPending", 0);
        item.addProperty("applicationsAccepted", 0);
        item.addProperty("applicationsRejected", 0);
        item.addProperty("lastApplicationAt", "");
        item.addProperty("lastSelectionAt", "");
        item.addProperty("taRecruitCount", taRecruitCount);
        item.addProperty("campus", campus);
        if (!applicationDeadline.isBlank()) {
            item.addProperty("applicationDeadline", applicationDeadline);
        }
        if (!teachingWeeks.getAsJsonArray("weeks").isEmpty()) {
            item.add("teachingWeeks", teachingWeeks);
        }
        if (!assessmentEvents.isEmpty()) {
            item.add("assessmentEvents", assessmentEvents);
        }
        item.add("requiredSkills", requiredSkills);
        item.addProperty("courseDescription", courseDescription);
        if (!recruitmentBrief.isBlank()) {
            item.addProperty("recruitmentBrief", recruitmentBrief);
        }
        if (!workload.isBlank()) {
            item.addProperty("workload", workload);
        }
        item.addProperty("createdAt", now);
        item.addProperty("updatedAt", now);
        String source = trim(getAsString(input, "source"));
        if (!source.isBlank()) {
            item.addProperty("source", source);
        }

        RecruitmentCoursesDao.appendPublishedJob(item);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("message", "课程发布成功");
        result.add("item", item.deepCopy());
        return result;
    }

    /**
     * 更新当前 MO 已发布课程的可编辑信息（课程编号不可改）；归属与 {@code jobId} 不变。
     */
    public JsonObject updateCourse(JsonObject input, String actingMoId) throws IOException {
        String courseCode = trim(getAsString(input, "courseCode"));
        if (courseCode.isEmpty()) {
            throw new IllegalArgumentException("课程编号不能为空");
        }
        String ownerMoId = assertMoOwnsCourse(actingMoId, courseCode);

        String courseName = trim(getAsString(input, "courseName"));
        if (courseName.isEmpty()) {
            throw new IllegalArgumentException("课程名称不能为空");
        }
        String recruitmentStatus = firstNonBlank(
                trim(getAsString(input, "recruitmentStatus")),
                trim(getAsString(input, "status")));
        if (recruitmentStatus.isEmpty()) {
            throw new IllegalArgumentException("招聘状态不能为空");
        }
        String semester = trim(getAsString(input, "semester"));
        boolean hasApplicationDeadlineField = input.has("applicationDeadline");
        String applicationDeadline = hasApplicationDeadlineField ? trim(getAsString(input, "applicationDeadline")) : "";

        JsonObject teachingWeeks = RecruitmentCoursesDao.normalizeTeachingWeeks(
                input.has("teachingWeeks") && input.get("teachingWeeks").isJsonObject()
                        ? input.getAsJsonObject("teachingWeeks")
                        : null);
        JsonArray assessmentEvents = RecruitmentCoursesDao.normalizeAssessmentEvents(
                input.has("assessmentEvents") ? input.get("assessmentEvents") : null);
        JsonObject requiredSkills = RecruitmentCoursesDao.normalizeRequiredSkills(
                input.has("requiredSkills") ? input.get("requiredSkills") : null);
        if (!RecruitmentCoursesDao.hasRequiredSkills(requiredSkills)) {
            throw new IllegalArgumentException("技能标签不能为空");
        }

        String courseDescription = trim(getAsString(input, "courseDescription"));
        if (courseDescription.isEmpty()) {
            throw new IllegalArgumentException("岗位描述不能为空");
        }
        String ownerMoName = resolveOwnerMoName(ownerMoId, input);
        String recruitmentBrief = trim(getAsString(input, "recruitmentBrief"));
        String workload = input.has("workload") ? trim(getAsString(input, "workload")) : null;
        if (!input.has("campus")) {
            throw new IllegalArgumentException("请选择校区");
        }
        String campus = RecruitmentCoursesDao.normalizeCampus(getAsString(input, "campus"));
        assertMoCampusPresent(campus);
        Integer studentCount = getOptionalInt(input, "studentCount");
        int normalizedStudentCount = studentCount == null ? -1 : studentCount;

        if (!input.has("taRecruitCount")) {
            throw new IllegalArgumentException("缺少 TA 招聘人数");
        }
        Integer requestedTaRecruit = getOptionalInt(input, "taRecruitCount");
        assertPositiveTaRecruitCount(requestedTaRecruit);
        JsonObject currentJob = RecruitmentCoursesDao.findNormalizedJobByCourseCode(courseCode);
        int recruitedFromJson = currentJob == null ? 0 : getAsInt(currentJob, "recruitedCount", 0);
        TaDerivedCourseApplicationStats derived = computeTaDerivedCourseApplicationStats(courseCode);
        int floor = Math.max(derived.accepted, recruitedFromJson);
        if (requestedTaRecruit < floor) {
            throw new IllegalArgumentException("TA 招聘人数不能少于已录用人数（当前下限为 " + floor + "）");
        }

        JsonObject patch = new JsonObject();
        patch.addProperty("courseName", courseName);
        patch.addProperty("ownerMoName", ownerMoName);
        patch.addProperty("semester", semester);
        patch.addProperty("recruitmentStatus", recruitmentStatus);
        patch.addProperty("status", recruitmentStatus);
        if (hasApplicationDeadlineField) {
            if (applicationDeadline.isEmpty()) {
                patch.add("applicationDeadline", JsonNull.INSTANCE);
            } else {
                patch.addProperty("applicationDeadline", applicationDeadline);
            }
        }
        patch.add("teachingWeeks", teachingWeeks);
        patch.add("assessmentEvents", assessmentEvents);
        patch.add("requiredSkills", requiredSkills);
        patch.addProperty("courseDescription", courseDescription);
        patch.addProperty("recruitmentBrief", recruitmentBrief);
        if (workload != null) {
            patch.addProperty("workload", workload);
        }
        patch.addProperty("campus", campus);
        patch.addProperty("studentCount", normalizedStudentCount);
        patch.addProperty("taRecruitCount", requestedTaRecruit);
        if (input.has("source")) {
            String source = trim(getAsString(input, "source"));
            if (!source.isBlank()) {
                patch.addProperty("source", source);
            } else {
                patch.addProperty("source", "");
            }
        }

        if (!RecruitmentCoursesDao.mergePublishedJobContentByCourseCode(courseCode, patch)) {
            throw new IllegalArgumentException("课程不存在");
        }
        new MoTaApplicationsMutationDao().refreshCourseSnapshotsForCourseCode(courseCode);
        JsonObject item = RecruitmentCoursesDao.findNormalizedJobByCourseCode(courseCode);
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("message", "课程信息已更新");
        result.add("item", item != null ? item.deepCopy() : new JsonObject());
        return result;
    }

    /**
     * 兼容调用：按 {@code courseCode} 读取申请人列表（同课号多岗位场景下仅锚定首个规范化岗位）。
     * <p>HTTP 主路径应使用 {@link #getApplicantsForJob(String, String, String)} 以 {@code jobId} 精确定位岗位。
     *
     * @param moId 当前登录 MO，用于岗位归属校验与已读状态
     */
    public synchronized JsonObject getApplicantsForCourse(String courseCode, String moId) throws IOException {
        String normalizedCourseCode = trim(courseCode);
        if (normalizedCourseCode.isBlank()) {
            throw new IllegalArgumentException("缺少 courseCode");
        }
        String normalizedMoId = assertMoOwnsCourse(moId, normalizedCourseCode);

        syncRecruitmentCourseApplicationStatsFromTa(normalizedCourseCode);

        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        JsonObject appStatusRoot = ensureTaApplicationStatusRoot();
        JsonArray moStatusItems = appStatusRoot.getAsJsonArray("items");
        MoApplicationReadStateDao readStateDao = new MoApplicationReadStateDao();

        JsonObject publishedJob = RecruitmentCoursesDao.findNormalizedJobByCourseCode(normalizedCourseCode);
        String scopeJobId = publishedJob != null ? getAsString(publishedJob, "jobId") : "";
        List<MoTaApplicationReadService.TaApplicationRecord> applications =
                readService.getApplicationsForCourseScopedToJob(normalizedCourseCode, scopeJobId);

        JsonArray rows = new JsonArray();
        int unreadCount = 0;
        for (MoTaApplicationReadService.TaApplicationRecord rec : applications) {
            if (!rec.active()) {
                continue;
            }
            JsonObject moRow = findLatestMoStatusForApplication(
                    moStatusItems,
                    rec.taId(),
                    rec.applicationId(),
                    trim(getAsString(rec.courseSnapshot(), "jobId")),
                    normalizedCourseCode);
            String displayStatus = resolveApplicantDisplayStatus(rec, moRow);
            String commentPreview = firstNonBlank(
                    firstNonBlank(
                            moRow != null ? getAsString(moRow, "moComment") : "",
                            moRow != null ? getAsString(moRow, "summary") : ""),
                    firstNonBlank(rec.summary(), rec.statusLabel())
            );
            boolean unread = readStateDao.isUnread(normalizedMoId, rec.applicationId(), rec.updatedAt());
            if (unread) {
                unreadCount++;
            }

            JsonObject row = new JsonObject();
            row.addProperty("applicationId", rec.applicationId());
            row.addProperty("taId", rec.taId());
            row.addProperty("uniqueKey", rec.uniqueKey());
            row.addProperty("name", firstNonBlank(rec.taName(), rec.taId()));
            row.addProperty("email", rec.taEmail());
            row.addProperty("phone", rec.taPhone());
            row.addProperty("studentId", rec.taStudentId());
            row.addProperty("intent", getAsString(rec.taSnapshot(), "applicationIntent"));
            row.addProperty("skills", joinSnapshotSkills(rec.taSnapshot()));
            row.addProperty("bio", getAsString(rec.taSnapshot(), "bio"));
            row.addProperty("avatar", getAsString(rec.taSnapshot(), "avatar"));
            row.addProperty("courseCode", rec.courseCode());
            row.addProperty("courseName", rec.courseName());
            if (!trim(scopeJobId).isBlank()) {
                row.addProperty("jobId", trim(scopeJobId));
            }
            row.addProperty("status", displayStatus);
            row.addProperty("submittedAt", rec.submittedAt());
            row.addProperty("updatedAt", rec.updatedAt());
            row.addProperty("comment", commentPreview);
            row.addProperty("unread", unread);
            row.addProperty("resumeFileName", firstNonBlank(rec.resumeMeta().originalFileName(), rec.resumeMeta().storedFileName()));
            rows.add(row);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        payload.addProperty("courseCode", normalizedCourseCode);
        payload.addProperty("count", rows.size());
        payload.addProperty("unreadCount", unreadCount);
        payload.add("items", rows);
        return payload;
    }

    /**
     * 返回指定岗位（{@code jobId}）下**真实投递**的申请人列表（来自 TA {@code applications.json}），
     * 合并 {@code application-status.json}，并计算 MO 未读数。
     *
     * @param courseCodeForValidate 非空时须与岗位上的 {@code courseCode} 一致，否则抛出参数异常
     */
    public synchronized JsonObject getApplicantsForJob(String jobId, String moId, String courseCodeForValidate) throws IOException {
        String normalizedJobId = trim(jobId);
        if (normalizedJobId.isBlank()) {
            throw new IllegalArgumentException("缺少 jobId");
        }
        JsonObject publishedJob = RecruitmentCoursesDao.findNormalizedJobByJobId(normalizedJobId);
        if (publishedJob == null) {
            throw new IllegalArgumentException("岗位不存在或未发布");
        }
        String normalizedCourseCode = trim(getAsString(publishedJob, "courseCode"));
        String v = trim(courseCodeForValidate);
        if (!v.isBlank() && !v.equalsIgnoreCase(normalizedCourseCode)) {
            throw new IllegalArgumentException("jobId 与 courseCode 不一致");
        }
        String normalizedMoId = assertMoOwnsJobId(moId, normalizedJobId);

        syncRecruitmentCourseApplicationStatsFromTaByJob(normalizedCourseCode, normalizedJobId);

        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        JsonObject appStatusRoot = ensureTaApplicationStatusRoot();
        JsonArray moStatusItems = appStatusRoot.getAsJsonArray("items");
        MoApplicationReadStateDao readStateDao = new MoApplicationReadStateDao();

        List<MoTaApplicationReadService.TaApplicationRecord> applications =
                readService.getApplicationsForCourseScopedToJob(normalizedCourseCode, normalizedJobId);

        JsonArray rows = new JsonArray();
        int unreadCount = 0;
        for (MoTaApplicationReadService.TaApplicationRecord rec : applications) {
            if (!rec.active()) {
                continue;
            }
            JsonObject moRow = findLatestMoStatusForApplication(
                    moStatusItems,
                    rec.taId(),
                    rec.applicationId(),
                    trim(getAsString(rec.courseSnapshot(), "jobId")),
                    normalizedCourseCode);
            String displayStatus = resolveApplicantDisplayStatus(rec, moRow);
            String commentPreview = firstNonBlank(
                    firstNonBlank(
                            moRow != null ? getAsString(moRow, "moComment") : "",
                            moRow != null ? getAsString(moRow, "summary") : ""),
                    firstNonBlank(rec.summary(), rec.statusLabel())
            );
            boolean unread = readStateDao.isUnread(normalizedMoId, rec.applicationId(), rec.updatedAt());
            if (unread) {
                unreadCount++;
            }

            JsonObject row = new JsonObject();
            row.addProperty("applicationId", rec.applicationId());
            row.addProperty("taId", rec.taId());
            row.addProperty("uniqueKey", rec.uniqueKey());
            row.addProperty("name", firstNonBlank(rec.taName(), rec.taId()));
            row.addProperty("email", rec.taEmail());
            row.addProperty("phone", rec.taPhone());
            row.addProperty("studentId", rec.taStudentId());
            row.addProperty("intent", getAsString(rec.taSnapshot(), "applicationIntent"));
            row.addProperty("skills", joinSnapshotSkills(rec.taSnapshot()));
            row.addProperty("bio", getAsString(rec.taSnapshot(), "bio"));
            row.addProperty("avatar", getAsString(rec.taSnapshot(), "avatar"));
            row.addProperty("courseCode", rec.courseCode());
            row.addProperty("courseName", rec.courseName());
            row.addProperty("jobId", normalizedJobId);
            row.addProperty("status", displayStatus);
            row.addProperty("submittedAt", rec.submittedAt());
            row.addProperty("updatedAt", rec.updatedAt());
            row.addProperty("comment", commentPreview);
            row.addProperty("unread", unread);
            row.addProperty("resumeFileName", firstNonBlank(rec.resumeMeta().originalFileName(), rec.resumeMeta().storedFileName()));
            rows.add(row);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        payload.addProperty("jobId", normalizedJobId);
        payload.addProperty("courseCode", normalizedCourseCode);
        payload.addProperty("count", rows.size());
        payload.addProperty("unreadCount", unreadCount);
        payload.add("items", rows);
        return payload;
    }

    private String assertMoOwnsJobId(String moId, String jobId) throws IOException {
        String m = new MoAccountDao().resolveCanonicalMoId(moId);
        if (m.isBlank()) {
            throw new IllegalArgumentException("缺少 moId");
        }
        JsonObject job = RecruitmentCoursesDao.findNormalizedJobByJobId(trim(jobId));
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在或未发布");
        }
        if (!jobOwnerMoIdMatchesMo(m, getAsString(job, "ownerMoId"))) {
            throw new IllegalArgumentException("当前 MO 无权管理该岗位的申请人");
        }
        return m;
    }

    /**
     * 当前 MO 名下全部岗位下的申请人列表（合并多门课程），用于「不筛选课程」视图。
     */
    public synchronized JsonObject getApplicantsForAllMoCourses(String moId) throws IOException {
        String normalizedMoId = new MoAccountDao().resolveCanonicalMoId(moId);
        if (normalizedMoId.isBlank()) {
            throw new IllegalArgumentException("缺少 moId");
        }
        JsonObject board = getJobBoardForMo(moId);
        JsonArray jobs = board.getAsJsonArray("items");
        JsonArray mergedRows = new JsonArray();
        int unreadTotal = 0;
        if (jobs != null) {
            for (JsonElement e : jobs) {
                if (e == null || !e.isJsonObject()) {
                    continue;
                }
                String jid = trim(getAsString(e.getAsJsonObject(), "jobId"));
                if (jid.isBlank()) {
                    continue;
                }
                JsonObject part = getApplicantsForJob(jid, normalizedMoId, "");
                JsonArray rows = part.getAsJsonArray("items");
                if (rows != null) {
                    for (JsonElement row : rows) {
                        mergedRows.add(row.deepCopy());
                    }
                }
                if (part.has("unreadCount") && part.get("unreadCount") != null && !part.get("unreadCount").isJsonNull()) {
                    unreadTotal += part.get("unreadCount").getAsInt();
                }
            }
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        payload.addProperty("courseCode", "");
        payload.addProperty("count", mergedRows.size());
        payload.addProperty("unreadCount", unreadTotal);
        payload.add("items", mergedRows);
        return payload;
    }

    private void assertMoOwnsCourseResolved(String resolvedMoId, String courseCode) throws IOException {
        JsonObject job = RecruitmentCoursesDao.findNormalizedJobByCourseCode(trim(courseCode));
        if (job == null) {
            throw new IllegalArgumentException("课程不存在或未发布");
        }
        if (!jobOwnerMoIdMatchesMo(resolvedMoId, getAsString(job, "ownerMoId"))) {
            throw new IllegalArgumentException("当前 MO 无权管理该课程的申请人");
        }
    }

    /**
     * 校验当前 MO 是否可管理该课程。
     *
     * @return 规范化的 moId，供后续持久化或子 DAO 调用
     */
    public String assertMoOwnsCourse(String moId, String courseCode) throws IOException {
        String m = new MoAccountDao().resolveCanonicalMoId(moId);
        if (m.isBlank()) {
            throw new IllegalArgumentException("缺少 moId");
        }
        assertMoOwnsCourseResolved(m, courseCode);
        return m;
    }

    public synchronized JsonObject markApplicationReadByMo(String moId, String applicationId) throws IOException {
        MoTaApplicationsMutationDao mutationDao = new MoTaApplicationsMutationDao();
        JsonObject app = mutationDao.findApplicationItem(trim(applicationId));
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        JsonObject snap = app.has("courseSnapshot") && app.get("courseSnapshot").isJsonObject()
                ? app.getAsJsonObject("courseSnapshot")
                : new JsonObject();
        String jobId = trim(getAsString(snap, "jobId"));
        if (jobId.isBlank()) {
            throw new IllegalArgumentException("申请缺少岗位 jobId");
        }
        String m = assertMoOwnsJobId(moId, jobId);
        mutationDao.markUnderReview(trim(applicationId));
        new MoApplicationReadStateDao().markRead(m, trim(applicationId));
        JsonObject ok = new JsonObject();
        ok.addProperty("success", true);
        ok.addProperty("message", "已标记已读，TA 侧申请已更新为审核中");
        ok.addProperty("applicationId", trim(applicationId));
        ok.addProperty("taStatus", "UNDER_REVIEW");
        return ok;
    }

    public synchronized JsonObject getApplicantDetail(String moId, String applicationId) throws IOException {
        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        MoTaApplicationReadService.TaApplicationRecord rec = readService.getApplicationById(applicationId);
        if (rec == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        String snapJob = trim(getAsString(rec.courseSnapshot(), "jobId"));
        if (snapJob.isBlank()) {
            throw new IllegalArgumentException("申请缺少岗位 jobId");
        }
        assertMoOwnsJobId(moId, snapJob);

        JsonObject appStatusRoot = ensureTaApplicationStatusRoot();
        JsonObject moRow = findLatestMoStatusForApplication(
                appStatusRoot.getAsJsonArray("items"),
                rec.taId(),
                rec.applicationId(),
                snapJob,
                rec.courseCode());

        MoApplicationCommentsDao commentsDao = new MoApplicationCommentsDao();
        JsonArray comments = commentsDao.listComments(applicationId);

        JsonObject detail = new JsonObject();
        detail.addProperty("success", true);
        detail.addProperty("applicationId", rec.applicationId());
        detail.addProperty("taId", rec.taId());
        detail.addProperty("courseCode", rec.courseCode());
        detail.addProperty("courseName", rec.courseName());
        if (!snapJob.isBlank()) {
            detail.addProperty("jobId", snapJob);
        }
        detail.add("taSnapshot", rec.taSnapshot().deepCopy());
        detail.add("courseSnapshot", rec.courseSnapshot().deepCopy());
        JsonObject resume = new JsonObject();
        resume.addProperty("originalFileName", rec.resumeMeta().originalFileName());
        resume.addProperty("storedFileName", rec.resumeMeta().storedFileName());
        resume.addProperty("relativePath", rec.resumeMeta().relativePath());
        resume.addProperty("mimeType", rec.resumeMeta().mimeType());
        resume.addProperty("size", rec.resumeMeta().size());
        detail.add("resume", resume);
        detail.add("events", GSON.toJsonTree(readService.getApplicationEvents(applicationId)));
        detail.add("comments", comments);
        if (moRow != null) {
            detail.add("moDecision", moRow.deepCopy());
        } else {
            detail.add("moDecision", new JsonObject());
        }
        detail.addProperty("submittedAt", rec.submittedAt());
        detail.addProperty("updatedAt", rec.updatedAt());
        detail.addProperty("summary", rec.summary());
        detail.addProperty("nextAction", rec.nextAction());
        detail.addProperty("status", resolveApplicantDisplayStatus(rec, moRow));
        return detail;
    }

    public synchronized JsonObject addApplicationComment(String moId, String applicationId, String text) throws IOException {
        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        MoTaApplicationReadService.TaApplicationRecord rec = readService.getApplicationById(applicationId);
        if (rec == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        String snapJob = trim(getAsString(rec.courseSnapshot(), "jobId"));
        if (snapJob.isBlank()) {
            throw new IllegalArgumentException("申请缺少岗位 jobId");
        }
        String m = assertMoOwnsJobId(moId, snapJob);
        JsonObject comment = new MoApplicationCommentsDao().addComment(applicationId, m, text);
        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        payload.add("comment", comment);
        return payload;
    }

    public synchronized int countUnreadApplicantsForMo(String moId) throws IOException {
        String m = new MoAccountDao().resolveCanonicalMoId(moId);
        if (m.isBlank()) {
            return 0;
        }
        JsonObject board = RecruitmentCoursesDao.readJobBoard();
        JsonArray jobs = board.getAsJsonArray("items");
        Set<String> ownedJobIds = new HashSet<>();
        for (JsonElement e : jobs) {
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject j = e.getAsJsonObject();
            if (jobOwnerMoIdMatchesMo(m, getAsString(j, "ownerMoId"))) {
                String jid = trim(getAsString(j, "jobId")).toUpperCase(Locale.ROOT);
                if (!jid.isBlank()) {
                    ownedJobIds.add(jid);
                }
            }
        }
        if (ownedJobIds.isEmpty()) {
            return 0;
        }
        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        MoApplicationReadStateDao readStateDao = new MoApplicationReadStateDao();
        int n = 0;
        for (MoTaApplicationReadService.TaApplicationRecord rec : readService.getActiveApplications()) {
            if (!rec.active()) {
                continue;
            }
            String snapJob = trim(getAsString(rec.courseSnapshot(), "jobId")).toUpperCase(Locale.ROOT);
            if (snapJob.isBlank() || !ownedJobIds.contains(snapJob)) {
                continue;
            }
            if (readStateDao.isUnread(m, rec.applicationId(), rec.updatedAt())) {
                n++;
            }
        }
        return n;
    }

    private String resolveApplicantDisplayStatus(MoTaApplicationReadService.TaApplicationRecord rec, JsonObject moRow) {
        if (moRow != null) {
            String st = getAsString(moRow, "status");
            if (isAcceptedDecisionText(st)) {
                return "已录用";
            }
            if (isRejectedDecisionText(st)) {
                return "未录用";
            }
            if (!st.isBlank()) {
                return st;
            }
        }
        return taSideDisplayStatusForMo(rec);
    }

    /**
     * MO 列表/详情：不采用 TA 侧「已投递」文案；按状态码映射为「待审核」等，避免与 MO 决策后的展示混淆。
     */
    private String taSideDisplayStatusForMo(MoTaApplicationReadService.TaApplicationRecord rec) {
        String code = getAsString(rec.rawRecord(), "status");
        String mapped = mapTaStatusCodeToShortLabel(code);
        String label = trim(rec.statusLabel());
        if ("已投递".equals(label)) {
            return mapped;
        }
        if (!label.isEmpty()) {
            return label;
        }
        return mapped;
    }

    private static boolean isAcceptedDecisionText(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return status.contains("录用") && !status.contains("未录") && !status.contains("未录用");
    }

    private static boolean isRejectedDecisionText(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return status.contains("未录用") || status.contains("未通过") || status.contains("拒绝");
    }

    private static String mapTaStatusCodeToShortLabel(String code) {
        String c = trim(code).toUpperCase(Locale.ROOT);
        return switch (c) {
            case "UNDER_REVIEW" -> "审核中";
            case "SUBMITTED" -> "待审核";
            default -> c.isEmpty() ? "待审核" : code;
        };
    }

    private String joinSnapshotSkills(JsonObject taSnapshot) {
        if (taSnapshot == null || !taSnapshot.has("skills") || !taSnapshot.get("skills").isJsonArray()) {
            return "";
        }
        return joinArrayAsText(taSnapshot.get("skills"));
    }

    /**
     * 与 {@link #syncRecruitmentCourseApplicationStatsFromTa} 相同的 TA + application-status 合并规则下推导出的统计（不写入课程 JSON）。
     */
    private TaDerivedCourseApplicationStats computeTaDerivedCourseApplicationStats(String normalizedCourseCode) throws IOException {
        JsonObject publishedJob = RecruitmentCoursesDao.findNormalizedJobByCourseCode(normalizedCourseCode);
        String scopeJobId = publishedJob != null ? getAsString(publishedJob, "jobId") : "";
        return computeTaDerivedCourseApplicationStats(normalizedCourseCode, scopeJobId);
    }

    private TaDerivedCourseApplicationStats computeTaDerivedCourseApplicationStats(
            String normalizedCourseCode,
            String scopeJobId) throws IOException {
        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        JsonObject appStatusRoot = ensureTaApplicationStatusRoot();
        JsonArray moStatusItems = appStatusRoot.getAsJsonArray("items");
        List<MoTaApplicationReadService.TaApplicationRecord> applications =
                readService.getApplicationsForCourseScopedToJob(normalizedCourseCode, scopeJobId);

        int total = 0;
        int accepted = 0;
        int rejected = 0;
        String maxSubmitted = "";
        String maxSelection = "";

        for (MoTaApplicationReadService.TaApplicationRecord rec : applications) {
            if (!rec.active()) {
                continue;
            }
            total++;
            JsonObject moRow = findLatestMoStatusForApplication(
                    moStatusItems,
                    rec.taId(),
                    rec.applicationId(),
                    trim(getAsString(rec.courseSnapshot(), "jobId")),
                    normalizedCourseCode);
            String displayStatus = resolveApplicantDisplayStatus(rec, moRow);
            if (isAcceptedDecisionText(displayStatus)) {
                accepted++;
                String decisionAt = firstNonBlank(moRow != null ? getAsString(moRow, "updatedAt") : "", rec.updatedAt());
                if (decisionAt.compareTo(maxSelection) > 0) {
                    maxSelection = decisionAt;
                }
            } else if (isRejectedDecisionText(displayStatus)) {
                rejected++;
            }
            String submitted = trim(rec.submittedAt());
            if (!submitted.isEmpty() && submitted.compareTo(maxSubmitted) > 0) {
                maxSubmitted = submitted;
            }
        }

        return new TaDerivedCourseApplicationStats(total, accepted, rejected, maxSubmitted, maxSelection);
    }

    /**
     * 从 TA {@code applications.json} 与 {@code application-status.json} 重算该课程的投递统计，
     * 并写回 {@code recruitment-courses.json}（与 {@link #getApplicantsForCourse} 的合并规则一致，非简单 +1）。
     */
    private void syncRecruitmentCourseApplicationStatsFromTa(String normalizedCourseCode) throws IOException {
        TaDerivedCourseApplicationStats s = computeTaDerivedCourseApplicationStats(normalizedCourseCode);
        int pending = Math.max(0, s.total - s.accepted - s.rejected);
        String now = Instant.now().toString();
        boolean ok = RecruitmentCoursesDao.syncPublishedJobApplicationStatsByCourseCode(
                normalizedCourseCode,
                s.total,
                pending,
                s.accepted,
                s.rejected,
                s.accepted,
                s.maxSubmitted,
                s.maxSelection,
                now);
        if (!ok) {
            throw new IllegalStateException("课程统计同步失败：未找到 courseCode 对应岗位");
        }
    }

    private void syncRecruitmentCourseApplicationStatsFromTaByJob(String normalizedCourseCode, String jobId) throws IOException {
        String normalizedJobId = trim(jobId);
        if (normalizedJobId.isEmpty()) {
            syncRecruitmentCourseApplicationStatsFromTa(normalizedCourseCode);
            return;
        }
        TaDerivedCourseApplicationStats s = computeTaDerivedCourseApplicationStats(normalizedCourseCode, normalizedJobId);
        int pending = Math.max(0, s.total - s.accepted - s.rejected);
        String now = Instant.now().toString();
        boolean ok = RecruitmentCoursesDao.syncPublishedJobApplicationStatsByJobId(
                normalizedJobId,
                s.total,
                pending,
                s.accepted,
                s.rejected,
                s.accepted,
                s.maxSubmitted,
                s.maxSelection,
                now);
        if (!ok) {
            throw new IllegalStateException("岗位统计同步失败：未找到 jobId 对应岗位");
        }
    }

    /**
     * 对岗位板中每一门课程按 TA 投递与 {@code application-status.json} 重算并写回
     * {@code recruitment-courses.json} 的申请统计（{@link RecruitmentCoursesDao#syncPublishedJobApplicationStatsByCourseCode}）。
     * 双端清理等维护工具在清空 TA 数据后应调用此方法，由 DAO 写盘，不要直接改写课程 JSON。
     */
    public synchronized void syncAllPublishedJobApplicationStatsFromTa() throws IOException {
        JsonObject board = RecruitmentCoursesDao.readJobBoard();
        JsonArray items = board.getAsJsonArray("items");
        for (JsonElement el : items) {
            if (el == null || !el.isJsonObject()) {
                continue;
            }
            String cc = trim(getAsString(el.getAsJsonObject(), "courseCode"));
            if (cc.isEmpty()) {
                continue;
            }
            String jid = trim(getAsString(el.getAsJsonObject(), "jobId"));
            if (!jid.isEmpty()) {
                syncRecruitmentCourseApplicationStatsFromTaByJob(cc, jid);
            } else {
                syncRecruitmentCourseApplicationStatsFromTa(cc);
            }
        }
    }

    /**
     * 兼容调用：按课程编号触发统计重算；同课号多岗位下建议使用按 {@code jobId} 的调用路径。
     */
    public synchronized void syncPublishedJobApplicationStatsForCourse(String courseCode) throws IOException {
        String cc = trim(courseCode);
        if (cc.isEmpty()) {
            return;
        }
        syncRecruitmentCourseApplicationStatsFromTa(cc);
    }

    /**
     * 单门课程下，按 {@link #computeTaDerivedCourseApplicationStats} 与 {@link #getApplicantsForCourse} 相同合并规则
     * 从 TA {@code applications.json} + {@code application-status.json} 推导出的聚合结果；用于写回课程 JSON 或校验招聘人数下限。
     */
    private static final class TaDerivedCourseApplicationStats {
        /** 有效（active）投递条数，对应课程 JSON 的 {@code applicationsTotal}。 */
        final int total;
        /** 展示状态为已录用的人数，与 {@code recruitedCount} / {@code applicationsAccepted} 对齐。 */
        final int accepted;
        /** 展示状态为未录用/拒绝的人数，对应 {@code applicationsRejected}。 */
        final int rejected;
        /** 上述投递中最大的 {@code submittedAt}（ISO 字符串比较），用于 {@code lastApplicationAt}；无则空串。 */
        final String maxSubmitted;
        /** 已录用路径上 MO 行或申请上较新的 {@code updatedAt}，用于 {@code lastSelectionAt}；无则空串。 */
        final String maxSelection;

        private TaDerivedCourseApplicationStats(int total, int accepted, int rejected, String maxSubmitted, String maxSelection) {
            this.total = total;
            this.accepted = accepted;
            this.rejected = rejected;
            this.maxSubmitted = maxSubmitted;
            this.maxSelection = maxSelection;
        }
    }

    /**
     * 兼容旧调用方：从岗位 {@code ownerMoId} 推导 {@code moId}（仅适用于本地脚本等场景；HTTP 接口应显式传 moId）。
     *
     * @deprecated 请优先使用 {@link #decideApplicationForJob(String, String, String, String, String, String)}
     */
    @Deprecated
    public synchronized JsonObject decideApplication(String courseCode, String taId, String decision, String comment) throws IOException {
        JsonObject job = RecruitmentCoursesDao.findNormalizedJobByCourseCode(trim(courseCode));
        String moId = firstNonBlank(getAsString(job, "ownerMoId"), "");
        if (moId.isBlank()) {
            throw new IllegalArgumentException("无法从岗位解析 ownerMoId，请使用五参数 decideApplication 并传入 moId");
        }
        return decideApplication(courseCode, taId, moId, decision, comment);
    }

    /**
     * 按 {@code jobId} 录用/拒绝/撤回（HTTP 主路径）。{@code courseCodeForValidate} 非空时须与岗位课号一致。
     */
    public synchronized JsonObject decideApplicationForJob(
            String jobId, String courseCodeForValidate, String taId, String moId, String decision, String comment) throws IOException {
        String normalizedJobId = trim(jobId);
        if (normalizedJobId.isBlank()) {
            throw new IllegalArgumentException("缺少 jobId");
        }
        JsonObject course = RecruitmentCoursesDao.findNormalizedJobByJobId(normalizedJobId);
        if (course == null) {
            throw new IllegalArgumentException("岗位不存在或未发布");
        }
        String normalizedCourseCode = trim(getAsString(course, "courseCode"));
        String v = trim(courseCodeForValidate);
        if (!v.isBlank() && !v.equalsIgnoreCase(normalizedCourseCode)) {
            throw new IllegalArgumentException("jobId 与 courseCode 不一致");
        }
        String normalizedMoId = assertMoOwnsJobId(moId, normalizedJobId);
        String normalizedTaId = trim(taId);
        if (normalizedTaId.isBlank()) {
            throw new IllegalArgumentException("taId 不能为空");
        }
        String normalizedDecision = validateDecisionAndReturnNormalized(decision);
        String normalizedComment = trim(comment);
        return executeMoApplicationDecision(
                course, normalizedCourseCode, normalizedJobId, normalizedTaId, normalizedMoId, normalizedDecision, normalizedComment);
    }

    /**
     * 兼容调用：按 {@code courseCode} 录用/拒绝（与 {@code TaMoSubmissionDecisionSimulator} 调用语义一致）。
     * <p>同课号多岗位时会先解析课程对应岗位，再以该岗位 {@code jobId} 继续处理。
     *
     * @deprecated HTTP 主路径请使用 {@link #decideApplicationForJob(String, String, String, String, String, String)}
     */
    @Deprecated
    public synchronized JsonObject decideApplication(String courseCode, String taId, String moId, String decision, String comment) throws IOException {
        String normalizedCourseCode = trim(courseCode);
        String normalizedTaId = trim(taId);
        if (normalizedCourseCode.isBlank() || normalizedTaId.isBlank()) {
            throw new IllegalArgumentException("courseCode 与 taId 不能为空");
        }
        String normalizedMoId = assertMoOwnsCourse(moId, normalizedCourseCode);
        String normalizedDecision = validateDecisionAndReturnNormalized(decision);
        String normalizedComment = trim(comment);
        JsonObject course = RecruitmentCoursesDao.findNormalizedJobByCourseCode(normalizedCourseCode);
        String jobIdForLookup = course != null ? trim(getAsString(course, "jobId")) : "";
        return executeMoApplicationDecision(
                course, normalizedCourseCode, jobIdForLookup, normalizedTaId, normalizedMoId, normalizedDecision, normalizedComment);
    }

    private static String validateDecisionAndReturnNormalized(String decision) {
        String normalizedDecision = trim(decision).toLowerCase(Locale.ROOT);
        boolean decisionSelected = "selected".equals(normalizedDecision);
        boolean decisionRejected = "rejected".equals(normalizedDecision);
        boolean decisionWithdrawn = "withdrawn".equals(normalizedDecision);
        if (!decisionSelected && !decisionRejected && !decisionWithdrawn) {
            throw new IllegalArgumentException("decision 仅支持 selected、rejected 或 withdrawn（撤回录用）");
        }
        return normalizedDecision;
    }

    private JsonObject executeMoApplicationDecision(
            JsonObject course,
            String normalizedCourseCode,
            String jobIdForLookup,
            String normalizedTaId,
            String normalizedMoId,
            String normalizedDecision,
            String normalizedComment) throws IOException {
        boolean decisionSelected = "selected".equals(normalizedDecision);
        boolean decisionWithdrawn = "withdrawn".equals(normalizedDecision);

        String effectiveJobId = trim(jobIdForLookup);
        if (effectiveJobId.isBlank() && course != null) {
            effectiveJobId = trim(getAsString(course, "jobId"));
        }

        MoTaApplicationsMutationDao mutationDao = new MoTaApplicationsMutationDao();
        JsonObject taApplication = mutationDao.findApplicationByTaAndCourse(
                normalizedTaId, normalizedCourseCode, effectiveJobId);
        if (taApplication == null) {
            throw new IllegalArgumentException("未找到该岗位下的有效申请");
        }

        JsonObject appRoot = ensureTaApplicationStatusRoot();
        JsonArray appItems = appRoot.getAsJsonArray("items");

        String now = Instant.now().toString();
        String statusText;
        String defaultSummary;
        if (decisionWithdrawn) {
            statusText = "审核中";
            defaultSummary = "MO 已撤回此前结论，申请重新进入审核流程。";
        } else if (decisionSelected) {
            statusText = "已录用";
            defaultSummary = "MO 已确认录用该 TA。";
        } else {
            statusText = "未录用";
            defaultSummary = "MO 已结束该候选人的招聘流程。";
        }
        String summaryText = normalizedComment.isBlank() ? defaultSummary : normalizedComment;

        String ctxAppId = getAsString(taApplication, "applicationId");
        JsonObject target = findLatestMoStatusForApplication(
                appItems, normalizedTaId, ctxAppId, effectiveJobId, normalizedCourseCode);
        JsonArray preservedComments = new JsonArray();
        if (target != null && target.has("comments") && target.get("comments").isJsonArray()) {
            preservedComments = target.getAsJsonArray("comments").deepCopy();
        }

        if (target == null) {
            target = new JsonObject();
            target.addProperty("applicationId", ctxAppId.isBlank()
                    ? ("APP-" + normalizedTaId + "-" + sanitizeCode(normalizedCourseCode))
                    : ctxAppId);
            target.addProperty("taId", normalizedTaId);
            target.addProperty("courseCode", normalizedCourseCode);
            target.addProperty("courseName", course == null ? normalizedCourseCode : getAsString(course, "courseName"));
            target.addProperty("jobSlug", normalizeSlug(normalizedCourseCode));
            target.addProperty("ownerMoId", normalizedMoId);
            target.add("tags", new JsonArray());
            target.add("timeline", new JsonArray());
            target.add("details", new JsonArray());
            target.add("notifications", new JsonArray());
            target.add("comments", preservedComments);
            appItems.add(target);
        } else {
            target.add("comments", preservedComments);
        }

        target.addProperty("applicationId", ctxAppId.isBlank()
                ? getAsString(target, "applicationId")
                : ctxAppId);
        target.addProperty("courseCode", normalizedCourseCode);
        target.addProperty("taId", normalizedTaId);
        target.addProperty("ownerMoId", normalizedMoId);
        target.addProperty("status", statusText);
        if (decisionWithdrawn) {
            target.addProperty("statusTone", "warn");
        } else {
            target.addProperty("statusTone", decisionSelected ? "success" : "danger");
        }
        target.addProperty("summary", summaryText);
        target.addProperty("moComment", summaryText);
        if (decisionWithdrawn) {
            target.addProperty("nextAction", "课程负责人将重新审核该申请。");
        } else {
            target.addProperty("nextAction", decisionSelected ? "请等待签约与排班通知。" : "可继续申请其他课程岗位。");
        }
        target.addProperty("nextStep", target.get("nextAction").getAsString());
        target.addProperty("updatedAt", now);
        target.addProperty("category", "MO 招聘流程");
        target.addProperty("matchLevel", decisionSelected ? "高" : "中");
        if (course != null && getAsString(target, "courseName").isBlank()) {
            target.addProperty("courseName", getAsString(course, "courseName"));
        }
        if (course != null) {
            String publishedJobId = trim(getAsString(course, "jobId"));
            if (!publishedJobId.isBlank()) {
                target.addProperty("jobId", publishedJobId);
            }
        } else if (!effectiveJobId.isBlank()) {
            target.addProperty("jobId", effectiveJobId);
        }

        touchTaApplicationStatusMeta(appRoot);
        writeJson(TA_APPLICATION_STATUS, appRoot);

        if (decisionWithdrawn) {
            String appId = trim(getAsString(target, "applicationId"));
            if (!appId.isEmpty()) {
                mutationDao.markUnderReview(appId);
            }
        }

        syncRecruitmentCourseApplicationStatsFromTaByJob(normalizedCourseCode, effectiveJobId);

        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        if (decisionWithdrawn) {
            payload.addProperty("message", "已撤回此前结论，状态已恢复为审核中");
        } else {
            payload.addProperty("message", decisionSelected ? "已录用该 TA" : "已拒绝该 TA");
        }
        payload.addProperty("taId", normalizedTaId);
        payload.addProperty("courseCode", normalizedCourseCode);
        if (!effectiveJobId.isBlank()) {
            payload.addProperty("jobId", effectiveJobId);
        }
        payload.addProperty("applicationId", getAsString(target, "applicationId"));
        payload.addProperty("status", statusText);
        payload.addProperty("updatedAt", now);
        return payload;
    }

    private JsonObject findLatestMoStatusForApplication(
            JsonArray apps,
            String taId,
            String applicationId,
            String jobId,
            String courseCode) {
        JsonObject latest = null;
        for (JsonElement element : apps) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject app = element.getAsJsonObject();
            if (!MoTaApplicationStatusMatcher.moStatusRowMatchesTaContext(app, taId, applicationId, jobId, courseCode)) {
                continue;
            }
            if (latest == null || getAsString(app, "updatedAt").compareTo(getAsString(latest, "updatedAt")) > 0) {
                latest = app;
            }
        }
        return latest;
    }

    private JsonObject ensureTaApplicationStatusRoot() throws IOException {
        Path path = TA_APPLICATION_STATUS;
        Files.createDirectories(path.getParent());
        if (!Files.exists(path)) {
            JsonObject root = new JsonObject();
            JsonObject meta = new JsonObject();
            meta.addProperty("schema", TA_SCHEMA);
            meta.addProperty("entity", TA_ENTITY_APPLICATION_STATUS);
            meta.addProperty("version", "1.0");
            meta.addProperty("updatedAt", Instant.now().toString());
            root.add("meta", meta);
            root.add("items", new JsonArray());
            writeJson(path, root);
            return root;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
                if (!root.has("meta") || !root.get("meta").isJsonObject()) {
                    root.add("meta", new JsonObject());
                }
                if (!root.has("items") || !root.get("items").isJsonArray()) {
                    root.add("items", new JsonArray());
                }
                touchTaApplicationStatusMeta(root);
                return root;
            }
        }
        JsonObject fallback = new JsonObject();
        fallback.add("meta", new JsonObject());
        fallback.add("items", new JsonArray());
        touchTaApplicationStatusMeta(fallback);
        return fallback;
    }

    private void touchTaApplicationStatusMeta(JsonObject root) {
        JsonObject meta = root.getAsJsonObject("meta");
        meta.addProperty("schema", TA_SCHEMA);
        meta.addProperty("entity", TA_ENTITY_APPLICATION_STATUS);
        meta.addProperty("version", "1.0");
        meta.addProperty("updatedAt", Instant.now().toString());
    }

    private void writeJson(Path path, JsonObject root) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private String joinArrayAsText(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (JsonElement item : element.getAsJsonArray()) {
            if (item != null && !item.isJsonNull()) {
                String text = trim(item.getAsString());
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
        }
        return String.join(", ", values);
    }

    private String normalizeSlug(String text) {
        return sanitizeCode(text).toLowerCase(Locale.ROOT);
    }

    private String sanitizeCode(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    /**
     * 列出当前 MO 的候选短名单（不改变 TA 申请状态）。
     */
    public synchronized JsonObject listApplicantShortlist(String moId) throws IOException {
        String m = new MoAccountDao().resolveCanonicalMoId(moId);
        if (m.isBlank()) {
            throw new IllegalArgumentException("缺少 moId");
        }
        JsonArray rows = new MoApplicantShortlistDao().listForMo(m);
        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        payload.addProperty("count", rows.size());
        payload.add("items", rows);
        return payload;
    }

    /**
     * 加入短名单：按 {@code jobId} 校验岗位归属与申请快照一致；不写 TA 侧数据。
     */
    public synchronized JsonObject addApplicantShortlistEntry(String moId, String jobId, String applicationId, String taIdOpt, String nameOpt) throws IOException {
        String j = trim(jobId);
        String appId = trim(applicationId);
        if (j.isBlank() || appId.isBlank()) {
            throw new IllegalArgumentException("缺少 jobId 或 applicationId");
        }
        String m = assertMoOwnsJobId(moId, j);
        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        MoTaApplicationReadService.TaApplicationRecord rec = readService.getApplicationById(appId);
        if (rec == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        String snapJob = trim(getAsString(rec.courseSnapshot(), "jobId"));
        if (!j.equalsIgnoreCase(snapJob)) {
            throw new IllegalArgumentException("申请与岗位 jobId 不一致");
        }
        String cc = trim(rec.courseCode());
        String taId = firstNonBlank(trim(taIdOpt), trim(rec.taId()));
        String name = firstNonBlank(trim(nameOpt), firstNonBlank(trim(rec.taName()), trim(rec.taRealName())));
        boolean inserted = new MoApplicantShortlistDao().addEntry(m, j, cc, appId, taId, name);
        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        payload.addProperty("inserted", inserted);
        payload.addProperty("message", inserted ? "已加入短名单" : "已在短名单中");
        return payload;
    }

    /**
     * 从短名单移除一行（按申请 ID；归属由申请快照中的 {@code jobId} 校验）。
     */
    public synchronized JsonObject removeApplicantShortlistEntry(String moId, String applicationId) throws IOException {
        String appId = trim(applicationId);
        if (appId.isBlank()) {
            throw new IllegalArgumentException("缺少 applicationId");
        }
        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        MoTaApplicationReadService.TaApplicationRecord rec = readService.getApplicationById(appId);
        if (rec == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        String j = trim(getAsString(rec.courseSnapshot(), "jobId"));
        if (j.isBlank()) {
            throw new IllegalArgumentException("申请缺少岗位 jobId");
        }
        String m = assertMoOwnsJobId(moId, j);
        boolean removed = new MoApplicantShortlistDao().removeEntry(m, appId);
        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        payload.addProperty("removed", removed);
        payload.addProperty("message", removed ? "已移出短名单" : "记录不存在");
        return payload;
    }

}
