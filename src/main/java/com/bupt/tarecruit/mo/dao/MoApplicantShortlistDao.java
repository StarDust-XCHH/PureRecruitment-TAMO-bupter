package com.bupt.tarecruit.mo.dao;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.getAsString;
import static com.bupt.tarecruit.common.util.GsonJsonObjectUtils.trim;

/**
 * 持久化 MO「候选短名单」：仅 MO 侧元数据，不修改 TA 申请与状态。
 */
public final class MoApplicantShortlistDao {

    private static final Path PATH = DataMountPaths.moApplicantShortlist();
    private static final String SCHEMA = "mo";
    private static final String ENTITY = "mo-applicant-shortlist";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public synchronized JsonArray listForMo(String canonicalMoId) throws IOException {
        String m = trim(canonicalMoId);
        if (m.isBlank()) {
            return new JsonArray();
        }
        JsonObject root = ensureRoot();
        JsonArray items = root.getAsJsonArray("items");
        JsonArray out = new JsonArray();
        for (JsonElement el : items) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject row = el.getAsJsonObject();
            if (m.equalsIgnoreCase(trim(getAsString(row, "moId")))) {
                out.add(row.deepCopy());
            }
        }
        return out;
    }

    /**
     * @return true 若新插入；false 若已存在同键（幂等）
     */
    public synchronized boolean addEntry(String canonicalMoId, String courseCode, String applicationId, String taId, String name) throws IOException {
        String m = trim(canonicalMoId);
        String cc = trim(courseCode);
        String aid = trim(applicationId);
        if (m.isBlank() || cc.isBlank() || aid.isBlank()) {
            throw new IllegalArgumentException("moId、courseCode、applicationId 不能为空");
        }
        JsonObject root = ensureRoot();
        JsonArray items = root.getAsJsonArray("items");
        if (findIndex(items, m, cc, aid) >= 0) {
            return false;
        }
        JsonObject row = new JsonObject();
        row.addProperty("moId", m);
        row.addProperty("courseCode", cc);
        row.addProperty("applicationId", aid);
        row.addProperty("taId", trim(taId));
        row.addProperty("name", trim(name));
        row.addProperty("addedAt", Instant.now().toString());
        items.add(row);
        touchMeta(root);
        writeRoot(root);
        return true;
    }

    /**
     * @return true 若删除了一行
     */
    public synchronized boolean removeEntry(String canonicalMoId, String courseCode, String applicationId) throws IOException {
        String m = trim(canonicalMoId);
        String cc = trim(courseCode);
        String aid = trim(applicationId);
        if (m.isBlank() || cc.isBlank() || aid.isBlank()) {
            throw new IllegalArgumentException("moId、courseCode、applicationId 不能为空");
        }
        JsonObject root = ensureRoot();
        JsonArray items = root.getAsJsonArray("items");
        int idx = findIndex(items, m, cc, aid);
        if (idx < 0) {
            return false;
        }
        items.remove(idx);
        touchMeta(root);
        writeRoot(root);
        return true;
    }

    private static int findIndex(JsonArray items, String moId, String courseCode, String applicationId) {
        for (int i = 0; i < items.size(); i++) {
            JsonElement el = items.get(i);
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject row = el.getAsJsonObject();
            if (moId.equalsIgnoreCase(trim(getAsString(row, "moId")))
                    && courseCode.equalsIgnoreCase(trim(getAsString(row, "courseCode")))
                    && applicationId.equalsIgnoreCase(trim(getAsString(row, "applicationId")))) {
                return i;
            }
        }
        return -1;
    }

    private JsonObject ensureRoot() throws IOException {
        Files.createDirectories(PATH.getParent());
        if (!Files.exists(PATH)) {
            JsonObject root = new JsonObject();
            root.add("meta", buildMeta());
            root.add("items", new JsonArray());
            writeRoot(root);
            return root;
        }
        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
                if (!root.has("items") || !root.get("items").isJsonArray()) {
                    root.add("items", new JsonArray());
                }
                touchMeta(root);
                return root;
            }
        }
        JsonObject fallback = new JsonObject();
        fallback.add("meta", buildMeta());
        fallback.add("items", new JsonArray());
        return fallback;
    }

    private JsonObject buildMeta() {
        JsonObject meta = new JsonObject();
        meta.addProperty("schema", SCHEMA);
        meta.addProperty("entity", ENTITY);
        meta.addProperty("version", "1.0");
        meta.addProperty("updatedAt", Instant.now().toString());
        return meta;
    }

    private void touchMeta(JsonObject root) {
        if (!root.has("meta") || !root.get("meta").isJsonObject()) {
            root.add("meta", buildMeta());
            return;
        }
        root.getAsJsonObject("meta").addProperty("updatedAt", Instant.now().toString());
    }

    private void writeRoot(JsonObject root) throws IOException {
        Files.createDirectories(PATH.getParent());
        try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }
}
