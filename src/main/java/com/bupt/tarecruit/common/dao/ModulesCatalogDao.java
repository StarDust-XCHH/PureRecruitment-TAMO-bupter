package com.bupt.tarecruit.common.dao;

import com.bupt.tarecruit.common.config.DataMountPaths;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Read-only access to {@code mountDataTAMObupter/common/modules-catalog.json}.
 */
public final class ModulesCatalogDao {

    private static final Object FILE_LOCK = new Object();
    private static final Path FILE = DataMountPaths.modulesCatalog();
    private ModulesCatalogDao() {
    }

    public static JsonObject readCatalog() throws IOException {
        synchronized (FILE_LOCK) {
            if (!Files.isRegularFile(FILE)) {
                JsonObject empty = new JsonObject();
                empty.addProperty("schema", "modules-catalog");
                empty.addProperty("version", "1.0");
                empty.addProperty("count", 0);
                empty.add("modules", new JsonArray());
                return empty;
            }
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
