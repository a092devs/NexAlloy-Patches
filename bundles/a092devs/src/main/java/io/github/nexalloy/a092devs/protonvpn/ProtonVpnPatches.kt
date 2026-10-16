package io.github.nexalloy.a092devs.protonvpn

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.PatcherLog
import io.github.nexalloy.patch

// Ported from Paresh-Maheshwari/paresh-patches (GPL-3.0):
// - Disable telemetry, analytics, and background worker scheduling
// - Premium UI unlock: remove onboarding upgrade popups, hide upgrade banners,
//   and enable NetShield UI setting.

val ProtonVpnDisableTelemetry = patch(
    name = "Disable telemetry",
    description = "Blocks telemetry, analytics, and observability data collection in Proton VPN.",
) {
    // Block Proton Core telemetry worker scheduling
    runCatching {
        val workerClass = classLoader.loadClass("me.proton.core.telemetry.data.worker.TelemetryWorkerManagerImpl")
        workerClass.declaredMethods.filter { it.name.startsWith("enqueueOrKeep") }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            })
        }
    }

    // Block observability events sending
    runCatching {
        val obsClass = classLoader.loadClass("me.proton.core.observability.data.usecase.SendObservabilityEventsImpl")
        obsClass.declaredMethods.filter { it.name == "invoke" }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = Unit
                }
            })
        }
    }

    // Block VPN telemetry event recording
    runCatching {
        val vpnTelemetryClass = classLoader.loadClass("com.protonvpn.android.telemetry.VpnTelemetry")
        vpnTelemetryClass.declaredMethods.filter { it.name == "addEvent" }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = Unit
                }
            })
        }
    }
}

val ProtonVpnPremiumUi = patch(
    name = "Premium UI & Ad-blocker",
    description = "Removes upgrade onboarding prompts, unlocks NetShield ad-blocker setting, and removes upsell banners.",
) {
    // Skip upgrade onboarding dialog after login
    runCatching {
        val launcherClass = classLoader.loadClass("com.protonvpn.android.ui.planupgrade.UpgradeDialogLauncherVM")
        XposedHelpers.findAndHookMethod(
            launcherClass,
            "launchOnboarding",
            android.content.Context::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
    }

    // NetShield availability -> AVAILABLE
    runCatching {
        val netShieldEnumClass = classLoader.loadClass("com.protonvpn.android.netshield.NetShieldAvailability")
        val availableConstant = netShieldEnumClass.enumConstants?.firstOrNull { (it as Enum<*>).name == "AVAILABLE" }
        if (availableConstant != null) {
            val netShieldManagerClass = classLoader.loadClass("com.protonvpn.android.netshield.NetShieldManager")
            netShieldManagerClass.declaredMethods.filter { it.returnType == netShieldEnumClass }.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = availableConstant
                    }
                })
            }
        }
    }

    // VpnUser tier overrides: report Plus tier for UI display without affecting server auth
    runCatching {
        val userClass = classLoader.loadClass("com.protonvpn.android.auth.data.VpnUser")
        XposedHelpers.findAndHookMethod(userClass, "getUserTier", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = 2
            }
        })
        XposedHelpers.findAndHookMethod(userClass, "isUserPlusOrAbove", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = true
            }
        })
        XposedHelpers.findAndHookMethod(userClass, "isFreeUser", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = false
            }
        })
        XposedHelpers.findAndHookMethod(userClass, "getUserTierName", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = "vpn2022"
            }
        })
        runCatching {
            XposedHelpers.findAndHookMethod(userClass, "getMaxTier", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = java.lang.Integer.valueOf(2)
                }
            })
        }
    }
}

val ProtonVpnPatches = arrayOf(
    ProtonVpnDisableTelemetry,
    ProtonVpnPremiumUi,
)

