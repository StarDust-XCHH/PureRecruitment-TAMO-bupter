package com.bupt.tools.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class FileSupport {
    private FileSupport() {
    }

    public static void ensureDirectory(Path directory) throws IOException {
        if (directory != null) {
            Files.createDirectories(directory);
        }
    }

    public static void resetDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> stream = Files.walk(directory)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException exception) {
                                throw new RuntimeException(exception);
                            }
                        });
            } catch (RuntimeException exception) {
                if (exception.getCause() instanceof IOException ioException) {
                    throw ioException;
                }
                throw exception;
            }
        }
        Files.createDirectories(directory);
    }

    public static List<Path> listRegularFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        List<Path> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .forEach(result::add);
        }
        result.sort(Comparator.comparing(Path::toString));
        return result;
    }

    public static void copyFile(Path source, Path target) throws IOException {
        ensureDirectory(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }
}
