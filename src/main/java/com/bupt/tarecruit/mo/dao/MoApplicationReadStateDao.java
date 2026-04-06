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
 * MO 端申请人「已读」状态；与 TA {@code applications.json} 的 {@code updatedAt} 比较判断是否未读。
 */
public final class MoApplicationReadStateDao {

    private static final Path PATH = DataMountPaths.moApplicationReadState();
    private static final String SCHEMA = "mo";
    private static final String ENTITY = "mo-application-read-state";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public synchronized boolean isUnread(String moId, String applicationId, String applicationUpdatedAt) throws IOException {
        String lastRead = getLastReadAt(trim(moId), trim(applicationId));
        if (lastRead.isEmpty()) {
            return true;
        }
        String updated = trim(applicationUpdatedAt);
        if (updated.isEmpty()) {
            return false;
        }
        return updated.compareTo(lastRead) > 0;
    }

    public synchronized String getLastReadAt(String moId, String applicationId) throws IOException {
        JsonObject root = ensureRoot();
        JsonArray items = root.getAsJsonArray("items");
        String m = trim(moId);
        String a = trim(applicationId);
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            if (m.equalsIgnoreCase(getAsString(row, "moId")) && a.equalsIgnoreCase(getAsString(row, "applicationId"))) {
                return getAsString(row, "lastReadAt");
            }
        }
        return "";
    }

    public synchronized void markRead(String moId, String applicationId) throws IOException {
        String m = trim(moId);
        String a = trim(applicationId);
        if (m.isEmpty() || a.isEmpty()) {
            throw new IllegalArgumentException("moId 与 applicationId 不能为空");
        }
        JsonObject root = ensureRoot();
        JsonArray items = root.getAsJsonArray("items");
        String now = Instant.now().toString();
        JsonObject existing = null;
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            if (m.equalsIgnoreCase(getAsString(row, "moId")) && a.equalsIgnoreCase(getAsString(row, "applicationId"))) {
                existing = row;
                break;
            }
        }
        if (existing == null) {
            existing = new JsonObject();
            existing.addProperty("moId", m);
            existing.addProperty("applicationId", a);
            items.add(existing);
        }
        existing.addProperty("lastReadAt", now);
        touchMeta(root);
        writeRoot(root);
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
