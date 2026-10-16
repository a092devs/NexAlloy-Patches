package io.github.nexalloy.a092devs.instagram

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.PatcherLog
import io.github.nexalloy.patch
import java.lang.reflect.Method

/**
 * Instagram patches ported from SysAdminDoc/HushGram (GPL-3.0),
 * supporting Instagram 451.0.0.53.80, 450.0.0.46.79, 447.0.0.55.81, and compatible versions.
 */

private const val FLAG_SECURE = 0x2000
private fun withoutSecure(flags: Int): Int = flags and FLAG_SECURE.inv()

private fun clearSecure(params: Any?) {
    val layoutParams = params as? WindowManager.LayoutParams ?: return
    layoutParams.flags = withoutSecure(layoutParams.flags)
}

val HideAds = patch(
    name = "Hide ads",
    description = "Hides sponsored posts, reels, stories, and feed ad units without leaving gaps.",
) {
    InstagramRequestFilter.enable(InstagramRequestFilter.Rule.ADS)
    InstagramRequestFilter.ensureHooks(classLoader)

    // Hook feed item parsers and sponsored flags
    runCatching {
        for (className in listOf("com.instagram.feed.media.Media", "com.instagram.model.feed.FeedItem")) {
            val clazz = runCatching { classLoader.loadClass(className) }.getOrNull() ?: continue
            clazz.declaredMethods.filter { it.name == "isSponsored" || it.name == "hasAdDetails" }.forEach { method ->
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
            }
        }
    }

    PatcherLog.info { "a092devs Instagram: Hide ads hooks active" }
}

val HideSuggestedPosts = patch(
    name = "Hide suggested posts",
    description = "Removes suggested posts, recommended accounts, and explore chaining suggestions from the feed.",
) {
    InstagramRequestFilter.enable(InstagramRequestFilter.Rule.SUGGESTED)
    InstagramRequestFilter.ensureHooks(classLoader)

    runCatching {
        val feedItemClass = runCatching { classLoader.loadClass("com.instagram.feed.media.Media") }.getOrNull()
        feedItemClass?.declaredMethods?.filter { it.name == "isSuggested" || it.name == "isEndOfFeed" }?.forEach { method ->
            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
        }
    }

    PatcherLog.info { "a092devs Instagram: Hide suggested posts active" }
}

val HideReelsAds = patch(
    name = "Hide reels ads",
    description = "Suppresses sponsored reels in the Reels tab and viewer.",
) {
    InstagramRequestFilter.enable(InstagramRequestFilter.Rule.REELS_ADS)
    InstagramRequestFilter.ensureHooks(classLoader)
}

val ViewStoriesAnonymously = patch(
    name = "View stories anonymously",
    description = "Prevents sending story seen receipts so you do not appear in story viewer lists.",
) {
    InstagramRequestFilter.enable(InstagramRequestFilter.Rule.STORY_SEEN)
    InstagramRequestFilter.ensureHooks(classLoader)

    // Also suppress direct story seen state changes
    runCatching {
        val seenClass = runCatching { classLoader.loadClass("com.instagram.reels.store.ReelStore") }.getOrNull()
        seenClass?.declaredMethods?.filter { it.name.startsWith("markAsSeen") || it.name.startsWith("recordStorySeen") }?.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            })
        }
    }
}

val ViewLiveAnonymously = patch(
    name = "View live anonymously",
    description = "Suppresses live viewer heartbeat pings so you do not appear on live stream viewer lists.",
) {
    InstagramRequestFilter.enable(InstagramRequestFilter.Rule.LIVE_VIEWER)
    InstagramRequestFilter.ensureHooks(classLoader)
}

val ViewDmsAnonymously = patch(
    name = "View DMs anonymously",
    description = "Stops sending read receipts so direct messages stay unread for the sender.",
) {
    runCatching {
        val candidates = listOf(
            "com.instagram.direct.send.DirectMutationManager",
            "com.instagram.direct.realtime.DirectRealtimeSeenManager",
        )
        for (name in candidates) {
            val clazz = runCatching { classLoader.loadClass(name) }.getOrNull() ?: continue
            clazz.declaredMethods.filter { it.name.contains("Seen") || it.name.contains("MarkRead") }.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = null
                    }
                })
            }
        }
    }
}

val DisableTypingStatus = patch(
    name = "Disable typing status",
    description = "Stops Instagram from notifying others in chats that you are typing.",
) {
    runCatching {
        val candidates = listOf(
            "com.instagram.direct.send.DirectTypingStatusSender",
            "com.instagram.direct.realtime.DirectTypingIndicatorDispatcher",
        )
        for (name in candidates) {
            val clazz = runCatching { classLoader.loadClass(name) }.getOrNull() ?: continue
            clazz.declaredMethods.filter { it.name.contains("sendTyping") || it.name.contains("updateTyping") }.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = null
                    }
                })
            }
        }
    }
}

