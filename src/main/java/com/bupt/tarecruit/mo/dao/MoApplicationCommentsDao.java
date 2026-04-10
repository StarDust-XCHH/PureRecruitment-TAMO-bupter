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
 * MO 对某条 TA 申请的评论列表（按 applicationId 分组）。
 */
public final class MoApplicationCommentsDao {

    private static final Path PATH = DataMountPaths.moApplicationComments();
    private static final String SCHEMA = "mo";
    private static final String ENTITY = "mo-application-comments";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public synchronized JsonArray listComments(String applicationId) throws IOException {
        JsonObject thread = findThread(trim(applicationId));
        if (thread == null || !thread.has("comments") || !thread.get("comments").isJsonArray()) {
            return new JsonArray();
        }
        return thread.getAsJsonArray("comments").deepCopy();
    }

    public synchronized JsonObject addComment(String applicationId, String moId, String text) throws IOException {
        String appId = trim(applicationId);
        String m = trim(moId);
        String body = trim(text);
        if (appId.isEmpty() || m.isEmpty()) {
            throw new IllegalArgumentException("applicationId 与 moId 不能为空");
        }
        if (body.isEmpty()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        JsonObject root = ensureRoot();
        JsonArray items = root.getAsJsonArray("items");
        JsonObject thread = null;
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            if (appId.equalsIgnoreCase(getAsString(row, "applicationId"))) {
                thread = row;
                break;
            }
        }
        if (thread == null) {
            thread = new JsonObject();
            thread.addProperty("applicationId", appId);
            thread.add("comments", new JsonArray());
            items.add(thread);
        }
        if (!thread.has("comments") || !thread.get("comments").isJsonArray()) {
            thread.add("comments", new JsonArray());
        }
        JsonArray comments = thread.getAsJsonArray("comments");
        JsonObject c = new JsonObject();
        c.addProperty("moId", m);
        c.addProperty("text", body);
        c.addProperty("createdAt", Instant.now().toString());
        comments.add(c);
        touchMeta(root);
        writeRoot(root);
        return c;
    }

    private JsonObject findThread(String applicationId) throws IOException {
        if (applicationId.isEmpty()) {
            return null;
        }
        JsonObject root = ensureRoot();
        for (JsonElement element : root.getAsJsonArray("items")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            if (applicationId.equalsIgnoreCase(getAsString(row, "applicationId"))) {
                return row;
            }
        }
        return null;
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
