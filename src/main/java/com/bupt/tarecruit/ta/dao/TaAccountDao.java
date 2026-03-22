package com.bupt.tarecruit.ta.dao;

import com.bupt.tarecruit.common.util.AuthUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Data access for TA accounts, profiles, settings, passwords, and application status.
 */
public class TaAccountDao {
    private static final String DATA_MOUNT_ENV = "mountDataTAMObupter";
    private static final Path DEFAULT_DATA_ROOT = Path.of("mountDataTAMObupter");

    private static final String TA_SCHEMA = "ta";
    private static final String TA_ENTITY = "tas";
    private static final String PROFILE_SCHEMA = "ta";
    private static final String PROFILE_ENTITY = "profiles";
    private static final String SETTINGS_SCHEMA = "ta";
    private static final String SETTINGS_ENTITY = "settings";
    private static final String APPLICATION_SCHEMA = "ta";
    private static final String APPLICATION_ENTITY = "application-status";
    private static final String JOB_BOARD_SCHEMA = "mo-pending-recruitment-courses";
    private static final String JOB_BOARD_ENTITY = "pending-recruitment-courses";

    private static final ResolvedDataRoot RESOLVED_DATA_ROOT = resolveDataRoot();
    private static final Path TA_DIR_PATH = RESOLVED_DATA_ROOT.rootPath().resolve("ta");
    private static final Path TA_DATA_PATH = TA_DIR_PATH.resolve("tas.json");
    private static final Path PROFILE_DATA_PATH = TA_DIR_PATH.resolve("profiles.json");
    private static final Path SETTINGS_DATA_PATH = TA_DIR_PATH.resolve("settings.json");
    private static final Path APPLICATION_STATUS_PATH = TA_DIR_PATH.resolve("application-status.json");
    private static final Path MO_DIR_PATH = RESOLVED_DATA_ROOT.rootPath().resolve("mo");
    private static final Path JOB_BOARD_DATA_PATH = MO_DIR_PATH.resolve("pending-recruitment-courses.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public synchronized TaLoginResult login(String identifier, String password) throws IOException {
        String normalizedIdentifier = trim(identifier);
        String rawPassword = password == null ? "" : password;

        if (normalizedIdentifier.isEmpty() || rawPassword.isEmpty()) {
            return TaLoginResult.failure(400, "Enter your account and password");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        Map<String, Object> matchedAccount = null;

        for (Map<String, Object> account : accounts) {
            if (matchesIdentifier(account, normalizedIdentifier)) {
                matchedAccount = account;
                break;
            }
        }

        if (matchedAccount == null) {
            return TaLoginResult.failure(404, "Account not found");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> auth = ensureAuthMap(matchedAccount);
        String salt = asString(auth.get("passwordSalt"));
        String passwordHash = asString(auth.get("passwordHash"));
        String currentTime = AuthUtils.nowIso();
        String lastLoginAt = asString(auth.get("lastLoginAt"));
        boolean isFirstLogin = lastLoginAt.isBlank();

        if (!AuthUtils.hashPassword(rawPassword, salt).equals(passwordHash)) {
            int currentFailures = asInt(auth.get("failedAttempts"));
            auth.put("failedAttempts", currentFailures + 1);
            matchedAccount.put("updatedAt", currentTime);
            saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
            return TaLoginResult.failure(401, "Incorrect username or password");
        }

        auth.put("failedAttempts", 0);
        auth.put("lastLoginAt", currentTime);
        matchedAccount.put("updatedAt", currentTime);
        saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taId", asString(matchedAccount.get("id")));
        data.put("name", asString(matchedAccount.get("name")));
        data.put("username", asString(matchedAccount.get("username")));
        data.put("email", asString(matchedAccount.get("email")));
        data.put("phone", asString(matchedAccount.get("phone")));
        data.put("department", asString(matchedAccount.get("department")));
        data.put("status", asString(matchedAccount.get("status")));
        data.put("loginAt", currentTime);
        data.put("isFirstLogin", isFirstLogin);
        return TaLoginResult.success(data);
    }

    public synchronized TaRegisterResult register(String taId, String name, String username, String email, String phone, String password) throws IOException {
        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        for (Map<String, Object> account : accounts) {
            if (asString(account.get("id")).equalsIgnoreCase(taId)) return TaRegisterResult.failure(409, "TA ID already exists");
            if (asString(account.get("username")).equalsIgnoreCase(username)) return TaRegisterResult.failure(409, "Username is already taken");
            if (asString(account.get("email")).equalsIgnoreCase(email)) return TaRegisterResult.failure(409, "Email is already registered");
            if (asString(account.get("phone")).equals(phone)) return TaRegisterResult.failure(409, "Phone number is already registered");
        }

        String currentTime = AuthUtils.nowIso();
        String salt = AuthUtils.generateSalt();
        String passwordHash = AuthUtils.hashPassword(password, salt);

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("id", taId);
        account.put("name", name);
        account.put("username", username);
        account.put("email", email);
        account.put("phone", phone);
        account.put("department", "");
        account.put("status", "active");
        account.put("createdAt", currentTime);
        account.put("updatedAt", currentTime);

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("passwordHash", passwordHash);
        auth.put("passwordSalt", salt);
        auth.put("lastLoginAt", "");
        auth.put("failedAttempts", 0);
        account.put("auth", auth);
        accounts.add(account);

        profiles.add(createDefaultProfile(taId, name, email, currentTime));
        settings.add(createDefaultSettings(taId, currentTime));

        saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taId", taId);
        data.put("username", username);
        data.put("createdAt", currentTime);
        return TaRegisterResult.success(data);
    }

    public synchronized ProfileResult getProfileSettings(String taId) throws IOException {
        String normalizedTaId = trim(taId);
        if (normalizedTaId.isEmpty()) {
            return ProfileResult.failure(400, "Missing TA identifier");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        Map<String, Object> account = findAccountByTaId(accounts, normalizedTaId);
        if (account == null) {
            return ProfileResult.failure(404, "TA account not found");
        }

        String now = AuthUtils.nowIso();
        Map<String, Object> profile = ensureProfileRecord(profiles, account, now);
        Map<String, Object> setting = ensureSettingsRecord(settings, normalizedTaId, now);

        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        return ProfileResult.success(mapProfileData(account, profile, setting));
    }

    public synchronized ProfileResult saveProfileSettings(ProfileUpdateInput input) throws IOException {
        if (input == null || trim(input.taId()).isEmpty()) {
            return ProfileResult.failure(400, "Missing TA identifier");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        List<Map<String, Object>> profiles = loadRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY);
        List<Map<String, Object>> settings = loadRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY);

        Map<String, Object> account = findAccountByTaId(accounts, trim(input.taId()));
        if (account == null) {
            return ProfileResult.failure(404, "TA account not found");
        }

        String now = AuthUtils.nowIso();
        Map<String, Object> profile = ensureProfileRecord(profiles, account, now);
        Map<String, Object> setting = ensureSettingsRecord(settings, trim(input.taId()), now);

        String normalizedRealName = trimToNull(input.realName());
        String normalizedIntent = trimToEmpty(input.applicationIntent());
        String normalizedEmail = trimToEmpty(input.contactEmail());
        String normalizedBio = trimToEmpty(input.bio());
        String normalizedAvatar = trimToEmpty(input.avatar());
        List<String> normalizedSkills = normalizeSkills(input.skills());

        profile.put("realName", normalizedRealName == null ? asString(account.get("name")) : normalizedRealName);
        profile.put("applicationIntent", normalizedIntent);
        profile.put("studentId", firstNonBlank(asString(profile.get("studentId")), asString(account.get("id"))));
        profile.put("contactEmail", normalizedEmail.isBlank() ? asString(account.get("email")) : normalizedEmail);
        profile.put("bio", normalizedBio);
        profile.put("avatar", normalizedAvatar);
        setting.put("avatar", normalizedAvatar);
        profile.put("skills", normalizedSkills);
        profile.put("lastUpdatedAt", now);

        if (normalizedRealName != null) {
            account.put("name", normalizedRealName);
        }
        if (!normalizedEmail.isBlank()) {
            account.put("email", normalizedEmail);
        }
        account.put("updatedAt", now);

        setting.put("profileSaved", true);
        setting.put("profileSavedAt", now);
        setting.put("lastProfileSyncStatus", "success");
        setting.put("lastProfileSyncMessage", "Profile saved successfully");
        setting.put("updatedAt", now);

        saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
        saveRecords(PROFILE_DATA_PATH, PROFILE_SCHEMA, PROFILE_ENTITY, profiles);
        saveRecords(SETTINGS_DATA_PATH, SETTINGS_SCHEMA, SETTINGS_ENTITY, settings);

        return ProfileResult.success(mapProfileData(account, profile, setting));
    }

    public synchronized PasswordUpdateResult updatePassword(String taId, String currentPassword, String newPassword) throws IOException {
        String normalizedTaId = trim(taId);
        String normalizedCurrentPassword = currentPassword == null ? "" : currentPassword;
        String normalizedNewPassword = newPassword == null ? "" : newPassword;

        if (normalizedTaId.isEmpty()) {
            return PasswordUpdateResult.failure(400, "Missing TA identifier");
        }
        if (normalizedCurrentPassword.isBlank() || normalizedNewPassword.isBlank()) {
            return PasswordUpdateResult.failure(400, "Enter your current password and a new password");
        }
        if (normalizedNewPassword.trim().length() < 6) {
            return PasswordUpdateResult.failure(400, "New password must be at least 6 characters");
        }
        if (Objects.equals(normalizedCurrentPassword, normalizedNewPassword)) {
            return PasswordUpdateResult.failure(400, "New password must differ from the current password");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        Map<String, Object> account = findAccountByTaId(accounts, normalizedTaId);
        if (account == null) {
            return PasswordUpdateResult.failure(404, "TA account not found");
        }

        Map<String, Object> auth = ensureAuthMap(account);
        String salt = asString(auth.get("passwordSalt"));
        String passwordHash = asString(auth.get("passwordHash"));
        if (!AuthUtils.hashPassword(normalizedCurrentPassword, salt).equals(passwordHash)) {
            int currentFailures = asInt(auth.get("failedAttempts"));
            auth.put("failedAttempts", currentFailures + 1);
            saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
            return PasswordUpdateResult.failure(401, "Current password is incorrect");
        }

        String now = AuthUtils.nowIso();
        String nextSalt = AuthUtils.generateSalt();
        auth.put("passwordSalt", nextSalt);
        auth.put("passwordHash", AuthUtils.hashPassword(normalizedNewPassword, nextSalt));
        auth.put("failedAttempts", 0);
        if (asString(auth.get("lastLoginAt")).isBlank()) {
            auth.put("lastLoginAt", now);
        }
        account.put("updatedAt", now);
        saveRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY, accounts);
        return PasswordUpdateResult.success();
    }

    public synchronized ApplicationStatusResult getApplicationStatus(String taId) throws IOException {
        String normalizedTaId = trim(taId);
        if (normalizedTaId.isEmpty()) {
            return ApplicationStatusResult.failure(400, "Missing TA identifier");
        }

        List<Map<String, Object>> accounts = loadRecords(TA_DATA_PATH, TA_SCHEMA, TA_ENTITY);
        Map<String, Object> account = findAccountByTaId(accounts, normalizedTaId);
        if (account == null) {
            return ApplicationStatusResult.failure(404, "TA account not found");
        }

        ensureApplicationStatusFile(normalizedTaId);
        JsonObject root = loadStructuredJson(APPLICATION_STATUS_PATH, APPLICATION_SCHEMA, APPLICATION_ENTITY);
        JsonArray items = root.getAsJsonArray("items");
        JsonArray userItems = new JsonArray();
        JsonObject summary = new JsonObject();
        summary.addProperty("totalCount", 0);
        summary.addProperty("interviewCount", 0);
        summary.addProperty("needMaterialCount", 0);
        summary.addProperty("offerCount", 0);
        summary.addProperty("closedCount", 0);
        summary.addProperty("activeCount", 0);
        summary.addProperty("estimatedFeedbackTime", "3-5 business days");
        summary.addProperty("latestStatusLabel", "No applications yet");

        int interviewCount = 0;
        int needMaterialCount = 0;
        int offerCount = 0;
        int closedCount = 0;
        String latestUpdatedAt = "";
        String latestStatusLabel = "No applications yet";
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (!normalizedTaId.equalsIgnoreCase(getAsString(item, "taId"))) {
                continue;
            }
            JsonObject normalizedItem = normalizeApplicationStatusItem(item);
            userItems.add(normalizedItem);
            String status = getAsString(normalizedItem, "status");
            if (containsAny(status, "interview", "Interview")) {
                interviewCount++;
            }
            if (containsAny(status, "material", "Material", "Pending materials")) {
                needMaterialCount++;
            }
            if (containsAny(status, "offer", "accepted", "Hired", "hired")) {
                offerCount++;
            }
            if (containsAny(status, "closed", "reject", "Not selected", "Rejected", "rejected")) {
                closedCount++;
            }
            String updatedAt = getAsString(normalizedItem, "updatedAt");
            if (latestUpdatedAt.isEmpty() || updatedAt.compareTo(latestUpdatedAt) > 0) {
                latestUpdatedAt = updatedAt;
                latestStatusLabel = status.isBlank() ? "In progress" : status;
            }
        }
        summary.addProperty("totalCount", userItems.size());
        summary.addProperty("interviewCount", interviewCount);
        summary.addProperty("needMaterialCount", needMaterialCount);
        summary.addProperty("offerCount", offerCount);
        summary.addProperty("closedCount", closedCount);
        summary.addProperty("activeCount", Math.max(userItems.size() - closedCount, 0));
        summary.addProperty("latestStatusLabel", latestStatusLabel);

        JsonArray messages = new JsonArray();
        JsonObject moNotice = new JsonObject();
        moNotice.addProperty("title", "MO notice");
        moNotice.addProperty("content", interviewCount > 0 ? "Some applications are moving forward—watch for updates in the app." : "No new interview invites yet; keep an eye on new postings.");
        messages.add(moNotice);

        JsonObject systemNotice = new JsonObject();
        systemNotice.addProperty("title", "System notice");
        systemNotice.addProperty("content", needMaterialCount > 0 ? "You have applications waiting on additional materials—please complete them soon." : "Your application statuses are up to date.");
        messages.add(systemNotice);

        JsonObject progressNotice = new JsonObject();
        progressNotice.addProperty("title", "Status summary");
        progressNotice.addProperty("content", userItems.size() > 0
                ? "You have " + userItems.size() + " application(s); latest status: \"" + latestStatusLabel + "\"."
                : "No applications yet—browse the job board to apply.");
        messages.add(progressNotice);

        return ApplicationStatusResult.success(userItems, summary, messages);
    }

    public synchronized JsonObject getPendingJobBoardData() throws IOException {
        ensureJobBoardDataFile();
        JsonObject root = loadStructuredJson(JOB_BOARD_DATA_PATH, JOB_BOARD_SCHEMA, JOB_BOARD_ENTITY);
        JsonObject normalized = new JsonObject();
        if (root.has("meta") && root.get("meta").isJsonObject()) {
            normalized.add("meta", root.getAsJsonObject("meta").deepCopy());
        }
        normalized.addProperty("schema", JOB_BOARD_SCHEMA);
        normalized.addProperty("generatedAt", getMetaString(root, "updatedAt"));
        JsonArray items = root.has("items") && root.get("items").isJsonArray()
                ? root.getAsJsonArray("items").deepCopy()
                : new JsonArray();
        normalized.addProperty("count", items.size());
        normalized.add("items", items);
        return normalized;
    }

    private List<Map<String, Object>> loadRecords(Path path, String schema, String entity) throws IOException {
        ensureDataFile(path, schema, entity);
        String content = Files.readString(path, StandardCharsets.UTF_8).trim();
        if (content.isEmpty() || content.equals("[]")) return new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (root.has("items")) {
                JsonArray itemsArray = root.getAsJsonArray("items");
                List<Map<String, Object>> result = GSON.fromJson(itemsArray, new TypeToken<List<Map<String, Object>>>() {}.getType());
                return result == null ? new ArrayList<>() : result;
            }
        } catch (Exception e) {
            System.err.println("[TA-DATA] JSON parse failed: " + path);
        }
        return new ArrayList<>();
    }