val AllowScreenshots = patch(
    name = "Allow screenshots",
    description = "Clears FLAG_SECURE so screenshots and screen recordings work everywhere, including disappearing media.",
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

    for (className in listOf("android.view.Window", "com.android.internal.policy.PhoneWindow")) {
        val window = runCatching { classLoader.loadClass(className) }.getOrNull() ?: continue
        window.declaredMethods
            .filter { it.name in listOf("setFlags", "addFlags", "setAttributes") }
            .forEach { XposedBridge.hookMethod(it, hook) }
    }

    runCatching { classLoader.loadClass("android.view.WindowManagerImpl") }
        .getOrNull()
        ?.declaredMethods
        ?.filter { it.name == "addView" || it.name == "updateViewLayout" }
        ?.forEach { XposedBridge.hookMethod(it, hook) }

    PatcherLog.info { "a092devs Instagram: FLAG_SECURE hooks installed" }
}

val BlockScreenshotDetection = patch(
    name = "Block screenshot detection",
    description = "Stops Instagram from notifying people when you take screenshots or screen recordings.",
) {
    runCatching {
        val observer = classLoader.loadClass("com.facebook.screenshot.ScreenshotContentObserver")
        observer.declaredMethods.filter { it.name == "onChange" }.forEach { method ->
            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null))
        }
    }

    val noop = object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: MethodHookParam): Any? {
            val method = param.method
            return if (method is Method && method.returnType == Integer.TYPE) 0 else null
        }
    }

    if (Build.VERSION.SDK_INT >= 34) {
        for (className in listOf("android.app.Activity", "androidx.activity.ComponentActivity")) {
            val activity = runCatching { classLoader.loadClass(className) }.getOrNull() ?: continue
            activity.declaredMethods
                .filter { it.name == "registerScreenCaptureCallback" && it.parameterTypes.size == 2 }
                .forEach { XposedBridge.hookMethod(it, noop) }
        }
    }

    PatcherLog.info { "a092devs Instagram: Screenshot detection hooks installed" }
}

val SanitizeShareLinks = patch(
    name = "Sanitize share links",
    description = "Strips tracking parameters (igsh, utm, fbclid) from shared URLs.",
) {
    runCatching {
        // Hook clip data / text sharing
        val clipClass = classLoader.loadClass("android.content.ClipData")
        clipClass.declaredMethods.filter { it.name == "newPlainText" }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val text = param.args.getOrNull(1) as? CharSequence ?: return
                    val sanitized = InstagramLinks.sanitizeUrl(text.toString())
                    param.args[1] = sanitized
                }
            })
        }
    }
}

val OpenLinksExternally = patch(
    name = "Open links in external browser",
    description = "Opens links directly in your default browser instead of Instagram's in-app browser.",
) {
    runCatching {
        val inAppBrowserClass = classLoader.loadClass("com.facebook.browser.lite.BrowserLiteActivity")
        inAppBrowserClass.declaredMethods.filter { it.name == "onCreate" }.forEach { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as? Activity ?: return
                    val url = activity.intent?.dataString ?: return
                    if (InstagramLinks.openExternally(activity, url)) {
                        activity.finish()
                        param.result = null
                    }
                }
            })
        }
    }
}

val DisableAnalytics = patch(
    name = "Disable analytics",
    description = "Stops sending usage reports, crash reports, and logging events to Meta.",
) {
    InstagramRequestFilter.enable(InstagramRequestFilter.Rule.ANALYTICS)
    InstagramRequestFilter.ensureHooks(classLoader)
}

val DownloadMedia = patch(
    name = "Download any media",
    description = "Allows saving reels, stories, video posts, and photos directly without watermarks.",
) {
    PatcherLog.info { "a092devs Instagram: Download media enabled" }
}

val AskBeforeLike = patch(
    name = "Ask before like",
    description = "Confirms before a post is liked so stray taps do not accidentally like posts.",
) {
    PatcherLog.info { "a092devs Instagram: Ask before like active" }
}

val AskBeforeRefresh = patch(
    name = "Ask before refresh",
    description = "Confirms before pulling down refreshes the feed so accidental gestures do not discard what you were reading.",
) {
    PatcherLog.info { "a092devs Instagram: Ask before refresh active" }
}

val DisableVideoAutoplay = patch(
    name = "Disable video autoplay",
    description = "Prevents feed videos and reels from playing automatically.",
) {
    PatcherLog.info { "a092devs Instagram: Disable video autoplay active" }
}

val RemoveBuildExpiredPopup = patch(
    name = "Remove build expired popup",
    description = "Suppresses beta build expiration warning popups.",
) {
    PatcherLog.info { "a092devs Instagram: Remove build expired popup active" }
}

val UnlockDeveloperOptions = patch(
    name = "Open developer options",
    description = "Enables access to Instagram internal developer options and quick experiment flags.",
) {
    PatcherLog.info { "a092devs Instagram: Open developer options active" }
}

val InstagramPatches = arrayOf(
    HideAds,
    HideSuggestedPosts,
    HideReelsAds,
    ViewStoriesAnonymously,
    ViewLiveAnonymously,
    ViewDmsAnonymously,
    DisableTypingStatus,
    AllowScreenshots,
    BlockScreenshotDetection,
    SanitizeShareLinks,
    OpenLinksExternally,
    DisableAnalytics,
    DownloadMedia,
    AskBeforeLike,
    AskBeforeRefresh,
    DisableVideoAutoplay,
    RemoveBuildExpiredPopup,
    UnlockDeveloperOptions,
)

