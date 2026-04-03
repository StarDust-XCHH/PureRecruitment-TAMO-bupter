package com.bupt.tarecruit.common.util;

import com.bupt.tarecruit.common.model.ApiResponse;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 将 {@link ApiResponse} 及常见 Java 值（Map、List、String 等）**手写**为 JSON 字符串，供 Servlet 直接写出。
 * <p>
 * 与 {@link GsonJsonObjectUtils} 无重叠：本类<strong>不</strong>使用 Gson {@code JsonObject}；若需解析/读取请求体或文件中的 Gson 结构，请用 {@code GsonJsonObjectUtils}。
 */
public final class JsonUtils {
    private JsonUtils() {
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return '"' + escaped + '"';
    }

    public static String toJson(ApiResponse<?> response) {
        StringBuilder builder = new StringBuilder();
        builder.append('{')
                .append("\"success\":")
                .append(response.isSuccess())
                .append(',')
                .append("\"message\":")
                .append(quote(response.getMessage()))
                .append(',')
                .append("\"data\":")
                .append(toJsonValue(response.getData()))
                .append('}');
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    public static String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return quote(stringValue);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                builder.append(quote(String.valueOf(entry.getKey())))
                        .append(':')
                        .append(toJsonValue(entry.getValue()));
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append('}');
            return builder.toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            for (int i = 0; i < list.size(); i++) {
                builder.append(toJsonValue(list.get(i)));
                if (i < list.size() - 1) {
                    builder.append(',');
                }
            }
            builder.append(']');
            return builder.toString();
        }
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            for (int i = 0; i < array.length; i++) {
                builder.append(toJsonValue(array[i]));
                if (i < array.length - 1) {
                    builder.append(',');
                }
            }
            builder.append(']');
            return builder.toString();
        }
        return quote(String.valueOf(value));
    }
}
