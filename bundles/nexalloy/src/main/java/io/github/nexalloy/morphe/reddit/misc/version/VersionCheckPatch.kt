package io.github.nexalloy.morphe.reddit.misc.version

import io.github.nexalloy.patch
import kotlin.properties.Delegates

var is_2024_03_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2025_48_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_04_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_11_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_14_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_15_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_16_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_18_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_20_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_21_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_24_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_25_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_26_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_30_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_32_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_36_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_38_0_or_greater: Boolean by Delegates.notNull()
    private set
var is_2026_39_0_or_greater: Boolean by Delegates.notNull()
    private set

val VersionCheck = patch {
    val versionName = appContext.packageManager
        .getPackageInfo(appContext.packageName, 0).versionName!!

    fun isEqualsOrGreaterThan(version: String): Boolean {
        return io.github.nexalloy.patches.isVersionAtLeast(versionName, version)
    }

    is_2024_03_0_or_greater = isEqualsOrGreaterThan("2024.03.0")
    is_2025_48_0_or_greater = isEqualsOrGreaterThan("2025.48.0")
    is_2026_04_0_or_greater = isEqualsOrGreaterThan("2026.04.0")
    is_2026_11_0_or_greater = isEqualsOrGreaterThan("2026.11.0")
    is_2026_14_0_or_greater = isEqualsOrGreaterThan("2026.14.0")
    is_2026_15_0_or_greater = isEqualsOrGreaterThan("2026.15.0")
    is_2026_16_0_or_greater = isEqualsOrGreaterThan("2026.16.0")
    is_2026_18_0_or_greater = isEqualsOrGreaterThan("2026.18.0")
    is_2026_20_0_or_greater = isEqualsOrGreaterThan("2026.20.0")
    is_2026_21_0_or_greater = isEqualsOrGreaterThan("2026.21.0")
    is_2026_24_0_or_greater = isEqualsOrGreaterThan("2026.24.0")
    is_2026_25_0_or_greater = isEqualsOrGreaterThan("2026.25.0")
    is_2026_26_0_or_greater = isEqualsOrGreaterThan("2026.26.0")
    is_2026_30_0_or_greater = isEqualsOrGreaterThan("2026.30.0")
    is_2026_32_0_or_greater = isEqualsOrGreaterThan("2026.32.0")
    is_2026_36_0_or_greater = isEqualsOrGreaterThan("2026.36.0")
    is_2026_38_0_or_greater = isEqualsOrGreaterThan("2026.38.0")
    is_2026_39_0_or_greater = isEqualsOrGreaterThan("2026.39.0")
}