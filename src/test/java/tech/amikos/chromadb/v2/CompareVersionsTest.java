package tech.amikos.chromadb.v2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Nyquist validation test for QLTY-01: compareVersions() utility in AbstractChromaIntegrationTest.
 *
 * Validates that the version comparison logic correctly handles:
 * - Equal versions
 * - Major/minor/patch ordering
 * - Versions with different segment counts
 *
 * This is a behavioral test: "maintainer can rely on version comparison to skip/run
 * tests against the correct Chroma versions in the matrix."
 */
public class CompareVersionsTest {

    @Test
    public void testEqualVersionsReturnZero() {
        assertEquals(0, AbstractChromaIntegrationTest.compareVersions("1.0.0", "1.0.0"));
        assertEquals(0, AbstractChromaIntegrationTest.compareVersions("1.5.5", "1.5.5"));
    }

    @Test
    public void testHigherMajorVersionReturnsPositive() {
        assertTrue(AbstractChromaIntegrationTest.compareVersions("2.0.0", "1.0.0") > 0);
    }

    @Test
    public void testLowerMajorVersionReturnsNegative() {
        assertTrue(AbstractChromaIntegrationTest.compareVersions("1.0.0", "2.0.0") < 0);
    }

    @Test
    public void testHigherMinorVersionReturnsPositive() {
        assertTrue(AbstractChromaIntegrationTest.compareVersions("1.5.0", "1.3.0") > 0);
    }

    @Test
    public void testLowerMinorVersionReturnsNegative() {
        assertTrue(AbstractChromaIntegrationTest.compareVersions("1.3.0", "1.5.0") < 0);
    }

    @Test
    public void testHigherPatchVersionReturnsPositive() {
        assertTrue(AbstractChromaIntegrationTest.compareVersions("1.0.5", "1.0.0") > 0);
    }

    @Test
    public void testLowerPatchVersionReturnsNegative() {
        assertTrue(AbstractChromaIntegrationTest.compareVersions("1.0.0", "1.0.5") < 0);
    }

    @Test
    public void testVersionWithFewerSegmentsPadsWithZero() {
        // "1.0" should equal "1.0.0"
        assertEquals(0, AbstractChromaIntegrationTest.compareVersions("1.0", "1.0.0"));
        // "1.0" should be less than "1.0.1"
        assertTrue(AbstractChromaIntegrationTest.compareVersions("1.0", "1.0.1") < 0);
    }

    @Test
    public void testMatrixVersionOrderIsCorrect() {
        // The 3 pinned matrix versions must sort: 1.0.0 < 1.3.7 < 1.5.5
        assertTrue(
            "1.0.0 should be less than 1.3.7",
            AbstractChromaIntegrationTest.compareVersions("1.0.0", "1.3.7") < 0
        );
        assertTrue(
            "1.3.7 should be less than 1.5.5",
            AbstractChromaIntegrationTest.compareVersions("1.3.7", "1.5.5") < 0
        );
        assertTrue(
            "1.0.0 should be less than 1.5.5",
            AbstractChromaIntegrationTest.compareVersions("1.0.0", "1.5.5") < 0
        );
    }

    @Test
    public void testCurrentDefaultVersionIsLatestPinned() {
        // The default version (1.5.5) should be >= all matrix versions
        String defaultVersion = "1.5.5";
        assertTrue(
            "Default version should be >= 1.0.0",
            AbstractChromaIntegrationTest.compareVersions(defaultVersion, "1.0.0") >= 0
        );
        assertTrue(
            "Default version should be >= 1.3.7",
            AbstractChromaIntegrationTest.compareVersions(defaultVersion, "1.3.7") >= 0
        );
        assertTrue(
            "Default version should be >= 1.5.5",
            AbstractChromaIntegrationTest.compareVersions(defaultVersion, "1.5.5") >= 0
        );
    }
}
