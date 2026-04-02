package com.bupt.tarecruit.common.util;

import com.google.gson.JsonObject;

/**
 * Gson {@link JsonObject} 的纯函数式读字段与字符串工具，无 IO、无业务状态。
 * 与具体 DAO / 持久化类解耦，供任意层复用。
 */
public final class GsonJsonObjectUtils {

    private GsonJsonObjectUtils() {
    }

    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static String getAsString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    public static int getAsInt(JsonObject object, String key, int fallback) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ex) {
            return fallback;
        }
    }

    public static Integer getOptionalInt(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ex) {
            return null;
        }
    }

    public static String firstNonBlank(String primary, String fallback) {
        String first = trim(primary);
        return first.isBlank() ? trim(fallback) : first;
    }
}
