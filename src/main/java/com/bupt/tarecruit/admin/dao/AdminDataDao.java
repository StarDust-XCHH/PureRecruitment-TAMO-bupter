package com.bupt.tarecruit.admin.dao;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块数据访问对象。
 * 提供读取 TA、MO 用户及系统数据的能力。
 */
public class AdminDataDao {

    private static final Path TA_DIR = DataMountPaths.taDir();
    private static final Path TA_ACCOUNTS_PATH = DataMountPaths.taAccounts();
    private static final Path TA_PROFILES_PATH = DataMountPaths.taProfiles();
    private static final Path TA_SETTINGS_PATH = TA_DIR.resolve("settings.json");

    private static final Path MO_DIR = DataMountPaths.moDir();
    private static final Path MO_ACCOUNTS_PATH = DataMountPaths.moAccounts();
    private static final Path MO_PROFILES_PATH = DataMountPaths.moProfiles();

    private static final Path ADMIN_DIR = DataMountPaths.adminDir();
    private static final Path ADMIN_ACCOUNTS_PATH = DataMountPaths.adminAccounts();
    private static final Path ADMIN_PROFILES_PATH = DataMountPaths.adminProfiles();
    private static final Path ADMIN_SETTINGS_PATH = DataMountPaths.adminSettings();

    private static final Path RECRUITMENT_COURSES_PATH = DataMountPaths.moRecruitmentCourses();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * 获取所有 TA 用户基本信息
     */
    public List<Map<String, Object>> getAllTaUsers() throws IOException {
        return loadJsonRecords(TA_ACCOUNTS_PATH);
    }

    /**
     * 获取所有 TA 用户设置信息
     */
    public List<Map<String, Object>> getAllTaSettings() throws IOException {
        return loadJsonRecords(TA_SETTINGS_PATH);
    }

    /**
     * 获取所有 TA 用户 profile 信息
     */
    public List<Map<String, Object>> getAllTaProfiles() throws IOException {
        return loadJsonRecords(TA_PROFILES_PATH);
    }

    /**
     * 获取所有 MO 用户基本信息
     */
    public List<Map<String, Object>> getAllMoUsers() throws IOException {
        return loadJsonRecords(MO_ACCOUNTS_PATH);
    }

    /**
     * 获取所有 MO 用户 profile 信息
     */
    public List<Map<String, Object>> getAllMoProfiles() throws IOException {
        return loadJsonRecords(MO_PROFILES_PATH);
    }

    /**
     * 获取所有 Admin 用户基本信息
     */
    public List<Map<String, Object>> getAllAdminUsers() throws IOException {
        return loadJsonRecords(ADMIN_ACCOUNTS_PATH);
    }

    /**
     * 获取所有 Admin 用户 profile 信息
     */
    public List<Map<String, Object>> getAllAdminProfiles() throws IOException {
        return loadJsonRecords(ADMIN_PROFILES_PATH);
    }

    /**
     * 获取所有 Admin 用户 settings 信息
     */
    public List<Map<String, Object>> getAllAdminSettings() throws IOException {
        return loadJsonRecords(ADMIN_SETTINGS_PATH);
    }

