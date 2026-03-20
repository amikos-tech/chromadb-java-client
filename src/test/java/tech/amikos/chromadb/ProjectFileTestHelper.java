package tech.amikos.chromadb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Shared helper for Nyquist validation tests that read project root files.
 * Used by Phase05, Phase06, and Phase07 validation tests.
 */
public final class ProjectFileTestHelper {

    private ProjectFileTestHelper() {}

    /**
     * Walk up from CWD to find the project root (directory containing both pom.xml and README.md).
     * Falls back to CWD if not found within 10 levels.
     */
    public static Path findProjectRoot() {
        Path candidate = Paths.get(System.getProperty("user.dir"));
        for (int i = 0; i < 10; i++) {
            if (Files.exists(candidate.resolve("pom.xml"))
                    && Files.exists(candidate.resolve("README.md"))) {
                return candidate;
            }
            candidate = candidate.getParent();
            if (candidate == null) break;
        }
        return Paths.get(System.getProperty("user.dir"));
    }

    /**
     * Read a file as a UTF-8 string. Fails with a JUnit assertion if the file does not exist.
     */
    public static String readFile(Path path) throws IOException {
        assertTrue("Required file not found: " + path, Files.exists(path));
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
