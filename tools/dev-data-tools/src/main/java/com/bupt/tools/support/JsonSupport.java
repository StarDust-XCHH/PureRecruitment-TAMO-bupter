package com.bupt.tools.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class JsonSupport {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private JsonSupport() {
    }

    public static boolean isJsonFile(Path path) {
        return path != null && String.valueOf(path.getFileName()).toLowerCase().endsWith(".json");
    }

    public static JsonElement readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
    }

    public static void writeJson(Path path, JsonElement jsonElement) throws IOException {
        FileSupport.ensureDirectory(path.getParent());
        Files.writeString(path, GSON.toJson(jsonElement), StandardCharsets.UTF_8);
    }

    public static boolean jsonEqualsIgnoringVolatileFields(Path leftPath, Path rightPath) throws IOException {
        JsonElement left = sanitize(readJson(leftPath));
        JsonElement right = sanitize(readJson(rightPath));
        return left.equals(right);
    }

    public static String summarizeDifference(Path leftPath, Path rightPath) throws IOException {
        JsonElement left = sanitize(readJson(leftPath));
        JsonElement right = sanitize(readJson(rightPath));
        if (left.equals(right)) {
            return "仅运行态字段不同或内容完全一致";
        }
        return "存在有效 JSON 内容差异";
    }

    public static JsonElement mergeWorkspaceIntoBaseline(Path baselinePath, Path workspacePath) throws IOException {
        JsonElement baseline = readJson(baselinePath);
        JsonElement workspace = readJson(workspacePath);
        return mergePreservingVolatileFields(baseline, workspace);
    }

    private static JsonElement sanitize(JsonElement element) {
        if (element == null || element instanceof JsonNull) {
            return JsonNull.INSTANCE;
        }
        if (element.isJsonObject()) {
            JsonObject source = element.getAsJsonObject();
            Map<String, JsonElement> ordered = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
                if (SyncRules.shouldIgnoreField(entry.getKey())) {
                    continue;
                }
                ordered.put(entry.getKey(), sanitize(entry.getValue()));
            }
            JsonObject result = new JsonObject();
            ordered.forEach(result::add);
            return result;
        }
        if (element.isJsonArray()) {
            JsonArray source = element.getAsJsonArray();
            List<JsonElement> items = new ArrayList<>();
            source.forEach(item -> items.add(sanitize(item)));
            JsonArray result = new JsonArray();
            items.forEach(result::add);
            return result;
        }
        return element.deepCopy();
    }

    private static JsonElement mergePreservingVolatileFields(JsonElement baseline, JsonElement workspace) {
        if (baseline == null || baseline instanceof JsonNull) {
            return workspace == null ? JsonNull.INSTANCE : workspace.deepCopy();
        }
        if (workspace == null || workspace instanceof JsonNull) {
            return baseline.deepCopy();
        }
        if (baseline.isJsonObject() && workspace.isJsonObject()) {
            JsonObject merged = new JsonObject();
            JsonObject baselineObject = baseline.getAsJsonObject();
            JsonObject workspaceObject = workspace.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : workspaceObject.entrySet()) {
                String key = entry.getKey();
                if (SyncRules.shouldIgnoreField(key) && baselineObject.has(key)) {
                    merged.add(key, baselineObject.get(key).deepCopy());
                } else {
                    JsonElement baselineValue = baselineObject.has(key) ? baselineObject.get(key) : JsonNull.INSTANCE;
                    merged.add(key, mergePreservingVolatileFields(baselineValue, entry.getValue()));
                }
            }

            for (Map.Entry<String, JsonElement> entry : baselineObject.entrySet()) {
                if (!merged.has(entry.getKey())) {
                    merged.add(entry.getKey(), entry.getValue().deepCopy());
                }
            }
            return merged;
        }
        if (baseline.isJsonArray() && workspace.isJsonArray()) {
            JsonArray result = new JsonArray();
            workspace.getAsJsonArray().forEach(item -> result.add(item.deepCopy()));
            return result;
        }
        if (baseline.isJsonPrimitive() && workspace.isJsonPrimitive()) {
            JsonPrimitive workspacePrimitive = workspace.getAsJsonPrimitive();
            return workspacePrimitive.deepCopy();
        }
        return workspace.deepCopy();
    }
}