    /**
     * 获取 Admin 用户及其 profile 信息的合并视图
     */
    public List<Map<String, Object>> getAdminUsersWithProfiles() throws IOException {
        List<Map<String, Object>> users = getAllAdminUsers();
        List<Map<String, Object>> profiles = getAllAdminProfiles();
        List<Map<String, Object>> settings = getAllAdminSettings();

        // 构建 profiles 映射
        Map<String, Map<String, Object>> profilesMap = new HashMap<>();
        for (Map<String, Object> profile : profiles) {
            String adminId = String.valueOf(profile.getOrDefault("adminId", ""));
            profilesMap.put(adminId, profile);
        }

        // 构建 settings 映射
        Map<String, Map<String, Object>> settingsMap = new HashMap<>();
        for (Map<String, Object> setting : settings) {
            String adminId = String.valueOf(setting.getOrDefault("adminId", ""));
            settingsMap.put(adminId, setting);
        }

        // 合并数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> user : users) {
            Map<String, Object> merged = new HashMap<>(user);
            String adminId = String.valueOf(user.getOrDefault("id", ""));
            Map<String, Object> userProfile = profilesMap.get(adminId);
            Map<String, Object> userSettings = settingsMap.get(adminId);

            if (userProfile != null) {
                merged.put("realName", userProfile.getOrDefault("realName", ""));
                merged.put("title", userProfile.getOrDefault("title", "管理员"));
                merged.put("contactEmail", userProfile.getOrDefault("contactEmail", ""));
                merged.put("bio", userProfile.getOrDefault("bio", ""));
                merged.put("permissions", userProfile.getOrDefault("permissions", new ArrayList<>()));
            }

            if (userSettings != null) {
                merged.put("theme", userSettings.getOrDefault("theme", "dark"));
                merged.put("notifications", userSettings.getOrDefault("notifications", new HashMap<>()));
                merged.put("dashboardLayout", userSettings.getOrDefault("dashboardLayout", "default"));
            }

            // 简化 auth 字段
            Object authObj = merged.get("auth");
            if (authObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> auth = (Map<String, Object>) authObj;
                merged.put("lastLoginAt", auth.getOrDefault("lastLoginAt", ""));
                merged.put("failedAttempts", auth.getOrDefault("failedAttempts", 0));
                merged.remove("auth");
            }

            merged.put("role", "ADMIN");
            result.add(merged);
        }

