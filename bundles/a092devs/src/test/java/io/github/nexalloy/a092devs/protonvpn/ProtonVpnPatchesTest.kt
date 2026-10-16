package io.github.nexalloy.a092devs.protonvpn

import io.github.nexalloy.a092devs.A092DevsPatches
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProtonVpnPatchesTest {

    @Test
    fun `bundle includes all expected proton vpn patches`() {
        assertEquals(5, ProtonVpnPatches.size)
        val names = ProtonVpnPatches.map { it.name }.toSet()
        assertTrue(names.contains("Disable telemetry"))
        assertTrue(names.contains("Remove server change delay"))
        assertTrue(names.contains("Unlock advanced settings & NetShield"))
        assertTrue(names.contains("Hide upgrade promotions"))
        assertTrue(names.contains("Plus UI badge"))
    }

    @Test
    fun `a092devs bundle declares proton vpn with device target version`() {
        val protonApp = A092DevsPatches.apps.firstOrNull { it.packageName == "ch.protonvpn.android" }
        assertNotNull(protonApp)
        assertEquals("Proton VPN", protonApp.appName)
        assertEquals(5, protonApp.patches.size)

        val targetVersions = protonApp.targets.map { it.version }.toSet()
        assertTrue(targetVersions.contains("5.20.74.0"), "Must support installed device version 5.20.74.0")
        assertTrue(targetVersions.contains("5.20.57.0"))
        assertTrue(targetVersions.contains("5.20.39.0"))
        assertTrue(targetVersions.contains("5.19.72.0"))
        assertTrue(targetVersions.contains("5.19.43.0"))
    }
}
