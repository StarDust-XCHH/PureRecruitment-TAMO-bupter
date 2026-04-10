package com.bupt.tools.support;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class SyncRules {
    private static final Set<String> IGNORED_DIRECTORY_NAMES = Set.of(
            ".git",
            ".workspace",
            "target",
            "image",
            "uploads",
            "avatars",
            "exports",
            "resumes"
    );

    private static final Set<String> IGNORED_FIELDS = Set.of(
            "updatedAt",
            "createdAt",
            "lastUpdatedAt",
            "lastLoginAt",
            "failedAttempts",
            "profileSavedAt",
            "loginAt"
    );

    private SyncRules() {
    }

    public static boolean shouldIgnoreDirectory(Path directory) {
        return directory != null && IGNORED_DIRECTORY_NAMES.contains(String.valueOf(directory.getFileName()));
    }

    public static boolean shouldIgnorePath(Path relativePath) {
        if (relativePath == null) {
            return false;
        }
        for (Path part : relativePath) {
            if (IGNORED_DIRECTORY_NAMES.contains(String.valueOf(part))) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldIgnoreField(String fieldName) {
        return fieldName != null && IGNORED_FIELDS.contains(fieldName);
    }

    public static List<String> ignoredDirectories() {
        return IGNORED_DIRECTORY_NAMES.stream().sorted().toList();
    }

    public static List<String> ignoredFields() {
        return IGNORED_FIELDS.stream().sorted().toList();
    }
}
