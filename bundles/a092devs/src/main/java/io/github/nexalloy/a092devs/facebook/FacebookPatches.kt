package io.github.nexalloy.a092devs.facebook

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

// Ported from SysAdminDoc/Hushfacebook (GPL-3.0),
// patches/src/main/kotlin/app/morphe/patches/facebook/misc/screenshots/.
//
// Upstream rewrites every set-window-flags call site and the screenshot observer in the APK. This
// port hooks the framework methods those call sites reach (Window, PhoneWindow, WindowManagerImpl,
// the screen capture callback registrations), and switches the observer off at its own method.

/** android.view.WindowManager.LayoutParams.FLAG_SECURE. */
private const val FLAG_SECURE = 0x2000

private fun withoutSecure(flags: Int): Int = flags and FLAG_SECURE.inv()

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

val FacebookPatches = arrayOf(
    AllowScreenshots,
    BlockScreenshotDetection,
)

/** Matches the Window flag setters this port clears the secure bit from. */
private fun Method.matchesWindowFlagsMethod(): Boolean = when (name) {
    "setFlags" -> parameterTypes.contentEquals(arrayOf(Integer.TYPE, Integer.TYPE))
    "addFlags" -> parameterTypes.contentEquals(arrayOf(Integer.TYPE))
    "setAttributes" -> parameterTypes.size == 1 && parameterTypes[0].name == "android.view.WindowManager\$LayoutParams"
    else -> false
}

private fun clearSecure(params: Any?) {
    val layoutParams = params as? WindowManager.LayoutParams ?: return
    layoutParams.flags = withoutSecure(layoutParams.flags)
}