    private void saveRecords(Path path, String schema, String entity, List<Map<String, Object>> records) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("schema", schema);
        meta.put("entity", entity);
        meta.put("version", "1.0");
        meta.put("updatedAt", Instant.now().toString());

        root.put("meta", meta);
        root.put("items", records);
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private void ensureDataFile(Path path, String schema, String entity) throws IOException {
        if (Files.notExists(path.getParent())) Files.createDirectories(path.getParent());
        if (Files.notExists(path)) saveRecords(path, schema, entity, new ArrayList<>());
    }

    private JsonObject loadStructuredJson(Path path, String schema, String entity) throws IOException {
        ensureStructuredJsonFile(path, schema, entity, null);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                ensureMeta(object, schema, entity);
                if (!object.has("items") || !object.get("items").isJsonArray()) {
                    object.add("items", new JsonArray());
                }
                return object;
            }
        }
        JsonObject fallback = new JsonObject();
        JsonObject meta = new JsonObject();
        meta.addProperty("schema", schema);
        meta.addProperty("entity", entity);
        meta.addProperty("version", "1.0");
        meta.addProperty("updatedAt", "");
        fallback.add("meta", meta);
        fallback.add("items", new JsonArray());
        return fallback;
    }

    private void ensureStructuredJsonFile(Path path, String schema, String entity, JsonObject defaultRoot) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            return;
        }
        JsonObject root = defaultRoot == null ? new JsonObject() : defaultRoot.deepCopy();
        if (!root.has("meta") || !root.get("meta").isJsonObject()) {
            JsonObject meta = new JsonObject();
            meta.addProperty("schema", schema);
            meta.addProperty("entity", entity);
            meta.addProperty("version", "1.0");
            meta.addProperty("updatedAt", Instant.now().toString());
            root.add("meta", meta);
        }
        if (!root.has("items") || !root.get("items").isJsonArray()) {
            root.add("items", new JsonArray());
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private void ensureApplicationStatusFile(String taId) throws IOException {
        if (Files.notExists(APPLICATION_STATUS_PATH)) {
            ensureStructuredJsonFile(APPLICATION_STATUS_PATH, APPLICATION_SCHEMA, APPLICATION_ENTITY, buildDefaultApplicationStatusRoot(taId));
            return;
        }

        JsonObject root = loadStructuredJson(APPLICATION_STATUS_PATH, APPLICATION_SCHEMA, APPLICATION_ENTITY);
        JsonArray items = root.getAsJsonArray("items");
        boolean exists = false;
        for (JsonElement element : items) {
            if (element.isJsonObject() && taId.equalsIgnoreCase(getAsString(element.getAsJsonObject(), "taId"))) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            JsonObject appendRoot = buildDefaultApplicationStatusRoot(taId);
            for (JsonElement element : appendRoot.getAsJsonArray("items")) {
                items.add(element);
            }
            root.getAsJsonObject("meta").addProperty("updatedAt", Instant.now().toString());
            try (Writer writer = Files.newBufferedWriter(APPLICATION_STATUS_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        }
    }

    private JsonObject buildDefaultApplicationStatusRoot(String taId) {
        JsonObject root = new JsonObject();
        JsonObject meta = new JsonObject();
        meta.addProperty("schema", APPLICATION_SCHEMA);
        meta.addProperty("entity", APPLICATION_ENTITY);
        meta.addProperty("version", "1.0");
        meta.addProperty("updatedAt", Instant.now().toString());
        root.add("meta", meta);

        JsonArray items = new JsonArray();
        items.add(createApplicationStatusItem(
                taId,
                "APP-" + taId + "-01",
                "EBU6304 Software Engineering TA",
                "Under review",
                "warn",
                "Initial resume screening is complete; a course-awareness interview is being scheduled.",
                "Please keep your phone and email available for interview updates.",
                "2026-03-16T10:00:00Z",
                "software-engineering-ta",
                "Instructional support / Software engineering",
                "High",
                arrayOf("Submitted", "Resume approved", "Interview pending"),
                arrayOf(
                        createTimelineStep("Application submitted", "Your application has been recorded.", "2026-03-13T09:30:00Z", true),
                        createTimelineStep("Resume screening passed", "The MO sees a strong match with the course focus.", "2026-03-14T14:00:00Z", true),
                        createTimelineStep("Interview scheduling", "Expect an interview invitation within 3-5 business days.", "2026-03-16T10:00:00Z", false)
                ),
                arrayOf(
                        createDetailEntry("Posting ID", "EBU6304"),
                        createDetailEntry("Course area", "Software engineering"),
                        createDetailEntry("Teaching venue", "Teaching Building 1, Room A305")
                ),
                arrayOf(
                        createNotificationEntry("MO notice", "Prepare project screenshots and a case you can walk through.", "info", "2026-03-16T10:05:00Z"),
                        createNotificationEntry("System notice", "Your application is moving to the next stage.", "success", "2026-03-16T10:06:00Z")
                )
        ));
        items.add(createApplicationStatusItem(
                taId,
                "APP-" + taId + "-02",
                "EBU6301 Computer Networks TA",
                "Pending materials",
                "warn",
                "Please add materials that demonstrate hands-on networking lab experience.",
                "Upload lab screenshots or a project link within 24 hours.",
                "2026-03-15T09:00:00Z",
                "computer-network-ta",
                "Network labs / Q&A support",
                "Medium",
                arrayOf("Submitted", "Pending materials"),
                arrayOf(
                        createTimelineStep("Application submitted", "Your application has been recorded.", "2026-03-12T11:00:00Z", true),
                        createTimelineStep("Pending materials", "The MO requests proof of networking lab experience.", "2026-03-15T09:00:00Z", false)
                ),
                arrayOf(
                        createDetailEntry("Posting ID", "EBU6301"),
                        createDetailEntry("Course area", "Computer networks"),
                        createDetailEntry("Materials required", "Network lab screenshots / repository link")
                ),
                arrayOf(
                        createNotificationEntry("Materials reminder", "After you submit materials, your file re-enters the review queue.", "warn", "2026-03-15T09:05:00Z")
                )
        ));
        items.add(createApplicationStatusItem(
                taId,
                "APP-" + taId + "-03",
                "EBU6402 Databases TA",
                "Hired",
                "success",
                "Strong match between teaching experience and database project work.",
                "Watch for onboarding paperwork and your first scheduling notice.",
                "2026-03-14T08:00:00Z",
                "database-ta",
                "Databases / Lab tutoring",
                "High",
                arrayOf("Submitted", "Screening passed", "Interview completed", "Hired"),
                arrayOf(
                        createTimelineStep("Application submitted", "Your application has been recorded.", "2026-03-10T10:00:00Z", true),
                        createTimelineStep("Screening passed", "Role fit assessment looks positive.", "2026-03-11T15:00:00Z", true),
                        createTimelineStep("Interview completed", "You completed the course-awareness and Q&A practice interview.", "2026-03-13T16:00:00Z", true),
                        createTimelineStep("Hired", "Please wait for contract and scheduling details.", "2026-03-14T08:00:00Z", true)
                ),
                arrayOf(
                        createDetailEntry("Posting ID", "EBU6402"),
                        createDetailEntry("Course area", "Databases"),
                        createDetailEntry("Expected start", "2026-03-21")
                ),
                arrayOf(
                        createNotificationEntry("Offer notice", "Congratulations—you have completed all steps for this posting.", "success", "2026-03-14T08:05:00Z")
                )
        ));
        root.add("items", items);
        return root;
    }

    private JsonObject createApplicationStatusItem(String taId, String applicationId, String courseName, String status,
                                                   String statusTone, String summary, String nextAction, String updatedAt,
                                                   String jobSlug, String category, String matchLevel,
                                                   JsonArray tags, JsonArray timeline, JsonArray details, JsonArray notifications) {
        JsonObject item = new JsonObject();
        item.addProperty("applicationId", applicationId);
        item.addProperty("taId", taId);
        item.addProperty("courseName", courseName);
        item.addProperty("status", status);
        item.addProperty("statusTone", statusTone);
        item.addProperty("summary", summary);
        item.addProperty("nextAction", nextAction);
        item.addProperty("updatedAt", updatedAt);
        item.addProperty("jobSlug", jobSlug);
        item.addProperty("category", category);
        item.addProperty("matchLevel", matchLevel);
        item.add("tags", tags == null ? new JsonArray() : tags);
        item.add("timeline", timeline == null ? new JsonArray() : timeline);
        item.add("details", details == null ? new JsonArray() : details);
        item.add("notifications", notifications == null ? new JsonArray() : notifications);
        item.addProperty("moComment", summary);
        item.addProperty("nextStep", nextAction);
        return item;
    }

    private JsonObject createTimelineStep(String label, String content, String time, boolean done) {
        JsonObject item = new JsonObject();
        item.addProperty("label", label);
        item.addProperty("content", content);
        item.addProperty("time", time);
        item.addProperty("done", done);
        return item;
    }

    private JsonObject createDetailEntry(String label, String value) {
        JsonObject item = new JsonObject();
        item.addProperty("label", label);
        item.addProperty("value", value);
        return item;
    }

    private JsonObject createNotificationEntry(String title, String content, String tone, String createdAt) {
        JsonObject item = new JsonObject();
        item.addProperty("title", title);
        item.addProperty("content", content);
        item.addProperty("tone", tone);
        item.addProperty("createdAt", createdAt);
        return item;
    }

    private JsonArray arrayOf(String... values) {
        JsonArray array = new JsonArray();
        if (values == null) {
            return array;
        }
        for (String value : values) {
            array.add(value == null ? "" : value);
        }
        return array;
    }

    private JsonArray arrayOf(JsonObject... values) {
        JsonArray array = new JsonArray();
        if (values == null) {
            return array;
        }
        for (JsonObject value : values) {
            if (value != null) {
                array.add(value);
            }
        }
        return array;
    }

    private JsonObject normalizeApplicationStatusItem(JsonObject source) {
        JsonObject item = source == null ? new JsonObject() : source.deepCopy();
        item.addProperty("summary", firstNonBlank(getAsString(item, "summary"), getAsString(item, "moComment")));
        item.addProperty("nextAction", firstNonBlank(getAsString(item, "nextAction"), getAsString(item, "nextStep")));
        if (!item.has("timeline") || !item.get("timeline").isJsonArray()) {
            JsonArray timeline = new JsonArray();
            timeline.add(createTimelineStep(getAsString(item, "status"), firstNonBlank(getAsString(item, "summary"), getAsString(item, "moComment")), getAsString(item, "updatedAt"), false));
            item.add("timeline", timeline);
        }
        if (!item.has("details") || !item.get("details").isJsonArray()) {
            JsonArray details = new JsonArray();
            details.add(createDetailEntry("Posting title", getAsString(item, "courseName")));
            details.add(createDetailEntry("Current status", getAsString(item, "status")));
            item.add("details", details);
        }
        if (!item.has("notifications") || !item.get("notifications").isJsonArray()) {
            JsonArray notifications = new JsonArray();
            notifications.add(createNotificationEntry("Status update", firstNonBlank(getAsString(item, "nextAction"), getAsString(item, "nextStep")), getAsString(item, "statusTone"), getAsString(item, "updatedAt")));
            item.add("notifications", notifications);
        }
        if (!item.has("tags") || !item.get("tags").isJsonArray()) {
            item.add("tags", new JsonArray());
        }
        if (!item.has("jobSlug") || item.get("jobSlug").isJsonNull()) {
            item.addProperty("jobSlug", "");
        }
        if (!item.has("category") || item.get("category").isJsonNull()) {
            item.addProperty("category", "");
        }
        if (!item.has("matchLevel") || item.get("matchLevel").isJsonNull()) {
            item.addProperty("matchLevel", "");
        }
        return item;
    }

    private void ensureJobBoardDataFile() throws IOException {
        if (Files.exists(JOB_BOARD_DATA_PATH)) {
            return;
        }
        JsonObject root = new JsonObject();
        JsonObject meta = new JsonObject();
        meta.addProperty("schema", JOB_BOARD_SCHEMA);
        meta.addProperty("entity", JOB_BOARD_ENTITY);
        meta.addProperty("version", "1.0");
        meta.addProperty("updatedAt", Instant.now().toString());
        root.add("meta", meta);
        root.add("items", new JsonArray());
        ensureStructuredJsonFile(JOB_BOARD_DATA_PATH, JOB_BOARD_SCHEMA, JOB_BOARD_ENTITY, root);
    }

    private String getMetaString(JsonObject root, String key) {
        if (root == null || !root.has("meta") || !root.get("meta").isJsonObject()) {
            return "";
        }
        JsonObject meta = root.getAsJsonObject("meta");
        if (!meta.has(key) || meta.get(key).isJsonNull()) {
            return "";
        }
        return meta.get(key).getAsString();
    }

    private Map<String, Object> ensureProfileRecord(List<Map<String, Object>> profiles, Map<String, Object> account, String now) {
        String taId = asString(account.get("id"));
        for (Map<String, Object> profile : profiles) {
            if (asString(profile.get("taId")).equalsIgnoreCase(taId)) {
                fillProfileDefaults(profile, account, now);
                return profile;
            }
        }
        Map<String, Object> created = createDefaultProfile(taId, asString(account.get("name")), asString(account.get("email")), now);
        profiles.add(created);
        return created;
    }

    private Map<String, Object> ensureSettingsRecord(List<Map<String, Object>> settings, String taId, String now) {
        for (Map<String, Object> setting : settings) {
            if (asString(setting.get("taId")).equalsIgnoreCase(taId)) {
                fillSettingsDefaults(setting, now);
                return setting;
            }
        }
        Map<String, Object> created = createDefaultSettings(taId, now);
        settings.add(created);
        return created;
    }

    private Map<String, Object> createDefaultProfile(String taId, String name, String email, String now) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", "PROFILE-" + taId);
        profile.put("taId", taId);
        profile.put("avatar", "");
        profile.put("realName", name == null ? "" : name);
        profile.put("applicationIntent", "");
        profile.put("studentId", taId);
        profile.put("contactEmail", email == null ? "" : email);
        profile.put("bio", "");
        profile.put("skills", new ArrayList<>());
        profile.put("availabilityHoursPerWeek", 0);
        profile.put("semester", "");
        profile.put("title", "TA");
        profile.put("lastUpdatedAt", now);
        return profile;
    }

    private Map<String, Object> createDefaultSettings(String taId, String now) {
        Map<String, Object> setting = new LinkedHashMap<>();
        setting.put("id", "SETTING-" + taId);
        setting.put("taId", taId);
        setting.put("theme", "dark");
        setting.put("avatar", "");
        setting.put("onboardingStep", 0);
        setting.put("guideCompleted", false);
        setting.put("onboardingCompletedAt", "");
        setting.put("onboardingDismissed", false);
        setting.put("profileSaved", false);
        setting.put("profileSavedAt", "");
        setting.put("lastProfileSyncStatus", "idle");
        setting.put("lastProfileSyncMessage", "");
        setting.put("createdAt", now);
        setting.put("updatedAt", now);
        return setting;
    }

    private void fillProfileDefaults(Map<String, Object> profile, Map<String, Object> account, String now) {
        profile.putIfAbsent("id", "PROFILE-" + asString(account.get("id")));
        profile.putIfAbsent("taId", asString(account.get("id")));
        profile.putIfAbsent("avatar", "");
        profile.putIfAbsent("realName", asString(account.get("name")));
        profile.putIfAbsent("applicationIntent", "");
        profile.putIfAbsent("studentId", asString(account.get("id")));
        profile.putIfAbsent("contactEmail", asString(account.get("email")));
        profile.putIfAbsent("bio", "");
        if (!(profile.get("skills") instanceof List<?>)) {
            profile.put("skills", new ArrayList<>());
        }
        profile.putIfAbsent("availabilityHoursPerWeek", 0);
        profile.putIfAbsent("semester", "");
        profile.putIfAbsent("title", "TA");
        profile.putIfAbsent("lastUpdatedAt", now);
    }

    private void fillSettingsDefaults(Map<String, Object> setting, String now) {
        setting.putIfAbsent("id", "SETTING-" + asString(setting.get("taId")));
        setting.putIfAbsent("theme", "dark");
        setting.putIfAbsent("avatar", "");
        setting.putIfAbsent("onboardingStep", 0);
        setting.putIfAbsent("guideCompleted", false);
        setting.putIfAbsent("onboardingCompletedAt", "");
        setting.putIfAbsent("onboardingDismissed", false);
        setting.putIfAbsent("profileSaved", false);
        setting.putIfAbsent("profileSavedAt", "");
        setting.putIfAbsent("lastProfileSyncStatus", "idle");
        setting.putIfAbsent("lastProfileSyncMessage", "");
        setting.putIfAbsent("createdAt", now);
        setting.putIfAbsent("updatedAt", now);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ensureAuthMap(Map<String, Object> account) {
        Object current = account.get("auth");
        if (current instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("passwordHash", "");
        auth.put("passwordSalt", "");
        auth.put("lastLoginAt", "");
        auth.put("failedAttempts", 0);
        account.put("auth", auth);
        return auth;
    }

    private ProfileData mapProfileData(Map<String, Object> account, Map<String, Object> profile, Map<String, Object> setting) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taId", asString(account.get("id")));
        data.put("realName", firstNonBlank(asString(profile.get("realName")), asString(account.get("name"))));
        data.put("applicationIntent", asString(profile.get("applicationIntent")));
        data.put("studentId", firstNonBlank(asString(profile.get("studentId")), asString(account.get("id"))));
        data.put("contactEmail", firstNonBlank(asString(profile.get("contactEmail")), asString(account.get("email"))));
        data.put("bio", asString(profile.get("bio")));
        String avatar = firstNonBlank(asString(profile.get("avatar")), asString(setting.get("avatar")));
        data.put("avatar", avatar);
        data.put("skills", normalizeSkills(asStringList(profile.get("skills"))));
        data.put("lastUpdatedAt", asString(profile.get("lastUpdatedAt")));
        data.put("theme", asString(setting.get("theme")));
        data.put("settingsAvatar", asString(setting.get("avatar")));
        data.put("onboardingStep", asInt(setting.get("onboardingStep")));
        data.put("guideCompleted", asBoolean(setting.get("guideCompleted")));
        data.put("onboardingCompletedAt", asString(setting.get("onboardingCompletedAt")));
        data.put("onboardingDismissed", asBoolean(setting.get("onboardingDismissed")));
        data.put("profileSaved", asBoolean(setting.get("profileSaved")));
        data.put("profileSavedAt", asString(setting.get("profileSavedAt")));
        data.put("lastProfileSyncStatus", asString(setting.get("lastProfileSyncStatus")));
        data.put("lastProfileSyncMessage", asString(setting.get("lastProfileSyncMessage")));
        return new ProfileData(data);
    }

    private Map<String, Object> findAccountByTaId(List<Map<String, Object>> accounts, String taId) {
        for (Map<String, Object> account : accounts) {
            if (asString(account.get("id")).equalsIgnoreCase(taId)) {
                return account;
            }
        }
        return null;
    }

    private boolean matchesIdentifier(Map<String, Object> account, String identifier) {
        String storedId = asString(account.get("id"));
        String storedUsername = asString(account.get("username"));
        String storedEmail = asString(account.get("email"));
        String storedPhone = asString(account.get("phone"));

        return storedId.equalsIgnoreCase(identifier)
                || storedId.equalsIgnoreCase("TA-" + identifier)
                || storedUsername.equalsIgnoreCase(identifier)
                || storedEmail.equalsIgnoreCase(identifier)
                || storedPhone.equals(identifier);
    }

    private static ResolvedDataRoot resolveDataRoot() {
        String envValue = System.getenv(DATA_MOUNT_ENV);
        if (envValue == null || envValue.trim().isEmpty()) {
            return new ResolvedDataRoot(false, DEFAULT_DATA_ROOT.toAbsolutePath().normalize());
        }
        return new ResolvedDataRoot(true, Path.of(envValue.trim()).toAbsolutePath().normalize());
    }

    public static String getDataMountStatusMessage() {
        return "[TA-DATA] env " + DATA_MOUNT_ENV + " "
                + (RESOLVED_DATA_ROOT.fromEnvironment() ? "mounted" : "not mounted, using default directory")
                + "; dataRoot=" + RESOLVED_DATA_ROOT.rootPath();
    }

    public static Path getResolvedDataRoot() {
        return RESOLVED_DATA_ROOT.rootPath();
    }

    public static Path getResolvedTaDataDir() {
        return TA_DIR_PATH;
    }

    private void ensureMeta(JsonObject root, String schema, String entity) {
        JsonObject meta;
        if (root.has("meta") && root.get("meta").isJsonObject()) {
            meta = root.getAsJsonObject("meta");
        } else {
            meta = new JsonObject();
            root.add("meta", meta);
        }
        if (!meta.has("schema")) {
            meta.addProperty("schema", schema);
        }
        if (!meta.has("entity")) {
            meta.addProperty("entity", entity);
        }
        if (!meta.has("version")) {
            meta.addProperty("version", "1.0");
        }
        if (!meta.has("updatedAt")) {
            meta.addProperty("updatedAt", "");
        }
    }

    private boolean containsAny(String value, String... candidates) {
        String text = value == null ? "" : value.toLowerCase();
        for (String candidate : candidates) {
            if (text.contains(candidate.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String getAsString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private String trim(String value) { return value == null ? "" : value.trim(); }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed.isEmpty() ? null : trimmed;
    }
    private String asString(Object value) { return value == null ? "" : String.valueOf(value); }
    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(asString(value));
    }

    private int asInt(Object value) {
        if (value instanceof Number num) return num.intValue();
        try {
            return (int) Double.parseDouble(asString(value));
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                String text = trimToEmpty(asString(item));
                if (!text.isBlank()) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private List<String> normalizeSkills(List<String> source) {
        List<String> normalized = new ArrayList<>();
        if (source == null) {
            return normalized;
        }
        for (String item : source) {
            String text = trimToEmpty(item);
            if (!text.isBlank() && !normalized.contains(text)) {
                normalized.add(text);
            }
        }
        return normalized;
    }

    private String firstNonBlank(String primary, String fallback) {
        String first = trim(primary);
        return first.isEmpty() ? trimToEmpty(fallback) : first;
    }

    private record ResolvedDataRoot(boolean fromEnvironment, Path rootPath) {}

    public static class ProfileData {
        private final Map<String, Object> data;

        public ProfileData(Map<String, Object> data) {
            this.data = data;
        }

        public Map<String, Object> toMap() {
            return data;
        }
    }

    public static class TaLoginResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;
        private TaLoginResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success; this.status = status; this.message = message; this.data = data;
        }
        public static TaLoginResult success(Map<String, Object> data) { return new TaLoginResult(true, 200, "Signed in successfully", data); }
        public static TaLoginResult failure(int status, String message) { return new TaLoginResult(false, status, message, null); }
        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    public static class TaRegisterResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;
        private TaRegisterResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success; this.status = status; this.message = message; this.data = data;
        }
        public static TaRegisterResult success(Map<String, Object> data) { return new TaRegisterResult(true, 201, "Registered successfully", data); }
        public static TaRegisterResult failure(int status, String message) { return new TaRegisterResult(false, status, message, null); }
        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    public record ProfileUpdateInput(String taId, String realName, String applicationIntent, String contactEmail,
                                     String bio, List<String> skills, String avatar) {
    }

    public static class ProfileResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final Map<String, Object> data;
        private ProfileResult(boolean success, int status, String message, Map<String, Object> data) {
            this.success = success; this.status = status; this.message = message; this.data = data;
        }
        public static ProfileResult success(ProfileData data) { return new ProfileResult(true, 200, "OK", data.toMap()); }
        public static ProfileResult failure(int status, String message) { return new ProfileResult(false, status, message, null); }
        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }

    public static class PasswordUpdateResult {
        private final boolean success;
        private final int status;
        private final String message;
        private PasswordUpdateResult(boolean success, int status, String message) {
            this.success = success; this.status = status; this.message = message;
        }
        public static PasswordUpdateResult success() { return new PasswordUpdateResult(true, 200, "Password updated"); }
        public static PasswordUpdateResult failure(int status, String message) { return new PasswordUpdateResult(false, status, message); }
        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
    }

    public static class ApplicationStatusResult {
        private final boolean success;
        private final int status;
        private final String message;
        private final JsonArray items;
        private final JsonObject summary;
        private final JsonArray messages;

        private ApplicationStatusResult(boolean success, int status, String message, JsonArray items, JsonObject summary, JsonArray messages) {
            this.success = success;
            this.status = status;
            this.message = message;
            this.items = items;
            this.summary = summary;
            this.messages = messages;
        }

        public static ApplicationStatusResult success(JsonArray items, JsonObject summary, JsonArray messages) {
            return new ApplicationStatusResult(true, 200, "OK", items, summary, messages);
        }

        public static ApplicationStatusResult failure(int status, String message) {
            return new ApplicationStatusResult(false, status, message, new JsonArray(), new JsonObject(), new JsonArray());
        }

        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public JsonArray getItems() { return items; }
        public JsonObject getSummary() { return summary; }
        public JsonArray getMessages() { return messages; }
    }
}
