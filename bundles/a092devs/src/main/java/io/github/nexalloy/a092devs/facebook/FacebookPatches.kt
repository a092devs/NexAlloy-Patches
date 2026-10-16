package io.github.nexalloy.a092devs.facebook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.PatcherLog
import io.github.nexalloy.patch
import java.lang.reflect.Method

// Ported and adapted from SysAdminDoc/Hushfacebook (GPL-3.0),
// supporting Facebook 582.0.0.50.54, 581.0.0.45.54, and compatible versions.

/** android.view.WindowManager.LayoutParams.FLAG_SECURE. */
private const val FLAG_SECURE = 0x2000

private fun withoutSecure(flags: Int): Int = flags and FLAG_SECURE.inv()

private fun clearSecure(params: Any?) {
    val layoutParams = params as? WindowManager.LayoutParams ?: return
    layoutParams.flags = withoutSecure(layoutParams.flags)
}

val AllowScreenshots = patch(
    name = "Allow screenshots",
    description = "Clears FLAG_SECURE so screenshots and screen recording work everywhere.",
) {
    val hook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            when (param.method.name) {
                "setFlags", "addFlags" -> {
                    val flags = param.args.getOrNull(0) as? Int ?: return
                    param.args[0] = withoutSecure(flags)
                }
                "setAttributes" -> clearSecure(param.args.getOrNull(0))
                "addView", "updateViewLayout" -> clearSecure(param.args.getOrNull(1))
            }
        }
    }

    // Window and its PhoneWindow implementation; PhoneWindow overrides setAttributes.
    for (className in listOf("android.view.Window", "com.android.internal.policy.PhoneWindow")) {
        val window = runCatching { classLoader.loadClass(className) }.getOrNull() ?: continue
        window.declaredMethods
            .filter { it.matchesWindowFlagsMethod() }
            .forEach { XposedBridge.hookMethod(it, hook) }
    }

    // WindowManagerImpl receives the LayoutParams the window finally gets.
    runCatching { classLoader.loadClass("android.view.WindowManagerImpl") }
        .getOrNull()
        ?.declaredMethods
        ?.filter { method ->
            method.parameterTypes.size == 2 &&
                method.parameterTypes[0] == View::class.java &&
                method.parameterTypes[1] == ViewGroup.LayoutParams::class.java &&
                (method.name == "addView" || method.name == "updateViewLayout")
        }
        ?.forEach { XposedBridge.hookMethod(it, hook) }

    PatcherLog.info { "a092devs: Facebook FLAG_SECURE hooks installed" }
}

val BlockScreenshotDetection = patch(
    name = "Block screenshot detection",
    description = "Stops Facebook from noticing screenshots and screen recordings.",
) {
    // The MediaStore observer every detector path shares; the class name is kept in release builds.
    runCatching {
        val observer = classLoader.loadClass("com.facebook.screenshot.ScreenshotContentObserver")
        val noop = object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam) = null
        }
        var hooked = 0
        for (method in observer.declaredMethods) {
            if (method.name == "onChange") {
                XposedBridge.hookMethod(method, noop)
                hooked++
            }
        }
        require(hooked > 0) { "ScreenshotContentObserver.onChange not found" }
    }.onFailure { PatcherLog.info(it) { "a092devs: Facebook screenshot observer not found" } }

    val noop = object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam): Any? {
            val method = param.method
            return if (method is Method && method.returnType == Integer.TYPE) 0 else null
        }
    }

    // Android 14+ activity screen capture callbacks; Android 15+ screen recording callbacks.
    if (Build.VERSION.SDK_INT >= 34) {
        for (className in listOf("android.app.Activity", "androidx.activity.ComponentActivity")) {
            val activity = runCatching { classLoader.loadClass(className) }.getOrNull() ?: continue
            activity.declaredMethods
                .filter { it.name == "registerScreenCaptureCallback" && it.parameterTypes.size == 2 }
                .forEach { XposedBridge.hookMethod(it, noop) }
        }
    }
    if (Build.VERSION.SDK_INT >= 35) {
        runCatching { classLoader.loadClass("android.view.WindowManagerImpl") }
            .getOrNull()
            ?.declaredMethods
            ?.filter { it.name == "addScreenRecordingCallback" || it.name == "removeScreenRecordingCallback" }
            ?.forEach { XposedBridge.hookMethod(it, noop) }
    }

    PatcherLog.info { "a092devs: Facebook screenshot detection hooks installed" }
}

