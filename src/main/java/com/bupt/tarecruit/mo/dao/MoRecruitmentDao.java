package com.bupt.tarecruit.mo.dao;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.bupt.tarecruit.common.dao.RecruitmentCoursesDao;
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
     * 返回指定课程下**真实投递**的申请人列表（来自 TA {@code applications.json}），
     * 合并 {@code application-status.json} 中的录用/拒绝与备注，并计算 MO 未读数。
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

        String targetSlug = normalizeSlug(normalizedCourseCode);
        List<MoTaApplicationReadService.TaApplicationRecord> applications =
                readService.getApplicationsByCourseCode(normalizedCourseCode);

        JsonArray rows = new JsonArray();
        int unreadCount = 0;
        for (MoTaApplicationReadService.TaApplicationRecord rec : applications) {
            if (!rec.active()) {
                continue;
            }
            JsonObject moRow = findLatestApplicationForCourse(moStatusItems, rec.taId(), normalizedCourseCode, targetSlug);
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
                String cc = trim(getAsString(e.getAsJsonObject(), "courseCode"));
                if (cc.isBlank()) {
                    continue;
                }
                JsonObject part = getApplicantsForCourse(cc, normalizedMoId);
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
        String courseCode = firstNonBlank(getAsString(app, "courseCode"), "");
        String m = assertMoOwnsCourse(moId, courseCode);
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
        assertMoOwnsCourse(moId, rec.courseCode());

        JsonObject appStatusRoot = ensureTaApplicationStatusRoot();
        String targetSlug = normalizeSlug(rec.courseCode());
        JsonObject moRow = findLatestApplicationForCourse(appStatusRoot.getAsJsonArray("items"), rec.taId(), rec.courseCode(), targetSlug);

        MoApplicationCommentsDao commentsDao = new MoApplicationCommentsDao();
        JsonArray comments = commentsDao.listComments(applicationId);

        JsonObject detail = new JsonObject();
        detail.addProperty("success", true);
        detail.addProperty("applicationId", rec.applicationId());
        detail.addProperty("taId", rec.taId());
        detail.addProperty("courseCode", rec.courseCode());
        detail.addProperty("courseName", rec.courseName());
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
        return detail;
    }

    public synchronized JsonObject addApplicationComment(String moId, String applicationId, String text) throws IOException {
        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        MoTaApplicationReadService.TaApplicationRecord rec = readService.getApplicationById(applicationId);
        if (rec == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        String m = assertMoOwnsCourse(moId, rec.courseCode());
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
        Set<String> owned = new HashSet<>();
        for (JsonElement e : jobs) {
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject j = e.getAsJsonObject();
            if (jobOwnerMoIdMatchesMo(m, getAsString(j, "ownerMoId"))) {
                String cc = trim(getAsString(j, "courseCode")).toUpperCase(Locale.ROOT);
                if (!cc.isBlank()) {
                    owned.add(cc);
                }
            }
        }
        if (owned.isEmpty()) {
            return 0;
        }
        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        MoApplicationReadStateDao readStateDao = new MoApplicationReadStateDao();
        int n = 0;
        for (MoTaApplicationReadService.TaApplicationRecord rec : readService.getActiveApplications()) {
            if (!rec.active()) {
                continue;
            }
            if (!owned.contains(rec.courseCode().toUpperCase(Locale.ROOT))) {
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
        return firstNonBlank(rec.statusLabel(), mapTaStatusCodeToShortLabel(getAsString(rec.rawRecord(), "status")));
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
            case "SUBMITTED" -> "已投递";
            default -> c.isEmpty() ? "已投递" : code;
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
        MoTaApplicationReadService readService = new MoTaApplicationReadService();
        JsonObject appStatusRoot = ensureTaApplicationStatusRoot();
        JsonArray moStatusItems = appStatusRoot.getAsJsonArray("items");
        String targetSlug = normalizeSlug(normalizedCourseCode);
        List<MoTaApplicationReadService.TaApplicationRecord> applications =
                readService.getApplicationsByCourseCode(normalizedCourseCode);

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
            JsonObject moRow = findLatestApplicationForCourse(moStatusItems, rec.taId(), normalizedCourseCode, targetSlug);
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
            syncRecruitmentCourseApplicationStatsFromTa(cc);
        }
    }

    /**
     * 按当前 TA 数据重算并写回单门课程的申请统计（投递成功、MO 拉取应聘列表等触发）。
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
     */
    public synchronized JsonObject decideApplication(String courseCode, String taId, String decision, String comment) throws IOException {
        JsonObject job = RecruitmentCoursesDao.findNormalizedJobByCourseCode(trim(courseCode));
        String moId = firstNonBlank(getAsString(job, "ownerMoId"), "");
        if (moId.isBlank()) {
            throw new IllegalArgumentException("无法从岗位解析 ownerMoId，请使用五参数 decideApplication 并传入 moId");
        }
        return decideApplication(courseCode, taId, moId, decision, comment);
    }

    /**
     * 录用/拒绝（与 {@code TaMoSubmissionDecisionSimulator} 调用的 DAO 语义一致，但不依赖该工具类）。
     * 会写入 {@code application-status.json}，并与 TA {@code applications.json} 中的真实 {@code applicationId} 对齐。
     */
    public synchronized JsonObject decideApplication(String courseCode, String taId, String moId, String decision, String comment) throws IOException {
        String normalizedCourseCode = trim(courseCode);
        String normalizedTaId = trim(taId);
        String normalizedDecision = trim(decision).toLowerCase(Locale.ROOT);
        String normalizedComment = trim(comment);

        if (normalizedCourseCode.isBlank() || normalizedTaId.isBlank()) {
            throw new IllegalArgumentException("courseCode 与 taId 不能为空");
        }
        String normalizedMoId = assertMoOwnsCourse(moId, normalizedCourseCode);
        if (!"selected".equals(normalizedDecision) && !"rejected".equals(normalizedDecision)) {
            throw new IllegalArgumentException("decision 仅支持 selected 或 rejected");
        }

        MoTaApplicationsMutationDao mutationDao = new MoTaApplicationsMutationDao();
        JsonObject taApplication = mutationDao.findApplicationByTaAndCourse(normalizedTaId, normalizedCourseCode);

        JsonObject appRoot = ensureTaApplicationStatusRoot();
        JsonArray appItems = appRoot.getAsJsonArray("items");
        JsonObject course = RecruitmentCoursesDao.findNormalizedJobByCourseCode(normalizedCourseCode);

        String now = Instant.now().toString();
        String statusText = "selected".equals(normalizedDecision) ? "已录用" : "未录用";
        String summaryText = normalizedComment.isBlank()
                ? ("selected".equals(normalizedDecision) ? "MO 已确认录用该 TA。" : "MO 已结束该候选人的招聘流程。")
                : normalizedComment;

        JsonObject target = findLatestApplicationForCourse(appItems, normalizedTaId, normalizedCourseCode, normalizeSlug(normalizedCourseCode));
        JsonArray preservedComments = new JsonArray();
        if (target != null && target.has("comments") && target.get("comments").isJsonArray()) {
            preservedComments = target.getAsJsonArray("comments").deepCopy();
        }

        if (target == null) {
            target = new JsonObject();
            String syntheticId = "APP-" + normalizedTaId + "-" + sanitizeCode(normalizedCourseCode);
            String resolvedApplicationId = taApplication != null ? getAsString(taApplication, "applicationId") : syntheticId;
            target.addProperty("applicationId", resolvedApplicationId);
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

        if (taApplication != null) {
            target.addProperty("applicationId", getAsString(taApplication, "applicationId"));
        }
        target.addProperty("courseCode", normalizedCourseCode);
        target.addProperty("taId", normalizedTaId);
        target.addProperty("ownerMoId", normalizedMoId);
        target.addProperty("status", statusText);
        target.addProperty("statusTone", "selected".equals(normalizedDecision) ? "success" : "danger");
        target.addProperty("summary", summaryText);
        target.addProperty("moComment", summaryText);
        target.addProperty("nextAction", "selected".equals(normalizedDecision) ? "请等待签约与排班通知。" : "可继续申请其他课程岗位。");
        target.addProperty("nextStep", target.get("nextAction").getAsString());
        target.addProperty("updatedAt", now);
        target.addProperty("category", "MO 招聘流程");
        target.addProperty("matchLevel", "selected".equals(normalizedDecision) ? "高" : "中");
        if (course != null && getAsString(target, "courseName").isBlank()) {
            target.addProperty("courseName", getAsString(course, "courseName"));
        }

        touchTaApplicationStatusMeta(appRoot);
        writeJson(TA_APPLICATION_STATUS, appRoot);

        syncRecruitmentCourseApplicationStatsFromTa(normalizedCourseCode);

        JsonObject payload = new JsonObject();
        payload.addProperty("success", true);
        payload.addProperty("message", "selected".equals(normalizedDecision) ? "已录用该 TA" : "已拒绝该 TA");
        payload.addProperty("taId", normalizedTaId);
        payload.addProperty("courseCode", normalizedCourseCode);
        payload.addProperty("applicationId", getAsString(target, "applicationId"));
        payload.addProperty("status", statusText);
        payload.addProperty("updatedAt", now);
        return payload;
    }

    private JsonObject findLatestApplicationForCourse(JsonArray apps, String taId, String courseCode, String courseSlug) {
        JsonObject latest = null;
        for (JsonElement element : apps) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject app = element.getAsJsonObject();
            if (!taId.equalsIgnoreCase(getAsString(app, "taId"))) {
                continue;
            }
            String appSlug = normalizeSlug(getAsString(app, "jobSlug"));
            String appCourseName = getAsString(app, "courseName");
            String appCourseCode = trim(getAsString(app, "courseCode")).toUpperCase(Locale.ROOT);
            String codeUpper = trim(courseCode).toUpperCase(Locale.ROOT);
            boolean matched = appSlug.equals(courseSlug)
                    || (!appCourseCode.isBlank() && appCourseCode.equals(codeUpper))
                    || (!appCourseName.isBlank() && appCourseName.toLowerCase(Locale.ROOT).contains(courseCode.toLowerCase(Locale.ROOT)));
            if (!matched) {
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

}