        return result;
    }

    /**
     * 获取所有用户（TA + MO + Admin）的合并视图
     */
    public List<Map<String, Object>> getAllUsers() throws IOException {
        List<Map<String, Object>> taUsers = getTaUsersWithSettings();
        List<Map<String, Object>> moUsers = getMoUsersWithProfiles();
        List<Map<String, Object>> adminUsers = getAdminUsersWithProfiles();
        List<Map<String, Object>> allUsers = new ArrayList<>(taUsers);
        allUsers.addAll(moUsers);
        allUsers.addAll(adminUsers);
        return allUsers;
    }

    /**
     * 获取用户统计信息
     */
    public Map<String, Object> getUserStats() throws IOException {
        List<Map<String, Object>> taUsers = getTaUsersWithSettings();
        List<Map<String, Object>> moUsers = getMoUsersWithProfiles();
        List<Map<String, Object>> adminUsers = getAdminUsersWithProfiles();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", taUsers.size() + moUsers.size() + adminUsers.size());
        stats.put("taUsers", taUsers.size());
        stats.put("moUsers", moUsers.size());
        stats.put("adminUsers", adminUsers.size());

        return stats;
    }

    /**
     * 获取 MO 用户及其 profile 信息的合并视图
     */
    public List<Map<String, Object>> getMoUsersWithProfiles() throws IOException {
        List<Map<String, Object>> users = getAllMoUsers();
        List<Map<String, Object>> profiles = getAllMoProfiles();

        // 构建 profiles 映射
        Map<String, Map<String, Object>> profilesMap = new HashMap<>();
        for (Map<String, Object> profile : profiles) {
            String moId = String.valueOf(profile.getOrDefault("moId", ""));
            profilesMap.put(moId, profile);
        }

        // 合并数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> user : users) {
            Map<String, Object> merged = new HashMap<>(user);
            String moId = String.valueOf(user.getOrDefault("id", ""));
            Map<String, Object> userProfile = profilesMap.get(moId);

            if (userProfile != null) {
                merged.put("realName", userProfile.getOrDefault("realName", ""));
                merged.put("title", userProfile.getOrDefault("title", ""));
                merged.put("contactEmail", userProfile.getOrDefault("contactEmail", ""));
                merged.put("bio", userProfile.getOrDefault("bio", ""));
            }

            // 简化 auth 字段
            Object authObj = merged.get("auth");
            if (authObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> auth = (Map<String, Object>) authObj;
                merged.put("lastLoginAt", auth.getOrDefault("lastLoginAt", ""));
                merged.put("failedAttempts", auth.getOrDefault("failedAttempts", 0));
                merged.remove("auth");
            }

            merged.put("role", "MO");
            result.add(merged);
        }

        return result;
    }

    /**
     * 获取 TA 用户及其 profile 信息的合并视图
     */
    public List<Map<String, Object>> getTaUsersWithProfiles() throws IOException {
        List<Map<String, Object>> users = getAllTaUsers();
        List<Map<String, Object>> profiles = getAllTaProfiles();

        // 构建 profiles 映射
        Map<String, Map<String, Object>> profilesMap = new HashMap<>();
        for (Map<String, Object> profile : profiles) {
            String taId = String.valueOf(profile.getOrDefault("taId", ""));
            profilesMap.put(taId, profile);
        }

        // 合并数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> user : users) {
            Map<String, Object> merged = new HashMap<>(user);
            String taId = String.valueOf(user.getOrDefault("id", ""));
            Map<String, Object> userProfile = profilesMap.get(taId);

            if (userProfile != null) {
                merged.put("realName", userProfile.getOrDefault("realName", ""));
                merged.put("studentId", userProfile.getOrDefault("studentId", ""));
                merged.put("contactEmail", userProfile.getOrDefault("contactEmail", ""));
                merged.put("bio", userProfile.getOrDefault("bio", ""));
                merged.put("skills", userProfile.getOrDefault("skills", new ArrayList<>()));
                merged.put("title", userProfile.getOrDefault("title", "TA"));
            }

            // 简化 auth 字段
            Object authObj = merged.get("auth");
            if (authObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> auth = (Map<String, Object>) authObj;
                merged.put("lastLoginAt", auth.getOrDefault("lastLoginAt", ""));
                merged.put("failedAttempts", auth.getOrDefault("failedAttempts", 0));
                merged.remove("auth");
            }

            merged.put("role", "TA");
            result.add(merged);
        }

        return result;
    }

    /**
     * 获取 TA 用户及其设置信息的合并视图（保留原有方法，兼容前端）
     */
    public List<Map<String, Object>> getTaUsersWithSettings() throws IOException {
        List<Map<String, Object>> users = getAllTaUsers();
        List<Map<String, Object>> settings = getAllTaSettings();
        List<Map<String, Object>> profiles = getAllTaProfiles();

        // 构建 settings 映射
        Map<String, Map<String, Object>> settingsMap = new HashMap<>();
        for (Map<String, Object> setting : settings) {
            String taId = String.valueOf(setting.getOrDefault("taId", ""));
            settingsMap.put(taId, setting);
        }

        // 构建 profiles 映射
        Map<String, Map<String, Object>> profilesMap = new HashMap<>();
        for (Map<String, Object> profile : profiles) {
            String taId = String.valueOf(profile.getOrDefault("taId", ""));
            profilesMap.put(taId, profile);
        }

        // 合并数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> user : users) {
            Map<String, Object> merged = new HashMap<>(user);
            String taId = String.valueOf(user.getOrDefault("id", ""));
            Map<String, Object> userSettings = settingsMap.get(taId);
            Map<String, Object> userProfile = profilesMap.get(taId);

            if (userSettings != null) {
                merged.put("theme", userSettings.getOrDefault("theme", "dark"));
                merged.put("profileSaved", userSettings.getOrDefault("profileSaved", false));
                merged.put("onboardingCompleted", userSettings.getOrDefault("onboardingCompletedAt", ""));
            }

            if (userProfile != null) {
                merged.put("realName", userProfile.getOrDefault("realName", ""));
                merged.put("studentId", userProfile.getOrDefault("studentId", ""));
                merged.put("contactEmail", userProfile.getOrDefault("contactEmail", ""));
                merged.put("bio", userProfile.getOrDefault("bio", ""));
                merged.put("skills", userProfile.getOrDefault("skills", new ArrayList<>()));
                merged.put("title", userProfile.getOrDefault("title", "TA"));
            }

            // 简化 auth 字段，只保留 lastLoginAt
            Object authObj = merged.get("auth");
            if (authObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> auth = (Map<String, Object>) authObj;
                merged.put("lastLoginAt", auth.getOrDefault("lastLoginAt", ""));
                merged.put("failedAttempts", auth.getOrDefault("failedAttempts", 0));
                merged.remove("auth");
            }

            merged.put("role", "TA");
            result.add(merged);
        }

        return result;
    }

    /**
     * 获取所有课程信息（从 recruitment-courses.json）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllCourses() throws IOException {
        List<Map<String, Object>> courses = new ArrayList<>();

        if (!Files.exists(RECRUITMENT_COURSES_PATH)) {
            return courses;
        }

        String content = Files.readString(RECRUITMENT_COURSES_PATH, StandardCharsets.UTF_8);
        JsonObject root = GSON.fromJson(content, JsonObject.class);

        if (root.has("items")) {
            var items = root.getAsJsonArray("items");
            for (var item : items) {
                Map<String, Object> course = new HashMap<>();
                JsonObject courseObj = item.getAsJsonObject();

                // 提取关键字段
                course.put("courseId", getJsonString(courseObj, "courseCode"));
                course.put("courseName", getJsonString(courseObj, "courseName"));
                course.put("semester", getJsonString(courseObj, "semester"));
                course.put("moId", getJsonString(courseObj, "ownerMoId"));
                course.put("moName", getJsonString(courseObj, "ownerMoName"));
                course.put("status", getJsonString(courseObj, "recruitmentStatus"));
                course.put("studentCount", courseObj.has("studentCount") ? courseObj.get("studentCount").getAsInt() : 0);
                course.put("taRecruitCount", courseObj.has("taRecruitCount") ? courseObj.get("taRecruitCount").getAsInt() : 0);
                course.put("campus", getJsonString(courseObj, "campus"));
                course.put("jobId", getJsonString(courseObj, "jobId"));
                course.put("applicationDeadline", getJsonString(courseObj, "applicationDeadline"));

                // 提取技能标签
                if (courseObj.has("requiredSkills")) {
                    JsonObject skills = courseObj.getAsJsonObject("requiredSkills");
                    List<String> fixedTags = new ArrayList<>();
                    if (skills.has("fixedTags")) {
                        var tags = skills.getAsJsonArray("fixedTags");
                        for (var tag : tags) {
                            fixedTags.add(tag.getAsString());
                        }
                    }
                    course.put("skills", fixedTags);
                } else {
                    course.put("skills", new ArrayList<>());
                }

                // 提取申请统计
                course.put("applicationsTotal", courseObj.has("applicationsTotal") ? courseObj.get("applicationsTotal").getAsInt() : 0);
                course.put("applicationsPending", courseObj.has("applicationsPending") ? courseObj.get("applicationsPending").getAsInt() : 0);
                course.put("applicationsAccepted", courseObj.has("applicationsAccepted") ? courseObj.get("applicationsAccepted").getAsInt() : 0);
                course.put("recruitedCount", courseObj.has("recruitedCount") ? courseObj.get("recruitedCount").getAsInt() : 0);

                // 创建时间
                course.put("createdAt", getJsonString(courseObj, "createdAt"));
                course.put("updatedAt", getJsonString(courseObj, "updatedAt"));

                courses.add(course);
            }
        }

        return courses;
    }

    /**
     * 获取课程统计数据
     */
    public Map<String, Object> getCourseStats() throws IOException {
        List<Map<String, Object>> courses = getAllCourses();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCourses", courses.size());

        long openCount = courses.stream()
                .filter(c -> "OPEN".equalsIgnoreCase(String.valueOf(c.getOrDefault("status", ""))))
                .count();
        stats.put("openCourses", openCount);

        long closedCount = courses.stream()
                .filter(c -> "CLOSED".equalsIgnoreCase(String.valueOf(c.getOrDefault("status", ""))))
                .count();
        stats.put("closedCourses", closedCount);

        int totalTaRecruitCount = courses.stream()
                .mapToInt(c -> (Integer) c.getOrDefault("taRecruitCount", 0))
                .sum();
        stats.put("totalTaRecruitCount", totalTaRecruitCount);

        int totalApplications = courses.stream()
                .mapToInt(c -> (Integer) c.getOrDefault("applicationsTotal", 0))
                .sum();
        stats.put("totalApplications", totalApplications);

        return stats;
    }

    /**
     * 辅助方法：安全获取 JSON 字符串
     */
    private String getJsonString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return "";
        }
        return obj.get(key).getAsString();
    }

    /**
     * 加载 JSON 文件中的记录列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadJsonRecords(Path filePath) throws IOException {
        List<Map<String, Object>> records = new ArrayList<>();

        if (!Files.exists(filePath)) {
            return records;
        }

        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        JsonObject root = GSON.fromJson(content, JsonObject.class);

        if (root.has("items")) {
            var items = root.getAsJsonArray("items");
            for (var item : items) {
                Map<String, Object> record = GSON.fromJson(item, Map.class);
                records.add(record);
            }
        }

        return records;
    }
}
