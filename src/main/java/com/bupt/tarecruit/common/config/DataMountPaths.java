package com.bupt.tarecruit.common.config;

import java.nio.file.Path;

public final class DataMountPaths {
    public static final String DATA_MOUNT_ENV = "mountDataTAMObupter";
    public static final Path DEFAULT_DATA_ROOT = Path.of("mountDataTAMObupter");

    private static final ResolvedDataRoot RESOLVED = resolveDataRoot();

    private DataMountPaths() {
    }

    public static Path root() {
        return RESOLVED.rootPath();
    }

    public static boolean fromEnvironment() {
        return RESOLVED.fromEnvironment();
    }

    /** Reserved for MO-only files under {@code <root>/mo}; not all MO data lives here. */
    @SuppressWarnings("unused")
    public static Path moDir() {
        return root().resolve("mo");
    }

    public static Path taDir() {
        return root().resolve("ta");
    }

    public static Path moRecruitmentCourses() {
        return root().resolve("common").resolve("recruitment-courses.json");
    }

    public static Path taAccounts() {
        return taDir().resolve("tas.json");
    }

    public static Path taProfiles() {
        return taDir().resolve("profiles.json");
    }

    public static Path taApplicationStatus() {
        return taDir().resolve("application-status.json");
    }

    private static ResolvedDataRoot resolveDataRoot() {
        String envValue = System.getenv(DATA_MOUNT_ENV);
        if (envValue == null || envValue.trim().isEmpty()) {
            return new ResolvedDataRoot(false, DEFAULT_DATA_ROOT.toAbsolutePath().normalize());
        }
        return new ResolvedDataRoot(true, Path.of(envValue.trim()).toAbsolutePath().normalize());
    }

    private record ResolvedDataRoot(boolean fromEnvironment, Path rootPath) {
    }
}