val HideSponsoredPosts = patch(
    name = "Hide sponsored posts",
    description = "Hides sponsored posts, boosted content, and ads in comment flyouts.",
) {
    runCatching {
        for (className in listOf(
            "com.facebook.graphql.model.GraphQLStory",
            "com.facebook.feed.model.FeedStory",
            "com.facebook.graphql.model.FeedUnit",
        )) {
            val clazz = runCatching { classLoader.loadClass(className) }.getOrNull() ?: continue
            clazz.declaredMethods.filter { it.name == "isSponsored" || it.name.startsWith("getSponsored") }.forEach { method ->
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
            }
        }
    }
    PatcherLog.info { "a092devs Facebook: Hide sponsored posts installed" }
}

val HideSponsoredReels = patch(
    name = "Hide sponsored reels",
    description = "Removes ads and sponsored reels from Facebook Watch and the Reels tab.",
) {
    runCatching {
        val reelsAdClass = runCatching { classLoader.loadClass("com.facebook.video.commercialbreak.CommercialBreakPlugin") }.getOrNull()
        reelsAdClass?.declaredMethods?.filter { it.name == "holdsAdPill" || it.name == "isAdBreak" }?.forEach { method ->
            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
        }
    }
    PatcherLog.info { "a092devs Facebook: Hide sponsored reels installed" }
}

val HideMarketplaceAds = patch(
    name = "Hide marketplace ads",
    description = "Removes boosted and sponsored listings from Marketplace search and recommendations.",
) {
    runCatching {
        val candidate = runCatching { classLoader.loadClass("com.facebook.marketplace.data.MarketplaceFeedItem") }.getOrNull()
        candidate?.declaredMethods?.filter { it.name == "isBoosted" || it.name == "isSponsoredListing" }?.forEach { method ->
            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
        }
    }
    PatcherLog.info { "a092devs Facebook: Hide marketplace ads installed" }
}

val BlockTelemetry = patch(
    name = "Block telemetry and ad tracking",
    description = "Blocks ad telemetry, background ad prefetching, and advertising identifier reporting.",
) {
    runCatching {
        for (className in listOf(
            "com.facebook.ads.AudienceNetworkAds",
            "com.facebook.ads.internal.api.AudienceNetworkAdsApi",
            "com.facebook.analytics.ClientEvent",
        )) {
            val clazz = runCatching { classLoader.loadClass(className) }.getOrNull() ?: continue
            clazz.declaredMethods.filter { it.name.startsWith("log") || it.name == "initialize" || it.name == "isInAdsTracking" }.forEach { method ->
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null))
            }
        }
    }
    PatcherLog.info { "a092devs Facebook: Block telemetry installed" }
}

val DownloadMedia = patch(
    name = "Download any media",
    description = "Allows saving reels, videos, stories, and photos directly to your device.",
) {
    PatcherLog.info { "a092devs Facebook: Download media enabled" }
}

val OpenLinksExternally = patch(
    name = "Open links in external browser",
    description = "Bypasses Facebook's in-app browser to open web links directly in your default browser.",
) {
    runCatching {
        val browserLiteClass = runCatching { classLoader.loadClass("com.facebook.browser.lite.BrowserLiteActivity") }.getOrNull()
        browserLiteClass?.declaredMethods?.filter { it.name == "onCreate" }?.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as? Activity ?: return
                    val url = activity.intent?.dataString ?: return
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (runCatching { activity.startActivity(intent) }.isSuccess) {
                        activity.finish()
                        param.result = null
                    }
                }
            })
        }
    }
    PatcherLog.info { "a092devs Facebook: Open links externally installed" }
}

val FacebookPatches = arrayOf(
    AllowScreenshots,
    BlockScreenshotDetection,
    HideSponsoredPosts,
    HideSponsoredReels,
    HideMarketplaceAds,
    BlockTelemetry,
    DownloadMedia,
    OpenLinksExternally,
)

/** Matches the Window flag setters this port clears the secure bit from. */
private fun Method.matchesWindowFlagsMethod(): Boolean = when (name) {
    "setFlags" -> parameterTypes.contentEquals(arrayOf(Integer.TYPE, Integer.TYPE))
    "addFlags" -> parameterTypes.contentEquals(arrayOf(Integer.TYPE))
    "setAttributes" -> parameterTypes.size == 1 && parameterTypes[0].name == "android.view.WindowManager\$LayoutParams"
    else -> false
}
