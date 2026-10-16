package io.github.nexalloy.patches

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionUtilTest {

    @Test
    fun `test numeric version comparison cases`() {
        // 21.39.0 is newer than 21.9.0
        assertTrue(compareAppVersions("21.39.0", "21.9.0") > 0)
        assertTrue(isVersionAtLeast("21.39.0", "21.9.0"))
        assertFalse(isVersionAtLeast("21.9.0", "21.39.0"))

        // 2026.16.0 is newer than 2026.4.0
        assertTrue(compareAppVersions("2026.16.0", "2026.4.0") > 0)
        assertTrue(isVersionAtLeast("2026.16.0", "2026.4.0"))
        assertFalse(isVersionAtLeast("2026.4.0", "2026.16.0"))

        // 9.33.0 is newer than 9.9.0
        assertTrue(compareAppVersions("9.33.0", "9.9.0") > 0)
        assertTrue(isVersionAtLeast("9.33.0", "9.9.0"))
        assertFalse(isVersionAtLeast("9.9.0", "9.33.0"))

        // 21.39.0 equals 21.39.0
        assertEquals(0, compareAppVersions("21.39.0", "21.39.0"))
        assertTrue(isVersionAtLeast("21.39.0", "21.39.0"))

        // 21.39.0 is older than 21.40.0
        assertTrue(compareAppVersions("21.39.0", "21.40.0") < 0)
        assertFalse(isVersionAtLeast("21.39.0", "21.40.0"))
        assertTrue(isVersionAtLeast("21.40.0", "21.39.0"))
    }

    @Test
    fun `test trailing zero segments and differing segment lengths`() {
        // 21.39 equals 21.39.0
        assertEquals(0, compareAppVersions("21.39", "21.39.0"))
        assertTrue(isVersionAtLeast("21.39", "21.39.0"))
        assertTrue(isVersionAtLeast("21.39.0", "21.39"))

        // 21.39.0.0 equals 21.39
        assertEquals(0, compareAppVersions("21.39.0.0", "21.39"))
        assertTrue(isVersionAtLeast("21.39.0.0", "21.39"))

        // 21.39 is older than 21.39.1
        assertTrue(compareAppVersions("21.39", "21.39.1") < 0)
        assertFalse(isVersionAtLeast("21.39", "21.39.1"))
        assertTrue(isVersionAtLeast("21.39.1", "21.39"))

        // Leading zeroes in segment (e.g., 20.22.00 vs 20.22.0, YouTube naming)
        assertEquals(0, compareAppVersions("20.22.00", "20.22.0"))
        assertTrue(isVersionAtLeast("20.22.00", "20.22.0"))
        assertTrue(isVersionAtLeast("20.22.0", "20.22.00"))

        // 21.05.000 vs 21.5.0
        assertEquals(0, compareAppVersions("21.05.000", "21.5.0"))
        assertTrue(isVersionAtLeast("21.05.000", "21.5.0"))
    }

    @Test
    fun `test suffixes and prerelease ordering`() {
        // Release version has higher precedence than prerelease version with identical base
        assertTrue(compareAppVersions("21.39.0", "21.39.0-alpha") > 0)
        assertTrue(isVersionAtLeast("21.39.0", "21.39.0-alpha"))
        assertFalse(isVersionAtLeast("21.39.0-alpha", "21.39.0"))

        assertTrue(compareAppVersions("21.39.0", "21.39.0-beta.1") > 0)
        assertTrue(isVersionAtLeast("21.39.0", "21.39.0-beta.1"))
        assertFalse(isVersionAtLeast("21.39.0-beta.1", "21.39.0"))

        // Base version difference takes precedence over prerelease
        assertTrue(compareAppVersions("21.40.0-alpha", "21.39.0") > 0)
        assertTrue(isVersionAtLeast("21.40.0-alpha", "21.39.0"))
        assertFalse(isVersionAtLeast("21.39.0", "21.40.0-alpha"))

        // Prerelease ordering between different identifiers
        assertTrue(compareAppVersions("21.39.0-alpha", "21.39.0-beta") < 0)
        assertFalse(isVersionAtLeast("21.39.0-alpha", "21.39.0-beta"))
        assertTrue(isVersionAtLeast("21.39.0-beta", "21.39.0-alpha"))

        // Numeric prerelease identifiers (release.0 vs release.1)
        assertTrue(compareAppVersions("12.19.1-release.0", "12.19.1-release.1") < 0)
        assertFalse(isVersionAtLeast("12.19.1-release.0", "12.19.1-release.1"))
        assertTrue(isVersionAtLeast("12.19.1-release.1", "12.19.1-release.0"))

        // Alpha.1 vs Alpha.2
        assertTrue(compareAppVersions("21.39.0-alpha.1", "21.39.0-alpha.2") < 0)
        assertFalse(isVersionAtLeast("21.39.0-alpha.1", "21.39.0-alpha.2"))
        assertTrue(isVersionAtLeast("21.39.0-alpha.2", "21.39.0-alpha.1"))

        // Same prerelease versions
        assertEquals(0, compareAppVersions("21.39.0-alpha.1", "21.39.0-alpha.1"))
        assertTrue(isVersionAtLeast("21.39.0-alpha.1", "21.39.0-alpha.1"))

        // Build metadata after '+' does not alter precedence
        assertEquals(0, compareAppVersions("21.39.0+build1", "21.39.0+build2"))
        assertTrue(isVersionAtLeast("21.39.0+build1", "21.39.0+build2"))
    }

    @Test
    fun `test non-numeric and malformed segment rejection`() {
        // 21abc vs 21.39.0: '21abc' has non-numeric characters in base segment -> invalid
        assertFalse(AppVersion.parse("21abc").isValid)
        assertTrue(compareAppVersions("21abc", "21.39.0") < 0)
        assertTrue(compareAppVersions("21.39.0", "21abc") > 0)
        assertFalse(isVersionAtLeast("21abc", "21.39.0"))
        assertFalse(isVersionAtLeast("21.39.0", "21abc"))

        // 21.foo.39 vs 21.39.0: 'foo' is non-numeric -> invalid
        assertFalse(AppVersion.parse("21.foo.39").isValid)
        assertTrue(compareAppVersions("21.foo.39", "21.39.0") < 0)
        assertTrue(compareAppVersions("21.39.0", "21.foo.39") > 0)
        assertFalse(isVersionAtLeast("21.foo.39", "21.39.0"))
        assertFalse(isVersionAtLeast("21.39.0", "21.foo.39"))

        // v21.39.0 vs 21.39.0: leading 'v' is non-numeric -> invalid (Android package versionNames do not include 'v')
        assertFalse(AppVersion.parse("v21.39.0").isValid)
        assertTrue(compareAppVersions("v21.39.0", "21.39.0") < 0)
        assertTrue(compareAppVersions("21.39.0", "v21.39.0") > 0)
        assertFalse(isVersionAtLeast("v21.39.0", "21.39.0"))
        assertFalse(isVersionAtLeast("21.39.0", "v21.39.0"))

        // Dangling hyphen or empty tokens
        assertFalse(AppVersion.parse("21.39.0-").isValid)
        assertFalse(AppVersion.parse("-alpha").isValid)
        assertFalse(AppVersion.parse("21..39").isValid)
        assertFalse(isVersionAtLeast("21.39.0-", "21.39.0"))
        assertFalse(isVersionAtLeast("21.39.0", "21.39.0-"))
    }

    @Test
    fun `test blank and malformed inputs deterministically`() {
        // Empty string vs valid version
        assertFalse(AppVersion.parse("").isValid)
        assertTrue(compareAppVersions("", "21.39.0") < 0)
        assertTrue(compareAppVersions("21.39.0", "") > 0)
        assertFalse(isVersionAtLeast("", "21.39.0"))
        assertFalse(isVersionAtLeast("21.39.0", ""))

        // Blank whitespace string vs valid version
        assertFalse(AppVersion.parse("   ").isValid)
        assertTrue(compareAppVersions("   ", "21.39.0") < 0)
        assertTrue(compareAppVersions("21.39.0", "   ") > 0)
        assertFalse(isVersionAtLeast("   ", "21.39.0"))
        assertFalse(isVersionAtLeast("21.39.0", "   "))

        // Garbage strings
        assertFalse(AppVersion.parse("unknown").isValid)
        assertFalse(AppVersion.parse("invalid").isValid)
        assertFalse(isVersionAtLeast("unknown", "21.39.0"))
        assertFalse(isVersionAtLeast("invalid", "21.39.0"))
        assertFalse(isVersionAtLeast("21.39.0", "unknown"))
        assertFalse(isVersionAtLeast("21.39.0", "invalid"))

        // Two invalid versions cannot be meaningfully ordered (return 0 in compareTo, false in isVersionAtLeast)
        assertEquals(0, compareAppVersions("", ""))
        assertEquals(0, compareAppVersions("invalid", "21abc"))
        assertEquals(0, compareAppVersions("v21.39.0", "21.foo.39"))
        assertFalse(isVersionAtLeast("", ""))
        assertFalse(isVersionAtLeast("invalid", "invalid"))
        assertFalse(isVersionAtLeast("21abc", "v21.39.0"))
    }
}
